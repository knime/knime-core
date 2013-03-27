/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 12, 2006 (meinl): created
 *   11.05.2006 (wiswedel, ohl) reviewed
 */
package org.knime.core.util;

import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a simple pool for reusable objects. The pool can be divided into
 * several sub pools, e.g. if the objects have different "sizes". If memory
 * becomes scarce some of the sub pools may get garbage collected, but this is
 * transparent.
 *
 * @param <T> any class whose objects are stored in the pool
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ObjectPool<T> {
    private final SoftReference<List<T>>[] m_pools;

    private final int[] m_maxSizes;

    // used only for statistics.
    private final int[] m_misses, m_hits;

    /**
     * Creates a new object pool.
     *
     * @param subPools the number of sub pools
     * @param maxSizes the maximum size of each sub pool
     */
    @SuppressWarnings("unchecked")
    // no generics in array definition
    public ObjectPool(final int subPools, final int maxSizes) {
        m_pools = new SoftReference[subPools];
        m_maxSizes = new int[subPools];

        for (int i = 0; i < subPools; i++) {
            m_pools[i] = new SoftReference<List<T>>(new ArrayList<T>());
            m_maxSizes[i] = maxSizes;
        }

        m_misses = new int[subPools];
        m_hits = new int[subPools];
    }

    /**
     * Returns an object from the (sub) pool, or <code>null</code> if the pool
     * is empty.
     *
     * @param subPool the number of the sub pool, starting with 0
     * @return an object or <code>null</code>
     */
    public T get(final int subPool) {
        SoftReference<List<T>> sr = m_pools[subPool];
        List<T> referent = sr.get();
        if ((referent != null) && (!referent.isEmpty())) {
            m_hits[subPool]++;
            return referent.remove(referent.size() - 1);
        }
        m_misses[subPool]++;
        return null;
    }

    /**
     * Puts an object into the specified subpool. If the pool is already full,
     * the object will not be added, in this case the method does nothing.
     *
     * @param object an object to store
     * @param subPool the index of the sub pool (starting with 0)
     */
    public void recycle(final T object, final int subPool) {
        SoftReference<List<T>> sr = m_pools[subPool];
        List<T> referent = sr.get();
        if (referent == null) {
            referent = new ArrayList<T>();
            sr = new SoftReference<List<T>>(referent);
            m_pools[subPool] = sr;
        }

        if (referent.size() < m_maxSizes[subPool]) {
            referent.add(object);
        }
    }

    /**
     * Prints statistics about hits and misses.
     *
     * @param out a print stream to which the statistic should be written
     */
    public void printStats(final PrintStream out) {
        out.println("Objectpool statistics:");
        for (int i = 0; i < m_pools.length; i++) {
            if (m_hits[i] + m_misses[i] > 0) {
                out.printf("Pool %1$3d => %2$8d accesses, "
                        + "%3$8d hits => %4$2.2f%%%n", i, m_hits[i]
                        + m_misses[i], m_hits[i], 100.0 * m_hits[i]
                        / (m_hits[i] + m_misses[i]));
            }
        }
    }
}

