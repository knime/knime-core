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
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 27, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.BitSet;

import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.util.MathUtils;
import org.knime.base.node.mine.treeensemble2.data.memberships.ColumnMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.learner.IImpurity;
import org.knime.base.node.mine.treeensemble2.learner.NumericSplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition.NumericOperator;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.MissingValueHandling;
import org.knime.base.node.util.DoubleFormat;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public abstract class TreeNumericColumnData extends TreeAttributeColumnData {

    private final int[] m_originalIndexInColumnList;

    TreeNumericColumnData(final TreeNumericColumnMetaData metaData,
        final TreeEnsembleLearnerConfiguration configuration, final int[] orginalIndexInColumnList) {
        super(metaData, configuration);
        m_originalIndexInColumnList = orginalIndexInColumnList;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNumericColumnMetaData getMetaData() {
        return (TreeNumericColumnMetaData)super.getMetaData();
    }

    /**
     * Gets the value for <b>index</b> in the sorted column
     *
     * @param index
     * @return value for <b>index</b> in sorted column
     */
    public abstract double getSorted(int index);

    /**
     * @return the number of non missing values
     */
    public abstract int getLengthNonMissing();

    private BitSet getMissedRows(final ColumnMemberships columnMemberships) {
        final BitSet missedRows = new BitSet();
        if (columnMemberships.nextIndexFrom(getLengthNonMissing())) {
            do {
                missedRows.set(columnMemberships.getIndexInDataMemberships());
            } while (columnMemberships.next());
        }
        columnMemberships.reset();
        return missedRows;
    }

    @Override
    public NumericSplitCandidate calcBestSplitClassification(final DataMemberships dataMemberships,
        final ClassificationPriors targetPriors, final TreeTargetNominalColumnData targetColumn, final RandomData rd) {
        final TreeEnsembleLearnerConfiguration config = getConfiguration();
        final NominalValueRepresentation[] targetVals = targetColumn.getMetaData().getValues();
        final boolean useAverageSplitPoints = config.isUseAverageSplitPoints();
        final int minChildNodeSize = config.getMinChildSize();

        // distribution of target for each attribute value
        final int targetCounts = targetVals.length;
        final double[] targetCountsLeftOfSplit = new double[targetCounts];
        final double[] targetCountsRightOfSplit = targetPriors.getDistribution().clone();
        assert targetCountsRightOfSplit.length == targetCounts;
        final double totalSumWeight = targetPriors.getNrRecords();
        final IImpurity impurityCriterion = targetPriors.getImpurityCriterion();
        final boolean useXGBoostMissingValueHandling = config.getMissingValueHandling() == MissingValueHandling.XGBoost;

        // get columnMemberships
        final ColumnMemberships columnMemberships =
            dataMemberships.getColumnMemberships(getMetaData().getAttributeIndex());

        // missing value handling
        boolean branchContainsMissingValues = containsMissingValues();
        boolean missingsGoLeft = true;
        final int lengthNonMissing = getLengthNonMissing();
        final double[] missingTargetCounts = new double[targetCounts];

        int lastValidSplitPosition = -1;
        double missingWeight = 0;

        columnMemberships.goToLast();
        do {
            final int indexInColumn = columnMemberships.getIndexInColumn();
            if (indexInColumn >= lengthNonMissing) {
                final double weight = columnMemberships.getRowWeight();
                final int classIdx = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                targetCountsRightOfSplit[classIdx] -= weight;
                missingTargetCounts[classIdx] += weight;
                missingWeight += weight;
            } else {
                if (lastValidSplitPosition < 0) {
                    lastValidSplitPosition = indexInColumn;
                } else if ((getSorted(lastValidSplitPosition) - getSorted(indexInColumn)) >= EPSILON) {
                    break;
                } else {
                    lastValidSplitPosition = indexInColumn;
                }
            }
        } while (columnMemberships.previous());

        // it is possible that the column contains missing values but in the current branch there are no missing values
        branchContainsMissingValues = missingWeight > 0.0;

        columnMemberships.reset();

        double sumWeightsLeftOfSplit = 0.0;
        double sumWeightsRightOfSplit = totalSumWeight - missingWeight;
        final double priorImpurity = useXGBoostMissingValueHandling || !branchContainsMissingValues
            ? targetPriors.getPriorImpurity()
            : impurityCriterion.getPartitionImpurity(
                TreeNominalColumnData.subtractMissingClassCounts(targetPriors.getDistribution(), missingTargetCounts),
                sumWeightsRightOfSplit);

        // all values in branch are missing
        if (sumWeightsRightOfSplit == 0) {
            // it is impossible to determine a split
            return null;
        }

        double bestSplit = Double.NEGATIVE_INFINITY;
        // gain for best split point, unnormalized (not using info gain ratio)
        double bestGain = Double.NEGATIVE_INFINITY;
        // gain for best split, normalized by attribute entropy when
        // info gain ratio is used.
        double bestGainValueForSplit = Double.NEGATIVE_INFINITY;
        final double[] tempArray1 = new double[2];
        double[] tempArray2 = new double[2];

        double lastSeenValue = Double.NEGATIVE_INFINITY;
        boolean mustTestOnNextValueChange = false;
        boolean testSplitOnStart = true;
        boolean firstIteration = true;
        int lastSeenTarget = -1;

        int indexInCol = -1;

        // main loop: iterate the entire sorted column, search for reasonable
        // split points (i.e. where the attribute value changes and value of the
        // target column), and for each split point compute the information
        // gain, keep the one that maximizes the split
        //
        // We iterate over the instances in the sample/branch instead of the whole data set
        while (columnMemberships.next() && (indexInCol = columnMemberships.getIndexInColumn()) < lengthNonMissing) {
            final double weight = columnMemberships.getRowWeight();
            assert weight >= EPSILON : "Rows with zero row weight should never be seen!";
            final double value = getSorted(indexInCol);
            final int target = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
            final boolean hasValueChanged = (value - lastSeenValue) >= EPSILON;
            final boolean hasTargetChanged = lastSeenTarget != target || indexInCol == lastValidSplitPosition;
            if (hasTargetChanged && !firstIteration) {
                mustTestOnNextValueChange = true;
                testSplitOnStart = false;
            }
            if (!firstIteration && hasValueChanged && (mustTestOnNextValueChange || testSplitOnStart)
                && sumWeightsLeftOfSplit >= minChildNodeSize && sumWeightsRightOfSplit >= minChildNodeSize) {
                double postSplitImpurity;
                boolean tempMissingsGoLeft = false;
                // missing value handling
                if (branchContainsMissingValues && useXGBoostMissingValueHandling) {
                    final double[] targetCountsLeftPlusMissing = new double[targetCounts];
                    final double[] targetCountsRightPlusMissing = new double[targetCounts];
                    for (int i = 0; i < targetCounts; i++) {
                        targetCountsLeftPlusMissing[i] = targetCountsLeftOfSplit[i] + missingTargetCounts[i];
                        targetCountsRightPlusMissing[i] = targetCountsRightOfSplit[i] + missingTargetCounts[i];
                    }
                    final double[][] temp = new double[2][2];
                    final double[] postSplitImpurities = new double[2];
                    // send all missing values left
                    tempArray1[0] = impurityCriterion.getPartitionImpurity(targetCountsLeftPlusMissing,
                        sumWeightsLeftOfSplit + missingWeight);
                    tempArray1[1] =
                        impurityCriterion.getPartitionImpurity(targetCountsRightOfSplit, sumWeightsRightOfSplit);
                    temp[0][0] = sumWeightsLeftOfSplit + missingWeight;
                    temp[0][1] = sumWeightsRightOfSplit;
                    postSplitImpurities[0] =
                        impurityCriterion.getPostSplitImpurity(tempArray1, temp[0], totalSumWeight);
                    // send all missing values right
                    tempArray1[0] =
                        impurityCriterion.getPartitionImpurity(targetCountsLeftOfSplit, sumWeightsLeftOfSplit);
                    tempArray1[1] = impurityCriterion.getPartitionImpurity(targetCountsRightPlusMissing,
                        sumWeightsRightOfSplit + missingWeight);
                    temp[1][0] = sumWeightsLeftOfSplit;
                    temp[1][1] = sumWeightsRightOfSplit + missingWeight;
                    postSplitImpurities[1] =
                        impurityCriterion.getPostSplitImpurity(tempArray1, temp[1], totalSumWeight);

                    // take better split
                    if (postSplitImpurities[0] < postSplitImpurities[1]) {
                        postSplitImpurity = postSplitImpurities[0];
                        tempArray2 = temp[0];
                        tempMissingsGoLeft = true;
                        // TODO random tie breaker
                    } else {
                        postSplitImpurity = postSplitImpurities[1];
                        tempArray2 = temp[1];
                        tempMissingsGoLeft = false;
                    }

                } else {
                    tempArray1[0] =
                        impurityCriterion.getPartitionImpurity(targetCountsLeftOfSplit, sumWeightsLeftOfSplit);
                    tempArray1[1] =
                        impurityCriterion.getPartitionImpurity(targetCountsRightOfSplit, sumWeightsRightOfSplit);
                    tempArray2[0] = sumWeightsLeftOfSplit;
                    tempArray2[1] = sumWeightsRightOfSplit;
                    postSplitImpurity = impurityCriterion.getPostSplitImpurity(tempArray1, tempArray2, totalSumWeight);
                }

                if (postSplitImpurity < priorImpurity) {
                    // Use absolute gain (IG) for split calculation even
                    // if the split criterion is information gain ratio (IGR).
                    // IGR wouldn't work as it favors extreme unfair splits,
                    // i.e. 1:9999 would have an attribute entropy
                    // (IGR denominator) of
                    //       9999/10000*log(9999/10000) + 1/10000*log(1/10000)
                    // which is ~0.00148
                    double gain = (priorImpurity - postSplitImpurity);
                    boolean randomTieBreaker = gain == bestGain ? rd.nextInt(0, 1) == 1 : false;
                    if (gain > bestGain || randomTieBreaker) {
                        bestGainValueForSplit =
                            impurityCriterion.getGain(priorImpurity, postSplitImpurity, tempArray2, totalSumWeight);
                        bestGain = gain;
                        bestSplit = useAverageSplitPoints ? getCenter(lastSeenValue, value) : lastSeenValue;
                        // Go with the majority if there are no missing values during training this is because we should
                        // still provide a missing direction for the case that there are missing values during prediction
                        missingsGoLeft = branchContainsMissingValues ? tempMissingsGoLeft
                            : sumWeightsLeftOfSplit > sumWeightsRightOfSplit;
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
            firstIteration = false;
        }
        columnMemberships.reset();
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

        if (useXGBoostMissingValueHandling) {
            //            return new NumericMissingSplitCandidate(this, bestSplit, bestGainValueForSplit, missingsGoLeft);
            return new NumericSplitCandidate(this, bestSplit, bestGainValueForSplit, new BitSet(),
                missingsGoLeft ? NumericSplitCandidate.MISSINGS_GO_LEFT : NumericSplitCandidate.MISSINGS_GO_RIGHT);
        }

        return new NumericSplitCandidate(this, bestSplit, bestGainValueForSplit, getMissedRows(columnMemberships),
            NumericSplitCandidate.NO_MISSINGS);
    }

    @Override
    public SplitCandidate calcBestSplitRegression(final DataMemberships dataMemberships,
        final RegressionPriors targetPriors, final TreeTargetNumericColumnData targetColumn, final RandomData rd) {
        final TreeEnsembleLearnerConfiguration config = getConfiguration();
        final boolean useAverageSplitPoints = config.isUseAverageSplitPoints();
        final int minChildNodeSize = config.getMinChildSize();

        // get columnMemberships
        final ColumnMemberships columnMemberships =
            dataMemberships.getColumnMemberships(getMetaData().getAttributeIndex());

        final int lengthNonMissing = getLengthNonMissing();

        // missing value handling
        final boolean useXGBoostMissingValueHandling = config.getMissingValueHandling() == MissingValueHandling.XGBoost;
        // are there missing values in this column (complete column)
        boolean branchContainsMissingValues = containsMissingValues();
        boolean missingsGoLeft = true;
        double missingWeight = 0.0;
        double missingY = 0.0;

        // check if there are missing values in this rowsample
        if (branchContainsMissingValues) {
            columnMemberships.goToLast();
            while (columnMemberships.getIndexInColumn() >= lengthNonMissing) {
                missingWeight += columnMemberships.getRowWeight();
                missingY += targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                if (!columnMemberships.previous()) {
                    break;
                }
            }
            columnMemberships.reset();
            branchContainsMissingValues = missingWeight > 0.0;
        }

        final double ySumTotal = targetPriors.getYSum() - missingY;
        final double nrRecordsTotal = targetPriors.getNrRecords() - missingWeight;
        final double criterionTotal = useXGBoostMissingValueHandling ? (ySumTotal + missingY) * (ySumTotal + missingY) / (nrRecordsTotal + missingWeight) : ySumTotal * ySumTotal / nrRecordsTotal;

        double ySumLeft = 0.0;
        double nrRecordsLeft = 0.0;

        double ySumRight = ySumTotal;
        double nrRecordsRight = nrRecordsTotal;

        // all values in the current branch are missing
        if (nrRecordsRight == 0) {
            // it is impossible to determine a split
            return null;
        }

        double bestSplit = Double.NEGATIVE_INFINITY;
        double bestImprovement = 0.0;

        double lastSeenY = Double.NaN;
        double lastSeenValue = Double.NEGATIVE_INFINITY;
        double lastSeenWeight = -1.0;

        // main loop: iterate the entire sorted column, and for each split point
        // compute the gain, keep the one that maximizes the split
        while (columnMemberships.next()) {
            final double weight = columnMemberships.getRowWeight();
            if (weight < EPSILON) {
                // ignore record: not in current branch or not in sample
                continue;
            } else if (Math.floor(weight) != weight) {
                throw new UnsupportedOperationException(
                    "weighted records (missing values?) not supported, " + "weight is " + weight);
            }

            final double value = getSorted(columnMemberships.getIndexInColumn());

            if (lastSeenWeight > 0.0) {
                ySumLeft += lastSeenWeight * lastSeenY;
                ySumRight -= lastSeenWeight * lastSeenY;

                nrRecordsLeft += lastSeenWeight;
                nrRecordsRight -= lastSeenWeight;

                if (nrRecordsLeft >= minChildNodeSize && nrRecordsRight >= minChildNodeSize && lastSeenValue < value) {
                    boolean tempMissingsGoLeft = true;
                    double childrenSquaredSum;
                    if (branchContainsMissingValues && useXGBoostMissingValueHandling) {
                        final double[] tempChildrenSquaredSum = new double[2];
                        tempChildrenSquaredSum[0] = ((ySumLeft + missingY) * (ySumLeft + missingY) / (nrRecordsLeft + missingWeight))
                            + (ySumRight * ySumRight / nrRecordsRight);
                        tempChildrenSquaredSum[1] = (ySumLeft * ySumLeft / nrRecordsLeft)
                            + ((ySumRight + missingY) * (ySumRight + missingY) / (nrRecordsRight + missingWeight));
                        if (tempChildrenSquaredSum[0] >= tempChildrenSquaredSum[1]) {
                            childrenSquaredSum = tempChildrenSquaredSum[0];
                            tempMissingsGoLeft = true;
                        } else {
                            childrenSquaredSum = tempChildrenSquaredSum[1];
                            tempMissingsGoLeft = false;
                        }
                    } else {
                        childrenSquaredSum =
                            (ySumLeft * ySumLeft / nrRecordsLeft) + (ySumRight * ySumRight / nrRecordsRight);
                    }
                    double criterion = childrenSquaredSum - criterionTotal;
                    boolean randomTieBreaker = criterion == bestImprovement ? rd.nextInt(0, 1) == 1 : false;
                    if (criterion > bestImprovement || randomTieBreaker) {
                        bestImprovement = criterion;
                        bestSplit = useAverageSplitPoints ? getCenter(lastSeenValue, value) : lastSeenValue;
                        // if there are no missing values go with majority
                        missingsGoLeft = branchContainsMissingValues ? tempMissingsGoLeft : nrRecordsLeft >= nrRecordsRight;
                    }
                }
            }
            lastSeenY = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
            lastSeenValue = value;
            lastSeenWeight = weight;
        }
//        assert areApproximatelyEqual(lastSeenWeight, nrRecordsRight) : "Expected left weight of " + nrRecordsRight
//            + ", was " + lastSeenWeight;

        //        assert areApproximatelyEqual(lastSeenWeight * lastSeenY, ySumRight) : "Expected y sum of " + ySumRight
        //            + " but was " + lastSeenY * lastSeenWeight;
        if (bestImprovement > 0.0) {
            if (useXGBoostMissingValueHandling) {
                //                return new NumericMissingSplitCandidate(this, bestSplit, bestImprovement, missingsGoLeft);
                return new NumericSplitCandidate(this, bestSplit, bestImprovement, new BitSet(),
                    missingsGoLeft ? NumericSplitCandidate.MISSINGS_GO_LEFT : NumericSplitCandidate.MISSINGS_GO_RIGHT);
            }
            return new NumericSplitCandidate(this, bestSplit, bestImprovement, getMissedRows(columnMemberships),
                NumericSplitCandidate.NO_MISSINGS);
        } else {
            return null;
        }
    }

    /**
     * @param value
     * @return first index with value equal to <b>value</b> (or larger than <b>value</b> if there is no index with equal
     *         value).
     */
    abstract protected int getFirstIndexWithValue(double value);

    private static boolean areApproximatelyEqual(final double d1, final double d2) {
        return MathUtils.equals(d1, d2, 0.0001);
    }

    private static double getCenter(final double left, final double right) {
        return left + 0.5 * (right - left);
    }

    private double[] getMeanForClass(final ColumnMemberships columnMemberships,
        final TreeTargetNominalColumnData targetColumn, final NominalValueRepresentation[] targetVals,
        final int targetValCount) {
        final double[] means = new double[targetValCount];
        final double[] targetCounters = new double[targetValCount];
        final int lengthNonMissing = getLengthNonMissing();
        while (columnMemberships.next() || columnMemberships.getIndexInColumn() < lengthNonMissing) {
            final int tar = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
            means[tar] += getSorted(columnMemberships.getIndexInColumn());
            targetCounters[tar] += columnMemberships.getRowWeight();
        }

        for (int i = 0; i < means.length; i++) {
            means[i] = means[i] / targetCounters[i];
        }

        return means;
    }

    @Override
    public BitSet updateChildMemberships(final TreeNodeCondition childCondition,
        final DataMemberships parentMemberships) {
        final TreeNodeNumericCondition numCondition = (TreeNodeNumericCondition)childCondition;
        final NumericOperator numOperator = numCondition.getNumericOperator();
        final double splitValue = numCondition.getSplitValue();
        final ColumnMemberships columnMemberships =
            parentMemberships.getColumnMemberships(getMetaData().getAttributeIndex());
        columnMemberships.reset();
        final BitSet inChild = new BitSet(columnMemberships.size());
        int startIndex = 0;
        //        if (numOperator.equals(NumericOperator.LargerThan)) {
        //            // jump to index with splitvalue OR first index larger than splitvalue
        //            startIndex = getFirstIndexWithValue(splitValue);
        //        }
        if (!columnMemberships.nextIndexFrom(startIndex)) {
            throw new IllegalStateException(
                "The current columnMemberships object contains no element that satisfies the splitcondition");
        }
        final int lengthNonMissing = getLengthNonMissing();
        do {
            final double value = getSorted(columnMemberships.getIndexInColumn());
            boolean matches;
            switch (numOperator) {
                case LessThanOrEqual:
                    matches = value <= splitValue;
                    break;
                case LargerThan:
                    matches = value > splitValue;
                    break;
                case LessThanOrEqualOrMissing:
                    matches = Double.isNaN(value) ? true : value <= splitValue;
                    break;
                case LargerThanOrMissing:
                    matches = Double.isNaN(value) ? true : value > splitValue;
                    break;
                default:
                    throw new IllegalStateException("Unknown operator " + numOperator);
            }
            if (matches) {
                inChild.set(columnMemberships.getIndexInDataMemberships());
            }
        } while (columnMemberships.next() && columnMemberships.getIndexInColumn() < lengthNonMissing);

        // reached end of columnMemberships
        if (columnMemberships.getIndexInColumn() < lengthNonMissing) {
            return inChild;
        }

        // handle missing values
        if (numOperator.equals(NumericOperator.LessThanOrEqualOrMissing)
            || numOperator.equals(NumericOperator.LargerThanOrMissing) || numCondition.acceptsMissings()) {
            do {
                inChild.set(columnMemberships.getIndexInDataMemberships());
            } while (columnMemberships.next());
        }

        return inChild;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getMetaData().getAttributeName());
        b.append(" [");
        final int length = m_originalIndexInColumnList.length;
        String[] sample = new String[Math.min(100, length)];
        for (int i = 0; i < length; i++) {
            int trueIndex = m_originalIndexInColumnList[i];
            if (trueIndex < sample.length) {
                sample[trueIndex] = DoubleFormat.formatDouble(getSorted(i));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getOriginalIndicesInColumnList() {
        return m_originalIndexInColumnList;
    }

    @Override
    public Object getValueAt(final int indexInColumn) {
        return getSorted(indexInColumn);
    }

}
