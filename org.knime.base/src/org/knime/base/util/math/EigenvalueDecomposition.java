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
 * Eigenvalues and eigenvectors of a real matrix.
 * <P>
 * If A is symmetric, then A = V*D*V' where the eigenvalue matrix D is diagonal
 * and the eigenvector matrix V is orthogonal. I.e. A =
 * V.times(D.times(V.transpose())) and V.times(V.transpose()) equals the
 * identity matrix.
 * <P>
 * If A is not symmetric, then the eigenvalue matrix D is block diagonal with
 * the real eigenvalues in 1-by-1 blocks and any complex eigenvalues, lambda +
 * i*mu, in 2-by-2 blocks, [lambda, mu; -mu, lambda]. The columns of V represent
 * the eigenvectors in the sense that A*V = V*D, i.e. A.times(V) equals
 * V.times(D). The matrix V may be badly conditioned, or even singular, so the
 * validity of the equation A = V*D*inverse(V) depends upon V.cond().
 * 
 * Most of the code of this class is taken from the corresponding JMathTools
 * class and is under the BSD licence with the following required disclaimer:
 * 
 * Copyright (c) 2003, Yann RICHET All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither the name of JMATHTOOLS nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
public class EigenvalueDecomposition {

    /**
     * Row and column dimension (square matrix).
     * 
     * @serial matrix dimension.
     */
    private int m_matrixSquareSize;

    /**
     * Symmetry flag.
     * 
     * @serial internal symmetry flag.
     */
    private boolean m_issymmetric;

    /**
     * Arrays for internal storage of eigenvalues.
     * 
     * @serial internal storage of eigenvalues.
     */
    private double[] m_eigVals1, m_eigVals2;

    /**
     * Array for internal storage of eigenvectors.
     * 
     * @serial internal storage of eigenvectors.
     */
    private double[][] m_eigVecs;

    /**
     * Array for internal storage of nonsymmetric Hessenberg form.
     * 
     * @serial internal storage of nonsymmetric Hessenberg form.
     */
    private double[][] m_nonSymHess;

    /**
     * Working storage for nonsymmetric algorithm.
     * 
     * @serial working storage for nonsymmetric algorithm.
     */
    private double[] m_store;

    /*
     * ------------------------ Private Methods ------------------------
     */

