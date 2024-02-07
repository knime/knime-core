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
 * History
 *   Oct 29, 2008 (mb): extracted from WorkflowManager
 */
package org.knime.core.node.workflow;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.PortType;
import org.knime.core.util.Pair;

/** Container class wrapping wrapping the network of nodes forming
 * a workflow together with some of the basic functionality, especially
 * traversal methods.
 *
 * @author M. Berthold, University of Konstanz
 */
class Workflow {

    /** my logger. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(Workflow.class);

    /** mapping from NodeID to Nodes. */
    private final TreeMap<NodeID, NodeContainer> m_nodes = new TreeMap<NodeID, NodeContainer>();

    // Connections (by node, source and destination). Note that meta
    // connections (in- and outgoing of this workflow) are also part
    // of these maps. They will have our own ID as source/dest ID.

    /** mapping from source NodeID to set of outgoing connections. */
    private final Map<NodeID, Set<ConnectionContainer>> m_connectionsBySource
                                  = new TreeMap<NodeID, Set<ConnectionContainer>>();
    /** mapping from destination NodeID to set of incoming connections. */
    private final Map<NodeID, Set<ConnectionContainer>> m_connectionsByDest
                                  = new TreeMap<NodeID, Set<ConnectionContainer>>();

    private WorkflowManager m_wfm;
    private NodeID m_id;

    /**
     * Constructor - initialize sets for metanode in/out connections.
     *
     * @param wfm WorkflowManager holding this workflow (note that this may not completely initalized!)
     * @param id of workflow
     *
     */
    Workflow(final WorkflowManager wfm, final NodeID id) {
        m_wfm = wfm;
        m_id = id;
        // add sets for this (meta-) node's in- and output connections
        m_connectionsByDest.put(id, new LinkedHashSet<ConnectionContainer>());
        m_connectionsBySource.put(id, new LinkedHashSet<ConnectionContainer>());
        clearGraphAnnotationCache();
    }

    /**
     * @return id of this workflow
     */
    NodeID getID() {
        return m_id;
    }

    /**
     * Create a new, unique node ID. Should be run within a synchronized
     * block to avoid duplicates!
     *
     * @return next available unused index.
     */
    NodeID createUniqueID() {
        int nextIndex = 1;
        if (getNrNodes() > 0) {
            NodeID lastID = m_nodes.lastKey();
            nextIndex = lastID.getIndex() + 1;
        }
        NodeID newID = new NodeID(this.getID(), nextIndex);
        assert !containsNodeKey(newID);
        return newID;
    }


    /** Return NodeContainer for a given id or null if that node does not exist
     * in this workflow.
     *
     * @param id of the node
     * @return node with that id
     */
    NodeContainer getNode(final NodeID id) {
        return m_nodes.get(id);
    }

    /** Store NodeContainer with a given id.
     *
     * @param id of NC
     * @param nc NodeContainer itself
     */
    void putNode(final NodeID id, final NodeContainer nc) {
        // create Sets of in and outgoing connections
        m_connectionsBySource.put(id, new LinkedHashSet<ConnectionContainer>());
        m_connectionsByDest.put(id, new LinkedHashSet<ConnectionContainer>());
        // and then add node (avoid inconsistent node - connection setup)
        m_nodes.put(id, nc);
        clearGraphAnnotationCache();
    }

    /** Remove given node.
     *
     * @param id of NodeContainer to be removed.
     * @return removed NodeContainer
     */
    NodeContainer removeNode(final NodeID id) {
        // remove node
        NodeContainer node = m_nodes.remove(id);
        // and then clean up the connection lists  (avoid inconsistent node - connection setup)
        m_connectionsBySource.remove(id);
        m_connectionsByDest.remove(id);
        clearGraphAnnotationCache();
        // and return removed node container
        return node;
    }

    /**
     * @return collection of all NodeContainers that are part of this workflow.
     */
    Collection<NodeContainer> getNodeValues() {
        Collection<NodeContainer> cnc = m_nodes.values();
        return Collections.unmodifiableCollection(cnc);
    }

    /**
     * @return unmodifiable collection of all NodeIDs that are part of this workflow.
     */
    Set<NodeID> getNodeIDs() {
        Set<NodeID> sn = m_nodes.keySet();
        return Collections.unmodifiableSet(sn);
    }

    /**
     * @return number of nodes
     */
    int getNrNodes() {
        return m_nodes.size();
    }

    /**
     * @param id of node.
     * @return true of a node with this key already exists.
     */
    boolean containsNodeKey(final NodeID id) {
        return m_nodes.containsKey(id);
    }

    /** Return all connections having the same destination.
     *
     * @param id of destination node
     * @return set as described above
     */
    Set<ConnectionContainer> getConnectionsByDest(final NodeID id) {
        Set<ConnectionContainer> scc = m_connectionsByDest.get(id);
        return scc == null ? null : Collections.unmodifiableSet(scc);
    }

    /** Return all connections having the same source.
     *
     * @param id of source node
     * @return set as described above
     */
    Set<ConnectionContainer> getConnectionsBySource(final NodeID id) {
        Set<ConnectionContainer> scc = m_connectionsBySource.get(id);
        return scc == null ? null : Collections.unmodifiableSet(scc);
    }

    /**
     * @return a collection of sets of ConnectionContainers, grouped by
     *   source node ID.
     */
    Collection<Set<ConnectionContainer>> getConnectionsBySourceValues() {
        Collection<Set<ConnectionContainer>> cscc = m_connectionsBySource.values();
        return cscc == null ? null : Collections.unmodifiableCollection(cscc);
    }

    /** Remove a connection.
     *
     * @param cc the connection to be removed.
     * @throws IllegalArgumentException if connection does not exist.
     */
    void removeConnection(final ConnectionContainer cc) throws IllegalArgumentException {
        clearGraphAnnotationCache();
        // 1) try to delete it from set of outgoing connections
        if (!m_connectionsBySource.get(cc.getSource()).remove(cc)) {
            throw new IllegalArgumentException("Connection does not exist!");
        }
        // 2) remove connection from set of ingoing connections
        if (!m_connectionsByDest.get(cc.getDest()).remove(cc)) {
            throw new IllegalArgumentException("Connection did not exist (it did exist as outcoming conn.)!");
        }
    }

    /** Add a connection.
    *
    * @param cc the connection to be added.
    * @throws IllegalArgumentException if connection cannot be added.
    */
    void addConnection(final ConnectionContainer cc) throws IllegalArgumentException {
        clearGraphAnnotationCache();
        // 1) try to insert it into set of outgoing connections
        if (!m_connectionsBySource.get(cc.getSource()).add(cc)) {
            throw new IllegalArgumentException("Connection already exists!");
        }
        // 2) insert connection into set of ingoing connections
        if (!m_connectionsByDest.get(cc.getDest()).add(cc)) {
            throw new IllegalArgumentException("Connection already exists (oddly enough only as incoming)!");
        }
    }

    /**
     * @see Workflow#getBreadthFirstListOfNodeAndSuccessors(NodeID, boolean, boolean)
     */
    LinkedHashMap<NodeID, Set<Integer>> getBreadthFirstListOfNodeAndSuccessors(final NodeID id, final boolean skipWFM) {
        return getBreadthFirstListOfNodeAndSuccessors(id, skipWFM, false);
    }

    /**
     * Return map of node ids connected to the given node sorted in breadth first order mapped to a set of portIDs. Note
     * that also nodes which have another predecessors not contained in this list may be included as long as at least
     * one input node is connected to a node in this list! The set of integers represents the indices of input ports
     * which are actually used within the graph covered in the result list.
     *
     * @param id of node
     * @param skipWFM if true, do not include WFM in the list
     * @param handleMetanodeAsSingleNode Whether to traverse a metanode as if it was a single node. If true, nodes
     *            connected to <i>any</i> metanode output port will be considered successors. If false, only nodes which
     *            are connected to the respective input are considered successors (as if the metanode was expanded).
     * @return map as described above.
     */
    LinkedHashMap<NodeID, Set<Integer>> getBreadthFirstListOfNodeAndSuccessors(
        final NodeID id, final boolean skipWFM, final boolean handleMetanodeAsSingleNode) {
        // assemble an unsorted list of successors
        HashSet<NodeID> inclusionList = new HashSet<NodeID>();
        getDepthFirstListOfNodeAndSuccessors(inclusionList, id, -1, nc -> false, false, handleMetanodeAsSingleNode);
        // and then get all successors that are part of this list in a nice BFS order
        LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes = new LinkedHashMap<NodeID, Set<Integer>>();
        // put the origin - note that none of its ports (if any) are of interest - into the map
        bfsSortedNodes.put(id, new HashSet<Integer>());
        expandListBreadthFirst(bfsSortedNodes, inclusionList, handleMetanodeAsSingleNode);
        // if wanted (and contained): remove WFM itself
        if (skipWFM && bfsSortedNodes.keySet().contains(this.getID())) {
            bfsSortedNodes.remove(this.getID());
        }
        return bfsSortedNodes;
    }

