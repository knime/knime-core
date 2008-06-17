/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.FileUtil;
import org.knime.core.util.MutableInteger;

/**
 * A buffer writes the rows from a {@link DataContainer} to a file.
 * This class serves as connector between the {@link DataContainer} and
 * the {@link org.knime.core.data.DataTable} that is returned by the container.
 * It "centralizes" the IO operations.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
class Buffer {

    /** Static field to enable/disable the usage of a GZipInput/OutpuStream
     * when writing the binary data. This option defaults to true, meaning
     * that we read/write to a compressed stream.
     *
     * Note: Changing this parameter makes it impossible to read workflows
     * written previously. It's only used for internal testing purposes.
     */
    private static final boolean IS_USE_GZIP = true;

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(Buffer.class);

    /** Contains the information whether or not certain blob cell
     * implementations shall be compressed when saved. This information
     * is retrieved from the field BlobDataCell#USE_COMPRESSION.
     */
    private static final Map<Class<? extends BlobDataCell>, Boolean>
        BLOB_COMPRESS_MAP =
            new HashMap<Class<? extends BlobDataCell>, Boolean>();

    /** The char for missing cells. */
    private static final byte BYTE_TYPE_MISSING = Byte.MIN_VALUE;

    /** The char for cell whose type needs serialization. */
    private static final byte BYTE_TYPE_SERIALIZATION = BYTE_TYPE_MISSING + 1;

    /** The first used char for the map char --> type. */
    private static final byte BYTE_TYPE_START = BYTE_TYPE_MISSING + 2;

    /** Separator for different rows. */
    private static final byte BYTE_ROW_SEPARATOR = BYTE_TYPE_MISSING + 3;
    
    /** Name of the zip entry containing the data. */
    static final String ZIP_ENTRY_DATA = "data.bin";

    /** Name of the zip entry containing the blob files (directory). */
    static final String ZIP_ENTRY_BLOBS = "blobs";

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

    /** Config entry whether or not this buffer contains blobs. */
    private static final String CFG_CONTAINS_BLOBS = "container.contains.blobs";

    /** Config entry whether this buffer resides in memory. */
    private static final String CFG_IS_IN_MEMORY = "container.inmemory";

    /** Config entry: internal buffer ID. */
    private static final String CFG_BUFFER_ID = "container.id";

    /** Config entries when writing the spec to the file (uses NodeSettings
     * object, which uses key-value pairs. Here: size of the table (#rows).
     */
    private static final String CFG_SIZE = "table.size";

    /** Config entry: Array of the used DataCell classes; storing the class
     * names as strings, see m_shortCutsLookup. */
    private static final String CFG_CELL_CLASSES = "table.datacell.classes";

    /** Current version string. */
    private static final String VERSION = "container_5";

    /** The version number corresponding to VERSION. */
    private static final int IVERSION = 5;

    private static final HashMap<String, Integer> COMPATIBILITY_MAP;

    static {
        COMPATIBILITY_MAP = new HashMap<String, Integer>();
        COMPATIBILITY_MAP.put("container_1.0.0", 1); // version 1.00
        COMPATIBILITY_MAP.put("container_1.1.0", 2); // version 1.1.x
        COMPATIBILITY_MAP.put("container_1.2.0", 3); // never released
        COMPATIBILITY_MAP.put("container_4", 4); // version 1.2.x - 1.3.x
        COMPATIBILITY_MAP.put(VERSION, IVERSION);    // version 2.0
    }

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

    /** Number of dirs/files per directory when blobs are saved. */
    private static final int BLOB_ENTRIES_PER_DIRECTORY = 1000;

    /** Is executing the shutdown hook? If so, no logging is done, bug
     * fix #862. */
    private static boolean isExecutingShutdownHook = false;

    /** Dummy object for the file iterator map. */
    private static final Object DUMMY = new Object();


    /** Adds a shutdown hook to the runtime that closes all open input streams
     * @see #OPENBUFFERS
     */
    static {
        try {
            Thread hook = new Thread() {
                @Override
                public void run() {
                    isExecutingShutdownHook = true;
                    for (WeakReference<Buffer> ref : OPENBUFFERS) {
                        Buffer it = ref.get();
                        if (it != null) {
                            it.clear();
                        }
                    }
                    DeleteInBackgroundThread.waitUntilFinished();
                }
            };
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (Exception e) {
            LOGGER.warn("Unable to add shutdown hook to delete temp files", e);
        }
    }

    private static boolean isUseCompressionForBlobs(
            final Class<? extends BlobDataCell> cl) {
        Boolean result = BLOB_COMPRESS_MAP.get(cl);
        if (result != null) {
            return result;
        }
        Exception exception = null;
        try {
            // Java will fetch a static field that is public, if you
            // declare it to be non-static or give it the wrong scope, it
            // automatically retrieves the static field from a super
            // class/interface. If this field has the wrong type, a coding
            // problem is reported.
            Field typeField = cl.getField("USE_COMPRESSION");
            Object typeObject = typeField.get(null);
            result = (Boolean)typeObject;
            if (result == null) {
                throw new NullPointerException("USE_COMPRESSION is null.");
            }
        } catch (NoSuchFieldException nsfe) {
            exception = nsfe;
        } catch (NullPointerException npe) {
            exception = npe;
        } catch (IllegalAccessException iae) {
            exception = iae;
        } catch (ClassCastException cce) {
            exception = cce;
        }
        if (exception != null) {
            LOGGER.coding("BlobDataCell interface \"" + cl.getSimpleName()
                    + "\" seems to have a problem with the static field "
                    + "\"USE_COMPRESSION\"", exception);
            // fall back - no meta information available
            result = false;
        }
        BLOB_COMPRESS_MAP.put(cl, result);
        return result;
    }

    /** the file to write to. */
    private File m_binFile;

    /** The directory where blob cells are stored or null if none available. */
    private File m_blobDir;

    /** true if any row contained in this buffer contains blob cells. */
    private boolean m_containsBlobs;

    /** The ID of this buffer. Used for blob serialization. This field is -1
     * when this buffer is not used within a BufferedDataTable (i.e. for
     * node outport serialization).
     * @see DataContainer#createInternalBufferID()
     */
    private final int m_bufferID;

    /** A map with other buffers that may have written certain blob cells.
     * We reference them by using the bufferID that is written to the file.
     * This member is a reference to the (WorkflowManager-)global table
     * repository. */
    private final Map<Integer, ContainerTable> m_globalRepository;

    /** A map with other buffers that may have written certain blob cells.
     * We reference them by using the bufferID that is written to the file.
     * This temporary repository is exists only while a node is being
     * executed. It is only important while writing to this buffer. */
    private Map<Integer, ContainerTable> m_localRepository;

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
    private List<BlobSupportDataRow> m_list;

    private int[] m_indicesOfBlobInColumns;

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
    private final WeakHashMap<FromFileIterator, Object> m_openIteratorSet;

    /** The iterator that is used to read the content back into memory.
     * This instance is used after the workflow is restored from disk. */
    private FromFileIterator m_backIntoMemoryIterator;

    /**
     * The version of the file we are reading (if initiated with
     * Buffer(File, boolean). Used to remember when we need to read a file
     * which has been written with another version of the Buffer, i.e. to
     * provide backward compatibility.
     */
    private int m_version = IVERSION;
    
    /*** Hash used to reduce the overhead of reading a blob cell over and 
     * over again. Useful in cases where a blob is added multiple times to 
     * a table... the iterator will read the blob address, treat it as unseen
     * an then ask the owning Buffer to restore the blob. */
    private BlobLRUCache m_blobLRUCache;

    /**
     * Creates new buffer for <strong>writing</strong>. It has assigned a
     * given spec, and a max row count that may resize in memory.
     *
     * @param maxRowsInMemory Maximum numbers of rows that are kept in memory
     *        until they will be subsequent written to the temp file. (0 to
     *        write immediately to a file)
     * @param globalRep Table repository for blob (de)serialization (read only).
     * @param localRep Local table repository for blob (de)serialization.
     * @param bufferID The id of this buffer used for blob (de)serialization.
     */
    Buffer(final int maxRowsInMemory, final int bufferID,
            final Map<Integer, ContainerTable> globalRep,
            final Map<Integer, ContainerTable> localRep) {
        assert (maxRowsInMemory >= 0);
        m_maxRowsInMem = maxRowsInMemory;
        m_list = new ArrayList<BlobSupportDataRow>();
        m_openIteratorSet = new WeakHashMap<FromFileIterator, Object>();
        m_size = 0;
        m_bufferID = bufferID;
        m_globalRepository = globalRep;
        m_localRepository = localRep;
    }

    /** Creates new buffer for <strong>reading</strong>. The
     * <code>binFile</code> is the binary file as written by this class, which
     * will be deleted when this buffer is cleared or finalized.
     *
     * @param binFile The binary file to read from (will be deleted on exit).
     * @param blobDir temp directory containing blobs (may be null).
     * @param spec The data table spec to which the this buffer complies to.
     * @param metaIn An input stream from which this constructor reads the
     * meta information (e.g. which byte encodes which DataCell).
     * @param bufferID The id of this buffer used for blob (de)serialization.
     * @param tblRep Table repository for blob (de)serialization.
     * @throws IOException If the header (the spec information) can't be read.
     */
    Buffer(final File binFile, final File blobDir, final DataTableSpec spec,
            final InputStream metaIn, final int bufferID,
            final Map<Integer, ContainerTable> tblRep) throws IOException {
        // just check if data is present!
        if (binFile == null || !binFile.canRead() || !binFile.isFile()) {
            throw new IOException("Unable to read from file: " + binFile);
        }
        m_spec = spec;
        m_binFile = binFile;
        m_blobDir = blobDir;
        m_bufferID = bufferID;
        m_globalRepository = tblRep;
        m_openIteratorSet = new WeakHashMap<FromFileIterator, Object>();
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
        Integer iVersion = COMPATIBILITY_MAP.get(version);
        if (iVersion == null) {
            throw new IOException("Unsupported version: \"" + version + "\"");
        }
        if (iVersion < IVERSION) {
            LOGGER.debug("Table has been written with a previous version of "
                    + "KNIME (\"" + version + "\"), using compatibility mode.");
        }
        return iVersion;
    }

    /**
     * Adds a row to the buffer. The rows structure is not validated against
     * the table spec that was given in the constructor. This should have been
     * done in the caller class <code>DataContainer</code>.
     * @param r The row to be added.
     */
    @SuppressWarnings("unchecked")
    public void addRow(final DataRow r) {
        try {
            BlobSupportDataRow row = saveBlobs(r);
            m_list.add(row);
            incrementSize();
            // if threshold exceeded, write all rows to disk
            if (m_list.size() > m_maxRowsInMem) {
                ensureTempFileExists();
                if (m_outStream == null) {
                    Buffer.onFileCreated();
                    m_outStream = initOutFile(new BufferedOutputStream(
                            new FileOutputStream(m_binFile)));
                }
                for (BlobSupportDataRow rowInList : m_list) {
                    writeRow(rowInList, m_outStream);
                }
                m_list.clear();
                // write next rows directly to file
                m_maxRowsInMem = 0;
            }
        } catch (Throwable e) {
            if (!(e instanceof IOException)) {
                LOGGER.coding("Writing cells to temporary buffer must not "
                        + "throw " + e.getClass().getSimpleName());
            }
            StringBuilder builder =
                new StringBuilder("Error while writing to buffer");
            if (m_binFile != null) {
                builder.append(", failed to write to file \"");
                builder.append(m_binFile.getName());
                builder.append("\"");
            }
            builder.append(": ");
            String message = e.getMessage();
            if (message == null || message.length() == 0) {
                message = "No details available";
            }
            builder.append(message);
            throw new RuntimeException(builder.toString(), e);
        }
    } // addRow(DataRow)

    private BlobSupportDataRow saveBlobs(final DataRow row) throws IOException {
        final int cellCount = row.getNumCells();
        DataCell[] cellCopies = null;
        if (!(row instanceof BlobSupportDataRow)) {
            cellCopies = new DataCell[cellCount];
            for (int i = 0; i < cellCount; i++) {
                cellCopies[i] = row.getCell(i);
            }
        }
        // take ownership of unassigned blob cells (if any)
        for (int col = 0; col < cellCount; col++) {
            DataCell cell = row instanceof BlobSupportDataRow
                ? ((BlobSupportDataRow)row).getRawCell(col)
                        : cellCopies[col];
            boolean isWrapperCell = cell instanceof BlobWrapperDataCell;
            boolean mustChangeRow = false;
            BlobAddress ad;
            final Class<? extends BlobDataCell> cl;
            BlobWrapperDataCell wc;
            if (isWrapperCell) {
                assert row instanceof BlobSupportDataRow;
                wc = (BlobWrapperDataCell)cell;
                ad = wc.getAddress();
                cl = wc.getBlobClass();
                assert ad != null : "Blob address must not be null in wrapper";
            } else if (cell instanceof BlobDataCell) {
                cl = ((BlobDataCell)cell).getClass();
                ad = ((BlobDataCell)cell).getBlobAddress();
            } else {
                continue; // ordinary cell (e.g. double cell)
            }
            final Buffer ownerBuffer;
            if (ad != null) {
                // has blob been added to this buffer in a preceding row?
                // (and this is not an ordinary buffer (but a BufferedDataCont.)
                if (ad.getBufferID() == getBufferID() && getBufferID() != -1) {
                    ownerBuffer = this;
                } else {
                    // table that's been created somewhere in the workflow
                    ContainerTable t = m_globalRepository.get(ad.getBufferID());
                    ownerBuffer = t != null ? t.getBuffer() : null;
                }
            } else {
                ownerBuffer = null;
            }
            if (ownerBuffer == null) {
                // need to set ownership if this blob was not assigned yet
                // or has been assigned to an unlinked (i.e. local) buffer
                boolean isCompress = ad != null ? ad.isUseCompression()
                        : isUseCompressionForBlobs(cl);
                BlobAddress rewrite =
                    new BlobAddress(m_bufferID, col, isCompress);
                if (ad == null) {
                    ((BlobDataCell)cell).setBlobAddress(rewrite);
                    ad = rewrite;
                }
                if (m_indicesOfBlobInColumns == null) {
                    m_indicesOfBlobInColumns = new int[cellCount];
                }
                ContainerTable b = m_localRepository.get(ad.getBufferID());
                if (b != null) {
                    int indexBlobInCol = m_indicesOfBlobInColumns[col]++;
                    rewrite.setIndexOfBlobInColumn(indexBlobInCol);
                    File source = b.getBuffer().getBlobFile(
                            ad.getIndexOfBlobInColumn(),
                            ad.getColumn(), false, ad.isUseCompression());
                    File dest = getBlobFile(
                            indexBlobInCol, col, true, ad.isUseCompression());
                    FileUtil.copy(source, dest);
                } else {
                    BlobDataCell bc;
                    if (isWrapperCell) {
                        bc = ((BlobWrapperDataCell)cell).getCell();
                    } else {
                        bc = (BlobDataCell)cell;
                    }
                    writeBlobDataCell(
                            bc, rewrite, getSerializerForDataCell(cl));
                }
                wc = new BlobWrapperDataCell(this, rewrite, cl);
                mustChangeRow = true;
            } else {
                // blob has been saved in one of the predecessor nodes
                if (isWrapperCell) {
                    wc = (BlobWrapperDataCell)cell;
                } else {
                    mustChangeRow = true;
                    wc = new BlobWrapperDataCell(ownerBuffer, ad, cl);
                }
            }
            if (mustChangeRow) {
                m_containsBlobs = true;
                if (cellCopies == null) {
                    cellCopies = new DataCell[cellCount];
                    for (int i = 0; i < cellCount; i++) {
                        cellCopies[i] = ((BlobSupportDataRow)row).getRawCell(i);
                    }
                }
                cellCopies[col] = wc;
            }
        }
        return cellCopies == null ? (BlobSupportDataRow)row
                : new BlobSupportDataRow(row.getKey(), cellCopies);
    }

    /** Creates temp file (m_binFile) and adds this buffer to shutdown hook. */
    private void ensureTempFileExists() throws IOException {
        if (m_binFile == null) {
            m_binFile = DataContainer.createTempFile();
            OPENBUFFERS.add(new WeakReference<Buffer>(this));
        }
    }

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
        assert spec != null : "Buffer is not open.";
        m_spec = spec;
        // everything is in the list, i.e. in memory
        if (m_outStream == null) {
            // disallow modification
            List<BlobSupportDataRow> newList =
                Collections.unmodifiableList(m_list);
            m_list = newList;
        } else {
            try {
                assert (m_list.isEmpty()) : "In-Memory list is not empty.";
                m_shortCutsLookup = closeFile(m_outStream);
                m_typeShortCuts = null; // garbage
                m_list = null;
                double sizeInMB = m_binFile.length() / (double)(1 << 20);
                String size = NumberFormat.getInstance().format(sizeInMB);
                LOGGER.debug("Buffer file (" + m_binFile.getAbsolutePath()
                        + ") is " + size + "MB in size");
            } catch (IOException ioe) {
                throw new RuntimeException("Cannot close stream of file \""
                        + m_binFile.getName() + "\"", ioe);
            }
        }
        m_localRepository = null;
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
        subSettings.addBoolean(CFG_CONTAINS_BLOBS, m_containsBlobs);
        subSettings.addBoolean(CFG_IS_IN_MEMORY, !usesOutFile());
        subSettings.addInt(CFG_BUFFER_ID, m_bufferID);
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
            m_containsBlobs = false;
            if (m_version >= 4) { // no blobs in version 1.1.x
                m_containsBlobs = subSettings.getBoolean(CFG_CONTAINS_BLOBS);
                int bufferID = subSettings.getInt(CFG_BUFFER_ID);
                // the bufferIDs may be different in cases when an 1.0.0 table
                // was read, then converted to a new version (done by
                // addToZipFile) and saved. Reading these tables will have a
                // bufferID of -1. 1.0.0 contain no blobs, so that's ok.
                if (m_containsBlobs && bufferID != m_bufferID) {
                    LOGGER.error("Table's buffer id is different from what has"
                        + " been passed in constructor (" + bufferID + " vs. "
                        + m_bufferID + "), unpredictable errors may occur");
                }
            }
            if (m_version >= 5) { // no back into memory option prior 2.0 
                if (subSettings.getBoolean(CFG_IS_IN_MEMORY)) {
                    restoreIntoMemory();
                }
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
    @SuppressWarnings("unchecked") // no generics in array definition
    private Class<? extends DataCell>[] createShortCutArray() {
        // unreported bug fix: NPE when the table only contains missing values.
        if (m_typeShortCuts == null) {
            m_typeShortCuts = new HashMap<Class<? extends DataCell>, Byte>();
        }
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
        return m_list == null;
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

    /** Restore content of this buffer into main memory (using a collection
     * implementation). The restoring will be performed with the next iteration.
     */
    final void restoreIntoMemory() {
        if (usesOutFile()) {
            m_list = new ArrayList<BlobSupportDataRow>(size());
            m_backIntoMemoryIterator = new FromFileIterator();
        }
    }

    /** Get reference to the table repository that this buffer was initially
     * instantiated with. Used for blob reading/writing.
     * @return (Worflow-) global table repository.
     */
    Map<Integer, ContainerTable> getGlobalRepository() {
        return m_globalRepository;
    }

    /** Get reference to the local table repository that this buffer was
     * initially instantiated with. Used for blob reading/writing. This
     * may be null.
     * @return (Worflow-) global table repository.
     */
    Map<Integer, ContainerTable> getLocalRepository() {
        return m_localRepository;
    }

    /**
     * Serializes a row to the output stream. This method
     * is called from <code>addRow(DataRow)</code>.
     * @throws IOException If an IO error occurs while writing to the file.
     */
    private void writeRow(final BlobSupportDataRow row,
            final DCObjectOutputStream outStream) throws IOException {
        RowKey id = row.getKey();
        writeRowKey(id, outStream);
        for (int i = 0; i < row.getNumCells(); i++) {
            DataCell cell = row.getRawCell(i);
            if (m_indicesOfBlobInColumns == null
                    && cell instanceof BlobDataCell) {
                m_indicesOfBlobInColumns = new int[row.getNumCells()];
            }
            writeDataCell(cell, outStream);
        }
        outStream.writeByte(BYTE_ROW_SEPARATOR);
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
        outStream.writeUTF(key.getString());
    }

    /** Reads a row key from a string. Is overridden in {@link NoKeyBuffer}
     * to return always the same key.
     * @param inStream To read from
     * @return The row key as read right from the stream.
     * @throws IOException If reading fails for IO problems.
     */
    RowKey readRowKey(final DCObjectInputStream inStream) throws IOException {
        String key;
        if (m_version >= 5) { // < 5 is version 1.3.x and before, see above
            key = inStream.readUTF();
        } else {
            key = readDataCell(inStream).toString();
        }
        return new RowKey(key);
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
        boolean isBlob = cell instanceof BlobWrapperDataCell;
        Class<? extends DataCell> cellClass = isBlob
        ? ((BlobWrapperDataCell)cell).getBlobClass()
                : cell.getClass();
        DataCellSerializer<DataCell> ser = getSerializerForDataCell(cellClass);
        Byte identifier = m_typeShortCuts.get(cellClass);
        // DataCell is datacell-serializable
        if (ser != null || isBlob) {
            // memorize type if it does not exist
            outStream.writeByte(identifier);
            if (isBlob) {
                BlobWrapperDataCell bc = (BlobWrapperDataCell)cell;
                outStream.writeBlobAddress(bc.getAddress());
            } else {
                outStream.writeDataCell(ser, cell);
            }
        } else {
            outStream.writeByte(BYTE_TYPE_SERIALIZATION);
            outStream.writeByte(identifier);
            outStream.writeObject(cell);
        }
    }

    /** Get the serializer object to be used for writing the argument cell
     * or <code>null</code> if it needs to be java-serialized.
     * @param cellClass The cell's class to write out.
     * @return The serializer to use or <code>null</code>.
     */
    private DataCellSerializer<DataCell> getSerializerForDataCell(
            final Class<? extends DataCell> cellClass) throws IOException {
        if (m_typeShortCuts == null) {
            m_typeShortCuts = new HashMap<Class<? extends DataCell>, Byte>();
        }
        @SuppressWarnings("unchecked")
        DataCellSerializer<DataCell> serializer =
            (DataCellSerializer<DataCell>)DataType.getCellSerializer(
                    cellClass);
        if (!m_typeShortCuts.containsKey(cellClass)) {
            int size = m_typeShortCuts.size();
            if (size + BYTE_TYPE_START > Byte.MAX_VALUE) {
                throw new IOException(
                "Too many different cell implementations");
            }
            Byte identifier = (byte)(size + BYTE_TYPE_START);
            m_typeShortCuts.put(cellClass, identifier);
        }
        return serializer;
    }

    /** Reads a datacell from inStream, does no exception handling. */
    @SuppressWarnings("unchecked")
    private DataCell readDataCell(final DCObjectInputStream inStream)
            throws IOException {
        if (m_version == 1) {
            return readDataCellVersion1(inStream);
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
        boolean isBlob = BlobDataCell.class.isAssignableFrom(type);
        if (isBlob) {
            BlobAddress address = inStream.readBlobAddress();
            Buffer blobBuffer = this;
            if (address.getBufferID() != m_bufferID) {
                ContainerTable cnTbl =
                    m_globalRepository.get(address.getBufferID());
                if (cnTbl == null) {
                    throw new IOException(
                    "Unable to retrieve table that owns the blob cell");
                }
                blobBuffer = cnTbl.getBuffer();
            }
            return new BlobWrapperDataCell(
                    blobBuffer, address, (Class<? extends BlobDataCell>)type);
        }
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
            return inStream.readDataCell(serializer);
        }
    }

    /** Backward compatibility: DataCells that are (java-) serialized are
     * not annotated with a byte identifying their type. We need that in the
     * future to make sure we use the right class loader.
     * @param inStream To read from.
     * @return The cell.
     * @throws IOException If fails.
     */
    private DataCell readDataCellVersion1(final DCObjectInputStream inStream)
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
    } // readDataCellVersion1(DCObjectInputStream)

    private void writeBlobDataCell(final BlobDataCell cell, final BlobAddress a,
            final DataCellSerializer<DataCell> ser) throws IOException {
        // addRow will make sure that m_indicesOfBlobInColumns is initialized
        // when this method is called. If this method is called from a different
        // buffer object, in means that this buffer has been closed!
        // (When can this happen? This buffer resizes in memory, a successor
        // node is written to disc; they have different memory policies.)
        if (m_indicesOfBlobInColumns == null) {
            assert m_spec != null;
            m_indicesOfBlobInColumns = new int[m_spec.getNumColumns()];
        }
        int column = a.getColumn();
        int indexInColumn = m_indicesOfBlobInColumns[column]++;
        a.setIndexOfBlobInColumn(indexInColumn);
        boolean isToCompress = isUseCompressionForBlobs(cell.getClass());
        File outFile = getBlobFile(indexInColumn, column, true, isToCompress);
        BlobAddress originalBA = cell.getBlobAddress();
        if (originalBA != a) {
            int originalBufferIndex = originalBA.getBufferID();
            Buffer originalBuffer = null;
            ContainerTable t = m_globalRepository.get(originalBufferIndex);
            if (t != null) {
                originalBuffer = t.getBuffer();
            } else if (m_localRepository != null) {
                t = m_localRepository.get(originalBufferIndex);
                if (t != null) {
                    originalBuffer = t.getBuffer();
                }
            }
            if (originalBuffer != null) {
                int index = originalBA.getIndexOfBlobInColumn();
                int col = originalBA.getColumn();
                boolean compress = originalBA.isUseCompression();
                File source = originalBuffer.getBlobFile(
                        index, col, false, compress);
                FileUtil.copy(source, outFile);
                return;
            }
        }
        Buffer.onFileCreated();
        OutputStream out =
            new BufferedOutputStream(new FileOutputStream(outFile));
        if (isToCompress) {
            out = new GZIPOutputStream(out);
        }
        OutputStream outStream = null;
        try {
            if (ser != null) { // DataCell is datacell-serializable
                outStream = new DataOutputStream(out);
                DataOutput output =
                    new LongUTFDataOutputStream((DataOutputStream)outStream);
                ser.serialize(cell, output);
            } else {
                outStream = new ObjectOutputStream(out);
                ((ObjectOutputStream)outStream).writeObject(cell);
            }
        } finally {
            // do the best to minimize the number of open streams.
            if (outStream != null) {
                outStream.close();
            }
        }
    }

    /**
     * Reads the blob from the given blob address.
     * @param blobAddress The address to read from.
     * @param cl The expected class.
     * @return The blob cell being read.
     * @throws IOException If that fails.
     */
    BlobDataCell readBlobDataCell(final BlobAddress blobAddress,
            final Class<? extends DataCell> cl) throws IOException {
        int blobBufferID = blobAddress.getBufferID();
        if (blobBufferID != m_bufferID) {
            ContainerTable cnTbl = m_globalRepository.get(blobBufferID);
            if (cnTbl == null) {
                throw new IOException(
                        "Unable to retrieve table that owns the blob cell");
            }
            Buffer blobBuffer = cnTbl.getBuffer();
            return blobBuffer.readBlobDataCell(blobAddress, cl);
        }
        if (m_blobLRUCache == null) {
            m_blobLRUCache = new BlobLRUCache();
        }
        SoftReference<BlobDataCell> softRef = m_blobLRUCache.get(blobAddress);
        BlobDataCell result = softRef != null ? softRef.get() : null;
        if (result == null) {
            int column = blobAddress.getColumn();
            int indexInColumn = blobAddress.getIndexOfBlobInColumn();
            boolean isCompress = blobAddress.isUseCompression();
            File inFile = getBlobFile(indexInColumn, column, false, isCompress);
            InputStream in = new BufferedInputStream(
                    new FileInputStream(inFile));
            if (isCompress) {
                in = new GZIPInputStream(in);
            }
            DataCellSerializer<? extends DataCell> ser =
                DataType.getCellSerializer(cl);
            InputStream inStream = null;
            try {
                if (ser != null) {
                    inStream = new DataInputStream(in);
                    DataInput input =
                        new LongUTFDataInputStream((DataInputStream)inStream);
                    // the DataType class will reject Serializer that do not 
                    // have the appropriate return type
                    result = (BlobDataCell)ser.deserialize(input);
                    result.setBlobAddress(blobAddress);
                } else {
                    inStream = new PriorityGlobalObjectInputStream(in);
                    ((PriorityGlobalObjectInputStream)inStream).
                        setCurrentClassLoader(cl.getClassLoader());
                    try {
                        result = (BlobDataCell)
                            ((ObjectInputStream)inStream).readObject();
                        result.setBlobAddress(blobAddress);
                    } catch (ClassNotFoundException cnfe) {
                        IOException e =
                            new IOException("Unable to restore blob cell");
                        e.initCause(cnfe);
                        throw e;
                    }
                }
            } finally {
                // do the best to minimize the number of open streams.
                if (inStream != null) {
                    inStream.close();
                }
            }
            m_blobLRUCache.put(
                    blobAddress, new SoftReference<BlobDataCell>(result));
        }
        return result;
    }
    
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
        OutputStream wrap;
        if (IS_USE_GZIP) {
            wrap = new GZIPOutputStream(outStream);
        } else {
            wrap = outStream;
        }
        return new DCObjectOutputStream(wrap);
    }

    private void ensureBlobDirExists() throws IOException {
        if (m_blobDir == null) {
            ensureTempFileExists();
            File blobDir = createBlobDirNameForTemp(m_binFile);
            if (!blobDir.mkdir()) {
                throw new IOException("Unable to create temp directory "
                        + blobDir.getAbsolutePath());
            }
            m_blobDir = blobDir;
        }
    }

    /** Guesses a "good" blob directory for a given binary temp file.
     * For instance, if the temp file is /tmp/knime_container_xxxx_xx.bin.gz,
     * the blob dir name is suggested to be /tmp/knime_container_xxxx_xx.
     * @param tempFile base name
     * @return proposed temp file
     */
    static File createBlobDirNameForTemp(final File tempFile) {
        String prefix = tempFile.getName();
        File parent = tempFile.getParentFile();
        String suf = ".bin.gz";
        if (prefix.length() > suf.length() + 3 && prefix.endsWith(suf)) {
            prefix = prefix.substring(0, prefix.length() - suf.length());
        }
        File blobDir = new File(parent, prefix);
        int count = 0;
        while (blobDir.exists()) {
            blobDir = new File(parent, prefix + "_" + (++count));
        }
        return blobDir;
    }

    /**
     * Determines the file location for a blob to be read/written with some
     * given coordinates (column and index in column).
     * @param indexBlobInCol The index in the column (generally the row number).
     * @param column The column index.
     * @param createPath Create the directory, if necessary (when writing)
     * @param isCompressed If file is (to be) compressed
     * @return The file location.
     * @throws IOException If that fails (e.g. blob dir does not exist).
     */
    File getBlobFile(final int indexBlobInCol, final int column,
            final boolean createPath, final boolean isCompressed)
        throws IOException {
        StringBuilder childPath = new StringBuilder();
        childPath.append("col_" + column);
        childPath.append(File.separatorChar);
        // the index of the folder in knime_container_xyz/col_0/
        int topFolderIndex = indexBlobInCol
            / (BLOB_ENTRIES_PER_DIRECTORY * BLOB_ENTRIES_PER_DIRECTORY);
        String topDir = getFileName(topFolderIndex);
        childPath.append(topDir);
        childPath.append(File.separatorChar);
        // the index of the folder in knime_container_xyz/col_0/topFolderIndex
        int subFolderIndex = (indexBlobInCol - (topFolderIndex
                * BLOB_ENTRIES_PER_DIRECTORY * BLOB_ENTRIES_PER_DIRECTORY))
                / BLOB_ENTRIES_PER_DIRECTORY;
        String subDir = getFileName(subFolderIndex);
        childPath.append(subDir);
        if (createPath) {
            ensureBlobDirExists();
        }
        File blobDir = new File(m_blobDir, childPath.toString());
        if (createPath) {
            if (!blobDir.exists() && !blobDir.mkdirs()) {
                throw new IOException("Unable to create directory "
                        + blobDir.getAbsolutePath());
            }
        } else {
            if (!blobDir.exists()) {
                throw new IOException("Blob file location \""
                        + blobDir.getAbsolutePath() + "\" does not exist");
            }
        }
        String file = Integer.toString(indexBlobInCol) + ".bin";
        if (isCompressed) {
            file = file.concat(".gz");
        }
        return new File(blobDir, file);
    }

    /**
     * Creates the string for a given file index. For instance 0 is
     * transformed to "0000", 34 to "0034" and so on.
     * @param fileIndex The index of the file/directory.
     * @return The beautified string.
     */
    private static String getFileName(final int fileIndex) {
        String s = Integer.toString(fileIndex);
        int sLength = s.length();
        int max = Integer.toString(BLOB_ENTRIES_PER_DIRECTORY - 1).length();
        char[] c = new char[max];
        Arrays.fill(c, '0');
        for (int i = 0; i < sLength; i++) {
            c[i + (max - sLength)] = s.charAt(i);
        }
        return new String(c);
    }

    /**
     * Get a new <code>RowIterator</code>, traversing all rows that have been
     * added. Calling this method makes only sense when the buffer has been
     * closed. However, no check is done (as it is available to package classes
     * only).
     * @return a new Iterator over all rows.
     */
    CloseableRowIterator iterator() {
        if (usesOutFile()) {
            return new FromFileIterator();
        } else {
            return new FromListIterator();
        }
    }

    /** True if any row containing blob cells is contained in this buffer.
     * @return if blob cells are present. */
    boolean containsBlobCells() {
        return m_containsBlobs;
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
        if (!usesOutFile() || m_version < IVERSION) {
            DCObjectOutputStream outStream =
                initOutFile(new NonClosableZipOutputStream(zipOut));
            int count = 1;
            for (RowIterator it = iterator(); it.hasNext();) {
                BlobSupportDataRow row = (BlobSupportDataRow)it.next();
                exec.setProgress(count / (double)size(), "Writing row "
                        + count + " (\"" + row.getKey() + "\")");
                exec.checkCanceled();
                writeRow(row, outStream);
                count++;
            }
            // if the table contains no rows at all, the shortcut
            // table may be null!
            if (m_typeShortCuts == null) {
                m_typeShortCuts =
                    new HashMap<Class<? extends DataCell>, Byte>();
            }
            shortCutsLookup = closeFile(outStream);
        } else {
            // no need for BufferedInputStream here as the copy method
            // does the buffering itself
            FileUtil.copy(new FileInputStream(m_binFile), zipOut);
            shortCutsLookup = m_shortCutsLookup;
        }
        if (m_blobDir != null) {
            addBlobsToZip(ZIP_ENTRY_BLOBS, zipOut, m_blobDir);
        }
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_META));
        writeMetaToFile(
                new NonClosableZipOutputStream(zipOut), shortCutsLookup);
    }

    /** Adds recursively the content of the directory <code>dir</code> to
     * a zip output stream, prefixed with <code>zipEntry</code>.
     */
    private void addBlobsToZip(final String zipEntry,
            final ZipOutputStream zipOut, final File dir) throws IOException {
        for (File f : dir.listFiles()) {
            String name = f.getName();
            if (f.isDirectory()) {
                String dirPath = zipEntry + "/" + name + "/";
                zipOut.putNextEntry(new ZipEntry(dirPath));
                addBlobsToZip(dirPath, zipOut, f);
            } else {
                zipOut.putNextEntry(new ZipEntry(zipEntry + "/" + name));
                InputStream i = new BufferedInputStream(new FileInputStream(f));
                FileUtil.copy(i, zipOut);
                i.close();
                zipOut.closeEntry();
            }
        }
    }

    /** Deletes the file underlying this buffer.
     * @see Object#finalize()
     */
    @Override
    protected void finalize() {
        clear();
    }

    /** Get this buffer's ID. It may be null if this buffer is not used
     * as part of the workflow (but rather just has been read/written from/to
     * a zip file.
     * @return the buffer ID or -1
     */
    int getBufferID() {
        return m_bufferID;
    }

    /**
     * Clears the temp file. Any subsequent iteration will fail!
     */
    void clear() {
        m_list = null;
        if (m_binFile != null) {
            synchronized (m_openIteratorSet) {
                for (FromFileIterator f : m_openIteratorSet.keySet()) {
                    if (f != null) {
                        f.clearIteratorInstance(false);
                    }
                }
                m_openIteratorSet.clear();
            }
            if (m_blobDir != null) {
                DeleteInBackgroundThread.delete(m_binFile, m_blobDir);
            } else {
                DeleteInBackgroundThread.delete(m_binFile);
            }
        }
        m_binFile = null;
        m_blobDir = null;
    }

    private static final int MAX_FILES_TO_CREATE_BEFORE_GC = 10000;
    private static final MutableInteger FILES_CREATED_COUNTER =
        new MutableInteger(0);

    /** Method being called each time a file is created. It maintains a counter
     * and calls each {@link #MAX_FILES_TO_CREATE_BEFORE_GC} files the garbage
     * collector. This fixes an unreported problem on windows, where (although
     * the file reference is null) there seems to be a hidden file lock,
     * which yields a "not enough system resources to perform operation" error.
     */
    private static void onFileCreated() {
        synchronized (FILES_CREATED_COUNTER) {
            FILES_CREATED_COUNTER.inc();
            if (FILES_CREATED_COUNTER.intValue()
                    % MAX_FILES_TO_CREATE_BEFORE_GC == 0) {
                LOGGER.debug("created " + FILES_CREATED_COUNTER.intValue()
                        + " files, performing garbage collection to"
                        + " release handles");
                System.gc();
            }
        }
    }

    /** Print a debug message. This method does nothing if
     * isExecutingShutdownHook is true. */
    private static void logDebug(final String message, final Throwable t) {
        if (!isExecutingShutdownHook) {
            if (t == null) {
                LOGGER.debug(message);
            } else {
                LOGGER.debug(message, t);
            }
        }
    }
    
    /** Last recently used cache for blobs. */
    private static final class BlobLRUCache 
        extends LinkedHashMap<BlobAddress, SoftReference<BlobDataCell>> {
        
        
        /** Default constructor, instructs for access order. */
        BlobLRUCache() {
            super(16, 0.75f, true); // args copied from HashMap implementation
        }
        
        /** {@inheritDoc} */
        @Override
        protected boolean removeEldestEntry(
                final Entry<BlobAddress, SoftReference<BlobDataCell>> eldest) {
            return size() >= 50;
        }
        
        /** {@inheritDoc} */
        @Override
        public synchronized SoftReference<BlobDataCell> get(final Object key) {
            return super.get(key);
        }
        
        /** {@inheritDoc} */
        @Override
        public synchronized SoftReference<BlobDataCell> put(
                final BlobAddress key, 
                final SoftReference<BlobDataCell> value) {
            return super.put(key, value);
        }
        
    }

    /**
     * Iterator that traverses the out file on the disk and deserializes
     * the rows.
     */
    private class FromFileIterator extends CloseableRowIterator {

        private int m_pointer;
        /** If an exception has been thrown while reading from this buffer (only
         * if it has been written to disc). If so, further error messages are
         * only written to debug output in order to reduce message spam on the
         * console.
         */
        private boolean m_hasThrownReadException;

        private DCObjectInputStream m_inStream;

        /**
         * Inits the input stream.
         */
        FromFileIterator() {
            m_pointer = 0;
            if (m_binFile == null) {
                throw new RuntimeException("Unable to read table from file, "
                        + "table has been cleared.");
            }
            try {
                BufferedInputStream bufferedStream =
                    new BufferedInputStream(new FileInputStream(m_binFile));
                InputStream in;
                // stream was not zipped in KNIME 1.1.x
                if (!IS_USE_GZIP || m_version < 3) {
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
            synchronized (m_openIteratorSet) {
                m_openIteratorSet.put(this, DUMMY);
            }
        }

        /** Get the name of the out file that this iterator works on.
         * @return The name of the out file
         */
        public String getOutFileName() {
            return m_binFile.getAbsolutePath();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            boolean hasNext = m_pointer < Buffer.this.m_size;
            if (!hasNext && (m_inStream != null)) {
                clearIteratorInstance(true);
            }
            return hasNext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized BlobSupportDataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Iterator at end");
            }
            final DCObjectInputStream inStream = m_inStream;
            RowKey key;
            try {
                key = readRowKey(inStream);
            } catch (Throwable throwable) {
                handleReadThrowable(throwable);
                // can't ensure that we generate a unique key but it should
                // cover 99.9% of all cases
                String keyS = "Read_failed__auto_generated_key_" + m_pointer;
                key = new RowKey(keyS);
            }
            int colCount = m_spec.getNumColumns();
            DataCell[] cells = new DataCell[colCount];
            for (int i = 0; i < colCount; i++) {
                DataCell nextCell;
                try {
                    nextCell = readDataCell(m_inStream);
                } catch (final Throwable e) {
                    handleReadThrowable(e);
                    nextCell = DataType.getMissingCell();
                }
                cells[i] = nextCell;
            }
            try {
                if (m_version >= 5) {
                    byte eoRow = inStream.readByte();
                    if (eoRow != BYTE_ROW_SEPARATOR) {
                        throw new IOException("Expected end of row byte, "
                            + "got '" + eoRow + "', (byte " + (int)eoRow + ")");
                    }
                } else {
                    char eoRow = inStream.readChar();
                    if (eoRow != '\n') {
                        throw new IOException("Expected end of row character, "
                            + "got '" + eoRow + "', (char " + (int)eoRow + ")");
                    }
                }
            } catch (IOException ioe) {
                handleReadThrowable(ioe);
            } finally {
                m_pointer++;
            }
            return new BlobSupportDataRow(key, cells);
        }

        private void handleReadThrowable(final Throwable throwable) {
            String warnMessage = "Errors while reading row "
                    + (m_pointer + 1) + " from file \""
                    + m_binFile.getName() + "\": " + throwable.getMessage();
            if (!m_hasThrownReadException) {
                warnMessage = warnMessage.concat(
                        "; Suppressing further warnings.");
                LOGGER.error(warnMessage, throwable);
            } else {
                LOGGER.debug(warnMessage, throwable);
            }
            if (!(throwable instanceof IOException)) {
                String messageCoding = throwable.getClass().getSimpleName()
                + " caught, implementation may only throw IOException.";
                if (!m_hasThrownReadException) {
                    LOGGER.coding(messageCoding);
                } else {
                    LOGGER.debug(messageCoding);
                }
            }
            m_hasThrownReadException = true;
        }

        private synchronized void clearIteratorInstance(
                final boolean removeFromHash) {
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
                logDebug(closeMes + m_nrOpenInputStreams + " remaining", null);
                m_inStream = null;
                if (removeFromHash) {
                    synchronized (m_openIteratorSet) {
                        m_openIteratorSet.remove(this);
                    }
                }
            } catch (IOException ioe) {
                logDebug(closeMes + "failed!", ioe);
            }
        }
        
        /** {@inheritDoc} */
        @Override
        public void close() {
            clearIteratorInstance(true);
        }

        /** {@inheritDoc} */
        @Override
        protected void finalize() throws Throwable {
            /* This all relates much to bug #63: The temp files are not
             * deleted under windows. It seems that there are open streams
             * when the VM closes.
             */
            clearIteratorInstance(true);
        }
    }

    /**
     * Iterator to be used when data is contained in m_list. It uses
     * access by index rather than wrapping an java.util.Iterator
     * as the list may be simultaneously modified while reading (in case
     * the content is fetched from disk and restored in memory).
     * This object is used when all rows fit in memory (no file).
     */
    private class FromListIterator extends CloseableRowIterator {

        // do not use iterator here, see inner class comment
        private int m_nextIndex = 0;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_nextIndex < size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more rows in buffer");
            }
            // assignment avoids race condition in following statements
            // (parallel thread may set m_backIntoMemoryIterator to null)
            FromFileIterator backIntoMemoryIterator = m_backIntoMemoryIterator;
            if (m_nextIndex < m_list.size()) {
                return m_list.get(m_nextIndex++);
            }
            if (backIntoMemoryIterator == null) {
                throw new InternalError("DataRow list contains fewer elements"
                        + " than buffer (" + m_list.size() + " vs. "
                        + size() + ")");
            }
            synchronized (backIntoMemoryIterator) {
                // intermediate change possible (by other iterator)
                if (m_nextIndex < m_list.size()) {
                    return m_list.get(m_nextIndex++);
                }
                BlobSupportDataRow next = m_backIntoMemoryIterator.next();
                if (next == null) {
                    throw new InternalError(
                            "Unable to restore data row from disk");
                }
                m_list.add(next);
                if (++m_nextIndex >= size()) {
                    assert !m_backIntoMemoryIterator.hasNext() : "File "
                        + "iterator returns more rows than buffer contains";
                    m_backIntoMemoryIterator = null;
                }
                return next;
            }
        }
        
        /** {@inheritDoc} */
        @Override
        public void close() {
            m_nextIndex = size();
        }
    }

    /** A background thread that deletes temporary files and directory
     * (which may be a very long lasting job - in particular when blob
     * directories need to get deleted). This is the long term fix for bug
     * 1051.
     * <p>Implementation note: There is singleton thread running that does the
     * deletion, if this thread is idle for a while, it is shut down and
     * recreated on demand.*/
    private static final class DeleteInBackgroundThread extends Thread {

        private static final NodeLogger THREAD_LOGGER =
            NodeLogger.getLogger(DeleteInBackgroundThread.class);
        private static DeleteInBackgroundThread instance;
        private final LinkedBlockingQueue<File> m_filesToDeleteList;

        private static final Object LOCK = new Object();

        private DeleteInBackgroundThread() {
            super("KNIME Temp File Deleter");
            m_filesToDeleteList = new LinkedBlockingQueue<File>();
        }

        /** Queues a set of files for deletion and returns immediately.
         * @param file To delete.
         */
        public static void delete(final File... file) {
            synchronized (LOCK) {
                if (instance == null || !instance.isAlive()) {
                    instance = new DeleteInBackgroundThread();
                    instance.start();
                }
                instance.addFile(file);
            }
        }

        /** Blocks the calling thread until all queued files have been
         * deleted. */
        public static void waitUntilFinished() {
            synchronized (LOCK) {
                if (instance == null || !instance.isAlive()) {
                    return;
                }
            }
            instance.blockUntilFinished();
        }

        private void addFile(final File[] files) {
            synchronized (LOCK) {
                m_filesToDeleteList.addAll(Arrays.asList(files));
                if (!m_filesToDeleteList.isEmpty()) {
                    LOCK.notifyAll();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            while (true) {
                synchronized (LOCK) {
                    if (m_filesToDeleteList.isEmpty()) {
                        try {
                            LOCK.wait(60 * 1000);
                        } catch (InterruptedException ie) {
                            if (!m_filesToDeleteList.isEmpty()) {
                                THREAD_LOGGER.warn("Deletion of "
                                        + m_filesToDeleteList.size() + "files "
                                        + "or directories failed because "
                                        + "deleter thread was interrupted");
                            }
                            return;
                        }
                        if (m_filesToDeleteList.isEmpty()) {
                            return;
                        }
                    }
                }
                executeDeletion();
            }
        }

        private synchronized void executeDeletion() {
            try {
                File first;
                while ((first = m_filesToDeleteList.poll()) != null) {
                    String type = first.isFile() ? "file" : "directory";
                    boolean deleted = deleteRecursively(first);
                    if (!deleted && first.exists()) {
                        // note: although all input streams are closed, the
                        // file can't be deleted. If we call the gc, it
                        // works. No clue. That only happens on windows!
            // http://forum.java.sun.com/thread.jspa?forumID=31&threadID=609458
                        System.gc();
                    }
                    if (deleted || deleteRecursively(first)) {
                        logDebug("Deleted temporary " + type + " \""
                                + first.getAbsolutePath() + "\"", null);
                    } else {
                        logDebug("Failed to delete temporary " + type + " \""
                                + first.getAbsolutePath() + "\"", null);
                    }
                }
            } finally {
                notifyAll();
            }
        }

        private synchronized void blockUntilFinished() {
            if (!m_filesToDeleteList.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                    // that should only be called from the shutdown hook,
                    // if someone interrupts us, so be it.
                }
            }
        }

        /** Makes sure the list is empty. This is necessary because when the
         * VM goes done, any running thread is stopped.
         * @see java.lang.Object#finalize()
         */
        @Override
        protected void finalize() throws Throwable {
            if (!m_filesToDeleteList.isEmpty()) {
                executeDeletion();
            }
        }

        /** Deletes the argument file or directory recursively and returns true
         * if this was successful. This method follows any symbolic link
         * (in comparison to
         * {@link org.knime.core.utilFileUtil#deleteRecursively(File)}.
         * @param f The (blob) directory or temp file to delete.
         * @return Whether or not the f has been deleted.
         */
        private static boolean deleteRecursively(final File f) {
            if (f.isFile()) {
                return f.delete();
            }
            File[] files = f.listFiles();
            if (files != null) {
                for (File file : files) {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        if (file.isDirectory()) {
                            deleteRecursively(file);
                        }
                    }
                }
            }
            return f.delete();
        }

    }

}
