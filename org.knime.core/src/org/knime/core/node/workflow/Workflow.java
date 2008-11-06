/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Oct 29, 2008 (mb): extracted from WorkflowManager
 */
package org.knime.core.node.workflow;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;

/** Container class wrapping wrapping the network of nodes forming
 * a workflow together with some of the basic functionality, especially
 * traversal methods.
 * 
 * @author M. Berthold, University of Konstanz
 */
class Workflow {

    /** mapping from NodeID to Nodes. */
    final TreeMap<NodeID, NodeContainer> m_nodes =
        new TreeMap<NodeID, NodeContainer>();

    // Connections (by node, source and destination). Note that meta
    // connections (in- and outgoing of this workflow) are also part
    // of these maps. They will have our own ID as source/dest ID.

    /** mapping from source NodeID to set of outgoing connections. */
    private final TreeMap<NodeID, Set<ConnectionContainer>>
        m_connectionsBySource =
        new TreeMap<NodeID, Set<ConnectionContainer>>();
    /** mapping from destination NodeID to set of incoming connections. */
    private final TreeMap<NodeID, Set<ConnectionContainer>>
        m_connectionsByDest =
        new TreeMap<NodeID, Set<ConnectionContainer>>();

    private NodeID m_id;
    
    /**
     * Constructor - initialize sets for meta node in/out connections.
     * @param id of workflow
     * 
     */
    Workflow(final NodeID id) {
        m_id = id;
        // add sets for this (meta-) node's in- and output connections
        m_connectionsByDest.put(id, new HashSet<ConnectionContainer>());
        m_connectionsBySource.put(id, new HashSet<ConnectionContainer>());
    }
    
    /**
     * @return id of this workflow
     */
    NodeID getID() {
        return m_id;
    }

    /** Return NodeContainer for a given id.
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
        m_nodes.put(id, nc);
        // create Sets of in and outgoing connections
        m_connectionsBySource.put(id, new HashSet<ConnectionContainer>());
        m_connectionsByDest.put(id, new HashSet<ConnectionContainer>());
    }

    /** Remove given node.
     * 
     * @param id of NodeContainer to be removed.
     * @return removed NodeContainer
     */
    NodeContainer removeNode(final NodeID id) {
        // clean up the connection lists
        m_connectionsBySource.remove(id);
        m_connectionsByDest.remove(id);
        // and return removed node id
        return m_nodes.remove(id);
    }
    
    /**
     * @return collection of all NodeContainers that are part of this workflow.
     */
    Collection<NodeContainer> getNodeValues() {
        return m_nodes.values();
    }

    /**
     * @return collection of all NodeIDs that are part of this workflow.
     */
    Set<NodeID> getNodeIDs() {
        return m_nodes.keySet();
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
        return m_connectionsByDest.get(id);
    }

    /** Return all connections having the same destination.
     * 
     * @param id of destination node
     * @return set as described above
     */
    Set<ConnectionContainer> getConnectionsBySource(final NodeID id) {
        return m_connectionsBySource.get(id);
    }
    
    /**
     * @return a collection of sets of ConnectionContainers, grouped by
     *   source node ID.
     */ 
    Collection<Set<ConnectionContainer>> getConnectionsBySourceValues() {
        return m_connectionsBySource.values();
    }


