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

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.RowAppender;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.util.ThreadPool;

/**
 * This model is an extension of the {@link NodeModel} that allows you to easily
 * process data in parallel. In contrast to the
 * {@link org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel},
 * this model is suitable for creating completely new output tables.
 *
 * All you have to do is create the output table specs in the
 * {@link #prepareExecute(DataTable[])} method and then implement the
 * {@link #processRow(DataRow, BufferedDataTable[], RowAppender[])} method
 * to produce one or more (or even no) output row(s) for each input row.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class ThreadedTableBuilderNodeModel extends NodeModel {
    private class Submitter implements Callable<Void> {
        private final BufferedDataTable[] m_additionalData;

        private final BufferedDataTable[] m_data;

        private final ExecutionContext m_exec;

        private final List<Future<BufferedDataContainer[]>> m_futures;

        final AtomicInteger m_processedRows = new AtomicInteger();

        private final DataTableSpec[] m_specs;

        Submitter(final BufferedDataTable[] inData,
                final List<Future<BufferedDataContainer[]>> futures,
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

        private Callable<BufferedDataContainer[]> createCallable(
                final BufferedDataTable data, final int chunkSize,
                final double max) {
            return new Callable<BufferedDataContainer[]>() {
                public BufferedDataContainer[] call() throws Exception {
                    BufferedDataContainer[] result =
                        new BufferedDataContainer[m_specs.length];
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

        /**
         * 
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

                    container = m_exec.createDataContainer(m_data[0]
                                    .getDataTableSpec());
                }
                container.addRowToTable(it.next());
            }
            return null;
        }
    }

    /** The execution service that is used. */
    private final ThreadPool m_workers;

    /**
     * Creates a new AbstractParallelNodeModel. The model
     *
     * @param nrDataIns The number of {@link DataTable} elements expected as
     *            inputs.
     * @param nrDataOuts The number of {@link DataTable} objects expected at the
     *            output.
     */
    public ThreadedTableBuilderNodeModel(final int nrDataIns,
            final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
        ThreadPool pool = ThreadPool.currentPool();
        if (pool != null) {
            m_workers = pool;
        } else {
            m_workers = KNIMEConstants.GLOBAL_THREAD_POOL;
        }
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
    public ThreadedTableBuilderNodeModel(final int nrDataIns,
            final int nrDataOuts, final ThreadPool workers) {
        super(nrDataIns, nrDataOuts);
        m_workers = workers;
    }

    /**
     * {@inheritDoc}
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
        if (outSpecs.length != getNrOutPorts()) {
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

        final List<Future<BufferedDataContainer[]>> futures =
                new ArrayList<Future<BufferedDataContainer[]>>();
        final BufferedDataTable[] additionalTables =
                new BufferedDataTable[Math.max(0, data.length - 1)];
        System.arraycopy(data, 1, additionalTables, 0, additionalTables.length);

        final Callable<?> submitter = new Submitter(data, futures, outSpecs, exec);

        try {
            m_workers.runInvisible(submitter);
        } catch (IllegalThreadStateException ex) {
            // this node has not been started by a thread from a thread pool.
            // This is odd, but may happen
            submitter.call();
        }

        final BufferedDataTable[][] tempTables =
                new BufferedDataTable[outSpecs.length][futures.size()];
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
                        + "failed, reason: data is null or number "
                        + "of outputs wrong.");
            }

            for (int i = 0; i < temp.length; i++) {
                tempTables[i][k] = temp[i].getTable();
            }
            k++;
        }

        final BufferedDataTable[] resultTables =
                new BufferedDataTable[outSpecs.length];
        for (int i = 0; i < resultTables.length; i++) {
            resultTables[i] = exec.createConcatenateTable(exec, tempTables[i]);
        }
        return resultTables;
    }

    /**
     * This method is called before the first row is processed. The method
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
     * This method is called once for each row in the first input table. The
     * remaining tables are passed completely in the second argument. The
     * output rows must then be written into the row appender(s). There is a
     * row appender for each output table.
     *
     * Please note that this method is called by many threads at the same time,
     * so do NOT synchronize it and make sure that write access to global
     * data does not mess up things.
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