    // Symmetric Householder reduction to tridiagonal form.
    private void tred2() {

        // This is derived from the Algol procedures tred2 by
        // Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
        // Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
        // Fortran subroutine in EISPACK.

        for (int j = 0; j < m_matrixSquareSize; j++) {
            m_eigVals1[j] = m_eigVecs[m_matrixSquareSize - 1][j];
        }

        // Householder reduction to tridiagonal form.

        for (int i = m_matrixSquareSize - 1; i > 0; i--) {

            // Scale to avoid under/overflow.

            double scale = 0.0;
            double h = 0.0;
            for (int k = 0; k < i; k++) {
                scale = scale + Math.abs(m_eigVals1[k]);
            }
            if (scale == 0.0) {
                m_eigVals2[i] = m_eigVals1[i - 1];
                for (int j = 0; j < i; j++) {
                    m_eigVals1[j] = m_eigVecs[i - 1][j];
                    m_eigVecs[i][j] = 0.0;
                    m_eigVecs[j][i] = 0.0;
                }
            } else {

                // Generate Householder vector.

                for (int k = 0; k < i; k++) {
                    m_eigVals1[k] /= scale;
                    h += m_eigVals1[k] * m_eigVals1[k];
                }
                double f = m_eigVals1[i - 1];
                double g = Math.sqrt(h);
                if (f > 0) {
                    g = -g;
                }
                m_eigVals2[i] = scale * g;
                h = h - f * g;
                m_eigVals1[i - 1] = f - g;
                for (int j = 0; j < i; j++) {
                    m_eigVals2[j] = 0.0;
                }

                // Apply similarity transformation to remaining columns.

                for (int j = 0; j < i; j++) {
                    f = m_eigVals1[j];
                    m_eigVecs[j][i] = f;
                    g = m_eigVals2[j] + m_eigVecs[j][j] * f;
                    for (int k = j + 1; k <= i - 1; k++) {
                        g += m_eigVecs[k][j] * m_eigVals1[k];
                        m_eigVals2[k] += m_eigVecs[k][j] * f;
                    }
                    m_eigVals2[j] = g;
                }
                f = 0.0;
                for (int j = 0; j < i; j++) {
                    m_eigVals2[j] /= h;
                    f += m_eigVals2[j] * m_eigVals1[j];
                }
                double hh = f / (h + h);
                for (int j = 0; j < i; j++) {
                    m_eigVals2[j] -= hh * m_eigVals1[j];
                }
                for (int j = 0; j < i; j++) {
                    f = m_eigVals1[j];
                    g = m_eigVals2[j];
                    for (int k = j; k <= i - 1; k++) {
                        m_eigVecs[k][j] -= (f * m_eigVals2[k] + g
                                * m_eigVals1[k]);
                    }
                    m_eigVals1[j] = m_eigVecs[i - 1][j];
                    m_eigVecs[i][j] = 0.0;
                }
            }
            m_eigVals1[i] = h;
        }

        // Accumulate transformations.

        for (int i = 0; i < m_matrixSquareSize - 1; i++) {
            m_eigVecs[m_matrixSquareSize - 1][i] = m_eigVecs[i][i];
            m_eigVecs[i][i] = 1.0;
            double h = m_eigVals1[i + 1];
            if (h != 0.0) {
                for (int k = 0; k <= i; k++) {
                    m_eigVals1[k] = m_eigVecs[k][i + 1] / h;
                }
                for (int j = 0; j <= i; j++) {
                    double g = 0.0;
                    for (int k = 0; k <= i; k++) {
                        g += m_eigVecs[k][i + 1] * m_eigVecs[k][j];
                    }
                    for (int k = 0; k <= i; k++) {
                        m_eigVecs[k][j] -= g * m_eigVals1[k];
                    }
                }
            }
            for (int k = 0; k <= i; k++) {
                m_eigVecs[k][i + 1] = 0.0;
            }
        }
        for (int j = 0; j < m_matrixSquareSize; j++) {
            m_eigVals1[j] = m_eigVecs[m_matrixSquareSize - 1][j];
            m_eigVecs[m_matrixSquareSize - 1][j] = 0.0;
        }
        m_eigVecs[m_matrixSquareSize - 1][m_matrixSquareSize - 1] = 1.0;
        m_eigVals2[0] = 0.0;
    }

    // Symmetric tridiagonal QL algorithm.

