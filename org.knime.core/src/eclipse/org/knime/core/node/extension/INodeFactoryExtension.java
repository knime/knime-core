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
 *   Oct 18, 2023 (leonard.woerteler): created
 */
package org.knime.core.node.extension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.osgi.framework.Bundle;

/**
 *
 * @author Leonard Woerteler, KNIME AG, Zurich, Switzerland
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
sealed interface INodeFactoryExtension permits NodeFactoryExtension, NodeSetFactoryExtension {

    /**
     * @return the internal name of the plugin, e.g., "org.knime.base"
     */
    String getPlugInSymbolicName();

    /**
     * Category path as per {@link org.knime.core.node.NodeSetFactory#getCategoryPath(String)}.
     *
     * @param id node factory id
     * @return nullable category path. Might be an empty string.
     */
    String getCategoryPath(String id);

    /**
     * @return the {@link NodeFactoryExtension#isHidden()} or the {@link NodeSetFactory#isHidden()}.
     */
    boolean isHidden();

    /**
     * The "after-id" as defined through {@link org.knime.core.node.NodeSetFactory#getAfterID(String)}.
     *
     * @param id node factory id
     * @return nullable after id. Might be just "/".
     */
    String getAfterID(String id);

    /**
     * @return the number of nodes defined through this {@link NodeSetFactory} or one for {@link NodeFactoryExtension}s.
     */
    default long getNumberOfNodes() {
        return 1;
    }

    /**
     * @return {@link NodeSetFactory#getNodeFactoryIds()} or
     */
    default Collection<String> getNodeFactoryIds() {
        return List.of("0");
    }

    /**
     * @param id for the factory
     * @return factory for the given id
     * @see #getNodeFactoryIds()
     */
    Optional<NodeFactory<? extends NodeModel>> getNodeFactory(final String id);

    /**
     * @return the OSGI bundle if it can be resolved from the {@link #getPlugInSymbolicName()}
     */
    default Optional<Bundle> getBundle() {
        return Optional.ofNullable(Platform.getBundle(getPlugInSymbolicName()));
    }

    /**
     * @return the name of the feature or the bundle name if the feature name is not available.
     */
    default Optional<String> getInstallableUnitName() {
        return getFeatureName().or(this::getBundleName);
    }

    /**
     * @return name of the feature, e.g., "KNIME Base nodes"
     */
    default Optional<String> getFeatureName() {
        return getBundle()//
            .flatMap(OSGIHelper::getFeature) //
            .map(f -> f.getProperty(IInstallableUnit.PROP_NAME, null));
    }

    /**
     * @return the name of the bundle, e.g., "KNIME Date and Time Handling"
     */
    default Optional<String> getBundleName() {
        return getBundle().map(bundle -> bundle.getHeaders().get("Bundle-Name"));
    }

    /**
     * @return whether this node set contains nodes that cannot be added by the user via the node repository, e.g.,
     *         <code>Component Input</code>.
     */
    default boolean isInternal() {
        return false;
    }
}
