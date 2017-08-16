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
 *   24.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;

/**
 * A stopping criterion tells the learning algorithm when to stop the training.
 *
 * @author Adrian Nembach, KNIME.com
 */
interface StoppingCriterion <T extends TrainingRow> {

    /**
     * Check if the training should be stopped.
     *
     * @param beta current estimate of the coefficient matrix
     * @return true if the training converged and should be stopped
     */
    public boolean checkConvergence(WeightMatrix<T> beta);

    /**
     * Creates a new stopping criterion that returns true for checkConvergence when either <b>first</b>
     * or <b>second</b> return true.
     *
     * @param first stopping criterion
     * @param second stopping criterion
     * @return or-combination of <b>first</b> and <b>second</b>
     */
//    public static <T extends TrainingRow> StoppingCriterion<T> or(final StoppingCriterion<T> first, final StoppingCriterion<T> second) {
//        return beta -> first.checkConvergence(beta) || second.checkConvergence(beta);
//    }

    /**
     * Creates a new stopping criterion that returns true for checkConvergence only if both <b>first</b> and <b>second</b>
     * return true.
     *
     * @param first stopping criterion
     * @param second stopping criterion
     * @return and-combination of <b>first</b> and <b>second</b>
     */
//    public static <T extends TrainingRow> StoppingCriterion<T> and(final StoppingCriterion<T> first, final StoppingCriterion<T> second) {
//        return beta -> first.checkConvergence(beta) && second.checkConvergence(beta);
//    }
}
