/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.MutableInteger;

/**
 * New statistic table utility class to compute statistical moments, such as
 * mean, variance, column sum, count missing values, min/max values, median, 
 * and count occurrences of all possible values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class Statistics2Table {

    /** Used to cache the media for each column. */
    private final double[] m_median;

    /** Array of maps containing DataValue value to number of occurrences. */
    private final Map<DataCell, Integer>[] m_nominalValues;

    /** Used to 'cache' the mean values. */
    private final double[] m_meanValues;

    /** Used to 'cache' the variance values. */
    private final double[] m_varianceValues;

    /** Used to cache the sum of each column. */
    private final double[] m_sum;

    /** Used to cache the number of missing values per columns. */
    private final double[] m_missingValueCnt;

    /** Used to 'cache' the minimum values. */
    private final double[] m_minValues;

    /** Used to 'cache' the maximum values. */
    private final double[] m_maxValues;
    
    /** Column name from the original data table spec. */
    private final DataTableSpec m_spec;
    
    /** If not null, a warning has been created during construction. */
    private final String m_warning;

    /**
     * Create new statistic table from an existing one. This constructor
     * calculates all values. It needs to traverse (twice) through the entire
     * specified table. User can cancel action if an execution monitor is
     * passed.
     * @param table table to be wrapped
     * @param computeMedian if the median has to be computed
     * @param numNomValuesOutput number of possible values in output table
     * @param nominalValueColumns columns used to determine all poss. values 
     * @param exec an object to check with if user canceled operation
     * @throws CanceledExecutionException if user canceled
     */
    public Statistics2Table(final BufferedDataTable table,
            final boolean computeMedian,
            final int numNomValuesOutput,
            final List<String> nominalValueColumns,
            final ExecutionContext exec) 
            throws CanceledExecutionException {
        int nrCols = table.getDataTableSpec().getNumColumns();
        m_spec = table.getDataTableSpec();
        // initialize cache arrays
        m_meanValues = new double[nrCols];
        m_varianceValues = new double[nrCols];
        m_sum = new double[nrCols];
        m_minValues = new double[nrCols];
        m_maxValues = new double[nrCols];
        m_missingValueCnt = new double[nrCols];
        m_median = new double[nrCols];
        m_nominalValues = new Map[nominalValueColumns.size()];
        // the number of non-missing cells in each column
        int[] validCount = new int[nrCols];
        double[] sumsquare = new double[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_missingValueCnt[i] = 0;
            m_meanValues[i] = Double.NaN;
            m_sum[i] = Double.NaN;
            m_varianceValues[i] = Double.NaN;
            m_minValues[i] = Double.NaN;
            m_maxValues[i] = Double.NaN;
            m_median[i] = Double.NaN;
            sumsquare[i] = 0.0;
            validCount[i] = 0;
        }
        
        // used to store warnings
        final StringBuilder warn = new StringBuilder();
        
        // temp map used to sort later based in occurrences
        Map<DataCell, MutableInteger>[] nominalValues = 
        	new Map[m_nominalValues.length];

        final int rowCnt = table.getRowCount();
        double diffProgress = rowCnt;
        if (computeMedian) {
            for (int i = 0; i < m_spec.getNumColumns(); i++) {
                if (m_spec.getColumnSpec(i).getType().isCompatible(
                        DoubleValue.class)) {
                    diffProgress += rowCnt;
                }
            }
        }
        int rowIdx = 0;
        for (RowIterator rowIt = table.iterator(); rowIt.hasNext(); rowIdx++) {
            DataRow row = rowIt.next();
            exec.setProgress(rowIdx / diffProgress,
                        "Calculating statistics, processing row "
                          + (rowIdx + 1) + " (\"" + row.getKey() + "\")");
            int colIdx = 0;
            for (int c = 0; c < nrCols; c++) {
                exec.checkCanceled();
                DataColumnSpec cspec = m_spec.getColumnSpec(c);
                final DataCell cell = row.getCell(c);
                if (!(cell.isMissing())) {
                    // for double columns we calc the sum (for the mean calc)
                    if (cspec.getType().isCompatible(DoubleValue.class)) {
                        double d = ((DoubleValue)cell).getDoubleValue();
                        // keep the min and max for each column
                        if ((Double.isNaN(m_minValues[c]))
                                || (Double.compare(d, m_minValues[c]) < 0)) {
                            m_minValues[c] = d;
                        }
                        if ((Double.isNaN(m_maxValues[c]))
                                || (Double.compare(m_maxValues[c], d) < 0)) {
                            m_maxValues[c] = d;
                        }
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
                if (nominalValueColumns.contains(cspec.getName())
                		&& (nominalValues[colIdx] == null 
                				|| (nominalValues[colIdx] != null 
                				&& nominalValues[colIdx].size() > 0))) {
                    if (nominalValues[colIdx] == null) {
                        nominalValues[colIdx] =
                            new LinkedHashMap<DataCell, MutableInteger>();
                    }
                    MutableInteger cnt = nominalValues[colIdx].get(cell);
                    if (cnt == null) {
                        nominalValues[colIdx].put(cell, new MutableInteger(1));
                    } else {
                        cnt.inc();
                    }
                    if (nominalValues[colIdx].size() == numNomValuesOutput) {
                    	if (warn.length() == 0) {
                    		warn.append("Maximum number of unique possible "
                    				+ "values (" + numNomValuesOutput 
                    				+ ") exceeds for column(s): ");
                    	} else {
                    		warn.append(",");
                    	}
                        warn.append("\"" + 
                        		m_spec.getColumnSpec(colIdx).getName() + "\"");
                        nominalValues[colIdx].clear();
                    }
                    colIdx++;
                }
            }
        }
        
        // init warning message
        if (warn.length() > 0) {
        	m_warning = warn.toString();
        } else {
        	m_warning = null;
        }

        for (int j = 0; j < nrCols; j++) {
            // in case we got an empty table or columns that contain only
            // missing values
            if (validCount[j] == 0) {
                m_minValues[j] = Double.NaN;
                m_maxValues[j] = Double.NaN;
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
                        + m_spec.getColumnSpec(j).getName()
                        + "\": "
                        + m_varianceValues[j];
            }
        }
        
        // copy map and sort each column
        for (int c = 0; c < nominalValues.length; c++) {
            Map<DataCell, MutableInteger> map = nominalValues[c];
            if (map == null) {
                m_nominalValues[c] = null;
                continue;
            }
            List<Map.Entry<DataCell, MutableInteger>> list =
                    Arrays.asList((Map.Entry<DataCell, MutableInteger>[])map
                            .entrySet().toArray(new Map.Entry[0]));
            Collections.sort(list,
                    new Comparator<Map.Entry<DataCell, MutableInteger>>() {
                        /** {@inheritDoc} */
                        @Override
                        public int compare(
                                final Entry<DataCell, MutableInteger> o1,
                                final Entry<DataCell, MutableInteger> o2) {
                            return o2.getValue().intValue()
                                    - o1.getValue().intValue();
                        }
                    });
            m_nominalValues[c] =
                    new LinkedHashMap<DataCell, Integer>(list.size());
            for (Map.Entry<DataCell, MutableInteger> e : list) {
                m_nominalValues[c].put(e.getKey(), e.getValue().intValue());
            }
        }
        
        // compute median values if desired
        if (computeMedian) {
            for (int c = 0; c < nrCols; c++) {
                exec.setMessage("Calculating median value for column \"" 
                        + m_spec.getColumnSpec(c).getName() + "\"...");
                if (m_spec.getColumnSpec(c).getType().isCompatible(
                        DoubleValue.class)) {
                    ColumnRearranger colre = new ColumnRearranger(m_spec);
                    colre.keepOnly(c);
                    ExecutionContext subexec = 
                    	exec.createSubExecutionContext(rowCnt / diffProgress);
                    BufferedDataTable singleColumn = 
                        exec.createColumnRearrangeTable(table, colre,
                        		exec.createSilentSubProgress(0.0)); 
                    SortedTable stable = new SortedTable(singleColumn, 
                        Arrays.asList(m_spec.getColumnSpec(c).getName()), 
                                new boolean[]{false}, false, subexec); 
                    int size = stable.getRowCount() 
                            - (int) m_missingValueCnt[c];
                    if (size % 2 == 0) {
                        size = size / 2;
                        double d1 = Double.NaN, d2 = Double.NaN;
                        for (DataRow row : stable) {
                            exec.checkCanceled();
                            if (size == 1) {
                                DataCell cell = row.getCell(0);
                                d1 = ((DoubleValue) cell).getDoubleValue();
                            }
                            if (size == 0) {
                                DataCell cell = row.getCell(0);
                                d2 = ((DoubleValue) cell).getDoubleValue();
                                break;
                            }
                            size--;
                        }
                        m_median[c] = (d1 + d2) / 2.0;
                    } else {
                        size = (size - 1) / 2;
                        for (DataRow row : stable) {
                            exec.checkCanceled();
                            if (size-- == 0) {
                                DataCell cell = row.getCell(0);
                                m_median[c] = 
                                    ((DoubleValue) cell).getDoubleValue();
                                break;
                            }
                        }
                    }
                    subexec.setProgress(1.0);
                }
            }
        }
    }
    
    private static final String[] ROW_HEADER = new String[]{"Minimum", 
        "Maximum", "Mean", "Std. deviation", "Variance", "Overall sum",
        "No. missings", "Median"};
    
    /**
     * Creates a table of statistic moments such as minimum, maximum, mean,
     * standard deviation, variance, overall sum, no. of missing vales, and
     * median.
     * @return a table with one moment in each row across all input columns
     */
    public DataTable createStatisticMomentsTable() {
        DataRow[] data = new DataRow[8];
        data[0] = createRow(ROW_HEADER[0], getMin());
        data[1] = createRow(ROW_HEADER[1], getMax());
        data[2] = createRow(ROW_HEADER[2], getMean());
        data[3] = createRow(ROW_HEADER[3], getStandardDeviation());
        data[4] = createRow(ROW_HEADER[4], getVariance());
        data[5] = createRow(ROW_HEADER[5], getSum());
        data[6] = createRow(ROW_HEADER[6], getNumberMissingValues());
        data[7] = createRow(ROW_HEADER[7], getMedian());
        return new DefaultTable(data, createOutSpecNumeric(m_spec));
    }
    
    /**
     * Create nominal value table containing all possible values together with
     * their occurrences.
     * @return nominal value output table
     */
    public DataTable createNominalValueTable(final List<String> nominalValues) {
        DataTableSpec outSpec = createOutSpecNominal(m_spec, nominalValues);
        Iterator[] it = new Iterator[outSpec.getNumColumns() / 2];
        int idx = 0;
        for (int i = 0; i < m_nominalValues.length; i++) {
            if (m_nominalValues[i] != null) {
                it[idx++] = m_nominalValues[i].entrySet().iterator();
            }
        }
        DataContainer cont = new DataContainer(outSpec);
        int rowIndex = 0;
        do {
            boolean addEnd = true;
            DataCell[] cells = new DataCell[2 * it.length];
            for (int i = 0; i < it.length; i++) {
               if (it[i].hasNext()) {
                   Map.Entry<DataCell, Integer> e = 
                       (Map.Entry<DataCell, Integer>) it[i].next();
                   cells[2 * i] = e.getKey();
                   cells[2 * i + 1] = new IntCell(e.getValue());
                   addEnd = false;
               } else {
                   cells[2 * i] = DataType.getMissingCell();
                   cells[2 * i + 1] = DataType.getMissingCell();
               }
            }
            if (addEnd) {
                break;
            }
            cont.addRowToTable(
                    new DefaultRow(RowKey.createRowKey(rowIndex++), cells));
        } while (true);
        cont.close();
        return cont.getTable();
    }
    
    private DataRow createRow(final String key, final double[] array) {
        int idx = 0;
        DataTableSpec outSpec = createOutSpecNumeric(m_spec);
        DataCell[] data = new DataCell[outSpec.getNumColumns()];
        for (int i = 0; idx < data.length; i++) {
            if (outSpec.getColumnSpec(idx).getName().equals(
                    m_spec.getColumnSpec(i).getName())) {
                if (Double.isNaN(array[i])) {
                    data[idx] = DataType.getMissingCell();
                } else {
                    data[idx] = new DoubleCell(array[i]);
                }
                idx++;
            }
        }
        return new DefaultRow(key, data);
    }
    
    /**
     * Create spec containing only numeric columns in same order as the input 
     * spec.
     * @param inSpec input spec 
     * @return a new spec with all numeric columns
     */
    public static DataTableSpec createOutSpecNumeric(
            final DataTableSpec inSpec) {
        ArrayList<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = inSpec.getColumnSpec(i);
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                cspecs.add(new DataColumnSpecCreator(cspec.getName(), 
                    DoubleCell.TYPE).createSpec());
            }
        }
        return new DataTableSpec(cspecs.toArray(new DataColumnSpec[0]));
    }
    
    /**
     * Create spec containing only nominal columns in same order as the input 
     * spec.
     * @param inSpec input spec 
     * @param nominalValues used in map of co-occurrences
     * @return a new spec with all nominal columns
     */
    public static DataTableSpec createOutSpecNominal(
            final DataTableSpec inSpec, final List<String> nominalValues) {
        ArrayList<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = inSpec.getColumnSpec(i);
            if (nominalValues.contains(cspec.getName())) {
                cspecs.add(cspec);
                String countCol = DataTableSpec.getUniqueColumnName(
                        inSpec, cspec.getName() + "_Count");
                cspecs.add(new DataColumnSpecCreator(countCol,
                            IntCell.TYPE).createSpec());
            }
        }
        return new DataTableSpec(cspecs.toArray(new DataColumnSpec[0]));
    }
    
    /**
     * @return array of column names
     */
    public String[] getColumnNames() {
        String[] names = new String[m_spec.getNumColumns()];
        for (int i = 0; i < names.length; i++) {
            names[i] = m_spec.getColumnSpec(i).getName();
        }
        return names;
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
     * 
     * @return number missing values for each dimensions
     */
    public double[] getNumberMissingValues() {
        double[] result = new double[m_missingValueCnt.length];
        System.arraycopy(m_missingValueCnt, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns the number of missing values for the given column index.
     * 
     * @param colIdx column index to consider
     * @return number of missing values in this columns
     */
    public double getNumberMissingValues(final int colIdx) {
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
     * Returns the minimum for all columns. Will be {@link Double#NaN} for
     * columns that only contain missing cells or for empty data tables.
     * 
     * @return the minimum values
     */
    public double[] getMin() {
        return Arrays.copyOf(m_minValues, m_minValues.length);
    }

    /**
     * Returns the maximum for all columns. Will be {@link Double#NaN} for
     * columns that only contain missing cells or for empty data tables.
     * 
     * @return the maximum values
     */
    public double[] getMax() {
        return Arrays.copyOf(m_maxValues, m_maxValues.length);
    }

    /**
     * Returns the median for the desired column.
     * 
     * @param colIdx the column index for which the median is calculated
     * @return median value
     */
    public double getMedian(final int colIdx) {
        if (m_median == null) {
            return Double.NaN;
        }
        return m_median[colIdx];
    }

    /**
     * Returns the median for all columns.
     * 
     * @return an array of median values with an item for each column
     */
    public double[] getMedian() {
        double[] result = new double[m_median.length];
        System.arraycopy(m_median, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns a map containing DataCell value to number of occurrences.
     * 
     * @param colIdx column index to return map for
     * @return map of DataCell values to occurrences
     */
    public Map<DataCell, Integer> getNominalValues(final int colIdx) {
        if (m_nominalValues[colIdx] == null) {
            return null;
        }
        return Collections.unmodifiableMap(m_nominalValues[colIdx]);
    }

    /**
     * Returns an array (for each column) of mappings containing DataCell value
     * to number of occurrences.
     * 
     * @return array of mappings of occurrences
     */
    public Map<DataCell, Integer>[] getNominalValues() {
        Map<DataCell, Integer>[] result = new Map[m_nominalValues.length];
        for (int i = 0; i < m_nominalValues.length; i++) {
            result[i] = getNominalValues(i);
        }
        return result;
    }
    
    private Statistics2Table(final DataTableSpec spec, 
            final double[] minValues, final double[] maxValues,
            final double[] meanValues, final double[] median,
            final double[] varianceValues, final double[] sum,
            final double[] missings, final Map<DataCell, Integer>[] nomValues) {
        m_spec = spec;
        m_minValues = minValues;
        m_maxValues = maxValues;
        m_meanValues = meanValues;
        m_median = median;
        m_varianceValues = varianceValues;
        m_sum = sum;
        m_missingValueCnt = missings;
        m_nominalValues = nomValues;
        m_warning = null;
    }
    
    /**
     * Returns warning message if number of possible values exceeds predefined 
     * maximum.
     * @return null or a warning issued during construction time
     */
    public String getWarning() {
    	return m_warning;
    }
    
    /**
     * Load a new statistic table by the given settings object.
     * @param sett to load this table from
     * @return a new statistic table
     * @throws InvalidSettingsException if the settings are corrupt
     */
    public static Statistics2Table load(final NodeSettingsRO sett) 
            throws InvalidSettingsException {
        DataTableSpec spec = DataTableSpec.load(sett.getConfig("spec"));
        Map<DataCell, Integer>[] nominalValues = new Map[spec.getNumColumns()];
        for (int c = 0; c < nominalValues.length; c++) {
            String name = spec.getColumnSpec(c).getName();
            if (!sett.containsKey(name)) {
                nominalValues[c] = null;
            } else {
                nominalValues[c] = new LinkedHashMap<DataCell, Integer>();
                NodeSettingsRO subSett = sett.getNodeSettings(name);
                for (String key : subSett.keySet()) {
                    NodeSettingsRO nomSett = subSett.getNodeSettings(key);
                    nominalValues[c].put(nomSett.getDataCell("key"),
                            nomSett.getInt("value"));
                }
            }
        }
        double[] min = sett.getDoubleArray("minimum");
        double[] max = sett.getDoubleArray("maximum");
        double[] mean = sett.getDoubleArray("mean");
        double[] var = sett.getDoubleArray("variance");
        double[] median = sett.getDoubleArray("median");
        double[] missings = sett.getDoubleArray("missings");
        double[] sums = sett.getDoubleArray("sums");
        return new Statistics2Table(spec, min, max, mean, median, 
                var, sums, missings, nominalValues);
    }

    /**
     * Saves this object to the given settings object.
     * @param sett this object is saved to
     */
    public void save(final NodeSettingsWO sett) {
        m_spec.save(sett.addConfig("spec"));
        sett.addDoubleArray("minimum", m_minValues);
        sett.addDoubleArray("maximum", m_maxValues);
        sett.addDoubleArray("mean", m_meanValues);
        sett.addDoubleArray("variance", m_varianceValues);
        sett.addDoubleArray("median", m_median);
        sett.addDoubleArray("missings", m_missingValueCnt);
        sett.addDoubleArray("sums", m_sum);
        for (int c = 0; c < m_nominalValues.length; c++) {
            if (m_nominalValues[c] != null) {
                NodeSettingsWO subSett = sett.addNodeSettings(
                        m_spec.getColumnSpec(c).getName());
                for (Map.Entry<DataCell, Integer> e 
                        : m_nominalValues[c].entrySet()) {
                    NodeSettingsWO nomSett = subSett.addNodeSettings(
                            e.getKey().toString());
                    nomSett.addDataCell("key", e.getKey());
                    nomSett.addInt("value", e.getValue());
                }
            }
        }
    }

}
