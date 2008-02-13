/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   24.02.2006 (Christoph Sieb): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.workflow.WorkflowManager;

import org.knime.workbench.editor2.editparts.AbstractPortEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Command that reconnects a connection. This basically corresponds to remove
 * the old connection edit part and insert a new one. Therefore this command
 * encapsulates the delete and create connection command.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
// TODO: not used
public class ReconnectConnectionCommand extends Command {
    private DeleteConnectionCommand m_deleteCommand;

    private CreateConnectionCommand m_createCommand;

    /**
     * Creates a reconnection command encapsulating a delete and create command.
     * 
     * @param connection The edit part of the connection to delete
     * @param manager The hosting workflow manager
     * @param host the source (NodeOutPortEditPart - normaly) of the connection
     * @param target the target edit part
     */
    public ReconnectConnectionCommand(
            final ConnectionContainerEditPart connection,
            final WorkflowManager manager, final AbstractPortEditPart host,
            final EditPart target) {

        // create the delete command
        m_deleteCommand = new DeleteConnectionCommand(connection, manager);

        // create the create command
        CreateConnectionCommand cmd = new CreateConnectionCommand();

        NodeContainerEditPart nodePart = (NodeContainerEditPart)host
                .getParent();

        if (host instanceof NodeOutPortEditPart) {

            // request started on out port?
            cmd.setSourceNode(nodePart);
            cmd.setSourcePortID(((AbstractPortEditPart)host).getId());
            cmd.setStartedOnOutPort(true);
            // LOGGER.debug("Started connection on out-port...");
        } else if (host instanceof NodeInPortEditPart) {
            // // request started on in port ?
            // cmd.setTargetNode(nodePart);
            // cmd.setTargetPortID(((NodeInPortEditPart) getHost()).getId());
            // cmd.setStartedOnOutPort(false);
            return;
            // LOGGER.debug("Started connection on in-port...");
        }

        // we need the manager to execute the command

        cmd.setManager(((WorkflowRootEditPart)nodePart.getParent())
                .getWorkflowManager());

        if (cmd == null) {
            return;
        }

        if ((target instanceof NodeOutPortEditPart)) {
            // cmd.setSourcePortID(((NodeOutPortEditPart) target).getId());
            // cmd.setSourceNode((NodeContainerEditPart) target.getParent());
            return;

            // LOGGER.debug("Ending connection on out-port...");
        } else if (target instanceof NodeInPortEditPart) {
            cmd.setTargetPortID(((NodeInPortEditPart)target).getId());
            cmd.setTargetNode((NodeContainerEditPart)target.getParent());

            // LOGGER.debug("Ending connection on in-port...");
        } else if (target instanceof NodeContainerEditPart) {

            if (cmd.wasStartedOnOutPort()) {
                cmd.setTargetPortID(-1);
                cmd.setTargetNode((NodeContainerEditPart)target);
            } else {
                cmd.setSourcePortID(-1);
                cmd.setSourceNode((NodeContainerEditPart)target);
            }
            // LOGGER.debug("Ending connection on NODE...");

        } else {
            return;

        }

        m_createCommand = cmd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_deleteCommand.canExecute() && m_createCommand.canExecute();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
        mb.setText("Confirm deletion...");
        mb.setMessage("Do you really want to reconnect"
                + " the selected connection ?");
        if (mb.open() != SWT.YES) {
            return;
        }

        // execute both commands
        m_deleteCommand.execute();
        m_createCommand.execute();
    }
}
