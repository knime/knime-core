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
 * History
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.container.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.RowIteratorBuilder;
import org.knime.core.data.RowIteratorBuilder.DefaultRowIteratorBuilder;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.Buffer;
import org.knime.core.data.container.CellClassInfo;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.KNIMEStreamConstants;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.PathUtils;

/**
 * The abstract reader for reading specialized table formats.
 *
 * @author wiswedel
 * @since 3.6
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractTableStoreReader implements KNIMEStreamConstants {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractTableStoreReader.class);

    private final File m_file;

    private final DataTableSpec m_spec;

    /**
     * List of file iterators that look at this buffer. Need to close them when the node is reset and the file shall be
     * deleted.
     */
    private final WeakHashMap<TableStoreCloseableRowIterator, Object> m_openIteratorSet;

    /** Dummy object for the file iterator map. */
    private static final Object DUMMY = new Object();

    /** Number of open file input streams on m_binFile. */
    private AtomicInteger m_nrOpenInputStreams = new AtomicInteger();

    private CellClassInfo[] m_shortCutsLookup;

    private FileStoreHandlerRepository m_fileStoreHandlerRepository;

    private Buffer m_buffer;

    /** Input stream version **/
    protected final int m_version;

    /**
     * Constructs an abstract table store reader.
     *
     * @param binFile the local file from which to read
     * @param spec Non-null spec of the table being read.
     * @param settings The settings (written by
     *            {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @param version The version as defined in the {@link Buffer} class
     * @throws IOException any type of I/O problem
     * @throws InvalidSettingsException thrown in case something goes wrong during de-serialization, e.g. a new version
     *             of a writer has been used which hasn't been installed on the current system.
     */
    protected AbstractTableStoreReader(final File binFile, final DataTableSpec spec, final NodeSettingsRO settings,
        final int version) throws IOException, InvalidSettingsException {
        m_file = CheckUtils.checkArgumentNotNull(binFile);
        m_spec = CheckUtils.checkArgumentNotNull(spec);
        m_openIteratorSet = new WeakHashMap<>();
        m_version = version;
        readMetaFromFile(settings, version);
    }

    /**
     * @param buffer the buffer to set
     * @param fileStoreHandlerRepository the fileStoreHandlerRepository to set
     */
    public final void setBufferAndFileStoreHandlerRepository(final Buffer buffer,
        final FileStoreHandlerRepository fileStoreHandlerRepository) {
        m_fileStoreHandlerRepository = fileStoreHandlerRepository;
        m_buffer = buffer;
    }

    /** @return the buffer */
    protected final Buffer getBuffer() {
        return m_buffer;
    }

    /**
     * Get underlying stream version. Important for file iterators.
     *
     * @return Underlying stream version.
     */
    public int getReadVersion() {
        return m_version;
    }

    public final BlobWrapperDataCell createBlobWrapperCell(final BlobAddress address, final CellClassInfo type)
        throws IOException {
        Buffer blobBuffer = getBuffer();
        if (address.getBufferID() != blobBuffer.getBufferID()) {
            ContainerTable cnTbl = blobBuffer.getGlobalRepository().get(address.getBufferID());
            if (cnTbl == null) {
                throw new IOException("Unable to retrieve table that owns the blob cell");
            }
            blobBuffer = cnTbl.getBuffer();
        }
        return new BlobWrapperDataCell(blobBuffer, address, type);
    }

    /**
     * @return the fileStoreHandlerRepository
     */
    public final FileStoreHandlerRepository getFileStoreHandlerRepository() {
        return m_fileStoreHandlerRepository;
    }

    /**
     * Returns a row iterator which returns each row one-by-one from the table.
     *
     * @return row iterator
     */
    public abstract TableStoreCloseableRowIterator iterator();

    /**
     * Returns a {@link RowIteratorBuilder} that can be used to assemble more complex
     * {@link TableStoreCloseableRowIterator}s that only iterate over parts of a table.
     *
     * @return a {@link RowIteratorBuilder} that can be used to assemble complex {@link TableStoreCloseableRowIterator}s
     *
     * @since 3.7
     */
    public RowIteratorBuilder<? extends TableStoreCloseableRowIterator> iteratorBuilder() {
        return new DefaultRowIteratorBuilder<TableStoreCloseableRowIterator>(() -> iterator(), m_spec) {
            @Override
            public TableStoreCloseableRowIterator build() {
                TableStoreCloseableRowIterator iterator = super.build();
                registerNewIteratorInstance(iterator);
                return iterator;
            }
        };
    }

    /**
     * Reads meta information, such as the classes of serialized {@link DataCell} instances.
     *
     * @param settings The settings (written by
     *            {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @param version The version as defined in the {@link Buffer} class
     * @throws IOException Any type of I/O problem.
     * @throws InvalidSettingsException thrown in case something goes wrong during de-serialization, e.g. a new version
     *             of a writer has been used which hasn't been installed on the current system.
     */
    protected void readMetaFromFile(final NodeSettingsRO settings, final int version)
        throws IOException, InvalidSettingsException {
        if (version <= 6) {
            m_shortCutsLookup = readCellClassInfoArrayFromMetaVersion1x(settings);
        } else {
            m_shortCutsLookup = readCellClassInfoArrayFromMetaVersion2(settings);
        }
    }

    @SuppressWarnings("unchecked")
    private static CellClassInfo[] readCellClassInfoArrayFromMetaVersion1x(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        String[] cellClasses = settings.getStringArray(TableStoreFormat.CFG_CELL_CLASSES);
        CellClassInfo[] shortCutsLookup = new CellClassInfo[cellClasses.length];

        for (int i = 0; i < cellClasses.length; i++) {
            String cellClassName = cellClasses[i];

            Class<?> cl = DataTypeRegistry.getInstance().getCellClass(cellClassName).orElseThrow(
                () -> new InvalidSettingsException("Data cell class \"" + cellClassName + "\" is unknown."));
            try {
                shortCutsLookup[i] = CellClassInfo.get((Class<? extends DataCell>)cl, null);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException(
                    "Unable to instantiate CellClassInfo for class \"" + cellClasses[i] + "\"", e);
            }
        }
        return shortCutsLookup;
    }

    @SuppressWarnings("unchecked")
    private static CellClassInfo[] readCellClassInfoArrayFromMetaVersion2(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        NodeSettingsRO typeSubSettings = settings.getNodeSettings(TableStoreFormat.CFG_CELL_CLASSES);
        Set<String> keys = typeSubSettings.keySet();
        CellClassInfo[] shortCutsLookup = new CellClassInfo[keys.size()];
        int i = 0;
        for (String s : keys) {
            NodeSettingsRO single = typeSubSettings.getNodeSettings(s);
            String className = single.getString(TableStoreFormat.CFG_CELL_SINGLE_CLASS);

            Class<?> cl = DataTypeRegistry.getInstance().getCellClass(className)
                .orElseThrow(() -> new InvalidSettingsException("Can't load data cell class '" + className + "'"));

            DataType elementType = null;
            if (single.containsKey(TableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE)) {
                NodeSettingsRO subTypeConfig = single.getNodeSettings(TableStoreFormat.CFG_CELL_SINGLE_ELEMENT_TYPE);
                elementType = DataType.load(subTypeConfig);
            }
            try {
                shortCutsLookup[i] = CellClassInfo.get((Class<? extends DataCell>)cl, elementType);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Unable to instantiate CellClassInfo for class \"" + className
                    + "\", element type: " + elementType);
            }
            i++;
        }
        return shortCutsLookup;
    }

    /**
     * Method for obtaining the {@link File} from which this reader reads.
     *
     * @return the file from which this reader reads
     */
    public File getFile() {
        return m_file;
    }

    /**
     * Method for obtaining the {@link DataTableSpec} beloning to the table read by this table store reader.
     *
     * @return the spec belonging to this reader, not null
     */
    protected final DataTableSpec getSpec() {
        return m_spec;
    }

    /**
     * Perform lookup for the DataCell class info given the argument byte.
     *
     * @param identifier The byte as read from the stream.
     * @return the associated cell class info
     * @throws IOException If the byte is invalid.
     */
    public final CellClassInfo getTypeForChar(final byte identifier) throws IOException {
        int shortCutIndex = (byte)(identifier - BYTE_TYPE_START);
        if (shortCutIndex < 0 || shortCutIndex >= m_shortCutsLookup.length) {
            throw new IOException("Unknown shortcut byte '" + identifier + "'");
        }
        return m_shortCutsLookup[shortCutIndex];
    }

    /**
     * Register a new iterator with this buffer.
     *
     * @param it The iterator
     */
    protected void registerNewIteratorInstance (final TableStoreCloseableRowIterator it) {
        LOGGER.debug("Opening input stream on file \"" + m_file.getAbsolutePath() + "\", "
                + m_nrOpenInputStreams + " open streams");
        it.setReader(this);
        synchronized (m_openIteratorSet) {
            m_nrOpenInputStreams.incrementAndGet();
            m_openIteratorSet.put(it, DUMMY);
        }
    }

    /**
     * Clear all iterators handled by this reader.
     */
    public void clearIteratorInstances() {
        synchronized (m_openIteratorSet) {
            // removeFromHash is false here, since clearing iterators would lead to a ConcurrentModificationException
            m_openIteratorSet.keySet().stream().filter(f -> f != null).forEach(f -> clearIteratorInstance(f, false));
            m_openIteratorSet.clear();
        }
    }

    /**
     * Clear the argument iterator (free the allocated resources).
     *
     * @param it The iterator
     * @param removeFromHash Whether to remove from global hash.
     */
    private void clearIteratorInstance(final TableStoreCloseableRowIterator it, final boolean removeFromHash) {
        String closeMes = (m_file != null) ? "Closing input stream on \"" + m_file.getAbsolutePath() + "\", " : "";
        try {
            if (it.performClose()) {
                synchronized (m_openIteratorSet) {
                    m_nrOpenInputStreams.decrementAndGet();
                    LOGGER.debug(closeMes + m_nrOpenInputStreams + " remaining", null);
                    if (removeFromHash) {
                        m_openIteratorSet.remove(it);
                    }
                }
            }
        } catch (IOException ioe) {
            LOGGER.debug(closeMes + "failed!", ioe);
        }
    }

    private static List<OutputStream> DEBUG_STREAMS = new ArrayList<>();

    static {
        if (KNIMEConstants.ASSERTIONS_ENABLED && Platform.OS_LINUX.equals(Platform.getOS())) {
            for (int i = 0; i < 10; i++) {
                try {
                    DEBUG_STREAMS.add(Files.newOutputStream(PathUtils.createTempFile("test", ".txt")));
                } catch (IOException ex) {
                    LOGGER.debug(ex.getMessage(), ex);
                }
            }
        }
    }

    /** prints debug output to catch random failures on test system, see AP-7978. */
    protected static void checkAndReportOpenFiles(final IOException ex) {
        if (KNIMEConstants.ASSERTIONS_ENABLED && Platform.OS_LINUX.equals(Platform.getOS())) {
            if (ex.getMessage().contains("Too many") || ex.getMessage().contains("Zu viele")) {
                try {
                    for (OutputStream os : DEBUG_STREAMS) {
                        os.close();
                    }
                    DEBUG_STREAMS.clear();

                    String pid = ManagementFactory.getRuntimeMXBean().getName();
                    pid = pid.substring(0, pid.indexOf('@'));
                    ProcessBuilder pb = new ProcessBuilder("lsof", "-p", pid);
                    Process lsof = pb.start();
                    try (InputStream is = lsof.getInputStream()) {
                        LOGGER.debug("Currently open files: " + IOUtils.toString(is, "US-ASCII"));
                    }
                } catch (IOException e) {
                    LOGGER.debug("Could not list open files: " + e.getMessage(), e);
                }
            }
        }
    }

    public static abstract class TableStoreCloseableRowIterator extends CloseableRowIterator {
        private AbstractTableStoreReader m_reader;

        /**
         * @param reader the table store reader to set
         *
         * @since 3.7
         */
        public void setReader(final AbstractTableStoreReader reader) {
            m_reader = reader;
        }

        /** {@inheritDoc} */
        @Override
        public final void close() {
            m_reader.clearIteratorInstance(this, true);
        }

        public abstract boolean performClose() throws IOException;
    }

}
