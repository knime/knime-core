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
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.container.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.CellClassInfo;
import org.knime.core.data.container.DCObjectOutputVersion2;
import org.knime.core.data.container.KNIMEStreamConstants;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * The abstract writer for writing specialized table formats.
 *
 * @author wiswedel
 * @since 3.6
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractTableStoreWriter implements AutoCloseable, KNIMEStreamConstants {

    /**
     * Map for all DataCells' type, which have been added to this buffer, they will be separately written to to the
     * meta.xml in a zip file.
     */
    private HashMap<CellClassInfo, Byte> m_typeShortCuts;

    private CellClassInfo[] m_shortCutsLookup;

    /** {@link #getFileStoreHandler()}. */
    private IWriteFileStoreHandler m_fileStoreHandler;

    private final boolean m_writeRowKey;

    private final DataTableSpec m_spec;

    /**
     * Constructs an abstract table store writer.
     *
     * @param spec the specification of the KNIME table to write to disk
     * @param writeRowKey a flag that determines whether to store the row keys in the Parquet file
     */
    protected AbstractTableStoreWriter(final DataTableSpec spec, final boolean writeRowKey) {
        m_spec = CheckUtils.checkArgumentNotNull(spec);
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
     * @return <code>true</code> if the implementation should also persist the {@link RowKey} in the row. This will be
     *         false for column-appending tables.
     */
    protected final boolean isWriteRowKey() {
        return m_writeRowKey;
    }

    /** @return the spec set at construction time. */
    protected final DataTableSpec getSpec() {
        return m_spec;
    }

    public abstract void writeRow(final DataRow row) throws IOException;

    /**
     * Writes meta information, such as the classes of serialized {@link DataCell} instances.
     *
     * @param settings The settings (to be read by
     *            {@link AbstractTableStoreReader#readMetaFromFile(org.knime.core.node.NodeSettingsRO, int)})
     */
    public void writeMetaInfoAfterWrite(final NodeSettingsWO settings) {
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
        NodeSettingsWO typeSubSettings = settings.addNodeSettings(TableStoreFormat.CFG_CELL_CLASSES);
        for (int i = 0; i < shortCutsLookup.length; i++) {
            CellClassInfo info = shortCutsLookup[i];
            NodeSettingsWO single = typeSubSettings.addNodeSettings("element_" + i);
            single.addString(TableStoreFormat.CFG_CELL_SINGLE_CLASS, info.getCellClass().getName());
            DataType elementType = info.getCollectionElementType();
            if (elementType != null) {
                NodeSettingsWO subTypeConfig =
                    single.addNodeSettings(TableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE);
                elementType.save(subTypeConfig);
            }
        }
    }

    /**
     * @param type the type for which the shortcut is to be retrieved
     * @return the identifier / shortcut of the passed type
     */
    public Byte getTypeShortCut(final CellClassInfo cellClass) {
        return m_typeShortCuts.get(cellClass);
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
    synchronized public DataCellSerializer<DataCell> getSerializerForDataCell(final CellClassInfo cellClass) throws IOException {
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

    /** {@inheritDoc} */
    @Override
    public abstract void close() throws IOException;

    /**
     * Gets the file store keys of the given cell and invokes flush on the cell
     *
     * @since 3.7
     * @param cell
     * @return null if the given cell is no FileStoreCell, otherwise the cell's FileStoreKeys translated to the local
     *         FileStore handler
     * @throws IOException
     */
    public FileStoreKey[] getFileStoreKeysAndFlush(final DataCell cell) throws IOException {
        FileStoreKey[] fileStoreKeys = null;
        if (cell instanceof FileStoreCell) {
            final FileStoreCell fsCell = (FileStoreCell)cell;
            FileStore[] fileStores = FileStoreUtil.getFileStores(fsCell);
            fileStoreKeys = new FileStoreKey[fileStores.length];

            for (int fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) {
                // TODO is the 'else' case realistic?
                if (getFileStoreHandler() instanceof IWriteFileStoreHandler) {
                    fileStoreKeys[fileStoreIndex] =
                        getFileStoreHandler().translateToLocal(fileStores[fileStoreIndex], fsCell);
                } else {
                    // handler is not an IWriteFileStoreHandler but the buffer still contains file stores:
                    // the flow is part of a workflow and all file stores were already properly handled
                    // (this buffer is restored from disc - and then a memory alert forces the data back onto disc)
                    fileStoreKeys[fileStoreIndex] = FileStoreUtil.getFileStoreKey(fileStores[fileStoreIndex]);
                }
            }

            FileStoreUtil.invokeFlush(fsCell);
        }
        return fileStoreKeys;
    }

    /**
     * Writes a data cell to the outStream.
     *
     * @param cell The cell to write.
     * @param outStream To write to.
     * @throws IOException If stream corruption happens.
     */
    public void writeDataCell(final DataCell cell, final DCObjectOutputVersion2 outStream) throws IOException {
        if (cell == DataType.getMissingCell()) {
            // only write 'missing' byte if that's the singleton missing cell;
            // missing cells with error cause are handled like ordinary cells below (via serializer)
            outStream.writeControlByte(BYTE_TYPE_MISSING);
            return;
        }

        final boolean isBlob = cell instanceof BlobWrapperDataCell;
        final CellClassInfo cellClass =
            isBlob ? ((BlobWrapperDataCell)cell).getBlobClassInfo() : CellClassInfo.get(cell);
        final DataCellSerializer<DataCell> ser = getSerializerForDataCell(cellClass);
        final Byte identifier = getTypeShortCut(cellClass);
        final FileStoreKey[] fileStoreKeys = getFileStoreKeysAndFlush(cell);

        if (ser == null && !isBlob) {
            outStream.writeControlByte(BYTE_TYPE_SERIALIZATION);
        }
        outStream.writeControlByte(identifier);

        if (fileStoreKeys != null) {
            outStream.writeFileStoreKeys(fileStoreKeys);
        }

        if (isBlob) {
            BlobWrapperDataCell bc = (BlobWrapperDataCell)cell;
            outStream.writeBlobAddress(bc.getAddress());
        } else if (ser == null) {
            // serialize using Java serialization
            outStream.writeDataCellPerJavaSerialization(cell);
        } else {
            // serialize using KNIME serialization
            outStream.writeDataCellPerKNIMESerializer(ser, cell);
        }
    }
}