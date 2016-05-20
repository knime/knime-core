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
 *   Jun 26, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.FileStoreProxy;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public abstract class FileStoreCell extends DataCell implements FlushCallback {

    private FileStoreProxy m_fileStoreProxy;
    private boolean m_isFlushedToFileStore;

    /**
     *  */
    protected FileStoreCell(final FileStore fileStore) {
        m_fileStoreProxy = new FileStoreProxy(fileStore);
    }

    /** Used when read from persisted stream.
     *  */
    protected FileStoreCell() {
        m_fileStoreProxy = new FileStoreProxy();
        m_isFlushedToFileStore = true;
    }

    /** @return the fileStoreKey */
    final FileStoreKey getFileStoreKey() {
        return m_fileStoreProxy.getFileStoreKey();
    }

    protected FileStore getFileStore() {
        return m_fileStoreProxy.getFileStore();
    }

    /** @noreference This method is not intended to be referenced by clients. */
    final void retrieveFileStoreHandlerFrom(final FileStoreKey key,
            final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException {
        m_fileStoreProxy.retrieveFileStoreHandlerFrom(key, fileStoreHandlerRepository);
        postConstruct();
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
            m_isFlushedToFileStore = true;
            flushToFileStore();
        }
    }

    /** Called before the cell is about to be serialized. Subclasses may override it to make sure the content
     * is sync'ed with the file (e.g. in-memory content is written to the FileStore).
     *
     * <p>This method is also called when the file underlying the cell is copied into a another context (from
     * a BufferedDataTable to DataTable).
     * @throws IOException If thrown, the cell will be replaced by a missing value in the data stream and
     * an error will be reported to the log.
     * @since 2.8 */
    protected void flushToFileStore() throws IOException {
        // no op.
    }

    /** @return the isFlushedToFileStore */
    boolean isFlushedToFileStore() {
        return m_isFlushedToFileStore;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_fileStoreProxy.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        FileStoreProxy otherFileStoreProxy = ((FileStoreCell)dc).m_fileStoreProxy;
        return m_fileStoreProxy.equals(otherFileStoreProxy);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_fileStoreProxy.hashCode();
    }

}
