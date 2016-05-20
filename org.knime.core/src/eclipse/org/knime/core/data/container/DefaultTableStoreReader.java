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
 *   Mar 17, 2016 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.container.Buffer.CompressionFormat;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;

/**
 *
 * @author wiswedel
 */
final class DefaultTableStoreReader extends AbstractTableStoreReader implements KNIMEStreamConstants {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultTableStoreReader.class);
    private CompressionFormat m_compressionFormat;
    private CellClassInfo[] m_shortCutsLookup;
    private final File m_binFile;
    private final DataTableSpec m_spec;
    private int m_version;
    private final boolean m_isReadRowKey;
    private Buffer m_buffer;

    /**
     * @param binFile
     * @param settings
     * @throws IOException
     * @throws InvalidSettingsException
     */
    DefaultTableStoreReader(final File binFile, final DataTableSpec spec, final NodeSettingsRO settings,
        final int bufferID, final Map<Integer, ContainerTable> tblRep,
        final int version, final boolean isReadRowKey)
                throws IOException, InvalidSettingsException {
        m_binFile = binFile;
        m_spec = spec;
        m_version = version;
        m_isReadRowKey = isReadRowKey;
        readMetaFromFile(settings, binFile, version);
    }

    void setBufferAfterConstruction(final Buffer buffer) {
        m_buffer = buffer;
    }

    @Override
    public TableStoreCloseableRowIterator iterator() throws IOException {
        if (m_version <= 5) { // 2.0 tech preview and before
            return new BufferFromFileIteratorVersion1x(this);
        } else {
            return new BufferFromFileIteratorVersion20(this);
        }
    }

    /** @return Whether stream is zipped. */
    CompressionFormat getBinFileCompressionFormat() {
        return m_compressionFormat;
    }

    /** @return Underlying binary file. */
    final File getBinFile() {
        return m_binFile;
    }

    /** @return The spec the buffer uses. */
    DataTableSpec getTableSpec() {
        return m_spec;
    }

    boolean isReadRowKey() {
        return m_isReadRowKey;
    }

    /**
     * Get underlying stream version. Important for file iterators.
     *
     * @return Underlying stream version.
     */
    int getReadVersion() {
        return m_version;
    }

    /** @return the buffer */
    Buffer getBuffer() {
        return m_buffer;
    }

    /**
     * @return numbe of records
     * @see org.knime.core.data.container.Buffer#size()
     */
    long size() {
        return m_buffer.size();
    }

    /**
     * Perform lookup for the DataCell class info given the argument byte.
     *
     * @param identifier The byte as read from the stream.
     * @return the associated cell class info
     * @throws IOException If the byte is invalid.
     */
    CellClassInfo getTypeForChar(final byte identifier) throws IOException {
        int shortCutIndex = (byte)(identifier - BYTE_TYPE_START);
        if (shortCutIndex < 0 || shortCutIndex >= m_shortCutsLookup.length) {
            throw new IOException("Unknown shortcut byte '" + identifier + "'");
        }
        return m_shortCutsLookup[shortCutIndex];
    }
    /**
     * Reads meta information, that is row count, version, byte assignments.
     *
     * @param metaIn To read from.
     * @throws IOException If reading fails.
     * @throws ClassNotFoundException If any of the classes can't be loaded.
     * @throws InvalidSettingsException If the internal structure is broken.
     */
    private void readMetaFromFile(final NodeSettingsRO settings, final File fileStoreDir, final int version) throws IOException, InvalidSettingsException {
        final CompressionFormat cF;
        if (version < 3) { // stream was not zipped in KNIME 1.1.x
            cF = CompressionFormat.None;
        } else if (version >= 8) { // added sometime between format 8 and 9 - no increment of version number
            String compFormat = settings.getString(DefaultTableStoreFormat.CFG_COMPRESSION,
                CompressionFormat.Gzip.name());
            try {
                cF = CompressionFormat.valueOf(compFormat);
            } catch (Exception e) {
                throw new InvalidSettingsException(String.format("Unable to parse \"%s\" property (\"%s\"): %s",
                    DefaultTableStoreFormat.CFG_COMPRESSION, compFormat, e.getMessage()), e);
            }
        } else {
            cF = CompressionFormat.Gzip;
        }
        m_compressionFormat = cF;
        if (version <= 6) {
            m_shortCutsLookup = readCellClassInfoArrayFromMetaVersion1x(settings);
        } else {
            m_shortCutsLookup = readCellClassInfoArrayFromMetaVersion2(settings);
        }
    }

    @SuppressWarnings("unchecked")
    private static CellClassInfo[] readCellClassInfoArrayFromMetaVersion1x(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String[] cellClasses = settings.getStringArray(DefaultTableStoreFormat.CFG_CELL_CLASSES);
        CellClassInfo[] shortCutsLookup = new CellClassInfo[cellClasses.length];


        for (int i = 0; i < cellClasses.length; i++) {
            String cellClassName = cellClasses[i];

            Class<?> cl = DataTypeRegistry.getInstance().getCellClass(cellClassName)
                .orElseThrow(() -> new InvalidSettingsException("Data cell class \"" + cellClassName + "\" is unknown."));
            try {
                shortCutsLookup[i] = CellClassInfo.get((Class<? extends DataCell>)cl, null);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException("Unable to instantiate CellClassInfo for class \"" + cellClasses[i]
                        + "\"", e);
            }
        }
        return shortCutsLookup;
    }

    @SuppressWarnings("unchecked")
    private static CellClassInfo[] readCellClassInfoArrayFromMetaVersion2(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        NodeSettingsRO typeSubSettings = settings.getNodeSettings(DefaultTableStoreFormat.CFG_CELL_CLASSES);
        Set<String> keys = typeSubSettings.keySet();
        CellClassInfo[] shortCutsLookup = new CellClassInfo[keys.size()];
        int i = 0;
        for (String s : keys) {
            NodeSettingsRO single = typeSubSettings.getNodeSettings(s);
            String className = single.getString(DefaultTableStoreFormat.CFG_CELL_SINGLE_CLASS);

            Class<?> cl = DataTypeRegistry.getInstance().getCellClass(className)
                .orElseThrow(() -> new InvalidSettingsException("Can't load data cell class '" +className + "'"));

            DataType elementType = null;
            if (single.containsKey(DefaultTableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE)) {
                NodeSettingsRO subTypeConfig = single.getNodeSettings(DefaultTableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE);
                elementType = DataType.load(subTypeConfig);
            }
            try {
                shortCutsLookup[i] = CellClassInfo.get((Class<? extends DataCell>)cl, elementType);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Unable to instantiate CellClassInfo for class \"" + className
                        + "\", element type: " + elementType);
            }
            i++;
        }
        return shortCutsLookup;
    }


    /** Super class of all file iterators. */
    abstract static class FromFileIterator extends TableStoreCloseableRowIterator implements KNIMEStreamConstants {

        /**
         * Called when the stream closing should take place.
         *
         * @return Whether the stream close was actually performed (that is false when already closed.)
         * @throws IOException If closing fails.
         */
        @Override
        public abstract boolean performClose() throws IOException;

        /** {@inheritDoc} */
        @Override
        public abstract BlobSupportDataRow next();
    }

}
