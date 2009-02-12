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


/**
 * Command to change the user node name in the figure.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class UserNodeNameCommand extends Command {
    private String m_newName, m_oldName;

    private NodeContainer m_nodeContainer;

    /**
     * Creates a new command to change the user node name.
     * 
     * @param nodeContainer the node container of the node to change the name
     * 
     * @param newName the new name to set
     */
    public UserNodeNameCommand(final NodeContainer nodeContainer,
            final String newName) {
        m_nodeContainer = nodeContainer;

        if (newName != null) {

            m_newName = newName;
        } else {

            m_newName = "";
        }
    }

    /**
     * Sets the new name into the model and remembers the old one for undo.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        m_oldName = m_nodeContainer.getCustomName();
        m_nodeContainer.setCustomName(m_newName);
    }

    /**
     * Sets the old name into the model for undo purpose.
     * 
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        m_nodeContainer.setCustomName(m_oldName);
    }
}
