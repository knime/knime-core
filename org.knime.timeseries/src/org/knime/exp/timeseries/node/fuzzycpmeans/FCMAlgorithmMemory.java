/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.exp.timeseries.node.fuzzycpmeans;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;


public class FCMAlgorithmMemory extends FCCAlgorithm {
  
    /*
     * Data to be clustered
     */
    private double[][] m_data;
    
    /*
     * Rowkeys assigned to each position of the double data array
     */
    private RowKey[] m_keys;
   
    /**
     * Constructor for a Fuzzy c-means algorithm (with no noise detection).
     * 
     * @param nrClusters the number of cluster prototypes to use
     * @param fuzzifier allows the clusters to overlap
     */
    public FCMAlgorithmMemory(final int nrClusters, final double fuzzifier) {
        super(nrClusters, fuzzifier);
    }

    /**
     * Constructor for a Fuzzy c-means algorithm with noise detection. It can be
     * indicated, whether the delta value of the noise cluster should be updated
     * automatically or if it should be calculated automatically. The last
     * parameter specifies either the delta value or the lambda value, depending
     * on the boolean flag in the parameter before.
     * 
     * @param nrClusters the number of clusters to use
     * @param fuzzifier the fuzzifier, controls how much the clusters can
     *            overlap
     * @param calculateDelta indicate whether delta should be calculated
     *            automatically
     * @param deltalambda the delta value, if the previous parameter is
     *            <code>false</code>, the lambda value otherwise
     */
    public FCMAlgorithmMemory(final int nrClusters, final double fuzzifier,
            final boolean calculateDelta, final double deltalambda) {
        super(nrClusters, fuzzifier, calculateDelta, deltalambda);
    }

    /**
     * Inits the cluster centers and the weight matrix. Must be called before
     * the iterations are carried out.
     * 
     * @param nrRows number of rows in the DataTable
     * @param dimension the dimension of the table
     * @param table the table to use.
     */
    @Override
    public void init(final int nrRows, final int dimension,
            final DataTable table) {
       super.init(nrRows, dimension, table);
       initData(table);
       double[][] m_clusters = getClusterCentres();
       for (int c = 0; c < m_clusters.length; c++) {
           for (int i = 0; i < m_clusters[c].length; i++) {
               m_clusters[c][i] = m_data[c][i];
           }
       }
    }
    
    /**
     * Inits the cluster centers and the weight matrix. Must be called before
     * the iterations are carried out.
     * 
     * @param keys the RowKeys for each data row.
     * @param data the DaaTable as 2 dimensional double array.
     */
    public void init(final RowKey[] keys, final double[][] data) {
       assert (keys.length == data.length);
       super.init(data.length, data[0].length, null);
       m_data = data;
       m_keys = keys;
    }
    
    /*
     * Reads the data from the given DataTable in the doublearray m_data
     */
    private void initData(final DataTable table) {
        m_data = new double[getNrRows()][getDimension()];
        m_keys = new RowKey[getNrRows()];
        int curRow = 0;
        for (DataRow dRow : table) {
            m_keys[curRow] = dRow.getKey();
            for (int j = 0; j < dRow.getNumCells(); j++) {
                if (!(dRow.getCell(j).isMissing())) {
                    DoubleValue dv = (DoubleValue)dRow.getCell(j);
                    m_data[curRow][j] = dv.getDoubleValue();

                } else {
                    m_data[curRow][j] = 0;
                }
            }
            curRow++;
        }
    }
    
    /**
     * Please make sure to call init() first in order to guarantee that
     * the DataTable is converted.
     * 
     * @return the input DataTable converted as a double array. If it has not
     * been produced yet, null is returned.
     */
    public double[][] getConvertedData() {
        assert m_data != null : "Please initialize first";
        return m_data;
    }
    
    /**
     * @param table DataTable to convert.
     * @return two-dimensional double array.
     */
    public double[][] getConvertedData(final DataTable table) {
        init(table);
        return m_data;
    }
    
    /**
     * Please make sure to call init() first in order to guarantee that
     * the DataTable is converted.
     * 
     * @return the RowKeys assigned to each position of the produced
     * double array
     * @see #getConvertedData()
     */
    public RowKey[] getRowKeys() {
        assert m_keys != null : "Please initialize first";
        return m_keys;
    }
    
    /**
     * Does one iteration in the Fuzzy c-means algorithm. First, the weight
     * matrix is updated and then the cluster prototypes are recalculated.
     * 
     * @param exec execution context to cancel the execution
     * @return the total change in the cluster prototypes. Allows to decide
     *         whether the algorithm can be stopped.
     * @throws CanceledExecutionException if the operation is canceled
     */
    @Override
    public double doOneIteration(final ExecutionContext exec)
            throws CanceledExecutionException {
        assert (m_data != null);
        if (exec != null) {
            exec.checkCanceled();
        }
        updateWeightMatrix(m_data);
        setTotalChange(0.0);
        updateClusterCenters(m_data);
        return getTotalChange();
    }

