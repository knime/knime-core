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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
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
 * the {@link DataContainer} that is returned by the container. It 
 * "centralizes" the IO operations.
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
    private static final String ZIP_ENTRY_DATA = "data.bin";
    
    /** Name of the zip entry containing the spec. */
    private static final String ZIP_ENTRY_SPEC = "spec.xml";

    /** Name of the zip entry containing the meta information (e.g. #rows). */
    private static final String ZIP_ENTRY_META = "meta.xml";
    
    /** Config entries when writing the meta information to the file,
     * this is a subconfig in meta.xml.
     */
    private static final String CFG_INTERNAL_META = "table.meta.internal";

    /** Config entries when writing the meta information to the file,
     * this is a subconfig in meta.xml for overridden classes.
     */
    private static final String CFG_ADDITIONAL_META = "table.meta.additional";
    
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
    
    /** Config entry: The spec of the table. */
    private static final String CFG_TABLESPEC = "table.spec";
    
    /** The version this container is able to read, may be an array in the 
     * future. */
    private static final String VERSION = "container_1.1.0";

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
    private int m_version = 110;

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
     * Creates new buffer with a given spec, and a max row count that may 
     * resize in memory.
     * 
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
        m_openIteratorSet = new HashSet<WeakReference<FromFileIterator>>();
        m_size = 0;
    }
    
    /** Creates new buffer that will immediately write to the given file.
     * 
     * <p>This constructor is used when data will be added using the addRow
     * method.
     * @param outFile The file to write to, will be created (or overwritten).
     * @throws IOException If opening file fails.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    Buffer(final File outFile) throws IOException {
        this(0);
        if (outFile == null) {
            throw new NullPointerException("Can't set null file!");
        }
        initOutFile(outFile);
    }
    
    /** Creates new buffer for <strong>reading</strong>. The file is
     * assumed to be a zip file as written by this class.
     * 
     * <p>This constructor is used when data will be read from a zip file.
     * @param inFile The file to read from.
     * @param ignored This argument is ignored. It serves to distinguish
     * from the other constructor.
     * @throws IOException If the header (the spec information) can't be read.
     */
    Buffer(final File inFile, final boolean ignored) throws IOException {
        assert ignored == ignored;
        // copy the file to temp first, we will delete it when done
        LOGGER.debug("Copying \"" + inFile.getAbsolutePath() 
                + "\" to temp directory.");
        m_outFile = DataContainer.createTempFile();
        m_hasCreatedTempFile = true;
        FileUtil.copy(inFile, m_outFile);
        m_maxRowsInMem = 0;
        ZipFile zipFile = new ZipFile(m_outFile);
        String errorFile = "";
        try {
            errorFile = "spec";
            m_spec = readSpecFromFile(zipFile);
            errorFile = "meta";
            readMetaFromFile(zipFile);
        } catch (ClassNotFoundException cnfe) {
            IOException ioe = new IOException(
                    "Unable to read " + errorFile + "from zip file \""
                    + inFile.getAbsolutePath() + "\"");
            ioe.initCause(cnfe);
            throw ioe;
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException(
                    "Unable to read " + errorFile + "from zip file \""
                    + inFile.getAbsolutePath() + "\"");
            ioe.initCause(ise);
            throw ioe;
        }
        // just check if data is present!
        InputStream dataInput = zipFile.getInputStream(
                new ZipEntry(ZIP_ENTRY_DATA));
        if (dataInput == null) {
            throw new IOException("Invalid file: No data entry");
        }
        m_openIteratorSet = new HashSet<WeakReference<FromFileIterator>>();
    }
    
    /** Get the version string to write to the meta file, may be overridden.
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
                    + "of KNIME (1.0.0), reading anyway.");
            return 100;
        }
        if (VERSION.equals(version)) {
            return 110;
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
            // Write spec.
            // we push the underlying stream forward; need to make
            // sure that m_outStream is done with everything.
            m_outStream.flush();
            ZipOutputStream zipOut = 
                (ZipOutputStream)m_outStream.getUnderylingStream();
            zipOut.closeEntry();
            // both method will create their own zip entry and close 
            // it afterwards
            writeSpecToFile(zipOut);
            writeMetaToFile(zipOut);
            zipOut.close();
            double sizeInMB = m_outFile.length() / (double)(1 << 20);
            String size = NumberFormat.getInstance().format(sizeInMB);
            LOGGER.info("Buffer file (" + m_outFile.getAbsolutePath() 
                    + ") is " + size + "MB in size");
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot close stream of file \"" 
                    + m_outFile.getName() + "\"", ioe); 
        }
    } // close()
    
    /** Called when buffer is closed and we are writing to a customized
     * file. This method will add a zip entry containing the spec.
     * @param outStream The stream to write to.
     * @throws IOException If that fails.
     */
    void writeSpecToFile(final ZipOutputStream outStream) throws IOException {
        outStream.putNextEntry(new ZipEntry(ZIP_ENTRY_SPEC));
        NodeSettings settings = new NodeSettings("Table Spec");
        NodeSettingsWO specSettings = settings.addNodeSettings(CFG_TABLESPEC);
        m_spec.save(specSettings);
        // will only close the zip entry, not the entire stream.
        NonClosableZipOutputStream nonClosable = 
            new NonClosableZipOutputStream(outStream);
        // calls close (and hence closeEntry)
        settings.saveToXML(nonClosable);
    }
    
    /** Writes internals to the zip output stream by adding a zip entry and 
     * writing to it.
     * @param zipOut To write to.
     * @throws IOException If that fails.
     */
    void writeMetaToFile(final ZipOutputStream zipOut) throws IOException {
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_META));
        NodeSettings settings = new NodeSettings("Table Meta Information");
        NodeSettingsWO subSettings = 
            settings.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addString(CFG_VERSION, getVersion());
        subSettings.addInt(CFG_SIZE, size());
        // m_shortCutsLookup to string array, saved in config
        String[] cellClasses = new String[m_shortCutsLookup.length];
        for (int i = 0; i < m_shortCutsLookup.length; i++) {
            cellClasses[i] = m_shortCutsLookup[i].getName();
        }
        subSettings.addStringArray(CFG_CELL_CLASSES, cellClasses);
        NodeSettingsWO addSubSettings = 
            settings.addNodeSettings(CFG_ADDITIONAL_META); 
        // allow subclasses to do private savings.
        addMetaForSaving(addSubSettings);
        // will only close the zip entry, not the entire stream.
        NonClosableZipOutputStream nonClosable = 
            new NonClosableZipOutputStream(zipOut);
        // calls close (and hence closeEntry)
        settings.saveToXML(nonClosable);
    }
    
    /** Intended for subclass NoKeyBuffer to add its private information.
     * @param settings Where to add meta information.
     */
    void addMetaForSaving(final NodeSettingsWO settings) {
        // checkstyle complains otherwise.
        assert settings == settings;
    }
    
    /**
     * Reads the zip entry containing the spec.
     * @param zipFile To read from.
     * @return The spec as read from file.
     * @throws IOException If reading fails.
     * @throws InvalidSettingsException If the internal structure is broken. 
     */
    @SuppressWarnings("unchecked") // cast with generics
    DataTableSpec readSpecFromFile(final ZipFile zipFile) 
            throws IOException, InvalidSettingsException {
        InputStream specInput = zipFile.getInputStream(
                new ZipEntry(ZIP_ENTRY_SPEC));
        if (specInput == null) {
            throw new IOException("Invalid file: No spec information");
        }
        InputStream inStream = new BufferedInputStream(specInput);
        try {
            NodeSettingsRO settings = NodeSettings.loadFromXML(inStream);
            NodeSettingsRO specSettings = 
                settings.getNodeSettings(CFG_TABLESPEC);
            return DataTableSpec.load(specSettings);
        } finally {
            inStream.close();
        }
    }
    
    /**
     * Reads the zip entry containing the meta information.
     * @param zipFile To read from.
     * @throws IOException If reading fails.
     * @throws ClassNotFoundException If any of the classes can't be loaded.
     * @throws InvalidSettingsException If the internal structure is broken. 
     */
    @SuppressWarnings("unchecked") // cast with generics
    void readMetaFromFile(final ZipFile zipFile) 
    throws IOException, ClassNotFoundException, InvalidSettingsException {
        InputStream metaIn = zipFile.getInputStream(
                new ZipEntry(ZIP_ENTRY_META));
        if (metaIn == null) {
            throw new IOException("Invalid file: No meta information");
        }
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
            readMetaFromSaving(settings.getNodeSettings(CFG_ADDITIONAL_META));
        } finally {
            inStream.close();
        }
    }
    
    /** Subclasses will override this and read the internals. This method
     * is called in the constructor (not advisable in general, but hard to 
     * avoid here.)
     * @param settings To read from.
     * @throws InvalidSettingsException If that fails.
     */
    void readMetaFromSaving(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        // Checksyle is really rigid with regard to unused variables. This
        // is dirty, yes.
        assert settings == settings;
        if (false) {
            throw new InvalidSettingsException("");
        }
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
    
    /** Get reference to the file that we use or <code>null</code> if we keep
     * the data in main memory.
     * @return The file we use.
     */
    File getFile() {
        return m_outFile;
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
    
    /** Writes the row key to the out stream. This method is overridden in
     * NoKeyBuffer in order to skip the row key. 
     * @param key The key to write.
     * @throws IOException If that fails.
     */
    void writeRowKey(final RowKey key) throws IOException {
        DataCell id = key.getId();
        writeDataCell(id);
    }
    
    /** Reads a row key from a string. Is overridden in NoKeyBuffer to return
     * always the same key.
     * @param inStream To read from
     * @return The row key as read right from the stream.
     * @throws IOException If that fails.
     */
    RowKey readRowKey(final DCObjectInputStream inStream) 
        throws IOException {
        DataCell id = readDataCell(inStream);
        return new RowKey(id);
    }
    
    /* Writes a data cell to the m_outStream. */
    private void writeDataCell(final DataCell cell) throws IOException {
        if (cell.isMissing()) {
            m_outStream.writeByte(BYTE_TYPE_MISSING);
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
            m_outStream.writeByte(identifier);
            m_outStream.writeDataCell(serializer, cell);
        } else {
            m_outStream.writeByte(BYTE_TYPE_SERIALIZATION);
            m_outStream.writeByte(identifier);
            m_outStream.writeObject(cell);
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
    }
    
    private Class<? extends DataCell> getTypeForChar(final byte identifier) 
        throws IOException {
        int shortCutIndex = (byte)(identifier - BYTE_TYPE_START);
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
            m_outFile = DataContainer.createTempFile();
            m_hasCreatedTempFile = true;
        } else {
            m_outFile = outFile;
            m_hasCreatedTempFile = false;
        }
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(m_outFile)));
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_DATA));
        OPENBUFFERS.add(new WeakReference<Buffer>(this));
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
    /**
     * Delegate method that's been called from the container table to save
     * the internals.
     * @param f To write to.
     * @param exec For progress/cancel
     * @throws IOException If it fails to write to a file.
     * @throws CanceledExecutionException If canceled.
     * @see org.knime.core.node.BufferedDataTable.KnowsRowCountTable
     * #saveToFile(File, NodeSettingsWO, ExecutionMonitor)
     */
    void saveToFile(final File f, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        if (!usesOutFile()) {
            initOutFile(null);
            close(m_spec); // will also use this file for subsequent iterations
        } 
        FileUtil.copy(m_outFile, f, exec);
    }
    
    /** Deletes the file underlying this buffer.
     * @see Object#finalize()
     */
    @Override
    protected void finalize() {
        if (m_hasCreatedTempFile) {
            clear();
        }
    }
    
    /**
     * Clears the temp file. Any subsequent iteration will fail!
     */
    void clear() {
        if (m_outFile != null) {
            for (WeakReference<FromFileIterator> w : m_openIteratorSet) {
                FromFileIterator f = w.get();
                if (f != null) {
                    f.clear();
                }
            }
            boolean deleted = m_outFile.delete();
            if (!deleted) {
                // note: altough all input streams are closed, the file
                // can't be deleted. If we call the gc, it works. No clue.
                // That only happens under windows!
 // http://forum.java.sun.com/thread.jspa?forumID=31&threadID=609458
                System.gc();
            }
            if (deleted || m_outFile.delete()) {
                LOGGER.debug("Deleted temp file \"" 
                        + m_outFile.getAbsolutePath() + "\"");
            } else {
                LOGGER.debug("Failed to delete temp file \"" 
                        + m_outFile.getAbsolutePath() + "\"");
            }
        }
        m_outFile = null;
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
                // this fixes bug #775: ZipFile has a bug if the file to read
                // is too large
                ZipInputStream zipIn = new ZipInputStream(
                       new BufferedInputStream(new FileInputStream(m_outFile)));
                ZipEntry zipEntry = null;
                do {
                    zipEntry = zipIn.getNextEntry();
                    if (zipEntry == null) {
                        throw new RuntimeException("Expected ZIP file entry '"
                                + ZIP_ENTRY_DATA + "' not found.");
                    }
                } while (!zipEntry.getName().equals(ZIP_ENTRY_DATA));
                
//                ZipFile zipFile = new ZipFile(m_outFile, ZipFile.OPEN_READ);
//                BufferedInputStream zipIn = new BufferedInputStream(
//                        zipFile.getInputStream(new ZipEntry(ZIP_ENTRY_DATA)));
                
                m_inStream = new DCObjectInputStream(zipIn);
                m_nrOpenInputStreams++;
                LOGGER.debug("Opening input stream on file \"" 
                        + m_outFile.getAbsolutePath() + "\", " 
                        + m_nrOpenInputStreams + " open streams");
            } catch (IOException ioe) {
                throw new RuntimeException("Cannot read file \"" 
                        + m_outFile.getName() + "\"", ioe);
            }
            m_openIteratorSet.add(new WeakReference<FromFileIterator>(this));
        }
        
        /** Get the name of the out file that this iterator works on. 
         * @return The name of the out file
         */
        public String getOutFileName() {
            return m_outFile.getAbsolutePath();
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
                        + m_outFile.getName() + "\"", ioe);
            } finally {
                m_pointer++;
            }
        }
        
        private synchronized void clear() {
            m_pointer = Buffer.this.m_size; // mark it as end of file
            // already closed (clear has been called before)
            if (m_inStream == null) {
                return;
            }
            
            String closeMes = (m_outFile != null) ? "Closing input stream on \""
                + m_outFile.getAbsolutePath() + "\", " : ""; 
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
            clear();
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
    
    /** Class that overrides the close method and just calls closeEntry on the
     * reference zip output. All other methods delegate directly.
     */
    private static class NonClosableZipOutputStream extends OutputStream {
        private final ZipOutputStream m_zipOut;
        
        /** Inits object, references argument.
         * @param zipOut The reference.
         */
        public NonClosableZipOutputStream(final ZipOutputStream zipOut) {
            m_zipOut = zipOut;
        }
        /**
         * @see java.io.OutputStream#close()
         */
        @Override
        public void close() throws IOException {
            m_zipOut.closeEntry();
        }
        /**
         * @see java.io.OutputStream#flush()
         */
        @Override
        public void flush() throws IOException {
            m_zipOut.flush();
        }
        /**
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        @Override
        public void write(final byte[] b, final int off, final int len) 
            throws IOException {
            m_zipOut.write(b, off, len);
        }
        /**
         * @see java.io.OutputStream#write(byte[])
         */
        @Override
        public void write(final byte[] b) throws IOException {
            m_zipOut.write(b);
        }
        /**
         * @see java.io.OutputStream#write(int)
         */
        @Override
        public void write(final int b) throws IOException {
            m_zipOut.write(b);
        }
    }

}
