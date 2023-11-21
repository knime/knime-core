/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Nov 3, 2023 (hornm): created
 */
package org.knime.core.node.extension;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSetFactory;

/**
 * Provides basic information for all installed nodes, e.g., metadata, supported input port types, etc.
 *
 * @since 5.2
 */
public final class NodeSpecCollectionProvider {

    /**
     * Write access to this field is confined to the {@link #getInstance()} method. Visibility to other threads is
     * guaranteed because {@link #getInstance()} is synchronized.
     */
    private static NodeSpecCollectionProvider instance;

    /**
     * This is an expensive operation. Its first invocation might take a couple of seconds.
     *
     * @return the singleton instance
     */
    public static NodeSpecCollectionProvider getInstance() {
        final var nodeFactoryExts = NodeFactoryProvider.getInstance().getAllExtensions();
        synchronized (NodeSpecCollectionProvider.class) {
            if (instance == null) {
                final var catExts = CategoryExtensionUtil.collectCategoryExtensions();

                // index node specs
                var nodeSpecCache = new NodeSpecCache(nodeFactoryExts, catExts);
                nodeSpecCache.startInitialization();

                instance = new NodeSpecCollectionProvider(nodeSpecCache, catExts);
            }
            return instance;
        }
    }

    private final NodeSpecCache m_cache;

    /**
     * Map from the complete category path to the associated {@link CategoryExtension}.
     */
    private final Map<String, CategoryExtension> m_categoryExtensions;

    private NodeSpecCollectionProvider(final NodeSpecCache cache,
        final Map<String, CategoryExtension> categoryExtensions) {
        m_cache = cache;
        m_categoryExtensions = Collections.unmodifiableMap(categoryExtensions);
    }

    /**
     * Blocks until the provider is initialized.
     *
     * @return null if the thread is interrupted while waiting for the provider to be initialized. Otherwise, an
     *         unmodifiable map from {@link NodeFactory#getFactoryId() FactoryId} to node properties for all nodes that
     *         are neither hidden nor deprecated.
     */
    public Map<String, NodeSpec> getActiveNodes() {
        return m_cache.getActiveNodes();
    }

    /**
     * Blocks until the provider is initialized.
     *
     * @return null if the thread is interrupted while waiting for the provider to be initialized. Otherwise, an
     *         unmodifiable map from {@link NodeFactory#getFactoryId() FactoryId} to node properties for all hidden
     *         nodes, i.e.,
     *         <ul>
     *         <li>nodes where the {@link NodeFactoryExtension} defines the the node as hidden or</li>
     *         <li>where the {@link NodeSetFactory} defines the node as hidden.</li>
     *         </ul>
     */
    public Map<String, NodeSpec> getHiddenNodes() {
        return m_cache.getHiddenNodes();
    }

    /**
     * Blocks until the provider is initialized.
     *
     * @return null if the thread is interrupted while waiting for the provider to be initialized. Otherwise, an
     *         unmodifiable map from {@link NodeFactory#getFactoryId() FactoryId} to node properties for all deprecated
     *         nodes, i.e.,
     *         <ul>
     *         <li>nodes where the {@link NodeFactoryExtension} defines the the node as deprecated or</li>
     *         <li>where the {@link NodeSetFactoryExtension} defines the node as deprecated or the {@link NodeFactory}
     *         provided by the {@link NodeSetFactory} defines the node as deprecated.</li>
     *         </ul>
     */
    public Map<String, NodeSpec> getDeprecatedNodes() {
        return m_cache.getDeprecatedNodes();
    }

    /**
     * Blocks until the provider is initialized.
     *
     * @return null if the thread is interrupted while waiting for the provider to be initialized. Otherwise, an
     *         unmodifiable map from {@link NodeFactory#getFactoryId() FactoryId} to node properties for all nodes,
     *         i.e., including hidden and deprecated nodes.
     */
    public Map<String, NodeSpec> getNodes() {
        return m_cache.getNodes();
    }

    /**
     * @return map from {@link CategoryExtension#getCompletePath()} to node repository category
     */
    public Map<String, CategoryExtension> getCategoryExtensions() {
        return m_categoryExtensions;
    }

