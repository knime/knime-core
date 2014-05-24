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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 17, 2013 by wiswedel
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class FileStoreProxy {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileStorePortObject.class);

    private FileStoreKey m_fileStoreKey;
    private IFileStoreHandler m_fileStoreHandler;

    /** Standard client constructor.
     * @param fileStore Non null file store object to wrap. */
    public FileStoreProxy(final FileStore fileStore) {
        m_fileStoreKey = FileStoreUtil.getFileStoreKey(fileStore);
        m_fileStoreHandler = FileStoreUtil.getFileStoreHandler(fileStore);
    }

    /** Used when read from persisted stream. */
    public FileStoreProxy() {
    }

    /** @return the fileStoreKey */
    public FileStoreKey getFileStoreKey() {
        return m_fileStoreKey;
    }

    /** @return the fileStoreHandler */
    public IFileStoreHandler getFileStoreHandler() {
        return m_fileStoreHandler;
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public void retrieveFileStoreHandlerFrom(final FileStoreKey key,
            final FileStoreHandlerRepository fileStoreHandlerRepository) throws IOException {
        m_fileStoreKey = key;
        UUID id = key.getStoreUUID();
        m_fileStoreHandler = fileStoreHandlerRepository.getHandlerNotNull(id);
    }

    public FileStore getFileStore() {
        if (m_fileStoreHandler == null) {
            throw new IllegalStateException("Can't read file store object, "
                    + "the proxy has not been fully initialized");
        }
        return m_fileStoreHandler.getFileStore(m_fileStoreKey);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_fileStoreKey.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FileStoreProxy)) {
            return false;
        }
        final FileStoreKey oKey = ((FileStoreProxy)obj).m_fileStoreKey;
        return m_fileStoreKey.equals(oKey);

    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_fileStoreKey.hashCode();
    }

    public FileStoreKey translateToLocal(final FlushCallback callback) {
        if (m_fileStoreHandler instanceof IWriteFileStoreHandler) {
            return ((IWriteFileStoreHandler)m_fileStoreHandler).translateToLocal(getFileStore(), callback);
        } else {
            return m_fileStoreKey;
        }
    }

    /** Marker interface implemented by the {@link FileStorePortObject} and {@link FileStoreCell}. Both classes
     * have a (protected) flush method, which gets called by the framework when the content needs to be written
     * to the file.
     *
     * <p>This interface doesn't require this method so that the method can stay protected. (This interface was
     * added after the flush method had been added to the FileStoreCell. Also, it seems wrong to make it public as it
     * should only be called by the framework.)
     */
    public interface FlushCallback {

    }


}
