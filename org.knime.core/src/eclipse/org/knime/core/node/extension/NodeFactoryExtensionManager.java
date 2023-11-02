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
import java.util.Set;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.ClassUtils;

/**
 * Represents the singleton used to collect nodes and node sets (such as for weka, spark, etc.)
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @deprecated Use {@link NodeFactoryProvider} instead. Left as a facade in case core gets updated but a dependent
 *             (e.g., workbench, gateway) plugin is not and still expects to find this.
 */
@Deprecated(forRemoval = true)
public final class NodeFactoryExtensionManager {

    private static final NodeFactoryExtensionManager INSTANCE = new NodeFactoryExtensionManager();

    public static synchronized NodeFactoryExtensionManager getInstance() {
        return INSTANCE;
    }

    /**
     * @return iterator over all known node extensions.
     */
    public Iterable<NodeFactoryExtension> getNodeFactoryExtensions() {
        return NodeFactoryProvider.getInstance().getAllExtensions().values().stream() //
            .flatMap(Set::stream) //
            .flatMap(ext -> ClassUtils.castStream(NodeFactoryExtension.class, ext)) //
            .toList();
    }

    /**
     * @return iterator over all known node sets.
     */
    public Iterable<NodeSetFactoryExtension> getNodeSetFactoryExtensions() {
        return NodeFactoryProvider.getInstance().getAllExtensions().values().stream() //
                .flatMap(Set::stream) //
                .flatMap(ext -> ClassUtils.castStream(NodeSetFactoryExtension.class, ext))//
                .toList();
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
        return NodeFactoryProvider.getInstance().getNodeFactory(factoryClassName) //
            .map(nodeFactory -> (NodeFactory<? extends NodeModel>)nodeFactory);
    }

    /**
     * Added in 4.2 as implementation to {@link NodeFactory#addLoadedFactory(Class)}. Access discouraged and also
     * deprecated as API in NodeFactory.
     *
     * @param factoryClass The factory to add
     * @since 4.2
     */
    public void addLoadedFactory(final Class<? extends NodeFactory<NodeModel>> factoryClass) {
        NodeFactoryProvider.getInstance().addLoadedFactory(factoryClass);
    }
}
