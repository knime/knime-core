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
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command for moving an absolute bendpoint on the connection.
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointMoveCommand extends Command {
    private Point m_newLocation;

    private Point m_oldLocation;

    private int m_index;

    private ConnectionUIInformation m_uiInfo;

    //private AbsoluteBendpoint m_bendpoint;

    private ZoomManager m_zoomManager;

    private final WorkflowManager m_manager;

    private final ConnectionID m_connID;

    /**
     * New bendpoint move command.
     *
     * @param connection The connection model
     * @param manager The workflow manager that contains the connection.
     * @param index The bendpoint index
     * @param newLocation the new location
     * @param zoomManager The zoom manager.
     */
    public NewBendpointMoveCommand(
            final ConnectionContainerEditPart connection,
            final WorkflowManager manager,
            final int index, final Point newLocation,
            final ZoomManager zoomManager) {
        m_manager = manager;
        m_uiInfo = (ConnectionUIInformation)connection.getUIInformation();
        m_connID = connection.getModel().getID();

        m_index = index;
        m_newLocation = newLocation;
        m_zoomManager = zoomManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        int[] p = m_uiInfo.getBendpoint(m_index);

        AbsoluteBendpoint bendpoint = new AbsoluteBendpoint(p[0], p[1]);
        m_oldLocation = bendpoint.getLocation();

        Point newLocation = m_newLocation.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, newLocation, true);

        bendpoint = new AbsoluteBendpoint(newLocation);

        m_uiInfo.removeBendpoint(m_index);
        m_uiInfo.addBendpoint(bendpoint.x, bendpoint.y, m_index);

        // issue notification
        m_manager.getConnection(m_connID).setUIInfo(m_uiInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        m_uiInfo.removeBendpoint(m_index);

        Point newLocation = m_newLocation.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, newLocation, true);

        m_uiInfo.addBendpoint(newLocation.x, newLocation.y, m_index);

        // issue notification
        m_manager.getConnection(m_connID).setUIInfo(m_uiInfo);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        Point oldLocation = m_oldLocation.getCopy();
        //WorkflowEditor.adaptZoom(m_zoomManager, oldLocation, true);

        m_uiInfo.removeBendpoint(m_index);
        m_uiInfo.addBendpoint(oldLocation.x, oldLocation.y, m_index);

        // issue notification
        m_manager.getConnection(m_connID).setUIInfo(m_uiInfo);
    }
}
