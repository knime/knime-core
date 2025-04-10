/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jul 15, 2012 (wiswedel): created
 */
package org.knime.core.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Unsynchronized last recently used cache.
 *
 * @see LinkedHashMap#LinkedHashMap(int, float, boolean)
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.6
 * @param <K> key type
 * @param <V> value type
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int m_maxHistory;

    private transient BiConsumer<K, V> m_onRemoveListener;

    /**
     * @param initialCapacity the initial capacity of the cache
     * @param maxHistory the maximum size of the cache
     * @since 4.0
     */
    public LRUCache(final int initialCapacity, final int maxHistory) {
        super(initialCapacity, 0.75f, true);
        if (maxHistory < 1) {
            throw new IllegalArgumentException("max history must be larger 0: " + maxHistory);
        }
        m_maxHistory = maxHistory;
    }

    /**
     * -
     * 
     * @param maxHistory maximum size of the cache before older elements are evicted on insertion of newer elements
     * @param onRemoveListener a listener to be invoked when an entry is removed from the cache. This includes automatic
     *            eviction of the least recently used entry.
     * @since 5.5
     */
    public LRUCache(final int maxHistory, final BiConsumer<K, V> onRemoveListener) {
        this(16, maxHistory);
        m_onRemoveListener = onRemoveListener;
    }

    /**
     * @param maxHistory cache size
     */
    public LRUCache(final int maxHistory) {
        this(16, maxHistory);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean removeEldestEntry(final Map.Entry<K, V> e) {
        var shouldRemove = size() > m_maxHistory;
        if (shouldRemove && m_onRemoveListener != null) {
            m_onRemoveListener.accept(e.getKey(), e.getValue());
        }
        return shouldRemove;
    }

}
