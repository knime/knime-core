/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;
import org.knime.testing.node.differNode.DataTableDiffer;
import org.knime.testing.node.differNode.TestEvaluationException;
import org.knime.testing.node.runtime.RuntimeNodeFactory;
import org.knime.testing.node.runtime.RuntimeNodeModel;

/**
 * Creates a workflow of a node that creates a BDT with large blob cells
 * (each about 1MB) and then adds a sequence of Cache nodes. The chain is
 * ended by a tester node that compares the tables. The test case checks
 * whether the saved workflow is of the approximated size.
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractBlobsInWorkflowTest extends TestCase {

    private File m_wfmDir;
    private WorkflowManager m_flow;

    protected abstract BufferedDataTable createBDT(final ExecutionContext exec);

    protected abstract long getApproximateSize();

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
        WorkflowManager m =
            WorkflowManager.ROOT.createAndAddProject("Blob test");
        RuntimeNodeModel createModel = new RuntimeNodeModel(0, 1) {
            /** {@inheritDoc} */
            @Override
            protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                    final ExecutionContext exec) throws Exception {
                return new BufferedDataTable[]{createBDT(exec)};
            }
        };
        NodeID createID = m.createAndAddNode(new RuntimeNodeFactory(createModel));
        // add a sequence of cache nodes
        NodeID[] cacheIDs = new NodeID[10];
        CacheNodeFactory cacheNodeFactory = new CacheNodeFactory();
        for (int i = 0; i < cacheIDs.length; i++) {
            cacheIDs[i] = m.createAndAddNode(cacheNodeFactory);
            if (i == 0) {
                m.addConnection(createID, 1, cacheIDs[i], 1);
            } else {
                m.addConnection(cacheIDs[i - 1], 1, cacheIDs[i], 1);
            }
        }
        final AtomicReference<Throwable> failure =
            new AtomicReference<Throwable>();
        RuntimeNodeModel checkModel = new RuntimeNodeModel(1, 0) {
            /** {@inheritDoc} */
            @Override
            protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                    final ExecutionContext exec) throws Exception {
                try {
                    new DataTableDiffer().compare(inData[0], createBDT(exec));
                } catch (TestEvaluationException tee) {
                    failure.set(tee);
                    throw tee;
                }
                return new BufferedDataTable[]{};
            }
        };
        NodeID checkID = m.createAndAddNode(new RuntimeNodeFactory(checkModel));
        m.addConnection(cacheIDs[cacheIDs.length - 1], 1, checkID, 1);
        m_flow = m;
        m.executeAllAndWaitUntilDone();
        assertNull(failure.get());
        assertTrue(m.getNodeContainerState().isExecuted());
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
        double sizeInMB = size / 1024 / 1024.0;
        long roughlyExpectedSize = getApproximateSize();
        double roughlyExpectedSizeInMB = roughlyExpectedSize / 1024 / 1024.0;
        String error = "Size of workflow out of range, expected ~"
            + roughlyExpectedSizeInMB + "MB, actual " + sizeInMB + "MB";
        assertTrue(error, size > 0.8 * roughlyExpectedSize);
        assertTrue(error, size < 1.2 * roughlyExpectedSize);
        System.out.println("Test succeeds: expected size (in MB) is " + sizeInMB
                + ", expected " + roughlyExpectedSizeInMB + " -- which is ok");
    }

}
