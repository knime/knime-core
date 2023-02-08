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
 *   19.03.2018 (Adrian Nembach): created
 */
package org.knime.core.data.sort;

import java.io.Closeable;
import java.util.ArrayList;
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
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;

/**
 * Implementation of the Fisher Yates shuffle, that guarantees that all n! possible outcomes are possible and equally
 * likely. The shuffling procedure requires only linear runtime. For further details see "Fisher-Yates shuffle", from
 * Dictionary of Algorithms and Data Structures, Paul E. Black, ed., NIST.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class ClosableShuffler implements Closeable {

    private final BufferedDataTable m_table;

    private final ExecutionContext m_exec;

    private final long m_seed;

    private BufferedDataTable m_intermediate;

    private BufferedDataTable m_sorted;

    private BufferedDataTable m_shuffled;

    /**
     * Create a {@link ClosableShuffler} to shuffle the input table with the given seed (for reproducibility). Call
     * {@link #getShuffled()} to execute the shuffling and get the shuffled table and call {@link #close()} to clear the
     * shuffled table and all intermediate tables (deleting temporary files).
     *
     * @param table the table to shuffle
     * @param exec execution context use for creating tables and reporting progress
     * @param seed random seed for permutation generation
     *
     * @since 4.3
     */
    public ClosableShuffler(final BufferedDataTable table, final ExecutionContext exec, final long seed) {
        CheckUtils.checkArgument(table.size() <= Integer.MAX_VALUE,
            "It's currently not possible to shuffle tables with more than Integer.MAX_VALUE rows.");

        m_table = table;
        m_exec = exec;
        m_seed = seed;
    }

    /**
     * Run the shuffling (only on the first call) and get the shuffled table.
     *
     * @return the shuffled table
     * @throws CanceledExecutionException if the execution was canceled
     * @since 4.3
     */
    public BufferedDataTable getShuffled() throws CanceledExecutionException {
        if (m_shuffled == null) {
            shuffle();
        }
        return m_shuffled;
    }

    @Override
    public void close() {
        m_exec.clearTable(m_sorted);
        m_exec.clearTable(m_shuffled);
        m_exec.clearTable(m_intermediate);
    }

    private void shuffle() throws CanceledExecutionException {
        try {
            final RandomNumberAppendFactory randomnumfac = RandomNumberAppendFactory.create(m_seed, m_table);
            ColumnRearranger colre = new ColumnRearranger(m_table.getDataTableSpec());
            colre.append(randomnumfac);
            m_intermediate = m_exec.createColumnRearrangeTable(m_table, colre, m_exec.createSubProgress(.2));
            List<String> include = new ArrayList<>();
            String randomcol = randomnumfac.getColumnSpecs()[0].getName();
            include.add(randomcol);
            BufferedDataTableSorter sorter = new BufferedDataTableSorter(m_intermediate, include, new boolean[]{true});
            m_sorted = sorter.sort(m_exec.createSubExecutionContext(0.75));
            colre = new ColumnRearranger(m_sorted.getDataTableSpec());
            colre.remove(randomcol);
            m_shuffled = m_exec.createColumnRearrangeTable(m_sorted, colre, m_exec.createSubProgress(.05));
        } catch (final CanceledExecutionException e) {
            m_exec.clearTable(m_sorted);
            throw e;
        }
    }

    /*
     * The CellFactory adds a shuffled number to each input DataRow.
     */
    private static final class RandomNumberAppendFactory extends SingleCellFactory {

        /** Shuffled row number array. */
        private int[] m_shuffle;

        /** Constructor. */
        private RandomNumberAppendFactory(final Long seed, final int rowCount, final DataColumnSpec appendSpec) {
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
        public DataCell getCell(final DataRow row, final long rowIndex) {
            assert (rowIndex <= m_shuffle.length);
            DataCell nextRandomNumberCell = new IntCell(m_shuffle[(int)rowIndex]);
            return nextRandomNumberCell;
        }

        /** Factory method to create a new random number append factory. */
        private static RandomNumberAppendFactory create(final Long seed, final BufferedDataTable inData) {
            final DataTableSpec spec = inData.getDataTableSpec();
            final long tableSize = inData.size();
            CheckUtils.checkArgument(tableSize < Integer.MAX_VALUE,
                "It's currently not possible to shuffle tables with more than Integer.MAX_VALUE rows.");
            final int rowCount = (int)tableSize;
            String appendName = "random_row_number";
            int uniquifier = 1;
            while (spec.containsName(appendName)) {
                appendName = "random_row_number_#" + uniquifier++;
            }
            DataColumnSpec s = new DataColumnSpecCreator(appendName, IntCell.TYPE).createSpec();
            return new RandomNumberAppendFactory(seed, rowCount, s);
        }

    }
}
