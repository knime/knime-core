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

import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;

/**
 * Implementation of SoftVoting
 *
 * @author Adrian Nembach, KNIME.com
 */
final class SoftVoting implements Voting {

    private int m_nrVotes;
    private float[] m_distribution;

    public SoftVoting(final int nrClasses) {
        m_nrVotes = 0;
        m_distribution = new float[nrClasses];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addVote(final TreeNodeClassification leaf) {
        m_nrVotes++;
        final float[] targetDistribution = leaf.getTargetDistribution();
        assert targetDistribution.length == m_distribution.length;
        float nrRecordsInLeaf = 0;
        for (float classCount : targetDistribution) {
            nrRecordsInLeaf += classCount;
        }
        for (int i = 0; i < m_distribution.length; i++) {
            m_distribution[i] += targetDistribution[i] / nrRecordsInLeaf;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMajorityClassIdx() {
        int majorityIdx = -1;
        float maxVotes = 0.0f;
        for (int i = 0; i < m_distribution.length; i++) {
            if (maxVotes < m_distribution[i]) {
                majorityIdx = i;
                maxVotes = m_distribution[i];
            }
        }
        return majorityIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float[] getClassProbabilities() {
        final float[] classProbabilities = new float[m_distribution.length];
        for (int i = 0; i < classProbabilities.length; i++) {
            classProbabilities[i] = m_distribution[i] / m_nrVotes;
        }
        return classProbabilities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrVotes() {
        return m_nrVotes;
    }

}
