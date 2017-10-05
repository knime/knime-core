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
 * Performs updates lazily, that is, only if the corresponding feature of a coefficient is present in the currently looked at row.
 *
 * @author Adrian Nembach, KNIME.com
 */
interface LazyUpdater <T extends TrainingRow> extends Updater<T> {

    /**
     * Accumulate updates for coefficients of present features in <b>x</b>.
     * Does not perform the actual updates.
     *
     * @param x the currently looked at row
     * @param sig the partial gradient for each linear model
     * @param beta the current estimate of the coefficient matrix
     * @param stepSize or learning rate for gradient descent steps
     * @param iteration the current iteration
     */
    void update(T x, double[] sig, WeightMatrix<T> beta, double stepSize, int iteration);

    /**
     * Apply accumulated updates for the coefficients for which the features are present in <b>x</b>.
     *
     * @param beta the current estimate of the coefficient matrix
     * @param x the currently looked at row
     * @param lastVisited array containing for each feature in which iteration it was last seen (non zero)
     * @param iteration the current iteration
     */
    void lazyUpdate(final WeightMatrix<T> beta, final T x, final int[] lastVisited, final int iteration);

    /**
     * Apply accumulated updates to all coefficients and reset tracking.
     * Called after an epoch is finished.
     *
     * @param beta the current estimate of the coefficient matrix
     * @param lastVisited array containing for each feature in which iteration it was last seen (non zero)
     */
    void resetJITSystem(final WeightMatrix<T> beta, final int[] lastVisited);

    /**
     * Apply accumulated updates to all coefficients but don't completely reset tracking.
     * This means that tracking continues from the current iteration.
     * Calling this function is equivalent to observing a row with all features present.
     *
     * @param beta the current estimate of the coefficient matrix
     * @param lastVisited array containing for each feature in which iteration it was last seen (non zero)
     * @param iteration the current iteration
     */
    void normalize(final WeightMatrix<T> beta, final int[] lastVisited, final int iteration);
}
