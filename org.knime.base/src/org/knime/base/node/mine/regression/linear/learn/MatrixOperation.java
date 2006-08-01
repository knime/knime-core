/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 27, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.learn;

/**
 * Utility class that allows to invert matrices. More functionality may follow
 * and moving to another package (as need arises). For now it supports to invert
 * matrices.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class MatrixOperation {
    private MatrixOperation() {
    }

    /**
     * Calculates the inverse matrix of a given matrix. The implementation
     * applies the decomposition according to Gauss-Jordan identifying pivot
     * elements.
     * 
     * @param aOrig the original matrix
     * @return the inverse matrix
     * @throws ArithmeticException if the matrix is not a square matrix or the
     *             inverse can not be computed (because of linear dependencies)
     * @throws NullPointerException if the argument is <code>null</code> or
     *             contains <code>null</code> elements
     */
    public static double[][] inverse(final double[][] aOrig) {
        final int size = aOrig.length;
        double[][] a = new double[size][];
        for (int r = 0; r < size; r++) {
            if (aOrig[r].length != size) {
                throw new ArithmeticException(
                        "Can't compute inverse of non-square matrix.");
            }
            double[] buf = new double[size];
            System.arraycopy(aOrig[r], 0, buf, 0, size);
            a[r] = buf;
        }
        double[][] e = new double[size][size];
        int[] rowOrder = new int[size];
        for (int i = 0; i < size; i++) {
            rowOrder[i] = i;
            e[i][i] = 1.0;
        }
        // over all columns
        for (int c = 0; c < size; c++) {
            // (a) determine pivot row, pivot element is at (c, P[c])
            int l = c;
            double max = Math.abs(a[rowOrder[l]][c]);
            // over all rows
            for (int r = c + 1; r < size; r++) {
                double value = Math.abs(a[rowOrder[r]][c]);
                if (value > max) {
                    max = value;
                    l = r;
                }
            }
            if (max == 0.0) {
                throw new ArithmeticException("No solution.");
            }
            int swap = rowOrder[c];
            rowOrder[c] = rowOrder[l];
            rowOrder[l] = swap;
            l = rowOrder[c];
            // normalize pivot row
            double pivotValue = a[l][c];
            for (int c1 = 0; c1 < size; c1++) {
                a[l][c1] = a[l][c1] / pivotValue;
                e[l][c1] = e[l][c1] / pivotValue;
            }
            for (int r1 = 0; r1 < size; r1++) {
                if (rowOrder[r1] != l) {
                    for (int c1 = 0; c1 < size; c1++) {
                        if (c1 != c) {
                            a[rowOrder[r1]][c1] -= a[rowOrder[r1]][c]
                                    * a[l][c1];
                        }
                        e[rowOrder[r1]][c1] -= a[rowOrder[r1]][c] * e[l][c1];
                    }
                }
            }
            for (int r1 = 0; r1 < size; r1++) {
                if (rowOrder[r1] != l) {
                    a[rowOrder[r1]][c] = 0.0;
                }
            }
        }
        double[][] inverse = new double[size][];
        for (int r = 0; r < size; r++) {
            inverse[r] = e[rowOrder[r]];
        }
        return inverse;
    }

    /**
     * Multiplies two matrices.
     * 
     * @param m1 the matrix to the left
     * @param m2 the matrix to the right
     * @return the matrix product, it will have as many rows as m1 and as many
     *         columns as m2
     * @throws ArithmeticException if that fails, e.g. invalid sizes of matrices
     * @throws NullPointerException if the argument is <code>null</code> or
     *             contains <code>null</code> elements
     */
    public static double[][] multiply(final double[][] m1, final double[][] m2) {
        int m1Rows = m1.length;
        int m2Rows = m2.length;
        int m1Cols = m1Rows > 0 ? m1[0].length : m2Rows;
        int m2Cols = m2Rows > 0 ? m2[0].length : 0;
        if (m1Cols != m2Rows) {
            throw new ArithmeticException(
                    "Matrix can't be multiplied, invalid sizes: (" + m1Rows
                            + "x" + m1Cols + ") vs. (" + m2Rows + "x" + m2Cols
                            + ")");
        }
        double[][] result = new double[m1Rows][m2Cols];
        for (int r = 0; r < m1Rows; r++) {
            for (int c1 = 0; c1 < m1Cols; c1++) {
                for (int c2 = 0; c2 < m2Cols; c2++) {
                    result[r][c2] += m1[r][c1] * m2[c1][c2];
                }
            }
        }
        return result;
    }
}
