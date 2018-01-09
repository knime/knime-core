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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.action.CollapseIntoMetaNodeResult;
import org.knime.core.util.FileUtil;

/**
 * Tests Credentials Input node with credentials saved in the workflow.
 * @author wiswedel, University of Konstanz
 */
public class Bug3673_CredentialsInputNode_Test1_SimpleNodeWithSavedPassword extends WorkflowTestCase {

    private NodeID m_credentialsInput_1;
    private NodeID m_credentialsValidate_2;
    private NodeID m_activeBranchInverter_3;
    private File m_workflowDirTemp;

    @Before
    public void setUp() throws Exception {
        File workflowDirSVN = getDefaultWorkflowDirectory();
        // will save the workflow in one of the test ...don't write SVN folder
        m_workflowDirTemp = FileUtil.createTempDir(workflowDirSVN.getName());
        FileUtil.copyDir(workflowDirSVN, m_workflowDirTemp);
        initFlow();
    }

    private void initFlow() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_workflowDirTemp);
        m_credentialsInput_1 = baseID.createChild(1);
        m_credentialsValidate_2 = baseID.createChild(2);
        m_activeBranchInverter_3 = baseID.createChild(3);
    }

    @Test
    public void testExecuteFlow() throws Exception {
        checkState(m_credentialsValidate_2, CONFIGURED);
        assertFalse("Not expected to be dirty", getManager().isDirty());
        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.EXECUTED);
        assertFalse(((NativeNodeContainer)findNodeContainer(m_credentialsInput_1)).isInactive());
    }

    @Test
    public void testCopyPasteExecuteFlow() throws Exception {
        WorkflowCopyContent.Builder cnt = WorkflowCopyContent.builder();
        cnt.setNodeIDs(m_credentialsInput_1, m_credentialsValidate_2);
        WorkflowCopyContent pasteCNT = getManager().copyFromAndPasteHere(getManager(), cnt.build());

        executeAndWait(pasteCNT.getNodeIDs());
        checkStateOfMany(EXECUTED, pasteCNT.getNodeIDs());
    }

    @Test
    public void testCollapseToSubnodeThenSaveLoad() throws Exception {
//        WorkflowCopyContent cnt = new WorkflowCopyContent();
//        cnt.setNodeIDs(m_credentialsInput_1);
//        getManager().copyFromAndPasteHere(getManager(), cnt);
        CollapseIntoMetaNodeResult collapseResult = getManager().collapseIntoMetaNode(
            new NodeID[] {m_credentialsInput_1}, new WorkflowAnnotation[0], "Collapsed-by-Testflow");
        WorkflowManager metaNode = getManager().getNodeContainer(
            collapseResult.getCollapsedMetanodeID(), WorkflowManager.class, true);
        getManager().convertMetaNodeToSubNode(metaNode.getID());
        assertFalse("Expected to be removed", getManager().containsNodeContainer(m_credentialsInput_1));

        SubNodeContainer subNode = getManager().getNodeContainer(metaNode.getID(), SubNodeContainer.class, true);
        subNode.updateOutputConfigurationToIncludeAllFlowVariables();
        NodeID subnodeID = subNode.getID();
        final int subNodeIDIndex = subnodeID.getIndex();

        ConnectionContainer findInConnection = findInConnection(m_credentialsValidate_2, 1);
        assertEquals("Source should be subnode", subnodeID, findInConnection.getSource());

        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);
        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, EXECUTED);

        closeWorkflow();
        initFlow();
        subnodeID = getManager().getID().createChild(subNodeIDIndex);
        checkState(subnodeID, CONFIGURED);
        executeAndWait(subnodeID);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);

        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, EXECUTED);

        closeWorkflow();
        initFlow();
        subnodeID = getManager().getID().createChild(subNodeIDIndex);
        checkState(subnodeID, EXECUTED);
        checkState(m_credentialsValidate_2, CONFIGURED);

        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, EXECUTED);
    }

    @Test
    public void testPartialExecuteSaveLoadExecute() throws Exception {
        executeAndWait(m_credentialsInput_1);
        checkState(m_credentialsInput_1, InternalNodeContainerState.EXECUTED);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.CONFIGURED);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);

        closeWorkflow();
        initFlow();
        checkState(m_credentialsInput_1, InternalNodeContainerState.EXECUTED);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.CONFIGURED);
        assertFalse("Not expected to be dirty", getManager().isDirty());
        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testLoadWhileInactive() throws Exception {
        getManager().addConnection(m_activeBranchInverter_3, 1, m_credentialsInput_1, 0);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);
        executeAllAndWait();

        checkState(getManager(), EXECUTED);
        assertTrue(((NativeNodeContainer)findNodeContainer(m_credentialsInput_1)).isInactive());

        closeWorkflow(); // don't save
        initFlow();
        checkState(m_credentialsInput_1, CONFIGURED);
        assertFalse("Not expected to be dirty", getManager().isDirty());
        executeAllAndWait();
        assertTrue(((NativeNodeContainer)findNodeContainer(m_credentialsInput_1)).isInactive());
        checkState(m_credentialsValidate_2, EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowDirTemp);
    }

}
