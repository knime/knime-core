/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Apr 19, 2007 (cebron): created
 */
package org.knime.exp.timeseries.node.fuzzycpmeans;

import org.knime.base.node.mine.bfn.Distance;
import org.knime.base.util.math.MathUtils;

/**
 * Utility class to compute several cluster quality measures based on
 * a Fuzzy c-means clustering.
 * 
 * @author cebron, University of Konstanz
 */
public class FCMQualityMeasures {

    private double[][] m_clustercenters;
    
    private double[][] m_memberships;
    
    private double[][] m_data;
    
    private double m_fuzzifier;
    
    private double m_iDF;
    
    /*
     * Sum of all within-cluster variations
     */
    private double m_allsum = 0;
    
    /**
     * Constructor.
     * Quality measures are calculated directly on double arrays of cluster 
     * prototypes and membership matrix.
     * @param clustercenters the clustercenters as 2-dimensional double-array.
     * @param memberships the membership matrix as 2-dimensional double-array.
     * @param data the dataset as 2-dimensional double-array.
     * @param fuzzifier that has been used in the FCM clustering.
     */
    public FCMQualityMeasures(final double[][] clustercenters,
            final double[][] memberships,
            final double[][] data,
            final double fuzzifier) {
        m_clustercenters = clustercenters;
        m_memberships = memberships;
        m_data = data;
        m_fuzzifier = fuzzifier;
    }
    
    /**
     * The partition coefficient is 1 for non-fuzzy cluster partition.
     * The smallest value is 1/Number of clusters, if every datapoint
     * is assigned to every cluster.
     * @return partition coefficient of the clustering.
     */
    public double getPartitionCoefficient() {
        double sum = 0;
        for (int c = 0; c < m_clustercenters.length; c++) {
            for (int n = 0; n < m_memberships.length; n++) {
                sum += m_memberships[n][c] * m_memberships[n][c];
            }
        }
        sum /= m_memberships.length;
        return sum;
    }
    
    /**
     * Partition entropy (should be maximized).
     * @return partition entropy of the clustering.
     */
    public double getPartitionEntropy() {
        double sum = 0;
        for (int c = 0; c < m_clustercenters.length; c++) {
            for (int n = 0; n < m_memberships.length; n++) {
                sum += m_memberships[n][c] * Math.log(m_memberships[n][c]);
            }
        }
        sum /= m_memberships.length;
        return sum;
    }
    
    /**
     * The Xie-Beni index, also called the compactness and separation 
     * validity function, is an index that involves the membership values
     * and the dataset.
     * @return the Xie-Beni index.
     */
    public double getXieBeniIndex() {
        Distance dist = Distance.getInstance();
        // numerator
        double numerator = 0;
        for (int c = 0; c < m_clustercenters.length; c++) {
            for (int n = 0; n < m_data.length; n++) {
                numerator +=  m_memberships[n][c]
                              * dist.compute(m_data[n], m_clustercenters[c]);
            }
        }
        double denominator = 0;
        double mindistance = Double.MAX_VALUE;
        for (int c = 0; c < m_clustercenters.length; c++) {
            for (int c2 = c + 1; c2 < m_clustercenters.length; c2++) {
                mindistance =
                        Math.min(mindistance, dist.compute(m_clustercenters[c],
                                m_clustercenters[c2]));
            }
        }
        denominator = mindistance * m_data.length;
        return numerator / denominator;
    }
    
    /**
     * Computes the fuzzy  cobariance matrix of a cluster.
     * @param cluster the cluster index.
     * @return covariance matrix.
     */
    public double[][] computeFuzzyCovarianceMatrix(final int cluster) {
        double summemberships = 0;
        int nrFeatures = m_data[0].length;
        double[][] summatrix = new double[nrFeatures][nrFeatures];
        for (int row = 0; row < summatrix.length; row++) {
            for (int col = 0; col < summatrix[0].length; col++) {
                summatrix[row][col] = 0;
            }
        }
        for (int j = 1; j < m_data.length; j++) {
            double[] vector = minus(m_data[j], m_clustercenters[cluster]);
            double[][] matrix = new double[nrFeatures][nrFeatures];
            for (int a = 0; a < nrFeatures; a++) {
                for (int b = 0; b < nrFeatures; b++) {
                    matrix[a][b] = vector[a] * vector[b];
                    matrix[a][b] *=
                            Math.pow(m_memberships[j][cluster], m_fuzzifier);
                    summemberships +=
                            Math.pow(m_memberships[j][cluster], m_fuzzifier);
                }
            }
            summatrix = MathUtils.add(summatrix, matrix);
        }
        for (int row = 0; row < summatrix.length; row++) {
            for (int col = 0; col < summatrix[0].length; col++) {
                summatrix[row][col] /= summemberships;
            }
        }
        return summatrix;
    }
    
