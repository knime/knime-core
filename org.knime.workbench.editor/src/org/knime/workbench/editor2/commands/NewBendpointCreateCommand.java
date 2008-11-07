/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command for creation of connection bendpoints. The bendpoints are stored in a
 * default implementation of an <code>ExtraInfo</code> object.
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointCreateCommand extends Command {
    private final Point m_location;

    private final int m_index;

    private ConnectionUIInformation m_extraInfo;

    private AbsoluteBendpoint m_bendpoint;

    private final ConnectionContainerEditPart m_connection;

    private final ZoomManager m_zoomManager;

    /**
     * New NewBendpointCreateCommand.
     *
     * @param connection The connection model
     * @param index bendpoint index
     * @param location where ?
     */
    public NewBendpointCreateCommand(
            final ConnectionContainerEditPart connection,
            final int index, final Point location, 
            final ZoomManager zoomManager) {
        m_connection = connection;
        m_extraInfo = (ConnectionUIInformation)connection
            .getUIInformation();
        if (m_extraInfo == null) {
            m_extraInfo = new ConnectionUIInformation();
        }
        m_index = index;
        m_location = location;

        m_zoomManager = zoomManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        Point location = m_location.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, location, true);

        m_bendpoint = new AbsoluteBendpoint(location);
        m_bendpoint.setLocation(location);
        m_extraInfo.addBendpoint(m_bendpoint.x, m_bendpoint.y, m_index);

        // we need this to fire some update event up
        m_connection.setUIInformation(m_extraInfo);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        Point location = m_location.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, location, true);

        m_extraInfo.addBendpoint(location.x, location.y, m_index);

        // we need this to fire some update event up
        m_connection.setUIInformation(m_extraInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        m_extraInfo.removeBendpoint(m_index);

        // we need this to fire some update event up
        m_connection.setUIInformation(m_extraInfo);
    }
}
