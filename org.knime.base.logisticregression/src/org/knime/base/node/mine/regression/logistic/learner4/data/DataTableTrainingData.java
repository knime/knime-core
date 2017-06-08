/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   23.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T>
 */
public class DataTableTrainingData <T extends TrainingRow> extends AbstractTrainingData<T> {

    private static final boolean[] SORT_ASCENDING = new boolean[]{true};

    private BufferedDataTable m_data;
    private final List<T> m_cachedRows;
    private final int m_cacheSize;
    private Iterator<DataRow> m_rowIterator;
    private int m_sampleCounter;
    private final int m_idColIdx;
    private final int m_shuffleColIdx;
    private final DataColumnSpec m_shuffleColSpec;
    private final ExecutionContext m_exec;

    /**
     * @param data
     * @param seed
     * @param rowBuilder
     * @param cacheSize
     * @throws CanceledExecutionException
     *
     */
    public DataTableTrainingData(final BufferedDataTable data, final Long seed, final TrainingRowBuilder<T> rowBuilder,
        final int cacheSize, final ExecutionContext exec) throws CanceledExecutionException {
        super(data, seed, rowBuilder);
        DataTableSpec tableSpec = data.getDataTableSpec();
        ColumnRearranger colre = new ColumnRearranger(tableSpec);
        String idColName = DataTableSpec.getUniqueColumnName(tableSpec, "id");
        String shuffleColName = DataTableSpec.getUniqueColumnName(tableSpec, "shuffle");
        DataColumnSpec idSpec = new DataColumnSpecCreator(idColName, IntCell.TYPE).createSpec();
        m_shuffleColSpec = new DataColumnSpecCreator(shuffleColName, IntCell.TYPE).createSpec();
        colre.append(new IdAppendFactory(idSpec));
        // use table's current ordering for first epoch
        colre.append(new IdAppendFactory(m_shuffleColSpec));
        m_data = exec.createColumnRearrangeTable(data, colre, exec.createSubProgress(0.0));
        DataTableSpec newTableSpec = m_data.getDataTableSpec();
        m_idColIdx = newTableSpec.findColumnIndex(idColName);
        m_shuffleColIdx = newTableSpec.findColumnIndex(shuffleColName);
        m_cacheSize = cacheSize;
        m_cachedRows = new ArrayList<>(m_cacheSize);
        m_rowIterator = m_data.iterator();
        m_sampleCounter = 0;
        m_exec = exec;
    }


    /**
     * {@inheritDoc}
     * @throws CanceledExecutionException
     */
    @Override
    public T getRandomRow() throws CanceledExecutionException {
        if (m_sampleCounter >= m_cacheSize || m_cachedRows.isEmpty()) {
            refillCache();
            m_sampleCounter = 0;
        }
        m_sampleCounter++;
        return drawFromCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return new DataTableIterator<>(m_data.iterator(), getRowBuilder());
    }

    private T drawFromCache() {
        int randomIdx = getRandomDataGenerator().nextInt(m_cacheSize);
        return m_cachedRows.get(randomIdx);
    }

    private void refillCache() throws CanceledExecutionException {
        m_cachedRows.clear();
        for (int i = 0; i < m_cacheSize; i++) {
            addRowToCache();
        }
    }

    private void addRowToCache() throws CanceledExecutionException {
        if (m_rowIterator.hasNext()) {
            DataRow tableRow = m_rowIterator.next();
            T row = getRowBuilder().build(tableRow, getId(tableRow));
            m_cachedRows.add(row);
        } else {
            // shuffle data
            shuffle();
            // start again from the beginning
            m_rowIterator = m_data.iterator();
            addRowToCache();
        }
    }

    private int getId(final DataRow row) {
        return ((IntCell)row.getCell(m_idColIdx)).getIntValue();
    }

    private void shuffle() throws CanceledExecutionException {
        int nrRows = (int)m_data.size();
        Random random = getRandomDataGenerator();
        // create shuffle column
        ColumnRearranger colre = new ColumnRearranger(m_data.getDataTableSpec());
        colre.replace(new RandomNumberAppendFactory(random.nextLong(), nrRows, m_shuffleColSpec), m_shuffleColIdx);
        m_data = m_exec.createColumnRearrangeTable(m_data, colre, m_exec.createSubProgress(0.0));
        // sort by shuffle column
        BufferedDataTableSorter sorter = new BufferedDataTableSorter(m_data, Collections.singleton(m_shuffleColSpec.getName()), SORT_ASCENDING);
        m_data = sorter.sort(m_exec.createSubExecutionContext(0.0));
    }

    private static class DataTableIterator <T extends TrainingRow> implements Iterator<T> {

        private TrainingRowBuilder<T> m_rowBuilder;
        private Iterator<DataRow> m_rowIterator;
        private int m_idCounter = 0;

        /**
         * @param rowIterator
         * @param rowBuilder
         *
         */
        public DataTableIterator(final Iterator<DataRow> rowIterator, final TrainingRowBuilder<T> rowBuilder) {
            m_rowBuilder = rowBuilder;
            m_rowIterator = rowIterator;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_rowIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T next() {
            return m_rowBuilder.build(m_rowIterator.next(), m_idCounter++);
        }
    }

    private static class IdAppendFactory extends SingleCellFactory {

        private int m_idCounter = 0;

        /**
         * @param newColSpec
         */
        public IdAppendFactory(final DataColumnSpec newColSpec) {
            super(newColSpec);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            return new IntCell(m_idCounter++);
        }

    }

    private static final class RandomNumberAppendFactory extends SingleCellFactory {

        /** Shuffled row number array. */
        private int[] m_shuffle;

        /** Position in array. */
        private int m_pos = 0;

        /** Constructor. */
        private RandomNumberAppendFactory(final Long seed,
            final int rowCount, final DataColumnSpec appendSpec) {
            super(appendSpec);
            Random random;
            if (seed != null) {
                random = new Random(seed.longValue());
            } else {
                random = new Random();
            }
            int nrRows = rowCount;

            // initialize
            m_shuffle = new int[nrRows];
            for (int i = 0; i < nrRows; i++) {
                m_shuffle[i] = i;
            }

            // let's shuffle
            for (int i = 0; i < m_shuffle.length; i++) {
                int r = random.nextInt(i + 1);
                int swap = m_shuffle[r];
                m_shuffle[r] = m_shuffle[i];
                m_shuffle[i] = swap;
            }
        }

        /** {@inheritDoc} */
        @Override
        public DataCell getCell(final DataRow row) {
            assert (m_pos <= m_shuffle.length);
            DataCell nextRandomNumberCell = new IntCell(m_shuffle[m_pos]);
            m_pos++;
            return nextRandomNumberCell;
        }
    }

}
