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

import java.util.Collection;
import java.util.HashMap;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.UIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.ui.layout.Graph.Node;

/**
 *
 * @author mader, University of Konstanz
 */
public class LayoutManager {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LayoutManager.class);

    private WorkflowManager m_wfm;

    private HashMap<NodeContainer, Node> m_workbenchToGraphNodes;

    private Graph m_g;

    /**
     * Conschtruggdor.
     *
     * @param wfManager contains the flow being laid out
     */
    public LayoutManager(final WorkflowManager wfManager) {
        m_wfm = wfManager;
        m_workbenchToGraphNodes = new HashMap<NodeContainer, Graph.Node>();
        m_g = new Graph();
    }

    /**
     *
     */
    public void doLayout() {

        // add all nodes (no input /output ports yet (meta nodes))
        Collection<NodeContainer> allNodes = m_wfm.getNodeContainers();
        for (NodeContainer nc : allNodes) {
            UIInformation uiInfo = nc.getUIInformation();
            int x = 0;
            int y = 0;
            Node gNode;
            if (uiInfo != null && uiInfo instanceof NodeUIInformation) {
                NodeUIInformation nui = (NodeUIInformation)uiInfo;
                int[] bounds = nui.getBounds();
                x = bounds[0];
                y = bounds[1];
                gNode = m_g.createNode(x, y);
            } else {
                gNode = m_g.createNode();
            }
            m_workbenchToGraphNodes.put(nc, gNode);
        }

        // add all connections
        Collection<ConnectionContainer> allConns =
                m_wfm.getConnectionContainers();
        for (ConnectionContainer conn : allConns) {
            m_g.createEdge(m_workbenchToGraphNodes.get(conn.getSource()),
                    m_workbenchToGraphNodes.get(conn.getDest()));
        }

        new SimpleLayouter().doLayout(m_g);

        // transfer new coordinates back to nodes
        for (NodeContainer nc : allNodes) {
            UIInformation uiInfo = nc.getUIInformation();
            if (uiInfo != null && uiInfo instanceof NodeUIInformation) {
                Node gNode = m_workbenchToGraphNodes.get(nc);
                NodeUIInformation nui = (NodeUIInformation)uiInfo;
                int[] b = nui.getBounds();
                int x =
                        (int)Math.round(m_g.getX(gNode)
                                * NodeContainerFigure.WIDTH * 2.34);
                int y =
                        (int)Math.round(m_g.getY(gNode)
                                * NodeContainerFigure.HEIGHT * 2.34);
                NodeUIInformation newCoord =
                        new NodeUIInformation(x, y, b[2], b[3],
                                nui.hasAbsoluteCoordinates());
                LOGGER.debug("Node " + nc + " gets auto-layout coordinates "
                        + newCoord);
                nc.setUIInformation(newCoord);
            }
        }
    }

}
