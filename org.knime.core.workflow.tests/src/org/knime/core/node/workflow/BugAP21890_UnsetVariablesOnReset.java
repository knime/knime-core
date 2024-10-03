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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** 
 * Tests unsetting the flow variable stack on nodes during reset.
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP21890_UnsetVariablesOnReset extends WorkflowTestCase {

    private NodeID m_concatenate_4;
    private NodeID m_comp_22;
    private NodeID m_concatenateComp_22_21;
    private NodeID m_columnFilter_3;
    private NodeID m_transpose_2;

    @Before
    public void setUp() throws Exception {
    	NodeID workflowID = loadAndSetWorkflow();
    	m_concatenate_4 = workflowID.createChild(4);
    	m_columnFilter_3 = workflowID.createChild(3);
    	m_transpose_2 = workflowID.createChild(2);
    	m_comp_22 = workflowID.createChild(22);
    	m_concatenateComp_22_21 = m_comp_22.createChild(0).createChild(21);
    }

    /** Basic test, then delete direct input connection. */
    @Test
    public void testUnchangedAndInputDeleted() throws Exception {
        assertThat("Unchanged input", extractVars(), hasItem("Var B"));
        executeAllAndWait(); // components only see vars after input is populated
        assertThat("Unchanged input", extractVarsComp(), hasItem("Var B"));
        
        deleteConnection(m_concatenate_4, 1);
        // contains neither Var A nor Var B
        assertThat("Vars after deleting connection", extractVars(), not(anyOf(hasItem("Var A"), hasItem("Var B"))));
        
        deleteConnection(m_comp_22, 1);
        // contains neither Var A nor Var B
        assertThat("Vars after deleting connection", extractVarsComp(), not(anyOf(hasItem("Var A"), hasItem("Var B"))));
        
    }

    /** Delete some connection further upstream, not directly connected to the concatenate node. */
    @Test
    public void testUpstreamConnectionDeleted() throws Exception {
    	executeAllAndWait();
    	deleteConnection(m_columnFilter_3, 1);
    	assertThat("Vars after connection delete", extractVars(), not(anyOf(hasItem("Var A"), hasItem("Var B"))));
		assertThat("Vars after connection delete (comp)", extractVarsComp(),
				not(anyOf(hasItem("Var A"), hasItem("Var B"))));
    }
    
    
    /** Reconnect to Transpose node (which only outputs spec after execution + has different variable. */
    @Test
	public void testReconnectToTranspose() throws Exception {
		deleteConnection(m_columnFilter_3, 1);
		getManager().addConnection(m_transpose_2, 1, m_columnFilter_3, 1);
		assertThat("Vars after connected to reset Transpose", extractVars(),
				not(anyOf(hasItem("Var A"), hasItem("Var B"))));
		assertThat("Vars after connected to reset Transpose", extractVarsComp(),
				not(anyOf(hasItem("Var A"), hasItem("Var B"))));
		// executing the transpose will make the variables available
		executeAllAndWait();
		assertThat("Vars after connected to reset Transpose", extractVars(), hasItems("Var A", "Var B"));
		assertThat("Vars after connected to reset Transpose (Comp)", extractVarsComp(), hasItems("Var A", "Var B"));

		deleteConnection(m_columnFilter_3, 0); // var connection to "Var B"
		executeAllAndWait();
		assertThat("Vars after connected to reset Transpose", extractVars(), hasItem("Var A"));
		assertThat("Vars after connected to reset Transpose", extractVars(), not(hasItem("Var B")));
		assertThat("Vars after connected to reset Transpose (Comp)", extractVarsComp(), hasItem("Var A"));
		assertThat("Vars after connected to reset Transpose (Comp)", extractVarsComp(), not(hasItem("Var B")));
	}

	private Set<String> extractVars() {
		NativeNodeContainer nnc = getManager().getNodeContainer(m_concatenate_4, NativeNodeContainer.class, true);
		return nnc.getFlowObjectStack().getAvailableFlowVariables(VariableType.getAllTypes()).keySet();
	}

	private Set<String> extractVarsComp() {
		NativeNodeContainer nnc = (NativeNodeContainer) findNodeContainer(m_concatenateComp_22_21);
		return nnc.getFlowObjectStack().getAvailableFlowVariables(VariableType.getAllTypes()).keySet();
	}

}
