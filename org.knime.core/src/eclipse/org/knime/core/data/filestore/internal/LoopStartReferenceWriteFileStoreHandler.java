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
 *   Jul 11, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.internal.LoopStartWritableFileStoreHandler.NestedLoopIdentifierProvider;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.FlowLoopContext;

/**
 * File store handler that is associated with a loop start node that is part of a loop body (nested loops).
 * They delegate to the outer loop start node but also keep a history of which file stores were created
 * in there (inner) loop execution to allow for a cleanup after each iteration.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class LoopStartReferenceWriteFileStoreHandler implements ILoopStartWriteFileStoreHandler {

    private final ILoopStartWriteFileStoreHandler m_reference;
    private final byte[] m_thisNestedLoopPath;
    private final NestedLoopIdentifierProvider m_nestedLoopIdentifierProvider;

    private final FlowLoopContext m_flowLoopContext;
    private InternalDuplicateChecker m_duplicateChecker;

    private FileStoresInLoopCache m_fileStoresInLoopCache;
    private BufferedDataTable m_tableWithKeysToPersist;


    /**
     * @param reference */
    public LoopStartReferenceWriteFileStoreHandler(final ILoopStartWriteFileStoreHandler reference,
            final FlowLoopContext flowLoopContext) {
        m_reference = reference;
        m_flowLoopContext = flowLoopContext;
        m_nestedLoopIdentifierProvider = new NestedLoopIdentifierProvider();
        m_thisNestedLoopPath = m_reference.createNestedLoopPath();
    }

    /** {@inheritDoc} */
    @Override
    public void addToRepository(final FileStoreHandlerRepository repository) {
        // ignore, handler does not define own file stores (only the start does)
    }

    /** {@inheritDoc} */
    @Override
    public UUID getStoreUUID() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public UUID getOutmostLoopStartStoreUUID() {
        return m_reference.getOutmostLoopStartStoreUUID();
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreKey translateToLocal(final FileStore fs) {
        return m_reference.translateToLocal(fs);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustBeFlushedPriorSave(final FileStore fs) {
        return m_reference.mustBeFlushedPriorSave(fs);
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreHandlerRepository getFileStoreHandlerRepository() {
        return m_reference.getFileStoreHandlerRepository();
    }

    /** {@inheritDoc} */
    @Override
    public void clearAndDispose() {
        clearFileStoresFromPreviousIteration();
        m_reference.clearNestedLoopPath(m_thisNestedLoopPath[m_thisNestedLoopPath.length - 1]);
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
    public FileStore createFileStoreInLoopBody(final String name) throws IOException {
        FileStore fs = m_reference.createFileStoreInNestedLoop(name, m_thisNestedLoopPath,
                m_flowLoopContext.getIterationIndex());
        m_fileStoresInLoopCache.add(fs);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        clearFileStoresFromPreviousIteration();
        m_fileStoresInLoopCache = new FileStoresInLoopCache(exec);
        m_duplicateChecker = new InternalDuplicateChecker();
    }

    /**
     *  */
    private void clearFileStoresFromPreviousIteration() {
        if (m_tableWithKeysToPersist != null) {
            try {
                m_fileStoresInLoopCache.onIterationEnd(m_tableWithKeysToPersist, this);
            } catch (CanceledExecutionException e) {
                throw new RuntimeException("Canceled", e);
            }
            m_tableWithKeysToPersist = null;
        }
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
    public FileStore createFileStoreInNestedLoop(final String name,
            final byte[] nestedLoopPath, final int iterationIndex) throws IOException {
        return m_reference.createFileStoreInNestedLoop(name, nestedLoopPath, iterationIndex);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onLoopEndFinish(
            final BufferedDataTable tableWithKeysToPersist) throws CanceledExecutionException {
        m_tableWithKeysToPersist = tableWithKeysToPersist;
        m_reference.addFileStoreKeysFromNestedLoop(m_tableWithKeysToPersist);
    }

    /** {@inheritDoc} */
    @Override
    public void addFileStoreKeysFromNestedLoop(final BufferedDataTable keysFromNestedLoop) {
        m_fileStoresInLoopCache.addFileStoreKeysFromNestedLoops(keysFromNestedLoop);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized byte[] createNestedLoopPath() {
        byte childByte = m_nestedLoopIdentifierProvider.checkOut();
        byte[] result = Arrays.copyOf(m_thisNestedLoopPath, m_thisNestedLoopPath.length + 1);
        result[m_thisNestedLoopPath.length] = childByte;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void clearNestedLoopPath(final byte childByte) {
        m_nestedLoopIdentifierProvider.checkIn(childByte);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCreatedInThisLoop(final FileStoreKey key) {
        if (!key.getStoreUUID().equals(getOutmostLoopStartStoreUUID())) {
            return false;
        }
        byte[] keyNestedLoopPath = key.getNestedLoopPath();
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

}
