/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.node.preproc.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.UniqueNameGenerator;

/**
 * This is the model implementation.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
final class BootstrapNodeModel extends NodeModel {

    private static final int MAX_CHUNK_SIZE = 10000000;

    private BootstrapConfiguration m_configuration;

    /**
     * Constructor for the node model.
     */
    protected BootstrapNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        // Init random object
        long seed = m_configuration.getUseSeed() ? m_configuration.getSeed() : System.currentTimeMillis();
        Random random = new Random(seed);
        // Create containers for output tables
        BufferedDataContainer bootstrap = exec.createDataContainer(getSamplesSpec(inData[0].getDataTableSpec()));
        BufferedDataContainer holdout = exec.createDataContainer(inData[0].getDataTableSpec());
        // Create iterator for all rows
        CloseableRowIterator iterator = inData[0].iterator();
        int numberOfRows = inData[0].getRowCount();
        // Init unprocessed rows with amount of all rows
        int unprocessedRows = numberOfRows;
        // Create progress object with amount of all rows
        Progress progress = new Progress(numberOfRows, exec);
        // Calculate number of samples
        int numberOfSamples;
        if (m_configuration.getInPercent()) {
            numberOfSamples = Math.round(numberOfRows * (m_configuration.getPercent() / 100));
        } else {
            numberOfSamples = m_configuration.getSize();
        }
        // Execute while until every row has been processed
        while (unprocessedRows > 0) {
            int chunkSize;
            int numberOfChunkSamples;
            // Only enter if branch if higher than max chunk size,
            // this ensures that the else branch is always executed at the end
            // and will take care of fixing rounding issues
            if (unprocessedRows > MAX_CHUNK_SIZE) {
                // Set to biggest allowed size
                chunkSize = MAX_CHUNK_SIZE;
                // Calculate amount of samples relative to the size of this chunk
                numberOfChunkSamples = Math.round((chunkSize / (float)numberOfRows) * numberOfSamples);
            } else {
                // Make this chunk as big as there are rows left
                chunkSize = unprocessedRows;
                // Generate the rest of the samples
                // (this will take care of rounding errors that may occur in the relative calculation)
                // we never put more than 2^31 rows in the bootstrap container, therefore it's safe to cast to int
                numberOfChunkSamples = numberOfSamples - (int) bootstrap.size();
            }
            // Sample this chunk
            sampleChunk(iterator, chunkSize, numberOfChunkSamples, bootstrap, holdout, random, progress);
            // Mark chunked rows as processed
            unprocessedRows -= chunkSize;
        }
        iterator.close();
        bootstrap.close();
        holdout.close();
        return new BufferedDataTable[]{bootstrap.getTable(), holdout.getTable()};
    }

    /**
     * Puts samples, for the chunk of rows, in the bootstrap container and unused ones in the holdout container.
     *
     * @param iterator Iterator over a set of rows. Must at least have chunkSize rows left
     * @param chunkSize The number of rows that will be processed
     * @param numberOfSamples The number of samples that should be generated
     * @param bootstrap The container for the bootstrap samples
     * @param holdout The container for the unused samples
     * @param random A random for selection of rows
     * @param progress A progress object that will be incremented for each processed row
     * @throws CanceledExecutionException If execution has been canceled
     */
    private void sampleChunk(final CloseableRowIterator iterator, final int chunkSize, final int numberOfSamples,
        final BufferedDataContainer bootstrap, final BufferedDataContainer holdout, final Random random,
        final Progress progress) throws CanceledExecutionException {
        // Array holding the amount of copies that will go into the bootstrap samples for each row
        int[] sampled = new int[chunkSize];
        // Init all with 0
        for (int i = 0; i < sampled.length; i++) {
            sampled[i] = 0;
        }
        // Increment randomly until amount of samples has been selected
        for (int i = 0; i < numberOfSamples; i++) {
            sampled[random.nextInt(sampled.length)]++;
        }
        // Iterate through as many rows as this chunk is big
        for (int i = 0; i < chunkSize; i++) {
            DataRow row = iterator.next();
            // Check if row will go into the bootstrap or holdout table
            if (sampled[i] == 0) {
                // Add the unchanged row to the holdout table
                holdout.addRowToTable(row);
            } else {
                // Add the row to the bootstrap table as many times as it has been selected
                for (int j = 0; j < sampled[i]; j++) {
                    DataRow newRow;
                    if (m_configuration.getAppendOccurrences() || m_configuration.getAppendOriginalRowId()) {
                        int appendedColumns = 0;
                        if (m_configuration.getAppendOccurrences()) {
                            appendedColumns++;
                        }
                        if (m_configuration.getAppendOriginalRowId()) {
                            appendedColumns++;
                        }
                        // Add occurrences column to the row
                        DataCell[] cells = new DataCell[row.getNumCells() + appendedColumns];
                        int k;
                        for (k = 0; k < row.getNumCells(); k++) {
                            cells[k] = row.getCell(k);
                        }
                        if (m_configuration.getAppendOccurrences()) {
                            cells[k++] = new IntCell(sampled[i]);
                        }
                        if (m_configuration.getAppendOriginalRowId()) {
                            cells[k++] = new StringCell(row.getKey().toString());
                        }
                        newRow = new DefaultRow(row.getKey() + m_configuration.getRowIdSeparator() + j, cells);
                    } else {
                        // Only change the row ID of the original row
                        newRow = new DefaultRow(row.getKey() + m_configuration.getRowIdSeparator() + j, row);
                    }
                    // Add row to the bootstrap table
                    bootstrap.addRowToTable(newRow);
                }
            }
            // Update progress
            progress.increment();
        }
    }

    /**
     * Generates the specs for the bootstrap samples table.
     *
     * @param inSpec Specs of the input table
     * @return Specs of the bootstrap samples table
     */
    private DataTableSpec getSamplesSpec(final DataTableSpec inSpec) {
        DataTableSpec outSpec;
        if (m_configuration != null
            && (m_configuration.getAppendOccurrences() || m_configuration.getAppendOriginalRowId())) {
            UniqueNameGenerator generator = new UniqueNameGenerator(inSpec);
            int appendedColumns = 0;
            if (m_configuration.getAppendOccurrences()) {
                appendedColumns++;
            }
            if (m_configuration.getAppendOriginalRowId()) {
                appendedColumns++;
            }
            // Output spec is spec from input table + count of occurrences column
            DataColumnSpec[] columnSpecs = new DataColumnSpec[inSpec.getNumColumns() + appendedColumns];
            int i;
            for (i = 0; i < inSpec.getNumColumns(); i++) {
                columnSpecs[i] = inSpec.getColumnSpec(i);
            }
            if (m_configuration.getAppendOccurrences()) {
                columnSpecs[i++] = generator.newColumn("Count of occurrences", IntCell.TYPE);
            }
            if (m_configuration.getAppendOriginalRowId()) {
                columnSpecs[i++] = generator.newColumn("Original RowID", StringCell.TYPE);
            }
            outSpec = new DataTableSpec(columnSpecs);
        } else {
            // Output spec is the same as spec from input table
            outSpec = inSpec;
        }
        return outSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_configuration == null) {
            // load default config
            m_configuration = new BootstrapConfiguration();
            m_configuration.load(new NodeSettings("empty"));
        }
        return new DataTableSpec[]{getSamplesSpec(inSpecs[0]), inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        BootstrapConfiguration config = new BootstrapConfiguration();
        config.loadAndValidate(settings);
        m_configuration = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new BootstrapConfiguration().loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Not used
    }

    /**
     * Class that keeps track of the current progress and updates the progress of the node.
     *
     * @author Patrick Winter
     */
    private static class Progress {

        private ExecutionContext m_exec;

        private long m_all;

        private long m_current;

        /**
         * Creates a new progress object for the given amount of increments.
         *
         * @param all How many increments until completed
         * @param exec ExecutionContext to update the progress of the nodes execution
         */
        public Progress(final long all, final ExecutionContext exec) {
            m_exec = exec;
            m_all = all;
            m_current = 0;
        }

        /**
         * Increment the progress by one and update the progress of the node.
         *
         * @throws CanceledExecutionException If execution has been canceled.
         */
        public void increment() throws CanceledExecutionException {
            m_exec.checkCanceled();
            m_current++;
            m_exec.setProgress((m_current) / (double)m_all);
        }

    }

}
