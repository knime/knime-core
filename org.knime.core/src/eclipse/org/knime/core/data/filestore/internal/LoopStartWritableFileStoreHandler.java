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
 *   Jul 10, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;
import java.util.BitSet;
import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.FlowLoopContext;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class LoopStartWritableFileStoreHandler
    extends WriteFileStoreHandler implements ILoopStartWriteFileStoreHandler {

    private static final byte[] OUTER_LOOP_PATH = new byte[] {};

    private final FlowLoopContext m_flowLoopContext;
    private FileStoresInLoopCache m_fileStoresInLoopCache;
    private final NestedLoopIdentifierProvider m_nestedLoopIdentifierProvider;

    /**
     * @param name
     * @param storeUUID
     * @param flowLoopContext */
    public LoopStartWritableFileStoreHandler(final String name, final UUID storeUUID,
            final FlowLoopContext flowLoopContext) {
        super(name, storeUUID);
        m_flowLoopContext = flowLoopContext;
        m_nestedLoopIdentifierProvider = new NestedLoopIdentifierProvider();
    }

    /** {@inheritDoc} */
    @Override
    public UUID getOutmostLoopStartStoreUUID() {
        return getStoreUUID();
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        super.open(exec);
        m_fileStoresInLoopCache = new FileStoresInLoopCache(exec);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore createFileStore(final String name) throws IOException {
        final FileStore fs = createFileStoreInLoopBody(name);
        super.addToDuplicateChecker(name);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore createFileStoreInLoopBody(final String name) throws IOException {
        final FileStore fs = createFileStoreInternal(name,
                OUTER_LOOP_PATH, m_flowLoopContext.getIterationIndex());
        m_fileStoresInLoopCache.add(fs);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore createFileStoreInNestedLoop(
            final String name, final byte[] nestedLoopPath, final int iterationIndex)
            throws IOException {
        return createFileStoreInternal(name, nestedLoopPath, iterationIndex);
    }

    /** {@inheritDoc}
     * @throws CanceledExecutionException */
    @Override
    public synchronized void onLoopEndFinish(final BufferedDataTable tableWithKeysToPersist)
    throws CanceledExecutionException {
        m_fileStoresInLoopCache.onIterationEnd(tableWithKeysToPersist, this);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void addFileStoreKeysFromNestedLoop(final BufferedDataTable keysFromNestedLoop) {
        m_fileStoresInLoopCache.addFileStoreKeysFromNestedLoops(keysFromNestedLoop);
    }

    /** {@inheritDoc} */
    @Override
    public byte[] createNestedLoopPath() {
        return new byte[] {m_nestedLoopIdentifierProvider.checkOut()};
    }

    /** {@inheritDoc} */
    @Override
    public void clearNestedLoopPath(final byte childByte) {
        m_nestedLoopIdentifierProvider.checkIn(childByte);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCreatedInThisLoop(final FileStoreKey key) {
        return key.getStoreUUID().equals(getStoreUUID());
    }

    static final class NestedLoopIdentifierProvider {
        private final BitSet m_childBytesInUse = new BitSet(Byte.MAX_VALUE);

        synchronized byte checkOut() {
            int nextByteAvailable = m_childBytesInUse.nextClearBit(0);
            if (nextByteAvailable > Byte.MAX_VALUE) {
                throw new IllegalStateException("Too many nested loops: " + nextByteAvailable);
            }
            m_childBytesInUse.set(nextByteAvailable);
            return (byte) nextByteAvailable;
        }

        synchronized void checkIn(final byte value) {
            m_childBytesInUse.clear(value);
        }

    }

}
