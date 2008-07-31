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
 *   17.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class FavoriteNodesManager {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            FavoriteNodesManager.class);
    
    private static FavoriteNodesManager instance;

    private Root m_root;
    private Category m_favNodes;
    private Category m_freqNodes;
    private Category m_lastNodes;
    
    private static final Image FAV_ICON = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/fav/folder_fav.png");
    // icons swapped on purpose
    private static final Image FREQ_ICON = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/fav/folder_last.png");
    // icons swapped on purpose
    private static final Image LAST_ICON = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/fav/folder_freq.png");
    
    // loading and saving
    private static final String TAG_FAVORITES = "favoritenodes";
    private static final String TAG_PERSONAL_FAVS = "personals";
    private static final String TAG_MOST_FREQUENT = "frequents";
    private static final String TAG_LAST_USED = "lastused";
    private static final String TAG_FAVORITE = "favorite";
    private static final String TAG_NODE_ID = "nodeid";
    
    
    /** ID of the personal favorites category. */
    public static final String FAV_CAT_ID = "fav";
    /**  
     * Title of the personal favorites category
     * (used by {@link FavoriteNodesDropTarget}).
     */
    static final String FAV_TITLE = "Personal favorite nodes";
    
    /**
     * 
     * @return singleton instance
     */
    public static final FavoriteNodesManager getInstance() {
        if (instance == null) {
            instance = new FavoriteNodesManager();
        }
        return instance;
    }
    
    private FavoriteNodesManager() {
        createTreeModel();
    }
    
    /**
     * 
     * @return the tree model with three categories: favorites, most frequent 
     * and last used
     */
    public Root getRoot() {
        return m_root;
    }
    
    /**
     * 
     */
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
        
        // lazy initialization -> load here the contents
        synchronized (RepositoryManager.INSTANCE.getRoot()) {            
            loadFavorites();
        }
    }
    
    
    /**
     * 
     * @param node adds this node to the favorite nodes category
     */
    public void addFavoriteNode(final NodeTemplate node) {
        m_favNodes.addChild(node);
    }
    
    /**
     * 
     * @param node removes this node from the favorites
     */
    public void removeFavoriteNode(final NodeTemplate node) {
        m_favNodes.removeChild(node);
    }
    
    /**
     * Updates the categories most frequent and last used with the information
     * from the {@link NodeUsageRegistry}.
     */
    public void updateNodes() {
        updateLastUsedNodes();
        updateFrequentUsedNodes();
    }
    
    /**
     * Updates last used nodes.
     */
    public void updateLastUsedNodes() {        
        // update last used
        m_lastNodes.removeAllChildren();
        m_lastNodes.addAllChildren(NodeUsageRegistry.getLastUsedNodes());
    }
    
    /**
     * Updates most frequently used nodes.
     */
    public void updateFrequentUsedNodes() {
        // update most frequent
        m_freqNodes.removeAllChildren();
        m_freqNodes.addAllChildren(NodeUsageRegistry.getMostFrequentNodes());
    }
    
    /**
     * Saves the ids of the favorite nodes to the state location of the plugin.
     */
    public void saveFavoriteNodes() {
        XMLMemento memento = XMLMemento.createWriteRoot(TAG_FAVORITES);
        saveFavoriteNodes(memento);
        FileWriter writer = null;
        try {
            writer = new FileWriter(getFavoriteNodesFile());
            memento.save(writer);
        } catch (IOException ioe) {
            LOGGER.error("Problems writing file for FavoriteNodes: ", ioe);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ioe) {
                LOGGER.error("Error closing input stream for FavoriteNodes ", 
                        ioe);
            }
        }
    }
    
    private void saveFavoriteNodes(final XMLMemento memento) {
        // personal favorites
        IMemento favNodes = memento.createChild(TAG_PERSONAL_FAVS);
        for (IRepositoryObject reposObj : m_favNodes.getChildren()) {
            IMemento item = favNodes.createChild(TAG_FAVORITE);
            item.putString(TAG_NODE_ID, ((NodeTemplate)reposObj).getID());
        }
        // most frequent
        IMemento freqNodes = memento.createChild(TAG_MOST_FREQUENT);
        NodeUsageRegistry.saveFrequentNodes(freqNodes);
        // last used
        IMemento lastUsedNodes = memento.createChild(TAG_LAST_USED);
        NodeUsageRegistry.saveLastUsedNodes(lastUsedNodes);
    }
    
    private File getFavoriteNodesFile() {
        return KNIMEUIPlugin.getDefault()
            .getStateLocation().append("favoriteNodes.xml").toFile();
    }
    
    private void loadFavorites() {
        // load the personal favorites
        FileReader reader = null; 
        try {
            reader = new FileReader(getFavoriteNodesFile());
            loadFavoriteNodes(XMLMemento.createReadRoot(reader));
        } catch (FileNotFoundException fnf) {
            // no favorites saved
        } catch (Exception e) {
            LOGGER.error("Failed to load favorite nodes file", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                LOGGER.error("Failed to close favorite nodes file", ioe);
            }
        }
    }
    
    private void loadFavoriteNodes(final XMLMemento favoriteNodes) {
        IMemento favNodes = favoriteNodes.getChild(TAG_PERSONAL_FAVS);
        if (RepositoryManager.INSTANCE.isRootAvailable()) {
            RepositoryManager.INSTANCE.getRoot();
            for (IMemento favNode : favNodes.getChildren(TAG_FAVORITE)) {
                String id = favNode.getString(TAG_NODE_ID);
//                LOGGER.debug("trying to load: " + id);
                NodeTemplate node = (NodeTemplate)RepositoryManager.INSTANCE
                    .getRoot().getChildByID(id, true);
                if (node != null) {
                    addFavoriteNode(node);
                }
            }
            IMemento freqNodes = favoriteNodes.getChild(TAG_MOST_FREQUENT);
            NodeUsageRegistry.loadFrequentNodes(freqNodes);
            IMemento lastNodes = favoriteNodes.getChild(TAG_LAST_USED);
            NodeUsageRegistry.loadLastUsedNodes(lastNodes);
            updateNodes();
            RepositoryManager.INSTANCE.releaseRoot();
        }
    }
}
