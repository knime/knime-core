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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.knime.core.node.NodeFactory;
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
     * {@link #getInstance()}. At the end of {@link #getInstance()} write access is passed on to the
     * NodeSpecCache which updates the progress for node spec collection.
     * </p>
     *
     * @since 5.2
     */
    public static final class Progress {

        /**
         * Property name for initialization progress events.
         *
         * @see #addPropertyChangeListener(java.beans.PropertyChangeListener)
         */
        public static final String PROGRESS_PROPERTY = "progress";

        /**
         * Property name for the event that is fired when the initialization is complete.
         *
         * @see #addPropertyChangeListener(java.beans.PropertyChangeListener)
         */
        public static final String ALL_DONE_PROPERTY = "allDone";

        /**
         * {@link NodeFactoryExtensionManager} initialization follows three stages.
         */
        public enum Stage {
                /** Collecting node factory extensions */
                NODE_FACTORY,
                /** Collecting node set factory extensions */
                NODE_SET_FACTORY,
                /** Creating node metadata */
                NODE_METADATA
        }

        /**
         * Represents the progress of the initialization of the {@link NodeSpecCollectionProvider} singleton.
         * @since 5.2
         */
        public static final class ProgressEvent {

            /** The current step of the initialization. */
            public final Stage m_currentStage;

            /**
             * Null for stage NODE_FACTORY and NODE_SET_FACTORY. After metadata indexing has started, this is the name
             * of the feature that is currently being loaded, e.g., "Vernalis KNIME Nodes", "KNIME Google Connectors",
             * or "KNIME Javasnippet".
             */
            public final String m_extensionName;

            /**
             * Progress over all three stages in range [0, 1].
             */
            public final double m_overallProgress;

            /** Number of units of work done in the current stage. */
            public final long m_stageWorkDone;

            /** Total number of units of work in the current stage. */
            public final long m_stageWorkTotal;

            /**
             * @param stage the current stage of initialization
             * @param extensionName null or in stage NODE_METADATA the name of the feature that is currently being
             *            loaded, e.g., "Vernalis KNIME Nodes",
             * @param overallProgress progress over all three stages in range [0, 1]
             * @param stageWorkDone number of units of work done in the current stage
             * @param stageWorkTotal total number of units of work in the current stage
             */
            private ProgressEvent(final Stage stage, final String extensionName,
                final double overallProgress, final long stageWorkDone, final long stageWorkTotal) {
                m_currentStage = stage;
                m_extensionName = extensionName;
                m_overallProgress = overallProgress;
                m_stageWorkDone = stageWorkDone;
                m_stageWorkTotal = stageWorkTotal;
            }

        }

        /** Progress of initialization, in range [0, 1]. */
        private static double fractionDone;

        /**
         * The currently loaded feature or bundle during node metadata creation.
         */
        private static String loadingFeature;

        private static Stage currentStage = Stage.NODE_FACTORY;

        private static EnumMap<Stage, Long> work = new EnumMap<>(Stage.class);

        private static EnumMap<Stage, Long> done = new EnumMap<>(Stage.class);

        static {
            Arrays.stream(Stage.values()).forEach(s -> work.put(s, 0L));
            Arrays.stream(Stage.values()).forEach(s -> done.put(s, 0L));
        }

        /** Nulled after initialization is complete. */
        private static PropertyChangeSupport progressEvents = new PropertyChangeSupport(Progress.class);

        static synchronized void setWork(final Stage type, final long total) {
            work.put(type, total);
            currentStage = type;
            updateProgress();
        }

        static synchronized void incrementDone(final Stage type, final long completed) {
            done.merge(type, completed, Long::sum);
            currentStage = type;
            updateProgress();
        }

        static synchronized void setLoadingFeature(final String feature) {
            loadingFeature = feature;
            updateProgress();
        }

        static synchronized void setDone() {
            fractionDone = 1.0;
            progressEvents.firePropertyChange(ALL_DONE_PROPERTY, false, true);
            Arrays.stream(progressEvents.getPropertyChangeListeners())
                .forEach(progressEvents::removePropertyChangeListener);
            progressEvents = null;
        }

        /**
         * @return whether the node-spec collection is completely loaded
         */
        public static boolean isDone() {
            return fractionDone == 1.0;
        }

        private static void updateProgress() {
            fractionDone = done.entrySet().stream()
                .mapToDouble(e -> work.get(e.getKey()) == 0 ? 0. : (e.getValue() / (double)work.get(e.getKey()))) //
                .sum() / 3.;
            var event = new ProgressEvent(currentStage, loadingFeature, fractionDone, done.get(currentStage),
                work.get(currentStage));
            progressEvents.firePropertyChange(PROGRESS_PROPERTY, null, event);
        }

        /**
         * <p>
         * The callback should be lightweight, as the callback is executed in the thread that initializes the
         * {@link NodeSpecCollectionProvider} instance (and thus also the {@link NodeFactoryProvider} instance).
         * </p>
         * The property for progress is {@value #PROGRESS_PROPERTY}. A single event with property name
         * {@value #ALL_DONE_PROPERTY} is fired when the initialization is complete.
         *
         * @param listener to be notified when the progress changes.
         *
         * @return false if the initialization process is already complete.
         */
        public static synchronized boolean addPropertyChangeListener(final PropertyChangeListener listener) {
            if (progressEvents == null) {
                return false;
            } else {
                progressEvents.addPropertyChangeListener(listener);
                return true;
            }
        }

        /** @param listener to remove */
        public static synchronized void removePropertyChangeListener(final PropertyChangeListener listener) {
            if (progressEvents != null) {
                progressEvents.removePropertyChangeListener(listener);
            }
        }
    }
}
