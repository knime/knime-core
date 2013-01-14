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
 *   Dec 28, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.util.Arrays;

import org.knime.base.node.mine.treeensemble.learner.IImpurity;
import org.knime.base.node.mine.treeensemble.learner.NominalSplitCandidate;
import org.knime.base.node.mine.treeensemble.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble.model.TreeNodeNominalCondition;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeNominalColumnData extends TreeAttributeColumnData {

    private final int[] m_nominalValueCounts;
    private final int[] m_orginalIndexInColumnList;

    TreeNominalColumnData(final TreeNominalColumnMetaData metaData,
            final TreeEnsembleLearnerConfiguration configuration,
            final int[] nominalValueCounts, final int[] originalIndexInColumnList) {
        super(metaData, configuration);
        m_nominalValueCounts = nominalValueCounts;
        m_orginalIndexInColumnList = originalIndexInColumnList;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNominalColumnMetaData getMetaData() {
        return (TreeNominalColumnMetaData)super.getMetaData();
    }

    /** {@inheritDoc} */
    @Override
    public NominalSplitCandidate calcBestSplitClassification(final double[]
            rowWeights, final ClassificationPriors targetPriors,
            final TreeTargetNominalColumnData targetColumn) {
        final NominalValueRepresentation[] targetVals =
            targetColumn.getMetaData().getValues();
        IImpurity impCriterion = targetPriors.getImpurityCriterion();
        // distribution of target for each attribute value
        final double[] targetCountsSplit = new double[targetVals.length];
        final NominalValueRepresentation[] nomVals = getMetaData().getValues();
        // number of valid records for each attribute value
        final double[] attWeights = new double[nomVals.length];
        // entropy wrt target column for each attribute value
        final double[] attEntropys = new double[nomVals.length];
        final int[] originalIndexInColumnList = m_orginalIndexInColumnList;
        // number (sum) of total valid values
        double totalWeight = 0.0;
        int start = 0;
        for (int att = 0; att < nomVals.length; att++) {
            int end = start + m_nominalValueCounts[att];
            Arrays.fill(targetCountsSplit, 0.0);
            double currentAttValWeight = 0.0;
            for (int index = start; index < end; index++) {
                final int originalIndex = originalIndexInColumnList[index];
                final double weight = rowWeights[originalIndex];
                if (weight < EPSILON) {
                    // ignore record: not in current branch or not in sample
                } else {
                    int target = targetColumn.getValueFor(originalIndex);
                    targetCountsSplit[target] += weight;
                    currentAttValWeight += weight;
                }
            }
            totalWeight += currentAttValWeight;
            attWeights[att] = currentAttValWeight;
            attEntropys[att] = impCriterion.getPartitionImpurity(
                    targetCountsSplit, currentAttValWeight);
            start = end;
        }
        double postSplitImpurity = impCriterion.getPostSplitImpurity(
                attEntropys, attWeights, totalWeight);
        double gain = impCriterion.getGain(targetPriors.getPriorImpurity(),
                postSplitImpurity, attWeights, totalWeight);
        return new NominalSplitCandidate(this, gain, attWeights);
    }


    /** {@inheritDoc} */
    @Override
    public SplitCandidate calcBestSplitRegression(final double[] rowWeights,
            final RegressionPriors targetPriors,
            final TreeTargetNumericColumnData targetColumn) {
        final NominalValueRepresentation[] nomVals = getMetaData().getValues();
        final double ySumTotal = targetPriors.getYSum();
        final double nrRecordsTotal = targetPriors.getNrRecords();
        final double criterionTotal = ySumTotal * ySumTotal / nrRecordsTotal;

        double criterionAfterSplit = 0.0;
        final int[] originalIndexInColumnList = m_orginalIndexInColumnList;
        final double[] sumWeightsAttributes = new double[nomVals.length];
        // number (sum) of total valid values
        double totalWeight = 0.0;
        int start = 0;
        for (int att = 0; att < nomVals.length; att++) {
            int end = start + m_nominalValueCounts[att];
            double weightSum = 0.0;
            double ySum = 0.0;
            for (int index = start; index < end; index++) {
                final int originalIndex = originalIndexInColumnList[index];
                final double weight = rowWeights[originalIndex];
                if (weight < EPSILON) {
                    // ignore record: not in current branch or not in sample
                } else {
                    ySum += weight * targetColumn.getValueFor(originalIndex);
                    weightSum += weight;
                }
            }
            criterionAfterSplit += ySum * ySum / weightSum;
            sumWeightsAttributes[att] = weightSum;
            totalWeight += weightSum;
            start = end;
        }

//        assert Math.abs((ySumTotal - totalWeight) / ySumTotal) < EPSILON
//            : "Expected similar values: " + ySumTotal + " vs. " + totalWeight;
        final double gain = criterionAfterSplit - criterionTotal;
        if (gain > 0.0) {
            return new NominalSplitCandidate(this, gain, sumWeightsAttributes);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void updateChildMemberships(final TreeNodeCondition childCondition,
            final double[] parentMemberships,
            final double[] childMembershipsToUpdate) {
        TreeNodeNominalCondition nomCondition =
            (TreeNodeNominalCondition)childCondition;
        String value = nomCondition.getValue();
        int att = -1;
        for (NominalValueRepresentation rep : getMetaData().getValues()) {
            if (rep.getNominalValue().equals(value)) {
                att = rep.getAssignedInteger();
                break;
            }
        }
        if (att == -1) {
            throw new IllegalStateException("Unknown value: " + value);
        }
        final int[] originalIndexInColumnList = m_orginalIndexInColumnList;
        int start = 0;
        for (int a = 0; a < att; a++) {
            start += m_nominalValueCounts[a];
        }
        int end = start + m_nominalValueCounts[att];
        Arrays.fill(childMembershipsToUpdate, 0.0);
        for (int index = start; index < end; index++) {
            final int indexInColumn = originalIndexInColumnList[index];
            final double weight = parentMemberships[indexInColumn];
            childMembershipsToUpdate[indexInColumn] = weight;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        TreeNominalColumnMetaData metaData = getMetaData();
        NominalValueRepresentation[] nomValues = metaData.getValues();
        StringBuilder b = new StringBuilder(metaData.getAttributeName());
        b.append(" [");
        final int length = m_orginalIndexInColumnList.length;
        String[] sample = new String[Math.min(100, length)];
        for (int i = 0; i < length; i++) {
            int trueIndex = m_orginalIndexInColumnList[i];
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

}
