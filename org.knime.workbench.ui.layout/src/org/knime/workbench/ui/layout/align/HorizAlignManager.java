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
 * Created: 29.03.2011
 * Author: Peter Ohl (KNIME.com)
 */
package org.knime.workbench.ui.layout.align;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.AbstractPortFigure;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.editor2.figures.NodeInPortFigure;
import org.knime.workbench.editor2.figures.NodeOutPortFigure;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class HorizAlignManager {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(HorizAlignManager.class);

    private WorkflowManager m_wfm;

    private HashMap<NodeID, NodeUIInformation> m_oldCoordinates;

    private HashMap<ConnectionID, ConnectionUIInformation> m_oldBendpoints;

    private final NodeContainerEditPart[] m_nodeParts;

    /**
     * The constructor.
     *
     * @param wfManager contains the flow being laid out
     * @param nodes the nodes to align
     *
     */
    public HorizAlignManager(final WorkflowManager wfManager,
            final NodeContainerEditPart[] nodes) {
        m_wfm = wfManager;
        m_nodeParts = nodes.clone();
    }

    /**
     *
     */
    public void doLayout() {

        Map<NodeContainerEditPart, Integer> offsets =
                HorizAlignmentCenter.doLayout(m_wfm, m_nodeParts);

        // if emtpy - do some other alignement. May be port alignement?

        // preserver the old stuff for undoers
        m_oldBendpoints = new HashMap<ConnectionID, ConnectionUIInformation>();
        m_oldCoordinates = new HashMap<NodeID, NodeUIInformation>();

        if (offsets.isEmpty()) {
            LOGGER.debug("Nodes already aligned. Doing nothing.");
            return;
        }

        // transfer new coordinates into nodes
        for (Map.Entry<NodeContainerEditPart, Integer> e : offsets.entrySet()) {
            NodeContainerEditPart node = e.getKey();
            NodeContainer nc = node.getNodeContainer();
            NodeUIInformation uiInfo = (NodeUIInformation)nc.getUIInformation();
            int[] b = uiInfo.getBounds();
            NodeUIInformation newCoord =
                    new NodeUIInformation(b[0], b[1] + e.getValue(), b[2],
                            b[3], uiInfo.hasAbsoluteCoordinates());
            LOGGER.debug("Node " + nc + " gets alignment coordinates "
                    + newCoord);
            // save old coordinates for undo
            m_oldCoordinates.put(nc.getID(), uiInfo);

            // adjust first bendpoints at port position
            adjustBendPoints(node, e.getValue());
            // triggers gui update
            nc.setUIInformation(newCoord);
        }

    }

    private void adjustBendPoints(final NodeContainerEditPart node,
            final int offset) {
        NodeContainer nc = node.getNodeContainer();

        Set<ConnectionContainer> inConns =
                m_wfm.getIncomingConnectionsFor(nc.getID());

        for (ConnectionContainer conn : inConns) {
            ConnectionUIInformation ui =
                    (ConnectionUIInformation)conn.getUIInfo();
            if (ui == null) {
                // easy: no bend points
                continue;
            }
            // incoming connections: look at the last bend point
            int bendPointIdx = ui.getAllBendpoints().length - 1;
            if (bendPointIdx < 0) {
                continue;
            }
            int[] bendPnt = ui.getBendpoint(bendPointIdx);

            int portY = getPortMiddle(getInPortFig(node, conn.getDestPort()));
            if (portY < 0) {
                // port not found.
                continue;
            }

            if (portY + 1 < bendPnt[1] || portY - 1 > bendPnt[1]) {
                // only move bendpoints (nearly) at the same line as the port
                continue;
            }

            int[] newBendPnt = new int[]{bendPnt[0], bendPnt[1] + offset};
            // see if this connection was already moved
            if (m_oldBendpoints.get(conn.getID()) == null) {
                m_oldBendpoints.put(conn.getID(), ui);
            }
            ConnectionUIInformation newUI = ui.clone();
            newUI.removeBendpoint(bendPointIdx);
            newUI.addBendpoint(newBendPnt[0], newBendPnt[1], bendPointIdx);
            LOGGER.debug("Node: " + nc + ", replacing bendpoint " + bendPointIdx
                    + " " + bendPnt[0] + "," + bendPnt[1] + " with "
                    + newBendPnt[0] + "," + newBendPnt[1]);
            conn.setUIInfo(newUI);
        }

        Set<ConnectionContainer> outConns =
                m_wfm.getOutgoingConnectionsFor(nc.getID());
        for (ConnectionContainer conn : outConns) {
            ConnectionUIInformation ui =
                    (ConnectionUIInformation)conn.getUIInfo();
            if (ui == null || ui.getAllBendpoints().length == 0) {
                // easy: no bend points
                continue;
            }
            // outgoing connections: look at the first bend point
            int bendPointIdx = 0;
            int[] bendPnt = ui.getBendpoint(bendPointIdx);

            int portY =
                    getPortMiddle(getOutPortFig(node, conn.getSourcePort()));
            if (portY < 0) {
                // port not found.
                continue;
            }

            if (portY + 1 < bendPnt[1] || portY - 1 > bendPnt[1]) {
                // only move bendpoints (nearly) at the same line as the port
                continue;
            }

            int[] newBendPnt = new int[]{bendPnt[0], bendPnt[1] + offset};
            // see if this connection was already moved
            if (m_oldBendpoints.get(conn.getID()) == null) {
                m_oldBendpoints.put(conn.getID(), ui);
            }
            ConnectionUIInformation newUI = ui.clone();
            newUI.removeBendpoint(bendPointIdx);
            newUI.addBendpoint(newBendPnt[0], newBendPnt[1], bendPointIdx);
            LOGGER.debug("Node: " + nc + ", replacing bendpoint " + bendPointIdx
                    + " " + bendPnt[0] + "," + bendPnt[1] + " with "
                    + newBendPnt[0] + "," + newBendPnt[1]);
            conn.setUIInfo(newUI);
        }

    }

    private int getPortMiddle(final AbstractPortFigure portFig) {
        if (portFig == null) {
            return -1;
        }
        return portFig.getBounds().y + portFig.getBounds().height / 2;
    }

    private NodeOutPortFigure getOutPortFig(final NodeContainerEditPart node,
            final int idx) {
        NodeContainerFigure nodeFig = (NodeContainerFigure)node.getFigure();
        for (Object f : nodeFig.getChildren()) {
            if (f instanceof NodeOutPortFigure) {
                NodeOutPortFigure portFig = (NodeOutPortFigure)f;
                if (portFig.getPortIndex() == idx) {
                    return portFig;
                }
            }
        }
        return null;
    }

    private NodeInPortFigure getInPortFig(final NodeContainerEditPart node,
            final int idx) {
        NodeContainerFigure nodeFig = (NodeContainerFigure)node.getFigure();
        for (Object f : nodeFig.getChildren()) {
            if (f instanceof NodeInPortFigure) {
                NodeInPortFigure portFig = (NodeInPortFigure)f;
                if (portFig.getPortIndex() == idx) {
                    return portFig;
                }
            }
        }
        return null;
    }

    public Map<NodeID, NodeUIInformation> getOldNodeCoordinates() {
        return Collections.unmodifiableMap(m_oldCoordinates);
    }

    public Map<ConnectionID, ConnectionUIInformation> getOldBendpoints() {
        return Collections.unmodifiableMap(m_oldBendpoints);
    }
}
