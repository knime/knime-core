/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
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
 *   09.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;

import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command that deletes a <code>ConnectionContainer</code> from the workflow
 * manager.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class DeleteConnectionCommand extends Command {

    private ConnectionContainerEditPart m_connection;

    private WorkflowManager m_manager;

    /**
     * 
     * @param connection The edit part of the connection to delete
     * @param manager The hosting workflow manager
     */
    public DeleteConnectionCommand(
            final ConnectionContainerEditPart connection,
            final WorkflowManager manager) {
        m_connection = connection;
        m_manager = manager;
    }

    /**
     * 
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        return true;

    }

    /**
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {

        // before removing the given connection check if it still exists

        // remove container from the WFM
        m_manager.removeConnection((ConnectionContainer) m_connection
                .getModel());
    }

}
