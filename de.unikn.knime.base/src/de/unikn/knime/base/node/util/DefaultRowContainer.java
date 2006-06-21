/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   09.03.2005 (ohl): created
 */
package de.unikn.knime.base.node.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataValueComparator;
import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnDomainCreator;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.StringValue;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;

/**
 * Can be used to locally store a certain number of rows. It provides random
 * access to the stored rows. It maintains the min and max value for each column
 * (min/max with respect to the row sample stored - not the entire data table).
 * These values can be changed, in case somebody knows better limits. It
 * provides a list of all values seen for each string column (i.e. a list of all
 * values appearing in the rows stored - not the entire data table).
 * 
 * @author ohl, University of Konstanz
 */
public class DefaultRowContainer implements RowContainer {

    /* this is where we store the rows. */
    private ArrayList<DataRow> m_rows;

    /* all occuring values for each string column */
    private Vector<LinkedHashSet<DataCell>> m_possVals;

    /* the max value for each column */
    private DataCell[] m_maxVal;

    /* the min values for each column */
    private DataCell[] m_minVal;

    /* the first row we've stored */
    private int m_firstRow;

    /*
     * we store the table spec - in case somebody needs name and type of the
     * stored rows
     */
    private DataTableSpec m_tSpec;

    /**
     * Constructs a random access container holding a certain number of rows
     * from the data table passed in. It will store the specified amount of rows
     * starting from the row specified in the "<code>first</code>"
     * parameter. The rows can be accessed by index later on always starting
     * with index zero.
     * 
     * @param dTable the data table to read the rows from.
     * @param firstRow the first row to store (must be greater than zero)
     * @param numOfRows the number of rows to store (must be zero or more)
     */
    public DefaultRowContainer(final DataTable dTable, final int firstRow,
            final int numOfRows) {
        try {
            init(dTable, firstRow, numOfRows, null);
        } catch (CanceledExecutionException cee) {
            // won't happen as we pass a null execMonitor...
        }
    }

    /**
     * Same, but allows for user cancelation from a progressmonitor, while the
     * container is filled.
     * 
     * @param dTable the data table to read the rows from.
     * @param firstRow the first row to store (must be greater than zero)
     * @param numOfRows the number of rows to store (must be zero or more)
     * @param execMon the object listening to our progress and providing cancel
     *            functionality
     * @throws CanceledExecutionException if the construction was canceled
     */
    public DefaultRowContainer(final DataTable dTable, final int firstRow,
            final int numOfRows, final ExecutionMonitor execMon)
            throws CanceledExecutionException {
        init(dTable, firstRow, numOfRows, execMon);
    }

