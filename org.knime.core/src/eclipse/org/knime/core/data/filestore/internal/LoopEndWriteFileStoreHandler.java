/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Jul 12, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.LRUCache;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class LoopEndWriteFileStoreHandler implements IWriteFileStoreHandler {

    private final ILoopStartWriteFileStoreHandler m_loopStartFSHandler;
    private FileStoresInLoopCache m_fileStoresInLoopCache;
    private InternalDuplicateChecker m_duplicateChecker;
    private LRUCache<FileStoreKey, FileStoreKey> m_fsKeysToKeepLRUCache;

    /**
     * @param loopStartFSHandler */
    public LoopEndWriteFileStoreHandler(final ILoopStartWriteFileStoreHandler loopStartFSHandler) {
        m_loopStartFSHandler = loopStartFSHandler;
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreHandlerRepository getFileStoreHandlerRepository() {
        return m_loopStartFSHandler.getFileStoreHandlerRepository();
    }

    /** {@inheritDoc} */
    @Override
    public UUID getStoreUUID() {
        // no own file stores
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreKey translateToLocal(final FileStore fs) {
        final FileStoreKey result = m_loopStartFSHandler.translateToLocal(fs);
        // might be called after node is closed, e.g. when workflow is saved
        boolean isClosed = m_fileStoresInLoopCache == null;
        if (!isClosed && m_loopStartFSHandler.isCreatedInThisLoop(result)) {
            if (m_fsKeysToKeepLRUCache.put(result, result) == null) {
                m_fileStoresInLoopCache.add(result);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustBeFlushedPriorSave(final FileStore fs) {
        return m_loopStartFSHandler.mustBeFlushedPriorSave(fs)
                // underlying files needs to be flushed as a subsequent save of the workflow will first save
                // the start node and then the loop body (which then would save the contained fs cells,
                // which is too late) ... all this is really only to make sure that smart implementations of
                // a file store cell get notified to save their stuff.
                || m_loopStartFSHandler.isCreatedInThisLoop(FileStoreUtil.getFileStoreKey(fs));
    }

    /** {@inheritDoc} */
    @Override
    public void clearAndDispose() {
        // ignore, loop start will be reset, too
    }

    /** {@inheritDoc} */
    @Override
    public FileStore getFileStore(final FileStoreKey key) {
        return m_loopStartFSHandler.getFileStore(key);
    }

    /** {@inheritDoc} */
    @Override
    public FileStore createFileStore(final String name) throws IOException {
        if (m_duplicateChecker == null) {
            throw new IllegalStateException("file store handler is not open");
        }
        FileStore fs = m_loopStartFSHandler.createFileStore(name);
        m_duplicateChecker.add(name);
        m_fileStoresInLoopCache.add(fs);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        m_fileStoresInLoopCache = new FileStoresInLoopCache(exec);
        m_duplicateChecker = new InternalDuplicateChecker();
        m_fsKeysToKeepLRUCache = new LRUCache<FileStoreKey, FileStoreKey>(1000);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (m_duplicateChecker != null) {
            m_duplicateChecker.close();
            try {
                BufferedDataTable keysToRetainTable = m_fileStoresInLoopCache.close();
                m_loopStartFSHandler.onLoopEndFinish(keysToRetainTable);
            } catch (CanceledExecutionException e) {
                throw new RuntimeException("Canceled", e);
            } finally {
                m_fsKeysToKeepLRUCache = null;
                m_fileStoresInLoopCache = null;
                m_duplicateChecker = null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addToRepository(final FileStoreHandlerRepository repository) {
        // ignore, handler does not define own file stores (only the start does)
    }

}
