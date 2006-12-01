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

 */
package org.knime.core.data.container;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;


/**
 * Class implementing the <code>DataTable</code> interface and using a buffer
 * from a <code>DataContainer</code> as data source. This class doesn't do 
 * functional things. It only provides the <code>DataTable</code> methods.
 * 
 * <p>We split it from the <code>Buffer</code> implementation as a buffer is
 * dynamic in size. This table should only be used when the buffer has been
 * fixed.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ContainerTable implements DataTable, KnowsRowCountTable {
    
    /** To read the data from. */
    private Buffer m_buffer;
    /** Contains functionality to copy the binary data to the temp file on
     * demand (e.g. iterator is opened). */
    private CopyOnAccessTask m_readTask;
    private DataTableSpec m_spec;
    
    /**
     * Create new Table based on a Buffer. This constructor is called from
     * <code>DataContainer.getTable()</code>.
     * @param buffer To read data from.
     * @see DataContainer#getTable()
     */
    ContainerTable(final Buffer buffer) {
        assert (buffer != null);
        m_buffer = buffer;
    }
    
    /**
     * Constructor when table is read from file. 
     * @param readTask Carries out the copy process when iterator is requested
     *        (just once).
     * @param spec The spec of this table.
     */
    ContainerTable(final CopyOnAccessTask readTask, 
            final DataTableSpec spec) {
        m_readTask = readTask;
        m_spec = spec;
    }

    /**
     * @see org.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        if (m_buffer != null) {
            return m_buffer.getTableSpec();
        }
        return m_spec;
    }

    /**
     * @see org.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        ensureBufferOpen();
        return m_buffer.iterator();
    }
    
    /**
     * @see KnowsRowCountTable#getRowCount()
     */
    public int getRowCount() {
        ensureBufferOpen();
        return m_buffer.size();
    }

    /** Get reference to buffer.
     * @return The buffer backing this object.
     */
    Buffer getBuffer() {
        ensureBufferOpen();
        return m_buffer;
    }

    /**
     * Do not call this method! Internal use!
     * @see KnowsRowCountTable#saveToFile(
     * File, NodeSettingsWO, ExecutionMonitor)
     */
    public void saveToFile(final File f, final NodeSettingsWO settings, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        ensureBufferOpen();
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(f)));
        m_buffer.addToZipFile(zipOut, exec);
        zipOut.close();
    }
    
    /**
     * Do not call this method! It's used internally to delete temp files. 
     * Any iteration on the table will fail!
     * @see KnowsRowCountTable#clear()
     */
    public void clear() {
        if (m_buffer != null) {
            m_buffer.clear();
        }
    }
    
    /**
     * Returns <code>null</code>. This method is used internally.
     * @see KnowsRowCountTable#getReferenceTable()
     */
    public BufferedDataTable getReferenceTable() {
        return null;
    }
    
    /** Executes the copy process when the content of this table is demanded 
     * for the first time. */
    private void ensureBufferOpen() {
        // do not synchronize this check here as this method most of the
        // the times returns immediately
        if (m_buffer != null) {
            return;
        }
        synchronized (m_readTask) {
            // synchronized may have blocked when another thread was
            // executing the copy task. If so, there is nothing else to 
            // do here
            if (m_buffer != null) {
                return;
            }
            m_buffer = m_readTask.createBuffer();
            m_spec = null;
            m_readTask = null;
        }
    }
    
}
