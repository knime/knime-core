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
 *   Feb 6, 2024 (lw): created
 */
package org.knime.core.eclipseUtil;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.IEarlyStartup;

/**
 * Utilizes the reflection API to once - during startup - replace the {@link HttpURLConnection}'s
 * authentication cache with a no-op cache (see {@link NoOpHashMap} in this file).
 * <p>
 * All auth-cache-requests are therefore no-ops and do not spill sensitive credentials data between
 * executions or nodes or workflows or users.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public class URLConnectionCacheDisabler implements IEarlyStartup {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(URLConnectionCacheDisabler.class);

    private static final String AUTH_VALUE_CLASS_NAME = "sun.net.www.protocol.http.AuthCacheValue";

    private static final String CACHE_IMPL_CLASS_NAME = "sun.net.www.protocol.http.AuthCacheImpl";

    @Override
    public void run() {
        try {
            // retrieving auth value holding the cache
            final Class<?> cacheValueClass = Class.forName(AUTH_VALUE_CLASS_NAME);
            final var cacheImplField = cacheValueClass.getDeclaredField("cache");
            cacheImplField.setAccessible(true); // NOSONAR

            // retrieving actual cache
            final Class<?> cacheImplClass = Class.forName(CACHE_IMPL_CLASS_NAME);
            final var hashMapField = cacheImplClass.getDeclaredField("hashtable");
            hashMapField.setAccessible(true); // NOSONAR

            /*
             * Retrieves the static cache field from the sun.net implementation, then extracts
             * its internal cache (being a HashMap), and replaces it by our no-op HashMap.
             */
            hashMapField.set(cacheImplField.get(null), new NoOpHashMap<String, LinkedList<?>>()); // NOSONAR
        } catch (ClassNotFoundException
                | NoSuchFieldException
                | SecurityException
                | IllegalArgumentException
                | IllegalAccessException e) {
            LOGGER.debug(String.format("Could not disable the authentication cache of the %s",
                HttpURLConnection.class.getName()), e);
        }
    }

    /**
     * A {@link HashMap} that does nothing.
     * Overrides every method of the {@link Map} interface to a no-operation.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    private static class NoOpHashMap<K, V> extends HashMap<K, V> {

        private static final long serialVersionUID = -1L;

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(final Object key) {
            return false;
        }

        @Override
        public boolean containsValue(final Object value) {
            return false;
        }

        @Override
        public V get(final Object key) {
            return null;
        }

        @Override
        public V put(final K key, final V value) {
            return null;
        }

        @Override
        public V remove(final Object key) {
            return null;
        }

        @Override
        public void putAll(final Map<? extends K, ? extends V> m) {
            // does nothing

        }

        @Override
        public void clear() {
            // does nothing
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }
}
