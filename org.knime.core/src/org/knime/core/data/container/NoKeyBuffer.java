/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Jul 12, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
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
    
    private static final RowKey DUMMY_KEY = new RowKey("non-existing");
    
    /** Current version string. */
    private static final String VERSION = "noRowKeyContainer_5";
    
    /** The version number corresponding to VERSION. */
    private static final int IVERSION = 5;
    
    private static final HashMap<String, Integer> COMPATIBILITY_MAP;
    
    static {
        // see Buffer static block for details
        COMPATIBILITY_MAP = new HashMap<String, Integer>();
        COMPATIBILITY_MAP.put("noRowKeyContainer_1.0.0", 1);
        COMPATIBILITY_MAP.put("noRowKeyContainer_1.1.0", 2);
        COMPATIBILITY_MAP.put("noRowKeyContainer_1.2.0", 3);
        COMPATIBILITY_MAP.put("noRowKeyContainer_4", 4);
        COMPATIBILITY_MAP.put(VERSION, IVERSION);
    }
    /**
     * For writing.
     * @see Buffer#Buffer(int, int, java.util.Map, java.util.Map)
     */
    NoKeyBuffer(final int maxRowsInMemory, 
            final int bufferID, final Map<Integer, ContainerTable> tblRep,
            final Map<Integer, ContainerTable> localTblRep) {
        super(maxRowsInMemory, bufferID, tblRep, localTblRep);
    }
    
    /**
     * @see Buffer#Buffer(File, File, DataTableSpec, InputStream, int, Map)
     */
    NoKeyBuffer(final File binFile, final File blobDir, 
            final DataTableSpec spec, final InputStream metaIn, 
            final int bufferID, final Map<Integer, ContainerTable> tblRep) 
            throws IOException {
        super(binFile, blobDir, spec, metaIn, bufferID, tblRep);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int validateVersion(final String version) throws IOException {
        Integer iVersion = COMPATIBILITY_MAP.get(version);
        if (iVersion == null) {
            throw new IOException("Unsupported version: \"" + version + "\"");
        }
        if (iVersion < IVERSION) {
            LOGGER.debug("Table has been written with a previous version of "
                    + "KNIME (\"" + version + "\", using compatibility mode.");
        }
        return iVersion;
    }
    /**
     * Does nothing as row keys are not stored.
     * {@inheritDoc}
     */
    @Override
    void writeRowKey(final RowKey key, final DCObjectOutputStream outStream) 
        throws IOException {
        // left empty, uses always the same key
    }
    
    /**
     * Returns always the same key, does nothing to the stream.
     * {@inheritDoc}
     */
    @Override
    RowKey readRowKey(final DCObjectInputStream inStream) {
        return DUMMY_KEY;
    }
}
