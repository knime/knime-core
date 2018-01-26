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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.data.container.storage.TableStoreFormat;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;

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
     * Static field to enable/disable the usage of a GZipInput/OutpuStream when writing the binary data. This option
     * defaults to {@value DataContainer#DEF_GZIP_COMPRESSION}. */
    static final boolean IS_USE_GZIP;

    static {
        // prefer this block over Boolean.getBoolean(...) to cover typos
        // on command line (warn on console)
        String isUseGzipString = System.getProperty(KNIMEConstants.PROPERTY_TABLE_GZIP_COMPRESSION);
        boolean debugLog = true;
        if (isUseGzipString == null) {
            isUseGzipString = Boolean.toString(DataContainer.DEF_GZIP_COMPRESSION);
            debugLog = false;
        }
        if ("true".equals(isUseGzipString)) {
            IS_USE_GZIP = true;
        } else if ("false".equals(isUseGzipString)) {
            IS_USE_GZIP = false;
        } else {
            debugLog = false;
            LOGGER.warn("Unable to read property " + KNIMEConstants.PROPERTY_TABLE_GZIP_COMPRESSION + " (\""
                    + isUseGzipString + "\"); defaulting to " + DataContainer.DEF_GZIP_COMPRESSION);
            IS_USE_GZIP = DataContainer.DEF_GZIP_COMPRESSION;
        }
        if (debugLog) {
            LOGGER.debug("Setting table stream compression to " + IS_USE_GZIP);
        }
    }

    /** Compression on the binary (main) file. */
    enum CompressionFormat {
            Gzip,
            None;
    }

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public String getFilenameSuffix() {
        return ".bin.gz";
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
        final boolean isReadRowKey)
                throws IOException, InvalidSettingsException {
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
