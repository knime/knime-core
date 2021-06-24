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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;

/**
 * Tests AP-11261: (API) Add hook to workflow loading routine to populate "knime.system.credentials"
 * https://knime-com.atlassian.net/browse/AP-11261
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP11261_CredentialsDefaultHook extends WorkflowTestCase {

    private NodeID m_credentialsValidate_1;
    private NodeID m_credentialsValidate_3;
    private NodeID m_credentialsValidate_6_4;
    private NodeID m_credentialsValidate_8_4;

    private void init() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_credentialsValidate_1 = baseID.createChild(1);
        m_credentialsValidate_3 = baseID.createChild(3);
        m_credentialsValidate_6_4 = baseID.createChild(6).createChild(0).createChild(4); // component
        m_credentialsValidate_8_4 = baseID.createChild(8).createChild(0).createChild(4); // component
    }


    /** Don't set master password, then execute workflow -- expect many failures. */
    @Test
    public void testExecuteFlow_Failure() throws Exception { // expected to fail
        init(); // don't populate system variables
        checkState(getManager(), InternalNodeContainerState.IDLE);
        checkStateOfMany(IDLE, m_credentialsValidate_1, m_credentialsValidate_6_4, m_credentialsValidate_8_4);
        checkState(m_credentialsValidate_3, EXECUTED);
        NodeMessage msgOfNode3 = findNodeContainer(m_credentialsValidate_3).getNodeMessage();
        // should complain about wrong password
        assertThat("Node message type of executed credentials validate node after load",
            msgOfNode3.getMessageType(),
            is(NodeMessage.Type.WARNING));
        assertThat("Node message text of executed credentials validate node after load",
            msgOfNode3.getMessage(), CoreMatchers.containsString("Wrong password"));
        assertThat("Workflow Manager dirty state", getManager().isDirty(), is(true));
        executeAllAndWait();

        for (NodeID id : Arrays.asList(m_credentialsValidate_1, m_credentialsValidate_6_4, m_credentialsValidate_8_4)) {
            NodeMessage msgOfNode = findNodeContainer(id).getNodeMessage();

            assertThat(String.format("Node message type after configure attempt (ID %s)", id),
                msgOfNode.getMessageType(),
                is(NodeMessage.Type.WARNING));
            assertThat(String.format("Node message after configure attempt (ID %s)", id),
                msgOfNode.getMessage(), CoreMatchers.containsString("Wrong password"));

        }
        checkState(getManager(), IDLE);
    }

    /** Set global master password prior loading, expect all nodes to execute. */
    @Test
    public void testExecuteFlow_Success() throws Exception {
        CredentialsStore.setKNIMESystemDefault("some", "default");
        init(); // don't populate system variables
        checkState(getManager(), IDLE);
        checkState(m_credentialsValidate_1, CONFIGURED);
        checkState(m_credentialsValidate_8_4, CONFIGURED);
        checkState(m_credentialsValidate_3, EXECUTED);
        checkState(m_credentialsValidate_6_4, IDLE);
        assertThat("Workflow Manager dirty state", getManager().isDirty(), is(false));
        executeAllAndWait();
        assertThat("Workflow Manager dirty state", getManager().isDirty(), is(true));
        checkStateOfMany(EXECUTED, m_credentialsValidate_1, m_credentialsValidate_3, m_credentialsValidate_6_4,
            m_credentialsValidate_8_4);
        checkState(getManager(), EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        CredentialsStore.setKNIMESystemDefault(null, null);
        super.tearDown();
    }

}
