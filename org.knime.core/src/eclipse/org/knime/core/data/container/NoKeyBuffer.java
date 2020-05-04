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
 *   Jul 12, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.node.NodeLogger;


/**
 * Buffer implementation that does not write the row keys. Used to write
 * data if only few columns have changed. This buffer writes the changed
 * columns.
 * <p>This class is used to save the data of the new columns in a
 * {@link RearrangeColumnsTable}.
 * @author Bernd Wiswedel, University of Konstanz
 */
class NoKeyBuffer extends Buffer {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(NoKeyBuffer.class);

    /** Current version string. */
    private static final String VERSION = "noRowKeyContainer_12";

    /** The version number corresponding to VERSION. */
    private static final int IVERSION = 12;

    private static final HashMap<String, Integer> COMPATIBILITY_MAP;

    static {
        // see Buffer static block for details
        COMPATIBILITY_MAP = new HashMap<String, Integer>();
        COMPATIBILITY_MAP.put("noRowKeyContainer_1.0.0", 1);
        COMPATIBILITY_MAP.put("noRowKeyContainer_1.1.0", 2);
        COMPATIBILITY_MAP.put("noRowKeyContainer_1.2.0", 3);
        COMPATIBILITY_MAP.put("noRowKeyContainer_4", 4);
        COMPATIBILITY_MAP.put("noRowKeyContainer_5", 5);
        COMPATIBILITY_MAP.put("noRowKeyContainer_6", 6);
        COMPATIBILITY_MAP.put("noRowKeyContainer_7", 7);
        COMPATIBILITY_MAP.put("noRowKeyContainer_8", 8);
        COMPATIBILITY_MAP.put("noRowKeyContainer_9", 9);
        COMPATIBILITY_MAP.put("noRowKeyContainer_10", 10);
        COMPATIBILITY_MAP.put("noRowKeyContainer_11", 11);
        COMPATIBILITY_MAP.put(VERSION, IVERSION);
    }

    /** Creates new buffer for writing.
     * @param spec Passed on to super.
     * @param maxRowsInMemory Passed on to super.
     * @param bufferID Passed on to super.
     * @param tblRep Passed on to super.
     * @param dataRepository Passed to super class.
     * @param localTblRep Passed on to super.
     * @param fileStoreHandler passed on to super.
     * @param forceSynchronousWrite passed on to super.
     */
    NoKeyBuffer(final DataTableSpec spec, final int maxRowsInMemory, final int bufferID,
        final IDataRepository dataRepository, final Map<Integer, ContainerTable> localTblRep,
        final IWriteFileStoreHandler fileStoreHandler) {
        super(spec, maxRowsInMemory, bufferID, dataRepository, localTblRep, fileStoreHandler);
    }

    /** Creates new buffer for reading.
     * @param binFile Passed on to super.
     * @param blobDir Passed on to super.
     * @param spec Passed on to super.
     * @param metaIn Passed on to super.
     * @param bufferID Passed on to super.
     * @param dataRepository Passed to super class.
     * @throws IOException Passed on from super.
     */
    NoKeyBuffer(final File binFile, final File blobDir, final DataTableSpec spec, final InputStream metaIn,
        final int bufferID, final IDataRepository dataRepository)
        throws IOException {
        super(binFile, blobDir, /*can't have fs dir in workflow*/null, spec, metaIn, bufferID, dataRepository);
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /** {@inheritDoc} */
    @Override
    int validateVersion(final String version) {
        Integer iVersion = COMPATIBILITY_MAP.get(version);
        if (iVersion == null) {
            LOGGER.warn("Unknown version string in persisted table file (\"" + version
                + "\") - was table created with a future version of KNIME? Using \"" + VERSION + "\" modus.");
            iVersion = IVERSION;
        }
        if (iVersion < IVERSION) {
            LOGGER.debug("Table has been written with a previous version of KNIME (\""
                    + version + "\", using compatibility mode.");
        }
        return iVersion;
    }

    /** {@inheritDoc} */
    @Override
    boolean shouldSkipRowKey() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    Buffer createLocalCloneForWriting() {
        return new NoKeyBuffer(getTableSpec(), 0, getBufferID(), getDataRepository(), Collections.emptyMap(),
            castAndGetFileStoreHandler());
    }
}
