/*
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Christian Pich
 *
 * The multidimensional scaling class (MDS) is free software; you can 
 * redistribute it and/or modify it under the terms of the GNU General Public 
 * License as published by the Free Software Foundation; either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St., Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.knime.base.node.mine.mds;

/**
 * @author Christian Pich, University of Konstanz
 */
public final class ClassicalMDS {

    /**
     * 
     * Adds a point to a <code>d</code>-dimensional configuration of
     * <code>n</code> other points,
     * 
     * depending on its distances to them in <code>D</code>-dimensional
     * Euclidean space.
     * 
     * 
     * 
     * @param x low (<code>d</code>) dimensional positions of the
     *            <code>n</code> other points (<code>d x n</code>)
     * 
     * @param X high (<code>D</code>) dimensional positions of the
     *            <code>n</code> other points (<code>D x n</code>)
     * 
     * @param high (<code>D</code>) dimensional positions of the additional
     *            point
     * 
     * @return low (<code>d</code>) dimensional positions of the additional
     *         point
     * 
     */

    public static double[] place(double[][] x, double[][] X, double[] point) {

        int D = point.length; // originaldimension

        int n = x[0].length; // zahl der bereits platzierten objekte

        int d = x.length; // einbettungsdimension

        double[] distances = new double[n];

        double[] result = new double[d];

        double mean = 0; // mittlere distanz zu den platzierten objekten

        for (int i = 0; i < n; i++) {

            for (int dim = 0; dim < D; dim++) {

                distances[i] += Math.pow(X[dim][i] - point[dim], 2);

            }

            distances[i] = Math.sqrt(distances[i]);

            mean += distances[i];

        }

        mean /= n;

        // platziere punkt in den gew. schwerpunkt der anderen objekte

        for (int dim = 0; dim < d; dim++) {

            for (int i = 0; i < n; i++) {

                result[dim] -= .5 * (distances[i] - mean) * x[dim][i];

            }

        }

        return result;

    }

    public static double strain(double[][] B, double[] x, double[] y) {

        double result = 0;

        int n = x.length;

        for (int i = 0; i < n; i++) {

            for (int j = 0; j < i; j++) {

                result += Math.pow(B[i][j] - (x[i] * x[j] + y[i] * y[j]), 2);

            }

        }

        return result * 2;

    }

    public static double stress(double[][] D, double[] x, double[] y) {

        double result = 0;

        int n = x.length;

        for (int i = 0; i < n; i++) {

            for (int j = 0; j < i; j++) {

                result +=
                        Math.pow(D[i][j]
                                - Math.sqrt(Math.pow(x[i] - x[j], 2)
                                        + Math.pow(y[i] - y[j], 2)), 2);

            }

        }

        return result;

    }

    public static double wstress(double[][] D, double[] x, double[] y) {

        double result = 0;

        int n = x.length;

        for (int i = 0; i < n; i++) {

            for (int j = 0; j < i; j++) {

                result +=
                        Math.pow(D[i][j], -2)
                                * Math.pow(D[i][j]
                                        - Math.sqrt(Math.pow(x[i] - x[j], 2)
                                                + Math.pow(y[i] - y[j], 2)), 2);

            }

        }

        return result;

    }

    private static double distance(double[][] matrix, int i, int j) {

        double result = 0;

        for (int m = 0; m < matrix.length; m++)

            result += Math.pow(matrix[m][i] - matrix[m][j], 2);

        return Math.sqrt(result);

    }

    public static double[][] randomPivotMatrix(double[][] matrix, int k) {

        int n = matrix[0].length;

        double[][] result = new double[k][n];

        boolean[] isPivot = new boolean[n];

        int pivot = 0;

        for (int i = 0; i < k; i++) {

            do {

                pivot = (int)(Math.random() * n);

            } while (isPivot[pivot]);

            isPivot[pivot] = true;

            for (int j = 0; j < n; j++) {

                result[i][j] = distance(matrix, pivot, j);

            }

        }

        return result;

    }

    public static double[][] maxminPivotMatrix(double[][] matrix, int k) {

        int n = matrix[0].length;

        double[][] result = new double[k][n];

        int pivot = 0;

        double[] min = new double[n];

        for (int i = 0; i < n; i++)
            min[i] = Double.MAX_VALUE;

        for (int i = 0; i < k; i++) {

            for (int j = 0; j < n; j++) {

                result[i][j] = distance(matrix, pivot, j);

            }

            pivot = 0;

            for (int j = 0; j < n; j++) {

                min[j] = Math.min(min[j], result[i][j]);

                if (min[j] > min[pivot])
                    pivot = j;

            }

        }

        return result;

    }

