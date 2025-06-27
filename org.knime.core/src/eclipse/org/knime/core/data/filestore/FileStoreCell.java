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
 *   Jun 26, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.FileStoreProxy;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;

/**
 * A {@link DataCell} that references {@link FileStore}s with direct file access, e.g. for the serialization of large
 * data blobs. When this cell is initially created, the {@link FileStore}s must be given. But when a {@link FileStoreCell} is
 * deserialized, the FileStore references will automatically be restored.
 *
 * Derived classes should implement postConstruct and flushToFileStore to load and save externally stored data. To read
 * data from the {@link FileStore}s after cell deserialization, postConstruct is called exactly once, when all
 * {@link FileStore}s are present. Similarly, when the cell is about to be serialized, flushToFileStore is invoked once
 * and expects all {@link FileStore}s to be written.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 2.6
 */
public abstract class FileStoreCell extends DataCell implements FlushCallback {

    private FileStoreProxy[] m_fileStoreProxies;

    private boolean m_isFlushedToFileStore;

    /**
     * Create a {@link FileStoreCell} with an array of {@link FileStore}s.
     *
     * @param fileStores The fileStores that this cell is allowed to read from and write to
     * @since 3.7
     */
    protected FileStoreCell(final FileStore[] fileStores) {
        m_fileStoreProxies = Arrays.stream(fileStores).map(FileStoreProxy::new).toArray(FileStoreProxy[]::new);
    }

    /**
     * Create a {@link FileStoreCell} with a single {@link FileStore}
     * @param fileStore
     */
    protected FileStoreCell(final FileStore fileStore) {
       this(new FileStore[] { fileStore });
    }

    /**
     * This constructor should only be used when the cell is read from a persisted stream. Referenced {@link FileStore}s
     * will be restored automatically.
     */
    protected FileStoreCell() {
        m_isFlushedToFileStore = true;
        m_fileStoreProxies = new FileStoreProxy[0];
    }

    /** @return the first fileStore. Attention: may only be called if the number of FileStores is == 1. */
    @Deprecated
    protected FileStore getFileStore() {
        assert m_fileStoreProxies.length == 1;
        return m_fileStoreProxies[0].getFileStore();
    }

    /**
     * @since 3.7
     * @return The number of file stores referenced from this cell.
     */
    final int getNumFileStores() {
        return m_fileStoreProxies.length;
    }

    /**
     * @since 3.7
     * @return An array of the {@link FileStoreKey}s corresponding to the {@link FileStore}s referenced by this cell, in
     *         the same order as the {@link FileStore}s.
     */
    final FileStoreKey[] getFileStoreKeys() {
        return Arrays.stream(m_fileStoreProxies).map(FileStoreProxy::getFileStoreKey).toArray(FileStoreKey[]::new);
    }

    /**
     * @since 3.7
     * @return The array of all referenced {@link FileStore}s.
     */
    protected FileStore[] getFileStores() {
        return Arrays.stream(m_fileStoreProxies).map(FileStoreProxy::getFileStore).toArray(FileStore[]::new);
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     * @since 4.3
     */
    final void retrieveFileStoreHandlersFrom(final FileStoreKey[] fileStoreKeys, final IDataRepository repository,
        final boolean postConstruct) throws IOException {
        m_fileStoreProxies = new FileStoreProxy[fileStoreKeys.length];
        int fsIdx = 0;
        for (FileStoreKey key : fileStoreKeys) {
            FileStoreProxy proxy = new FileStoreProxy();
            proxy.retrieveFileStoreHandlerFrom(key, repository);
            m_fileStoreProxies[fsIdx] = proxy;
            fsIdx++;
        }
        if (postConstruct) {
            postConstruct();
        }
    }


    /** Called after the cell is deserialized from a stream. Clients
     * can now access the file.
     * @throws IOException If thrown, the cell will be replaced by a missing value in the data stream and
     * an error will be reported to the log.  */
    protected void postConstruct() throws IOException {
        // no op.
    }

    void callFlushIfNeeded() throws IOException {
        if (!m_isFlushedToFileStore) {
            flushToFileStore();
            m_isFlushedToFileStore = true;
        }
    }

    /**
     * Called before the cell is about to be serialized. Subclasses may override it to make sure the content is sync'ed
     * with the file (e.g. in-memory content is written to the FileStore). This method is called only once, even if the
     * cell uses multiple FileStores.
     *
     * <p>
     * This method is also called when the file underlying the cell is copied into a another context (from a
     * BufferedDataTable to DataTable).
     *
     * @throws IOException If thrown, the cell will be replaced by a missing value in the data stream and an error will
     *             be reported to the log.
     * @since 2.8
     */
    protected void flushToFileStore() throws IOException {
        // no op.
    }

    /** @return whether this cell's content has been flushed to the {@link FileStore}s */
    boolean isFlushedToFileStore() {
        return m_isFlushedToFileStore;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Arrays.stream(m_fileStoreProxies).map(Object::toString).collect(Collectors.joining(", "));
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        FileStoreProxy[] otherFileStoreProxies = ((FileStoreCell)dc).m_fileStoreProxies;
        if (m_fileStoreProxies.length != otherFileStoreProxies.length) {
            return false;
        }

        for (int fsIdx = 0; fsIdx < m_fileStoreProxies.length; fsIdx++) {
            if (!otherFileStoreProxies[fsIdx].equals(m_fileStoreProxies[fsIdx])) {
                return equalFileStoreContent(dc);
            }
        }
        return true;
    }

    /**
     * @param dc datacell to compare, has the same type like this {@link FileStoreCell}
     * @return if the cells have equal content
     * @since 5.1
     */
    protected boolean equalFileStoreContent(final DataCell dc) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_fileStoreProxies);
    }
}
