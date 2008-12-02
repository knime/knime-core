/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
