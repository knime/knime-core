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
package org.knime.core.node.workflow.wrapped;

import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Workaround for "1:1" wrappers. Within the eclipse UI very often instances are checked for object equality.
 * Consequently, there must be exactly one wrapper class instance for a certain object to be wrapped. Two wrapper
 * instances wrapping the same object should be avoided (this happens if, e.g. a getter method is called twice and each
 * time a new wrapper instance is created around the same object returned).
 *
 * This global map keeps all instances and their wrappers (weak references) for look up.
 *
 * TODO rename since its functionally is probably not only limited to wrapper classes
 *
 * @author Martin Horn, University of Konstanz
 */
public class WrapperMapUtil {

    private WrapperMapUtil() {
        // utility class
    }

    private static final WeakHashMap<Object, Object> MAP = new WeakHashMap<Object, Object>();

    /**
     *
     * @param key
     * @param fct
     * @param wrapperClass
     * @return TODO, <code>null</code> if the key is <code>null</code>
     */
    public static <K, V> V getOrCreate(final K key, final Function<K, V> fct, final Class<V> wrapperClass) {
        if (key == null) {
            return null;
        } else if (wrapperClass.isAssignableFrom(key.getClass())) {
            //if the key is already a wrapper
            return wrapperClass.cast(key);
        } else {
            V obj = (V)MAP.get(key);
            if (obj == null) {
                //create wrapper entry
                obj = fct.apply(key);
                MAP.put(key, obj);
            }
            return obj;
        }
    }

}
