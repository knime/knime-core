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
 * Created: 28.03.2011
 * Author: Martin Mader
 */
package org.knime.workbench.ui.layout;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.ui.layout.Graph.Edge;
import org.knime.workbench.ui.layout.Graph.Node;
import org.knime.workbench.ui.layout.layeredlayout.SimpleLayeredLayouter;

/**
 *
 * @author mader, University of Konstanz
 */
public class LayoutManager {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LayoutManager.class);

    private WorkflowManager m_wfm;

    private long m_initPlacementSeed;

    private HashMap<NodeContainer, Node> m_workbenchToGraphNodes;

    private HashMap<ConnectionContainer, Edge> m_workbenchToGraphEdges;

    // nodes not laid out - but connected to nodes being laid out
    private HashMap<NodeContainer, Node> m_workbenchIncomingNodes;

    // nodes not laid out - but connected to nodes being laid out
    private HashMap<NodeContainer, Node> m_workbenchOutgoingNodes;

    // Meta node incoming port indices connected to nodes being laid out
    private HashMap<Integer, Node> m_workbenchWFMInports;

    // Meta node outgoing port indices connected to nodes being laid out
    private HashMap<Integer, Node> m_workbenchWFMOutports;

    /* the graph stores only one edge between two nodes. The connections
     * represented are in the list.
     */
    private HashMap<Edge, List<ConnectionContainer>> m_parallelConns;

    private Graph m_g;

    private HashMap<NodeID, NodeUIInformation> m_oldCoordinates;

    private HashMap<ConnectionID, ConnectionUIInformation> m_oldBendpoints;

    /**
     * The constructor.
     *
     * @param wfManager contains the flow being laid out
     */
    public LayoutManager(final WorkflowManager wfManager,
            final long initialPlacementSeed) {
        m_wfm = wfManager;
        m_initPlacementSeed = initialPlacementSeed;
        m_workbenchToGraphNodes = new HashMap<NodeContainer, Graph.Node>();
        m_workbenchToGraphEdges =
                new HashMap<ConnectionContainer, Graph.Edge>();
        m_workbenchIncomingNodes = new HashMap<NodeContainer, Graph.Node>();
        m_workbenchOutgoingNodes = new HashMap<NodeContainer, Graph.Node>();
        m_workbenchWFMInports = new HashMap<Integer, Graph.Node>();
        m_workbenchWFMOutports = new HashMap<Integer, Graph.Node>();
        m_parallelConns = new HashMap<Edge, List<ConnectionContainer>>();
        m_g = new Graph();
    }

    /**
     * @param nodes the nodes that should be laid out. If null, all nodes of the
     *            workflow manager passed to the constructor are laid out.
     *
     */
    public void doLayout(final Collection<NodeContainer> nodes) {

        int X_STRETCH = 100;
        int Y_STRETCH = 120;

        if (WorkflowEditor.getActiveEditorSnapToGrid()) {
            if (WorkflowEditor.getActiveEditorGridX() >= 70) {
                X_STRETCH = WorkflowEditor.getActiveEditorGridX();
            } else {
                X_STRETCH = WorkflowEditor.getActiveEditorGridXOffset(X_STRETCH);
            }
            Y_STRETCH = WorkflowEditor.getActiveEditorGridYOffset(Y_STRETCH);
        }
        // add all nodes that should be laid out to the graph
        Collection<NodeContainer> allNodes = nodes;
        if (allNodes == null || allNodes.size() <= 1) {
            allNodes = m_wfm.getNodeContainers();
        }
        // keep the left upper corner of the node cluster.
        // Nodes laid out are placed right and below
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        // add all nodes that are to be laid out
        for (NodeContainer nc : allNodes) {
            Node gNode = createGraphNodeForNC(nc);
            m_workbenchToGraphNodes.put(nc, gNode);
            NodeUIInformation ui = nc.getUIInformation();
            minX = (ui.getBounds()[0] < minX) ? ui.getBounds()[0] : minX;
            minY = (ui.getBounds()[1] < minY) ? ui.getBounds()[1] : minY;
            if (WorkflowEditor.getActiveEditorSnapToGrid()) {
                Point nextGridLocation = WorkflowEditor.getActiveEditorNextGridLocation(new Point(minX, minY));
                minX = nextGridLocation.x;
                minY = nextGridLocation.y;
            }
        }

        // find all connections that connect from/to our nodes,
        // keep a flag that states: isClusterInternal
        HashMap<ConnectionContainer, Boolean> allConns =
                new HashMap<ConnectionContainer, Boolean>();
        for (ConnectionContainer conn : m_wfm.getConnectionContainers()) {
            Node src = null;
            if (!conn.getSource().equals(m_wfm.getID())) {
                // if it's not a meta node incoming connection
                src =
                        m_workbenchToGraphNodes.get(m_wfm.getNodeContainer(conn
                                .getSource()));
            }
            Node dest = null;
            if (!conn.getDest().equals(m_wfm.getID())) {
                // if it is not a meta node outgoing connection
                dest =
                        m_workbenchToGraphNodes.get(m_wfm.getNodeContainer(conn
                                .getDest()));

            }
            boolean isInternal = (src != null && dest != null);
            // if at least one node is auto laid out we need the connection
            if (src != null || dest != null) {
                allConns.put(conn, isInternal);
            }
        }

        // Add all connections (internal and leading in/out the cluster)
        // to the graph
        Edge gEdge;
        for (ConnectionContainer conn : allConns.keySet()) {
            Node srcGraphNode;
            Node destGraphNode;
            if (conn.getSource().equals(m_wfm.getID())) {
                // it connects to a meta node input port:
                int portIdx = conn.getSourcePort();
                srcGraphNode = m_workbenchWFMInports.get(portIdx);
                if (srcGraphNode == null) {
                    srcGraphNode =
                            m_g.createNode("Incoming " + portIdx, 0, portIdx
                                    * Y_STRETCH);
                    m_workbenchWFMInports.put(portIdx, srcGraphNode);
                }
            } else {
                NodeContainer s = m_wfm.getNodeContainer(conn.getSource());
                srcGraphNode = m_workbenchToGraphNodes.get(s);
                if (srcGraphNode == null) {
                    // then it connects to an "outside" node
                    srcGraphNode = m_workbenchIncomingNodes.get(s);
                    if (srcGraphNode == null) {
                        srcGraphNode = createGraphNodeForNC(s);
                        m_workbenchIncomingNodes.put(s, srcGraphNode);
                    }
                } // else it is a connection inside the layout cluster
            }
            if (conn.getDest().equals(m_wfm.getID())) {
                // it connects to a meta node output port
                int portIdx = conn.getDestPort();
                destGraphNode = m_workbenchWFMOutports.get(portIdx);
                if (destGraphNode == null) {
                    destGraphNode =
                            m_g.createNode("Outgoing " + portIdx, 250, portIdx
                                    * Y_STRETCH);
                    m_workbenchWFMOutports.put(portIdx, destGraphNode);
                }
            } else {
                NodeContainer d = m_wfm.getNodeContainer(conn.getDest());
                destGraphNode = m_workbenchToGraphNodes.get(d);
                if (destGraphNode == null) {
                    // then it connects to an "outside" node
                    destGraphNode = m_workbenchOutgoingNodes.get(d);
                    if (destGraphNode == null) {
                        destGraphNode = createGraphNodeForNC(d);
                        m_workbenchOutgoingNodes.put(d, destGraphNode);
                    }
                } // else it is a connection within the layout cluster
            }

            gEdge = m_g.createEdge(srcGraphNode, destGraphNode);
            if (gEdge != null) {
                m_workbenchToGraphEdges.put(conn, gEdge);
                m_parallelConns.put(gEdge, new LinkedList<ConnectionContainer>(
                        Collections.singletonList(conn)));
            } else {
                // a connection between these node already exists in the graph
                Edge graphEdge = srcGraphNode.getEdge(destGraphNode);
                assert graphEdge != null;
                // add the connection to list of parallel connections.
                m_parallelConns.get(graphEdge).add(conn);
            }
        }

        // AFTER creating all nodes, mark the incoming/outgoing nodes as fixed
        boolean anchorsExist = false;
        Map<Node, Boolean> anchorNodes = m_g.createBoolNodeMap();
        for (Node n : m_workbenchIncomingNodes.values()) {
            anchorsExist = true;
            anchorNodes.put(n, Boolean.TRUE);
        }
        for (Node n : m_workbenchOutgoingNodes.values()) {
            anchorsExist = true;
            anchorNodes.put(n, Boolean.TRUE);
        }
        for (Node n : m_workbenchWFMInports.values()) {
            anchorsExist = true;
            anchorNodes.put(n, Boolean.TRUE);
        }
        for (Node n : m_workbenchWFMOutports.values()) {
            anchorsExist = true;
            anchorNodes.put(n, Boolean.TRUE);
        }

        SimpleLayeredLayouter layouter = new SimpleLayeredLayouter(m_initPlacementSeed);
        layouter.setBalanceBranchings(!WorkflowEditor.getActiveEditorSnapToGrid());
        if (anchorsExist) {
            layouter.doLayout(m_g, anchorNodes);
        } else {
            layouter.doLayout(m_g, null);
        }

        // preserver the old stuff for undoers
        m_oldBendpoints = new HashMap<ConnectionID, ConnectionUIInformation>();
        m_oldCoordinates = new HashMap<NodeID, NodeUIInformation>();

        // transfer new coordinates back to nodes

        // with fixed nodes (lots of) the new coordinates of the nodes may not
        // start at 0.
        double coordOffsetX = Integer.MAX_VALUE;
        double coordOffsetY = Integer.MAX_VALUE;
        for (NodeContainer nc : allNodes) {
            Node gNode = m_workbenchToGraphNodes.get(nc);
            coordOffsetX = Math.min(coordOffsetX, m_g.getX(gNode));
            coordOffsetY = Math.min(coordOffsetY, m_g.getY(gNode));
        }
        for (NodeContainer nc : allNodes) {

            NodeUIInformation uiInfo = nc.getUIInformation();
            if (uiInfo != null) {
                Node gNode = m_workbenchToGraphNodes.get(nc);
                int[] b = uiInfo.getBounds();
                int x = (int)Math.round((m_g.getX(gNode) - coordOffsetX)
                        * X_STRETCH) + minX;
                int y = (int)Math.round((m_g.getY(gNode) - coordOffsetY)
                        * Y_STRETCH) + minY;
                NodeUIInformation newCoord =
                        new NodeUIInformation(x, y, b[2], b[3],
                                uiInfo.hasAbsoluteCoordinates());
                newCoord.setSnapToGrid(WorkflowEditor.getActiveEditorSnapToGrid());
                LOGGER.debug("Node " + nc + " gets auto-layout coordinates "
                        + newCoord);
                // save old coordinates for undo
                m_oldCoordinates.put(nc.getID(), uiInfo);
                // triggers gui update
                nc.setUIInformation(newCoord);
            }
        }

        // delete old bendpoints - transfer new ones
        for (ConnectionContainer conn : allConns.keySet()) {

            // store old bendpoint for undo
            ConnectionUIInformation ui = conn.getUIInfo();
            if (ui != null) {
                m_oldBendpoints.put(conn.getID(), ui);
            } else {
                m_oldBendpoints.put(conn.getID(), null);
            }

            ConnectionUIInformation newUI = new ConnectionUIInformation();
            Edge e = m_workbenchToGraphEdges.get(conn);
            if (e == null) {
                // a parallel connection not represented by the edge
                continue;
            }

            List<ConnectionContainer> conns = m_parallelConns.get(e);
            assert conns.size() > 0;
            assert conns.get(0) == conn; // that is how we created it!

            ArrayList<Point2D> newBends = m_g.bends(e);
            if (newBends != null && !newBends.isEmpty()) {
                int extraX = 16; // half the node icon size...
                int extraY = 24;
                for (int i = 0; i < newBends.size(); i++) {
                    Point2D b = newBends.get(i);
                    newUI.addBendpoint((int)Math.round((b.getX() - coordOffsetX) * X_STRETCH) + extraX + minX,
                            (int)Math.round((b.getY() - coordOffsetY) * Y_STRETCH) + extraY + minY, i);
                }
            }
            conn.setUIInfo(newUI);

            // compute bendpoints for parallel connections (slightly offset)
            for (int i = 1; i < conns.size(); i++) { // idx 0 == conn!
                ConnectionContainer parConn = conns.get(i);
                // destination port determines offset
                int yOffset = (parConn.getDestPort() - conn.getDestPort()) * 10;
                ConnectionUIInformation parUI =
                    newUI.createNewWithOffsetPosition(new int[] {0, yOffset});
                parConn.setUIInfo(parUI);
            }
        }

    }

    /**
     * Creates a new graph node with the coordinates from the UI info and the
     * label set to custom name.
     *
     * @param nc
     * @return
     */
    private Node createGraphNodeForNC(final NodeContainer nc) {
        NodeUIInformation uiInfo = nc.getUIInformation();
        int x = 0;
        int y = 0;
        String label = nc.getCustomName();
        if (label == null || label.isEmpty()) {
            label = "Node " + nc.getID().toString();
        }
        if (uiInfo != null) {
            int[] bounds = uiInfo.getBounds();
            x = bounds[0];
            y = bounds[1];
            return m_g.createNode(label, x, y);
        } else {
            return m_g.createNode(label);
        }

    }

    public Map<NodeID, NodeUIInformation> getOldNodeCoordinates() {
        return Collections.unmodifiableMap(m_oldCoordinates);
    }

    public Map<ConnectionID, ConnectionUIInformation> getOldBendpoints() {
        return Collections.unmodifiableMap(m_oldBendpoints);
    }
}
