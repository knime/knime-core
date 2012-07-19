/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Jan 12, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.util.BitSet;

import org.knime.base.node.mine.treeensemble.learner.BitSplitCandidate;
import org.knime.base.node.mine.treeensemble.learner.IImpurity;
import org.knime.base.node.mine.treeensemble.learner.SplitCandidate;
import org.knime.base.node.mine.treeensemble.model.TreeNodeBitCondition;
import org.knime.base.node.mine.treeensemble.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeBitVectorColumnData extends TreeAttributeColumnData {

    private final BitSet m_columnBitSet;
    private final int m_length;

    /**
     * @param metaData
     * @param columnBitSet */
    TreeBitVectorColumnData(final TreeBitColumnMetaData metaData,
            final TreeEnsembleLearnerConfiguration configuration,
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
    public SplitCandidate calcBestSplit(final double[] rowWeights,
            final PriorDistribution targetPriors,
            final TreeTargetNominalColumnData targetColumn) {
        final NominalValueRepresentation[] targetVals =
            targetColumn.getMetaData().getValues();
        final IImpurity impurityCriterion =
            targetPriors.getImpurityCriterion();
        // distribution of target for On ('1') and Off ('0') bits
        final double[] onTargetWeights = new double[targetVals.length];
        final double[] offTargetWeights = new double[targetVals.length];
        double onWeights = 0.0;
        double offWeights = 0.0;

        for (int i = 0; i < m_length; i++) {
            final double weight = rowWeights[i];
            if (weight < EPSILON) {
                // ignore record: not in current branch or not in sample
            } else {
                final int target = targetColumn.getValueFor(i);
                if (m_columnBitSet.get(i)) {
                    onWeights += weight;
                    onTargetWeights[target] += weight;
                } else {
                    offWeights += weight;
                    offTargetWeights[target] += weight;
                }
            }
        }
        final double weightSum = onWeights + offWeights;
        final double onImpurity = impurityCriterion.getPartitionImpurity(
                onTargetWeights, onWeights);
        final double offImpurity = impurityCriterion.getPartitionImpurity(
                offTargetWeights, offWeights);
        final double[] partitionWeights = new double[] {onWeights, offWeights};
        final double postSplitImpurity = impurityCriterion.getPostSplitImpurity(
                new double[] {onImpurity, offImpurity},
                partitionWeights, weightSum);
        final double gainValue = impurityCriterion.getGain(
                targetPriors.getPriorImpurity(), postSplitImpurity,
                partitionWeights, weightSum);
        return new BitSplitCandidate(this, targetPriors, gainValue);
    }

    /** {@inheritDoc} */
    @Override
    public void updateChildMemberships(final TreeNodeCondition childCondition,
            final double[] parentMemberships,
            final double[] childMembershipsToUpdate) {
        TreeNodeBitCondition bitCondition =
            (TreeNodeBitCondition)childCondition;
        assert getMetaData().getAttributeName().equals(
                bitCondition.getColumnMetaData().getAttributeName());
        final boolean value = bitCondition.getValue();
        for (int i = 0; i < m_length; i++) {
            if (m_columnBitSet.get(i) != value) {
                childMembershipsToUpdate[i] = 0.0;
            } else {
                assert childMembershipsToUpdate[i]
                    == parentMemberships[i];
            }
        }
    }

}
