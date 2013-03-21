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
 * ---------------------------------------------------------------------
 *
 * Created on 19.03.2013 by Mia
 */
package org.knime.core.data.util.memory;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.knime.core.node.NodeLogger;

/**
 * API not public yet
 *
 * @author dietzc
 */
public final class MemoryObjectTracker {

    enum Strategy {
        /* Completely frees memory */
        FREE_ALL,
        /* Free a certain percentage of all objects in the memory.*/
        FREE_PERCENTAGE,
        /* Remove oldest object (possible) */
        FREE_ONE;
    }

    /* Release Strategy */
    private Strategy m_strategy = Strategy.FREE_ALL;

    private final NodeLogger LOGGER = NodeLogger.getLogger(MemoryObjectTracker.class);

    /*
    * The list of tracked objects, whose memory will be freed, if the
    * memory runs out.
    */
    private final WeakHashMap<MemoryReleasable, Long> TRACKED_OBJECTS =
            new WeakHashMap<MemoryReleasable, Long>();

    private long m_lastAccess;

    // Singleton instance of this object
    private static MemoryObjectTracker m_instance;

    /*
    * Memory Warning System
    */
    private final MemoryWarningSystem MEMORY_WARNING_SYSTEM = MemoryWarningSystem.getInstance();

    /*
     * Private constructor, singleton
     */
    private MemoryObjectTracker() {

        MEMORY_WARNING_SYSTEM.setPercentageUsageThreshold(0.7);

        MEMORY_WARNING_SYSTEM.registerListener(new MemoryWarningSystem.MemoryWarningListener() {

            @Override
            public void memoryUsageLow(final long usedMemory, final long maxMemory) {

                synchronized (TRACKED_OBJECTS) {
                    LOGGER.debug("low memory . used mem: " + usedMemory + ";max mem: " + maxMemory + ".");
                    switch (m_strategy) {
                        case FREE_ONE:
                            freeAllMemory(0);
                            break;
                        case FREE_ALL:
                            freeAllMemory(100);
                            break;
                        case FREE_PERCENTAGE:
                            freeAllMemory(0.5);
                            break;
                        default:
                            LOGGER.warn("Unknown MemoryObjectTracker.Strategy, using default");
                            freeAllMemory(100);
                            break;
                    }
                }
            }
        });

    }

    /**
     * Track memory releasable objects. If the memory gets low, on all tracked objects the
     * {@link MemoryReleasable#freeMemory()} method will be called and the objects itself will be removed from the
     * cache.
     *
     * @param obj
     */
    public void addMemoryReleaseable(final MemoryReleasable obj) {
        synchronized (TRACKED_OBJECTS) {
            TRACKED_OBJECTS.put(obj, m_lastAccess++);
            LOGGER.debug(TRACKED_OBJECTS.size() + " objects tracked, Latest Obj: " + obj);
        }
    }

    public void removeMemoryReleaseable(final MemoryReleasable obj) {
        synchronized (TRACKED_OBJECTS) {
            TRACKED_OBJECTS.remove(obj);
        }
    }

    /**
     * Promotes obj in LRU Cache. If obj was added to removeList before, it will be removed from removeList
     *
     * @param obj
     */
    public void promoteMemoryReleaseable(final MemoryReleasable obj) {
        synchronized (TRACKED_OBJECTS) {
            TRACKED_OBJECTS.put(obj, m_lastAccess++);
        }
    }

    /*
    * Frees the memory of some objects in the list.
    */
    private void freeAllMemory(final double percentage) {
        synchronized (TRACKED_OBJECTS) {

            double initSize = TRACKED_OBJECTS.size();
            int count = 0;
            List<Map.Entry<MemoryReleasable, Long>> entryValues =
                    new LinkedList<Map.Entry<MemoryReleasable, Long>>(TRACKED_OBJECTS.entrySet());
            Collections.sort(entryValues, new Comparator<Map.Entry<MemoryReleasable, Long>>() {
                @Override
                public int compare(final Entry<MemoryReleasable, Long> o1, final Entry<MemoryReleasable, Long> o2) {
                    if (o1.getValue() < o2.getValue()) {
                        return -1;
                    } else if (o1.getValue() > o2.getValue()) {
                        return +1;
                    } else {
                        assert false : "Equal update time stamp";
                        return 0;
                    }
                }
            });

            for (Iterator<Map.Entry<MemoryReleasable, Long>> it = entryValues.iterator(); it.hasNext();) {
                Map.Entry<MemoryReleasable, Long> entry = it.next();
                MemoryReleasable memoryReleasable = entry.getKey();
                if (memoryReleasable != null) {
                    // Since now the memory alert object is null. may change in the future
                    if (memoryReleasable.memoryAlert(null)) {
                        entryValues.remove(entry);
                        count++;
                    }
                }
                if (count / initSize >= percentage) {
                    break;
                }
            }
            TRACKED_OBJECTS.entrySet().retainAll(entryValues);

            LOGGER.debug(count + " tracked objects have been released.");
        }
    }

    /**
     * Singleton on MemoryObjectTracker
     */
    public static MemoryObjectTracker getInstance() {
        if (m_instance == null) {
            m_instance = new MemoryObjectTracker();
        }
        return m_instance;
    }
}
