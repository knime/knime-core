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
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command that deletes a <code>ConnectionContainer</code> from the workflow
 * manager.
 *
 * @author Florian Georg, University of Konstanz
 */
public class DeleteConnectionCommand extends Command {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            DeleteConnectionCommand.class);

    private final ConnectionContainerEditPart m_connection;

    private final WorkflowManager m_manager;

    /**
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
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        if (m_connection == null || m_connection.getModel() == null) {
            return false;
        }
        boolean workflowAllowsDeletion =
                m_manager.canRemoveConnection((ConnectionContainer)
                        m_connection.getModel());
        return workflowAllowsDeletion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            m_manager.removeConnection((ConnectionContainer)m_connection
                    .getModel());
        } catch (Exception ex) {
            LOGGER.warn("Operation not allowed.", ex);
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_INFORMATION | SWT.OK);
            mb.setText("Operation not allowed");
            mb.setMessage("You cannot remove a connection while the workflow"
                    + " is in execution.");
            mb.open();
        }
    }
}
