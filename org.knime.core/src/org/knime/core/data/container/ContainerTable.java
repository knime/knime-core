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

 */
package org.knime.core.data.container;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
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
public final class ContainerTable implements DataTable, KnowsRowCountTable {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ContainerTable.class);
    
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
    ContainerTable(final CopyOnAccessTask readTask, final DataTableSpec spec) {
        m_readTask = readTask;
        m_spec = spec;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        if (m_buffer != null) {
            return m_buffer.getTableSpec();
        }
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    public CloseableRowIterator iterator() {
        ensureBufferOpen();
        return m_buffer.iterator();
    }
    
    /**
     * {@inheritDoc}
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
     * Delegates to buffer to get its ID.
     * @return the buffer ID
     * @see Buffer#getBufferID()
     */
    public int getBufferID() {
        if (m_buffer != null) {
            return m_buffer.getBufferID();
        }
        return m_readTask.getBufferID();
    }
    
    /** Instruct the underlying buffer to cache the rows into main 
     * memory to accelerate future iterations. This method does nothing
     * if the buffer is reading from memory already.
     * @see Buffer#restoreIntoMemory() */
    protected void restoreIntoMemory() {
        if (m_buffer != null) {
            m_buffer.restoreIntoMemory();
        } else {
            m_readTask.setRestoreIntoMemory();
        }
    }

    /**
     * Do not call this method! Internal use!
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        rep.put(getBufferID(), this);
        /* The following assertion must generally hold. Unfortunately, we have
         * a bug in pre 2.0 versions (bug #1291), which prevents us from 
         * enabling this assertion. The bug can lead to different tables with
         * the exact same content. If we enable the assertion, we may run into
         * problems with workflows saved in 1.x (more precisely the disturber
         * node in the testing plugin was copying input files). */
        // ContainerTable old = rep.put(getBufferID(), this);
        // assert old == null || old == this
        //     : "Different container table with same ID " + getBufferID() 
        //         + " already present in global table repository: "
        //         + Arrays.toString(rep.keySet().toArray());
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        if (rep.remove(getBufferID()) == null) {
            LOGGER.debug("Failed to remove container table with id " 
                    + getBufferID() + " from global table repository.");
        }
    }
    
    /**
     * Do not call this method! It's used internally to delete temp files. 
     * Any subsequent iteration on the table will fail!
     * @see KnowsRowCountTable#clear()
     */
    public void clear() {
        if (m_buffer != null) {
            m_buffer.clear();
            // it may not even be in there
            m_buffer.getGlobalRepository().remove(m_buffer.getBufferID());
        }
    }
    
    /** Do not use this method (only invoked by the framework).
     * {@inheritDoc} */
    public void ensureOpen() {
        ensureBufferOpen();
    }
    
    private static final BufferedDataTable[] EMPTY_ARRAY = 
        new BufferedDataTable[0];
    
    /**
     * Returns an empty array. This method is used internally.
     * {@inheritDoc}
     */
    public BufferedDataTable[] getReferenceTables() {
        return EMPTY_ARRAY;
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
            try {
                m_buffer = m_readTask.createBuffer();
            } catch (IOException i) {
                throw new RuntimeException("Exception while accessing file: \"" 
                        + m_readTask.getFileName() + "\": " 
                        + i.getMessage(), i);
            }
            m_spec = null;
            m_readTask = null;
        }
    }
    
}
