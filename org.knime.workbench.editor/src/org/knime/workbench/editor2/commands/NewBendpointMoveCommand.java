/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
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
    private final NodeID m_destNodeID;
    private final int m_destPort;

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
        m_destNodeID = connection.getModel().getDest();
        m_destPort = connection.getModel().getDestPort();
        
        m_index = index;
        m_newLocation = newLocation;
        m_zoomManager = zoomManager;
    }

    private ConnectionContainer getConnectionContainer() {
        return m_manager.getIncomingConnectionFor(m_destNodeID, m_destPort);
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
        getConnectionContainer().setUIInfo(m_uiInfo);
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
        getConnectionContainer().setUIInfo(m_uiInfo);

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
        getConnectionContainer().setUIInfo(m_uiInfo);
    }
}
