/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 28, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.random.RandomData;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.knime.base.node.mine.treeensemble2.data.BinaryNominalSplitsPCA.CombinedAttributeValues;
import org.knime.base.node.mine.treeensemble2.data.memberships.ColumnMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.learner.IImpurity;
import org.knime.base.node.mine.treeensemble2.learner.NominalBinarySplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.NominalMultiwaySplitCandidate;
import org.knime.base.node.mine.treeensemble2.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalBinaryCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalCondition;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.MissingValueHandling;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeNominalColumnData extends TreeAttributeColumnData {

    private final int[] m_nominalValueCounts;

    private final int[] m_originalIndexInColumnList;

    private final int m_idxOfFirstMissing;

    TreeNominalColumnData(final TreeNominalColumnMetaData metaData,
        final TreeEnsembleLearnerConfiguration configuration, final int[] nominalValueCounts,
        final int[] originalIndexInColumnList, final boolean containsMissingValues) {
        super(metaData, configuration);
        m_nominalValueCounts = nominalValueCounts;
        m_originalIndexInColumnList = originalIndexInColumnList;
        if (containsMissingValues) {
            int lengthNonMissing = 0;
            for (int i = 0; i < m_nominalValueCounts.length - 1; i++) {
                lengthNonMissing += m_nominalValueCounts[i];
            }
            m_idxOfFirstMissing = lengthNonMissing - 1;
        } else {
            m_idxOfFirstMissing = -1;
        }
    }

    /** {@inheritDoc} */
    @Override
    public TreeNominalColumnMetaData getMetaData() {
        return (TreeNominalColumnMetaData)super.getMetaData();
    }

    private BitSet getMissedRows(final ColumnMemberships columnMemberships) {
        // during creation it is ensured that if there are missing values, they are the last nominal value
        if (m_idxOfFirstMissing != -1) {
            final BitSet missedRows = new BitSet();
            if (columnMemberships.nextIndexFrom(m_idxOfFirstMissing)) {
                do {
                    missedRows.set(columnMemberships.getIndexInDataMemberships());
                } while (columnMemberships.next());
            }
            columnMemberships.reset();
            return missedRows;
        } else {
            return NO_MISSED_ROWS;
        }
    }

    /** {@inheritDoc} */
    @Override
    public SplitCandidate calcBestSplitClassification(final DataMemberships dataMemberships,
        final ClassificationPriors targetPriors, final TreeTargetNominalColumnData targetColumn, final RandomData rd) {
        final NominalValueRepresentation[] targetVals = targetColumn.getMetaData().getValues();
        IImpurity impCriterion = targetPriors.getImpurityCriterion();
        // distribution of target for each attribute value
        final NominalValueRepresentation[] nomVals = getMetaData().getValues();
        final boolean useBinaryNominalSplits = getConfiguration().isUseBinaryNominalSplits();
        final ColumnMemberships columnMemberships =
            dataMemberships.getColumnMemberships(getMetaData().getAttributeIndex());

        if (useBinaryNominalSplits) {
            if (targetVals.length == 2) {
                return calcBestSplitClassificationBinaryTwoClass(columnMemberships, targetPriors, targetColumn,
                    impCriterion, nomVals, targetVals, rd);
            } else {
                return calcBestSplitClassificationBinaryPCA(columnMemberships, targetPriors, targetColumn, impCriterion,
                    nomVals, targetVals, rd);
                //                return calcBestSplitClassificationBinary(membershipController, rowWeights, targetPriors, targetColumn,
                //                    impCriterion, nomVals, targetVals, originalIndexInColumnList, rd);
            }
        } else {
            return calcBestSplitClassificationMultiway(columnMemberships, targetPriors, targetColumn, impCriterion,
                nomVals, targetVals, rd);
        }
    }

    private NominalMultiwaySplitCandidate calcBestSplitClassificationMultiway(final ColumnMemberships columnMemberships,
        final ClassificationPriors targetPriors, final TreeTargetNominalColumnData targetColumn,
        final IImpurity impCriterion, final NominalValueRepresentation[] nomVals,
        final NominalValueRepresentation[] targetVals, final RandomData rd) {

        final boolean useXGBoostMissingValueHandling =
            getConfiguration().getMissingValueHandling() == MissingValueHandling.XGBoost;
        // missing values are stored as last nominal value
        final int lengthNonMissing = containsMissingValues() ? nomVals.length - 1 : nomVals.length;

        // distribution of target for each attribute value
        final double[][] targetCountsSplit = new double[lengthNonMissing][targetVals.length];
        // number of valid records for each attribute value
        final double[] attWeights = new double[lengthNonMissing];
        // entropy wrt target column for each attribute value
        final double[] attEntropys = new double[lengthNonMissing];
        // number (sum) of total valid values
        double totalWeight = 0.0;
        boolean branchContainsMissingValues = containsMissingValues();
        double highestWeight = 0.0;
        int missingsGoWithIdx = -1;

        int start = 0;
        columnMemberships.next();
        for (int att = 0; att < lengthNonMissing; att++) {
            int end = start + m_nominalValueCounts[att];
            Arrays.fill(targetCountsSplit[att], 0.0);
            double currentAttValWeight = 0.0;
            boolean reachedEnd = false;
            for (int index = columnMemberships.getIndexInColumn(); index < end; index =
                columnMemberships.getIndexInColumn()) {
                final double weight = columnMemberships.getRowWeight();

                assert weight > EPSILON : "Instances in columnMemberships must have weights larger than EPSILON.";
                final int target = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                targetCountsSplit[att][target] += weight;
                currentAttValWeight += weight;
                if (!columnMemberships.next()) {
                    // reached end of columnMemberships
                    reachedEnd = true;
                    break;
                }

            }
            totalWeight += currentAttValWeight;
            attWeights[att] = currentAttValWeight;
            //            attEntropys[att] = impCriterion.getPartitionImpurity(targetCountsSplit, currentAttValWeight);
            start = end;
            if (currentAttValWeight > highestWeight) {
                missingsGoWithIdx = att;
                highestWeight = currentAttValWeight;
            }
            if (reachedEnd) {
                break;
            }
        }

        // account for missing values and their weight
        double missingWeight = 0.0;
        double[] missingClassCounts = null;
        // if there are missing values in the branch, start represents the beginning of the missing values
        // otherwise the current indexInColumn won't be larger than start
        if (columnMemberships.getIndexInColumn() >= start) {
            missingClassCounts = new double[targetVals.length];
            do {
                final double recordWeight = columnMemberships.getRowWeight();
                final int recordClass = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                missingWeight += recordWeight;
                missingClassCounts[recordClass] += recordWeight;
            } while (columnMemberships.next());
        }

        if (missingWeight > EPSILON) {
            branchContainsMissingValues = true;
        }

        double gain = Double.NEGATIVE_INFINITY;
        if (branchContainsMissingValues && useXGBoostMissingValueHandling) {
            // send missing values with each possible nominal value and take the best
            for (int i = 0; i < lengthNonMissing; i++) {
                // send missing values with current attribute
                attWeights[i] += missingWeight;
                for (int j = 0; j < lengthNonMissing; j++) {
                    if (i == j) {
                        attEntropys[j] = impCriterion.getPartitionImpurity(
                            addMissingClassCounts(targetCountsSplit[i], missingClassCounts), attWeights[j]);
                    } else {
                        attEntropys[j] = impCriterion.getPartitionImpurity(targetCountsSplit[j], attWeights[j]);
                    }
                }

                final double postSplitImpurity =
                    impCriterion.getPostSplitImpurity(attEntropys, attWeights, totalWeight + missingWeight);
                final double tempGain = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity,
                    attWeights, totalWeight + missingWeight);
                if (tempGain > gain) {
                    gain = tempGain;
                    missingsGoWithIdx = i;
                }
                // restore weight of current attribute
                attWeights[i] -= missingWeight;
            }
        } else {
            for (int i = 0; i < lengthNonMissing; i++) {
                attEntropys[i] = impCriterion.getPartitionImpurity(targetCountsSplit[i], attWeights[i]);
            }
            double postSplitImpurity = impCriterion.getPostSplitImpurity(attEntropys, attWeights, totalWeight);
            gain = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity, attWeights, totalWeight);
        }
        if (useXGBoostMissingValueHandling) {
            if (!branchContainsMissingValues) {
                // ensure that missing values are sent to the child that the majority of the rows are sent to.
                double majorityWeight = 0.0;
                for (int i = 0; i < attWeights.length; i++) {
                    if (attWeights[i] > majorityWeight) {
                        missingsGoWithIdx = i;
                        majorityWeight = attWeights[i];
                    }
                }
            }
            return new NominalMultiwaySplitCandidate(this, gain, attWeights, NO_MISSED_ROWS, missingsGoWithIdx);
        }
        return new NominalMultiwaySplitCandidate(this, gain, attWeights, getMissedRows(columnMemberships),
            NominalMultiwaySplitCandidate.NO_MISSINGS);
    }

    NominalBinarySplitCandidate calcBestSplitClassificationBinary(final ColumnMemberships columnMemberships,
        final ClassificationPriors targetPriors, final TreeTargetNominalColumnData targetColumn,
        final IImpurity impCriterion, final NominalValueRepresentation[] nomVals,
        final NominalValueRepresentation[] targetVals, final RandomData rd) {
        if (nomVals.length <= 1) {
            return null;
        }
        final int minChildSize = getConfiguration().getMinChildSize();

        final int lengthNonMissing = containsMissingValues() ? nomVals.length - 1 : nomVals.length;

        // distribution of target for each attribute value
        final double[][] targetCountsSplitPerAttribute = new double[lengthNonMissing][targetVals.length];
        // number of valid records for each attribute value
        final double[] attWeights = new double[lengthNonMissing];

        // number (sum) of total valid values
        double totalWeight = 0.0;
        int start = 0;
        columnMemberships.next();
        for (int att = 0; att < lengthNonMissing; att++) {
            final int end = start + m_nominalValueCounts[att];
            double currentAttValWeight = 0.0;
            for (int index = columnMemberships.getIndexInColumn(); index < end; columnMemberships.next(), index =
                columnMemberships.getIndexInColumn()) {
                final double weight = columnMemberships.getRowWeight();
                assert weight > EPSILON : "The usage of datamemberships should ensure that no rows with zero weight are encountered";
                int target = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                targetCountsSplitPerAttribute[att][target] += weight;
                currentAttValWeight += weight;
            }
            totalWeight += currentAttValWeight;
            attWeights[att] = currentAttValWeight;
            start = end;
        }
        BinarySplitEnumeration splitEnumeration;
        if (nomVals.length <= 10) {
            splitEnumeration = new FullBinarySplitEnumeration(nomVals.length);
        } else {
            int maxSearch = (1 << 10 - 2);
            splitEnumeration = new RandomBinarySplitEnumeration(nomVals.length, maxSearch, rd);
        }
        BigInteger bestPartitionMask = null;
        boolean isBestSplitValid = false;
        double bestPartitionGain = Double.NEGATIVE_INFINITY;
        final double[] targetCountsSplitLeft = new double[targetVals.length];
        final double[] targetCountsSplitRight = new double[targetVals.length];

        final double[] binaryImpurityValues = new double[2];
        final double[] binaryPartitionWeights = new double[2];
        do {
            Arrays.fill(targetCountsSplitLeft, 0.0);
            Arrays.fill(targetCountsSplitRight, 0.0);
            double weightLeft = 0.0;
            double weightRight = 0.0;
            for (int i = 0; i < nomVals.length; i++) {
                final boolean isAttributeInRightBranch = splitEnumeration.isInRightBranch(i);
                double[] targetCountsCurrentAttribute = targetCountsSplitPerAttribute[i];
                for (int targetVal = 0; targetVal < targetVals.length; targetVal++) {
                    if (isAttributeInRightBranch) {
                        targetCountsSplitRight[targetVal] += targetCountsCurrentAttribute[targetVal];
                    } else {
                        targetCountsSplitLeft[targetVal] += targetCountsCurrentAttribute[targetVal];
                    }
                }
                if (isAttributeInRightBranch) {
                    weightRight += attWeights[i];
                } else {
                    weightLeft += attWeights[i];
                }
            }
            binaryPartitionWeights[0] = weightRight;
            binaryPartitionWeights[1] = weightLeft;
            boolean isValidSplit = weightRight >= minChildSize && weightLeft >= minChildSize;
            binaryImpurityValues[0] = impCriterion.getPartitionImpurity(targetCountsSplitRight, weightRight);
            binaryImpurityValues[1] = impCriterion.getPartitionImpurity(targetCountsSplitLeft, weightLeft);
            double postSplitImpurity =
                impCriterion.getPostSplitImpurity(binaryImpurityValues, binaryPartitionWeights, totalWeight);
            double gain = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity,
                binaryPartitionWeights, totalWeight);
            // use random tie breaker if gains are equal
            boolean randomTieBreaker = gain == bestPartitionGain ? rd.nextInt(0, 1) == 1 : false;
            // store if better than before or first valid split
            if (gain > bestPartitionGain || (!isBestSplitValid && isValidSplit) || randomTieBreaker) {
                if (isValidSplit || !isBestSplitValid) {
                    bestPartitionGain = gain;
                    bestPartitionMask = splitEnumeration.getValueMask();
                    isBestSplitValid = isValidSplit;
                }
            }
        } while (splitEnumeration.next());
        if (bestPartitionGain > 0.0) {
            return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask,
                getMissedRows(columnMemberships), NominalBinarySplitCandidate.NO_MISSINGS);
        }
        return null;
    }

    /**
     * Implements the approach proposed by Coppersmith et al. (1999) in their paper
     * "Partitioning Nominal Attributes in Decision Trees"
     *
     * @param membershipController
     * @param rowWeights
     * @param targetPriors
     * @param targetColumn
     * @param impCriterion
     * @param nomVals
     * @param targetVals
     * @param originalIndexInColumnList
     * @return the best binary split candidate or null if there is no valid split with positive gain
     */
    private NominalBinarySplitCandidate calcBestSplitClassificationBinaryPCA(final ColumnMemberships columnMemberships,
        final ClassificationPriors targetPriors, final TreeTargetNominalColumnData targetColumn,
        final IImpurity impCriterion, final NominalValueRepresentation[] nomVals,
        final NominalValueRepresentation[] targetVals, final RandomData rd) {
        final TreeEnsembleLearnerConfiguration config = getConfiguration();
        final int minChildSize = config.getMinChildSize();
        final boolean useXGBoostMissingValueHandling = config.getMissingValueHandling() == MissingValueHandling.XGBoost;

        // The algorithm combines attribute values with the same class probabilities into a single attribute
        // therefore it is necessary to track the known classProbabilities
        final LinkedHashMap<ClassProbabilityVector, CombinedAttributeValues> combinedAttValsMap =
            new LinkedHashMap<ClassProbabilityVector, CombinedAttributeValues>();

        columnMemberships.next();
        double totalWeight = 0.0;
        boolean branchContainsMissingValues = containsMissingValues();
        int start = 0;
        final int lengthNonMissing = containsMissingValues() ? nomVals.length - 1 : nomVals.length;
        final int attToConsider = useXGBoostMissingValueHandling ? nomVals.length : lengthNonMissing;
        for (int att = 0; att < lengthNonMissing /*attToConsider*/; att++) {
            int end = start + m_nominalValueCounts[att];
            double attWeight = 0.0;
            final double[] classFrequencies = new double[targetVals.length];
            boolean reachedEnd = false;
            for (int index = columnMemberships.getIndexInColumn(); index < end; index =
                columnMemberships.getIndexInColumn()) {
                double weight = columnMemberships.getRowWeight();

                assert weight > EPSILON : "Instances in columnMemberships must have weights larger than EPSILON.";

                int instanceClass = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                classFrequencies[instanceClass] += weight;
                attWeight += weight;
                totalWeight += weight;

                if (!columnMemberships.next()) {
                    // reached end of columnMemberships
                    reachedEnd = true;
                    if (att == nomVals.length - 1) {
                        // if the column contains no missing values, the last possible nominal value is
                        // not the missing value and therefore branchContainsMissingValues needs to be false
                        branchContainsMissingValues = branchContainsMissingValues && true;
                    }
                    break;
                }
            }
            start = end;

            if (attWeight < EPSILON) {
                // attribute value did not occur in this branch or sample
                continue;
            }

            final double[] classProbabilities = new double[targetVals.length];
            for (int i = 0; i < classProbabilities.length; i++) {
                classProbabilities[i] = truncateDouble(8, classFrequencies[i] / attWeight);
            }
            CombinedAttributeValues attVal =
                new CombinedAttributeValues(classFrequencies, classProbabilities, attWeight, nomVals[att]);
            ClassProbabilityVector classProbabilityVector = new ClassProbabilityVector(classProbabilities);
            CombinedAttributeValues knownAttVal = combinedAttValsMap.get(classProbabilityVector);
            if (knownAttVal == null) {
                combinedAttValsMap.put(classProbabilityVector, attVal);
            } else {
                knownAttVal.combineAttributeValues(attVal);
            }

            if (reachedEnd) {
                break;
            }
        }

        // account for missing values and their weight
        double missingWeight = 0.0;
        double[] missingClassCounts = null;
        // if there are missing values in the branch, start represents the beginning of the missing values
        // otherwise the current indexInColumn won't be larger than start
        if (columnMemberships.getIndexInColumn() >= start) {
            missingClassCounts = new double[targetVals.length];
            do {
                final double recordWeight = columnMemberships.getRowWeight();
                final int recordClass = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                missingWeight += recordWeight;
                missingClassCounts[recordClass] += recordWeight;
            } while (columnMemberships.next());
        }

        if (missingWeight > EPSILON) {
            branchContainsMissingValues = true;
        }

        ArrayList<CombinedAttributeValues> attValList = Lists.newArrayList(combinedAttValsMap.values());
        CombinedAttributeValues[] attVals =
            combinedAttValsMap.values().toArray(new CombinedAttributeValues[combinedAttValsMap.size()]);

        attVals = BinaryNominalSplitsPCA.calculatePCAOrdering(attVals, totalWeight, targetVals.length);
        // EigenDecomposition failed
        if (attVals == null) {
            return null;
        }

        // Start searching for split candidates
        final int highestBitPosition = containsMissingValues() ? nomVals.length - 2 : nomVals.length - 1;

        final double[] binaryImpurityValues = new double[2];
        final double[] binaryPartitionWeights = new double[2];

        double sumRemainingWeights = totalWeight;
        double sumCurrPartitionWeight = 0.0;
        RealVector targetFrequenciesCurrentPartition = MatrixUtils.createRealVector(new double[targetVals.length]);
        RealVector targetFrequenciesRemaining = MatrixUtils.createRealVector(new double[targetVals.length]);
        for (CombinedAttributeValues attVal : attValList) {
            targetFrequenciesRemaining = targetFrequenciesRemaining.add(attVal.m_classFrequencyVector);
        }
        BigInteger currPartitionBitMask = BigInteger.ZERO;

        double bestPartitionGain = Double.NEGATIVE_INFINITY;
        BigInteger bestPartitionMask = null;
        boolean isBestSplitValid = false;
        boolean missingsGoLeft = false;

        for (int i = 0; i < attVals.length; i++) {
            CombinedAttributeValues currAttVal = attVals[i];
            sumCurrPartitionWeight += currAttVal.m_totalWeight;
            sumRemainingWeights -= currAttVal.m_totalWeight;

            targetFrequenciesCurrentPartition =
                targetFrequenciesCurrentPartition.add(currAttVal.m_classFrequencyVector);
            targetFrequenciesRemaining = targetFrequenciesRemaining.subtract(currAttVal.m_classFrequencyVector);

            currPartitionBitMask = currPartitionBitMask.or(currAttVal.m_bitMask);
            boolean partitionIsRightBranch = currPartitionBitMask.testBit(highestBitPosition);

            boolean isValidSplit;
            double gain;
            boolean tempMissingsGoLeft = true;

            if (branchContainsMissingValues && useXGBoostMissingValueHandling) {
                // send missing values with partition
                boolean isValidSplitFirst =
                    sumCurrPartitionWeight + missingWeight >= minChildSize && sumRemainingWeights >= minChildSize;
                binaryImpurityValues[0] = impCriterion.getPartitionImpurity(
                    addMissingClassCounts(targetFrequenciesCurrentPartition.toArray(), missingClassCounts),
                    sumCurrPartitionWeight + missingWeight);
                binaryImpurityValues[1] =
                    impCriterion.getPartitionImpurity(targetFrequenciesRemaining.toArray(), sumRemainingWeights);

                binaryPartitionWeights[0] = sumCurrPartitionWeight + missingWeight;
                binaryPartitionWeights[1] = sumRemainingWeights;

                double postSplitImpurity = impCriterion.getPostSplitImpurity(binaryImpurityValues,
                    binaryPartitionWeights, totalWeight + missingWeight);

                double gainFirst = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity,
                    binaryPartitionWeights, totalWeight + missingWeight);

                //send missing values with remaining
                boolean isValidSplitSecond =
                    sumCurrPartitionWeight >= minChildSize && sumRemainingWeights + missingWeight >= minChildSize;
                binaryImpurityValues[0] = impCriterion.getPartitionImpurity(
                    targetFrequenciesCurrentPartition.toArray(), sumCurrPartitionWeight);
                binaryImpurityValues[1] = impCriterion.getPartitionImpurity(
                    addMissingClassCounts(targetFrequenciesRemaining.toArray(), missingClassCounts),
                    sumRemainingWeights + missingWeight);

                binaryPartitionWeights[0] = sumCurrPartitionWeight;
                binaryPartitionWeights[1] = sumRemainingWeights + missingWeight;

                postSplitImpurity = impCriterion.getPostSplitImpurity(binaryImpurityValues, binaryPartitionWeights,
                    totalWeight + missingWeight);

                double gainSecond = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity,
                    binaryPartitionWeights, totalWeight + missingWeight);

                // choose alternative with better gain
                if (gainFirst >= gainSecond) {
                    gain = gainFirst;
                    isValidSplit = isValidSplitFirst;
                    tempMissingsGoLeft = !partitionIsRightBranch;
                } else {
                    gain = gainSecond;
                    isValidSplit = isValidSplitSecond;
                    tempMissingsGoLeft = partitionIsRightBranch;
                }
            } else {
                // TODO if invalid splits should not be considered skip partition
                isValidSplit = sumCurrPartitionWeight >= minChildSize && sumRemainingWeights >= minChildSize;

                binaryImpurityValues[0] = impCriterion.getPartitionImpurity(targetFrequenciesCurrentPartition.toArray(),
                    sumCurrPartitionWeight);
                binaryImpurityValues[1] =
                    impCriterion.getPartitionImpurity(targetFrequenciesRemaining.toArray(), sumRemainingWeights);

                binaryPartitionWeights[0] = sumCurrPartitionWeight;
                binaryPartitionWeights[1] = sumRemainingWeights;

                double postSplitImpurity =
                    impCriterion.getPostSplitImpurity(binaryImpurityValues, binaryPartitionWeights, totalWeight);

                gain = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity, binaryPartitionWeights,
                    totalWeight);
            }
            // use random tie breaker if gains are equal
            boolean randomTieBreaker = gain == bestPartitionGain ? rd.nextInt(0, 1) == 1 : false;
            // store if better than before or first valid split
            if (gain > bestPartitionGain || (!isBestSplitValid && isValidSplit) || randomTieBreaker) {
                if (isValidSplit || !isBestSplitValid) {
                    bestPartitionGain = gain;
                    bestPartitionMask = partitionIsRightBranch ? currPartitionBitMask : BigInteger.ZERO
                        .setBit(highestBitPosition + 1).subtract(BigInteger.ONE).xor(currPartitionBitMask);
                    isBestSplitValid = isValidSplit;
                    if (branchContainsMissingValues) {
                        missingsGoLeft = tempMissingsGoLeft;
                        // missing values are encountered during the search for the best split
                        //                        missingsGoLeft = partitionIsRightBranch;
                    } else {
                        // no missing values were encountered during the search for the best split
                        // missing values should be sent with the majority
                        missingsGoLeft = partitionIsRightBranch ? sumCurrPartitionWeight < sumRemainingWeights
                            : sumCurrPartitionWeight >= sumRemainingWeights;
                    }
                }
            }
        }

        if (isBestSplitValid && bestPartitionGain > 0.0) {
            if (useXGBoostMissingValueHandling) {
                return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask, NO_MISSED_ROWS,
                    missingsGoLeft ? NominalBinarySplitCandidate.MISSINGS_GO_LEFT
                        : NominalBinarySplitCandidate.MISSINGS_GO_RIGHT);
            }
            return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask,
                getMissedRows(columnMemberships), NominalBinarySplitCandidate.NO_MISSINGS);
        }

        return null;
    }

    private static double[] addMissingClassCounts(final double[] nonMissingClassCounts,
        final double[] missingClassCounts) {
        final double[] result = new double[nonMissingClassCounts.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = nonMissingClassCounts[i] + missingClassCounts[i];
        }
        return result;
    }

    private static double truncateDouble(final int positions, final double value) {
        Double toTruncate = Double.valueOf(value);
        BigDecimal truncated = new BigDecimal(toTruncate).setScale(positions, BigDecimal.ROUND_HALF_UP);

        return truncated.doubleValue();
    }

    private static class ClassProbabilityVector {
        private final double[] m_data;

        public ClassProbabilityVector(final double[] data) {
            m_data = data;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ClassProbabilityVector) {
                ClassProbabilityVector that = (ClassProbabilityVector)obj;
                if (m_data.length == that.m_data.length) {
                    return Arrays.equals(m_data, that.m_data);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(m_data);
        }
    }

    private NominalBinarySplitCandidate calcBestSplitClassificationBinaryTwoClass(
        final ColumnMemberships columnMemberships, final ClassificationPriors targetPriors,
        final TreeTargetNominalColumnData targetColumn, final IImpurity impCriterion,
        final NominalValueRepresentation[] nomVals, final NominalValueRepresentation[] targetVals,
        final RandomData rd) {

        if (targetColumn.getMetaData().getValues().length != 2) {
            throw new IllegalArgumentException("This method can only be used for two class problems.");
        }
        final TreeEnsembleLearnerConfiguration config = getConfiguration();
        final int minChildSize = config.getMinChildSize();
        final boolean useXGBoostMissingValueHandling = config.getMissingValueHandling() == MissingValueHandling.XGBoost;

        int start = 0;
        final int firstClass = targetColumn.getMetaData().getValues()[0].getAssignedInteger();
        double totalWeight = 0.0;
        double totalFirstClassWeight = 0.0;
        final ArrayList<NomValProbabilityPair> nomValProbabilities = new ArrayList<NomValProbabilityPair>();
        if (!columnMemberships.next()) {
            throw new IllegalStateException("The columnMemberships has not been reset or is empty.");
        }
        final int lengthNonMissing = containsMissingValues() ? nomVals.length - 1 : nomVals.length;
        //        final int attToConsider = useXGBoostMissingValueHandling ? nomVals.length : lengthNonMissing;
        boolean branchContainsMissingValues = containsMissingValues();
        // calculate probabilities for first class in each nominal value
        for (int att = 0; att < /*attToConsider*/ lengthNonMissing; att++) {
            int end = start + m_nominalValueCounts[att];
            double attFirstClassWeight = 0;
            double attWeight = 0;

            boolean reachedEnd = false;
            for (int index = columnMemberships.getIndexInColumn(); index < end; index =
                columnMemberships.getIndexInColumn()) {
                double weight = columnMemberships.getRowWeight();

                assert weight > EPSILON : "Instances in columnMemberships must have weights larger than EPSILON.";

                final int instanceClass = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                if (instanceClass == firstClass) {
                    attFirstClassWeight += weight;
                    totalFirstClassWeight += weight;
                }
                attWeight += weight;
                totalWeight += weight;
                if (!columnMemberships.next()) {
                    // reached end of columnMemberships
                    reachedEnd = true;
                    if (att == nomVals.length - 1) {
                        // if the column contains no missing values, the last possible nominal value is
                        // not the missing value and therefore branchContainsMissingValues needs to be false
                        branchContainsMissingValues = branchContainsMissingValues && true;
                    }
                    break;
                }
            }

            if (attWeight > 0) {
                final double firstClassProbability = attFirstClassWeight / attWeight;
                final NominalValueRepresentation nomVal = getMetaData().getValues()[att];
                nomValProbabilities
                    .add(new NomValProbabilityPair(nomVal, firstClassProbability, attWeight, attFirstClassWeight));
            }

            start = end;
            if (reachedEnd) {
                break;
            }
        }

        // account for missing values and their weight
        double missingWeight = 0.0;
        double missingWeightFirstClass = 0.0;
        // if there are missing values in the branch, start represents the beginning of the missing values
        // otherwise the current indexInColumn won't be larger than start
        if (columnMemberships.getIndexInColumn() >= start) {
            do {
                final double recordWeight = columnMemberships.getRowWeight();
                missingWeight += recordWeight;
                final int recordClass = targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                if (recordClass == firstClass) {
                    missingWeightFirstClass += recordWeight;
                }
            } while (columnMemberships.next());
        }

        if (missingWeight > EPSILON) {
            branchContainsMissingValues = true;
        }

        nomValProbabilities.sort(null);
        int highestBitPosition = getMetaData().getValues().length - 1;
        if (containsMissingValues()) {
            highestBitPosition--;
        }

        final double[] targetCountsSplitPartition = new double[2];
        final double[] targetCountsSplitRemaining = new double[2];

        final double[] binaryImpurityValues = new double[2];
        final double[] binaryPartitionWeights = new double[2];

        BigInteger partitionMask = BigInteger.ZERO;

        double bestPartitionGain = Double.NEGATIVE_INFINITY;
        BigInteger bestPartitionMask = null;
        boolean isBestSplitValid = false;

        double sumWeightsPartitionTotal = 0.0;
        double sumWeightsPartitionFirstClass = 0.0;
        boolean missingsGoLeft = false;

        for (int i = 0; i < nomValProbabilities.size() - 1; i++) {
            NomValProbabilityPair nomVal = nomValProbabilities.get(i);
            sumWeightsPartitionTotal += nomVal.m_sumWeights;
            sumWeightsPartitionFirstClass += nomVal.m_firstClassSumWeights;
            partitionMask = partitionMask.or(nomVal.m_bitMask);

            // check if split represented by currentSplitList is in the right branch
            // by convention a split goes towards the right branch if the highest possible bit is set to 1
            final boolean isRightBranch = partitionMask.testBit(highestBitPosition);
            double gain;
            boolean isValidSplit;
            boolean tempMissingsGoLeft = true;
            if (branchContainsMissingValues && useXGBoostMissingValueHandling) {
                // send missing values both ways and take the better direction

                // send missings left
                targetCountsSplitPartition[0] = sumWeightsPartitionFirstClass + missingWeightFirstClass;
                targetCountsSplitPartition[1] =
                    sumWeightsPartitionTotal + missingWeight - targetCountsSplitPartition[0];
                binaryPartitionWeights[1] = sumWeightsPartitionTotal + missingWeight;

                // totalFirstClassWeight and totalWeight only include non missing values
                targetCountsSplitRemaining[0] = totalFirstClassWeight - sumWeightsPartitionFirstClass;
                targetCountsSplitRemaining[1] = totalWeight - sumWeightsPartitionTotal - targetCountsSplitRemaining[0];
                binaryPartitionWeights[0] = totalWeight - sumWeightsPartitionTotal;

                boolean isValidSplitLeft =
                    binaryPartitionWeights[0] >= minChildSize && binaryPartitionWeights[1] >= minChildSize;
                binaryImpurityValues[0] =
                    impCriterion.getPartitionImpurity(targetCountsSplitRemaining, binaryPartitionWeights[0]);
                binaryImpurityValues[1] =
                    impCriterion.getPartitionImpurity(targetCountsSplitPartition, binaryPartitionWeights[1]);
                double postSplitImpurity = impCriterion.getPostSplitImpurity(binaryImpurityValues,
                    binaryPartitionWeights, totalWeight + missingWeight);
                double gainLeft = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity,
                    binaryPartitionWeights, totalWeight + missingWeight);

                // send missings right
                targetCountsSplitPartition[0] = sumWeightsPartitionFirstClass;
                targetCountsSplitPartition[1] = sumWeightsPartitionTotal - sumWeightsPartitionFirstClass;
                binaryPartitionWeights[1] = sumWeightsPartitionTotal;

                targetCountsSplitRemaining[0] =
                    totalFirstClassWeight - sumWeightsPartitionFirstClass + missingWeightFirstClass;
                targetCountsSplitRemaining[1] =
                    totalWeight - sumWeightsPartitionTotal + missingWeight - targetCountsSplitRemaining[0];
                binaryPartitionWeights[0] = totalWeight + missingWeight - sumWeightsPartitionTotal;

                boolean isValidSplitRight =
                    binaryPartitionWeights[0] >= minChildSize && binaryPartitionWeights[1] >= minChildSize;
                binaryImpurityValues[0] =
                    impCriterion.getPartitionImpurity(targetCountsSplitRemaining, binaryPartitionWeights[0]);
                binaryImpurityValues[1] =
                    impCriterion.getPartitionImpurity(targetCountsSplitPartition, binaryPartitionWeights[1]);
                postSplitImpurity = impCriterion.getPostSplitImpurity(binaryImpurityValues, binaryPartitionWeights,
                    totalWeight + missingWeight);
                double gainRight = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity,
                    binaryPartitionWeights, totalWeight + missingWeight);

                // decide which is better (better gain)
                if (gainLeft >= gainRight) {
                    gain = gainLeft;
                    isValidSplit = isValidSplitLeft;
                    tempMissingsGoLeft = true;
                } else {
                    gain = gainRight;
                    isValidSplit = isValidSplitRight;
                    tempMissingsGoLeft = false;
                }

            } else {

                // assign weights to branches
                targetCountsSplitPartition[0] = sumWeightsPartitionFirstClass;
                targetCountsSplitPartition[1] = sumWeightsPartitionTotal - sumWeightsPartitionFirstClass;
                binaryPartitionWeights[1] = sumWeightsPartitionTotal;

                targetCountsSplitRemaining[0] = totalFirstClassWeight - sumWeightsPartitionFirstClass;
                targetCountsSplitRemaining[1] = totalWeight - sumWeightsPartitionTotal - targetCountsSplitRemaining[0];
                binaryPartitionWeights[0] = totalWeight - sumWeightsPartitionTotal;

                isValidSplit = binaryPartitionWeights[0] >= minChildSize && binaryPartitionWeights[1] >= minChildSize;
                binaryImpurityValues[0] =
                    impCriterion.getPartitionImpurity(targetCountsSplitRemaining, binaryPartitionWeights[0]);
                binaryImpurityValues[1] =
                    impCriterion.getPartitionImpurity(targetCountsSplitPartition, binaryPartitionWeights[1]);

                double postSplitImpurity =
                    impCriterion.getPostSplitImpurity(binaryImpurityValues, binaryPartitionWeights, totalWeight);
                gain = impCriterion.getGain(targetPriors.getPriorImpurity(), postSplitImpurity, binaryPartitionWeights,
                    totalWeight);
            }

            // use random tie breaker if gains are equal
            boolean randomTieBreaker = gain == bestPartitionGain ? rd.nextInt(0, 1) == 1 : false;
            // store if better than before or first valid split
            if (gain > bestPartitionGain || (!isBestSplitValid && isValidSplit) || randomTieBreaker) {
                if (isValidSplit || !isBestSplitValid) {
                    bestPartitionGain = gain;
                    bestPartitionMask = isRightBranch ? partitionMask
                        : BigInteger.ZERO.setBit(highestBitPosition + 1).subtract(BigInteger.ONE).xor(partitionMask);
                    isBestSplitValid = isValidSplit;
                    // missingsGoLeft is only used later on if XGBoost Missing Value Handling is used
                    if (branchContainsMissingValues) {
                        //                        missingsGoLeft = isRightBranch;
                        missingsGoLeft = tempMissingsGoLeft;
                    } else {
                        // no missing values in this branch
                        // send missing values with the majority
                        missingsGoLeft = isRightBranch ? sumWeightsPartitionTotal < 0.5 * totalWeight
                            : sumWeightsPartitionTotal >= 0.5 * totalWeight;
                    }
                }
            }

        }

        if (isBestSplitValid && bestPartitionGain > 0.0) {
            if (useXGBoostMissingValueHandling) {
                return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask, NO_MISSED_ROWS,
                    missingsGoLeft ? NominalBinarySplitCandidate.MISSINGS_GO_LEFT
                        : NominalBinarySplitCandidate.MISSINGS_GO_RIGHT);
            }
            return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask,
                getMissedRows(columnMemberships), NominalBinarySplitCandidate.NO_MISSINGS);
        }

        return null;
    }

    private static class NomValProbabilityPair implements Comparable<NomValProbabilityPair> {

        private final NominalValueRepresentation m_nomValRep;

        private final double m_firstClassProbability;

        private final BigInteger m_bitMask;

        private final double m_sumWeights;

        private final double m_firstClassSumWeights;

        public NomValProbabilityPair(final NominalValueRepresentation nomValRep, final double firstClassProbability,
            final double sumWeights, final double firstClassSumWeights) {
            m_nomValRep = nomValRep;
            m_firstClassProbability = firstClassProbability;
            m_bitMask = BigInteger.ZERO.setBit(nomValRep.getAssignedInteger());
            m_sumWeights = sumWeights;
            m_firstClassSumWeights = firstClassSumWeights;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final NomValProbabilityPair that) {
            if (m_firstClassProbability < that.m_firstClassProbability) {
                return -1;
            }
            if (m_firstClassProbability > that.m_firstClassProbability) {
                return 1;
            } else {
                return 0;
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public SplitCandidate calcBestSplitRegression(final DataMemberships dataMemberships,
        final RegressionPriors targetPriors, final TreeTargetNumericColumnData targetColumn, final RandomData rd) {

        final NominalValueRepresentation[] nomVals = getMetaData().getValues();

        final ColumnMemberships columnMemberships =
            dataMemberships.getColumnMemberships(getMetaData().getAttributeIndex());

        final boolean useBinaryNominalSplits = getConfiguration().isUseBinaryNominalSplits();

        if (useBinaryNominalSplits) {
            return calcBestSplitRegressionBinaryBreiman(columnMemberships, targetPriors, targetColumn, nomVals, rd);
        } else {
            return calcBestSplitRegressionMultiway(columnMemberships, targetPriors, targetColumn, nomVals, rd);
        }
    }

    private NominalMultiwaySplitCandidate calcBestSplitRegressionMultiway(final ColumnMemberships columnMemberships,
        final RegressionPriors targetPriors, final TreeTargetNumericColumnData targetColumn,
        final NominalValueRepresentation[] nomVals, final RandomData rd) {

        final double ySumTotal = targetPriors.getYSum();
        final double nrRecordsTotal = targetPriors.getNrRecords();
        final double criterionTotal = ySumTotal * ySumTotal / nrRecordsTotal;

        final int lengthNonMissing = containsMissingValues() ? nomVals.length - 1 : nomVals.length;
        final double[] sumWeightsAttributes = new double[lengthNonMissing];
        final double[] sumYAttributes = new double[lengthNonMissing];
        // number (sum) of total valid values
        double totalWeight = 0.0;
        int start = 0;
        columnMemberships.next();
        for (int att = 0; att < lengthNonMissing; att++) {
            int end = start + m_nominalValueCounts[att];
            double weightSum = 0.0;
            double ySum = 0.0;
            boolean reachedEnd = false;
            for (int index = columnMemberships.getIndexInColumn(); index < end; index =
                columnMemberships.getIndexInColumn()) {
                final double weight = columnMemberships.getRowWeight();
                assert weight > EPSILON : "Instances in columnMemberships must have weights larger than EPSILON.";
                ySum += weight * targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                weightSum += weight;

                if (!columnMemberships.next()) {
                    // reached end of columnMemberships
                    reachedEnd = true;
                    break;
                }
            }
            sumWeightsAttributes[att] = weightSum;
            sumYAttributes[att] = ySum;
            totalWeight += weightSum;
            start = end;
            if (reachedEnd == true) {
                break;
            }
        }

        //        assert Math.abs((ySumTotal - totalWeight) / ySumTotal) < EPSILON
        //            : "Expected similar values: " + ySumTotal + " vs. " + totalWeight;

        final boolean useXGBoostMissingValueHandling = getConfiguration().getMissingValueHandling() == MissingValueHandling.XGBoost;
        boolean branchContainsMissingValues = containsMissingValues();
        double missingWeight = 0.0;
        double missingY = 0.0;
        if (branchContainsMissingValues) {
            columnMemberships.goToLast();
            while (columnMemberships.getIndexInColumn() >= m_idxOfFirstMissing) {
                final double weight = columnMemberships.getRowWeight();
                missingWeight += weight;
                missingY += weight * targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                if (!columnMemberships.previous()) {
                    break;
                }
            }
            branchContainsMissingValues = missingWeight > EPSILON;
        }

        if (branchContainsMissingValues && useXGBoostMissingValueHandling) {
            double best = 0.0;
            int missingsGoWith = -1;

            for (int i = 0; i < lengthNonMissing; i++) {
                double crit = 0.0;
                for (int j = 0; j < lengthNonMissing; j++) {
                    if (i == j) {
                        double weightWithMissing = sumWeightsAttributes[j] + missingWeight;
                        double yWithMissing = sumYAttributes[j] + missingY;
                        crit += yWithMissing * yWithMissing / weightWithMissing;
                    } else {
                        crit += sumYAttributes[j] * sumYAttributes[j] / sumWeightsAttributes[j];
                    }
                }
                if (crit > best) {
                    missingsGoWith = i;
                    best = crit;
                }
            }
            return new NominalMultiwaySplitCandidate(this, best - criterionTotal, sumWeightsAttributes, NO_MISSED_ROWS, missingsGoWith);
        }
        double criterionAfterSplit = 0.0;
        for (int i = 0; i < lengthNonMissing; i++) {
            criterionAfterSplit += sumYAttributes[i] * sumYAttributes[i] / sumWeightsAttributes[i];
        }
        final double gain = criterionAfterSplit - criterionTotal;
        if (gain > 0.0) {
            if (useXGBoostMissingValueHandling) {
                int missingsGoWith = -1;
                double maxWeight = 0.0;
                for (int i = 0; i < sumWeightsAttributes.length; i++) {
                    if (sumWeightsAttributes[i] > maxWeight) {
                        maxWeight = sumWeightsAttributes[i];
                        missingsGoWith = i;
                    }
                }
                // send missing values with majority
                return new NominalMultiwaySplitCandidate(this, gain, sumWeightsAttributes, NO_MISSED_ROWS, missingsGoWith);
            }
            return new NominalMultiwaySplitCandidate(this, gain, sumWeightsAttributes, getMissedRows(columnMemberships),
                NominalMultiwaySplitCandidate.NO_MISSINGS);
        }
        return null;
    }

    private NominalBinarySplitCandidate calcBestSplitRegressionBinary(final ColumnMemberships columnMemberships,
        final RegressionPriors targetPriors, final TreeTargetNumericColumnData targetColumn,
        final NominalValueRepresentation[] nomVals, final RandomData rd) {

        final int minChildSize = getConfiguration().getMinChildSize();

        final double ySumTotal = targetPriors.getYSum();
        final double nrRecordsTotal = targetPriors.getNrRecords();
        final double criterionTotal = ySumTotal * ySumTotal / nrRecordsTotal;

        final double[] ySums = new double[nomVals.length];
        final double[] sumWeightsAttributes = new double[nomVals.length];

        columnMemberships.next();
        int start = 0;
        for (int att = 0; att < nomVals.length; att++) {
            int end = start + m_nominalValueCounts[att];
            double weightSum = 0.0;
            double ySum = 0.0;
            boolean reachedEnd = false;
            for (int index = columnMemberships.getIndexInColumn(); index < end; index =
                columnMemberships.getIndexInColumn()) {
                final double weight = columnMemberships.getRowWeight();

                assert weight > EPSILON : "Instances in columnMemberships must have weights larger than EPSILON.";

                ySum += weight * targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                weightSum += weight;

                if (!columnMemberships.next()) {
                    // reached end of columnMemberships
                    reachedEnd = true;
                    break;
                }
            }
            sumWeightsAttributes[att] = weightSum;
            ySums[att] = ySum;
            start = end;
            if (reachedEnd) {
                break;
            }
        }

        BinarySplitEnumeration splitEnumeration;
        if (nomVals.length <= 10) {
            splitEnumeration = new FullBinarySplitEnumeration(nomVals.length);
        } else {
            int maxSearch = (1 << 10 - 2);
            splitEnumeration = new RandomBinarySplitEnumeration(nomVals.length, maxSearch, rd);
        }
        BigInteger bestPartitionMask = null;
        boolean isBestSplitValid = false;
        double bestPartitionGain = Double.NEGATIVE_INFINITY;

        do {
            double weightLeft = 0.0;
            double ySumLeft = 0.0;
            double weightRight = 0.0;
            double ySumRight = 0.0;
            for (int i = 0; i < nomVals.length; i++) {
                final boolean isAttributeInRightBranch = splitEnumeration.isInRightBranch(i);
                if (isAttributeInRightBranch) {
                    weightRight += sumWeightsAttributes[i];
                    ySumRight += ySums[i];
                } else {
                    weightLeft += sumWeightsAttributes[i];
                    ySumLeft += ySums[i];
                }
            }
            final boolean isValidSplit = weightRight >= minChildSize && weightLeft >= minChildSize;
            double gain = ySumRight * ySumRight / weightRight + ySumLeft * ySumLeft / weightLeft - criterionTotal;
            // use random tie breaker if gains are equal
            boolean randomTieBreaker = gain == bestPartitionGain ? rd.nextInt(0, 1) == 1 : false;
            // store if better than before or first valid split
            if (gain > bestPartitionGain || (!isBestSplitValid && isValidSplit) || randomTieBreaker) {
                if (isValidSplit || !isBestSplitValid) {
                    bestPartitionGain = gain;
                    bestPartitionMask = splitEnumeration.getValueMask();
                    isBestSplitValid = isValidSplit;
                }
            }
        } while (splitEnumeration.next());
        if (bestPartitionGain > 0.0) {
            return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask,
                getMissedRows(columnMemberships), NominalBinarySplitCandidate.NO_MISSINGS);
        }
        return null;
    }

    /**
     * If an attribute value does not appear in the current branch, it is not guaranteed in which child branch this
     * value will fall. (This should not be a problem since we cannot make any assumptions about this attribute value
     * anyway)
     *
     * @param membershipController
     * @param rowWeights
     * @param targetPriors
     * @param targetColumn
     * @param nomVals
     * @param originalIndexInColumnList
     * @return best split candidate or null if there is no split candidate with positive gain or too small child nodes
     */
    private NominalBinarySplitCandidate calcBestSplitRegressionBinaryBreiman(final ColumnMemberships columnMemberships,
        final RegressionPriors targetPriors, final TreeTargetNumericColumnData targetColumn,
        final NominalValueRepresentation[] nomVals, final RandomData rd) {

        final int minChildSize = getConfiguration().getMinChildSize();
        double sumYTotal = targetPriors.getYSum();
        double sumWeightTotal = targetPriors.getNrRecords();
        final double criterionTotal = sumYTotal * sumYTotal / sumWeightTotal;
        final boolean useXGBoostMissingValueHandling =
            getConfiguration().getMissingValueHandling() == MissingValueHandling.XGBoost;
        boolean branchContainsMissingValues = containsMissingValues();

        double missingWeight = 0.0;
        double missingY = 0.0;
        if (branchContainsMissingValues) {
            columnMemberships.goToLast();
            while (columnMemberships.getIndexInColumn() >= m_idxOfFirstMissing) {
                final double weight = columnMemberships.getRowWeight();
                missingWeight += weight;
                missingY += weight * targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                if (!columnMemberships.previous()) {
                    break;
                }
            }
            sumYTotal -= missingY;
            sumWeightTotal -= missingWeight;
            branchContainsMissingValues = missingWeight > 0.0;
            columnMemberships.reset();
        }

        final ArrayList<AttValTupleRegression> attValList = Lists.newArrayList();

        columnMemberships.next();
        int start = 0;
        final int lengthNonMissing = branchContainsMissingValues ? nomVals.length - 1 : nomVals.length;
        for (int att = 0; att < lengthNonMissing; att++) {
            double sumY = 0.0;
            double sumWeight = 0.0;

            int end = start + m_nominalValueCounts[att];

            boolean reachedEnd = false;

            for (int index = columnMemberships.getIndexInColumn(); index < end; index =
                columnMemberships.getIndexInColumn()) {
                double weight = columnMemberships.getRowWeight();

                assert weight > EPSILON : "Instances in columnMemberships must have weights larger than EPSILON.";

                sumY += targetColumn.getValueFor(columnMemberships.getOriginalIndex());
                sumWeight += weight;
                if (!columnMemberships.next()) {
                    reachedEnd = true;
                    break;
                }
            }

            start = end;

            if (sumWeight < EPSILON) {
                // attribute value is not present in this branch
                // we cannot make any assumptions about this attribute value
                continue;
            }

            attValList.add(new AttValTupleRegression(sumY, sumWeight, sumY / sumWeight, nomVals[att]));
            if (reachedEnd) {
                break;
            }
        }

        // sort attribute values according to their mean Y value
        attValList.sort(null);

        BigInteger bestPartitionMask = null;
        boolean isBestSplitValid = false;
        double bestPartitionGain = Double.NEGATIVE_INFINITY;
        final int highestBitPosition = branchContainsMissingValues ? nomVals.length - 2 : nomVals.length - 1;

        double sumYPartition = 0.0;
        double sumWeightPartition = 0.0;
        BigInteger partitionMask = BigInteger.ZERO;
        double sumYRemaining = sumYTotal;
        double sumWeightRemaining = sumWeightTotal;
        boolean missingsGoLeft = true;

        for (int i = 0; i < attValList.size() - 1; i++) {
            AttValTupleRegression attVal = attValList.get(i);
            sumYPartition += attVal.m_sumY;
            sumWeightPartition += attVal.m_sumWeight;
            sumYRemaining -= attVal.m_sumY;
            sumWeightRemaining -= attVal.m_sumWeight;
            partitionMask = partitionMask.or(attVal.m_bitMask);

            double gain;
            boolean isValidSplit;
            boolean tempMissingsGoLeft = true;
            if (branchContainsMissingValues && useXGBoostMissingValueHandling) {
                boolean isValidSplitPartitionWithMissing =
                    sumWeightPartition + missingWeight >= minChildSize && sumWeightRemaining >= minChildSize;
                double sumYMissingWithPartition = sumYPartition + missingY;
                double gainMissingWithPartition =
                    sumYMissingWithPartition * sumYMissingWithPartition / (sumWeightPartition + missingWeight)
                        + sumYRemaining * sumYRemaining / sumWeightRemaining - criterionTotal;

                boolean isValidSplitRemainingWithMissing =
                    sumWeightPartition >= minChildSize && sumWeightRemaining + missingWeight >= minChildSize;
                double sumYMissingWithRemaining = sumYRemaining + missingY;
                double gainMissingWithRemaining = sumYPartition * sumYPartition / sumWeightPartition
                    + sumYMissingWithRemaining * sumYMissingWithRemaining / (sumWeightRemaining + missingWeight)
                    - criterionTotal;

                if (gainMissingWithPartition >= gainMissingWithRemaining) {
                    gain = gainMissingWithPartition;
                    isValidSplit = isValidSplitPartitionWithMissing;
                    tempMissingsGoLeft = !partitionMask.testBit(highestBitPosition);
                } else {
                    gain = gainMissingWithRemaining;
                    isValidSplit = isValidSplitRemainingWithMissing;
                    tempMissingsGoLeft = partitionMask.testBit(highestBitPosition);
                }
            } else {
                isValidSplit = sumWeightPartition >= minChildSize && sumWeightRemaining >= minChildSize;

                gain = sumYPartition * sumYPartition / sumWeightPartition
                    + sumYRemaining * sumYRemaining / sumWeightRemaining - criterionTotal;
            }

            // use random tie breaker if gains are equal
            boolean randomTieBreaker = gain == bestPartitionGain ? rd.nextInt(0, 1) == 1 : false;

            // store if better than before or first valid split
            if (gain > bestPartitionGain || (!isBestSplitValid && isValidSplit) || randomTieBreaker) {
                if (isValidSplit || !isBestSplitValid) {
                    bestPartitionGain = gain;
                    // right branch must by convention always contain the nominal value
                    // with the highest assigned integer
                    bestPartitionMask = partitionMask.testBit(highestBitPosition) ? partitionMask
                        : BigInteger.ZERO.setBit(highestBitPosition + 1).subtract(BigInteger.ONE).xor(partitionMask);
                    isBestSplitValid = isValidSplit;
                    if (branchContainsMissingValues) {
                        missingsGoLeft = tempMissingsGoLeft;
                    } else {
                        // no missings in this branch, but we still have to provide a direction for missing values
                        // send missings in the direction the most records in the node are sent to
                        boolean sendWithPartition = sumWeightPartition >= sumWeightRemaining;
                        missingsGoLeft = sendWithPartition ? !partitionMask.testBit(highestBitPosition)
                            : partitionMask.testBit(highestBitPosition);
                    }
                }
            }

        }

        if (bestPartitionGain > 0.0 && isBestSplitValid) {
            if (useXGBoostMissingValueHandling) {
                return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask, NO_MISSED_ROWS,
                    missingsGoLeft ? NominalBinarySplitCandidate.MISSINGS_GO_LEFT
                        : NominalBinarySplitCandidate.MISSINGS_GO_RIGHT);
            }
            return new NominalBinarySplitCandidate(this, bestPartitionGain, bestPartitionMask,
                getMissedRows(columnMemberships), NominalBinarySplitCandidate.NO_MISSINGS);
        }

        return null;
    }

    private static class AttValTupleRegression implements Comparable<AttValTupleRegression> {

        private final double m_sumY;

        private final double m_sumWeight;

        private final double m_meanY;

        private final BigInteger m_bitMask;

        /**
         *
         */
        public AttValTupleRegression(final double sumY, final double sumWeight, final double meanY,
            final NominalValueRepresentation nomVal) {
            m_sumY = sumY;
            m_sumWeight = sumWeight;
            m_meanY = meanY;
            m_bitMask = BigInteger.ZERO.setBit(nomVal.getAssignedInteger());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final AttValTupleRegression that) {
            return ComparisonChain.start().compare(m_meanY, that.m_meanY).result();
        }

    }

    /** {@inheritDoc} */
    @Override
    public BitSet updateChildMemberships(final TreeNodeCondition childCondition,
        final DataMemberships parentMemberships) {
        if (childCondition instanceof TreeNodeNominalCondition) {
            return updateChildMembershipsMultiway((TreeNodeNominalCondition)childCondition, parentMemberships);
        } else if (childCondition instanceof TreeNodeNominalBinaryCondition) {
            return updateChildMembershipsBinary((TreeNodeNominalBinaryCondition)childCondition, parentMemberships);
        } else {
            throw new IllegalStateException(
                "Invalid tree node condition class: " + childCondition.getClass().getSimpleName());
        }
    }

    private BitSet updateChildMembershipsMultiway(final TreeNodeNominalCondition nomCondition,
        final DataMemberships parentMemberships) {
        String value = nomCondition.getValue();
        int att = -1;
        final NominalValueRepresentation[] reps = getMetaData().getValues();
        for (final NominalValueRepresentation rep : reps) {
            if (rep.getNominalValue().equals(value)) {
                att = rep.getAssignedInteger();
                break;
            }
        }
        if (att == -1) {
            throw new IllegalStateException("Unknown value: " + value);
        }
        ColumnMemberships columnMemberships = parentMemberships.getColumnMemberships(getMetaData().getAttributeIndex());
        BitSet inChild = new BitSet(columnMemberships.size());
        columnMemberships.reset();
        int start = 0;
        for (int a = 0; a < att; a++) {
            start += m_nominalValueCounts[a];
        }
        // Make sure that we are using an index >= start
        if (!columnMemberships.nextIndexFrom(start)) {
            return inChild;
        }
        boolean reachedEnd = false;
        int end = start + m_nominalValueCounts[att];
        for (int index = columnMemberships.getIndexInColumn(); index < end; index =
            columnMemberships.getIndexInColumn()) {
            inChild.set(columnMemberships.getIndexInDataMemberships());
            if (!columnMemberships.next()) {
                reachedEnd = true;
                break;
            }
        }

        if (!reachedEnd && containsMissingValues() && nomCondition.acceptsMissings()) {
            // move to missing values
            for (int i = att; i < reps.length - 1; i++) {
                start += m_nominalValueCounts[i];
            }
            if (columnMemberships.nextIndexFrom(start)) {
                do {
                    inChild.set(columnMemberships.getIndexInDataMemberships());
                } while (columnMemberships.next());
            }
        }

        return inChild;
    }

    private BitSet updateChildMembershipsBinary(final TreeNodeNominalBinaryCondition childBinaryCondition,
        final DataMemberships parentMemberships) {
        ColumnMemberships columnMemberships = parentMemberships.getColumnMemberships(getMetaData().getAttributeIndex());
        columnMemberships.reset();
        BitSet inChild = new BitSet(columnMemberships.size());
        // TODO Check if this can be done more efficiently
        NominalValueRepresentation[] reps = getMetaData().getValues();
        int start = 0;
        boolean reachedEnd = false;
        final int lengthNonMissing = containsMissingValues() ? reps.length - 1 : reps.length;
        for (int att = 0; att < lengthNonMissing; att++) {
            if (childBinaryCondition.testCondition(att)) {
                // move columnMemberships to correct position
                if (!columnMemberships.nextIndexFrom(start)) {
                    // reached end of columnMemberships
                    break;
                }
                int end = start + m_nominalValueCounts[att];
                for (int index = columnMemberships.getIndexInColumn(); index < end; index =
                    columnMemberships.getIndexInColumn()) {
                    inChild.set(columnMemberships.getIndexInDataMemberships());
                    if (!columnMemberships.next()) {
                        reachedEnd = true;
                        break;
                    }
                }
            }
            start += m_nominalValueCounts[att];
        }

        if (!reachedEnd && containsMissingValues() && childBinaryCondition.acceptsMissings()) {
            if (columnMemberships.nextIndexFrom(start)) {
                do {
                    inChild.set(columnMemberships.getIndexInDataMemberships());
                } while (columnMemberships.next());
            }
        }

        return inChild;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        TreeNominalColumnMetaData metaData = getMetaData();
        NominalValueRepresentation[] nomValues = metaData.getValues();
        StringBuilder b = new StringBuilder(metaData.getAttributeName());
        b.append(" [");
        final int length = m_originalIndexInColumnList.length;
        String[] sample = new String[Math.min(100, length)];
        for (int i = 0; i < length; i++) {
            int trueIndex = m_originalIndexInColumnList[i];
            if (trueIndex < sample.length) {
                int valueIndex = 0;
                int start = 0;
                for (int att = 0; att < m_nominalValueCounts.length; att++) {
                    int end = start + m_nominalValueCounts[att];
                    if (start <= i && i < end) {
                        valueIndex = att;
                    }
                    start = end;
                }
                String value = nomValues[valueIndex].getNominalValue();
                sample[trueIndex] = value;
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

    /**
     * Enumeration of binary splits for a given number of unique values in a categorical column. It's using a mask that
     * determines whether a value goes left or right. The number of possible splits is "2 ^ (#values - 1) - 1".
     *
     * For a column with 4 unique values we have 7 tuples</br>
     * 1: 1000 2: 1001 3: 1010 4: 1011 5: 1100 6: 1101 7: 1110
     *
     * The left most bit is always set to 1 as the counterparts with left-bit = 0 is just left and right branch swapped
     * (note the exponent in the formula).
     *
     * To be called like #init do #isInRightBranch ... if (...) #getValueMask while #next
     */
    static abstract class BinarySplitEnumeration {

        private final int m_count;

        /**
         * @param count Number of unique values in the column.
         */
        BinarySplitEnumeration(final int count) {
            m_count = count;
        }

        /** Set next mask, return true if there is valid next mask. */
        abstract boolean next();

        /** Get current mask, to be called before {@link #next()}. */
        abstract BigInteger getValueMask();

        /** Is the value at position index included in the right branch. */
        abstract boolean isInRightBranch(final int index);

        final int getCount() {
            return m_count;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return String.format("# Unique Values = %d, current mask: %s", getCount(), getValueMask().toString(2));
        }
    }

    /** Enumerates all possible binary splits. */
    static final class FullBinarySplitEnumeration extends BinarySplitEnumeration {

        private int m_mask;

        /**
         * @param count Number of unique values in the column.
         */
        FullBinarySplitEnumeration(final int count) {
            super(count);
            assert count <= 32 : String.format("Count must be smaller than 32: %d", count);
            // left most bit is always set, e.g. if count = 3 this would be binary "100"
            m_mask = 1 << (count - 1);
        }

        @Override
            boolean next() {
            m_mask += 1;
            return m_mask != ((1 << getCount()) - 1);
        }

        @Override
            BigInteger getValueMask() {
            return BigInteger.valueOf(m_mask);
        }

        @Override
            boolean isInRightBranch(final int index) {
            return (m_mask & (1 << index)) != 0;
        }

    }

    /** Enumerates a random sample of all binary splits. */
    static final class RandomBinarySplitEnumeration extends BinarySplitEnumeration {

        private final Set<BigInteger> m_maskHistory;

        private final int m_maxNumberPermutations;

        private final Random m_random;

        /** Needed to swap all bits to avoid duplicate masks. It's 1111...111. */
        private final BigInteger m_reverseXORMask;

        private BigInteger m_current;

        RandomBinarySplitEnumeration(final int count, final int maxNumberPermutations, final RandomData rd) {
            super(count);
            m_maxNumberPermutations = maxNumberPermutations;
            m_maskHistory = new HashSet<>(3 * maxNumberPermutations / 2 + 1);
            m_random = new Random(rd.nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
            m_reverseXORMask = BigInteger.ZERO.setBit(count).subtract(BigInteger.ONE);
            next();
        }

        @Override
            boolean next() {
            if (m_maskHistory.size() >= m_maxNumberPermutations) {
                return false;
            }
            final int count = getCount();
            BigInteger s;
            do {
                s = new BigInteger(count, m_random);
                // make sure the MSB is set (by definition the value with largest assigned integer is in right branch)
                // if not set then flip all bits -- no '~' available on BigInteger
                if (!s.testBit(count - 1)) {
                    // xor with 111111111...1111
                    s = s.xor(m_reverseXORMask);
                }
            } while (s.bitCount() >= count || m_maskHistory.contains(s));
            m_current = s;
            m_maskHistory.add(s);
            return true;
        }

        @Override
            BigInteger getValueMask() {
            return m_current;
        }

        @Override
            boolean isInRightBranch(final int index) {
            return getValueMask().testBit(index);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsMissingValues() {
        return m_idxOfFirstMissing != -1;
    }

    /**
     * {@inheritDoc}
     *
     * NOTE: here the assigned Integer is returned and NOT the nominal value
     */
    @Override
    public Integer getValueAt(final int indexInColumn) {
        int counter = 0;
        int attVal;
        for (attVal = 0; attVal < m_nominalValueCounts.length; attVal++) {
            counter += m_nominalValueCounts[attVal];
            if (indexInColumn < counter) {
                break;
            }
        }
        return getMetaData().getValues()[attVal].getAssignedInteger();
    }

}
