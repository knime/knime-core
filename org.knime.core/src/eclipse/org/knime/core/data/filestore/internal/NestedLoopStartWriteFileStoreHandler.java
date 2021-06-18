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
 *
 * History
 *   Jul 11, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.data.filestore.internal.LoopStartWriteFileStoreHandler.NestedLoopIdentifierProvider;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.FlowLoopContext;

/**
 * File store handler that is associated with a loop start node that is part of a loop body (nested loops).
 * They delegate to the outer loop start node but also keep a history of which file stores were created
 * in their (inner) loop execution to allow for a cleanup after each iteration.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 */
public final class NestedLoopStartWriteFileStoreHandler implements ILoopStartWriteFileStoreHandler {

    private final ILoopStartWriteFileStoreHandler m_reference;
    private final int[] m_thisNestedLoopPath;
    private final NestedLoopIdentifierProvider m_nestedLoopIdentifierProvider;

    private final FlowLoopContext m_flowLoopContext;
    private InternalDuplicateChecker m_duplicateChecker;

    private FileStoresInLoopCache m_fileStoresInLoopCache;
    private FileStoresInLoopCache m_endNodeCacheWithKeysToPersist;


    /**
     * @param reference */
    public NestedLoopStartWriteFileStoreHandler(final ILoopStartWriteFileStoreHandler reference,
            final FlowLoopContext flowLoopContext) {
        m_reference = reference;
        m_flowLoopContext = flowLoopContext;
        m_nestedLoopIdentifierProvider = new NestedLoopIdentifierProvider();
        m_thisNestedLoopPath = m_reference.createNestedLoopPath();
    }

    /** {@inheritDoc} */
    @Override
    public void addToRepository(final IDataRepository repository) {
        // ignore, handler does not define own file stores (only the start does)
    }

    /** {@inheritDoc} */
    @Override
    public UUID getStoreUUID() {
        return m_reference.getStoreUUID();
    }

    /** {@inheritDoc} */
    @Override
    public UUID getOutmostLoopStartStoreUUID() {
        return m_reference.getOutmostLoopStartStoreUUID();
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreKey translateToLocal(final FileStore fs, final FlushCallback flushCallback) {
        return m_reference.translateToLocal(fs, flushCallback);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustBeFlushedPriorSave(final FileStore fs) {
        return m_reference.mustBeFlushedPriorSave(fs);
    }

    /** {@inheritDoc} */
    @Override
    public IDataRepository getDataRepository() {
        return m_reference.getDataRepository();
    }

    /** {@inheritDoc} */
    @Override
    public void clearAndDispose() {
        ILoopStartWriteFileStoreHandler.clearFileStoresFromPreviousIteration(m_endNodeCacheWithKeysToPersist,
            m_fileStoresInLoopCache, this);
        m_endNodeCacheWithKeysToPersist = null;
        m_reference.clearNestedLoopPath(m_thisNestedLoopPath[m_thisNestedLoopPath.length - 1]);
        if (m_fileStoresInLoopCache != null) {
            m_fileStoresInLoopCache.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override
    public FileStore getFileStore(final FileStoreKey key) {
        return m_reference.getFileStore(key);
    }

    /** {@inheritDoc} */
    @Override
    public FileStore createFileStore(final String name) throws IOException {
        FileStore fs = createFileStoreInLoopBody(name);
        m_duplicateChecker.add(name);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public FileStore createFileStore(final String name, final int[] nestedLoopPath, final int iterationIndex)
        throws IOException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public FileStore createFileStoreInLoopBody(final String name) throws IOException {
        FileStore fs = m_reference.createFileStoreInNestedLoop(name, m_thisNestedLoopPath,
                m_flowLoopContext.getIterationIndex());
        m_fileStoresInLoopCache.add(fs);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        ILoopStartWriteFileStoreHandler.clearFileStoresFromPreviousIteration(m_endNodeCacheWithKeysToPersist,
            m_fileStoresInLoopCache, this);
        m_endNodeCacheWithKeysToPersist = null;
        m_fileStoresInLoopCache = new FileStoresInLoopCache(exec);
        m_duplicateChecker = new InternalDuplicateChecker();
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (m_duplicateChecker != null) {
            m_duplicateChecker.close();
            m_duplicateChecker = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void ensureOpenAfterLoad() throws IOException {
        m_reference.ensureOpenAfterLoad();
    }

    /** {@inheritDoc} */
    @Override
    public FileStore createFileStoreInNestedLoop(final String name,
            final int[] nestedLoopPath, final int iterationIndex) throws IOException {
        return m_reference.createFileStoreInNestedLoop(name, nestedLoopPath, iterationIndex);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onLoopEndFinish(
            final FileStoresInLoopCache endNodeCacheWithKeysToPersist) throws CanceledExecutionException {
        m_endNodeCacheWithKeysToPersist = endNodeCacheWithKeysToPersist;
        m_reference.addFileStoreKeysFromNestedLoop(m_endNodeCacheWithKeysToPersist);
    }

    /** {@inheritDoc} */
    @Override
    public void addFileStoreKeysFromNestedLoop(final FileStoresInLoopCache endNodeCacheWithKeysToPersist) {
        m_fileStoresInLoopCache.addFileStoreKeysFromNestedLoops(endNodeCacheWithKeysToPersist);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized int[] createNestedLoopPath() {
        int child = m_nestedLoopIdentifierProvider.checkOut();
        int[] result = Arrays.copyOf(m_thisNestedLoopPath, m_thisNestedLoopPath.length + 1);
        result[m_thisNestedLoopPath.length] = child;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void clearNestedLoopPath(final int child) {
        m_nestedLoopIdentifierProvider.checkIn(child);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCreatedInThisLoop(final FileStoreKey key) {
        if (!key.getStoreUUID().equals(getOutmostLoopStartStoreUUID())) {
            return false;
        }
        int[] keyNestedLoopPath = key.getNestedLoopPath();
        if (keyNestedLoopPath == null || keyNestedLoopPath.length < m_thisNestedLoopPath.length) {
            return false;
        }
        for (int i = 0; i < m_thisNestedLoopPath.length; i++) {
            if (m_thisNestedLoopPath[i] != keyNestedLoopPath[i]) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReference() {
        return true;
    }

}
