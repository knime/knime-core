/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   31.08.2015 (thor): created
 */
package org.knime.core.node;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

/**
 * Repository that allows access to all node factories registered at the node extension point.
 * This class is currently evolving and therefore not yet exposed to the public
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.0
 */
final class NodeFactoryRepository {
    private static final String EXT_POINT_ID_NODES = "org.knime.workbench.repository.nodes";

    private static final String EXT_POINT_ID_NODE_SETS = "org.knime.workbench.repository.nodesets";

    private static final NodeFactoryRepository INSTANCE = new NodeFactoryRepository();

    /**
     * Returns the singleton instance.
     *
     * @return the node factory repository singleton
     */
    public static NodeFactoryRepository getInstance() {
        return INSTANCE;
    }

    private final Map<String, IConfigurationElement> m_nodeFactories = new HashMap<>();

    private final Map<String, IConfigurationElement> m_nodesetFactories = new HashMap<>();

    private final Map<Class<? extends NodeModel>, Boolean> m_deprecatedFactories = new HashMap<>();

    private NodeFactoryRepository() {
        // read all factories at once, because simply traversing the extension point is quite cheap
        IExtensionRegistry registry = Platform.getExtensionRegistry();

        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID_NODES);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID_NODES;
        Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
            .filter(e -> (e.getAttribute("factory-class") != null))
            .forEach(e -> m_nodeFactories.put(e.getAttribute("factory-class"), e));

        point = registry.getExtensionPoint(EXT_POINT_ID_NODE_SETS);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID_NODE_SETS;
        Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
            .filter(e -> (e.getAttribute("factory-class") != null))
            .forEach(e -> m_nodesetFactories.put(e.getAttribute("factory-class"), e));
    }

    /**
     * Returns whether the given node factory is deprecated. This method checks the extension point for the deprecated
     * flag and will therefore only work for nodes that are still registered.
     *
     * @param fac any node factory
     * @return <code>true</code> if the node is deprecated, <code>false</code> otherwise
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized boolean isDeprecated(final NodeFactory<? extends NodeModel> fac) {
        Boolean deprecated = m_deprecatedFactories.get(fac);
        if (deprecated != null) {
            return deprecated;
        }

        String className = fac.getClass().getName();
        IConfigurationElement facElement = m_nodeFactories.get(className);
        if (facElement != null) {
            deprecated = "true".equalsIgnoreCase(facElement.getAttribute("deprecated"));
        } else {
            // check node sets
            String factoryBundle = FrameworkUtil.getBundle(fac.getClass()).getSymbolicName();
            deprecated = m_nodesetFactories.values().stream()
                .filter(elem -> factoryBundle.equals(elem.getNamespaceIdentifier()))
                .filter(elem -> "true".equalsIgnoreCase(elem.getAttribute("deprecated")))
                .filter(elem -> containsNodeFactory(elem, fac))
                .findFirst().isPresent();
        }

        m_deprecatedFactories.put((Class) fac.getClass(), deprecated);
        return deprecated;
    }

    private static boolean containsNodeFactory(final IConfigurationElement elem, final NodeFactory<? extends NodeModel> fac) {
        try {
            NodeSetFactory setFactory = (NodeSetFactory)elem.createExecutableExtension("factory-class");

            return setFactory.getNodeFactoryIds().stream()
                    .map(id -> setFactory.getNodeFactory(id))
                    .filter(fc -> fc.equals(fac.getClass()))
                    .findFirst().isPresent();
        } catch (CoreException ex) {
            NodeLogger.getLogger(NodeFactoryRepository.class).error("Could not create node set factory '"
                + elem.getAttribute("factory-class") + "' from plug-in '"
                + elem.getNamespaceIdentifier() + "': " + ex.getMessage(), ex);
            return false;
        }
    }
}
