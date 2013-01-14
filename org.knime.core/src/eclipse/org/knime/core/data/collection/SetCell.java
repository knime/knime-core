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

/**
 * Default implementation of a {@link CollectionDataValue}, whereas the
 * underlying data structure is a set (i.e. there won't be duplicates stored in
 * the collection).
 *
 * @author ohl, University of Konstanz
 */
public class SetCell extends DataCell implements SetDataValue {

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
     * Rather use one of the factory methods.
     *
     * @param cellSet the set that will be taken over.
     * @see CollectionCellFactory#createSetCell(Collection)
     * @see CollectionCellFactory#createSetCell(DataRow, int[])
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
    @Override
    public boolean contains(final DataCell cell) {
        return m_set.contains(cell);
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
