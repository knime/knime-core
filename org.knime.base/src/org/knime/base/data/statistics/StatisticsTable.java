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
 * If you have any quesions please contact the copyright holder:
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
    /*
     * table to be wrapped
     */
    private final DataTable m_table;

    /*
     * Used to 'cache' the mean values
     */
    private final double[] m_meanValues;

    /*
     * Used to 'cache' the variance values
     */
    private final double[] m_varianceValues;

    /*
     * Used to 'cache' the minimum values
     */
    private final DataCell[] m_minValues;

    /*
     * Used to 'cache' the maximum values
     */
    private final DataCell[] m_maxValues;

    /*
     * Number of rows of the DataTable
     */
    private int m_nrRows;

    /*
     * a table spec we've created with ranges added to all numerical columns
     */
    private final DataTableSpec m_tSpec;

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
        m_table = table;
        int nrCols = m_table.getDataTableSpec().getNumColumns();
        // initialize cache arrays
        m_meanValues = new double[nrCols];
        m_varianceValues = new double[nrCols];
        m_minValues = new DataCell[nrCols];
        m_maxValues = new DataCell[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_meanValues[i] = Double.NaN;
            m_varianceValues[i] = Double.NaN;
            m_minValues[i] = null;
            m_maxValues[i] = null;
        }
        m_tSpec = calculateAllMoments(exec);
    }

    /**
     * Produces a DataTableSpec for the statistics table which contains the
     * range values calculated here.
     * 
     * @return a table spec with ranges set in column. If the spec of the
     *         underlying table had ranges set nothing will change.
     */
    public DataTableSpec getDataTableSpec() {
        return m_tSpec;

    }

    /**
     * Returns the row iterator of the original data table.
     * 
     * 
     * @see DataTable#iterator()
     */
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
     * Calculates <b>all the statistical moments in one pass </b>. After the
     * call of this operation, the statistical moments can be obtained very fast
     * from all the other methods.
     * 
     * @param exec object to check with if user canceled the operation
     * @return the newly calculated spec
     * @throws CanceledExecutionException if user canceled
     */
    private DataTableSpec calculateAllMoments(final ExecutionMonitor exec)
            throws CanceledExecutionException {

        DataTableSpec origSpec = m_table.getDataTableSpec();
        int numOfCols = origSpec.getNumColumns();

        // Initialize all temp-array
        double[] sum = new double[numOfCols];
        // the number of non-missing cells in each column
        int[] validCount = new int[numOfCols];
        double[] sumsquare = new double[numOfCols];
        DataValueComparator[] comp = new DataValueComparator[numOfCols];

        for (int i = 0; i < numOfCols; i++) {
            sum[i] = 0.0;
            sumsquare[i] = 0.0;
            validCount[i] = 0;
            comp[i] = origSpec.getColumnSpec(i).getType().getComparator();
            assert comp[i] != null;
        }

        if (m_table instanceof BufferedDataTable) {
            m_nrRows = ((BufferedDataTable)m_table).getRowCount();
        } else {
            m_nrRows = Integer.MAX_VALUE;
        }
        int nrRows = 0;
        DataRow row;
        for (RowIterator rowIt = m_table.iterator(); rowIt.hasNext(); nrRows++) {
            row = rowIt.next();
            if (exec != null) {
                exec.setProgress(Math.max(0.1, (double)nrRows
                        / (double)m_nrRows),
                        "Calculating statistics, processing row "
                                + (nrRows + 1) + " (\"" + row.getKey() + "\")");
                exec.checkCanceled(); // throws exception if user canceled
            }
            for (int c = 0; c < numOfCols; c++) {
                if (!(row.getCell(c).isMissing())) {
                    // keep the min and max for each column
                    if ((m_minValues[c] == null)
                            || (comp[c].compare(row.getCell(c), m_minValues[c]) < 0)) {
                        m_minValues[c] = row.getCell(c);
                    }
                    if ((m_maxValues[c] == null)
                            || (comp[c].compare(m_maxValues[c], row.getCell(c)) < 0)) {
                        m_maxValues[c] = row.getCell(c);
                    }
                    // for double columns we calc the sum (for the mean calc)
                    DataType type = origSpec.getColumnSpec(c).getType();
                    if (type.isCompatible(DoubleValue.class)) {
                        double d = ((DoubleValue)row.getCell(c))
                                .getDoubleValue();
                        sum[c] += d;
                        sumsquare[c] += d * d;
                        validCount[c]++;
                    }
                }
            }
        }
        m_nrRows = nrRows;

        for (int j = 0; j < numOfCols; j++) {
            // in case we got an emtpy table or columns that contain only
            // missing values
            if (validCount[j] == 0 || m_minValues[j] == null) {
                DataCell mc = DataType.getMissingCell();
                m_minValues[j] = mc;
                m_maxValues[j] = mc;
                m_meanValues[j] = Double.NaN;
                m_varianceValues[j] = Double.NaN;
            } else {
                m_meanValues[j] = sum[j] / validCount[j];
                m_varianceValues[j] = (sumsquare[j] - ((sum[j] * sum[j]) / validCount[j]))
                        / (validCount[j] - 1);
            }
        }

        // compute resulting table spec
        int nrCols = m_table.getDataTableSpec().getNumColumns();

        DataColumnSpec[] cSpec = new DataColumnSpec[nrCols];

        for (int c = 0; c < nrCols; c++) {
            DataColumnSpec s = m_table.getDataTableSpec().getColumnSpec(c);
            // we create domains with our bounds.
            Set<DataCell> values = (s.getDomain() == null ? null : s
                    .getDomain().getValues());
            DataColumnDomain newDomain = new DataColumnDomainCreator(values,
                    m_minValues[c], m_maxValues[c]).createDomain();
            DataColumnSpecCreator creator = new DataColumnSpecCreator(s);
            creator.setDomain(newDomain);
            cSpec[c] = creator.createSpec();
        }
        return new DataTableSpec(cSpec);

    }

    /**
     * Returns the mean for the desired column. Throws an exception if the
     * specified column is not comaptible to DoubleValue. Returns
     * {@link Double#NaN} if the specified column contains only missing cells or
     * if the table is empty.
     * 
     * @param colIdx the column index for which the mean is calculated
     * @return mean value or {@link Double#NaN}
     */
    public double getMean(final int colIdx) {
        if (!m_table.getDataTableSpec().getColumnSpec(colIdx).getType()
                .isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException("Can only calculate variance"
                    + "of double columns (Col " + colIdx + " is not)");
        }
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
     * Returns the variance for the desired column. Throws an exception if the
     * specified column is not comaptible to {@link DoubleValue}. Returns
     * {@link Double#NaN} if the specified column contains only missing cells or
     * if the table is empty.
     * 
     * @param colIdx the column index for which the variance is calculated
     * @return variance or {@link Double#NaN}
     */
    public double getVariance(final int colIdx) {
        if (!m_table.getDataTableSpec().getColumnSpec(colIdx).getType()
                .isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException("Can only calculate variance"
                    + "of double columns (Col " + colIdx + " is not)");
        }
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
     * was emtpy.
     * 
     * @param colIdx the index of the column for which the standard deviation is
     *            tobe calculated
     * @return standard deviation or zero if its a column of missing values of
     *         the table is empty
     */
    public double getStandardDeviation(final int colIdx) {
        if (!m_table.getDataTableSpec().getColumnSpec(colIdx).getType()
                .isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException("Can only calculate standard"
                    + "deviation of double columns (Col " + colIdx + " is not)");
        }
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
        if ((colIdx < 0) || (colIdx >= m_minValues.length)) {
            throw new IllegalArgumentException("Column index (" + colIdx
                    + ") out of bounds. (0 <= idx <= " + m_minValues.length
                    + ")");
        }
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
        if ((colIdx < 0) || (colIdx >= m_maxValues.length)) {
            throw new IllegalArgumentException("Column index (" + colIdx
                    + ") out of bounds. (0 <= idx <= " + m_maxValues.length
                    + ")");
        }
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
