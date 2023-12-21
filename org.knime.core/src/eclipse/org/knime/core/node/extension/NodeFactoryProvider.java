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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.extension.NodeSpecCollectionProvider.Progress;
import org.knime.core.node.util.CheckUtils;
import org.osgi.framework.Bundle;

/**
 * Provides access to the factory of each installed node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.2
 */
public final class NodeFactoryProvider {

    private static NodeFactoryProvider instance;

    /** ID of "node" extension point */
    private static final String ID_NODE = "org.knime.workbench.repository.nodes";

    /** ID of "nodesets" extension point */
    private static final String ID_NODE_SET = "org.knime.workbench.repository.nodesets";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeFactoryProvider.class);

    /**
     * Map of node factory class name to its {@link NodeFactoryExtension}, e.g.
     * <ul>
     * <li>"org.knime.base.node.io.filereader.FileReaderNodeFactory" -&gt; ...*</li>
     * <li>"org.knime.cloud.aws.filehandling.nodes.S3connection.S3ConnectionNodeFactory" -&gt; ...</li>
     * </ul>
     */
    private Map<String, NodeFactoryExtension> m_factoryNameToNodeFactoryExtensionMap;

    /**
     * Map of dynamic node factory class name to its {@link NodeSetFactoryExtension}, e.g.
     * <ul>
     * <li>"org.knime.dynamic.js.v212.DynamicJSNodeFactory" -&gt; 'instance-1'</li>
     * <li>"org.knime.dynamic.js.v30.DynamicJSNodeFactory" -&gt; 'instance-1'</li>
     * <li>"com.knime.bigdata.spark.node.io.database.reader.Database2SparkNodeFactory" -&gt; 'instance-2'</li>
     * </ul>
     */
    private Map<String, NodeSetFactoryExtension> m_factoryNameToNodeSetFactoryExtensionMap;

    private Collection<Class<NodeFactory<NodeModel>>> m_loadedNodeFactories = new ConcurrentLinkedDeque<>();

    private NodeFactoryProvider() {
    }

    /**
     * @return the singleton instance
     */
    public static synchronized NodeFactoryProvider getInstance() {
        // NB: getInstance is called by `NodeFactory`s during collectNodeFactoryExtensions to add themselves via
        // {@link #addLoadedFactory(Class)} obtaining an instance that is not fully initialized
        if(instance == null) {
            instance = new NodeFactoryProvider();

            long start = System.currentTimeMillis();
            instance.m_factoryNameToNodeFactoryExtensionMap = collectNodeFactoryExtensions();
            instance.logNodeStats(start);

            start = System.currentTimeMillis();
            instance.m_factoryNameToNodeSetFactoryExtensionMap = collectNodeSetFactoryExtensions();
            instance.logNodeSetStats(start);
        }
        return instance;
    }

    /**
     * Attempts to instantiate a concrete {@link NodeFactory} with the given fully qualified class name. It will first
     * consult the regular node extension, then the node set extensions, then node registered through
     * {@link NodeFactory#addLoadedFactory(Class)}, and finally give up and throw an exception.
     *
     * @param factoryClassName fully qualified class name
     * @return The factory instance
     * @throws InstantiationException Problems invoking the factory constructor
     * @throws IllegalAccessException Problems invoking the factory constructor
     * @throws InvalidNodeFactoryExtensionException Problems finding the class despite it being registered through an
     *             extension point
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    public <T extends NodeModel> Optional<NodeFactory<T>> getNodeFactory(final String factoryClassName)
        throws InstantiationException, IllegalAccessException, InvalidNodeFactoryExtensionException {
        final var nodeFactoryExtension = m_factoryNameToNodeFactoryExtensionMap.get(factoryClassName);
        if (nodeFactoryExtension != null) {
            return Optional.of((NodeFactory<T>)nodeFactoryExtension.createFactory());
        }
        final var nodeSetFactoryExtension = m_factoryNameToNodeSetFactoryExtensionMap.get(factoryClassName);
        if (nodeSetFactoryExtension != null) {
            Class<? extends NodeFactory<? extends NodeModel>> classForFactoryClassName =
                nodeSetFactoryExtension.getClassForFactoryClassName(factoryClassName);
            return Optional.of((NodeFactory<T>)classForFactoryClassName.newInstance());
        }
        Optional<Class<NodeFactory<NodeModel>>> loadedNodeFactory = m_loadedNodeFactories.stream() //
            .filter(f -> f.getClass().getName().equals(factoryClassName)).findFirst();
        if (loadedNodeFactory.isPresent()) {
            return Optional.of((NodeFactory<T>)loadedNodeFactory.get().newInstance());
        }
        return Optional.empty();
    }

    /**
     *
     * @param <T>
     * @param nodeSpec
     * @return the node factory with additional factory settings loaded, e.g., a node dir for a dynamic node factory
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvalidNodeFactoryExtensionException
     * @throws InvalidSettingsException
     */
    public <T extends NodeModel> Optional<NodeFactory<T>> getInitializedNodeFactory(final NodeSpec nodeSpec)
            throws InstantiationException, IllegalAccessException, InvalidNodeFactoryExtensionException,
            InvalidSettingsException {
        final var optFactory = this.<T>getNodeFactory(nodeSpec.factory().className());

        if (optFactory.isPresent()) {
            final var uninitialized = optFactory.get();
            if (uninitialized instanceof DynamicNodeFactory<?> dynamic) {
                dynamic.loadAdditionalFactorySettings(nodeSpec.factory().factorySettings());
            }
            if (nodeSpec.deprecated()) {
                Node.invokeNodeFactorySetDeprecated(uninitialized);
            }
        }

        return optFactory;
    }

    /**
     * @param factoryClass
     */
    @SuppressWarnings("unchecked")
    public void addLoadedFactory(final Class<? extends NodeFactory<NodeModel>> factoryClass) {
        m_loadedNodeFactories.add((Class<NodeFactory<NodeModel>>)factoryClass);
    }

    /**
     * @return
     */
    Map<Bundle, Set<INodeFactoryExtension>> getAllExtensions() {
        final var allExtensions = new HashMap<Bundle, Set<INodeFactoryExtension>>();
        for (final var nodeFactoryExtension : m_factoryNameToNodeFactoryExtensionMap.values()) {
            var bundle = nodeFactoryExtension.getBundle()
                    .orElseThrow(() -> new IllegalStateException("Node factory extension %s does not specify a bundle"
                        .formatted(nodeFactoryExtension.getFactoryClassName())));
            allExtensions.computeIfAbsent(bundle, k -> new HashSet<>()).add(nodeFactoryExtension);
        }
        for (final var nodeSetFactoryExtension : m_factoryNameToNodeSetFactoryExtensionMap.values()) {
            var bundle = nodeSetFactoryExtension.getBundle().orElseThrow(() -> new IllegalStateException(
                "Node set factory extension %s does not specify a bundle".formatted(nodeSetFactoryExtension)));
            allExtensions.computeIfAbsent(bundle, k -> new HashSet<>()).add(nodeSetFactoryExtension);
        }
        return allExtensions;
    }

    private static Map<String, NodeFactoryExtension> collectNodeFactoryExtensions() {
        final var factoryNameToExtension = new LinkedHashMap<String, NodeFactoryExtension>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(ID_NODE);
        CheckUtils.checkState(point != null, "Invalid extension point: %s", ID_NODE);
        var configElements = registry.getConfigurationElementsFor(ID_NODE);

        Progress.setWork(Progress.Stage.NODE_FACTORY, configElements.length);

        for (var configElement : configElements) {
            try {
                final var nodeFactoryExtension = new NodeFactoryExtension(configElement);
                final var factoryClassName = nodeFactoryExtension.getFactoryClassName();
                if (factoryNameToExtension.containsKey(factoryClassName)) {
                    NodeLogger.getLogger(NodeFactoryProvider.class)
                        .debug("Duplicate node factory id: %s".formatted(factoryClassName));
                }
                factoryNameToExtension.put(factoryClassName, nodeFactoryExtension);
                Progress.incrementDone(Progress.Stage.NODE_FACTORY, 1);
            } catch (IllegalArgumentException iae) {
                NodeLogger.getLogger(NodeFactoryProvider.class).error(iae.getMessage(), iae);
            }
        }
        return factoryNameToExtension;
    }

    private static Map<String, NodeSetFactoryExtension> collectNodeSetFactoryExtensions() {
        final var factoryNameToExtension = new LinkedHashMap<String, NodeSetFactoryExtension>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(ID_NODE_SET);
        CheckUtils.checkState(point != null, "Invalid extension point: %s", ID_NODE_SET);

        var configElements = registry.getConfigurationElementsFor(ID_NODE_SET);
        var nodeSetFactoryExtensions = Arrays.stream(configElements)//
            .parallel()// extensions will be activated in this stream, which is expensive
            .map(NodeSetFactoryExtension::from) //
            .flatMap(Optional::stream) //
            .toList();

        Progress.setWork(Progress.Stage.NODE_SET_FACTORY, nodeSetFactoryExtensions.size());

        for (NodeSetFactoryExtension nodeSetFactoryExtension : nodeSetFactoryExtensions) {
            nodeSetFactoryExtension.getClassNameToFactoryMap().keySet().stream()
                .forEach(clName -> factoryNameToExtension.put(clName, nodeSetFactoryExtension));
            Progress.incrementDone(Progress.Stage.NODE_SET_FACTORY, 1);
        }
        return factoryNameToExtension;
    }

    /**
     * @param start {@link System#currentTimeMillis()} when the collection of node set factory extensions was started
     */
    private void logNodeSetStats(final long start) {
        long nodeSetCount = m_factoryNameToNodeSetFactoryExtensionMap.values().stream().distinct().count();
        int nodesCount = m_factoryNameToNodeSetFactoryExtensionMap.size();
        NodeLogger.getLogger(NodeFactoryProvider.class).debugWithFormat(
            "Collected %s extensions... found %d node sets with %d nodes in %.1fs",
            NodeSetFactory.class.getSimpleName(), nodeSetCount, nodesCount,
            (System.currentTimeMillis() - start) / 1000.0);
    }

    /**
     * @param start {@link System#currentTimeMillis()} when the collection of node factory extensions was started
     */
    private void logNodeStats(final long start) {
        long totalNodes = m_factoryNameToNodeFactoryExtensionMap.size();
        long deprecatedNodes = m_factoryNameToNodeFactoryExtensionMap.values()//
            .stream().filter(NodeFactoryExtension::isDeprecated).count();
        long hiddenNodes = m_factoryNameToNodeFactoryExtensionMap.values()//
            .stream().filter(NodeFactoryExtension::isHidden).count();

        NodeLogger.getLogger(NodeFactoryProvider.class).debugWithFormat(
            "Collected %s extensions... found %d nodes (%d deprecated, %d hidden) in %.1fs",
            NodeFactory.class.getSimpleName(), totalNodes, deprecatedNodes, hiddenNodes,
            (System.currentTimeMillis() - start) / 1000.0);
    }

    /**(
     * @return empty if the factory was added at runtime {@link #addLoadedFactory(Class)}
     */
    private Optional<INodeFactoryExtension> getExtension(final NodeFactory<?> factory) {
        final var fcn = factory.getClass().getName();
        return Optional.ofNullable((INodeFactoryExtension)m_factoryNameToNodeFactoryExtensionMap.get(fcn))
            .or(() -> Optional.ofNullable(m_factoryNameToNodeSetFactoryExtensionMap.get(fcn)));
    }

    /**
     * @param factory that is problematic or creates the problematic node
     * @param message describes the problem
     * @param throwable
     */
    void logExtensionProblem(final NodeFactory<?> factory, final String message, final Throwable throwable) {
        final var fcn = factory.getClass().getName();
        final var optExt = getExtension(factory);
        final var extName = optExt.flatMap(INodeFactoryExtension::getInstallableUnitName).orElse("providing " + fcn);
        LOGGER.warn("Problem with extension \"%s\". Some nodes might not be available. ".formatted(extName)
            + "Please contact the vendor of the extension. "
            + "Node factory %s reported \"%s\"".formatted(fcn, message), throwable);
    }

    /**
     * @param ext extension that threw an error or exception
     * @param throwable
     */
    static void logExtensionProblem(final INodeFactoryExtension ext, final Throwable throwable) {
        final var extName = ext.getInstallableUnitName().orElse(ext.getPlugInSymbolicName());
        final var extType = ext instanceof NodeSetFactoryExtension ? " set" : "";
        LOGGER
            .warn("Problem with node%s extension \"%s\". Some nodes might not be available.".formatted(extType, extName)
                + " Please contact the vendor of the extension.", throwable);
    }
}
