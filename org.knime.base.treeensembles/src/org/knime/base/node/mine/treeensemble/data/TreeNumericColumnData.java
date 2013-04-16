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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 27, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import org.knime.base.node.mine.treeensemble.learner.IImpurity;
import org.knime.base.node.mine.treeensemble.learner.NumericSplitCandidate;
import org.knime.base.node.mine.treeensemble.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble.model.TreeNodeNumericCondition;
import org.knime.base.node.mine.treeensemble.model.TreeNodeNumericCondition.NumericOperator;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.util.DoubleFormat;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeNumericColumnData extends TreeAttributeColumnData {

    private final double[] m_sortedData;
    private final int[] m_originalIndexInColumnList;

    TreeNumericColumnData(final TreeNumericColumnMetaData metaData,
            final TreeEnsembleLearnerConfiguration configuration,
            final double[] sortedData, final int[] orginalIndexInColumnList) {
        super(metaData, configuration);
        m_sortedData = sortedData;
        m_originalIndexInColumnList = orginalIndexInColumnList;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNumericColumnMetaData getMetaData() {
        return (TreeNumericColumnMetaData)super.getMetaData();
    }

    /** {@inheritDoc} */
    @Override
    public NumericSplitCandidate calcBestSplitClassification(final double[] rowWeights,
            final ClassificationPriors targetPriors,
            final TreeTargetNominalColumnData targetColumn) {
        final NominalValueRepresentation[] targetVals = targetColumn.getMetaData().getValues();
        final int[] originalIndexInColumnList = m_originalIndexInColumnList;
        final boolean useAverageSplitPoints = getConfiguration().isUseAverageSplitPoints();
        final int minChildNodeSize = getConfiguration().getMinChildSize();

        // distribution of target for each attribute value
        final int targetCounts = targetVals.length;
        final double[] targetCountsLeftOfSplit = new double[targetCounts];
        final double[] targetCountsRightOfSplit = targetPriors.getDistribution().clone();
        assert targetCountsRightOfSplit.length == targetCounts;
        final double totalSumWeight = targetPriors.getNrRecords();
        final double priorImpurity = targetPriors.getPriorImpurity();
        final IImpurity impurityCriterion = targetPriors.getImpurityCriterion();

        double sumWeightsLeftOfSplit = 0.0;
        double sumWeightsRightOfSplit = totalSumWeight;
        double bestSplit = Double.NEGATIVE_INFINITY;
        // gain for best split point, unnormalized (not using info gain ratio)
        double bestGain = Double.NEGATIVE_INFINITY;
        // gain for best split, normalized by attribute entropy when
        // info gain ratio is used.
        double bestGainValueForSplit = Double.NEGATIVE_INFINITY;
        final double[] tempArray1 = new double[2];
        final double[] tempArray2 = new double[2];

        double lastSeenValue = Double.NEGATIVE_INFINITY;
        boolean mustTestOnNextValueChange = false;
        int lastSeenTarget = -1;
        // main loop: iterate the entire sorted column, search for reasonable
        // split points (i.e. where the attribute value changes and value of the
        // target column), and for each split point compute the information
        // gain, keep the one that maximizes the split
        for (int i = 0; i < m_originalIndexInColumnList.length; i++) {
            final int originalIndex = originalIndexInColumnList[i];
            final double weight = rowWeights[originalIndex];
            if (weight < EPSILON) {
                // ignore record: not in current branch or not in sample
                continue;
            }
            final double value = m_sortedData[i];
            final int target = targetColumn.getValueFor(originalIndex);
            final boolean hasValueChanged = (value - lastSeenValue) >= EPSILON;
            final boolean hasTargetChanged = lastSeenTarget != target;
            if (hasTargetChanged) {
                mustTestOnNextValueChange = true;
            }
            if (hasValueChanged && mustTestOnNextValueChange
                    && sumWeightsLeftOfSplit >= minChildNodeSize && sumWeightsRightOfSplit >= minChildNodeSize) {
                tempArray1[0] = impurityCriterion.getPartitionImpurity(targetCountsLeftOfSplit, sumWeightsLeftOfSplit);
                tempArray1[1] = impurityCriterion.getPartitionImpurity(
                           targetCountsRightOfSplit, sumWeightsRightOfSplit);
                tempArray2[0] = sumWeightsLeftOfSplit;
                tempArray2[1] = sumWeightsRightOfSplit;
                double postSplitImpurity = impurityCriterion.getPostSplitImpurity(
                              tempArray1, tempArray2, totalSumWeight);
                if (postSplitImpurity < priorImpurity) {
                    // Use absolute gain (IG) for split calculation even
                    // if the split criterion is information gain ratio (IGR).
                    // IGR wouldn't work as it favors extreme unfair splits,
                    // i.e. 1:9999 would have an attribute entropy
                    // (IGR denominator) of
                    //       9999/10000*log(9999/10000) + 1/10000*log(1/10000)
                    // which is ~0.00148
                    double gain = (priorImpurity - postSplitImpurity);
                    if (gain > bestGain) {
                        bestGainValueForSplit = impurityCriterion.getGain(priorImpurity, postSplitImpurity,
                                tempArray2, totalSumWeight);
                        bestGain = gain;
                        bestSplit = useAverageSplitPoints ? getCenter(lastSeenValue, value) : lastSeenValue;
                    }
                }
                mustTestOnNextValueChange = false;
            }
            targetCountsLeftOfSplit[target] += weight;
            sumWeightsLeftOfSplit += weight;
            targetCountsRightOfSplit[target] -= weight;
            sumWeightsRightOfSplit -= weight;
            lastSeenTarget = target;
            lastSeenValue = value;
        }
        // don't need to check after complete iteration - even if
        // mustTestOnNextValueChange is true - because we didn't run into
        // another split

        if (bestGainValueForSplit < 0.0) {
            // might be negative, because
            //   (0) never seen a valuable split
            //   (1) gain ratio is negative (attribute entropy too large)
            //       (see info gain ratio implementation)
            return null;
        }
        return new NumericSplitCandidate(this, bestSplit, bestGainValueForSplit);
    }

    /** {@inheritDoc} */
    @Override
    public SplitCandidate calcBestSplitRegression(final double[] rowWeights,
            final RegressionPriors targetPriors,
            final TreeTargetNumericColumnData targetColumn) {
        final int[] originalIndexInColumnList = m_originalIndexInColumnList;
        final boolean useAverageSplitPoints = getConfiguration().isUseAverageSplitPoints();
        final int minChildNodeSize = getConfiguration().getMinChildSize();

        final double ySumTotal = targetPriors.getYSum();
        final double nrRecordsTotal = targetPriors.getNrRecords();
        final double criterionTotal = ySumTotal * ySumTotal / nrRecordsTotal;

        double ySumLeft = 0.0;
        double nrRecordsLeft = 0.0;

        double ySumRight = ySumTotal;
        double nrRecordsRight = nrRecordsTotal;

        double bestSplit = Double.NEGATIVE_INFINITY;
        double bestImprovement = 0.0;

        double lastSeenY = Double.NaN;
        double lastSeenValue = Double.NEGATIVE_INFINITY;
        double lastSeenWeight = -1.0;

        // main loop: iterate the entire sorted column, and for each split point
        // compute the gain, keep the one that maximizes the split
        for (int i = 0; i < m_originalIndexInColumnList.length; i++) {
            final int originalIndex = originalIndexInColumnList[i];
            final double weight = rowWeights[originalIndex];
            if (weight < EPSILON) {
                // ignore record: not in current branch or not in sample
                continue;
            } else if (Math.floor(weight) != weight) {
                throw new UnsupportedOperationException("weighted records (missing values?) not supported, "
                        + "weight is " + weight);
            }

            final double value = m_sortedData[i];

            if (lastSeenWeight > 0.0) {
                ySumLeft += lastSeenWeight * lastSeenY;
                ySumRight -= lastSeenWeight * lastSeenY;

                nrRecordsLeft += lastSeenWeight;
                nrRecordsRight -= lastSeenWeight;

                if (nrRecordsLeft >= minChildNodeSize && nrRecordsRight >= minChildNodeSize && lastSeenValue < value) {
                    double criterion = (ySumLeft * ySumLeft / nrRecordsLeft)
                        + (ySumRight * ySumRight / nrRecordsRight) - criterionTotal;
                    if (criterion > bestImprovement) {
                        bestImprovement = criterion;
                        bestSplit = useAverageSplitPoints ? getCenter(lastSeenValue, value) : lastSeenValue;
                    }
                }
            }
            lastSeenY = targetColumn.getValueFor(originalIndex);
            lastSeenValue = value;
            lastSeenWeight = weight;
        }
        assert areApproximatelyEqual(lastSeenWeight, nrRecordsRight) :
            "Expected left weight of " + nrRecordsRight + ", was " + lastSeenWeight;

        assert areApproximatelyEqual(lastSeenWeight * lastSeenY, ySumRight) :
            "Expected y sum of " + ySumRight + " but was " + lastSeenY * lastSeenWeight;
        if (bestImprovement > 0.0) {
            return new NumericSplitCandidate(this, bestSplit, bestImprovement);
        } else {
            return null;
        }
    }

    private static boolean areApproximatelyEqual(final double d1, final double d2) {
        double quot = d1 / d2;
        return Math.abs(quot - 1.0) < 0.001;
    }

    private static double getCenter(final double left, final double right) {
        return left + 0.5 * (right - left);
    }

    /** {@inheritDoc} */
    @Override
    public void updateChildMemberships(final TreeNodeCondition childCondition,
            final double[] parentMemberships, final double[] childMembershipsToUpdate) {
        final TreeNodeNumericCondition numCondition =
            (TreeNodeNumericCondition)childCondition;
        final NumericOperator numOperator = numCondition.getNumericOperator();
        final double splitValue = numCondition.getSplitValue();
        for (int i = 0; i < m_sortedData.length; i++) {
            final double value = m_sortedData[i];
            final int originalColIndex = m_originalIndexInColumnList[i];
            boolean matches;
            switch (numOperator) {
            case LessThanOrEqual:
                matches = value <= splitValue;
                break;
            case LargerThan:
                matches = value > splitValue;
                break;
            default: throw new IllegalStateException(
                    "Unknown operator " + numOperator);
            }
            if (!matches) {
                childMembershipsToUpdate[originalColIndex] = 0.0;
            } else {
                assert childMembershipsToUpdate[originalColIndex]
                    == parentMemberships[originalColIndex];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getMetaData().getAttributeName());
        b.append(" [");
        final int length = m_originalIndexInColumnList.length;
        String[] sample = new String[Math.min(100, length)];
        for (int i = 0; i < length; i++) {
            int trueIndex = m_originalIndexInColumnList[i];
            if (trueIndex < sample.length) {
                sample[trueIndex] = DoubleFormat.formatDouble(m_sortedData[i]);
            }
        }
        for (int i = 0; i < sample.length; i++) {
            b.append(i == 0 ? "" : ", ");
            b.append(sample[i]);
        }
        if (b.length() < length) {
            b.append(", ...");
        }
        b.append("]");
        return b.toString();

    }

}
