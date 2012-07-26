/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   14.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private static final Map<NodeTemplateFrequency, NodeTemplateFrequency> FREQUENCIES =
            new HashMap<NodeTemplateFrequency, NodeTemplateFrequency>();

    private static final LinkedList<NodeTemplate> LAST_USED =
            new LinkedList<NodeTemplate>();

    private static final Set<NodeUsageListener> LISTENERS =
            new HashSet<NodeUsageListener>();

    private static List<NodeTemplate> cachedFrequent;

    private NodeUsageRegistry() {
    }

    /**
     *
     * @param listener adds the listener (if not already registered), which gets
     *            informed if a node was added to thew last used or most
     *            frequent nodes
     */
    public static void addNodeUsageListener(final NodeUsageListener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /**
     *
     * @param listener deregisters this listener
     */
    public static void removeNodeUsageListener(final NodeUsageListener listener) {
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
     * @param newMaxSize the new max size for the most frequent nodes
     */
    public static void setMaxFrequentSize(final int newMaxSize) {
        cachedFrequent = null;
        maxMostFrequent = newMaxSize;
        notifyFrequencyHistoryListener();
    }

    /**
     *
     * @param newMaxSize the new max size for the most frequent nodes
     */
    public static void setMaxLastUsedSize(final int newMaxSize) {
        synchronized (LAST_USED) {
            maxLastUsed = newMaxSize;
            while (LAST_USED.size() > maxLastUsed) {
                LAST_USED.removeLast();
            }
        }
        notifyLastHistoryListener();
    }

    /**
     *
     * @param node the last used node (is added to last used nodes and the
     *            frequency is counted
     */
    public static void addNode(final NodeTemplate node) {
        NodeTemplateFrequency nodeFreq = new NodeTemplateFrequency(node);
        NodeTemplateFrequency existing = FREQUENCIES.get(nodeFreq);
        if (existing == null) {
            FREQUENCIES.put(nodeFreq, nodeFreq);
            existing = nodeFreq;
        }
        cachedFrequent = null;
        existing.increment();
        addToLastUsedNodes(node);
        notifyListener();
    }

    private static void addToLastUsedNodes(final NodeTemplate node) {
        synchronized(LAST_USED) {
            LAST_USED.remove(node);
            LAST_USED.addFirst(node);
            // check size of list
            // if larger 10 (TODO: adjustable via preferences)
            if (LAST_USED.size() > maxLastUsed) {
                // remove first node
                LAST_USED.removeLast();
            }
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
        List<NodeTemplateFrequency> mostFrequent =
                new ArrayList<NodeTemplateFrequency>(FREQUENCIES.values());
        Collections.sort(mostFrequent);
        List<NodeTemplate> temp = new ArrayList<NodeTemplate>();

        int max = Math.min(maxMostFrequent, mostFrequent.size());
        for (int i = 0; i < max; i++) {
            temp.add(mostFrequent.get(i).getNode());
        }
        return (cachedFrequent = temp);
    }

    /**
     *
     * @return the <code>n</code> most last used nodes (where <code>n</code> is
     *         defined by the max size parameter
     */
    public static List<NodeTemplate> getLastUsedNodes() {
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

    private static class NodeTemplateFrequency implements
            Comparable<NodeTemplateFrequency> {

        private final NodeTemplate m_node;

        private int m_frequency;

        /**
         *
         * @param node creates a new node template frequency for the given
         *            {@link NodeTemplate} with frequency 0
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
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result =
                    prime * result + ((m_node == null) ? 0 : m_node.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NodeTemplateFrequency other = (NodeTemplateFrequency)obj;
            if (m_node == null) {
                if (other.m_node != null) {
                    return false;
                }
            } else if (!m_node.equals(other.m_node)) {
                return false;
            }
            return true;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
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
        for (NodeTemplateFrequency nodeFreq : FREQUENCIES.values()) {
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
     *            nodes
     */
    public static void loadFrequentNodes(final IMemento freqNodes) {
        for (IMemento freqNode : freqNodes.getChildren(TAG_FAVORITE)) {
            String id = freqNode.getString(TAG_NODE_ID);
            int frequency = freqNode.getInteger(TAG_FREQUENCY);
            NodeTemplate node = RepositoryManager.INSTANCE.getNodeTemplate(id);
            if (node != null) {
                NodeTemplateFrequency nodeFreq =
                        new NodeTemplateFrequency(node);
                nodeFreq.m_frequency = frequency;
                FREQUENCIES.put(nodeFreq, nodeFreq);
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
            NodeTemplate node = RepositoryManager.INSTANCE.getNodeTemplate(id);
            if (node != null) {
                addToLastUsedNodes(node);
            }
        }
    }

}
