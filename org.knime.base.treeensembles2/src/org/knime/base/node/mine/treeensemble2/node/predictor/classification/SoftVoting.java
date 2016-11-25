/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.07.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.classification;

import java.util.Map;

import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;

/**
 * Implements soft voting.
 * In soft voting the probabilities of all trees are aggregated instead of votes.
 *
 * @author Adrian Nembach, KNIME.com
 */
final class SoftVoting extends AbstractVoting {

    private float[] m_distribution;

    public SoftVoting(final Map<String, Integer> targetValueToIndexMap) {
        super(targetValueToIndexMap);
        m_distribution = new float[targetValueToIndexMap.size()];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getClassProbabilityForClass(final String classValue) {
        int classIdx = getIndexForClass(classValue);
        float prob = m_distribution[classIdx] / getNrVotes();
        return prob;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateDistribution(final TreeNodeClassification leaf) {
        final float[] targetDistribution = leaf.getTargetDistribution();
        assert targetDistribution.length == m_distribution.length;
        float nrRecordsInLeaf = 0;
        for (float classCount : targetDistribution) {
            nrRecordsInLeaf += classCount;
        }
        final NominalValueRepresentation[] targetVals = leaf.getTargetMetaData().getValues();
        for (int i = 0; i < m_distribution.length; i++) {
            /* the nominal values in targetVals are in the order in which they first appeared in the
             training table. This is not necessarily the same as the order in the domain information of
             the target column
             */
            int idxInVotingDistr = getIndexForClass(targetVals[i].getNominalValue());
            m_distribution[i] += targetDistribution[idxInVotingDistr] / nrRecordsInLeaf;
        }
    }
}
