MISSINGpackage org.knime.core.node.workflow;

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