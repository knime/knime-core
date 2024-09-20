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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.ExecutionMonitor;

/**
 * Default implementation of a {@link CellFactory}, which creates more than
 * a single new column.
 *
 * The cells can be produced concurrently, see {@link Parallelization} for details.
 *
 * @see SingleCellFactory
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractCellFactory implements CellFactory {

    /**
     * A capability defined in the constructor suggesting to the framework how the computation can be parallelized. This
     * is a hint to the framework and does not necessarily mean that the computation is actually parallelized.
     *
     * @since 5.4
     */
    public enum Parallelization {
            /** No parallelization is possible (e.g. due to row-dependencies) */
            NONE,
            /** Parallelization by row, this will also enable parallel computation when used with table row backend. */
            ASSUME_EXPENSIVE,
            /**
             * Parallelization by table chunk (partition of the table), this will enable parallelization in the columnar
             * backend but not in the row backend.
             */
            ASSUME_CHEAP
    }

    /**
     * Template for the progress message.
     */
    private static final String PROGRESS_TEMPLATE = "Processed row %s/%s (\"%s\")";

    /**
     * The parallelization mode for this factory. Effectively final unless you use deprecated API.
     */
    private Parallelization m_parallelization;

    private final DataColumnSpec[] m_colSpecs;

    private int m_maxParallelWorkers = -1;
    private int m_maxQueueSize = -1;

    /** True if the deprecated {@link #setProgress(int, int, RowKey, ExecutionMonitor)} method is overridden. If so,
     * it will be called by the default implementation of the (new) setProgress method. */
    private final boolean m_isSetProgressWithIntOverridden;

    private FileStoreFactory m_factory;

    /**
     * Creates instance, which will produce content for the columns as specified by the array argument. The calculation
     * is done sequentially (no parallel processing of input).
     *
     * @param colSpecs The specs of the columns being created.
     * @see #AbstractCellFactory(Parallelization, DataColumnSpec...)
     */
    protected AbstractCellFactory(final DataColumnSpec... colSpecs) {
        this(Parallelization.NONE, colSpecs);
    }

    /**
     * Creates instance, which will produce content for the columns as specified by the array argument.
     *
     * @param parallelization how the creation of cells may be parallelized
     * @param colSpecs The specs of the columns being created.
     */
    protected AbstractCellFactory(final Parallelization parallelization, final DataColumnSpec... colSpecs) {
        if (colSpecs == null || Arrays.asList(colSpecs).contains(null)) {
            throw new NullPointerException("Argument must not be null or contain null elements");
        }
        m_colSpecs = colSpecs;
        m_parallelization = parallelization;
        m_isSetProgressWithIntOverridden = isSetProgressWithIntOverridden();
    }



    /** Called by the framework to set the file store factory prior execution.
     * See {@link #getFileStoreFactory()}.
     * @param factory The factory to set or null (after processing of the table). */
    final void setFileStoreFactory(final FileStoreFactory factory) {
        m_factory = factory;
    }

    /** Access to a file store factory during the invocation of {@link #getCells(org.knime.core.data.DataRow)}. This
     * method returns a non-null file store factory only during processing of the input table (when getCells is called).
     * @return the factory A non-null factory when {@link #getCells(org.knime.core.data.DataRow)} is (repeatedly) called
     * by the framework.
     * @since 2.11
     */
    protected final FileStoreFactory getFileStoreFactory() {
        return m_factory;
    }

    /**
     * Creates instance, which will produce content for the columns as specified by the array argument.
     *
     * @param processConcurrently If to process the rows concurrently (must only be true if there are no interdependency
     *            between the rows).
     * @param colSpecs The specs of the columns being created.
     * @see #setParallelProcessing(boolean)
     * @deprecated use {@link #AbstractCellFactory(Parallelization, DataColumnSpec...)}
     */
    @Deprecated(since = "5.4")
    protected AbstractCellFactory(final boolean processConcurrently, final DataColumnSpec... colSpecs) {
        this(processConcurrently ? Parallelization.ASSUME_EXPENSIVE : Parallelization.NONE, colSpecs);
        setParallelProcessing(processConcurrently);
    }

    /**
     * Creates instance, which will produce content for the columns as specified by the array argument.
     *
     * @param processConcurrently If to process the rows concurrently (must only be true if there are no interdependency
     *            between the rows).
     * @param workerCount see {@link #setParallelProcessing(boolean, int, int)}
     * @param maxQueueSize see {@link #setParallelProcessing(boolean, int, int)}
     * @param colSpecs The specs of the columns being created.
     * @see #setParallelProcessing(boolean, int, int)
     * @deprecated use {@link #AbstractCellFactory(Parallelization, DataColumnSpec...)}
     */
    @Deprecated(since = "5.4")
    protected AbstractCellFactory(final boolean processConcurrently, final int workerCount, final int maxQueueSize,
        final DataColumnSpec... colSpecs) {
        this(processConcurrently ? Parallelization.ASSUME_EXPENSIVE : Parallelization.NONE, colSpecs);
        setParallelProcessing(processConcurrently, workerCount, maxQueueSize);
    }

    /**
     * @return the parallelization defined in the constructor.
     * @since 5.4
     */
    public Parallelization getParallelization() {
        return m_parallelization;
    }

    /** Enables or disables parallel processing of the rows. The two relevant
     * parameters for the number of parallel workers and maximum work queue
     * size are determined automatically based on the number of available cores.
     * @param value If to enable parallel processing (assumes independency
     * of individual row calculations).
     * @since 2.5
     * @deprecated use {@link #AbstractCellFactory(Parallelization, DataColumnSpec...)}
     */
    @Deprecated(since = "5.4")
    public final void setParallelProcessing(final boolean value) {
        int maxParallelWorkers = (int)Math.ceil(1.5
                * Runtime.getRuntime().availableProcessors());
        int maxQueueSize = 10 * maxParallelWorkers;
        setParallelProcessing(value, maxParallelWorkers, maxQueueSize);
    }

    /** Returns true if parallel processing is enabled, either via the
     * constructor argument or by calling the
     * {@link #setParallelProcessing(boolean)} method.
     * @return true if input is processed concurrently. If true, the values
     * for {@link #getMaxParallelWorkers()} and {@link #getMaxQueueSize()} will
     * be at least 1 (whereby 1 is a corner case where the input is processed
     * synchronously with one dedicated worker thread).
     * @since 2.5.2
     * @deprecated use {@link #getParallelization()} instead
     */
    @Deprecated(since = "5.4")
    public final boolean isParallelProcessing() {
        return (getMaxParallelWorkers() > 0 && getMaxQueueSize() > 0) //
                || m_parallelization == Parallelization.ASSUME_EXPENSIVE; // this is the old behaviour

    }

    /** Enables or disables parallel processing of the rows. The two relevant
     * parameters for the number of parallel workers and maximum work queue
     * need to be specified.
     * @param value If to enable parallel processing (assumes independence
     * of individual row calculation).
     * @param maxParallelWorkers The number of parallel execution threads to
     * process the rows. This value may be overruled by the global thread pool
     * limit.
     * @param maxQueueSize The number of finished calculations that are stored
     * in memory before further executions are paused. (Background: The rows
     * are processed in the order defined by the input table's iterator. If a
     * certain row computation takes long the framework needs to temporarily
     * cache the results of the following rows - the cache size is determined
     * by this parameter. If this cache is full, no further row computations
     * are queued until the long-running task finishes.)
     * @since 2.5
     * @deprecated use {@link #AbstractCellFactory(Parallelization, DataColumnSpec...)}
     */
    @Deprecated(since = "5.4")
    public final void setParallelProcessing(final boolean value,
            final int maxParallelWorkers, final int maxQueueSize) {
        if (value) {
            if (maxParallelWorkers <= 0) {
                throw new IllegalArgumentException("Worker count must be "
                        + "larger than 0: " + maxParallelWorkers);
            }
            if (maxQueueSize < maxParallelWorkers) {
                throw new IllegalArgumentException("Queue size must be larger "
                        + "than worker count: "
                        + maxQueueSize + "<" + maxParallelWorkers);
            }
            m_maxParallelWorkers = maxParallelWorkers;
            m_maxQueueSize = maxQueueSize;
            m_parallelization = Parallelization.ASSUME_EXPENSIVE;
        } else {
            m_maxParallelWorkers = -1;
            m_maxQueueSize = -1;
            m_parallelization = Parallelization.NONE;
        }
    }

    /** The number of parallel workers or -1 if the input is processed
     * sequentially. See {@link #setParallelProcessing(boolean, int, int)}
     * for a detailed description. If parallel processing is enabled but
     * no detailed parameters are provided, the number of workers and the
     * {@link #getMaxQueueSize() queue size} are determined heuristically based
     * on the {@link Runtime#availableProcessors() available processors}.
     * @return The number of parallel workers or -1.
     * @since 2.5.2
     * @deprecated
     */
    @Deprecated(since = "5.4")
    public final int getMaxParallelWorkers() {
        return m_maxParallelWorkers;
    }

    /** The size of the processing queue if parallel processing is enabled or
     * -1 if the input is processed sequentially. See
     * {@link #setParallelProcessing(boolean, int, int)} and
     * {@link #getMaxParallelWorkers()} for details.
     * @return the maxQueueSize or -1.
     * @since 2.5.2
     * @deprecated
     */
    @Deprecated(since = "5.4")
    public final int getMaxQueueSize() {
        return m_maxQueueSize;
    }

    /** {@inheritDoc} */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_colSpecs;
    }

    /** Called after all rows have been processed (either successfully or failed).
     * Subclasses may override it to, e.g. set warning messages, release memory listeners etc.
     * @since 2.6 */
    public void afterProcessing() {
        // no op, possibly overwritten
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #setProgress(long, long, RowKey, ExecutionMonitor)} instead which supports more than
     *             {@link Integer#MAX_VALUE} rows
     */
    @Deprecated
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount,
            () -> String.format(PROGRESS_TEMPLATE, curRowNr, rowCount, lastKey));
    }


    /**
     * {@inheritDoc}
     * @since 3.0
     */
    @Override
    public void setProgress(final long curRowNr, final long rowCount, final RowKey lastKey, final ExecutionMonitor exec) {
        if (m_isSetProgressWithIntOverridden) {
            setProgress((int)curRowNr, KnowsRowCountTable.checkRowCount(rowCount), lastKey, exec);
        } else {
            exec.setProgress(curRowNr / (double)rowCount,
                () -> String.format(PROGRESS_TEMPLATE, curRowNr, rowCount, lastKey));
        }
    }

    /** See {@link #m_isSetProgressWithIntOverridden}.
     * @return that flag
     */
    private boolean isSetProgressWithIntOverridden() {
        Class<?> cl = getClass();
        do {
            try {
                cl.getDeclaredMethod("setProgress", Integer.TYPE, Integer.TYPE, RowKey.class, ExecutionMonitor.class);
                return true;
            } catch (Exception e) {
                // ignore, check superclass
            }
            cl = cl.getSuperclass();
        } while (!AbstractCellFactory.class.equals(cl));
        return false;
    }
}
