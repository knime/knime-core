/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 5, 2008 (wiswedel): created
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

/**
 * Default implementation of a {@link CollectionDataValue}, whereby the
 * underlying data structure is a list.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ListCell extends DataCell implements ListDataValue {

    /**
     * Convenience method to determine the type of collection. This is a
     * shortcut for <code>DataType.getType(ListCell.class, elementType)</code>.
     *
     * @param elementType The type of the elements
     * @return a DataType representing the collection.
     */
    public static final DataType getCollectionType(final DataType elementType) {
        return DataType.getType(ListCell.class, elementType);
    }

    private final BlobSupportDataCellList m_list;

    private static final DataCellSerializer<ListCell> SERIALIZER =
            new ListCellSerializer();

    /**
     * Get serializer as required by {@link DataCell}.
     *
     * @return Such a serializer.
     */
    public static final DataCellSerializer<ListCell> getCellSerializer() {
        return SERIALIZER;
    }

    /**
     * Rather use one of the factory methods.
     *
     * @param list the list that will be taken over.
     * @see CollectionCellFactory#createListCell(Collection)
     * @see CollectionCellFactory#createListCell(DataRow, int[])
     */
    protected ListCell(final BlobSupportDataCellList list) {
        m_list = list;
    }

    /** {@inheritDoc} */
    @Override
    public DataType getElementType() {
        return m_list.getElementType();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<DataCell> iterator() {
        return m_list.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell get(final int index) {
        return m_list.get(index);
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return m_list.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsBlobWrapperCells() {
        return m_list.containsBlobWrapperCells();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (dc == this) {
            return true;
        }
        return m_list.equals(((ListCell)dc).m_list);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_list.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_list.toString();
    }

    /**
     * @return the list
     */
    protected BlobSupportDataCellList getList() {
        return m_list;
    }


    private static final class ListCellSerializer implements
            DataCellSerializer<ListCell> {

        /** {@inheritDoc} */
        @Override
        public ListCell deserialize(final DataCellDataInput input)
                throws IOException {
            return new ListCell(BlobSupportDataCellList.deserialize(input));
        }

        /** {@inheritDoc} */
        @Override
        public void serialize(final ListCell cell,
                final DataCellDataOutput output) throws IOException {
            cell.m_list.serialize(output);
        }
    }

}
