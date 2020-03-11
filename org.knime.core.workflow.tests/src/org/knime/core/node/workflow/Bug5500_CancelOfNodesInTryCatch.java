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

import org.junit.Before;
import org.junit.Test;

/** Bug 5500: Cancelled R snippet in Try-catch produces an invalid state
 * http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5500
 *
 * The cause of the problem that some nodes (R and java snippet) were not detected as canceled when they were canceled.
 * See bugzilla comment and check-in diff for details.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug5500_CancelOfNodesInTryCatch extends WorkflowTestCase {

    private NodeID m_tryStart_8;
    private NodeID m_tryEnd_7;
    private NodeID m_javaSnippet_10;
    private NodeID m_failBranch_BigDataGenerator_12;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tryStart_8 = new NodeID(baseID, 8);
        m_tryEnd_7 = new NodeID(baseID, 7);
        m_javaSnippet_10 = new NodeID(baseID, 10);
        m_failBranch_BigDataGenerator_12 = new NodeID(baseID, 12);
    }

    @Test
    public void testExecuteThenCancel() throws Exception {
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_tryEnd_7, m_tryStart_8, m_javaSnippet_10,
            m_failBranch_BigDataGenerator_12);
        getManager().executeUpToHere(m_tryEnd_7);
        final NodeContainer javaSnippetNC = getManager().getNodeContainer(m_javaSnippet_10);
        waitWhile(javaSnippetNC, new Hold() {
            @Override
            protected boolean shouldHold() {
                // skip pre-execute etc.
                return javaSnippetNC.getInternalState().isExecutionInProgress()
                        && !javaSnippetNC.getInternalState().equals(InternalNodeContainerState.EXECUTING);
            }
        });
        getManager().cancelExecution(javaSnippetNC);
        waitWhile(javaSnippetNC, new Hold() { // wait for cancel to propagate
            @Override
            protected boolean shouldHold() {
                return javaSnippetNC.getNodeContainerState().isExecutionInProgress();
            }
        });
        checkState(m_tryEnd_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_failBranch_BigDataGenerator_12, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaSnippet_10, InternalNodeContainerState.CONFIGURED);
        NodeMessage nodeMessage = javaSnippetNC.getNodeMessage();
        assertEquals(NodeMessage.Type.WARNING, nodeMessage.getMessageType());
        assertEquals("Execution canceled", nodeMessage.getMessage());
    }

}
