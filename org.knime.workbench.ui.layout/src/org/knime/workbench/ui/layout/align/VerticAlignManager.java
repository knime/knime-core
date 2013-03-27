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
 * Created: Mar 31, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.layout.align;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 *
 * @author ohl, University of Konstanz
 */
public class VerticAlignManager {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(VerticAlignManager.class);

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
    public VerticAlignManager(final WorkflowManager wfManager,
            final NodeContainerEditPart[] nodes) {
        m_wfm = wfManager;
        m_nodeParts = nodes.clone();
    }

    /**
     *
     */
    public void doLayout() {

        Map<NodeContainerEditPart, Integer> offsets =
            VerticAlignmentCenter.doLayout(m_wfm, m_nodeParts);

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
                    new NodeUIInformation(b[0] + e.getValue(), b[1], b[2],
                            b[3], uiInfo.hasAbsoluteCoordinates());
            LOGGER.debug("Node " + nc + " gets alignment coordinates "
                    + newCoord);
            // save old coordinates for undo
            m_oldCoordinates.put(nc.getID(), uiInfo);

            // triggers gui update
            nc.setUIInformation(newCoord);
        }

    }

    public Map<NodeID, NodeUIInformation> getOldNodeCoordinates() {
        return Collections.unmodifiableMap(m_oldCoordinates);
    }

    public Map<ConnectionID, ConnectionUIInformation> getOldBendpoints() {
        return Collections.unmodifiableMap(m_oldBendpoints);
    }

}