    private void init(final DataTable dTable, final int firstRow,
            final int numOfRows, final ExecutionMonitor execMon)
            throws CanceledExecutionException {

        if (dTable == null) {
            throw new NullPointerException("Must provide non-null data table"
                    + " for RowContainer");
        }
        if (firstRow < 1) {
            throw new IllegalArgumentException("Starting row must be greater"
                    + " than zero");
        }
        if (numOfRows < 0) {
            throw new IllegalArgumentException("Number of rows to read must be"
                    + " greater than or equal zero");
        }

        DataTableSpec tSpec = dTable.getDataTableSpec();

        int numOfColumns = tSpec.getNumColumns();

        m_firstRow = firstRow;
        m_rows = new ArrayList<DataRow>(numOfColumns);
        m_maxVal = new DataCell[numOfColumns];
        m_minVal = new DataCell[numOfColumns];

        // create a new list for the values - but only for native string columns
        m_possVals = new Vector<LinkedHashSet<DataCell>>();
        m_possVals.setSize(numOfColumns);
        for (int c = 0; c < numOfColumns; c++) {
            if (tSpec.getColumnSpec(c).getType().isCompatible(
                    StringValue.class)) {
                m_possVals.set(c, new LinkedHashSet<DataCell>());
            }
        }

        // now fill our data structures
        RowIterator rIter = dTable.iterator();
        int rowNumber = 0;

        while ((rIter.hasNext()) && (m_rows.size() < numOfRows)) {
            // get the next row
            DataRow row = rIter.next();
            rowNumber++;

            if (rowNumber < firstRow) {
                // skip all rows until we see the specified first row
                continue;
            }

            // store it.
            m_rows.add(row);

            // check min, max values and possible values for each column
            for (int c = 0; c < numOfColumns; c++) {
                DataCell cell = row.getCell(c);
                DataValueComparator comp = cell.getType().getComparator();

                if (cell.isMissing()) {
                    // ignore missing values.
                    continue;
                }

                // test the min value
                if (m_minVal[c] == null) {
                    m_minVal[c] = cell;
                } else {
                    if (comp.compare(m_minVal[c], cell) > 0) {
                        m_minVal[c] = cell;
                    }
                }
                // test the max value
                if (m_maxVal[c] == null) {
                    m_maxVal[c] = cell;
                } else {
                    if (comp.compare(m_maxVal[c], cell) < 0) {
                        m_maxVal[c] = cell;
                    }
                }
                // add it to the possible values if we record them for this col
                LinkedHashSet<DataCell> possVals = m_possVals.get(c);
                if (possVals != null) {
                    // non-string cols have a null list and will be skipped here
                    possVals.add(cell);
                }
            } // for all columns in the row

            // see if user wants us to stop
            if (execMon != null) {
                // will throw an exception if we are supposed to cancel
                execMon.checkCanceled();
                execMon.setProgress((double)m_rows.size() / (double)numOfRows,
                        "reading rows " + m_firstRow + " to "
                                + (numOfRows + firstRow) + "(max.) ...");
            }

        } // while ((!rIter.atEnd()) && (numOfRowsRead < numOfRows))

        // make sure that the table spec's domain is set properly. 
        // Use as is when there is information available, otherwise set it.
        DataColumnSpec[] colSpecs = new DataColumnSpec[numOfColumns];
        boolean changed = false; // do we need to set our own table spec
        for (int i = 0; i < numOfColumns; i++) {
            boolean colChanged = false;
            DataColumnSpec origColSpec = tSpec.getColumnSpec(i);
            DataType type = origColSpec.getType();
            DataColumnSpecCreator creator = 
                new DataColumnSpecCreator(origColSpec);
            DataColumnDomain origColDomain = origColSpec.getDomain();
            DataColumnDomainCreator domainCreator = 
                new DataColumnDomainCreator(origColDomain);
            if (type.isCompatible(StringValue.class) 
                    && !origColDomain.hasValues()) {
                domainCreator.setValues(m_possVals.get(i));
                colChanged = true;
            }
            if (type.isCompatible(DoubleValue.class)) {
                if (!origColDomain.hasLowerBound()) {
                    domainCreator.setLowerBound(m_minVal[i]);
                    colChanged = true;
                }
                if (!origColDomain.hasUpperBound()) {
                    domainCreator.setUpperBound(m_maxVal[i]);
                    colChanged = true;
                }
            }
            if (colChanged) {
                changed = true;
                creator.setDomain(domainCreator.createDomain());
                colSpecs[i] = creator.createSpec();
            } else {
                colSpecs[i] = origColSpec;
            }
        } // for all columns
        if (changed) {
            m_tSpec = new DataTableSpec(colSpecs);
        } else {
            m_tSpec = tSpec;
        }
    }

    /**
     * Returns the row from the container with index <code>idx</code>. Index
     * starts at zero and must be less than the size of the container (which
     * could be less than the number of rows requested at construction time as
     * the table could be shorter than that). The original row number in the
     * table can be reconstructed by adding the index to the result of the
     * <code>getFirstRowNumber</code> method.
     * 
     * @param idx the index of the row to return (must be between 0 and size of
     *            the row container)
     * @return the row from the container with index <code>idx</code> or
     *         throws an IndexOutOfBounds exception.
     */
    public DataRow getRow(final int idx) {
        return m_rows.get(idx);

    }

