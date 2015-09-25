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
 *
 * History
 *   Jul 12, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.File;
import java.util.Collections;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.util.FileUtil;
import org.knime.core.util.MutableInteger;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class FileStoresInLoopCache {

    private static final String COL_NAME = "file-store-keys";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileStoresInLoopCache.class);

    static final DataTableSpec LOOP_FILE_STORE_SPEC = new DataTableSpec(
            new DataColumnSpecCreator(COL_NAME, FileStoreKeyDataCell.TYPE).createSpec());

    private final ExecutionContext m_exec;
    private BufferedDataContainer m_createdFileStoresContainer;
    private BufferedDataTable m_createdFileStoresTable;
    private FileStoreKey m_lastAddedKey;
    private boolean m_keysWereAddedSorted = true;

    /**
     *  */
    FileStoresInLoopCache(final ExecutionContext exec) {
        m_exec = exec;
        m_createdFileStoresContainer = exec.createDataContainer(LOOP_FILE_STORE_SPEC);
    }

    void onIterationEnd(final FileStoresInLoopCache endNodeCacheWithKeysToPersist,
            final ILoopStartWriteFileStoreHandler handler) throws CanceledExecutionException {
        close();
        deletableUnusedFileStores(endNodeCacheWithKeysToPersist, handler);
    }

    void add(final FileStore fs) {
        add(FileStoreUtil.getFileStoreKey(fs));
    }

    synchronized void add(final FileStoreKey key) {
        assert m_createdFileStoresContainer != null : "close was already called";
        if (m_lastAddedKey != null) {
            int c = m_lastAddedKey.compareTo(key);
            if (c == 0) {
                return; // already added (last), avoid duplicates where possible
            } else if (c > 0) {
                m_keysWereAddedSorted = false;
            }
        }
        m_lastAddedKey = key;
        m_createdFileStoresContainer.addRowToTable(new DefaultRow(
                RowKey.createRowKey(m_createdFileStoresContainer.size()),
                new FileStoreKeyDataCell(key)));
    }

    /**
     * @param endNodeCacheWithKeysToPersist */
    synchronized void addFileStoreKeysFromNestedLoops(final FileStoresInLoopCache endNodeCacheWithKeysToPersist) {
        for (DataRow r : endNodeCacheWithKeysToPersist.getCreatedFileStoresTable()) {
            add(getFileStoreKey(r));
        }
    }

    void deletableUnusedFileStores(final FileStoresInLoopCache endNodeCacheWithKeysToPersist,
            final ILoopStartWriteFileStoreHandler handler) throws CanceledExecutionException {
        MutableInteger nrFilesDeleted = new MutableInteger(0);
        MutableInteger nrFailedDeletes = new MutableInteger(0);
        CloseableRowIterator allKeysIterator = m_createdFileStoresTable.iterator();
        CloseableRowIterator endNodeKeysIterator = endNodeCacheWithKeysToPersist.getCreatedFileStoresTable().iterator();

        FileStoreKey nextLoopEndFSKey = next(endNodeKeysIterator, null);
        FileStoreKey nextAllFSKey = next(allKeysIterator, null);

        while (nextLoopEndFSKey != null) {
            if (nextAllFSKey != null) {
                final int compare = nextLoopEndFSKey.compareTo(nextAllFSKey);
                if (compare == 0) {
                    nextLoopEndFSKey = next(endNodeKeysIterator, nextLoopEndFSKey);
                    nextAllFSKey = next(allKeysIterator, nextAllFSKey);
                } else if (compare > 0) {
                    delete(nextAllFSKey, handler, nrFilesDeleted, nrFailedDeletes);
                    nextAllFSKey = next(allKeysIterator, nextAllFSKey);
                } else {
                    nextLoopEndFSKey = next(endNodeKeysIterator, nextLoopEndFSKey);
                }
            } else {
                break;
            }
        }
        while (nextAllFSKey != null) {
            delete(nextAllFSKey, handler, nrFilesDeleted, nrFailedDeletes);
            nextAllFSKey = next(allKeysIterator, nextAllFSKey);
        }
        allKeysIterator.close();
        endNodeKeysIterator.close();
        if (nrFilesDeleted.intValue() > 0) {
            StringBuilder b = new StringBuilder("Deleted ");
            b.append(nrFilesDeleted.intValue()).append(" files ");
            if (nrFailedDeletes.intValue() > 0) {
                b.append("; ").append(nrFailedDeletes.intValue()).append(" of which failed");
            } else {
                b.append("successfully");
            }
            LOGGER.debug(b.toString());
        }
    }

    /**
     * @throws CanceledExecutionException
     *  */
    BufferedDataTable close() throws CanceledExecutionException {
        m_createdFileStoresContainer.close();
        BufferedDataTable table = m_createdFileStoresContainer.getTable();
        m_createdFileStoresContainer = null;
        if (m_keysWereAddedSorted) {
            m_createdFileStoresTable = table;
        } else {
            BufferedDataTableSorter sorter = new BufferedDataTableSorter(table,
                    Collections.singletonList(COL_NAME), new boolean[] {true});
            BufferedDataTable sort = sorter.sort(m_exec.createSilentSubExecutionContext(0.0));
            BufferedDataContainer unique = m_exec.createDataContainer(LOOP_FILE_STORE_SPEC);
            FileStoreKey last = null;
            for (DataRow r : sort) {
                FileStoreKey key = getFileStoreKey(r);
                if (!ConvenienceMethods.areEqual(last, key)) {
                    unique.addRowToTable(r);
                }
                last = key;
            }
            unique.close();
            m_exec.clearTable(table);
            m_createdFileStoresTable = unique.getTable();
        }
        return m_createdFileStoresTable;
    }

    /**
     * @return the createdFileStoresTable
     */
    BufferedDataTable getCreatedFileStoresTable() {
        CheckUtils.checkState(m_createdFileStoresTable != null, "Close has not been called");
        return m_createdFileStoresTable;
    }

    void dispose() {
        if (m_createdFileStoresTable != null) {
            m_exec.clearTable(m_createdFileStoresTable);
            m_createdFileStoresTable = null;
        }
    }

    private void delete(final FileStoreKey key, final ILoopStartWriteFileStoreHandler handler,
            final MutableInteger nrFilesDeleted, final MutableInteger nrFilesFailedDelete) {
        FileStore fileStore = handler.getFileStore(key);
        File file = fileStore.getFile();
        if (file.exists() && !FileUtil.deleteRecursively(file)) {
            nrFilesFailedDelete.inc();
        }
        nrFilesDeleted.inc();
    }

    private static FileStoreKey next(final RowIterator it, final FileStoreKey previousKey) {
        FileStoreKey newKey;
        // the table may have duplicates, but they are sorted and therefore consecutive.
        do {
            DataRow nextRow = it.hasNext() ? it.next() : null;
            if (nextRow == null) {
                return null;
            }
            newKey = getFileStoreKey(nextRow);
        } while (newKey != null && previousKey != null && newKey.compareTo(previousKey) == 0);
        return newKey;
    }

    private static boolean equalFileStoreCell(final FileStoreKey key1, final FileStoreKey key2) {
        if (key1 == null || key2 == null) {
            return false;
        }
        return key1.compareTo(key2) == 0;
    }

    private static FileStoreKey getFileStoreKey(final DataRow r1) {
        return ((FileStoreKeyDataValue)r1.getCell(0)).getKey();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override
    public String toString() {
        if (m_createdFileStoresTable != null) {
            return "Closed - " + m_createdFileStoresTable.size() + " element(s):\n"
                    + DefaultTable.toString(m_createdFileStoresTable);
        } else {
            return "Open - currently " + m_createdFileStoresContainer.size() + " element(s)";
        }
    }

}
