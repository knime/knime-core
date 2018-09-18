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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 15, 2016 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DefaultTableStoreFormat.CompressionFormat;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author wiswedel
 */
final class DefaultTableStoreWriter extends AbstractTableStoreWriter implements KNIMEStreamConstants {

    private final CompressionFormat m_compressionFormat;

    /**
     * the stream that writes to the file, it's a special object output stream, in which we can mark the end of an entry
     * (to figure out when a cell implementation reads too many or too few bytes).
     */
    private final DCObjectOutputVersion2 m_outStream;

    /**
     * Constructs a writer for writing KNIME tables to disk.
     *
     * @param spec the specification of the KNIME table to write to disk
     * @param outputStream
     * @param writeRowKey a flag that determines whether to store the row keys in the Parquet file
     * @throws IOException any type of I/O problem
     */
    public DefaultTableStoreWriter(final DataTableSpec spec, final OutputStream outputStream, final boolean writeRowKey)
        throws IOException {
        super(spec, writeRowKey);
        m_compressionFormat = DefaultTableStoreFormat.IS_USE_GZIP ? CompressionFormat.Gzip : CompressionFormat.None;
        m_outStream = initOutFile(new BufferedOutputStream(outputStream));
    }

    /**
     * Serializes a row to the output stream. This method is called from <code>addRow(DataRow)</code>.
     *
     * @throws IOException If an IO error occurs while writing to the file.
     */
    @Override
    public void writeRow(final DataRow row) throws IOException {
        RowKey id = row.getKey();
        writeRowKey(id, m_outStream);
        for (int i = 0; i < row.getNumCells(); i++) {
            DataCell cell =
                row instanceof BlobSupportDataRow ? ((BlobSupportDataRow)row).getRawCell(i) : row.getCell(i);
            writeDataCell(cell, m_outStream);
            m_outStream.endBlock();
        }
        m_outStream.endRow();
    }

    /**
     * Writes the row key to the out stream. This method is overridden in {@link NoKeyBuffer} in order to skip the row
     * key.
     *
     * @param key The key to write.
     * @param outStream To write to.
     * @throws IOException If that fails.
     */
    void writeRowKey(final RowKey key, final DCObjectOutputVersion2 outStream) throws IOException {
        if (isWriteRowKey()) {
            outStream.writeRowKey(key);
            outStream.endBlock();
        }
    }

    /**
     * Writes a data cell to the outStream.
     *
     * @param cell The cell to write.
     * @param outStream To write to.
     * @throws IOException If stream corruption happens.
     */
    void writeDataCell(final DataCell cell, final DCObjectOutputVersion2 outStream) throws IOException {
        if (cell == DataType.getMissingCell()) {
            // only write 'missing' byte if that's the singleton missing cell;
            // missing cells with error cause are handled like ordinary cells below (via serializer)
            outStream.writeControlByte(BYTE_TYPE_MISSING);
            return;
        }
        boolean isBlob = cell instanceof BlobWrapperDataCell;
        CellClassInfo cellClass = isBlob ? ((BlobWrapperDataCell)cell).getBlobClassInfo() : CellClassInfo.get(cell);
        DataCellSerializer<DataCell> ser = getSerializerForDataCell(cellClass);
        Byte identifier = getTypeShortCut(cellClass);
        FileStoreKey[] fileStoreKeys = super.getFileStoreKeysAndFlush(cell);
        final boolean isJavaSerializationOrBlob = ser == null && !isBlob;
        if (isJavaSerializationOrBlob) {
            outStream.writeControlByte(BYTE_TYPE_SERIALIZATION);
        }
        outStream.writeControlByte(identifier);
        if (fileStoreKeys != null) {
            outStream.writeFileStoreKeys(fileStoreKeys);
        }
        // DataCell is datacell-serializable
        if (!isJavaSerializationOrBlob) {
            if (isBlob) {
                BlobWrapperDataCell bc = (BlobWrapperDataCell)cell;
                outStream.writeBlobAddress(bc.getAddress());
            } else {
                outStream.writeDataCellPerKNIMESerializer(ser, cell);
            }
        } else {
            outStream.writeDataCellPerJavaSerialization(cell);
        }
    }

    /**
     * Creates short cut array and wraps the argument stream in a {@link DCObjectOutputVersion2}.
     */
    private DCObjectOutputVersion2 initOutFile(final OutputStream outStream) throws IOException {
        OutputStream wrap;
        switch (m_compressionFormat) {
            case Gzip:
                wrap = new GZIPOutputStream(outStream);
                // buffering the input stream is important as the blockable
                // stream, which will be put on top of it, reads bytes individually
                // (had a table, on which a single read-scan took ~6min without
                // and ~30s with buffering)
                wrap = new BufferedOutputStream(wrap);
                break;
            case None:
                wrap = outStream;
                break;
            default:
                throw new IOException("Unsupported compression format: " + m_compressionFormat);
        }
        return new DCObjectOutputVersion2(wrap, this);
    }

    /** {@inheritDoc} */
    @Override
    public void writeMetaInfoAfterWrite(final NodeSettingsWO settings) {
        settings.addString(DefaultTableStoreFormat.CFG_COMPRESSION, m_compressionFormat.name());
        super.writeMetaInfoAfterWrite(settings);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        m_outStream.close();
    }

}
