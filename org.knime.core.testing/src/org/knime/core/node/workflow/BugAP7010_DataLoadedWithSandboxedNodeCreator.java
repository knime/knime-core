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

import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.exec.SandboxedNodeCreator;
import org.knime.core.node.exec.SandboxedNodeCreator.SandboxedNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Create a Sandboxed Node of a node and execute the workflow afterwards.
 * Tests if there are dead data references after the SandboxedNode creation. See AP-7010.
 * @author Benjamin Wilhelm, University of Konstanz
 */
public class BugAP7010_DataLoadedWithSandboxedNodeCreator extends WorkflowTestCase {

    private File m_workflowDir;
    private NodeID m_rowSplitter;
    private NodeID m_tableCreator;
    private WorkflowManager m_sandboxedWM;

    @Before
    public void setUp() throws Exception {
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        m_sandboxedWM = WorkflowManager.ROOT.createAndAddProject(
            "Sandboxed Temp Workflow", new WorkflowCreationHelper());
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_tableCreator = new NodeID(baseID, 1);
        m_rowSplitter = new NodeID(baseID, 2);
        return loadResult;
    }

    /**
     * Creates an SandboxedNode of a node of the workflow and
     * executes the workflow afterwards.
     * @throws Exception
     */
    @Test(timeout = 30000L)
    public void testExecuteAfterSandboxedNodeCreation() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, CONFIGURED);

        // Get some things we need to create the SandboxedNode
        NodeContainer tableCreator = findNodeContainer(m_tableCreator);
        NodeContainer rowSplitter = findNodeContainer(m_rowSplitter);
        PortObject[] inputData = new PortObject[2];
        inputData[1] = tableCreator.getOutPort(1).getPortObject();
        ExecutionMonitor exec = new ExecutionMonitor();

        // Set the location and the WFM
        File sandboxedDir = FileUtil.createTempDir(getClass().getSimpleName() + "-sandboxed");


        // Create the SandboxedNode
        SandboxedNodeCreator nodeCreator = new SandboxedNodeCreator(rowSplitter, inputData, m_sandboxedWM);
        nodeCreator.setLocalWorkflowDir(CheckUtils.checkArgumentNotNull(sandboxedDir));
        nodeCreator.setCopyData(true);
        NodeContext.pushContext(rowSplitter);
        NodeID workflowID;
        try (SandboxedNode node = nodeCreator.createSandbox(exec)) {
            workflowID = node.getSandboxNode(NodeContainer.class).getParent().getID();
        } finally {
            NodeContext.removeLastContext();
        }

        // Now, try to execute the original workflow. This should still work
        executeAllAndWait();
        checkState(manager, EXECUTED);
    }


    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        WorkflowManager.ROOT.removeProject(m_sandboxedWM.getID());
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowDir);
    }

}
