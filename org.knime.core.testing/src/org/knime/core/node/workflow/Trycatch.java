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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author M. Berthold, University of Konstanz
 */
public class Trycatch extends WorkflowTestCase {

    private NodeID m_try;
    private NodeID m_catch;
    private NodeID m_if;
    private NodeID m_endif;
    private NodeID m_endloop;
    private NodeID m_finalnode;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_try = new NodeID(baseID, 10);
        m_catch = new NodeID(baseID, 8);
        m_if = new NodeID(baseID, 15);
        m_endif = new NodeID(baseID, 16);
        m_endloop = new NodeID(baseID, 13);
        m_finalnode = new NodeID(baseID, 7);
    }

    @Test(expected=NoSuchElementException.class)
    public void testExecuted() throws Exception {
    	executeAllAndWait();
    	// check node states - inactive interna/active end.
        checkState(m_try, InternalNodeContainerState.EXECUTED);
        checkState(m_catch, InternalNodeContainerState.EXECUTED);
        checkState(m_if, InternalNodeContainerState.EXECUTED);
        assertTrue(((SingleNodeContainer)(getManager().getNodeContainer(m_if))).isInactive());
        checkState(m_endif, InternalNodeContainerState.EXECUTED);
        assertTrue(((SingleNodeContainer)(getManager().getNodeContainer(m_endif))).isInactive());
        checkState(m_endloop, InternalNodeContainerState.EXECUTED);
        assertTrue(((SingleNodeContainer)(getManager().getNodeContainer(m_endloop))).isInactive());
        checkState(m_catch, InternalNodeContainerState.EXECUTED);
        assertFalse(((SingleNodeContainer)(getManager().getNodeContainer(m_catch))).isInactive());
        // check variable in scope and hiding outside
        NativeNodeContainer endifNNC = (NativeNodeContainer)(getManager().getNodeContainer(m_endif));
        try {
        	endifNNC.getNode().getNodeModel().peekFlowVariableString("innerScopeVariable");
        } catch (NoSuchElementException nsee) {
        	Assert.fail("Variable does not exist but should!");
        }
        SingleNodeContainer finalnodeSNC = (SingleNodeContainer)(getManager().getNodeContainer(m_finalnode));
        // expected to fail
        finalnodeSNC.getOutgoingFlowObjectStack().peekFlowVariable("innerScopeVariable", FlowVariable.Type.STRING);
        Assert.fail("Variable exists but shouldn't!");
    }

}
