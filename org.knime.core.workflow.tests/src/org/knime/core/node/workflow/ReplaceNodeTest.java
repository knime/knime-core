/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   May 23, 2020 (hornm): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.NoSuchElementException;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.action.ReplaceNodeResult;
import org.knime.core.node.workflow.node.configurable.ConfigurableNodeWithoutPortsConfigBuilderNodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Tests behavior related to updating native node ports, see SRV-2696 and NXT-1229.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Kai Franze, KNIME GmbH
 */
public class ReplaceNodeTest extends WorkflowTestCase {

	private NodeID m_datagenerator_1;

	private NodeID m_concatenate_2;

	private NodeID m_metanode_4;

	private NodeID m_caseswitchend_12;

	private Integer m_nodePortsChangedEventCounter;

	private final WorkflowListener m_workflowListener = mock(WorkflowListener.class);

	/**
	 * Load workflow.
	 *
	 * @throws Exception
	 */
	@BeforeEach
	public void setup() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		m_datagenerator_1 = new NodeID(baseID, 1);
		m_concatenate_2 = new NodeID(baseID, 2);
		m_metanode_4 = new NodeID(baseID, 4);
		m_caseswitchend_12 = new NodeID(baseID, 12);
		var wfm = getManager();
		wfm.addListener(m_workflowListener);
		setManager(wfm);
		m_nodePortsChangedEventCounter = 0;
		doAnswer(invocation -> {
			m_nodePortsChangedEventCounter++;
			return null;
		}).when(m_workflowListener)
				.workflowChanged(argThat(event -> event.getType().equals(WorkflowEvent.Type.NODE_PORTS_CHANGED)));
	}

	/**
	 * Tests {@link WorkflowManager#canReplaceNode(NodeID)}.
	 */
	@Test
	public void testCanReplaceNode() {
		WorkflowManager wfm = getManager();
		assertThat("metanode can't be replaced", wfm.canReplaceNode(m_metanode_4), is(false));
		wfm.executeAll();
		assertThat("executing node can't be replaced", wfm.canReplaceNode(m_concatenate_2), is(false));
		wfm.cancelExecution();
	}

	/**
	 * Tests
	 * {@link WorkflowManager#replaceNode(NodeID, ModifiableNodeCreationConfiguration)}.
	 */
	@Test
	public void testReplaceNode() {
		WorkflowManager wfm = getManager();
		NativeNodeContainer oldNC = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		ModifiableNodeCreationConfiguration creationConfig = oldNC.getNode().getCopyOfCreationConfig().get();
		creationConfig.getPortConfig().get().getExtendablePorts().get("input").addPort(BufferedDataTable.TYPE);
		creationConfig.getPortConfig().get().getExtendablePorts().get("input").addPort(BufferedDataTable.TYPE);
		wfm.replaceNode(m_concatenate_2, creationConfig);

		NativeNodeContainer newNC = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		assertNodeReplacement(oldNC, newNC, 5, 2);
		assertThat("missing in connections", wfm.getIncomingConnectionsFor(m_concatenate_2).size(), is(2));
		assertThat("missing out connections", wfm.getOutgoingConnectionsFor(m_concatenate_2).size(), is(1));

		// test if node cannot be replaced -> exception
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> wfm.replaceNode(m_metanode_4, creationConfig));
		assertThat("unexpected exception message", e.getMessage(), is("Node cannot be replaced"));

		waitAndCheckNodePortsChangedEventCounterIs(1);
	}

    /**
     * Tests
     * {@link WorkflowManager#replaceNode(NodeID, ModifiableNodeCreationConfiguration, NodeFactory))}.
     */
	@Test
	public void testReplaceNodeWithAnotherNodeType() {
		WorkflowManager wfm = getManager();
		var nodeFactory = ((NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2)).getNode().getFactory();
		var result = wfm.replaceNode(m_datagenerator_1, null, nodeFactory);

		var concatenateNode = wfm.getNodeContainer(m_datagenerator_1, NativeNodeContainer.class, true);
		assertThat("Create new node", concatenateNode.getNode().getFactory().getClass().getSimpleName(),
				is("AppendedRowsNodeFactory"));
		assertThat("Restore a single connection", wfm.getOutgoingConnectionsFor(m_datagenerator_1).size(), is(1));

		result.undo();
		var dataGenNode = wfm.getNodeContainer(m_datagenerator_1, NativeNodeContainer.class, true);
		assertThat("Reverted node replacement", dataGenNode.getNode().getFactory().getClass().getSimpleName(),
				is("SampleDataNodeFactory"));
		assertThat("Restore multiple connections", wfm.getOutgoingConnectionsFor(m_datagenerator_1).size(), is(2));

	}

	/**
	 * Tests that connections remain if a port is inserted before another already
	 * connected one.
	 */
	@Test
	public void testInsertPortBeforeAnotherPort() {
		WorkflowManager wfm = getManager();
		NodeID modelwriter_13 = new NodeID(wfm.getID(), 13);
		NativeNodeContainer oldNC = (NativeNodeContainer) wfm.getNodeContainer(modelwriter_13);
		assertTrue(oldNC.getNodeAnnotation().getData().isDefault(), "node annotation expected to be the default one");

		// Add new port
		ModifiableNodeCreationConfiguration creationConfig = oldNC.getNode().getCopyOfCreationConfig().get();
		creationConfig.getPortConfig().get().getExtendablePorts().get("File System Connection")
				.addPort(FileSystemPortObject.TYPE);
		ReplaceNodeResult replaceRes = wfm.replaceNode(modelwriter_13, creationConfig);

		// Check that connection remains
		NativeNodeContainer newNC = (NativeNodeContainer) wfm.getNodeContainer(modelwriter_13);
		assertThat("connection is gone unexpectedly", wfm.getIncomingConnectionFor(modelwriter_13, 2), notNullValue());
		assertTrue(newNC.getNodeAnnotation().getData().isDefault(), "node annotation expected to be the default one");

		// Try undo
		replaceRes.undo();
		newNC = (NativeNodeContainer) wfm.getNodeContainer(modelwriter_13);
		assertThat("connection is gone unexpectedly", wfm.getIncomingConnectionFor(modelwriter_13, 1), notNullValue());
		assertTrue(newNC.getNodeAnnotation().getData().isDefault(), "node annotation expected to be the default one");

		waitAndCheckNodePortsChangedEventCounterIs(2);
	}

	/**
	 * Tests {@link ReplaceNodeResult#undo()} and makes sure that connections and bendpoints
	 * removed during the node replacement are added back again on undo.
	 */
	@Test
	public void testReplaceNodeUndo() {
		WorkflowManager wfm = getManager();
		NativeNodeContainer oldNC = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);

		// Add port and connect
		ModifiableNodeCreationConfiguration creationConfig = oldNC.getNode().getCopyOfCreationConfig().get();
		creationConfig.getPortConfig().get().getExtendablePorts().get("input").addPort(BufferedDataTable.TYPE);
		wfm.replaceNode(m_concatenate_2, creationConfig);
		ConnectionContainer newCC = wfm.addConnection(m_datagenerator_1, 1, m_concatenate_2, 3);
		ConnectionUIInformation bendPoints = ConnectionUIInformation.builder().addBendpoint(5, 5, 0).build();
		newCC.setUIInfo(bendPoints);
		NativeNodeContainer newNC = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		assertThat("unexpected number of input ports", newNC.getNrInPorts(), is(4));

		// Remove port again
		ReplaceNodeResult replaceRes = wfm.replaceNode(m_concatenate_2,
				oldNC.getNode().getCopyOfCreationConfig().get());
		newNC = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		assertThat("unexpected number of input ports", newNC.getNrInPorts(), is(3));
		assertThat("unexpected connection", wfm.getConnection(newCC.getID()), nullValue());
		assertThat("canUndo expected to be possible", replaceRes.canUndo(), is(true));

		// Undo remove operation
		replaceRes.undo();
		NativeNodeContainer undoNC = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		assertNotSame(newNC, undoNC);
		assertThat("unexpected number of input ports", undoNC.getNrInPorts(), is(4));
		assertThat("connection missing", wfm.getConnection(newCC.getID()), notNullValue());
		assertThat("bendpoints missing", wfm.getConnection(newCC.getID()).getUIInfo().getAllBendpoints(),
				equalTo(bendPoints.getAllBendpoints()));

		waitAndCheckNodePortsChangedEventCounterIs(3);
	}

	/**
	 * Tests if the workflow event also fires when the number of ports stays the
	 * same or decreases
	 */
	@Test
	public void testAddAndRemovePorts() {
		var wfm = getManager();
		var oldNC = (NativeNodeContainer) wfm.getNodeContainer(m_caseswitchend_12);
		var creationConfig = oldNC.getNode().getCopyOfCreationConfig().get();

		// Add new output port to case switch end
		creationConfig.getPortConfig().get().getExtendablePorts().get("Output").addPort(FlowVariablePortObject.TYPE);
		wfm.replaceNode(m_caseswitchend_12, creationConfig);

		var newNC = (NativeNodeContainer) wfm.getNodeContainer(m_caseswitchend_12);
		assertNodeReplacement(oldNC, newNC, 3, 2);
		waitAndCheckNodePortsChangedEventCounterIs(1);

		// Add new input port to case switch end
		var newCreationConfig = newNC.getNode().getCopyOfCreationConfig().get();
		newCreationConfig.getPortConfig().get().getExtendablePorts().get("Input").addPort(FlowVariablePortObject.TYPE);
		wfm.replaceNode(m_caseswitchend_12, newCreationConfig);

		var newNewNC = (NativeNodeContainer) wfm.getNodeContainer(m_caseswitchend_12);
		assertNodeReplacement(newNC, newNewNC, 4, 2);
		waitAndCheckNodePortsChangedEventCounterIs(2);

		// Remove input port from case switch end
		var newNewCreationConfig = newNewNC.getNode().getCopyOfCreationConfig().get();
		newNewCreationConfig.getPortConfig().get().getExtendablePorts().get("Input").removeLastPort();
		wfm.replaceNode(m_caseswitchend_12, newNewCreationConfig);

		var finalNC = (NativeNodeContainer) wfm.getNodeContainer(m_caseswitchend_12);
		assertNodeReplacement(newNewNC, finalNC, 3, 2);
		waitAndCheckNodePortsChangedEventCounterIs(3);

		var caseswitchstart_14 = new NodeID(wfm.getID(), 14);
		var oldNC14 = (NativeNodeContainer) wfm.getNodeContainer(caseswitchstart_14);
		var creationConfig14 = oldNC14.getNode().getCopyOfCreationConfig().get();

		// Add new input port to case switch start
		creationConfig14.getPortConfig().get().getExtendablePorts().get("Input").addPort(BufferedDataTable.TYPE);
		wfm.replaceNode(caseswitchstart_14, creationConfig14);

		var newNC14 =(NativeNodeContainer) wfm.getNodeContainer(caseswitchstart_14);
		assertNodeReplacement(oldNC14, newNC14, 2, 3);
		waitAndCheckNodePortsChangedEventCounterIs(4);

		// Add new output port to case switch end
		var newCreationConfig14 = newNC14.getNode().getCopyOfCreationConfig().get();
		newCreationConfig14.getPortConfig().get().getExtendablePorts().get("Output").addPort(BufferedDataTable.TYPE);
		wfm.replaceNode(caseswitchstart_14, newCreationConfig14);

		var newNewNC14 =(NativeNodeContainer) wfm.getNodeContainer(caseswitchstart_14);
		assertNodeReplacement(newNC14, newNewNC14, 2, 4);
		waitAndCheckNodePortsChangedEventCounterIs(5);				 
	}

	/**
	 * Test certain unreasonable `replaceNode` calls
	 */
	@Test
	public void testReplaceNodeCallsThatShouldNotWorkOrDoNothing() {
		var wfm = getManager();
		var oldNC = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		var creationConfig2 = oldNC.getNode().getCopyOfCreationConfig().get();

		// Try to replace a has no `ModifiableNodeCreationConfiguration`
		var exception1 = assertThrows(IllegalStateException.class,
				() -> wfm.replaceNode(m_datagenerator_1, creationConfig2));
		assertThat("Unexpected exception message", exception1.getMessage(),
				containsString("cannot be replaced, no `ModifiableNodeCreationConfiguration`"));

		// Try to replace a node with a creation config that does not specify ports
		var exception2 = assertThrows(NoSuchElementException.class,
				() -> wfm.replaceNode(m_concatenate_2, new ModifiableNodeCreationConfiguration(null)));
		assertThat("Unexpected exception message", exception2.getMessage(), is("No value present"));

		// Try to replace a node with an identical config
		oldNC = (NativeNodeContainer) wfm.getNodeContainer(m_caseswitchend_12);
		var creationConfig12 = oldNC.getNode().getCopyOfCreationConfig().get();
		wfm.replaceNode(m_caseswitchend_12, creationConfig12);

		waitAndCheckNodePortsChangedEventCounterIs(0);
	}

	/**
	 * Tests replacing a node that doesn't provide a port configuration
	 * @throws  
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@Test
	public void testReplaceNodeWithOutPortConfig() throws Exception {
		var wfm = getManager();
		var factory = NodeFactoryProvider.getInstance() //
				.getNodeFactory(ConfigurableNodeWithoutPortsConfigBuilderNodeFactory.class.getName()).orElse(null);
		var nodeID = wfm.createAndAddNode(factory);
		var oldNC = (NativeNodeContainer) wfm.getNodeContainer(nodeID);
		var creationConfig = oldNC.getNode().getCopyOfCreationConfig().get();

		assertThat(creationConfig.getPortConfig().isEmpty(), is(true));

		wfm.replaceNode(nodeID, creationConfig);

		// Assert nothing changed
		var newNC = (NativeNodeContainer) wfm.getNodeContainer(nodeID);
		assertThat("node ports have changed but shouldn't",
				newNC.getNode().getCopyOfCreationConfig().get().getPortConfig(),
				is(oldNC.getNode().getCopyOfCreationConfig().get().getPortConfig()));

		waitAndCheckNodePortsChangedEventCounterIs(0);
	}

	/**
	 * Makes sure that a previous port configuration (and the respective
	 * connections) is restored on undo after a node was replaced with another node
	 * type
	 *
	 * @throws Exception
	 */
	@Test
	public void testRestorePortConfigOnUndoAfterReplaceWithAnotherNode() throws Exception {
		var wfm = getManager();
		var caseSwitchStart19 = wfm.getID().createChild(19);
		var newFactory = NodeFactoryProvider.getInstance() //
				.getNodeFactory("org.knime.base.node.io.filehandling.model.reader.ModelReaderNodeFactory")
				.orElse(null);
		assertThat(newFactory, is(notNullValue()));
		var replaceResult = wfm.replaceNode(caseSwitchStart19, null, newFactory);
		var nc = wfm.getNodeContainer(caseSwitchStart19, NativeNodeContainer.class, true);
		assertThat(nc.getNode().getFactory().getClass().getSimpleName(), is("ModelReaderNodeFactory"));
		// when undo
		replaceResult.undo();
		// then make sure the same connections are restored
		nc = wfm.getNodeContainer(caseSwitchStart19, NativeNodeContainer.class, true);
		assertThat(nc.getNode().getFactory().getClass().getSimpleName(), is("CaseStartAnyNodeFactory"));
		// output port 1 connects to node 21
        // output port 2 connects to node 22
        // output port 3 connects to node 23
		for(var entry : Map.of(1, 21, 2, 22, 3, 23).entrySet()) {
            var outConnections = wfm.getOutgoingConnectionsFor(caseSwitchStart19, entry.getKey());
            String reason = "Expected a connection from output port %s to node %s".formatted(entry.getKey(), entry.getValue());
            assertThat(reason, outConnections.size(), is(1));
            var outConnection = outConnections.stream().findFirst().get();
            assertThat(reason, outConnection.getDest(), is(wfm.getID().createChild(entry.getValue())));
        }
	}

	/**
	 * Make sure that the original settings are restored when undoing the node
	 * replacement.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRestoreNodeSettingsAndLabelOnUndo() throws Exception {
		var wfm = getManager();
		var originalSettings = new NodeSettings("original settings");
		wfm.saveNodeSettings(m_concatenate_2, originalSettings);
		assertThat(originalSettings.getNodeSettings("model").getString("suffix"), is("_dup_original_setting"));

		var oldNode = ((NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2));
		var replaceResult = wfm.replaceNode(m_concatenate_2, oldNode.getNode().getCopyOfCreationConfig().get(), null,
				false);
		var nnc = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		var newSettings = nnc.getNodeSettings();
		var newLabel = nnc.getNodeAnnotation().getText();
		assertThat(newSettings.getNodeSettings("model").getString("suffix"), is("_dup"));
		assertThat(newLabel, is(""));

		replaceResult.undo();
		var originalSettingsAfterUndo = new NodeSettings("original settings");
		wfm.saveNodeSettings(m_concatenate_2, originalSettingsAfterUndo);
		var originalLabelAfterUndo = wfm.getNodeContainer(m_concatenate_2).getNodeAnnotation().getText();
		assertThat(originalSettingsAfterUndo.getNodeSettings("model").getString("suffix"), is("_dup_original_setting"));
		assertThat(originalLabelAfterUndo, is("Node 2"));
	}

	/**
	 * Assert that settings are transfered to the node replacement, if desired.
	 * 
	 * @throws InvalidSettingsException
	 */
	@Test
	public void testTransferSettingsAndLabel() throws InvalidSettingsException {
		var wfm = getManager();
		var originalSettings = new NodeSettings("original settings");
		wfm.saveNodeSettings(m_concatenate_2, originalSettings);
		assertThat(originalSettings.getNodeSettings("model").getString("suffix"), is("_dup_original_setting"));

		// transfer settings and label true
		var oldNode = ((NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2));
		wfm.replaceNode(m_concatenate_2, oldNode.getNode().getCopyOfCreationConfig().get(), null, true);
		var nnc = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		var newSettings = nnc.getNodeSettings();
		var newLabel = nnc.getNodeAnnotation().getText();
		assertThat(newSettings.getNodeSettings("model").getString("suffix"), is("_dup_original_setting"));
		assertThat(newLabel, is("Node 2"));

		// transfer settings and label false
		wfm.replaceNode(m_concatenate_2, oldNode.getNode().getCopyOfCreationConfig().get(), null, false);
		nnc = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		newSettings = nnc.getNodeSettings();
		newLabel = nnc.getNodeAnnotation().getText();
		assertThat(newSettings.getNodeSettings("model").getString("suffix"), is("_dup"));
		assertThat(newLabel, is(""));
	}

	/**
	 * Replaces a node of the same type by explicitly specifying the type. But
	 * without providing a creation config.
	 *
	 * @throws InvalidSettingsException
	 */
	@Test
	public void testReplaceNodeOfSameTypeWithoutCreationConfig() throws InvalidSettingsException {
		var wfm = getManager();
		var originalSettings = new NodeSettings("original settings");
		wfm.saveNodeSettings(m_concatenate_2, originalSettings);
		assertThat(originalSettings.getNodeSettings("model").getString("suffix"), is("_dup_original_setting"));

		var nnc = (NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2);
		// provide no node creation config, but the exact same factory
		var result = wfm.replaceNode(m_concatenate_2, null, nnc.getNode().getFactory(), false);
		var newSettings = ((NativeNodeContainer) wfm.getNodeContainer(m_concatenate_2)).getNodeSettings();
		assertThat(newSettings.getNodeSettings("model").getString("suffix"), is("_dup"));

		result.undo();
		var undoSettings = new NodeSettings("original settings");
		wfm.saveNodeSettings(m_concatenate_2, undoSettings);
		assertThat(undoSettings.getNodeSettings("model").getString("suffix"), is("_dup_original_setting"));
	}

	private void waitAndCheckNodePortsChangedEventCounterIs(final int numberOfEvents) {
		Awaitility.waitAtMost(Duration.ONE_SECOND).untilAsserted(
				() -> assertThat("NODE_PORTS_CHANGED was not received exactly " + numberOfEvents + " times",
						m_nodePortsChangedEventCounter, is(numberOfEvents)));
	}

	private void assertNodeReplacement(final NativeNodeContainer oldNC, final NativeNodeContainer newNC,
			final int nrInPorts, final int nrOutPorts) {
		assertThat("node hasn't been replaced", newNC, is(not(oldNC)));
		assertThat("node annotations not the same", newNC.getNodeAnnotation().getData().getText(),
				is(oldNC.getNodeAnnotation().getData().getText()));
		assertThat("node's x postion changed", newNC.getUIInformation().getBounds()[0],
				is(oldNC.getUIInformation().getBounds()[0]));
		assertThat("node's y postion changed", newNC.getUIInformation().getBounds()[1],
				is(oldNC.getUIInformation().getBounds()[1]));
		assertThat("unexpected number of input ports", newNC.getNrInPorts(), is(nrInPorts));
		assertThat("unexpected number of output ports", newNC.getNrOutPorts(), is(nrOutPorts));
	}

}