    /*
     * The update method for the weight matrix
     */
    private void updateWeightMatrix(final double[][] data) {
       int nrRows = getNrRows();
       double[][] clusters = getClusters().clone();
       boolean noise = isNoise();
       double fuzzifier = getFuzzifier();
       double delta = getDelta();
        for (int currentRow = 0; currentRow < nrRows; currentRow++) {
            double[] row = data[currentRow];

            /*
             * The weight of a data point is 1 if it is exactly on the position
             * of the cluster, in this case 0 for the others
             */
            double epsilon = 1E-8;
                // calculate the fuzzy membership to each cluster
                for (int j = 0; j < clusters.length; j++) {
                    // for each cluster
                    double distNumerator = 0;
                    if (noise && j == clusters.length - 1) {
                        distNumerator = Math.pow(delta, 2.0);
                    } else {
                        distNumerator = getDistance(clusters[j], row, j, currentRow)+epsilon;
                    }
                    double sum = 0;
                    for (int k = 0; k < clusters.length; k++) {
                        double distance = 0;
                        if (noise && k == clusters.length - 1) {
                            distance = Math.pow(delta, 2.0);
                        } else {
                            distance = getDistance(clusters[k], row, k, currentRow)+epsilon;
                        }
                        sum += Math.pow((distNumerator / distance),
                                (1.0 / (fuzzifier - 1.0)));
                    }
                    setWeightMatrixValue(currentRow, j, (1 / sum));
                    //System.out.print(weightMatrix[currentRow][j]+" ");
                }
                //System.out.println();
        }
        //System.out.println();
    }

    /*
     * Helper method for the quadratic distance between two double-arrays.
     * 
     */
    private double getDistance(final double[] vector1, final double[] vector2,int j,int i) {
    	DFFT.calcDistance(vector1, vector2);
    	//System.out.println("i="+i+" j="+j+" o="+DFFT.getOffset()+" d="+DFFT.getDistValue());
        return DFFT.getDistValue();
    }

    /*
     * The update method for the cluster centers
     */
    private void updateClusterCenters(final double[][] data) {
        int dimension = getDimension();
        int nrRows = getNrRows();
        int nrClusters = getNrClusters();
        double[][] clusters = getClusters().clone();
        for (int i=0;i<clusters.length;++i) clusters[i]=clusters[i].clone();
        double[][] weightMatrix = getweightMatrix().clone();
        double fuzzifier = getFuzzifier();
        boolean noise = isNoise();
        double[] sumNumerator = new double[3*dimension];
        double[] sumDenominator = new double[3*dimension];
        double sumupdate = 0;
        // for each cluster center
        for (int c = 0; c < nrClusters; c++) {
            if (noise) {
                // stop updating at noise cluster position.
                if (c == nrClusters - 1) {
                    break;
                }
            }
            for (int j = 0; j < sumNumerator.length; j++) {
                sumNumerator[j] = 0;
                sumDenominator[j] = 0;
            }

            for (int currentRow = 0; currentRow < nrRows; currentRow++) {
                double[] row = data[currentRow];
                getDistance(clusters[c], row, c, currentRow);
				final int o = -DFFT.getOffset();
				
                // for all attributes in X
				for (int i=0;i<dimension;++i) {
					sumNumerator[dimension+i+o] += Math.pow(weightMatrix[currentRow][c],fuzzifier)*row[i];
					sumDenominator[dimension+i+o] += Math.pow(weightMatrix[currentRow][c], fuzzifier);
				}				

                if (noise && isCalculateDelta()) {
                    sumupdate += getDistance(clusters[c], row, c, currentRow);
                }
            } // end while for all datarows sum up

			// finde die beste Position fuer die �bernahme als Prototyp
			double wsum = 0;
			for (int i=0;i<dimension;++i) wsum+=sumDenominator[i];
			double maxsum = wsum;
			int maxpos = 0;
			for (int i=1;i<dimension*2;++i) {
				wsum += sumDenominator[dimension+i-1]-sumDenominator[i-1];
				if (wsum>maxsum) { maxsum = wsum; maxpos = i; }
			}
			
			// �bernahme           
			double t = 0;
           for (int j = 0; j < dimension; j++) {
             double newValue = sumNumerator[maxpos+j]/sumDenominator[maxpos+j];
             t+=newValue*newValue;
           }
           t=Math.sqrt(t);
           for (int j = 0; j < dimension; j++) {
               double newValue = (sumNumerator[maxpos+j]/sumDenominator[maxpos+j])/t;
               addTotalChange(Math.abs(clusters[c][j] - newValue));
               setClusterValue(c, j, newValue);
               //System.out.print((int)(newValue*100)+" ");
           }
           //System.out.println();
        }

        /*
         * Update the delta-value automatically if choosen.
         */
        if (noise && isCalculateDelta()) {
            setDelta(Math.sqrt(getLambda()
                    * (sumupdate / (nrRows * (clusters.length - 1)))));
        }

    } // end update cluster centers

    
    
}
