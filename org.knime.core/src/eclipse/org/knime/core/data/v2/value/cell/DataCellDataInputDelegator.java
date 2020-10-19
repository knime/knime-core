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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.v2.DataCellSerializerFactory;

/**
 * {@link DataCellDataInput} implementation on {@link ByteArrayInputStream}.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
final class DataCellDataInputDelegator implements DataCellDataInput {

    private final DataCellSerializerFactory m_factory;

    private final IDataRepository m_dataRepository;

    private final DataInput m_delegate;

    DataCellDataInputDelegator(final DataCellSerializerFactory factory, //
        final IDataRepository dataRepository, //
        final DataInput input) {

        m_factory = factory;
        m_dataRepository = dataRepository;
        m_delegate = input;
    }

    @Override
    public DataCell readDataCell() throws IOException {
        final DataCell result = m_factory.getSerializerByIdx(readByte()).getSerializer().deserialize(this);
        if (FileStoreCell.class.isAssignableFrom(result.getClass())) {
            final FileStoreKey[] fileStoreKeys = readFileStoreKeys();
            final FileStoreCell fsCell = (FileStoreCell)result;
            FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_dataRepository);
        }

        return result;
    }

    @Override
    public void readFully(final byte[] b) throws IOException {
        m_delegate.readFully(b);
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        m_delegate.readFully(b, off, len);
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        return m_delegate.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return m_delegate.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return m_delegate.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return m_delegate.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return m_delegate.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return m_delegate.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return m_delegate.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return m_delegate.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return m_delegate.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return m_delegate.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return m_delegate.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return m_delegate.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return m_delegate.readUTF();
    }

    private FileStoreKey[] readFileStoreKeys() throws IOException {
        int numFileStoreKeys = readInt();
        FileStoreKey[] fileStoreKeys = new FileStoreKey[numFileStoreKeys];
        for (int fsIdx = 0; fsIdx < numFileStoreKeys; fsIdx++) {
            fileStoreKeys[fsIdx] = FileStoreKey.load(this);
        }
        return fileStoreKeys;
    }

}
