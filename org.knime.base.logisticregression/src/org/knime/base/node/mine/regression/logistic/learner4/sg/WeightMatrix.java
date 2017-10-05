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
 *   20.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;

/**
 * Represents the coefficient vector/matrix (also sometimes called beta) of a linear model.
 * The first column is reserved for the intercept term and gets a special treatment.
 *
 * @author Adrian Nembach, KNIME.com
 */
interface WeightMatrix <T extends TrainingRow> {

    /**
     * Scale the entire vector/matrix except for the intercept term.
     *
     * @param scaleFactor the factor by which to scale the coefficients
     */
    public void scale(double scaleFactor);

    /**
     * Apply eager updates to the coefficients.
     * That means that we iterate over all coefficients (except for the intercept term if <b>includeIntercept</b> is false).
     *
     * @param func function to apply to the individual coefficients
     * @param includeIntercept flag that indicates whether the updates should also be calculated for the intercept terms
     */
    public void update(final WeightVectorConsumer1 func, final boolean includeIntercept);

    /**
     * Apply updates only to those coefficients that are non zero in <b>row</b>
     * @param func the function to apply to the individual coefficients
     * @param includeIntercept flag that indicates whether the updates should also be calculated for the intercept terms
     * @param row for which the coefficients need to be updated
     */
    public void update(final WeightVectorConsumer2 func, final boolean includeIntercept, final TrainingRow row);

    /**
     * Apply any accumulated updates (e.g. scaling).
     */
    public void normalize();

    /**
     * Returns an array of double arrays that represents the coefficients in a matrix form.
     *
     * @return the coefficient matrix as array of double arrays
     */
    public double[][] getWeightVector();

    /**
     * Calculates the outputs of the linear models represented by this weight vector/matrix.
     * Each row of the matrix yields a prediction which is calculated as the inner product of the weightMatrix with <b>row</b>.
     *
     * @param row to predict
     * @return a vector with the predictions of the individual linear models represented by this WeightVector
     */
    public double[] predict(final T row);

    /**
     * The scaling factor of this WeightMatrix
     *
     * @return the current scale
     */
    public double getScale();

    /**
     * Returns the number of variables on which this object operates.
     * @return the number of variables
     */
    public int getNVariables();

    /**
     * Returns the number of vectors (models).
     * @return the number of vectors (models)
     */
    public int getNVectors();

    /**
     * Functional interface for functions of the coefficient value, its category index (row in the matrix) and its feature index (column in the matrix).
     *
     * @author Adrian Nembach, KNIME.com
     */
    interface WeightVectorConsumer1 {
        /**
         *
         * @param val value of the coefficient
         * @param c category (row) index of coefficient
         * @param i feature (column) index of coefficient
         * @return value of the function you want to apply
         */
        public double calculate(double val, int c, int i);
    }

    /**
     * Functional interface for functions of the coefficient value, its category (row) index, its feature (column) index and
     * the value of the feature in the row we are currently looking at.
     *
     * @author Adrian Nembach, KNIME.com
     */
    interface WeightVectorConsumer2 {
        /**
         *
         * @param betaValue value of the coefficient
         * @param category (row) index of coefficient
         * @param featureIdx (column) index of coefficient
         * @param featureValue value of the feature in the currently looked at row
         * @return value of the function you want to apply
         */
        public double calculate(double betaValue, int category, int featureIdx, double featureValue);
    }

}
