/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.container.storage;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author wiswedel
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractTableStoreWriter implements AutoCloseable {

    /** {@link #getFileStoreHandler()}. */
    private IWriteFileStoreHandler m_fileStoreHandler;

    private final boolean m_writeRowKey;

    private final DataTableSpec m_spec;


    protected AbstractTableStoreWriter(final DataTableSpec spec, final boolean writeRowKey) {
        m_spec = spec;
        m_writeRowKey = writeRowKey;
    }

    public final void setFileStoreHandler(final IWriteFileStoreHandler writeFileStoreHandler) {
        m_fileStoreHandler = writeFileStoreHandler;
    }

    /** @return the write file store handler set at construction time. */
    public final IWriteFileStoreHandler getFileStoreHandler() {
        return m_fileStoreHandler;
    }

    /**
     * @return <code>true</code> if the implementation should also persist the {@link RowKey} in the row. This will
     * be false for column-appending tables.
     */
    protected final boolean isWriteRowKey() {
        return m_writeRowKey;
    }

    /** @return the spec set at construction time. */
    protected final DataTableSpec getSpec() {
        return m_spec;
    }

    public abstract void writeRow(final DataRow row) throws IOException;

    public abstract void writeMetaInfoAfterWrite(final NodeSettingsWO settings);

    /** {@inheritDoc} */
    @Override
    public abstract void close() throws IOException;

    /**
     * @param cell
     * @return
     * @throws IOException
     */
    public FileStoreKey getFileStoreKeyAndFlush(final DataCell cell) throws IOException {
        FileStoreKey fileStoreKey = null;
        if (cell instanceof FileStoreCell) {
            final FileStoreCell fsCell = (FileStoreCell)cell;
            FileStore fileStore = FileStoreUtil.getFileStore(fsCell);
            // TODO is the 'else' case realistic?
            if (getFileStoreHandler() instanceof IWriteFileStoreHandler) {
                fileStoreKey = getFileStoreHandler().translateToLocal(fileStore, fsCell);
            } else {
                // handler is not an IWriteFileStoreHandler but the buffer still contains file stores:
                // the flow is part of a workflow and all file stores were already properly handled
                // (this buffer is restored from disc - and then a memory alert forces the data back onto disc)
                fileStoreKey = FileStoreUtil.getFileStoreKey(fileStore);
            }
            FileStoreUtil.invokeFlush(fsCell);
        }
        return fileStoreKey;
    }
}
