/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Nov 27, 2008 (wiswedel): created
 */
package org.knime.core.data.container.blobs;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.knime.base.node.util.cache.CacheNodeFactory;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.util.FileUtil;
import org.knime.testing.data.blob.LargeBlobCell;
import org.knime.testing.node.differNode.DataTableDiffer;
import org.knime.testing.node.differNode.TestEvaluationException;
import org.knime.testing.node.runtime.RuntimeNodeFactory;
import org.knime.testing.node.runtime.RuntimeNodeModel;

/**
 * Creates a workflow of a node that creates a BDT with 20 large blob cells 
 * (each about 1MB) and then adds a sequence of Cache nodes. The chain is
 * ended by a tester node that compares the tables. The test case checks
 * wether the saved workflow is of the approximated size. 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class OneBlobManyTimesInTableWorkflowTest extends TestCase {

    private static final int ROW_COUNT = 200;
    private File m_wfmDir;
    private WorkflowManager m_flow;

    private static final BufferedDataTable createBDT(
            final ExecutionContext exec, final String prefix, final int rowCount) {
        BufferedDataContainer c = exec.createDataContainer(
                new DataTableSpec(new DataColumnSpecCreator(
                        "Blobs", LargeBlobCell.TYPE).createSpec()));
        LargeBlobCell cell = new LargeBlobCell("This is a big cell");
        for (int i = 0; i < rowCount; i++) {
            String s = prefix + "_" + i;
            c.addRowToTable(new DefaultRow(s, cell));
        }
        c.close();
        return c.getTable();
    }
    
    private static final long calculateSize(final File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                size += calculateSize(f);
            }
        } else if (dir.isFile()) {
            size += dir.length();
        }
        return size;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        m_wfmDir = FileUtil.createTempDir(getClass().getSimpleName());
        WorkflowManager m = WorkflowManager.ROOT.createAndAddProject();
        final String prefix = "testcase";
        RuntimeNodeModel createModel = new RuntimeNodeModel(0, 1) {
            /** {@inheritDoc} */
            @Override
            protected BufferedDataTable[] execute(BufferedDataTable[] inData,
                    ExecutionContext exec) throws Exception {
                return new BufferedDataTable[]{createBDT(exec, prefix, ROW_COUNT)};
            }
        };
        NodeID createID = m.createAndAddNode(new RuntimeNodeFactory(createModel));
        // add a sequence of cache nodes
        NodeID[] cacheIDs = new NodeID[10];
        CacheNodeFactory cacheNodeFactory = new CacheNodeFactory();
        for (int i = 0; i < cacheIDs.length; i++) {
            cacheIDs[i] = m.createAndAddNode(cacheNodeFactory);
            if (i == 0) {
                m.addConnection(createID, 0, cacheIDs[i], 0);
            } else {
                m.addConnection(cacheIDs[i - 1], 0, cacheIDs[i], 0);
            }
        }
        final AtomicReference<Throwable> failure = 
            new AtomicReference<Throwable>(); 
        RuntimeNodeModel checkModel = new RuntimeNodeModel(1, 0) {
            /** {@inheritDoc} */
            @Override
            protected BufferedDataTable[] execute(BufferedDataTable[] inData,
                    ExecutionContext exec) throws Exception {
                try {
                    new DataTableDiffer().compare(inData[0], 
                            createBDT(exec, prefix, ROW_COUNT));
                } catch (TestEvaluationException tee) {
                    failure.set(tee);
                    throw tee;
                }
                return new BufferedDataTable[]{};
            }
        };
        NodeID checkID = m.createAndAddNode(new RuntimeNodeFactory(checkModel));
        m.addConnection(cacheIDs[cacheIDs.length - 1], 0, checkID, 0);
        m_flow = m;
        m.executeAllAndWaitUntilDone();
        assertTrue(m.getState().equals(State.EXECUTED));
        assertNull(failure.get());
    }
    
    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        if (m_wfmDir != null) {
            FileUtil.deleteRecursively(m_wfmDir);
        }
        if (m_flow != null) {
            WorkflowManager.ROOT.removeProject(m_flow.getID());
        }
    }
    
    public void testSize() throws Exception {
        m_flow.save(m_wfmDir, new ExecutionMonitor(), true);
        long size = calculateSize(m_wfmDir);
        int sizeInMB = (int)(size / 1024 / 1024.0);
        long roughlyExpectedSize = LargeBlobCell.SIZE_OF_CELL;
        int roughlyExpectedSizeInMB = 
            (int)(roughlyExpectedSize / 1024 / 1024.0);
        String error = "Size of workflow out of range, expected ~" 
            + roughlyExpectedSizeInMB + "MB, actual " + sizeInMB + "MB";
        assertTrue(error, size > 0.8 * roughlyExpectedSize);
        assertTrue(error, size < 1.2 * roughlyExpectedSize);
    }

}