    private void tql2() {

        // This is derived from the Algol procedures tql2, by
        // Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
        // Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
        // Fortran subroutine in EISPACK.

        for (int i = 1; i < m_matrixSquareSize; i++) {
            m_eigVals2[i - 1] = m_eigVals2[i];
        }
        m_eigVals2[m_matrixSquareSize - 1] = 0.0;

        double f = 0.0;
        double tst1 = 0.0;
        double eps = Math.pow(2.0, -52.0);
        for (int l = 0; l < m_matrixSquareSize; l++) {

            // Find small subdiagonal element

            tst1 = Math.max(tst1, Math.abs(m_eigVals1[l])
                    + Math.abs(m_eigVals2[l]));
            int m = l;
            while (m < m_matrixSquareSize) {
                if (Math.abs(m_eigVals2[m]) <= eps * tst1) {
                    break;
                }
                m++;
            }

            // If m == l, d[l] is an eigenvalue,
            // otherwise, iterate.

            if (m > l) {
                int iter = 0;
                do {
                    iter = iter + 1; // (Could check iteration count here.)

                    // Compute implicit shift

                    double g = m_eigVals1[l];
                    double p = (m_eigVals1[l + 1] - g) / (2.0 * m_eigVals2[l]);
                    double r = MathUtils.hypotenuse(p, 1.0);
                    if (p < 0) {
                        r = -r;
                    }
                    m_eigVals1[l] = m_eigVals2[l] / (p + r);
                    m_eigVals1[l + 1] = m_eigVals2[l] * (p + r);
                    double dl1 = m_eigVals1[l + 1];
                    double h = g - m_eigVals1[l];
                    for (int i = l + 2; i < m_matrixSquareSize; i++) {
                        m_eigVals1[i] -= h;
                    }
                    f = f + h;

                    // Implicit QL transformation.

                    p = m_eigVals1[m];
                    double c = 1.0;
                    double c2 = c;
                    double c3 = c;
                    double el1 = m_eigVals2[l + 1];
                    double s = 0.0;
                    double s2 = 0.0;
                    for (int i = m - 1; i >= l; i--) {
                        c3 = c2;
                        c2 = c;
                        s2 = s;
                        g = c * m_eigVals2[i];
                        h = c * p;
                        r = MathUtils.hypotenuse(p, m_eigVals2[i]);
                        m_eigVals2[i + 1] = s * r;
                        s = m_eigVals2[i] / r;
                        c = p / r;
                        p = c * m_eigVals1[i] - s * g;
                        m_eigVals1[i + 1] = h + s * (c * g + s * m_eigVals1[i]);

                        // Accumulate transformation.

                        for (int k = 0; k < m_matrixSquareSize; k++) {
                            h = m_eigVecs[k][i + 1];
                            m_eigVecs[k][i + 1] = s * m_eigVecs[k][i] + c * h;
                            m_eigVecs[k][i] = c * m_eigVecs[k][i] - s * h;
                        }
                    }
                    p = -s * s2 * c3 * el1 * m_eigVals2[l] / dl1;
                    m_eigVals2[l] = s * p;
                    m_eigVals1[l] = c * p;

                    // Check for convergence.

                } while (Math.abs(m_eigVals2[l]) > eps * tst1);
            }
            m_eigVals1[l] = m_eigVals1[l] + f;
            m_eigVals2[l] = 0.0;
        }

        // Sort eigenvalues and corresponding vectors.

        for (int i = 0; i < m_matrixSquareSize - 1; i++) {
            int k = i;
            double p = m_eigVals1[i];
            for (int j = i + 1; j < m_matrixSquareSize; j++) {
                if (m_eigVals1[j] < p) {
                    k = j;
                    p = m_eigVals1[j];
                }
            }
            if (k != i) {
                m_eigVals1[k] = m_eigVals1[i];
                m_eigVals1[i] = p;
                for (int j = 0; j < m_matrixSquareSize; j++) {
                    p = m_eigVecs[j][i];
                    m_eigVecs[j][i] = m_eigVecs[j][k];
                    m_eigVecs[j][k] = p;
                }
            }
        }
    }

    // Nonsymmetric reduction to Hessenberg form.

    private void orthes() {

        // This is derived from the Algol procedures orthes and ortran,
        // by Martin and Wilkinson, Handbook for Auto. Comp.,
        // Vol.ii-Linear Algebra, and the corresponding
        // Fortran subroutines in EISPACK.

        int low = 0;
        int high = m_matrixSquareSize - 1;

        for (int m = low + 1; m <= high - 1; m++) {

            // Scale column.

            double scale = 0.0;
            for (int i = m; i <= high; i++) {
                scale = scale + Math.abs(m_nonSymHess[i][m - 1]);
            }
            if (scale != 0.0) {

                // Compute Householder transformation.

                double h = 0.0;
                for (int i = high; i >= m; i--) {
                    m_store[i] = m_nonSymHess[i][m - 1] / scale;
                    h += m_store[i] * m_store[i];
                }
                double g = Math.sqrt(h);
                if (m_store[m] > 0) {
                    g = -g;
                }
                h = h - m_store[m] * g;
                m_store[m] = m_store[m] - g;

                // Apply Householder similarity transformation
                // H = (I-u*u'/h)*H*(I-u*u')/h)

                for (int j = m; j < m_matrixSquareSize; j++) {
                    double f = 0.0;
                    for (int i = high; i >= m; i--) {
                        f += m_store[i] * m_nonSymHess[i][j];
                    }
                    f = f / h;
                    for (int i = m; i <= high; i++) {
                        m_nonSymHess[i][j] -= f * m_store[i];
                    }
                }

                for (int i = 0; i <= high; i++) {
                    double f = 0.0;
                    for (int j = high; j >= m; j--) {
                        f += m_store[j] * m_nonSymHess[i][j];
                    }
                    f = f / h;
                    for (int j = m; j <= high; j++) {
                        m_nonSymHess[i][j] -= f * m_store[j];
                    }
                }
                m_store[m] = scale * m_store[m];
                m_nonSymHess[m][m - 1] = scale * g;
            }
        }

        // Accumulate transformations (Algol's ortran).

        for (int i = 0; i < m_matrixSquareSize; i++) {
            for (int j = 0; j < m_matrixSquareSize; j++) {
                m_eigVecs[i][j] = (i == j ? 1.0 : 0.0);
            }
        }

        for (int m = high - 1; m >= low + 1; m--) {
            if (m_nonSymHess[m][m - 1] != 0.0) {
                for (int i = m + 1; i <= high; i++) {
                    m_store[i] = m_nonSymHess[i][m - 1];
                }
                for (int j = m; j <= high; j++) {
                    double g = 0.0;
                    for (int i = m; i <= high; i++) {
                        g += m_store[i] * m_eigVecs[i][j];
                    }
                    // Double division avoids possible underflow
                    g = (g / m_store[m]) / m_nonSymHess[m][m - 1];
                    for (int i = m; i <= high; i++) {
                        m_eigVecs[i][j] += g * m_store[i];
                    }
                }
            }
        }
    }

