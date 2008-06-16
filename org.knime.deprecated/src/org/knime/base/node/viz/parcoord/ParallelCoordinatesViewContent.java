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
 */
package org.knime.base.node.viz.parcoord;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;


/**
 * The view's content model which keeps track on the data and all its additional
 * properties.
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public final class ParallelCoordinatesViewContent implements Serializable {

    /** <code>m_inputSpec</code> the m_inputSpec of m_input. */
    private final transient DataTableSpec m_inputSpec;

    /** <code>m_columnCount</code> the number of columns for m_input. */
    private final int m_columnCount;
    
    /** the number of visible columns. */
    private int m_nrVisibleColumns;
    
    /** remember which columns are hidden. */
    private boolean[] m_hideColumn;
    
    /** <code>m_columnCount</code> the number of rows for m_input. */
    private final int m_rowCount;
    
    private boolean m_storesAllRows;

    /**
     * <code>m_stringValuesNo</code> the number of distinct nominal values for
     * a certain column.
     */
    private final int[] m_stringValuesNo;

    /**
     * <code>m_minVector</code> the minimum values for number columns.
     */
    private final double[] m_minVector;

    /**
     * <code>m_maxVector</code> the maximum values for number columns.
     */
    private final double[] m_maxVector;

    /** <code>m_keyVector</code> the keys for each row. */
    private final RowKey[] m_keyVector;

    /** <code>m_colorVector</code> the colors for each row. */
    private final ColorAttr[] m_colorVector;
    
    /** the sizes of all points. */
    private final double[] m_sizes;

    /** <code>m_typesVector</code> the types for each column. */
    private final String[] m_typesVector;

    /** <code>m_namesVector</code> the names for each column. */
    private final String[] m_namesVector;

    /** <code>m_stringValues</code> the nominal values for each column. */
    private final String[][] m_stringValues;

    /**
     * <code>m_doubleArray</code> the array containing pairs of min, max
     * values corresponding to each row and axis.
     */
    private final TwoDim[][] m_doubleArray;

    private final boolean[] m_rowIsDrawable;

    /**
     * <code>getInputSpec</code> the m_inputSpec.
     * 
     * @return the inputSpec of the input table
     */
