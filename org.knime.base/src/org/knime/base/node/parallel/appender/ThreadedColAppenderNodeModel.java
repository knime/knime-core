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
 */
package org.knime.base.node.parallel.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.util.ThreadPool;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class ThreadedColAppenderNodeModel extends NodeModel {
    private class Submitter implements Callable<Void> {
        private final BufferedDataTable[] m_data;

        private final ExtendedCellFactory[] m_cellFacs;

        private final List<Future<BufferedDataContainer[]>> m_futures;

        private final ExecutionContext m_exec;

        private final DataTableSpec[] m_specs;

        final AtomicInteger m_processedRows = new AtomicInteger();

        Submitter(final BufferedDataTable[] data,
                final ExtendedCellFactory[] cellFacs,
                final List<Future<BufferedDataContainer[]>> futures,
                final ExecutionContext exec) {
            m_data = data;
            m_cellFacs = cellFacs;
            m_futures = futures;
            m_exec = exec;

            m_specs = new DataTableSpec[cellFacs.length];
            for (int i = 0; i < m_specs.length; i++) {
                m_specs[i] = new DataTableSpec(m_cellFacs[i].getColumnSpecs());
            }
        }

        /**
         * {@inheritDoc}
         */
        public Void call() throws Exception {
            final double max = m_data[0].getRowCount();
            final int chunkSize =
                    (int)Math.ceil(max / (4.0 * m_workers.getMaxThreads()));
            final RowIterator it = m_data[0].iterator();
            BufferedDataContainer container = null;
            int count = 0, chunks = 0;
            while (chunkSize > 0) {
                m_exec.checkCanceled();

                if ((count++ % chunkSize == 0) || !it.hasNext()) {
                    if (container != null) {
                        container.close();
                        chunks++;
                        m_futures.add(m_workers.submit(createCallable(
                                container.getTable(), chunkSize, max)));
                    }
                    if (!it.hasNext()) {
                        break;
                    }

                    container =
                            m_exec.createDataContainer(m_data[0]
                                    .getDataTableSpec());
                }
                container.addRowToTable(it.next());
            }
            return null;
        }

        private Callable<BufferedDataContainer[]> createCallable(
                final BufferedDataTable data, final int chunkSize,
                final double max) {
            return new Callable<BufferedDataContainer[]>() {
                public BufferedDataContainer[] call() throws Exception {
                    BufferedDataContainer[] result =
                            new BufferedDataContainer[m_specs.length];
                    for (int i = 0; i < result.length; i++) {
                        result[i] = m_exec.createDataContainer(m_specs[i]);

                        for (DataRow r : data) {
                            m_exec.checkCanceled();
                            DataCell[] newCells = m_cellFacs[i].getCells(r);
                            DataRow newRow =
                                    new DefaultRow(r.getKey(), newCells);
                            result[i].addRowToTable(newRow);

                            int pr = m_processedRows.incrementAndGet();
                            if (pr % 10 == 0) {
                                // 5% of the progress are reserved for combining
                                // the partial results lateron
                                m_exec.setProgress(0.9 * pr / max, "Processed "
                                        + pr + " rows");
                            }
                        }
                        result[i].close();
                    }

                    return result;
                }
            };
        }

    }

    /** The default maximum number of threads for each threaded node. */
    public static final int DEFAULT_MAX_THREAD_COUNT =
            Runtime.getRuntime().availableProcessors() + 1;

    /** The execution service that is used. */
    private final ThreadPool m_workers =
            KNIMEConstants.GLOBAL_THREAD_POOL
                    .createSubPool(DEFAULT_MAX_THREAD_COUNT);

    private BufferedDataTable[] m_additionalTables;

    /**
     * Creates a new AbstractParallelNodeModel.
     *
     * @param nrDataIns The number of {@link DataTable} elements expected as
     *            inputs.
     * @param nrDataOuts The number of {@link DataTable} objects expected at the
     *            output.
     */
    public ThreadedColAppenderNodeModel(final int nrDataIns,
            final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }

    /**
     * This method is called before the first chunked is processed. The method
     * must return a cell factory for each output table. The factory must create
     * the new cells for each row in the input table and also specify where the
     * new columns should be placed in the output table.
     *
     * @param data the input data tables
     * @return extended cell factories, one for each output table
     * @throws Exception if something goes wrong during preparation
     */
    protected abstract ExtendedCellFactory[] prepareExecute(
            final DataTable[] data) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected final BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {
        final ExtendedCellFactory[] cellFacs = prepareExecute(data);

        assert (cellFacs != null) : "Implementation Error: The array of "
                + "generated output table specs must not be null.";
        assert (cellFacs.length == getNrOutPorts()) : "Implementation Error: "
                + " Number of provided DataTableSpecs doesn't match number of "
                + "output ports";

        if (data[0].getRowCount() == 0) {
            BufferedDataTable[] result = new BufferedDataTable[getNrOutPorts()];
            for (int i = 0; i < cellFacs.length; i++) {
                DataTableSpec spec =
                        createOutputSpec(data[i].getDataTableSpec(),
                                cellFacs[i]);
                BufferedDataContainer cont = exec.createDataContainer(spec);
                cont.close();
                result[i] = cont.getTable();
            }
            return result;
        }

        exec.setProgress(0, "Processing chunks");
        m_additionalTables =
                new BufferedDataTable[Math.max(0, data.length - 1)];
        System.arraycopy(data, 1, m_additionalTables, 0,
                m_additionalTables.length);

        final List<Future<BufferedDataContainer[]>> futures =
                new ArrayList<Future<BufferedDataContainer[]>>();

        Submitter submitter = new Submitter(data, cellFacs, futures, exec);

        try {
            m_workers.runInvisible(submitter);
        } catch (IllegalThreadStateException ex) {
            // this node has not been started by a thread from a thread pool.
            // This is odd, but may happen
            submitter.call();
        }

        final BufferedDataTable[] combinedResults =
                getCombinedResults(futures, exec);
        final BufferedDataTable[] resultTables =
                new BufferedDataTable[getNrOutPorts()];

        for (int i = 0; i < getNrOutPorts(); i++) {
            final int leftColCount = data[i].getDataTableSpec().getNumColumns();
            ColumnDestination[] dests = cellFacs[i].getColumnDestinations();
            ColumnRearranger crea =
                    new ColumnRearranger(data[i].getDataTableSpec());
            int[] newPositions = new int[leftColCount + dests.length];
            for (int m = 0; m < newPositions.length; m++) {
                newPositions[m] = m;
            }

            // first part of handling replacements: remove the columns
            // that should be replaced; necessary because of duplicate
            // column names in the appended ones
            for (int k = 0; k < dests.length; k++) {
                if (dests[k] instanceof ReplaceColumn) {
                    int insertIndex = ((ReplaceColumn)dests[k]).getIndex();
                    crea.remove(newPositions[insertIndex]);
                    for (int m = insertIndex; m < newPositions.length; m++) {
                        newPositions[m]--;
                    }

                    newPositions[insertIndex] = Integer.MIN_VALUE;
                }
            }

            resultTables[i] =
                    exec.createColumnRearrangeTable(data[i], crea, exec
                            .createSubExecutionContext(0));

            resultTables[i] =
                    exec.createJoinedTable(resultTables[i], combinedResults[i],
                            exec.createSubExecutionContext(0.1));

            // move replacement columns to their final destinations
            crea = new ColumnRearranger(resultTables[i].getDataTableSpec());
            for (int k = 0; k < dests.length; k++) {
                if (dests[k] instanceof ReplaceColumn) {
                    int oldPos = newPositions[k + leftColCount];
                    int insertIndex = ((ReplaceColumn)dests[k]).getIndex();
                    if (oldPos != insertIndex) {
                        crea.move(oldPos, insertIndex);

                        for (int m = 0; m < newPositions.length; m++) {
                            if ((newPositions[m] >= insertIndex)
                                    && (newPositions[m] < oldPos)) {
                                newPositions[m]++;
                            }
                        }
                        newPositions[k + leftColCount] = insertIndex;
                    }
                }
            }

            // then handle explicit inserts
            for (int k = 0; k < dests.length; k++) {
                if (dests[k] instanceof InsertColumn) {
                    int oldPos = newPositions[k + leftColCount];
                    int insertIndex = ((InsertColumn)dests[k]).getIndex();
                    crea.move(oldPos, insertIndex);
                    for (int m = 0; m < newPositions.length; m++) {
                        if ((newPositions[m] >= insertIndex)
                                && (newPositions[m] < oldPos)) {
                            newPositions[m]++;
                        }
                    }
                    newPositions[k + leftColCount] = insertIndex;
                }
            }
            resultTables[i] =
                    exec.createColumnRearrangeTable(resultTables[i], crea, exec
                            .createSubExecutionContext(0));
        }

        m_additionalTables = null;
        return postExecute(resultTables, exec);
    }

    /**
     * This method is called after all rows have been processed and combined
     * into the final result tables. Implementors of subclasses may override
     * this in order to e.g. change the spec of the result tables. The default
     * implementation returns the parameter unaltered.
     *
     * @param res the combined result tables
     * @param exec the currently active execution context
     * @return the output tables of the node.
     */
    protected BufferedDataTable[] postExecute(final BufferedDataTable[] res,
            final ExecutionContext exec) {
        return res;
    }

    /**
     * Returns all additional tables passed into the node, i.e. tables from 1 to
     * n. This result is only non-<code>null</code> during
     * {@link #execute(BufferedDataTable[], ExecutionContext)}.
     *
     * @return the array of additional input tables, or <code>null</code> if
     *         the node is not currently executing
     */
    protected final BufferedDataTable[] getAdditionalTables() {
        return m_additionalTables;
    }

    private BufferedDataTable[] getCombinedResults(
            final List<Future<BufferedDataContainer[]>> futures,
            final ExecutionContext exec) throws InterruptedException,
            ExecutionException, CanceledExecutionException {
        final BufferedDataTable[][] tempTables =
                new BufferedDataTable[getNrOutPorts()][futures.size()];
        int k = 0;
        for (Future<BufferedDataContainer[]> results : futures) {
            try {
                exec.checkCanceled();
            } catch (CanceledExecutionException ex) {
                for (Future<BufferedDataContainer[]> cancel : futures) {
                    cancel.cancel(true);
                }
                throw ex;
            }

            final BufferedDataContainer[] temp = results.get();

            if ((temp == null) || (temp.length != getNrOutPorts())) {
                throw new IllegalStateException("Invalid result. Execution "
                        + " failed, reason: data is null or number "
                        + "of outputs wrong.");
            }

            for (int i = 0; i < temp.length; i++) {
                tempTables[i][k] = temp[i].getTable();
            }
            k++;
        }

        final BufferedDataTable[] combinedResults =
                new BufferedDataTable[getNrOutPorts()];
        for (int i = 0; i < combinedResults.length; i++) {
            combinedResults[i] =
                    exec.createConcatenateTable(exec, tempTables[i]);
        }

        return combinedResults;
    }

    /**
     * Sets the maximum number of threads that may be used by this node.
     *
     * @param count the maximum thread count
     */
    public void setMaxThreads(final int count) {
        m_workers.setMaxThreads(count);
    }

    /**
     * Returns the output spec based on the input spec and the cell factory.
     *
     * @param inSpec the input spec
     * @param cellFactory the cell factory used
     * @return the output spec
     */
    protected static DataTableSpec createOutputSpec(final DataTableSpec inSpec,
            final ExtendedCellFactory cellFactory) {
        ColumnRearranger rea = new ColumnRearranger(inSpec);

        ColumnDestination[] dests = cellFactory.getColumnDestinations();
        for (int k = 0; k < dests.length; k++) {
            ColumnDestination cd = dests[k];

            CellFactory cf =
                    new SingleCellFactory(cellFactory.getColumnSpecs()[k]) {
                        @Override
                        public DataCell getCell(final DataRow row) {
                            return null;
                        }
                    };

            if (cd instanceof AppendColumn) {
                rea.append(cf);
            } else if (cd instanceof InsertColumn) {
                rea.insertAt(((InsertColumn)cd).getIndex(), cf);
            } else {
                rea.replace(cf, ((ReplaceColumn)cd).getIndex());
            }
        }
        return rea.createSpec();
    }
}