    // Complex scalar division.
    private transient double m_cdivr, m_cdivi;

    private void cdiv(final double xr, final double xi, final double yr,
            final double yi) {
        double r, d;
        if (Math.abs(yr) > Math.abs(yi)) {
            r = yi / yr;
            d = yr + r * yi;
            m_cdivr = (xr + r * xi) / d;
            m_cdivi = (xi - r * xr) / d;
        } else {
            r = yr / yi;
            d = yi + r * yr;
            m_cdivr = (r * xr + xi) / d;
            m_cdivi = (r * xi - xr) / d;
        }
    }

    // Nonsymmetric reduction from Hessenberg to real Schur form.

    private void hqr2() {

        // This is derived from the Algol procedure hqr2,
        // by Martin and Wilkinson, Handbook for Auto. Comp.,
        // Vol.ii-Linear Algebra, and the corresponding
        // Fortran subroutine in EISPACK.

        // Initialize

        int nn = this.m_matrixSquareSize;
        int n = nn - 1;
        int low = 0;
        int high = nn - 1;
        double eps = Math.pow(2.0, -52.0);
        double exshift = 0.0;
        double p = 0, q = 0, r = 0, s = 0, z = 0, t, w, x, y;

        // Store roots isolated by balanc and compute matrix norm

        double norm = 0.0;
        for (int i = 0; i < nn; i++) {
            if (i < low | i > high) {
                m_eigVals1[i] = m_nonSymHess[i][i];
                m_eigVals2[i] = 0.0;
            }
            for (int j = Math.max(i - 1, 0); j < nn; j++) {
                norm = norm + Math.abs(m_nonSymHess[i][j]);
            }
        }

        // Outer loop over eigenvalue index

        int iter = 0;
        while (n >= low) {

            // Look for single small sub-diagonal element

            int l = n;
            while (l > low) {
                s = Math.abs(m_nonSymHess[l - 1][l - 1])
                        + Math.abs(m_nonSymHess[l][l]);
                if (s == 0.0) {
                    s = norm;
                }
                if (Math.abs(m_nonSymHess[l][l - 1]) < eps * s) {
                    break;
                }
                l--;
            }

            // Check for convergence
            // One root found

            if (l == n) {
                m_nonSymHess[n][n] = m_nonSymHess[n][n] + exshift;
                m_eigVals1[n] = m_nonSymHess[n][n];
                m_eigVals2[n] = 0.0;
                n--;
                iter = 0;

                // Two roots found

            } else if (l == n - 1) {
                w = m_nonSymHess[n][n - 1] * m_nonSymHess[n - 1][n];
                p = (m_nonSymHess[n - 1][n - 1] - m_nonSymHess[n][n]) / 2.0;
                q = p * p + w;
                z = Math.sqrt(Math.abs(q));
                m_nonSymHess[n][n] = m_nonSymHess[n][n] + exshift;
                m_nonSymHess[n - 1][n - 1] = m_nonSymHess[n - 1][n - 1]
                        + exshift;
                x = m_nonSymHess[n][n];

                // Real pair

                if (q >= 0) {
                    if (p >= 0) {
                        z = p + z;
                    } else {
                        z = p - z;
                    }
                    m_eigVals1[n - 1] = x + z;
                    m_eigVals1[n] = m_eigVals1[n - 1];
                    if (z != 0.0) {
                        m_eigVals1[n] = x - w / z;
                    }
                    m_eigVals2[n - 1] = 0.0;
                    m_eigVals2[n] = 0.0;
                    x = m_nonSymHess[n][n - 1];
                    s = Math.abs(x) + Math.abs(z);
                    p = x / s;
                    q = z / s;
                    r = Math.sqrt(p * p + q * q);
                    p = p / r;
                    q = q / r;

                    // Row modification

                    for (int j = n - 1; j < nn; j++) {
                        z = m_nonSymHess[n - 1][j];
                        m_nonSymHess[n - 1][j] = q * z + p * m_nonSymHess[n][j];
                        m_nonSymHess[n][j] = q * m_nonSymHess[n][j] - p * z;
                    }

                    // Column modification

                    for (int i = 0; i <= n; i++) {
                        z = m_nonSymHess[i][n - 1];
                        m_nonSymHess[i][n - 1] = q * z + p * m_nonSymHess[i][n];
                        m_nonSymHess[i][n] = q * m_nonSymHess[i][n] - p * z;
                    }

                    // Accumulate transformations

                    for (int i = low; i <= high; i++) {
                        z = m_eigVecs[i][n - 1];
                        m_eigVecs[i][n - 1] = q * z + p * m_eigVecs[i][n];
                        m_eigVecs[i][n] = q * m_eigVecs[i][n] - p * z;
                    }

                    // Complex pair

                } else {
                    m_eigVals1[n - 1] = x + p;
                    m_eigVals1[n] = x + p;
                    m_eigVals2[n - 1] = z;
                    m_eigVals2[n] = -z;
                }
                n = n - 2;
                iter = 0;

                // No convergence yet

            } else {

                // Form shift

                x = m_nonSymHess[n][n];
                y = 0.0;
                w = 0.0;
                if (l < n) {
                    y = m_nonSymHess[n - 1][n - 1];
                    w = m_nonSymHess[n][n - 1] * m_nonSymHess[n - 1][n];
                }

                // Wilkinson's original ad hoc shift

                if (iter == 10) {
                    exshift += x;
                    for (int i = low; i <= n; i++) {
                        m_nonSymHess[i][i] -= x;
                    }
                    s = Math.abs(m_nonSymHess[n][n - 1])
                            + Math.abs(m_nonSymHess[n - 1][n - 2]);
                    x = y = 0.75 * s;
                    w = -0.4375 * s * s;
                }

                // MATLAB's new ad hoc shift

                if (iter == 30) {
                    s = (y - x) / 2.0;
                    s = s * s + w;
                    if (s > 0) {
                        s = Math.sqrt(s);
                        if (y < x) {
                            s = -s;
                        }
                        s = x - w / ((y - x) / 2.0 + s);
                        for (int i = low; i <= n; i++) {
                            m_nonSymHess[i][i] -= s;
                        }
                        exshift += s;
                        x = y = w = 0.964;
                    }
                }

                iter = iter + 1; // (Could check iteration count here.)

                // Look for two consecutive small sub-diagonal elements

                int m = n - 2;
                while (m >= l) {
                    z = m_nonSymHess[m][m];
                    r = x - z;
                    s = y - z;
                    p = (r * s - w) / m_nonSymHess[m + 1][m]
                            + m_nonSymHess[m][m + 1];
                    q = m_nonSymHess[m + 1][m + 1] - z - r - s;
                    r = m_nonSymHess[m + 2][m + 1];
                    s = Math.abs(p) + Math.abs(q) + Math.abs(r);
                    p = p / s;
                    q = q / s;
                    r = r / s;
                    if (m == l) {
                        break;
                    }
                    if (Math.abs(m_nonSymHess[m][m - 1])
                            * (Math.abs(q) + Math.abs(r)) < eps
                            * (Math.abs(p) * (Math
                                    .abs(m_nonSymHess[m - 1][m - 1])
                                    + Math.abs(z) + Math
                                    .abs(m_nonSymHess[m + 1][m + 1])))) {
                        break;
                    }
                    m--;
                }

                for (int i = m + 2; i <= n; i++) {
                    m_nonSymHess[i][i - 2] = 0.0;
                    if (i > m + 2) {
                        m_nonSymHess[i][i - 3] = 0.0;
                    }
                }

                // Double QR step involving rows l:n and columns m:n

                for (int k = m; k <= n - 1; k++) {
                    boolean notlast = (k != n - 1);
                    if (k != m) {
                        p = m_nonSymHess[k][k - 1];
                        q = m_nonSymHess[k + 1][k - 1];
                        r = (notlast ? m_nonSymHess[k + 2][k - 1] : 0.0);
                        x = Math.abs(p) + Math.abs(q) + Math.abs(r);
                        if (x != 0.0) {
                            p = p / x;
                            q = q / x;
                            r = r / x;
                        }
                    }
                    if (x == 0.0) {
                        break;
                    }
                    s = Math.sqrt(p * p + q * q + r * r);
                    if (p < 0) {
                        s = -s;
                    }
                    if (s != 0) {
                        if (k != m) {
                            m_nonSymHess[k][k - 1] = -s * x;
                        } else if (l != m) {
                            m_nonSymHess[k][k - 1] = -m_nonSymHess[k][k - 1];
                        }
                        p = p + s;
                        x = p / s;
                        y = q / s;
                        z = r / s;
                        q = q / p;
                        r = r / p;

                        // Row modification

                        for (int j = k; j < nn; j++) {
                            p = m_nonSymHess[k][j] + q * m_nonSymHess[k + 1][j];
                            if (notlast) {
                                p = p + r * m_nonSymHess[k + 2][j];
                                m_nonSymHess[k + 2][j] = m_nonSymHess[k + 2][j]
                                        - p * z;
                            }
                            m_nonSymHess[k][j] = m_nonSymHess[k][j] - p * x;
                            m_nonSymHess[k + 1][j] = m_nonSymHess[k + 1][j] - p
                                    * y;
                        }

                        // Column modification

                        for (int i = 0; i <= Math.min(n, k + 3); i++) {
                            p = x * m_nonSymHess[i][k] + y
                                    * m_nonSymHess[i][k + 1];
                            if (notlast) {
                                p = p + z * m_nonSymHess[i][k + 2];
                                m_nonSymHess[i][k + 2] = m_nonSymHess[i][k + 2]
                                        - p * r;
                            }
                            m_nonSymHess[i][k] = m_nonSymHess[i][k] - p;
                            m_nonSymHess[i][k + 1] = m_nonSymHess[i][k + 1] - p
                                    * q;
                        }

                        // Accumulate transformations

                        for (int i = low; i <= high; i++) {
                            p = x * m_eigVecs[i][k] + y * m_eigVecs[i][k + 1];
                            if (notlast) {
                                p = p + z * m_eigVecs[i][k + 2];
                                m_eigVecs[i][k + 2] = m_eigVecs[i][k + 2] - p
                                        * r;
                            }
                            m_eigVecs[i][k] = m_eigVecs[i][k] - p;
                            m_eigVecs[i][k + 1] = m_eigVecs[i][k + 1] - p * q;
                        }
                    } // (s != 0)
                } // k loop
            } // check convergence
        } // while (n >= low)

        // Backsubstitute to find vectors of upper triangular form

        if (norm == 0.0) {
            return;
        }

        for (n = nn - 1; n >= 0; n--) {
            p = m_eigVals1[n];
            q = m_eigVals2[n];

            // Real vector

            if (q == 0) {
                int l = n;
                m_nonSymHess[n][n] = 1.0;
                for (int i = n - 1; i >= 0; i--) {
                    w = m_nonSymHess[i][i] - p;
                    r = 0.0;
                    for (int j = l; j <= n; j++) {
                        r = r + m_nonSymHess[i][j] * m_nonSymHess[j][n];
                    }
                    if (m_eigVals2[i] < 0.0) {
                        z = w;
                        s = r;
                    } else {
                        l = i;
                        if (m_eigVals2[i] == 0.0) {
                            if (w != 0.0) {
                                m_nonSymHess[i][n] = -r / w;
                            } else {
                                m_nonSymHess[i][n] = -r / (eps * norm);
                            }

                            // Solve real equations

                        } else {
                            x = m_nonSymHess[i][i + 1];
                            y = m_nonSymHess[i + 1][i];
                            q = (m_eigVals1[i] - p) * (m_eigVals1[i] - p)
                                    + m_eigVals2[i] * m_eigVals2[i];
                            t = (x * s - z * r) / q;
                            m_nonSymHess[i][n] = t;
                            if (Math.abs(x) > Math.abs(z)) {
                                m_nonSymHess[i + 1][n] = (-r - w * t) / x;
                            } else {
                                m_nonSymHess[i + 1][n] = (-s - y * t) / z;
                            }
                        }

                        // Overflow control

                        t = Math.abs(m_nonSymHess[i][n]);
                        if ((eps * t) * t > 1) {
                            for (int j = i; j <= n; j++) {
                                m_nonSymHess[j][n] = m_nonSymHess[j][n] / t;
                            }
                        }
                    }
                }

                // Complex vector

            } else if (q < 0) {
                int l = n - 1;

                // Last vector component imaginary so matrix is triangular

                if (Math.abs(m_nonSymHess[n][n - 1]) > Math
                        .abs(m_nonSymHess[n - 1][n])) {
                    m_nonSymHess[n - 1][n - 1] = q / m_nonSymHess[n][n - 1];
                    m_nonSymHess[n - 1][n] = -(m_nonSymHess[n][n] - p)
                            / m_nonSymHess[n][n - 1];
                } else {
                    cdiv(0.0, -m_nonSymHess[n - 1][n],
                            m_nonSymHess[n - 1][n - 1] - p, q);
                    m_nonSymHess[n - 1][n - 1] = m_cdivr;
                    m_nonSymHess[n - 1][n] = m_cdivi;
                }
                m_nonSymHess[n][n - 1] = 0.0;
                m_nonSymHess[n][n] = 1.0;
                for (int i = n - 2; i >= 0; i--) {
                    double ra, sa, vr, vi;
                    ra = 0.0;
                    sa = 0.0;
                    for (int j = l; j <= n; j++) {
                        ra = ra + m_nonSymHess[i][j] * m_nonSymHess[j][n - 1];
                        sa = sa + m_nonSymHess[i][j] * m_nonSymHess[j][n];
                    }
                    w = m_nonSymHess[i][i] - p;

                    if (m_eigVals2[i] < 0.0) {
                        z = w;
                        r = ra;
                        s = sa;
                    } else {
                        l = i;
                        if (m_eigVals2[i] == 0) {
                            cdiv(-ra, -sa, w, q);
                            m_nonSymHess[i][n - 1] = m_cdivr;
                            m_nonSymHess[i][n] = m_cdivi;
                        } else {

                            // Solve complex equations

                            x = m_nonSymHess[i][i + 1];
                            y = m_nonSymHess[i + 1][i];
                            vr = (m_eigVals1[i] - p) * (m_eigVals1[i] - p)
                                    + m_eigVals2[i] * m_eigVals2[i] - q * q;
                            vi = (m_eigVals1[i] - p) * 2.0 * q;
                            if (vr == 0.0 & vi == 0.0) {
                                vr = eps
                                        * norm
                                        * (Math.abs(w) + Math.abs(q)
                                                + Math.abs(x) + Math.abs(y) + Math
                                                .abs(z));
                            }
                            cdiv(x * r - z * ra + q * sa, x * s - z * sa - q
                                    * ra, vr, vi);
                            m_nonSymHess[i][n - 1] = m_cdivr;
                            m_nonSymHess[i][n] = m_cdivi;
                            if (Math.abs(x) > (Math.abs(z) + Math.abs(q))) {
                                m_nonSymHess[i + 1][n - 1] = (-ra - w
                                        * m_nonSymHess[i][n - 1] + q
                                        * m_nonSymHess[i][n])
                                        / x;
                                m_nonSymHess[i + 1][n] = (-sa - w
                                        * m_nonSymHess[i][n] - q
                                        * m_nonSymHess[i][n - 1])
                                        / x;
                            } else {
                                cdiv(-r - y * m_nonSymHess[i][n - 1], -s - y
                                        * m_nonSymHess[i][n], z, q);
                                m_nonSymHess[i + 1][n - 1] = m_cdivr;
                                m_nonSymHess[i + 1][n] = m_cdivi;
                            }
                        }

                        // Overflow control

                        t = Math.max(Math.abs(m_nonSymHess[i][n - 1]), Math
                                .abs(m_nonSymHess[i][n]));
                        if ((eps * t) * t > 1) {
                            for (int j = i; j <= n; j++) {
                                m_nonSymHess[j][n - 1] = m_nonSymHess[j][n - 1]
                                        / t;
                                m_nonSymHess[j][n] = m_nonSymHess[j][n] / t;
                            }
                        }
                    }
                }
            }
        }

        // Vectors of isolated roots

        for (int i = 0; i < nn; i++) {
            if (i < low | i > high) {
                for (int j = i; j < nn; j++) {
                    m_eigVecs[i][j] = m_nonSymHess[i][j];
                }
            }
        }

        // Back transformation to get eigenvectors of original matrix

        for (int j = nn - 1; j >= low; j--) {
            for (int i = low; i <= high; i++) {
                z = 0.0;
                for (int k = low; k <= Math.min(j, high); k++) {
                    z = z + m_eigVecs[i][k] * m_nonSymHess[k][j];
                }
                m_eigVecs[i][j] = z;
            }
        }
    }

