/* Created on Apr 12, 2006 9:17:20 AM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Apr 12, 2006 (thor): created
 */
package de.unikn.knime.core.util;

import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a simple pools for object that can be reused. The pool can be
 * divided into several sub pool, e.g. if the object have different "sizes". If
 * memory becomes scarce some of the sub pools may get garbage collected, but
 * this is transparent.
 * 
 * @param <T> any class whose object are stored in the pool
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ObjectPool<T> {
    private final SoftReference<List<T>>[] m_pools;
    private final int[] m_maxSizes;
    private final int[] m_misses, m_hits; 
    
    /**
     * Creates a new object pool.
     * 
     * @param subPools the number of sub pools
     * @param maxSizes the maximum size of each sub pool     
    */
    @SuppressWarnings("unchecked")
    public ObjectPool(final int subPools, final int maxSizes) {
        m_pools = (SoftReference<List<T>>[]) new SoftReference[subPools];
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
     * @return ann object or <code>null</code>
     */
    public T get(final int subPool) {
        SoftReference<List<T>> sr = m_pools[subPool];
        if ((sr.get() != null) && (sr.get().size() > 0)) {
            m_hits[subPool]++;
            return sr.get().remove(sr.get().size() - 1);
        }
        m_misses[subPool]++;
        return null;
    }
    
    
    /**
     * Puts an object into the pool.
     * 
     * @param object an object
     * @param subPool the number of the sub pool starting with 0
     */
    public void recycle(final T object, final int subPool) {
         SoftReference<List<T>> sr = m_pools[subPool];
         if (sr.get() == null) {
             sr = new SoftReference<List<T>>(new ArrayList<T>());
             m_pools[subPool] = sr;
         }

         if (sr.get().size() < m_maxSizes[subPool]) {
            sr.get().add(object);
        }
    }
    
    
    /**
     * Prints statistics about hits and misses.
     * @param out a print stream to which the statistic should be written
     */
    public void printStats(final PrintStream out) {
        out.println("Objectpool statistics:");
        for (int i = 0; i < m_pools.length; i++) {
            if (m_hits[i] + m_misses[i] > 0) {
                out.printf("Pool %1$3d => %2$8d accesses, %3$8d hits => %4$2.2f%%\n",
                        i, m_hits[i] + m_misses[i], m_hits[i],
                        100.0 * m_hits[i] / (m_hits[i] + m_misses[i]));
            }
        }
    }
}
