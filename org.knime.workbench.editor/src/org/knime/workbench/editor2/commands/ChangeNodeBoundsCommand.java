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
 * -------------------------------------------------------------------
 *
 * History
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Arrays;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * GEF Command for changing the bounds of a <code>NodeContainer</code> in the
 * workflow. The bounds are stored into the <code>ExtraInfo</code> object of the
 * <code>NodeContainer</code>
 *
 * @author Florian Georg, University of Konstanz
 */
public class ChangeNodeBoundsCommand extends Command {

    private final int[] m_oldBounds;

    private final int[] m_newBounds;

    /* must not keep NodeContainer here to enable undo/redo (the node container
     * instance may change if deleted and the delete is undone. */
    private final NodeID m_nodeID;

    private final WorkflowManager m_manager;

    /**
     *
     * @param container The node container to change
     * @param figureBounds The new bounds of the figure
     * @param figure the figure that is going to be moved
     */
    public ChangeNodeBoundsCommand(final NodeContainer container,
            final NodeContainerFigure figure, final Rectangle figureBounds) {

        // must translate figure bounds into node figure ui info - which is
        // relative to the node's symbol
        NodeUIInformation uiInfo =
            (NodeUIInformation)container.getUIInformation();
        Point offset = figure.getOffsetToRefPoint(uiInfo);
        m_oldBounds = uiInfo.getBounds();
        m_newBounds = new int[]{figureBounds.x + offset.x,
                figureBounds.y + offset.y, figureBounds.width,
                figureBounds.height};;
        m_nodeID = container.getID();
        m_manager = container.getParent();
    }

    /**
     * Sets the new bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        if (!Arrays.equals(m_oldBounds, m_newBounds)) {
            NodeUIInformation information =
                    new NodeUIInformation(m_newBounds[0], m_newBounds[1],
                            m_newBounds[2], m_newBounds[3], true);
            NodeContainer container = m_manager.getNodeContainer(m_nodeID);
            // must set explicitly so that event is fired by container
            container.setUIInformation(information);
        }
    }

    /**
     * Sets the old bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void undo() {
        if (!Arrays.equals(m_oldBounds, m_newBounds)) {
            NodeUIInformation information =
                    new NodeUIInformation(m_oldBounds[0], m_oldBounds[1],
                            m_oldBounds[2], m_oldBounds[3], true);
            NodeContainer container = m_manager.getNodeContainer(m_nodeID);
            container.setUIInformation(information);
        }
    }
}
