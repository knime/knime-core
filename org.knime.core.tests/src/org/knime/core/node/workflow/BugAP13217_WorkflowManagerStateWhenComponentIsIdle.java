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
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * AP-13217: Workflow containing idle Component claims to be not executable although it is
 * https://knime-com.atlassian.net/browse/AP-13217
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP13217_WorkflowManagerStateWhenComponentIsIdle extends WorkflowTestCase {

    private NodeID m_component5;
    private NodeID m_dataGenerator_5_1;
    private NodeID m_componentInput_5_4;

    @Before
    public void setUp() throws Exception {
        NodeID id = loadAndSetWorkflow();
        m_component5 = id.createChild(5);
        m_dataGenerator_5_1 = m_component5.createChild(0).createChild(1);
        m_componentInput_5_4 = m_component5.createChild(0).createChild(4);
    }

    /** Check executable state directly after load */
    @Test
    public void testStateOfWorkflowAfterLoad() throws Exception {
        final WorkflowManager manager = getManager();
        checkState(manager, IDLE);
        checkState(m_component5, IDLE);
        checkState(m_dataGenerator_5_1, CONFIGURED);
        Assert.assertThat("'canExecuteNode(Project-wfm)' after load",
            manager.getParent().canExecuteNode(manager.getID()), is(true));
        executeAllAndWait();
        checkState(manager, EXECUTED);
        checkState(m_component5, EXECUTED);
    }

    /** Check executable state after removing the executable node */
    @Test
    public void testStateAfterRemovingCONFIGUREDNodes() throws Exception {
        final WorkflowManager manager = getManager();
        SubNodeContainer component = manager.getNodeContainer(m_component5, SubNodeContainer.class, true);
        component.getWorkflowManager().removeNode(m_dataGenerator_5_1);
        // this is fishy -- the source (data gen) is removed but there's still a yellow node: component input
        // after discussion with TM we decided to consider that a corner case and will run this node first before
        // any additional checks are done
        executeAndWait(m_componentInput_5_4);
        checkState(manager, IDLE);
        checkState(m_component5, IDLE);
        Assert.assertThat("'canExecuteNode(Project-wfm)' after removing source node",
            manager.getParent().canExecuteNode(manager.getID()), is(false));
        executeAllAndWait();
        checkState(manager, IDLE);
        checkState(m_component5, IDLE);
    }


}