    public static double[][] distanceMatrix(double[][] matrix) {

        int n = matrix[0].length;

        double[][] result = new double[n][n];

        for (int i = 0; i < n; i++) {

            for (int j = 0; j < n; j++) {

                result[i][j] = distance(matrix, i, j);

            }

        }

        return result;

    }

    public static void squareDoubleCenter(double[][] matrix) {

        squareEntries(matrix);

        doubleCenter(matrix);

    }

    public static void squareEntries(double[][] matrix) {

        int n = matrix[0].length;

        int k = matrix.length;

        for (int i = 0; i < k; i++)

            for (int j = 0; j < n; j++)

                matrix[i][j] = Math.pow(matrix[i][j], 2);

    }

    public static void randomize(double[][] matrix) {

        java.util.Random random = new java.util.Random();

        for (int i = 0; i < matrix.length; i++) {

            for (int j = 0; j < matrix[0].length; j++) {

                matrix[i][j] = random.nextDouble();

            }

        }

    }

    public static void pivotmds(double[][] input, double[][] result) {

        double[] evals = new double[result.length];

        svd(input, result, evals);

        for (int i = 0; i < result.length; i++) {

            for (int j = 0; j < result[0].length; j++) {

                result[i][j] *= Math.sqrt(evals[i]);

            }

        }

    }

    public static void svd(double[][] matrix, double[][] evecs, double[] evals) {

        int k = matrix.length;

        int n = matrix[0].length;

        int d = evecs.length;

        for (int m = 0; m < d; m++)
            evals[m] = normalize(evecs[m]);

        double[][] K = new double[k][k];

        // C^TC berechnen

        selfprod(matrix, K);

        double[][] temp = new double[d][k];

        // starting vectors for power iteration

        for (int m = 0; m < d; m++) {

            for (int i = 0; i < k; i++) {

                for (int j = 0; j < n; j++) {

                    temp[m][i] += matrix[i][j] * evecs[m][j];

                }

            }

        }

        for (int m = 0; m < d; m++)
            evals[m] = normalize(evecs[m]);

        eigen(K, temp, evals);

        double[][] tempOld = new double[d][k];

        for (int m = 0; m < d; m++)

            for (int i = 0; i < k; i++)

                for (int j = 0; j < k; j++)

                    tempOld[m][j] += K[i][j] * temp[m][i];

        for (int m = 0; m < d; m++)
            evals[m] = normalize(tempOld[m]);

        // final matrix multiplication

        for (int m = 0; m < d; m++) {

            evals[m] = Math.sqrt(evals[m]);

            for (int i = 0; i < n; i++) { // knoten i

                evecs[m][i] = 0;

                for (int j = 0; j < k; j++) { // pivot j

                    evecs[m][i] += matrix[j][i] * temp[m][j];

                }

            }

        }

        for (int m = 0; m < d; m++)
            normalize(evecs[m]);

    }

    private static double prod(double[] x, double[] y) {

        double result = 0;

        for (int i = 0; i < x.length; i++)
            result += x[i] * y[i];

        return result;

    }

    private static double normalize(double[] x) {

        double norm = Math.sqrt(prod(x, x));

        for (int i = 0; i < x.length; i++)
            x[i] /= norm;

        return norm;

    }

    private static void selfprod(final double[][] d, double[][] result) {

        int k = d.length;

        int n = d[0].length;

        for (int i = 0; i < k; i++) {

            for (int j = 0; j <= i; j++) {

                double sum = 0;

                for (int m = 0; m < n; m++)
                    sum += d[i][m] * d[j][m];

                result[i][j] = sum;

                result[j][i] = sum;

            }

        }

    }

