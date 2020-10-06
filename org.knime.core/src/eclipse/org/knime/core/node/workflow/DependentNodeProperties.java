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
 *   Sep 21, 2020 (hornm): created
 */
package org.knime.core.node.workflow;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents node properties that depend on other nodes in the workflow graph (i.e. successors or predecessors). This
 * class helps to be able to calculate those properties in one go and caches them for subsequent usage.
 *
 * Dependent node properties are {@link WorkflowManager#hasExecutablePredecessor(NodeID)} and
 * {@link WorkflowManager#hasSuccessorInProgress(NodeID)}.
 *
 * NOTE: this class is meant to be a TEMPORARY workaround. A long-term solution to this problem (the efficient
 * calculation of those dependent node properties) should be covered by and tightly integrated into the workflow-manager
 * API/framework.
 *
 * Another note: every time the {@link #update()}-method is called the entire workflow (of the associated workflow
 * manager and partly of the parent workflow manager) graph is traversed. That maybe could be avoided somehow if we knew
 * what parts of the graph have been changed (a changed node state itself is not enough).
 *
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class DependentNodeProperties {

    // all nodes including the nc parent (metanode or component; unless this is the project workflow)
    private final Map<NodeID, Properties> m_props = new HashMap<>();

    private final WorkflowManager m_wfm;

    /**
     * @param wfm the 'dependent properties' of the nodes contained in the given workflow manager will be updated
     */
    DependentNodeProperties(final WorkflowManager wfm) {
        m_wfm = wfm;
    }

    /**
     * Whether a node can be executed. Equivalent to the {@link WorkflowManager#canExecuteNode(NodeID)}-method, but
     * faster if called for many (or usually all) nodes.
     *
     * @param id
     * @return <code>true</code> if the node can be executed, otherwise <code>false</code>
     * @throws NoSuchElementException if the given node id couldn't be found
     */
    public boolean canExecuteNode(final NodeID id) {
        if (!m_props.containsKey(id)) {
            throw new NoSuchElementException("No pre-calculated can-execute predicate found for node id " + id);
        }
        return m_wfm.canExecuteNode(id, i -> m_props.get(i).hasExecutablePredecessors());
    }

   /**
     * Whether a node can be reset. Equivalent to the {@link WorkflowManager#canResetNode(NodeID)}-method, but faster if
     * called for many (or usually all) nodes.
     *
     * @param id
     * @return <code>true</code> if the node can be reset, otherwise <code>false</code>
     * @throws NoSuchElementException if the given node id couldn't be found
     */
    public boolean canResetNode(final NodeID id) {
        if (!m_props.containsKey(id)) {
            throw new NoSuchElementException("No pre-calculated can-reset predicate found for node id " + id);
        }
        return m_wfm.canResetNode(id, i -> m_props.get(i).hasExecutingSuccessors());
    }

    /**
     * Updates the node dependent properties. The respective workflow need to be locked when the update is performed.
     */
    void update() {
        assert m_wfm.isLockedByCurrentThread();
        LinkedList<NodeID> hasExecutingSuccessors = new LinkedList<>();
        LinkedList<NodeID> hasExecutablePredecessors = new LinkedList<>();
        findAndInitStartNodes(m_wfm, m_props, hasExecutingSuccessors, hasExecutablePredecessors);
        if (m_props.size() > m_wfm.getNodeContainers().size()) {
            removeSurplusNodes(m_wfm, m_props);
        }
        updateHasExecutablePredecessorsProperties(hasExecutablePredecessors);
        updateHasExecutingSuccessorsProperties(hasExecutingSuccessors);
    }

    private void updateHasExecutablePredecessorsProperties(final Queue<NodeID> startNodes) {
        iterateNodes(startNodes, this::hasExecutablePredecessorVisitor, id -> successors(id, m_wfm));
    }

    private void updateHasExecutingSuccessorsProperties(final Queue<NodeID> startNodes) {
        iterateNodes(startNodes, this::hasExecutingSuccessorVisitor, id -> predecessors(id, m_wfm));
    }

    private static boolean isComponentWFM(final WorkflowManager wfm) {
        return wfm.getDirectNCParent() instanceof SubNodeContainer;
    }

    private static void removeSurplusNodes(final WorkflowManager wfm, final Map<NodeID, Properties> props) {
        props.entrySet().removeIf(e -> !wfm.containsNodeContainer(e.getKey()) && !e.getKey().equals(wfm.getID()));
    }

    private static void iterateNodes(final Queue<NodeID> queue, final Predicate<NodeID> visitor,
        final Function<NodeID, Set<NodeID>> nextNodes) {
        while (!queue.isEmpty()) {
            NodeID current = queue.poll();
            for (NodeID next : nextNodes.apply(current)) {
                if (visitor.test(next)) {
                    queue.add(next);
                }
            }
        }
    }

    private static Set<NodeID> predecessors(final NodeID id, final WorkflowManager wfm) {
        if (wfm.containsNodeContainer(id)) {
            return wfm.getIncomingConnectionsFor(id).stream().filter(cc -> !cc.getSource().equals(wfm.getID()))
                .map(ConnectionContainer::getSource).collect(Collectors.toSet());
        } else {
            if (isComponentWFM(wfm)) {
                return singleton(getParentComponent(wfm).getVirtualOutNodeID());
            } else {
                // collect all predecessors connected from the within the workflow
                return wfm.getParent().getIncomingConnectionsFor(id).stream()
                    .filter(cc -> cc.getSource().equals(wfm.getID()))
                    .map(cc -> wfm.getIncomingConnectionFor(wfm.getID(), cc.getSourcePort()).getSource())
                    .collect(toSet());
            }
        }
    }

    private static Set<NodeID> successors(final NodeID id, final WorkflowManager wfm) {
        if (wfm.containsNodeContainer(id)) {
            return wfm.getOutgoingConnectionsFor(id).stream().filter(cc -> !cc.getDest().equals(wfm.getID()))
                .map(ConnectionContainer::getDest).collect(Collectors.toSet());
        } else {
            if (isComponentWFM(wfm)) {
                // regard the parent component as successor-free
                return Collections.emptySet();
            } else {
                // collect all successors connected within the workflow
                return wfm.getParent().getOutgoingConnectionsFor(id).stream()
                    .filter(cc -> cc.getDest().equals(wfm.getID()))
                    .map(cc -> wfm.getOutgoingConnectionsFor(wfm.getID(), cc.getDestPort())).flatMap(Collection::stream)
                    .map(ConnectionContainer::getDest).collect(toSet());
            }
        }
    }

    private static void findAndInitStartNodes(final WorkflowManager wfm, final Map<NodeID, Properties> props,
        final Queue<NodeID> hasExecutingSuccessors, final Queue<NodeID> hasExecutablePredecessors) {

        findNodesConnectedToTheWorkflowFromTheOutside(wfm, hasExecutingSuccessors, hasExecutablePredecessors);

        // iterate the node within the workflow
        for (NodeContainer nc : wfm.getNodeContainers()) {
            NodeID id = nc.getID();
            Properties p = props.computeIfAbsent(id, i -> new Properties());

            NodeContainerState s = nc.getNodeContainerState();
            p.setHasExecutingSuccessors(isExecutionInProgress(s));
            if (isExecuting(s)) {
                hasExecutingSuccessors.add(id);
            }

            p.setHasExecutablePredecessors(s.isConfigured());
            if (s.isConfigured()) {
                hasExecutablePredecessors.add(id);
            }
        }

    }

    /*
     * If the workflow is part of a component: the parent component itself is added to the list of executing successors
     * if it is executing (but not(!) to the list of executable predecessors because the direct predecessor of a
     * component needs to be executed anyway for a component to be executable in its entirety)
     *
     * If this workflow is a metanode: all nodes (in the parent workflow) connected to metanode are added to the list
     * of 'executing successors' or 'executable predecessors', iff they are executing or executable (parts of the parent
     * workflow is traversed in order to check that).
     */
    private static void findNodesConnectedToTheWorkflowFromTheOutside(final WorkflowManager wfm, // NOSONAR
        final Queue<NodeID> hasExecutingSuccessors, final Queue<NodeID> hasExecutablePredecessors) {
        if (!wfm.isProject() && !wfm.isComponentProjectWFM()) {
            if (isComponentWFM(wfm)) {
                SubNodeContainer snc = getParentComponent(wfm);
                if (snc.getParent().hasSuccessorInProgress(snc.getID())) {
                    hasExecutingSuccessors.add(snc.getID());
                }
            } else {
                WorkflowManager parent = wfm.getParent();
                wfm.getParent().getOutgoingConnectionsFor(wfm.getID()).forEach(c -> {
                    NodeID id = c.getDest();
                    if (parent.hasSuccessorInProgress(id)) {
                        hasExecutingSuccessors.add(id);
                    }
                });
                wfm.getParent().getIncomingConnectionsFor(wfm.getID()).forEach(c -> {
                    NodeID id = c.getSource();
                    if (parent.hasExecutablePredecessor(id)) {
                        hasExecutablePredecessors.add(id);
                    }
                });
            }
        }
    }

    private static boolean isExecutionInProgress(final NodeContainerState s) {
        return s.isExecutionInProgress() || s.isExecutingRemotely();
    }

    private static boolean isExecuting(final NodeContainerState s) {
        return (s.isExecutionInProgress() && !s.isWaitingToBeExecuted()) || s.isExecutingRemotely();
    }

    private boolean hasExecutablePredecessorVisitor(final NodeID id) {
        Properties p = m_props.get(id);
        if (p.hasExecutablePredecessors()) {
            return false;
        } else {
            p.setHasExecutablePredecessors(true);
            return true;
        }
    }

    private boolean hasExecutingSuccessorVisitor(final NodeID id) {
        Properties p = m_props.get(id);
        if (p.hasExecutingSuccessors()) {
            return false;
        } else {
            p.setHasExecutingSuccessors(true);
            return true;
        }
    }

    private static SubNodeContainer getParentComponent(final WorkflowManager wfm) {
        return (SubNodeContainer)wfm.getDirectNCParent();
    }

    private static class Properties {

        private boolean m_hasExecutablePredecessors;

        private boolean m_hasExecutingSuccessors;

        Properties() {
            this(false, false);
        }

        Properties(final boolean hasExecutablePredecessors, final boolean hasExecutingSuccessors) {
            m_hasExecutablePredecessors = hasExecutablePredecessors;
            m_hasExecutingSuccessors = hasExecutingSuccessors;
        }

        boolean hasExecutablePredecessors() {
            return m_hasExecutablePredecessors;
        }

        boolean hasExecutingSuccessors() {
            return m_hasExecutingSuccessors;
        }

        void setHasExecutablePredecessors(final boolean b) {
            m_hasExecutablePredecessors = b;
        }

        void setHasExecutingSuccessors(final boolean b) {
            m_hasExecutingSuccessors = b;
        }
    }

}
