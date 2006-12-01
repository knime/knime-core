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
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.FileUtil;

/**
 * A buffer writes the rows from a {@link DataContainer} to a file. 
 * This class serves as connector between the {@link DataContainer} and 
 * the {@link org.knime.core.data.DataTable} that is returned by the container.
 * It "centralizes" the IO operations.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
class Buffer {
    
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(Buffer.class);
    
    /** Separator for different rows, new line. */
    private static final char ROW_SEPARATOR = '\n';
    
    /** The char for cell whose type needs serialization. */
    private static final byte BYTE_TYPE_MISSING = Byte.MIN_VALUE;

    /** The char for cell whose type needs serialization. */
    private static final byte BYTE_TYPE_SERIALIZATION = BYTE_TYPE_MISSING + 1;
    
    /** The first used char for the map char --> type. */
    private static final byte BYTE_TYPE_START = BYTE_TYPE_MISSING + 2;

    /** Name of the zip entry containing the data. */
    static final String ZIP_ENTRY_DATA = "data.bin";
    
    /** Name of the zip entry containing the meta information (e.g. #rows). */
    static final String ZIP_ENTRY_META = "meta.xml";
    
    /** Config entries when writing the meta information to the file,
     * this is a subconfig in meta.xml.
     */
    private static final String CFG_INTERNAL_META = "table.meta.internal";

    /** Config entries when writing the meta info to the file (uses NodeSettings
     * object, which uses key-value pairs. Here: the version of the writing 
     * method.
     */
    private static final String CFG_VERSION = "container.version";
    
    /** Config entries when writing the spec to the file (uses NodeSettings
     * object, which uses key-value pairs. Here: size of the table (#rows).
     */
    private static final String CFG_SIZE = "table.size";
    
    /** Config entry: Array of the used DataCell classes; storing the class
     * names as strings, see m_shortCutsLookup. */
    private static final String CFG_CELL_CLASSES = "table.datacell.classes";
    
    /** The version this container is able to read, may be an array in the 
     * future. */
    private static final String VERSION = "container_1.2.0";

    /**
     * Contains weak references to file iterators that have ever been created
     * but not (yet) garbage collected. We will add a shutdown hook 
     * (Runtime#addShutDownHook) and close the streams of all iterators that 
     * are open. This is a workaround for bug #63 (see also
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4722539): Temp files
     * are not deleted on windows when there are open streams. 
     */
    private static final HashSet<WeakReference<Buffer>> 
        OPENBUFFERS = new HashSet<WeakReference<Buffer>>();

    /** the file to write to. */
    private File m_binFile;
    
    /** Number of open file input streams on m_binFile. */
    private int m_nrOpenInputStreams;
    
    /** the stream that writes to the file, it's a special object output
     * stream, in which we can mark the end of an entry (to figure out
     * when a cell implementation reads too many or too few bytes). */
    private DCObjectOutputStream m_outStream;
    
    /** maximum number of rows that are in memory. */
    private int m_maxRowsInMem;
    
    /** the current row count (how often has addRow been called). */
    private int m_size;
    
    /** the list that keeps up to m_maxRowsInMem in memory. */
    private List<DataRow> m_list;
    
    /** the spec the rows comply with, no checking is done, however. */
    private DataTableSpec m_spec;

    /** Map for all DataCells' type, which have been added to this buffer,
     * they will be separately written to to the meta.xml in a zip file.
     */
    private HashMap<Class<? extends DataCell>, Byte> m_typeShortCuts;
    
    /** Inverse map of m_typeShortCuts - it stores to each shortcut 
     * (like 'A', 'B', ...) the corresponding DataType.
     * This object is null unless close() has been called.  
     */
    private Class<? extends DataCell>[] m_shortCutsLookup;
    
    /**
     * List of file iterators that look at this buffer. Need to close them
     * when the node is reset and the file shall be deleted.
     */
    private final HashSet<WeakReference<FromFileIterator>> m_openIteratorSet;
    
    /**
     * The version of the file we are reading (if initiated with 
     * Buffer(File, boolean). Used to remember when we need to read a file 
     * which has been written with another version of the Buffer, i.e. to
     * provide backward compatibility.
     */
    private int m_version = 120;

    /** Adds a shutdown hook to the runtime that closes all open input streams
     * @see #OPENBUFFERS
     */
    static {
        try {
            Thread hook = new Thread() {
                @Override
                public void run() {
                    for (WeakReference<Buffer> ref : OPENBUFFERS) {
                        Buffer it = ref.get();
                        if (it != null) {
                            it.clear();
                        }
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (Exception e) {
            LOGGER.warn("Unable to add shutdown hook to delete temp files", e);
        }
    }

    /**
     * Creates new buffer for <strong>writing</strong>. It has assigned a 
     * given spec, and a max row count that may resize in memory. 
     * 
     * @param maxRowsInMemory Maximum numbers of rows that are kept in memory 
     *        until they will be subsequent written to the temp file. (0 to 
     *        write immediately to a file)
     */
    Buffer(final int maxRowsInMemory) {
        assert (maxRowsInMemory >= 0);
        m_maxRowsInMem = maxRowsInMemory;
        m_list = new LinkedList<DataRow>();
        m_openIteratorSet = new HashSet<WeakReference<FromFileIterator>>();
        m_size = 0;
    }
    
    /** Creates new buffer for <strong>reading</strong>. The 
     * <code>binFile</code> is the binary file as written by this class, which
     * will be deleted when this buffer is cleared or finalized. 
     * 
     * @param binFile The binary file to read from (will be deleted on exit).
     * @param spec The data table spec to which the this buffer complies to.
     * @param metaIn An input stream from which this constructor reads the 
     * meta information (e.g. which byte encodes which DataCell).
     * @throws IOException If the header (the spec information) can't be read.
     */
    Buffer(final File binFile, final DataTableSpec spec, 
            final InputStream metaIn) throws IOException {
        m_spec = spec;
        m_binFile = binFile;
        if (metaIn == null) {
            throw new IOException("No meta information given (null)");
        }
        m_maxRowsInMem = 0;
        try {
            readMetaFromFile(metaIn);
        } catch (ClassNotFoundException cnfe) {
            IOException ioe = new IOException(
                    "Unable to read meta information from file \"" 
                    + metaIn + "\"");
            ioe.initCause(cnfe);
            throw ioe;
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException(
                    "Unable to read meta information from file \"" 
                    + metaIn + "\"");
            ioe.initCause(ise);
            throw ioe;
        }
        // just check if data is present!
        if (binFile == null || !binFile.canRead() || !binFile.isFile()) {
            throw new IOException("Unable to reade from file: " + binFile);
        }
        m_openIteratorSet = new HashSet<WeakReference<FromFileIterator>>();
    }
    
    /** Get the version string to write to the meta file.
     * This method is overridden in the {@link NoKeyBuffer} to distinguish
     * streams written by the different implementations.
     * @return The version string.
     */
    public String getVersion() {
        return VERSION;
    }
    
    /**
     * Validate the version as read from the file if it can be parsed by
     * this implementation.
     * @param version As read from file.
     * @return The version ID for internal use.
     * @throws IOException If it can't be parsed.
     */
    public int validateVersion(final String version) throws IOException {
        if ("container_1.0.0".equals(version)) {
            LOGGER.debug("Table has been written with a previous version " 
                    + "of KNIME (1.0.0), using compatibility mode.");
            return 100;
        }
        if ("container_1.1.0".equals(version)) {
            LOGGER.debug("Table has been written with a previous version " 
                    + "of KNIME (1.1.0), using compatibility mode.");
            return 110;
        }
        if (VERSION.equals(version)) {
            return 120;
        }
        throw new IOException("Unsupported version: \"" + version 
                + "\" (expected \"" + VERSION + "\")");
    }
    
    /** 
     * Adds a row to the buffer. The rows structure is not validated against
     * the table spec that was given in the constructor. This should have been
     * done in the caller class <code>DataContainer</code>.
     * @param row The row to be added.
     */
    public void addRow(final DataRow row) {
        m_list.add(row);
        incrementSize();
        // if size is violated
        if (m_list.size() > m_maxRowsInMem) {
            try {
                if (m_binFile == null) {
                    m_binFile = DataContainer.createTempFile();
                    OPENBUFFERS.add(new WeakReference<Buffer>(this));
                    m_outStream = initOutFile(new BufferedOutputStream(
                            new FileOutputStream(m_binFile)));
                }
                while (!m_list.isEmpty()) {
                    DataRow firstRow = m_list.remove(0);
                    writeRow(firstRow, m_outStream); // write it to the file
                }
                // write next rows directly to file  
                m_maxRowsInMem = 0;
            } catch (IOException ioe) {
                String fileName = (m_binFile != null 
                        ? "\"" + m_binFile.getName() + "\"" : "");
                throw new RuntimeException(
                        "Unable to write to file " + fileName , ioe);
            }
        }
        assert (m_list.size() <= m_maxRowsInMem);
    } // addRow(DataRow)
    
    /** Increments the row counter by one, used in addRow. */
    void incrementSize() {
        m_size++;
    }
    
    /**
     * Flushes and closes the stream. If no file has been created and therefore
     * everything fits in memory (according to the settings in the constructor),
     * it will stay in memory (no file created).
     * @param spec The spec the rows have to follow. No sanity check is done.
     */
    void close(final DataTableSpec spec) {
        assert (spec != null);
        m_spec = spec;
        // everything is in the list, i.e. in memory
        if (!usesOutFile()) {
            // disallow modification
            List<DataRow> newList = Collections.unmodifiableList(m_list);
            m_list = newList;
        } else {
            try {
                assert (m_list.isEmpty()) : "In-Memory list is not empty.";
                m_shortCutsLookup = closeFile(m_outStream);
                m_typeShortCuts = null; // garbage
                m_list = null;
                double sizeInMB = m_binFile.length() / (double)(1 << 20);
                String size = NumberFormat.getInstance().format(sizeInMB);
                LOGGER.info("Buffer file (" + m_binFile.getAbsolutePath() 
                        + ") is " + size + "MB in size");
            } catch (IOException ioe) {
                throw new RuntimeException("Cannot close stream of file \"" 
                        + m_binFile.getName() + "\"", ioe); 
            }
        }
    } // close()
    
    /**
     * Called when the buffer is closed or when the in-memory content (i.e.
     * using m_list) is written to a file.
     * @param outStream The output stream to close (to add meta-data, e.g.).
     * @return The lookup table, will be assigned to m_shortCutsLookup when
     * called from {@link #close(DataTableSpec)}.
     * @throws IOException If that fails.
     */
    private Class<? extends DataCell>[] closeFile(
            final DCObjectOutputStream outStream) throws IOException {
        Class<? extends DataCell>[] shortCutsLookup = createShortCutArray();
        outStream.close();
        return shortCutsLookup;
    }
    
    /** Writes internals to the an output stream (using the xml scheme from
     * NodeSettings).
     * @param out To write to.
     * @param shortCutsLookup The lookup table of this buffer, generally it's
     * m_shortCutsLookup but it may be different when this buffer operates
     * in-memory (i.e. uses m_list) but is written to a destination file.
     * @throws IOException If that fails.
     */
    private void writeMetaToFile(final OutputStream out, 
            final Class<? extends DataCell>[] shortCutsLookup) 
            throws IOException {
        NodeSettings settings = new NodeSettings("Table Meta Information");
        NodeSettingsWO subSettings = 
            settings.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addString(CFG_VERSION, getVersion());
        subSettings.addInt(CFG_SIZE, size());
        // m_shortCutsLookup to string array, saved in config
        String[] cellClasses = new String[shortCutsLookup.length];
        for (int i = 0; i < shortCutsLookup.length; i++) {
            cellClasses[i] = shortCutsLookup[i].getName();
        }
        subSettings.addStringArray(CFG_CELL_CLASSES, cellClasses);
        // calls close (and hence closeEntry)
        settings.saveToXML(out);
    }
    
    /**
     * Reads meta information, that is row count, version, byte assignments.
     * @param metaIn To read from.
     * @throws IOException If reading fails.
     * @throws ClassNotFoundException If any of the classes can't be loaded.
     * @throws InvalidSettingsException If the internal structure is broken. 
     */
    @SuppressWarnings("unchecked") // cast with generics
    private void readMetaFromFile(final InputStream metaIn) 
    throws IOException, ClassNotFoundException, InvalidSettingsException {
        InputStream inStream = new BufferedInputStream(metaIn);
        try {
            NodeSettingsRO settings = NodeSettings.loadFromXML(inStream);
            NodeSettingsRO subSettings = 
                settings.getNodeSettings(CFG_INTERNAL_META);
            String version = subSettings.getString(CFG_VERSION);
            m_version = validateVersion(version);
            m_size = subSettings.getInt(CFG_SIZE);
            if (m_size < 0) {
                throw new IOException("Table size must not be < 0: " + m_size);
            }
            String[] cellClasses = subSettings.getStringArray(CFG_CELL_CLASSES);
            m_shortCutsLookup = new Class[cellClasses.length];
            for (int i = 0; i < cellClasses.length; i++) {
                Class cl = GlobalClassCreator.createClass(cellClasses[i]);
                if (!DataCell.class.isAssignableFrom(cl)) {
                    throw new InvalidSettingsException("No data cell class: \"" 
                            + cellClasses[i] + "\"");
                }
                m_shortCutsLookup[i] = cl;
            }
        } finally {
            inStream.close();
        }
    }
    
    /** Create the shortcut table, it translates m_typeShortCuts to 
     * m_shortCutsLookup. */
    @SuppressWarnings("unchecked") // no generics in array definiton
    private Class<? extends DataCell>[] createShortCutArray() {
        m_shortCutsLookup = new Class[m_typeShortCuts.size()];
        for (Map.Entry<Class<? extends DataCell>, Byte> e 
                : m_typeShortCuts.entrySet()) {
            byte shortCut = e.getValue();
            Class<? extends DataCell> type = e.getKey();
            m_shortCutsLookup[shortCut - BYTE_TYPE_START] = type;
        }
        return m_shortCutsLookup;
    }
    
    /** Does the buffer use a file?
     * @return true If it does.
     */
    boolean usesOutFile() {
        return m_binFile != null;
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
     * is called from <code>addRow(DataRow)</code>.
     * @throws IOException If an IO error occurs while writing to the file.
     */
    private void writeRow(final DataRow row, 
            final DCObjectOutputStream outStream) throws IOException {
        RowKey id = row.getKey();
        writeRowKey(id, outStream);
        for (int i = 0; i < row.getNumCells(); i++) {
            DataCell cell = row.getCell(i);
            writeDataCell(cell, outStream);
        }
        outStream.writeChar(ROW_SEPARATOR);
        outStream.reset();
    }
    
    /** Writes the row key to the out stream. This method is overridden in
     * {@link NoKeyBuffer} in order to skip the row key. 
     * @param key The key to write.
     * @param outStream To write to.
     * @throws IOException If that fails.
     */
    void writeRowKey(final RowKey key, 
            final DCObjectOutputStream outStream) throws IOException {
        DataCell id = key.getId();
        writeDataCell(id, outStream);
    }
    
    /** Reads a row key from a string. Is overridden in {@link NoKeyBuffer} 
     * to return always the same key.
     * @param inStream To read from
     * @return The row key as read right from the stream.
     * @throws IOException If that fails.
     */
    RowKey readRowKey(final DCObjectInputStream inStream) 
        throws IOException {
        DataCell id = readDataCell(inStream);
        return new RowKey(id);
    }
    
    /** Writes a data cell to the outStream.
     * @param cell The cell to write.
     * @param outStream To write to.
     * @throws IOException
     */
    private void writeDataCell(final DataCell cell, 
            final DCObjectOutputStream outStream) throws IOException {
        if (cell.isMissing()) {
            outStream.writeByte(BYTE_TYPE_MISSING);
            return;
        }
        Class<? extends DataCell> cellClass = cell.getClass();
        @SuppressWarnings("unchecked")
        DataCellSerializer<DataCell> serializer = 
            (DataCellSerializer<DataCell>)DataType.getCellSerializer(
                    cellClass);
        Byte identifier = m_typeShortCuts.get(cellClass); 
        if (identifier == null) {
            int size = m_typeShortCuts.size();
            if (size + BYTE_TYPE_START > Byte.MAX_VALUE) {
                throw new IOException(
                "Too many different cell implemenations");
            }
            identifier = (byte)(size + BYTE_TYPE_START);
            m_typeShortCuts.put(cell.getClass(), identifier);
        }
        // DataCell is datacell-serializable
        if (serializer != null) {
            // memorize type if it does not exist
            outStream.writeByte(identifier);
            outStream.writeDataCell(serializer, cell);
        } else {
            outStream.writeByte(BYTE_TYPE_SERIALIZATION);
            outStream.writeByte(identifier);
            outStream.writeObject(cell);
        }
    }

    /* Reads a datacell from a string. */
    private DataCell readDataCell(final DCObjectInputStream inStream) 
        throws IOException {
        if (m_version == 100) {
            return readDataCellVersion100(inStream);
        }
        inStream.setCurrentClassLoader(null);
        byte identifier = inStream.readByte();
        if (identifier == BYTE_TYPE_MISSING) {
            return DataType.getMissingCell();
        }
        final boolean isSerialized = identifier == BYTE_TYPE_SERIALIZATION;
        if (isSerialized) {
            identifier = inStream.readByte();
        }
        Class<? extends DataCell> type = getTypeForChar(identifier);
        if (isSerialized) {
            try {
                ClassLoader cellLoader = type.getClassLoader();
                inStream.setCurrentClassLoader(cellLoader);
                return (DataCell)inStream.readObject();
            } catch (ClassNotFoundException cnfe) {
                IOException ioe = new IOException(cnfe.getMessage());
                ioe.initCause(cnfe);
                throw ioe;
            }
        } else {
            DataCellSerializer<? extends DataCell> serializer = 
                DataType.getCellSerializer(type);
            assert serializer != null;
            try {
                return inStream.readDataCell(serializer);
            } catch (IOException ioe) {
                LOGGER.warn("Unable to read cell from file.", ioe);
                return DataType.getMissingCell();
            }
        }
    }
    
    /** Backward compatibility: DataCells that are (java-) serialized are
     * not annotated with a byte identifying its type. We need that in the
     * future to make sure we use the right class loader.
     * @param inStream To read from.
     * @return The cell.
     * @throws IOException If fails.
     */
    private DataCell readDataCellVersion100(final DCObjectInputStream inStream)
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
            DataCellSerializer<? extends DataCell> serializer =
                DataType.getCellSerializer(type);
            assert serializer != null;
            try {
                return inStream.readDataCell(serializer);
            } catch (IOException ioe) {
                LOGGER.debug("Unable to read cell from file.", ioe);
                return DataType.getMissingCell();
            }
        }
    } // readDataCellVersion100(DCObjectInputStream)
    
    private Class<? extends DataCell> getTypeForChar(final byte identifier) 
        throws IOException {
        int shortCutIndex = (byte)(identifier - BYTE_TYPE_START);
        if (shortCutIndex < 0 || shortCutIndex >= m_shortCutsLookup.length) {
            throw new IOException("Unknown shortcut byte '" + identifier + "'");
        }
        return m_shortCutsLookup[shortCutIndex];
    }
    
    /**
     * Creates short cut array and wraps the argument stream in a
     * DCObjectOutputStream.
     */
    private DCObjectOutputStream initOutFile(
            final OutputStream outStream) throws IOException {
        m_typeShortCuts = new HashMap<Class<? extends DataCell>, Byte>();
        return new DCObjectOutputStream(new GZIPOutputStream(outStream));
    }
    
    /**
     * Get a new <code>RowIterator</code>, traversing all rows that have been
     * added. Calling this method makes only sense when the buffer has been 
     * closed. However, no check is done (as it is available to package classes
     * only).
     * @return a new Iterator over all rows.
     */
    RowIterator iterator() {
        if (usesOutFile()) {
            return new FromFileIterator();
        } else {
            return new FromListIterator();
        }
    }
    
    /**
     * Method that's been called from the {@link ContainerTable} 
     * to save the content. It will add zip entries to the <code>zipOut</code>
     * argument and not close the output stream when done, allowing 
     * to add additional content elsewhere (for instance the 
     * <code>DataTableSpec</code>).
     * @param zipOut To write to.
     * @param exec For progress/cancel
     * @throws IOException If it fails to write to a file.
     * @throws CanceledExecutionException If canceled.
     * @see org.knime.core.node.BufferedDataTable.KnowsRowCountTable
     * #saveToFile(File, NodeSettingsWO, ExecutionMonitor)
     */
    void addToZipFile(final ZipOutputStream zipOut, 
            final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        if (m_spec == null) {
            throw new IOException("Can't save an open Buffer.");
        }
        // binary data is already deflated
        zipOut.setLevel(Deflater.NO_COMPRESSION);
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_DATA));
        Class<? extends DataCell>[] shortCutsLookup;
        if (!usesOutFile()) {
            DCObjectOutputStream outStream = 
                initOutFile(new NonClosableZipOutputStream(zipOut));
            int count = 1;
            for (DataRow row : m_list) {
                exec.setProgress(count / (double)size(), "Writing row " 
                        + count + " (\"" + row.getKey() + "\")");
                exec.checkCanceled();
                writeRow(row, outStream);
                count++;
            }
            shortCutsLookup = closeFile(outStream);
        } else {
            // no need for BufferedInputStream here as the copy method
            // does the buffering itself
            FileUtil.copy(new FileInputStream(m_binFile), zipOut);
            shortCutsLookup = m_shortCutsLookup;
        }
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_META));
        writeMetaToFile(
                new NonClosableZipOutputStream(zipOut), shortCutsLookup);
    }
    
    /** Deletes the file underlying this buffer.
     * @see Object#finalize()
     */
    @Override
    protected void finalize() {
        clear();
    }
    
    /**
     * Clears the temp file. Any subsequent iteration will fail!
     */
    void clear() {
        m_list = null;
        if (m_binFile != null) {
            for (WeakReference<FromFileIterator> w : m_openIteratorSet) {
                FromFileIterator f = w.get();
                if (f != null) {
                    f.clearIteratorInstance();
                }
            }
            boolean deleted = m_binFile.delete();
            if (!deleted) {
                // note: although all input streams are closed, the file
                // can't be deleted. If we call the gc, it works. No clue.
                // That only happens under windows!
 // http://forum.java.sun.com/thread.jspa?forumID=31&threadID=609458
                System.gc();
            }
            if (deleted || m_binFile.delete()) {
                LOGGER.debug("Deleted temp file \"" 
                        + m_binFile.getAbsolutePath() + "\"");
            } else {
                LOGGER.debug("Failed to delete temp file \"" 
                        + m_binFile.getAbsolutePath() + "\"");
            }
        }
        m_binFile = null;
    }
    
    /**
     * Iterator that traverses the out file on the disk and deserializes
     * the rows.
     */
    private class FromFileIterator extends RowIterator {
        
        private int m_pointer;
        private DCObjectInputStream m_inStream;
        
        /**
         * Inits the input stream.
         */
        FromFileIterator() {
            m_pointer = 0;
            try {
                BufferedInputStream bufferedStream =
                    new BufferedInputStream(new FileInputStream(m_binFile));
                InputStream in;
                if (m_version < 120) { // stream was not zipped in KNIME 1.1.x 
                    in = bufferedStream;
                } else {
                    in = new GZIPInputStream(bufferedStream);
                }
                m_inStream = new DCObjectInputStream(in);
                m_nrOpenInputStreams++;
                LOGGER.debug("Opening input stream on file \"" 
                        + m_binFile.getAbsolutePath() + "\", " 
                        + m_nrOpenInputStreams + " open streams");
            } catch (IOException ioe) {
                throw new RuntimeException("Cannot read file \"" 
                        + m_binFile.getName() + "\"", ioe);
            }
            m_openIteratorSet.add(new WeakReference<FromFileIterator>(this));
        }
        
        /** Get the name of the out file that this iterator works on. 
         * @return The name of the out file
         */
        public String getOutFileName() {
            return m_binFile.getAbsolutePath();
        }

        /**
         * @see org.knime.core.data.RowIterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            boolean hasNext = m_pointer < Buffer.this.m_size;
            if (!hasNext && (m_inStream != null)) {
                try {
                    m_inStream.close();
                    m_inStream = null;
                } catch (IOException ioe) {
                    LOGGER.warn("Unable to close stream from DataContainer: " 
                            + ioe.getMessage(), ioe);
                    throw new RuntimeException(ioe);
                }
            }
            return hasNext;
        }
        
        /**
         * @see org.knime.core.data.RowIterator#next()
         */
        @Override
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
                char eoRow = inStream.readChar();
                if (eoRow != ROW_SEPARATOR) {
                    throw new IOException("Exptected end of row character, " 
                            + "got '" + eoRow + "'");
                }
                return new DefaultRow(key, cells);
            } catch (Exception ioe) {
                throw new RuntimeException("Cannot read line "  
                    + (m_pointer + 1) + " from file \"" 
                        + m_binFile.getName() + "\"", ioe);
            } finally {
                m_pointer++;
            }
        }
        
        private synchronized void clearIteratorInstance() {
            m_pointer = Buffer.this.m_size; // mark it as end of file
            // already closed (clear has been called before)
            if (m_inStream == null) {
                return;
            }
            
            String closeMes = (m_binFile != null) ? "Closing input stream on \""
                + m_binFile.getAbsolutePath() + "\", " : ""; 
            try {
                m_inStream.close();
                m_nrOpenInputStreams--;
                LOGGER.debug(closeMes + m_nrOpenInputStreams + " remaining");
                m_inStream = null;
            } catch (IOException ioe) {
                LOGGER.debug(closeMes + "failed!", ioe);
            }
        }
        
        /**
         * @see java.lang.Object#finalize()
         */
        @Override
        protected void finalize() throws Throwable {
            /* This all relates much to bug #63: The temp files are not
             * deleted under windows. It seems that there are open streams
             * when the VM closes.
             */
            clearIteratorInstance();
        }
    }
    
    /**
     * Class wrapping the iterator of a java.util.List to a RowIterator.
     * This object is used when all rows fit in memory (no file).
     */
    private class FromListIterator extends RowIterator {
        
        private Iterator<DataRow> m_it = m_list.iterator();

        /**
         * @see org.knime.core.data.RowIterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return m_it.hasNext();
        }

        /**
         * @see org.knime.core.data.RowIterator#next()
         */
        @Override
        public DataRow next() {
            return m_it.next();
        }
        
    }
}
