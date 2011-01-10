/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
