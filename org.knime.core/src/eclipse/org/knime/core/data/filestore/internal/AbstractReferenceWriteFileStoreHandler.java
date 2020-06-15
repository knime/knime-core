/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 14, 2020 (hornm): created
 */
package org.knime.core.data.filestore.internal;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.node.ExecutionContext;

/**
 * Implementation that wraps/references another {@link IWriteFileStoreHandler} and delegates all the calls to it.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
abstract class AbstractReferenceWriteFileStoreHandler implements IWriteFileStoreHandler {

    private final IWriteFileStoreHandler m_delegate;

    /**
     * @param delegate the instance to delegate the calls to
     */
    protected AbstractReferenceWriteFileStoreHandler(final IWriteFileStoreHandler delegate) {
        m_delegate = delegate;
    }

    /**
     * @return the delegate file store handler
     */
    protected IWriteFileStoreHandler getDelegate() {
        return m_delegate;
    }

    @Override
    public IDataRepository getDataRepository() {
        return m_delegate.getDataRepository();
    }

    @Override
    public FileStore createFileStore(final String name) throws IOException {
        return m_delegate.createFileStore(name);
    }

    @Override
    public FileStore createFileStore(final String name, final int[] nestedLoopPath, final int iterationIndex)
        throws IOException {
        return m_delegate.createFileStore(name, nestedLoopPath, iterationIndex);
    }

    @Override
    public void clearAndDispose() {
        m_delegate.clearAndDispose();
    }

    @Override
    public FileStore getFileStore(final FileStoreKey key) {
        return m_delegate.getFileStore(key);
    }

    @Override
    public void open(final ExecutionContext exec) {
        m_delegate.open(exec);
    }

    @Override
    public void addToRepository(final IDataRepository repository) {
        m_delegate.addToRepository(repository);
    }

    @Override
    public void close() {
        m_delegate.close();
    }

    @Override
    public void ensureOpenAfterLoad() throws IOException {
        m_delegate.ensureOpenAfterLoad();
    }

    @Override
    public FileStoreKey translateToLocal(final FileStore fs, final FlushCallback flushCallback) {
        return m_delegate.translateToLocal(fs, flushCallback);
    }

    @Override
    public boolean mustBeFlushedPriorSave(final FileStore fs) {
        return m_delegate.mustBeFlushedPriorSave(fs);
    }

    @Override
    public UUID getStoreUUID() {
        return m_delegate.getStoreUUID();
    }

    @Override
    public File getBaseDir() {
        if (isReference()) {
            return null;
        } else {
            return m_delegate.getBaseDir();
        }
    }

}
