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
 *   Jan 9, 2026 (wiswedel): created
 */
package org.knime.core.node.workflow;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.knime.core.node.workflow.WorkflowEvent.Type.NODE_ADDED;
import static org.knime.core.node.workflow.WorkflowEvent.Type.NODE_REMOVED;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.port.PortType;

/**
 * Tests that workflow listeners registered on nested workflows correctly receive events when nodes are removed,
 * either directly from the nested workflow or when parent containers are removed.
 *
 * @author wiswedel
 */
final class WorkflowListenerTest {

    /** factory ID of SOME node type that is used internally, can be any. */
    private static final String TEST_NODE = "org.knime.testing.node.executioncount.ExecutionCountNodeFactory";
    private static NodeFactory<NodeModel> testNodeFactory;

    private WorkflowManager m_wfMgr;
    private WorkflowManager m_outerMetaNode;
    private AtomicReference<WorkflowEvent> m_outerMetaNodeLastEventReference;
    private AtomicReference<WorkflowEvent> m_innerMetaNodeLastEventReference;
    private WorkflowManager m_innerMetaNode;
    private NodeContainer m_innerSingleNodeContainer;

    @BeforeAll
    static void beforeAll() throws Exception {
        final Optional<NodeFactory<NodeModel>> testNodeFactoryOpt =
            NodeFactoryProvider.getInstance().getNodeFactory(TEST_NODE);
        assertThat(String.format("Factory '%s' is no longer known, update the test case", TEST_NODE),
            testNodeFactoryOpt.isPresent());
        testNodeFactory = testNodeFactoryOpt.orElseThrow();
    }

    @AfterAll
    static void afterAll() {
        testNodeFactory = null;
    }

    @BeforeEach
    void initWorkflow() throws Exception {
        m_wfMgr = WorkflowManager.ROOT.createAndAddProject(WorkflowListenerTest.class.getSimpleName(),
            new WorkflowCreationHelper(null));
        m_outerMetaNode = m_wfMgr.createAndAddSubWorkflow(new PortType[0], new PortType[0], "Outer Metanode");

        m_outerMetaNodeLastEventReference = new AtomicReference<>();
        m_outerMetaNode.addListener(m_outerMetaNodeLastEventReference::set);

        m_innerMetaNode = m_outerMetaNode.createAndAddSubWorkflow(new PortType[0], new PortType[0], "Inner Metanode");
        awaitWithTimeout().untilAsserted(() -> {
            assertNotNull(m_outerMetaNodeLastEventReference.get(), "Outer Add Event was sent");
            assertThat("Event type", m_outerMetaNodeLastEventReference.get().getType(), is(NODE_ADDED));
            assertThat("Event value", m_outerMetaNodeLastEventReference.get().getNewValue(), is(m_innerMetaNode));
        });
        m_outerMetaNodeLastEventReference.set(null);

        m_innerMetaNodeLastEventReference = new AtomicReference<>();
        m_innerMetaNode.addListener(m_innerMetaNodeLastEventReference::set);

        NodeID innerNodeId = m_innerMetaNode.createAndAddNode(testNodeFactory);
        m_innerSingleNodeContainer = m_innerMetaNode.getNodeContainer(innerNodeId);
        awaitWithTimeout().untilAsserted(() -> {
            assertNotNull(m_innerMetaNodeLastEventReference.get(), "Inner Add Event was sent");
            assertThat("Event type", m_innerMetaNodeLastEventReference.get().getType(), is(NODE_ADDED));
            assertThat("Event value", m_innerMetaNodeLastEventReference.get().getNewValue(),
                is(m_innerSingleNodeContainer));
        });
        m_innerMetaNodeLastEventReference.set(null);
    }

    @AfterEach
    void discardWorkflow() {
        if (m_wfMgr != null && m_wfMgr.getParent().containsNodeContainer(m_wfMgr.getID())) {
            m_wfMgr.getParent().removeNode(m_wfMgr.getID());
        }
        m_wfMgr = null;
    }

    @Test
    void testEventWhenRemovingInnerNode() throws Exception {
        m_innerMetaNode.removeNode(m_innerSingleNodeContainer.getID());
        awaitWithTimeout().untilAsserted(() -> {
            assertNotNull(m_innerMetaNodeLastEventReference.get(), "Inner Remove Event was sent");
            assertThat("Event type", m_innerMetaNodeLastEventReference.get().getType(), is(NODE_REMOVED));
            assertThat("Event value", m_innerMetaNodeLastEventReference.get().getOldValue(),
                is(m_innerSingleNodeContainer));
        });
    }

    @Test
    void testEventWhenRemovingInnerMetaNode() throws Exception {
        m_outerMetaNode.removeNode(m_innerMetaNode.getID());

        awaitWithTimeout().untilAsserted(() -> {
            assertNotNull(m_innerMetaNodeLastEventReference.get(), "Inner Remove Event was sent");
            assertThat("Event type", m_innerMetaNodeLastEventReference.get().getType(), is(NODE_REMOVED));
            assertThat("Event value", m_innerMetaNodeLastEventReference.get().getOldValue(),
                is(m_innerSingleNodeContainer));
        });

        awaitWithTimeout().untilAsserted(() -> {
            assertNotNull(m_outerMetaNodeLastEventReference.get(), "Outer Remove Event was sent");
            assertThat("Event type", m_outerMetaNodeLastEventReference.get().getType(), is(NODE_REMOVED));
            assertThat("Event value", m_outerMetaNodeLastEventReference.get().getOldValue(),
                is(m_innerMetaNode));
        });
    }

    /**
     * Tests that listeners on nested workflows receive NODE_REMOVED events for all contained nodes when the entire
     * project is removed from its parent.
     */
    @Test
    void testEventWhenRemovingProject() throws Exception {
        m_wfMgr.getParent().removeNode(m_wfMgr.getID());

        awaitWithTimeout().untilAsserted(() -> {
            assertNotNull(m_innerMetaNodeLastEventReference.get(), "Inner Remove Event was sent");
            assertThat("Event type", m_innerMetaNodeLastEventReference.get().getType(), is(NODE_REMOVED));
            assertThat("Event value", m_innerMetaNodeLastEventReference.get().getOldValue(),
                is(m_innerSingleNodeContainer));
        });

        awaitWithTimeout().untilAsserted(() -> {
            assertNotNull(m_outerMetaNodeLastEventReference.get(), "Outer Remove Event was sent");
            assertThat("Event type", m_outerMetaNodeLastEventReference.get().getType(), is(NODE_REMOVED));
            assertThat("Event value", m_outerMetaNodeLastEventReference.get().getOldValue(),
                is(m_innerMetaNode));
        });
    }

    private static ConditionFactory awaitWithTimeout() {
        return Awaitility.await().pollInterval(20, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.SECONDS);
    }
}
