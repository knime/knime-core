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
 */
package de.unikn.knime.dev.data.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.eclipseUtil.GlobalObjectInputStream;
import de.unikn.knime.core.node.NodeLogger;

/**
 * A buffer writes the rows from a <code>DataContainer</code> to a temp file. 
 * This class serves as connector between the <code>DataContainer</code> and 
 * the <code>DataTable</code> that is returned by the container. It 
 * "centralizes" the IO operations.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class Buffer1 implements Buffer {
    
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(Buffer1.class);
    
    /** the temp file will have a time stamp in its name. */
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyyMMdd");

    /** Contains weak references to file iterators that have ever been created
     * but not (yet) garbage collected. We will add a shutdown hook 
     * (Runtime#addShutDownHook) and close the streams of all iterators that 
     * are open. This is a workaround for bug #63 (see also
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4722539): Temp files
     * are not deleted on windows when there are open streams. 
     */
    private static final HashSet<WeakReference<FromFileIterator>> 
        OPENSTREAMS = new HashSet<WeakReference<FromFileIterator>>();

    /** the temp file to write to. */
    private File m_tempFile;
    
    /** Number of open file input streams on m_tempFile. */
    private int m_nrOpenInputStreams;
    
    /** the stream that writes to the temp file, the stream is zipped! */
    private ObjectOutputStream m_outStream;
    
    /** maximum number of rows that are in memory. */
    private final int m_maxRowsInMem;
    
    /** the current row count (how often has addRow been called). */
    private int m_size;
    
    /** the list that keeps up to m_maxRowsInMem in memory. */
    private List<DataRow> m_list;
    
    /** the spec the rows comply with, no checking is done, however. */
    private DataTableSpec m_spec;
    
    /** Adds a shutdown hook to the runtime that closes all open input streams
     * @see #OPENSTREAMS
     */
    static {
        try {
            Thread hook = new Thread() {
                public void run() {
                    for (WeakReference<FromFileIterator> ref : OPENSTREAMS) {
                        FromFileIterator it = ref.get();
                        if (it != null) {
                            try {
                                it.m_inStream.close();
                            } catch (IOException ioe) {
                                LOGGER.warn("Unable to close input stream on " 
                                        + "file " + it.getTempFileName(), ioe);
                            }
                        }
                    }
                };
            };
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (Exception e) {
            LOGGER.warn("Unable to add shutdown hook to delete temp files", e);
        }
    }

    /**
     * Creates new buffer with a given spec, and a max row count that may 
     * resize in memory.
     * @param maxRowsInMemory Maximum numbers of rows that are kept in memory 
     *        until they will be subsequent written to the temp file. (0 to 
     *        write immediately to a file)
     */
    Buffer1(final int maxRowsInMemory) {
        assert (maxRowsInMemory >= 0);
        m_maxRowsInMem = maxRowsInMemory;
        m_list = new LinkedList<DataRow>();
        m_size = 0;
    }
    
    /** 
     * Adds a row to the buffer. The rows structure is not validated against
     * the table spec that was given in the constructor. This should have been
     * done in the caller class <code>DataContainer</code>.
     * @param row The row to be added.
     */
    public void addRow(final DataRow row) {
        m_list.add(row);
        m_size++;
        if (m_list.size() > m_maxRowsInMem) { // if size is violated
            try {
                if (m_outStream == null) {
                    initTempFile();
                }
                writeEldestRow();             // write it to the file
            } catch (IOException ioe) {
                String fileName = (m_tempFile != null 
                        ? "\"" + m_tempFile.getName() + "\"" : "");
                throw new RuntimeException(
                        "Unable to write to temp file " + fileName , ioe);
            }
        }
        assert (m_list.size() <= m_maxRowsInMem);
    } // addRow(DataRow)
    
    /**
     * Flushes and closes the stream. If no file has been created and therefore
     * everything fits in memory (according to the settings in the constructor),
     * it will stay in memory (no file created).
     * @param spec The spec the rows have to follow. No sanity check is done.
     */
    public void close(final DataTableSpec spec) {
        assert (spec != null);
        m_spec = spec;
        // everything is in the list, i.e. in memory
        if (!usesTempFile()) {
            // disallow modification
            List<DataRow> newList = Collections.unmodifiableList(m_list);
            m_list = newList;
            return;
        }
        try {
            // if it uses the file anyway: write also last rows to it.
            while (!m_list.isEmpty()) {
                writeEldestRow();
            }
            m_outStream.close();
            double sizeInMB = m_tempFile.length() / (double)(1 << 20);
            String size = NumberFormat.getInstance().format(sizeInMB);
            LOGGER.info("Buffer file (" + m_tempFile.getAbsolutePath() 
                    + ") is " + size + "MB in size");
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot close stream of temp file \"" 
                    + m_tempFile.getName() + "\"", ioe); 
        }
    } // close()
    
    /** Does the buffer use a temp file?
     * @return true If it does.
     */
    boolean usesTempFile() {
        return m_tempFile != null;
    }
    
    /** Get the table spec that was set in the constructor.
     * @return The spec the buffer uses.
     */
    public DataTableSpec getTableSpec() {
        return m_spec;
    }
    
    /** Get the row count.
     * @return How often has addRow() been called.
     */
    public int size() {
        return m_size;
    }
    
    /**
     * Serializes the first element in the list to the temp file. This method
     * is called from <code>addRow(DataRow)</code> and <code>close()</code>.
     * @throws IOException If an IO error occurs while writing to the file.
     */
    private void writeEldestRow() throws IOException {
        DataRow firstRow = m_list.remove(0);
        RowKey id = firstRow.getKey();
        m_outStream.writeObject(id);
        for (int i = 0; i < firstRow.getNumCells(); i++) {
            DataCell cell = firstRow.getCell(i);
            m_outStream.writeObject(cell);
        }
        m_outStream.flush();
        m_outStream.reset();
    } // writeEldestRow()
    
    /** Creates the temp file and the stream that writes to it.
     * @throws IOException If the file or stream cannot be instantiated.
     */
    private void initTempFile() throws IOException {
        assert (m_outStream == null);
        String date = DATE_FORMAT.format(new Date());
        String fileName = "knime_container_" + date + "_";
        String suffix = ".ser.zip";
        // TODO: Do we need to set our own tempory directory 
        // as knime preferences?
        m_tempFile = File.createTempFile(fileName, suffix);
        m_tempFile.deleteOnExit();
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(m_tempFile)));
        zipOut.putNextEntry(new ZipEntry(fileName + ".ser"));
        m_outStream =  new ObjectOutputStream(zipOut);
    } // initTempFile()
    
    /**
     * Get a new <code>RowIterator</code>, traversing all rows that have been
     * added. Calling this method makes only sense when the buffer has been 
     * closed. However, no check is done (as it is available to package classes
     * only).
     * @return a new Iterator over all rows.
     */
    public RowIterator iterator() {
        if (usesTempFile()) {
            return new FromFileIterator();
        } else {
            return new FromListIterator();
        }
    }
    
    /**
     * Iterator that traverses the temp file on the disk and deserializes
     * the rows.
     */
    private class FromFileIterator extends RowIterator {
        
        private int m_pointer;
        private final ObjectInputStream m_inStream;
        
        /**
         * Inits the input stream.
         */
        FromFileIterator() {
            m_pointer = 0;
            try {
                ZipInputStream zipIn = new ZipInputStream(
                        new BufferedInputStream(
                                new FileInputStream(m_tempFile)));
                zipIn.getNextEntry();
                m_inStream = new GlobalObjectInputStream(zipIn);
                m_nrOpenInputStreams++;
                LOGGER.debug("Opening input stream on temp file \"" 
                        + m_tempFile.getAbsolutePath() + "\", " 
                        + m_nrOpenInputStreams + " open streams");
            } catch (IOException ioe) {
                throw new RuntimeException("Cannot read temp file \"" 
                        + m_tempFile.getName() + "\"", ioe);
            }
            OPENSTREAMS.add(new WeakReference<FromFileIterator>(this));
        }
        
        /** Get the name of the temp file that this iterator works on. 
         * @return The name of the temp file
         */
        public String getTempFileName() {
            return m_tempFile.getAbsolutePath();
        }

        /**
         * @see de.unikn.knime.core.data.RowIterator#hasNext()
         */
        public boolean hasNext() {
            boolean hasNext = m_pointer < Buffer1.this.m_size;
            if (!hasNext) {
                try {
                    m_inStream.close();
                } catch (IOException ioe) {
                    LOGGER.warn("Unable to close stream from DataContainer: " 
                            + ioe.getMessage(), ioe);
                    throw new RuntimeException(ioe);
                }
            }
            return hasNext;
        }

        /**
         * @see de.unikn.knime.core.data.RowIterator#next()
         */
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Iterator at end");
            }
            try {
                RowKey key = (RowKey)m_inStream.readObject();
                DataCell[] cells = new DataCell[getTableSpec().getNumColumns()];
                for (int i = 0; i < cells.length; i++) {
                    cells[i] = (DataCell)m_inStream.readObject();
                }
                DataRow row = new DefaultRow(key, cells);
                return row;
            } catch (Exception ioe) {
                throw new RuntimeException("Cannot read line "  
                    + (m_pointer + 1) + " from temp file \"" 
                        + m_tempFile.getName() + "\"", ioe);
            } finally {
                m_pointer++;
            }
        }
        
        /**
         * @see java.lang.Object#finalize()
         */
        protected void finalize() throws Throwable {
            /* This all relates much to bug #63: The temp files are not
             * deleted under windows. It seems that there are open streams
             * when the VM closes.
             */
            LOGGER.debug("Closing input stream on \""
                    + m_tempFile.getAbsolutePath() + "\"");
            try {
                m_inStream.close();
                m_nrOpenInputStreams--;
            } catch (IOException ioe) {
                LOGGER.debug("Closing failed", ioe);
                throw ioe;
            } finally {
                LOGGER.debug(m_nrOpenInputStreams + " open streams");
                super.finalize();
            }
        }
    }
    
    /**
     * Class wrapping the iterator of a java.util.List to a RowIterator.
     * This object is used when all rows fit in memory (no temp file).
     */
    private class FromListIterator extends RowIterator {
        
        private Iterator<DataRow> m_it = m_list.iterator();

        /**
         * @see de.unikn.knime.core.data.RowIterator#hasNext()
         */
        public boolean hasNext() {
            return m_it.hasNext();
        }

        /**
         * @see de.unikn.knime.core.data.RowIterator#next()
         */
        public DataRow next() {
            return m_it.next();
        }
        
    }
    
}
