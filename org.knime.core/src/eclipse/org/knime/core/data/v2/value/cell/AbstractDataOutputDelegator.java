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
 */
package org.knime.core.data.v2.value.cell;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.container.LongUTFDataOutput;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;

/**
 * {@link DataCellDataOutput} implementation on {@link ByteArrayOutputStream}.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractDataOutputDelegator extends OutputStream implements DataCellDataOutput {

    private final IWriteFileStoreHandler m_fsHandler;

    private final DataOutput m_delegate;

    protected AbstractDataOutputDelegator(final IWriteFileStoreHandler fsHandler, final DataOutput output) {
        m_delegate = new LongUTFDataOutput(output);
        m_fsHandler = fsHandler;
    }

    protected abstract void writeDataCellImpl(final DataCell cell) throws IOException;

    @Override
    public final void writeDataCell(final DataCell cell) throws IOException {
        writeDataCellImpl(cell);

        if (cell instanceof FileStoreCell fsCell) {
            handleFileStoreCell(fsCell);
        }
    }

    private void handleFileStoreCell(final FileStoreCell fsCell) throws IOException {
        final FileStore[] fileStores = FileStoreUtil.getFileStores(fsCell);
        final var fileStoreKeys = new FileStoreKey[fileStores.length];

        // TODO In case we already did that in DataCellValueFactory WriteValue,
        //      can we avoid doing this again? Is it harmful?
        for (int fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) { // NOSONAR
            fileStoreKeys[fileStoreIndex] = m_fsHandler.translateToLocal(fileStores[fileStoreIndex], fsCell);
        }

        // In case we didn't flush yet.
        FileStoreUtil.invokeFlush(fsCell);

        writeInt(fileStoreKeys.length);
        for (final FileStoreKey fsKey : fileStoreKeys) {
            fsKey.save(this);
        }
    }

    @Override
    public final void write(final int b) throws IOException {
        m_delegate.write(b);
    }

    @Override
    public final void write(final byte[] b) throws IOException {
        m_delegate.write(b);
    }

    @Override
    public final void write(final byte[] b, final int off, final int len) throws IOException {
        m_delegate.write(b, off, len);
    }

    @Override
    public final void writeByte(final int v) throws IOException {
        m_delegate.write(v);
    }

    @Override
    public final void writeBoolean(final boolean v) throws IOException {
        m_delegate.writeBoolean(v);
    }

    @Override
    public final void writeShort(final int v) throws IOException {
        m_delegate.writeShort(v);
    }

    @Override
    public final void writeChar(final int v) throws IOException {
        m_delegate.writeChar(v);
    }

    @Override
    public final void writeInt(final int v) throws IOException {
        m_delegate.writeInt(v);
    }

    @Override
    public final void writeLong(final long v) throws IOException {
        m_delegate.writeLong(v);
    }

    @Override
    public final void writeFloat(final float v) throws IOException {
        m_delegate.writeFloat(v);
    }

    @Override
    public final void writeDouble(final double v) throws IOException {
        m_delegate.writeDouble(v);
    }

    @Override
    public final void writeBytes(final String s) throws IOException {
        m_delegate.writeBytes(s);
    }

    @Override
    public void writeChars(final String s) throws IOException {
        m_delegate.writeChars(s);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        m_delegate.writeUTF(s);
    }
}
