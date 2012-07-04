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
 *   Jun 26, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore;

import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.filestore.internal.FileStoreHandler;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.FileStoreKey;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public abstract class FileStoreCell extends DataCell {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileStoreCell.class);

    private final FileStoreKey m_fileStoreKey;
    private FileStoreHandler m_fileStoreHandler;

    /**
     *  */
    protected FileStoreCell(final FileStore fileStore) {
        m_fileStoreKey = fileStore.getKey();
        m_fileStoreHandler = fileStore.getFileStoreHandler();
    }

    protected FileStoreCell(final DataCellDataInput input) throws IOException {
        m_fileStoreKey = FileStoreKey.load(input);
    }

    protected void save(final DataOutput output) throws IOException {
        m_fileStoreKey.save(output);
    }

    public FileStore acquire() {
        if (m_fileStoreHandler == null) {
            throw new IllegalStateException("Can't read file store object, "
                    + "probably the cell has not been fully initialized");
        }
        // TODO, really acquire
        return m_fileStoreHandler.getFileStore(m_fileStoreKey);
    }

    public void release() {
        // TODO, really release
    }

    /** @noreference This method is not intended to be referenced by clients. */
    public void retrieveFileStoreHandlerFrom(
            final FileStoreHandlerRepository fileStoreHandlerRepository) {
        UUID id = m_fileStoreKey.getStoreUUID();
        m_fileStoreHandler = fileStoreHandlerRepository.getHandler(id);
        try {
            postConstruct();
        } catch (Exception e) {
            LOGGER.error(getClass().getSimpleName() + " must not throw exception in post construct", e);
        }
    }

    /** Called after the cell is deserialized from a stream. Clients
     * can now access the file. */
    protected void postConstruct() {
        // no op.
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_fileStoreKey.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        final FileStoreKey oKey = ((FileStoreCell)dc).m_fileStoreKey;
        return m_fileStoreKey.equals(oKey);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_fileStoreKey.hashCode();
    }

}
