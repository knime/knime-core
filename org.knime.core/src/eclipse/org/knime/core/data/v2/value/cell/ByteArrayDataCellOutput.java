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

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.container.LongUTFDataOutputStream;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.DataCellSerializerFactory.DataCellSerializerInfo;

/**
 * {@link DataCellDataOutput} implementation on {@link ByteArrayOutputStream}.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
final class ByteArrayDataCellOutput implements DataCellDataOutput, AutoCloseable {

    private final DataCellSerializerFactory m_factory;

    private final IWriteFileStoreHandler m_fsHandler;

    private final ByteArrayOutputStream m_byteStream;

    private final LongUTFDataOutputStream m_delegateStream;

    ByteArrayDataCellOutput(final DataCellSerializerFactory factory, final IWriteFileStoreHandler fsHandler) {
        m_byteStream = new ByteArrayOutputStream(64);
        // TODO Christian
        m_delegateStream = new LongUTFDataOutputStream(new DataOutputStream(m_byteStream));
        m_factory = factory;
        m_fsHandler = fsHandler;
    }

    private void handleFileStoreCell(final FileStoreCell fsCell) throws IOException {
        if (mustBeFlushedPriorSave(fsCell)) {
            final FileStore[] fileStores = FileStoreUtil.getFileStores(fsCell);
            final FileStoreKey[] fileStoreKeys = new FileStoreKey[fileStores.length];
            for (int fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) {
                // TODO code is different from AbstractTableStoreWriter#211. In case of bug, this would be a good place to start looking...
                fileStoreKeys[fileStoreIndex] = m_fsHandler.translateToLocal(fileStores[fileStoreIndex], fsCell);
            }

            FileStoreUtil.invokeFlush(fsCell);

            writeInt(fileStoreKeys.length);
            for (FileStoreKey fsKey : fileStoreKeys) {
                fsKey.save(this);
            }
        }
    }

    private boolean mustBeFlushedPriorSave(final FileStoreCell cell) {
        FileStore[] fileStores = FileStoreUtil.getFileStores(cell);
        for (FileStore fs : fileStores) {
            if (m_fsHandler.mustBeFlushedPriorSave(fs)) {
                return true;
            }
        }
        return false;
    }

    byte[] toByteArray() throws IOException {
        m_delegateStream.flush();
        return m_byteStream.toByteArray();
    }

    @Override
    public void writeDataCell(final DataCell cell) throws IOException {
        final DataCellSerializerInfo info = m_factory.getSerializer(cell);
        final DataCellSerializer<DataCell> serializer = info.getSerializer();
        write(info.getInternalIndex());
        serializer.serialize(cell, this);

        if (cell instanceof FileStoreCell) {
            handleFileStoreCell((FileStoreCell)cell);
        }
    }

    @Override
    public void write(final int b) throws IOException {
        m_byteStream.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        m_byteStream.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        m_byteStream.write(b, off, len);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        m_byteStream.write(v);
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        m_delegateStream.writeBoolean(v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        m_delegateStream.writeShort(v);
    }

    @Override
    public void writeChar(final int v) throws IOException {
        m_delegateStream.writeChar(v);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        m_delegateStream.writeInt(v);
    }

    @Override
    public void writeLong(final long v) throws IOException {
        m_delegateStream.writeLong(v);
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        m_delegateStream.writeFloat(v);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        m_delegateStream.writeDouble(v);
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        m_delegateStream.writeBytes(s);
    }

    @Override
    public void writeChars(final String s) throws IOException {
        m_delegateStream.writeChars(s);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        m_delegateStream.writeUTF(s);
    }

    @Override
    public void close() throws Exception {
        m_delegateStream.close();
        m_byteStream.close();
    }
}
