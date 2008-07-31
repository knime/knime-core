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
 *   14.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FavoriteNodesDragSource implements DragSourceListener {
    
    private FavoritesView m_view;
   
    /**
     * 
     * @param view the view
     */
    public FavoriteNodesDragSource(final FavoritesView view) {
        m_view = view;
        
    }
    
    /**
     * {@inheritDoc}
     */
    public void dragFinished(final DragSourceEvent event) {
    }

    /**
     * {@inheritDoc}
     */
    public void dragSetData(final DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection)m_view.getSelection();
        LocalSelectionTransfer.getTransfer().setSelection(sel);
    }

    /**
     * {@inheritDoc}
     */
    public void dragStart(final DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection)m_view.getSelection();
        LocalSelectionTransfer.getTransfer().setSelection(sel);
        event.detail = DND.DROP_COPY;
        // cancel event, if not an NodeTemplate, or not exactly one element
        // selected
        if (!(sel.getFirstElement() instanceof NodeTemplate)
                || (sel.size() != 1)) {
            event.doit = false;
        } else {
            event.doit = true;
        }
    }

}