    /**
     * Calculates the Within-Cluster Variation for each cluster. We take 'crisp'
     * cluster centers to determine the membership from a datarow to a cluster
     * center.
     * 
     * @return withinClusterVariations
     */
    public double[] getWithinClusterVariations() {

        // calculate within-cluster cariation for each cluster center
        double[] sum = new double[m_clustercenters.length];
        int[] nrRows = new int[m_clustercenters.length];

        int winner = -1;
        double[] dRow;
        int i = 0;
        Distance dist = Distance.getInstance();
        while (i < m_data.length) {
            dRow = m_data[i];
            winner = getWinner(m_memberships[i]);
            sum[winner] += dist.compute(m_clustercenters[winner], dRow);
            nrRows[winner]++;
            i++;
        }

        m_allsum = 0;
        double[] withinclustervariations = new double[m_clustercenters.length];
        for (int c = 0; c < m_clustercenters.length; c++) {
            double clustervariation = sum[c] / nrRows[c];
            withinclustervariations[c] = clustervariation;
            m_allsum += clustervariation;
        }
        m_allsum = (m_allsum / m_clustercenters.length);
        return withinclustervariations;
    }
    
    /**
     * Calculates the Between-Cluster Variation.
     * 
     * @return the between cluster variation.
     */
    public double getBetweenClusterVariation() {
        double sum = 0;
        for (int j = 0; j < m_clustercenters.length; j++) {
            for (int i = 0; i < m_clustercenters.length; i++) {
                sum +=
                        clusterdistance(m_clustercenters[i],
                                m_clustercenters[j]);
            }
        }
        sum = sum / (m_clustercenters.length * m_clustercenters.length);
        return (sum / m_allsum);
    }

    /*
     * Helper method to calculate the distance between two cluster centers
     */
    private double clusterdistance(final double[] cluster1,
            final double[] cluster2) {
        double distance = 0;
        for (int i = 0; i < cluster1.length; i++) {
            double d = cluster1[i] - cluster2[i];
            distance += d * d;
        }
        return distance;
    }
    
    /**
     * Helper method to determine the winner cluster center (The cluster center
     * to which the DataRow has the highest membership value).
     * @param weights the weights.
     * @return winner index.
     */
    private final int getWinner(final double[] weights) {
        int max = -1;
        double maxvalue = -1;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] > maxvalue) {
                maxvalue = weights[i];
                max = i;
            }
        }
        return max;
    }
    
    /**
     * Computes the Fuzzy HyperVolume for a given cluster.
     * @param c the cluster index to use.
     * @return Fuzzy Hypervolume of cluster c.
     */
    public double getFuzzyHyperVolume(final int c) {
        double[][] covarianceMatrix = computeFuzzyCovarianceMatrix(c);
        return Math.sqrt(determinant(covarianceMatrix));
    }
    
    /*
     * Helper method for subtraction of two vectors.
     */
    private double[] minus(final double[] vec1, final double[] vec2) {
        assert (vec1.length == vec2.length);
        int nrElements = vec1.length;
        double[] result = new double[nrElements];
        for (int i = 0; i < nrElements; i++) {
            result[i] = vec1[i] - vec2[i];
        }
        return result;
    }
    
    
    /*
     * Transforms the matrix into a triangle matrix, containing zero's above the
     * diagonal.
     */
    private double[][] upperTriangle(final double[][] matrix) {
        double f1 = 0;
        double temp = 0;
        int nrCols = matrix.length;
        int v = 1;
        double[][] trianglematrix = matrix;

        m_iDF = 1;

        for (int col = 0; col < nrCols - 1; col++) {
            for (int row = col + 1; row < nrCols; row++) {
                v = 1;
                // check for 0 in diagonal
                while (trianglematrix[col][col] == 0) {
                    // if so, switch until not
                    if (col + v >= nrCols) {
                        // check if all rows have been switched
                        m_iDF = 0;
                        break;
                    } else {
                        for (int c = 0; c < nrCols; c++) {
                            // switch rows
                            temp = trianglematrix[col][c];
                            trianglematrix[col][c] = trianglematrix[col + v][c];
                            trianglematrix[col + v][c] = temp;
                        }
                        v++; // count row switchs
                        m_iDF = m_iDF * -1; // each switch changes determinant
                        // factor
                    }
                }

                if (trianglematrix[col][col] != 0) {
                    f1 =
                            (-1) * trianglematrix[row][col]
                                    / trianglematrix[col][col];
                    for (int i = col; i < nrCols; i++) {
                        trianglematrix[row][i] =
                                f1 * trianglematrix[col][i]
                                        + trianglematrix[row][i];
                    }
                }
            }
        }
        return trianglematrix;
    }

    
    /*
     * Gauss-algorithm for computing a determinant by transforming the matrix
     * into a triangle matrix and computing the product of the diagonal.
     */
    private double determinant(final double[][] matrix) {
       
        int diagonallength = matrix.length;
        double det = 1;
        double[][] trianglematrix = upperTriangle(matrix);
        // multiply down diagonal
        for (int i = 0; i < diagonallength; i++) {
            det = det * trianglematrix[i][i];
        } 
        det = det * m_iDF; // adjust w/ determinant factor
        return det;
    }
}
