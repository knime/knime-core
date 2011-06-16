/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: May 11, 2011
 * Author: ohl
 */
package org.knime.core.util;

import java.io.File;
import java.util.HashMap;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;

/**
 * Locks file exclusively for this VM - but accepts multiple locks within this
 * VM.
 *
 * @author ohl, University of Konstanz
 */
public final class VMFileLocker {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(VMFileLocker.class);

    private static final HashMap<File, MutableInteger> COUNTS =
            new HashMap<File, MutableInteger>();

    private static final HashMap<File, FileLocker> LOCKS =
            new HashMap<File, FileLocker>();

    private static final boolean DISABLE_VM_LOCKS =
        Boolean.getBoolean(KNIMEConstants.PROPERTY_DISABLE_VM_FILE_LOCK);

    /** filename of lock file. */
    public static final String LOCK_FILE = ".knimeLock";

    /**
     *
     */
    private VMFileLocker() {
        // its a static class
    }

    /**
     * Locks the specified directory (by creating a ".knimeLock" file and
     * placing an exclusive lock on it). Multiple lock requests on the same dir
     * (within the same VM) will succeed. Other instances will not be able to
     * lock the file. Each call to lock must be eventually followed by a call to
     * unlock. Locks are released if the VM is gone.
     *
     * @param dir to lock
     * @return true if the dir was locked, false if it is already locked (by
     *         another instance) or the file could not be created (if the dir is
     *         r/o).
     */
    public static synchronized boolean lockForVM(final File dir) {
        if (DISABLE_VM_LOCKS) {
            return true;
        }
        if (!dir.isDirectory()) {
            LOGGER.coding("Files should not be locked. Only directories.");
            return false;
        }
        MutableInteger cnt = COUNTS.get(dir);
        if (cnt == null) {
            // maps must be in synch
            assert LOCKS.get(dir) == null;

            FileLocker fl = null;
            if (dir.canWrite()) {
                // try to acquire a new lock
                fl = new FileLocker(new File(dir, LOCK_FILE));
                try {
                    if (!fl.lock()) {
                        return false;
                    }
                } catch (Exception e) {
                    String m =
                            e.getMessage() == null ? "<no details>" : e
                                    .getMessage();
                    LOGGER.warn("I/O Error while trying to lock dir \"" + dir
                            + "\": " + m, e);
                    return false;
                }
            } // else we assume a read-only dir - no need to lock...
            cnt = new MutableInteger(1);
            COUNTS.put(dir, cnt);
            LOCKS.put(dir, fl);
            return true;
        }

        cnt.inc();
        // maps must be in sync
        assert LOCKS.get(dir) != null;
        return true;
    }

    /**
     * Unlocks the specified dir. It must have been locked before! If the dir
     * was locked multiple times it decrements the lock count. (See #lockForVM.)
     *
     * @param dir to release one lock from
     */
    public static synchronized void unlockForVM(final File dir) {
        if (DISABLE_VM_LOCKS) {
            return;
        }
        if (!dir.isDirectory()) {
            LOGGER.coding("Files should not be un/locked. Only directories.");
            return;
        }
        MutableInteger cnt = COUNTS.get(dir);
        FileLocker fl = LOCKS.get(dir);
        if (cnt == null) {
            // maps should be in sync
            assert fl == null;
            if (fl != null) {
                // in case assertions are off
                fl.release();
                LOCKS.remove(dir);
            }
            LOGGER.coding("Trying to unlock a directory that is not locked!");
            return;
        }
        cnt.dec();
        if (cnt.intValue() <= 0) {
            // last lock released: release file lock
            if (fl != null) { // could be null for r/o dirs
                fl.release();
            }
            COUNTS.remove(dir);
            LOCKS.remove(dir);
        }
    }

    /**
     * @param dir to test
     * @return true if this VM has a lock on the specified directory
     */
    public static synchronized boolean isLockedForVM(final File dir) {
        if (DISABLE_VM_LOCKS) {
            return true;
        }
        if (!dir.isDirectory()) {
            LOGGER.coding("Files should not be un/locked. Only directories.");
            return false;
        }
        MutableInteger cnt = COUNTS.get(dir);
        return cnt != null;
    }

}