    /** Return map of node ids connected to the given outport of the given node
     * sorted in breadth first order mapped to a set of portIDs.
     * See also {@link #getBreadthFirstListOfNodeAndSuccessors(NodeID, boolean)}
     *
     * @param id of node
     * @param outPortIndices of node (empty set for all ports)
     * @param skipWFM if true, do not include WFM in the list
     * @return map as described above.
     */
    LinkedHashMap<NodeID, Set<Integer>> getBreadthFirstListOfPortSuccessors(
            final NodeID id, final Set<Integer> outPortIndices,
            final boolean skipWFM) {
        // assemble unsorted list of successors
        HashSet<NodeID> inclusionList = new HashSet<NodeID>();
        completeSetFromOutPort(inclusionList, id, outPortIndices);
        // add starting node as well (since it will be predecessor
        // for the following BFS!
        inclusionList.add(id);
        // now put all nodes which are part of this list in a nice BFS order
        LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes
                    = new LinkedHashMap<NodeID, Set<Integer>>();
        // put the successors of the origin into the map (together with the
        // connected inports) as "source" of this search:
        for (ConnectionContainer cc: getConnectionsBySource(id)) {
            if (outPortIndices.isEmpty() || outPortIndices.contains(cc.getSourcePort())) {
                bfsSortedNodes.put(cc.getDest(), new HashSet<Integer>(cc.getDestPort()));
            }
        }
        // and expand+sort the list, using the inclusion list as basis:
        expandListBreadthFirst(bfsSortedNodes, inclusionList);
        // if wanted (and contained): remove WFM itself
        if (skipWFM && bfsSortedNodes.keySet().contains(this.getID())) {
            bfsSortedNodes.remove(this.getID());
        }
        // and finally remove start node from result list:
        bfsSortedNodes.remove(id);
        return bfsSortedNodes;
    }

    /**
     * @param includeNodes the nodes that induce the subgraph - all connections between these nodes will be included in
     *            the result.
     * @param includeDanglingConnections if true, also include connections where only one endpoint is in the included
     *            node set
     * @return connections on the selected nodes. Useful for copying a part of the workflow.
     */
    Set<ConnectionContainer> getInducedConnections(final Set<NodeID> includeNodes, final boolean includeDanglingConnections) {
        HashSet<ConnectionContainer> inducedConnections = new HashSet<>();

        for (NodeID nodeID : includeNodes) {
            // throws exception if not present in workflow
            m_wfm.getNodeContainer(nodeID);

            // outgoing connections
            for (ConnectionContainer out : getConnectionsBySource(nodeID)) {
                // induced <=> both nodes are in the include set
                final boolean induced = includeNodes.contains(out.getDest());
                if (induced || includeDanglingConnections) {
                    inducedConnections.add(out);
                }
            }

            // incoming connections - only for dangling incoming connections
            if (includeDanglingConnections) {
                for (ConnectionContainer in : getConnectionsByDest(nodeID)) {
                    // will add induced connections twice, but the set filters them out
                    inducedConnections.add(in);
                }
            }
        }
        return inducedConnections;
    }

    /** Return map of node ids to set of port indices based on argument list
     * of node ids. The map is sorted by traversing the graph breadth first,
     * the set of port indices represents the input ports actually used
     * within the graph covered.
     *
     * @param ids of interest, for example m_workflow.m_nodes.keySet()
     * @param skipWFM if true, do not include WFM in the list
     * @return BF sorted list of node ids
     */
    LinkedHashMap<NodeID, Set<Integer>> createBreadthFirstSortedList(
            final Set<NodeID> ids,
            final boolean skipWFM) {
        // first create list of nodes without predecessor or only the WFM
        // itself (i.e. connected to outside "world" only.
        Set<NodeID> sources = getSourceNodes(ids);
        LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes
                        = new LinkedHashMap<NodeID, Set<Integer>>();
        for (NodeID thisNode : sources) {
            // put this source - note that none of it's ports (if any
            // exist) are of interest
            bfsSortedNodes.put(thisNode, new HashSet<Integer>());
        }
        // and finally complete this list by adding all successors...
        expandListBreadthFirst(bfsSortedNodes, ids);
        // if wanted (and contained): remove WFM itself
        if (skipWFM && bfsSortedNodes.containsKey(this.getID())) {
            bfsSortedNodes.remove(this.getID());
        }
        return bfsSortedNodes;
    }

    /**
     * Complete set of nodes depth-first starting with node id. If the given node is already in the set, nothing
     * happens. Note that this function does not pursue connections leaving this workflow - we will only add our own ID
     * (the workflow) as "last" node in the chain.
     *
     * @param nodes set of nodes to be completed
     * @param id of node to start search from
     * @param incomingPortIndex index of port the incoming connection connected to (useful when start node is a
     *            metanode!)
     * @param stopCondition if the condition evaluates to <code>true</code> for a given node, the node will be added but
     *            none it's successors, never <code>null</code>
     * @param includePerfectSuccessorsOnly a perfect successor is a node whose predecessors are all part of provided
     *            node set. If this parameter is <code>true</code>, only perfect successors will be included. If
     *            <code>false</code> nodes with at least one predecessor in the node set will be included.
     * @param handleMetaNodeAsSingleNode Whether to traverse a metanode as if it was a single node. If true, nodes
     *            connected to <i>any</i> metanode output port will be considered successors. If false, only nodes which
     *            are connected to the respective input are considered successors (as if the metanode was expanded).
     */
    void getDepthFirstListOfNodeAndSuccessors(final Set<NodeID> nodes, final NodeID id, final int incomingPortIndex,
        final Predicate<NodeContainer> stopCondition, final boolean includePerfectSuccessorsOnly,
        final boolean handleMetaNodeAsSingleNode) {
        if (nodes.add(id)) { // only if id was not already contained in set!
            NodeContainer thisNode = m_nodes.get(id);
            if (stopCondition.test(thisNode)) {
                //stop adding the successors at this point
                return;
            }
            for (ConnectionContainer cc : m_connectionsBySource.get(id)) {
                NodeID nextNodeID = cc.getDest();
                if (!nextNodeID.equals(getID())) {
                    if (includePerfectSuccessorsOnly) {
                        // check whether all predecessor nodes are in the current list of nodes
                        // if not, skip the node
                        if (!m_connectionsByDest.get(nextNodeID).stream()
                            .allMatch(predCC -> nodes.contains(predCC.getSource()))) {
                            continue;
                        }
                    }
                    boolean addNextNode;
                    // avoid to follow any connections leaving the workflow!
                    if (thisNode instanceof WorkflowManager && !handleMetaNodeAsSingleNode) {
                        WorkflowManager wfm = (WorkflowManager)thisNode;
                        // not so easy - we need to find out who is connected
                        // through this WFM (if we have a port index, of course)
                        if (incomingPortIndex < 0) {
                            // TODO check for unconnected metaoutports?
                            addNextNode = true;
                        } else {
                            Set<Integer> outports = wfm.getWorkflow().connectedOutPorts(incomingPortIndex);
                            if (outports.contains(cc.getSourcePort())) {
                                addNextNode = true;
                            } else {
                                addNextNode = false;
                            }
                        }
                    } else {
                        addNextNode = true;
                    }
                    if (addNextNode) {
                        getDepthFirstListOfNodeAndSuccessors(nodes, nextNodeID, cc.getDestPort(), stopCondition,
                            includePerfectSuccessorsOnly, handleMetaNodeAsSingleNode);
                    }
                } else {
                    // make sure the WFM itself is in the list (if reached)
                    nodes.add(nextNodeID);
                }
            }
        }
    }

