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
 *   04.04.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import org.knime.base.node.mine.regression.logistic.learner4.TrainingRow;
import org.knime.core.node.util.CheckUtils;

/**
 * Decays the learning rate over time depending on the epoch. <br>
 * Note that the epochs are expected to be 0 based i.e. the first epoch is epoch 0.<br>
 * The learning rate for each epoch is calculated as follows: <br>
 * {@code newLearningRate = initialLearningRate / (1 + epoch / annealingRate)} <br>
 * It is important to note that a larger annealing
 *
 * @author Adrian Nembach, KNIME.com
 */
class AnnealingLearningRateStrategy <T extends TrainingRow> implements LearningRateStrategy<T> {

    private final double m_annealingRate;
    private final double m_initialLearningRate;
    private double m_currentLearningRate;

    /**
     * Constructs an instance of AnnealingRateStrategy.
     *
     * @param annealingRate specifies how strongly the learning rate is
     * annealed after each epoch
     * @param initialLearningRate the learning rate to start from
     *
     */
    public AnnealingLearningRateStrategy(final double annealingRate, final double initialLearningRate) {
        CheckUtils.checkArgument(annealingRate > 0, "The annealingRate must be larger than 0 (but was " +
                annealingRate + ").");
        CheckUtils.checkArgument(initialLearningRate > 0.0, "The initial learning rate must be larger than 0 (but was " +
                initialLearningRate + ").");
        m_annealingRate = annealingRate;
        m_initialLearningRate = initialLearningRate;
        // set current learning rate to be the initial learning rate
        // should be overwritten by startNewEpoch before the first
        // call to getCurrentLearningRate
        m_currentLearningRate = m_initialLearningRate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCurrentLearningRate(final T row, final double[] prediction, final double[] gradient) {
        return m_currentLearningRate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startNewEpoch(final int epoch) {
        m_currentLearningRate = m_initialLearningRate / (1 + epoch / m_annealingRate);
        System.out.println("Step size: " + m_currentLearningRate + " in epoch " + epoch);
    }

}