    /** Return map of node ids connected to the given node sorted in breadth
     * first order mapped to a set of portIDs. Note that also nodes which
     * have another predecessors not contained in this list may be included as
     * long as at least one input node is connected to a node in this list!
     * The set of integers represents the indices of input ports which are
     * actually used within the graph covered in the result list.
     *
     * @param id of node
     * @param skipWFM if true, do not include WFM in the list
     * @return map as described above.
     */
    SortedMap<NodeID, Set<Integer>> getBreadthFirstListOfNodeAndSuccessors(
            final NodeID id, final boolean skipWFM) {
        // assemble unsorted list of successors
        HashSet<NodeID> inclusionList = new HashSet<NodeID>();
        completeSet(inclusionList, id, -1);
        // and then get all successors which are part of this list in a nice
        // BFS order
        TreeMap<NodeID, Set<Integer>> bfsSortedNodes
                    = new TreeMap<NodeID, Set<Integer>>();
        // put the origin - not that none of it's ports (if any) are of
        // interest -  into the map
        bfsSortedNodes.put(id, new HashSet<Integer>());
        expandListBreadthFirst(bfsSortedNodes, inclusionList);
        // if wanted (and contained): remove WFM itself
        if (skipWFM && bfsSortedNodes.keySet().contains(this.getID())) {
            bfsSortedNodes.remove(this.getID());
        }
        return bfsSortedNodes;
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
    SortedMap<NodeID, Set<Integer>> createBreadthFirstSortedList(
            final Set<NodeID> ids,
            final boolean skipWFM) {
        // first create list of nodes without predecessor or only the WFM
        // itself (i.e. connected to outside "world" only.
        SortedMap<NodeID, Set<Integer>> bfsSortedNodes
                        = new TreeMap<NodeID, Set<Integer>>();
        for (NodeID thisNode : ids) {
            // find the nodes in the list which are sources (i.e. not
            // preceeded by any others in the list)
            Set<ConnectionContainer> incomingConns
                       = m_connectionsByDest.get(thisNode);
            boolean isSource = true;
            for (ConnectionContainer thisConn : incomingConns) {
                if (ids.contains(thisConn.getSource())) {
                    isSource = false;
                }
            }
            if (isSource) {
                // put this source - note that none of it's ports (if any
                // exist) are of interest
                bfsSortedNodes.put(thisNode, new HashSet<Integer>());
            }
        }
        // and finally complete this list by adding all successors...
        expandListBreadthFirst(bfsSortedNodes, ids);
        // if wanted (and contained): remove WFM itself
        if (skipWFM && bfsSortedNodes.containsKey(this.getID())) {
            bfsSortedNodes.remove(this.getID());
        }
        return bfsSortedNodes;
    }

    /** Complete set of nodes depth-first starting with node id. If the given
     * node is already in the set, nothing happens. Note that this function
     * does not pursue connections leaving this workflow - we will only add
     * our own ID (the workflow) as "last" node in the chain.
     * 
     * @param nodes set of nodes to be completed
     * @param id of node to start search from
     * @param index of port the incoming connection connected to
     */
    private void completeSet(HashSet<NodeID> nodes, final NodeID id,
            final int incomingPortIndex) {
        if (nodes.add(id)) {  // only if id was not already contained in set!
            NodeContainer thisNode = m_nodes.get(id);
            for (ConnectionContainer cc : m_connectionsBySource.get(id)) {
                NodeID nextNodeID = cc.getDest();
                if (!nextNodeID.equals(getID())) {
                    // avoid to follow any connections leaving the workflow!
                    if (thisNode instanceof SingleNodeContainer) {
                        // easy - just add normal nodes
                        completeSet(nodes, nextNodeID, cc.getDestPort());
                    } else {
                        assert thisNode instanceof WorkflowManager;
                        WorkflowManager wfm = (WorkflowManager)thisNode;
                        // not so easy - we need to find out who is connected
                        // through this WFM (if we have a port index, of course)
                        if (incomingPortIndex < 0) {
                            // TODO check for unconnected metaoutports?
                            completeSet(nodes, nextNodeID, cc.getDestPort());
                        } else {
                            Set<Integer> outports = wfm.m_workflow
                                           .connectedOutPorts(cc.getDestPort());
                            if (outports.contains(cc.getSourcePort())) {
                                completeSet(nodes, nextNodeID,
                                            cc.getDestPort());
                            }
                        }
                    }
                } else {
                    // make sure the WFM itself is in the list (if reached)
                    nodes.add(nextNodeID);
                }
            }
        }
    }

    /** Expand a given list of nodes to include all successors which are
     * connected to anyone of the nodes in a breadth first manner. Don't
     * include any of the nodes not contained in the "inclusion" list
     * if given.
     * 
     * @param bfsSortedNodes existing, already sorted list of nodes
     * @param inclusionList complete list of nodes to be sorted breadth first
     */
    private void expandListBreadthFirst(
            SortedMap<NodeID, Set<Integer>> bfsSortedNodes,
            final Set<NodeID> inclusionList) {
        // keep adding nodes until we can't find new ones anymore
        for (int i = 0; i < bfsSortedNodes.size(); i++) {
            // FIXME: not a very nice way to iterate over the keys of a map...
            //   (but since we constantly add to it in this loop?!)
            Object[] ani = bfsSortedNodes.keySet().toArray();
            NodeID currNode = (NodeID)(ani[i]);
            // avoid to close loop and start with WFM again:
            if (currNode.equals(this.getID())) {
                continue;
            }
            // look at all successors of this node
            for (ConnectionContainer cc : m_connectionsBySource.get(currNode)) {
                NodeID succNode = cc.getDest();
                // don't check nodes which are already in the list...
                if (!bfsSortedNodes.containsKey(succNode)) {
                    // and make sure all predecessors which are part of the
                    // inclusion list of this successor are already
                    // in the list
                    boolean allContained = true;
                    Set<Integer> incomingPorts = new HashSet<Integer>();
                    for (ConnectionContainer cc2
                                   : m_connectionsByDest.get(succNode)) {
                        NodeID pred = cc2.getSource();
                        if (!pred.equals(getID())) {
                            // its not a WFMIN connection...
                            if (!bfsSortedNodes.containsKey(pred)) {
                                // ...and its not already in the list...
                                if (inclusionList.contains(pred)) {
                                    // ...but if it is in the inclusion list
                                    // then do not (yet!) include it!
                                    allContained = false;
                                }
                            } else {
                                // not WFMIN but source is in our list: needs
                                // to be remembered as "incoming" port within
                                // this BF search.
                                incomingPorts.add(cc2.getDestPort());
                            }
                        }
                    }
                    if (allContained) {
                        // if all predecessors are already in the BFS list (or
                        // not to be considered): add it!
                        bfsSortedNodes.put(succNode, incomingPorts);
                    }
                }
            }
        }
    }
    
    /** Determine outports which are connected (directly or indirectly) to
     * the given inport in this workflow.
     * 
     * @param inPortIx index of inport
     * @return set of outport indices
     */
    Set<Integer> connectedOutPorts(final int inPortIx) {
        HashSet<Integer> outSet = new HashSet<Integer>();
        // Map to remember connected nodes with the index of the corresponding
        // input port
        TreeMap<NodeID, Integer> nodesToCheck
                            = new TreeMap<NodeID, Integer>();
        // find everything that is connected to an input port of this workflow
        // with an index contained in the set:
        for (ConnectionContainer cc : m_connectionsBySource.get(getID())) {
            if (inPortIx == cc.getSourcePort()) {
                NodeID nextID = cc.getDest();
                if (nextID.equals(this.getID())) {
                    assert cc.getType().
                         equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
                    outSet.add(cc.getDestPort());
                } else {
                    nodesToCheck.put(nextID, cc.getDestPort());
                }
            }
        }
        // now follow those nodes and see if we reach a workflow outport
        int currentNode = 0;
        while (currentNode < nodesToCheck.size()) {
            // FIXME: Not a very nice way to iterate over this set but
            //   since we are adding things to it inside the loop?!
            Object[] ani = nodesToCheck.keySet().toArray();
            NodeID thisID = (NodeID)(ani[currentNode]);
            assert !(thisID.equals(this.getID()));
            NodeContainer thisNode = m_nodes.get(thisID);
            if (thisNode instanceof SingleNodeContainer) {
                // simply add everything that is connected to this node
                for (ConnectionContainer cc : m_connectionsBySource.get(
                        thisID)) {
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
                Set<Integer> connectedOutPorts = 
                     ((WorkflowManager)thisNode).m_workflow.
                                          connectedOutPorts(portToCheck);
                for (ConnectionContainer cc : m_connectionsBySource.get(
                        thisID)) {
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

    /** Determine inports which are connected (directly or indirectly) to
     * the given outport in this workflow.
     * 
     * @param outPortIx index of outport
     * @return set of inport indices
     */
    Set<Integer> connectedInPorts(final int outPortIx) {
        HashSet<Integer> inSet = new HashSet<Integer>();
        // Map to remember connected nodes with the index of the corresponding
        // output port
        TreeMap<NodeID, Integer> nodesToCheck
                            = new TreeMap<NodeID, Integer>();
        // find everything that is connected to an output port of this workflow
        // with an index contained in the set:
        for (ConnectionContainer cc : m_connectionsByDest.get(getID())) {
            if (outPortIx == cc.getDestPort()) {
                NodeID prevID = cc.getSource();
                if (prevID.equals(this.getID())) {
                    assert cc.getType().
                         equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
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
            // FIXME: Not a very nice way to iterate over this set but
            //   since we are adding things to it inside the loop?!
            Object[] ani = nodesToCheck.keySet().toArray();
            NodeID thisID = (NodeID)(ani[currentNode]);
            assert !(thisID.equals(this.getID()));
            NodeContainer thisNode = m_nodes.get(thisID);
            if (thisNode instanceof SingleNodeContainer) {
                // simply add everything that is connected to this node
                for (ConnectionContainer cc : m_connectionsByDest.get(
                        thisID)) {
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
                     ((WorkflowManager)thisNode).m_workflow.
                                          connectedInPorts(portToCheck);
                for (ConnectionContainer cc : m_connectionsByDest.get(
                        thisID)) {
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
     * workflow. We will only add our own ID (the workflow) as "last" node
     * in the chain.
     * 
     * @param nodes set of nodes to be completed
     * @param id of node to start search from
     * @param index of port the outgoing connection connected to
     */
    private void completeSetBackwards(HashSet<NodeID> nodes, final NodeID id,
            final int outgoingPortIndex) {
        NodeContainer thisNode = m_nodes.get(id);
        for (ConnectionContainer cc : m_connectionsByDest.get(id)) {
            NodeID prevNodeID = cc.getSource();
            if (!prevNodeID.equals(getID())) {
                // avoid to follow any connections leaving the workflow!
                if (thisNode instanceof SingleNodeContainer) {
                    // easy - just add normal nodes
                    completeSet(nodes, prevNodeID, cc.getSourcePort());
                } else {
                    assert thisNode instanceof WorkflowManager;
                    WorkflowManager wfm = (WorkflowManager)thisNode;
                    // not so easy - we need to find out who is connected
                    // through this WFM (if we have a port index, of course)
                    if (outgoingPortIndex < 0) {
                        // TODO check for unconnected metaoutports?
                        completeSetBackwards(nodes, prevNodeID,
                                cc.getSourcePort());
                    } else {
                        // find out which inports are connected through this
                        // WFM to the given outport
                        Set<Integer> inports = wfm.m_workflow
                                      .connectedInPorts(cc.getSourcePort());
                        // and only add the predeccessor if he is connected
                        // to one of those
                        if (inports.contains(cc.getDestPort())) {
                            completeSetBackwards(nodes, prevNodeID,
                                        cc.getSourcePort());
                        }
                    }
                }
            } else {
                // make sure the WFM itself is in the list (if reached)
                nodes.add(prevNodeID);
            }
        }
    }

    /** Return map of node ids to set of port indices based on list of output
     * ports. The map is sorted by traversing the graph backwards, breadth
     * first, the set of port indices represents the input ports actually used
     * within the graph covered. Include this WFM if any incoming ports are
     * connected.
     * 
     * @param set of integers indicating the ports of interest
     * @return backwards, BF sorted list of node ids
     */
    SortedMap<NodeID, Set<Integer>> createBackwardsBreadthFirstSortedList(
            final Set<Integer> outportIndices) {
        // this will our result
        SortedMap<NodeID, Set<Integer>> sortedNodes
                        = new TreeMap<NodeID, Set<Integer>>();
        // find everything that is connected to an output port of this workflow
        // with an index contained in the set and complete the list,
        // quick&dirty backwards depth first:
        HashSet<NodeID> inclusionList = new HashSet<NodeID>();
        for (ConnectionContainer cc : m_connectionsByDest.get(getID())) {
            if (outportIndices.contains(cc.getDestPort())) {
                NodeID prevID = cc.getSource();
                inclusionList.add(prevID);
                if (!prevID.equals(this.getID())) {
                    completeSetBackwards(inclusionList, prevID,
                            cc.getSourcePort());
                    // also add this node as starting point for our ordered list
                    if (sortedNodes.containsKey(prevID)) {
                        // node already added: add port index to set
                        Set<Integer> is = sortedNodes.get(prevID);
                        is.add(cc.getSourcePort());
                    } else {
                        // node does not yet exist:
                        Set<Integer> is = new HashSet<Integer>();
                        is.add(cc.getSourcePort());
                        sortedNodes.put(prevID, is);
                    }
                } else {
                    assert cc.getType().equals(ConnectionType.WFMTHROUGH);
                    Set<Integer> is = new HashSet<Integer>();
                    is.add(cc.getSourcePort());
                    sortedNodes.put(prevID, is);
                }
            }
        }
        // now take this set of unordered nodes as inclusion set for a proper
        // backwards breadth first search:
        expandListBackwardsBreadthFirst(sortedNodes, inclusionList);
        // return the list...
        return sortedNodes;
    }

    /** Expand a given list of nodes to include all predecessors which are
     * connected to anyone of the nodes in a backwards breadth first manner.
     * Also includes WFM itself but stops then. Do not include any other
     * nodes which are not contained in the inclusion list.
     * 
     * @param sortedNodes existing, already sorted list of (end) nodes
     * @param inclusionList all nodes which are to be considered
     */
    private void expandListBackwardsBreadthFirst(
            SortedMap<NodeID, Set<Integer>> sortedNodes,
            final Set<NodeID> inclusionList) {
        // keep adding nodes until we can't find new ones anymore
        for (int i = 0; i < sortedNodes.size(); i++) {
            // FIXME: not a very nice way to iterate over the keys of a map...
            //   (but since we constantly add to it in this loop?!)
            Object[] ani = sortedNodes.keySet().toArray();
            NodeID currNode = (NodeID)(ani[i]);
            // avoid to close loop and start with WFM again:
            if (currNode.equals(this.getID())) {
                continue;
            }
            // look at all predecessors of this node
            for (ConnectionContainer cc : m_connectionsByDest.get(currNode)) {
                NodeID prevNode = cc.getSource();
                // don't check nodes which are already in the list...
                if (!sortedNodes.containsKey(prevNode)) {
                    // and make sure all successors which are part of the
                    // inclusion list of this nodes are already
                    // in the list
                    boolean allContained = true;
                    Set<Integer> outgoingPorts = new HashSet<Integer>();
                    for (ConnectionContainer cc2
                                   : m_connectionsBySource.get(prevNode)) {
                        NodeID succ = cc2.getDest();
                        if (!succ.equals(getID())) {
                            // its not a WFMOUT connection...
                            if (!sortedNodes.containsKey(succ)) {
                                // ...and its not already in the list...
                                if (inclusionList.contains(succ)) {
                                    // ...but if it is in the inclusion list
                                    // then do not (yet!) include it!
                                    allContained = false;
                                }
                            } else {
                                // not WFMOUT but dest is in our list: needs
                                // to be remembered as "outgoing" port within
                                // this BF search.
                                outgoingPorts.add(cc2.getSourcePort());
                            }
                        }
                    }
                    if (allContained) {
                        // if all successors are already in the BFS list (or
                        // not to be considered): add it!
                        sortedNodes.put(prevNode, outgoingPorts);
                    }
                }
            }
        }
    }

}
