/* ------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.knime.workbench.repository.NodeUsageListener;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.view.RepositoryContentProvider;
import org.knime.workbench.repository.view.RepositoryLabelProvider;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FavoritesView extends ViewPart implements NodeUsageListener {
    
    private TreeViewer m_viewer;
    
    private Root m_root;
    private Category m_favNodes;
    private Category m_freqNodes;
    private Category m_lastNodes;
    
    private static final Image FAV_ICON = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/fav/folder_fav.png");
    private static final Image FREQ_ICON = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/fav/folder_freq.png");
    private static final Image LAST_ICON = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/fav/folder_last.png");
    
    /** ID of the personal favorites category. */
    public static final String FAV_CAT_ID = "fav";
    /**  
     * Title of the personal favorites category
     * (used by {@link FavoriteNodesDropTarget}).
     */
    static final String FAV_TITLE = "Personal favorite nodes";
    
    
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
        
        createTreeModel();
        NodeUsageRegistry.addNodeUsageListener(this);
    }

    
    private void createTreeModel() {
        m_root = new Root();
        m_root.setSortChildren(false);
        m_favNodes = new Category(FAV_CAT_ID);
        m_favNodes.setName(FAV_TITLE);
        m_favNodes.setIcon(FAV_ICON);
        m_favNodes.setAfterID("");
        m_favNodes.setSortChildren(true);
        m_root.addChild(m_favNodes);
        
        m_freqNodes = new Category("freq");
        m_freqNodes.setName("Most frequently used nodes");
        m_freqNodes.setIcon(FREQ_ICON);
        m_freqNodes.setAfterID("fav");
        m_freqNodes.setSortChildren(false);
        m_root.addChild(m_freqNodes);
        
        m_lastNodes = new Category("last");
        m_lastNodes.setName("Last used nodes");
        m_lastNodes.setIcon(LAST_ICON);
        m_lastNodes.setAfterID("freq");
        m_lastNodes.setSortChildren(false);
        m_root.addChild(m_lastNodes);
        
        
        m_viewer.setInput(m_root);
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
        m_favNodes.addChild(template);
        m_viewer.refresh();
        m_viewer.expandToLevel(template, TreeViewer.ALL_LEVELS);
    }


    /**
     * 
     * {@inheritDoc}
     */
    public void nodeAdded() {
        // update last used
        m_lastNodes.removeAllChildren();
        m_lastNodes.addAllChildren(NodeUsageRegistry.getLastUsedNodes());
        // update most frequent
        m_freqNodes.removeAllChildren();
        m_freqNodes.addAllChildren(NodeUsageRegistry.getMostFrequentNodes());
        m_viewer.refresh();
    }


    /**
     * Removes the given node from the personal favorites and refreshes the 
     * tree.
     * @param node node to be removed from the personal favorite nodes 
     */
    public void removeFavorite(final NodeTemplate node) {
        m_favNodes.removeChild(node);
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

}
