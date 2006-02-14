/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   ${date} (${user}): created
 */
package de.unikn.knime.workbench.editor2.commands;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.workbench.editor2.extrainfo.ModellingConnectionExtraInfo;

/**
 * Command for moving an absolute bendpoint on the connection.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointMoveCommand extends Command {
    private Point m_newLocation;

    private Point m_oldLocation;

    private int m_index;

    private ModellingConnectionExtraInfo m_extraInfo;

    private AbsoluteBendpoint m_bendpoint;

    private ConnectionContainer m_connection;

    /**
     * New bendpoint move command.
     * 
     * @param connection The connection model
     * @param index The bendpoint index
     * @param newLocation the new location
     */
    public NewBendpointMoveCommand(final ConnectionContainer connection,
            final int index, final Point newLocation) {
        m_extraInfo = (ModellingConnectionExtraInfo) connection.getExtraInfo();
        m_connection = connection;

        m_index = index;
        m_newLocation = newLocation;
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {
        int[] p = m_extraInfo.getBendpoint(m_index);

        AbsoluteBendpoint bendpoint = new AbsoluteBendpoint(p[0], p[1]);
        m_oldLocation = bendpoint.getLocation();
        bendpoint = new AbsoluteBendpoint(m_newLocation);

        m_extraInfo.removeBendpoint(m_index);
        m_extraInfo.addBendpoint(bendpoint.x, bendpoint.y, m_index);

        // issue notfication
        m_connection.setExtraInfo(m_extraInfo);

    }

    /**
     * @see org.eclipse.gef.commands.Command#redo()
     */
    public void redo() {
        m_extraInfo.removeBendpoint(m_index);
        m_extraInfo.addBendpoint(m_bendpoint.x, m_bendpoint.y, m_index);

        // issue notfication
        m_connection.setExtraInfo(m_extraInfo);

    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    public void undo() {
        m_bendpoint = new AbsoluteBendpoint(m_oldLocation);

        m_extraInfo.removeBendpoint(m_index);
        m_extraInfo.addBendpoint(m_bendpoint.x, m_bendpoint.y, m_index);

        // issue notfication
        m_connection.setExtraInfo(m_extraInfo);

    }
}
