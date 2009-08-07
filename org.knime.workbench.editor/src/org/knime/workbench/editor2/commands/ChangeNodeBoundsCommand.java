
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
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Arrays;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * GEF Command for changing the bounds of a <code>NodeContainer</code> in the
 * workflow. The bounds are stored into the <code>ExtraInfo</code> object of
 * the <code>NodeContainer</code>
 *
 * @author Florian Georg, University of Konstanz
 */
public class ChangeNodeBoundsCommand extends Command {

    private final int[] m_oldBounds;
    private final int[] m_newBounds;

    /* must not keep NodeContainer here to enable undo/redo (the node container
     * instance may change if deleted and the delete is undone. */
    private final NodeID m_nodeID;
    private final WorkflowManager m_manager;

    /**
     *
     * @param container The node container to change
     * @param newBounds The new bounds
     */
    public ChangeNodeBoundsCommand(final NodeContainer container,
            final int[] newBounds) {
        // right info type

        NodeUIInformation uiInfo = 
            (NodeUIInformation) container.getUIInformation();
        m_oldBounds = uiInfo.getBounds();
        m_newBounds = newBounds;
        m_nodeID = container.getID();
        m_manager = container.getParent();
    }

    /**
     * Sets the new bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        if (!Arrays.equals(m_oldBounds, m_newBounds)) {
            NodeUIInformation information = new NodeUIInformation(
                    m_newBounds[0], m_newBounds[1],
                    m_newBounds[2], m_newBounds[3], true);
            NodeContainer container = m_manager.getNodeContainer(m_nodeID);
            // must set explicitly so that event is fired by container
            container.setUIInformation(information);
        }
    }

    /**
     * Sets the old bounds.
     *
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void undo() {
        if (!Arrays.equals(m_oldBounds, m_newBounds)) {
            NodeUIInformation information = new NodeUIInformation(
                    m_oldBounds[0], m_oldBounds[1],
                    m_oldBounds[2], m_oldBounds[3], true);
            NodeContainer container = m_manager.getNodeContainer(m_nodeID);
            container.setUIInformation(information);
        }
    }
}

