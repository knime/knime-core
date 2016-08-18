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
 *   Aug 17, 2016 (adrian): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.classification;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;
import org.knime.core.node.util.CheckUtils;

/**
 * Acts as super-class for HardVoting and SoftVoting.
 * Contains the logic that maps the target values to the internally index used
 * for the class distributions.
 *
 * @author Adrian Nembach, University of Konstanz
 */
abstract class AbstractVoting implements Voting {

    private final Map<String, Integer> m_targetValueToIndexMap;
    private int m_nrVotes;

    /**
     * @param targetValueToIndexMap a map that assigns a unique index to each target value
     *
     */
    public AbstractVoting(final Map<String, Integer> targetValueToIndexMap) {
        m_targetValueToIndexMap = targetValueToIndexMap;
        m_nrVotes = 0;
    }

    protected final int getIndexForClass(final String classValue) {
        final Integer idx = m_targetValueToIndexMap.get(classValue);
        CheckUtils.checkArgumentNotNull(idx, "The class \"%s\" is unknown.", classValue);
        return idx.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addVote(final TreeNodeClassification leaf) {
        m_nrVotes++;
        updateDistribution(leaf);
    }

    /**
     * Updates the target distribution that is maintained by sub classes.
     *
     * @param leaf the matching leaf in the current tree.
     */
    protected abstract void updateDistribution(final TreeNodeClassification leaf);

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getMajorityClass() {
        String majorityClass = null;
        if (m_nrVotes == 0) {
            return majorityClass;
        }
        float highestProb = -1.0f;
        final Set<Entry<String, Integer>> entries = m_targetValueToIndexMap.entrySet();
        for (Entry<String, Integer> entry : entries) {
            final String classValue = entry.getKey();
            final float prob = getClassProbabilityForClass(classValue);
            if (prob > highestProb) {
                highestProb = prob;
                majorityClass = classValue;
            }
        }
        assert majorityClass != null : "It is not possible that no class has a probability >= 0.";
        return majorityClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getNrVotes() {
        return m_nrVotes;
    }
}
