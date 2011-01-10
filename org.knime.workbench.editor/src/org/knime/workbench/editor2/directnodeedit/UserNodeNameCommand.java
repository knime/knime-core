/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
