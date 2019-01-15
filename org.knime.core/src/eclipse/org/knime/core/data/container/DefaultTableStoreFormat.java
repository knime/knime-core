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
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.data.container.storage.TableStoreFormat;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

/**
 *
 * @author wiswedel
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DefaultTableStoreFormat implements TableStoreFormat {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultTableStoreFormat.class);

    /** Compression format. */
    static final String CFG_COMPRESSION = "container.compression";

    /**
     * Checked function interface throwing an IOException.
     *
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    @FunctionalInterface
    private static interface CheckedIOFunction<T, R> {

        /**
         * Applies an I/O function to the given argument.
         *
         * @param t the I/O function argument
         * @return the function result
         * @throws IOException - If the I/O function fails
         *
         */
        R apply(T t) throws IOException;
    }

    /**
     * Various compression formats for KNIME datatables.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    static enum CompressionFormat {

            /** No compression. */
            NONE(".bin", //
                i -> new BufferedInputStream(i), //
                o -> o),

            /** GZip compression. */
            GZIP(".bin.gz", //
                i -> new BufferedInputStream(new GZIPInputStream(i)), //
                o -> new BufferedOutputStream(new GZIPOutputStream(o))),

            /** LZ4 compression. */
            LZ4(".bin.lz4", //
                i -> new BufferedInputStream(new LZ4BlockInputStream(i)), //
                o -> new BufferedOutputStream(new LZ4BlockOutputStream(o))),

            /** Snappy compression. */
            SNAPPY(".bin.snappy", //
                i -> new BufferedInputStream(new SnappyInputStream(i)), //
                o -> new BufferedOutputStream(new SnappyOutputStream(o)));

        /** The file name extension. */
        private final String m_fileNameExtension;

        /** The input stream create function. */
        private final CheckedIOFunction<InputStream, InputStream> m_inFunc;

        /** The output stream create function. */
        private final CheckedIOFunction<OutputStream, OutputStream> m_outFunc;

        /**
         * Constructor.
         *
         * @param fileNameExtension the file name extension
         */
        private CompressionFormat(final String fileNameExtension,
            final CheckedIOFunction<InputStream, InputStream> inFunc,
            final CheckedIOFunction<OutputStream, OutputStream> outFunc) {
            m_fileNameExtension = fileNameExtension;
            m_inFunc = inFunc;
            m_outFunc = outFunc;
        }

        /**
         * Returns the file name extension.
         *
         * @return the file name extension
         */
        String getFileExtension() {
            return m_fileNameExtension;
        }

        /**
         * Returns the compressed output stream.
         *
         * @param out the output stream
         * @return the compressed output stream
         * @throws IOException - If GZip compression fails
         */
        OutputStream getOutputStream(final OutputStream out) throws IOException {
            try {
                return m_outFunc.apply(out);
            } catch (final IOException e) {
                out.close();
                throw e;
            }
        }

        /**
         * Returns the uncompressed input stream.
         *
         * @param file the file to be written to
         * @return the compressed input stream
         * @throws IOException - If the input file does not exist or GZip compression fails
         */
        @SuppressWarnings("resource")
        public InputStream getInputStream(final File file) throws IOException {
            final FileInputStream fis = new FileInputStream(file);
            try {
                return m_inFunc.apply(fis);
            } catch (final IOException e) {
                fis.close();
                throw e;
            }
        }

        /**
         * Returns the {@link CompressionFormat} constant associated with the specified name. Case-sensitivity is
         * ignored to match an identifier used to declare an enum constant of this type.
         *
         * @param arg0 the enum constant name
         * @return the associated enum constant
         */
        static CompressionFormat getCompressionFormat(final String arg0) {
            final String upperCase = arg0.toUpperCase();
            // backwards compatibility
            if (upperCase.equals("TRUE")) {
                return CompressionFormat.GZIP;
            } else if (upperCase.equals("FALSE")) {
                return CompressionFormat.NONE;
            }
            return valueOf(upperCase);
        }
    }

    /** The compression type. */
    static final CompressionFormat COMPRESSION;

    // Initialize the compression according to the user settings.
    static {
        final String compName = System.getProperty(KNIMEConstants.PROPERTY_TABLE_GZIP_COMPRESSION);
        if (compName == null) {
            COMPRESSION = DataContainer.DEF_COMPRESSION;
        } else {
            CompressionFormat compType = DataContainer.DEF_COMPRESSION;
            try {
                compType = CompressionFormat.getCompressionFormat(compName);
                LOGGER.debug("Setting table stream compression to " + compType);
            } catch (final IllegalArgumentException iae) {
                LOGGER.warn("Unable to read property " + KNIMEConstants.PROPERTY_TABLE_GZIP_COMPRESSION + " (\""
                    + compName + "\"); defaulting to " + DataContainer.DEF_COMPRESSION);
            }
            COMPRESSION = compType;
        }
    }

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public String getFilenameSuffix() {
        return COMPRESSION.getFileExtension();
    }

    /** {@inheritDoc} */
    @Override
    public boolean accepts(final DataTableSpec spec) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractTableStoreWriter createWriter(final File binFile, final DataTableSpec spec,
        final boolean writeRowKey) throws IOException {
        return createWriter(new FileOutputStream(binFile), spec, writeRowKey);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractTableStoreWriter createWriter(final OutputStream output, final DataTableSpec spec,
        final boolean writeRowKey) throws IOException {
        return new DefaultTableStoreWriter(spec, output, writeRowKey);
    }

    @Override
    public AbstractTableStoreReader createReader(final File binFile, final DataTableSpec spec,
        final IDataRepository dataRepository, final NodeSettingsRO settings, final int version,
        final boolean isReadRowKey) throws IOException, InvalidSettingsException {
        return new DefaultTableStoreReader(binFile, spec, settings, version, isReadRowKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return Buffer.VERSION; // we write it but don't read it
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateVersion(final String versionString) {
        return true; // this method is really only called for 3rd party types. Actual validation happens in class Buffer

    }

}
