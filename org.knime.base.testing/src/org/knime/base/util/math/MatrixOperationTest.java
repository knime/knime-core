/*
 * -------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Feb 28, 2006 (wiswedel): created
 */
package org.knime.base.util.math;

import java.util.Random;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;

/** Test case for utility class MatrixOperation. */
public class MatrixOperationTest extends TestCase {

    /**
     * Test method for 'MatrixOperation.hypotenuse(double, double)'.
     */
    public void testHypotenuse() {
        assertEquals(MathUtils.hypotenuse(2,1),Math.sqrt(5));
        assertEquals(MathUtils.hypotenuse(1,2),Math.sqrt(5));
        assertEquals(MathUtils.hypotenuse(-2,1),Math.sqrt(5));
        assertEquals(MathUtils.hypotenuse(-2,-1),Math.sqrt(5));
        assertEquals(MathUtils.hypotenuse(0,2),2.0);
        assertEquals(MathUtils.hypotenuse(2,0),2.0);
    }

    /**
     * Test method for 'MatrixOperation.multiply(double[][], double[][])'.
     */
    public void testMultiply() {
        double[][] d1 = new double[][]{new double[]{3, 4}, new double[]{6, 8},
                new double[]{4, 1}};
        double[][] d2 = new double[][]{new double[]{-1, 2, 3},
                new double[]{1, 4, 5}};
        double[][] mult = MathUtils.multiply(d1, d2);
        assertEquals(mult.length, 3);
        assertEquals(mult[0].length, 3);
        assertEquals(mult[1].length, 3);
        assertEquals(mult[2].length, 3);
        assertEquals(mult[0][0], 1.0);
        assertEquals(mult[0][1], 22.0);
        assertEquals(mult[0][2], 29.0);
        assertEquals(mult[1][0], 2.0);
        assertEquals(mult[1][1], 44.0);
        assertEquals(mult[1][2], 58.0);
        assertEquals(mult[2][0], -3.0);
        assertEquals(mult[2][1], 12.0);
        assertEquals(mult[2][2], 17.0);
        try {
            MathUtils.multiply(d1, d1);
            fail("Accepted invalid matrix size");
        } catch (IllegalArgumentException ae) {
            // nothing
        }
    }

    /**
     * Test method for 'MathUtils.add(double[][], double[][])'.
     */
    public void testAddMatrices() {
        double[][] matrixA = new double[][]{new double[]{1.3, 3.3,-0.6},
                                            new double[]{0.2, 1.9, 3.8}};
        double[][] matrixB = new double[][]{new double[]{-1.3, 2.3, 3.1},
                                            new double[]{0,   -1.4, 2.3}};
        double[][] expected = new double[][]{new double[]{0, 5.6, 2.5},
                                             new double[]{0.2, 0.5, 6.1}};
        double[][] result;
        try {
            result = MathUtils.add(matrixA, new double[][]{new double[]{2.6, 1},
                                                                new double[]{0.2, 3.3}});
            fail("Matrices of uncompatible sizes accepted.");
        } catch (IllegalArgumentException e) {
            // OK
        }

        result = MathUtils.add(matrixA, matrixB);
        assertEquals(expected.length, result.length);
        assertEquals(expected[0].length, result[0].length);
        assertEquals(expected[1].length, result[1].length);
        for (int r = 0; r < result.length; r++) {
            for (int c = 0; c < expected[r].length; c++) {
                assertEquals(expected[r][c], result[r][c]);
            }
        }
    }

    /**
     * Test method for 'MathUtils.subtract(double[][], double[][])'.
     */
    public void testSubtractMatrices() {
        double[][] matrixA = new double[][]{new double[]{1.3, 3.3,-0.6},
                                            new double[]{0.2, 1.8, 3.8}};
        double[][] matrixB = new double[][]{new double[]{-1.3, 2.3, 3.1},
                                            new double[]{0,   -1.5, 2.3}};
        double[][] expected = new double[][]{new double[]{2.6, 1, -3.7},
                                             new double[]{0.2, 3.3, 1.5}};
        double[][] result;
        try {
            result = MathUtils.subtract(matrixA, new double[][]{new double[]{2.6, 1},
                                                                new double[]{0.2, 3.3}});
            fail("Matrices of uncompatible sizes accepted.");
        } catch (IllegalArgumentException e) {
            // OK
        }

        result = MathUtils.subtract(matrixA, matrixB);
        assertEquals(expected.length, result.length);
        assertEquals(expected[0].length, result[0].length);
        assertEquals(expected[1].length, result[1].length);
        for (int r = 0; r < result.length; r++) {
            for (int c = 0; c < expected[r].length; c++) {
                assertEquals(expected[r][c], result[r][c]);
            }
        }
    }

