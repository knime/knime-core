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
 *   18 Oct 2023 (carlwitt): created
 */
package org.knime.core.node.extension;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.extension.NodeSpecCollectionProvider.Progress.Stage;
import org.osgi.framework.Bundle;

/**
 * Extracts node information (name, vendor information, port types, etc.) from node factories. This requires node
 * instantiation and can be expensive for a large set of nodes.
 * <p>
 * Stores the information on disk in the bundle data directories, see {@link DiskBasedNodeSpecCache}.
 * </p>
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Leonard Wörteler, KNIME AG, Zurich, Switzerland
 * @since 5.2
 */
final class NodeSpecCache {

    private enum View {
            ALL(nm -> true), //
            ACTIVE(NodeSpec.IS_HIDDEN.or(NodeSpec.IS_DEPRECATED).negate()), //
            HIDDEN(NodeSpec.IS_HIDDEN), //
            DEPRECATED(NodeSpec.IS_DEPRECATED);

        private final Predicate<NodeSpec> m_predicate;

        View(final Predicate<NodeSpec> predicate) {
            m_predicate = predicate;
        }
    }

    /** Contains unmodifiable maps. */
    private final EnumMap<View, Map<String, NodeSpec>> m_views = new EnumMap<>(View.class);

    /**
     * Protects {@link #m_NodeSpec} and {@link #m_nodeSetMetadata} from being read before the {@link Initializer}
     * thread has finished.
     */
    private final CountDownLatch m_initialized = new CountDownLatch(1);

    private final Thread m_initializerThread;

    /**
     * @param allExtensions extension point implementations that contribute nodes or node sets
     * @param catExts node repository category extension point implementations
     * @param updateProgress
     */
    NodeSpecCache(final Map<Bundle, Set<INodeFactoryExtension>> allExtensions,
        final Map<String, CategoryExtension> catExts) {
        m_initializerThread = new Thread(new Initializer(allExtensions, catExts));
        m_initializerThread.setName("Node Repository Initializer");
    }

    /**
     * Start the asynchronous initialization of the cache. This method may only be called once, by the initialization
     * code of {@link NodeSpecCollectionProvider#getInstance()}.
     */
    void startInitialization() {
        // NB this should not be done in the constructor because the thread would have access to the partially
        // constructed object
        m_initializerThread.start();
    }

    private final class Initializer implements Runnable {

        private final Map<String, CategoryExtension> m_categoryExtensions;

        /** To compute metadata from */
        private final Map<Bundle, Set<INodeFactoryExtension>> m_extensions;

        private Initializer(final Map<Bundle, Set<INodeFactoryExtension>> allExtensions,
            final Map<String, CategoryExtension> catExts) {
            m_extensions = allExtensions;
            m_categoryExtensions = catExts;
            var numNodes = m_extensions.values().stream() //
                .flatMap(Set::stream)//
                .filter(Predicate.not(INodeFactoryExtension::isInternal)) //
                .mapToLong(INodeFactoryExtension::getNumberOfNodes)//
                .sum();
            NodeSpecCollectionProvider.Progress.setWork(Stage.NODE_METADATA, numNodes);
        }

        @Override
        public void run() {
            try {
                // cannot load in parallel because creation of executable extensions seems to be able to
                // deadlock on org.eclipse.osgi.internal.serviceregistry.ServiceRegistry
                // however, it might be possible to process nodes in parallel to node sets

                // compute feature names for better progress
                var exts = m_extensions.values().stream()//
                    .flatMap(Set::stream) //
                    .filter(Predicate.not(INodeFactoryExtension::isInternal)) //
                    .collect(Collectors.groupingBy(ext -> ext.getInstallableUnitName().orElse("KNIME Nodes")));

                var allNodes = new HashMap<String, NodeSpec>();
                for (var nameAndExts : exts.entrySet()) {
                    var featureName = nameAndExts.getKey();
                    var extensions = nameAndExts.getValue();
                    NodeSpecCollectionProvider.Progress.setLoadingFeature(featureName);
                    for (var ext : extensions) {
                        var nodeSpecs = DiskBasedNodeSpecCache.get(ext) //
                            .orElse(NodeSpec.of(m_categoryExtensions, ext));
                        // report progress
                        NodeSpecCollectionProvider.Progress.incrementDone(Stage.NODE_METADATA, nodeSpecs.size());
                        for (var ns : nodeSpecs) {
                            var factoryId = ns.factory().id();
                            if (allNodes.containsKey(factoryId)) {
                                NodeLogger.getLogger(getClass()).debug("Duplicate node factory id: " + factoryId);
                            }
                            allNodes.put(factoryId, ns);
                        }
                    }

                }

                for (var view : View.values()) {
                    m_views.put(view, allNodes.values().stream().filter(view.m_predicate)
                        .collect(Collectors.toUnmodifiableMap(ns -> ns.factory().id(), nm -> nm)));
                }
            } finally {
                // signal that the cache is now ready to use
                m_initialized.countDown();
                NodeSpecCollectionProvider.Progress.setDone();
            }
        }
    }

    public Map<String, NodeSpec> getNodes() {
        try {
            m_initialized.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null; // NOSONAR null is more accurate than an empty map
        }
        return m_views.get(View.ALL);
    }

    public Map<String, NodeSpec> getActiveNodes() {
        try {
            m_initialized.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null; // NOSONAR null is more accurate than an empty map
        }
        return m_views.get(View.ACTIVE);
    }

    public Map<String, NodeSpec> getHiddenNodes() {
        try {
            m_initialized.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null; // NOSONAR null is more accurate than an empty map
        }
        return m_views.get(View.HIDDEN);
    }

    public Map<String, NodeSpec> getDeprecatedNodes() {
        try {
            m_initialized.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null; // NOSONAR null is more accurate than an empty map
        }
        return m_views.get(View.DEPRECATED);
    }
}
