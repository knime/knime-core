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

import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.util.FileUtil;

/**
 * File store handler used for non-start nodes that are part of a loop body (not the loop end). They forward all calls
 * to the file store handler associated with the loop start (i.e. the file store handler of another node).
 *
 * Can also be used to create a file store handler that delegates to another {@link IWriteFileStoreHandler} - see
 * {@link #ReferenceWriteFileStoreHandler(WriteFileStoreHandler, NodeID)}.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ReferenceWriteFileStoreHandler extends DelegateWriteFileStoreHandler {

    private InternalDuplicateChecker m_duplicateChecker;
    private NodeID m_nodeId;

    /**
     * @param reference the file store handler to delegate to (must be the file store handler of another node!)
     */
    public ReferenceWriteFileStoreHandler(final ILoopStartWriteFileStoreHandler reference) {
        super(reference);
        if (reference == null) {
            throw new NullPointerException("Argument must not be null.");
        }
    }

    /**
     * @param reference the file store handler to delegate to (must be the file store handler of another node!)
     * @param nodeId the node id of the node this file store handler belongs to
     */
    public ReferenceWriteFileStoreHandler(final IWriteFileStoreHandler reference, final NodeID nodeId) {
        super(reference);
        CheckUtils.checkArgumentNotNull(reference);
        CheckUtils.checkArgumentNotNull(nodeId);
        m_nodeId = nodeId;
    }

    /** {@inheritDoc} */
    @Override
    public void addToRepository(final IDataRepository repository) {
        // ignore, handler does not define own file stores (only the start does)
    }

    /** {@inheritDoc} */
    @Override
    public void clearAndDispose() {
        // ignore
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore createFileStore(final String name) throws IOException {
        if (m_duplicateChecker == null) {
            throw new IOException("File store handler \"" + toString() + "\" is read only/closed");
        }
        m_duplicateChecker.add(name);
        if (getDelegate() instanceof ILoopStartWriteFileStoreHandler) {
            return ((ILoopStartWriteFileStoreHandler)getDelegate()).createFileStoreInLoopBody(name);
        } else {
            assert m_nodeId != null;
            return getDelegate().createFileStore(FileUtil.getValidFileName(name + "#" + m_nodeId, 0));
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void open(final ExecutionContext exec) {
        m_duplicateChecker = new InternalDuplicateChecker();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void close() {
        if (m_duplicateChecker != null) {
            m_duplicateChecker.close();
            m_duplicateChecker = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Reference on " + getDelegate().toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReference() {
        return true;
    }

}