//    public DataTableSpec getInputSpec() {
//        return m_inputSpec;
//    }

    /**
     * @return the number of columns that are actually visible
     */
    public int getColumnCount() {
        return m_nrVisibleColumns;
    }

    /**
     * @return the number of rows for m_input
     */
    public int getRowCount() {
        return m_rowCount;
    }

    /**
     * @return the minimum values for number columns
     */
    public double[] getMinVector() {
        return m_minVector;
    }

    /**
     * @return the maximum values for number columns
     */
    public double[] getMaxVector() {
        return m_maxVector;
    }

    /**
     * @return the row keys for each row
     */
    public RowKey[] getKeyVector() {
        return m_keyVector;
    }
    
    /**
     * @return the colors for each row
     */
    public ColorAttr[] getColorVector() {
        return m_colorVector;
    }

    /**
     * @return the sizes of all rows
     */
    public double[] getSizes() {
        return m_sizes;
    }

    /**
     * @return the types for each column
     */
    public String[] getTypesVector() {
        return m_typesVector;
    }

    /**
     * @return the names for each column
     */
    public String[] getNamesVector() {
        return m_namesVector;
    }

    /**
     * @return the string values for each column
     */
    public String[][] getStringValues() {
        return m_stringValues;
    }

    /**
     * @return the number of string values for each column
     */
    public int[] getStringValuesNo() {
        return m_stringValuesNo;
    }

    /**
     * <code>getDoubleArray</code>.
     * 
     * @return the array containing pairs of min, max values corresponding to
     *         each row and axis
     */
    public TwoDim[][] getDoubleArray() {
        return m_doubleArray;
    }

    /**
     * <code>normalize</code> a normalizing function.
     * 
     * @param value the value to be normalized
     * @param min the min boundary
     * @param max the max boundary
     * @return the normalized value
     */
    private double normalize(final double value, final double min,
            final double max) {
        return (value - min) / (max - min);
    }

    /**
     * Creates a new content view model based on the given
     * <code>DataTable</code>.
     * 
     * @param data The input table.
     * @param hiddenColumns an array of column names to be hidden
     * @param desiredMaxNumRows maximum number of rows to be displayed or -1 for
     *        all.
     * @param exec an object to ask if user canceled operation.
     * @throws CanceledExecutionException if user canceled.s
     */
    public ParallelCoordinatesViewContent(final DataTable data,
            final String[] hiddenColumns, final int desiredMaxNumRows,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        m_inputSpec = data.getDataTableSpec();
        // determine which columns will be visible in the view
        m_hideColumn = new boolean[m_inputSpec.getNumColumns()];
        m_nrVisibleColumns = 0;
        for (int i = 0; i < m_inputSpec.getNumColumns(); i++) {
            m_hideColumn[i] = false;
            int j = 0;
            while (j < hiddenColumns.length) {
                if (hiddenColumns[j].equals(
                        m_inputSpec.getColumnSpec(i).getName())) {
                    m_hideColumn[i] = true;
                    j = hiddenColumns.length;
                }
                j++;
            }
            if (!m_hideColumn[i]) {
                m_nrVisibleColumns++;
            }
        }
        m_columnCount = m_inputSpec.getNumColumns();
        m_namesVector = new String[m_nrVisibleColumns]; // names of dimensions
        m_typesVector = new String[m_nrVisibleColumns]; // types of dimensions
        final int maxNumRows;
        if (desiredMaxNumRows <= 0) {
            int count = 0;
            if (data instanceof BufferedDataTable) {
                count = ((BufferedDataTable)data).getRowCount();
            }
            if (count < 0) {
                // set max number of rows to actual number of rows
                // only iterate over data table to get the #rows when no 
                // max given
                for (RowIterator it = data.iterator(); it.hasNext(); 
                    it.next()) {
                    count++;
                    if (exec != null) {
                        exec.checkCanceled();
                    }
                }
            }
            assert count >= 0;
            maxNumRows = count;
        } else {
            maxNumRows = desiredMaxNumRows;
        }
        // the drawable rows by id
        m_rowIsDrawable = new boolean[maxNumRows];
        // vector of minimal values on an axis(coordinate)
        m_minVector = new double[m_nrVisibleColumns];
        // vector of maximal values on an axis(coordinate)
        m_maxVector = new double[m_nrVisibleColumns];
        // a vector of treeSets that holds for each column an ordered
        // set of possible values
        m_stringValues = new String[m_nrVisibleColumns][];
        m_stringValuesNo = new int[m_nrVisibleColumns];
        Vector<Set<DataCell>> stringie = new Vector<Set<DataCell>>();
        stringie.setSize(m_nrVisibleColumns);
        Vector<Map<String, Integer>> stringieMap = 
            new Vector<Map<String, Integer>>();
        stringieMap.setSize(m_nrVisibleColumns);
        // assigning treesets to every column & initialisation of min, max
        // vectors of values for each coordinate
        for (int col = 0, realCol = 0; col < m_columnCount; col++) {
            if (!m_hideColumn[col]) {
                if (m_inputSpec.getColumnSpec(col).getDomain() != null) {
                    // the maximum boundary
                    if (m_inputSpec.getColumnSpec(col).getDomain()
                            .hasUpperBound()) {
                        m_maxVector[realCol] = ((DoubleValue)(m_inputSpec
                                .getColumnSpec(col).getDomain()
                                .getUpperBound())).getDoubleValue();
                    } else {
                        m_maxVector[realCol] = -Double.MAX_VALUE;
                    }
                    // the minimum boundary
                    if (m_inputSpec.getColumnSpec(col).getDomain()
                            .hasLowerBound()) {
                        m_minVector[realCol] = ((DoubleValue)(m_inputSpec
                                .getColumnSpec(col).getDomain()
                                .getLowerBound())).getDoubleValue();
                    } else {
                        m_minVector[realCol] = Double.MAX_VALUE;
                    }
                    stringieMap.set(realCol,
                            new LinkedHashMap<String, Integer>());
                    // the values of string columns
                    if ((m_inputSpec.getColumnSpec(col).getDomain()
                            .hasValues())) {
                        stringie.set(realCol, null);
                        Set<DataCell> values = m_inputSpec.getColumnSpec(col)
                                .getDomain().getValues();
                        m_stringValuesNo[realCol] = values.size();
                        m_stringValues[realCol] =
                            new String[m_stringValuesNo[realCol]];
                        int it = 0;
                        for (DataCell cell : values) {
                            String cellString = cell.toString().trim();
                            // put nominal value+index as Integer into TreeMap
                            stringieMap.get(realCol).put(cellString, it);
                            // add nominal value to array as well
                            m_stringValues[realCol][it] = cellString;
                            it++;
                        }
                    } else {
                        stringie.set(realCol, new LinkedHashSet<DataCell>());
                    }
                }
                realCol++;
            }
        }
        // calculating m_typesVector, m_namesVector
        for (int col = 0, realCol = 0; col < m_columnCount; col++) {
            if (!m_hideColumn[col]) {
                m_namesVector[realCol] = m_inputSpec.getColumnSpec(col)
                        .getName().toString();
                DataType colType = m_inputSpec.getColumnSpec(col).getType();
                if (colType.isCompatible(DoubleValue.class)) {
                    m_typesVector[realCol] = "double";
                } else if (colType.isCompatible(FuzzyIntervalValue.class)) {
                    m_typesVector[realCol] = "fuzzyinterval";
                } else {
                    m_typesVector[realCol] = "string";
                }
                realCol++;
            }
        }
        // calculating m_minVector, m_maxVector
        int nrRows = 0;
        int i = 0;
        RowIterator it = data.iterator();
        for (; (i < maxNumRows) && it.hasNext(); i++) {
            if (exec != null) {
                exec.checkCanceled();
                // we are doing only half of the job here
                exec.setProgress(nrRows / maxNumRows / 2.0);
            }
            DataRow row = it.next();
            m_rowIsDrawable[i] = true;
            for (int col = 0, realCol = 0; col < m_columnCount; col++) {
                if (!m_hideColumn[col]) {
                    DataCell cell = row.getCell(col);
                    if (cell.isMissing()) {
                        m_rowIsDrawable[i] = false;
                    } else {
                        if (cell.getType().isCompatible(DoubleValue.class)) {
                            DoubleValue dcell = (DoubleValue)cell;
                            double dValue = dcell.getDoubleValue();
                            if (dValue < m_minVector[realCol]) {
                                m_minVector[realCol] = dValue;
                            }
                            if (dValue > m_maxVector[realCol]) {
                                m_maxVector[realCol] = dValue;
                            }
                        } else if (cell.getType().isCompatible(
                                FuzzyIntervalValue.class)) {
                            FuzzyIntervalValue fcell = (FuzzyIntervalValue)cell;
                            double mincore = fcell.getMinCore();
                            double maxcore = fcell.getMaxCore();
                            m_minVector[realCol] =
                                Math.min(m_minVector[realCol], mincore);
                            m_maxVector[realCol] =
                                Math.max(m_maxVector[realCol], maxcore);
                        } else {
                            if (stringie.get(realCol) != null) {
                                // the column is at least of string type. If
                                // there was no list of values in the
                                // DataTableSpec create it.
                                stringie.get(realCol).add(row.getCell(col));
                            }
                        }
                    }
                    realCol++;
                }
            }
            if (m_rowIsDrawable[i]) {
                nrRows++;
            }
        }
        // remember if we reached the maximum number of rows allowed
        m_storesAllRows = !(it.hasNext());
        m_rowCount = nrRows; // finally set the row count that is actually seen
        // building map for every nominal column that had no list of poss. 
        // values defined in the DTS
        for (int col = 0, realCol = 0; col < m_columnCount; col++) {
            if (!m_hideColumn[col]) {
                if (stringie.get(realCol) != null) {
                    i = 0;
                    m_stringValuesNo[realCol] = stringie.get(realCol).size();
                    m_stringValues[realCol] =
                        new String[m_stringValuesNo[realCol]];
                    for (Iterator<DataCell> stringieIt = stringie.get(realCol)
                            .iterator(); stringieIt.hasNext(); i++) {
                        String s = stringieIt.next().toString().trim();
                        stringieMap.get(realCol).put((s), new Integer(i));
                        m_stringValues[realCol][i] = s;
                    }
                }
                realCol++;
            }
        }
        // computing the double values
        m_keyVector = new RowKey[m_rowCount];
        m_colorVector = new ColorAttr[m_rowCount];
        m_sizes = new double[m_rowCount];
        m_doubleArray = new TwoDim[m_rowCount][m_nrVisibleColumns];
        // compute double values
        computeDoubleValues(data, stringieMap, exec, m_rowCount);
    } // ParallelCoordinatesViewContent(DataTable)
    
    /**
     * @return flag indicating if model does indeed hold all rows or if
     *   storage was ended prematurely because max-nr-rows was reached.
     */
    public boolean storesAllRows() {
        return m_storesAllRows;
    }

    private void computeDoubleValues(final DataTable data,
            final Vector<Map<String, Integer>> stringieMap,
            final ExecutionMonitor exec, final int nrRows)
            throws CanceledExecutionException {
        int i = 0; // iterate over the rows that are drawable
        int j = 0; // iterate over the rows
        for (RowIterator it = data.iterator(); (i < nrRows) && it.hasNext();
                                     j++) {
            if (exec != null) {
                exec.checkCanceled();
                // we are doing only half of the job here (but the second half)
                exec.setProgress((j / nrRows / 2.0) + 1.0 / 2.0);
            }
            DataRow row = it.next();
            if (m_rowIsDrawable[j]) {
                m_doubleArray[i] = new TwoDim[m_nrVisibleColumns];
                m_keyVector[i] = row.getKey();
                m_colorVector[i] = m_inputSpec.getRowColor(row);
                m_sizes[i] = data.getDataTableSpec().getRowSize(row);
                for (int col = 0, realCol = 0;
                                         col < m_columnCount; col++) {
                    if (!m_hideColumn[col]) {
                        m_doubleArray[i][realCol] = new TwoDim();
                        DataCell cell = row.getCell(col);
                        if (cell.getType().isCompatible(DoubleValue.class)) {
                            DoubleValue dcell = (DoubleValue)cell;
                            double dValue = normalize(dcell.getDoubleValue(),
                                    m_minVector[realCol], m_maxVector[realCol]);
                            m_doubleArray[i][realCol].setMin(dValue);
                            m_doubleArray[i][realCol].setMax(dValue);
                        } else if (cell.getType().isCompatible(
                                FuzzyIntervalValue.class)) {
                            if (cell.isMissing()) {
                                m_doubleArray[i][realCol].setMin(0);
                                m_doubleArray[i][realCol].setMax(1);
                            } else {
                                FuzzyIntervalValue fcell =
                                    (FuzzyIntervalValue)cell;
                                double mincore = fcell.getMinCore();
                                double maxcore = fcell.getMaxCore();
                                m_doubleArray[i][realCol].setMin(
                                        normalize(mincore, m_minVector[realCol],
                                                m_maxVector[realCol]));
                                m_doubleArray[i][realCol].setMax(
                                        normalize(maxcore, m_minVector[realCol],
                                                m_maxVector[realCol]));
                            }
                        } else { // if the column is a string one
                            // no. corresponding to the string value
                            int intValue = 0;
                            double dValue = 0;
                            intValue = ((stringieMap.get(realCol).get(cell
                                    .toString().trim()))).intValue();
                            dValue = (double)intValue
                                    / (stringieMap.get(realCol).size() - 1);
                            m_doubleArray[i][realCol].setMin(dValue);
                            m_doubleArray[i][realCol].setMax(dValue);
                            m_doubleArray[i][realCol]
                                             .setStringReference(intValue);
                        }
                        realCol++;
                    } else {  // column is hidden
                        // do nothing
                    }
                }
                i++;
            }
        }
    }

} // class ParallelCoordinatesViewContent
