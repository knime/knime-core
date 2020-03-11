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

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4409_inactiveInnerLoop extends WorkflowTestCase {

    private NodeID m_outerLoopStart_137;
    private NodeID m_outerLoopEnd_138;
    private NodeID m_innerLoopStart_767;
    private NodeID m_innerLoopEnd_780;
    private NodeID m_innerJavaSnippet_782;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_outerLoopStart_137 = new NodeID(baseID, 137);
        m_outerLoopEnd_138 = new NodeID(baseID, 138);
        m_innerLoopStart_767 = new NodeID(baseID, 767);
        m_innerLoopEnd_780 = new NodeID(baseID, 780);
        m_innerJavaSnippet_782 = new NodeID(baseID, 782);
    }

    @Test
    public void testExecuteAll() throws Exception {
        checkState(m_outerLoopEnd_138, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(m_outerLoopEnd_138, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteAllThenReset() throws Exception {
        executeAllAndWait();
        reset(m_innerLoopStart_767); // propagates to outer loop
        checkState(m_outerLoopStart_137, InternalNodeContainerState.CONFIGURED);
        checkState(m_innerLoopStart_767, InternalNodeContainerState.CONFIGURED);
    }

    @Test
    public void testPartialExecuteThenResetInnerLoop() throws Exception {
        executeAndWait(m_innerLoopEnd_780);
        checkState(m_innerLoopEnd_780, InternalNodeContainerState.EXECUTED);
        checkState(m_innerLoopStart_767, InternalNodeContainerState.EXECUTED);
        reset(m_innerLoopEnd_780);
        checkState(m_innerLoopEnd_780, InternalNodeContainerState.CONFIGURED);
        // this currently doesn't work: if you reset an inactive loop end
        // it doesn't reset its loop start - not sure if it should?
        // checkState(m_innerLoopStart_767, InternalNodeContainerState.CONFIGURED);

        // execute all again.
        executeAllAndWait();
        checkState(m_outerLoopEnd_138, InternalNodeContainerState.EXECUTED);
    }

}
