/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.08.2008 (ohl): created
 */
package org.knime.core.data.collection;

import java.util.Collection;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;

/**
 * Factory class used to create {@link DataCell}s that contain a collection of
 * {@link DataCell}s. Also provides convenient methods to determine the type of
 * the elements (the common super type), if no such collection cell is at hand.
 *
 * @author ohl, University of Konstanz
 */
public final class CollectionCellFactory {

    private CollectionCellFactory() {
        // don't instantiate me
    }

    /**
     * Factory method to create a {@link ListCell} based on a collection.
     * <p>
     * If the underlying collection stems from a {@link DataRow} (as read from a
     * any table), consider to use {@link #createListCell(DataRow, int[])} in
     * order to minimize cell access.
     *
     * @param coll The underlying collection.
     * @return The newly created {@link ListCell}.
     * @throws NullPointerException If the argument is null or contains null
     *             values.
     */
    public static ListCell createListCell(
            final Collection<? extends DataCell> coll) {
        BlobSupportDataCellList l = BlobSupportDataCellList.create(coll);
        return new ListCell(l);
    }

    /**
     * Creates a new {@link ListCell} based on selected cells from a
     * {@link DataRow}. Using this method will check if the row is returned by
     * a {@link BufferedDataTable} and will handle blobs appropriately.
     *
     * @param row The underlying row
     * @param cols The indices of interest.
     * @return A newly created {@link ListCell}.
     * @throws NullPointerException If either argument is null.
     * @throws IndexOutOfBoundsException If the indices are invalid.
     */
    public static ListCell createListCell(final DataRow row, final int[] cols) {
        BlobSupportDataCellList l = BlobSupportDataCellList.create(row, cols);
        return new ListCell(l);
    }

    /**
     * Factory method to create a {@link SetCell} that contains a data cell set
     * based on a collection.
     * <p>
     * If the underlying collection stems from a {@link DataRow} (as read from a
     * any table), consider to use {@link #createSetCell(DataRow, int[])} in
     * order to minimize cell access.
     *
     * @param coll The underlying collection.
     * @return The newly created {@link SetCell}.
     * @throws NullPointerException If the argument is null or contains null
     *             values.
     */
    public static SetCell createSetCell(
            final Collection<? extends DataCell> coll) {
        BlobSupportDataCellSet l = BlobSupportDataCellSet.create(coll);
        return new SetCell(l);
    }

    /**
     * Create new {@link SetCell} containing a set based on selected cell from a
     * {@link DataRow}. Using this method will check if the row is returned by
     * a {@link BufferedDataTable} and will handle blobs appropriately.
     *
     * @param row The underlying row
     * @param cols The indices of cells to be stored in the set.
     * @return A newly created {@link SetCell}.
     * @throws NullPointerException If either argument is null.
     * @throws IndexOutOfBoundsException If the indices are invalid.
     */
    public static SetCell createSetCell(final DataRow row, final int[] cols) {
        BlobSupportDataCellSet l = BlobSupportDataCellSet.create(row, cols);
        return new SetCell(l);
    }

    /**
     * Determines the super type of the specified columns. This type will be the
     * element type if a collection is created from the elements in the
     * specified columns.
     *
     * @param tableSpec containing the types of the selected columns
     * @param cols the indices of the columns to determine the common super type
     *            of
     * @return the common super type of the specified columns in the table spec
     * @throws NullPointerException if either of the arguments is null
     * @throws ArrayIndexOutOfBoundsException if the provided column indices are
     *             invalid.
     */
    public static DataType getElementType(final DataTableSpec tableSpec,
            final int[] cols) {
        DataType[] colType = new DataType[cols.length];
        for (int i = 0; i < cols.length; i++) {
            DataColumnSpec colSpec = tableSpec.getColumnSpec(cols[i]);
            colType[i] = colSpec.getType();
        }
        return getElementType(colType);
    }

    /**
     * Determines the super type of all types passed. This type will be the
     * element type if a collection is created from element of this type.
     *
     * @param colType the types of which the common super type should be
     *            returned.
     *
     * @return the common super type of the specified types
     * @throws NullPointerException if the array or one of its elements is null
     */
    public static DataType getElementType(final DataType[] colType) {
        DataType result = null;
        for (DataType cT : colType) {
            if (result == null) {
                result = cT;
            } else {
                result = DataType.getCommonSuperType(result, cT);
            }
        }
        if (result == null) {
            DataType.getType(DataCell.class);
        }
        return result;
    }
}