    /**
     * Test method for 'MatrixOperation.multiply(double[][], double[])'.
     */
    public void testMultiplyMatrixVector() {
        double[][] d1 = new double[][]{new double[]{3, 4}, new double[]{6, 8},
                new double[]{4, 1}};
        double[] d2 = new double[]{-1, 1};
        double[] result = MathUtils.multiply(d1, d2);
        assertEquals(result.length, 3);
        assertEquals(result[0], 1.0);
        assertEquals(result[1], 2.0);
        assertEquals(result[2], -3.0);
        try {
            MathUtils.multiply(d1, new double[]{-1, 1, 2});
            fail("Accepted matrix and vector of incompatible sizes.");
        } catch (IllegalArgumentException ae) {
            // nothing
        }
    }

    /**
     * Test method for 'MathUtils.subtract(double[], double[])'.
     */
    public void testSubtractVectors() {
        double[] vectorA = new double[]{-1.5, 5, 3};
        double[] vectorB = new double[]{-4, 2, 7.3};
        double[] expected = {2.5, 3, -4.3};
        double[] result;
        try {
            result = MathUtils.subtract(vectorA, new double[vectorA.length + 1]);
            fail("Vectors of different lengths accepted.");
        } catch (IllegalArgumentException e) {
            // OK
        }

        result = MathUtils.subtract(vectorA, vectorB);
        assertEquals(expected.length, result.length);
        for (int d = 0; d < result.length; d++) {
            assertEquals(expected[d], result[d]);
        }
    }

