/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   23.12.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.NodeContainer;

import org.knime.workbench.ui.views.EmbeddedNodeView;

/**
 * Opens a node view embedded inside the Workbench - "Node View".
 * 
 * @author Florian Georg, University of Konstanz
 */
public class OpenViewEmbeddedAction extends OpenViewAction {

    private NodeContainer m_container;

    private int m_viewIndex;

    private static int numInstances = 0;

    /**
     * @param nodeContainer The node container
     * @param viewIndex The view index.
     */
    public OpenViewEmbeddedAction(final NodeContainer nodeContainer,
            final int viewIndex) {
        super(nodeContainer, viewIndex);
        m_container = nodeContainer;
        m_viewIndex = viewIndex;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            IViewPart part =
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage().showView(
                                    EmbeddedNodeView.ID,
                                    "" + numInstances++,
                                    IWorkbenchPage.VIEW_CREATE
                                            | IWorkbenchPage.VIEW_ACTIVATE);
            if (part != null) {
                EmbeddedNodeView view = (EmbeddedNodeView)part;

                NodeView nodeView = m_container.getView(m_viewIndex);

                // sets the Node-view to the Eclipse-View...
                view.setNodeView(nodeView);
                Workbench.getInstance().getActiveWorkbenchWindow()
                        .getActivePage().activate(view);
            }
        } catch (PartInitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
