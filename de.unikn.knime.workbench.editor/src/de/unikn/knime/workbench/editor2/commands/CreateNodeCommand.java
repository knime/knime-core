/* 
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
 *   29.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;

import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.extrainfo.ModellingNodeExtraInfo;
import de.unikn.knime.workbench.repository.RepositoryManager;
import de.unikn.knime.workbench.repository.model.NodeTemplate;

/**
 * GEF command for adding a <code>Node</code> to the
 * <code>WorkflowManager</code>.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class CreateNodeCommand extends Command {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(CreateNodeCommand.class);

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
        m_container = m_manager.addNewNode(m_factory);

        // lookup extra info from node repository and store it in the instance
        NodeTemplate template = RepositoryManager.INSTANCE.getRoot()
                .findTemplateByFactory(m_factory.getClass().getName());

        // create extra info and set it
        ModellingNodeExtraInfo info = new ModellingNodeExtraInfo();
        info.setNodeLocation(m_location.x, m_location.y, -1, -1);
        info.setType(template.getType());
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
        m_manager.removeNode(m_container);
    }

}
