/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.sample;

import java.util.BitSet;
import java.util.Random;

import org.knime.base.node.preproc.filter.row.RowFilterTable;
import org.knime.base.node.preproc.filter.row.rowfilter.FalseRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowNoRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.TrueRowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * Utility class that allows to create row filters for sampling.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class Sampler {

    private Sampler() {
    }

    /**
     * Convenience method that creates a new {@link DataTable} that samples rows
     * according to a given row filter.
     * 
     * @param table the table to wrap, i.e. to sample from
     * @param filter the filter to use
     * @return a new {@link RowFilterTable}
     * @see RowFilterTable#RowFilterTable(DataTable, RowFilter)
     */
    public static final DataTable createSamplingTable(final DataTable table,
            final RowFilter filter) {
        return new RowFilterTable(table, filter);
    }

    /**
     * Creates a filter that to filter the first <code>100 * fraction</code>
     * rows from a table. The row counter is determined based on the row number
     * of <code>table</code>.
     * 
     * @param table the table from which to get the final row count
     * @param fraction the fraction of the row count that shall survive
     * @param exec an execution monitor to check for cancelation
     * @return a row filter for this purpose
     * @throws CanceledExecutionException if exec cancels the row counting
     */
    public static final RowFilter createRangeFilter(final DataTable table,
            final double fraction, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        int rowCount = countRows(table, exec);
        int count = (int)(Math.round(fraction * rowCount));
        if (count == 0) {
            return new FalseRowFilter();
        } else {
            return new RowNoRowFilter(0, count - 1, true);
        }
    }

    /**
     * Creates a filter that passes only the first <code>count</code> rows.
     * 
     * @param count the number of rows that survive (starting from top)
     * @return a filter that only filter the first <code>count</code> rows
     */
    public static final RowFilter createRangeFilter(final int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be > 0: " + count);
        }
        return new RowNoRowFilter(0, count - 1, true);
    }

    /**
     * Creates row filter that randomly samples about
     * <code>100 * fraction</code> percent from a table.
     * 
     * @param fraction the fraction being used, must be in [0, 1]
     * @return such a filter
     * @see RandomFractionRowFilter#RandomFractionRowFilter(double)
     */
    public static final RowFilter createSampleFilter(final double fraction) {
        return new RandomFractionRowFilter(fraction);
    }

    /**
     * Creates row filter that samples precisely a given fraction of rows. This
     * requires a scan on the table in order to count the rows and determine the
     * right number of sampled rows.
     * 
     * @param table to count rows on
     * @param fraction the fraction to be sampled, must be in [0, 1]
     * @param exec to check canceled status on and report progress
     * @return such a filter
     * @throws CanceledExecutionException if canceled
     */
    public static final RowFilter createSampleFilter(final DataTable table,
            final double fraction, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        return createSampleFilter(table, fraction, null, exec);
    }

    /**
     * Creates row filter that samples precisely a given fraction of rows. This
     * requires a scan on the table in order to count the rows and determine the
     * right number of sampled rows. A given Random object makes the sampling
     * &quot;deterministic&quot;.
     * 
     * @param table to count rows on
     * @param fraction the fraction to be sampled, must be in [0, 1]
     * @param rand the random object for controlled sampling. (If
     *            <code>null</code>, uses default)
     * @param exec to check canceled status on and report progress.
     * @return such a filter
     * @throws CanceledExecutionException if canceled
     */
    public static final RowFilter createSampleFilter(final DataTable table,
            final double fraction, final Random rand,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        int rowCount = countRows(table, exec);
        int count = (int)(Math.round(fraction * rowCount));
        return createRandomNumberRowFilter(count, rowCount, rand);
    }

    /**
     * Creates row filter that samples arbitrary <code>count</code> rows from
     * <code>table</code>.
     * 
     * @param table the table from which to create the sample
     * @param count the number of rows the should go "through" the filter
     * @param exec an execution monitor to check for cancelation (this method
     *            requires an iteration over table - which might take long)
     * @return a row ilter to be used for this kind of sampling.
     * @throws CanceledExecutionException if exec was canceled
     */
    public static final RowFilter createSampleFilter(final DataTable table,
            final int count, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        return createSampleFilter(table, count, null, exec);
    }

    /**
     * Creates row filter that samples arbitrary <code>count</code> rows from
     * <code>table</code>. A given Random object makes the sampling
     * &quot;deterministic&quot;.
     * 
     * @param table the table from which to create the sample
     * @param count the number of rows the should go "through" the filter
     * @param rand the random object for controlled sampling. (If
     *            <code>null</code>, uses default)
     * @param exec an execution monitor to check for cancelation (this method
     *            requires an iteration over table - which might take long)
     * @return a row filter to be used for this kind of sampling
     * @throws CanceledExecutionException if exec was canceled
     */
    public static final RowFilter createSampleFilter(final DataTable table,
            final int count, final Random rand, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        final int rowCount = countRows(table, exec);
        return createRandomNumberRowFilter(count, rowCount, rand);
    }

    /*
     * Counts rows in table.
     * 
     * If the table is of type
     * {@link org.knime.core.node.BufferedDataTable} the row count is
     * retreived directly.
     */
    private static final int countRows(final DataTable table,
            final ExecutionMonitor exec) throws CanceledExecutionException {

        // if buffered table
        if (table instanceof BufferedDataTable) {
            return ((BufferedDataTable)table).getRowCount();
        }

        // determine row count
        int rowCount = 0;
        for (RowIterator it = table.iterator(); it.hasNext(); rowCount++) {
            DataRow row = it.next();
            if (exec != null) {
                exec.setMessage("Counting Rows... " + rowCount + " (\""
                    + row.getKey() + "\")");
                exec.checkCanceled();
            }
        }
        return rowCount;
    }

    /*
     * Creates random number row filter that samples count rows from a table
     * with overall allCount rows.
     */
    private static final RowFilter createRandomNumberRowFilter(final int count,
            final int allCount, final Random rand) {
        Random random = rand != null ? rand : new Random();
        if (allCount <= count) {
            return new TrueRowFilter();
        }
        BitSet bitset = new BitSet(allCount);
        // hm, I'm sure there is a better way to draw arbitrary bits
        int[] vals = new int[allCount];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = i;
        }
        for (int i = vals.length; --i >= 0;) {
            int swapIndex = random.nextInt(i + 1);
            int swap = vals[swapIndex];
            vals[swapIndex] = vals[i];
            vals[i] = swap;
        }
        for (int i = 0; i < count; i++) {
            bitset.set(vals[i]);
        }
        return new RandomNumberRowFilter(bitset);
    }
}
