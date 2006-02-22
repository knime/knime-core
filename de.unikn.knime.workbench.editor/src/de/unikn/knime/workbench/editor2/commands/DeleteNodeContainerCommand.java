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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This is the command to delete <code>NodeContainer</code>s from the
 * workflow.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class DeleteNodeContainerCommand extends Command {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DeleteConnectionCommand.class);

    private NodeContainerEditPart m_part;

    private WorkflowManager m_manager;

    /**
     * If <code>true</code> the deletion is protected by a message box
     * verification.
     */
    private boolean m_verify;

    /**
     * Creates a new delete command for a <code>NodeContainer</code>.
     * 
     * @param nodePart The container edit part to delete
     * @param manager The manager hosting the container
     */
    public DeleteNodeContainerCommand(final NodeContainerEditPart nodePart,
            final WorkflowManager manager) {
        m_part = nodePart;
        m_manager = manager;
        
        // by default verification is on
        m_verify = true;
    }

    /**
     * If the edit part is locked (= busy), we can't delete the underlying node.
     * 
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        return !m_part.isLocked();
    }

    /**
     * To specify whether a message box should be displayed before deletion.
     * 
     * @param verify if true a message box is displayed
     */
    public void messageBoxVerification(final boolean verify) {

        m_verify = verify;
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {

        // if verification is on, check the deletion decission with a message
        // box
        if (m_verify) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO
                    | SWT.CANCEL);
            mb.setText("Confirm deletion...");
            mb.setMessage("Do you really want to delete the selected node ?");
            if (mb.open() != SWT.YES) {
                return;
            }
        }

        LOGGER.debug("Deleting node #" + m_part.getNodeContainer().getID()
                + " from Workflow");

        // The WFM must removes all connections for us, before the node is
        // removed.
        m_manager.removeNode(m_part.getNodeContainer());

    }

    /**
     * TODO FIXME: no undo by now, as the connections can't be restored and the
     * node gets a new ID.
     * 
     * @see org.eclipse.gef.commands.Command#canUndo()
     */
    public boolean canUndo() {
        return false;
    }

}
