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
 *   09.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sag;

import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingRow;

/**
 * Represents a loss function used by the {@link SagOptimizer}.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> The type of TrainingRow on which this loss can be evaluated
 */
public interface Loss<T extends TrainingRow> {

    /**
     * Returns the loss for <b>row</b> with respect to the <b>prediction</b> supplied by the linear model.
     *
     * @param row for which the loss should be evaluated
     * @param prediction of the linear model for <b>row</b>
     * @return the loss for <b>row</b> and <b>prediction</b>
     */
    public double evaluate(final T row, final double[] prediction);

    /**
     * Returns the gradient of the loss with respect to the <b>row</b> and the <b>prediction</b>.
     *
     * @param row for which the gradient should be calculated
     * @param prediction of the linear model for <b>row</b>
     * @return the gradient with respect to <b>row</b> and <b>prediction</b>
     */
    public double[] gradient(final T row, final double[] prediction);

    /**
     * Returns the hessian matrix (second derivatives) for the given weights.
     *
     * @param data data used to optimize the weights in <b>beta</b>
     * @param beta weight vector describing a linear model for <b>data</b>
     * @return the hessian matrix
     */
    public double[][] hessian(final TrainingData<T> data, final WeightVector<T> beta);
}
