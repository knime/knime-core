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
 *   10.05.2005 (sieb): created
 */
package org.knime.workbench.editor2.directnodeedit;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;


/**
 * Command to change the user node name in the figure.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class UserNodeNameCommand extends Command {
    
    private String m_newName, m_oldName;

    // must not keep reference to NodeContainer (may be obsolete
    // as part of undo/redo sequence)
    private final NodeID m_nodeID;
    private final WorkflowManager m_manager; 

    /**
     * Creates a new command to change the user node name.
     * 
     * @param nodeID the nodeID of the node to change the name
     * @param manager The manager containing the node
     * @param newName the new name to set
     */
    public UserNodeNameCommand(final NodeID nodeID, 
            final WorkflowManager manager, final String newName) {
        m_nodeID = nodeID;
        m_manager = manager;

        if (newName != null) {
            m_newName = newName;
        } else {
            m_newName = "";
        }
    }
    
    /** @return the node container to be edited. */
    private NodeContainer getNodeContainer() {
        return m_manager.getNodeContainer(m_nodeID);
    }

    /**
     * Sets the new name into the model and remembers the old one for undo.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        NodeContainer nodeContainer = getNodeContainer();
        m_oldName = nodeContainer.getCustomName();
        nodeContainer.setCustomName(m_newName);
    }

    /**
     * Sets the old name into the model for undo purpose.
     * 
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        getNodeContainer().setCustomName(m_oldName);
    }
}
