/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.testing.node.blocking;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A static repository of {@link Lock} objects, identified by (String) id.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BlockingRepository {

    private static final Map<String, ReentrantLock> LOCK_REPOSITORY =
        new HashMap<String, ReentrantLock>();

    private BlockingRepository() {
    }

    /**
     * Add a lock to the repository.
     *
     * @param id The id of the lock
     * @param lock The lock
     * @throws NullPointerException if either arg is null
     * @throws IllegalArgumentException If id is already in use
     */
    public static synchronized void put(
            final String id, final ReentrantLock lock) {
        if (id == null || lock == null) {
            throw new IllegalArgumentException("id or lock must not be null");
        }
        if (LOCK_REPOSITORY.containsKey(id)) {
            throw new IllegalArgumentException("Lock ID already in use: " + id);
        }
        LOCK_REPOSITORY.put(id, lock);
    }

    /**
     * Get the lock associated with the id or null if not present.
     *
     * @param id The id of interest
     * @return The lock or null
     */
    public static synchronized ReentrantLock get(final String id) {
        return LOCK_REPOSITORY.get(id);
    }

    /**
     * Remove and get the lock associated with the id. (The id will not present
     * in the map after the call.)
     * @param id The id of interest.
     * @return The lock previously assigned to the id or null if not present.
     */
    public static synchronized ReentrantLock remove(final String id) {
        return LOCK_REPOSITORY.remove(id);
    }
}
