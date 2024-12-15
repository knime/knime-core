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
 */
package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Expanding a component having "through" connections fails. 
 * 
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP20727_ExpandComponentWithThroughConnection extends WorkflowTestCase { // NOSONAR this naming is our standard

	private NodeID m_tableCreator_A_1;
	private NodeID m_tableCreator_B_2;
	private NodeID m_javaSnippet_A_4;
	private NodeID m_javaSnippet_B_3;
	private NodeID m_javaSnippet_B_10;
	private NodeID m_javaSnippet_C_8;
	private NodeID m_component_9;
	private NodeID m_javaSnippet_B_9_3;


    @BeforeEach
    public void beforeEach() throws Exception {
        final var baseID = loadAndSetWorkflow();
        m_tableCreator_A_1 = baseID.createChild(1);
        m_tableCreator_B_2 = baseID.createChild(2);
        m_javaSnippet_A_4 = baseID.createChild(4);
        m_javaSnippet_B_3 = baseID.createChild(3);
        m_javaSnippet_B_10 = baseID.createChild(10);
        m_javaSnippet_C_8 = baseID.createChild(8);
        m_component_9 = baseID.createChild(9);
        m_javaSnippet_B_9_3 = m_component_9.createChild(0).createChild(3);
    }

    @Test
    public void testExecuteUnmodified() throws Exception {
    	executeAllAndWait();
    	checkState(getManager(), EXECUTED);
    }

    @Test
    public void testExpandAndExecute() throws Exception {
    	final var expandResult = getManager().expandSubWorkflow(m_component_9);
    	assertThat("Component present", getManager().containsNodeContainer(m_component_9), is(false));

    	final var connA_4 = findInConnection(m_javaSnippet_A_4, 1);
		assertThat("Input connection to java snippet #4", connA_4, notNullValue());
		assertThat("Input connection to java snippet #4 - source", connA_4.getSource(), is(m_tableCreator_A_1));
		assertThat("Input connection to java snippet #4 - source port", connA_4.getSourcePort(), is(1));

		final var connB_10 = findInConnection(m_javaSnippet_B_10, 1);
		assertThat("Input connection to java snippet #10", connB_10, notNullValue());
		assertThat("Input connection to java snippet #10 - source", connB_10.getSource(), is(m_tableCreator_B_2));
		assertThat("Input connection to java snippet #10 - source port", connB_10.getSourcePort(), is(1));

		final var connB_3 = findInConnection(m_javaSnippet_B_3, 1);
		assertThat("Input connection to java snippet #3", connB_3, notNullValue());
		assertThat("Input connection to java snippet #3 - source", connB_3.getSource(), is(m_tableCreator_B_2));
		assertThat("Input connection to java snippet #3 - source port", connB_3.getSourcePort(), is(1));

		final var connC_8 = findInConnection(m_javaSnippet_C_8, 1);
		final var tableCreator_C_5 = getManager().getID().createChild(5); // created by 'expand' operation
		assertThat("Node inserted after subnode expand", findNodeContainer(tableCreator_C_5), notNullValue());
		assertThat("Input connection to java snippet #8", connC_8, notNullValue());
		assertThat("Input connection to java snippet #8 - source", connC_8.getSource(), is(tableCreator_C_5));
		assertThat("Input connection to java snippet #8 - source port", connC_8.getSourcePort(), is(1));

		// created by 'expand' operation (and '11' is a logical guess but might change in the future)
		final var javaSnippet_B_11 = getManager().getID().createChild(11);
		assertThat("Node inserted after subnode expand", findNodeContainer(javaSnippet_B_11), notNullValue());
		final var connB_11 = findInConnection(javaSnippet_B_11, 1);
		assertThat("Input connection to java snippet #11", connB_11, notNullValue());
		assertThat("Input connection to java snippet #11 - source", connB_11.getSource(), is(m_tableCreator_B_2));
		assertThat("Input connection to java snippet #11 - source port", connB_11.getSourcePort(), is(1));

		executeAllAndWait();
		checkState(getManager(), EXECUTED);

		// now undo, execute, check state
		assertThat("Can undo the expand", expandResult.canUndo(), is(true));
		expandResult.undo();
		assertThat("Node inserted after undo'ing subnode expand", findNodeContainer(m_component_9), notNullValue());
		assertThat("Node removed after undo'ing subnode expand", getManager().containsNodeContainer(javaSnippet_B_11),
				is(false));
		assertThat("Node removed after undo'ing subnode expand", getManager().containsNodeContainer(tableCreator_C_5),
				is(false));

		executeAllAndWait();
		checkState(getManager(), EXECUTED);
    }

}