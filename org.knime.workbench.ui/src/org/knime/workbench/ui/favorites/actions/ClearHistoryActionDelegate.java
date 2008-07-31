/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.knime.workbench.repository.NodeUsageRegistry;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ClearHistoryActionDelegate implements IViewActionDelegate {

    private IViewPart m_view;
    
    /**
     * {@inheritDoc}
     */
    public void init(final IViewPart view) {
        m_view = view;
    }

    /**
     * {@inheritDoc}
     */
    public void run(final IAction action) {
        if (MessageDialog.openQuestion(m_view.getViewSite().getShell(), 
                "Clear History?", "Do really want to clear history? " 
                + "Affects frequently used and last used nodes.")) {
            NodeUsageRegistry.clearHistory();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void selectionChanged(final IAction action, 
            final ISelection selection) {
    }

}
