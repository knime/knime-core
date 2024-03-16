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
 *
 */
package org.knime.core.node.workflow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.port.MetaPortInfo;

/**
 * Tests https://knime-com.atlassian.net/browse/AP-15527
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP15527_reconfigureSubNodeAndMetaNodePorts extends WorkflowTestCase {

    private NodeID m_subNodeGenerate_5;
    private NodeID m_metaNodeGenerate_6;
    private NodeID m_metaNodeTest_10;
    private NodeID m_metaNodeTest_11;
    private NodeID m_subNodeTest_9;
    private NodeID m_subNodeTest_12;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_subNodeGenerate_5 = baseID.createChild(5);
        m_metaNodeGenerate_6 = baseID.createChild(6);
        m_metaNodeTest_10 = baseID.createChild(10);
        m_metaNodeTest_11 = baseID.createChild(11);
        m_subNodeTest_9 = baseID.createChild(9);
        m_subNodeTest_12 = baseID.createChild(12);
    }
    
    /**
     * Just run workflow unmodified. Baseline test.
     */
    @Test
    public void testExecuteAllUnchanged() throws Exception {
    	executeAllAndWait();
    	assertAllExecuted();
    }
    
    /**
     * Swap connections on Node 5, then execute all and expect it to be green.
     */
    @Test
    public void testSwappedConnections_SubNodeGenerate() throws Exception {
    	executeAllAndWait();
        final WorkflowManager manager = getManager();
		MetaPortInfo[] outPorts = manager.getSubnodeOutputPortInfo(m_subNodeGenerate_5);
        MetaPortInfo[] newOutPorts = new MetaPortInfo[3];
        newOutPorts[0] = outPorts[0];
        newOutPorts[1] = MetaPortInfo.builder(outPorts[2]).setNewIndex(1).build();
        newOutPorts[2] = MetaPortInfo.builder(outPorts[1]).setNewIndex(2).build();
        doAndWaitForWorkflowEvent(manager, () -> manager.changeSubNodeOutputPorts(m_subNodeGenerate_5, newOutPorts));
        checkState(m_subNodeGenerate_5, InternalNodeContainerState.CONFIGURED);
        checkState(m_subNodeTest_9, InternalNodeContainerState.IDLE);
        executeAllAndWait();
        assertAllExecuted();
    }

    /**
     * Swap connections on Node 6, then execute all and expect it to be green.
     */
    @Test
    public void testSwappedConnections_MetaNodeGenerate() throws Exception {
    	executeAllAndWait();
        final WorkflowManager manager = getManager();
		MetaPortInfo[] outPorts = manager.getMetanodeOutputPortInfo(m_metaNodeGenerate_6);
        MetaPortInfo[] newOutPorts = new MetaPortInfo[2];
        newOutPorts[0] = MetaPortInfo.builder(outPorts[1]).setNewIndex(0).build();
        newOutPorts[1] = MetaPortInfo.builder(outPorts[0]).setNewIndex(1).build();
        doAndWaitForWorkflowEvent(manager, () -> manager.changeMetaNodeOutputPorts(m_metaNodeGenerate_6, newOutPorts));
		Awaitility
				.await().atMost(5, TimeUnit.SECONDS).pollInterval(10,
						TimeUnit.MILLISECONDS)
				.untilAsserted(() -> assertThat("State of metanode output port 0 after port switch",
						manager.getNodeContainer(m_metaNodeGenerate_6, WorkflowManager.class, true).getOutPort(0)
								.getNodeState(),
						is(InternalNodeContainerState.EXECUTED)));
		Awaitility
				.await().atMost(5, TimeUnit.SECONDS).pollInterval(10,
						TimeUnit.MILLISECONDS)
				.untilAsserted(() -> assertThat("State of metanode output port 1 after port switch",
						manager.getNodeContainer(m_metaNodeGenerate_6, WorkflowManager.class, true).getOutPort(1)
								.getNodeState(),
						is(InternalNodeContainerState.EXECUTED)));
        checkState(m_subNodeTest_12, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        assertAllExecuted();
    }
    
    /**
     * Swap connections on Node 9 and 12, then execute all and expect it to be green.
     */
    @Test
    public void testSwappedConnections_SubNodeTest() throws Exception {
    	executeAllAndWait();
        final WorkflowManager manager = getManager();
		MetaPortInfo[] inPorts = manager.getSubnodeInputPortInfo(m_subNodeTest_9);
        MetaPortInfo[] newInPorts = new MetaPortInfo[3];
        newInPorts[0] = inPorts[0];
        newInPorts[1] = MetaPortInfo.builder(inPorts[2]).setNewIndex(1).build();
        newInPorts[2] = MetaPortInfo.builder(inPorts[1]).setNewIndex(2).build();
        manager.changeSubNodeInputPorts(m_subNodeTest_9, newInPorts);
        checkState(m_subNodeGenerate_5, InternalNodeContainerState.EXECUTED);
        checkState(m_subNodeTest_9, InternalNodeContainerState.CONFIGURED);

        MetaPortInfo[] inPorts2 = manager.getSubnodeInputPortInfo(m_subNodeTest_12);
        MetaPortInfo[] newInPorts2 = new MetaPortInfo[3];
        newInPorts2[0] = inPorts2[0];
        newInPorts2[1] = MetaPortInfo.builder(inPorts2[2]).setNewIndex(1).build();
        newInPorts2[2] = MetaPortInfo.builder(inPorts2[1]).setNewIndex(2).build();
        manager.changeSubNodeInputPorts(m_subNodeTest_12, newInPorts2);
		assertThat("State of metanode output port 0 after port switch", manager
				.getNodeContainer(m_metaNodeGenerate_6, WorkflowManager.class, true).getOutPort(0).getNodeState(),
				is(InternalNodeContainerState.EXECUTED));
        checkState(m_subNodeTest_12, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        assertAllExecuted();
    }
    
    /**
     * Swap connections on Node 10 and 11, then execute all and expect it to be green.
     */
    @Test
    public void testSwappedConnections_MetaNodeTest() throws Exception {
    	executeAllAndWait();
        MetaPortInfo[] inPorts1 = getManager().getMetanodeInputPortInfo(m_metaNodeTest_10);
        MetaPortInfo[] newInPorts1 = new MetaPortInfo[2];
        newInPorts1[0] = MetaPortInfo.builder(inPorts1[1]).setNewIndex(0).build();
        newInPorts1[1] = MetaPortInfo.builder(inPorts1[0]).setNewIndex(1).build();
        getManager().changeMetaNodeInputPorts(m_metaNodeTest_10, newInPorts1);
        checkState(m_subNodeGenerate_5, InternalNodeContainerState.EXECUTED);
		for (NodeContainer nc : getManager().getNodeContainer(m_metaNodeTest_10, WorkflowManager.class, true)
				.getNodeContainers()) {
			checkState(nc, InternalNodeContainerState.CONFIGURED);
		}

		MetaPortInfo[] inPorts2 = getManager().getMetanodeInputPortInfo(m_metaNodeTest_11);
		MetaPortInfo[] newInPorts2 = new MetaPortInfo[2];
		newInPorts2[0] = MetaPortInfo.builder(inPorts2[1]).setNewIndex(0).build();
		newInPorts2[1] = MetaPortInfo.builder(inPorts2[0]).setNewIndex(1).build();
		getManager().changeMetaNodeInputPorts(m_metaNodeTest_11, newInPorts2);
		checkState(m_subNodeGenerate_5, InternalNodeContainerState.EXECUTED);
		for (NodeContainer nc : getManager().getNodeContainer(m_metaNodeTest_11, WorkflowManager.class, true)
				.getNodeContainers()) {
			checkState(nc, InternalNodeContainerState.CONFIGURED);
		}
		
        executeAllAndWait();
        assertAllExecuted();
    }
    
    private void assertAllExecuted() throws Exception {
    	checkState(m_subNodeGenerate_5, EXECUTED);
    	checkState(m_metaNodeGenerate_6, EXECUTED);
    	checkState(m_metaNodeTest_10, EXECUTED);
    	checkState(m_metaNodeTest_11, EXECUTED);
    	checkState(m_subNodeTest_9, EXECUTED);
    	checkState(m_subNodeTest_12, EXECUTED);
    }
    
}
