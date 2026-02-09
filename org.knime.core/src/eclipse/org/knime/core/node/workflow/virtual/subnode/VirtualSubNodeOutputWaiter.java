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
 *   May 23, 2023 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.subnode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.knime.core.node.message.Message;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Listener added to a subnode container's WFM to listen for node adding / reset / execution to guarantee that the
 * subnode's virtual output node is the last to execute, possibly pulling the execution of the contained nodes.
 *
 * <p>
 * It's instantiated and run in a subnode's output node <i>execution</i> (so it's relatively short-living) and it works
 * by adding a state change listener to all nodes currently in execution, and checking if there are any executing nodes
 * after these nodes have completed. It needs the extra step/check during the executing since there are node constructs
 * in KNIME that add new nodes while a workflow is executing (nodes such <i>Workflow Executor</i> or <i>Parallel Chunk
 * Loop Start/End</i>.
 *
 * <p>Nodes that were added manually during these steps or nodes that fail their executing will cause the end to fail.
 *
 * @author Bernd Wiswedel, KNIME
 */
final class VirtualSubNodeOutputWaiter {

    private final SubNodeContainer m_subnodeContainer;

    private VirtualSubNodeOutputWaiter(final SubNodeContainer subnodeContainer) {
        m_subnodeContainer = CheckUtils.checkArgumentNotNull(subnodeContainer);
    }

    static VirtualSubNodeOutputWaiter startExecutionAndCreate(final SubNodeContainer subNodeContainer) {
        subNodeContainer.getWorkflowManager().executeAll();
        return new VirtualSubNodeOutputWaiter(subNodeContainer);
    }

    /**
     * Registers listeners to all executing nodes in the subnode and returns a non-empty message when there are
     * non-executed nodes after all nodes have completed their execution.
     *
     * @return A message describing nodes that have not been executed (e.g. due to missing connections) or an empty
     *         {@link Optional} when all nodes (except for the output node are executed).
     * @throws InterruptedException if (output) node's execution was canceled.
     */
    Optional<Message> waitForNodesToExecute() throws InterruptedException {
        List<NodeContainer> nonExecutedNodesList;
        final var wfm = m_subnodeContainer.getWorkflowManager();
        try (var lock = wfm.lock()) {
            boolean hasSeenNodesInExecution;
            final var outputNodeID = m_subnodeContainer.getVirtualOutNodeID();
            do {
                nonExecutedNodesList = new ArrayList<>();
                final var nodesInExecutionToListenerMap = new LinkedHashMap<NodeID, NodeStateChangeListener>();
                try (var waiter =
                    new MultipleNodesToExecutedStateWaiter(nodesInExecutionToListenerMap, lock.getReentrantLock())) {
                    // list might be different in each iteration, see class comment
                    for (var nc : wfm.getNodeContainers()) {
                        if (nc.getID().equals(outputNodeID)) { // NOSONAR (nesting)
                            continue; // the output node causes this code here to be run - ignore it
                        }
                        final var ncState = nc.getNodeContainerState();
                        if (ncState.isExecutionInProgress()) { // NOSONAR (nesting)
                            final var stateChangeListener = new MyStateChangeListener(nc, waiter);
                            nc.addNodeStateChangeListener(stateChangeListener);
                            nodesInExecutionToListenerMap.put(nc.getID(), stateChangeListener);
                        } else if (!ncState.isExecuted()) {
                            nonExecutedNodesList.add(nc);
                        }
                    }
                    hasSeenNodesInExecution = !nodesInExecutionToListenerMap.isEmpty();
                    waiter.awaitNodesExecutions();
                }
            } while (hasSeenNodesInExecution);
            if (nonExecutedNodesList.isEmpty()) {
                return Optional.empty();
            } else {
                // get errors (or, if there are none, warnings) from workflow, prepend some text
                return Optional.of(wfm.getNodeErrorSummary().or(wfm::getNodeWarningSummary) //
                    .map(msg -> msg.modify().withSummary("Errors in workflow - %s".formatted(msg.getSummary())).build()
                        .orElseThrow())
                    .orElse(getNonExecutedNodesListMessage(nonExecutedNodesList, wfm)));
            }
        }
    }

    /**
     * New message with list of nodes that were not executed.
     */
    private static Message getNonExecutedNodesListMessage(final List<NodeContainer> nonExecutedNodesList,
        final WorkflowManager parent) {
        final var msgBuilder = Message.builder() //
            .withSummary("%s node(s) were not executed".formatted(nonExecutedNodesList.size()));
        nonExecutedNodesList.stream().limit(3).forEach(nc -> msgBuilder.addTextIssue(nc.getNameWithID(parent.getID())));
        return msgBuilder.build().orElseThrow();
    }

    /** Allows the caller thread to sleep until all currently executing nodes are done. */
    final class MultipleNodesToExecutedStateWaiter implements AutoCloseable {

        private final ReentrantLock m_waiterLock;
        private final Condition m_waiterCondition;
        private final Map<NodeID, NodeStateChangeListener> m_executingNodeToListenerMap;

        /**
         * Separate list where all listeners of completed nodes are collected for later removal (avoids unregistering
         * the listener from a listener callback).
         */
        private final Map<NodeID, NodeStateChangeListener> m_executedNodeToListenerMap;

        MultipleNodesToExecutedStateWaiter(final Map<NodeID, NodeStateChangeListener> map,
            final ReentrantLock waiterLock) {
            m_executingNodeToListenerMap = map;
            m_executedNodeToListenerMap = new LinkedHashMap<>();
            m_waiterLock = waiterLock;
            m_waiterCondition = waiterLock.newCondition();
        }

        /** Called by each individual node state change listener when it is no longer executing. */
        void done(final NodeID id) {
            CheckUtils.checkState(m_waiterLock.isHeldByCurrentThread(), "Lock should be held during state transition");
            m_waiterLock.lock();
            try {
                var listener = m_executingNodeToListenerMap.remove(id);
                if (listener != null) {
                    m_executedNodeToListenerMap.put(id, listener);
                }
                if (m_executingNodeToListenerMap.isEmpty()) {
                    m_waiterCondition.signalAll();
                }
            } finally {
                m_waiterLock.unlock();
            }
        }

        void awaitNodesExecutions() throws InterruptedException {
            while (!m_executingNodeToListenerMap.isEmpty()) {
                m_waiterCondition.await();
            }
        }

        @Override
        public void close() {
            final var wfm = m_subnodeContainer.getWorkflowManager();
            m_waiterLock.lock();
            try {
                for (var map : List.of(m_executedNodeToListenerMap, m_executingNodeToListenerMap)) {
                    for (var entry : map.entrySet()) {
                        final var nc = wfm.getNodeContainer(entry.getKey(), NodeContainer.class, false);
                        if (nc != null) { // NOSONAR (nesting)
                            nc.removeNodeStateChangeListener(entry.getValue());
                        }
                    }
                    map.clear();
                }
            } finally {
                m_waiterLock.unlock();
            }
        }
    }

    /** Listener added to any executing node to notify waiter when node's state is no longer executing. */
    static final class MyStateChangeListener implements NodeStateChangeListener {

        private final NodeContainer m_nc;
        private final MultipleNodesToExecutedStateWaiter m_waiter;

        MyStateChangeListener(final NodeContainer nc, final MultipleNodesToExecutedStateWaiter waiter) {
            m_nc = nc;
            m_waiter = waiter;
        }

        @Override
        public void stateChanged(final NodeStateEvent state) {
            if (!m_nc.getNodeContainerState().isExecutionInProgress()) {
                m_waiter.done(m_nc.getID());
            }
        }
    }

}
