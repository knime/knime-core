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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * GEF command for adding a <code>Node</code> to the
 * <code>WorkflowManager</code>.
 *
 * @author Florian Georg, University of Konstanz
 */
public class CreateNodeCommand extends Command {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateNodeCommand.class);

    private final WorkflowManager m_manager;

    private final NodeFactory<?> m_factory;

    private final Point m_location;

    private NodeContainer m_container;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param location Initial visual location in the
     */
    public CreateNodeCommand(final WorkflowManager manager,
            final NodeFactory<?> factory, final Point location) {
        m_manager = manager;
        m_factory = factory;
        m_location = location;
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return (m_manager != null) && (m_factory != null)
                && (m_location != null);
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // Add node to workflow and get the container
        try {
            NodeID id = m_manager.createAndAddNode(m_factory);
            m_container = m_manager.getNodeContainer(id);
        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", t);
            MessageBox mb = new MessageBox(Display.getDefault().
                    getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb.setMessage("The selected node could not be created "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return;
        }
        // create extra info and set it
        NodeUIInformation info = new NodeUIInformation(
                m_location.x, m_location.y, -1, -1, false);
        m_container.setUIInformation(info);

    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_container != null 
            && m_manager.canRemoveNode(m_container.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        if (canUndo()) {
            m_manager.removeNode(m_container.getID());
        } else {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                    "Operation no allowed", "The node " 
                    + m_container.getNameWithID() 
                    + " can currently not be removed");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void dispose() {
        super.dispose();
        m_container = null;
    }
}