    /**
     * Test method for 'MathUtils.dotProduct(double[], double[])'.
     */
    public void testDotProduct() {
        double[] vectorA = new double[]{-1.5, 5, 3};
        double[] vectorB = new double[]{-4, 2, 7.3};
        assertEquals(37.9, MathUtils.dotProduct(vectorA, vectorB));
        try {
            MathUtils.dotProduct(vectorA, new double[]{-4, 2});
            fail("Vectors of different lengths accepted.");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    /**
     * Test method for 'MathUtils.multiplyLeftWithTranspose(double[][], double[])'.
     */
    public void testMultiplyLeftWithTranspose() {

        double[][] matrix = new double[][]{new double[]{3, 4},
            new double[]{6, 8},
            new double[]{4, 1}};
        double[][] expected = new double[][]{
            new double[]{61, 64},
            new double[]{64, 81}};
        double[][] result = MathUtils.multiplyLeftWithTranspose(matrix);
        assertEquals(expected[0][0], result[0][0]);
        assertEquals(expected[0][1], result[0][1]);
        assertEquals(expected[1][0], result[1][0]);
        assertEquals(expected[1][1], result[1][1]);


        matrix = new double[][]{new double[]{3, 6, 4},
                                new double[]{4, 8, 1}};
        expected = new double[][]{
            new double[]{25, 50,  16},
            new double[]{50, 100, 32},
            new double[]{16, 32,  17}};
        result = MathUtils.multiplyLeftWithTranspose(matrix);

        assertEquals(expected[0][0], result[0][0]);
        assertEquals(expected[0][1], result[0][1]);
        assertEquals(expected[0][2], result[0][2]);
        assertEquals(expected[1][0], result[1][0]);
        assertEquals(expected[1][1], result[1][1]);
        assertEquals(expected[1][2], result[1][2]);
        assertEquals(expected[2][0], result[2][0]);
        assertEquals(expected[2][1], result[2][1]);
        assertEquals(expected[2][2], result[2][2]);
    }

    /**
     * Test method for 'MatrixOperation.transpose(double[][])'.
     */
    public void testTranspose() {

        double[][] matrix = new double[][]{new double[]{3, 4},
                                           new double[]{6, 8},
                                           new double[]{4, 1}};
        double[][] expected = new double[][]{
            new double[]{3, 6, 4},
            new double[]{4, 8, 1}};
        double[][] result = MathUtils.transpose(matrix);
        assertEquals(matrix[0].length, result.length);
        assertEquals(matrix.length, result[0].length);
        assertEquals(expected[0][0], result[0][0]);
        assertEquals(expected[0][1], result[0][1]);
        assertEquals(expected[0][2], result[0][2]);
        assertEquals(expected[1][0], result[1][0]);
        assertEquals(expected[1][1], result[1][1]);
        assertEquals(expected[1][2], result[1][2]);
    }

    /**
     * Test method for 'MatrixOperation.inverse(double[][])'.
     */
    public void testInverse() {
        double[][] d;
        double[][] inverse;
        long seed = System.currentTimeMillis();
        NodeLogger.getLogger(getClass()).info("Testing with random seed: " + seed);
        Random rand = new Random(seed);
        for (int k = 0; k < 20; k++) {
            // draw size of matrix in [1, 100]
            int size = 1 + rand.nextInt(100);
            d = new double[size][size];
            for (int i = 0; i < d.length; i++) {
                for (int j = 0; j < d[i].length; j++) {
                    d[i][j] = -0.5 + 2 * rand.nextDouble();
                }
            }

            inverse = MathUtils.inverse(d);
            double[][] unity = MathUtils.multiply(inverse, d);
            double error = compareToUnitMatrix(unity);
            if (error > 1.1E-9) {
                fail("Matrix inversion failed, max error: " + error);
            }
        }
        // linear dependency should not allow a solution
        d = new double[][]{new double[]{3, 4, 2}, new double[]{6, 8, 4},
                // multiple of first row
                new double[]{4, 1, 5}};
        try {
            inverse = MathUtils.inverse(d);
            fail("Got invalid solution");
        } catch (ArithmeticException ae) {
            // nothing to do here
        }
        // non-square matrix
        d = new double[][]{new double[]{3, 4, 2}, new double[]{4, 1, 5}};
        try {
            inverse = MathUtils.inverse(d);
            fail("Got solution for non-square matrix");
        } catch (ArithmeticException ae) {
            // nothing to do here
        }


        // non-invertible matrix (determinant is 0)
        d = new double[][]{new double[]{1, 2, 3}, new double[]{4, 5, 6},
                new double[]{7, 8, 9}};
        try {
            inverse = MathUtils.inverse(d);
            fail("Got invalid solution");
        } catch (ArithmeticException ae) {
            // nothing to do here
        }
    }

    /**
     * Test method for 'MatrixOperation.normalizeMatrix(double[][])'.
     */
    public void testNormalizeMatrix() {
        double[][] matrix = new double[][]{new double[]{2, 4},
            new double[]{6, 7},
            new double[]{4, 4}};
        double [][] result = MathUtils.normalizeMatrix(matrix);
        assertEquals(matrix.length, result.length);
        assertEquals(matrix[0].length, result[0].length);
        assertEquals(matrix[1].length, result[1].length);
        assertEquals(matrix[2].length, result[2].length);

        double[] mean = new double[]{4, 5};
        double[] sd = new double[]{2, Math.sqrt(3)};
        for(int r = 0; r < matrix.length; r++) {
            for(int c = 0; c < matrix[0].length; c++) {
                assertEquals((matrix[r][c] - mean[c]) / sd[c], result[r][c]);
            }
        }
    }

    /**
     * Test method for 'MatrixOperation.normalizeMatrix(double[][], double[])'.
     */
    public void testNormalizeMatrixWithMean() {
        double[][] matrix = new double[][]{new double[]{2, 4},
            new double[]{6, 7},
            new double[]{4, 4}};
        double[] mean = new double[]{-1, 2};
        double [][] result = MathUtils.normalizeMatrix(matrix, mean);
        assertEquals(matrix.length, result.length);
        assertEquals(matrix[0].length, result[0].length);
        assertEquals(matrix[1].length, result[1].length);
        assertEquals(matrix[2].length, result[2].length);

        for(int r = 0; r < matrix.length; r++) {
            for(int c = 0; c < matrix[0].length; c++) {
                assertEquals((matrix[r][c] - mean[c]), result[r][c]);
            }
        }
    }

    /**
     * Test method for 'MatrixOperation.normalizeMatrix(double[][], double[], double[])'.
     */
    public void testNormalizeMatrixWithMeanAndSD() {
        double[][] matrix = new double[][]{new double[]{2, 4},
            new double[]{6, 7},
            new double[]{4, 4}};
        double[] mean = new double[]{-1, 2};
        double[] sd = new double[]{3, -1};

        double[][] result = new double[3][2];
        try {
            result = MathUtils.normalizeMatrix(matrix, sd, mean);
            fail("Negative SD accepted");
        } catch (IllegalArgumentException e) {
            //OK
        }

        sd = new double[]{3, 0};
        try {
            result = MathUtils.normalizeMatrix(matrix, sd, mean);
            fail("Zero SD accepted");
        } catch (IllegalArgumentException e) {
            //OK
        }

        sd = new double[]{3, 2};
        result = MathUtils.normalizeMatrix(matrix, sd, mean);
        assertEquals(matrix.length, result.length);
        assertEquals(matrix[0].length, result[0].length);
        assertEquals(matrix[1].length, result[1].length);
        assertEquals(matrix[2].length, result[2].length);

        for(int r = 0; r < matrix.length; r++) {
            for(int c = 0; c < matrix[0].length; c++) {
                assertEquals((matrix[r][c] - mean[c]) / sd[c], result[r][c]);
            }
        }
    }

    /**
     * Test method for 'MatrixOperation.denormalizeVector(double[], double, double)'.
     */
    public void testDenormalizeVectorWithMeanAndSD() {
        double[] vector = new double[]{2, 4, 6};
        double mean = 4.0;
        double sd = 2;
        double [] result = MathUtils.denormalizeVector(vector, sd, mean);
        assertEquals(vector.length, result.length);

        for(int r = 0; r < vector.length; r++) {
            assertEquals((vector[r] * sd) + mean, result[r]);
        }
    }

    /**
     * Test method for 'MatrixOperation.denormalizeVector(double[], double)'.
     */
    public void testDenormalizeVectorWithMean() {
        double[] vector = new double[]{2, 4, 6};
        double mean = -4.0;
        double [] result = MathUtils.denormalizeVector(vector, mean);
        assertEquals(vector.length, result.length);

        for(int r = 0; r < vector.length; r++) {
            assertEquals(vector[r] + mean, result[r]);
        }
    }

    /**
     * Test method for 'MatrixOperation.denormalizeMatrix(double[][], double[], double[])'.
     */
    public void testDenormalizeMatrix() {
        double[][] matrix = new double[][]{new double[]{2, 4},
            new double[]{6, 7},
            new double[]{4, 4}};
        double[] mean = new double[]{4, 5};
        double[] sd = new double[]{2, Math.sqrt(3)};
        double [][] result = MathUtils.denormalizeMatrix(matrix, sd, mean);
        assertEquals(matrix.length, result.length);
        assertEquals(matrix[0].length, result[0].length);
        assertEquals(matrix[1].length, result[1].length);
        assertEquals(matrix[2].length, result[2].length);

        for(int r = 0; r < matrix.length; r++) {
            for(int c = 0; c < matrix[0].length; c++) {
                assertEquals((matrix[r][c] * sd[c]) + mean[c], result[r][c]);
            }
        }
    }

    /**
     * Test method for 'MatrixOperation.denormalizeMatrix(double[][], double[])'.
     */
    public void testDenormalizeMatrixWithMean() {
        double[][] matrix = new double[][]{new double[]{2, 4},
            new double[]{6, 7},
            new double[]{4, 4}};
        double[] mean = new double[]{-2.4, 0};
        double [][] result = MathUtils.denormalizeMatrix(matrix, mean);
        assertEquals(matrix.length, result.length);
        assertEquals(matrix[0].length, result[0].length);
        assertEquals(matrix[1].length, result[1].length);
        assertEquals(matrix[2].length, result[2].length);

        for(int r = 0; r < matrix.length; r++) {
            for(int c = 0; c < matrix[0].length; c++) {
                assertEquals(matrix[r][c] + mean[c], result[r][c]);
            }
        }
    }

    /**
     * Test method for 'MathUtils.addArrays(double[], double[])'.
     */
    public void testAddArrays() {
        double[] vector1 = new double[]{2, 4, 6};
        double[] vector2 = new double[]{0, -4, 2.2};

        MathUtils.addArrays(vector1, vector2);
        assertEquals(2.0, vector1[0]);
        assertEquals(0.0, vector1[1]);
        assertEquals(8.2, vector1[2]);
    }

    /**
     * Test method for 'MathUtils.addMatrix(double[][], double[][])'.
     */
    public void testAddMatrix() {
        double[][] matrixA = new double[][]{new double[]{1.3, 3.3,-0.6},
                                            new double[]{0.2, 1.9, 3.8}};
        double[][] matrixB = new double[][]{new double[]{-1.3, 2.3, 3.1},
                                            new double[]{0,   -1.4, 2.3}};
        double[][] expected = new double[][]{new double[]{0, 5.6, 2.5},
                                             new double[]{0.2, 0.5, 6.1}};
        MathUtils.addMatrix(matrixA, matrixB);
        assertEquals(expected.length, matrixA.length);
        assertEquals(expected[0].length, matrixA[0].length);
        assertEquals(expected[1].length, matrixA[1].length);
        for (int r = 0; r < matrixA.length; r++) {
            for (int c = 0; c < expected[r].length; c++) {
                assertEquals(expected[r][c], matrixA[r][c]);
            }
        }
    }

    private double compareToUnitMatrix(final double[][] matrix) {
        int size = matrix.length;
        double maxDiff = 0.0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double diff = matrix[i][j] - (i == j ? 1.0 : 0.0);
                maxDiff = Math.max(maxDiff, Math.abs(diff));
            }
        }
        return maxDiff;
    }
}
