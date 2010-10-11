/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 */

package org.knime.core.data.collection;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.container.BlobWrapperDataCell;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;


/**
 * Sparse implementation of a {@link CollectionDataValue}. The class
 * stores a default value and a mapping of indices and values unequal to the
 * default value.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SparseListCell extends DataCell implements SparseListDataValue {

    private static final DataCellSerializer<SparseListCell> SERIALIZER =
            new SparseListCellSerializer();

    private static final class SparseListCellSerializer implements
            DataCellSerializer<SparseListCell> {

        /** {@inheritDoc} */
        @Override
        public SparseListCell deserialize(final DataCellDataInput input)
                throws IOException {
            //read the list size
            final int size = input.readInt();
            //read the index array
            final int length = input.readInt();
            final int[] elementIdxs = new int[length];
            for (int i = 0; i < length; i++) {
                elementIdxs[i] = input.readInt();
            }
            //read the default element
            final DataCell defaultElement = input.readDataCell();
            //read the value list
            final BlobSupportDataCellList elements =
                BlobSupportDataCellList.deserialize(input);
            return new SparseListCell(size, elements,
                    elementIdxs, defaultElement);
        }

        /** {@inheritDoc} */
        @SuppressWarnings("synthetic-access")
        @Override
        public void serialize(final SparseListCell cell,
                final DataCellDataOutput output) throws IOException {
            final int size = cell.size();
            final DataCell defaultElement = cell.getDefaultElement();
            final BlobSupportDataCellList list = cell.getValueList();
            //write list length
            output.writeInt(size);
            //index array
            output.writeInt(cell.m_idxs.length);
            for (int i = 0; i < cell.m_idxs.length; i++) {
                output.writeInt(cell.m_idxs[i]);
            }
            //write the default element
            output.writeDataCell(defaultElement);
            //write the value list
            list.serialize(output);
        }
    }

    /**
     * Convenience method to determine the type of collection. This is a
     * shortcut for <code>DataType.getType(ListCell.class, elementType)</code>.
     *
     * @param elementType The type of the elements
     * @return a DataType representing the collection.
     */
    public static DataType getCollectionType(final DataType elementType) {
        return DataType.getType(SparseListCell.class, elementType);
    }

    /**
     * Get serializer as required by {@link DataCell}.
     *
     * @return Such a serializer.
     */
    public static DataCellSerializer<SparseListCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final BlobSupportDataCellList m_list;

    private final int[] m_idxs;

    private final int m_length;

    private final DataCell m_defaultElement;

    private final DataType m_elementType;

    /**Constructor for class SparseListCell.
     * @param size the size of the vector.
 *                  Indices must be smaller than this number.
     * @param elements the elements unequal to the default element
     * @param elementIdxs the array containing the indices of the elements to
     *            store. MUST be sorted (lowest index first).
     * @param defaultElement the element that should be returned as default
 *                          value if no value is set
     *
     * @throws IllegalArgumentException if length is negative or if the array
     *             contains negative indices or indices larger than length - or
     *             if the array is not sorted or the arrays do not have the same
     *             length or the default element is <code>null</code>!
     */
    protected SparseListCell(final int size,
            final BlobSupportDataCellList elements, final int[] elementIdxs,
            final DataCell defaultElement) {
        if (elements.size() != elementIdxs.length) {
            throw new IllegalArgumentException(
                "Number of indices must be equal to the number of elements");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        if (size < elementIdxs.length) {
            throw new IllegalArgumentException(
                    "Length must not be less the number of indices");
        }
        if (defaultElement == null) {
            throw new IllegalArgumentException(
                    "Default element must not be null");
        }
        //loop over all indexes to check that they are valid and ordered
        m_idxs = new int[elementIdxs.length];
        int idx = -1;
        int lastVal = -1;
        final int maxArrayIdx = elementIdxs.length - 1;
        while (idx < maxArrayIdx) {
            idx++;
            if (elementIdxs[idx] >= size) {
                throw new IllegalArgumentException("Initialization array"
                        + " contains index out of range at array index " + idx
                        + " (vector length=" + size + ", index="
                        + elementIdxs[idx] + ")");
            }
            if (elementIdxs[idx] < 0) {
                throw new IllegalArgumentException("Initialization array"
                        + " contains a negative index at array index " + idx
                        + "(index=" + elementIdxs[idx] + ")");
            }
            if (elementIdxs[idx] <= lastVal) {
                throw new IllegalArgumentException("Initialization array"
                        + " is not sorted at array index " + idx
                        + " (previousVal=" + lastVal + ", indexVal="
                        + elementIdxs[idx] + ")");
            }
            m_idxs[idx] = elementIdxs[idx];
            lastVal = elementIdxs[idx];
        }
        m_length = size;
        m_list = elements;
        m_defaultElement = defaultElement;
        m_elementType = DataType.getCommonSuperType(m_defaultElement.getType(),
                elements.getElementType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getElementType() {
        return m_elementType;
    }

    /**
     * @param idx the index in the index array to get the stored index for
     * @return the index at the given position in the index array
     */
    protected int getIdx(final int idx) {
        return m_idxs[idx];
    }

    /**
     * @return the number of explicitly stored values
     */
    protected int getIdxLength() {
        return m_idxs.length;
    }

    /**
     * @return the value list
     */
    protected BlobSupportDataCellList getValueList() {
        return m_list;
    }


    /**
     * @return the indices that corresponds to the values in the value list
     */
    protected int[] getIdxs() {
        return m_idxs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell get(final int index) {
        if (index >= m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + index
                    + "') too large for vector of length " + m_length);
        }
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("Index can't be negative");
        }
        if (m_idxs[0] > index
                || m_idxs[m_idxs.length - 1] < index) {
            // no value set yet - or only higher or lower indices
            return m_defaultElement;
        }
        final int storageIdx =
                Arrays.binarySearch(m_idxs, 0, m_idxs.length, index);
        if (storageIdx < 0) {
            // no element set at that position
            return m_defaultElement;
        }
        return m_list.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getDefaultElement() {
        return m_defaultElement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<DataCell> iterator() {
        return new SparseListCellIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return m_length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsBlobWrapperCells() {
        return m_list.containsBlobWrapperCells()
                || (m_defaultElement instanceof BlobWrapperDataCell)
                || (m_defaultElement instanceof BlobDataCell);
    }

    /**
     * Returns a copy of the array that contains all indices of the explicitly
     * set values of this list. They are sorted in increasing order.
     *
     * @return all indices that have a explicitly set value attached
     */
    @Override
    public int[] getAllIndices() {
        return Arrays.copyOf(m_idxs, m_idxs.length);
    }

    private class SparseListCellIterator implements Iterator<DataCell> {
        private int m_iterIdx = 0;
        private int m_realIdxIdx = 0;
        private int m_nextRealIdx = -1;

        /**Constructor for class SparseListCell.SparseListCellIterator.
         *
         */
        public SparseListCellIterator() {
            //initialize the the real index index with the first
            //position of the explicitly set indices
            if (m_realIdxIdx < getIdxLength()) {
                m_nextRealIdx = getIdx(m_realIdxIdx++);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            if (m_iterIdx < size()) {
                return true;
            }
            return false;
        }
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public DataCell next() {
            //check if the current iterator index is a explicitly set one
            if (m_iterIdx++ == m_nextRealIdx) {
                //if that's the case get the explicitly set value for the
                //index
                final DataCell result = m_list.get(m_realIdxIdx - 1);
                //check if we have another explicitly set index in the
                //index array
                if (m_realIdxIdx < getIdxLength()) {
                    //if that's the case get the index of the explicitly
                    //set value
                    m_nextRealIdx = getIdx(m_realIdxIdx++);
                }
                return result;
            }
            //if the current iterator index is not related to an explicitly
            //set value return the default element
            return getDefaultElement();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (dc == this) {
            return true;
        }
        final SparseListCell slc = (SparseListCell)dc;
        if (!m_defaultElement.equals(slc.getDefaultElement())) {
            return false;
        }
        if (!Arrays.equals(m_idxs, slc.getIdxs())) {
            return false;
        }
        if (!m_list.equals(slc.getValueList())) {
            return false;
        }
        return true;
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
}
