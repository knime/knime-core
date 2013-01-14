/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.knime.core.node.Node.LoopRole;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.util.Pair;

/** Container class wrapping wrapping the network of nodes forming
 * a workflow together with some of the basic functionality, especially
 * traversal methods.
 *
 * @author M. Berthold, University of Konstanz
 */
class Workflow {

    /** mapping from NodeID to Nodes. */
    private final TreeMap<NodeID, NodeContainer> m_nodes =
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
     * @return tree map of NodeID/NodeContainers.
     */
    TreeMap<NodeID, NodeContainer> getNodeMap() {
        return m_nodes;
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

    /** Return map of node ids connected to the given outpot of the given node
     * sorted in breadth first order mapped to a set of portIDs.
     * See also {@link #getBreadthFirstListOfNodeAndSuccessors()}
     *
     * @param id of node
     * @param outPortIndex of node
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

    /** Complete set of nodes depth-first starting with node id. If the given
     * node is already in the set, nothing happens. Note that this function
     * does not pursue connections leaving this workflow - we will only add
     * our own ID (the workflow) as "last" node in the chain.
     *
     * @param nodes set of nodes to be completed
     * @param id of node to start search from
     * @param index of port the incoming connection connected to
     *   (useful when start node is a metanode!)
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
                completeSet(nodes, nextNodeID, cc.getDestPort());
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
        // don't add parent to list throughout search to avoid
        // infinite loops (i.e. starting with incoming connections again
        // but if encountered remember to node&ports at the end of the search:
        Set<Integer> parentOutgoingPorts = new HashSet<Integer>();
        // keep adding nodes until we can't find new ones anymore
        for (int i = 0; i < bfsSortedNodes.size(); i++) {
            // Not a very nice way to iterate over the keys of a map...
            //   (but since we constantly add to it in this loop?!)
            Object[] ani = bfsSortedNodes.keySet().toArray();
            NodeID currNode = (NodeID)(ani[i]);
            Set<Integer> currInPorts = bfsSortedNodes.get(currNode);
            Set<Integer> currOutPorts = new HashSet<Integer>();
            NodeContainer currNC = getNode(currNode);
            if ((currNC != null) && (currNC instanceof WorkflowManager)) {
                for (int in : currInPorts) {
                     Set<Integer> outs = ((WorkflowManager)currNC).getWorkflow().connectedOutPorts(in);
                     currOutPorts.addAll(outs);
                }
            }
            // look at all successors of this node
            for (ConnectionContainer cc : m_connectionsBySource.get(currNode)) {
                if (currOutPorts.isEmpty() || currOutPorts.contains(cc.getSourcePort())) {
                    NodeID succNode = cc.getDest();
                    if (this.getID().equals(succNode)) {
                        parentOutgoingPorts.add(cc.getDestPort());
                    } else {
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
                                        // not WFMIN but source is in our list:
                                        // needs to be remembered as "incoming"
                                        // port within this BF search.
                                        incomingPorts.add(cc2.getDestPort());
                                    }
                                }
                            }
                            if (allContained) {
                                // if all predecessors are already in the BFS list
                                // (or not to be considered): add it!
                                bfsSortedNodes.put(succNode, incomingPorts);
                            }
                        }
                    }
                }
            }
        }
        // and finally add parents if any connections were found:
        if (parentOutgoingPorts.size() > 0) {
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
    		Set<ConnectionContainer> incomingConns
    		= m_connectionsByDest.get(thisNode);
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

    /** Return list of nodes directly connected to given port or unconnected
     * start nodes if inPort = -1.
     *
     * @param inPort port index of -1 if no port given.
     * @return set of directly connected nodes or source nodes.
     */
    HashMap<NodeID, Integer> getStartNodes(final int inPort) {
    	LinkedHashMap<NodeID, Integer> result
    	                = new LinkedHashMap<NodeID, Integer>();
    	if (inPort < 0) {
    		Set<NodeID> sources = getSourceNodes(m_nodes.keySet());
    		for (NodeID id : sources) {
    			result.put(id, -1);
    		}
    	} else {
            for (ConnectionContainer cc : m_connectionsBySource.get(getID())) {
                if (cc.getSourcePort() == inPort) {
                    result.put(cc.getDest(), cc.getDestPort());
                }
            }
    	}
    	return result;
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
            // Not a very nice way to iterate over this set but
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
            if ((startPorts == null)
                        || (startPorts.contains(cc.getSourcePort()))) {
                NodeID nextID = cc.getDest();
                if (nextID.equals(endID)) {
                    // don't add the end node!
                } else if (nextID.equals(this.getID())) {
                    // don't record outgoing connections
                    if (startID.equals(this.getID())) {
                        assert cc.getType().
                         equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
                    } else {
                        assert cc.getType().
                           equals(ConnectionContainer.ConnectionType.WFMOUT);
                    }
                } else {
                    tempOutput.add(new NodeAndInports(cc.getDest(),
                            cc.getDestPort(), /*depth=*/0));
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
                    Workflow currWorkflow
                                  = ((WorkflowManager)currNode).getWorkflow();
                    Set<Integer> connectedOutports
                                   = currWorkflow.connectedOutPorts(inPortIx);
                    currOutports.addAll(connectedOutports);
                }
            }
            for (ConnectionContainer cc : this.getConnectionsBySource(currID)) {
                if (currOutports.contains(cc.getSourcePort())) {
                    // only if one of the affected outports is connected:
                    NodeID destID = cc.getDest();
                    if ((!destID.equals(this.getID()))
                            && (!destID.equals(endID))) {
                        // only if we have not yet reached an outport or
                        // the "end" node
                        // try to find node in existing list:
                        int ix = 0;
                        for (ix = 0; ix < tempOutput.size(); ix++) {
                            if (tempOutput.get(ix).m_nodeId.equals(destID)) {
                                break;
                            }
                        }
                        if (ix >= tempOutput.size()) {
                            // ...it's a node not yet in our list: add it
                            tempOutput.add(new NodeAndInports(destID,
                                    cc.getDestPort(), currDepth + 1));
                        } else {
                            assert ix != currIndex;
                            // node is already in list, adjust depth to new
                            // maximum and add port if not already contained:
                            NodeAndInports nai = tempOutput.get(ix);
                            if (!nai.getInports().contains(cc.getDestPort())) {
                                nai.addInport(cc.getDestPort());
                            } else {
                                // entries that we already have
                                // (parallel branches can cause this)
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
            // Not a very nice way to iterate over this set but
            // since we are adding things to it inside the loop?!
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
    private void completeSetBackwards(final HashSet<NodeID> nodes,
            final NodeID id,
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
        // this will be our result
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
            // Not a very nice way to iterate over the keys of a map...
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
            result[i] = new MetaPortInfo(portType, isConnected, message, i);
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
            for (ConnectionContainer cc : wfmFlow.getConnectionsByDest(
                    metaNodeID)) {
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
                    message = "Connected to one upstream node and "
                        + outsideCount + " downstream node(s)";
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
            result[i] = new MetaPortInfo(portType, isConnected, message, i);
        }
        return result;
    }

    /**
     * @param newPorts */
    List<Pair<ConnectionContainer, ConnectionContainer>>
        changeDestinationPortsForMetaNode(final NodeID metaNodeID,
                final MetaPortInfo[] newPorts) {
        // argument node is either a contained meta node or this wfm itself
        // (latter only when updating outgoing connections)
        assert metaNodeID.equals(getID()) || m_nodes.get(metaNodeID) instanceof WorkflowManager;
        List<Pair<ConnectionContainer, ConnectionContainer>> result =
            new ArrayList<Pair<ConnectionContainer, ConnectionContainer>>();
        final Set<ConnectionContainer> connectionsToMetaNode = m_connectionsByDest.get(metaNodeID);
        for (ConnectionContainer cc : connectionsToMetaNode) {
            int destPort = cc.getDestPort();
            boolean hasBeenFound = false;
            for (MetaPortInfo mpi : newPorts) {
                if (mpi.getOldIndex() == destPort) {
                    hasBeenFound = true;
                    if (mpi.getNewIndex() != destPort) {
                        ConnectionContainer newConn = new ConnectionContainer(cc.getSource(),
                                cc.getSourcePort(), metaNodeID, mpi.getNewIndex(), cc.getType());
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

    /**
     * @param metaNodeID
     * @param newPorts
     * @return */
    List<Pair<ConnectionContainer, ConnectionContainer>>
        changeSourcePortsForMetaNode(final NodeID metaNodeID,
                final MetaPortInfo[] newPorts) {
        // argument node is either a contained meta node or this wfm itself
        // (latter only when updating outgoing connections)
        assert metaNodeID.equals(getID()) || m_nodes.get(metaNodeID) instanceof WorkflowManager;
        List<Pair<ConnectionContainer, ConnectionContainer>> result =
            new ArrayList<Pair<ConnectionContainer, ConnectionContainer>>();
        final Set<ConnectionContainer> connectionsFromMetaNode = m_connectionsBySource.get(metaNodeID);
        for (ConnectionContainer cc : connectionsFromMetaNode) {
            int sourcePort = cc.getSourcePort();
            boolean hasBeenFound = false;
            for (MetaPortInfo mpi : newPorts) {
                if (mpi.getOldIndex() == sourcePort) {
                    hasBeenFound = true;
                    if (mpi.getNewIndex() != sourcePort) {
                        ConnectionContainer newConn = new ConnectionContainer(metaNodeID,
                                mpi.getNewIndex(), cc.getDest(), cc.getDestPort(), cc.getType());
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
    }

    /** Return matching LoopEnd node for the given LoopStart.
     *
     * @param id The requested start node (instanceof LoopStart)
     * @throws IllegalLoopException if loop setup is wrong
     * @throws IllegalArgumentException if argument is not a LoopStart node
     * @return id of end node or null if no such node was found.
     */
    NodeID getMatchingLoopEnd(final NodeID id)
    throws IllegalLoopException {
        NodeContainer nc = getNode(id);
        if (!(nc instanceof SingleNodeContainer)) {
            throw new IllegalArgumentException("Not a Loop Start Node " + id);
        }
        SingleNodeContainer snc = (SingleNodeContainer)nc;
        if (!LoopRole.BEGIN.equals(snc.getLoopRole())) {
            throw new IllegalArgumentException("Not a Loop Start Node " + id);
        }
        NodeID foundEnd = null;
        // create stack for Breitensuche: also store the level of loop nesting
        Stack<Pair<NodeID, Integer>> st = new Stack<Pair<NodeID, Integer>>();
        st.push(new Pair<NodeID, Integer>(id, 0));
        while (!st.isEmpty()) {
            Pair<NodeID, Integer> p = st.pop();
            NodeID currentID = p.getFirst();
            int currentDepth = p.getSecond();
            for (ConnectionContainer cc
                    : m_connectionsBySource.get(currentID)) {
                assert currentID.equals(cc.getSource());
                NodeID destID = cc.getDest();
                if (this.getID().equals(destID)) {
                    throw new IllegalLoopException("Loops can not leave"
                                                             + " workflow!");
                }
                NodeContainer destNC = getNode(destID);
                if (destNC instanceof SingleNodeContainer) {
                    SingleNodeContainer destSNC = (SingleNodeContainer)destNC;
                    if (LoopRole.END.equals(destSNC.getLoopRole())) {
                        if (currentDepth == 0) {
                            if ((foundEnd != null)
                                    && (!foundEnd.equals(destID))) {
                                // we can reach it twice but we should never
                                // reach another end node!
                                throw new IllegalLoopException("Loops can not"
                                      + " connect to more than one End Node!");
                            }
                            foundEnd = destID;
                            continue;
                        } else {
                            currentDepth--;
                        }
                    }
                    if (LoopRole.BEGIN.equals(destSNC.getLoopRole())) {
                        currentDepth++;
                    }
                }
                st.push(new Pair<NodeID, Integer>(destID, currentDepth));
            }
        }
        return foundEnd;
    }

    /** Return matching LoopStart node for the given LoopEnd.
     *
     * @param id The requested end node (instanceof LoopEnd)
     * @throws IllegalLoopException if loop setup is wrong
     * @throws IllegalArgumentException if argument is not a LoopEnd node
     * @return id of start node or null if no such node was found.
     */
    NodeID getMatchingLoopStart(final NodeID id)
    throws IllegalLoopException {
        NodeContainer nc = getNode(id);
        if (!(nc instanceof SingleNodeContainer)) {
            throw new IllegalArgumentException("Not a Loop End Node " + id);
        }
        SingleNodeContainer snc = (SingleNodeContainer)nc;
        if (!LoopRole.END.equals(snc.getLoopRole())) {
            throw new IllegalArgumentException("Not a Loop End Node " + id);
        }
        NodeID foundStart = null;
        // create stack for Breitensuche: also store the level of loop nesting
        Stack<Pair<NodeID, Integer>> st = new Stack<Pair<NodeID, Integer>>();
        st.push(new Pair<NodeID, Integer>(id, 0));
        while (!st.isEmpty()) {
            Pair<NodeID, Integer> p = st.pop();
            NodeID currentID = p.getFirst();
            for (ConnectionContainer cc
                    : m_connectionsByDest.get(currentID)) {
                assert currentID.equals(cc.getDest());
                int currentDepth = p.getSecond();
                NodeID srcID = cc.getSource();
                if (this.getID().equals(srcID)) {
                    // ignore connections from outside the workflow (they
                    // can still feed data into an existing loop but the
                    // head of the loop can not be outside this WFM!).
                    continue;
                }
                NodeContainer srcNC = getNode(srcID);
                if (srcNC instanceof SingleNodeContainer) {
                    SingleNodeContainer srcSNC = (SingleNodeContainer)srcNC;
                    if (LoopRole.BEGIN.equals(srcSNC.getLoopRole())) {
                        if (currentDepth == 0) {
                            if ((foundStart != null)
                                && (!foundStart.equals(srcID))) {
                                    // we can reach it twice but we should never
                                    // reach another end node!
                                throw new IllegalLoopException("Loops can not"
                                    + " have more than one Start Node!");
                            }
                            foundStart = srcID;
                            continue;
                        } else {
                            currentDepth--;
                        }
                    }
                    if (LoopRole.END.equals(srcSNC.getLoopRole())) {
                        currentDepth++;
                    }
                }
                st.push(new Pair<NodeID, Integer>(srcID, currentDepth));
            }
        }
        return foundStart;
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
     */
    ArrayList<NodeAndInports> findAllNodesConnectedToLoopBody(
            final NodeID startNode,
            final NodeID endNode) throws IllegalLoopException {
        ArrayList<NodeAndInports> tempOutput =
                     findAllNodesInbetween(startNode, null, endNode);
        if (startNode.equals(endNode)) {
            // silly case - start = end node.
            return tempOutput;
        }
        // check that no connection from within the loop leaves workflow:
        for (NodeAndInports nai : tempOutput) {
            if (nai.getID().equals(this.getID())) {
                // if any branch leaves this WFM, complain!
                throw new IllegalLoopException(
                    "Loops are not permitted to leave workflows!");
            }
        }
        // make sure we have no branches from within the loop body reconnecting
        // to the flow after the loop end node (= skipping over loop end)
        HashMap<NodeID, Set<Integer>> nodesAfterEndNode =
            createBreadthFirstSortedList(Collections.singleton(endNode), true);
        for (NodeAndInports nai2 : tempOutput) {
            if (nodesAfterEndNode.containsKey(nai2.getID())) {
                throw new IllegalLoopException(
                        "Branches are not permitted to leave loops!");
            }
        }
        return tempOutput;
    }

}
