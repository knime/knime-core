/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   24.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;

/**
 * Checks the relative changes of the beta matrix.
 * Cheap because only beta is iterated.
 *
 * @author Adrian Nembach, KNIME.com
 */
class BetaChangeStoppingCriterion <T extends TrainingRow> implements StoppingCriterion<T> {

    private final double[][] m_oldBeta;
    private final int m_nCats;
    private final int m_nFets;
    private final double m_epsilon;

    /**
     * Creates a BetaChangeStoppingCriterion.
     * For a K class logistic regression problem <b>nLinModels</b> would be K-1.
     *
     * @param nFets number of features including the intercept term
     * @param nLinModels number of linear models
     * @param epsilon threshold for the relative change
     *
     */
    public BetaChangeStoppingCriterion(final int nFets, final int nLinModels, final double epsilon) {
        m_nCats = nLinModels;
        m_nFets = nFets;
        m_epsilon = epsilon;
        m_oldBeta = new double[nLinModels][nFets];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkConvergence(final WeightMatrix<T> beta) {
        double absMaxVal = Double.NEGATIVE_INFINITY;
        double absMaxChange = Double.NEGATIVE_INFINITY;
        double[][] betaMat = beta.getWeightVector();
        for (int c = 0; c < m_nCats; c++) {
            for (int i = 0; i < m_nFets; i++) {
                double val = betaMat[c][i];
                double absVal = Math.abs(val);
                double absChange = Math.abs(val - m_oldBeta[c][i]);
                absMaxChange = absChange > absMaxChange ? absChange : absMaxChange;
                absMaxVal = absVal > absMaxVal ? absVal : absMaxVal;
                m_oldBeta[c][i] = val;
            }
        }
        return absMaxChange / absMaxVal < m_epsilon;
    }

}
