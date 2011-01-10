/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 *   Feb 28, 2006 (wiswedel): created
 */
package org.knime.base.util.math;

import java.util.Random;

import junit.framework.TestCase;

/** Test case for utitlity class MatrixOperation. */
public class MatrixOperationTest extends TestCase {
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
     * Test method for 'MatrixOperation.inverse(double[][])'.
     */
    public void testInverse() {
        double[][] d;
        double[][] inverse;
        long seed = System.currentTimeMillis();
        System.out.println("Testing with random seed: " + seed);
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
            if (error > 1E-10) {
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