    /**
     * returns a list of all different values seen in the specified column. Will
     * always return null if the idx doesn't specifiy a column of type
     * <code>StringCell</code> (or derived from that). The list will be in the
     * order the values appeared in the rows read in. It contains only the
     * values showing in these rows, the complete table may contain more values.
     * The list doesn't contain "missing value" cells.
     * 
     * @param colIdx the index of the column to return the possible values for
     * @return a list of possible values of the specified column in the order
     *         they appear in the rows read. The list includes only values seen
     *         in the rows stored in the container. Returns null for non-string
     *         columns.
     */
    public Set<DataCell> getValues(final int colIdx) {
        return m_possVals.get(colIdx);
    }

    /**
     * @param colIdx the index of the column to return the min value for
     * @return the minimum value seen in the specified column in the rows read
     *         in (the entire table could contain a smaller value). Or the min
     *         value set with the corresponding setter method. Will return null
     *         if the number of rows actually stored is zero, or the column
     *         contains only missing cells.
     */
    public DataCell getMinValue(final int colIdx) {
        return m_minVal[colIdx];
    }

    /**
     * @param colIdx the index of the column to return the max value for
     * @return the maximum value seen in the specified column in the rows read
     *         in (the entire table could contain a larger value). Or the max
     *         value set with the corresponding setter method. Will return null
     *         if the number of rows actually stored is zero, or the column
     *         contains only missing cells.
     */
    public DataCell getMaxValue(final int colIdx) {
        return m_maxVal[colIdx];
    }

    /**
     * Sets a new max value for the specified column.
     * 
     * @param colIdx the index of the column to set the new max value for.
     * @param newMaxValue the new max value for the specified column. Must not
     *            be null. Must fit the type of the column.
     */
    public void setMaxValue(final int colIdx, final DataCell newMaxValue) {
        if (newMaxValue == null) {
            throw new NullPointerException("The minValue must not be null");
        }
        if (!m_tSpec.getColumnSpec(colIdx).getType().isASuperTypeOf(
                newMaxValue.getType())) {
            throw new IllegalArgumentException(
                    "new max value is of wrong type");
        }
        m_maxVal[colIdx] = newMaxValue;
    }

    /**
     * Sets a new min value for the specified column.
     * 
     * @param colIdx the index of the column to set the new min value for. Must
     *            be between zero and the size of this container.
     * @param newMinValue the new min value for the specified column. Must not
     *            be null. Must fit the type of the column.
     */
    public void setMinValue(final int colIdx, final DataCell newMinValue) {
        if (newMinValue == null) {
            throw new NullPointerException("The maxValue must not be null");
        }
        if (!m_tSpec.getColumnSpec(colIdx).getType().isASuperTypeOf(
                newMinValue.getType())) {
            throw new IllegalArgumentException(
                    "new min value is of wrong type");
        }
        m_minVal[colIdx] = newMinValue;
    }

    /**
     * @return the size of the container, i.e. the number of rows actually
     *         stored. Could be different from the number fo rows requested, if
     *         the table is shorter than the sum of the first row and the number
     *         of rows specified to the constructor.
     */
    public int size() {
        return m_rows.size();
    }

    /**
     * @return the number of the row with index 0 - i.e. the original row number
     *         in the underlying data table of any row with index i in the
     *         container can be reconstructed by i + getFirstRowNumber().
     */
    public int getFirstRowNumber() {
        return m_firstRow;
    }

    /**
     * @return an iterator to traverse the container. Unfortunately the iterator
     *         returns objects, i.e. you would have to use a typecast to
     *         <code>DataRow</code> to obtain the real type of the object.
     */
    public Iterator<DataRow> iterator() {
        return m_rows.iterator();
    }

    /**
     * @see RowContainer#getTableSpec()
     */
    public DataTableSpec getTableSpec() {
        return m_tSpec;
    }

}
