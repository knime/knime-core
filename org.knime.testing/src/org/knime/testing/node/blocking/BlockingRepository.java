/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Nov 3, 2008 (wiswedel): created
 */
package org.knime.testing.node.blocking;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * A static repository of {@link Lock} objects, identified by (String) id.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BlockingRepository {
    
    private final static Map<String, Lock> LOCK_REPOSITORY =
        new HashMap<String, Lock>();
    
    private BlockingRepository() {
    }
    
    /**
     * Add a lock to the repository
     * @param id The id of the lock
     * @param lock The lock
     * @throws NullPointerException if either arg is null
     * @throws IllegalArgumentException If id is already in use
     */
    public static synchronized void put(final String id, final Lock lock) {
        if (id == null || lock == null) {
            throw new NullPointerException();
        }
        if (LOCK_REPOSITORY.containsKey(id)) {
            throw new IllegalArgumentException("Lock ID already in use: " + id);
        }
        LOCK_REPOSITORY.put(id, lock);
    }
    
    /**
     * Get the lock associated with the id or null if not present
     * @param id The id of interest
     * @return The lock or null
     */
    public static synchronized Lock get(final String id) {
        return LOCK_REPOSITORY.get(id);
    }
    
    /**
     * Remove and get the lock associated with the id. (The id will not present
     * in the map after the call.) 
     * @param id The id of interest.
     * @return The lock previously assigned to the id or null if not present.
     */
    public static synchronized Lock remove(final String id) {
        return LOCK_REPOSITORY.remove(id);
    }
}
