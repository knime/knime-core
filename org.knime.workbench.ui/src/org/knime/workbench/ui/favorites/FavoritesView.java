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
 *   13.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;
import org.knime.workbench.repository.NodeUsageListener;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.view.RepositoryContentProvider;
import org.knime.workbench.repository.view.RepositoryLabelProvider;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FavoritesView extends ViewPart implements NodeUsageListener {
    
    private TreeViewer m_viewer;
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        m_viewer = new TreeViewer(parent, 
                SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        this.getSite().setSelectionProvider(m_viewer);
        Transfer[] transfers = new Transfer[]{
                LocalSelectionTransfer.getTransfer()};
        m_viewer.addDragSupport(DND.DROP_COPY, transfers,
                new FavoriteNodesDragSource(this));
        m_viewer.addDropSupport(DND.DROP_COPY, transfers,
                new FavoriteNodesDropTarget(this));
        m_viewer.setLabelProvider(new RepositoryLabelProvider());
        m_viewer.setContentProvider(new RepositoryContentProvider());
        
        // no sorting
        m_viewer.setComparator(null);
        
        // lazy initialization of the manager 
        Display.getDefault().syncExec(new Runnable() {

            public void run() {                
                m_viewer.setInput(FavoriteNodesManager.getInstance().getRoot());
                Object category = FavoriteNodesManager.getInstance().getRoot()
                    .getChildByID(FavoriteNodesManager.FAV_CAT_ID, false);
                m_viewer.expandToLevel(category, 1);
            }
            
        });
        NodeUsageRegistry.addNodeUsageListener(this);
    }

    

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
    }
    
    /**
     * 
     * @return the selection of the underlying list
     */
    public ISelection getSelection() {
        return m_viewer.getSelection();
    }
   
    /**
     * 
     * @param template adds this node template to the favorites
     */
    void addNodeTemplate(final NodeTemplate template) {
        FavoriteNodesManager.getInstance().addFavoriteNode(template);
        m_viewer.refresh();
        m_viewer.expandToLevel(template, TreeViewer.ALL_LEVELS);
    }



    /**
     * Removes the given node from the personal favorites and refreshes the 
     * tree.
     * @param node node to be removed from the personal favorite nodes 
     */
    public void removeFavorite(final NodeTemplate node) {
        FavoriteNodesManager.getInstance().removeFavoriteNode(node);
        m_viewer.refresh();
    }


    /**
     * Expands all tree items (the categories).
     */
    public void expandAll() {
        m_viewer.expandAll();
    }
    
    /**
     * Collapses the whole tree.
     */
    public void collapseAll() {
        m_viewer.collapseAll();
    }

    
    /**
     * 
     * {@inheritDoc}
     */
    public void nodeAdded() {
        FavoriteNodesManager.getInstance().updateNodes();
        Display.getDefault().syncExec(new Runnable() {

            public void run() {   
                if (!m_viewer.getControl().isDisposed()) {
                    m_viewer.refresh();
                }
            }
            
        });
    }


    /**
     * 
     * {@inheritDoc}
     */
    public void frequentHistoryChanged() {
        FavoriteNodesManager.getInstance().updateFrequentUsedNodes();
        Display.getDefault().syncExec(new Runnable() {

            public void run() {   
                if (!m_viewer.getControl().isDisposed()) {
                    m_viewer.refresh();
                }
            }
            
        });
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void usedHistoryChanged() {
        FavoriteNodesManager.getInstance().updateLastUsedNodes();
        // TODO: if the manager would know the view,
        // or the view would have access to the categories
        // we could refresh more specifically this categroy
        Display.getDefault().syncExec(new Runnable() {

            public void run() {   
                if (!m_viewer.getControl().isDisposed()) {
                    m_viewer.refresh();
                }
            }
            
        });
    }

}
