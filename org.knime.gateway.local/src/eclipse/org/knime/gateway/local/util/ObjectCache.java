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
 *   Oct 19, 2016 (hornm): created
 */
package org.knime.gateway.local.util;

import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Object instance cache to be referenced with a dedicated key.
 *
 * Its primary purpose is to workaround "1:1" wrappers. Within the eclipse UI very often instances are checked for object equality.
 * Consequently, there must be exactly one wrapper class instance for a certain object to be wrapped. Two wrapper
 * instances wrapping the same object should be avoided (this happens if, e.g. a getter method is called twice and each
 * time a new wrapper instance is created around the same object returned).
 *
 * This global map caches object instances (weak references) for look up with a dedicated key.
 *
 * @author Martin Horn, University of Konstanz
 */
public class ObjectCache {

    private final WeakHashMap<Object, Object> m_objectMap;

    public ObjectCache() {
        m_objectMap = new WeakHashMap<Object, Object>();
    }

    /**
     * Creates, casts or returns a cached object
     *
     * @param key the key to look for in the internal map (and to store newly created objects with)
     * @param fct tells how to create the object if not present in the internal map
     * @param targetClass if the key is assignable to this class, the (casted) key-object will be returned
     * @return<ul>
     * <li><code>null</code> if the key is <code>null</code>
     * <li>a newly created instance of the object to be created,</li>
     * <li>the key itself if it can be cast to the given target class</li>
     * <li>or the object stored in the internal map for the given key</li>
     * </ul>
     */
    public <K, V> V getOrCreate(final K key, final Function<K, V> fct, final Class<V> targetClass) {
        if (key == null) {
            return null;
        } else if (targetClass.isAssignableFrom(key.getClass())) {
            //if the key is already a wrapper
            return targetClass.cast(key);
        } else {
            V obj = (V)m_objectMap.get(key);
            if (obj == null) {
                //create wrapper entry
                obj = fct.apply(key);
                m_objectMap.put(key, obj);
            }
            return obj;
        }
    }

    /**
     * Creates or returns a cached object.
     *
     * @param key key the key to look for in the internal map (and to store newly created objects with)
     * @param fct tells how to create the object if not present in the internal map
     * @return <ul>
     * <li><code>null</code> if the key is <code>null</code>
     * <li>a newly created instance of the object to be created,</li>
     * <li>or the object stored in the internal map for the given key</li>
     * </ul>
     */
    public <K, V> V getOrCreate(final K key, final Function<K, V> fct) {
        if (key == null) {
            return null;
        } else {
            V obj = (V)m_objectMap.get(key);
            if (obj == null) {
                //create wrapper entry
                obj = fct.apply(key);
                m_objectMap.put(key, obj);
            }
            return obj;
        }
    }

}
