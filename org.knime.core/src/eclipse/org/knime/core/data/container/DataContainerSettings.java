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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BufferSettings.BufferSettingsBuilder;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.DuplicateChecker;

/**
 * Represents settings when creating {@link DataContainer}, usually as part of anode's execution. Node developers
 * will only set settings exposed via {@link #builder()}. Another builder method, {@link #internalBuilder()}, is
 * solely used by the framework/benchmarks/test.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DataContainerSettings {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataContainerSettings.class);

    /** The default number of cells to be held in memory. */
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

    /** The default initialize domain flag. */
    private static final boolean DEF_INIT_DOMAIN = false;

    /**
     * Internal Builder that allows KNIME framework and testing code to customize containers, no public API.
     * Method descriptions can be derived from the corresponding getters.
     *
     * @since 5.3
     * @noreference This interface is not intended to be referenced by clients.
     */
    @SuppressWarnings("javadoc")
    public sealed interface InternalBuilder permits BuilderImpl {

        InternalBuilder withBufferSettings(Consumer<BufferSettingsBuilder> builderConsumer);

        InternalBuilder withRowKeysEnabled(final boolean enableRowKeys);

        InternalBuilder withForceCopyOfBlobs(final boolean forceCopyOfBlobs);

        InternalBuilder withMaxDomainValues(final int maxDomainValues);

        InternalBuilder withInitializedDomain(final boolean initDomain);

        InternalBuilder withRowBatchSize(final int rowBatchSize);

        InternalBuilder withMaxThreadsPerContainer(final int maxThreadsPerDataContainer);

        InternalBuilder withMaxContainerThreads(final int maxDataContainerThreads);

        InternalBuilder withForceSequentialRowHandling(final boolean useSequentialIO);

        InternalBuilder withMaxCellsInMemory(final int maxCellsInMemory);

        Builder toExternalBuilder();

        DataContainerSettings build(); // shortcut to avoid detour via toExternalBuilder()

    }

    /**
     * @since 5.3
     */
    public sealed interface Builder permits BuilderImpl {

        Builder setRowKeyDuplicateCheck(final boolean enableDuplicateChecking);

        Builder setDomainUpdate(final boolean enableDomainUpdate);

        DataContainerSettings build();
    }

    /**
     * Builder pattern.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private final static class BuilderImpl implements InternalBuilder, Builder {

        /** The maximum number of cells in memory. */
        private int m_maxCellsInMemory;

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

        private boolean m_enableDuplicateChecking;

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
        }

        @Override
        public InternalBuilder withMaxCellsInMemory(final int maxCellsInMemory) {
            m_maxCellsInMemory = maxCellsInMemory;
            return this;
        }

        @Override
        public InternalBuilder withForceSequentialRowHandling(final boolean useSequentialIO) {
            m_sequentialIO = useSequentialIO;
            return this;
        }

        @Override
        public InternalBuilder withMaxContainerThreads(final int maxDataContainerThreads) {
            m_maxDataContainerThreads = maxDataContainerThreads;
            return this;
        }

        @Override
        public InternalBuilder withMaxThreadsPerContainer(final int maxThreadsPerDataContainer) {
            m_maxThreadsPerDataContainer = maxThreadsPerDataContainer;
            return this;
        }

        @Override
        public InternalBuilder withRowBatchSize(final int rowBatchSize) {
            m_rowBatchSize = rowBatchSize;
            return this;
        }

        @Override
        public InternalBuilder withInitializedDomain(final boolean initDomain) {
            m_initDomain = initDomain;
            return this;
        }

        @Override
        public InternalBuilder withMaxDomainValues(final int maxDomainValues) {
            m_maxDomainValues = maxDomainValues;
            return this;
        }

        @Override
        public InternalBuilder withForceCopyOfBlobs(final boolean forceCopyOfBlobs) {
            m_forceCopyOfBlobs = forceCopyOfBlobs;
            return this;
        }

        @Override
        public InternalBuilder withRowKeysEnabled(final boolean enableRowKeys) {
            m_enableRowKeys = enableRowKeys;
            return this;
        }

        @Override
        public InternalBuilder withBufferSettings(final Consumer<BufferSettingsBuilder> builderConsumer) {
            BufferSettingsBuilder builder = BufferSettings.builder();
            builderConsumer.accept(builder);
            m_bufferSettings = builder.build();
            return this;
        }

        @Override
        public Builder toExternalBuilder() {
            return this;
        }

        @Override
        public Builder setRowKeyDuplicateCheck(final boolean enableDuplicateChecking) {
            m_enableDuplicateChecking = enableDuplicateChecking;
            return this;
        }

        @Override
        public Builder setDomainUpdate(final boolean enableDomainUpdate) {
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

    /** The function creating new instances of {@link DuplicateChecker}. */
    private final Supplier<DuplicateChecker> m_duplicateCheckerCreator;

    /** The function creating new instances of {@link DataTableDomainCreator}. */
    private final BiFunction<DataTableSpec, Boolean, DataTableDomainCreator> m_tableDomainCreatorFunction;

    /** The maximum number of cells in memory. */
    private final int m_maxCellsInMemory;

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

    private DataContainerSettings() {
        m_duplicateCheckerCreator = () -> new DuplicateChecker(Integer.MAX_VALUE);
        m_tableDomainCreatorFunction = DataTableDomainCreator::new;
        m_maxCellsInMemory = initMaxCellsInMemory();
        m_sequentialIO = initSequentialIO();
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
        m_maxDomainValues = initMaxDomainValues();
        m_forceCopyOfBlobs = initForceCopyOfBlobs();
        m_enableRowKeys = initEnableRowKeys();
        m_bufferSettings = BufferSettings.getDefault();
    }

    private DataContainerSettings(final BuilderImpl builder) {
        m_duplicateCheckerCreator = () -> new DuplicateChecker(Integer.MAX_VALUE);
        m_tableDomainCreatorFunction = DataTableDomainCreator::new;
        m_maxCellsInMemory = builder.m_maxCellsInMemory;
        m_sequentialIO = builder.m_sequentialIO;
        m_maxDataContainerThreads = builder.m_maxDataContainerThreads;
        m_maxThreadsPerDataContainer = Math.min(m_maxDataContainerThreads, builder.m_maxThreadsPerDataContainer);
        m_rowBatchSize = builder.m_rowBatchSize;
        m_initDomain = builder.m_initDomain;
        m_maxDomainValues = builder.m_maxDomainValues;
        m_bufferSettings = builder.m_bufferSettings;
        m_forceCopyOfBlobs = builder.m_forceCopyOfBlobs;
        m_enableRowKeys = builder.m_enableRowKeys;
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
        return newInternalBuilderImpl();
    }

    /**
     * New builder with defaults (inherited from code, system properties, installation).
     * @return a new builder instance.
     * @since 5.3
     */
    public static Builder builder() {
        return newInternalBuilderImpl();
    }

    private static BuilderImpl newInternalBuilderImpl() {
        return new BuilderImpl(getDefault());
    }

    /**
     * Returns the maximum number of cells kept in memory, only concerns row-backend.
     *
     * @return max cells in memory, per table.
     */
    public int getMaxCellsInMemory() {
        return m_maxCellsInMemory;
    }

    /**
     * Returns whether to force rows to be handled sequentially, i.e. one row after another. Handling a row encompasses
     * (1) validation against a given table spec, (2) updating the table's domain, (3) checking for duplicates among row
     * keys, and (4) handling of blob and file store cells. Independent of this setting, the underlying {@link Buffer}
     * class always writes rows to disk sequentially, yet potentially asynchronously.
     *
     * @return flag indicating whether to force rows to be handled sequentially
     */
    public boolean isForceSequentialRowHandling() {
        return m_sequentialIO;
    }

    /**
     * Returns the maximum total number threads that can be used by {@link DataContainer} instances.
     *
     * @return maximum number of asynchronous write threads
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
     */
    public boolean getInitializeDomain() {
        return m_initDomain;
    }

    /**
     * Returns the maximum number of domain values.
     *
     * @return returns the maximum number of domain values
     */
    public int getMaxDomainValues() {
        return m_maxDomainValues;
    }

    /**
     * Creates a {@link DuplicateChecker} ensuring that the row keys are unique.
     *
     * @return a {@code DuplicateChecker}
     */
    DuplicateChecker createDuplicateChecker() {
        return m_duplicateCheckerCreator.get();
    }

    /**
     * Initializes a domain creator.
     *
     * @param spec the data table sepc
     * @return an instance of {@link DataTableDomainCreator}
     */
    DataTableDomainCreator createDomainCreator(final DataTableSpec spec) {
        final DataTableDomainCreator creator = m_tableDomainCreatorFunction.apply(spec, m_initDomain);
        creator.setMaxPossibleValues(m_maxDomainValues);
        return creator;
    }

    /**
     * @return <source>true</source> if blobs should be copied by this {@link DataContainer}.
     */
    public boolean isForceCopyOfBlobs() {
        return m_forceCopyOfBlobs;
    }

    /**
     * @return <source>true</source> if {@link RowKey}s are part of this {@link DataContainer}.
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
            LOGGER.debug("Handling rows sequentially; " + KNIMEConstants.PROPERTY_SYNCHRONOUS_IO + " is set");
            return true;
        } else {
            return false;
        }
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
