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

import java.util.Optional;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.FactoryIDUniquifierProvider;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.CheckUtils;
import org.osgi.framework.Bundle;

/**
 * A node defined via extension point "org.knime.workbench.repository.nodes". Encapsulates everything known from a
 * node extension (via extension point), i.e. factory class name, (lazily initialized factory), contributing plug-in,
 * etc.
 *
 * <p>
 * Used within the framework to make node implementations known to the framework/core and the workbench.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public final class NodeFactoryExtension implements INodeFactoryExtension {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeFactoryExtension.class);

    private static final String FACTORY_CLASS_ATTRIBUTE = "factory-class";

    private final String m_factoryClassName;

    private final IConfigurationElement m_configurationElement;

    /**
     * null = not yet read
     * true = declared in ext point
     * false = ...
     */
    private Boolean m_isDeprecated;

    /**
     * Cache for a node factory instance.
     */
    private NodeFactory<? extends NodeModel> m_factory;

    /**
     * @param factoryClassName
     * @param configurationElement
     */
    NodeFactoryExtension(final IConfigurationElement configurationElement) {
        m_configurationElement = configurationElement;
        m_factoryClassName = configurationElement.getAttribute(FACTORY_CLASS_ATTRIBUTE);
        CheckUtils.checkArgument(StringUtils.isNotBlank(m_factoryClassName),
            "Factory class name in attribute \"%s\" must not be blank (contributing plug-in %s)",
            FACTORY_CLASS_ATTRIBUTE, configurationElement.getContributor().getName());
    }

    /** @return symbolic name of the plug-in contributing the node/extension. */
    @Override
    public String getPlugInSymbolicName() {
        return m_configurationElement.getContributor().getName();
    }

    /**
     * @return the factoryClassName as per extension point definition, not null.
     */
    public String getFactoryClassName() {
        return m_factoryClassName;
    }

    /**
     * NXT-2175: we used to instantiate the node factory here to check if the node description might declare the node as
     * deprecated (without having the deprecation flag on the extension set). As this adds several seconds to the
     * startup we only return the information from the extension.
     *
     * @return the "deprecated" field in the extension point.
     */
    public boolean isDeprecated() {
        if (m_isDeprecated == null) {
            final var isDeprecated = Boolean.parseBoolean(m_configurationElement.getAttribute("deprecated"));
            // there used to be a consistency check here that would warn developers if they forgot to mark the extension
            // deprecated. This warning is now issued in NodeSpec.
            m_isDeprecated = Boolean.valueOf(isDeprecated);
        }
        return m_isDeprecated;
    }

    @Override
    public boolean isHidden() {
        return Boolean.parseBoolean(m_configurationElement.getAttribute("hidden"));
    }

    /** @return the "after" field in the extension point or an empty string. */
    public String getAfterID() {
        return ObjectUtils.defaultIfNull(m_configurationElement.getAttribute("after"), "");
    }

    @Override
    public String getAfterID(final String id) {
        return getAfterID();
    }

    /** @return the "category-path" field in the extension point or "/". */
    public String getCategoryPath() {
        return ObjectUtils.defaultIfNull(m_configurationElement.getAttribute("category-path"), "/");
    }

    @Override
    public String getCategoryPath(final String id) {
        return getCategoryPath();
    }

    /**
     * Gives access to a cached(!) factory instance. When called for the first time, this single instance will be
     * created. Every subsequent call will return the very same instance.
     *
     * It is intended to avoid the unnecessary creation of the node factory instances (which is a bit costly due to xml
     * parsing etc.). Usually used by node repository implementations.
     *
     * @return a factory instance or an empty Optional if there couldn't be found one for the given id
     * @throws InvalidNodeFactoryExtensionException if the creation on the first call fails
     */
    public synchronized NodeFactory<? extends NodeModel> getFactory() throws InvalidNodeFactoryExtensionException {
        if (m_factory == null) {
            m_factory = createFactory();
        }
        return m_factory;
    }

    @Override
    public Optional<NodeFactory<? extends NodeModel>> getNodeFactory(final String id) {
        try {
            return Optional.of(getFactory());
        } catch (InvalidNodeFactoryExtensionException ex) {
            LOGGER.warn("Could not create node factory", ex);
            return Optional.empty();
        }
    }

    /** Returns a new factory instance.
     * @return a new instance, not null.
     * @throws InvalidNodeFactoryExtensionException if that fails
     */
    public synchronized NodeFactory<? extends NodeModel> createFactory()
        throws InvalidNodeFactoryExtensionException {
        final IConfigurationElement el = m_configurationElement;
        try {
            @SuppressWarnings("unchecked")
            NodeFactory<? extends NodeModel> factory =
                (NodeFactory<? extends NodeModel>)el.createExecutableExtension(FACTORY_CLASS_ATTRIBUTE);
            if (factory instanceof FactoryIDUniquifierProvider) {
                throw new InvalidNodeFactoryExtensionException(
                    "Factory '" + m_factoryClassName + "'" + " can create multiple nodes, "
                    + "but is registered as normal node factory.");
            }
            if (Boolean.parseBoolean(m_configurationElement.getAttribute("deprecated"))) {
                // only needed in case plugin.xml and node description xml are not in sync wrt deprecation status
                Node.invokeNodeFactorySetDeprecated(factory);
            }
            return factory;
        } catch (Throwable e) {
            final String pi = getPlugInSymbolicName();
            String message = String.format("Node '%s' from plugin '%s' could not be created.", m_factoryClassName, pi);
            Bundle bundle = Platform.getBundle(pi);

            if ((bundle == null) || (bundle.getState() != Bundle.ACTIVE)) {
                // if the plugin is null, the plugin could not be activated maybe due to a not
                // activateable plugin (plugin class cannot be found)
                message += " The corresponding plugin bundle could not be activated!";
            }
            throw new InvalidNodeFactoryExtensionException(message, e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)%s", getFactoryClassName(), getPlugInSymbolicName(),
            Boolean.parseBoolean(m_configurationElement.getAttribute("deprecated")) ? " -- deprecated" : "");
    }

}
