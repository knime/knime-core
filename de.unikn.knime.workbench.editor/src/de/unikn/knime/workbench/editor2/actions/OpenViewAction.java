/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   25.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.ImageRepository;

/**
 * Action to open a view of a node.
 * 
 * TODO: Embedd view in an eclipse view (preference setting)
 * 
 * @author Florian Georg, University of Konstanz
 */
public class OpenViewAction extends Action {

    private NodeContainer m_nodeContainer;

    private int m_index;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenPortViewAction.class);

    /**
     * New action to opne a node view.
     * 
     * @param nodeContainer The node
     * @param portIndex The index of the node view
     */
    public OpenViewAction(final NodeContainer nodeContainer, final int portIndex) {
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
        return "Opens node view " + m_index + ": "
                + m_nodeContainer.getViewName(m_index);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "View: "
                + m_nodeContainer.getViewName(m_index);
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        LOGGER.debug("Open Node View " + m_nodeContainer.nodeToString() + " (#"
                + m_index + ")");
        m_nodeContainer.showView(m_index);
    }

}
