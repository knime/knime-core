/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 13, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.container;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BufferSettings.BufferSettingsBuilder;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.DuplicateChecker;

/**
 * Represents settings when creating {@link DataContainer}, usually as part of a node's execution. Node developers
 * will only set settings exposed via {@link #builder()}. Another builder method, {@link #internalBuilder()}, is
 * solely used by the framework/benchmarks/test.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DataContainerSettings {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataContainerSettings.class);

    /** The default number of cells to be held in memory, ignoring system properties.  */
    private static final int DEF_MAX_CELLS_IN_MEMORY = 5000;

    /**
     * The amount of rows to be processed by a single thread when not forced to handle rows sequentially. It's the
     * number of rows that are kept in memory until handed off to the write routines.
     *
     * @see KNIMEConstants#PROPERTY_ASYNC_WRITE_CACHE_SIZE
     */
    private static final int DEF_ROW_BATCH_SIZE = 100;

    /**
     * The default number of possible values being kept at most. If the number of possible values in a column exceeds
     * this values, no values will be memorized. Can be changed via system property
     * {@link KNIMEConstants#PROPERTY_DOMAIN_MAX_POSSIBLE_VALUES}.
     *
     * @since 2.10
     */
    private static final int DEF_MAX_POSSIBLE_VALUES = 60;

    private static final boolean DEF_SEQUENTIAL_ROW_HANDLING = initSequentialIO();

    /** The default initialize domain flag. */
    private static final boolean DEF_INIT_DOMAIN = true;

    /**
     * Number of cells that are cached without being written to the temp file (see Buffer implementation); Its default
     * value can be changed using the java property {@link KNIMEConstants#PROPERTY_CELLS_IN_MEMORY}.
     *
     * <p>
     * This property should not be of interested to node implementations. It only concerns the (legacy) row table
     * backend.
     *
     * @since 5.3
     */
    public static final int MAX_CELLS_IN_MEMORY = initMaxCellsInMemory();

    /**
     * The default number of possible values in a column domain to be kept at most. This default value can be changed
     * using the java property {@link KNIMEConstants#PROPERTY_DOMAIN_MAX_POSSIBLE_VALUES}.
     *
     * @since 5.3
     */
    public static final int MAX_POSSIBLE_VALUES = initMaxDomainValues();

    /**
     * Internal Builder that allows KNIME framework and testing code to customize containers, no public API.
     * Method descriptions can be derived from the corresponding getters.
     *
     * @since 5.3
     * @noreference This interface is not intended to be referenced by clients.
     */
    @SuppressWarnings("javadoc")
    public sealed interface InternalBuilder extends Builder permits BuilderImpl {

        InternalBuilder withBufferSettings(Consumer<BufferSettingsBuilder> builderConsumer);

        InternalBuilder withRowKeysEnabled(final boolean enableRowKeys);

        InternalBuilder withForceCopyOfBlobs(final boolean forceCopyOfBlobs);

        InternalBuilder withMaxDomainValues(final int maxDomainValues);

        InternalBuilder withRowBatchSize(final int rowBatchSize);

        InternalBuilder withMaxThreadsPerContainer(final int maxThreadsPerDataContainer);

        InternalBuilder withMaxContainerThreads(final int maxDataContainerThreads);

        InternalBuilder withForceSequentialRowHandling(final boolean useSequentialIO);

        /**
         * For the (old) row backend, the size of the table (in #cells) until which the table is kept in memory. This
         * setting is no longer used in the columnar backend. When the row backend is active, the default is derived
         * from the node's memory policy. For non-{@link org.knime.core.node.BufferedDataTable} the default is
         * {@value DataContainerSettings#DEF_MAX_CELLS_IN_MEMORY}, whereby it can also be set via the
         * {@link KNIMEConstants#PROPERTY_CELLS_IN_MEMORY} property.
         *
         * <p>
         * This setting can be considered deprecated once the columnar backend is default.
         *
         * @param maxCellsInMemory The value, whereby a negative values assumes the default value (see above)
         * @return this
         */
        InternalBuilder withMaxCellsInMemory(final int maxCellsInMemory);

        @Override
        InternalBuilder withInitializedDomain(final boolean initDomain);

        @Override
        InternalBuilder withCheckDuplicateRowKeys(final boolean enableCheckDuplicateRowKeys);

        @Override
        InternalBuilder withDomainUpdate(final boolean enableDomainUpdate);

    }

    /**
     * A builder allowing to set build and tuning properties for a {@link DataContainer} or {@link RowContainer}.
     * @since 5.3
     */
    public sealed interface Builder permits InternalBuilder {

        /**
         * Whether the container should inherit the column domains from the {@link DataTableSpec} that is used to
         * initialize the container/table. Default value is <code>true</code>.
         *
         * @param initDomain <code>false</code> when not using the domain for the argument spec, instead calculating the
         *            domain while rows are added.
         * @return this
         */
        Builder withInitializedDomain(final boolean initDomain);

        /**
         * Whether row key duplicate checking is turned on or off. Default is <code>true</code>, unless set by
         * {@link KNIMEConstants#PROPERTY_DISABLE_ROWID_DUPLICATE_CHECK system property}. This property is usually
         * set when copying (parts of) an input table or when uniqueness can be guaranteed otherwise.
         *
         * @param enableCheckDuplicateRowKeys Whether to enable this property.
         * @return this
         */
        Builder withCheckDuplicateRowKeys(final boolean enableCheckDuplicateRowKeys);

        /**
         * Whether the columns' domain should be updated while rows are added. Default is <code>true</code> but
         * can be disabled by this method, e.g. when (parts of) an input table are copied to the output.
         *
         * @param enableDomainUpdate This property.
         * @return this
         */
        Builder withDomainUpdate(final boolean enableDomainUpdate);

        /** @return A new settings object representing the currently set properties. */
        DataContainerSettings build();
    }

    /**
     * Builder pattern.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private static final class BuilderImpl implements InternalBuilder {

        /** The maximum number of cells in memory. */
        private OptionalInt m_maxCellsInMemory;

        /** The sequential write flag. */
        private boolean m_sequentialIO;

        /** The maximum number of threads that can be used by {@link DataContainer} instances. */
        private int m_maxDataContainerThreads;

        /** The maximum number of threads per {@link DataContainer} instance. */
        private int m_maxThreadsPerDataContainer;

        /** The amount of rows to be processed by a single thread when not forced to handle rows sequentially. */
        private int m_rowBatchSize;

        /** The initialize domain flag used by the {@link DataTableDomainCreator}. */
        private boolean m_initDomain;

        /** The maximum number of domain values used by {@link DuplicateChecker}. */
        private int m_maxDomainValues;

        /** The force copy of blobs flag **/
        private boolean m_forceCopyOfBlobs;

        /** The flag to enable row keys in this {@link DataContainer}. **/
        private boolean m_enableRowKeys;

        /** The {@link BufferSettings}. */
        private BufferSettings m_bufferSettings = BufferSettings.getDefault();

        private boolean m_enableCheckDuplicateRowKeys;

        private boolean m_enableDomainUpdate;

        /**
         * Constructor.
         *
         * @param settings the {@code DataContainerSettings} to be copied
         */
        BuilderImpl(final DataContainerSettings settings) {
            m_maxCellsInMemory = settings.m_maxCellsInMemory;
            m_sequentialIO = settings.m_sequentialIO;
            m_maxDataContainerThreads = settings.m_maxDataContainerThreads;
            m_maxThreadsPerDataContainer = settings.m_maxThreadsPerDataContainer;
            m_rowBatchSize = settings.m_rowBatchSize;
            m_initDomain = settings.m_initDomain;
            m_maxDomainValues = settings.m_maxDomainValues;
            m_bufferSettings = settings.m_bufferSettings;
            m_enableRowKeys = settings.m_enableRowKeys;
            m_forceCopyOfBlobs = settings.m_forceCopyOfBlobs;
            m_enableCheckDuplicateRowKeys = settings.m_enableCheckDuplicateRowKeys;
            m_enableDomainUpdate = settings.m_enableDomainUpdate;
        }

        @Override
        public BuilderImpl withMaxCellsInMemory(final int maxCellsInMemory) {
            m_maxCellsInMemory = maxCellsInMemory < 0 ? OptionalInt.empty() : OptionalInt.of(maxCellsInMemory);
            return this;
        }

        @Override
        public BuilderImpl withForceSequentialRowHandling(final boolean useSequentialIO) {
            m_sequentialIO = useSequentialIO;
            return this;
        }

        @Override
        public BuilderImpl withMaxContainerThreads(final int maxDataContainerThreads) {
            m_maxDataContainerThreads = maxDataContainerThreads;
            return this;
        }

        @Override
        public BuilderImpl withMaxThreadsPerContainer(final int maxThreadsPerDataContainer) {
            m_maxThreadsPerDataContainer = maxThreadsPerDataContainer;
            return this;
        }

        @Override
        public BuilderImpl withRowBatchSize(final int rowBatchSize) {
            m_rowBatchSize = rowBatchSize;
            return this;
        }

        @Override
        public BuilderImpl withMaxDomainValues(final int maxDomainValues) {
            m_maxDomainValues = maxDomainValues;
            return this;
        }

        @Override
        public BuilderImpl withForceCopyOfBlobs(final boolean forceCopyOfBlobs) {
            m_forceCopyOfBlobs = forceCopyOfBlobs;
            return this;
        }

        @Override
        public BuilderImpl withRowKeysEnabled(final boolean enableRowKeys) {
            m_enableRowKeys = enableRowKeys;
            return this;
        }

        @Override
        public BuilderImpl withBufferSettings(final Consumer<BufferSettingsBuilder> builderConsumer) {
            BufferSettingsBuilder builder = BufferSettings.builder();
            builderConsumer.accept(builder);
            m_bufferSettings = builder.build();
            return this;
        }

        @Override
        public BuilderImpl withInitializedDomain(final boolean initDomain) {
            m_initDomain = initDomain;
            return this;
        }

        @Override
        public BuilderImpl withCheckDuplicateRowKeys(final boolean enableCheckDuplicateRowKeys) {
            m_enableCheckDuplicateRowKeys = enableCheckDuplicateRowKeys;
            return this;
        }

        @Override
        public BuilderImpl withDomainUpdate(final boolean enableDomainUpdate) {
            m_enableDomainUpdate = enableDomainUpdate;
            return this;
        }

        /**
         * @return the new {@link DataContainerSettings} representing the current values.
         */
        @Override
        public DataContainerSettings build() {
            return new DataContainerSettings(this);
        }

    }

    /** The maximum number of cells in memory, empty object to use defaults (special handling for BDT in nodes) */
    private final OptionalInt m_maxCellsInMemory;

    /** The sequential write flag. */
    private final boolean m_sequentialIO;

    /** The maximum number of threads that can be used by {@link DataContainer} instances. */
    private final int m_maxDataContainerThreads;

    /** The maximum number of threads per {@link DataContainer} instance. */
    private final int m_maxThreadsPerDataContainer;

    /** The amount of rows to be processed by a single thread when not forced to handle rows sequentially. */
    private final int m_rowBatchSize;

    /** The initialize domain flag used by the {@link DataTableDomainCreator}. */
    private final boolean m_initDomain;

    /** The maximum number of domain values used by {@link DuplicateChecker}. */
    private final int m_maxDomainValues;

    /** The force copy of blobs flag **/
    private final boolean m_forceCopyOfBlobs;

    /** The flag to enable row keys in this {@link DataContainer}. **/
    private final boolean m_enableRowKeys;

    /** The {@link BufferSettings}. */
    private final BufferSettings m_bufferSettings;

    /** Whether perform row key duplicate checking while creating tables. */
    private boolean m_enableCheckDuplicateRowKeys;

    /** If to update domain while rows are added. */
    private boolean m_enableDomainUpdate;

    private DataContainerSettings() {
        m_maxCellsInMemory = OptionalInt.empty();
        m_sequentialIO = DEF_SEQUENTIAL_ROW_HANDLING;
        m_maxDataContainerThreads = initMaxDataContainerThreads();
        int maxThreadsPerDataContainer = initThreadsPerDataContainerInstance();
        if (maxThreadsPerDataContainer > m_maxDataContainerThreads) {
            maxThreadsPerDataContainer = m_maxDataContainerThreads;
            LOGGER.debug(
                "The number of threads per data container cannot be larger than the total number of data container "
                    + "threads. Value has been set to according to the the total number of data container threads");
        }
        m_maxThreadsPerDataContainer = maxThreadsPerDataContainer;
        m_rowBatchSize = initRowBatchSize();
        m_initDomain = initDomain();
        m_maxDomainValues = MAX_POSSIBLE_VALUES;
        m_forceCopyOfBlobs = initForceCopyOfBlobs();
        m_enableRowKeys = initEnableRowKeys();
        m_bufferSettings = BufferSettings.getDefault();
        m_enableCheckDuplicateRowKeys = initRowKeyDuplicateCheck();
        m_enableDomainUpdate = true;
    }

    private DataContainerSettings(final BuilderImpl builder) {
        m_maxCellsInMemory = builder.m_maxCellsInMemory;
        // system property overwrites everything
        m_sequentialIO = DEF_SEQUENTIAL_ROW_HANDLING || builder.m_sequentialIO;
        m_maxDataContainerThreads = builder.m_maxDataContainerThreads;
        m_maxThreadsPerDataContainer = Math.min(m_maxDataContainerThreads, builder.m_maxThreadsPerDataContainer);
        m_rowBatchSize = builder.m_rowBatchSize;
        m_initDomain = builder.m_initDomain;
        m_maxDomainValues = builder.m_maxDomainValues;
        m_bufferSettings = builder.m_bufferSettings;
        m_forceCopyOfBlobs = builder.m_forceCopyOfBlobs;
        m_enableRowKeys = builder.m_enableRowKeys;
        m_enableCheckDuplicateRowKeys = builder.m_enableCheckDuplicateRowKeys;
        m_enableDomainUpdate = builder.m_enableDomainUpdate;
    }

    /**
     * @return the default {@code DataContainerSettings}, inherits properties from installation, workflow, etc.
     */
    public static DataContainerSettings getDefault() {
        // While it would be tempting to always return the same instance here, it does not make sense, since the
        // default settings can change while KAP is running (e.g., when the user changes which data storage format to
        // use)
        return Optional.ofNullable(NodeContext.getContext())//
            .map(NodeContext::getWorkflowManager)
            .flatMap(WorkflowManager::getDataContainerSettings)
            .orElseGet(DataContainerSettings::new);
    }

    /**
     * Creates a {@link InternalBuilder} instance, used by the framework, testing and benchmarks. No public API.
     * @return A new instance.
     * @since 5.3
     * @noreference This method is not intended to be referenced by clients.
     */
    public static InternalBuilder internalBuilder() {
        return internalBuilder(getDefault());
    }

    /**
     * Creates a {@link InternalBuilder} instance, used by the framework, testing and benchmarks. No public API.
     * @param settings draft settings
     * @return A new instance.
     * @since 5.3
     * @noreference This method is not intended to be referenced by clients.
     */
    public static InternalBuilder internalBuilder(final DataContainerSettings settings) {
        return new BuilderImpl(settings);
    }

    /**
     * New builder with defaults (inherited from code, system properties, installation). See the various methods in the
     * the returned class for further configuration. The create settings object is then used in the
     * {@link org.knime.core.node.ExecutionContext} to build a table.
     *
     * @return a new builder instance.
     * @since 5.3
     */
    public static Builder builder() {
        return internalBuilder(getDefault());
    }

    /**
     * Returns the maximum number of cells kept in memory, only concerns row-backend. Empty optional to use default
     * ({@link DataContainerSettings#MAX_CELLS_IN_MEMORY}).
     *
     * @return max cells in memory, per table.
     * @noreference This method is not intended to be referenced by clients.
     */
    public OptionalInt getMaxCellsInMemory() {
        return m_maxCellsInMemory;
    }

    /**
     * Returns whether to force rows to be handled sequentially, i.e. one row after another. Handling a row encompasses
     * (1) validation against a given table spec, (2) updating the table's domain, (3) checking for duplicates among row
     * keys, and (4) handling of blob and file store cells. Independent of this setting, the underlying {@link Buffer}
     * class always writes rows to disk sequentially, yet potentially asynchronously.
     *
     * @return flag indicating whether to force rows to be handled sequentially
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean isForceSequentialRowHandling() {
        return m_sequentialIO;
    }

    /**
     * Returns the maximum total number threads that can be used by {@link DataContainer} instances.
     *
     * @return maximum number of asynchronous write threads
     * @noreference This method is not intended to be referenced by clients.
     */
    public int getMaxContainerThreads() {
        return m_maxDataContainerThreads;
    }

    /**
     * Returns the maximum number of threads per {@link DataContainer}.
     *
     * @return maximum number of threads per container
     */
    int getMaxThreadsPerContainer() {
        return m_maxThreadsPerDataContainer;
    }

    /**
     * Returns the amount of rows to be processed by a single thread when not forced to handle rows sequentially.
     *
     * @return the row batch size
     * @noreference This method is not intended to be referenced by clients.
     */
    public int getRowBatchSize() {
        return m_rowBatchSize;
    }

    /**
     * Returns the initialize domain flag, used to initialize a container's domain with the spec passed in
     * the constructor.
     *
     * @since 4.2.2
     * @return the initialize domain flag
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean isInitializeDomain() {
        return m_initDomain;
    }

    /**
     * Returns the maximum number of domain values.
     *
     * @return returns the maximum number of domain values
     * @noreference This method is not intended to be referenced by clients.
     */
    public int getMaxDomainValues() {
        return m_maxDomainValues;
    }

    /**
     * Row key duplicate checking property, see {@link Builder#withCheckDuplicateRowKeys(boolean)}.
     *
     * @return the enableDuplicateChecking property
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.3
     */
    public boolean isCheckDuplicateRowKeys() {
        return m_enableCheckDuplicateRowKeys;
    }

    /**
     * Enable domain updates while table is created, see {@link Builder#withCheckDuplicateRowKeys(boolean)}.
     *
     * @return the enableDomainUpdate property.
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.3
     */
    public boolean isEnableDomainUpdate() {
        return m_enableDomainUpdate;
    }

    /**
     * @return <source>true</source> if blobs should be copied by this {@link DataContainer}.
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean isForceCopyOfBlobs() {
        return m_forceCopyOfBlobs;
    }

    /**
     * @return <source>true</source> if {@link RowKey}s are part of this {@link DataContainer}.
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean isEnableRowKeys() {
        return m_enableRowKeys;
    }

    /**
     * Returns the {@link BufferSettings}.
     *
     * @return the {@code BufferSettings}
     */
    public BufferSettings getBufferSettings() {
        return m_bufferSettings;
    }

    /**
     * @return default value for force copy of blobs
     */
    private static boolean initForceCopyOfBlobs() {
        return false;
    }

    /**
     * @return default value.
     */
    private static boolean initEnableRowKeys() {
        return true;
    }

    /**
     * Initializes the maximum number of cells in memory w.r.t. the defined properties.
     *
     * @return the maximum number of cells in memory
     */
    private static int initMaxCellsInMemory() {
        int size = DEF_MAX_CELLS_IN_MEMORY;
        String envCellsInMem = KNIMEConstants.PROPERTY_CELLS_IN_MEMORY;
        String valCellsInMem = System.getProperty(envCellsInMem);
        if (valCellsInMem != null) {
            String s = valCellsInMem.trim();
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new IllegalArgumentException("max cell count in memory < 0" + newSize);
                }
                size = newSize;
                LOGGER.debug("Setting max cell count to be held in memory to " + size);
            } catch (NumberFormatException e) {
                LOGGER.warn(
                    "Unable to parse property " + envCellsInMem + ", using default (" + DEF_MAX_CELLS_IN_MEMORY + ")",
                    e);
            }
        }
        return size;
    }

    /**
     * Initializes the sequential I/O flag w.r.t. the defined properties.
     *
     * @return the sequential I/O flag
     */
    private static boolean initSequentialIO() {
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_SYNCHRONOUS_IO)) {
            LOGGER.debugWithFormat("(Row Backend:) Handling rows sequentially; %s is set",
                KNIMEConstants.PROPERTY_SYNCHRONOUS_IO);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Whether row id duplicate checking is turned on/off by default wrt to sys props.
     *
     * @return row id checking property (default is true)
     */
    private static boolean initRowKeyDuplicateCheck() {
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_DISABLE_ROWID_DUPLICATE_CHECK)) {
            LOGGER.debugWithFormat("RowID duplicate checking is off; %s is set",
                KNIMEConstants.PROPERTY_DISABLE_ROWID_DUPLICATE_CHECK);
            return false;
        }
        return true;
    }

    /**
     * Initializes the maximum number of that can be used by {@link DataContainer} instances.
     *
     * @return the maximum number of that can be used by {@code DataContainer} instances
     *
     */
    private static int initMaxDataContainerThreads() {
        int maxDataContainerThreads = Runtime.getRuntime().availableProcessors();
        final String prop = KNIMEConstants.PROPERTY_MAX_THREADS_TOTAL;
        final String val = System.getProperty(prop);
        if (val != null) {
            String s = val.trim();
            try {
                maxDataContainerThreads = Integer.parseInt(s);
                if (maxDataContainerThreads <= 0) {
                    throw new IllegalArgumentException(
                        "maximum number of container threads cannot be less than or equal to 0");
                }
                LOGGER.debug("Settings maximum number of container threads to " + maxDataContainerThreads);
            } catch (final IllegalArgumentException e) {
                LOGGER.warn("Unable to parse property " + prop + ", using default (" + maxDataContainerThreads
                    + " = number of available processors)");
            }
        }
        return maxDataContainerThreads;
    }

    /**
     * Initializes the maximum number of threads that can be used per {@link DataContainer} instance.
     *
     * @return the maximum number of that can be used per {@code DataContainer} instances
     *
     */
    private static int initThreadsPerDataContainerInstance() {
        int maxThreadsPerContainer = Runtime.getRuntime().availableProcessors();
        final String prop = KNIMEConstants.PROPERTY_MAX_THREADS_INSTANCE;
        final String val = System.getProperty(prop);
        if (val != null) {
            String s = val.trim();
            try {
                maxThreadsPerContainer = Integer.parseInt(s);
                if (maxThreadsPerContainer <= 0) {
                    throw new IllegalArgumentException(
                        "maximum number of threads per data container cannot be less than or equal to 0");
                }
                LOGGER.debug("Settings maximum number of threads per container to " + maxThreadsPerContainer);
            } catch (final IllegalArgumentException e) {
                LOGGER.warn("Unable to parse property " + prop + ", using default (" + maxThreadsPerContainer
                    + " = number of available processors)");
            }
        }
        return maxThreadsPerContainer;
    }

    /**
     * Initializes the row batch size w.r.t. the defined properties.
     *
     * @return the row batch size
     */
    private static int initRowBatchSize() {
        int rowBatchSize = DEF_ROW_BATCH_SIZE;
        String envAsyncCache = KNIMEConstants.PROPERTY_ASYNC_WRITE_CACHE_SIZE;
        String valAsyncCache = System.getProperty(envAsyncCache);
        if (valAsyncCache != null) {
            String s = valAsyncCache.trim();
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new IllegalArgumentException("row batch size < 0" + newSize);
                }
                rowBatchSize = newSize;
                LOGGER.debug("Setting row batch size to " + rowBatchSize + " row(s)");
            } catch (IllegalArgumentException e) {
                LOGGER.warn(
                    "Unable to parse property " + envAsyncCache + ", using default (" + DEF_ROW_BATCH_SIZE + ")", e);
            }
        }
        return rowBatchSize;
    }

    /**
     * Initializes the initialize domain flag.
     *
     * @return the initialize domain flag
     */
    private static boolean initDomain() {
        return DEF_INIT_DOMAIN;
    }

    /**
     * Initializes the maximum number of possible domain values w.r.t. the defined properties.
     *
     * @return the maximum number of possible domain values
     */
    private static int initMaxDomainValues() {
        int maxPossValues = DEF_MAX_POSSIBLE_VALUES;
        String envPossValues = KNIMEConstants.PROPERTY_DOMAIN_MAX_POSSIBLE_VALUES;
        String valPossValues = System.getProperty(envPossValues);
        if (valPossValues != null) {
            String s = valPossValues.trim();
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new IllegalArgumentException("max possible value count < 0" + newSize);
                }
                maxPossValues = newSize;
                LOGGER.debug("Setting default count for possible domain values to " + maxPossValues);
            } catch (IllegalArgumentException e) {
                LOGGER.warn(
                    "Unable to parse property " + envPossValues + ", using default (" + DEF_MAX_POSSIBLE_VALUES + ")",
                    e);
            }
        }
        return maxPossValues;
    }
}
