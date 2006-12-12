/*  
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Mar 13, 2006 (thor): created
 */
package org.knime.base.node.parallel.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.base.data.append.row.AppendedRowsTable;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.RowAppender;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeModel;
import org.knime.core.util.ThreadPool;

/**
 * This model is an extension of the AbstractParallelNodeModel that uses a sub
 * pool of the global KNIME thread pool.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class ThreadedTableBuilderNodeModel extends NodeModel {
    private class Submitter implements Runnable {
        private final BufferedDataTable[] m_data;

        private final BufferedDataTable[] m_additionalData;

        private final ExecutionContext m_exec;

        private final List<Future<DataContainer[]>> m_futures;

        private final DataTableSpec[] m_specs;

        final AtomicInteger m_processedRows = new AtomicInteger();

        Submitter(final BufferedDataTable[] inData,
                final List<Future<DataContainer[]>> futures,
                final DataTableSpec[] outSpecs, final ExecutionContext exec) {
            m_data = inData;
            m_exec = exec;
            m_futures = futures;
            m_specs = outSpecs;
            m_additionalData = new BufferedDataTable[inData.length - 1];
            for (int i = 1; i < inData.length; i++) {
                m_additionalData[i - 1] = inData[i];
            }
        }

        public void run() {
            final double max = m_data[0].getRowCount();
            final int chunkSize =
                    (int)Math.ceil(max / (4.0 * m_workers.getMaxThreads()));
            final RowIterator it = m_data[0].iterator();
            BufferedDataContainer container = null;
            int count = 0, chunks = 0;
            while (true) {
                try {
                    m_exec.checkCanceled();
                } catch (CanceledExecutionException ex) {
                    return;
                }

                if ((count++ % chunkSize == 0) || !it.hasNext()) {
                    if (container != null) {
                        container.close();
                        try {
                            chunks++;
                            m_futures.add(m_workers.submit(createCallable(
                                    container.getTable(), chunkSize, max)));
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

        private Callable<DataContainer[]> createCallable(
                final BufferedDataTable data, final int chunkSize,
                final double max) {
            return new Callable<DataContainer[]>() {
                public DataContainer[] call() throws Exception {
                    DataContainer[] result = new DataContainer[m_specs.length];
                    for (int i = 0; i < result.length; i++) {
                        result[i] = m_exec.createDataContainer(m_specs[i]);
                    }

                    for (DataRow r : data) {
                        m_exec.checkCanceled();
                        processRow(r, m_additionalData, result);

                        int pr = m_processedRows.incrementAndGet();
                        if (pr % 10 == 0) {
                            // 5% of the progress are reserved for combining
                            // the partial results lateron
                            m_exec.setProgress(0.95 * pr / max);
                        }
                    }

                    for (int i = 0; i < result.length; i++) {
                        result[i].close();
                    }

                    return result;
                }
            };
        }
    }

    /**
     * Sets the maximum number of threads that may be used by this node.
     * 
     * @param count the maximum thread count
     */
    public void setMaxThreads(final int count) {
        m_workers.setMaxThreads(count);
    }

    /** The execution service that is used. */
    private final ThreadPool m_workers;

    /**
     * Creates a new AbstractParallelNodeModel.
     * 
     * @param nrDataIns the number of {@link DataTable}s expected as inputs
     * @param nrDataOuts the number of {@link DataTable}s expected at the
     *            output
     * @param nrPredParamsIns the number of
     *            {@link org.knime.core.node.ModelContent} elements available as
     *            inputs
     * @param nrPredParamsOuts the number of
     *            {@link org.knime.core.node.ModelContent} objects available at
     *            the output
     * @param workers a thread pool where threads for processing the chunks are
     *            taken from
     */
    public ThreadedTableBuilderNodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts,
            final ThreadPool workers) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, nrPredParamsOuts);
        m_workers = workers;
    }

    /**
     * Creates a new AbstractParallelNodeModel.
     * 
     * @param nrDataIns The number of {@link DataTable} elements expected as
     *            inputs.
     * @param nrDataOuts The number of {@link DataTable} objects expected at the
     *            output.
     * @param workers a thread pool where threads for processing the chunks are
     *            taken from
     */
    public ThreadedTableBuilderNodeModel(final int nrDataIns, final int nrDataOuts,
            final ThreadPool workers) {
        super(nrDataIns, nrDataOuts);
        m_workers = workers;
    }

    /**
     * This method is called before the first chunked is processed. The method
     * must return the data table specification(s) for the result table(s)
     * because the {@link RowAppender} passed to
     * {@link #processRow(DataRow, BufferedDataTable[], RowAppender[])} must be
     * constructed accordingly.
     * 
     * @param data the input data tables
     * @return the table spec(s) of the result table(s) in the right order. The
     *         result and none of the table specs must be null!
     * @throws Exception if something goes wrong during preparation
     */
    protected abstract DataTableSpec[] prepareExecute(final DataTable[] data)
            throws Exception;

    /**
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected final BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {
        final DataTableSpec[] outSpecs = prepareExecute(data);

        // do some consistency checks to bail out as early as possible
        if (outSpecs == null) {
            throw new NullPointerException("Implementation Error: The "
                    + "array of generated output table specs can't be null.");
        }
        if (outSpecs.length != getNrDataOuts()) {
            throw new IllegalStateException("Implementation Error: Number of"
                    + " provided DataTableSpecs doesn't match number of output"
                    + " ports");
        }
        for (DataTableSpec outSpec : outSpecs) {
            if (outSpec == null) {
                throw new IllegalStateException("Implementation Error: The"
                        + " generated output DataTableSpec is null.");
            }
        }

        final List<Future<DataContainer[]>> futures =
                new ArrayList<Future<DataContainer[]>>();
        final BufferedDataTable[] additionalTables =
                new BufferedDataTable[Math.max(0, data.length - 1)];
        System.arraycopy(data, 1, additionalTables, 0, additionalTables.length);

        final Runnable submitter = new Submitter(data, futures, outSpecs, exec);

        try {
            m_workers.runInvisible(submitter);
        } catch (IllegalThreadStateException ex) {
            // this node has not been started by a thread from a thread pool.
            // This is odd, but may happen
            submitter.run();
        }

        final DataTable[][] tempTables =
                new DataTable[outSpecs.length][futures.size()];
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
                        + "failed, reason: data is null or number "
                        + "of outputs wrong.");
            }

            for (int i = 0; i < temp.length; i++) {
                tempTables[i][k] = temp[i].getTable();
            }
            k++;
        }

        final AppendedRowsTable[] resultTables =
                new AppendedRowsTable[outSpecs.length];
        for (int i = 0; i < resultTables.length; i++) {
            resultTables[i] = new AppendedRowsTable(tempTables[i]);
        }

        return exec.createBufferedDataTables(resultTables, exec);
    }

    /**
     * This method is called as often as necessary by multiple threads. The
     * <code>inData</code>-table will contain at most
     * <code>maxChunkSize</code> rows from the the first table in the array
     * passed to {@link #execute(BufferedDataTable[], ExecutionContext)}, the
     * <code>additionalData</code>-tables are passed completely.
     * 
     * @param inRow an input row
     * @param additionalData the complete tables of additional data
     * @param outputTables data containers for the output tables where the
     *            computed rows must be added
     * @throws Exception if an exception occurs
     */
    protected abstract void processRow(final DataRow inRow,
            final BufferedDataTable[] additionalData,
            final RowAppender[] outputTables) throws Exception;
}
