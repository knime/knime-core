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
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.action.CollapseIntoMetaNodeResult;
import org.knime.core.util.FileUtil;

/**
 * Tests Credentials Input node with credentials NOT saved in the workflow.
 * @author wiswedel, University of Konstanz
 */
public class Bug3673_CredentialsInputNode_Test2_SimpleNodeWithNoSavedPassword extends WorkflowTestCase {

    private NodeID m_credentialsInput_1;
    private NodeID m_credentialsValidate_2;
    private NodeID m_credentialsValidate_4;
    private NodeID m_activeBranchInverter_3;
    private File m_workflowDirTemp;

    @Before
    public void setUp() throws Exception {
        File workflowDirSVN = getDefaultWorkflowDirectory();
        // will save the workflow in one of the test ...don't write SVN folder
        m_workflowDirTemp = FileUtil.createTempDir(workflowDirSVN.getName());
        FileUtil.copyDir(workflowDirSVN, m_workflowDirTemp);
    }

    private TestWorkflowLoadHelper initFlow(final TestWorkflowLoadHelper loadHelper) throws Exception {
        WorkflowManager manager = loadWorkflow(
            m_workflowDirTemp, new ExecutionMonitor(), loadHelper).getWorkflowManager();
        setManager(manager);
        NodeID baseID = manager.getID();
        m_credentialsInput_1 = baseID.createChild(1);
        m_credentialsValidate_2 = baseID.createChild(2);
        m_activeBranchInverter_3 = baseID.createChild(3);
        m_credentialsValidate_4 = baseID.createChild(4);
        return loadHelper;
    }

