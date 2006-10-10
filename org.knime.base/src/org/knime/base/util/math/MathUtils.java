/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.10.2006 (sieb): created
 */
package org.knime.base.util.math;

/**
 * Implements basic mathematical functions.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public final class MathUtils {

    private MathUtils() {

    }

    /**
     * Calculates sqrt(x^2 + y^2) reducing the risk of over- or underflow. The
     * default equation is transformed as follows:<br>
     * result = sqrt(x^2 + y^2)<br>
     * result^2 = x^2 + y^2<br>
     * result^2 = x^2 * (1 + y^2/x^2)<br>
     * result^2 = x^2 * (1 + (y/x)^2)<br>
     * result = |x| * sqrt(1 + (y/x)^2)<br>
     * 
     * It is important to perform a case differentiation. The formula is
     * transformed the same way but by extracting y instead of x. The advantage
     * is that the ^2 is performed on a mostly smaller number due to the
     * division.
     * 
     * @param x the x value
     * @param y the y value
     * @return sqrt(x^2 + y^2)
     */
    public static double hypotenuse(final double x, final double y) {

        double result;

        if (Math.abs(x) > Math.abs(y)) {
            result = y / x;
            result = Math.abs(x) * Math.sqrt(1 + result * result);
        } else if (y != 0) {
            result = x / y;
            result = Math.abs(y) * Math.sqrt(1 + result * result);
        } else {
            result = 0.0;
        }
        
        return result;
    }

    /**
     * Multiplies two matrices. Matrix 1 is multiplied from the left to matrix
     * 2. Therefore, result matrix = matrix 1 * matrix 2. The matrices must be
     * compatible, i.e. the number of columns of matrix 1 must equal to the
     * number of rows of matrix 2.
     * 
     * @param matrix1 the matrix on the left side
     * @param matrix2 the matrix on the right side
     * @return the result matrix
     * @throws IllegalArgumentException if the matrices are not compatible
     */
    public static double[][] multiply(final double[][] matrix1,
            final double[][] matrix2) throws IllegalArgumentException {

        // set the number of columns for both matrices M1 and M2
        int numColsM1 = matrix1[0].length;
        int numColsM2 = matrix2[0].length;

        // set the number of rows for both matrices M1 and M2
        int numRowsM1 = matrix1.length;
        int numRowsM2 = matrix2.length;

        // check matrix compatibility
        if (numColsM1 != numRowsM2) {
            throw new IllegalArgumentException(
                    "Uncompatible matrices for multiplication.");
        }

        // the result matrix has the number of rows of matrix 1 and the
        // number of columns of matrix 2
        double[][] resultMatrix = new double[numRowsM1][numColsM2];

        // the result matrix is created row by row, i.e. it is iterated
        // over the rows of matrix 1
        for (int rowM1 = 0; rowM1 < numRowsM1; rowM1++) {

            // now it is iterated over the number of columns of matrix 2
            for (int colM2 = 0; colM2 < numColsM2; colM2++) {

                // finally perform the multiplication for all columns of the
                // current row of matrix 1 with all row fields of the current
                // column of matrix 2, as the number of cols of matrix 1
                // must be equal with the number of rows of matrix 2 (matrix
                // compatibility) it does not matter which variable is
                // used for the loop as termination value (here: numColM1)
                double tmp = 0;
                for (int k = 0; k < numColsM1; k++) {
                    tmp += matrix1[rowM1][k] * matrix2[k][colM2];
                }
                resultMatrix[rowM1][colM2] = tmp;
            }
        }

        return resultMatrix;
    }

    /**
     * Transposes the given matrix.
     * 
     * @param inputMatrix the matrix to transposed
     * @return the transposed matrix where the number of rows and columns is
     *         changed according to the given matrix
     */
    public static double[][] transpose(final double[][] inputMatrix) {

        int numCols = inputMatrix[0].length;
        int numRows = inputMatrix.length;

        // create the result matrix
        double[][] transposedMatrix = new double[numCols][numRows];

        for (int row = 0; row < numRows; row++) {

            for (int col = 0; col < numCols; col++) {

                transposedMatrix[col][row] = inputMatrix[row][col];
            }
        }

        return transposedMatrix;
    }

    /**
     * Normalizes the matrix relative to the mean of the input data and to the
     * standard deviation.
     * 
     * @param matrix the matrix to normalize
     * @param standardDev the standard deviation for all columns used to
     *            normalize the matrix
     * @param mean the mean for all columns used to normalize the matrix
     * @return the normalized matrix
     */
    public static double[][] normalizeMatrix(final double[][] matrix,
            final double[] standardDev, final double[] mean) {

        double[][] normMatrix = new double[matrix.length][matrix[0].length];

        for (int row = 0; row < normMatrix.length; row++) {

            for (int column = 0; column < normMatrix[row].length; column++) {

                normMatrix[row][column] = (matrix[row][column] - mean[column])
                        / standardDev[column];
            }
        }

        return normMatrix;
    }

    /**
     * Normalizes the matrix relative to the mean of the input data.
     * 
     * @param matrix the matrix to normalize
     * @param mean the mean for all columns used to normalize the matrix
     * 
     * @return the normalized matrix
     */
    public static double[][] normalizeMatrix(final double[][] matrix,
            final double[] mean) {

        double[][] normMatrix = new double[matrix.length][matrix[0].length];

        for (int row = 0; row < normMatrix.length; row++) {

            for (int column = 0; column < normMatrix[row].length; column++) {

                normMatrix[row][column] = matrix[row][column] - mean[column];
            }
        }

        return normMatrix;
    }

    /**
     * Denormalizes the matrix relativ to the mean of the input data and to the
     * standard deviation.
     * 
     * @param y the matrix to denormalize
     * @param standardDev the standard deviation for all columns used to
     *            denormalize the matrix
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized matrix
     */
    public static double[][] denormalizeMatrix(final double[][] y,
            final double[] standardDev, final double[] mean) {

        double[][] denormMatrix = new double[y.length][y[0].length];

        for (int i = 0; i < denormMatrix.length; i++) {

            for (int j = 0; j < denormMatrix[i].length; j++) {

                denormMatrix[i][j] = (y[i][j] * standardDev[j]) + mean[j];
            }
        }

        return denormMatrix;
    }

    /**
     * Denormalizes the vector relative to the mean of the input data and to the
     * standard deviation.
     * 
     * @param vector the input array to denormalize
     * @param standardDev the standard deviation for all columns used to
     *            denormalize the matrix
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized vector
     */
    public static double[] denormalizeVector(final double[] vector,
            final double standardDev, final double mean) {

        return denormalizeMatrix(new double[][]{vector},
                new double[]{standardDev}, new double[]{mean})[0];
    }

    /**
     * Denormalizes the vector relative to the mean of the input data.
     * 
     * @param vector the input array to denormalize
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized vector
     */
    public static double[] denormalizeVector(final double[] vector,
            final double mean) {

        return denormalizeMatrix(new double[][]{vector}, new double[]{mean})[0];
    }

    /**
     * Denormalizes the matrix relativ to the mean of the input data.
     * 
     * @param y the matrix to denormalize
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized matrix
     */
    public static double[][] denormalizeMatrix(final double[][] y,
            final double[] mean) {

        double[][] denormMatrix = new double[y.length][y[0].length];

        for (int i = 0; i < denormMatrix.length; i++) {

            for (int j = 0; j < denormMatrix[i].length; j++) {

                denormMatrix[i][j] = y[i][j] + mean[j];
            }
        }

        return denormMatrix;
    }

    private static void print(double[][] matrix) {
        for (double[] row : matrix) {
            for (double field : row) {
                System.out.print(field + "\t");
            }
            System.out.print("\n");
        }
    }

    public static void main(String[] args) {

        double[][] m1 = {{-0.678, -0.735}, {-0.735, 0.678}};
        double[][] m2 = {{0.69, 0.49}, {-1.31, -1.21}, {0.39, 0.99}};

        System.out.println("Orignial eigen vectors:");
        print(m1);
        System.out.println("Orignial normalized data:");
        print(m2);

        m1 = transpose(m1);
        m2 = transpose(m2);

        System.out.println("Transposed eigen vectors:");
        print(m1);
        System.out.println("Transposed normalized data:");
        print(m2);

        m1 = multiply(m1, m2);
        System.out.println("Multiplied matrix:");
        print(m1);

        m1 = transpose(m1);
        System.out.println("Back transposed multiplied matrix:");
        print(m1);

    }
}
