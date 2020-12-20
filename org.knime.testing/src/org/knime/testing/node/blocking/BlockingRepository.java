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
 */
package org.knime.testing.node.blocking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.Pair;
import org.knime.core.node.util.CheckUtils;

/**
 * A static repository of {@link Lock} objects, identified by (String) id.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BlockingRepository {

    /** Configures which method is locked (in NodeModel). */
    public enum LockedMethod {
            /** Locked during #execute */
            EXECUTE,
            /** Locked during #configure */
            CONFIGURE,
            /** Locked during #saveInternals */
            SAVE_INTERNALS
    }

    private static final Map<Pair<String, LockedMethod>, ReentrantLock> LOCK_REPOSITORY = new HashMap<>();

    private BlockingRepository() {
    }

    /**
     * Add a lock to the repository.
     *
     * @param id The id of the lock
     * @param lockedMethod ...
     * @param lock The lock
     * @throws NullPointerException if either arg is null
     * @throws IllegalArgumentException If id is already in use
     */
    public static synchronized void put(final String id, final LockedMethod lockedMethod, final ReentrantLock lock) {
        CheckUtils.checkArgument(id != null && lock != null, "id and lock must not be null");
        Pair<String, LockedMethod> key = Pair.of(id, lockedMethod);
        CheckUtils.checkArgument(!LOCK_REPOSITORY.containsKey(key), "Lock ID already in use: %s", id);
        LOCK_REPOSITORY.put(key, lock);
    }

    /**
     * Get the lock associated with the id, expecting a non-null return value (otherwise throwing exception).
     *
     * @param id The id of interest
     * @param lockedMethod ...
     * @return The lock
     * @throws IllegalStateException If there is no lock assigned for this id and method.
     */
    public static synchronized ReentrantLock getNonNull(final String id, final LockedMethod lockedMethod) {
        return get(id, lockedMethod).orElseThrow(() -> new IllegalStateException(
            String.format("No lock set for id '%s' and method '%s'", id, lockedMethod)));
    }

    /**
     * Get the lock associated with the id, or an empty optional if not set.
     *
     * @param id The id of interest
     * @param lockedMethod ...
     * @return The lock
     */
    public static synchronized Optional<ReentrantLock> get(final String id, final LockedMethod lockedMethod) {
        return Optional.ofNullable(LOCK_REPOSITORY.get(Pair.of(id, lockedMethod)));
    }

    /**
     * Remove and get the lock associated with the id.
     *
     * @param id The id of interest.
     * @param lockedMethod ...
     * @return The lock previously assigned to the id or null if not present.
     */
    public static synchronized ReentrantLock remove(final String id, final LockedMethod lockedMethod) {
        return LOCK_REPOSITORY.remove(Pair.of(id, lockedMethod));
    }

    /**
     * Remove all lock associated with the id.
     *
     * @param id The id of interest.
     */
    public static synchronized void removeAll(final String id) {
        for (Iterator<Map.Entry<Pair<String, LockedMethod>, ReentrantLock>> it =
            LOCK_REPOSITORY.entrySet().iterator(); it.hasNext();) {
            Entry<Pair<String, LockedMethod>, ReentrantLock> entry = it.next();
            if (entry.getKey().getLeft().equals(id)) {
                it.remove();
            }
        }
    }
}