    public static void eigen(double[][] matrix, double[][] evecs, double[] evals) {

        int d = evals.length;

        int k = matrix.length;

        final double eps = 0.0000000001;

        double r = 0;

        for (int m = 0; m < d; m++)
            evals[m] = normalize(evecs[m]);

        int iterations = 0;

        while (r < 1 - eps) {

            double[][] tempOld = new double[d][k];

            // remember old values

            for (int m = 0; m < d; m++) {

                for (int i = 0; i < k; i++) {

                    tempOld[m][i] = evecs[m][i];

                    evecs[m][i] = 0;

                }

            }

            // matrix multiplication

            for (int m = 0; m < d; m++)

                for (int i = 0; i < k; i++)

                    for (int j = 0; j < k; j++)

                        evecs[m][j] += matrix[i][j] * tempOld[m][i];

            // orthogonalisieren

            for (int m = 0; m < d; m++) {

                for (int p = 0; p < m; p++) {

                    double fac =
                            prod(evecs[p], evecs[m]) / prod(evecs[p], evecs[p]);

                    for (int i = 0; i < k; i++)
                        evecs[m][i] -= fac * evecs[p][i];

                }

            }

            // normalization

            for (int m = 0; m < d; m++)
                evals[m] = normalize(evecs[m]);

            r = 1;

            for (int m = 0; m < d; m++)
                r = Math.min(Math.abs(prod(evecs[m], tempOld[m])), r);

            iterations++;

        }

    }

    public static void doubleCenter(double[][] matrix) {

        int n = matrix[0].length;

        int k = matrix.length;

        // center rows

        for (int j = 0; j < k; j++) {

            double avg = 0;

            for (int i = 0; i < n; i++)
                avg += matrix[j][i];

            avg /= n;

            for (int i = 0; i < n; i++)
                matrix[j][i] -= avg;

        }

        // center columns

        for (int i = 0; i < n; i++) {

            double avg = 0;

            for (int j = 0; j < k; j++)
                avg += matrix[j][i];

            avg /= matrix.length;

            for (int j = 0; j < k; j++)
                matrix[j][i] -= avg;

        }

    }

    public static void fullmds(double[][] distances, double[][] result) {

        double[] evals = new double[result.length];

        ClassicalMDS.eigen(distances, result, evals);

        for (int i = 0; i < result.length; i++) {

            evals[i] = Math.sqrt(evals[i]);

            for (int j = 0; j < result[0].length; j++) {

                result[i][j] *= evals[i];

            }

        }

    }

    public static void multiply(double[][] matrix, double factor) {

        for (int i = 0; i < matrix.length; i++) {

            for (int j = 0; j < matrix[0].length; j++) {

                matrix[i][j] *= factor;

            }

        }

    }

    public static void main(String[] args) {

        /*
         * // klassische Skalierung, d=Distanzen, n=#Objekte
         * 
         * int n=10000;
         * 
         * double[][] y = new double[2][n];
         * 
         * double[][] B = new double[n][n];
         * 
         * for (int i = 0; i < n; i++)
         * 
         * for (int j = 0; j < n; j++)
         * 
         * B[i][j] = d[i][j];
         * 
         * MDS.squareEntries(B);
         * 
         * MDS.doubleCenter(B);
         * 
         * MDS.multiply(B, -.5);
         * 
         * 
         * 
         * MDS.randomize(y);
         * 
         * MDS.fullmds(B, y);
         * 
         */

        // This is your data matrix of high-dimensional points
        // #features x #objects
        // double[][] data={{0,0,1,1,1.5,1.5},{0,1,0,1,0,1}};
        // double[] point={0.5,0.5};
        double[][] data =
                {{0, 1, 0, 1, 0, 1, 0, 1}, {1, 1, 0, 0, 1, 1, 0, 0},
                        {1, 1, 1, 1, 0, 0, 0, 0}};

        double[] point = {0.5, 0.5, 0.5};

        // number of objects

        final int n = data[0].length;

        // number of output dimensions

        final int d = 2;

        // create (n x n) input matrix

        // double[][] pivotMatrix=MDS.maxminPivotMatrix(data, k);

        // double[][] pivotMatrix=MDS.randomPivotMatrix(data, k);

        double[][] dist = ClassicalMDS.distanceMatrix(data);

        ClassicalMDS.squareEntries(dist);

        ClassicalMDS.doubleCenter(dist);

        ClassicalMDS.multiply(dist, -.5);

        // create (d x n) result matrix, initialize with random nonzero stuff

        double[][] output = new double[d][n];

        ClassicalMDS.randomize(output);

        ClassicalMDS.fullmds(dist, output);

        double[] placement = place(output, data, point);

        System.out.println(placement[0] + "  " + placement[1]);

        // output output, e.g. for gnuplot

        for (int i = 0; i < n; i++)
            System.out.println(output[0][i] + "  " + output[1][i]);

    }

}
