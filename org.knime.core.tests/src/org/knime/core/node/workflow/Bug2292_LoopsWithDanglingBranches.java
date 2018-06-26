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

import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * bug 2292: Loop restart with dangling branches flaky.
 * https://bugs.knime.org/show_bug.cgi?id=2292
 * @author wiswedel, University of Konstanz
 */
public class Bug2292_LoopsWithDanglingBranches extends WorkflowTestCase {

    private NodeID m_javaSnippet7;
    private NodeID m_loopEnd4;
    private NodeID m_javaSnippet13;
    private NodeID m_loopEnd12;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_javaSnippet7 = new NodeID(baseID, 7);
        m_javaSnippet13 = new NodeID(baseID, 13);
        m_loopEnd4 = new NodeID(baseID, 4);
        m_loopEnd12 = new NodeID(baseID, 12);
    }

    /** Exec all - expect everything to be executed. */
    @Test
    public void testExecuteAll() throws Exception {
        checkState(getManager(), CONFIGURED);
        executeAllAndWait();
        checkState(getManager(), EXECUTED);
        checkStateOfMany(EXECUTED, m_loopEnd4, m_javaSnippet7);
        checkStateOfMany(EXECUTED, m_loopEnd12, m_javaSnippet13);
    }

    /** Execute top loop (one iteration) - the side branch used to be not executed. */
    @Test
    public void testSingleIterationExecuteUpToHere() throws Exception {
        executeAndWait(m_loopEnd4);
        checkState(m_loopEnd4, EXECUTED);
        // this used to be configured before 3.0 - fixed as part of the bug fix
        NodeContainerState jsState = getManager().getNodeContainer(m_javaSnippet7).getNodeContainerState();
        assertTrue("Not executing, nor executed: " + jsState, jsState.isExecutionInProgress() || jsState.isExecuted());
        waitWhileInExecution();
        checkState(m_javaSnippet7, EXECUTED);
    }

    /** Execute bottom loop (many iterations) - the is executed (and has always been). */
    @Test
    public void testManyIterationsExecuteUpToHere() throws Exception {
        executeAndWait(m_loopEnd12);
        waitWhileInExecution();
        checkState(m_loopEnd12, EXECUTED);
        checkState(m_javaSnippet13, EXECUTED); // this was also executed before 2.12 - multiple iterations
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
