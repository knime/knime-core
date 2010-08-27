/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
