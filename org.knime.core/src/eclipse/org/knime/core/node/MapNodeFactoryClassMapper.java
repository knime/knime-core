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
 *   Oct 29, 2019 (awalter): created
 */
package org.knime.core.node;

import java.util.Collections;
import java.util.Map;

/**
 * Base class for {@link NodeFactoryClassMapper}s which map via concrete key-value pairs.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public abstract class MapNodeFactoryClassMapper extends NodeFactoryClassMapper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MapNodeFactoryClassMapper.class);

    private final Map<String, Class<? extends NodeFactory<? extends NodeModel>>> m_map;

    /**
     * Creates a {@link NodeFactoryClassMapper} which maps classes with the given key-value pairs.
     *
     * @param map a {@code Map} which maps node factory class names to the appropriate corresponding {@link NodeFactory}
     *            class. The contained {@link NodeFactory} class objects are expected to have a public no args
     *            constructor such that {@link Class#newInstance()} succeeds.
     */
    protected MapNodeFactoryClassMapper(final Map<String, Class<? extends NodeFactory<? extends NodeModel>>> map) {
        m_map = map == null ? Collections.emptyMap() : map;
    }

    /**
     * Returns the {@code Map} which backs this {@link NodeFactoryClassMapper}.
     * <p>
     * The returned {@code Map} should contain keys corresponding to the "old" node factory class names and values which
     * are the corresponding updated factory class objects.
     * </p>
     *
     * @return the {@code Map} for this mapper
     */
    public Map<String, Class<? extends NodeFactory<? extends NodeModel>>> getMap() {
        return Collections.unmodifiableMap(m_map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeFactory<? extends NodeModel> mapFactoryClassName(final String factoryClassName) {
        for (final String key : m_map.keySet()) {
            if (key.equals(factoryClassName)) {
                final Class<? extends NodeFactory<? extends NodeModel>> nodeFactoryClass = m_map.get(key);
                try {
                    return nodeFactoryClass.newInstance();
                } catch (final InstantiationException | IllegalAccessException ex) {
                    LOGGER.debug(
                        "Could not instantiate " + nodeFactoryClass.getName() + " in place of " + factoryClassName);
                }
            }
        }
        return null;
    }

}
