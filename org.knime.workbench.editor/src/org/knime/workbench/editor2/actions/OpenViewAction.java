/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;

/**
 * Action to open a view of a node.
 *
 * TODO: Embedd view in an eclipse view (preference setting)
 *
 * @author Florian Georg, University of Konstanz
 */
public class OpenViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private final int m_index;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenViewAction.class);

    /**
     * New action to opne a node view.
     *
     * @param nodeContainer The node
     * @param viewIndex The index of the node view
     */
    public OpenViewAction(final NodeContainer nodeContainer,
            final int viewIndex) {
        m_nodeContainer = nodeContainer;
        m_index = viewIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/openView.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens node view " + m_index + ": "
                + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "View: " + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug("Open Node View " + m_nodeContainer.getName() + " (#"
                + m_index + ")");
        try {
            String title = m_nodeContainer.getID().toString();
            if (m_nodeContainer.getCustomName() != null) { 
                    title = m_nodeContainer.getCustomName();
            } 
            title += " : " + m_nodeContainer.getViewName(m_index);
            final String finalString = title;
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {                    
                    m_nodeContainer.getView(m_index).createFrame(finalString);
                }
            });
        } catch (Throwable t) {
            MessageBox mb = new MessageBox(
                    Display.getDefault().getActiveShell(),
                    SWT.ICON_ERROR | SWT.OK);
            mb.setText("View cannot be opened");
            mb.setMessage("The view cannot be opened for the " 
                    + "following reason:\n" + t.getMessage());
            mb.open();
            LOGGER.error("The view for node '"
                    + m_nodeContainer.getNameWithID() + "' has thrown a '"
                    + t.getClass().getSimpleName()
                    + "'. That is most likely an " 
                    + "implementation error.", t);
        } 
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "knime.open.view.action";
    }
}
