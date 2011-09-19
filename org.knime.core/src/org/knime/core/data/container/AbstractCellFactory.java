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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.ExecutionMonitor;

/**
 * Default implementation of a {@link CellFactory}, which creates more than
 * a single new column.
 *
 * <p>As of v2.5 the input table can be processed concurrently. This property
 * should only be set if (i) the processing of an individual row is expensive,
 * i.e. takes significantly longer than pure I/O and (ii) there are no
 * interdependency between the row calculations.
 *
 * @see SingleCellFactory
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractCellFactory implements CellFactory {

    private final DataColumnSpec[] m_colSpecs;

    private int m_maxParallelWorkers = -1;
    private int m_maxQueueSize = -1;

    /** Creates instance, which will produce content for the columns as
     * specified by the array argument. The calculation is done sequentially
     * (no parallel processing of input).
     * @param colSpecs The specs of the columns being created.
     */
    public AbstractCellFactory(final DataColumnSpec... colSpecs) {
        if (colSpecs == null || Arrays.asList(colSpecs).contains(null)) {
            throw new NullPointerException("Argument must not be null or "
                    + "contain null elements");
        }
        m_colSpecs = colSpecs;
    }

    /** Creates instance, which will produce content for the columns as
     * specified by the array argument.
     * @param processConcurrently If to process the rows concurrently (must
     * only be true if there are no interdependency between the rows).
     * @param colSpecs The specs of the columns being created.
     * @see #setParallelProcessing(boolean)
     */
    public AbstractCellFactory(final boolean processConcurrently,
            final DataColumnSpec... colSpecs) {
        this(colSpecs);
        setParallelProcessing(processConcurrently);
    }

    /** Creates instance, which will produce content for the columns as
     * specified by the array argument.
     * @param processConcurrently If to process the rows concurrently (must
     * only be true if there are no interdependency between the rows).
     * @param workerCount see {@link #setParallelProcessing(boolean, int, int)}
     * @param maxQueueSize see {@link #setParallelProcessing(boolean, int, int)}
     * @param colSpecs The specs of the columns being created.
     * @see #setParallelProcessing(boolean, int, int)
     */
    public AbstractCellFactory(final boolean processConcurrently,
            final int workerCount, final int maxQueueSize,
            final DataColumnSpec... colSpecs) {
        this(colSpecs);
        setParallelProcessing(processConcurrently, workerCount, maxQueueSize);
    }

    /** Enables or disables parallel processing of the rows. The two relevant
     * parameters for the number of parallel workers and maximum work queue
     * size are determined automatically based on the number of available cores.
     * @param value If to enable parallel processing (assumes independency
     * of individual row calculations).
     */
    public final void setParallelProcessing(final boolean value) {
        int maxParallelWorkers = (int)Math.ceil(1.5
                * Runtime.getRuntime().availableProcessors());
        int maxQueueSize = 10 * maxParallelWorkers;
        setParallelProcessing(value, maxParallelWorkers, maxQueueSize);
    }

    /** Enables or disables parallel processing of the rows. The two relevant
     * parameters for the number of parallel workers and maximum work queue
     * need to be specified.
     * @param value If to enable parallel processing (assumes independency
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
     * are queued until the long-running taks finishes.)
     */
    public final void setParallelProcessing(final boolean value,
            final int maxParallelWorkers, final int maxQueueSize) {
        if (value) {
            if (maxParallelWorkers <= 0) {
                throw new IllegalArgumentException("Worker count must be "
                        + "larger than 0: " + maxParallelWorkers);
            }
            if (maxQueueSize < maxParallelWorkers) {
                throw new IllegalArgumentException("Queue size must be larger"
                        + "than worker count: "
                        + maxQueueSize + "<" + maxParallelWorkers);
            }
            m_maxParallelWorkers = maxParallelWorkers;
            m_maxQueueSize = maxQueueSize;
        } else {
            m_maxParallelWorkers = -1;
            m_maxQueueSize = -1;
        }
    }

    /** @return the maxParallelWorkers */
    int getMaxParallelWorkers() {
        return m_maxParallelWorkers;
    }

    /** @return the maxQueueSize */
    int getMaxQueueSize() {
        return m_maxQueueSize;
    }

    /** {@inheritDoc} */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_colSpecs;
    }

    /** {@inheritDoc} */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount, "Processed row "
                + curRowNr + " (\"" + lastKey + "\")");
    }

}
