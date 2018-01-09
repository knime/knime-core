/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.data.container.blobs;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.knime.base.node.util.cache.CacheNodeFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
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

        WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
        creationHelper.setWorkflowContext(new WorkflowContext.Factory(m_wfmDir).createContext());

        WorkflowManager m =
            WorkflowManager.ROOT.createAndAddProject("Blob test", creationHelper);
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
        if (m_flow != null) {
            WorkflowManager.ROOT.removeProject(m_flow.getID());
        }
        if (m_wfmDir != null) {
            FileUtil.deleteRecursively(m_wfmDir);
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
        NodeLogger.getLogger(getClass()).info("Test succeeds: expected size (in MB) is " + sizeInMB
                + ", expected " + roughlyExpectedSizeInMB + " -- which is ok");
    }

}
