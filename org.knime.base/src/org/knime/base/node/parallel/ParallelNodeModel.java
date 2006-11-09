/* Created on Nov 8, 2006 1:07:07 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.knime.base.data.append.row.AppendedRowsTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.util.ThreadPool;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class ParallelNodeModel extends NodeModel {
    private class Submitter implements Runnable {
        private final BufferedDataTable[] m_data;

        private final ExtendedCellFactory[] m_cellFacs;

        private final List<Future<DataContainer[]>> m_futures;

        private final ExecutionContext m_exec;

        private final DataTableSpec[] m_specs;

        Submitter(final BufferedDataTable[] data,
                final ExtendedCellFactory[] cellFacs,
                final List<Future<DataContainer[]>> futures,
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
         * @see java.lang.Runnable#run()
         */
        public void run() {
            final double max = m_data[0].getRowCount();
            final int chunkSize = (int)(max / (4 * m_workers.getMaxThreads()));
            final RowIterator it = m_data[0].iterator();
            BufferedDataContainer container = null;
            int count = 0, chunks = 0;
            while (true) {
                if ((count++ % chunkSize == 0) || !it.hasNext()) {
                    try {
                        m_exec.checkCanceled();
                    } catch (CanceledExecutionException ex) {
                        return;
                    }

                    if (container != null) {
                        container.close();
                        try {
                            chunks++;
                            // 5% of work are reserved for combining the results
                            m_futures.add(m_workers.submit(getCallable(
                                    container.getTable(), chunkSize, chunks,
                                    0.95*max)));
                        } catch (InterruptedException ex) {
                            return;
                        }
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
        }

        private Callable<DataContainer[]> getCallable(
                final BufferedDataTable data, final int chunkSize,
                final int chunk, final double max) {
            return new Callable<DataContainer[]>() {
                public DataContainer[] call() throws Exception {
                    DataContainer[] result = new DataContainer[m_specs.length];
                    for (int i = 0; i < result.length; i++) {
                        result[i] = new DataContainer(m_specs[i]);

                        for (DataRow r : data) {
                            DataCell[] newCells = m_cellFacs[i].getCells(r);
                            DataRow newRow =
                                    new DefaultRow(r.getKey(), newCells);
                            result[i].addRowToTable(newRow);
                        }
                        result[i].close();
                    }

                    m_exec.setProgress(chunk * chunkSize / max);
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
     * @param nrDataIns the number of {@link DataTable}s expected as inputs
     * @param nrDataOuts the number of {@link DataTable}s expected at the
     *            output
     * @param nrPredParamsIns the number of
     *            {@link org.knime.core.node.ModelContent} elements available as
     *            inputs
     */
    public ParallelNodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, 0);
    }

    /**
     * Creates a new AbstractParallelNodeModel.
     * 
     * @param nrDataIns The number of {@link DataTable} elements expected as
     *            inputs.
     * @param nrDataOuts The number of {@link DataTable} objects expected at the
     *            output.
     */
    public ParallelNodeModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }

    /**
     * This method is called before the first chunked is processed. The method
     * must return a cell factory for each output table. The factory must create
     * the new cells for each row in the input table and also specifiy where the
     * new columns should be placed in the output table.
     * 
     * @param data the input data tables
     * @return extended cell factories, one for each output table
     * @throws Exception if something goes wrong during preparation
     */
    protected abstract ExtendedCellFactory[] prepareExecute(
            final DataTable[] data) throws Exception;

    /**
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected final BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {
        final ExtendedCellFactory[] cellFacs = prepareExecute(data);

        assert (cellFacs != null) : "Implementation Error: The array of "
                + "generated output table specs must not be null.";
        assert (cellFacs.length == getNrDataOuts()) : "Implementation Error: "
                + " Number of provided DataTableSpecs doesn't match number of "
                + "output ports";

        m_additionalTables =
                new BufferedDataTable[Math.max(0, data.length - 1)];
        System.arraycopy(data, 1, m_additionalTables, 0,
                m_additionalTables.length);

        final List<Future<DataContainer[]>> futures =
                new ArrayList<Future<DataContainer[]>>();

        Submitter submitter = new Submitter(data, cellFacs, futures, exec);

        try {
            m_workers.runInvisible(submitter);
        } catch (IllegalThreadStateException ex) {
            // this node has not been started by a thread from a thread pool.
            // This is odd, but may happen
            submitter.run();
        }

        final AppendedRowsTable[] combinedResults =
                getCombinedResults(futures, exec);
        final BufferedDataTable[] resultTables =
                new BufferedDataTable[getNrDataOuts()];

        for (int i = 0; i < getNrDataOuts(); i++) {
            final int outTableNr = i;

            ColumnRearranger rea =
                    new ColumnRearranger(data[0].getDataTableSpec());

            ColumnDestination[] dests = cellFacs[i].getColumnDestinations();
            for (int k = 0; k < dests.length; k++) {
                final int cellIndex = k;

                ColumnDestination cd = dests[k];

                CellFactory cf = new CellFactory() {
                    public DataCell[] getCells(final DataRow row) {
                        return new DataCell[]{row.getCell(cellIndex)};
                    }

                    public DataColumnSpec[] getColumnSpecs() {
                        return new DataColumnSpec[]{combinedResults[outTableNr]
                                .getDataTableSpec().getColumnSpec(cellIndex)};
                    }

                    public void setProgress(final int curRowNr,
                            final int rowCount, final RowKey lastKey,
                            final ExecutionMonitor exek) {
                        // do nothing here
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

            resultTables[i] =
                    exec.createColumnRearrangeTable(data[0], rea, exec
                            .createSilentSubProgress(0.05));
        }

        return resultTables;
    }

    private AppendedRowsTable[] getCombinedResults(
            final List<Future<DataContainer[]>> futures,
            final ExecutionContext exec) throws InterruptedException,
            ExecutionException, CanceledExecutionException {
        final DataTable[][] tempTables =
                new DataTable[getNrDataOuts()][futures.size()];
        int k = 0;
        for (Future<DataContainer[]> results : futures) {
            try {
                exec.checkCanceled();
            } catch (CanceledExecutionException ex) {
                for (Future<DataContainer[]> cancel : futures) {
                    cancel.cancel(true);
                }
                throw ex;
            }

            final DataContainer[] temp = results.get();

            if ((temp == null) || (temp.length != getNrDataOuts())) {
                throw new IllegalStateException("Invalid result. Execution "
                        + " failed, reason: data is null or number "
                        + "of outputs wrong.");
            }

            for (int i = 0; i < temp.length; i++) {
                tempTables[i][k] = temp[i].getTable();
            }
            k++;
        }

        final AppendedRowsTable[] combinedResults =
                new AppendedRowsTable[getNrDataOuts()];
        for (int i = 0; i < combinedResults.length; i++) {
            combinedResults[i] = new AppendedRowsTable(tempTables[i]);
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
}
