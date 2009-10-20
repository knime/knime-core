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
