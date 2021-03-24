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
 *   Feb 28, 2020 (wiswedel): created
 */
package org.knime.core.node.extension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.util.CheckUtils;

/**
 * Represents the singleton used to collect nodes and node sets (such as for weka, spark, etc.)
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public final class NodeFactoryExtensionManager {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeFactoryExtensionManager.class);

    /** ID of "node" extension point */
    static final String ID_NODE = "org.knime.workbench.repository.nodes";

    /** ID of "nodesets" extension point */
    static final String ID_NODE_SET = "org.knime.workbench.repository.nodesets";

    /**
     * List of nodes added via {@link NodeFactory#addLoadedFactory(Class)} (even though that is discourages as of 4.2)
     */
    private final Collection<Class<? extends NodeFactory<NodeModel>>> m_loadedNodeFactories =
        new ConcurrentLinkedQueue<>();

    private static NodeFactoryExtensionManager instance;

    /** Map of node factory class name to its {@link NodeFactoryExtension}, e.g.
     * <ul>
     * <li>"org.knime.base.node.io.filereader.FileReaderNodeFactory" -&gt; ...* </li>
     * <li>"org.knime.cloud.aws.filehandling.nodes.S3connection.S3ConnectionNodeFactory" -&gt; ...</li>
     * </ul>
     */
    private final Map<String, NodeFactoryExtension> m_factoryNameToNodeFactoryExtensionMap = new LinkedHashMap<>();

    /** Map of dynamic node factory class name to its {@link NodeSetFactoryExtension}, e.g.
     * <ul>
     * <li>"org.knime.dynamic.js.v212.DynamicJSNodeFactory" -&gt; 'instance-1' </li>
     * <li>"org.knime.dynamic.js.v30.DynamicJSNodeFactory" -&gt; 'instance-1'</li>
     * <li>"com.knime.bigdata.spark.node.io.database.reader.Database2SparkNodeFactory" -&gt; 'instance-2' </li>
     * </ul>
     */
    private final Map<String, NodeSetFactoryExtension> m_factoryNameToNodeSetFactoryExtensionMap =
        new LinkedHashMap<>();

    private List<NodeSetFactoryExtension> m_nodeSetFactoryExtensions;

    /**
     * @return the singleton instance
     */
    public static synchronized NodeFactoryExtensionManager getInstance() {
        if (instance == null) {
            instance = new NodeFactoryExtensionManager();
            long start = System.currentTimeMillis();
            instance.collectNodeFactoryExtensions();
            long totalNodes = instance.m_factoryNameToNodeFactoryExtensionMap.size();
            long deprecatedNodes = instance.m_factoryNameToNodeFactoryExtensionMap.values()//
                    .stream().filter(NodeFactoryExtension::isDeprecated).count();
            long hiddenNodes = instance.m_factoryNameToNodeFactoryExtensionMap.values()//
                    .stream().filter(NodeFactoryExtension::isHidden).count();

            LOGGER.debugWithFormat("Collected %s extensions... found %d nodes (%d deprecated, %d hidden) in %.1fs",
                NodeFactory.class.getSimpleName(), totalNodes, deprecatedNodes, hiddenNodes,
                (System.currentTimeMillis() - start) / 1000.0);
            start = System.currentTimeMillis();
            instance.collectNodeSetFactoryExtensions();
            long nodeSetCount = instance.m_factoryNameToNodeSetFactoryExtensionMap.values().stream().distinct().count();
            int nodesCount = instance.m_factoryNameToNodeSetFactoryExtensionMap.size();
            LOGGER.debugWithFormat("Collected %s extensions... found %d node sets with %d nodes in %.1fs",
                NodeSetFactory.class.getSimpleName(), nodeSetCount, nodesCount,
                (System.currentTimeMillis() - start) / 1000.0);

        }
        return instance;
    }

    private NodeFactoryExtensionManager() {
    }

    /**
     * @return iterator over all known node extensions.
     */
    public Iterable<NodeFactoryExtension> getNodeFactoryExtensions() {
        return m_factoryNameToNodeFactoryExtensionMap.values();
    }

    /**
     * @return iterator over all known node sets.
     */
    public Iterable<NodeSetFactoryExtension> getNodeSetFactoryExtensions() {
        return m_nodeSetFactoryExtensions;
    }

    /**
     * Attempts to instantiate a concrete {@link NodeFactory} with the given fully qualified class name. It will first
     * consult the regular node extension, then the node set extensions, then node registered through
     * {@link NodeFactory#addLoadedFactory(Class)}, and finally give up an throw an exception.
     *
     * @param factoryClassName fully qualified class name
     * @return The factory instance
     * @throws InstantiationException Problems invoking the factory constructor
     * @throws IllegalAccessException Problems invoking the factory constructor
     * @throws InvalidNodeFactoryExtensionException Problems finding the class despite it being registered through an
     *             extension point
     */
    public Optional<NodeFactory<? extends NodeModel>> createNodeFactory(final String factoryClassName)
        throws InstantiationException, IllegalAccessException, InvalidNodeFactoryExtensionException {
        NodeFactoryExtension nodeFactoryExtension = m_factoryNameToNodeFactoryExtensionMap.get(factoryClassName);
        if (nodeFactoryExtension != null) {
            return Optional.of(nodeFactoryExtension.createFactory());
        }
        NodeSetFactoryExtension nodeSetFactoryExtension = m_factoryNameToNodeSetFactoryExtensionMap.get(factoryClassName);
        if (nodeSetFactoryExtension != null) {
            Class<? extends NodeFactory<? extends NodeModel>> classForFactoryClassName =
                nodeSetFactoryExtension.getClassForFactoryClassName(factoryClassName);
            return Optional.of(classForFactoryClassName.newInstance());
        }
        Optional<Class<? extends NodeFactory<NodeModel>>> loadedNodeFactory = m_loadedNodeFactories.stream()//
            .filter(f -> f.getClass().getName().equals(factoryClassName)).findFirst();
        if (loadedNodeFactory.isPresent()) {
            return Optional.of(loadedNodeFactory.get().newInstance());
        }
        return Optional.empty();
    }

    private void collectNodeFactoryExtensions() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(ID_NODE);
        CheckUtils.checkState(point != null, "Invalid extension point: %s", ID_NODE);
        @SuppressWarnings("null")
        Iterator<IConfigurationElement> it = Arrays.stream(point.getExtensions())//
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())).iterator();
        while (it.hasNext()) {
            try {
                NodeFactoryExtension nodeFactoryExtension = new NodeFactoryExtension(it.next());
                m_factoryNameToNodeFactoryExtensionMap.put(nodeFactoryExtension.getFactoryClassName(), nodeFactoryExtension);
            } catch (IllegalArgumentException iae) {
                LOGGER.error(iae.getMessage(), iae);
            }

        }
    }

    private void collectNodeSetFactoryExtensions() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(ID_NODE_SET);
        CheckUtils.checkState(point != null, "Invalid extension point: %s", ID_NODE_SET);
        m_nodeSetFactoryExtensions = Arrays.stream(point.getExtensions())//
            .flatMap(ext -> Stream.of(ext.getConfigurationElements()))//
            .parallel()// extensions will be activated in this stream, which is expensive
            .map(NodeSetFactoryExtension::from)//
            .filter(Optional::isPresent)//
            .map(Optional::get)//
            .collect(Collectors.toList());
        for (NodeSetFactoryExtension nodeSetFactoryExtension : m_nodeSetFactoryExtensions) {
            nodeSetFactoryExtension.getClassNameToFactoryMap().keySet().stream()
                .forEach(clName -> m_factoryNameToNodeSetFactoryExtensionMap.put(clName, nodeSetFactoryExtension));
        }
    }

    /**
     * Added in 4.2 as implementation to {@link NodeFactory#addLoadedFactory(Class)}. Access discouraged and also
     * deprecated as API in NodeFactory.
     * @param factoryClass The factory to add
     * @since 4.2
     */
    public void addLoadedFactory(final Class<? extends NodeFactory<NodeModel>> factoryClass) {
        m_loadedNodeFactories.add(factoryClass);
    }

    @Override
    public String toString() {
        long totalNodes = m_factoryNameToNodeFactoryExtensionMap.size();
        long deprecatedNodes = m_factoryNameToNodeFactoryExtensionMap.values()//
                .stream().filter(NodeFactoryExtension::isDeprecated).count();
        long hiddenNodes = m_factoryNameToNodeFactoryExtensionMap.values()//
                .stream().filter(NodeFactoryExtension::isHidden).count();
        long nodesets = m_nodeSetFactoryExtensions.size();
        return String.format("%d nodes (%d deprecated, %d hidden), %d node sets", totalNodes, deprecatedNodes,
            hiddenNodes, nodesets);
    }
}