    /*
     * ------------------------ Constructor ------------------------
     */

    /**
     * Check for symmetry, then construct the eigenvalue decomposition.
     * 
     * @param arg Square matrix
     */
    public EigenvalueDecomposition(final double[][] arg) {

        m_matrixSquareSize = arg[0].length;
        m_eigVecs = new double[m_matrixSquareSize][m_matrixSquareSize];
        m_eigVals1 = new double[m_matrixSquareSize];
        m_eigVals2 = new double[m_matrixSquareSize];

        m_issymmetric = true;
        for (int j = 0; (j < m_matrixSquareSize) & m_issymmetric; j++) {
            for (int i = 0; (i < m_matrixSquareSize) & m_issymmetric; i++) {
                m_issymmetric = (arg[i][j] == arg[j][i]);
            }
        }

        if (m_issymmetric) {
            for (int i = 0; i < m_matrixSquareSize; i++) {
                for (int j = 0; j < m_matrixSquareSize; j++) {
                    m_eigVecs[i][j] = arg[i][j];
                }
            }

            // Tridiagonalize.
            tred2();

            // Diagonalize.
            tql2();

        } else {
            m_nonSymHess = new double[m_matrixSquareSize][m_matrixSquareSize];
            m_store = new double[m_matrixSquareSize];

            for (int j = 0; j < m_matrixSquareSize; j++) {
                for (int i = 0; i < m_matrixSquareSize; i++) {
                    m_nonSymHess[i][j] = arg[i][j];
                }
            }

            // Reduce to Hessenberg form.
            orthes();

            // Reduce Hessenberg to real Schur form.
            hqr2();
        }
    }

