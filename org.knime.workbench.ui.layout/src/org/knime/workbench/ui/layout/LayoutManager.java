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
 * Created: 28.03.2011
 * Author: Martin Mader
 */
package org.knime.workbench.ui.layout;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.UIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
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

    private HashMap<NodeContainer, Node> m_workbenchToGraphNodes;

    private HashMap<ConnectionContainer, Edge> m_workbenchToGraphEdges;

    // nodes not laid out - but connected to nodes being laid out
    private HashMap<NodeContainer, Node> m_workbenchIncomingNodes;

    // nodes not laid out - but connected to nodes being laid out
    private HashMap<NodeContainer, Node> m_workbenchOutgoingNodes;

    // Meta node incoming port indices
    private HashMap<Integer, Node> m_workbenchWFMInports;

    // Meta node outgoing port indices
    private HashMap<Integer, Node> m_workbenchWFMOutports;

    private Graph m_g;

    private HashMap<NodeID, NodeUIInformation> m_oldCoordinates;

    private HashMap<ConnectionID, ConnectionUIInformation> m_oldBendpoints;

    /**
     * The constructor.
     *
     * @param wfManager contains the flow being laid out
     */
    public LayoutManager(final WorkflowManager wfManager) {
        m_wfm = wfManager;
        m_workbenchToGraphNodes = new HashMap<NodeContainer, Graph.Node>();
        m_workbenchToGraphEdges =
                new HashMap<ConnectionContainer, Graph.Edge>();
        m_workbenchIncomingNodes = new HashMap<NodeContainer, Graph.Node>();
        m_workbenchOutgoingNodes = new HashMap<NodeContainer, Graph.Node>();
        m_workbenchWFMInports = new HashMap<Integer, Graph.Node>();
        m_workbenchWFMOutports = new HashMap<Integer, Graph.Node>();
        m_g = new Graph();
    }

    /**
     * @param nodes the nodes that should be laid out. If null, all nodes of the
     *            workflow manager passed to the constructor are laid out.
     *
     */
    public void doLayout(final Collection<NodeContainer> nodes) {

        final double X_STRETCH = NodeContainerFigure.WIDTH * 1.5;
        final int X_OFFSET = 25;
        final double Y_STRETCH = NodeContainerFigure.HEIGHT * 2;
        final int Y_OFFSET = 25;

        // add all nodes that should be laid out to the graph
        Collection<NodeContainer> allNodes = nodes;
        if (allNodes == null) {
            allNodes = m_wfm.getNodeContainers();
        }
        for (NodeContainer nc : allNodes) {
            Node gNode = createGraphNodeForNC(nc);
            m_workbenchToGraphNodes.put(nc, gNode);
        }

        // add all connections
        Collection<ConnectionContainer> allConns =
                m_wfm.getConnectionContainers();
        Edge gEdge;
        for (ConnectionContainer conn : allConns) {
            NodeContainer s = m_wfm.getNodeContainer(conn.getSource());
            NodeContainer d = m_wfm.getNodeContainer(conn.getDest());
            Node srcGraphNode;
            Node destGraphNode;
            if (s == m_wfm) {
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
            if (d == m_wfm) {
                // it connects to a meta node output port
                int portIdx = conn.getDestPort();
                destGraphNode = m_workbenchWFMOutports.get(portIdx);
                if (destGraphNode == null) {
                    destGraphNode =
                            m_g.createNode("Outgoing " + portIdx, 250, portIdx
                                    * Y_STRETCH);
                    m_workbenchWFMOutports.put(portIdx, srcGraphNode);
                }
            } else {
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
            m_workbenchToGraphEdges.put(conn, gEdge);
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

        if (anchorsExist) {
            new SimpleLayeredLayouter().doLayout(m_g, anchorNodes);
        } else {
            new SimpleLayeredLayouter().doLayout(m_g, null);
        }

        // preserver the old stuff for undoers
        m_oldBendpoints = new HashMap<ConnectionID, ConnectionUIInformation>();
        m_oldCoordinates = new HashMap<NodeID, NodeUIInformation>();

        // transfer new coordinates back to nodes
        for (NodeContainer nc : allNodes) {

            UIInformation uiInfo = nc.getUIInformation();
            if (uiInfo != null && uiInfo instanceof NodeUIInformation) {
                Node gNode = m_workbenchToGraphNodes.get(nc);
                NodeUIInformation nui = (NodeUIInformation)uiInfo;
                int[] b = nui.getBounds();
                int x = (int)Math.round(m_g.getX(gNode) * X_STRETCH) + X_OFFSET;
                int y = (int)Math.round(m_g.getY(gNode) * Y_STRETCH) + Y_OFFSET;
                NodeUIInformation newCoord =
                        new NodeUIInformation(x, y, b[2], b[3],
                                nui.hasAbsoluteCoordinates());
                LOGGER.debug("Node " + nc + " gets auto-layout coordinates "
                        + newCoord);
                // save old coordinates for undo
                m_oldCoordinates.put(nc.getID(), nui);
                // triggers gui update
                nc.setUIInformation(newCoord);
            }
        }

        // delete old bendpoints - transfer new ones
        for (ConnectionContainer conn : allConns) {
            // store old bendpoint for undo
            UIInformation ui = conn.getUIInfo();
            if (ui != null && ui instanceof ConnectionUIInformation) {
                ConnectionUIInformation cui = (ConnectionUIInformation)ui;
                m_oldBendpoints.put(conn.getID(), cui);
            } else {
                m_oldBendpoints.put(conn.getID(), null);
            }

            ConnectionUIInformation newUI = new ConnectionUIInformation();
            Edge e = m_workbenchToGraphEdges.get(conn);
            ArrayList<Point2D> newBends = m_g.bends(e);
            if (newBends != null && !newBends.isEmpty()) {
                // add more X to every other bendpoint
                int extraX = 16; // half the node icon size...
                int extraY = 24;
                for (int i = 0; i < newBends.size(); i++) {
                    Point2D b = newBends.get(i);
                    newUI.addBendpoint((int)Math.round(b.getX() * X_STRETCH)
                            + X_OFFSET + extraX,
                            (int)Math.round(b.getY() * Y_STRETCH) + Y_OFFSET
                                    + extraY, i);
                }
            }
            conn.setUIInfo(newUI);
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
        UIInformation uiInfo = nc.getUIInformation();
        int x = 0;
        int y = 0;
        String label = nc.getCustomName();
        if (label == null || label.isEmpty()) {
            label = "Node " + nc.getID().getIDWithoutRoot();
        }
        if (uiInfo != null && uiInfo instanceof NodeUIInformation) {
            NodeUIInformation nui = (NodeUIInformation)uiInfo;
            int[] bounds = nui.getBounds();
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
