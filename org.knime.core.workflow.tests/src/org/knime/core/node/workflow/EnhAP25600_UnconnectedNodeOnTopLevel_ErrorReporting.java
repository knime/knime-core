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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AP-25600: Error reporting on KNIME Hub is not sufficient in case the top level workflows contains unconnected nodes.
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.11
 */
final class EnhAP25600_UnconnectedNodeOnTopLevel_ErrorReporting extends WorkflowTestCase {
    
    private NodeID m_dataGenerator_2;
    private NodeID m_columnFilter_1;
    private NodeID m_component_4;
    private NodeID m_metanode_5;
    
    @BeforeEach
    void beforeEach() throws Exception {
        final NodeID wfmID = loadAndSetWorkflow();
        m_columnFilter_1 = wfmID.createChild(1);
        m_dataGenerator_2 = wfmID.createChild(2);
        m_component_4 = wfmID.createChild(4);
        m_metanode_5 = wfmID.createChild(5);
    }

    @Test
    void testStatesOnLoad() throws Exception {
        final WorkflowManager wfm = getManager();
        checkState(wfm, InternalNodeContainerState.IDLE);
        checkState(m_columnFilter_1, InternalNodeContainerState.IDLE);
        checkState(m_dataGenerator_2, InternalNodeContainerState.CONFIGURED);
        assertThat("workflow is executable", wfm.getParent().canExecuteNode(wfm.getID()), is(true));
        
        wfm.removeNode(m_dataGenerator_2);
        assertThat("workflow is not executable", wfm.getParent().canExecuteNode(wfm.getID()), is(false));
    }

    @Test
    void testExecuteAllUnmodified() throws Exception {
        final WorkflowManager wfm = getManager();
        executeAllAndWait();
        checkState(m_dataGenerator_2, InternalNodeContainerState.EXECUTED);
        checkState(m_columnFilter_1, InternalNodeContainerState.IDLE);
        //  contains one executed, one idle node --> so cannot be run = idle
        checkState(m_metanode_5, InternalNodeContainerState.IDLE); 
        NodeMessage wfmMessage = wfm.getNodeMessage();
        assertThat("Workflow Message contains \"unconnected\"", wfmMessage.getMessage(), containsString("unconnected"));
    }
    
    @Test
    void testExecuteAllOnlyComponent() throws Exception {
        final WorkflowManager wfm = getManager();
        wfm.removeNode(m_columnFilter_1);
        wfm.removeNode(m_metanode_5);
        executeAllAndWait();
        checkState(wfm, InternalNodeContainerState.IDLE);
        checkState(m_dataGenerator_2, InternalNodeContainerState.EXECUTED);
        NodeMessage wfmMessage = wfm.getNodeMessage();
        assertThat("Workflow message of wfm w/ component", wfmMessage.getMessage(), 
                containsString("Contains one node with execution failure (Component #4)"));
        Optional<String> issueMsg = wfmMessage.getIssue();
        assertThat("Workflow issue message contains \"unconnected\"", issueMsg.orElse(""),
                containsString("unconnected"));
    }
    
    @Test
    void testExecuteAllOnlyMetanode() throws Exception {
        final WorkflowManager wfm = getManager();
        wfm.removeNode(m_columnFilter_1);
        wfm.removeNode(m_component_4);
        executeAllAndWait();
        checkState(wfm, InternalNodeContainerState.IDLE);
        checkState(m_metanode_5, InternalNodeContainerState.IDLE);
        NodeMessage wfmMessage = wfm.getNodeMessage();
        assertThat("Workflow message of wfm w/ metanode", wfmMessage.getMessage(), 
                containsString("Contains one node with execution failure (Metanode #5)"));
        Optional<String> issueMsg = wfmMessage.getIssue();
        assertThat("Workflow issue message contains \"unconnected\"", issueMsg.orElse(""),
                containsString("unconnected"));
    }

    @Test
    void testExecuteAllOnlyTopLevelNode() throws Exception {
        final WorkflowManager wfm = getManager();
        wfm.removeNode(m_component_4);
        wfm.removeNode(m_metanode_5);
        executeAllAndWait();
        checkState(wfm, InternalNodeContainerState.IDLE);
        checkState(m_dataGenerator_2, InternalNodeContainerState.EXECUTED);
        NodeMessage wfmMessage = wfm.getNodeMessage();
        assertThat("Workflow Message contains \"unconnected\"", wfmMessage.getMessage(), 
                containsString("Contains an unconnected node (\"Column Filter #1\")"));
    }

}
