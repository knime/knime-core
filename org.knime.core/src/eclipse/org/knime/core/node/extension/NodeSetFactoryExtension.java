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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.osgi.framework.Bundle;

/**
 * A node <i>set</i> defined via extension point &quot;org.knime.workbench.repository.nodeset&quot;. Encapsulates
 * everything known from a node set extension, i.e. factory class name, (lazily initialized factory), contributing
 * plug-in, etc.
 *
 * <p>
 * Used within the framework to make node set implementations known to the framework/core and the workbench.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class NodeSetFactoryExtension implements INodeFactoryExtension {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeSetFactoryExtension.class);

    private static final String SET_FACTORY_CLASS_ATTRIBUTE = "factory-class";

    private final IConfigurationElement m_configurationElement;

    private final NodeSetFactory m_setFactory;

    private final Map<String, Class<? extends NodeFactory<? extends NodeModel>>> m_classNameToFactoryMap;

    private final long m_numberOfNodes;

    /**
     * Cache of node factory instances.
     */
    private Map<String, NodeFactory<? extends NodeModel>> m_nodeFactories;


    /**
     * @param configurationElement
     * @param classToCountMap
     * @param setFactory
     */
    private NodeSetFactoryExtension(final IConfigurationElement configurationElement,
        final NodeSetFactory setFactory,
        final Map<Class<? extends NodeFactory<? extends NodeModel>>, Long> classToCountMap) {
        m_configurationElement = configurationElement;
        m_setFactory = setFactory;
        m_numberOfNodes = classToCountMap.values().stream().mapToLong(Long::longValue).sum();
        m_classNameToFactoryMap = classToCountMap.keySet().stream().collect(Collectors.toMap(Class::getName, f -> f));
    }

    /**
     * @return name of the plug-in defining the extension.
     */
    @Override
    public String getPlugInSymbolicName() {
        return getContributingPlugIn(m_configurationElement);
    }

    /**
     * @return The "default-category-icon" field from the plugin.xml
     */
    public Optional<String> getDefaultCategoryIconPath() {
        // empty optional if no icon path is specified
        return Optional.ofNullable(//
            StringUtils.defaultIfBlank(m_configurationElement.getAttribute("default-category-icon"), null));
    }

    /**
     * @return the setFactory, not <code>null</code>.
     */
    public NodeSetFactory getNodeSetFactory() {
        return m_setFactory;
    }

    /** Get the factory class object for a fully qualified class name; throws an exception if invalid.
     * @param factoryClassName The non-null class name
     * @return The factory class with the same name, not null
     * @throws NoSuchElementException ...
     */
    Class<? extends NodeFactory<? extends NodeModel>> getClassForFactoryClassName(final String factoryClassName) {
        Class<? extends NodeFactory<? extends NodeModel>> cl = m_classNameToFactoryMap.get(factoryClassName);
        if (cl == null) {
            throw new NoSuchElementException(
                String.format("factory class \"%s\" is unknown to node set factory contributed by %s", factoryClassName,
                    getPlugInSymbolicName()));
        }
        return cl;
    }

    @Override
    public long getNumberOfNodes() {
        return m_numberOfNodes;
    }

    /**
     * @return the classNameToFactoryMap
     */
    Map<String, Class<? extends NodeFactory<? extends NodeModel>>> getClassNameToFactoryMap() {
        return m_classNameToFactoryMap;
    }

    /**
     * @return delegates to {@link org.knime.core.node.NodeSetFactory#getNodeFactoryIds()}.
     */
    @Override
    public Collection<String> getNodeFactoryIds() {
        return m_setFactory.getNodeFactoryIds();
    }

    /**
     * Gives access to a cached(!) factory instance for the given id. When called for the first time, this single
     * instance will be created. Every subsequent call with the same id will return the very same instance.
     *
     * It is intended to avoid the unnecessary creation of the node factory instances (which is a bit costly due to xml
     * parsing etc.). Usually used by node repository implementations.
     *
     * @param id as per {@link #getNodeFactoryIds()}
     * @return a factory instance or an empty Optional if there couldn't be found one for the given id
     * @since 4.5
     */
    @Override
    public Optional<NodeFactory<? extends NodeModel>> getNodeFactory(final String id) {
        if (m_nodeFactories == null) {
            m_nodeFactories = new HashMap<>();
        }
        NodeFactory<? extends NodeModel> factory = m_nodeFactories.get(id);
        if (factory == null) {
            factory = createNodeFactory(id).orElse(null);
        }
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory);
    }

    /**
     * Creates a factory instance for the given id, returning and empty optional if that's not possible (error logging
     * done here...)
     *
     * @param id as per {@link #getNodeFactoryIds()}
     * @return new instance of a the factory or an empty Optional.
     */
    public Optional<NodeFactory<? extends NodeModel>> createNodeFactory(final String id) {
        Optional<Class<? extends NodeFactory<? extends NodeModel>>> factoryClass =
            getNodeFactoryClass(m_configurationElement, m_setFactory, id);
        if (!factoryClass.isPresent()) {
            return Optional.empty();
        }
        try {
            NodeFactory<? extends NodeModel> instance = factoryClass.get().newInstance();
            instance.loadAdditionalFactorySettings(m_setFactory.getAdditionalSettings(id));
            if (isDeprecated()) {
                Node.invokeNodeFactorySetDeprecated(instance);
            }
            return Optional.of(instance);
        } catch (Exception e) {
            LOGGER.errorWithFormat(
                "Unable to instantiate class %s for dynamic node set, contributed via \"%s\", id of factory is \"%s\"",
                factoryClass.get().getName(), getPlugInSymbolicName(), id);
            return Optional.empty();
        }
    }

    @Override
    public String getCategoryPath(final String id) {
        return StringUtils.defaultString(m_setFactory.getCategoryPath(id), "");
    }

    @Override
    public String getAfterID(final String id) {
        return StringUtils.defaultIfEmpty(m_setFactory.getAfterID(id), "/");
    }

    /** Parses the "deprecated" field in the extension point contribution. Null values mean "false". This deprecation
     * refers to all nodes in the node set.
     * @return ...
     */
    public boolean isDeprecated() {
        return Boolean.parseBoolean(m_configurationElement.getAttribute("deprecated"));
    }

    @Override
    public boolean isInternal() {
        return Optional.ofNullable(m_configurationElement.getAttribute("internal"))//
            .map(Boolean::parseBoolean)//
            .orElse(false);
    }

    @Override
    public boolean isHidden() {
        return m_setFactory.isHidden();
    }

    @Override
    public String toString() {
        return String.format("%s (via %s) -- %d node(s)", getNodeSetFactory().getClass().getName(),
            getPlugInSymbolicName(), getNumberOfNodes());
    }

    static Optional<NodeSetFactoryExtension> from(final IConfigurationElement configurationElement) {
        String setFactoryClassName = configurationElement.getAttribute(SET_FACTORY_CLASS_ATTRIBUTE);
        final String pluginName = getContributingPlugIn(configurationElement);
        if (StringUtils.isBlank(setFactoryClassName)) {
            LOGGER.errorWithFormat("%s class name in attribute \"%s\" must not be blank (contributing plug-in %s)",
                NodeSetFactory.class.getSimpleName(), SET_FACTORY_CLASS_ATTRIBUTE,
                pluginName);
        }
        NodeSetFactory setFactory;
        try {
            setFactory = (NodeSetFactory)configurationElement.createExecutableExtension(SET_FACTORY_CLASS_ATTRIBUTE);
        } catch (Throwable e) {
            String message = String.format("%s '%s' from plugin '%s' could not be created.",
                NodeSetFactory.class.getSimpleName(), setFactoryClassName, pluginName);
            Bundle bundle = Platform.getBundle(pluginName);

            if ((bundle == null) || (bundle.getState() != Bundle.ACTIVE)) {
                // if the plugin is null, the plugin could not be activated maybe due to a not
                // activateable plugin (plugin class cannot be found)
                message += " The corresponding plugin bundle could not be activated!";
            }
            LOGGER.error(message, e);
            return Optional.empty();
        }
        // unfortunately a NodeSetFactory may return different factory classes for different IDs and these classes
        // need to be known (during workflow load)

        // a map from the distinct NodeFactory classes used in the current NodeSetFactory to their counts, e.g.
        //    org.knime.dynamic.js.v30.DynamicJSNodeFactory -> 12
        //    org.knime.dynamic.js.v212.DynamicJSNodeFactory -> 7
        // (counts only for logging)
        Map<Class<? extends NodeFactory<? extends NodeModel>>, Long> classToCountMap =
            setFactory.getNodeFactoryIds().stream()//
                .map(id -> getNodeFactoryClass(configurationElement, setFactory, id)) //
                .filter(Optional::isPresent)//
                .map(Optional::get)//
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return Optional.of(new NodeSetFactoryExtension(configurationElement, setFactory, classToCountMap));
    }

    /** The name of the plugin (e.g. "org.knime.ext.weka") for a contribution.
     * @param configurationElement non-null
     * @return Name of the plugin as per {@link IConfigurationElement#getContributor()}
     */
    static String getContributingPlugIn(final IConfigurationElement configurationElement) {
        return configurationElement.getContributor().getName();
    }

    /** Calls and returns {@link NodeSetFactory#getNodeFactory(String)} but does all the error handling. */
    private static Optional<Class<? extends NodeFactory<? extends NodeModel>>> getNodeFactoryClass(
        final IConfigurationElement configurationElement, final NodeSetFactory nodeSetFactory,
        final String nodeFactoryID) {
        try {
            Class<? extends NodeFactory<? extends NodeModel>> nodeFactory =
                nodeSetFactory.getNodeFactory(nodeFactoryID);
            if (nodeFactory == null) {
                LOGGER.errorWithFormat(
                    "Unable to load %s class for dynamic node set, contributed via \"%s\", id of factory is \"%s\" -- "
                        + "class object is null",
                    NodeFactory.class.getSimpleName(), getContributingPlugIn(configurationElement), nodeFactoryID);
                return Optional.empty();
            }
            return Optional.of(nodeFactory);
        } catch (Throwable th) {
            LOGGER.error(String.format(
                "Unable to load %s class for dynamic node set, contributed via \"%s\", id of factory is \"%s\"",
                NodeFactory.class.getSimpleName(), getContributingPlugIn(configurationElement), nodeFactoryID), th);
            return Optional.empty();
        }
    }
}
