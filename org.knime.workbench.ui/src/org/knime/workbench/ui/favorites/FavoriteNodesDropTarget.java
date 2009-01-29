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
 *   14.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FavoriteNodesDropTarget extends DropTargetAdapter {

    private FavoritesView m_view;
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            FavoriteNodesDropTarget.class); 
    
    /**
     * 
     * @param view the view
     */
    public FavoriteNodesDropTarget(final FavoritesView view) {
        m_view = view;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(final DropTargetEvent event) {
       event.detail = DND.DROP_COPY;
//       LOGGER.debug("event item: " + event.item);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetEvent event) {
        event.detail = DND.DROP_NONE;
        if (event.item != null & event.item instanceof TreeItem) {
                event.detail = DND.DROP_COPY;
//            }
        }
    }
    
       
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
//        LOGGER.debug("drop " + event);
        if (isNodeTemplate()) {
            NodeTemplate template = (NodeTemplate)((IStructuredSelection)
                    LocalSelectionTransfer.getTransfer()
                    .getSelection()).getFirstElement();
            m_view.addNodeTemplate(template);
        }
    }
    
    
    private boolean isNodeTemplate() {
        Object template = ((IStructuredSelection)LocalSelectionTransfer
                .getTransfer().getSelection()).getFirstElement();
        if (!(template instanceof NodeTemplate)) {
            // Last change: Ask adaptables for an adapter object
            if (template instanceof IAdaptable) {
                template = ((IAdaptable) template).getAdapter(
                        NodeTemplate.class);
            }
        }
        return template instanceof NodeTemplate;
    } 
    
}
