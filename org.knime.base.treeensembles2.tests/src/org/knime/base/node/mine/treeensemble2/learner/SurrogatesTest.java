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
 *   08.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.BitSet;

import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnDataTest;
import org.knime.base.node.mine.treeensemble2.data.TreeOrdinaryNumericColumnData;
import org.knime.base.node.mine.treeensemble2.data.memberships.ColumnMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class SurrogatesTest {

    private static final String COLBEST = "1, 2, 7, 7, NaN, 4, NaN, 9, 3, NaN";
    private static final String COL1 = "2, 1, 4, 8, NaN, 1, 9, 6, 7, NaN";
    private static final String COL2 = "1, 9, NaN, 7, 3, 6, 7, 8, NaN, 2";
    private static final double SPLITVAL = 5;

    private static SplitCandidate[] createSplitCandidates(final TreeEnsembleLearnerConfiguration config) {
        double[] colBestData = TreeNumericColumnDataTest.asDataArray(COLBEST);
        double[] col1Data = TreeNumericColumnDataTest.asDataArray(COL1);
        double[] col2Data = TreeNumericColumnDataTest.asDataArray(COL2);

        TreeOrdinaryNumericColumnData colBest = TreeNumericColumnDataTest.createNumericColumnData(config, colBestData, "colBest", 0);
        colBest.getMetaData().setAttributeIndex(0);
        TreeOrdinaryNumericColumnData col1 = TreeNumericColumnDataTest.createNumericColumnData(config, col1Data, "col1", 1);
        col1.getMetaData().setAttributeIndex(1);
        TreeOrdinaryNumericColumnData col2 = TreeNumericColumnDataTest.createNumericColumnData(config, col2Data, "col2", 2);
        col2.getMetaData().setAttributeIndex(2);

        NumericSplitCandidate splitBest = new NumericSplitCandidate(colBest, SPLITVAL, 0.5, createMissingBitSet(colBestData), NumericSplitCandidate.NO_MISSINGS);
        NumericSplitCandidate split1 = new NumericSplitCandidate(col1, SPLITVAL, 0.25, createMissingBitSet(col1Data), NumericSplitCandidate.NO_MISSINGS);
        NumericSplitCandidate split2 = new NumericSplitCandidate(col2, SPLITVAL, 0.1, createMissingBitSet(col2Data), NumericSplitCandidate.NO_MISSINGS);

        return new SplitCandidate[] {splitBest, split1, split2};
    }

    private static DataMemberships createDataMembershipsFormSplitCandidates(final SplitCandidate[] splitCandidates) {
        TreeAttributeColumnData[] cols = new TreeAttributeColumnData[splitCandidates.length];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = splitCandidates[i].getColumnData();
        }
        return new TestingDataMemberships(cols[0].getOriginalIndicesInColumnList().length, cols);
    }

    private static BitSet createMissingBitSet(final double[] data) {
        BitSet bs = new BitSet();
        for (int i = 0; i < data.length; i++) {
            if (data[i] == Double.NaN) {
                bs.set(i);
            }
        }
        return bs;
    }

//    @Test
    public void testCalculateSurrogates() {
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(false);
        SplitCandidate[] splitCandidates = createSplitCandidates(config);
        DataMemberships datMem = createDataMembershipsFormSplitCandidates(splitCandidates);
        SurrogateSplit surrogateSplit = Surrogates.calculateSurrogates(datMem, splitCandidates);
        AbstractTreeNodeSurrogateCondition[] surrogateConditions = surrogateSplit.getChildConditions();
        AbstractTreeNodeSurrogateCondition leftChildCondition = surrogateConditions[0];
        AbstractTreeNodeSurrogateCondition rightChildCondition = surrogateConditions[1];

//        assertEquals("col1 < 5", leftChildCondition.getColumnCondition(1).toString());
    }





    private static class TestingDataMemberships implements DataMemberships {
        private final double[] m_rowWeights;
        private final TestingColumnMemberships[] m_colMemberships;
        private final int[] m_originalIndices;

        public TestingDataMemberships(final int numInstances, final TreeAttributeColumnData[] cols) {
            m_rowWeights = new double[numInstances];
            m_originalIndices = new int[numInstances];
            for (int i = 0; i < numInstances; i++) {
                m_rowWeights[i] = 1.0;
                m_originalIndices[i] = i;
            }
            m_colMemberships = new TestingColumnMemberships[cols.length];
            for (int i = 0; i < cols.length; i++) {
                m_colMemberships[i] = new TestingColumnMemberships(cols[i]);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double[] getRowWeights() {
            return m_rowWeights;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ColumnMemberships getColumnMemberships(final int index) {
            return m_colMemberships[index];
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
            return null;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public double getRowWeight(final int index) {
            return m_rowWeights[index];
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
            return m_rowWeights.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCountInRoot() {
            return m_rowWeights.length;
        }
    }

    private static class TestingColumnMemberships implements ColumnMemberships {

        private final int m_numRows;
        private int m_pointer;
        private int[] m_indicesInDataMemberships;

        public TestingColumnMemberships(final TreeAttributeColumnData col) {
            m_indicesInDataMemberships = col.getOriginalIndicesInColumnList();
            m_numRows = m_indicesInDataMemberships.length;
            m_pointer = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return m_numRows;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean next() {
            if (m_pointer + 1 < m_numRows) {
                m_pointer++;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean nextIndexFrom(final int indexInColumn) {
            if (indexInColumn < m_numRows) {
                m_pointer = indexInColumn > 0 ? indexInColumn : 0;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getRowWeight() {
            return 1.0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getOriginalIndex() {
            return m_indicesInDataMemberships[m_pointer];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getIndexInColumn() {
            return m_pointer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getIndexInDataMemberships() {
            return m_indicesInDataMemberships[m_pointer];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() {
            m_pointer = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void goToLast() {
            // TODO Auto-generated method stub

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean previous() {
            // TODO Auto-generated method stub
            return false;
        }

    }

}
