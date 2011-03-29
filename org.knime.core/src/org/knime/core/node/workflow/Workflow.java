/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
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
    LinkedHashMap<NodeID, Set<Integer>> getBreadthFirstListOfNodeAndSuccessors(
            final NodeID id, final boolean skipWFM) {
        // assemble unsorted list of successors
        HashSet<NodeID> inclusionList = new HashSet<NodeID>();
        completeSet(inclusionList, id, -1);
        // and then get all successors which are part of this list in a nice
        // BFS order
        LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes
                    = new LinkedHashMap<NodeID, Set<Integer>>();
        // put the origin - note that none of it's ports (if any) are of
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
    LinkedHashMap<NodeID, Set<Integer>> createBreadthFirstSortedList(
            final Set<NodeID> ids,
            final boolean skipWFM) {
        // first create list of nodes without predecessor or only the WFM
        // itself (i.e. connected to outside "world" only.
        LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes
                        = new LinkedHashMap<NodeID, Set<Integer>>();
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
    private void completeSet(final HashSet<NodeID> nodes, final NodeID id,
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
                            Set<Integer> outports = wfm.getWorkflow()
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
            final LinkedHashMap<NodeID, Set<Integer>> bfsSortedNodes,
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
        LinkedHashMap<NodeID, Integer> nodesToCheck
                            = new LinkedHashMap<NodeID, Integer>();
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
                     ((WorkflowManager)thisNode).getWorkflow().
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

    /** Determine all nodes which are connected (directly or indirectly) to
     * the given inports in this workflow. The list is sorted according to
     * "longest path layering" making sure that nodes are always added behind
     * all of their predecessors.
     *
     * @param inPorts indices of inports
     * @return set of nodes with used inports
     *
     * @FIXME: this is an almost complete replication of the function
     *   findAllNodesConnectedToLoopBody - they should both call a more general
     *   implementation!
     */
    ArrayList<NodeAndInports> findAllConnectedNodes(final Set<Integer> inPorts) {
        ArrayList<NodeAndInports> tempOutput = new ArrayList<NodeAndInports>();
        // find everything that is connected to an input port of this workflow
        // with an index contained in the set:
        for (ConnectionContainer cc : m_connectionsBySource.get(getID())) {
            if (inPorts.contains(cc.getSourcePort())) {
                NodeID nextID = cc.getDest();
                if (nextID.equals(this.getID())) {
                    assert cc.getType().
                         equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
                } else {
                    tempOutput.add(new NodeAndInports(cc.getDest(),
                            cc.getDestPort(), /*depth=*/0));
                }
            }
        }
        // now follow those nodes and keep adding until we reach the end of the workflow
        int currIndex = 0;
        while (currIndex < tempOutput.size()) {
            NodeID currID = tempOutput.get(currIndex).getID();
            NodeContainer currNode = m_nodes.get(currID);
            Set<Integer> currInports = tempOutput.get(currIndex).getInports();
            int currDepth = tempOutput.get(currIndex).getDepth();
            Set<Integer> currOutports = new HashSet<Integer>();
            if (   (currNode instanceof SingleNodeContainer)
                || (currInports == null)) {
                // simple: all outports are affected
                // (SNC or WFM without listed inports)
                for (int i=0; i<currNode.getNrOutPorts(); i++) {
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
                assert (cc.getSource().equals(currID));
                if (currOutports.contains(cc.getSourcePort())) {
                    // only if one of the affected outports is connected:
                    NodeID destID = cc.getDest();
                    if (!destID.equals(this.getID())) {
                        // only if we have not yet reached an outport!
                        // try to find node in existing list:
                        int ix = 0;
                        for (ix = 0; ix < tempOutput.size(); ix++) {
                            if (tempOutput.get(ix).m_id.equals(destID)) {
                                break;
                            }
                        }
                        if (ix >= tempOutput.size()) {
                            // ...and it's a node not yet in our list: add it
                            tempOutput.add(new NodeAndInports(destID,
                                    cc.getDestPort(), currDepth + 1));
                        } else {
                            assert ix != currIndex;
                            // node is already in list, adjust depth to new
                            // maximum and add port if not already contained:
                            NodeAndInports nai = tempOutput.get(ix);
                            if (!nai.getInports().contains(cc.getDestPort())) {
                                nai.addInport(cc.getDestPort());
                            }
                            if (nai.getDepth() != currDepth + 1) {
                                // depth has to be smaller or equal
                                assert nai.getDepth() < currDepth + 1;
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
                            }
                        }
                    }
                }
            }
            currIndex++;
        }
        // make sure nodes are list sorted by their final depth!
        Collections.sort(tempOutput, new Comparator() {
            public int compare(final Object nai0, final Object nai1) {
                return (new Integer(((NodeAndInports)nai0).m_depth).
                        compareTo(((NodeAndInports)nai1).m_depth));
            }
        });
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
                     ((WorkflowManager)thisNode).getWorkflow().
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
    private void completeSetBackwards(final HashSet<NodeID> nodes, final NodeID id,
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
                        Set<Integer> inports = wfm.getWorkflow()
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

    /**
     * Return map of node ids to set of port indices based on list of output
     * ports. The map's iterator returns the elements sorted by traversing the
     * graph backwards, breadth first, the set of port indices represents the
     * input ports actually used within the graph covered. Include this WFM if
     * any incoming ports are connected.
     *
     * @param outportIndices set of integers indicating the ports of interest
     * @return BF sorted list of node ids
     */
    LinkedHashMap<NodeID, Set<Integer>> createBackwardsBreadthFirstSortedList(
            final Set<Integer> outportIndices) {
        // this will our result
        LinkedHashMap<NodeID, Set<Integer>> sortedNodes
                        = new LinkedHashMap<NodeID, Set<Integer>>();
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
            final LinkedHashMap<NodeID, Set<Integer>> sortedNodes,
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

    /** Helper class for lists of nodes with their inports */
    class NodeAndInports {
        private NodeID m_id;
        private int m_depth;  // indicates max depth from start node(s)
        private Set<Integer> m_inports;
        public NodeAndInports(final NodeID id, final Integer portIx,
                final int depth) {
            m_id = id;
            m_inports = new HashSet<Integer>();
            if (portIx != null) {
                m_inports.add(portIx);
            }
            m_depth = depth;
        }
        public NodeID getID() { return m_id; }
        public Set<Integer> getInports() { return m_inports; }
        public void addInport(final int ip) { m_inports.add(ip); }
        public void setDepth(final int d) { m_depth = d; }
        public int getDepth() { return m_depth; }
    }
    
    /** Return matching LoopEnd node for the given LoopStart
     * 
     * @param loopStart The requested start node (instanceof LoopStart) 
     * @throws IllegalLoopException if loop setup is wrong
     * @return id of end node or null if no such node was found.
     */
    NodeID getMatchingLoopEnd(final NodeID loopStart) 
    throws IllegalLoopException {
        return null;
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
     * @return list of nodes within loop body & any dangling branches. The list
     *   also contains the used input ports of each node.
     * @throws IllegalLoopException 
     *    If there is a ill-posed loop (dangling branches)
     *
     * @FIXME: this is an almost complete replication of the function
     *   findAllConnectedNodes - they should both call a more general
     *   implementation!
     */
    ArrayList<NodeAndInports> findAllNodesConnectedToLoopBody(
            final NodeID startNode,
            final NodeID endNode) throws IllegalLoopException {
        ArrayList<NodeAndInports> tempOutput = new ArrayList<NodeAndInports>();
        if (startNode.equals(endNode)) {
            // silly case - start = end node.
            return tempOutput;
        }
        // for the breath first search (temporarily) add start node:
        tempOutput.add(new NodeAndInports(startNode, null, 0));
        // iterate over index since we add new nodes at the end of the list
        int currIndex = 0;
        while (currIndex < tempOutput.size()) {
            NodeAndInports currNAI = tempOutput.get(currIndex);
            NodeID currID = currNAI.getID();
            int currDepth = currNAI.getDepth();
            NodeContainer currNode = m_nodes.get(currID);
            // determine set of indices of affected outports for current node
            Set<Integer> currInports = currNAI.getInports();
            Set<Integer> currOutports = new HashSet<Integer>();
            if ((currNode instanceof SingleNodeContainer)
                || (currInports == null)) {
                // simple: all outports are affected
                // (SNC or WFM without listed inports)
                for (int i = 0; i < currNode.getNrOutPorts(); i++) {
                    currOutports.add(i);
                }
            } else {
                assert currNode instanceof WorkflowManager;
                // less simple: we need to determine which outports are
                // connected to the listed inports:
                for (Integer inPortIx : currInports) {
                    Workflow currWorkflow =
                        ((WorkflowManager)currNode).getWorkflow();
                    Set<Integer> connectedOutports =
                        currWorkflow.connectedOutPorts(inPortIx);
                    currOutports.addAll(connectedOutports);
                }
            }
            // and now find immediate successors:
            for (ConnectionContainer cc : this.getConnectionsBySource(currID)) {
                assert (cc.getSource().equals(currID));
                if (currOutports.contains(cc.getSourcePort())) {
                    // only if one of the affected outports is connected:
                    NodeID destID = cc.getDest();
                    if (destID.equals(this.getID())) {
                        // if any branch leaves this WFM, complain!
                        throw new IllegalLoopException(
                            "Loops are not permitted to leave workflows!");
                    }
                    if ((!destID.equals(endNode))) {
                        // we have not yet reached the end...
                        // try to find node in existing list:
                        int ix = 0;
                        for (ix = 0; ix < tempOutput.size(); ix++) {
                            if (tempOutput.get(ix).m_id.equals(destID)) {
                                break;
                            }
                        }
                        if (ix >= tempOutput.size()) {
                            // ...we did not find it: add it to the list
                            tempOutput.add(new NodeAndInports(destID,
                                    cc.getDestPort(), currDepth + 1));
                        } else {
                            assert ix != currIndex;
                            // node is already in list, adjust depth to new
                            // maximum and add port if not already contained:
                            NodeAndInports nai = tempOutput.get(ix);
                            if (!nai.getInports().contains(cc.getDestPort())) {
                                nai.addInport(cc.getDestPort());
                            }
                            if (nai.getDepth() != currDepth + 1) {
                                // depth has to be smaller or equal
                                assert nai.getDepth() < currDepth + 1;
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
                            }
                        }
                    }
                }
            }
            currIndex += 1;
        }
        // remove start node
        NodeAndInports nai = tempOutput.remove(0);
        assert (nai.getID().equals(startNode));
        // make sure we have no branches from within the loop body reconnecting
        // to the flow after the loop end node
        HashMap<NodeID, Set<Integer>> nodesAfterEndNode =
            createBreadthFirstSortedList(Collections.singleton(endNode), true);
        for (NodeAndInports nai2 : tempOutput) {
            if (nodesAfterEndNode.containsKey(nai2.getID())) {
                throw new IllegalLoopException(
                        "Branches are not permitted to leave loops!");
            }
        }
        // make sure nodes are list sorted by their final depth!
        Collections.sort(tempOutput, new Comparator<NodeAndInports>() {
            @Override
            public int compare(
                    final NodeAndInports n0, final NodeAndInports n1) {
                return n0.m_depth - n1.m_depth;
            }
        });
        return tempOutput;
    }


}
