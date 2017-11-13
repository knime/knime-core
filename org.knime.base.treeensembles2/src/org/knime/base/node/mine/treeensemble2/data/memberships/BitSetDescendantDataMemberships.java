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
 *   16.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import java.util.BitSet;
import java.util.HashMap;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class BitSetDescendantDataMemberships implements DataMemberships {

    private final RootDataMemberships m_root;
    private final BitSet m_included;
    private final HashMap<Integer, ColumnMemberships> m_cachedColumnMemberships;

    public BitSetDescendantDataMemberships(final RootDataMemberships root, final BitSet included) {
        m_root = root;
        m_included = included;
        m_cachedColumnMemberships = new HashMap<Integer, ColumnMemberships>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getRowWeights() {
        final double[] weights = new double[m_included.cardinality()];
        int iterator = 0;
        boolean overflow = false;
        for (int i = m_included.nextSetBit(0); i >= 0; i = m_included.nextSetBit(i + 1)) {
            weights[iterator++] = m_root.getRowWeight(i);
            if (i >= Integer.MAX_VALUE) {
                overflow = true;
                break;
            }
        }

        if (overflow) {
            throw new IllegalStateException("Possible BitSet overflow, please check the implementation");
        }

        return weights;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnMemberships getColumnMemberships(final int index) {
        ColumnMemberships colMem = m_cachedColumnMemberships.get(index);
        if (colMem == null) {
            colMem = m_root.descendantGetColumnMemberships(index, m_included);
            m_cachedColumnMemberships.put(index, colMem);
        }
        return colMem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getOriginalIndices() {
        final int[] originalIndices = new int[m_included.cardinality()];
        int iterator = 0;
        for (int i = m_included.nextSetBit(0); i >= 0; i = m_included.nextSetBit(i + 1)) {
            originalIndices[iterator++] = m_root.getOriginalIndex(i);
            if (i >= Integer.MAX_VALUE) {
                throw new IllegalStateException("Possible overflow during traversal of BitSet detected, please check implementation");
            }
        }
        return originalIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataMemberships createChildMemberships(final BitSet inChild) {
        return m_root.createDescendantMemberships(inChild);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getRowWeight(final int index) {
        final int indexInRoot = getRootIndex(index);
        if (indexInRoot == -1) {
            throw new IndexOutOfBoundsException("Index is not contained");
        }
        return m_root.getRowWeight(indexInRoot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex(final int index) {
        final int indexInRoot = getRootIndex(index);
        if (indexInRoot == -1) {
            throw new IndexOutOfBoundsException("Index is not contained");
        }
        return m_root.getOriginalIndex(indexInRoot);
    }

    private int getRootIndex(final int index) {
        if (index > m_included.cardinality()) {
            throw new IndexOutOfBoundsException("The BitSet does not contain that many set bits.");
        }
        int counter = 0;
        for (int i = m_included.nextSetBit(0); i >= 0; i = m_included.nextSetBit(i + 1)) {
            if (counter++ == index) {
                return i;
            } else if (i >= Integer.MAX_VALUE) {
                throw new IllegalStateException("Possible overflow during traversal of BitSet detected, please check the implementation");
            }
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_included.cardinality();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCountInRoot() {
        return m_root.getRowCountInRoot();
    }

    /**
     * For testing purposes
     *
     * @return BitSet that marks the included rows with a set bit
     */
    BitSet getBitSet() {
        return m_included;
    }

}
