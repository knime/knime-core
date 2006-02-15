/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   02.05.2005 (bernd): created
 */
package de.unikn.knime.base.node.util;

import java.util.Iterator;
import java.util.List;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;

/**
 * Can be used to locally store a certain number of rows. It provides random
 * access to the stored rows. It maintains the min and max value for each column
 * (min/max with respect to the row sample stored - not the entire data table).
 * It provides a list of all values seen for each string column (i.e. a list of
 * all values appearing in the rows stored - not the entire data table).
 * 
 * @author Peter Ohl, University of Konstanz
 */
public interface RowContainer extends Iterable {
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
    DataRow getRow(final int idx);

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
    List<DataCell> getValues(final int colIdx);

    /**
     * @param colIdx the index of the column to return the min value for
     * @return the minimum value seen in the specified column in the rows read
     *         in (the entire table could contain a smaller value). Or the min
     *         value set with the corresponding setter method. Will return null
     *         if the number of rows actually stored is zero, or the column
     *         contains only missing cells.
     */
    DataCell getMinValue(final int colIdx);

    /**
     * @param colIdx the index of the column to return the max value for
     * @return the maximum value seen in the specified column in the rows read
     *         in (the entire table could contain a larger value). Or the max
     *         value set with the corresponding setter method. Will return null
     *         if the number of rows actually stored is zero, or the column
     *         contains only missing cells.
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
     *         in the underlying data table of any row with index i in the
     *         container can be reconstructed by i + getFirstRowNumber().
     */
    int getFirstRowNumber();

    /**
     * @return an iterator to traverse the container. Unfortunately the iterator
     *         returns objects, i.e. you would have to use a typecast to
     *         <code>DataRow</code> to obtain the real type of the object.
     */
    Iterator<DataRow> iterator();

    /**
     * @return the table spec belonging to the rows stored.
     */
    DataTableSpec getTableSpec();
}
