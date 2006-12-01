/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;


/**
 * Buffer implementation that does not write the row keys. Used to write
 * data if only few columns have changed. This buffer writes the changed 
 * columns.
 * <p>This class is used to save the data of the new columns in a 
 * {@link RearrangeColumnsTable}.
 * @author Bernd Wiswedel, University of Konstanz
 */
class NoKeyBuffer extends Buffer {
    
    private static final RowKey DUMMY_KEY = new RowKey("non-existing");
    
    private static final String VERSION = "noRowKeyContainer_1.2.0";
    
    /**
     * For writing.
     * @see Buffer#Buffer(int)
     */
    NoKeyBuffer(final int maxRowsInMemory) {
        super(maxRowsInMemory);
    }
    
    /**
     * @see Buffer#Buffer(File, DataTableSpec, InputStream)
     */
    NoKeyBuffer(final File binFile, final DataTableSpec spec, 
            final InputStream metaIn) throws IOException {
        super(binFile, spec, metaIn);
    }
    
    /**
     * @see org.knime.core.data.container.Buffer#getVersion()
     */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * @see Buffer#validateVersion(String)
     */
    @Override
    public int validateVersion(final String version) throws IOException {
        if ("noRowKeyContainer_1.0.0".equals(version)) {
            return 100;
        } 
        if ("noRowKeyContainer_1.1.0".equals(version)) {
            return 110;
        }
        if (VERSION.equals(version)) {
            return 120;
        }
        throw new IOException("Unsupported version: \"" + version 
                + "\" (expected \"" + VERSION + "\")");
    }
    /**
     * Does nothing as row keys are not stored.
     * @see Buffer#writeRowKey(RowKey, DCObjectOutputStream)
     */
    @Override
    void writeRowKey(final RowKey key, final DCObjectOutputStream outStream) 
        throws IOException {
        // left empty, uses always the same key
    }
    
    /**
     * Returns always the same key, does nothing to the stream.
     * @see Buffer#readRowKey(DCObjectInputStream)
     */
    @Override
    RowKey readRowKey(final DCObjectInputStream inStream) throws IOException {
        return DUMMY_KEY;
    }
}
