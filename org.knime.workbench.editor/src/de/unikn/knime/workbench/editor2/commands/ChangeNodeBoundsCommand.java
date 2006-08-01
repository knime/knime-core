
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
 *   30.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.NodeContainer;

import de.unikn.knime.workbench.editor2.extrainfo.ModellingNodeExtraInfo;

/**
 * GEF Command for changing the bounds of a <code>NodeContainer</code> in the
 * workflow. The bounds are stored into the <code>ExtraInfo</code> object of
 * the <code>NodeContainer</code>
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ChangeNodeBoundsCommand extends Command {
    private int[] m_oldBounds;

    private int[] m_newBounds;

    private NodeContainer m_container;

    private ModellingNodeExtraInfo m_extraInfo;

    /**
     * 
     * @param container The node container to change
     * @param newBounds The new bounds
     */
    public ChangeNodeBoundsCommand(final NodeContainer container,
            final int[] newBounds) {
        // right info type
        assert (container.getExtraInfo() instanceof ModellingNodeExtraInfo);

        m_extraInfo = (ModellingNodeExtraInfo) container.getExtraInfo();
        m_oldBounds = m_extraInfo.getBounds();
        m_newBounds = newBounds;
        m_container = container;

    }

    /**
     * Sets the new bounds.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        m_extraInfo.setBounds(m_newBounds);
        // must set explicitly so that event is fired by container
        m_container.setExtraInfo(m_extraInfo);
    }

    /**
     * Sets the old bounds.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void undo() {
        m_extraInfo.setBounds(m_oldBounds);
        // must set explicitly so that event is fired by container
        m_container.setExtraInfo(m_extraInfo);
    }
}

