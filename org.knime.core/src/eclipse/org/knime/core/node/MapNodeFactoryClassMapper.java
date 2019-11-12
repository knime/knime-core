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
 * <p>
 * This class exposes the node mappings, such that the mappings can be replicated in other platforms.
 * </p>
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public abstract class MapNodeFactoryClassMapper extends NodeFactoryClassMapper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MapNodeFactoryClassMapper.class);

    private Map<String, Class<? extends NodeFactory<? extends NodeModel>>> m_map;

    /**
     * Returns the read only {@code Map} which backs this {@link NodeFactoryClassMapper}.
     * <p>
     * The returned {@code Map} should contain keys corresponding to the "old" node factory class names and values which
     * are the corresponding updated factory class objects.
     * </p>
     *
     * @return the read only {@code Map} for this mapper
     */
    public final Map<String, Class<? extends NodeFactory<? extends NodeModel>>> getMap() {
        if (m_map == null) {
            setMap();
        }
        return m_map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeFactory<? extends NodeModel> mapFactoryClassName(final String factoryClassName) {
        for (final String key : getMap().keySet()) {
            if (key.equals(factoryClassName)) {
                final Class<? extends NodeFactory<? extends NodeModel>> nodeFactoryClass = getMap().get(key);
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

    /**
     * Returns the {@code Map} which backs this {@link NodeFactoryClassMapper}.
     * <p>
     * The returned {@code Map} should contain keys corresponding to the "old" node factory class names and values which
     * are the corresponding updated factory class objects.
     * </p>
     *
     * <p>
     * This method is for internal use only, the {@link #getMap()} method calls this method and ensures the returned
     * {@code Map} is read only and non-null.
     * </p>
     *
     * @return {@code Map} for this mapper
     */
    protected abstract Map<String, Class<? extends NodeFactory<? extends NodeModel>>> getMapInternal();

    // -- helper methods --

    /**
     * This method calls {@link #getMapInternal()}, and caches the result.
     */
    private synchronized void setMap() {
        if (m_map != null) {
            return;
        }
        final Map<String, Class<? extends NodeFactory<? extends NodeModel>>> returnedMap = getMapInternal();
        m_map = returnedMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(returnedMap);
    }
}
