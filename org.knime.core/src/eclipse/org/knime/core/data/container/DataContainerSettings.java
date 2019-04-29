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
import java.util.function.Supplier;

import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataTableDomainCreator;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConfigurableWorkflowContext;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.IDuplicateChecker;

/**
 * The data container settings. Solely used for benchmarking.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class DataContainerSettings {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataContainerSettings.class);

    /** The default number of cells to be held in memory. */
    private static final int DEF_MAX_CELLS_IN_MEMORY = 5000;

    /**
     * For asynchronous table writing (default) the cache size. It's the number of rows that are kept in memory until
     * handed off to the write routines.
     *
     * @see KNIMEConstants#PROPERTY_ASYNC_WRITE_CACHE_SIZE
     */
    private static final int DEF_ASYNC_CACHE_SIZE = 10;

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
     * Builder pattern.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private static class Builder {

        /** The maximum number of cells in memory. */
        private int m_maxCellsInMemory;

        /** The synchronous write flag. */
        private boolean m_syncIO;

        /** The maximum number of asynchronous write threads. */
        private int m_maxAsyncWriteThreads;

        /** The asynchronous cache size. */
        private int m_asyncCacheSize;

        /** The initialize domain flag used by the {@link IDataTableDomainCreator}. */
        private boolean m_initDomain;

        /** The maximum number of domain values used by {@link IDuplicateChecker}. */
        private int m_maxDomainValues;

        /** The function creating new instances of {@link IDuplicateChecker}. */
        private Supplier<IDuplicateChecker> m_duplicateCheckerCreator;

        /** The function creating new instances of {@link IDataTableDomainCreator}. */
        private BiFunction<DataTableSpec, Boolean, IDataTableDomainCreator> m_tableDomainCreatorFunction;

        /** The {@link BufferSettings}. */
        private BufferSettings m_bufferSettings;

        /**
         * Constructor.
         *
         * @param settings the {@code DataContainerSettings} to be copied
         */
        Builder(final DataContainerSettings settings) {
            m_maxCellsInMemory = settings.m_maxCellsInMemory;
            m_syncIO = settings.m_syncIO;
            m_maxAsyncWriteThreads = settings.m_maxAsyncWriteThreads;
            m_asyncCacheSize = settings.m_asyncCacheSize;
            m_initDomain = settings.m_initDomain;
            m_maxDomainValues = settings.m_maxDomainValues;
            m_duplicateCheckerCreator = settings.m_duplicateCheckerCreator;
            m_tableDomainCreatorFunction = settings.m_tableDomainCreatorFunction;
            m_bufferSettings = settings.m_bufferSettings;
        }

        Builder setMaxCellsInMemory(final int maxCellsInMemory) {
            m_maxCellsInMemory = maxCellsInMemory;
            return this;
        }

        Builder useSyncIO(final boolean useSyncIO) {
            m_syncIO = useSyncIO;
            return this;
        }

        Builder setMaxAsyncWriteThreads(final int maxAsyncWriteThreads) {
            m_maxAsyncWriteThreads = maxAsyncWriteThreads;
            return this;
        }

        Builder setAsyncCacheSize(final int asyncCacheSize) {
            m_asyncCacheSize = asyncCacheSize;
            return this;
        }

        Builder setInitDomain(final boolean initDomain) {
            m_initDomain = initDomain;
            return this;
        }

        Builder setMaxDomainValues(final int maxDomainValues) {
            m_maxDomainValues = maxDomainValues;
            return this;
        }

        Builder setDuplicateCheckerCreator(final Supplier<IDuplicateChecker> supplier) {
            m_duplicateCheckerCreator = supplier;
            return this;
        }

        Builder setTableDomainCreatorFunction(final BiFunction<DataTableSpec, Boolean, IDataTableDomainCreator> func) {
            m_tableDomainCreatorFunction = func;
            return this;
        }

        Builder setBufferSettings(final BufferSettings bufferSettings) {
            m_bufferSettings = bufferSettings;
            return this;
        }

        /**
         * Creates the {@link DataContainerSettings}.
         *
         * @return the {@code DataContainerSettings}
         */
        DataContainerSettings build() {
            return new DataContainerSettings(this);
        }

    }

    /** The maximum number of cells in memory. */
    private final int m_maxCellsInMemory;

    /** The synchronous write flag. */
    private final boolean m_syncIO;

    /** The maximum number of asynchronous write threads. */
    private final int m_maxAsyncWriteThreads;

    /** The asynchronous cache size. */
    private final int m_asyncCacheSize;

    /** The initialize domain flag used by the {@link IDataTableDomainCreator}. */
    private final boolean m_initDomain;

    /** The maximum number of domain values used by {@link IDuplicateChecker}. */
    private final int m_maxDomainValues;

    /** The function creating new instances of {@link IDuplicateChecker}. */
    private final Supplier<IDuplicateChecker> m_duplicateCheckerCreator;

    /** The function creating new instances of {@link IDataTableDomainCreator}. */
    private final BiFunction<DataTableSpec, Boolean, IDataTableDomainCreator> m_tableDomainCreatorFunction;

    /** The {@link BufferSettings}. */
    private final BufferSettings m_bufferSettings;

    /** The default {@code BufferSettings} instance. */
    private static final BufferSettings DEFAULT_BUFFER_INSTANCE = new BufferSettings();

    /** The default {@code DataContainerSettings} instance. */
    private static final DataContainerSettings DEFAULT_CONTAINER_INSTANCE = new DataContainerSettings();

    /**
     * Default constructor.
     */
    private DataContainerSettings() {
        m_maxCellsInMemory = initMaxCellsInMemory();
        m_syncIO = initSynchronousIO();
        m_maxAsyncWriteThreads = initMaxAsyncWriteThreads();
        m_asyncCacheSize = initAsyncCacheSize();
        m_initDomain = initDomain();
        m_maxDomainValues = initMaxDomainValues();
        m_duplicateCheckerCreator = () -> new DuplicateChecker();
        m_tableDomainCreatorFunction = (spec, initDomain) -> new DataTableDomainCreator(spec, initDomain);
        m_bufferSettings = DEFAULT_BUFFER_INSTANCE;
    }

    /**
     * Constructor.
     *
     * @param builder the builder holding the settings
     */
    private DataContainerSettings(final Builder builder) {
        m_maxCellsInMemory = builder.m_maxCellsInMemory;
        m_syncIO = builder.m_syncIO;
        m_maxAsyncWriteThreads = builder.m_maxAsyncWriteThreads;
        m_asyncCacheSize = builder.m_asyncCacheSize;
        m_initDomain = builder.m_initDomain;
        m_maxDomainValues = builder.m_maxDomainValues;
        m_duplicateCheckerCreator = builder.m_duplicateCheckerCreator;
        m_tableDomainCreatorFunction = builder.m_tableDomainCreatorFunction;
        m_bufferSettings = builder.m_bufferSettings;
    }

    /**
     * Returns the default {@link DataContainerSettings}.
     *
     * @return the default {@code DataContainerSettings}
     */
    public static DataContainerSettings getDefault() {
        Optional<WorkflowContext> optContext = Optional.ofNullable(NodeContext.getContext())//
            .map(NodeContext::getWorkflowManager)//
            .map(WorkflowManager::getContext);
        if (optContext.isPresent() && optContext.get() instanceof ConfigurableWorkflowContext) {
            return ((ConfigurableWorkflowContext)optContext.get()).getContainerSettings();
        }
        return DEFAULT_CONTAINER_INSTANCE;
    }

    /**
     * Returns the maximum number of cells kept in memory.
     *
     * @return max cells in memory
     */
    public int getMaxCellsInMemory() {
        return m_maxCellsInMemory;
    }

    /**
     * Returns whether to write tables in a synchronous or asynchronous fashion.
     *
     * @return flag indicating how to write tables to disc
     */
    public boolean useSyncIO() {
        return m_syncIO;
    }

    /**
     * Returns the maximum number of asynchronous write threads.
     *
     * @return maximum number of asynchronous write threads
     */
    int getMaxAsyncWriteThreads() {
        return m_maxAsyncWriteThreads;
    }

    /**
     * Returns the asynchronous cache size.
     *
     * @return the asynchronous cache size
     */
    int getAsyncCacheSize() {
        return m_asyncCacheSize;
    }

    /**
     * Returns the initialize domain flag.
     *
     * @return the initialize domain flag
     */
    boolean getInitializeDomain() {
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
     * Creates a {@link IDuplicateChecker} ensuring that the row keys are unique.
     *
     * @return a {@link IDuplicateChecker}
     */
    IDuplicateChecker createDuplicateChecker() {
        return m_duplicateCheckerCreator.get();
    }

    /**
     * Initializes a domain creator.
     *
     * @param spec the data table sepc
     * @return an instance of {@link IDataTableDomainCreator}
     */
    IDataTableDomainCreator createDomainCreator(final DataTableSpec spec) {
        final IDataTableDomainCreator creator = m_tableDomainCreatorFunction.apply(spec, m_initDomain);
        creator.setMaxPossibleValues(m_maxDomainValues);
        return creator;
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
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the maximum number of cells in memory.
     *
     * @param maxCellsInMemory the new maximum number of cells in memory
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withMaxCellsInMemory(final int maxCellsInMemory) {
        final Builder b = new Builder(this);
        b.setMaxCellsInMemory(maxCellsInMemory);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the use synchronous write flag.
     *
     * @param useSyncIO the new use synchronous write flag
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withSyncIO(final boolean useSyncIO) {
        final Builder b = new Builder(this);
        b.useSyncIO(useSyncIO);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the maximum number of asynchronous write threads.
     *
     * @param maxAsyncWriteThreads the new maximum number of asynchronous write threads
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withMaxAsyncWriteThreads(final int maxAsyncWriteThreads) {
        final Builder b = new Builder(this);
        b.setMaxAsyncWriteThreads(maxAsyncWriteThreads);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the asynchronous cache size.
     *
     * @param asyncCacheSize the new asynchronous cache size
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withAsyncCacheSize(final int asyncCacheSize) {
        final Builder b = new Builder(this);
        b.setAsyncCacheSize(asyncCacheSize);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the initialize domain flag used by the
     * {@link IDuplicateChecker}.
     *
     * @param initDomain the new init domain flag
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withInitializedDomain(final boolean initDomain) {
        final Builder b = new Builder(this);
        b.setInitDomain(initDomain);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the maximum number of domain values stored by the
     * {@link IDataTableDomainCreator}.
     *
     * @param maxDomainValues the new maximum number of domain values
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withMaxDomainValues(final int maxDomainValues) {
        final Builder b = new Builder(this);
        b.setMaxDomainValues(maxDomainValues);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the supplier to create instances of
     * {@link IDuplicateChecker}.
     *
     * @param supplier the new {@code IDuplicateChecker} function
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withDuplicateChecker(final Supplier<IDuplicateChecker> supplier) {
        final Builder b = new Builder(this);
        b.setDuplicateCheckerCreator(supplier);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the function to creates instances of
     * {@link IDataTableDomainCreator}.
     *
     * @param func the new {@code IDataTableDomainCreator} function
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings
        withDomainCreator(final BiFunction<DataTableSpec, Boolean, IDataTableDomainCreator> func) {
        final Builder b = new Builder(this);
        b.setTableDomainCreatorFunction(func);
        return b.build();
    }

    /**
     * Creates a new <code>DataContainerSetting</code> object by replicating the current
     * <code>DataContainerSetting</code> instance and solely changes the {@link BufferSettings}.
     *
     * @param bufferSettings the {@code BufferSettings}
     * @return a new instance of {@code DataContainerSettings}
     */
    public DataContainerSettings withBufferSettings(final BufferSettings bufferSettings) {
        final Builder b = new Builder(this);
        b.setBufferSettings(bufferSettings);
        return b.build();
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
                    throw new NumberFormatException("max cell count in memory < 0" + newSize);
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
     * Initializes the synchronous I/O flag w.r.t. the defined properties.
     *
     * @return the synchronous I/O flag
     */
    private static boolean initSynchronousIO() {
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_SYNCHRONOUS_IO)) {
            LOGGER.debug("Using synchronous IO; " + KNIMEConstants.PROPERTY_SYNCHRONOUS_IO + " is set");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Initializes the maximum number of asynchronous write threads depending on the OS architecture.
     *
     * @return the maximum number of asynchornous write threads
     */
    private static int initMaxAsyncWriteThreads() {
        return Platform.ARCH_X86.equals(Platform.getOSArch()) ? 10 : 50;
    }

    /**
     * Initializes the asynchronous cache size w.r.t. the defined properties.
     *
     * @return the asynchronous chace size
     */
    private static int initAsyncCacheSize() {
        int asyncCacheSize = DEF_ASYNC_CACHE_SIZE;
        String envAsyncCache = KNIMEConstants.PROPERTY_ASYNC_WRITE_CACHE_SIZE;
        String valAsyncCache = System.getProperty(envAsyncCache);
        if (valAsyncCache != null) {
            String s = valAsyncCache.trim();
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new NumberFormatException("async write cache < 0" + newSize);
                }
                asyncCacheSize = newSize;
                LOGGER.debug("Setting asynchronous write cache to " + asyncCacheSize + " row(s)");
            } catch (NumberFormatException e) {
                LOGGER.warn(
                    "Unable to parse property " + envAsyncCache + ", using default (" + DEF_ASYNC_CACHE_SIZE + ")", e);
            }
        }
        return asyncCacheSize;
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
                    throw new NumberFormatException("max possible value count < 0" + newSize);
                }
                maxPossValues = newSize;
                LOGGER.debug("Setting default count for possible domain values to " + maxPossValues);
            } catch (NumberFormatException e) {
                LOGGER.warn(
                    "Unable to parse property " + envPossValues + ", using default (" + DEF_MAX_POSSIBLE_VALUES + ")",
                    e);
            }
        }
        return maxPossValues;
    }
}