    /** Complete set of nodes depth-first starting with all nodes connected
     * to the outport of the given node. See {@link #completeSet()}.
     *
     * @param nodes set of nodes to be completed
     * @param id of node to start search from
     * @param outPortIndices of outports
     */
    private void completeSetFromOutPort(final HashSet<NodeID> nodes,
            final NodeID id, final Set<Integer> outPortIndices) {
        for (ConnectionContainer cc : m_connectionsBySource.get(id)) {
            NodeID nextNodeID = cc.getDest();
            if (!nextNodeID.equals(getID())
                    && outPortIndices.contains(cc.getSourcePort())) {
                // avoid to follow any connections leaving the workflow!
                getDepthFirstListOfNodeAndSuccessors(nodes, nextNodeID, cc.getDestPort(), nc -> false, false, false);
            }
        }
    }

    private void expandListBreadthFirst(final LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes,
        final Set<NodeID> inclusionList) {
        expandListBreadthFirst(bfsSortedNodes, inclusionList, false);
    }

    /**
     * Expand a given list of nodes to include all successors that are connected to any of the nodes in a breadth-first
     * manner. Don't include any of the nodes not contained in the "inclusion" list if given.
     *
     * @param bfsSortedNodes existing, already sorted list of nodes
     * @param inclusionList complete list of nodes to be sorted breadth first
     * @param handleMetanodeAsSingleNode Whether to traverse a metanode as if it was a single node. If true, nodes
     *            connected to <i>any</i> metanode output port will be considered successors. If false, only nodes which
     *            are connected to the respective input are considered successors (as if the metanode was expanded).
     */
    private void expandListBreadthFirst(
            final LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes,
        final Set<NodeID> inclusionList, final boolean handleMetanodeAsSingleNode) {
        // don't add parent to list throughout search to avoid infinite loops
        Set<Integer> parentOutgoingPorts = new HashSet<>();
        // keep adding nodes until we can't find new ones anymore
        for (int i = 0; i < bfsSortedNodes.size(); i++) {
            Object[] ani = bfsSortedNodes.keySet().toArray();
            NodeID node = (NodeID)(ani[i]);
            Set<Integer> inPorts = bfsSortedNodes.get(node);
            // Output ports to traverse to reach successors. If empty, consider all output ports.
            Set<Integer> outPortsToTraverse = new HashSet<Integer>();
            // Determine output ports to traverse for the case of metanodes.
            NodeContainer currNC = getNode(node);
            if (!handleMetanodeAsSingleNode && (currNC != null) && (currNC instanceof WorkflowManager)) {
                for (int in : inPorts) {
                     Set<Integer> connectedMetanodeOutports = ((WorkflowManager)currNC).getWorkflow().connectedOutPorts(in);
                     outPortsToTraverse.addAll(connectedMetanodeOutports);
                }
            }
            // look at all successors of this node
            m_connectionsBySource.get(node).stream().filter(connection -> {
                // If the collection is empty, consider all outgoing connections, else only those adjacent to the
                // specified output ports.
                return outPortsToTraverse.isEmpty() || outPortsToTraverse.contains(connection.getSourcePort());
            }).forEach(connection -> {
                NodeID successor = connection.getDest();
                if (this.getID().equals(successor)) {
                    parentOutgoingPorts.add(connection.getDestPort());
                } else {
                    // don't check nodes which are already in the list...
                    if (!bfsSortedNodes.containsKey(successor)) {
                        // and make sure all predecessors that are part of the inclusion list of this successor are
                        // already in the list
                        boolean allContained = true;
                        Set<Integer> incomingPorts = new HashSet<Integer>();
                        for (ConnectionContainer connectionToSuccessor : m_connectionsByDest.get(successor)) {
                            NodeID predecessor = connectionToSuccessor.getSource();
                            if (!predecessor.equals(getID())) {
                                // its not a WFMIN connection...
                                if (!bfsSortedNodes.containsKey(predecessor)) {
                                    // ...and its not already in the list...
                                    if (inclusionList.contains(predecessor)) {
                                        // ...but if it is in the inclusion list then do not (yet!) include it!
                                        allContained = false;
                                    }
                                } else {
                                    // not WFMIN but source is in our list: needs to be remembered as "incoming" port
                                    // within this BF search.
                                    incomingPorts.add(connectionToSuccessor.getDestPort());
                                }
                            }
                        }
                        if (allContained) {
                            // if all predecessors are already in the BFS list (or not to be considered): add it!
                            bfsSortedNodes.put(successor, incomingPorts);
                        }
                    }
                }
            });
        }
        // add parents if any connections were found
        if (!parentOutgoingPorts.isEmpty()) {
            bfsSortedNodes.put(this.getID(), parentOutgoingPorts);
        }
    }

    /** Return set of nodes without any predecessors (sources) in the
     * set of given nodes.
     *
     * @param ids the nodes to check
     * @return set of nodes without predecessors
     */
    private LinkedHashSet<NodeID> getSourceNodes(final Set<NodeID> ids) {
        LinkedHashSet<NodeID> result = new LinkedHashSet<NodeID>();
        for (NodeID thisNode : ids) {
            // find the nodes in the list which are sources (i.e. not
            // preceded by any others in the list)
            Set<ConnectionContainer> incomingConns = m_connectionsByDest.get(thisNode);
            boolean isSource = true;
            for (ConnectionContainer thisConn : incomingConns) {
                if (ids.contains(thisConn.getSource())) {
                    isSource = false;
                }
            }
            if (isSource) {
                result.add(thisNode);
            }
        }
        return result;
    }

    /** Return list of nodes, which are either source nodes (no inputs) in this workflow or which are connected to
     * a workflow input port.
     *
     * @return set of directly connected nodes or source nodes.
     */
    HashMap<NodeID, Integer> getStartNodes(/*final int inPort*/) {
        LinkedHashMap<NodeID, Integer> result = new LinkedHashMap<NodeID, Integer>();
//        if (inPort < 0) {
        for (NodeID id : m_nodes.keySet()) {
            Set<ConnectionContainer> ccByDest = getConnectionsByDest(id);
            // either we have no incoming connections: it is a source!
            boolean isSource = (ccByDest.size() == 0);
            // or we find at least one port that is connected to an inport of the WFM
            for (ConnectionContainer cc : ccByDest) {
                if (cc.getSource().equals(this.getID())) {
                    // node has incoming connection from metanode inport
                    isSource = true;
                }
            }
            if (isSource) {
                result.put(id, -1);
            }
        }
//        } else {
//            // only find nodes that are connected to this port (ignores true source nodes!)
//            for (ConnectionContainer cc : m_connectionsBySource.get(getID())) {
//                if (cc.getSourcePort() == inPort) {
//                    result.put(cc.getDest(), cc.getDestPort());
//                }
//            }
//        }
        return result;
    }

