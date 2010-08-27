/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.10.2006 (sieb): created
 */
package org.knime.base.util.math;

/**
 * Implements basic statistical functions.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public final class StatisticUtils {

    private StatisticUtils() {

    }

    /**
     * Calculates the covariance matrix for the given input matrix.
     * 
     * @param dataMatrix The input data matrix. The first dimension represents
     *            data rows, the second dimension the columns (attributes).
     * 
     * @return the square covariance matrix
     */
    public static double[][] covariance(final double[][] dataMatrix) {

        int numRows = dataMatrix.length;
        int numCols = dataMatrix[0].length;

        double[][] covMatrix = new double[numCols][numCols];

        int degrees = (numRows - 1);

        double sum;
        double meanCol1;
        double meanCol2;

        // the start index for the second column loop
        // as the covariance matrix is symmetric in each
        // further row in the matrix requires one element less of
        // computation
        int colIdx2Start = 0;

        // the outer two loops iterate all fields of the
        // covariance matrix
        for (int colIdx1 = 0; colIdx1 < numCols; colIdx1++) {

            for (int colIdx2 = colIdx2Start; colIdx2 < numCols; colIdx2++) {

                sum = 0;
                meanCol1 = 0;
                meanCol2 = 0;
                for (int rowIdx = 0; rowIdx < numRows; rowIdx++) {
                    meanCol1 += dataMatrix[rowIdx][colIdx1];
                    meanCol2 += dataMatrix[rowIdx][colIdx2];
                }

                meanCol1 = meanCol1 / numRows;
                meanCol2 = meanCol2 / numRows;
                for (int k = 0; k < numRows; k++) {
                    sum += (dataMatrix[k][colIdx1] - meanCol1)
                            * (dataMatrix[k][colIdx2] - meanCol2);
                }

                // set the covariance to the coresponding field in
                // the covariance matrix
                covMatrix[colIdx1][colIdx2] = sum / degrees;

                // also set the same value to the inverse index
                // as the covariance matrix is symmetric
                covMatrix[colIdx2][colIdx1] = covMatrix[colIdx1][colIdx2];
            }

            colIdx2Start++;
        }
        return covMatrix;
    }

    /**
     * Calculates the standard deviation for each column of the given input
     * matrix.
     * 
     * @param matrix the input matrix
     * @return an array with the standard deviation for each column
     */
    public static double[] standardDeviation(final double[][] matrix) {

        double[] var = variance(matrix);
        for (int i = 0; i < var.length; i++) {

            var[i] = Math.sqrt(var[i]);
        }

        return var;
    }

    /**
     * Calculates the variance for each column of the given input matrix.
     * 
     * @param matrix the input matrix
     * @return an array with the variance for each column
     */
    public static double[] variance(final double[][] matrix) {

        int numRows = matrix.length;
        int numCols = matrix[0].length;

        double[] var = new double[numCols];
        int degrees = (numRows - 1);
        double sum;
        double mean;

        for (int col = 0; col < numCols; col++) {

            sum = 0;
            mean = 0;
            for (int row = 0; row < numRows; row++) {
                mean += matrix[row][col];
            }
            mean = mean / numRows;

            for (int row = 0; row < numRows; row++) {
                sum += (matrix[row][col] - mean) * (matrix[row][col] - mean);
            }

            var[col] = sum / degrees;
        }
        return var;
    }

    /**
     * Calculates the mean for each column of the given input matrix.
     * 
     * @param matrix the input matrix
     * @return an array with the mean for each column
     */
    public static double[] mean(final double[][] matrix) {

        int numRows = matrix.length;
        int numCols = matrix[0].length;

        double[] mean = new double[numCols];

        for (int col = 0; col < numCols; col++) {
            
            for (int row = 0; row < numRows; row++) {

                mean[col] += matrix[row][col];
            }
            
            mean[col] /= numRows;
        }
        
        return mean;
    }
}
