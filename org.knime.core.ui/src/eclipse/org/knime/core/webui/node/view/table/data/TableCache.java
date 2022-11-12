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
 *   Nov 13, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data;

import java.util.Arrays;
import java.util.function.Supplier;

import org.knime.core.data.DataTable;
import org.knime.core.data.container.ContainerTable;

/**
 * TODO
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class TableCache {

    private Object[] m_previousKeyValues;

    private ContainerTable m_cachedTable;

    private boolean m_wasUpdated = false;

    /**
     * @param tableSupplier supplies the new table to be cached in case the cached table needs to be updated (or no
     *            table has been cached so far)
     * @param shallClearCache if {@code true} the cached table will be removed and this table cache won't reference any
     *            table anymore
     * @param keyValues the cache's key values; i.e. if those change, the cached table will be updated/replaced;
     *            otherwise the originally cached table will be kept
     */
    void conditionallyUpdateCachedTable(final Supplier<ContainerTable> tableSupplier, final boolean shallClearCache,
        final Object... keyValues) {
        if (shallClearCache) {
            m_wasUpdated = m_cachedTable != null;
            clear();
            return;
        }
        if (m_cachedTable == null || !Arrays.deepEquals(m_previousKeyValues, keyValues)) {
            // update the cache
            m_previousKeyValues = keyValues;
            m_cachedTable = tableSupplier.get();
            m_wasUpdated = true;
        } else {
            m_wasUpdated = false;
        }
    }

    /**
     * @return whether the cache has been updated either because a new table was cached or the cache has been cleared
     */
    boolean wasUpdated() {
        return m_wasUpdated;
    }

    /**
     * @param elseTable returned in case there is no table cached
     * @return the cached table if there is any, otherwise the 'elseTable'
     */
    DataTable getCachedTableOrElse(final Supplier<DataTable> elseTable) {
        return m_cachedTable == null ? elseTable.get() : m_cachedTable;
    }

    void clear() {
        if (m_cachedTable != null) {
            m_cachedTable.close();
            m_cachedTable = null;
            m_previousKeyValues = null;
        }
    }

}