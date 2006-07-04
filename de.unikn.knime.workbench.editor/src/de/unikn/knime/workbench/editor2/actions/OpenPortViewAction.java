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
 *   Aug 5, 2005 (georg): created
 */
package de.unikn.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.ImageRepository;

/**
 * Action to open a port view on a specific out-port.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class OpenPortViewAction extends Action {

    private NodeContainer m_nodeContainer;

    private int m_index;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenPortViewAction.class);

    /**
     * New action to opne view on a port.
     * 
     * @param nodeContainer The node
     * @param portIndex The index of the out-port
     */
    public OpenPortViewAction(final NodeContainer nodeContainer,
            final int portIndex) {
        m_nodeContainer = nodeContainer;
        m_index = portIndex;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openView.gif");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    public String getToolTipText() {
        return "Opens a view on outport #" + m_index;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {

        // the name is constructed like "Model outport <specificIndex>"
        String name;

        if (m_nodeContainer.isDataOutPort(m_index)) {
            name = "Data Outport " + m_index;
        } else {
            name = "Model Outport "
                    + (m_index - m_nodeContainer.getNrDataOutPorts());
        }

        // the text will be displayed in the context menu
        // it consists of the specific port type and index and its description
        String description = m_nodeContainer.getOutportName(m_index);

        return name + ": " + description;
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        LOGGER.debug("Open Port View " + m_nodeContainer.nodeToString() + " (#"
                + m_index + ")");
        m_nodeContainer.openPortView(m_index);
    }
}
