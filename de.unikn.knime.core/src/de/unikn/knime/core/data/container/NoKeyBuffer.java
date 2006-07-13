/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Jul 12, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.container;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.node.NodeSettings;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class NoKeyBuffer extends Buffer {
    
    private static final RowKey DUMMY_KEY = new RowKey("non-existing");
    private static final DataRow DUMMY_ROW = 
        new DefaultRow(DUMMY_KEY, new DataCell[0]);
    
    private static final String VERSION = "noRowKeyContainer_1.0.0";
    int m_loadID;
    
    NoKeyBuffer(final int maxRowsInMemory) {
        super(maxRowsInMemory);
    }
    
    /**
     * For writing.
     * @see Buffer#Buffer(File)
     */
    NoKeyBuffer(final File outFile, final NodeSettings additionalMeta) 
        throws IOException {
        super(outFile);
    }
    
    /**
     * For reading.
     * @see Buffer#Buffer(File, boolean)
     */
    NoKeyBuffer(final File inFile, final int loadID) 
        throws IOException {
        super(inFile, false);
    }
    
    /**
     * @see de.unikn.knime.core.data.container.Buffer#getVersion()
     */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * @see Buffer#validateVersion(String)
     */
    @Override
    public void validateVersion(final String version) throws IOException {
        if (!VERSION.equals(version)) {
            throw new IOException("Unsupported version: \"" + version 
                    + "\" (expected \"" + VERSION + "\")");
        }
    }
    
    @Override
    public void addRow(final DataRow row) {
        if (row.getNumCells() > 0) {
            super.addRow(row);
        } else {
            incrementSize();
        }
    }
    
    @Override
    public RowIterator iterator() {
        if (getTableSpec().getNumColumns() > 0) {
            return super.iterator();
        } else {
            return new RowIterator() {
                private int m_count = 0;
                public boolean hasNext() {
                    return m_count < size();
                }
                public DataRow next() {
                    m_count++;
                    return DUMMY_ROW;
                };
            };
        }
    }
    
    /**
     * Does nothing as row keys are not stored.
     * @see Buffer#writeRowKey(RowKey)
     */
    @Override
    void writeRowKey(final RowKey key) throws IOException {
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
