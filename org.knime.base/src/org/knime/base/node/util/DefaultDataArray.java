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
 *   09.03.2005 (ohl): created
 */
package org.knime.base.node.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

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
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;


/**
 * Can be used to locally store a certain number of rows. It provides random
 * access to the stored rows. It maintains the min and max value for each column
 * (min/max with respect to the row sample stored - not the entire data table).
 * These values can be changed, in case somebody knows better limits. It
 * provides a list of all values seen for each string column (i.e. a list of all
 * values appearing in the rows stored - not the entire data table).
 * If the maximal number of possible values (2000) is exceeded, no possible
 * values are available.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class DefaultDataArray implements DataArray {

    /* this is where we store the rows. */
    private ArrayList<DataRow> m_rows;

    /* all occurring values for each string column */
    private Vector<LinkedHashSet<DataCell>> m_possVals;

    /* the max value for each column */
    private DataCell[] m_maxVal;

    /* the min values for each column */
    private DataCell[] m_minVal;

    /* the first row we've stored */
    private int m_firstRow;

    private static final int MAX_POSS_VALUES = 2000;

    private boolean[] m_ignoreCols;

    /*
     * we store the table spec - in case somebody needs name and type of the
     * stored rows
     */
    private DataTableSpec m_tSpec;

    /**
     * Constructs a random access container holding a certain number of rows
     * from the data table passed in. It will store the specified amount of rows
     * starting from the row specified in the "<code>firstRow</code>"
     * parameter (where the first row is number 1). The rows can be accessed by
     * index later on always starting with index zero.
     *
     * @param dTable the data table to read the rows from
     * @param firstRow the first row to store (must be greater than zero)
     * @param numOfRows the number of rows to store (must be zero or more)
     */
    public DefaultDataArray(final DataTable dTable, final int firstRow,
            final int numOfRows) {
        try {
            init(dTable, firstRow, numOfRows, null);
        } catch (CanceledExecutionException cee) {
            // won't happen as we pass a null execMonitor...
        }
    }

    /**
     * Same, but allows for user cancellation from a progress monitor, while the
     * container is filled.
     *
     * @param dTable the data table to read the rows from
     * @param firstRow the first row to store (must be greater than zero)
     * @param numOfRows the number of rows to store (must be zero or more)
     * @param execMon the object listening to our progress and providing cancel
     *            functionality
     * @throws CanceledExecutionException if the construction was canceled
     */
    public DefaultDataArray(final DataTable dTable, final int firstRow,
            final int numOfRows, final ExecutionMonitor execMon)
            throws CanceledExecutionException {
        init(dTable, firstRow, numOfRows, execMon);
    }

    private void init(final DataTable dTable, final int firstRow,
            final int numOfRows, final ExecutionMonitor execMon)
            throws CanceledExecutionException {
        if (dTable == null) {
            throw new NullPointerException("Must provide non-null data table"
                    + " for DataArray");
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
        m_ignoreCols = new boolean[numOfColumns];
        for (int c = 0; c < numOfColumns; c++) {
            m_ignoreCols[c] = false;
            if (tSpec.getColumnSpec(c).getType()
                    .isCompatible(NominalValue.class)) {
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

                if (cell.isMissing()) {
                    // ignore missing values.
                    continue;
                }

                if (m_ignoreCols[c]) {
                    continue;
                }

                DataValueComparator comp =
                    tSpec.getColumnSpec(c).getType().getComparator();

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
                    // now check if we have more than MAX_POSS_VALUES values
                    if (possVals.size() > MAX_POSS_VALUES) {
                        m_possVals.add(c, null);
                        m_minVal[c] = null;
                        m_maxVal[c] = null;
                        m_ignoreCols[c] = true;
                    }
                }
            } // for all columns in the row

            // see if user wants us to stop
            if (execMon != null) {
                // will throw an exception if we are supposed to cancel
                execMon.checkCanceled();
                execMon.setProgress((double)m_rows.size()
                            / (double)numOfRows, "read row " + m_rows.size()
                            + " of max. " + numOfRows);
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
            DataColumnSpecCreator creator = new DataColumnSpecCreator(
                    origColSpec);
            DataColumnDomain origColDomain = origColSpec.getDomain();
            DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(
                    origColDomain);
            if (type.isCompatible(NominalValue.class)
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
     * {@inheritDoc}
     */
    public DataRow getRow(final int idx) {
        return m_rows.get(idx);

    }

    /**
     * {@inheritDoc}
     */
    public Set<DataCell> getValues(final int colIdx) {
        return m_possVals.get(colIdx);
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getMinValue(final int colIdx) {
        return m_minVal[colIdx];
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getMaxValue(final int colIdx) {
        return m_maxVal[colIdx];
    }

    /**
     * Sets a new max value for the specified column.
     *
     * @param colIdx the index of the column to set the new max value for
     * @param newMaxValue the new max value for the specified column. Must not
     *            be <code>null</code> and must fit the type of the column.
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
     *            be <code>null</code> and must fit the type of the column.
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
     * {@inheritDoc}
     */
    public int size() {
        return m_rows.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getFirstRowNumber() {
        return m_firstRow;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return new DefaultRowIterator(m_rows);
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_tSpec;
    }
}
