/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   18.02.2010 (hofer): created
 */
package org.knime.base.node.preproc.joiner;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.ExecutionContext;

/**
 * Used to query if memory on the heap is low. Observed are the memory pools
 * of long living objects which are "Tenured Gen" for the client vm and
 * "PS Old Gen" for the server vm.
 *
 * @author Heiko Hofer
 */
final class MemoryService {
    /** The threshold for the low memory condition. */
    private long m_threshold;

    /** The observed {@link MemoryPoolMXBean}. */
    private MemoryPoolMXBean m_memPool;

    /** {@link GarbageCollectorMXBean}s associated with the observed
     * {@link MemoryPoolMXBean}. */
    private ArrayList<GarbageCollectorMXBean> m_gcBean;

    // Flag controlling the output of isMemoryLow
    private boolean m_waitForNextGC;
    // Flag controlling the output of isMemoryLow
    private long m_lastCount;

    private boolean m_useCollectionUsage;

    /**
     * Creates a new instance. This instance will report a low memory condition
     * when the quotient of used memory to the total amount of memory is above
     * this threshold.
     *
     * @param usedMemoryThreshold A number between 0 and 1.
     */
    public MemoryService(final double usedMemoryThreshold) {
        this(usedMemoryThreshold, 0, true);

    }

    /**
     * @param usedMemoryThreshold
     * @param minAvailableMemory
     */
    public MemoryService(final double usedMemoryThreshold,
            final long minAvailableMemory,
            final boolean useCollectionUsage) {
        // Determine the memory pool to be observed
        List<String> toObserve = Arrays.asList("Tenured Gen", "PS Old Gen");
        for (MemoryPoolMXBean memoryPool
                : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.isUsageThresholdSupported()) {
                if (toObserve.contains(memoryPool.getName())) {
                    m_memPool = memoryPool;
                }
            }
        }

        // Get the GarbageCollectorMXBeans associated with the observed
        // MemoryPoolMXBean
        m_gcBean = new ArrayList<GarbageCollectorMXBean>();
        for (GarbageCollectorMXBean gcBean
                : ManagementFactory.getGarbageCollectorMXBeans()) {
            for (String poolName : Arrays.asList(gcBean.getMemoryPoolNames())) {
                if (toObserve.contains(poolName)) {
                    m_gcBean.add(gcBean);
                }
            }
        }

        // Compute the threshold in bytes
        long maxMem = m_memPool.getUsage().getMax();
        long usedMem = m_memPool.getUsage().getUsed();

        m_threshold = (long)(usedMemoryThreshold * maxMem);

        // if currently available memory is lower than the minimum
        if (m_threshold - usedMem < minAvailableMemory) {
            // try to free memory
            Runtime.getRuntime().gc();
            Runtime.getRuntime().gc();
            usedMem = m_memPool.getUsage().getUsed();
            m_threshold = Math.min(usedMem + minAvailableMemory, maxMem);
        }

        m_waitForNextGC = false;
        m_useCollectionUsage = useCollectionUsage;
    }

    /**
     * This is a stateful method return true in the low memory condition. Note,
     * that this method only returns true at most once between two garbage
     * collections. All listeners are notified by this method if it returns
     * true.
     *
     * @param exec The {@link ExecutionContext}
     * @return True if memory is low.
     */
    public boolean isMemoryLow(final ExecutionContext exec) {
        if (m_waitForNextGC && m_useCollectionUsage) {
            long thisCount = m_gcBean.iterator().next().getCollectionCount();
            if (thisCount == m_lastCount) {
                return false;
            } else {
                m_waitForNextGC = false;
            }
        }

        MemoryUsage usage = null;
        if (m_useCollectionUsage) {
            usage = m_memPool.getCollectionUsage();
        }
        if (null == usage) {
            usage = m_memPool.getUsage();
        }
        if (null != usage) {
            boolean memoryIsLow = usage.getUsed() > m_threshold;
            if (memoryIsLow) {
                m_waitForNextGC = true;
                m_lastCount = m_gcBean.iterator().next().getCollectionCount();
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalStateException(
                    "The memory pool is not valid.");
        }
    }

}
