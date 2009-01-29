/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
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
    private static final String VERSION = "noRowKeyContainer_8";
    
    /** The version number corresponding to VERSION. */
    private static final int IVERSION = 8;
    
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
        COMPATIBILITY_MAP.put(VERSION, IVERSION);
    }
    
    /** Creates new buffer for writing.
     * @param maxRowsInMemory Passed on to super.
     * @param bufferID Passed on to super.
     * @param tblRep Passed on to super.
     * @param localTblRep Passed on to super.
     */
    NoKeyBuffer(final int maxRowsInMemory, 
            final int bufferID, final Map<Integer, ContainerTable> tblRep,
            final Map<Integer, ContainerTable> localTblRep) {
        super(maxRowsInMemory, bufferID, tblRep, localTblRep);
    }
    
    /** Creates new buffer for reading.
     * @param binFile Passed on to super.
     * @param blobDir Passed on to super.
     * @param spec Passed on to super.
     * @param metaIn Passed on to super.
     * @param bufferID Passed on to super.
     * @param tblRep Passed on to super.
     * @throws IOException Passed on from super.
     */
    NoKeyBuffer(final File binFile, final File blobDir, 
            final DataTableSpec spec, final InputStream metaIn, 
            final int bufferID, final Map<Integer, ContainerTable> tblRep) 
            throws IOException {
        super(binFile, blobDir, spec, metaIn, bufferID, tblRep);
    }
    
    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /** {@inheritDoc} */
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
    
    /** {@inheritDoc} */
    @Override
    boolean shouldSkipRowKey() {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    Buffer createLocalCloneForWriting() {
        return new NoKeyBuffer(0, getBufferID(), 
                getGlobalRepository(), Collections.EMPTY_MAP);
    }
    
}
