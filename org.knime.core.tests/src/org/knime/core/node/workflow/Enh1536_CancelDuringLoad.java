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
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.node.adapter.AdapterNodeFactory;
import org.knime.core.node.workflow.node.adapter.AdapterNodeModel;
import org.knime.core.util.FileUtil;

/**
 * Tests if workflow loading can be canceled. Sets up a 3-node workflow and executes it. During the test the loading
 * gets canceled in the inner node's "loadInternals" method. The last node should then never be canceled and the
 * workflow should have been removed from the ROOTs workflow list.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Enh1536_CancelDuringLoad extends WorkflowTestCase {

    private enum LoadNodeState {
        Default,
        MiddleNodeCanceled,
        LastNodeLoaded;
    }

    private static ThreadLocal<DefaultNodeProgressMonitor> NODE_PROGRESS_THREAD_LOCAL = new ThreadLocal<>();
    private static ThreadLocal<LoadNodeState> LOAD_NODE_STATE_THREAD_LOCAL = new ThreadLocal<>();
    private File m_workflowDirectory;

    @Before
    public void setUp() throws Exception {
        m_workflowDirectory = FileUtil.createTempDir(getClass().getSimpleName());
        NODE_PROGRESS_THREAD_LOCAL.set(new DefaultNodeProgressMonitor());
        final WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
        creationHelper.setWorkflowContext(new WorkflowContext.Factory(m_workflowDirectory).createContext());
        WorkflowManager wm = WorkflowManager.ROOT.createAndAddProject(
            getClass().getSimpleName(), creationHelper);
        NodeID sourceNode = wm.createAndAddNode(new AdapterNodeFactory(true));
        NodeID cancelOnLoadNode = wm.createAndAddNode(new CancelDuringLoadInternalsNodeFactory());
        NodeID checkLoadInternalNotCalledNode = wm.createAndAddNode(new CheckLoadInternalsNotCalledNodeFactory());
        wm.addConnection(sourceNode, 1, cancelOnLoadNode, 1);
        wm.addConnection(cancelOnLoadNode, 1, checkLoadInternalNotCalledNode, 1);
        wm.executeAllAndWaitUntilDone();
        assertEquals(wm.printNodeSummary(wm.getID(), 0),
            InternalNodeContainerState.EXECUTED, wm.getInternalState());
        wm.save(m_workflowDirectory, new ExecutionMonitor(), true);
        WorkflowManager.ROOT.removeNode(wm.getID());
    }

    @Test
    public void testCancelWhileLoad() throws Exception {
        int previousChildCount = WorkflowManager.ROOT.getNodeContainers().size();
        LOAD_NODE_STATE_THREAD_LOCAL.set(LoadNodeState.Default);
        DefaultNodeProgressMonitor progMon = NODE_PROGRESS_THREAD_LOCAL.get();
        try {
            final WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(m_workflowDirectory);
            WorkflowManager.ROOT.load(m_workflowDirectory, new ExecutionMonitor(progMon), loadHelper, true);
            fail("Workflow should not load as it's canceled");
        } catch (CanceledExecutionException e) {
            assertEquals(LoadNodeState.MiddleNodeCanceled, LOAD_NODE_STATE_THREAD_LOCAL.get());
        } finally {
            LOAD_NODE_STATE_THREAD_LOCAL.set(null);
        }
        assertEquals(previousChildCount, WorkflowManager.ROOT.getNodeContainers().size());
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowDirectory);
        NODE_PROGRESS_THREAD_LOCAL.set(null);
    }

    public static final class CancelDuringLoadInternalsNodeFactory extends AdapterNodeFactory {

        @Override
        public AdapterNodeModel createNodeModel() {
            return new AdapterNodeModel(1, 1) {
                @Override
                protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
                            throws IOException, CanceledExecutionException {
                    DefaultNodeProgressMonitor progressMonitor = NODE_PROGRESS_THREAD_LOCAL.get();
                    if (progressMonitor != null) {
                        progressMonitor.setExecuteCanceled();
                    }
                    if (Objects.equals(LOAD_NODE_STATE_THREAD_LOCAL.get(), LoadNodeState.Default)) {
                        LOAD_NODE_STATE_THREAD_LOCAL.set(LoadNodeState.MiddleNodeCanceled);
                    }
                }
            };
        }
    }

    public static final class CheckLoadInternalsNotCalledNodeFactory extends AdapterNodeFactory {

        @Override
        public AdapterNodeModel createNodeModel() {
            return new AdapterNodeModel(1, 1) {
                @Override
                protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
                        throws IOException, CanceledExecutionException {
                    LOAD_NODE_STATE_THREAD_LOCAL.set(LoadNodeState.LastNodeLoaded);
                }
            };
        }
    }

}
