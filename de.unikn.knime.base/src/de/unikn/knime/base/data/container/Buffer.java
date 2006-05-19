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
package de.unikn.knime.base.data.container;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataCellSerializer;
import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnProperties;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultDataColumnDomain;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.data.property.ColorAttr;
import de.unikn.knime.core.node.NodeLogger;

/**
 * A buffer writes the rows from a <code>DataContainer</code> to a file. 
 * This class serves as connector between the <code>DataContainer</code> and 
 * the <code>DataTable</code> that is returned by the container. It 
 * "centralizes" the IO operations.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class Buffer {
    
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(Buffer.class);
    
    /** the temp file will have a time stamp in its name. */
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyyMMdd");
    
    /** Separator for different rows, new line. */
    private static final char ROW_SEPARATOR = '\n';
    
    /** The char for cell whose type needs serialization. */
    private static final byte BYTE_TYPE_MISSING = Byte.MIN_VALUE;

    /** The char for cell whose type needs serialization. */
    private static final byte BYTE_TYPE_SERIALIZATION = BYTE_TYPE_MISSING + 1;
    
    /** The first used char for the map char --> type. */
    private static final byte BYTE_TYPE_START = BYTE_TYPE_MISSING + 2;

    /** Name of the zip entry containing the data. */
    private static final String ZIP_ENTRY_DATA = "data.bin";
    
    /** Name of the zip entry containing the spec. */
    private static final String ZIP_ENTRY_SPEC = "spec.bin";

    /** Contains weak references to file iterators that have ever been created
     * but not (yet) garbage collected. We will add a shutdown hook 
     * (Runtime#addShutDownHook) and close the streams of all iterators that 
     * are open. This is a workaround for bug #63 (see also
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4722539): Temp files
     * are not deleted on windows when there are open streams. 
     */
    private static final HashSet<WeakReference<FromFileIterator>> 
        OPENSTREAMS = new HashSet<WeakReference<FromFileIterator>>();

    /** the file to write to. */
    private File m_outFile;
    
    /** true when we created a temp file, false when either no IO has
     * happened or the file was explicitely given.
     */
    private boolean m_hasCreatedTempFile;
    
    /** Number of open file input streams on m_outFile. */
    private int m_nrOpenInputStreams;
    
    /** the stream that writes to the file, used for plain cells. */
    private DCObjectOutputStream m_outStream;
    
    /** maximum number of rows that are in memory. */
    private final int m_maxRowsInMem;
    
    /** the current row count (how often has addRow been called). */
    private int m_size;
    
    /** the list that keeps up to m_maxRowsInMem in memory. */
    private List<DataRow> m_list;
    
    /** the spec the rows comply with, no checking is done, however. */
    private DataTableSpec m_spec;

    /** Map for all DataCells' type, which have been added to this buffer,
     * they will be separately written to the zip file (if any).
     */
    private HashMap<Class<? extends DataCell>, Byte> m_typeShortCuts;
    
    /** Inverse map of m_typeShortCuts - it stores to each shortcut 
     * (like 'A', 'B', ...) the corresponding DataType.
     * This object is null unles close() has been called.  
     */
    private Class<? extends DataCell>[] m_shortCutsLookup;

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
                                        + "file " + it.getOutFileName(), ioe);
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
    Buffer(final int maxRowsInMemory) {
        assert (maxRowsInMemory >= 0);
        m_maxRowsInMem = maxRowsInMemory;
        m_list = new LinkedList<DataRow>();
        m_typeShortCuts = new HashMap<Class<? extends DataCell>, Byte>();
        m_list = new LinkedList<DataRow>();
        m_size = 0;
    }
    
    Buffer(final File outFile) throws IOException {
        this(0);
        if (outFile == null) {
            throw new NullPointerException("Can't set null file!");
        }
        initOutFile(outFile);
    }
    
    Buffer(final File inFile, boolean ignored) throws IOException {
        m_maxRowsInMem = 0;
        ZipFile zipFile = new ZipFile(inFile);
        InputStream specInput = zipFile.getInputStream(
                new ZipEntry(ZIP_ENTRY_SPEC));
        ObjectInputStream inStream = new ObjectInputStream(
                new BufferedInputStream(specInput));
        try {
            readSpecFromFile(inStream);
        } catch (ClassNotFoundException cnfe) {
            IOException ioe = new IOException(
                    "Unable to read spec from zip file \""
                    + inFile.getAbsolutePath() + "\"");
            ioe.initCause(cnfe);
            throw ioe;
        }
        inStream.close();
        m_outFile = inFile;
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
                    initOutFile(/*File=*/null);
                }
                writeEldestRow();             // write it to the file
            } catch (IOException ioe) {
                String fileName = (m_outFile != null 
                        ? "\"" + m_outFile.getName() + "\"" : "");
                throw new RuntimeException(
                        "Unable to write to file " + fileName , ioe);
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
        if (!usesOutFile()) {
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
            createShortCutArray();
            // if we write to a customized cell, we also need to 
            // add the table spec information (storing permanently)
            m_outStream.flush();
            if (!m_hasCreatedTempFile) {
                // we push the underlying stream forward; need to make
                // sure that m_outStream is done with everything.
                ZipOutputStream zipOut = (ZipOutputStream)m_outStream.getUnderylingStream();
                zipOut.closeEntry();
                zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_SPEC));
                ObjectOutputStream outStream = new ObjectOutputStream(zipOut);
                writeSpecToFile(outStream);
                outStream.flush();
                zipOut.closeEntry();
            }
            m_outStream.close();
            double sizeInMB = m_outFile.length() / (double)(1 << 20);
            String size = NumberFormat.getInstance().format(sizeInMB);
            LOGGER.info("Buffer file (" + m_outFile.getAbsolutePath() 
                    + ") is " + size + "MB in size");
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot close stream of file \"" 
                    + m_outFile.getName() + "\"", ioe); 
        }
    } // close()
    
    private void writeSpecToFile(ObjectOutputStream outStream) throws IOException {
        outStream.writeInt(m_size);
        // first write the short cut array
        outStream.writeObject(m_shortCutsLookup);
        DataTableSpec spec = m_spec;
        int colCount = spec.getNumColumns();
        outStream.writeInt(colCount);
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec cSpec = spec.getColumnSpec(i);
            outStream.writeObject(cSpec.getName());
            outStream.writeObject(cSpec.getType());
            DataColumnDomain domain = cSpec.getDomain();
            outStream.writeObject(domain.getValues());
            outStream.writeObject(domain.getLowerBound());
            outStream.writeObject(domain.getUpperBound());
            outStream.writeObject(cSpec.getProperties());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void readSpecFromFile(final ObjectInputStream inStream) 
        throws IOException, ClassNotFoundException {
        m_size = inStream.readInt();
        m_shortCutsLookup = (Class<? extends DataCell>[])inStream.readObject();
        int colCount = inStream.readInt();
        DataColumnSpec[] colSpecs = new DataColumnSpec[colCount];
        for (int i = 0; i < colCount; i++) {
            DataCell name = (DataCell)inStream.readObject();
            DataType type = (DataType)inStream.readObject();
            Set<DataCell> values = (Set<DataCell>)inStream.readObject();
            DataCell lowerBound = (DataCell)inStream.readObject();
            DataCell upperBound = (DataCell)inStream.readObject();
            DataColumnDomain domain = 
                new DefaultDataColumnDomain(values, lowerBound, upperBound);
            DataColumnProperties props =
                (DataColumnProperties)inStream.readObject();
            DataColumnSpecCreator creator = 
                new DataColumnSpecCreator(name, type);
            creator.setDomain(domain);
            creator.setProperties(props);
            colSpecs[i] = creator.createSpec();
        }
        m_spec = new DataTableSpec(colSpecs);
    }
    
    /** Create the shortcut table. */
    @SuppressWarnings("unchecked") // no generics in array definiton
    private void createShortCutArray() {
        m_shortCutsLookup = new Class[m_typeShortCuts.size()];
        for (Map.Entry<Class<? extends DataCell>, Byte> e 
                : m_typeShortCuts.entrySet()) {
            byte shortCut = e.getValue();
            Class<? extends DataCell> type = e.getKey();
            m_shortCutsLookup[shortCut - BYTE_TYPE_START] = type;
        }
        m_typeShortCuts = null;
    }
    
    /** Does the buffer use a file?
     * @return true If it does.
     */
    boolean usesOutFile() {
        return m_outFile != null;
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
     * Serializes the first element in the list to the out file. This method
     * is called from <code>addRow(DataRow)</code> and <code>close()</code>.
     * @throws IOException If an IO error occurs while writing to the file.
     */
    private void writeEldestRow() throws IOException {
        DataRow firstRow = m_list.remove(0);
        RowKey id = firstRow.getKey();
        writeRowKey(id);
        for (int i = 0; i < firstRow.getNumCells(); i++) {
            DataCell cell = firstRow.getCell(i);
            writeDataCell(cell);
        }
        m_outStream.writeChar(ROW_SEPARATOR);
        m_outStream.reset();
    } // writeEldestRow()
    
    /** Writes the row key to the out stream. */
    private void writeRowKey(final RowKey key) throws IOException {
        DataCell id = key.getId();
        writeDataCell(id);
        ColorAttr c = key.getColorAttr();
        Color color = c.getColor();
        m_outStream.writeInt(color.getRGB());
    }
    
    /** Reads a row key from a string. */
    private RowKey readRowKey(final DCObjectInputStream inStream) 
        throws IOException {
        DataCell id = readDataCell(inStream);
        int color = inStream.readInt();
        return new RowKey(id, ColorAttr.getInstance(new Color(color)));
    }
    
    /** Writes a data cell to the m_outStream. */
    private void writeDataCell(final DataCell cell) throws IOException {
        if (cell.isMissing()) {
            m_outStream.writeByte(BYTE_TYPE_MISSING);
            return;
        }
        DataCellSerializer serializer = 
            DataType.getCellSerializer(cell.getClass());
        // DataCell is datacell-serializable
        if (serializer != null) {
            Byte identifier = m_typeShortCuts.get(cell.getClass()); 
            if (identifier == null) {
                int size = m_typeShortCuts.size();
                if (size + BYTE_TYPE_START > Byte.MAX_VALUE) {
                    throw new IOException(
                            "Too many different cell implemenations");
                }
                identifier = (byte)(size + BYTE_TYPE_START);
                m_typeShortCuts.put(cell.getClass(), identifier);
            }
            // memorize type if it does not exist
            m_outStream.writeByte((byte)identifier);
            m_outStream.writeDataCell(serializer, cell);
        } else {
            m_outStream.writeByte(BYTE_TYPE_SERIALIZATION);
            m_outStream.writeObject(cell);
        }
    }

    /** Reads a datacell from a string. */
    private DataCell readDataCell(final DCObjectInputStream inStream) 
        throws IOException {
        byte identifier = inStream.readByte();
        if (identifier == BYTE_TYPE_MISSING) {
            return DataType.getMissingCell();
        }
        if (identifier == BYTE_TYPE_SERIALIZATION) {
            try {
                return (DataCell)inStream.readObject();
            } catch (ClassNotFoundException cnfe) {
                IOException ioe = new IOException(cnfe.getMessage());
                ioe.initCause(cnfe);
                throw ioe;
            }
        } else {
            Class<? extends DataCell> type = getTypeForChar(identifier);
            DataCellSerializer serializer = DataType.getCellSerializer(type);
            assert serializer != null;
            try {
                return inStream.readDataCell(serializer);
            } catch (IOException ioe) {
                LOGGER.debug("Unable to read cell from file.", ioe);
                return DataType.getMissingCell();
            }
        }
    }
    
    private Class<? extends DataCell> getTypeForChar(final byte identifier) 
        throws IOException {
        int shortCutIndex = (byte)((int)identifier - BYTE_TYPE_START);
        if (shortCutIndex < 0 || shortCutIndex >= m_shortCutsLookup.length) {
            throw new IOException("Unknown shortcut byte '" + identifier + "'");
        }
        return m_shortCutsLookup[shortCutIndex];
    }
    
    /** Creates the out file and the stream that writes to it.
     * @param outFile The file to write to. If <code>null</code>, a 
     * temp file is created and deleted on exit.
     * @throws IOException If the file or stream cannot be instantiated.
     */
    private void initOutFile(final File outFile) throws IOException {
        assert (m_outStream == null);
        if (outFile == null) {
            String date = DATE_FORMAT.format(new Date());
            String fileName = "knime_container_" + date + "_";
            String suffix = ".ser.zip";
            // TODO: Do we need to set our own tempory directory 
            // as knime preferences?
            m_outFile = File.createTempFile(fileName, suffix);
            m_outFile.deleteOnExit();
            m_hasCreatedTempFile = true;
        } else {
            m_outFile = outFile;
            m_hasCreatedTempFile = false;
        }
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(m_outFile)));
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_DATA));
        m_outStream = new DCObjectOutputStream(zipOut);
    } // initOutFile()
    
    /**
     * Get a new <code>RowIterator</code>, traversing all rows that have been
     * added. Calling this method makes only sense when the buffer has been 
     * closed. However, no check is done (as it is available to package classes
     * only).
     * @return a new Iterator over all rows.
     */
    public RowIterator iterator() {
        if (usesOutFile()) {
            return new FromFileIterator();
        } else {
            return new FromListIterator();
        }
    }
    
    /** Deletes the file underlying this buffer.
     * @see Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        if (m_outFile != null && m_hasCreatedTempFile) {
            if (m_outFile.delete()) {
                LOGGER.debug("Deleted temp file \"" 
                        + m_outFile.getAbsolutePath() + "\"");
            } else {
                LOGGER.debug("Failed to delete temp file \"" 
                        + m_outFile.getAbsolutePath() + "\"");
            }
        }
        super.finalize();
    }
    
    /**
     * Iterator that traverses the out file on the disk and deserializes
     * the rows.
     */
    private class FromFileIterator extends RowIterator {
        
        private int m_pointer;
        private final DCObjectInputStream m_inStream;
        
        /**
         * Inits the input stream.
         */
        FromFileIterator() {
            m_pointer = 0;
            try {
                ZipFile zipFile = new ZipFile(m_outFile, ZipFile.OPEN_READ);
                BufferedInputStream zipIn = new BufferedInputStream(
                        zipFile.getInputStream(new ZipEntry(ZIP_ENTRY_DATA)));
                m_inStream = new DCObjectInputStream(zipIn);
                m_nrOpenInputStreams++;
                LOGGER.debug("Opening input stream on file \"" 
                        + m_outFile.getAbsolutePath() + "\", " 
                        + m_nrOpenInputStreams + " open streams");
            } catch (IOException ioe) {
                throw new RuntimeException("Cannot read file \"" 
                        + m_outFile.getName() + "\"", ioe);
            }
            OPENSTREAMS.add(new WeakReference<FromFileIterator>(this));
        }
        
        /** Get the name of the out file that this iterator works on. 
         * @return The name of the out file
         */
        public String getOutFileName() {
            return m_outFile.getAbsolutePath();
        }

        /**
         * @see de.unikn.knime.core.data.RowIterator#hasNext()
         */
        public boolean hasNext() {
            boolean hasNext = m_pointer < Buffer.this.m_size;
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
        public synchronized DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Iterator at end");
            }
            final DCObjectInputStream inStream = m_inStream;
            try {
                // read Row key
                RowKey key = readRowKey(inStream);
                int colCount = m_spec.getNumColumns();
                DataCell[] cells = new DataCell[colCount];
                for (int i = 0; i < colCount; i++) {
                    cells[i] = readDataCell(inStream);
                }
                char eoRow = m_inStream.readChar();
                if (eoRow != ROW_SEPARATOR) {
                    throw new IOException("Exptected end of row character, " 
                            + "got '" + eoRow + "'");
                }
                return new DefaultRow(key, cells);
            } catch (Exception ioe) {
                throw new RuntimeException("Cannot read line "  
                    + (m_pointer + 1) + " from file \"" 
                        + m_outFile.getName() + "\"", ioe);
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
            String closeMes = "Closing input stream on \""
                + m_outFile.getAbsolutePath() + "\", "; 
            try {
                m_inStream.close();
                m_nrOpenInputStreams--;
                LOGGER.debug(closeMes + m_nrOpenInputStreams + " remaining");
            } catch (IOException ioe) {
                LOGGER.debug(closeMes + "failed!", ioe);
                throw ioe;
            } finally {
                super.finalize();
            }
        }
    }
    
    /**
     * Class wrapping the iterator of a java.util.List to a RowIterator.
     * This object is used when all rows fit in memory (no file).
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
