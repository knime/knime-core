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
 *   01.04.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.BitSet;

import org.junit.Test;

/**
 * This class contains unit tests for the DescendantColumnMemberships
 *
 * @author Adrian Nembach, KNIME.com
 */
public class DescendantColumnMembershipsTest {
    private static final int[] INTERNAL_INDEX = new int[]{0, 3, 4, 6, 8, 9};

    private static final int[] COLUMN_INDEX = new int[]{0, 4, 7, 12, 17, 25};

    private static final int[] DATAMEM_INDEX = new int[]{2, 7, 3, 14, 1, 0};

    private static final int[] ORIGINAL_INDEX = new int[]{2, 12, 5, 26, 1, 0};

    private static final double[] WEIGHTS = new double[]{1.0, 2.0, 5.0, 3.0, 2.0, 4.0};

    private static BitSet createIncludedBitSet() {
        final BitSet included = new BitSet();
        for (final int idx : INTERNAL_INDEX) {
            included.set(idx);
        }
        return included;
    }

    private static class MockIntArrayColumnMemberships extends IntArrayColumnMemberships {

        private final double[] m_weights;

        private final int[] m_originalIndex;

        private final int[] m_internalIndex;

        private final int[] m_columnIndex;

        private final int[] m_dataMemIndex;

        /**
         * @param indexInColumn
         * @param indexInDataMemberships
         * @param dataMemberships
         */
        public MockIntArrayColumnMemberships(final int[] indexInColumn, final int[] indexInDataMemberships,
            final double[] weights, final int[] originalIndex, final int[] internalIndex) {
            super(indexInColumn, indexInDataMemberships, null);
            m_weights = weights;
            m_originalIndex = originalIndex;
            m_internalIndex = internalIndex;
            m_columnIndex = indexInColumn;
            m_dataMemIndex = indexInDataMemberships;
        }

        private int getIndexFor(final int internalIdx) {
            int idx = -1;
            for (int i = 0; i < m_internalIndex.length; i++) {
                if (m_internalIndex[i] == internalIdx) {
                    idx = i;
                    break;
                }
            }
            return idx;
        }

        /**
         * {@inheritDoc}
         */
        @Override
            int descendantGetIndexInColumn(final int internalIndex) {
            return m_columnIndex[getIndexFor(internalIndex)];
        }

        /**
         * {@inheritDoc}
         */
        @Override
            int descendantGetIndexInDataMemberships(final int internalIndex) {
            return m_dataMemIndex[getIndexFor(internalIndex)];
        }

        /**
         * {@inheritDoc}
         */
        @Override
            int descendantGetIndexInOriginal(final int internalIndex) {
            return m_originalIndex[getIndexFor(internalIndex)];
        }

        /**
         * {@inheritDoc}
         */
        @Override
            double descendantGetRowWeight(final int internalIndex) {
            return m_weights[getIndexFor(internalIndex)];
        }

    }

    /**
     * Tests the methods related to the iteration over the indices: <br>
     * {@link ColumnMemberships#next()} <br>
     * {@link ColumnMemberships#previous()} <br>
     * {@link ColumnMemberships#reset()} <br>
     * {@link ColumnMemberships#goToLast()} <br>
     * <br>
     * It also uses the getter Methods: <br>
     * {@link ColumnMemberships#getIndexInColumn()} <br>
     * {@link ColumnMemberships#getIndexInDataMemberships()} <br>
     * {@link ColumnMemberships#getOriginalIndex()} <br>
     * {@link ColumnMemberships#getRowWeight()}
     *
     * @throws Exception
     */
    @Test
    public void testIteration() throws Exception {
        final MockIntArrayColumnMemberships mockRootColMem =
            new MockIntArrayColumnMemberships(COLUMN_INDEX, DATAMEM_INDEX, WEIGHTS, ORIGINAL_INDEX, INTERNAL_INDEX);
        final DescendantColumnMemberships colMem =
            new DescendantColumnMemberships(mockRootColMem, createIncludedBitSet());
        // test iteration from start to end
        int i = 0;
        while (colMem.next()) {
            assertEquals("column index did not match", COLUMN_INDEX[i], colMem.getIndexInColumn());
            assertEquals("original index did not match", ORIGINAL_INDEX[i], colMem.getOriginalIndex());
            assertEquals("DataMemberships index did not match", DATAMEM_INDEX[i], colMem.getIndexInDataMemberships());
            assertEquals("Weight did not match", WEIGHTS[i++], colMem.getRowWeight(), 0.0);
        }

        // test reset
        colMem.reset();
        colMem.next();
        assertEquals("reset did not work", COLUMN_INDEX[0], colMem.getIndexInColumn());

        // test goToLast
        colMem.goToLast();
        assertEquals("goToLast did not work", COLUMN_INDEX[COLUMN_INDEX.length - 1], colMem.getIndexInColumn());

        // test iteration from end to start
        i = COLUMN_INDEX.length - 1;
        do {
            assertEquals("column index did not match", COLUMN_INDEX[i], colMem.getIndexInColumn());
            assertEquals("original index did not match", ORIGINAL_INDEX[i], colMem.getOriginalIndex());
            assertEquals("DataMemberships index did not match", DATAMEM_INDEX[i], colMem.getIndexInDataMemberships());
            assertEquals("Weight did not match", WEIGHTS[i--], colMem.getRowWeight(), 0.0);
        } while (colMem.previous());

        colMem.reset();

        // test nextIndexFrom
        colMem.nextIndexFrom(COLUMN_INDEX[COLUMN_INDEX.length / 2] + 1);
        assertEquals("nextIndexFrom did not work.", COLUMN_INDEX[COLUMN_INDEX.length / 2 + 1],
            colMem.getIndexInColumn());

        assertFalse(colMem.nextIndexFrom(100));
    }

    /**
     * Tests the method {@link ColumnMemberships#size()}
     *
     * @throws Exception
     */
    @Test
    public void testSize() throws Exception {
        final BitSet included = createIncludedBitSet();
        final MockIntArrayColumnMemberships mockRootColMem =
                new MockIntArrayColumnMemberships(COLUMN_INDEX, DATAMEM_INDEX, WEIGHTS, ORIGINAL_INDEX, INTERNAL_INDEX);
            final DescendantColumnMemberships colMem =
                new DescendantColumnMemberships(mockRootColMem, createIncludedBitSet());
        assertEquals("Size does not work correctly.", included.cardinality(), colMem.size());
        final BitSet smallerInc = (BitSet)included.clone();
        smallerInc.set(smallerInc.nextSetBit(0), false);
        final DescendantColumnMemberships smallerColMem = new DescendantColumnMemberships(mockRootColMem, smallerInc);
        assertEquals("Size does not work correctly.", colMem.size() - 1, smallerColMem.size());
    }
}
