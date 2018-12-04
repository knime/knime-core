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
 * ---------------------------------------------------------------------
 *
 *
 * History
 *   19.06.2012 (wiswedel): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.action.ExpandSubnodeResult;

/**
 * Simple expands, collapses a subnode workflow.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6029_ExpandSubnode extends WorkflowTestCase {

    private NodeID m_tableDiffer7;
    private NodeID m_subnode8;
    private NodeID m_javaSnippet_After_Expand_3;
    private NodeID m_stringInput5;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableDiffer7 = new NodeID(baseID, 7);
        m_subnode8 = new NodeID(baseID, 8);
        m_javaSnippet_After_Expand_3 = new NodeID(baseID, 3);
        m_stringInput5 = new NodeID(baseID, 5);
    }

    @Test
    public void testSimpleLoadAndExecute() throws Exception {
        checkState(m_tableDiffer7, InternalNodeContainerState.IDLE);
        checkState(m_subnode8, InternalNodeContainerState.IDLE);

        WorkflowManager mgr = getManager();
        assertThat("Not expected to be dirty after load", mgr.isDirty(), is(false));

        executeAllAndWait();
        checkState(mgr, InternalNodeContainerState.EXECUTED);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableDiffer7, m_subnode8);
    }

    @Test
    public void testExecuteAfterExpandAndCollapse() throws Exception {
        WorkflowManager mgr = getManager();
        ExpandSubnodeResult expandSubWorkflowResult = mgr.expandSubWorkflow(m_subnode8);
        assertThat("Expected to be dirty after expand", mgr.isDirty(), is(true));
        assertThat("Subnode must not longer exist", mgr.getNodeContainer(m_subnode8, SubNodeContainer.class, false),
            is(nullValue()));
        ConnectionContainer flowVarConn = findInConnection(m_javaSnippet_After_Expand_3, 0);
        assertNotNull("didn't find connection after expand", flowVarConn);
        assertThat("Source should be string input node", flowVarConn.getSource(), is(m_stringInput5));
        executeAllAndWait();

        checkState(mgr, InternalNodeContainerState.EXECUTED);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableDiffer7, m_javaSnippet_After_Expand_3);
        assertThat("Should be expandable but is not", expandSubWorkflowResult.canUndo(), is(Boolean.TRUE));

        expandSubWorkflowResult.undo();
        checkState(mgr, InternalNodeContainerState.CONFIGURED);
        assertThat("Subnode must have been re-created", mgr.getNodeContainer(m_subnode8, SubNodeContainer.class, false),
            is(not(nullValue())));

        assertThat("Java Snippet must have been removed/collapsed", mgr.getNodeContainer(m_javaSnippet_After_Expand_3,
            NativeNodeContainer.class, false), is(nullValue()));

        executeAllAndWait();
        checkState(mgr, InternalNodeContainerState.EXECUTED);

        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableDiffer7, m_subnode8);

    }

    /** Tests AP-10946 - expansion of metanodes with workflow annotations is broken. */
    @Test
    public void testExpandAndCheckWorkflowAnnotationBugAP10946() throws Exception {
        WorkflowManager mgr = getManager();
        ExpandSubnodeResult expandSubWorkflowResult = mgr.expandSubWorkflow(m_subnode8);
        assertThat("Number of workflow annotations in copy content",
            expandSubWorkflowResult.getExpandedCopyContent().getAnnotationIDs().length, is(1));
        Collection<WorkflowAnnotation> workflowAnnotations = mgr.getWorkflowAnnotations();
        assertThat("Number of workflow annotations in workflow", workflowAnnotations.size(), is(1));
        WorkflowAnnotation annotation = workflowAnnotations.stream().findFirst().get();
        assertThat("Text in workflow annotation", annotation.getText(), is("Some Workflow Annotation"));
    }


}
