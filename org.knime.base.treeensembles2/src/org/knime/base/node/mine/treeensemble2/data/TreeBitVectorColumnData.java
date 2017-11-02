/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 12, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.BitSet;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.memberships.ColumnMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.learner.BitSplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.IImpurity;
import org.knime.base.node.mine.treeensemble2.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeBitCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class TreeBitVectorColumnData extends TreeAttributeColumnData {

    private final BitSet m_columnBitSet;

    private final int m_length;

    /**
     * @param metaData
     * @param columnBitSet
     */
    TreeBitVectorColumnData(final TreeBitColumnMetaData metaData, final TreeEnsembleLearnerConfiguration configuration,
        final BitSet columnBitSet, final int length) {
        super(metaData, configuration);
        m_columnBitSet = columnBitSet;
        m_length = length;
    }

    /** {@inheritDoc} */
    @Override
    public TreeBitColumnMetaData getMetaData() {
        return (TreeBitColumnMetaData)super.getMetaData();
    }

    /** {@inheritDoc} */
    @Override
    public SplitCandidate calcBestSplitClassification(final DataMemberships dataMemberships,
        final ClassificationPriors targetPriors, final TreeTargetNominalColumnData targetColumn, final RandomData rd) {
        final NominalValueRepresentation[] targetVals = targetColumn.getMetaData().getValues();
        final IImpurity impurityCriterion = targetPriors.getImpurityCriterion();
        final int minChildSize = getConfiguration().getMinChildSize();
        // distribution of target for On ('1') and Off ('0') bits
        final double[] onTargetWeights = new double[targetVals.length];
        final double[] offTargetWeights = new double[targetVals.length];
        double onWeights = 0.0;
        double offWeights = 0.0;

        final ColumnMemberships columnMemberships = dataMemberships.getColumnMemberships(getMetaData().getAttributeIndex());

        while (columnMemberships.next()) {
            final double weight = columnMemberships.getRowWeight();
            if (weight < EPSILON) {
                // ignore record: not in current branch or not in sample
                assert false : "This code should never be reached!";
            } else {
                final int target = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                if (m_columnBitSet.get(columnMemberships.getIndexInColumn())) {
                    onWeights += weight;
                    onTargetWeights[target] += weight;
                } else {
                    offWeights += weight;
                    offTargetWeights[target] += weight;
                }
            }
        }
        if (onWeights < minChildSize || offWeights < minChildSize) {
            return null;
        }
        final double weightSum = onWeights + offWeights;
        final double onImpurity = impurityCriterion.getPartitionImpurity(onTargetWeights, onWeights);
        final double offImpurity = impurityCriterion.getPartitionImpurity(offTargetWeights, offWeights);
        final double[] partitionWeights = new double[]{onWeights, offWeights};
        final double postSplitImpurity =
            impurityCriterion.getPostSplitImpurity(new double[]{onImpurity, offImpurity}, partitionWeights, weightSum);
        final double gainValue =
            impurityCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity, partitionWeights, weightSum);
        return new BitSplitCandidate(this, gainValue);
    }

    /** {@inheritDoc} */
    @Override
    public SplitCandidate calcBestSplitRegression(final DataMemberships dataMemberships,
        final RegressionPriors targetPriors, final TreeTargetNumericColumnData targetColumn, final RandomData rd) {
        final double ySumTotal = targetPriors.getYSum();
        final double nrRecordsTotal = targetPriors.getNrRecords();
        final double criterionTotal = ySumTotal * ySumTotal / nrRecordsTotal;
        final int minChildSize = getConfiguration().getMinChildSize();

        final ColumnMemberships columnMemberships = dataMemberships.getColumnMemberships(getMetaData().getAttributeIndex());

        double onWeights = 0.0;
        double offWeights = 0.0;
        double ySumOn = 0.0;
        double ySumOff = 0.0;
        while (columnMemberships.next()) {
            final double weight = columnMemberships.getRowWeight();
            if (weight < EPSILON) {
                // ignore record: not in current branch or not in sample
            } else {
                final double y = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                if (m_columnBitSet.get(columnMemberships.getIndexInColumn())) {
                    onWeights += weight;
                    ySumOn += weight * y;
                } else {
                    offWeights += weight;
                    ySumOff += weight * y;
                }
            }
        }

        if (onWeights < minChildSize || offWeights < minChildSize) {
            return null;
        }

        final double onCriterion = ySumOn * ySumOn / onWeights;
        final double offCriterion = ySumOff * ySumOff / offWeights;
        final double gain = onCriterion + offCriterion - criterionTotal;
        if (gain > 0) {
            return new BitSplitCandidate(this, gain);
        }
        return null;
    }


    /** {@inheritDoc} */
    @Override
    public BitSet updateChildMemberships(final TreeNodeCondition childCondition,
        final DataMemberships parentMemberships) {
        TreeNodeBitCondition bitCondition = (TreeNodeBitCondition)childCondition;
        assert getMetaData().getAttributeName().equals(bitCondition.getColumnMetaData().getAttributeName());
        final boolean value = bitCondition.getValue();

        final ColumnMemberships columnMemberships = parentMemberships.getColumnMemberships(getMetaData().getAttributeIndex());
        BitSet inChild = new BitSet(columnMemberships.size());
        columnMemberships.reset();
        columnMemberships.next();

        for (int i = columnMemberships.getIndexInColumn(); ; i = columnMemberships.getIndexInColumn()) {
            if (m_columnBitSet.get(i) == value) {
                inChild.set(columnMemberships.getIndexInDataMemberships());
            }
            if (!columnMemberships.next()) {
                break;
            }
        }
        return inChild;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getOriginalIndicesInColumnList() {
        int[] originalIndices = new int[m_length];
        for (int i = 0; i < originalIndices.length; i++) {
            originalIndices[i] = i;
        }
        return originalIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsMissingValues() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int indexInColumn) {
        return m_columnBitSet.get(indexInColumn);
    }


}
