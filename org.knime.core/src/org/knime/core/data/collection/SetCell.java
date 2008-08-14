/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   13.08.2008 (ohl): created
 */
package org.knime.core.data.collection;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;

/**
 * Default implementation of a {@link CollectionDataValue}, whereas the
 * underlying data structure is a set (i.e. there won't be duplicates stored in
 * the collection).
 *
 * @author ohl, University of Konstanz
 */
public class SetCell extends DataCell implements CollectionDataValue {

    /**
     * Convenience method to determine the type of collection. This is a
     * shortcut for <code>DataType.getType(SetCell.class, elementType)</code>.
     *
     * @param elementType The type of the elements
     * @return a DataType representing the collection.
     */
    public static final DataType getCollectionType(final DataType elementType) {
        return DataType.getType(SetCell.class, elementType);
    }

    private static final DataCellSerializer<SetCell> SERIALIZER =
            new SetCellSerializer();

    /**
     * Get serializer as required by {@link DataCell}.
     *
     * @return the serializer.
     */
    public static final DataCellSerializer<SetCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final BlobSupportDataCellSet m_set;

    /**
     * Factory method to create a data cell set based on a collection.
     * <p>
     * If the underlying collection stems from a {@link DataRow} (as read from a
     * any table), consider to use {@link #create(DataRow, int[])} in order to
     * minimize cell access.
     *
     * @param coll The underlying collection.
     * @return The newly created set cell.
     * @throws NullPointerException If the argument is null or contains null
     *             values.
     */
    public static DataCell create(final Collection<DataCell> coll) {
        BlobSupportDataCellSet l = BlobSupportDataCellSet.create(coll);
        return new SetCell(l);
    }

    /**
     * Create new set based on selected cell from a {@link DataRow}. Using this
     * method will check if the row is returned by a {@link BufferedDataTable}
     * and will handle blobs appropriately.
     *
     * @param row The underlying row
     * @param cols The indices of cells to be stored in the set.
     * @return A newly created set.
     * @throws NullPointerException If either argument is null.
     * @throws IndexOutOfBoundsException If the indices are invalid.
     */
    public static DataCell create(final DataRow row, final int[] cols) {
        BlobSupportDataCellSet l = BlobSupportDataCellSet.create(row, cols);
        return new SetCell(l);
    }

    /**
     * Rather use one of the factory methods.
     *
     * @param cellSet the set that will be taken over.
     * @see #create(Collection)
     * @see #create(DataRow, int[])
     */
    protected SetCell(final BlobSupportDataCellSet cellSet) {
        m_set = cellSet;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsBlobWrapperCells() {
        return m_set.containsBlobWrapperCells();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return m_set.equals(((SetCell)dc).m_set);
    }

    /**
     * {@inheritDoc}
     */
    public DataType getElementType() {
        return m_set.getElementType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_set.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return m_set.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return m_set.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_set.toString();
    }

    /**
     * @return the set
     */
    protected BlobSupportDataCellSet getSet() {
        return m_set;
    }

    /*
     * --------- the serializer -------------------------------------------
     */
    private static final class SetCellSerializer implements
            DataCellSerializer<SetCell> {

        /** {@inheritDoc} */
        @Override
        public SetCell deserialize(final DataCellDataInput input)
                throws IOException {
            return new SetCell(BlobSupportDataCellSet.deserialize(input));
        }

        /** {@inheritDoc} */
        @Override
        public void serialize(final SetCell cell,
                final DataCellDataOutput output) throws IOException {
            cell.m_set.serialize(output);
        }
    }

}
