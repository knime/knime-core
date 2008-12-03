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
 *   15.04.2005 (cebron): created
 */
package org.knime.base.data.statistics;

import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * A wrapper table that is able to compute statistics for each row The following
 * moments are available:
 * <ul>
 * <li>Mean</li>
 * <li>Standard deviation</li>
 * <li>Variance</li>
 * <li>Minimum</li>
 * <li>Maximum</li>
 * </ul>
 * <b>Important: </b> If you need all statistical values from a
 * {@link org.knime.core.data.DataTable} consider calling the
 * {@link #calculateAllMoments(ExecutionMonitor)}-method first for a faster
 * processing speed.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class StatisticsTable implements DataTable {
    
    /** Table to be wrapped. */
    private final DataTable m_table;

    /** Used to 'cache' the mean values. */
    private final double[] m_meanValues;

    /** Used to 'cache' the variance values. */
    private final double[] m_varianceValues;
    
    /** Used to cache the sum of each column. */
    private final double[] m_sum;
    
    /** Used to cache the number of missing values per columns. */
    private final int[] m_missingValueCnt;
    
    /** Used to 'cache' the minimum values. */
    private final DataCell[] m_minValues;

    /** Used to 'cache' the maximum values. */
    private final DataCell[] m_maxValues;

    /** Number of rows of the DataTable. */
    private int m_nrRows;

    /** A table spec created with ranges added to all numerical columns. */
    private DataTableSpec m_tSpec;
    
    /** To be used in derived classes that do additional calculations. Please
     * do call calculateAllMoments when done!
     * @param table To wrap.
     */
    protected StatisticsTable(final DataTable table) {
        m_table = table;
        int nrCols = m_table.getDataTableSpec().getNumColumns();
        // initialize cache arrays
        m_meanValues = new double[nrCols];
        m_varianceValues = new double[nrCols];
        m_sum = new double[nrCols];
        m_minValues = new DataCell[nrCols];
        m_maxValues = new DataCell[nrCols];
        m_missingValueCnt = new int[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_missingValueCnt[i] = 0;
            m_meanValues[i] = Double.NaN;
            m_sum[i] = Double.NaN;
            m_varianceValues[i] = Double.NaN;
            m_minValues[i] = null;
            m_maxValues[i] = null;
        }
    }

    /**
     * Create new wrapper table from an existing one. This constructor
     * calculates all values. It needs to traverse (twice) through the entire
     * specified table. User can cancel action if an execution monitor is
     * passed.
     * 
     * @param table table to be wrapped
     * @param exec an object to check with if user canceled operation
     * @throws CanceledExecutionException if user canceled
     * @see DataTable#getDataTableSpec()
     */
    public StatisticsTable(final DataTable table, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        this(table);
        calculateAllMoments(exec);
    }

    /**
     * Produces a DataTableSpec for the statistics table which contains the
     * range values calculated here.
     * 
     * @return a table spec with ranges set in column. If the spec of the
     *         underlying table had ranges set nothing will change.
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        if (m_tSpec == null) {
            throw new IllegalStateException(
                    "Table spec should have been determined in constructor.");
        }
        return m_tSpec;
    }

    /**
     * Returns the row iterator of the original data table.
     * {@inheritDoc} 
     */
    @Override
    public RowIterator iterator() {
        return m_table.iterator();
    }

    /**
     * Computes the number of rows of the data table.
     * 
     * @return number of rows
     */
    public int getNrRows() {
        return m_nrRows;
    }
    
    /**
     * Getter for the underlying table.
     * @return Table as passed in constructor.
     */
    protected DataTable getUnderlyingTable() {
        return m_table;
    }

    /**
     * Calculates <b>all the statistical moments in one pass </b>. After the
     * call of this operation, the statistical moments can be obtained very fast
     * from all the other methods.
     * 
     * @param exec object to check with if user canceled the operation
     * @throws CanceledExecutionException if user canceled
     */
    protected void calculateAllMoments(final ExecutionMonitor exec)
        throws CanceledExecutionException {
        double nrRows;
        if (m_table instanceof BufferedDataTable) {
            nrRows = ((BufferedDataTable)m_table).getRowCount();
        } else {
            nrRows = Double.NaN;
        }
        calculateAllMoments(nrRows, exec); 
    }
    
    /**
     * Calculates <b>all the statistical moments in one pass </b>. After the
     * call of this operation, the statistical moments can be obtained very fast
     * from all the other methods.
     * 
     * @param rowCount Row count of table for progress, may be NaN if unknown.
     * @param exec object to check with if user canceled the operation
     * @throws CanceledExecutionException if user canceled
     * @throws IllegalArgumentException if rowCount argument < 0
     */
    protected void calculateAllMoments(final double rowCount, 
            final ExecutionMonitor exec) throws CanceledExecutionException {

        if (rowCount < 0.0) {
            throw new IllegalArgumentException(
                    "rowCount argument must not < 0: " + rowCount);
        }

        DataTableSpec origSpec = m_table.getDataTableSpec();
        int numOfCols = origSpec.getNumColumns();

        // the number of non-missing cells in each column
        int[] validCount = new int[numOfCols];
        double[] sumsquare = new double[numOfCols];
        final DataValueComparator[] comp = new DataValueComparator[numOfCols];

        for (int i = 0; i < numOfCols; i++) {
            sumsquare[i] = 0.0;
            validCount[i] = 0;
            comp[i] = origSpec.getColumnSpec(i).getType().getComparator();
            assert comp[i] != null;
        }
                
        int nrRows = 0;
        for (RowIterator rowIt = m_table.iterator(); 
            rowIt.hasNext(); nrRows++) {
            DataRow row = rowIt.next();
            if (exec != null) {
                double prog = Double.isNaN(rowCount) ? 0.0 : nrRows / rowCount;
                exec.setProgress(prog, "Calculating statistics, processing row "
                                + (nrRows + 1) + " (\"" + row.getKey() + "\")");
                exec.checkCanceled(); // throws exception if user canceled
            }
            for (int c = 0; c < numOfCols; c++) {
                final DataCell cell = row.getCell(c);
                if (!(cell.isMissing())) {
                    // keep the min and max for each column
                    if ((m_minValues[c] == null) 
                            || (comp[c].compare(cell, m_minValues[c]) < 0)) {
                        m_minValues[c] = cell;
                    }
                    if ((m_maxValues[c] == null)
                            || (comp[c].compare(m_maxValues[c], cell) < 0)) {
                        m_maxValues[c] = cell;
                    }
                    // for double columns we calc the sum (for the mean calc)
                    DataType type = origSpec.getColumnSpec(c).getType();
                    if (type.isCompatible(DoubleValue.class)) {
                        double d = ((DoubleValue) cell).getDoubleValue();
                        if (Double.isNaN(m_sum[c])) {
                            m_sum[c] = d;
                        } else {
                            m_sum[c] += d;
                        }
                        sumsquare[c] += d * d;
                        validCount[c]++;
                    }
                } else {
                    m_missingValueCnt[c]++;
                }
            }
            calculateMomentInSubClass(row);
        }
        m_nrRows = nrRows;
        
        for (int j = 0; j < numOfCols; j++) {
            // in case we got an empty table or columns that contain only
            // missing values
            if (validCount[j] == 0 || m_minValues[j] == null) {
                DataCell mc = DataType.getMissingCell();
                m_minValues[j] = mc;
                m_maxValues[j] = mc;
                m_meanValues[j] = Double.NaN;
                m_varianceValues[j] = Double.NaN;
            } else {
                m_meanValues[j] = m_sum[j] / validCount[j];
                if (validCount[j] > 1) {
                    m_varianceValues[j] = (sumsquare[j] - ((m_sum[j] * m_sum[j])
                            / validCount[j])) / (validCount[j] - 1);
                } else {
                    m_varianceValues[j] = 0.0;
                }
                // unreported bug fix: in cases in which a column contains 
                // almost only one value (for instance 1.0) but one single 
                // 'outlier' whose value is, for instance 0.9999998, we get 
                // round-off errors resulting in negative variance values
                if (m_varianceValues[j] < 0.0 && m_varianceValues[j] > -1.0E8) {
                    m_varianceValues[j] = 0.0;
                }
                assert m_varianceValues[j] >= 0.0 
                    : "Variance can not be negative (column \"" 
                        + origSpec.getColumnSpec(j).getName() + "\": " 
                        + m_varianceValues[j];
            }
        }

        // compute resulting table spec
        int nrCols = m_table.getDataTableSpec().getNumColumns();

        DataColumnSpec[] cSpec = new DataColumnSpec[nrCols];

        for (int c = 0; c < nrCols; c++) {
            DataColumnSpec s = m_table.getDataTableSpec().getColumnSpec(c);
            // we create domains with our bounds.
            Set<DataCell> values =
                    (s.getDomain() == null ? null : s.getDomain().getValues());
            DataColumnDomain newDomain =
                    new DataColumnDomainCreator(values,
                            (m_minValues[c] == null || m_minValues[c]
                                    .isMissing()) ? null : m_minValues[c],
                            (m_maxValues[c] == null || m_maxValues[c]
                                    .isMissing()) ? null : m_maxValues[c])
                            .createDomain();
            DataColumnSpecCreator creator = new DataColumnSpecCreator(s);
            creator.setDomain(newDomain);
            cSpec[c] = creator.createSpec();
        }
        m_tSpec = new DataTableSpec(cSpec);
    }
    
    /**
     * Derived classes may do additional calculations here. This method
     * is called from {@link #calculateAllMoments(ExecutionMonitor)} with
     * all of the rows.
     * @param row For processing.
     */
    protected void calculateMomentInSubClass(final DataRow row) {
        // please checkstyle
        assert (row == row);
    }

    /**
     * Returns the mean for the desired column. Throws an exception if the
     * specified column is not compatible to DoubleValue. Returns
     * {@link Double#NaN} if the specified column contains only missing cells or
     * if the table is empty.
     * 
     * @param colIdx the column index for which the mean is calculated
     * @return mean value or {@link Double#NaN}
     */
    public double getMean(final int colIdx) {
        return m_meanValues[colIdx];
    }

    /**
     * Returns the means for all columns. Returns {@link Double#NaN} if the
     * column type is not of type {@link DoubleValue}.
     * 
     * @return an array of mean values with an item for each column, which is
     *         {@link Double#NaN} if the column type is not {@link DoubleValue}
     */
    public double[] getMean() {
        double[] result = new double[m_meanValues.length];
        System.arraycopy(m_meanValues, 0, result, 0, result.length);
        return result;
    }
    
    /**
     * Returns the sum for the desired column. Throws an exception if the
     * specified column is not compatible to DoubleValue. Returns
     * {@link Double#NaN} if the specified column contains only missing cells or
     * if the table is empty.
     * 
     * @param colIdx the column index for which the mean is calculated
     * @return sum value or {@link Double#NaN}
     */
    public double getSum(final int colIdx) {
        return m_sum[colIdx];
    }

    /**
     * Returns the sum values for all columns. Returns {@link Double#NaN} if the
     * column type is not of type {@link DoubleValue}.
     * 
     * @return an array of sum values with an item for each column, which is
     *         {@link Double#NaN} if the column type is not {@link DoubleValue}
     */
    public double[] getSum() {
        double[] result = new double[m_sum.length];
        System.arraycopy(m_sum, 0, result, 0, result.length);
        return result;
    }
    
    /**
     * Returns an array of the number of missing values for each dimension.
     * @return number missing values for each dimensions
     */
    public int[] getNumberMissingValues() {
        int[] result = new int[m_missingValueCnt.length];
        System.arraycopy(m_missingValueCnt, 0, result, 0, result.length);
        return result;
    }
    
    /**
     * Returns the number of missing values for the given column index.
     * @param colIdx column index to consider
     * @return number of missing values in this columns
     */
    public int getNumberMissingValues(final int colIdx) {
        return m_missingValueCnt[colIdx];
    }
    
    /**
     * Returns the variance for the desired column. Throws an exception if the
     * specified column is not compatible to {@link DoubleValue}. Returns
     * {@link Double#NaN} if the specified column contains only missing cells or
     * if the table is empty.
     * 
     * @param colIdx the column index for which the variance is calculated
     * @return variance or {@link Double#NaN}
     */
    public double getVariance(final int colIdx) {
        return m_varianceValues[colIdx];
    }

    /**
     * Returns the variance for all columns. Returns {@link Double#NaN} if the
     * column type is not of type {@link DoubleValue}, if the entire column
     * contains missing cells, or if the table is empty.
     * 
     * @return variance values
     */
    public double[] getVariance() {
        double[] result = new double[m_varianceValues.length];
        System.arraycopy(m_varianceValues, 0, result, 0, result.length);
        return result;
    }

    /**
     * Calculates the standard deviation for the desired column. Throws an
     * exception if the column type is not compatible to {@link DoubleValue}.
     * Will return zero if the column contains only missing cells or the table
     * was empty.
     * 
     * @param colIdx the index of the column for which the standard deviation is
     *            to be calculated
     * @return standard deviation or zero if its a column of missing values of
     *         the table is empty
     */
    public double getStandardDeviation(final int colIdx) {
        return Math.sqrt(m_varianceValues[colIdx]);
    }

    /**
     * Returns the standard deviation for all columns. The returned array
     * contains no valid value (i.e. {@link Double#NaN}) for column that are
     * not compatible to {@link DoubleValue}.
     * 
     * @return standard deviation values
     */
    public double[] getStandardDeviation() {
        double[] temp = getVariance();
        for (int i = 0; i < temp.length; i++) {
            if (!Double.isNaN(temp[i])) {
                temp[i] = Math.sqrt(temp[i]);
            }
        }
        return temp;
    }

    /**
     * Returns the minimum for the desired column. Returns a missing cell, if
     * the column contains only missing cells or if the table is empty.
     * 
     * @param colIdx the index of the column for which the minimum is calculated
     * @return minimum or a missing cell if the column contains only missing
     *         cells, or if the table is empty
     */
    public DataCell getMin(final int colIdx) {
        return m_minValues[colIdx];
    }

    /**
     * Returns the minimum for all columns. Will be a missing cell for columns
     * that only contain missing cells or for empty data tables.
     * 
     * @return the minimum values
     */
    public DataCell[] getMin() {
        DataCell[] result = new DataCell[m_minValues.length];
        System.arraycopy(m_minValues, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns the minimum for all columns. Will be {@link Double#NaN} for
     * columns that only contain missing cells or for empty data tables.
     * 
     * @return the minimum values
     */
    public double[] getdoubleMin() {
        double[] result = new double[m_minValues.length];
        for (int i = 0; i < m_minValues.length; i++) {
            if (!m_minValues[i].isMissing()) {
                result[i] = ((DoubleValue)m_minValues[i]).getDoubleValue();
            } else {
                result[i] = Double.NaN;
            }
        }
        return result;
    }

    /**
     * Returns the maximum for the desired column. Returns a missing cell, if
     * the column contains only missing cells or if the table is empty.
     * 
     * @param colIdx the index of the column for which the maximum is calculated
     * @return maximum or a missing cell if the column contains only missing
     *         cells, or if the table is empty
     */
    public DataCell getMax(final int colIdx) {
        return m_maxValues[colIdx];
    }

    /**
     * Returns the maximum for all columns. Will be a missing cell for columns
     * that only contain missing cells or for empty data tables.
     * 
     * @return the maximum values
     */
    public DataCell[] getMax() {
        DataCell[] result = new DataCell[m_maxValues.length];
        System.arraycopy(m_maxValues, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns the maximum for all columns. Will be {@link Double#NaN} for
     * columns that only contain missing cells or for empty data tables.
     * 
     * @return the maximum values
     */
    public double[] getdoubleMax() {
        double[] result = new double[m_maxValues.length];
        for (int i = 0; i < m_maxValues.length; i++) {
            if (!m_maxValues[i].isMissing()) {
                result[i] = ((DoubleValue)m_maxValues[i]).getDoubleValue();
            } else {
                result[i] = Double.NaN;
            }
        }
        return result;
    }
}
