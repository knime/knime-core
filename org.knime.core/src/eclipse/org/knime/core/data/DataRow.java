/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   09.01.2006(all): reviewed
 *   02.11.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

/**
 * Container interface for a vector of {@link DataCell}s and a row key for
 * unique identification.
 * 
 * <p>
 * Each <code>DataRow</code> represents one row of a {@link DataTable} and
 * contains a fixed number of {@link DataCell} elements which are directly
 * accessible and read-only. In addition, each <code>DataRow</code> contains a
 * unique identifier key (which is not part of the data vector).
 * <p>
 * A <code>DataRow</code> must not contain a <code>null</code> element or a
 * <code>null</code> key.
 * 
 * <p>
 * This <code>DataRow</code> interface extends the {@link Iterable} interface
 * but does not allow the removal of {@link DataCell}s. Implementors must
 * therefore throw an {@link UnsupportedOperationException} in the Iterators
 * remove method.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see DataTable
 * @see DataCell
 * @see RowIterator
 * @see RowKey
 */
public interface DataRow extends Iterable<DataCell> {

    /**
     * Returns the length of this row, that is the number of columns of the
     * DataTable (not including the row key).
     * 
     * @return length of this row
     */
    int getNumCells();

    /**
     * Returns the row key.
     * 
     * @return the row key
     */
    RowKey getKey();

    /**
     * Returns the {@link DataCell} at the provided index within this row.
     * 
     * @param index the index of the cell to retrieve (indices start from 0)
     * @return the {@link DataCell} at the given index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    DataCell getCell(final int index);
}
