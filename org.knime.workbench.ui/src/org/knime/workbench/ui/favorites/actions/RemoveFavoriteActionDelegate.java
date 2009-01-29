/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   17.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.ui.favorites.FavoriteNodesManager;
import org.knime.workbench.ui.favorites.FavoritesView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class RemoveFavoriteActionDelegate implements IViewActionDelegate {

    private FavoritesView m_view;
    
    private NodeTemplate m_node;
    
    /**
     * {@inheritDoc}
     */
    public void run(final IAction action) {
        m_view.removeFavorite(m_node);
    }

    /**
     * {@inheritDoc}
     */
    public void selectionChanged(final IAction action, 
            final ISelection selection) {
        action.setEnabled(false);
        if (((IStructuredSelection)selection)
                .getFirstElement() != null && ((IStructuredSelection)selection)
                .getFirstElement() instanceof NodeTemplate) {
            NodeTemplate node = (NodeTemplate)((IStructuredSelection)selection)
                .getFirstElement();
            if (node.getParent() != null && node.getParent().getID() != null
                    && node.getParent().getID().equals(
                            FavoriteNodesManager.FAV_CAT_ID)) {
                m_node = (NodeTemplate)((IStructuredSelection)selection)
                .getFirstElement();
                action.setEnabled(true);
            }            
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void init(final IViewPart view) {
        m_view = (FavoritesView)view;
    }

}
