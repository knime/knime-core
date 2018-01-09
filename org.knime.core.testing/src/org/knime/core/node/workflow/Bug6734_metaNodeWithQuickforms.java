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
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import org.junit.Before;
import org.junit.Test;

/** Bug 6734: New Quickforms shouldn't be visible in meta node dialog
 * https://bugs.knime.org/show_bug.cgi?id=6734
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6734_metaNodeWithQuickforms extends WorkflowTestCase {

    private NodeID m_metaNodeWithQF_5;
    private NodeID m_metaNodeWithNewQF_6;
    private NodeID m_subNodeWithNewQF_8;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_metaNodeWithNewQF_6 = new NodeID(baseID, 6);
        m_metaNodeWithQF_5 = new NodeID(baseID, 5);
        m_subNodeWithNewQF_8 = new NodeID(baseID, 8);
    }

    /** Add connection, all nodes reset. */
    @Test
    public void testExecuteAllAndCheckDialogs() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, IDLE);
        executeAllAndWait();
        checkState(manager, EXECUTED);
        WorkflowManager mgr = getManager();
        WorkflowManager metanode5 = mgr.getNodeContainer(m_metaNodeWithQF_5, WorkflowManager.class, true);
        MetaNodeDialogPane dialog5 = (MetaNodeDialogPane)metanode5.getDialogPaneWithSettings();
        assertEquals("Unexpected dialog node count", 1, dialog5.getNodes().size());

        WorkflowManager metanode6 = mgr.getNodeContainer(m_metaNodeWithNewQF_6, WorkflowManager.class, true);
        MetaNodeDialogPane dialog6 = (MetaNodeDialogPane)metanode6.getDialogPaneWithSettings();
        assertEquals("Unexpected dialog node count", 0, dialog6.getNodes().size());

        SubNodeContainer subnode8 = mgr.getNodeContainer(m_subNodeWithNewQF_8, SubNodeContainer.class, true);
        MetaNodeDialogPane dialog8 = (MetaNodeDialogPane)subnode8.getDialogPaneWithSettings();
        assertEquals("Unexpected dialog node count", 1, dialog8.getNodes().size());
    }

}
