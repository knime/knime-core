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
 *   Mar 17, 2016 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DefaultTableStoreFormat.CompressionFormat;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 *
 * @author wiswedel
 */
final class DefaultTableStoreReader extends AbstractTableStoreReader {

    private CompressionFormat m_compressionFormat;

    private final File m_binFile;

    private final DataTableSpec m_spec;

    private final boolean m_isReadRowKey;

    /**
     * Constructs a reader for materializing serialized KNIME tables.
     *
     * @param binFile the local file from which to read
     * @param spec the specification of the data table
     * @param settings The settings (written by
     *            {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @param version The version as defined in the {@link Buffer} class
     * @param isReadRowKey whether or not row keys are to be read (or generated anew)
     * @throws IOException any type of I/O problem
     * @throws InvalidSettingsException thrown in case something goes wrong during de-serialization, e.g. a new version
     *             of a writer has been used which hasn't been installed on the current system.
     */
    DefaultTableStoreReader(final File binFile, final DataTableSpec spec, final NodeSettingsRO settings,
        final int version, final boolean isReadRowKey) throws IOException, InvalidSettingsException {
        super(binFile, spec, settings, version);
        if (version <= 6) {
            readCellClassInfoArrayFromMetaVersion1x(settings);
        } else {
            readCellClassInfoArrayFromMetaVersion2(settings);
        }

        m_binFile = binFile;
        m_spec = spec;
        m_isReadRowKey = isReadRowKey;

        final CompressionFormat cF;
        if (version < 3) { // stream was not zipped in KNIME 1.1.x
            cF = CompressionFormat.NONE;
        } else if (version >= 8) { // added sometime between format 8 and 9 - no increment of version number
            cF = CompressionFormat.loadSettings(settings);
        } else {
            // use gzip compression
            cF = CompressionFormat.GZIP;
        }
        m_compressionFormat = cF;
    }

    @Override
    public TableStoreCloseableRowIterator iterator() {
        try {
            if (getReadVersion() <= 5) { // 2.0 tech preview and before
                return new BufferFromFileIteratorVersion1x(this);
            } else {
                return new BufferFromFileIteratorVersion20(this);
            }
        } catch (IOException ioe) {
            StringBuilder b = new StringBuilder("Cannot read file \"");
            b.append(m_binFile != null ? m_binFile.getName() : "<unknown>");
            b.append("\"");
            checkAndReportOpenFiles(ioe);
            throw new RuntimeException(b.toString(), ioe);
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
     * @return number of records
     * @see org.knime.core.data.container.Buffer#size()
     */
    long size() {
        return getBuffer().size();
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

        /**
         * Opens the (decompressed) input stream.
         *
         * @param tableFormatReader the table format reader
         * @return the (decompressed) input stream
         * @throws IOException - If the file could not be opened or the an error occurred creating the (decompressed)
         *             stream
         */
        static final InputStream getInputStream(final DefaultTableStoreReader tableFormatReader)
            throws IOException {
            // get the decompression format
            final CompressionFormat cType = tableFormatReader.getBinFileCompressionFormat();
            // return the (decompressed) stream
            return cType.getInputStream(tableFormatReader.getBinFile());
        }

    }

}
