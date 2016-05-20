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
 *   Mar 15, 2016 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.DefaultTableStoreFormat.CompressionFormat;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.util.FileUtil;

/**
 *
 * @author wiswedel
 */
final class DefaultTableStoreWriter extends AbstractTableStoreWriter implements KNIMEStreamConstants {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultTableStoreWriter.class);

    /**
     * Map for all DataCells' type, which have been added to this buffer, they will be separately written to to the
     * meta.xml in a zip file.
     */
    private HashMap<CellClassInfo, Byte> m_typeShortCuts;

    private int[] m_indicesOfBlobInColumns;

    private final CompressionFormat m_compressionFormat;

    /**
     * the stream that writes to the file, it's a special object output stream, in which we can mark the end of an entry
     * (to figure out when a cell implementation reads too many or too few bytes).
     */
    private final DCObjectOutputVersion2 m_outStream;


    /**
     * @param spec TODO
     * @param writeRowKey
     * @throws IOException
     * @throws FileNotFoundException
     */
    public DefaultTableStoreWriter(final DataTableSpec spec, final OutputStream outputStream,
        final boolean writeRowKey) throws IOException {
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
            DataCell cell = row instanceof BlobSupportDataRow ? ((BlobSupportDataRow)row).getRawCell(i) : row.getCell(i);
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
        Byte identifier = m_typeShortCuts.get(cellClass);
        FileStoreKey fileStoreKey = super.getFileStoreKeyAndFlush(cell);
        final boolean isJavaSerializationOrBlob = ser == null && !isBlob;
        if (isJavaSerializationOrBlob) {
            outStream.writeControlByte(BYTE_TYPE_SERIALIZATION);
        }
        outStream.writeControlByte(identifier);
        if (fileStoreKey != null) {
            outStream.writeFileStoreKey(fileStoreKey);
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

    public void writeBlobDataCell(final BlobDataCell cell,
        final BlobAddress a, final Buffer buffer) throws IOException {
        DataCellSerializer<DataCell> ser = getSerializerForDataCell(CellClassInfo.get(cell));
        // addRow will make sure that m_indicesOfBlobInColumns is initialized
        // when this method is called. If this method is called from a different
        // buffer object, it means that this buffer has been closed!
        // (When can this happen? This buffer resides in memory, a successor
        // node is written to disc; they have different memory policies.)
        if (m_indicesOfBlobInColumns == null) {
            m_indicesOfBlobInColumns = new int[getSpec().getNumColumns()];
        }
        int column = a.getColumn();
        int indexInColumn = m_indicesOfBlobInColumns[column]++;
        a.setIndexOfBlobInColumn(indexInColumn);
        boolean isToCompress = Buffer.isUseCompressionForBlobs(CellClassInfo.get(cell));
        File outFile = buffer.getBlobFile(indexInColumn, column, true, isToCompress);
        BlobAddress originalBA = cell.getBlobAddress();
        if (!ConvenienceMethods.areEqual(originalBA, a)) {
            int originalBufferIndex = originalBA.getBufferID();
            Buffer originalBuffer = null;
            ContainerTable t = buffer.getGlobalRepository().get(originalBufferIndex);
            if (t != null) {
                originalBuffer = t.getBuffer();
            } else if (buffer.getLocalRepository() != null) {
                t = buffer.getLocalRepository().get(originalBufferIndex);
                if (t != null) {
                    originalBuffer = t.getBuffer();
                }
            }
            if (originalBuffer != null) {
                int index = originalBA.getIndexOfBlobInColumn();
                int col = originalBA.getColumn();
                boolean compress = originalBA.isUseCompression();
                File source = originalBuffer.getBlobFile(index, col, false, compress);
                FileUtil.copy(source, outFile);
                return;
            }
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
        Buffer.onFileCreated(outFile);
        if (isToCompress) {
            out = new GZIPOutputStream(out);
            // buffering the gzip stream brings another performance boost
            // (in one case from 5mins down to 2 mins)
            out = new BufferedOutputStream(out);
        }
        try (DCObjectOutputVersion2 outStream = new DCObjectOutputVersion2(out, this)) {
            if (ser != null) { // DataCell is datacell-serializable
                outStream.writeDataCellPerKNIMESerializer(ser, cell);
            } else {
                outStream.writeDataCellPerJavaSerialization(cell);
            }
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

    /**
     * Get the serializer object to be used for writing the argument cell or <code>null</code> if it needs to be
     * java-serialized.
     *
     * @param cellClass The cell's class to write out.
     * @return The serializer to use or <code>null</code>.
     * @throws IOException If there are too many different cell implementations (currently 253 are theoretically
     *             supported)
     */
    private DataCellSerializer<DataCell> getSerializerForDataCell(final CellClassInfo cellClass) throws IOException {
        if (m_typeShortCuts == null) {
            m_typeShortCuts = new HashMap<CellClassInfo, Byte>();
        }
        @SuppressWarnings("unchecked")
        DataCellSerializer<DataCell> serializer = (DataCellSerializer<DataCell>)cellClass.getSerializer();
        if (!m_typeShortCuts.containsKey(cellClass)) {
            int size = m_typeShortCuts.size();
            if (size + BYTE_TYPE_START > Byte.MAX_VALUE) {
                throw new IOException("Too many different cell implementations");
            }
            Byte identifier = (byte)(size + BYTE_TYPE_START);
            m_typeShortCuts.put(cellClass, identifier);
        }
        return serializer;
    }

    CellClassInfo[] m_shortCutsLookup;

    /** {@inheritDoc} */
    @Override
    public void writeMetaInfoAfterWrite(final NodeSettingsWO settings) {
        settings.addString(DefaultTableStoreFormat.CFG_COMPRESSION, m_compressionFormat.name());
        // unreported bug fix: NPE when the table only contains missing values.
        if (m_typeShortCuts == null) {
            m_typeShortCuts = new HashMap<CellClassInfo, Byte>();
        }
        CellClassInfo[] shortCutsLookup = new CellClassInfo[m_typeShortCuts.size()];
        for (Map.Entry<CellClassInfo, Byte> e : m_typeShortCuts.entrySet()) {
            byte shortCut = e.getValue();
            CellClassInfo type = e.getKey();
            shortCutsLookup[shortCut - BYTE_TYPE_START] = type;
        }
        m_shortCutsLookup = shortCutsLookup;
        NodeSettingsWO typeSubSettings = settings.addNodeSettings(DefaultTableStoreFormat.CFG_CELL_CLASSES);
        for (int i = 0; i < shortCutsLookup.length; i++) {
            CellClassInfo info = shortCutsLookup[i];
            NodeSettingsWO single = typeSubSettings.addNodeSettings("element_" + i);
            single.addString(DefaultTableStoreFormat.CFG_CELL_SINGLE_CLASS, info.getCellClass().getName());
            DataType elementType = info.getCollectionElementType();
            if (elementType != null) {
                NodeSettingsWO subTypeConfig = single.addNodeSettings(DefaultTableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE);
                elementType.save(subTypeConfig);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        m_outStream.close();
    }

}
