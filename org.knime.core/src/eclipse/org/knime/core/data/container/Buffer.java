/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.collection.BlobSupportDataCellIterator;
import org.knime.core.data.collection.CellCollection;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.BufferResource.BufferResourceRegistry;
import org.knime.core.data.container.DCObjectOutputVersion2.BlockableDCObjectOutputVersion2;
import org.knime.core.data.container.filter.FilterDelegateRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.data.container.storage.AbstractTableStoreReader.TableStoreCloseableRowIterator;
import org.knime.core.data.container.storage.AbstractTableStoreWriter;
import org.knime.core.data.container.storage.TableStoreFormat;
import org.knime.core.data.container.storage.TableStoreFormatRegistry;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.EmptyFileStoreHandler;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.ROWriteFileStoreHandler;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.data.util.memory.MemoryAlert;
import org.knime.core.data.util.memory.MemoryAlertListener;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LRUCache;
import org.knime.core.util.MutableBoolean;
import org.knime.core.util.ShutdownHelper;

/**
 * A buffer writes the rows from a {@link DataContainer} to a file. This class serves as connector between the
 * {@link DataContainer} and the {@link org.knime.core.data.DataTable} that is returned by the container. It
 * "centralizes" the IO operations.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Bernd Wiswedel, University of Konstanz
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class Buffer implements KNIMEStreamConstants {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(Buffer.class);

    /**
     * Default minimum disc space requirement, see {@link KNIMEConstants#PROPERTY_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB}.
     *
     * @since 2.8
     */
    private static final int DEF_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB = 100;

    /**
     * Minimum disc space requirement, see {@link KNIMEConstants#PROPERTY_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB}.
     *
     * @since 2.8
     */
    private static final int MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB;

    static {
        // initialize the min free disc in temp
        int minFreeDiscSpaceMB = DEF_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB;
        String minFree = System.getProperty(KNIMEConstants.PROPERTY_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB);
        if (minFree != null) {
            String s = minFree.trim();
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new NumberFormatException("minFreeDiscSpace < 0" + newSize);
                }
                minFreeDiscSpaceMB = newSize;
                LOGGER.debug("Setting min free disc space to " + minFreeDiscSpaceMB + "MB");
            } catch (NumberFormatException e) {
                LOGGER.warn("Unable to parse property \"" + KNIMEConstants.PROPERTY_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB
                    + "\", using default (" + DEF_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB + "MB)", e);
            }
        }
        MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB = minFreeDiscSpaceMB;
    }

    /**
     * True if ZipOutputStream / the underlying Zlib library supports changed compression level between entries. This
     * wasn't a problem for a long time (2004-2017) but broke with MacOX 10.13 (Sep '17). Relevant pointers are:
     * bugs.knime.org/AP-8083 https://www.knime.com/forum/knime-general/high-sierra-and-node-out-zip-files
     * https://github.com/madler/zlib/issues/305
     */
    private static final boolean ZLIB_SUPPORTS_LEVEL_SWITCH_AP8083;

    /**
     * Contains the information whether or not certain blob cell implementations shall be compressed when saved. This
     * information is retrieved from the field BlobDataCell#USE_COMPRESSION.
     */
    private static final Map<Class<? extends BlobDataCell>, Boolean> BLOB_COMPRESS_MAP =
        new HashMap<Class<? extends BlobDataCell>, Boolean>();

    /** Name of the zip entry containing the data. */
    static final String ZIP_ENTRY_DATA = "data.bin";

    /** Name of the zip entry containing the blob files (directory). */
    static final String ZIP_ENTRY_BLOBS = "blobs";

    /**
     * Name of the zip entry containing the filestore files (directory); only used when the buffer is isolated (not in
     * workflow).
     */
    static final String ZIP_ENTRY_FILESTORES = "filestores";

    /** Name of the zip entry containing the meta information (e.g. #rows). */
    static final String ZIP_ENTRY_META = "meta.xml";

    /**
     * Config entries when writing the meta information to the file, this is a subconfig in meta.xml.
     */
    private static final String CFG_INTERNAL_META = "table.meta.internal";

    /**
     * Config entries when writing the meta info to the file (uses NodeSettings object, which uses key-value pairs.
     * Here: the version of the writing method.
     */
    private static final String CFG_VERSION = "container.version";

    /** Config entry whether or not this buffer contains blobs. */
    private static final String CFG_CONTAINS_BLOBS = "container.contains.blobs";

    /**
     * Config entry (String) for buffer's filestore handler UUID, only applicable if buffer is not in workflow.
     */
    private static final String CFG_FILESTORES_UUID = "container.filestores.uuid";

    /** Config entry whether this buffer resides in memory. */
    private static final String CFG_IS_IN_MEMORY = "container.inmemory";

    /** Config entry: table format - full class name. */
    private static final String CFG_TABLE_FORMAT = "container.format";

    /** Config entry: table format internals -- implementation specific. */
    private static final String CFG_TABLE_FORMAT_CONFIG = "container.format.config";

    /** Config entry: internal buffer ID. */
    private static final String CFG_BUFFER_ID = "container.id";

    /**
     * Config entries when writing the spec to the file (uses NodeSettings object, which uses key-value pairs. Here:
     * size of the table (#rows).
     *
     * @deprecated because it's only an int
     */
    @Deprecated
    private static final String CFG_SIZE = "table.size";

    /**
     * Config entries when writing the spec to the file (uses NodeSettings object, which uses key-value pairs. Here:
     * size of the table (#rows).
     */
    private static final String CFG_SIZE_L = "table.size.long";

    /** Current version string. */
    public static final String VERSION = "container_12";

    /** The version number corresponding to {@link #VERSION}. */
    public static final int IVERSION = 12;

    private static final HashMap<String, Integer> COMPATIBILITY_MAP;

    static {
        COMPATIBILITY_MAP = new HashMap<String, Integer>();
        COMPATIBILITY_MAP.put("container_1.0.0", 1); // version 1.00
        COMPATIBILITY_MAP.put("container_1.1.0", 2); // version 1.1.x
        COMPATIBILITY_MAP.put("container_1.2.0", 3); // never released
        COMPATIBILITY_MAP.put("container_4", 4); // version 1.2.x - 1.3.x
        COMPATIBILITY_MAP.put("container_5", 5); // 2.0 TechPreview Version
        COMPATIBILITY_MAP.put("container_6", 6); // 2.0 Alpha
        COMPATIBILITY_MAP.put("container_7", 7); // 2.0.0 (final)
        COMPATIBILITY_MAP.put("container_8", 8); // version 2.0.1
        COMPATIBILITY_MAP.put("container_9", 9); // never released - some workflow tests contain it (BW used a nightly)
        COMPATIBILITY_MAP.put("container_10", 10); // version 3.6 (multiple table formats)
        COMPATIBILITY_MAP.put("container_11", 11); // version 3.7 - add FileStoreCell support for multiple FileStores
        COMPATIBILITY_MAP.put(VERSION, IVERSION); // version 3.8 - changed default compression to Snappy
        // NOTE consider to also
        // - increment the workflow.knime version number when updating this list
        // - update list in NoKeyBuffer
    }

    /**
     * Contains weak references to file iterators that have ever been created but not (yet) garbage collected. We will
     * add a shutdown hook (Runtime#addShutDownHook) and close the streams of all iterators that are open. This is a
     * workaround for bug #63 (see also http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4722539): Temp files are not
     * deleted on windows when there are open streams.
     */
    private static final Set<WeakReference<Buffer>> OPENBUFFERS =
        Collections.synchronizedSet(new HashSet<WeakReference<Buffer>>());

    /** Number of dirs/files per directory when blobs are saved. */
    private static final int BLOB_ENTRIES_PER_DIRECTORY = 1000;

    /**
     * Is executing the shutdown hook? If so, no logging is done, bug fix #862.
     */
    private static boolean isExecutingShutdownHook = false;

    /**
     * Adds a shutdown hook to the runtime that closes all open input streams
     */
    static {
        ZLIB_SUPPORTS_LEVEL_SWITCH_AP8083 = isZLIBSupportsLevelSwitchAP8083();
        if (!ZLIB_SUPPORTS_LEVEL_SWITCH_AP8083) {
            LOGGER.debug("Zlib library doesn't support compression level switch");
        }
        try {
            ShutdownHelper.getInstance().appendShutdownHook(() -> {
                isExecutingShutdownHook = true;
                for (WeakReference<Buffer> ref : OPENBUFFERS) {
                    Buffer it = ref.get();
                    if (it != null) {
                        it.clear();
                    }
                }
                DeleteInBackgroundThread.waitUntilFinished();
            });
        } catch (Exception e) {
            LOGGER.warn("Unable to add shutdown hook to delete temp files", e);
        }
    }

    /** See {@link KNIMEConstants#PROPERTY_TABLE_CACHE}. */
    static final String PROPERTY_TABLE_CACHE = KNIMEConstants.PROPERTY_TABLE_CACHE;

    /**
     * The default for whether to use the {@link SoftRefLRULifecycle} as opposed to the
     * {@link MemorizeIfSmallLifecycle}.
     */
    static final String DEF_TABLE_CACHE = "LRU";

    /**
     * Whether to use an SoftRefLRUAsyncWriteLifecycle as opposed to the MemorizeIfSmallLifecycle.
     *
     * @since 4.0
     * @noreference This field is not intended to be referenced by clients.
     */
    public static final boolean ENABLE_LRU;

    /** The running long for handing out unique buffer ids */
    private static final AtomicLong RUNNING_ID = new AtomicLong();

    static {
        final String envTableCache = PROPERTY_TABLE_CACHE;
        final String valTableCache = System.getProperty(envTableCache);
        String tableCache = DEF_TABLE_CACHE;
        if (valTableCache != null) {
            final String s = valTableCache.trim().toUpperCase();
            switch (tableCache) {
                case "LRU":
                case "SMALL":
                    tableCache = s;
                    break;
                default:
                    LOGGER.warn("Unknown setting for table caching: " + valTableCache + ". Using default: "
                        + DEF_TABLE_CACHE + ".");
            }
        }
        ENABLE_LRU = tableCache.equals("LRU");
    }

    /** See {@link KNIMEConstants#PROPERTY_DISCOURAGE_GC}. */
    private static final String PROPERTY_DISCOURAGE_GC = KNIMEConstants.PROPERTY_DISCOURAGE_GC;

    private static final boolean DEF_DISCOURAGE_GC = true;

    private static final boolean DISCOURAGE_GC;
    static {
        boolean discourageGc = DEF_DISCOURAGE_GC;
        final String discourageGcString = System.getProperty(PROPERTY_DISCOURAGE_GC);
        if (discourageGcString != null) {
            if (discourageGcString.equals("true")) {
                discourageGc = true;
            } else if (discourageGcString.equals("false")) {
                discourageGc = false;
            }
        }
        DISCOURAGE_GC = discourageGc;
    }

    /** A cache for holding tables in memory. */
    private static final BufferCache CACHE = new BufferCache();

    /** A single-threaded executor for asynchronous disk I/O threads. */
    static final ExecutorService ASYNC_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger m_threadCount = new AtomicInteger();

        /** {@inheritDoc} */
        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "KNIME-BackgroundTableWriter-" + m_threadCount.incrementAndGet());
        }
    });

    /**
     * Hash used to reduce the overhead of reading a blob cell over and over again. Useful in cases where a blob is
     * added multiple times to a table... the iterator will read the blob address, treat it as unseen and then ask the
     * owning Buffer to restore the blob.
     */
    private final BlobLRUCache m_blobLRUCache = new BlobLRUCache();

    static boolean isUseCompressionForBlobs(final CellClassInfo cellClassInfo) {
        @SuppressWarnings("unchecked")
        Class<? extends BlobDataCell> cl = (Class<? extends BlobDataCell>)cellClassInfo.getCellClass();
        Boolean result = BLOB_COMPRESS_MAP.get(cl);
        if (result != null) {
            return result;
        }
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
        } catch (final NoSuchFieldException | NullPointerException | IllegalAccessException | ClassCastException e) {
            LOGGER.coding("BlobDataCell interface \"" + cl.getSimpleName()
                + "\" seems to have a problem with the static field \"USE_COMPRESSION\"", e);
            // fall back - no meta information available
            result = Boolean.FALSE;
        }
        BLOB_COMPRESS_MAP.put(cl, result);
        return result.booleanValue();
    }

    /** the file to write to. */
    private File m_binFile;

    /** a flag that determines whether this Buffer has its own temporary m_binFile to write to */
    private boolean m_hasTempFile = true;

    /**
     * A flag that is set to true once this Buffer has been cleared and that is locked while the Buffer is being cleared
     * (to prevent concurrent clear operations and to prevent an {@link ASyncWriteCallable} from writing rows while the
     * buffer is concurrently cleared).
     */
    private MutableBoolean m_isClearedLock = new MutableBoolean(false);

    /** The directory where blob cells are stored or null if none available. */
    private File m_blobDir;

    /** true if any row contained in this buffer contains blob cells. */
    private boolean m_containsBlobs;

    /**
     * The integer id of a buffer associated with a workflow and created by a {@link BufferedDataTable}. This id is used
     * only for blob serialization and is not unique, as buffers might have the same id when (a) they are associated
     * with different workflows, (b) one is a clone of the other, or (c) they have been assigned the constant value
     * {@link DataContainer#NOT_IN_WORKFLOW_BUFFER} as a consequence of being created outside of a
     * {@link BufferedDataTable}.
     *
     * @see DataContainer#createInternalBufferID()
     */
    private final int m_bufferID;

    /**
     * The unique id of this buffer, as required for interacting with the static {@link BufferCache CACHE}. Will never
     * be null.
     */
    private final Long m_uniqueID = RUNNING_ID.getAndIncrement();

    /**
     * A map with other buffers that may have written certain blob cells. We reference them by using the bufferID that
     * is written to the file. This temporary repository exists only while a node is executing. It is only important
     * while writing to this buffer.
     */
    private Map<Integer, ContainerTable> m_localRepository;

    /** {@link #getDataRepository()}. */
    private final IDataRepository m_dataRepository;

    /** {@link #getFileStoreHandler()}. */
    private IFileStoreHandler m_fileStoreHandler;

    private TableStoreFormat m_outputFormat;

    private AbstractTableStoreWriter m_outputWriter;

    private AbstractTableStoreReader m_outputReader;

    /**
     * The settings for the table store format that describes how the table is persisted. That is:
     * <ul>
     * <li>while writing: null
     * <li>after write: the settings that the writer writes as part of {@link #closeInternal()}
     * <li>during read: the settings read in the constructor ({@link #readMetaFromFile(InputStream, File)})
     * </ul>
     */
    private NodeSettingsRO m_formatSettings;

    /** the current row count (how often has addRow been called). */
    private long m_size;

    /** The buffer settings. */
    private final BufferSettings m_bufferSettings;

    /** The lifecycle of this buffer, which specifies when and how tables are cached and flushed to disk. */
    private final Lifecycle m_lifecycle;

    /** A flag that is set when this buffer has been flushed to disk (for whatever reason). */
    private boolean m_flushedToDisk;

    /** maximum number of rows that are in memory. */
    private final int m_maxRowsInMem;

    /**
     * A table held in memory while still being modifiable and before being added to the cache. This is only ever true
     * when the writing buffer is not closed and rows are still being added to it. Setting this field to
     * <code>null</code> before the buffer has been closed will lead to a flushing of the buffer on the next access.
     * Such a prematurely flushed buffer will not be placed inside the cache ever.
     */
    private List<BlobSupportDataRow> m_listWhileAddRow;

    private int[] m_indicesOfBlobInColumns;

    /** the spec the rows comply with, no checking is done, however. */
    private DataTableSpec m_spec;

    /**
     * A registry that holds open resources owned by iterators iterating over this buffer. These resources are usually
     * released (and unregistered from the registry) when their auto-closeable owning iterators are closed. The purpose
     * of this data structure is to release resources also when these iterators are not closed properly. To this end, it
     * identifies resources whose owning iterators have been garbage-collected and releases such stale resources. In
     * addition, when the buffer is cleared, all registered resources are released.
     *
     * Specifically, the BufferResources are held by owners and registered in this registry as follows:
     *
     * (1) any <code>BackIntoMemoryTableDroppers</code> owned by <code>FromListIterators</code> and created in
     * <code>FromListIterator#setBackIntoMemoryIterator</code>, (2) any <code>TableStoreCloseableRowIterators</code>
     * owned by <code>FromListFallBackFromFileIterators</code> and created in
     * <code>FromListFallBackFromFileIterator#initFallBackFromFileIterator</code>, (3) a potential
     * <code>TableStoreCloseableRowIterator</code> owned by the <code>BackIntoMemoryIterator</code> and created in
     * <code>#iteratorWithFilter</code>, and (4) <code>TableStoreCloseableRowIterators</code> "owned" by itself and
     * created in <code>#iteratorWithFilter</code>.
     */
    private final BufferResourceRegistry m_openResources = new BufferResourceRegistry();

    /** Number of open file input streams on m_binFile. */
    private AtomicInteger m_nrOpenInputStreams = new AtomicInteger();

    /**
     * The iterator that is used to read the content back into memory. This instance is used after the workflow is
     * restored from disk or when a table that fits into the cache is dropped from the cache due to not having been used
     * recently. The BackIntoMemoryIterator is only weakly referenced here so that we can garbage-collect it and its
     * members when it isn't referenced any longer by and iterators.
     */
    private WeakReference<BackIntoMemoryIterator> m_backIntoMemoryIteratorRef;

    /**
     * A flag indicating whether the next call to iterator() hast to initialize m_backIntoMemoryIterator. This flag is
     * mostly false but may be true right after object initialization when the settings contain the
     * "was-previously-in-memory" flag.
     */
    private boolean m_useBackIntoMemoryIterator = false;

    /**
     * The version of the file we are reading (if initiated with Buffer(File, boolean). Used to remember when we need to
     * read a file which has been written with another version of the Buffer, i.e. to provide backward compatibility.
     */
    private int m_version = IVERSION;

    /**
     * Map of blob addresses that were copied into this buffer. It maps the original id to the new id (having m_bufferID
     * as owner). Copying is necessary if - we copy from or to a buffer with id "-1" or - this buffer is instructed to
     * copy all blobs (important for loop end nodes). If we didn't use such a map, we wouldn't notice if any one blob
     * gets copied into this buffer multiple times ... we would copy each time it is added, which is bad. This member is
     * null if this buffer is not in write-mode or it does not need to copy blobs.
     */
    private Map<BlobAddress, BlobAddress> m_copiedBlobsMap;

    /** To debug AP-8469 -- leaking Buffer objects when running text processing test workflows. */
    private final String m_fullStackTraceAtConstructionTime = Arrays.stream(Thread.currentThread().getStackTrace())
        .map(s -> s.toString()).collect(Collectors.joining("\n  "));

    /**
     * Creates new buffer for <strong>writing</strong>. It has assigned a given spec, and a max row count that may
     * resize in memory.
     *
     * @param spec ... used to define schema (non-KNIME file formats require schema)
     * @param maxRowsInMemory Maximum numbers of rows that are kept in memory until they will be subsequent written to
     *            the temp file. (0 to write immediately to a file)
     * @param bufferID The id of this buffer used for blob (de)serialization.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     * @param localRep Local table repository for blob (de)serialization.
     * @param fileStoreHandler the file store handlers
     * @param outputFormat the output table store format
     */
    Buffer(final DataTableSpec spec, final int maxRowsInMemory, final int bufferID,
        final IDataRepository dataRepository, final Map<Integer, ContainerTable> localRep,
        final IWriteFileStoreHandler fileStoreHandler) {
        this(spec, maxRowsInMemory, bufferID, dataRepository, localRep, fileStoreHandler, BufferSettings.getDefault());
    }

    /**
     * Creates new buffer for <strong>writing</strong>. It has assigned a given spec, and a max row count that may
     * resize in memory.
     *
     * @param spec ... used to define schema (non-KNIME file formats require schema)
     * @param maxRowsInMemory Maximum numbers of rows that are kept in memory until they will be subsequent written to
     *            the temp file. (0 to write immediately to a file)
     * @param bufferID The id of this buffer used for blob (de)serialization.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     * @param localRep Local table repository for blob (de)serialization.
     * @param fileStoreHandler the file store handlers
     * @param settings the {@link BufferSettings}
     */
    Buffer(final DataTableSpec spec, final int maxRowsInMemory, final int bufferID,
        final IDataRepository dataRepository, final Map<Integer, ContainerTable> localRep,
        final IWriteFileStoreHandler fileStoreHandler, final BufferSettings settings) {
        assert (maxRowsInMemory >= 0);
        m_flushedToDisk = false;
        m_bufferSettings = settings;
        m_maxRowsInMem = maxRowsInMemory;
        m_lifecycle = m_bufferSettings.useLRU() ? new SoftRefLRULifecycle() : new MemorizeIfSmallLifecycle();
        CACHE.setLRUCacheSize(m_bufferSettings.getLRUCacheSize());
        /**
         * independent of the lifecycle, if maxRowsInMemory is zero, the buffer is expected to flush to disk (e.g, see
         * {@link org.knime.core.data.sort.DataTableSorter#createDataContainer(DataTableSpec, boolean)}).
         */
        m_listWhileAddRow = maxRowsInMemory > 0 ? new ArrayList<BlobSupportDataRow>() : null;
        m_size = 0;
        m_bufferID = bufferID;
        m_localRepository = localRep;
        m_fileStoreHandler = fileStoreHandler;
        m_dataRepository = dataRepository;
        m_spec = spec;
        m_outputFormat = m_bufferSettings.getOutputFormat(m_spec);
        BufferTracker.getInstance().bufferCreated(this);
    }

    /**
     * Creates new buffer for <strong>reading</strong>. The <code>binFile</code> is the binary file as written by this
     * class, which will be deleted when this buffer is cleared or finalized.
     *
     * @param binFile The binary file to read from (will be deleted on exit).
     * @param blobDir temp directory containing blobs (may be null).
     * @param fileStoreDir ...
     * @param spec The data table spec to which the this buffer complies to.
     * @param metaIn An input stream from which this constructor reads the meta information (e.g. which byte encodes
     *            which DataCell).
     * @param bufferID The id of this buffer used for blob (de)serialization.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     * @throws IOException If the header (the spec information) can't be read.
     */
    Buffer(final File binFile, final File blobDir, final File fileStoreDir, final DataTableSpec spec,
        final InputStream metaIn, final int bufferID, final IDataRepository dataRepository) throws IOException {
        this(binFile, blobDir, fileStoreDir, spec, metaIn, bufferID, dataRepository, BufferSettings.getDefault());
    }

    /**
     * Creates new buffer for <strong>reading</strong>. The <code>binFile</code> is the binary file as written by this
     * class, which will be deleted when this buffer is cleared or finalized.
     *
     * @param binFile The binary file to read from (will be deleted on exit).
     * @param blobDir temp directory containing blobs (may be null).
     * @param fileStoreDir ...
     * @param spec The data table spec to which the this buffer complies to.
     * @param metaIn An input stream from which this constructor reads the meta information (e.g. which byte encodes
     *            which DataCell).
     * @param bufferID The id of this buffer used for blob (de)serialization.
     * @param dataRepository the data repository (needed for blobs, file stores, and table ids)
     * @param settings the {@link BufferSettings}
     * @throws IOException If the header (the spec information) can't be read.
     */
    Buffer(final File binFile, final File blobDir, final File fileStoreDir, final DataTableSpec spec,
        final InputStream metaIn, final int bufferID, final IDataRepository dataRepository,
        final BufferSettings settings) throws IOException {
        // just check if data is present!
        if (binFile == null || !binFile.canRead() || !binFile.isFile()) {
            throw new IOException("Unable to read from file: " + binFile);
        }
        m_spec = spec;
        m_binFile = binFile;
        m_blobDir = blobDir;
        m_bufferID = bufferID;
        if (dataRepository == null) {
            LOGGER
                .debug("no data repository set, using new instance of " + NotInWorkflowDataRepository.class.getName());
            m_dataRepository = NotInWorkflowDataRepository.newInstance();
        } else {
            m_dataRepository = dataRepository;
        }
        if (metaIn == null) {
            throw new IOException("No meta information given (null)");
        }
        m_flushedToDisk = true;
        m_bufferSettings = settings;
        m_maxRowsInMem = 0;
        m_lifecycle = m_bufferSettings.useLRU() ? new SoftRefLRULifecycle() : new MemorizeIfSmallLifecycle();
        CACHE.setLRUCacheSize(m_bufferSettings.getLRUCacheSize());
        try {
            readMetaFromFile(metaIn, fileStoreDir);
        } catch (InvalidSettingsException ise) {
            String message = "Unable to read meta information from file.";
            final String causeMessage = ise.getMessage();
            if (causeMessage != null) {
                message += " " + causeMessage;
            }
            throw new IOException(message, ise);
        }
        BufferTracker.getInstance().bufferCreated(this);
    }

    /**
     * Get the version string to write to the meta file. This method is overridden in the {@code NoKeyBuffer} to
     * distinguish streams written by the different implementations.
     *
     * @return The version string.
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Get underlying stream version. Important for file iterators.
     *
     * @return Underlying stream version.
     */
    final int getReadVersion() {
        return m_version;
    }

    /** @return Underlying binary file. */
    final File getBinFile() {
        return m_binFile;
    }

    /** @return the outputFormat, not null */
    final TableStoreFormat getOutputFormat() {
        return m_outputFormat;
    }

    /**
     * Validate the version as read from the file if it can be parsed by this implementation. If unknown, uses latest
     * known version (good luck).
     *
     * @param version As read from file.
     * @return The version ID for internal use.
     */
    int validateVersion(final String version) {
        Integer iVersion = COMPATIBILITY_MAP.get(version);
        if (iVersion == null) {
            LOGGER.warn("Unknown version string in persisted table file (\"" + version
                + "\") - was table created with a future version of KNIME? Using \"" + VERSION + "\" modus.");
            iVersion = IVERSION;
        }
        if (iVersion < IVERSION) {
            LOGGER.debug("Table has been written with a previous version of KNIME (\"" + version
                + "\", using compatibility mode.");
        }
        return iVersion;
    }

    /**
     * Adds a row to the buffer. The rows structure is not validated against the table spec that was given in the
     * constructor. This should have been done in the caller class <code>DataContainer</code>.
     *
     * @param r The row to be added.
     * @param isCopyOfExisting Whether to copy blobs (this is only true when and existing buffer gets copied (version
     *            hop))
     * @param forceCopyOfBlobs If true any blob that is not owned by this buffer, will be copied and this buffer will
     *            take ownership. This option is true for loop end nodes, which need to aggregate the data generated in
     *            the loop body
     */
    synchronized void addRow(final DataRow r, final boolean isCopyOfExisting, final boolean forceCopyOfBlobs) {
        try {
            BlobSupportDataRow row = saveBlobsAndFileStores(r, isCopyOfExisting, forceCopyOfBlobs);
            addBlobSupportDataRow(row);
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                LOGGER.coding("Writing cells to temporary buffer must not throw " + e.getClass().getSimpleName(), e);
            }
            StringBuilder builder = new StringBuilder("Error while writing to buffer");
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
    }

    synchronized void addBlobSupportDataRow(final BlobSupportDataRow row) throws IOException {
        if (getAndIncrementSize() == Integer.MAX_VALUE) {
            /**
             * Since m_list is an ArrayList, it cannot hold more than Integer.MAX_VALUE rows, so we have to flush
             * independent of the lifecycle.
             */
            flushBuffer();
        }
        if (m_listWhileAddRow != null) {
            m_listWhileAddRow.add(row);
            if (m_listWhileAddRow.size() > m_maxRowsInMem) {
                m_lifecycle.onAddRowToLargeList();
            }
        } else {
            flushBuffer();
            m_outputWriter.writeRow(row);
        }
    }

    /**
     * @throws IOException
     */
    private void initOutputWriter(final Supplier<OutputStream> output)
        throws IOException, UnsupportedOperationException {
        m_outputWriter = m_outputFormat.createWriter(output.get(), m_spec, !shouldSkipRowKey());
        m_outputWriter.setFileStoreHandler((IWriteFileStoreHandler)m_fileStoreHandler);
    }

    /**
     * @throws IOException
     */
    private void initOutputWriter(final File binFile) throws IOException {
        m_outputWriter = m_outputFormat.createWriter(binFile, m_spec, !shouldSkipRowKey());
        m_outputWriter.setFileStoreHandler((IWriteFileStoreHandler)m_fileStoreHandler);
    }

    BlobSupportDataRow saveBlobsAndFileStores(final DataRow row, final boolean forceCopyOfBlobs) throws IOException {
        final BlobSupportDataRow blobRow = saveBlobsAndFileStores(row, false, forceCopyOfBlobs);
        return blobRow;
    }

    private BlobSupportDataRow saveBlobsAndFileStores(final DataRow row, final boolean isCopyOfExisting,
        final boolean forceCopyOfBlobs) throws IOException {

        final int cellCount = row.getNumCells();
        final boolean isBlobRow = row instanceof BlobSupportDataRow;
        final BlobSupportDataRow blobRow = isBlobRow ? (BlobSupportDataRow)row : null;

        DataCell[] cellCopies = null;
        if (!isBlobRow) {
            cellCopies = new DataCell[cellCount];
            for (int i = 0; i < cellCount; i++) {
                cellCopies[i] = row.getCell(i);
            }
        }

        // take ownership of unassigned blob cells (if any)
        for (int col = 0; col < cellCount; col++) {
            @SuppressWarnings("null")
            DataCell cell = isBlobRow ? blobRow.getRawCell(col) : cellCopies[col];

            final boolean isWrapperCell = cell instanceof BlobWrapperDataCell;
            final boolean isCollectionCell = cell instanceof CellCollection;

            DataCell processedCell = handleIncomingBlob(cell, col, cellCount, isCopyOfExisting, forceCopyOfBlobs,
                isWrapperCell, isCollectionCell);

            // e.g. loop end nodes need to flush the data to disc in case the loop defines file stores,
            // otherwise file store data is transient and won't be saved when the loop is reset or the workflow
            // is saved (right to left)
            if (mustBeFlushedPriorSave(processedCell, isWrapperCell, isCollectionCell)) {
                if (!isFlushedToDisk()) {
                    LOGGER.debug("Forcing buffer to disc as it contains file store cells that need special handling");
                    flushBuffer();
                }
            }
            if (processedCell != cell) {
                if (cellCopies == null) {
                    cellCopies = new DataCell[cellCount];
                    for (int i = 0; i < cellCount; i++) {
                        // cellCopies can only be null for blob rows
                        @SuppressWarnings("null")
                        final DataCell rawCell = blobRow.getRawCell(i);
                        cellCopies[i] = rawCell;
                    }
                }
                cellCopies[col] = processedCell;
            }
        }
        return cellCopies == null ? blobRow : new BlobSupportDataRow(row.getKey(), cellCopies);
    }

    private DataCell handleIncomingBlob(final DataCell cell, final int col, final int totalColCount,
        final boolean copyForVersionHop, final boolean forceCopyOfBlobsArg, final boolean isWrapperCell,
        final boolean isCollectionCell) throws IOException {
        if (!isWrapperCell && !(cell instanceof BlobDataCell)) {
            if (isCollectionCell) {
                CellCollection cdv = (CellCollection)cell;
                if (cdv.containsBlobWrapperCells()) {
                    Iterator<DataCell> it = cdv.iterator();
                    if (!(it instanceof BlobSupportDataCellIterator)) {
                        LOGGER.coding("(Collection) DataCell of class \"" + cell.getClass().getSimpleName()
                            + "\" contains Blobs, but does not return an iterator supporting those (expected "
                            + BlobSupportDataCellIterator.class.getName() + ", got " + it.getClass().getName() + ")");
                    }
                    while (it.hasNext()) {
                        DataCell n = it instanceof BlobSupportDataCellIterator
                            ? ((BlobSupportDataCellIterator)it).nextWithBlobSupport() : it.next();
                        DataCell correctedCell = handleIncomingBlob(n, col, totalColCount, copyForVersionHop,
                            forceCopyOfBlobsArg, n instanceof BlobWrapperDataCell, n instanceof CellCollection);
                        if (correctedCell != n) {
                            if (it instanceof BlobSupportDataCellIterator) {
                                BlobSupportDataCellIterator bsdi = (BlobSupportDataCellIterator)it;
                                bsdi.replaceLastReturnedWithWrapperCell(correctedCell);
                            } else {
                                // coding problem was reported above.
                            }
                        }
                    }
                }
                return cell;
            } else {
                return cell; // ordinary cell (e.g. double cell)
            }
        }
        // at this point cell can only be a WrapperCell or BlobDataCell
        synchronized (this) {
            BlobAddress ad;
            final CellClassInfo cl;
            BlobWrapperDataCell wc;

            // treat Wrapper and BlobDataCell differently
            if (isWrapperCell) {
                wc = (BlobWrapperDataCell)cell;
                ad = wc.getAddress();
                cl = wc.getBlobClassInfo();
            } else {
                wc = null;
                cl = CellClassInfo.get(cell);
                ad = ((BlobDataCell)cell).getBlobAddress();
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
                    Optional<ContainerTable> t = m_dataRepository.getTable(ad.getBufferID());
                    ContainerTable containerTable = t.orElse(null);
                    if (containerTable != null) {
                        ownerBuffer = ((BufferedContainerTable)containerTable).getBuffer();
                    } else {
                        ownerBuffer = null;
                    }
                }
                /* this can only be true if the argument row contains wrapper
                 * cells for blobs that do not have a buffer set; that is,
                 * someone took a BlobDataCell from a predecessor node
                 * (ad != null) and put it manually into a new wrapper cell
                 * (wc != null) - by doing that you loose the buffer info
                 * (wc.getBuffer == null) */
                if (isWrapperCell) {
                    @SuppressWarnings("null")
                    final Buffer buf = wc.getBuffer();
                    if (buf == null) {
                        wc.setAddressAndBuffer(ad, ownerBuffer);
                    }
                }
            } else {
                ownerBuffer = null;
            }
            // if we have to make a clone of the blob cell (true if
            // isCopyOfExisting is true and the blob address corresponds to the next
            // assignable m_indicesOfBlobInColumns[col])
            boolean isToCloneForVersionHop = false;
            if (copyForVersionHop) {
                if (ad != null && ad.getBufferID() == getBufferID()) {
                    isToCloneForVersionHop = true;
                    // this if statement handles cases where a blob is added to the
                    // buffer multiple times -- don't copy the duplicates
                    if (m_indicesOfBlobInColumns == null) {
                        // first to assign
                        isToCloneForVersionHop = ad.getIndexOfBlobInColumn() == 0;
                        assert isToCloneForVersionHop : "Clone of buffer does not return blobs in order";
                    } else {
                        isToCloneForVersionHop = ad.getIndexOfBlobInColumn() == m_indicesOfBlobInColumns[col];
                    }
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
                        return new BlobWrapperDataCell(this, previousCopyAddress, cl);
                    }
                }
            }

            // if either not previously assigned (ownerBuffer == null) or
            // we have to make a clone
            if (ownerBuffer == null || isToCloneForVersionHop || isToCloneDueToForceCopyOfBlobs) {
                // need to set ownership if this blob was not assigned yet
                // or has been assigned to an unlinked (i.e. local) buffer
                boolean isCompress = ad != null ? ad.isUseCompression() : isUseCompressionForBlobs(cl);
                BlobAddress rewrite = new BlobAddress(m_bufferID, col, isCompress);
                if (ad == null) {
                    // take ownership
                    if (isWrapperCell) {
                        ((BlobWrapperDataCell)cell).setAddressAndBuffer(rewrite, this);
                    } else {
                        ((BlobDataCell)cell).setBlobAddress(rewrite);
                    }
                    ad = rewrite;
                }
                if (m_indicesOfBlobInColumns == null) {
                    m_indicesOfBlobInColumns = new int[totalColCount];
                }
                Buffer b = null; // to buffer to copy the blob from (if at all)
                if (isToCloneDueToForceCopyOfBlobs) {
                    b = ownerBuffer;
                    m_copiedBlobsMap.put(ad, rewrite);
                } else {
                    ContainerTable tbl = m_localRepository.get(ad.getBufferID());
                    b = tbl == null ? null : ((BufferedContainerTable)tbl).getBuffer();
                }
                if (b != null && !isToCloneForVersionHop) {
                    int indexBlobInCol = m_indicesOfBlobInColumns[col]++;
                    rewrite.setIndexOfBlobInColumn(indexBlobInCol);
                    File source = b.getBlobFile(ad.getIndexOfBlobInColumn(), ad.getColumn(), false, ad.isUseCompression());
                    File dest = getBlobFile(indexBlobInCol, col, true, ad.isUseCompression());
                    FileUtil.copy(source, dest);
                    wc = new BlobWrapperDataCell(this, rewrite, cl);
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
                        if (m_outputWriter == null) {
                            ensureTempFileExists();
                            initOutputWriter(m_binFile);
                        }
                        writeBlobDataCell(bc, rewrite);
                        wc = new BlobWrapperDataCell(this, rewrite, cl, bc);
                    } else {
                        wc = new BlobWrapperDataCell(this, rewrite, cl);
                    }
                }
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
    }



    private void writeBlobDataCell(final BlobDataCell cell, final BlobAddress a) throws IOException {
        DataCellSerializer<DataCell> ser = m_outputWriter.getSerializerForDataCell(CellClassInfo.get(cell));
        // addRow will make sure that m_indicesOfBlobInColumns is initialized
        // when this method is called. If this method is called from a different
        // buffer object, it means that this buffer has been closed!
        // (When can this happen? This buffer resides in memory, a successor
        // node is written to disc; they have different memory policies.)
        if (m_indicesOfBlobInColumns == null) {
            m_indicesOfBlobInColumns = new int[m_spec.getNumColumns()];
        }
        int column = a.getColumn();
        int indexInColumn = m_indicesOfBlobInColumns[column]++;
        a.setIndexOfBlobInColumn(indexInColumn);
        boolean isToCompress = Buffer.isUseCompressionForBlobs(CellClassInfo.get(cell));
        File outFile = getBlobFile(indexInColumn, column, true, isToCompress);
        BlobAddress originalBA = cell.getBlobAddress();
        if (!Objects.equals(originalBA, a)) {
            int originalBufferIndex = originalBA.getBufferID();
            Buffer originalBuffer = null;
            Optional<ContainerTable> tableOptional = getDataRepository().getTable(originalBufferIndex);
            if (tableOptional.isPresent()) {
                originalBuffer = ((BufferedContainerTable)tableOptional.get()).getBuffer();
            } else if (getLocalRepository() != null) {
                ContainerTable t = getLocalRepository().get(originalBufferIndex);
                if (t != null) {
                    originalBuffer = ((BufferedContainerTable)t).getBuffer();
                }
            }
            if (originalBuffer != null) {
                int index = originalBA.getIndexOfBlobInColumn();
                int col = originalBA.getColumn();
                boolean compress = originalBA.isUseCompression();
                File source = originalBuffer.getBlobFile(index, col, false, compress);
                FileUtil.copy(source, outFile);
                return;
            }
        }

        try (final OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
                final BlockableDCObjectOutputVersion2 outStream = new BlockableDCObjectOutputVersion2(
                    isToCompress ? new BufferedOutputStream(new GZIPOutputStream(out)) : out)) {
            // buffering the gzip stream brings another performance boost
            // (in one case from 5mins down to 2 mins)
            Buffer.onFileCreated(outFile);
            if (ser != null) { // DataCell is datacell-serializable
                outStream.writeDataCellPerKNIMESerializer(ser, cell);
            } else {
                outStream.writeDataCellPerJavaSerialization(cell);
            }
        }
    }

    private boolean mustBeFlushedPriorSave(final DataCell cell, final boolean isWrapperCell,
        final boolean isCollectionCell) {
        if (cell instanceof FileStoreCell) {
            FileStore[] fileStores = FileStoreUtil.getFileStores((FileStoreCell)cell);
            for (FileStore fs : fileStores) {
                if (((IWriteFileStoreHandler)m_fileStoreHandler).mustBeFlushedPriorSave(fs)) {
                    return true;
                }
            }
        } else if (isCollectionCell) {
            if (cell instanceof CollectionDataValue) {
                for (DataCell c : (CollectionDataValue)cell) {
                    if (mustBeFlushedPriorSave(c, c instanceof BlobWrapperDataCell, c instanceof CellCollection)) {
                        return true;
                    }
                }
            }
        } else if (isWrapperCell) {
            final BlobWrapperDataCell blobWrapperCell = (BlobWrapperDataCell)cell;
            Class<? extends BlobDataCell> blobClass = blobWrapperCell.getBlobClass();
            if (CollectionDataValue.class.isAssignableFrom(blobClass)) {
                DataCell c = blobWrapperCell.getCell();
                return mustBeFlushedPriorSave(c, c instanceof BlobWrapperDataCell, c instanceof CellCollection);
            }
        }
        return false;
    }

    /** Creates temp file (m_binFile) and adds this buffer to shutdown hook. */
    private void ensureTempFileExists() throws IOException {
        if (m_binFile == null) {
            m_binFile = DataContainer.createTempFile(m_outputFormat.getFilenameSuffix());
            OPENBUFFERS.add(new WeakReference<Buffer>(this));
        }
    }

    /**
     * Increments the row counter by one, used in addRow.
     *
     * @return previous size (before incrementing it).
     */
    private long getAndIncrementSize() {
        return m_size++;
    }

    /**
     * Flushes and closes the stream. If no file has been created and therefore everything fits in memory (according to
     * the settings in the constructor), it will stay in memory (no file created).
     *
     * @param spec The spec the rows have to follow. No sanity check is done.
     */
    synchronized void close(final DataTableSpec spec) {
        assert spec != null : "Buffer is not open.";
        closeInternal();
        m_spec = spec;
    }

    /** Closes by creating shortcut array for file access. */
    void closeInternal() {
        assert Thread.holdsLock(this);
        if (m_listWhileAddRow != null) {
            // buffer still held in memory; can be cached
            CACHE.put(Buffer.this, m_listWhileAddRow);
            m_listWhileAddRow = null;
            m_lifecycle.onCloseIfCached();
        } else {
            // buffer has been flushed during initialization or by DC due to low memory event
            flushBuffer();
            closeWriterAndWriteMeta();
        }
        m_localRepository = null;
    }

    private void ensureWriterIsOpen() throws IOException {
        if (m_hasTempFile) {
            ensureTempFileExists();
            if (m_outputWriter == null) {
                if (!m_binFile.getParentFile().isDirectory()) {
                    throw new FileNotFoundException(
                        "Directory " + m_binFile.getParentFile() + " for buffer " + m_bufferID + " does not exist");
                }

                initOutputWriter(m_binFile);
                Buffer.onFileCreated(m_binFile);
            }
        }
        m_flushedToDisk = true;
    }

    private void closeWriterAndWriteMeta() {
        try {
            m_outputWriter.close();
            NodeSettings formatSettings = new NodeSettings(CFG_TABLE_FORMAT_CONFIG);
            m_outputWriter.writeMetaInfoAfterWrite(formatSettings);
            m_formatSettings = formatSettings;
            if (m_hasTempFile) {
                double sizeInMB = m_binFile.length() / (double)(1 << 20);
                String size = NumberFormat.getInstance().format(sizeInMB);
                LOGGER.debug("Buffer file (" + m_binFile.getAbsolutePath() + ") is " + size + "MB in size");
                initOutputReader(formatSettings, IVERSION);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot close stream of file \"" + m_binFile.getName() + "\"", ioe);
        } catch (InvalidSettingsException ex) {
            throw new RuntimeException("Cannot init reader after buffer is closed", ex);
        }
    }

    /**
     * Writes internals to the an output stream (using the xml scheme from NodeSettings).
     *
     * @param out To write to.
     * @throws IOException If that fails.
     */
    private void writeMetaToFile(final Supplier<OutputStream> out) throws IOException {
        NodeSettings settings = new NodeSettings("Table Meta Information");
        NodeSettingsWO subSettings = settings.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addString(CFG_VERSION, getVersion());
        if (size() < Integer.MAX_VALUE) {
            subSettings.addInt(CFG_SIZE, (int)size());
        } else {
            subSettings.addLong(CFG_SIZE_L, size());
        }
        subSettings.addBoolean(CFG_CONTAINS_BLOBS, m_containsBlobs);
        // added between version 8 and 9 - no increment of version number
        String fileStoresUUID = null;
        if (m_fileStoreHandler instanceof NotInWorkflowWriteFileStoreHandler) {
            NotInWorkflowWriteFileStoreHandler notInWorkflowFSH =
                (NotInWorkflowWriteFileStoreHandler)m_fileStoreHandler;
            if (notInWorkflowFSH.hasCopiedFileStores()) {
                fileStoresUUID = notInWorkflowFSH.getStoreUUID().toString();
            }
        }
        subSettings.addString(CFG_FILESTORES_UUID, fileStoresUUID);
        subSettings.addBoolean(CFG_IS_IN_MEMORY, m_lifecycle.shallLoadBackIntoMemory());
        subSettings.addInt(CFG_BUFFER_ID, m_bufferID);
        subSettings.addString(CFG_TABLE_FORMAT, m_outputFormat.getClass().getName());
        NodeSettingsWO formatSettings = subSettings.addNodeSettings(CFG_TABLE_FORMAT_CONFIG);
        m_formatSettings.copyTo(formatSettings);
        if (m_outputWriter instanceof DefaultTableStoreWriter) {
            // AP-8954 -- for standard KNIME tables write the meta information into the root so that 3.5 and before
            // can load it;
            // these settings are no longer read in newer versions of KNIME (3.6+) -- attempt of forward compatibility
            m_formatSettings.copyTo(subSettings);
        }
        settings.saveToXML(out.get());
    }

    /**
     * Reads meta information, that is row count, version, byte assignments.
     *
     * @param metaIn To read from.
     * @throws IOException If reading fails.
     * @throws ClassNotFoundException If any of the classes can't be loaded.
     * @throws InvalidSettingsException If the internal structure is broken.
     */
    private void readMetaFromFile(final InputStream metaIn, final File fileStoreDir)
        throws IOException, InvalidSettingsException {
        try (InputStream inStream = new BufferedInputStream(metaIn)) {
            NodeSettingsRO settings = NodeSettings.loadFromXML(inStream);
            NodeSettingsRO subSettings = settings.getNodeSettings(CFG_INTERNAL_META);
            String version = subSettings.getString(CFG_VERSION);
            m_version = validateVersion(version);
            if (subSettings.containsKey(CFG_SIZE_L)) {
                m_size = subSettings.getLong(CFG_SIZE_L);
            } else {
                m_size = subSettings.getInt(CFG_SIZE);
            }
            if (m_size < 0) {
                throw new IOException("Table size must not be < 0: " + m_size);
            }
            // added sometime between format 8 and 9
            m_containsBlobs = false;
            if (m_version >= 4) { // no blobs in version 1.1.x
                m_containsBlobs = subSettings.getBoolean(CFG_CONTAINS_BLOBS);
                int bufferID = subSettings.getInt(CFG_BUFFER_ID);
                // the bufferIDs may be different in cases when an 1.0.0 table
                // was read, then converted to a new version (done by
                // addToZipFile) and saved. Reading these tables will have a
                // bufferID of -1. 1.0.0 contain no blobs, so that's ok.
                if (m_containsBlobs && bufferID != m_bufferID) {
                    LOGGER.error("Table's buffer id is different from what has been passed in constructor (" + bufferID
                        + " vs. " + m_bufferID + "), unpredictable errors may occur");
                }
            }
            IFileStoreHandler fileStoreHandler = new EmptyFileStoreHandler(m_dataRepository);
            if (m_version >= 8) { // file stores added between version 8 and 9
                String fileStoresUUIDS = subSettings.getString(CFG_FILESTORES_UUID, null);
                UUID fileStoresUUID = null;
                if (fileStoresUUIDS != null) {
                    try {
                        fileStoresUUID = UUID.fromString(fileStoresUUIDS);
                    } catch (IllegalArgumentException iae) {
                        throw new InvalidSettingsException("Can't parse UUID " + fileStoresUUIDS, iae);
                    }
                }
                if (fileStoresUUID != null) {
                    NotInWorkflowWriteFileStoreHandler notInWorkflowFSH =
                        new NotInWorkflowWriteFileStoreHandler(fileStoresUUID, m_dataRepository);
                    notInWorkflowFSH.setBaseDir(fileStoreDir);
                    fileStoreHandler = notInWorkflowFSH;
                }
            }
            m_fileStoreHandler = fileStoreHandler;
            if (m_version >= 8) { // no back into memory option prior 2.0
                // the "back into memory" option was added in buffer
                // version 5 (2.0 TechPreview Version), though it has a bug
                // (#1631) which caused all data to be held in memory when
                // converted into 2.0 schema. Therefore we enable this feature
                // with buffer version 8 (> 2.0)
                if (subSettings.getBoolean(CFG_IS_IN_MEMORY)) {
                    setRestoreIntoMemoryOnCacheMiss();
                }
            }
            String outputFormat = subSettings.getString(CFG_TABLE_FORMAT, DefaultTableStoreFormat.class.getName());
            m_outputFormat = TableStoreFormatRegistry.getInstance().getTableStoreFormat(outputFormat);
            NodeSettingsRO outputFormatSettings =
                m_version >= 10 ? subSettings.getNodeSettings(CFG_TABLE_FORMAT_CONFIG) : subSettings;
            m_formatSettings = outputFormatSettings;
            initOutputReader(outputFormatSettings, m_version);
        }
    }

    /**
     * @param outputFormatSettings
     * @param version
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private void initOutputReader(final NodeSettingsRO outputFormatSettings, final int version)
        throws IOException, InvalidSettingsException {
        m_outputReader = m_outputFormat.createReader(m_binFile, m_spec, m_dataRepository, outputFormatSettings, version,
            !shouldSkipRowKey());
        m_outputReader.setBufferAndDataRepository(this, m_dataRepository);
    }

    /**
     * Have all data rows that we have encountered so far been written to disk? This is true for reading buffers in
     * general, but is also true for writing buffers if the table store writer has already been opened in anticipation
     * of data rows being added, yet no rows have actually been added to the buffer (i.e, the total number of data rows
     * encountered so far is zero).
     *
     * @return true if the table held by the buffer has been flushed to disk
     */
    boolean isFlushedToDisk() {
        return m_flushedToDisk;
    }

    /**
     * Does the buffer reside in memory? Use with caution and consider: if the table is held in the cache but has been
     * cleared for garbage collection, this method will return <code>true</code>, even though the table could be
     * garbage-collected shortly.
     *
     * @return true if the table held by the buffer is held in memory
     */
    boolean isHeldInMemory() {
        return m_listWhileAddRow != null || CACHE.contains(this);
    }

    /**
     * Get the table spec that was set in the constructor.
     *
     * @return The spec the buffer uses.
     */
    public DataTableSpec getTableSpec() {
        return m_spec;
    }

    /**
     * Get the row count.
     *
     * @return How often has addRow() been called.
     */
    public long size() {
        return m_size;
    }

    /**
     * Get whether the buffer wants to persist row keys. Here hard-coded to <code>true</code> but overwritten in
     * {@link NoKeyBuffer}.
     *
     * @return whether row keys needs to be written/read.
     */
    boolean shouldSkipRowKey() {
        return false;
    }

    /**
     * Restore content of this buffer into main memory (using a collection implementation). The restoring will be
     * performed with the next iteration.
     */
    final synchronized void setRestoreIntoMemoryOnCacheMiss() {
        m_useBackIntoMemoryIterator = true;
    }

    /**
     * Used while reading file store cells and referenced tables and blobs.
     *
     * @return the data repository set at construction time
     */
    public final IDataRepository getDataRepository() {
        return m_dataRepository;
    }

    /**
     * Get reference to the local table repository that this buffer was initially instantiated with. Used for blob
     * reading/writing. This may be null.
     *
     * @return (Workflow-) global table repository.
     */
    final Map<Integer, ContainerTable> getLocalRepository() {
        return m_localRepository;
    }

    /**
     * Reads the blob from the given blob address.
     *
     * @param blobAddress The address to read from.
     * @param cl The expected class.
     * @return The blob cell being read.
     * @throws IOException If that fails.
     */
    BlobDataCell readBlobDataCell(final BlobAddress blobAddress, final CellClassInfo cl) throws IOException {
        int blobBufferID = blobAddress.getBufferID();
        if (blobBufferID != m_bufferID) {
            ContainerTable cnTbl = m_dataRepository.getTable(blobBufferID)
                .orElseThrow(() -> new IOException("Unable to retrieve table that owns the blob cell"));
            Buffer blobBuffer =( (BufferedContainerTable)cnTbl).getBuffer();
            return blobBuffer.readBlobDataCell(blobAddress, cl);
        }
        SoftReference<BlobDataCell> softRef = m_blobLRUCache.get(blobAddress);
        BlobDataCell result = softRef != null ? softRef.get() : null;
        if (result != null) {
            return result;
        }
        if (getReadVersion() <= 5) { // 2.0 TechPreview and earlier
            result = BufferFromFileIteratorVersion1x.readBlobDataCell(this, blobAddress, cl);
        } else {
            result = BufferFromFileIteratorVersion20.readBlobDataCell(blobAddress, cl, this);
        }
        m_blobLRUCache.put(blobAddress, new SoftReference<BlobDataCell>(result));
        return result;
    }

    private void ensureBlobDirExists() throws IOException {
        if (m_blobDir == null) {
            ensureTempFileExists();
            File blobDir = createBlobDirNameForTemp(m_binFile);
            if (!blobDir.mkdir()) {
                throw new IOException("Unable to create temp directory " + blobDir.getAbsolutePath());
            }
            m_blobDir = blobDir;
        }
    }

    /**
     * Guesses a "good" blob directory for a given binary temp file. For instance, if the temp file is
     * /tmp/knime_container_xxxx_xx.bin.gz, the blob dir name is suggested to be /tmp/knime_container_xxxx_xx.
     *
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

    /** @return size of m_binFile in bytes, -1 if not set. Only for debugging/test purposes. */
    long getBufferFileSize() {
        if (m_binFile != null) {
            return m_binFile.length();
        }
        return -1L;
    }

    /**
     * Determines the file location for a blob to be read/written with some given coordinates (column and index in
     * column).
     *
     * @param indexBlobInCol The index in the column (generally the row number).
     * @param column The column index.
     * @param createPath Create the directory, if necessary (when writing)
     * @param isCompressed If file is (to be) compressed
     * @return The file location.
     * @throws IOException If that fails (e.g. blob dir does not exist).
     */
    File getBlobFile(final int indexBlobInCol, final int column, final boolean createPath, final boolean isCompressed)
        throws IOException {
        StringBuilder childPath = new StringBuilder();
        childPath.append("col_" + column);
        childPath.append(File.separatorChar);
        // the index of the folder in knime_container_xyz/col_0/
        int topFolderIndex = indexBlobInCol / (BLOB_ENTRIES_PER_DIRECTORY * BLOB_ENTRIES_PER_DIRECTORY);
        String topDir = getFileName(topFolderIndex);
        childPath.append(topDir);
        childPath.append(File.separatorChar);
        // the index of the folder in knime_container_xyz/col_0/topFolderIndex
        int subFolderIndex =
            (indexBlobInCol - (topFolderIndex * BLOB_ENTRIES_PER_DIRECTORY * BLOB_ENTRIES_PER_DIRECTORY))
                / BLOB_ENTRIES_PER_DIRECTORY;
        String subDir = getFileName(subFolderIndex);
        childPath.append(subDir);
        if (createPath) {
            ensureBlobDirExists();
        }
        File blobDir = new File(m_blobDir, childPath.toString());
        if (createPath) {
            if (!blobDir.exists() && !blobDir.mkdirs()) {
                throw new IOException("Unable to create directory " + blobDir.getAbsolutePath());
            }
        } else {
            if (!blobDir.exists()) {
                throw new IOException("Blob file location \"" + blobDir.getAbsolutePath() + "\" does not exist");
            }
        }
        String file = Integer.toString(indexBlobInCol) + ".bin";
        if (isCompressed) {
            file = file.concat(".gz");
        }
        return new File(blobDir, file);
    }

    /**
     * Creates the string for a given file index. For instance 0 is transformed to "0000", 34 to "0034" and so on.
     *
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
     * Creates the row iterator.
     *
     * @return the row iterator
     * @noreference This method is not intended to be referenced by clients.
     */
    public final synchronized CloseableRowIterator iterator() {
        return iteratorWithFilter(null);
    }

    /**
     * Creates the row iterator builder that is filtered according to a {@link TableFilter}.
     *
     * @param filter the filter to be applied while iterating
     * @return the filtered row iterator
     * @noreference This method is not intended to be referenced by clients.
     */
    public final synchronized CloseableRowIterator iteratorWithFilter(final TableFilter filter) {
        return iteratorWithFilter(filter, null);
    }

    // This method might return a FilterDelegateRowIterator that wraps a CloseableRowIterator. This leads to a warning
    // about the wrapped iterator potentially not being closed. It is safe to disregard this warning though, since
    // the FilterDelegateRowIterator takes care of closing the wrapped iterator.
    @SuppressWarnings("resource")
    final synchronized CloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {

        if (m_isClearedLock.booleanValue()) {
            throw new IllegalStateException("Cannot iterate over table: buffer has been cleared.");
        }

        final List<BlobSupportDataRow> list = obtainListFromCacheOrBackIntoMemoryIterator();
        if (list == null) {

            // Case 1: We don't have have the table in memory and want to iterate it back into memory.
            if (m_useBackIntoMemoryIterator) {
                m_useBackIntoMemoryIterator = false;
                final TableStoreCloseableRowIterator tableStoreIt = m_outputReader.iterator();
                tableStoreIt.setBuffer(this);
                m_nrOpenInputStreams.incrementAndGet();
                final BackIntoMemoryIterator backIntoMemoryIt = new BackIntoMemoryIterator(tableStoreIt, size());
                m_openResources.register(tableStoreIt, backIntoMemoryIt);
                m_backIntoMemoryIteratorRef = new WeakReference<>(backIntoMemoryIt);
                final FromListIterator listIt = new FromListIterator(backIntoMemoryIt.getList(), exec);
                listIt.setBackIntoMemoryIterator(backIntoMemoryIt);
                return filter == null ? listIt : new FilterDelegateRowIterator(listIt, filter, size(), exec);
            }

            // Case 2: We don't have have the table in memory.
            final TableStoreCloseableRowIterator tableStoreIt =
                filter == null ? m_outputReader.iterator() : m_outputReader.iteratorWithFilter(filter, exec);
            // register the table store iterator with this buffer
            tableStoreIt.setBuffer(this);
            m_nrOpenInputStreams.incrementAndGet();
            m_openResources.register(tableStoreIt, tableStoreIt);
            return tableStoreIt;

        } else {
            final BackIntoMemoryIterator backIntoMemoryIt =
                m_backIntoMemoryIteratorRef != null ? m_backIntoMemoryIteratorRef.get() : null;
            if (filter != null && size() > 0) {

                // Case 3: We have the full table in memory and want to apply a filter.
                if (backIntoMemoryIt == null) {
                    // We never store more than 2^31 rows in memory, therefore it's safe to cast to int.
                    final int fromIndex = (int)Math.min(Integer.MAX_VALUE, filter.getFromRowIndex().orElse(0l));
                    final int toIndex = (int)Math.min(Integer.MAX_VALUE, filter.getToRowIndex().orElse(size() - 1));
                    final FromListRangeIterator rangeIterator =
                        new FromListRangeIterator(list, fromIndex, toIndex, exec);

                    /**
                     * In a future world (a world of predicates, see AP-11805), the filter might be configured to keep
                     * only rows with an index between 1000 and 2000 and a value greater than 42 in column 13. The
                     * rangeIterator will take care of only returning rows with an index between 1000 and 2000. In
                     * fact, it will return the row with index 1000 as its first row. Therefore, the
                     * FilterDelegateRowIterator that handles the column-13-greater-than-42-predicate, has to be
                     * provided with a copied filter with adjusted from- and toRowIndices.
                     */
                    final TableFilter offsetFilter = new TableFilter.Builder(filter)//
                        .withFromRowIndex(0)//
                        .withToRowIndex(toIndex - fromIndex)//
                        .build();
                    return new FilterDelegateRowIterator(rangeIterator, offsetFilter, size(), exec);
                }

                // Case 4: We are currently iterating the table back into memory and want to apply a filter.
                else {
                    final FromListIterator listIt = new FromListIterator(list, exec);
                    listIt.setBackIntoMemoryIterator(backIntoMemoryIt);
                    return new FilterDelegateRowIterator(listIt, filter, size(), exec);
                }

            }

            // Case 5: We have at least parts of the table in memory and don't want to apply a filter.
            final FromListIterator listIt = new FromListIterator(list, exec);
            if (backIntoMemoryIt != null) {
                listIt.setBackIntoMemoryIterator(backIntoMemoryIt);
            }
            return listIt;
        }
    }

    private List<BlobSupportDataRow> obtainListFromCacheOrBackIntoMemoryIterator() {
        final Optional<List<BlobSupportDataRow>> optionalList = CACHE.get(this);
        if (optionalList.isPresent()) {
            return optionalList.get();
        }

        final WeakReference<BackIntoMemoryIterator> backIntoMemoryIteratorRef = m_backIntoMemoryIteratorRef;
        if (backIntoMemoryIteratorRef != null) {
            BackIntoMemoryIterator backIntoMemoryIterator = backIntoMemoryIteratorRef.get();
            if (backIntoMemoryIterator != null) {
                return backIntoMemoryIterator.getList();
            }
        }

        return null;
    }

    /**
     * True if any row containing blob cells is contained in this buffer.
     *
     * @return if blob cells are present.
     */
    boolean containsBlobCells() {
        return m_containsBlobs;
    }

    /**
     * @return true if this buffer represents an isolated table that has file stores copied from another table.
     */
    boolean hasOwnFileStoreCells() {
        return m_fileStoreHandler instanceof NotInWorkflowWriteFileStoreHandler
            && ((NotInWorkflowWriteFileStoreHandler)m_fileStoreHandler).hasCopiedFileStores();
    }

    /**
     * @return the directory having file store if {@link #hasOwnFileStoreCells()} returns true (otherwise throws an
     *         exception).
     */
    File getOwnFileStoreCellsDirectory() {
        assert hasOwnFileStoreCells();
        return ((NotInWorkflowWriteFileStoreHandler)m_fileStoreHandler).getBaseDir();
    }

    /**
     * Creates a clone of this buffer for writing the content to a stream that is of the current version. The cloned
     * buffer is always synchronously flushed to disk and never keeps its table in memory.
     *
     * @return A new buffer with the same ID, which is only used locally to update the stream.
     */
    Buffer createLocalCloneForWriting() {
        return new Buffer(m_spec, 0, getBufferID(), m_dataRepository, Collections.emptyMap(),
            castAndGetFileStoreHandler(), m_bufferSettings);
    }

    /**
     * Returns a file store handler used when data needs to be copied (version hop, see
     * {@link #createLocalCloneForWriting()}). When this buffer was created using the 'write' constructor this will be
     * the constructor file store handler. Otherwise it will be a wrapper that is write-protected.
     *
     * @return ...
     */
    final IWriteFileStoreHandler castAndGetFileStoreHandler() {
        if (m_fileStoreHandler instanceof IWriteFileStoreHandler) {
            return (IWriteFileStoreHandler)m_fileStoreHandler;
        }
        LOGGER.debug("file store handler is not writable, creating fallback");
        return new ROWriteFileStoreHandler(getDataRepository());
    }

    /**
     * Method that's been called from the {@link ContainerTable} to save the content. It will add zip entries to the
     * <code>zipOut</code> argument and not close the output stream when done, allowing to add additional content
     * elsewhere (for instance the <code>DataTableSpec</code>).
     *
     * @param zipOut To write to.
     * @param exec For progress/cancel
     * @throws IOException If it fails to write to a file.
     * @throws CanceledExecutionException If canceled.
     * @see org.knime.core.node.BufferedDataTable.KnowsRowCountTable #saveToFile(File, NodeSettingsWO, ExecutionMonitor)
     */
    synchronized void addToZipFile(final ZipOutputStream zipOut, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        m_lifecycle.onSave();
        if (m_spec == null) {
            throw new IOException("Can't save an open Buffer.");
        }
        // binary data is already deflated
        if (ZLIB_SUPPORTS_LEVEL_SWITCH_AP8083) {
            zipOut.setLevel(Deflater.NO_COMPRESSION);
        }
        zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_DATA));
        // these are the conditions:
        //    !usesOutFile() --> data all kept in memory, small tables
        //    m_version< ... --> container version bump
        if (!isFlushedToDisk() || m_version < IVERSION) {
            // need to use new buffer since we otherwise write properties
            // of this buffer, which prevents it from further reading (version
            // conflict) - see bug #1364
            Buffer copy = createLocalCloneForWriting();
            File tempFile = null;
            try {
                copy.initOutputWriter(() -> new NonClosableOutputStream.Zip(zipOut));
                copy.m_hasTempFile = false;
            } catch (UnsupportedOperationException notSupported) {
                tempFile = DataContainer.createTempFile(copy.m_outputFormat.getFilenameSuffix());
                copy.m_binFile = tempFile;
                copy.initOutputWriter(tempFile);
            }
            int count = 1;
            try (CloseableRowIterator it = iterator()) {
                while (it.hasNext()) {
                    final BlobSupportDataRow row = (BlobSupportDataRow)it.next();
                    final int countCurrent = count;
                    exec.setProgress(count / (double)size(),
                        () -> "Writing row " + countCurrent + " (\"" + row.getKey() + "\")");
                    exec.checkCanceled();
                    // make a deep copy of blobs if we have a version hop
                    copy.addRow(row, m_version < IVERSION, false);
                    count++;
                }
            }
            synchronized (copy) {
                copy.closeInternal();
            }
            if (tempFile != null) {
                try (final InputStream in = new FileInputStream(tempFile);
                        final NonClosableOutputStream ncOut = new NonClosableOutputStream(zipOut)) {
                    IOUtils.copyLarge(in, ncOut);
                } finally {
                    tempFile.delete();
                }
            }
            // bug fix #1631 ... the memory policy is not properly preserved
            // in this if-statement
            if (isFlushedToDisk()) {
                // can safely be set to null because it wrote to stream already
                copy.m_listWhileAddRow = null;
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
                addToZip(ZIP_ENTRY_BLOBS, zipOut, blobDir);
            }
            if (hasOwnFileStoreCells()) {
                addToZip(ZIP_ENTRY_FILESTORES, zipOut, getOwnFileStoreCellsDirectory());
            }
            zipOut.closeEntry();
            if (ZLIB_SUPPORTS_LEVEL_SWITCH_AP8083) {
                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
            }
            zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_META));
            copy.writeMetaToFile(() -> new NonClosableOutputStream.Zip(zipOut));
        } else {
            // no need for BufferedInputStream here as the copy method
            // does the buffering itself
            try (InputStream is = new FileInputStream(m_binFile)) {
                FileUtil.copy(is, zipOut);
            }
            if (m_blobDir != null) {
                addToZip(ZIP_ENTRY_BLOBS, zipOut, m_blobDir);
            }
            if (hasOwnFileStoreCells()) {
                addToZip(ZIP_ENTRY_FILESTORES, zipOut, getOwnFileStoreCellsDirectory());
            }
            zipOut.closeEntry();
            if (ZLIB_SUPPORTS_LEVEL_SWITCH_AP8083) {
                zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
            }
            zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_META));
            writeMetaToFile(() -> new NonClosableOutputStream.Zip(zipOut));
        }
    }

    /**
     * Adds recursively the content of the directory <code>dir</code> to a zip output stream, prefixed with
     * <code>zipEntry</code>.
     */
    private static void addToZip(final String zipEntry, final ZipOutputStream zipOut, final File dir)
        throws IOException {
        for (File f : dir.listFiles()) {
            String name = f.getName();
            if (f.isDirectory()) {
                String dirPath = zipEntry + "/" + name + "/";
                zipOut.putNextEntry(new ZipEntry(dirPath));
                addToZip(dirPath, zipOut, f);
            } else {
                zipOut.putNextEntry(new ZipEntry(zipEntry + "/" + name));
                try (final InputStream i = new BufferedInputStream(new FileInputStream(f))) {
                    FileUtil.copy(i, zipOut);
                }
                zipOut.closeEntry();
            }
        }
    }

    /**
     * Deletes the file underlying this buffer.
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        clear();
    }

    /**
     * Get this buffer's ID. It may be null if this buffer is not used as part of the workflow (but rather just has been
     * read/written from/to a zip file.
     *
     * @return the buffer ID or -1
     */
    public int getBufferID() {
        return m_bufferID;
    }

    /**
     * Get this buffer's unique ID.
     *
     * @return the unique ID of this buffer
     */
    Long getUniqueID() {
        return m_uniqueID;
    }

    /**
     * Get the number of open input streams associated with this buffer. For testing purposes only.
     *
     * @return the number of open input streams
     */
    int getNrOpenInputStreams() {
        return m_nrOpenInputStreams.get();
    }

    /**
     * Get the number of open {@link BufferResource BufferResources} owned by non-garbage-collected
     * {@link CloseableRowIterator CloseableRowIterators} iterating over this buffer. For testing purposes only.
     *
     * @return the number of open resources owned by iterators iterating over this buffer
     */
    int getNrOpenResources() {
        return m_openResources.size();
    }

    /**
     * Clear the argument iterator (free the allocated resources.
     *
     * @param it The iterator
     */
    public synchronized void clearIteratorInstance(final TableStoreCloseableRowIterator it,
        final boolean removeFromHash) {
        final String closeMes =
            (m_binFile != null) ? String.format("Closing input stream on \"%s\", ", m_binFile.getAbsolutePath()) : "";
        try {
            if (it.performClose()) {
                m_nrOpenInputStreams.decrementAndGet();
                logDebug(String.format("%s%s remaining", closeMes, m_nrOpenInputStreams), null);
                if (removeFromHash) {
                    m_openResources.unregister(it);
                }
            }
        } catch (IOException ioe) {
            logDebug(String.format("%sfailed!", closeMes), ioe);
        }
    }

    /** Clears the temp file. Any subsequent iteration will fail! */
    synchronized void clear() {
        /** lock clear flag to prevent concurrent clearing or asynchronous writing to this buffer */
        synchronized (m_isClearedLock) {
            if (!m_isClearedLock.booleanValue()) {
                /** prevent subsequent clears */
                m_isClearedLock.setValue(true);

                BufferTracker.getInstance().bufferCleared(this);
                m_listWhileAddRow = null;
                CACHE.invalidate(this);
                m_openResources.releaseResourcesAndClear();
                if (m_binFile != null) {
                    if (m_outputWriter != null) {
                        try {
                            m_outputWriter.close();
                        } catch (IOException ioe) {
                            // Exception indicates that stream has already been closed. If exception was thrown for another
                            // reason, we are OK with it as well, since we're clearing this buffer anyways.
                        }
                    }
                    if (m_blobDir != null) {
                        DeleteInBackgroundThread.delete(m_binFile, m_blobDir);
                    } else {
                        DeleteInBackgroundThread.delete(m_binFile);
                    }
                }
                if (m_fileStoreHandler instanceof NotInWorkflowWriteFileStoreHandler) {
                    m_fileStoreHandler.clearAndDispose();
                }
                if (m_blobLRUCache != null) {
                    m_blobLRUCache.clear();
                }
                m_binFile = null;
                m_blobDir = null;
            }
        }

        m_lifecycle.onClear();
    }

    private static final int MAX_FILES_TO_CREATE_BEFORE_GC = 10000;

    private static final AtomicInteger FILES_CREATED_COUNTER = new AtomicInteger(0);

    /**
     * Method being called each time a file is created. It maintains a counter and calls each
     * {@link #MAX_FILES_TO_CREATE_BEFORE_GC} files the garbage collector. This fixes an unreported problem on windows,
     * where (although the file reference is null) there seems to be a hidden file lock, which yields a "not enough
     * system resources to perform operation" error.
     *
     * @param file The existing file
     * @throws IOException If there is not enough space left on the partition of the temp folder
     */
    static void onFileCreated(final File file) throws IOException {
        int count = FILES_CREATED_COUNTER.incrementAndGet();
        long freeSpace = file.exists() ? file.getUsableSpace() : Long.MAX_VALUE;
        long minSpace = MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB * (1024L * 1024L);
        if (freeSpace < minSpace) {
            throw new IOException("The partition of the temp file \"" + file.getAbsolutePath()
                + "\" is too low on disc space (" + freeSpace / (1024 * 1024) + "MB available but at least "
                + MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB * (1024L * 1024L) + "MB are required). "
                + " You can tweak the limit by changing the \""
                + KNIMEConstants.PROPERTY_MIN_FREE_DISC_SPACE_IN_TEMP_IN_MB + "\" java property.");
        }
        if (count % MAX_FILES_TO_CREATE_BEFORE_GC == 0 && !DISCOURAGE_GC) {
            LOGGER.debug("created " + count + " files, performing garbage collection to release handles");
            System.gc();
        }
    }

    /**
     * Print a debug message. This method does nothing if isExecutingShutdownHook is true.
     */
    private static void logDebug(final String message, final Throwable t) {
        if (!isExecutingShutdownHook) {
            if (t == null) {
                LOGGER.debug(message);
            } else {
                LOGGER.debug(message, t);
            }
        }
    }

    /** Write all rows from list into file. Used while rows are added and if low mem condition is met. */
    synchronized void flushBuffer() {
        writeList(m_listWhileAddRow);
        m_listWhileAddRow = null; // don't write to internal cache any more
    }

    private void writeList(final List<BlobSupportDataRow> list) {
        try {
            ensureWriterIsOpen();
            if (list != null) {
                for (BlobSupportDataRow row : list) {
                    m_outputWriter.writeRow(row);
                }
            }
        } catch (IOException ioe) {
            LOGGER.error("Failed to write rows from buffer to file.", ioe);
        } catch (IllegalStateException ise) {
            LOGGER.error(ise.getMessage() + "; Construction time call stack:\n" + m_fullStackTraceAtConstructionTime);
        }
    }

    /** Last recently used cache for blobs. */
    private static final class BlobLRUCache extends LinkedHashMap<BlobAddress, SoftReference<BlobDataCell>> {

        private static final long serialVersionUID = 1L;

        /** Default constructor, instructs for access order. */
        BlobLRUCache() {
            super(16, 0.75f, true); // args copied from HashMap implementation
        }

        /** {@inheritDoc} */
        @Override
        protected synchronized boolean removeEldestEntry(final Entry<BlobAddress, SoftReference<BlobDataCell>> eldest) {
            return size() >= 100;
        }

        /** {@inheritDoc} */
        @Override
        public synchronized SoftReference<BlobDataCell> get(final Object key) {
            return super.get(key);
        }

        /** {@inheritDoc} */
        @Override
        public synchronized SoftReference<BlobDataCell> put(final BlobAddress key,
            final SoftReference<BlobDataCell> value) {
            return super.put(key, value);
        }

    }

    /**
     * The BackIntoMemoryIterator holds lists of datarows read from a file. It is strongly referenced only by the
     * FromListIterators and is only weak-referenced in the outer Buffer class. This way, we make sure that the
     * reference on m_list held by the BackIntoMemoryIterator is dropped when FromListIterators are closed.
     */
    private final class BackIntoMemoryIterator extends CloseableRowIterator {

    	/**
    	 * The iterator for reading additional rows from disk.
    	 */
        private final CloseableRowIterator m_iterator;

        /**
         * A table held in memory while still being modifiable and before being added to the cache. This is only ever
         * true when the moves a fully written buffer that has previously fit into memory back into memory. To prevent
         * per-datarow hash lookups, the list is not put into the cache yet (the cache holds only unmodifiable lists
         * anyways).
         */
        private final List<BlobSupportDataRow> m_listWhileBackIntoMemory;

        /**
         * Creates a new BackIntoMemoryIterator.
         *
         * @param iterator the file-based iterator underlying this BackIntoMemoryIterator
         * @param size the size of the table to be read back into memory
         */
        private BackIntoMemoryIterator(final CloseableRowIterator iterator, final long size) {
            m_iterator = iterator;
            m_listWhileBackIntoMemory = new ArrayList<>((int)size);
        }

        @Override
        public boolean hasNext() {
            final boolean hasNext = m_iterator.hasNext();
            if (!hasNext) {
                m_iterator.close();
            }
            return hasNext;
        }

        @Override
        public DataRow next() {
            DataRow next = m_iterator.next();
            if (!hasNext()) {
                // ... we put the table back into the cache
                CACHE.put(Buffer.this, m_listWhileBackIntoMemory);
                m_lifecycle.onAllRowsReadBackIntoMemory();
            }
            return next;
        }

        private List<BlobSupportDataRow> getList() {
            return m_listWhileBackIntoMemory;
        }

        @Override
        public void close() {
            m_iterator.close();
        }

    }

    /**
     * Iterator that reads a range of rows from a list that is weakly-referenced in memory. Should the list be
     * garbage-collected, the iterator is able to use a fallback that reads rows from disk.
     */
    private abstract class FromListFallBackFromFileIterator extends CloseableRowIterator {

        private final WeakReference<List<BlobSupportDataRow>> m_listRef;

        private final int m_toIndex;

        private final ExecutionMonitor m_exec;

        int m_nextIndex;

        private TableStoreCloseableRowIterator m_fallBackFromFileIterator;

        private FromListFallBackFromFileIterator(final List<BlobSupportDataRow> list, final int fromIndex,
            final int toIndex, final ExecutionMonitor exec) {
            assert fromIndex >= 0;
            assert toIndex < size();
            m_listRef = new WeakReference<>(list);
            m_toIndex = toIndex;
            m_exec = exec;
            m_nextIndex = fromIndex;
        }

        @Override
        public boolean hasNext() {
            final boolean hasNext = m_nextIndex <= m_toIndex;
            if (!hasNext) {
                closeFallBackFromFileIterator();
            }
            return hasNext;
        }

        void initFallBackFromFileIterator() {
            m_fallBackFromFileIterator =
                m_outputReader.iteratorWithFilter(TableFilter.filterRangeOfRows(m_nextIndex, m_toIndex), m_exec);
            m_fallBackFromFileIterator.setBuffer(Buffer.this);
            m_openResources.register(m_fallBackFromFileIterator, this);
            m_nrOpenInputStreams.incrementAndGet();
        }

        @Override
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more rows in buffer");
            }

            // case 1: list has been garbage-collected; use fallback
            if (m_fallBackFromFileIterator != null) {
                m_nextIndex++;
                return m_fallBackFromFileIterator.next();
            }

            final List<BlobSupportDataRow> list = m_listRef.get();
            if (list == null) {
                initFallBackFromFileIterator();
                return next();
            }

            // case 2: list in memory, proceed
            return nextFromList(list);
        }

        abstract DataRow nextFromList(final List<BlobSupportDataRow> list);

        @Override
        public void close() {
            closeFallBackFromFileIterator();
            m_nextIndex = (int) size();
        }

        private void closeFallBackFromFileIterator() {
            if (m_fallBackFromFileIterator != null) {
                m_fallBackFromFileIterator.close();
            }
        }

    }

    /**
     * Iterator to be used when all data is kept in a list in memory. It uses access by index and allows to iterate over
     * a range of rows.
     */
    private final class FromListRangeIterator extends FromListFallBackFromFileIterator {

        private FromListRangeIterator(final List<BlobSupportDataRow> list, final int fromIndex, final int toIndex,
            final ExecutionMonitor exec) {
            super(list, fromIndex, toIndex, exec);
        }

        @Override
        DataRow nextFromList(final List<BlobSupportDataRow> list) {
            return list.get(m_nextIndex++);
        }

    }

    /**
     * Memory alert listener that will - on memory alert - prevent a FromListIterator to read some table further back
     * into memory.
     */
    private static final class BackIntoMemoryIteratorDropper extends BufferMemoryAlertListener
        implements BufferResource {

        private final WeakReference<FromListIterator> m_iteratorRef;

        BackIntoMemoryIteratorDropper(final FromListIterator iterator) {
            m_iteratorRef = new WeakReference<>(iterator);
        }

        @SuppressWarnings("resource")
        @Override
        protected boolean memoryAlert(final MemoryAlert alert) {
            final FromListIterator iterator = m_iteratorRef.get();
            if (iterator != null) {
                iterator.dropBackIntoMemoryIterator();
            }
            return true;
        }

        @Override
        public final void releaseResource() {
            unregister();
        }
    }

    /**
     * Iterator to be used when data is kept in a list in memory or read back into memory using a
     * {@link BackIntoMemoryIterator}. It uses access by index rather than wrapping an java.util.Iterator as the list
     * may be simultaneously modified while reading (in case the content is fetched from disk and restored in memory).
     * This object is used when all rows fit in memory (no file).
     */
    private final class FromListIterator extends FromListFallBackFromFileIterator {

        private BackIntoMemoryIteratorDropper m_memoryAlertListener;

        private BackIntoMemoryIterator m_backIntoMemoryIterator;

        private FromListIterator(final List<BlobSupportDataRow> list, final ExecutionMonitor exec) {
            super(list, 0, (int)size() - 1, exec);
        }

        void setBackIntoMemoryIterator(final BackIntoMemoryIterator backIntoMemoryIterator) {
            CheckUtils.checkState(m_backIntoMemoryIterator == null, "Back into memory iterator has already been set.");
            CheckUtils.checkArgumentNotNull(backIntoMemoryIterator, "Back into memory iterator must not be null.");
            m_backIntoMemoryIterator = backIntoMemoryIterator;
            m_memoryAlertListener = new BackIntoMemoryIteratorDropper(this);
            m_memoryAlertListener.register();
            m_openResources.register(m_memoryAlertListener, this);
        }

        @Override
        DataRow nextFromList(final List<BlobSupportDataRow> list) {
        	final BackIntoMemoryIterator backIntoMemoryIterator = m_backIntoMemoryIterator;
        	// need to synchronize access to list, as it is potentially modified by the backIntoMemoryIterator
            final Object semaphore = backIntoMemoryIterator != null ? backIntoMemoryIterator : this;
            synchronized (semaphore) {

                // case 2a: read from memory
                if (m_nextIndex < list.size()) {
                    return list.get(m_nextIndex++);
                }

                // a memory alert has caused the reference on the back into memory iterator to be dropped; use fallback
                if (backIntoMemoryIterator == null) {
                    initFallBackFromFileIterator();
                    return next();
                }

                // case 2b: read from file back into memory
                final BlobSupportDataRow next = (BlobSupportDataRow)backIntoMemoryIterator.next();
                if (next == null) {
                    throw new InternalError("Unable to restore data row from disk");
                }
                list.add(next);
                // once we've read all rows back into memory, ...
                if (++m_nextIndex >= size()) {
                	assert !backIntoMemoryIterator.hasNext() : "File iterator returns more rows than buffer contains";
                	dropBackIntoMemoryIterator();
                }
                return next;
            }
        }

        @Override
        public boolean hasNext() {
            final boolean hasNext = super.hasNext();
            if (!hasNext) {
                dropBackIntoMemoryIterator();
            }
            return hasNext;
        }

        @Override
        public void close() {
            super.close();
            dropBackIntoMemoryIterator();
        }

        private void dropBackIntoMemoryIterator() {
            if (m_backIntoMemoryIterator != null) {
                m_backIntoMemoryIterator = null;
                m_memoryAlertListener.unregister();
                m_openResources.unregister(m_memoryAlertListener);
                m_memoryAlertListener = null;
            }
        }

    }

    /**
     * A background thread that deletes temporary files and directory (which may be a very long lasting job - in
     * particular when blob directories need to get deleted). This is the long term fix for bug 1051.
     * <p>
     * Implementation note: There is singleton thread running that does the deletion, if this thread is idle for a
     * while, it is shut down and recreated on demand.
     */
    private static final class DeleteInBackgroundThread extends Thread {

        private static final NodeLogger THREAD_LOGGER = NodeLogger.getLogger(DeleteInBackgroundThread.class);

        private static DeleteInBackgroundThread instance;

        private final LinkedBlockingQueue<File> m_filesToDeleteList;

        private static final Object LOCK = new Object();

        private DeleteInBackgroundThread() {
            super("KNIME-Temp-File-Deleter");
            m_filesToDeleteList = new LinkedBlockingQueue<File>();
        }

        /**
         * Queues a set of files for deletion and returns immediately.
         *
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

        /**
         * Blocks the calling thread until all queued files have been deleted.
         */
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
                                THREAD_LOGGER.warn("Deletion of " + m_filesToDeleteList.size() + "files "
                                    + "or directories failed because deleter thread was interrupted");
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
                    if (!deleted && first.exists() && !DISCOURAGE_GC) {
                        // note: although all input streams are closed, the
                        // file can't be deleted. If we call the gc, it
                        // works. No clue. That only happens on windows!
                        // http://forum.java.sun.com/thread.jspa?forumID=31&threadID=609458
                        System.gc();
                    }
                    if (deleted || deleteRecursively(first)) {
                        logDebug("Deleted temporary " + type + " \"" + first.getAbsolutePath() + "\"", null);
                    } else {
                        logDebug("Failed to delete temporary " + type + " \"" + first.getAbsolutePath() + "\"", null);
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

        /**
         * Makes sure the list is empty. This is necessary because when the VM goes done, any running thread is stopped.
         *
         * @see java.lang.Object#finalize()
         */
        @Override
        protected void finalize() throws Throwable {
            if (!m_filesToDeleteList.isEmpty()) {
                executeDeletion();
            }
        }

        /**
         * Deletes the argument file or directory recursively and returns true if this was successful. This method
         * follows any symbolic link (in comparison to {@link org.knime.core.utilFileUtil#deleteRecursively(File)}.
         *
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

    /**
     * See {@link #ZLIB_SUPPORTS_LEVEL_SWITCH_AP8083} for details.
     *
     * @return hopefully mostly true but false in case we are on a broken zlib
     */
    private static boolean isZLIBSupportsLevelSwitchAP8083() {
        ByteArrayOutputStream byteArrayOut;
        byte[] nullBytes = new byte[1024 * 1024];
        try (ZipOutputStream zipOut = new ZipOutputStream(byteArrayOut = new ByteArrayOutputStream())) {
            zipOut.setLevel(Deflater.BEST_SPEED);
            zipOut.putNextEntry(new ZipEntry("deflated.bin"));
            zipOut.write(nullBytes);
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("stored.bin"));
            zipOut.setLevel(Deflater.BEST_COMPRESSION);
            zipOut.write(nullBytes);
            zipOut.closeEntry();
        } catch (IOException e) {
            LOGGER.error("Unexpected error creating test zipped output", e);
            return false;
        }
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(byteArrayOut.toByteArray()))) {
            for (int i = 0; i < 2; i++) {
                zipIn.getNextEntry();
                while (zipIn.read(nullBytes) >= 0) {
                }
            }
        } catch (ZipException e) {
            return false;
        } catch (IOException e) {
            LOGGER.error("Unexpected error creating test zipped output", e);
            return false;
        }
        return true;
    }

    private abstract static class BufferMemoryAlertListener extends MemoryAlertListener {

        private final MemoryAlertSystem m_mas = MemoryAlertSystem.getInstanceUncollected();

        final void register() {
            m_mas.addListener(this);
        }

        final void unregister() {
            m_mas.removeListener(this);
        }
    }

    /**
     * Background task that will write the buffer data on memory alert. This is kept as static inner class in order to
     * allow for a garbage collection of the outer class.
     */
    private static final class BufferFlusher extends BufferMemoryAlertListener {

        private final WeakReference<Buffer> m_bufferRef;

        BufferFlusher(final Buffer buffer) {
            m_bufferRef = new WeakReference<>(buffer);
        }

        @Override
        protected boolean memoryAlert(final MemoryAlert alert) {
            final Buffer buffer = m_bufferRef.get();
            if (buffer != null) {
                ASYNC_EXECUTOR.submit(new ASyncWriteCallable(buffer));
                LOGGER.debugWithFormat("Writing %d rows in order to free memory.", buffer.size());
            }
            return true;
        }
    }

    /**
     * Background task that will invalidate the buffer in the cache on memory alert. This is kept as static inner class
     * in order to allow for a garbage collection of the outer class.
     */
    private static final class CacheInvalidator extends BufferMemoryAlertListener {

        private final WeakReference<Buffer> m_bufferRef;

        CacheInvalidator(final Buffer buffer) {
            m_bufferRef = new WeakReference<>(buffer);
        }

        @Override
        protected boolean memoryAlert(final MemoryAlert alert) {
            final Buffer buffer = m_bufferRef.get();
            if (buffer != null) {
                CACHE.invalidate(buffer);
                LOGGER.debugWithFormat("Invalidating buffer with %d rows in order to free memory.", buffer.size());
            }
            return true;
        }
    }

    /**
     * An interface that described the lifecycle of a buffer. It is interfaced during multiple stages of a buffer's
     * existence and is responsible for determining (a) when to evict tables from the cache, (b) when to move tables
     * back into the cache, and (c) when to flush tables to disk. By default (i.e., if this interface is implemented and
     * all methods have empty buffers), a table will be flushed only (a) if memory becomes critical while adding rows to
     * the buffer, (b) when it is explicitly configured to do so, (c) when it holds more rows than
     * {@link Integer#MAX_VALUE}, or (d) when the workflow is saved. If not flushed, it will remain hard-referenced in
     * the cache. Once flushed, it will never re-enter the cache. See https://knime-com.atlassian.net/browse/AP-10684
     * for a visualization of a buffer's lifecycle.
     *
     * @author Marc Bux, KNIME GmbH, Berlin, Germany
     */
    interface Lifecycle {

        /**
         * Synchronously called after adding a row to this buffer's m_listWhileAddRow if it is larger than
         * m_maxRowsInMem
         *
         * @throws IOException any kind of I/O error when handling the data row
         */
        void onAddRowToLargeList() throws IOException;

        /**
         * Synchronously called after closing this buffer and adding the table to the cache.
         */
        void onCloseIfCached();

        /**
         * Synchronously called after clearing this buffer.
         */
        void onClear();

        /**
         * Synchronously called before saving the table held by this buffer.
         */
        void onSave();

        /**
         * Asynchronously called after an {@link ASyncWriteCallable} succesfully wrote the table held in memory to disk.
         */
        void onWriteSuccessful();

        /**
         * Asynchronously called while saving the table held by this buffer to determine whether the table held by this
         * buffer should be read back into memory after load.
         *
         * @return <code>true</code> iff table should be read back into memory
         */
        boolean shallLoadBackIntoMemory();

        /**
         * Asynchronously called from back-into-memory iterator after the last row was read and the table was restored
         * into the cache.
         */
        void onAllRowsReadBackIntoMemory();

    }

    /**
     * The default lifecycle until KNIME 3.7.x. Tables are hard-referenced in the cache until they grow larger than a
     * certain amount of rows, which by default is derived from {@link DataContainerSettings#DEF_MAX_CELLS_IN_MEMORY}.
     * When the {@link MemoryAlertSystem} notices that memory becomes critical, all tables are flushed to disk. Tables
     * kept in memory while the workflow is saved are (lazily) read back into memory upon first iteration over the
     * table.
     *
     * @author Marc Bux, KNIME GmbH, Berlin, Germany
     */
    final class MemorizeIfSmallLifecycle implements Lifecycle {

        private BufferMemoryAlertListener m_memoryAlertListener;

        @Override
        public void onAddRowToLargeList() {
            assert Thread.holdsLock(Buffer.this);

            flushBuffer();
        }

        @Override
        public void onCloseIfCached() {
            assert Thread.holdsLock(Buffer.this);

            m_memoryAlertListener = new BufferFlusher(Buffer.this);
            m_memoryAlertListener.register();
        }

        @Override
        public void onClear() {
            assert Thread.holdsLock(Buffer.this);

            if (m_memoryAlertListener != null) {
                m_memoryAlertListener.unregister();
                m_memoryAlertListener = null;
            }
        }

        @Override
        public void onSave() {
        }

        @Override
        public void onWriteSuccessful() {
            CACHE.invalidate(Buffer.this);
        }

        @Override
        public boolean shallLoadBackIntoMemory() {
            return !isFlushedToDisk();
        }

        @Override
        public void onAllRowsReadBackIntoMemory() {
            synchronized (Buffer.this) {
                if (m_memoryAlertListener == null) {
                    m_memoryAlertListener = new CacheInvalidator(Buffer.this);
                }
            }
        }

    }

    /**
     * The default lifecycle since KNIME 3.8.0. Similar to the {@link MemorizeIfSmallLifecycle}, small tables are
     * hard-referenced in the cache. However, when the {@link MemoryAlertSystem} notices that memory becomes critical,
     * small tables are not dropped from the cache, but merely cleared for garbage collection. Also, tables larger than
     * {@link DataContainerSettings#DEF_MAX_CELLS_IN_MEMORY} are attempted to be kept in memory. They are written to
     * disk asynchronously when the buffer is closed and cleared for garbage collection. When the garbage collector
     * notices that memory becomes scarce, tables cleared for garbage collection are evicted from the cache in
     * least-recently-used (LRU) order (see {@link LRUCache}). A table that has been cached once will always be read
     * back into the cache when iterated over.
     *
     * @author Marc Bux, KNIME GmbH, Berlin, Germany
     */
    final class SoftRefLRULifecycle implements Lifecycle {

        /**
         * A flag that denotes whether the table held by this buffer was held in memory when the buffer was closed (and
         * can therefore fit into memory).
         */
        private boolean m_fitsIntoMemory = false;

        private BufferMemoryAlertListener m_memoryAlertListener;

        /**
         * The object that represent the pending task of writing a full table from memory to disk.
         */
        private Future<Void> m_asyncAddFuture;

        @Override
        public void onAddRowToLargeList() throws IOException {
        }

        @Override
        public void onCloseIfCached() {
            assert Thread.holdsLock(Buffer.this);

            /** The table apparently fits into memory and should be restored whenever it is evicted. */
            m_fitsIntoMemory = true;
            setRestoreIntoMemoryOnCacheMiss();

            if (size() <= m_maxRowsInMem) {
                m_memoryAlertListener = new BufferFlusher(Buffer.this);
                m_memoryAlertListener.register();
            } else {
                /**
                 * We'd like to flush early so that we can garbage-collect if memory becomes critical and we don't run out
                 * of memory. At the same time, we'd like to flush late so that we don't interfere with I/O tasks in the
                 * node generating this table. In this implementation, we flush as soon as possible once the buffer has been
                 * closed (and the node likely has terminated).
                 */
                m_asyncAddFuture = ASYNC_EXECUTOR.submit(new ASyncWriteCallable(Buffer.this));
            }
        }

        @Override
        public void onWriteSuccessful() {
            CACHE.clearForGarbageCollection(Buffer.this);
        }

        @Override
        public void onClear() {
            assert Thread.holdsLock(Buffer.this);

            if (m_memoryAlertListener != null) {
                m_memoryAlertListener.unregister();
                m_memoryAlertListener = null;
            }

            if (m_asyncAddFuture != null && !m_asyncAddFuture.isDone()) {
                /**
                 * We should cancel the asynchronous writer thread. We do not have to wait for the thread to terminate
                 * gracefully, since it takes care of calling performClear itself. This is also not the right place and
                 * time to check whether the thread has thrown any exceptions, since we're clearing the buffer anyways.
                 */
                m_asyncAddFuture.cancel(true);
            }

            m_asyncAddFuture = null;
        }

        @Override
        public void onSave() {
            assert Thread.holdsLock(Buffer.this);

            /**
             * We have to wait for the asynchronous writer thread and check for exceptions. This is the only time we
             * ever check for exceptions in the asynchronous writer thread, since the graceful termination of that
             * thread is required when we're saving the workflow. It is never required at any other time.
             */
            if (m_asyncAddFuture != null) {
                try {
                    m_asyncAddFuture.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for asynchronous disk write thread.", e);
                } catch (ExecutionException e) {
                    final StringBuilder error = new StringBuilder();
                    final Throwable t = e.getCause();
                    if (t.getMessage() != null) {
                        error.append(t.getMessage());
                    } else {
                        error.append("Writing table to file threw exception \"");
                        error.append(t.getClass().getSimpleName()).append("\"");
                    }
                    throw new RuntimeException(error.toString(), t);
                } catch (CancellationException e) {
                    /** The asynchronous writer thread was cancelled in onClear(). Nothing to do here. */
                }
            }
            m_asyncAddFuture = null;
        }

        @Override
        public void onAllRowsReadBackIntoMemory() {
            synchronized (Buffer.this) {
                CACHE.clearForGarbageCollection(Buffer.this);
                if (shallLoadBackIntoMemory()) {
                    /** When having read the table back into memory, we should do so again the next time we need to. */
                    setRestoreIntoMemoryOnCacheMiss();
                }
            }
        }

        @Override
        public boolean shallLoadBackIntoMemory() {
            /**
             * Load back into memory on workflow load if it has fit into memory before. Caution: This could entail that
             * a workflow saved on a high-memory machine cannot be loaded on a low-memory machine. However, this
             * behavior is in-line with the behavior of the MemorizeIfSmallLifecycle.
             */
            return m_fitsIntoMemory;
        }

    }

    /**
     * Background task that will write the output data. This is kept as static inner class in order to allow for a
     * garbage collection of the outer class (which indicates an early stopped buffer writing).
     */
    private static final class ASyncWriteCallable implements Callable<Void> {

        private final WeakReference<Buffer> m_bufferRef;

        private final NodeContext m_nodeContext;

        ASyncWriteCallable(final Buffer buffer) {
            m_bufferRef = new WeakReference<>(buffer);
            /** The node context may be null if the Buffer has been created outside of a node's context (e.g., in unit
             * tests). This is also the reason why this class does not extend the CallableWithContect class. */
            m_nodeContext = NodeContext.getContext();
        }

        @Override
        public final Void call() throws Exception {
            /**
             * we have to 100% make sure no methods that are synchronized on the buffer are called in here and
             * everything we call in here does not interfere with other methods potentially called asynchronously on
             * this buffer.
             */

            /** Writer thread has been cancelled during clear(). */
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            NodeContext.pushContext(m_nodeContext);
            try {
                Buffer buffer = m_bufferRef.get();
                if (buffer == null) {
                    LOGGER.debug("Attempting to swap a Buffer that was already garbage collected ... will ignore.");
                    /** Buffer was already discarded (no rows added) */
                    return null;
                }
                // START debug AP-13181 buffers not being cleared when workflow is closed and cleaned up
                if (m_nodeContext != null) {
                    final WorkflowManager wfm = m_nodeContext.getWorkflowManager();
                    if (wfm != null) {
                        final WorkflowContext workflowContext = wfm.getContext();
                        if (workflowContext != null) {
                            final File tempLocation = workflowContext.getTempLocation();
                            if (!tempLocation.isDirectory()) {
                                throw new IOException(
                                    "Workflow temp directory " + tempLocation.toString() + " has been deleted.");
                            }
                        }
                    }
                }
                // END debug AP-13181 buffers not being cleared when workflow is closed and cleaned up

                buffer.ensureWriterIsOpen();
                final List<BlobSupportDataRow> list = CACHE.getSilent(buffer).get();
                final AbstractTableStoreWriter outputWriter = buffer.m_outputWriter;
                buffer = null;

                if (list != null) {
                    for (BlobSupportDataRow rowInList : list) {
                        /** Writer thread has been cancelled during clear(). */
                        if (Thread.currentThread().isInterrupted()) {
                            return null;
                        }

                        outputWriter.writeRow(rowInList);
                    }
                }

                buffer = m_bufferRef.get();
                if (buffer == null) {
                    /** Buffer was already discarded */
                    return null;
                }
                /** Prevent asynchronous clearing of buffer during close. */
                synchronized (buffer.m_isClearedLock) {
                    buffer.closeWriterAndWriteMeta();
                    buffer.m_lifecycle.onWriteSuccessful();
                }
                buffer = null;

            } catch (Throwable t) {
                /** only log error if buffer has neither been garbage-collected nor cleared */
                final Buffer buffer = m_bufferRef.get();
                if (buffer != null) {
                    /** wait for potential asynchronous clear processes before checking value of m_isClearedLock */
                    synchronized (buffer.m_isClearedLock) {
                        if (!buffer.m_isClearedLock.booleanValue()) {

                            final StringBuilder error = new StringBuilder();
                            error.append("Writing of table to file");
                            appendNodeNameAndError(t, error);

                            LOGGER.error(error.toString(), t);
                            LOGGER.error("Table will be held in memory until node is cleared.");
                            LOGGER.error("Workflow can't be saved in this state.");

                            // START debug AP-13181 buffers not being cleared when workflow is closed and cleaned up
                            LOGGER.debugWithFormat("Buffer stack trace at construction time:\n%s",
                                buffer.m_fullStackTraceAtConstructionTime);
                            // END debug AP-13181 buffers not being cleared when workflow is closed and cleaned up

                            throw new IOException(error.toString(), t);
                        }
                    }
                }

            } finally {
                NodeContext.removeLastContext();
            }

            return null;
        }

        private void appendNodeNameAndError(final Throwable t, final StringBuilder error) {
            Optional.ofNullable(m_nodeContext)//
                .map(NodeContext::getNodeContainer)//
                .ifPresent(c -> error.append(" at node ").append(c.getNameWithID()));
            Optional.ofNullable(m_nodeContext)//
                .map(NodeContext::getWorkflowManager)//
                .ifPresent(w -> error.append(" at workflow ").append(w.getNameWithID()));
            error.append(" encountered error: ");
            error.append(t.getClass().getSimpleName());
            if (t.getMessage() != null) {
                error.append(": " + t.getMessage());
            }
        }

    }

}
