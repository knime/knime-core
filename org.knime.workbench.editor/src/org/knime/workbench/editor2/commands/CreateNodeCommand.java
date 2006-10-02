/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowInExecutionException;
import org.knime.core.node.workflow.WorkflowManager;

import org.knime.workbench.editor2.extrainfo.ModellingNodeExtraInfo;

/**
 * GEF command for adding a <code>Node</code> to the
 * <code>WorkflowManager</code>.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class CreateNodeCommand extends Command {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateNodeCommand.class);

    private WorkflowManager m_manager;

    private NodeFactory m_factory;

    private Point m_location;

    private NodeContainer m_container;

    /**
     * Creates a new command.
     * 
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param location Initial visual location in the
     */
    public CreateNodeCommand(final WorkflowManager manager,
            final NodeFactory factory, final Point location) {
        m_manager = manager;
        m_factory = factory;
        m_location = location;
    }

    /**
     * We can execute, if all components were 'non-null' in the constructor.
     * 
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    @Override
    public boolean canExecute() {
        return (m_manager != null) && (m_factory != null)
                && (m_location != null);
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        // Add node to workflow and get the container
        try {
            m_container = m_manager.addNewNode(m_factory);
        } catch (Throwable t) {
            // if fails notify the user
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb
                    .setMessage("The selected node could not be created due to the following reason:\n"
                            + t.getMessage());
            mb.open();
            return;
        }
        // create extra info and set it
        ModellingNodeExtraInfo info = new ModellingNodeExtraInfo();
        info.setNodeLocation(m_location.x, m_location.y, -1, -1);
        m_container.setExtraInfo(info);

    }

    /**
     * This can always be undone.
     * 
     * @see org.eclipse.gef.commands.Command#canUndo()
     */
    @Override
    public boolean canUndo() {
        return true;
    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        try {
            m_manager.removeNode(m_container);
        } catch (WorkflowInExecutionException ex) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_INFORMATION | SWT.OK);
            mb.setText("Operation not allowed");
            mb.setMessage("You cannot remove a node while the workflow is in"
                    + " execution.");
            mb.open();
        }
    }
}