    /*
     * ------------------------ Public Methods ------------------------
     */

    /**
     * @return the eigenvector matrix.
     */
    public boolean isDReal() {
        boolean b = false;
        for (int i = 0; i < m_eigVals2.length; i++) {
            if (m_eigVals2[i] > 0) {
                b = true;
            }
        }
        return b;
    }

    /**
     * @return the eigenvector matrix
     */
    public double[][] getV() {
        return m_eigVecs;
        // return new Matrix(V,n,n);
    }

    /**
     * @return the block diagonal eigenvalue matrix
     */
    public double[][] getD() {

        double[][] matrix = new double[m_matrixSquareSize][m_matrixSquareSize];
        for (int i = 0; i < m_matrixSquareSize; i++) {
            matrix[i][i] = m_eigVals1[i];
            if (m_eigVals2[i] > 0) {
                matrix[i][i + 1] = m_eigVals2[i];
            } else if (m_eigVals2[i] < 0) {
                matrix[i][i - 1] = m_eigVals2[i];
            }
        }
        return matrix;
    }

    /**
     * @return Real(D) the real diagonal eigenvalue matrix
     */
    public double[] get1DRealD() {
        return m_eigVals1;
    }

    /**
     * @return Imag(D) the imaginary diagonal eigenvalue matrix
     */
    public double[] get1DImagD() {
        return m_eigVals2;
    }

    /**
     * @return Real(D) the real diagonal eigenvalue matrix
     */
    public double[][] getRealD() {
        double[][] matrix = new double[m_matrixSquareSize][m_matrixSquareSize];
        for (int i = 0; i < m_matrixSquareSize; i++) {
            matrix[i][i] = m_eigVals1[i];
        }
        return matrix;
    }

    /**
     * @return Imag(D) the imaginary diagonal eigenvalue matrix
     */
    public double[][] getImagD() {
        double[][] matrix = new double[m_matrixSquareSize][m_matrixSquareSize];
        for (int i = 0; i < m_matrixSquareSize; i++) {
            matrix[i][i] = m_eigVals2[i];
        }
        return matrix;
    }
}
