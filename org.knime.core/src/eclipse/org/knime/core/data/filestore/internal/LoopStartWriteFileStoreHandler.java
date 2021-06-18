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
 *   Jul 10, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;
import java.util.BitSet;
import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowLoopContext;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NativeNodeContainer;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 */
public class LoopStartWriteFileStoreHandler extends DelegateWriteFileStoreHandler
    implements ILoopStartWriteFileStoreHandler {

    private static final int[] OUTER_LOOP_PATH = new int[] {};

    private final FlowLoopContext m_flowLoopContext;
    private final NativeNodeContainer m_startNodeContainer;
    private FileStoresInLoopCache m_fileStoresInLoopCache;
    private final NestedLoopIdentifierProvider m_nestedLoopIdentifierProvider;

    private FileStoresInLoopCache m_endNodeCacheWithKeysToPersist;

    /**
     * If this loop start file store handler just references another one.
     */
    private final boolean m_referencesAnotherFileStoreHandler;

    /**
     * Initializes a new loop start file store handler with the given store id. All file stores in the loop will be
     * created at the node 'owning' this file store handler.
     *
     * @param startNode the {@link LoopStartNode}
     * @param storeUUID the file store handler's id
     * @param flowLoopContext the loop's context
     */
    public LoopStartWriteFileStoreHandler(final NativeNodeContainer startNode, final UUID storeUUID,
            final FlowLoopContext flowLoopContext) {
        super(new WriteFileStoreHandler(startNode.getNameWithID(), storeUUID));
        CheckUtils.checkArgument(startNode.isModelCompatibleTo(
            LoopStartNode.class), "Node not a start node: %s", startNode);
        m_startNodeContainer = startNode;
        m_flowLoopContext = flowLoopContext;
        m_nestedLoopIdentifierProvider = new NestedLoopIdentifierProvider();
        m_referencesAnotherFileStoreHandler = false;
    }

    /**
     * Initializes a new loop start file store handler that references the provided one. I.e. in that case it is a
     * 'reference file store handler', {@link IWriteFileStoreHandler#isReference()} returns <code>true</code> and the
     * actual file stores are created in the referenced file store handler.
     *
     * @param startNode the {@link LoopStartNode}
     * @param fsh the file store handler to be referenced (and delegated the most calls to)
     * @param flowLoopContext the loop's context
     */
    public LoopStartWriteFileStoreHandler(final NativeNodeContainer startNode, final IWriteFileStoreHandler fsh,
        final FlowLoopContext flowLoopContext) {
        super(fsh);
        CheckUtils.checkArgument(startNode.isModelCompatibleTo(LoopStartNode.class), "Node not a start node: %s",
            startNode);
        m_startNodeContainer = startNode;
        m_flowLoopContext = flowLoopContext;
        m_nestedLoopIdentifierProvider = new NestedLoopIdentifierProvider();
        m_referencesAnotherFileStoreHandler = true;
    }

    /** {@inheritDoc} */
    @Override
    public UUID getOutmostLoopStartStoreUUID() {
        return super.getStoreUUID();
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        super.open(exec);
        ILoopStartWriteFileStoreHandler.clearFileStoresFromPreviousIteration(m_endNodeCacheWithKeysToPersist,
            m_fileStoresInLoopCache, this);
        m_endNodeCacheWithKeysToPersist = null;
        m_fileStoresInLoopCache = new FileStoresInLoopCache(exec);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore createFileStore(final String name) throws IOException {
        final FileStore fs = createFileStoreInLoopBody(name);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore createFileStoreInLoopBody(final String name) throws IOException {
        final FileStore fs = super.createFileStore(name,
                OUTER_LOOP_PATH, m_flowLoopContext.getIterationIndex());
        m_fileStoresInLoopCache.add(fs);
        return fs;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore createFileStoreInNestedLoop(
            final String name, final int[] nestedLoopPath, final int iterationIndex)
            throws IOException {
        markStartNodeDirty();
        return super.createFileStore(name, nestedLoopPath, iterationIndex);
    }

    /** {@inheritDoc}
     * @throws CanceledExecutionException */
    @Override
    public synchronized void onLoopEndFinish(final FileStoresInLoopCache endNodeCacheWithKeysToPersist)
    throws CanceledExecutionException {
        markStartNodeDirty();
        m_endNodeCacheWithKeysToPersist = endNodeCacheWithKeysToPersist;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void addFileStoreKeysFromNestedLoop(final FileStoresInLoopCache endNodeCacheWithKeysToPersist) {
        m_fileStoresInLoopCache.addFileStoreKeysFromNestedLoops(endNodeCacheWithKeysToPersist);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAndDispose() {
        super.clearAndDispose();
        if (m_endNodeCacheWithKeysToPersist != null) {
            m_endNodeCacheWithKeysToPersist.dispose();
            m_endNodeCacheWithKeysToPersist = null;
        }
        if (m_fileStoresInLoopCache != null) {
            m_fileStoresInLoopCache.dispose();
            m_fileStoresInLoopCache = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int[] createNestedLoopPath() {
        return new int[] {m_nestedLoopIdentifierProvider.checkOut()};
    }

    /** {@inheritDoc} */
    @Override
    public void clearNestedLoopPath(final int child) {
        m_nestedLoopIdentifierProvider.checkIn(child);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCreatedInThisLoop(final FileStoreKey key) {
        // if this loop start file store handler just references another file store handler (e.g. because
        // it's part of a virtual scope), the store UUID of the delegate would not be unique for this loop -
        // is this a problem? (would possibly also cause file stores to be flushed in another, upstream loop)
        return key.getStoreUUID().equals(super.getStoreUUID());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReference() {
        return m_referencesAnotherFileStoreHandler;
    }

    private void markStartNodeDirty() {
        m_startNodeContainer.setDirty();
    }

    static final class NestedLoopIdentifierProvider {
        private final BitSet m_childsInUse = new BitSet();

        synchronized int checkOut() {
            int nextSlotAvailable = m_childsInUse.nextClearBit(0);
            m_childsInUse.set(nextSlotAvailable);
            return nextSlotAvailable;
        }

        synchronized void checkIn(final int value) {
            m_childsInUse.clear(value);
        }

    }

}
