/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
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
import org.knime.core.data.collection.BlobSupportDataCellIterator;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.BufferFromFileIteratorVersion20.DataCellStreamReader;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.util.FileUtil;

/**
 * A buffer writes the rows from a {@link DataContainer} to a file.
 * This class serves as connector between the {@link DataContainer} and
 * the {@link org.knime.core.data.DataTable} that is returned by the container.
 * It "centralizes" the IO operations.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
class Buffer implements KNIMEStreamConstants {
    
    /** Static field to enable/disable the usage of a GZipInput/OutpuStream
     * when writing the binary data. This option defaults to
     * {@value DataContainer#DEF_GZIP_COMPRESSION}.
     *
     * Note: Changing this parameter makes it impossible to read workflows
     * written previously. It's only used for internal testing purposes.
     */
    private static final boolean IS_USE_GZIP;

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

    /** Config entry: Sub element in config that keeps the list of cell
     * class information (used to be a plain array). */
    private static final String CFG_CELL_CLASSES = "table.datacell.classes";

    /** Config entry: Class name of data cell. */
    private static final String CFG_CELL_SINGLE_CLASS = "class";
    
    /** Config entry: element type if a cell represents a collection. */
    private static final String CFG_CELL_SINGLE_ELEMENT_TYPE = 
        "collection.element.type";
    
    /** Current version string. */
    private static final String VERSION = "container_8";

    /** The version number corresponding to VERSION. */
    private static final int IVERSION = 8;

    private static final HashMap<String, Integer> COMPATIBILITY_MAP;

    static {
        COMPATIBILITY_MAP = new HashMap<String, Integer>();
        COMPATIBILITY_MAP.put("container_1.0.0", 1); // version 1.00
        COMPATIBILITY_MAP.put("container_1.1.0", 2); // version 1.1.x
        COMPATIBILITY_MAP.put("container_1.2.0", 3); // never released
        COMPATIBILITY_MAP.put("container_4", 4);     // version 1.2.x - 1.3.x
        COMPATIBILITY_MAP.put("container_5", 5);     // 2.0 TechPreview Version
        COMPATIBILITY_MAP.put("container_6", 6);     // 2.0 Alpha
        COMPATIBILITY_MAP.put("container_7", 7);     // 2.0.0 (final)
        COMPATIBILITY_MAP.put(VERSION, IVERSION);    // version 2.0.1++
        // NOTE consider to also increment the workflow.knime version number
        // when updating this list
    }

    /**
     * Contains weak references to file iterators that have ever been created
     * but not (yet) garbage collected. We will add a shutdown hook
     * (Runtime#addShutDownHook) and close the streams of all iterators that
     * are open. This is a workaround for bug #63 (see also
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4722539): Temp files
     * are not deleted on windows when there are open streams.
     */
    private static final Set<WeakReference<Buffer>>
        OPENBUFFERS = Collections.synchronizedSet(
                new HashSet<WeakReference<Buffer>>());

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
        // prefer this block over Boolean.getBoolean(...) to cover typos 
        // on command line (warn on console)
        String isUseGzipString = System.getProperty(
                KNIMEConstants.PROPERTY_TABLE_GZIP_COMPRESSION);
        boolean debugLog = true;
        if (isUseGzipString == null) {
            isUseGzipString = Boolean.toString(
                    DataContainer.DEF_GZIP_COMPRESSION);
            debugLog = false;
        }
        if ("true".equals(isUseGzipString)) {
            IS_USE_GZIP = true;
        } else if ("false".equals(isUseGzipString)) {
            IS_USE_GZIP = false;
        } else {
            debugLog = false;
            LOGGER.warn("Unable to read property " 
                    + KNIMEConstants.PROPERTY_TABLE_GZIP_COMPRESSION + " (\""
                    + isUseGzipString + "\"); defaulting to " 
                    + DataContainer.DEF_GZIP_COMPRESSION);
            IS_USE_GZIP = DataContainer.DEF_GZIP_COMPRESSION;
        }
        if (debugLog) {
            LOGGER.debug("Setting table stream compression to " + IS_USE_GZIP);
        }
    }

    /**
     * Hash used to reduce the overhead of reading a blob cell over and over
     * again. Useful in cases where a blob is added multiple times to a table...
     * the iterator will read the blob address, treat it as unseen and then ask
     * the owning Buffer to restore the blob.
     */
    private final BlobLRUCache m_blobLRUCache = new BlobLRUCache();

    private static boolean isUseCompressionForBlobs(
            final CellClassInfo cellClassInfo) {
        @SuppressWarnings("unchecked")
        Class<? extends BlobDataCell> cl = 
            (Class<? extends BlobDataCell>)cellClassInfo.getCellClass();
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
    private AtomicInteger m_nrOpenInputStreams = new AtomicInteger();

    /** the stream that writes to the file, it's a special object output
     * stream, in which we can mark the end of an entry (to figure out
     * when a cell implementation reads too many or too few bytes). */
    private DCObjectOutputVersion2 m_outStream;

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
    private HashMap<CellClassInfo, Byte> m_typeShortCuts;

    /** Inverse map of m_typeShortCuts - it stores to each shortcut
     * (like 'A', 'B', ...) the corresponding type.
     * This object is null unless close() has been called.
     */
    private CellClassInfo[] m_shortCutsLookup;

    /**
     * List of file iterators that look at this buffer. Need to close them
     * when the node is reset and the file shall be deleted.
     */
    private final WeakHashMap<FromFileIterator, Object> m_openIteratorSet;

    /** The iterator that is used to read the content back into memory.
     * This instance is used after the workflow is restored from disk. */
    private CloseableRowIterator m_backIntoMemoryIterator;
    
    /** A flag indicating whether the next call to iterator() hast to 
     * initialize m_backIntoMemoryIterator. This flag is mostly false but may
     * be true right after object initialization when the settings contain
     * the "was-previously-in-memory" flag. */
    private boolean m_useBackIntoMemoryIterator = false;

    /**
     * The version of the file we are reading (if initiated with
     * Buffer(File, boolean). Used to remember when we need to read a file
     * which has been written with another version of the Buffer, i.e. to
     * provide backward compatibility.
     */
    private int m_version = IVERSION;
    
    /** Map of blob addresses that were copied into this buffer. It maps the 
     * original id to the new id (having m_bufferID as owner). 
     * Copying is necessary if 
     * - we copy from or to a buffer with id "-1" or
     * - this buffer is instructed to copy all blobs 
     *   (important for loop end nodes).
     * If we didn't use such a map, we wouldn't notice if any one blob gets
     * copied into this buffer multiple times ... we would copy each time it is
     * added, which is bad.
     * This member is null if this buffer is not in write-mode or it does not
     * need to copy blobs.
     */
    private HashMap<BlobAddress, BlobAddress> m_copiedBlobsMap;
    
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
    
    /** Get underlying stream version. Important for file iterators.
     * @return Underlying stream version.
     */
    final int getReadVersion() {
        return m_version;
    }
    
    /** @return Underlying binary file. */
    final File getBinFile() {
        return m_binFile;
    }
    
    /** @return Whether stream is zipped. */
    final boolean isBinFileGZipped() {
        return IS_USE_GZIP;
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
     * @param isCopyOfExisting Whether to copy blobs (this is only true when
     *        and existing buffer gets copied (version hop))
     * @param forceCopyOfBlobs If true any blob that is not owned by this 
     * buffer, will be copied and this buffer will take ownership. This option
     * is true for loop end nodes, which need to aggregate the data generated
     * in the loop body 
     */
    void addRow(final DataRow r, final boolean isCopyOfExisting, 
            final boolean forceCopyOfBlobs) {
        try {
            BlobSupportDataRow row = 
                saveBlobs(r, isCopyOfExisting, forceCopyOfBlobs);
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
                        + "throw " + e.getClass().getSimpleName(), e);
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

    private BlobSupportDataRow saveBlobs(final DataRow row,
            final boolean isCopyOfExisting, final boolean forceCopyOfBlobs)
    throws IOException {
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
            DataCell processedCell = handleIncomingBlob(
                    cell, col, row.getNumCells(), 
                    isCopyOfExisting, forceCopyOfBlobs);
            if (processedCell != cell) {
                if (cellCopies == null) {
                    cellCopies = new DataCell[cellCount];
                    for (int i = 0; i < cellCount; i++) {
                        cellCopies[i] = ((BlobSupportDataRow)row).getRawCell(i);
                    }
                }
                cellCopies[col] = processedCell;
            }
        }
        return cellCopies == null ? (BlobSupportDataRow)row
                : new BlobSupportDataRow(row.getKey(), cellCopies);
    }
    
    private DataCell handleIncomingBlob(final DataCell cell,  final int col, 
            final int totalColCount, final boolean copyForVersionHop, 
            final boolean forceCopyOfBlobsArg) throws IOException {
        // whether the content of the argument row needs to be copied
        // into a new BlobSupportDataRow (will do that when either this 
        // flag is true or cellCopies != null)
        boolean isWrapperCell = cell instanceof BlobWrapperDataCell;
        BlobAddress ad;
        final CellClassInfo cl;
        BlobWrapperDataCell wc = null;
        if (isWrapperCell) {
            wc = (BlobWrapperDataCell)cell;
            ad = wc.getAddress();
            cl = wc.getBlobClassInfo();
        } else if (cell instanceof BlobDataCell) {
            cl = CellClassInfo.get(cell);
            ad = ((BlobDataCell)cell).getBlobAddress();
        } else if (cell instanceof CollectionDataValue) {
            CollectionDataValue cdv = (CollectionDataValue)cell;
            if (cdv.containsBlobWrapperCells()) {
                Iterator<DataCell> it = cdv.iterator();
                if (!(it instanceof BlobSupportDataCellIterator)) {
                    LOGGER.coding("(Collection) DataCell of class \"" 
                            + cell.getClass().getSimpleName() 
                            + "\" contains Blobs, but does not "
                            + "return an iterator supporting those " 
                            + "(expected " + BlobSupportDataCellIterator
                            .class.getName() + ", got " 
                            + it.getClass().getName() + ")");
                }
                while (it.hasNext()) {
                    DataCell n = it instanceof BlobSupportDataCellIterator 
                            ? ((BlobSupportDataCellIterator)
                                    it).nextWithBlobSupport() : it.next();
                    // we disregard the return value here
                    handleIncomingBlob(n, col, totalColCount, 
                            copyForVersionHop, forceCopyOfBlobsArg);
                }
            }
            return cell;
        } else {
            return cell; // ordinary cell (e.g. double cell)
        }
        boolean forceCopyOfBlobs = forceCopyOfBlobsArg;
        Buffer ownerBuffer;
        if (ad != null) {
            // either copying from or to an isolated buffer (or both)
            forceCopyOfBlobs |= ad.getBufferID() == -1 || getBufferID() == -1;
            // has blob been added to this buffer in a preceding row?
            // (and this is not an ordinary buffer (but a BufferedDataCont.)
            if (ad.getBufferID() == getBufferID() && getBufferID() != -1) {
                ownerBuffer = this;
            } else {
                // table that's been created somewhere in the workflow
                ContainerTable t = m_globalRepository.get(ad.getBufferID());
                ownerBuffer = t != null ? t.getBuffer() : null;
            }
            /* this can only be true if the argument row contains wrapper 
             * cells for blobs that do not have a buffer set; that is, 
             * someone took a BlobDataCell from a predecessor node 
             * (ad != null) and put it manually into a new wrapper cell
             * (wc != null) - by doing that you loose the buffer info
             * (wc.getBuffer == null) */
            if (isWrapperCell && wc.getBuffer() == null) {
                wc.setAddressAndBuffer(ad, ownerBuffer);
            }
        } else {
            ownerBuffer = null;
        }
        // if we have to make a clone of the blob cell (true if 
        // isCopyOfExisting is true and the blob address corresponds to the next
        // assignable m_indicesOfBlobInColumns[col])
        boolean isToCloneForVersionHop = false;
        if (copyForVersionHop) {
            isToCloneForVersionHop = ad != null 
            && ad.getBufferID() == getBufferID();
            // this if statement handles cases where a blob is added to the 
            // buffer multiple times -- don't copy the duplicates
            if (isToCloneForVersionHop && m_indicesOfBlobInColumns == null) {
                // first to assign
                isToCloneForVersionHop = ad.getIndexOfBlobInColumn() == 0;
                assert isToCloneForVersionHop 
                    : "Clone of buffer does not return blobs in order";
            } else if (isToCloneForVersionHop 
                    && m_indicesOfBlobInColumns != null) {
                isToCloneForVersionHop = ad.getIndexOfBlobInColumn() 
                        == m_indicesOfBlobInColumns[col];
            }
        }
        
        // if we have to clone the blob because the forceCopyOfBlobs flag is
        // on (e.g. because the owning node is a loop end node)
        boolean isToCloneDueToForceCopyOfBlobs = false;
        // don't overwrite the deep-clone
        if (forceCopyOfBlobs && !isToCloneForVersionHop) {
            if (m_copiedBlobsMap == null) {
                m_copiedBlobsMap = new HashMap<BlobAddress, BlobAddress>();
            }
            // if not previously copied into this buffer
            if (ad != null) {
                BlobAddress previousCopyAddress = m_copiedBlobsMap.get(ad);
                if (previousCopyAddress == null) {
                    isToCloneDueToForceCopyOfBlobs = true;
                    if (isWrapperCell && ownerBuffer == null) {
                        ownerBuffer = ((BlobWrapperDataCell)cell).getBuffer();
                    }
                } else {
                    return new BlobWrapperDataCell(
                            this, previousCopyAddress, cl);
                }
            }
        }
        
        // if either not previously assigned (ownerBuffer == null) or
        // we have to make a clone
        if (ownerBuffer == null || isToCloneForVersionHop 
                || isToCloneDueToForceCopyOfBlobs) {
            // need to set ownership if this blob was not assigned yet
            // or has been assigned to an unlinked (i.e. local) buffer
            boolean isCompress = ad != null ? ad.isUseCompression()
                    : isUseCompressionForBlobs(cl);
            BlobAddress rewrite =
                new BlobAddress(m_bufferID, col, isCompress);
            if (ad == null) {
                // take ownership
                if (isWrapperCell) {
                    ((BlobWrapperDataCell)cell).setAddressAndBuffer(
                            rewrite, this);
                } else {
                    ((BlobDataCell)cell).setBlobAddress(rewrite);
                }
                ad = rewrite;
            }
            if (m_indicesOfBlobInColumns == null) {
                m_indicesOfBlobInColumns = new int[totalColCount];
            }
            Buffer b;
            if (isToCloneDueToForceCopyOfBlobs) {
                b = ownerBuffer;
                m_copiedBlobsMap.put(ad, rewrite);
            } else {
                ContainerTable tbl = m_localRepository.get(ad.getBufferID());
                b = tbl == null ? null : tbl.getBuffer();
            }
            if (b != null && !isToCloneForVersionHop) {
                int indexBlobInCol = m_indicesOfBlobInColumns[col]++;
                rewrite.setIndexOfBlobInColumn(indexBlobInCol);
                File source = b.getBlobFile(ad.getIndexOfBlobInColumn(),
                        ad.getColumn(), false, ad.isUseCompression());
                File dest = getBlobFile(indexBlobInCol, col, true, 
                        ad.isUseCompression());
                FileUtil.copy(source, dest);
            } else {
                BlobDataCell bc;
                if (isWrapperCell) {
                    DataCell c = ((BlobWrapperDataCell)cell).getCell();
                    bc = c.isMissing() ? null : (BlobDataCell)c;
                } else {
                    bc = (BlobDataCell)cell;
                }
                // the null case can only happen if there were problems reading
                // the persisted blob (caught exception and returned missing 
                // cell) - reading the blob that is "not saved" here will 
                // also cause trouble ("no such file"), which is ok as we need
                // to take an error along
                if (bc != null) {
                    writeBlobDataCell(
                            bc, rewrite, getSerializerForDataCell(cl));
                }
            }
            wc = new BlobWrapperDataCell(this, rewrite, cl);
            m_containsBlobs = true;
        } else {
            // blob has been saved in one of the predecessor nodes
            if (isWrapperCell) {
                wc = (BlobWrapperDataCell)cell;
            } else {
                wc = new BlobWrapperDataCell(ownerBuffer, ad, cl);
            }
        }
        return wc;
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
    private CellClassInfo[] closeFile(
            final DCObjectOutputVersion2 outStream) throws IOException {
        CellClassInfo[] shortCutsLookup = createShortCutArray();
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
            final CellClassInfo[] shortCutsLookup)
            throws IOException {
        NodeSettings settings = new NodeSettings("Table Meta Information");
        NodeSettingsWO subSettings =
            settings.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addString(CFG_VERSION, getVersion());
        subSettings.addInt(CFG_SIZE, size());
        subSettings.addBoolean(CFG_CONTAINS_BLOBS, m_containsBlobs);
        subSettings.addBoolean(CFG_IS_IN_MEMORY, !usesOutFile());
        subSettings.addInt(CFG_BUFFER_ID, m_bufferID);
        NodeSettingsWO typeSubSettings = 
            subSettings.addNodeSettings(CFG_CELL_CLASSES);
        for (int i = 0; i < shortCutsLookup.length; i++) {
            CellClassInfo info = shortCutsLookup[i];
            NodeSettingsWO single = 
                typeSubSettings.addNodeSettings("element_" + i);
            single.addString(CFG_CELL_SINGLE_CLASS, 
                    info.getCellClass().getName());
            DataType elementType = info.getCollectionElementType();
            if (elementType != null) {
                NodeSettingsWO subTypeConfig = 
                    single.addNodeSettings(CFG_CELL_SINGLE_ELEMENT_TYPE);
                elementType.save(subTypeConfig);
            }
        }
        settings.saveToXML(out);
    }

    /**
     * Reads meta information, that is row count, version, byte assignments.
     * @param metaIn To read from.
     * @throws IOException If reading fails.
     * @throws ClassNotFoundException If any of the classes can't be loaded.
     * @throws InvalidSettingsException If the internal structure is broken.
     */
    private void readMetaFromFile(final InputStream metaIn)
    throws IOException, InvalidSettingsException {
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
            if (m_version >= 8) { // no back into memory option prior 2.0
                // the "back into memory" option was added in buffer
                // version 5 (2.0 TechPreview Version), though it has a bug
                // (#1631) which caused all data to be held in memory when
                // converted into 2.0 schema. Therefore we enable this feature
                // with buffer version 8 (> 2.0)
                if (subSettings.getBoolean(CFG_IS_IN_MEMORY)) {
                    restoreIntoMemory();
                }
            }
            if (m_version <= 6) {
                m_shortCutsLookup = 
                    readCellClassInfoArrayFromMetaVersion1x(subSettings);
            } else {
                m_shortCutsLookup =
                    readCellClassInfoArrayFromMetaVersion2(subSettings);
            }
        } finally {
            inStream.close();
        }
    }
    
    @SuppressWarnings("unchecked")
    private static CellClassInfo[] readCellClassInfoArrayFromMetaVersion1x(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        String[] cellClasses = settings.getStringArray(CFG_CELL_CLASSES);
        CellClassInfo[] shortCutsLookup = new CellClassInfo[cellClasses.length];
        for (int i = 0; i < cellClasses.length; i++) {
            Class<?> cl;
            try {
                cl = GlobalClassCreator.createClass(cellClasses[i]);
            } catch (ClassNotFoundException e) {
                throw new InvalidSettingsException("Can't load data cell " 
                        + "class \"" + cellClasses[i] + "\"", e);
            }
            if (!DataCell.class.isAssignableFrom(cl)) {
                throw new InvalidSettingsException("No data cell class: \""
                        + cellClasses[i] + "\"");
            }
            try {
                shortCutsLookup[i] = CellClassInfo.get(
                        (Class<? extends DataCell>)cl, null);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException(
                        "Unable to instantiate CellClassInfo for class \""
                        + cellClasses[i] + "\"", e);
            }
        }
        return shortCutsLookup;
    }
    
    @SuppressWarnings("unchecked")
    private static CellClassInfo[] readCellClassInfoArrayFromMetaVersion2(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO typeSubSettings = 
            settings.getNodeSettings(CFG_CELL_CLASSES);
        Set<String> keys = typeSubSettings.keySet();
        CellClassInfo[] shortCutsLookup = new CellClassInfo[keys.size()];
        int i = 0;
        for (String s : keys) {
            NodeSettingsRO single = typeSubSettings.getNodeSettings(s);
            String className = single.getString(CFG_CELL_SINGLE_CLASS); 
            Class<?> cl;
            try {
                cl = GlobalClassCreator.createClass(className);
            } catch (ClassNotFoundException e) {
                throw new InvalidSettingsException("Can't load data cell " 
                        + "class \"" + className + "\"", e);
            }
            if (!DataCell.class.isAssignableFrom(cl)) {
                throw new InvalidSettingsException("No data cell class: \""
                        + className + "\"");
            }
            DataType elementType = null;
            if (single.containsKey(CFG_CELL_SINGLE_ELEMENT_TYPE)) {
                NodeSettingsRO subTypeConfig = 
                    single.getNodeSettings(CFG_CELL_SINGLE_ELEMENT_TYPE);
                elementType = DataType.load(subTypeConfig);
            }
            try {
                shortCutsLookup[i] = CellClassInfo.get(
                        (Class<? extends DataCell>)cl, elementType);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException(
                        "Unable to instantiate CellClassInfo for class \""
                        + className + "\", element type: " + elementType);
            }
            i++;
        }
        return shortCutsLookup;
    }

    /** Create the shortcut table, it translates m_typeShortCuts to
     * m_shortCutsLookup. */
    private CellClassInfo[] createShortCutArray() {
        // unreported bug fix: NPE when the table only contains missing values.
        if (m_typeShortCuts == null) {
            m_typeShortCuts = new HashMap<CellClassInfo, Byte>();
        }
        m_shortCutsLookup = new CellClassInfo[m_typeShortCuts.size()];
        for (Map.Entry<CellClassInfo, Byte> e : m_typeShortCuts.entrySet()) {
            byte shortCut = e.getValue();
            CellClassInfo type = e.getKey();
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
    
    /** Get whether the buffer wants to persist row keys. Here hard-coded
     * to <code>true</code> but overwritten in {@link NoKeyBuffer}.
     * @return whether row keys needs to be written/read.
     */
    boolean shouldSkipRowKey() {
        return false;
    }

    /** Restore content of this buffer into main memory (using a collection
     * implementation). The restoring will be performed with the next iteration.
     */
    final void restoreIntoMemory() {
        m_useBackIntoMemoryIterator = true;
    }

    /** Get reference to the table repository that this buffer was initially
     * instantiated with. Used for blob reading/writing.
     * @return (Workflow-) global table repository.
     */
    Map<Integer, ContainerTable> getGlobalRepository() {
        return m_globalRepository;
    }

    /** Get reference to the local table repository that this buffer was
     * initially instantiated with. Used for blob reading/writing. This
     * may be null.
     * @return (Workflow-) global table repository.
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
            final DCObjectOutputVersion2 outStream) throws IOException {
        RowKey id = row.getKey();
        writeRowKey(id, outStream);
        for (int i = 0; i < row.getNumCells(); i++) {
            DataCell cell = row.getRawCell(i);
            if (m_indicesOfBlobInColumns == null
                    && cell instanceof BlobDataCell) {
                m_indicesOfBlobInColumns = new int[row.getNumCells()];
            }
            writeDataCell(cell, outStream);
            outStream.endBlock();
        }
        outStream.endRow();
    }

    /** Writes the row key to the out stream. This method is overridden in
     * {@link NoKeyBuffer} in order to skip the row key.
     * @param key The key to write.
     * @param outStream To write to.
     * @throws IOException If that fails.
     */
    void writeRowKey(final RowKey key,
            final DCObjectOutputVersion2 outStream) throws IOException {
        if (shouldSkipRowKey()) {
            return;
        }
        outStream.writeRowKey(key);
        outStream.endBlock();
    }

    /** Writes a data cell to the outStream.
     * @param cell The cell to write.
     * @param outStream To write to.
     * @throws IOException If stream corruption happens.
     */
    void writeDataCell(final DataCell cell,
            final DCObjectOutputVersion2 outStream) throws IOException {
        if (cell.isMissing()) {
            outStream.writeControlByte(BYTE_TYPE_MISSING);
            return;
        }
        boolean isBlob = cell instanceof BlobWrapperDataCell;
        CellClassInfo cellClass = isBlob
        ? ((BlobWrapperDataCell)cell).getBlobClassInfo()
                : CellClassInfo.get(cell);
        DataCellSerializer<DataCell> ser = getSerializerForDataCell(cellClass);
        Byte identifier = m_typeShortCuts.get(cellClass);
        // DataCell is datacell-serializable
        if (ser != null || isBlob) {
            // memorize type if it does not exist
            outStream.writeControlByte(identifier);
            if (isBlob) {
                BlobWrapperDataCell bc = (BlobWrapperDataCell)cell;
                outStream.writeBlobAddress(bc.getAddress());
            } else {
                outStream.writeDataCellPerKNIMESerializer(ser, cell);
            }
        } else {
            outStream.writeControlByte(BYTE_TYPE_SERIALIZATION);
            outStream.writeControlByte(identifier);
            outStream.writeDataCellPerJavaSerialization(cell);
        }
    }

    /** Get the serializer object to be used for writing the argument cell
     * or <code>null</code> if it needs to be java-serialized.
     * @param cellClass The cell's class to write out.
     * @return The serializer to use or <code>null</code>.
     * @throws IOException If there are too many different cell 
     * implementations (currently 253 are theoretically supported)
     */
    private DataCellSerializer<DataCell> getSerializerForDataCell(
            final CellClassInfo cellClass) throws IOException {
        if (m_typeShortCuts == null) {
            m_typeShortCuts = new HashMap<CellClassInfo, Byte>();
        }
        @SuppressWarnings("unchecked")
        DataCellSerializer<DataCell> serializer =
            (DataCellSerializer<DataCell>)DataType.getCellSerializer(
                    cellClass.getCellClass());
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

    private void writeBlobDataCell(final BlobDataCell cell, final BlobAddress a,
            final DataCellSerializer<DataCell> ser) throws IOException {
        // addRow will make sure that m_indicesOfBlobInColumns is initialized
        // when this method is called. If this method is called from a different
        // buffer object, it means that this buffer has been closed!
        // (When can this happen? This buffer resides in memory, a successor
        // node is written to disc; they have different memory policies.)
        if (m_indicesOfBlobInColumns == null) {
            assert m_spec != null : "Spec is null, buffer not setup for write";
            m_indicesOfBlobInColumns = new int[m_spec.getNumColumns()];
        }
        int column = a.getColumn();
        int indexInColumn = m_indicesOfBlobInColumns[column]++;
        a.setIndexOfBlobInColumn(indexInColumn);
        boolean isToCompress = 
            isUseCompressionForBlobs(CellClassInfo.get(cell));
        File outFile = getBlobFile(indexInColumn, column, true, isToCompress);
        BlobAddress originalBA = cell.getBlobAddress();
        if (!ConvenienceMethods.areEqual(originalBA, a)) {
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
            // buffering the gzip stream brings another performance boost 
            // (in one case from 5mins down to 2 mins)
            out = new BufferedOutputStream(out);
        }
        DCObjectOutputVersion2 outStream = 
            new DCObjectOutputVersion2(out, this);
        try {
            if (ser != null) { // DataCell is datacell-serializable
                outStream.writeDataCellPerKNIMESerializer(ser, cell);
            } else {
                outStream.writeDataCellPerJavaSerialization(cell);
            }
        } finally {
            // do the best to minimize the number of open streams.
            outStream.close();
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
            final CellClassInfo cl) throws IOException {
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
        SoftReference<BlobDataCell> softRef = m_blobLRUCache.get(blobAddress);
        BlobDataCell result = softRef != null ? softRef.get() : null;
        if (result != null) {
            return result;
        }
        if (getReadVersion() <= 5) { // 2.0 TechPreview and earlier
            result = BufferFromFileIteratorVersion1x.readBlobDataCell(
                    this, blobAddress, cl);
        } else {
            result = new DataCellStreamReader(this).readBlobDataCell(
                    blobAddress, cl);
        }
        m_blobLRUCache.put(
                blobAddress, new SoftReference<BlobDataCell>(result));
        return result;
    }
    
    /** Perform lookup for the DataCell class info given the argument byte.
     * @param identifier The byte as read from the stream.
     * @return the associated cell class info
     * @throws IOException If the byte is invalid.
     */
    CellClassInfo getTypeForChar(final byte identifier)
        throws IOException {
        int shortCutIndex = (byte)(identifier - BYTE_TYPE_START);
        if (shortCutIndex < 0 || shortCutIndex >= m_shortCutsLookup.length) {
            throw new IOException("Unknown shortcut byte '" + identifier + "'");
        }
        return m_shortCutsLookup[shortCutIndex];
    }

    /** Creates short cut array and wraps the argument stream in a
     * {@link DCObjectOutputVersion2}. */
    private DCObjectOutputVersion2 initOutFile(
            final OutputStream outStream) throws IOException {
        OutputStream wrap;
        if (IS_USE_GZIP) {
            wrap = new GZIPOutputStream(outStream);
            // buffering the input stream is important as the blockable
            // stream, which will be put on top of it, reads bytes individually
            // (had a table, on which a single read-scan took ~6min without
            // and ~30s with buffering) 
            wrap = new BufferedOutputStream(wrap);
        } else {
            wrap = outStream;
        }
        return new DCObjectOutputVersion2(wrap, this);
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
            synchronized (this) {
                if (m_useBackIntoMemoryIterator) {
                    // the order of the following lines is very important!
                    m_useBackIntoMemoryIterator = false;
                    m_backIntoMemoryIterator = iterator();
                    m_list = new ArrayList<BlobSupportDataRow>(size());
                    return new FromListIterator();
                }
            }
            FromFileIterator f;
            try {
                if (getReadVersion() <= 5) { // 2.0 tech preview and before
                    f = new BufferFromFileIteratorVersion1x(this);
                } else {
                    f = new BufferFromFileIteratorVersion20(this);
                }
                m_nrOpenInputStreams.incrementAndGet();
                LOGGER.debug("Opening input stream on file \""
                        + m_binFile.getAbsolutePath() + "\", "
                        + m_nrOpenInputStreams + " open streams");
                synchronized (m_openIteratorSet) {
                    m_openIteratorSet.put(f, DUMMY);
                }
            } catch (IOException ioe) {
                StringBuilder b = new StringBuilder("Cannot read file \"");
                b.append(m_binFile != null ? m_binFile.getName() : "<unknown>");
                b.append("\"");
                throw new RuntimeException(b.toString(), ioe);
            }
            return f;
        } else {
            return new FromListIterator();
        }
    }

    /** True if any row containing blob cells is contained in this buffer.
     * @return if blob cells are present. */
    boolean containsBlobCells() {
        return m_containsBlobs;
    }
    
    /** Creates a clone of this buffer for writing the content to a stream
     * that is of the current version. 
     * @return A new buffer with the same ID, which is only used locally to
     * update the stream.
     */
    @SuppressWarnings("unchecked")
    Buffer createLocalCloneForWriting() {
        return new Buffer(0, getBufferID(), 
                getGlobalRepository(), Collections.EMPTY_MAP);
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
        CellClassInfo[] shortCutsLookup;
        if (!usesOutFile() || m_version < IVERSION) {
            // need to use new buffer since we otherwise write properties
            // of this buffer, which prevents it from further reading (version 
            // conflict) - see bug #1364
            Buffer copy = createLocalCloneForWriting();
            copy.m_outStream  = copy.initOutFile(
                    new NonClosableOutputStream.Zip(zipOut));
            int count = 1;
            for (RowIterator it = iterator(); it.hasNext();) {
                BlobSupportDataRow row = (BlobSupportDataRow)it.next();
                exec.setProgress(count / (double)size(), "Writing row "
                        + count + " (\"" + row.getKey() + "\")");
                exec.checkCanceled();
                // make a deep copy of blobs if we have a version hop
                copy.addRow(row, m_version < IVERSION, false);
                count++;
            }
            shortCutsLookup = copy.closeFile(copy.m_outStream);
            // bug fix #1631 ... the memory policy is not properly preserved
            // in this if-statement
            if (usesOutFile()) {
                // can safely be set to null because it wrote to stream already
                copy.m_list = null;
            }
            File blobDir = m_blobDir;
            // use the copy's blob dir if we have a version hop 
            // (otherwise its blob dir will be empty
            if (m_version < IVERSION) {
                blobDir = copy.m_blobDir;
            } else {
                assert copy.m_blobDir == null;
            }
            if (blobDir != null) {
                addBlobsToZip(ZIP_ENTRY_BLOBS, zipOut, blobDir);
            }
            zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
            zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_META));
            copy.writeMetaToFile(
                    new NonClosableOutputStream.Zip(zipOut), shortCutsLookup);
        } else {
            // no need for BufferedInputStream here as the copy method
            // does the buffering itself
            FileUtil.copy(new FileInputStream(m_binFile), zipOut);
            shortCutsLookup = m_shortCutsLookup;
            if (m_blobDir != null) {
                addBlobsToZip(ZIP_ENTRY_BLOBS, zipOut, m_blobDir);
            }
            zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
            zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_META));
            writeMetaToFile(
                    new NonClosableOutputStream.Zip(zipOut), shortCutsLookup);
        }
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
    
    /** Clear the argument iterator (free the allocated resources.
     * @param it The iterator
     * @param removeFromHash Whether to remove from global hash.
     */
    synchronized void clearIteratorInstance(final FromFileIterator it,
            final boolean removeFromHash) {
        String closeMes = (m_binFile != null) ? "Closing input stream on \""
            + m_binFile.getAbsolutePath() + "\", " : "";
        try {
            if (it.performClose()) {
                m_nrOpenInputStreams.decrementAndGet();
                logDebug(closeMes + m_nrOpenInputStreams + " remaining", null);
                if (removeFromHash) {
                    synchronized (m_openIteratorSet) {
                        m_openIteratorSet.remove(it);
                    }
                }
            }
        } catch (IOException ioe) {
            logDebug(closeMes + "failed!", ioe);
        }
    }

    /** Clears the temp file. Any subsequent iteration will fail! */
    void clear() {
        m_list = null;
        if (m_binFile != null) {
            synchronized (m_openIteratorSet) {
                for (FromFileIterator f : m_openIteratorSet.keySet()) {
                    if (f != null) {
                        clearIteratorInstance(f, false);
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
        if (m_blobLRUCache != null) {
            m_blobLRUCache.clear();
        }
        m_binFile = null;
        m_blobDir = null;
    }

    private static final int MAX_FILES_TO_CREATE_BEFORE_GC = 10000;
    private static final AtomicInteger FILES_CREATED_COUNTER =
        new AtomicInteger(0);

    /** Method being called each time a file is created. It maintains a counter
     * and calls each {@link #MAX_FILES_TO_CREATE_BEFORE_GC} files the garbage
     * collector. This fixes an unreported problem on windows, where (although
     * the file reference is null) there seems to be a hidden file lock,
     * which yields a "not enough system resources to perform operation" error.
     */
    private static void onFileCreated() {
        int count = FILES_CREATED_COUNTER.incrementAndGet();
        if (count % MAX_FILES_TO_CREATE_BEFORE_GC == 0) {
            LOGGER.debug("created " + count + " files, performing garbage "
                    + "collection to release handles");
            System.gc();
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
        protected synchronized boolean removeEldestEntry(
                final Entry<BlobAddress, SoftReference<BlobDataCell>> eldest) {
            return size() >= 100;
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
    
    /** Super class of all file iterators. */
    abstract static class FromFileIterator extends CloseableRowIterator 
    implements KNIMEStreamConstants {
        
        /** Called when the stream closing should take place.
         * @return Whether the stream close was actually performed (that is 
         * false when already closed.)
         * @throws IOException If closing fails.
         */
        abstract boolean performClose() throws IOException;
        
        /** {@inheritDoc} */
        @Override
        public abstract BlobSupportDataRow next();
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
            CloseableRowIterator backIntoMemoryIterator = 
                m_backIntoMemoryIterator;
            
            Object semaphore = backIntoMemoryIterator != null 
            	? backIntoMemoryIterator : FromListIterator.this;
            synchronized (semaphore) {
            	// need to synchronize access to the list as the list is 
            	// potentially modified by the backIntoMemoryIterator
            	if (m_nextIndex < m_list.size()) {
            		return m_list.get(m_nextIndex++);
            	}
            	if (backIntoMemoryIterator == null) {
            		throw new InternalError("DataRow list contains fewer "
            				+ " elements than buffer (" + m_list.size() 
            				+ " vs. " + size() + ")");
            	}
                // intermediate change possible (by other iterator)
                if (m_nextIndex < m_list.size()) {
                    return m_list.get(m_nextIndex++);
                }
                BlobSupportDataRow next = 
                    (BlobSupportDataRow)m_backIntoMemoryIterator.next();
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
            super("KNIME-Temp-File-Deleter");
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
