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

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.container.Buffer;
import org.knime.core.data.container.CellClassInfo;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DefaultTableStoreFormat;
import org.knime.core.data.container.KNIMEStreamConstants;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * The abstract reader for reading specialized table formats.
 *
 * @author wiswedel
 * @since 3.6
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractTableStoreReader implements KNIMEStreamConstants {
    private FileStoreHandlerRepository m_fileStoreHandlerRepository;

    private CellClassInfo[] m_shortCutsLookup;

    /**
     * Constructs an abstract table store reader.
     *
     * @param binFile the local file from which to read
     * @param settings The settings (written by
     *            {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @param version The version as defined in the {@link Buffer} class
     * @throws IOException any type of I/O problem
     * @throws InvalidSettingsException thrown in case something goes wrong during de-serialization, e.g. a new version
     *             of a writer has been used which hasn't been installed on the current system.
     */
    protected AbstractTableStoreReader(final File binFile, final NodeSettingsRO settings, final int version)
        throws IOException, InvalidSettingsException {
        readMetaFromFile(settings, version);
    }

    /**
     * @return the fileStoreHandlerRepository
     */
    public final FileStoreHandlerRepository getFileStoreHandlerRepository() {
        return m_fileStoreHandlerRepository;
    }

    /**
     * @param fileStoreHandlerRepository the fileStoreHandlerRepository to set
     */
    public final void setFileStoreHandlerRepository(final FileStoreHandlerRepository fileStoreHandlerRepository) {
        m_fileStoreHandlerRepository = fileStoreHandlerRepository;
    }

    public abstract TableStoreCloseableRowIterator iterator() throws IOException;

    /**
     * Reads meta information, such as the classes of serialized {@link DataCell} instances.
     *
     * @param settings The settings (written by
     *            {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @param version The version as defined in the {@link Buffer} class
     * @throws IOException Any type of I/O problem.
     * @throws InvalidSettingsException thrown in case something goes wrong during de-serialization, e.g. a new version
     *             of a writer has been used which hasn't been installed on the current system.
     */
    protected void readMetaFromFile(final NodeSettingsRO settings, final int version)
        throws IOException, InvalidSettingsException {
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

            Class<?> cl = DataTypeRegistry.getInstance().getCellClass(cellClassName).orElseThrow(
                () -> new InvalidSettingsException("Data cell class \"" + cellClassName + "\" is unknown."));
            try {
                shortCutsLookup[i] = CellClassInfo.get((Class<? extends DataCell>)cl, null);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException(
                    "Unable to instantiate CellClassInfo for class \"" + cellClasses[i] + "\"", e);
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
                .orElseThrow(() -> new InvalidSettingsException("Can't load data cell class '" + className + "'"));

            DataType elementType = null;
            if (single.containsKey(DefaultTableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE)) {
                NodeSettingsRO subTypeConfig =
                    single.getNodeSettings(DefaultTableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE);
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

    /**
     * Perform lookup for the DataCell class info given the argument byte.
     *
     * @param identifier The byte as read from the stream.
     * @return the associated cell class info
     * @throws IOException If the byte is invalid.
     */
    public CellClassInfo getTypeForChar(final byte identifier) throws IOException {
        int shortCutIndex = (byte)(identifier - BYTE_TYPE_START);
        if (shortCutIndex < 0 || shortCutIndex >= m_shortCutsLookup.length) {
            throw new IOException("Unknown shortcut byte '" + identifier + "'");
        }
        return m_shortCutsLookup[shortCutIndex];
    }

    public static abstract class TableStoreCloseableRowIterator extends CloseableRowIterator {
        private Buffer m_buffer;

        /**
         * @param buffer the buffer to set
         */
        public void setBuffer(final Buffer buffer) {
            m_buffer = buffer;
        }

        /** {@inheritDoc} */
        @Override
        public final void close() {
            m_buffer.clearIteratorInstance(this, true);
        }

        public abstract boolean performClose() throws IOException;
    }

}
