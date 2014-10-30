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
        if (!dir.exists()) {
            LOGGER.warn("Directory '" + dir.getAbsolutePath() + "' does not exist, cannot lock it");
            return false;
        } else if (!dir.isDirectory()) {
            LOGGER.coding("Files should not be locked. Only directories. ("
                    + dir.getAbsolutePath() + " is not a dir.)");
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
        assert LOCKS.containsKey(dir);
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
        if (!dir.exists()) {
            LOGGER.warn("Directory '" + dir.getAbsolutePath() + "' does not exist (any more), cannot unlock it");
            COUNTS.remove(dir);
            LOCKS.remove(dir);
            return;
        } else if (!dir.isDirectory()) {
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
