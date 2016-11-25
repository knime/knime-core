/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   11.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import java.util.ArrayList;
import java.util.BitSet;

import org.knime.base.node.mine.treeensemble2.data.TreeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class RootDataMemberships implements DataMemberships {

    // When building huge trees with many levels it might be a good
    // idea to create a new RootDataMemberships object if the fraction
    // of the data in the descendant datamemberships falls below
    private static final double NEW_ROOT_THRESHOLD = 0;

    private static final long CACHE_SIZE = 1000;

    final private int m_numCols;

//    private final HashMap<Integer, ColumnMembershipsEntry> m_cacheHashMap;

    private final LoadingCache<Integer, ColumnMembershipsEntry> m_cache;

    private final int[] m_originalIndices;

//    private final double[] m_rowWeights;

    private final WeightContainer m_weights;

    private final int m_rowCountInRoot;

    private final IDataIndexManager m_indexManager;

    /**
     * @param rowSample
     * @param data
     * @param indexManager
     */
    public RootDataMemberships(final RowSample rowSample, final TreeData data, final IDataIndexManager indexManager) {
        final int numRows = rowSample.getNrRows();
        m_numCols = data.getNrAttributes();
//        m_cacheHashMap = new HashMap<Integer, ColumnMembershipsEntry>((int)(m_numCols * 1.5));
        m_cache = CacheBuilder.newBuilder()
                .softValues()
                .build(new ColumnMembershipsEntryCacheLoader(this));
        m_indexManager = indexManager;
        int approximatedNumRows = (int)(numRows * 0.7);
        ArrayList<Integer> rowCounts = new ArrayList<Integer>(approximatedNumRows);
        ArrayList<Integer> originalIndices = new ArrayList<Integer>(approximatedNumRows);

        for (int originalIndex = 0; originalIndex < data.getNrRows(); originalIndex++) {
            final int count = rowSample.getCountFor(originalIndex);
            if (count > 0) {
                rowCounts.add(count);
                originalIndices.add(originalIndex);
            }
        }
        m_rowCountInRoot = rowCounts.size();
        m_weights = new ByteWeightContainer(rowCounts);
//        m_rowWeights = new double[m_rowCountInRoot];
        m_originalIndices = new int[m_rowCountInRoot];

        for (int i = 0; i < rowCounts.size(); i++) {
//            m_rowWeights[i] = rowCounts.get(i);
            m_originalIndices[i] = originalIndices.get(i);
        }
    }

    /**
     * only for testing purposes
     *
     * @param rowWeights
     * @param data
     * @param indexManager
     */
    public RootDataMemberships(final double[] rowWeights, final TreeData data, final IDataIndexManager indexManager) {
        m_numCols = data.getNrAttributes();
//        m_cacheHashMap = new HashMap<Integer, ColumnMembershipsEntry>();
        m_cache = CacheBuilder.newBuilder()
                .maximumSize(CACHE_SIZE)
                .build(new ColumnMembershipsEntryCacheLoader(this));
        m_indexManager = indexManager;
        ArrayList<Integer> weights = Lists.newArrayList();
        ArrayList<Integer> originalIndices = Lists.newArrayList();

        for (int originalIndex = 0; originalIndex < data.getNrRows(); originalIndex++) {
            final double weight = rowWeights[originalIndex];
            if (weight > TreeColumnData.EPSILON) {
                weights.add((int)weight);
                originalIndices.add(originalIndex);
            }
        }
        m_rowCountInRoot = weights.size();
//        m_rowWeights = new double[m_rowCountInRoot];
        m_weights = new ByteWeightContainer(weights);
        m_originalIndices = new int[m_rowCountInRoot];
        for (int i = 0; i < weights.size(); i++) {
//            m_rowWeights[i] = weights.get(i);
            m_originalIndices[i] = originalIndices.get(i);
        }

    }

    private RootDataMemberships(final int numCols, final IDataIndexManager indexManager, final int[] originalIndices,
        final WeightContainer weights, final int rowCountInRoot) {
        m_numCols = numCols;
        m_indexManager = indexManager;
        m_originalIndices = originalIndices;
        m_weights = weights;
//        m_cacheHashMap = new HashMap<Integer, ColumnMembershipsEntry>();
        m_cache = CacheBuilder.newBuilder()
                .maximumSize(CACHE_SIZE)
                .build(new ColumnMembershipsEntryCacheLoader(this));
        m_rowCountInRoot = rowCountInRoot;
    }

    ColumnMemberships descendantGetColumnMemberships(final int index, final int[] indicesInRoot) {
        final ColumnMembershipsEntry colMemEntry = m_cache.getUnchecked(index);
        final IntArrayColumnMemberships rootColMemberships = colMemEntry.m_colMem;
        final int[] indicesInColMemberships = colMemEntry.m_indicesInColMem;
        final BitSet colMemBs = new BitSet(m_originalIndices.length);

        for (int indexInRoot : indicesInRoot) {
            colMemBs.set(indicesInColMemberships[indexInRoot]);
        }

        return new DescendantColumnMemberships(rootColMemberships, colMemBs);
    }

    ColumnMemberships descendantGetColumnMemberships(final int index, final BitSet included) {
        final ColumnMembershipsEntry colMemEntry = m_cache.getUnchecked(index);
        final IntArrayColumnMemberships rootColMemberships = colMemEntry.m_colMem;
        final int[] indicesInColMemberships = colMemEntry.m_indicesInColMem;
        final BitSet colMemBs = new BitSet(m_originalIndices.length);
        boolean overflow = false;
        for (int indexInRoot = included.nextSetBit(0); indexInRoot >= 0; indexInRoot =
            included.nextSetBit(indexInRoot + 1)) {
            colMemBs.set(indicesInColMemberships[indexInRoot]);
            if (indexInRoot >= Integer.MAX_VALUE) {
                overflow = true;
                break;
            }
        }
        if (overflow) {
            throw new IllegalStateException("Possible overflow during BitSet traversal, please check implementation.");
        }
        return new DescendantColumnMemberships(rootColMemberships, colMemBs);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getRowWeights() {
        return m_weights.getAllWeights();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntArrayColumnMemberships getColumnMemberships(final int index) {
        if (index < 0 || index >= m_numCols) {
            throw new IndexOutOfBoundsException();
        }
        return m_cache.getUnchecked(index).m_colMem;
    }


    @Override
    public DataMemberships createChildMemberships(final BitSet inChild) {
//        if (((double)inChild.cardinality()) / m_rowWeights.length < NEW_ROOT_THRESHOLD) {
//            return createChildRootMemberships(inChild);
//        }

        return createDescendantMemberships(inChild);
    }

    DataMemberships createDescendantMemberships(final BitSet inDescendant) {
        return new BitSetDescendantDataMemberships(this, inDescendant);
        //        final int descendantSize = inDescendant.cardinality();
        //        final int[] indicesInRoot = new int[descendantSize];
        //        int index = 0;
        //        for (int i = inDescendant.nextSetBit(0); i >= 0; i = inDescendant.nextSetBit(i+1)) {
        //            indicesInRoot[index++] = i;
        //        }
        //
        //        return new DescendantDataMemberships(this, indicesInRoot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getOriginalIndices() {
        return m_originalIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRowWeight(final int index) {
        return m_weights.getWeight(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex(final int index) {
        return m_originalIndices[index];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_originalIndices.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCountInRoot() {
        return m_rowCountInRoot;
    }

    private static class ColumnMembershipsEntry {
        private final IntArrayColumnMemberships m_colMem;
        private final int[] m_indicesInColMem;

        public ColumnMembershipsEntry(final IntArrayColumnMemberships columnMemberships, final int[] indicesInColMem) {
            m_colMem = columnMemberships;
            m_indicesInColMem = indicesInColMem;
        }
    }

    private class ColumnMembershipsEntryCacheLoader extends CacheLoader<Integer, ColumnMembershipsEntry> {
        private final DataMemberships m_dataMem;

        public ColumnMembershipsEntryCacheLoader(final DataMemberships dataMem) {
            m_dataMem = dataMem;
        }

        @Override
        public ColumnMembershipsEntry load(final Integer key) throws Exception {
            final int dataMembershipsSize = m_originalIndices.length;
            final int[] original2Column = m_indexManager.getPositionsInColumn(key);
            final int originalSize = original2Column.length;
            final BitSet colBitSet = new BitSet(originalSize);
            final int[] columnIndex2dataMemIndex = new int[originalSize];
            for (int dataMemIndex = 0; dataMemIndex < dataMembershipsSize; dataMemIndex++) {
                final int indexInColumn = original2Column[m_originalIndices[dataMemIndex]];
                colBitSet.set(indexInColumn);
                columnIndex2dataMemIndex[indexInColumn] = dataMemIndex;
            }

            final int[] indicesInDataMemberships = new int[dataMembershipsSize];
            final int[] indicesInColumn = new int[dataMembershipsSize];
            final int[] indicesInColumnMemberships = new int[dataMembershipsSize];
            int indexInColumnMembership = 0;

            boolean overflow = false;
            for (int indexInColumn = colBitSet.nextSetBit(0); indexInColumn >= 0; indexInColumn =
                colBitSet.nextSetBit(indexInColumn + 1)) {
                indicesInColumn[indexInColumnMembership] = indexInColumn;
                final int dataMembershipsIndex = columnIndex2dataMemIndex[indexInColumn];
                indicesInDataMemberships[indexInColumnMembership] = dataMembershipsIndex;
                indicesInColumnMemberships[dataMembershipsIndex] = indexInColumnMembership;
                indexInColumnMembership++;

                if (indexInColumn == Integer.MAX_VALUE) {
                    overflow = true;
                    break;
                }
            }

            if (overflow) {
                throw new IllegalStateException("An overflow occurred, please check the implementation.");
            }
            final IntArrayColumnMemberships colMem =
                new IntArrayColumnMemberships(indicesInColumn, indicesInDataMemberships, m_dataMem);
            return new ColumnMembershipsEntry(colMem, indicesInColumnMemberships);
        }

    }
}
