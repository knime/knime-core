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
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.Buffer;
import org.knime.core.data.container.CellClassInfo;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.KNIMEStreamConstants;
import org.knime.core.data.container.filter.FilterDelegateRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.ExecutionMonitor;
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

    private CellClassInfo[] m_shortCutsLookup;

    private IDataRepository m_dataRepository;

    private Buffer m_buffer;

    /** Input stream version **/
    private final int m_version;

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
        m_version = version;
    }

    /**
     * @param buffer the buffer to set
     * @param dataRepository Non-null repository to retrieve file stores and blobs from.
     */
    public final void setBufferAndDataRepository(final Buffer buffer,
        final IDataRepository dataRepository) {
        m_dataRepository = CheckUtils.checkArgumentNotNull(dataRepository);
        m_buffer = buffer;
    }

    /** @return the data repository set at construction time, not null. */
    public final IDataRepository getDataRepository() {
        return m_dataRepository;
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

    /**
     * Method for deserializig blob cells.
     *
     * @param address the address of the blob
     * @param type the type of the blob cell
     * @return the deserialized blob cell
     * @throws IOException if something goes wrong during deserialization
     */
    public final BlobWrapperDataCell createBlobWrapperCell(final BlobAddress address, final CellClassInfo type)
        throws IOException {
        Buffer blobBuffer = getBuffer();
        if (address.getBufferID() != blobBuffer.getBufferID()) {
            Optional<ContainerTable> cnTbl = blobBuffer.getDataRepository().getTable(address.getBufferID());
            if (!cnTbl.isPresent()) {
                throw new IOException("Unable to retrieve table that owns the blob cell");
            }
            blobBuffer = cnTbl.get().getBuffer();
        }
        return new BlobWrapperDataCell(blobBuffer, address, type);
    }

    /**
     * Returns a row iterator which returns each row one-by-one from the table.
     *
     * @return row iterator
     */
    public abstract TableStoreCloseableRowIterator iterator();

    /**
     * Provides a {@link TableStoreCloseableRowIterator} that is filtered according to a given {@link TableFilter} and
     * can be iterated over.
     *
     * @param filter the filter to be applied
     * @return a filtered iterator
     * @since 3.8
     */
    public TableStoreCloseableRowIterator iteratorWithFilter(final TableFilter filter) {
        return iteratorWithFilter(filter, null);
    }

    /**
     * Provides a {@link TableStoreCloseableRowIterator} that is filtered according to a given {@link TableFilter} and
     * can be iterated over. During iteration, a given {@link ExecutionMonitor} will update its progress.
     *
     * @param filter the filter to be applied
     * @param exec the execution monitor that shall be updated with progress or null if no progress updates are desired
     * @return a filtered iterator
     * @since 3.8
     */
    @SuppressWarnings("resource")
    public TableStoreCloseableRowIterator iteratorWithFilter(final TableFilter filter, final ExecutionMonitor exec) {
        final TableStoreCloseableRowIterator delegate = iterator();
        final FilterDelegateRowIterator filterDelegate = new FilterDelegateRowIterator(delegate, filter, exec);

        return new TableStoreCloseableRowIterator() {
            @Override
            public DataRow next() {
                return filterDelegate.next();
            }

            @Override
            public boolean hasNext() {
                return filterDelegate.hasNext();
            }

            @Override
            public void setBuffer(final Buffer buffer) {
                super.setBuffer(buffer);
                delegate.setBuffer(buffer);
            }

            @Override
            public boolean performClose() throws IOException {
                return delegate.performClose();
            }

        };
    }

    /**
     * Reads the cell class info shortcuts array from the node settings for container versions 6 and lower.
     *
     * @param settings The settings (written by
     *            {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @throws InvalidSettingsException thrown in case something goes wrong during de-serialization, e.g. a new version
     *             of a writer has been used which hasn't been installed on the current system.
     */
    @SuppressWarnings("unchecked")
    protected void readCellClassInfoArrayFromMetaVersion1x(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        String[] cellClasses = settings.getStringArray(TableStoreFormat.CFG_CELL_CLASSES);
        m_shortCutsLookup = new CellClassInfo[cellClasses.length];

        for (int i = 0; i < cellClasses.length; i++) {
            String cellClassName = cellClasses[i];

            Class<?> cl = DataTypeRegistry.getInstance().getCellClass(cellClassName).orElseThrow(
                () -> new InvalidSettingsException("Data cell class \"" + cellClassName + "\" is unknown."));
            try {
                m_shortCutsLookup[i] = CellClassInfo.get((Class<? extends DataCell>)cl, null);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException(
                    "Unable to instantiate CellClassInfo for class \"" + cellClasses[i] + "\"", e);
            }
        }
    }

    /**
     * Reads the cell class info shortcuts array from the node settings for container versions 7 and higher.
     *
     * @param settings The settings (written by
     *            {@link AbstractTableStoreWriter#writeMetaInfoAfterWrite(org.knime.core.node.NodeSettingsWO)})
     * @throws InvalidSettingsException thrown in case something goes wrong during de-serialization, e.g. a new version
     *             of a writer has been used which hasn't been installed on the current system.
     */
    @SuppressWarnings("unchecked")
    protected void readCellClassInfoArrayFromMetaVersion2(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        NodeSettingsRO typeSubSettings = settings.getNodeSettings(TableStoreFormat.CFG_CELL_CLASSES);
        Set<String> keys = typeSubSettings.keySet();
        m_shortCutsLookup = new CellClassInfo[keys.size()];
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
                m_shortCutsLookup[i] = CellClassInfo.get((Class<? extends DataCell>)cl, elementType);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Unable to instantiate CellClassInfo for class \"" + className
                    + "\", element type: " + elementType);
            }
            i++;
        }
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

    /**
     * Prints debug output to catch random failures on test system, see AP-7978.
     *
     * @param ex the exception to parse and evaluate
     */
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

    /**
     * A {@link CloseableRowIterator} that obtains the {@link DataRow DataRows} it iterates over from a table store on
     * the hard disk. It provides additional methods for registering the iterator with a {@link Buffer} and for closing
     * the file input stream provided by the underlying table store.
     */
    public static abstract class TableStoreCloseableRowIterator extends CloseableRowIterator {
        private Buffer m_buffer;

        /**
         * @param buffer the buffer to set
         *
         * @since 3.8
         */
        public void setBuffer(final Buffer buffer) {
            m_buffer = buffer;
        }

        /** {@inheritDoc} */
        @Override
        public final void close() {
            m_buffer.clearIteratorInstance(this, true);
        }

        /**
         * Close the file input stream provided by the underlying table store.
         *
         * @return true iff the operation was successful
         * @throws IOException if anything goes wrong during the close attempt
         */
        public abstract boolean performClose() throws IOException;
    }

}
