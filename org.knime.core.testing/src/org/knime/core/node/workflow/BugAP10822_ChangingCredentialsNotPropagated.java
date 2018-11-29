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

import static org.junit.Assert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/** The test flow contains three pairs of string manipulation + credentials validate. There are also two workflow
 * credentials defined, which will be changed during test execution.
 *
 * @author wiswedel, KNIME AG Zurich
 */
public class BugAP10822_ChangingCredentialsNotPropagated extends WorkflowTestCase {

    private NodeID m_stringVar_1;
    private NodeID m_credentialsValidate_2;
    private NodeID m_stringVar_3;
    private NodeID m_credentialsValidate_4;
    private NodeID m_stringVar_5;
    private NodeID m_credentialsValidate_6;

    @Before
    public void setUp() throws Exception {
        NodeID id = loadAndSetWorkflow();
        m_stringVar_1 = id.createChild(1);
        m_credentialsValidate_2 = id.createChild(2);
        m_stringVar_3 = id.createChild(3);
        m_credentialsValidate_4 = id.createChild(4);
        m_stringVar_5 = id.createChild(5);
        m_credentialsValidate_6 = id.createChild(6);
    }

    @Test
    public void testStateAfterLoad() throws Exception {
        checkStateOfMany(IDLE, m_credentialsValidate_2, m_credentialsValidate_4, m_credentialsValidate_6);
        // saved CONFIGURED, loaded IDLE
        assertThat("Workflow Dirty Flag after load", getManager().isDirty(), Matchers.is(true));
        Collection<String> credentialsList = getManager().getCredentialsStore().listNames();
        assertThat("List of workflow credentials after load", credentialsList,
            Matchers.containsInAnyOrder("credentials", "credentials_removed"));
        // the other two credentials will be added later
    }

    @Test
    public void testStateAfterSettingPassword_Node2() throws Exception {
        WorkflowManager manager = getManager();
        manager.updateCredentials(new Credentials("credentials", "user", "password"));
        checkState(m_credentialsValidate_2, CONFIGURED);
        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, EXECUTED);
        manager.updateCredentials(new Credentials("credentials", "user", "wrong-password"));
        checkState(m_credentialsValidate_2, EXECUTED); // does not reset
    }

    @Test
    public void testStateAfterPartialExecuteAndSettingPassword_Node2() throws Exception {
        WorkflowManager manager = getManager();
        executeAndWait(m_stringVar_1);
        checkState(m_stringVar_1, EXECUTED);
        checkState(m_credentialsValidate_2, IDLE);
        manager.updateCredentials(new Credentials("credentials", "user", "password"));
        checkState(m_credentialsValidate_2, CONFIGURED);
        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, EXECUTED);
    }

    @Test
    public void testStateAfterAddingCredentials_Node4() throws Exception {
        WorkflowManager manager = getManager();
        manager.updateCredentials(new Credentials("credentials", "user", "password"));
        checkState(m_credentialsValidate_2, CONFIGURED);
        executeAndWait(m_credentialsValidate_2);
        checkState(m_credentialsValidate_2, EXECUTED);
        manager.updateCredentials(new Credentials("credentials", "user", "wrong-password"));
        checkState(m_credentialsValidate_2, EXECUTED); // does not reset
    }

    @Test
    public void testStateAfterPartialExecuteAndAddingCredentials_Node4() throws Exception {
        WorkflowManager manager = getManager();
        executeAndWait(m_stringVar_3);
        checkState(m_stringVar_3, EXECUTED);
        checkState(m_credentialsValidate_4, IDLE);
        manager.updateCredentials(new Credentials("credentials", "user", "password"),
            new Credentials("credentials_addedlater", "user", "password"));
        checkState(m_credentialsValidate_4, CONFIGURED);
        executeAndWait(m_credentialsValidate_4);
        checkState(m_credentialsValidate_4, EXECUTED);
    }

    @Test
    public void testStateAfterRemovingCredentials_Node6() throws Exception {
        WorkflowManager manager = getManager();
        manager.updateCredentials(new Credentials("credentials_removed", "user", "password"));
        executeAndWait(m_credentialsValidate_6);
        checkState(m_credentialsValidate_6, EXECUTED);
        manager.updateCredentials(); // removes all credentials
        checkState(m_credentialsValidate_6, EXECUTED); // does not reset
        manager.resetAndConfigureNode(m_credentialsValidate_6);
        checkState(m_credentialsValidate_6, IDLE);
    }

    @Test
    public void testStateAfterPartialExecuteAndRemovingCredentials_Node6() throws Exception {
        WorkflowManager manager = getManager();
        executeAndWait(m_stringVar_5);
        checkState(m_stringVar_5, EXECUTED);
        manager.updateCredentials(new Credentials("credentials_removed", "user", "password"));
        executeAndWait(m_credentialsValidate_6);
        checkState(m_credentialsValidate_6, EXECUTED);
        manager.updateCredentials(new Credentials("credentials", "user", "password")); // the other is removed
        checkState(m_credentialsValidate_6, EXECUTED);
        manager.resetAndConfigureNode(m_credentialsValidate_6);
        checkState(m_credentialsValidate_6, IDLE);
    }

}
