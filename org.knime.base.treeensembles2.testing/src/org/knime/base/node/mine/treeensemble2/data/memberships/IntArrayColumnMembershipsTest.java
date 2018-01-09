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
 *   31.03.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

/**
 * This class contains unit tests for the corresponding class IntArrayColumnMemberships
 *
 * @author Adrian Nembach, KNIME.com
 */
public class IntArrayColumnMembershipsTest {

    private static final double[] WEIGHTS = new double[]{1, 3, 2, 5, 6, 4, 8};

    private static final int[] ORIGINAL_INDICES = new int[]{0, 3, 7, 10, 11, 12, 20};

    private static final int[] DATAMEMBERSHIPS_INDICES = new int[]{1, 6, 5, 2, 3, 4, 0};

    private static final int[] COLUMN_INDICES = new int[]{1, 2, 5, 6, 9, 12, 17};


    /**
     * Tests the method responsible for iterating over the ColumnMemberships: <br>
     * {@link IntArrayColumnMemberships#next()} <br>
     * {@link IntArrayColumnMemberships#previous()} <br>
     * {@link IntArrayColumnMemberships#reset()} <br>
     * {@link IntArrayColumnMemberships#goToLast()} <br>
     * {@link IntArrayColumnMemberships#nextIndexFrom(int)} <br>
     *
     * <br>
     * <br>
     *
     * It also uses the getter Methods: <br>
     * {@link IntArrayColumnMemberships#getIndexInColumn()} <br>
     * {@link IntArrayColumnMemberships#getIndexInDataMemberships()} <br>
     * {@link IntArrayColumnMemberships#getOriginalIndex()} <br>
     * {@link IntArrayColumnMemberships#getRowWeight()} <br>
     *
     * @throws Exception
     */
    @Test
    public void testIteration() throws Exception {
        final DataMemberships dataMem = new MockDataMemberships(ORIGINAL_INDICES, WEIGHTS);
        final IntArrayColumnMemberships colMem =
            new IntArrayColumnMemberships(COLUMN_INDICES, DATAMEMBERSHIPS_INDICES, dataMem);
        // iterate from start to end
        int i = 0;
        while (colMem.next()) {
            assertEquals("Wrong index in column", COLUMN_INDICES[i], colMem.getIndexInColumn());
            assertEquals("Wrong index in dataMemberships", DATAMEMBERSHIPS_INDICES[i], colMem.getIndexInDataMemberships());
            assertEquals("Wrong original index", ORIGINAL_INDICES[DATAMEMBERSHIPS_INDICES[i]], colMem.getOriginalIndex());
            assertEquals("Wrong rowWeight", WEIGHTS[DATAMEMBERSHIPS_INDICES[i++]], colMem.getRowWeight(), 0.0);
        }
        assertEquals("Wrong number of records in column memberships", COLUMN_INDICES.length, i);

        // reset
        colMem.reset();
        assertTrue("Reset unsuccessful", colMem.next());
        assertEquals("ColumnMemberships at wrong position after reset", COLUMN_INDICES[0], colMem.getIndexInColumn());

        // go to last valid position
        colMem.goToLast();
        assertEquals("ColumnMemberships at wrong position after goToLast", COLUMN_INDICES[COLUMN_INDICES.length - 1], colMem.getIndexInColumn());

        // iterate from end to start
        i = COLUMN_INDICES.length - 1;
        do {
            assertEquals("Wrong index in column", COLUMN_INDICES[i], colMem.getIndexInColumn());
            assertEquals("Wrong index in dataMemberships", DATAMEMBERSHIPS_INDICES[i], colMem.getIndexInDataMemberships());
            assertEquals("Wrong original index", ORIGINAL_INDICES[DATAMEMBERSHIPS_INDICES[i]], colMem.getOriginalIndex());
            assertEquals("Wrong rowWeight", WEIGHTS[DATAMEMBERSHIPS_INDICES[i--]], colMem.getRowWeight(), 0.0);
        } while (colMem.previous());

        colMem.reset();

        // move to next index from index 7 in the column
        assertTrue(colMem.nextIndexFrom(7));
        assertEquals("nextIndexFrom moved to the wrong position", 9, colMem.getIndexInColumn());
    }

    /**
     * Tests the methods called by {@link DescendantColumnMemberships}.
     *
     * @throws Exception
     */
    @Test
    public void testDescendantMethods() throws Exception {
        final DataMemberships dataMem = new MockDataMemberships(ORIGINAL_INDICES, WEIGHTS);
        final IntArrayColumnMemberships colMem = new IntArrayColumnMemberships(COLUMN_INDICES, DATAMEMBERSHIPS_INDICES, dataMem);
        for(int i = 0; i < WEIGHTS.length; i++) {
            assertEquals("Wrong index in column", COLUMN_INDICES[i], colMem.descendantGetIndexInColumn(i));
            assertEquals("Wrong index in dataMemberships", DATAMEMBERSHIPS_INDICES[i], colMem.descendantGetIndexInDataMemberships(i));
            assertEquals("Wrong original index", ORIGINAL_INDICES[DATAMEMBERSHIPS_INDICES[i]], colMem.descendantGetIndexInOriginal(i));
            assertEquals("Wrong weight", WEIGHTS[DATAMEMBERSHIPS_INDICES[i]], colMem.descendantGetRowWeight(i), 0.0);
        }
    }

    /**
     * Tests {@link IntArrayColumnMemberships#size()}
     *
     * @throws Exception
     */
    @Test
    public void testSize() throws Exception {
        final DataMemberships dataMem = new MockDataMemberships(ORIGINAL_INDICES, WEIGHTS);
        final IntArrayColumnMemberships colMem = new IntArrayColumnMemberships(COLUMN_INDICES, DATAMEMBERSHIPS_INDICES, dataMem);
        assertEquals("Wrong size returned", COLUMN_INDICES.length, colMem.size());

        final int[] indexInColumn = Arrays.copyOfRange(COLUMN_INDICES, 1, 3);
        final int[] indexInDataMemberships = Arrays.copyOfRange(indexInColumn, 1, 3);
        final IntArrayColumnMemberships smallerColMem = new IntArrayColumnMemberships(indexInColumn, indexInDataMemberships, dataMem);
        assertEquals("Wrong size returned", 2, smallerColMem.size());
    }

    private static class MockDataMemberships implements DataMemberships {

        private final int[] m_originalIndices;
        private final double[] m_weights;

        public MockDataMemberships(final int[] originalIndices, final double[] weights) {
            m_originalIndices = originalIndices;
            m_weights = weights;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double[] getRowWeights() {
            return m_weights;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ColumnMemberships getColumnMemberships(final int index) {
            return null;
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
        public DataMemberships createChildMemberships(final BitSet inChild) {
            throw new NotImplementedException("This datamemberships object does not create children.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getRowWeight(final int index) {
            return m_weights[index];
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
            return getRowCount();
        }

    }
}