    /** Determine outports which are connected (directly or indirectly) to
     * the given inport in this workflow.
     *
     * @param inPortIx index of inport
     * @return set of outport indices
     */
    Set<Integer> connectedOutPorts(final int inPortIx) {
        if (m_nodeAnnotationCache == null) {
            updateGraphAnnotationCache();
        }
        HashSet<Integer> outSet = new HashSet<Integer>();
        for (ConnectionContainer cc : m_connectionsBySource.get(this.getID())) {
            if (cc.getSourcePort() == inPortIx) {
                if (cc.getDest().equals(this.getID())) {
                    assert ConnectionContainer.ConnectionType.WFMTHROUGH.equals(cc.getType());
                    outSet.add(cc.getDestPort());
                } else {
                    for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
                        if (nga.getID().equals(cc.getDest())) {
                            if (nga.getOutportIndex() == -1) {
                                // the simple one, just add all metanode outports this node connects to:
                                outSet.addAll(nga.getConnectedOutportIndices());
                            } else {
                                // more complex, a metanode. We need to first figure out which ports we
                                // are connected to before potentially adding the outport lists.
                                NodeContainer nc = getNode(nga.getID());
                                assert nc instanceof WorkflowManager;
                                Set<Integer> outPorts
                                            = ((WorkflowManager)nc).getWorkflow().connectedOutPorts(cc.getDestPort());
                                if (outPorts.contains(nga.getOutportIndex())) {
                                    outSet.addAll(nga.getConnectedOutportIndices());
                                }
                            }
                        }
                    }
                }
            }
        }
        return outSet;
    }

    /** Determine outports which are connected (directly or indirectly) to
     * the given inport in this workflow.
     *
     * @param inPortIx index of inport
     * @return set of outport indices
     */
    Set<Integer> connectedOutPortsOLD(final int inPortIx) {
        HashSet<Integer> outSet = new HashSet<Integer>();
        // Map to remember connected nodes with the index of the corresponding input port
        LinkedHashMap<NodeID, Integer> nodesToCheck = new LinkedHashMap<NodeID, Integer>();
        // find everything that is connected to an input port of this workflow
        // with an index contained in the set:
        for (ConnectionContainer cc : m_connectionsBySource.get(getID())) {
            if (inPortIx == cc.getSourcePort()) {
                NodeID nextID = cc.getDest();
                if (nextID.equals(this.getID())) {
                    assert cc.getType().equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
                    outSet.add(cc.getDestPort());
                } else {
                    nodesToCheck.put(nextID, cc.getDestPort());
                }
            }
        }
        // now follow those nodes and see if we reach a workflow outport
        int currentNode = 0;
        while (currentNode < nodesToCheck.size()) {
            // Not a very nice way to iterate over this set but since we are adding things to it inside the loop?!
            Object[] ani = nodesToCheck.keySet().toArray();
            NodeID thisID = (NodeID)(ani[currentNode]);
            assert !(thisID.equals(this.getID()));
            NodeContainer thisNode = m_nodes.get(thisID);
            if (thisNode instanceof SingleNodeContainer) {
                // simply add everything that is connected to this node
                for (ConnectionContainer cc : m_connectionsBySource.get(thisID)) {
                    NodeID nextID = cc.getDest();
                    if (nextID.equals(this.getID())) {
                        outSet.add(cc.getDestPort());
                    } else {
                        nodesToCheck.put(nextID, cc.getDestPort());
                    }
                }
            } else {
                // only add nodes that are connected to ports connected to the
                // inport we are currently considering (recurse...)
                assert thisNode instanceof WorkflowManager;
                int portToCheck = nodesToCheck.get(thisID);
                Set<Integer> connectedOutPorts = ((WorkflowManager)thisNode).getWorkflow().
                                                        connectedOutPorts(portToCheck);
                for (ConnectionContainer cc : m_connectionsBySource.get(thisID)) {
                    if (connectedOutPorts.contains(cc.getSourcePort())) {
                        NodeID nextID = cc.getDest();
                        if (nextID.equals(this.getID())) {
                            outSet.add(cc.getDestPort());
                        } else {
                            nodesToCheck.put(nextID, cc.getDestPort());
                        }
                    }
                }
            }
            currentNode++;
        }
        // done - return set of connected outports (well: their indices)
        return outSet;
    }

    /** Determine all nodes which are connected (directly or indirectly) to
     * the given inports in this workflow. The list is sorted according to
     * "longest path layering" making sure that nodes are always added behind
     * all of their predecessors.
     *
     * @param inPorts indices of inports
     * @return set of nodes with used inports
     */
    ArrayList<NodeAndInports> findAllConnectedNodes(
            final Set<Integer> inPorts) {
        return findAllNodesInbetween(this.getID(), inPorts, this.getID());
    }

    /** Determine all nodes which are connected (directly or indirectly) to
     * the given inports of the given start node and the given end node in
     * this workflow. Both start and end node can also be the workflowmanager
     * itself.
     * The list is sorted according to "longest path layering" making sure
     * that nodes are always added behind all of their predecessors.
     *
     * @param startID id of first node (or id of WFM)
     * @param startPorts indices of inports (use all ports if null)
     * @param endID id of last node (or id of WFM)
     * @return set of nodes with used inports
     */
    private ArrayList<NodeAndInports> findAllNodesInbetween(
            final NodeID startID, final Set<Integer> startPorts,
            final NodeID endID) {
        // prepare the result list
        ArrayList<NodeAndInports> tempOutput = new ArrayList<NodeAndInports>();
        // find everything that is connected to an output port of the
        // "startNode" (which can be the WFM itself or a LoopStartNode or
        // any other "start" node) with a port index contained in the set
        for (ConnectionContainer cc : m_connectionsBySource.get(startID)) {
            if ((startPorts == null) || (startPorts.contains(cc.getSourcePort()))) {
                NodeID nextID = cc.getDest();
                if (nextID.equals(endID)) {
                    // don't add the end node!
                } else if (nextID.equals(this.getID())) {
                    // don't record outgoing connections
                    if (startID.equals(this.getID())) {
                        assert cc.getType().equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
                    } else {
                        assert cc.getType().equals(ConnectionContainer.ConnectionType.WFMOUT);
                    }
                } else {
                    int ix = 0;
                    for (ix = 0; ix < tempOutput.size(); ix++) {
                        if (tempOutput.get(ix).m_nodeId.equals(cc.getDest())) {
                            break;
                        }
                    }
                    if (ix >= tempOutput.size()) {
                        // ...it's a node not yet in our list: add it
                        tempOutput.add(new NodeAndInports(cc.getDest(), cc.getDestPort(), /*depth=*/0));
                    } else {
                        // node is already in list. Add port if not already contained:
                        NodeAndInports nai = tempOutput.get(ix);
                        if (!nai.getInports().contains(cc.getDestPort())) {
                            nai.addInport(cc.getDestPort());
                        } else {
                            // ignore entries that we already have (parallel branches can cause this)
                        }
                    }
                }
            }
        }
        // now follow those nodes and keep adding until we reach the end of
        // the workflow or the dedicated end node.
        int currIndex = 0;
        while (currIndex < tempOutput.size()) {
            NodeID currID = tempOutput.get(currIndex).getID();
            NodeContainer currNode = m_nodes.get(currID);
            assert currNode != null;  // WFM will never be added!
            Set<Integer> currInports = tempOutput.get(currIndex).getInports();
            int currDepth = tempOutput.get(currIndex).getDepth();
            Set<Integer> currOutports = new HashSet<Integer>();
            if ((currNode instanceof SingleNodeContainer) || (currInports == null)) {
                // simple: all outports are affected (SNC or WFM without listed inports)
                for (int i = 0; i < currNode.getNrOutPorts(); i++) {
                    currOutports.add(i);
                }
            } else {
                assert currNode instanceof WorkflowManager;
                // less simple: we need to determine which outports are
                // connected to the listed inports:
                for (Integer inPortIx : currInports) {
                    Workflow currWorkflow = ((WorkflowManager)currNode).getWorkflow();
                    Set<Integer> connectedOutports = currWorkflow.connectedOutPorts(inPortIx);
                    currOutports.addAll(connectedOutports);
                }
            }
            for (ConnectionContainer cc : this.getConnectionsBySource(currID)) {
                if (currOutports.contains(cc.getSourcePort())) {
                    // only if one of the affected outports is connected:
                    NodeID destID = cc.getDest();
                    if ((!destID.equals(this.getID())) && (!destID.equals(endID))) {
                        // only if we have not yet reached an outport or the "end" node
                        // try to find node in existing list:
                        int ix = 0;
                        for (ix = 0; ix < tempOutput.size(); ix++) {
                            if (tempOutput.get(ix).m_nodeId.equals(destID)) {
                                break;
                            }
                        }
                        if (ix >= tempOutput.size()) {
                            // ...it's a node not yet in our list: add it
                            tempOutput.add(new NodeAndInports(destID, cc.getDestPort(), currDepth + 1));
                        } else {
                            assert ix != currIndex;
                            // node is already in list, adjust depth to new
                            // maximum and add port if not already contained:
                            NodeAndInports nai = tempOutput.get(ix);
                            if (!nai.getInports().contains(cc.getDestPort())) {
                                nai.addInport(cc.getDestPort());
                            } else {
                                // ignore entries that we already have (parallel branches can cause this)
                            }
                            if (nai.getDepth() <= currDepth) {
                                // fix depth if smaller or equal
                                nai.setDepth(currDepth + 1);
                                if (ix < currIndex) {
                                    // move this node to end of list if it was
                                    // already "touched" so that depth of
                                    // successors will also be adjusted!
                                    nai = tempOutput.remove(ix);
                                    tempOutput.add(nai);
                                    // critical: we removed an element in our
                                    // list which resided before our pointer.
                                    // Make sure we still point to current node.
                                    currIndex--;
                                }
                            } else {
                                // don't fix, depth is already larger. Node was seen previously
                            }
                        }
                    }
                }
            }
            currIndex++;
        }
        // make sure nodes are sorted by their final depth!
        Collections.sort(tempOutput);
        // done - return set of nodes and ports
        return tempOutput;
    }

    /** Determine inports which are connected (directly or indirectly) to
     * the given outport in this workflow.
     *
     * @param outPortIx index of outport
     * @return set of inport indices
     */
    Set<Integer> connectedInPorts(final int outPortIx) {
        if (m_nodeAnnotationCache == null) {
            updateGraphAnnotationCache();
        }
        HashSet<Integer> inSet = new HashSet<Integer>();
        for (ConnectionContainer cc : m_connectionsByDest.get(this.getID())) {
            if (cc.getDestPort() == outPortIx) {
                if (cc.getSource().equals(this.getID())) {
                    assert ConnectionContainer.ConnectionType.WFMTHROUGH.equals(cc.getType());
                    inSet.add(cc.getSourcePort());
                } else {
                    for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
                        if (nga.getID().equals(cc.getSource())) {
                            if ((nga.getOutportIndex() == -1) || (nga.getOutportIndex() == cc.getSourcePort())) {
                                inSet.addAll(nga.getConnectedInportIndices());
                            }
                        }
                    }
                }
            }
        }
        return inSet;
    }

    /** Determine inports which are connected (directly or indirectly) to
     * the given outport in this workflow.
     *
     * @param outPortIx index of outport
     * @return set of inport indices
     */
    Set<Integer> connectedInPortsOLD(final int outPortIx) {
        HashSet<Integer> inSet = new HashSet<Integer>();
        // Map to remember connected nodes with the index of the corresponding
        // output port
        TreeMap<NodeID, Integer> nodesToCheck = new TreeMap<NodeID, Integer>();
        // find everything that is connected to an output port of this workflow
        // with an index contained in the set:
        for (ConnectionContainer cc : m_connectionsByDest.get(getID())) {
            if (outPortIx == cc.getDestPort()) {
                NodeID prevID = cc.getSource();
                if (prevID.equals(this.getID())) {
                    assert cc.getType().equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
                    inSet.add(cc.getSourcePort());
                } else {
                    nodesToCheck.put(prevID, cc.getSourcePort());
                }
            }
        }
        // now follow those nodes (backwards!) and see if we reach a workflow
        // inport
        int currentNode = 0;
        while (currentNode < nodesToCheck.size()) {
            // Not a very nice way to iterate over this set but
            // since we are adding things to it inside the loop?!
            Object[] ani = nodesToCheck.keySet().toArray();
            NodeID thisID = (NodeID)(ani[currentNode]);
            assert !(thisID.equals(this.getID()));
            NodeContainer thisNode = m_nodes.get(thisID);
            if (thisNode instanceof SingleNodeContainer) {
                // simply add everything that is connected to this node
                for (ConnectionContainer cc : m_connectionsByDest.get(thisID)) {
                    NodeID prevID = cc.getSource();
                    if (prevID.equals(this.getID())) {
                        inSet.add(cc.getDestPort());
                    } else {
                        nodesToCheck.put(prevID, cc.getSourcePort());
                    }
                }
            } else {
                // only add nodes that are connected to ports connected to the
                // outports we are currently considering (recurse...)
                assert thisNode instanceof WorkflowManager;
                int portToCheck = nodesToCheck.get(thisID);
                Set<Integer> connectedInPorts =
                     ((WorkflowManager)thisNode).getWorkflow().connectedInPorts(portToCheck);
                for (ConnectionContainer cc : m_connectionsByDest.get(thisID)) {
                    if (connectedInPorts.contains(cc.getDestPort())) {
                        NodeID prevID = cc.getSource();
                        if (prevID.equals(this.getID())) {
                            inSet.add(cc.getDestPort());
                        } else {
                            nodesToCheck.put(prevID, cc.getSourcePort());
                        }
                    }
                }
            }
            currentNode++;
        }
        // done - return set of connected inports (well: their indices)
        return inSet;
    }

    /** Complete set of nodes backwards depth-first starting with node id.
     * Note that this function does not pursue connections leaving this
     * workflow. We will only add our own ID (the workflow) in that case.
     *
     * @param nodes set of nodes to be completed
     * @param id of node to start search from
     * @param index of port the outgoing connection connected to
     */
    private void completeSetBackwards(final HashSet<NodeID> nodes, final NodeID id, final int outgoingPortIndex) {
        NodeContainer thisNode = m_nodes.get(id);
        for (ConnectionContainer cc : m_connectionsByDest.get(id)) {
            NodeID prevNodeID = cc.getSource();
            if (!prevNodeID.equals(getID())) {
                // avoid to follow any connections leaving the workflow!
                if (thisNode instanceof SingleNodeContainer) {
                    // easy - just add normal nodes
                    getDepthFirstListOfNodeAndSuccessors(nodes, prevNodeID, cc.getSourcePort(), nc -> false, false,
                        false);
                } else {
                    assert thisNode instanceof WorkflowManager;
                    WorkflowManager wfm = (WorkflowManager)thisNode;
                    // not so easy - we need to find out who is connected
                    // through this WFM (if we have a port index, of course)
                    if (outgoingPortIndex < 0) {
                        // TODO check for unconnected metaoutports?
                        completeSetBackwards(nodes, prevNodeID, cc.getSourcePort());
                    } else {
                        // find out which inports are connected through this
                        // WFM to the given outport
                        Set<Integer> inports = wfm.getWorkflow().connectedInPorts(cc.getSourcePort());
                        // and only add the predeccessor if he is connected
                        // to one of those
                        if (inports.contains(cc.getDestPort())) {
                            completeSetBackwards(nodes, prevNodeID, cc.getSourcePort());
                        }
                    }
                }
            } else {
                // make sure the WFM itself is in the list (if reached)
                nodes.add(prevNodeID);
            }
        }
    }

    /** Create (fairly unordered) set of predecessors of the given node. The list stops at the
     * parent WFM itself (it's ID is included if any connection from the give node does reach
     * it, though). The node itself is not included in this list!
     *
     * @param id ...
     * @return set of predecessors.
     */
    public Set<NodeID> getPredecessors(final NodeID id) {
        final HashSet<NodeID> result = new HashSet<NodeID>();
        completePredecessorSet(result, id, -1);
        result.remove(id);
        return result;
    }

    /** Complete set of nodes backwards depth-first starting with node id.
     * Note that this function does not pursue connections leaving this
     * workflow. We will only add our own ID (the workflow) in that case.
     *
     * @param nodes set of nodes to be completed
     * @param id of node to start search from
     * @param outgoingPortIndex port the outgoing connection connected to
     */
    private void completePredecessorSet(final HashSet<NodeID> nodes, final NodeID id, final int outgoingPortIndex) {
        if (nodes.add(id)) {  // only if the node is not already contained:
            if (id.equals(this.getID())) {
                // don't continue outside of this WFM.
                return;
            }
            NodeContainer thisNode = m_nodes.get(id);
            for (ConnectionContainer cc : m_connectionsByDest.get(id)) {
                NodeID prevNodeID = cc.getSource();
                if (!prevNodeID.equals(getID())) {
                    // avoid to follow any connections leaving the workflow!
                    if (thisNode instanceof SingleNodeContainer) {
                        // easy - just continue with predecessors of normal nodes
                        completePredecessorSet(nodes, prevNodeID, cc.getSourcePort());
                    } else {
                        assert thisNode instanceof WorkflowManager;
                        WorkflowManager wfm = (WorkflowManager)thisNode;
                        // not so easy - we need to find out who is connected
                        // through this WFM (if we have a port index, of course)
                        if (outgoingPortIndex < 0) {
                            // we ignore the connection structure of the metanode inside since we
                            // do not seem to care about it's outports (likely this was the starting point):
                            completePredecessorSet(nodes, prevNodeID, cc.getSourcePort());
                        } else {
                            // find out which inports are connected through this
                            // WFM to the given outport
                            Set<Integer> inports = wfm.getWorkflow().connectedInPorts(outgoingPortIndex);
                            // and only add the predeccessor if he is connected
                            // to one of those
                            if (inports.contains(cc.getDestPort())) {
                                completePredecessorSet(nodes, prevNodeID, cc.getSourcePort());
                            }
                        }
                    }
                } else {
                    // add the WFM itself if reached but don't continue the search:
                    nodes.add(prevNodeID);
                }
            }
        }
    }

    /** Implementation of {@link WorkflowManager#getMetanodeInputPortInfo(NodeID)}.
     * @param metaNodeID ...
     * @return ...
     */
    MetaPortInfo[] getMetanodeInputPortInfo(final NodeID metaNodeID) {
        WorkflowManager wfm = (WorkflowManager)m_nodes.get(metaNodeID);
        Workflow wfmFlow = wfm.getWorkflow();

        MetaPortInfo[] result = new MetaPortInfo[wfm.getNrInPorts()];
        for (int i = 0; i < result.length; i++) {
            int insideCount = 0;
            for (ConnectionContainer cc : wfmFlow.getConnectionsBySource(
                    metaNodeID)) {
                if (cc.getSourcePort() == i) {
                    // could also be a through connection
                    insideCount += 1;
                }
            }
            boolean hasOutsideConnection = false;
            for (ConnectionContainer outCC : getConnectionsByDest(metaNodeID)) {
                if (outCC.getDestPort() == i) {
                    hasOutsideConnection = true;
                    break;
                }
            }
            String message;
            boolean isConnected;
            PortType portType = wfm.getInPort(i).getPortType();
            if (hasOutsideConnection || insideCount > 0) {
                isConnected = true;
                if (hasOutsideConnection && insideCount > 0) {
                    message = "Connected to one upstream node and "
                        + insideCount + " downstream node(s)";
                } else if (hasOutsideConnection) {
                    message = "Connected to one upstream node";
                } else {
                    message = "Connected to " + insideCount + " downstream node(s)";
                }
            } else {
                isConnected = false;
                message = null;
            }
            result[i] = MetaPortInfo.builder()
                    .setPortType(portType)
                    .setIsConnected(isConnected)
                    .setMessage(message)
                    .setOldIndex(i).build();
        }
        return result;
    }

    /** Implementation of {@link WorkflowManager#getMetanodeOutputPortInfo(NodeID)}.
     * @param metaNodeID ...
     * @return ...
     */
    MetaPortInfo[] getMetanodeOutputPortInfo(final NodeID metaNodeID) {
        WorkflowManager wfm = (WorkflowManager)m_nodes.get(metaNodeID);
        Workflow wfmFlow = wfm.getWorkflow();

        MetaPortInfo[] result = new MetaPortInfo[wfm.getNrOutPorts()];
        for (int i = 0; i < result.length; i++) {
            boolean hasInsideConnection = false;
            for (ConnectionContainer cc : wfmFlow.getConnectionsByDest(metaNodeID)) {
                if (cc.getDestPort() == i) {
                    hasInsideConnection = true;
                    break;
                }
            }
            int outsideCount = 0;
            for (ConnectionContainer outCC : getConnectionsBySource(metaNodeID)) {
                if (outCC.getSourcePort() == i) {
                    outsideCount += 1;
                }
            }
            String message;
            boolean isConnected;
            PortType portType = wfm.getOutPort(i).getPortType();
            if (hasInsideConnection || outsideCount > 0) {
                isConnected = true;
                if (hasInsideConnection && outsideCount > 0) {
                    message = "Connected to one upstream node and " + outsideCount + " downstream node(s)";
                } else if (hasInsideConnection) {
                    // could also be a through conn but we ignore here
                    message = "Connected to one upstream node";
                } else {
                    message = "Connected to " + outsideCount + " downstream node(s)";
                }
            } else {
                isConnected = false;
                message = null;
            }
            result[i] = MetaPortInfo.builder()
                .setPortType(portType)
                .setIsConnected(isConnected)
                .setMessage(message)
                .setOldIndex(i).build();
        }
        return result;
    }

    /**
     * @param metaNodeID ID of the metanode
     * @param newPorts The new ports
     * @param includeUnchanged If connections that will not change should be included
     * @return List of pairs of original (first) and changed (second) connections
     */
    List<Pair<ConnectionContainer, ConnectionContainer>>
    changeDestinationPortsForMetaNode(final NodeID metaNodeID, final MetaPortInfo[] newPorts,
            final boolean includeUnchanged) {
        // argument node is either a contained metanode or this wfm itself
        // (latter only when updating outgoing connections)
        List<Pair<ConnectionContainer, ConnectionContainer>> result =
            new ArrayList<Pair<ConnectionContainer, ConnectionContainer>>();
        final Set<ConnectionContainer> connectionsToMetaNode = m_connectionsByDest.get(metaNodeID);
        for (ConnectionContainer cc : connectionsToMetaNode) {
            int destPort = cc.getDestPort();
            boolean hasBeenFound = false;
            for (MetaPortInfo mpi : newPorts) {
                if (mpi.getOldIndex() == destPort) {
                    hasBeenFound = true;
                    if (mpi.getNewIndex() != destPort || includeUnchanged) {
                        ConnectionContainer newConn = new ConnectionContainer(cc.getSource(), cc.getSourcePort(),
                            metaNodeID, mpi.getNewIndex(), cc.getType(), cc.isFlowVariablePortConnection());
                        newConn.setUIInfo(cc.getUIInfo());
                        result.add(new Pair<ConnectionContainer, ConnectionContainer>(cc, newConn));
                    }
                    break;
                }
            }
            if (!hasBeenFound) {
                throw new IllegalStateException("New meta port information array "
                        + "does not include currently connected ports, unseen connection: " + cc);
            }
        }
        return result;
    }

    /**
     * @param metaNodeID ID of the metanode
     * @param newPorts The new ports
     * @param includeUnchanged If connections that will not change should be included
     * @return List of pairs of original (first) and changed (second) connections
     */
    List<Pair<ConnectionContainer, ConnectionContainer>>
    changeSourcePortsForMetaNode(final NodeID metaNodeID, final MetaPortInfo[] newPorts,
            final boolean includeUnchanged) {
        // argument node is either a contained metanode or this wfm itself
        // (latter only when updating outgoing connections)
        List<Pair<ConnectionContainer, ConnectionContainer>> result =
            new ArrayList<Pair<ConnectionContainer, ConnectionContainer>>();
        final Set<ConnectionContainer> connectionsFromMetaNode = m_connectionsBySource.get(metaNodeID);
        for (ConnectionContainer cc : connectionsFromMetaNode) {
            int sourcePort = cc.getSourcePort();
            boolean hasBeenFound = false;
            for (MetaPortInfo mpi : newPorts) {
                if (mpi.getOldIndex() == sourcePort) {
                    hasBeenFound = true;
                    if (mpi.getNewIndex() != sourcePort || includeUnchanged) {
                        ConnectionContainer newConn = new ConnectionContainer(metaNodeID, mpi.getNewIndex(),
                            cc.getDest(), cc.getDestPort(), cc.getType(), cc.isFlowVariablePortConnection());
                        newConn.setUIInfo(cc.getUIInfo());
                        result.add(new Pair<ConnectionContainer, ConnectionContainer>(cc, newConn));
                    }
                    break;
                }
            }
            if (!hasBeenFound) {
                throw new IllegalStateException("New meta port information array "
                        + "does not include currently connected ports, unseen connection: "
                        + cc);
            }
        }
        return result;
    }

    /** Helper class for lists of nodes with their inports and the depth
     * in the list. */
    static class NodeAndInports implements Comparable<NodeAndInports> {
        private NodeID m_nodeId;
        private int m_depth;  // indicates max depth from start node(s)
        private Set<Integer> m_inports;
        /** Create new wrapper hold node, indices of inports, and depth.
         * @param id of node
         * @param portIx index of inport
         * @param depth initial depth
         */
        public NodeAndInports(final NodeID id, final Integer portIx,
                final int depth) {
            m_nodeId = id;
            m_inports = new HashSet<Integer>();
            if (portIx != null) {
                m_inports.add(portIx);
            }
            m_depth = depth;
        }
        /** @return id of node. */
        public NodeID getID() { return m_nodeId; }
        /** @return input port indices. */
        public Set<Integer> getInports() { return m_inports; }
        /** @param ip inport index to be added list. */
        public void addInport(final int ip) { m_inports.add(ip); }
        /** @param d new depth of node. */
        public void setDepth(final int d) { m_depth = d; }
        /** @return depth of node. */
        public int getDepth() { return m_depth; }
        /** {@inheritDoc} */
        @Override
        public int compareTo(final NodeAndInports o2) {
            return (Integer.valueOf(this.m_depth).compareTo(o2.m_depth));
        }
        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof NodeAndInports)) {
                return false;
            }
            return this.m_depth == ((NodeAndInports)obj).m_depth;
        }
        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return m_nodeId.hashCode() + m_depth;
        }
    }

    /**
     * Return matching {@link ScopeEndNode} node for the given {@link ScopeStartNode}.
     *
     * @param id The requested scope start node
     * @throws IllegalLoopException if scope setup is wrong
     * @throws IllegalArgumentException if argument is not a scope start node
     * @return id of scope end node or null if no such node was found.
     */
    NodeID getMatchingScopeEnd(final NodeID id, final Class<?> startNodeType, final Class<?> endNodeType)
        throws IllegalScopeException {
        assertNodeType(id, startNodeType);
        if (m_nodeAnnotationCache == null) {
            this.updateGraphAnnotationCache();
        }
        for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
            if (nga.getID().equals(id)) {
                assert nga.getOutportIndex() == -1; // must be SingleNodeContainer, ports don't matter.
                NodeID end = nga.peekEndNodeStack();
                if (end != null) {
                    return end;
                } else {
                    throw new IllegalScopeException(
                        "Could not find matching " + splitCamelCase(endNodeType.getSimpleName()) + "!");
                }
            }
        }
        assert false : "Failed to find NodeGraphAnnotation for node from this very workflow.";
        throw new IllegalScopeException(
            "Could not find matching " + splitCamelCase(endNodeType.getSimpleName()) + " (missing node annotation)!");
    }

    private static String splitCamelCase(final String s) {
        return Arrays.stream(s.split("(?<!^)(?=[A-Z])")).map(s2 -> s2.toLowerCase()).collect(Collectors.joining(" "));
    }

    private void assertNodeType(final NodeID id, final Class<?> nodeType) {
        NodeContainer nc = getNode(id);
        if (!(nc instanceof SingleNodeContainer)) {
            throw new IllegalArgumentException("Not a SingleNodeContainer / " + nodeType.getSimpleName() + id);
        }
        SingleNodeContainer snc = (SingleNodeContainer)nc;
        if (!snc.isModelCompatibleTo(nodeType)) {
            throw new IllegalArgumentException("Not a " + nodeType.getSimpleName() + id);
        }
    }

    /** Return matching LoopEnd node for the given LoopStart.
    *
    * @param id The requested start node (instanceof LoopStart)
    * @throws IllegalLoopException if loop setup is wrong
    * @throws IllegalArgumentException if argument is not a LoopStart node
    * @return id of end node or null if no such node was found.
    */
    NodeID getMatchingLoopEnd(final NodeID id) throws IllegalLoopException {
        try {
            return getMatchingScopeEnd(id, LoopStartNode.class, LoopEndNode.class);
        } catch (IllegalScopeException ex) {
            throw new IllegalLoopException(ex.getMessage(), ex);
        }
    }

    /**
     * Return matching {@link ScopeStartNode} for the given {@link ScopeEndNode}.
     *
     * @param id The requested end node
     * @throws IllegalScopeException if scope setup is wrong
     * @throws IllegalArgumentException if argument is not a scope start node
     * @return id of scope start node or null if no such node was found.
     */
    NodeID getMatchingScopeStart(final NodeID id, final Class<?> startNodeType, final Class<?> endNodeType)
        throws IllegalScopeException {
        assertNodeType(id, endNodeType);
        if (m_nodeAnnotationCache == null) {
            this.updateGraphAnnotationCache();
        }

        for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
            if (nga.getID().equals(id)) {
                assert nga.getOutportIndex() == -1; // must be SingleNodeContainer, ports don't matter.
                NodeID start = nga.peekStartNodeStack();
                if (start != null) {
                    NodeContainer ncls = getNode(start);
                    if (!(ncls instanceof SingleNodeContainer)) {
                        throw new IllegalScopeException(
                            id + " is not connected to a SNC / " + startNodeType.getSimpleName() + " but " + start);
                    }
                    SingleNodeContainer sncls = (SingleNodeContainer)ncls;
                    if (!sncls.isModelCompatibleTo(startNodeType)) {
                        throw new IllegalScopeException(
                            id + " is not connected to a " + startNodeType.getSimpleName() + " but " + start);
                    }
                    return start;
                } else {
                    throw new IllegalScopeException(
                        "Could not find matching " + splitCamelCase(startNodeType.getSimpleName()) + "!");
                }
            }
        }
        assert false : "Failed to find NodeGraphAnnotation for node from this very workflow.";
        throw new IllegalScopeException(
            "Could not find matching " + splitCamelCase(startNodeType.getSimpleName()) + " (missing node annotation)!");
    }

    /** Return matching LoopStart node for the given LoopEnd.
    *
    * @param id The requested end node (instanceof LoopEnd)
    * @throws IllegalLoopException if loop setup is wrong
    * @throws IllegalArgumentException if argument is not a LoopEnd node
    * @return id of start node or null if no such node was found.
    */
    NodeID getMatchingLoopStart(final NodeID id) throws IllegalLoopException {
        try {
            return getMatchingScopeStart(id, LoopStartNode.class, LoopEndNode.class);
        } catch (IllegalScopeException ex) {
            throw new IllegalLoopException(ex.getMessage(), ex);
        }
    }

    /** Create list of nodes (id)s that are part of a loop body. Note that
     * this also includes any dangling branches which leave the loop but
     * do not connect back to the end-node. Used to re-execute all nodes
     * of a loop. NOTE that this list can contain nodes more than one with
     * different inport indices! The list is sorted according to "longest
     * path layering" making sure that nodes are always added behind
     * all of their predecessors.
     *
     * The list does not contain the start node or end node.
     *
     * @param startNode id of head of loop
     * @param endNode if of tail of loop
     * @return list of nodes within loop body &amp; any dangling branches. The list
     *         also contains the used input ports of each node.
     * @throws IllegalLoopException If there is a ill-posed loop (dangling branches)
     */
    ArrayList<NodeAndInports> findAllNodesConnectedToLoopBody(final NodeID startNode, final NodeID endNode)
            throws IllegalLoopException {
        ArrayList<NodeAndInports> tempOutput = findAllNodesInbetween(startNode, null, endNode);
        if (startNode.equals(endNode)) {
            // silly case - start = end node.
            return tempOutput;
        }
        // check that no connection from within the loop leaves workflow:
        for (NodeAndInports nai : tempOutput) {
            if (nai.getID().equals(this.getID())) {
                // if any branch leaves this WFM, complain!
                throw new IllegalLoopException("Loops are not permitted to leave workflows!");
            }
        }
        // make sure we have no branches from within the loop body reconnecting
        // to the flow after the loop end node (= skipping over loop end)
        HashMap<NodeID, Set<Integer>> nodesAfterEndNode =
            createBreadthFirstSortedList(Collections.singleton(endNode), true);
        for (NodeAndInports nai2 : tempOutput) {
            if (nodesAfterEndNode.containsKey(nai2.getID())) {
                throw new IllegalLoopException("Branches are not permitted to leave loops!");
            }
        }
        return tempOutput;
    }

    /** Return list of nodes that are part of the same scope as the given one.
     * List will contain anchor node alone if there is no scope around it.
     *
     * @param anchor node
     * @return list of nodes.
     * @since 2.8
     */
    public List<NodeContainer> getNodesInScope(final SingleNodeContainer anchor) {
        if (m_nodeAnnotationCache == null) {
            updateGraphAnnotationCache();
        }
        NodeID scope = null;
        for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
            if (nga.getID().equals(anchor.getID())) {
                scope = nga.peekStartNodeStack();
                break;
            }
        }
        if (scope == null) {
            // no scope - return anchor only
            return asList(anchor);
        } else {
            // this is a set because m_nodeAnnotationCache 'contains' metanodes multiple times under some
            // circumstances (metanodes with multiple outports)
            Set<NodeID> result = new LinkedHashSet<>();
            for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
                if (nga.startNodeStackContains(scope)) {
                    result.add(nga.getID());
                }
            }
            return result.stream().map(m_nodes::get).collect(Collectors.toList());
        }
    }

    ///////////////////////////
    // Workflow Graph Analysis.
    ///////////////////////////

    /**
     * @param id of node.
     * @return set of graph annotations for this node.
     * @since 2.8
     */
    public Set<NodeGraphAnnotation> getNodeGraphAnnotations(final NodeID id) {
        if (m_nodeAnnotationCache == null) {
            updateGraphAnnotationCache();
        }
        HashSet<NodeGraphAnnotation> output = new HashSet<NodeGraphAnnotation>();
        for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
            if (nga.getID().equals(id)) {
                output.add(nga);
            }
        }
        return output;
    }

    /**
     * @param id of node.
     * @return set of graph annotations for the given SingleNodeContainer.
     * @throws IllegalArgumentException if node is not a SingleNodeContainer
     * @since 3.0
     */
    public NodeGraphAnnotation getSingleNodeGraphAnnotation(final NodeID id)
    throws IllegalArgumentException {
        if (!(m_nodes.get(id) instanceof SingleNodeContainer)) {
            throw new IllegalArgumentException(id + " is not a SingleNodeContainer!");
        }
        if (m_nodeAnnotationCache == null) {
            updateGraphAnnotationCache();
        }
        for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
            if (nga.getID().equals(id)) {
                return nga;
            }
        }
        throw new IllegalArgumentException("No NodeGraphAnnotation found for " + id);
    }

    /** hold graph based annotations for all nodes. */
    private ArrayList<NodeGraphAnnotation> m_nodeAnnotationCache = null;

    /** clean cache - called internally whenever the structure (connections/nodes) are altered. */
    private void clearGraphAnnotationCache() {
        m_nodeAnnotationCache = null;
        // also clear cache in parent - changes here may affect the connectivity outside as well.
        if (m_wfm != null && m_wfm.getParent() != null && m_wfm.getParent().getWorkflow() != null) {
            m_wfm.getParent().getWorkflow().clearGraphAnnotationCache();
        }
    }

    /** Analyse entire workflow graph and mark scope start/end node pairs and
     * level of layered depth search. Do not dive into metanodes but consider
     * their internal connectivity to continue outside search on appropriate
     * ports.
     */
    private void updateGraphAnnotationCache() {
        LOGGER.debug("Triggering graph analysis on " + getID());
        assert m_nodeAnnotationCache == null;
        m_nodeAnnotationCache = new ArrayList<NodeGraphAnnotation>();
        // 1) add start nodes.
        // insert metanode itself with all connected inports as "outport" indices
        for (ConnectionContainer cc : getConnectionsBySource(getID())) {
            NodeGraphAnnotation nls = new NodeGraphAnnotation(getID(), cc.getSourcePort());
            if (!m_nodeAnnotationCache.contains(nls)) {
                m_nodeAnnotationCache.add(nls);
            }
        }
        // also add source nodes with all of their outports (SNC or WFM doesn't matter here!)
        for (NodeID id : m_nodes.keySet()) {
            if (m_connectionsByDest.get(id).size() == 0) {
                NodeContainer nc = m_nodes.get(id);
                NodeGraphAnnotation nls = new NodeGraphAnnotation(nc);
                m_nodeAnnotationCache.add(nls);
            }
        }
        // 2) follow chain of nodes and keep adding until we reach an end or a metanode outport.
        int currIndex = 0;
        while (currIndex < m_nodeAnnotationCache.size()) {
            NodeGraphAnnotation currNGA = m_nodeAnnotationCache.get(currIndex);
            NodeID currID = currNGA.getID();
            int currOutport = currNGA.getOutportIndex();
            // find all nodes that are connected to this node/outport pair
            for (ConnectionContainer cc : this.getConnectionsBySource(currID)) {
                if ((currOutport == -1) || (currOutport == cc.getSourcePort())) {
                    NodeID destID = cc.getDest();
                    if (!destID.equals(this.getID())) {
                        // only if we have not yet reached an outport - try to find node/port in existing list:
                        NodeContainer destNC = m_nodes.get(destID);
                        int destInPort = cc.getDestPort();
                        // determine set of relevant outport indices
                        Set<Integer> connectedOutports;
                        if (destNC instanceof SingleNodeContainer) {
                            // trivial for SNC: all ports (indicated by -1 place holder)
                            connectedOutports = new HashSet<Integer>();
                            connectedOutports.add(-1);
                        } else {
                            assert destNC instanceof WorkflowManager;
                            // retrieve outports of this node that are (internally) connected to given inport
                            connectedOutports = ((WorkflowManager)destNC).getWorkflow().connectedOutPorts(destInPort);
                        }
                        int ix = 0;
                        while (ix < m_nodeAnnotationCache.size()) {
                            NodeGraphAnnotation nga = m_nodeAnnotationCache.get(ix);
                            int outportIndex = nga.getOutportIndex();
                            if ((nga.getID().equals(destID)) && (connectedOutports.contains(outportIndex))) {
                                assert ix != currIndex;
                                // node is already in list, merge stacks with "new" element
                                // and check if we made any adjustments:
                                if (nga.mergeForward(new NodeGraphAnnotation(destNC, outportIndex, currNGA))) {
                                    // changes were made, let's check if we need to move the node.
                                    if (ix < currIndex) {
                                        // move node to end of list if it was already "touched" so that depth,
                                        // stacks, and other info of its successors will also be adjusted!
                                        NodeGraphAnnotation ngaOrg = m_nodeAnnotationCache.remove(ix);
                                        assert ngaOrg.getID().equals(nga.getID());
                                        m_nodeAnnotationCache.add(nga);
                                        // critical: we removed an element in our list which resided before our
                                        // pointer. Make sure we still point to current node.
                                        currIndex--;
                                        ix--;
                                    }
                                }
                                // remove this port from our list - no need to add it "as new" later.
                                connectedOutports.remove(outportIndex);
                            }
                            if (connectedOutports.size() == 0) {
                                // skip the rest of the list...
                                ix = m_nodeAnnotationCache.size();
                            }
                            ix++;
                        }
                        for (int o : connectedOutports) {
                            // ...it's a node/port combo not yet in our list: add it
                            NodeGraphAnnotation nga = new NodeGraphAnnotation(destNC, o, currNGA);
                            m_nodeAnnotationCache.add(nga);
                        }
                    }
                }
            }
            currIndex++;
        }
        // make sure nodes are inversely sorted by their final depth!
        Collections.sort(m_nodeAnnotationCache);
        Collections.reverse(m_nodeAnnotationCache);
        // now let's do all of this backwards, so that we also detect end nodes depending on
        // the same start node:
        for (NodeGraphAnnotation nga : m_nodeAnnotationCache) {
            NodeID currID = nga.getID();
            int currOutPort = nga.getOutportIndex();
            HashSet<NodeGraphAnnotation> connectedNGAs = new HashSet<NodeGraphAnnotation>();
            for (ConnectionContainer cc : m_connectionsBySource.get(currID)) {
                if ((currOutPort == -1) || currOutPort == cc.getSourcePort()) {
                    NodeID destID = cc.getDest();
                    if (destID.equals(getID())) {
                        // leaving metanode, remember port index!
                        nga.addConnectedOutport(cc.getDestPort());
                    } else {
                        NodeContainer destNC = getNode(destID);
                        if (destNC instanceof SingleNodeContainer) {
                            // just add the NGA of the successor
                            for (NodeGraphAnnotation nga2 : m_nodeAnnotationCache) {
                                if (nga2.getID().equals(destID)) {
                                    connectedNGAs.add(nga2);
                                }
                            }
                        } else {
                            assert destNC instanceof WorkflowManager;
                            // add only NGAs that are available on outports which are connected this inport
                            Set<Integer> connectedOutPorts
                                    = ((WorkflowManager)destNC).getWorkflow().connectedOutPorts(cc.getDestPort());
                            for (NodeGraphAnnotation nga2 : m_nodeAnnotationCache) {
                                if (nga2.getID().equals(destID) && connectedOutPorts.contains(nga2.getOutportIndex())) {
                                    connectedNGAs.add(nga2);
                                }
                            }
                        }
                    }
                }
            }
            nga.setAndMergeBackwards(connectedNGAs);
        }
        // and finally sort node again:
        Collections.reverse(m_nodeAnnotationCache);
    }
}
