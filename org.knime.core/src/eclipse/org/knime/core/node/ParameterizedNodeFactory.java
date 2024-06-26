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
 *   May 2, 2024 (Jonas Klotz): created
 */
package org.knime.core.node;

/**
 * Implemented by {@link NodeFactory}-instances to indicate that they can create multiple types of nodes. It is only
 * necessary if the node factory is created as part of a node set (see {@link NodeSetFactory#getNodeFactory(String)}).
 * Implementing this interface has two important effects:
 * <ul>
 * <li>It ensures that the node factories will be initialized with the appropriate parameters, accessible by overwriting
 * {@link NodeFactory#loadAdditionalFactorySettings}. The parameters are initially defined in
 * {@link NodeSetFactory#getAdditionalSettings(String)}.</li>
 * <li>It requires one to specify a ‘factory id uniquifier’ to guarantee that the derived factory id is globally unique
 * and stable, even if the node name changes.</li>
 * </ul>
 *
 * @author Jonas Klotz
 * @since 5.3
 */
public interface ParameterizedNodeFactory {

    /**
     * Returns a string that globally uniquifies the factory id because the factory-class-name is not sufficient to
     * identify a single node since it's being used for multiple nodes.
     *
     * The returned id-uniquifier must only be unique within the {@link NodeSetFactory} this node is part of. But it
     * must always remain the same for a particular node as soon as the node has been released to users.
     *
     * @return the factory-id-uniquifier or {@code null} in which case the node-name is being used (which is not optimal
     *         since it is not guaranteed to remain the same)
     * @since 5.3
     */
    String getFactoryIdUniquifier();

}
