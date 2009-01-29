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
package org.knime.workbench.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.ui.IMemento;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class NodeUsageRegistry {
    
    private static int maxMostFrequent = 10;
    private static int maxLastUsed = 10;
    
    private static final List<NodeTemplateFrequency> FREQUENCIES 
        = new LinkedList<NodeTemplateFrequency>();
    
    private static final List<NodeTemplate> LAST_USED 
        = new ArrayList<NodeTemplate>();
    
    private static final Set<NodeUsageListener> LISTENERS 
        = new HashSet<NodeUsageListener>();
    
    private static List<NodeTemplate> cachedFrequent;

    private NodeUsageRegistry() { }
    
    
    
    /**
     * 
     * @param listener adds the listener (if not already registered), which gets
     * informed if a node was added to thew last used or most frequent nodes
     */
    public static void addNodeUsageListener(
            final NodeUsageListener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }
    
    /**
     * 
     * @param listener deregisters this listener
     */
    public static void removeNodeUsageListener(
            final NodeUsageListener listener) {
        LISTENERS.remove(listener);
    }
    
    private static void notifyListener() {
        for (NodeUsageListener listener : LISTENERS) {
            listener.nodeAdded();
        }
    }
    
    private static void notifyLastHistoryListener() {
        for (NodeUsageListener listener : LISTENERS) {
            listener.usedHistoryChanged();
        }        
    }
    
    private static void notifyFrequencyHistoryListener() {
        for (NodeUsageListener listener : LISTENERS) {
            listener.frequentHistoryChanged();
        }        
    }

    
    /**
     * 
     * @param newMaxSize the new max size for the most frequent 
     *  nodes
     */
    public static void setMaxFrequentSize(final int newMaxSize) {
        cachedFrequent = null;
        maxMostFrequent = newMaxSize;
        notifyFrequencyHistoryListener();
    }

    /**
     * 
     * @param newMaxSize the new max size for the most frequent 
     *  nodes
     */
    public static void setMaxLastUsedSize(final int newMaxSize) {
        synchronized (LAST_USED) {
            maxLastUsed = newMaxSize;
            List<NodeTemplate>temp = new ArrayList<NodeTemplate>(); 
            for (int i = 0; i < Math.min(LAST_USED.size(), maxLastUsed); i++) {
                temp.add(LAST_USED.get(i));
            }
            LAST_USED.clear();
            LAST_USED.addAll(temp);
        }
        notifyLastHistoryListener();
    }
    
    
    /**
     * 
     * @param node the last used node (is added to last used nodes and the
     *  frequency is counted
     */
    public static void addNode(final NodeTemplate node) {
        NodeTemplateFrequency nodeFreq = new NodeTemplateFrequency(node); 
        if (!FREQUENCIES.contains(nodeFreq)) {
            FREQUENCIES.add(nodeFreq);
            Collections.sort(FREQUENCIES);
        }
        cachedFrequent = null;
        int pos = FREQUENCIES.indexOf(nodeFreq);
        
        FREQUENCIES.get(pos).increment();
        Collections.sort(FREQUENCIES);
        
        addToLastUsedNodes(node);
        notifyListener();
    }
    
    private static void addToLastUsedNodes(final NodeTemplate node) {
        // check if it is already contained in
        if (LAST_USED.contains(node)) {
            // if yes move it again to top
            LAST_USED.remove(LAST_USED.lastIndexOf(node));
        }
        LAST_USED.add(0, node);
        // check size of list
        // if larger 10 (TODO: adjustable via preferences)
        if (LAST_USED.size() > maxLastUsed) {
            // remove first node
            LAST_USED.remove(LAST_USED.size() - 1);
        }
    }
    
    /**
     * 
     * @return the n (defined by max size) most frequently used nodes
     */
    public static List<NodeTemplate> getMostFrequentNodes() {
        
        if (cachedFrequent != null) {
            return cachedFrequent;
        }
        List<NodeTemplateFrequency> mostFrequent = FREQUENCIES.subList(0, 
                Math.min(FREQUENCIES.size(), maxMostFrequent));
        cachedFrequent = new ArrayList<NodeTemplate>();
        // TODO: add in correct order
        for (NodeTemplateFrequency freq : mostFrequent) {
            cachedFrequent.add(freq.getNode());
        }
        /*
        for (int i = (mostFrequent.size() - 1); i >= 0; i--) {
            cachedFrequent.add(mostFrequent.get(i).getNode());
        }
        */
        return cachedFrequent;
    }
    
    /**
     * 
     * @return the <code>n</code> most last used nodes (where <code>n</code> 
     * is defined by the max size parameter
     */
    public static List<NodeTemplate>getLastUsedNodes() {
        return LAST_USED;
    }
    
    /**
     * Clears most frequent and last used history.
     */
    public static void clearHistory() {
        clearFrequencyHistory();
        clearLastUsedHistory();
        notifyListener();
    }
    
    private static void clearFrequencyHistory() {
        cachedFrequent = null;
        FREQUENCIES.clear();
    }
    
    private static void clearLastUsedHistory() {
        LAST_USED.clear();
    }
    
    private static class NodeTemplateFrequency 
        implements Comparable<NodeTemplateFrequency> {
        
        private final NodeTemplate m_node;
        private int m_frequency;
        
        /**
         * 
         * @param node creates a new node template frequency for the given 
         * {@link NodeTemplate} with frequency 0
         */
        public NodeTemplateFrequency(final NodeTemplate node) {
            m_node = node;
            m_frequency = 0;
        }
        
        /**
         * Incremetns the freqeuncy of the node template.
         */
        public void increment() {
            m_frequency++;
        }
        
        /**
         * 
         * @return the underlying node
         */
        public NodeTemplate getNode() {
            return m_node;
        }
        
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            return m_node.equals(((NodeTemplateFrequency)obj).m_node);
        }
        
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_node.hashCode();
        }

        /**
         * 
         * {@inheritDoc}
         */
        public int compareTo(final NodeTemplateFrequency o) {
            return o.m_frequency - m_frequency;
        }
        
        
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_node.toString();
        }
    }
    
    private static final String TAG_NODE_ID = "nodeid";
    private static final String TAG_FAVORITE = "favorite";
    private static final String TAG_FREQUENCY = "frequency";
    
    /**
     * Saves most frequent nodes to XML memento. Called from 
     * FavoriteNodesManager#saveFavoriteNodes.
     * 
     * @see #loadFrequentNodes(IMemento)
     * @param freqNodes XML memento to save most frequently used nodes to
     */
    public static void saveFrequentNodes(final IMemento freqNodes) {
        for (NodeTemplateFrequency nodeFreq : FREQUENCIES) {
            IMemento item = freqNodes.createChild(TAG_FAVORITE);
            item.putString(TAG_NODE_ID, nodeFreq.getNode().getID());
            item.putInteger(TAG_FREQUENCY, nodeFreq.m_frequency);
        }
    }

    /**
     * Saves last used nodes to XML memento. Called from 
     * FavoriteNodesManager#saveFavoriteNodes.
     * 
     * @see #loadLastUsedNodes(IMemento)
     * @param lastUsedNodes XML memento to save last used nodes to
     */
    public static void saveLastUsedNodes(final IMemento lastUsedNodes) {
        for (NodeTemplate node : LAST_USED) {
            IMemento item = lastUsedNodes.createChild(TAG_FAVORITE);
            item.putString(TAG_NODE_ID, node.getID());
        }
    }
    
    /**
     * Loads the most frequently used nodes from XML memento. Called from 
     * FavoriteNodesManager#loadFavoriteNodes.
     * 
     * @see #saveFrequentNodes(IMemento)
     * @param freqNodes the XML memento containing the most frequently used 
     *  nodes
     */
    public static void loadFrequentNodes(final IMemento freqNodes) {
        for (IMemento freqNode : freqNodes.getChildren(TAG_FAVORITE)) {
            String id = freqNode.getString(TAG_NODE_ID);
            int frequency = freqNode.getInteger(TAG_FREQUENCY);
            NodeTemplate node = (NodeTemplate)RepositoryManager.INSTANCE
                .getRoot().clone().getChildByID(id, true);
            if (node != null) {
                NodeTemplateFrequency nodeFreq = new NodeTemplateFrequency(
                        node);
                nodeFreq.m_frequency = frequency;
                FREQUENCIES.add(nodeFreq);
            } 
        }
    }
    
    /**
     * Loads the last used nodes from XML memento. Called from 
     * FavoriteNodesManager#loadFavoriteNodes.
     * 
     * @see #saveLastUsedNodes(IMemento)
     * @param lastUsedNodes the XML memento to load the last used nodes from
     */
    public static void loadLastUsedNodes(final IMemento lastUsedNodes) {
        for (IMemento lastNode : lastUsedNodes.getChildren(TAG_FAVORITE)) {
            String id = lastNode.getString(TAG_NODE_ID);
            NodeTemplate node = (NodeTemplate)RepositoryManager.INSTANCE
                .getRoot().clone().getChildByID(id, true);
            if (node != null) {
                addToLastUsedNodes(node);
            }
        }
    }
    
    
}
