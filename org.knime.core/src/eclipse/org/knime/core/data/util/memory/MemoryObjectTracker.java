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

import java.lang.ref.WeakReference;
import java.util.LinkedList;

import org.knime.core.node.NodeLogger;

/**
 * API not public yet
 *
 * @author dietzc
 */
public class MemoryObjectTracker {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MemoryObjectTracker.class);

    /*
    * The list of tracked objects, whose memory will be freed, if the
    * memory runs out.
    */
    private static final LinkedList<WeakReference<MemoryReleasable>> TRACKED_OBJECTS =
            new LinkedList<WeakReference<MemoryReleasable>>();

    /*
    * Memory Warning System
    */
    private static final MemoryWarningSystem MEMORY_WARNING_SYSTEM = new MemoryWarningSystem();

    static {
        MemoryWarningSystem.setPercentageUsageThreshold(0.7);

        MEMORY_WARNING_SYSTEM.addListener(new MemoryWarningSystem.Listener() {

            public void memoryUsageLow(final long usedMemory, final long maxMemory) {

                synchronized (TRACKED_OBJECTS) {

                    LOGGER.debug("low memory. used mem: " + usedMemory + ";max mem: " + maxMemory + ".");
                    freeAllMemory();

                }

            }
        });
    }

    private MemoryObjectTracker() {
        // utility class

    }

    /**
     * Track memory releasable objects. If the memory gets low, on all tracked objects the
     * {@link MemoryReleasable#freeMemory()} method will be called and the objects itself will be removed from the
     * cache.
     *
     * @param obj
     */
    public static void trackMemoryReleasableObject(final MemoryReleasable obj) {
        synchronized (TRACKED_OBJECTS) {
            WeakReference<MemoryReleasable> ref = new WeakReference<MemoryReleasable>(obj);
            TRACKED_OBJECTS.add(ref);
            LOGGER.debug(TRACKED_OBJECTS.size() + " objects tracked");
        }
    }

    /**
     * Heuristic to make sure that memory is available for a given object
     * TODO: This is not working yet.
     *
     * @param allocator
     */
    public static <T> T safeInstantiation(final MemoryAllocator<T> allocator) {
        synchronized (TRACKED_OBJECTS) {
            freeAllMemory();
            return allocator.allocate();
        }
    }

    /*
    * Frees the memory of all objects in the list.
    *
    * TODO: Are there race conditions if freeMemory is called and at the
    * same time some performs a null check on getData in a cell?!
    *
    * TODO: Is there a better way than just cleaning everything? LRU?
    */
    private synchronized static void freeAllMemory() {
        synchronized (TRACKED_OBJECTS) {

            WeakReference<MemoryReleasable> ref;
            int countT = 0;

            while (TRACKED_OBJECTS.size() > 1) {
                ref = TRACKED_OBJECTS.removeFirst();
                MemoryReleasable memoryReleasable = ref.get();
                if (memoryReleasable != null) {
                    if (memoryReleasable.freeMemory() > 0) {
                        countT++;
                    }
                }
            }

            LOGGER.debug(countT + " tracked objects have been released.");
        }
    }
}
