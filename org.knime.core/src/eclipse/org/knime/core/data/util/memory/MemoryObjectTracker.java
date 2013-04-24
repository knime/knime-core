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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.apache.commons.io.FileUtils;
import org.knime.core.node.NodeLogger;

/**
 * API not public yet.
 *
 * @author dietzc
 */
public final class MemoryObjectTracker {

    private static final MemoryAlertObject MEMORY_ALERT = new MemoryAlertObject();

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

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MemoryObjectTracker.class);

    /*
    * The list of tracked objects, whose memory will be freed, if the
    * memory runs out.
    */
    private final WeakHashMap<MemoryReleasable, Long> TRACKED_OBJECTS = new WeakHashMap<MemoryReleasable, Long>();

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
                LOGGER.debug("Low memory encountered. Used memory: " + FileUtils.byteCountToDisplaySize(usedMemory)
                             + "; maximum memory: " + FileUtils.byteCountToDisplaySize(maxMemory) + ".");
                final double percentageToFree;
                switch (m_strategy) {
                    case FREE_ONE:
                        percentageToFree = 0.0;
                        break;
                    case FREE_ALL:
                        percentageToFree = 1.0;
                        break;
                    case FREE_PERCENTAGE:
                        percentageToFree = 0.5;
                        break;
                    default:
                        percentageToFree = 1.0;
                        LOGGER.warn("Unknown MemoryObjectTracker.Strategy, using default");
                        break;
                }
                // run in separate thread so that it can be debugged and we don't mess around with system threads
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        freeAllMemory(percentageToFree);
                    }
                }, "KNIME-Memory-Cleaner").start();
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
        if (obj != null) {
            synchronized (TRACKED_OBJECTS) {
                TRACKED_OBJECTS.put(obj, m_lastAccess++);
                LOGGER.debug("Adding " + obj.getClass().getName() + " (" + TRACKED_OBJECTS.size() + " in total)");
            }
        }
    }

    public void removeMemoryReleaseable(final MemoryReleasable obj) {
        if (obj != null) {
            synchronized (TRACKED_OBJECTS) {
                String name = obj.getClass().getName();
                if (TRACKED_OBJECTS.remove(obj) == null) {
                    LOGGER.debug("Attempted to remove " + name + ", which was not tracked");
                } else {
                    LOGGER.debug("Removing " + name + " (" + TRACKED_OBJECTS.size() + " remaining)");
                }
            }
        }
    }

    /**
     * Promotes obj in LRU Cache. If obj was added to removeList before, it will be removed from removeList
     *
     * @param obj
     */
    public void promoteMemoryReleaseable(final MemoryReleasable obj) {
        if (obj != null) {
            synchronized (TRACKED_OBJECTS) {
                TRACKED_OBJECTS.put(obj, m_lastAccess++);
            }
        }
    }

    /*
    * Frees the memory of some objects in the list.
    */
    private void freeAllMemory(final double percentage) {
        int initSize = TRACKED_OBJECTS.size();

        List<Map.Entry<MemoryReleasable, Long>> entryValues =
                new ArrayList<Map.Entry<MemoryReleasable, Long>>(initSize);

        int count = 0;
        synchronized (TRACKED_OBJECTS) {
            entryValues.addAll(TRACKED_OBJECTS.entrySet());
        }

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

        LOGGER.debug("Trying to release " + entryValues.size() + " memory objects");
        for (Iterator<Map.Entry<MemoryReleasable, Long>> it = entryValues.iterator(); it.hasNext();) {
            Map.Entry<MemoryReleasable, Long> entry = it.next();
            MemoryReleasable memoryReleasable = entry.getKey();
            if (memoryReleasable != null) {
                boolean isToRemove;
                try {
                    isToRemove = memoryReleasable.memoryAlert(MEMORY_ALERT);
                } catch (Exception e) {
                    LOGGER.error("Exception while alerting low memory condition", e);
                    isToRemove = true;
                }
                if (isToRemove) {
                    synchronized (TRACKED_OBJECTS) {
                        TRACKED_OBJECTS.remove(entry);
                    }
                    count++;
                }
            }

            if (count / (double)initSize >= percentage) {
                break;
            }
        }
        int remaining;
        synchronized (TRACKED_OBJECTS) {
            remaining = TRACKED_OBJECTS.size();
        }
        LOGGER.debug(count + "/" + initSize + " tracked objects have been released (" + remaining + " remaining)");
    }

    /**
     * @return singleton on MemoryObjectTracker.
     */
    public static MemoryObjectTracker getInstance() {
        if (m_instance == null) {
            m_instance = new MemoryObjectTracker();
        }
        return m_instance;
    }
}
