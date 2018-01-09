/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   09.03.2005 (ohl): created
 */
package org.knime.base.node.util;

import java.util.ArrayList;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;
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
            throw new IllegalArgumentException("Must provide non-null data table"
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
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tSpec, true);
        int numOfColumns = tSpec.getNumColumns();

        m_firstRow = firstRow;
        m_rows = new ArrayList<DataRow>(numOfColumns);

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
            domainCreator.updateDomain(row);

            // see if user wants us to stop
            if (execMon != null) {
                // will throw an exception if we are supposed to cancel
                execMon.checkCanceled();
                execMon.setProgress((double)m_rows.size()
                            / (double)numOfRows, "read row " + m_rows.size()
                            + " of max. " + numOfRows);
            }

        } // while ((!rIter.atEnd()) && (numOfRowsRead < numOfRows))

        if (rIter instanceof CloseableRowIterator) {
            ((CloseableRowIterator)rIter).close();
        }
        m_tSpec = domainCreator.createSpec();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow getRow(final int idx) {
        return m_rows.get(idx);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<DataCell> getValues(final int colIdx) {
        return m_tSpec.getColumnSpec(colIdx).getDomain().getValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getMinValue(final int colIdx) {
        return m_tSpec.getColumnSpec(colIdx).getDomain().getLowerBound();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getMaxValue(final int colIdx) {
        return m_tSpec.getColumnSpec(colIdx).getDomain().getUpperBound();
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
            throw new IllegalArgumentException("The new maximum value must not be null");
        }
        if (!m_tSpec.getColumnSpec(colIdx).getType().isASuperTypeOf(newMaxValue.getType())) {
            throw new IllegalArgumentException("new maximum value is of wrong type");
        }

        DataColumnSpec[] colSpecs = new DataColumnSpec[m_tSpec.getNumColumns()];
        for (int i = 0; i < colSpecs.length; i++) {
            colSpecs[i] = m_tSpec.getColumnSpec(i);
        }

        DataColumnSpecCreator sCrea = new DataColumnSpecCreator(colSpecs[colIdx]);
        DataColumnDomainCreator dCrea = new DataColumnDomainCreator(colSpecs[colIdx].getDomain());
        dCrea.setUpperBound(newMaxValue);
        sCrea.setDomain(dCrea.createDomain());

        colSpecs[colIdx] = sCrea.createSpec();
        m_tSpec = new DataTableSpec(m_tSpec.getName(), colSpecs);
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
            throw new IllegalArgumentException("The new minimum value must not be null");
        }
        if (!m_tSpec.getColumnSpec(colIdx).getType().isASuperTypeOf(newMinValue.getType())) {
            throw new IllegalArgumentException("new minimum value is of wrong type");
        }

        DataColumnSpec[] colSpecs = new DataColumnSpec[m_tSpec.getNumColumns()];
        for (int i = 0; i < colSpecs.length; i++) {
            colSpecs[i] = m_tSpec.getColumnSpec(i);
        }

        DataColumnSpecCreator sCrea = new DataColumnSpecCreator(colSpecs[colIdx]);
        DataColumnDomainCreator dCrea = new DataColumnDomainCreator(colSpecs[colIdx].getDomain());
        dCrea.setLowerBound(newMinValue);
        sCrea.setDomain(dCrea.createDomain());

        colSpecs[colIdx] = sCrea.createSpec();
        m_tSpec = new DataTableSpec(m_tSpec.getName(), colSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return m_rows.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFirstRowNumber() {
        return m_firstRow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowIterator iterator() {
        return new DefaultRowIterator(m_rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_tSpec;
    }
}