    @Test
    public void testExecuteFlow() throws Exception {
        TestWorkflowLoadHelper loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        assertTrue("No password prompted", loadHelper.hasBeenPrompted());
        checkState(m_credentialsValidate_2, CONFIGURED);
        assertFalse("Not expected to be dirty", getManager().isDirty());

        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.EXECUTED);
        assertFalse(((NativeNodeContainer)findNodeContainer(m_credentialsInput_1)).isInactive());
    }

    @Test
    public void testExecuteWrongPassword() throws Exception {
        TestWorkflowLoadHelper loadHelper = initFlow(new TestWorkflowLoadHelper("some-wrong-password"));
        checkState(m_credentialsValidate_2, IDLE);
        assertTrue("Expected to be dirty", getManager().isDirty()); // dirty because of CONFIGURED -> IDLE

        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.IDLE);
        assertFalse(((NativeNodeContainer)findNodeContainer(m_credentialsInput_1)).isInactive());
        assertTrue("No password prompted", loadHelper.hasBeenPrompted());
    }

    @Test
    public void testCopyPasteExecuteFlow() throws Exception {
        initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        WorkflowCopyContent.Builder cnt = WorkflowCopyContent.builder();
        cnt.setNodeIDs(m_credentialsInput_1, m_credentialsValidate_2);
        WorkflowCopyContent pasteCNT = getManager().copyFromAndPasteHere(getManager(), cnt.build());

        executeAndWait(pasteCNT.getNodeIDs());
        checkStateOfMany(EXECUTED, pasteCNT.getNodeIDs());
    }

    @Test
    public void testPartialExecuteSaveLoadExecute() throws Exception {
        TestWorkflowLoadHelper loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        executeAndWait(m_credentialsInput_1);
        checkState(m_credentialsInput_1, InternalNodeContainerState.EXECUTED);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.CONFIGURED);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);
        assertTrue("No password prompted", loadHelper.hasBeenPrompted());

        closeWorkflow();

        loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        // input node is executed - so no prompt expected
        assertFalse("Password prompted but shouldn't", loadHelper.hasBeenPrompted());

        checkState(m_credentialsInput_1, InternalNodeContainerState.EXECUTED);
        checkState(m_credentialsValidate_2, InternalNodeContainerState.IDLE);
        assertTrue("Expected to be dirty", getManager().isDirty()); // because 2nd validator is now idle

        executeAndWait(m_credentialsValidate_2, m_credentialsValidate_4);
        // expected to fail - null password
        checkState(m_credentialsValidate_2, InternalNodeContainerState.IDLE);
        checkState(m_credentialsValidate_4, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testLoadWhileInactive() throws Exception {
        TestWorkflowLoadHelper loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        assertTrue("No password prompted", loadHelper.hasBeenPrompted());
        getManager().addConnection(m_activeBranchInverter_3, 1, m_credentialsInput_1, 0);
        executeAndWait(m_activeBranchInverter_3); // must be executed to make downstream flow inactive
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);

        closeWorkflow(); // don't save
        loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        assertFalse("Not expected to be dirty", getManager().isDirty());
        assertFalse("password prompted although inactive", loadHelper.hasBeenPrompted());

        checkState(m_credentialsInput_1, CONFIGURED);
        executeAllAndWait();
        checkState(getManager(), EXECUTED);
    }

    @Test
    public void testCollapseToSubnodeThenSaveLoad() throws Exception {
        TestWorkflowLoadHelper loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        assertTrue("No password prompted", loadHelper.hasBeenPrompted());

        /* Collapse into subnode - make sure it's there */
        CollapseIntoMetaNodeResult collapseResult = getManager().collapseIntoMetaNode(new NodeID[] {m_credentialsInput_1},
            new WorkflowAnnotation[0], "Collapsed-by-Testflow");
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

        /* Load: subnode contained but not executed - prompt expected, enter wrong password */
        loadHelper = initFlow(new TestWorkflowLoadHelper("some-wrong-password"));
        subnodeID = getManager().getID().createChild(subNodeIDIndex);
        checkState(subnodeID, CONFIGURED);
        assertTrue("No password prompted", loadHelper.hasBeenPrompted());

        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);

        executeAndWait(m_credentialsValidate_2);
        checkState(subnodeID, EXECUTED); // wrong password
        checkState(m_credentialsValidate_2, IDLE); // wrong password
        closeWorkflow();

        /* Load: subnode contained but not executed - prompt expected, enter correct password */
        loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        assertFalse("Expected to be removed", getManager().containsNodeContainer(m_credentialsInput_1));
        subnodeID = getManager().getID().createChild(subNodeIDIndex);
        checkState(subnodeID, CONFIGURED);
        assertTrue("password prompt not expected", loadHelper.hasBeenPrompted());

        executeAndWait(subnodeID);
        checkState(subnodeID, EXECUTED);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);

        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, EXECUTED);
        closeWorkflow();

        /* Load: subnode contained and executed - prompt not expected, downstream nodes need to fail. */
        loadHelper = initFlow(new TestWorkflowLoadHelper("some-fixed-password"));
        subnodeID = getManager().getID().createChild(subNodeIDIndex);
        checkState(subnodeID, EXECUTED);
        assertFalse("password prompt not expected", loadHelper.hasBeenPrompted());

        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, IDLE);

        executeAndWait(m_credentialsValidate_4);
        checkState(m_credentialsValidate_4, EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowDirTemp);
    }

    private class TestWorkflowLoadHelper extends ConfigurableWorkflowLoadHelper {

        private final String m_password;
        private boolean m_hasGottenPrompted;

        /** @param workflowLocation */
        TestWorkflowLoadHelper(final String password) {
            super(m_workflowDirTemp);
            m_password = password;
        }

        /** {@inheritDoc} */
        @Override
        public List<Credentials> loadCredentials(final List<Credentials> credentials) {
            CheckUtils.checkState(credentials.size() == 1, "Expected 1 credentials set, got %d", credentials.size());
            Credentials c = credentials.get(0);
            CheckUtils.checkState(c.getName().equals("credentials-input"), "Wrong identifier: %s", c.getName());
            CheckUtils.checkState(c.getLogin().equals("some-fixed-username"), "Wrong identifier: %s", c.getLogin());
            CheckUtils.checkState(c.getPassword() == null, "Expected null password");

            m_hasGottenPrompted = true;
            return Collections.singletonList(new Credentials(c.getName(), c.getLogin(), m_password));
        }

        /**
         * @return the hasBeenPrompted
         */
        public boolean hasBeenPrompted() {
            return m_hasGottenPrompted;
        }

    }

}
