/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on Sep 17, 2013 by wiswedel
 */
package org.knime.core.data.filestore;

import java.io.IOException;

import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.FileStoreKey;
import org.knime.core.data.filestore.internal.FileStoreProxy;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;

/**
 * Abstract super class of {@link PortObject}, which reference files.
 *
 * <p>Pending API. Don't extend (yet).
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public abstract class FileStorePortObject implements PortObject, FlushCallback {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileStorePortObject.class);

    private FileStoreProxy m_fileStoreProxy;

    /** Standard client constructor.
     * @param fileStore Non null file store object to wrap. */
    protected FileStorePortObject(final FileStore fileStore) {
        m_fileStoreProxy = new FileStoreProxy(fileStore);
    }

    /** Used when read from persisted stream. */
    protected FileStorePortObject() {
        m_fileStoreProxy = new FileStoreProxy();
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

    /** Called before the cell about to be serialized. Subclasses may override it to make sure the content
     * is sync'ed with the file (e.g. in-memory content is written to the FileStore).
     *
     * <p>This method is also called when the file underlying object is copied into a another context (e.g. persisted
     * outside the scope of the corresponding workflow).
     * @throws IOException If unable to flush. */
    protected void flushToFileStore() throws IOException {
        // no op.
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_fileStoreProxy.toString();
    }

    /** Access to the underlying file store. Derived class will use it to read the content of the file when needed.
     * This method must not be called in the {@link #FileStorePortObject() serialization constructor}
     * (use {@link #postConstruct()} instead.
     * @return The file store
     */
    protected FileStore getFileStore() {
        return m_fileStoreProxy.getFileStore();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FileStorePortObject)) {
            return false;
        }
        return m_fileStoreProxy.equals(((FileStorePortObject)obj).m_fileStoreProxy);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_fileStoreProxy.hashCode();
    }

}
