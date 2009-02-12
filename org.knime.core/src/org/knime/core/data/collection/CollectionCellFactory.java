/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
    public static ListCell createListCell(final Collection<DataCell> coll) {
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
    public static SetCell createSetCell(final Collection<DataCell> coll) {
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
