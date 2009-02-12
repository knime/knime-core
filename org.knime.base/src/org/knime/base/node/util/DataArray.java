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
 *   02.05.2005 (bernd): created
 */
package org.knime.base.node.util;

import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;


/**
 * Can be used to locally store a certain number of rows. It provides random
 * access to the stored rows. It maintains the min and max value for each column
 * (min/max with respect to the row sample stored - not the entire data table).
 * It provides a list of all values seen for each string column (i.e. a list of
 * all values appearing in the rows stored - not the entire data table).
 * 
 * @author Peter Ohl, University of Konstanz
 */
public interface DataArray extends DataTable, Iterable<DataRow> {
    /**
     * Returns the row from the container with index <code>idx</code>. Index
     * starts at zero and must be less than the size of the container (which
     * could be less than the number of rows requested at construction time as
     * the table could be shorter than that). The original row number in the
     * table can be reconstructed by adding the index to the result of the
     * {@link #getFirstRowNumber()} method.
     * 
     * @param idx the index of the row to return (must be between 0 and size of
     *            the row container)
     * @return the row from the container with index <code>idx</code>
     * @throws IndexOutOfBoundsException if the row does not exist
     */
    DataRow getRow(final int idx);

    /**
     * Returns a set of all different values seen in the specified column. Will
     * always return null if the idx doesn't specifiy a column of type
     * {@link org.knime.core.data.def.StringCell} (or derived from that).
     * The list will be in the order the values appeared in the rows read in. It
     * contains only the values showing in these rows, the complete table may
     * contain more values. The list doesn't contain "missing value" cells.
     * 
     * @param colIdx the index of the column to return the possible values for
     * @return a list of possible values of the specified column in the order
     *         they appear in the rows read. The list includes only values seen
     *         in the rows stored in the container. Returns <code>null</code>
     *         for non-string columns.
     */
    Set<DataCell> getValues(final int colIdx);

    /**
     * @param colIdx the index of the column to return the min value for
     * @return the minimum value seen in the specified column in the rows read
     *         in (the entire table could contain a smaller value). Or the min
     *         value set with the corresponding setter method. Will return
     *         <code>null</code> if the number of rows actually stored is
     *         zero, or the column contains only missing cells.
     */
    DataCell getMinValue(final int colIdx);

    /**
     * @param colIdx the index of the column to return the max value for
     * @return the maximum value seen in the specified column in the rows read
     *         in (the entire table could contain a larger value). Or the max
     *         value set with the corresponding setter method. Will return
     *         <code>null</code> if the number of rows actually stored is
     *         zero, or the column contains only missing cells.
     */
    DataCell getMaxValue(final int colIdx);

    /**
     * @return the size of the container, i.e. the number of rows actually
     *         stored. Could be different from the number fo rows requested, if
     *         the table is shorter than the sum of the first row and the number
     *         of rows specified to the constructor.
     */
    int size();

    /**
     * @return the number of the row with index 0 - i.e. the original row number
     *         in the underlying data table of any row with index <code>i</code>
     *         in the container can be reconstructed by
     *         <code>i + getFirstRowNumber()</code>.
     */
    int getFirstRowNumber();

    /**
     * @return an iterator to traverse the container. Unfortunately the iterator
     *         returns objects, i.e. you would have to use a typecast to
     *         {@link DataRow} to obtain the real type of the object.
     */
    RowIterator iterator();

    /**
     * Get the table spec corresponding the the rows. The domain information is
     * ensured to be included, i.e. for all string compatible columns it
     * contains the possible values and for all double compatible columns it
     * contains lower and upper bounds.
     * 
     * @return the table spec belonging to the rows stored.
     */
    DataTableSpec getDataTableSpec();
}
