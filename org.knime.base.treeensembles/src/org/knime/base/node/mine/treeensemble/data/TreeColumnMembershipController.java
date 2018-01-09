/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   22.10.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Adrian Nembach
 */
public class TreeColumnMembershipController {

    private String m_colName;

    private int m_colIndex;

    private Map<Integer, Integer> m_original2InternalIndex;

    private IndexTuple[] m_indexTuples;

    private int m_nextInternalIndex;

    /**
     * @param column
     * @param sampleWeights
     */
    public TreeColumnMembershipController(final TreeAttributeColumnData column, final double[] sampleWeights) {
        m_colName = column.getMetaData().getAttributeName();
        m_colIndex = column.getMetaData().getAttributeIndex();
        int[] indexInOriginalColumnList = column.getOriginalIndicesInColumnList();

        ArrayList<IndexTuple> indexTupleList = new ArrayList<IndexTuple>();
//        m_indexTuples = new IndexTuple[indexInOriginalColumnList.length];
        m_original2InternalIndex = new HashMap<Integer, Integer>();

        for (int i = 0; i < indexInOriginalColumnList.length; i++) {
            int originalIndex = indexInOriginalColumnList[i];
            if (sampleWeights[originalIndex] > TreeColumnData.EPSILON) {
                m_original2InternalIndex.put(originalIndex, indexTupleList.size());
                IndexTuple indexTuple = new IndexTuple();
                indexTuple.m_originalIndex = originalIndex;
                indexTuple.m_indexInColumn = i;
                indexTupleList.add(indexTuple);
            }
        }
        m_indexTuples = indexTupleList.toArray(new IndexTuple[indexTupleList.size()]);
    }

    private TreeColumnMembershipController(final String colName, final int colIndex, final IndexTuple[] indexTuples,
        final Map<Integer, Integer> original2InternalIndex) {
        m_colName = colName;
        m_colIndex = colIndex;
        m_indexTuples = indexTuples;
        m_original2InternalIndex = original2InternalIndex;
    }

    /**
     *
     * @param originalIndices
     */
    public void updateMemberships(final int[] originalIndices) {
        ArrayList<IndexTuple> tempArrayList = new ArrayList<IndexTuple>();
        Map<Integer, Integer> tempIndexMap = new HashMap<Integer, Integer>();
        for (int originalIndex : originalIndices) {
            Integer internalIndex = m_original2InternalIndex.get(originalIndex);
            if (internalIndex != null) {
                tempArrayList.add(m_indexTuples[internalIndex]);
                tempIndexMap.put(originalIndex, internalIndex);
            }
        }
        m_original2InternalIndex = tempIndexMap;
        m_indexTuples = tempArrayList.toArray(new IndexTuple[tempArrayList.size()]);
        Arrays.sort(m_indexTuples);
    }

    /**
     * Creates a child TreeColumnMembershipController based on the weights in <b>childWeights</b>.
     *
     * @param childWeights
     * @return child controller
     */
    public TreeColumnMembershipController createChildTreeColumnMembershipController(final double[] childWeights) {
        ArrayList<IndexTuple> tempArrayList = new ArrayList<IndexTuple>();
        Map<Integer, Integer> tempIndexMap = new HashMap<Integer, Integer>();
        for (IndexTuple indexTuple : m_indexTuples) {
            if (childWeights[indexTuple.m_originalIndex] > TreeColumnData.EPSILON) {
                tempIndexMap.put(indexTuple.m_originalIndex, tempArrayList.size());
                tempArrayList.add(indexTuple);
            }
        }
        IndexTuple[] tempArray = tempArrayList.toArray(new IndexTuple[tempArrayList.size()]);
//        Arrays.sort(tempArray);
        return new TreeColumnMembershipController(m_colName, m_colIndex, tempArray, tempIndexMap);
    }

    /**
     * @param originalIndex
     * @return true if the original index <b>originalIndex</b> is managed by this object.
     */
    public boolean containsOriginalIndex(final int originalIndex) {
        return m_original2InternalIndex.containsKey(originalIndex);
    }

    /**
     * @param index
     * @return the index in the TreeAttributeColumn for internal index <b>index</b>
     */
    public int getIndexInColumn(final int index) {
        if (index < 0 || index >= m_indexTuples.length) {
            throw new IndexOutOfBoundsException("The provided index \"" + index + "\" is too large or negative.");
        }

        return m_indexTuples[index].m_indexInColumn;
    }

    /**
     * @return true if this object contains another index
     */
    public boolean hasNext() {
        return m_nextInternalIndex < m_indexTuples.length - 1;
    }


    /**
     * @return the current index
     */
    public int getCurrent() {
        return m_indexTuples[m_nextInternalIndex].m_indexInColumn;
    }

    /**
     * Goes to the next index
     */
    public void goToNext() {
        if (hasNext()) {
            m_nextInternalIndex++;
        }
        throw new IllegalStateException("There is no next index.");
    }

    /**
     * Goes to previous index
     */
    public void goToPrevious() {
        if (hasPrevious()) {
            m_nextInternalIndex--;
        }
        throw new IllegalStateException("There is no previous index.");
    }

    /**
     * @return true if the current internal index is valid
     */
    public boolean isValidIndex() {
        return m_nextInternalIndex >= 0 && m_nextInternalIndex < m_indexTuples.length;
    }

    /**
     * @return true if there is a previous index
     */
    public boolean hasPrevious() {
        return m_nextInternalIndex - 1 >= 0;
    }

    /**
     * @return the number of indices handled by <b>this</b>
     */
    public int getNrIndices() {
        return m_indexTuples.length;
    }

    private class IndexTuple implements Comparable<IndexTuple> {

        private int m_originalIndex;

        private int m_indexInColumn;

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final IndexTuple arg0) {

            return this.m_indexInColumn - arg0.m_indexInColumn;
        }

    }


}