    /**
     * Keeps track of the overall progress of the {@link #getInstance()} method to inform any thread that wants to
     * access the instance.
     * <p>
     * Does not need to be thread-safe because access is serially confined to the first thread to call
     * {@link #getInstance()}. At the end of {@link #getInstance()} write access is passed on to the NodeSpecCache which
     * updates the progress for node spec collection.
     * </p>
     *
     * @since 5.2
     */
    public static final class Progress {

        /**
         * {@link NodeFactoryExtensionManager} initialization follows three stages.
         */
        enum Stage {
                /** Collecting node factory extensions */
                NODE_FACTORY,
                /** Collecting node set factory extensions */
                NODE_SET_FACTORY,
                /** Creating node metadata */
                NODE_METADATA
        }

        /**
         * Represents the progress of the initialization of the {@link NodeSpecCollectionProvider} singleton.
         *
         * @param extensionName null or in stage NODE_METADATA the name of the feature that is currently being loaded,
         *            e.g., "Vernalis KNIME Nodes",
         * @param overallProgress progress over all three stages in range [0, 1]
         * @param isDone whether initialization has finished
         *
         * @since 5.2
         */
        public static record ProgressEvent(String extensionName, double overallProgress, boolean isDone) {
        }

        private static Set<ProgressListener> listeners = new CopyOnWriteArraySet<>();

        /** Whether initialization has finished. */
        private static boolean complete;

        /**
         * The currently loaded feature or bundle during node metadata creation.
         */
        private static String loadingFeature;

        private static EnumMap<Stage, Long> work = new EnumMap<>(Stage.class);

        private static EnumMap<Stage, Long> done = new EnumMap<>(Stage.class);

        static {
            Arrays.stream(Stage.values()).forEach(s -> work.put(s, 0L));
            Arrays.stream(Stage.values()).forEach(s -> done.put(s, 0L));
        }

        static synchronized void setWork(final Stage type, final long total) {
            work.put(type, total);
            updateProgress();
        }

        static synchronized void incrementDone(final Stage type, final long completed) {
            done.merge(type, completed, Long::sum);
            updateProgress();
        }

        static synchronized void setLoadingFeature(final String feature) {
            loadingFeature = feature;
            updateProgress();
        }

        static synchronized void setDone() {
            complete = true;
            updateProgress();
            listeners.clear();
            listeners = null;
            work = null;
            done = null;
        }

        /**
         * @return whether the node-spec collection is completely loaded
         */
        public static synchronized boolean isDone() {
            return complete;
        }

        private static void updateProgress() {
            final var fractionDone = done.entrySet().stream()
                .mapToDouble(e -> work.get(e.getKey()) == 0 ? 0. : (e.getValue() / (double)work.get(e.getKey()))) //
                .sum() / 3.;
            final var event = new ProgressEvent(loadingFeature, fractionDone, complete);
            for (var listener : listeners) {
                try {
                    // since this is executed on the node repository initializer thread, we must protect against
                    // abnormal termination due to exceptions bubbling up from listener code
                    listener.progress(event);
                } catch (Exception ex) { // NOSONAR can't restrict the exceptions thrown by arbitrary listeners
                    NodeLogger.getLogger(NodeSpecCollectionProvider.class)
                        .warn("Exception in listener to the node spec cache initialization progress.", ex);
                }
            }
        }

        /**
         * @param listener notified every time progress is made
         * @return false and take no action if initialization is already complete
         */
        public static synchronized boolean addListener(final ProgressListener listener) {
            if (complete) {
                return false;
            }
            listeners.add(listener);
            return true;
        }

        /** @param listener to be removed */
        public static synchronized void removeListener(final ProgressListener listener) {
            if (listeners != null) {
                listeners.remove(listener);
            }
        }

        /**
         * To receive updates on the progress of the initialization of the {@link NodeSpecCollectionProvider} singleton.
         */
        public interface ProgressListener {
            /** @param progressEvent current progress */
            void progress(ProgressEvent progressEvent);
        }
    }
}
