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
 *   29.03.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships.test;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.knime.base.node.mine.treeensemble2.data.memberships.ColumnMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.RootDataMemberships;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;

/**
 * This class represents an more memory efficient alternative to the approach of {@link RootDataMemberships}.<br>
 * The idea is to store the rowWeights in a Map with the original index as key, and using BitSet like structures
 * to store which original indices are in which nodes. <br>
 * This reduces memory requirements because it makes caching superfluous.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class RootDataMem implements DataMemberships {

    private final Map<Integer, Byte> m_weightMap;
//    private final TIntByteMap m_weightMap;
    private final BitSet m_contained;
    private final IDataIndexManager m_indexManager;

    /**
     * Creates a RootDataMem object
     *
     * @param rowSample the subset for the tree to be built.
     * @param indexManager the DataIndexManager object that contains the projections
     *        from original index to column indices and vice versa
     */
    public RootDataMem(final RowSample rowSample, final IDataIndexManager indexManager) {
        m_weightMap = new HashMap<Integer,Byte>(rowSample.getNrRows());
//        m_weightMap = new TIntByteHashMap(rowSample.getNrRows());
        m_contained = new BitSet();
        m_indexManager = indexManager;
        for (int i = 0; i < rowSample.getNrRows(); i++) {
            final int count = rowSample.getCountFor(i);
            if (count > 0) {
                m_weightMap.put(i, (byte)count);
                m_contained.set(i);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getRowWeights() {
        return getRowWeigtsForBitSet(m_contained);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnMemberships getColumnMemberships(final int index) {
        return descGetColumnMemberships(index, m_contained);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getOriginalIndices() {
        return getOriginalIdxForBitSet(m_contained);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataMemberships createChildMemberships(final BitSet inChild) {
        return new DescDataMem(this, inChild);
    }

    /**
     * {@inheritDoc}
     * The internal index of this dataMemberships object is the original index
     */
    @Override
    public double getRowWeight(final int index) {
        return m_weightMap.get(index);
    }

    /**
     * {@inheritDoc}
     * The internal index of this dataMemberships object is the original index
     */
    @Override
    public int getOriginalIndex(final int index) {
        return index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_weightMap.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCountInRoot() {
        return m_weightMap.size();
    }

    private double[] getRowWeigtsForBitSet(final BitSet contained) {
        final double[] weights = new double[contained.cardinality()];
        int j = 0;
        for (int i = contained.nextSetBit(0); i >= 0; i = contained.nextSetBit(i + 1)) {
            weights[j++] = m_weightMap.get(i);
        }
        return weights;
    }

    private static int[] getOriginalIdxForBitSet(final BitSet contained) {
        final int[] originalIdx = new int[contained.cardinality()];
        int j = 0;
        for (int i = contained.nextSetBit(0); i >= 0; i = contained.nextSetBit(i + 1)) {
            originalIdx[j++] = i;
        }
        return originalIdx;
    }

    double[] descGetRowWeights(final BitSet contained) {
        return getRowWeigtsForBitSet(contained);
    }

    int[] descGetOriginalIndices(final BitSet contained) {
        return getOriginalIdxForBitSet(contained);
    }

    ColMem descGetColumnMemberships(final int colIdx, final BitSet contained) {
        final BitSet sorted = new BitSet();
        final int[] idxInCol = m_indexManager.getPositionsInColumn(colIdx);
        for (int i = contained.nextSetBit(0); i >= 0; i = contained.nextSetBit(i + 1)) {
            sorted.set(idxInCol[i]);
        }
        return new ColMem(sorted, m_indexManager, this, colIdx);
    }

}
