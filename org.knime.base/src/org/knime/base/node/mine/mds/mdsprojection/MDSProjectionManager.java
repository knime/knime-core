/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   07.03.2008 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds.mdsprojection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.mds.DataPoint;
import org.knime.base.node.mine.mds.MDSManager;
import org.knime.base.node.mine.mds.distances.DistanceManager;
import org.knime.base.node.mine.mds.distances.DistanceManagerFactory;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

/**
 * The <code>MDSProjectionManager</code> handling the MDS algorithmic. 
 * Like the {@link MDSManager} for each row of the input data a lower 
 * dimensional representation is computed. The difference is that the points
 * of the input data are not adjusted to themselves but to a set of fixed
 * data points and their corresponding lower diensional representation.
 * The rearrangement is an iterative process running as
 * many epochs as specified. The learn rate, specifying the step size is 
 * reduced after each epoch, so that the process converges at the end.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSProjectionManager {

    /**
     * The default random seed.
     */
    public static final int DEFAULT_SEED = 1000000;

    /**
     * The minimum random seed.
     */
    public static final int MIN_SEED = 0;

    /**
     * The maximum random seed.
     */
    public static final int MAX_SEED = Integer.MAX_VALUE;

    protected double m_minDistThreshold = MDSManager.DEF_MINDIST_THRESHOLD; 
    
    protected Set<DataPoint> m_unmodifiablePoints = new HashSet<DataPoint>();    
    
    protected int m_dimension;

    protected DistanceManager m_distMan;

    protected DistanceManager m_euclideanDistMan;

    protected BufferedDataTable m_inData;

    protected Hashtable<RowKey, DataPoint> m_points;

    protected BufferedDataTable m_fixedDataPoints;
    
    protected Hashtable<RowKey, DataPoint> m_fixedPoints;

    protected double m_learningrate;

    protected double m_initialLearningrate;

    protected double m_epochs;

    protected double m_finalLearningRate = 0.001;

    protected boolean m_isInit = false;

    protected ExecutionContext m_exec;
    
    protected boolean m_projectOnly = true;

    /**
     * Creates a new instance of <code>MDSProjectionManager</code> with the 
     * given dimension, distance metric, fuzzy flag, in data and fixed data to 
     * use. If the dimension is less or equals zero, the fixedDataPoints is 
     * <code>null</code>, the low dimension of the fixed data is not equal the
     * specified dimension or the high dimension of the fixed data is not equal
     * to the dimension of the input data an 
     * <code>IllegalArgumentException</code> is thrown. The fixed data is used
     * to project the input data. First the in data is placed with respect to 
     * the fixed data and than it is moved by means of mds. 
     * 
     * @param dimension The output MDS dimension
     * @param distance The distance metric to use.
     * @param fuzzy <code>true</code> if the in data is fuzzy valued data.
     * @param inData The in data to use.
     * @param subProgMonitor The <code>ExecutionMonitor</code> to monitor the 
     * progress.
     * @param fixedDataPoints The fixed data points to project the in data at.
     * @param fixedDataMdsIndices Array, containing the indices of the
     * fixed mds data points according to the fixedDataPoints data table.
     * @throws IllegalArgumentException if the specified dimension is less or 
     * equals zero or dimension incompatibilities of in data and fixed data 
     * occur. 
     * @throws CanceledExecutionException If execution was canceled by the user.
     */
    public MDSProjectionManager(final int dimension, final String distance, 
            final boolean fuzzy, final BufferedDataTable inData,
            final BufferedDataTable fixedDataPoints, 
            final int[] fixedDataMdsIndices, 
            final ExecutionContext subProgMonitor) 
    throws IllegalArgumentException, CanceledExecutionException {
        if (dimension <= 0) {
            throw new IllegalArgumentException(
                    "Dimension must not be smaller than 1!");
        }
        if (fixedDataPoints == null) {
            throw new IllegalArgumentException(
                    "Fixed data points may not be null!");
        }
        if (fixedDataMdsIndices.length != dimension) {
            throw new IllegalArgumentException(
                    "Value of dimension and length of indices array "
                  + "specifying fixed mds data points must be equal!");
        }
        for (int i : fixedDataMdsIndices) {
            if (i < 0) {
                throw new IllegalArgumentException(
                        "All indices of indices array have to be greater "
                      + "than 0!");
            }
        }
        
        m_dimension = dimension;
        m_distMan = DistanceManagerFactory.createDistanceManager(distance, 
                fuzzy, true);
        
        m_euclideanDistMan = DistanceManagerFactory.createDistanceManager(
                DistanceManagerFactory.EUCLIDEAN_DIST, fuzzy);
        m_inData = inData;
        m_points = new Hashtable<RowKey, DataPoint>(m_inData.getRowCount());
        m_exec = subProgMonitor;
        
        // handle data table with fixed data
        m_fixedDataPoints = fixedDataPoints;
        m_fixedPoints = new Hashtable<RowKey, DataPoint>(
                m_fixedDataPoints.getRowCount());
        preprocFixedDataPoints(fixedDataMdsIndices);
        m_fixedDataPoints = m_exec.createBufferedDataTable(
                new FilterColumnTable(m_fixedDataPoints, false, 
                        fixedDataMdsIndices), m_exec);
    }
    
    protected void preprocFixedDataPoints(final int[] fixedDataMdsIndices) 
    throws CanceledExecutionException {
        m_exec.setMessage("Preprocessing fixed data points");
        
        // sort indices
        Arrays.sort(fixedDataMdsIndices);
        
        RowIterator it = m_fixedDataPoints.iterator();
        while (it.hasNext()) {
            m_exec.checkCanceled();
            DataRow row = it.next();
            
            DataPoint p = new DataPoint(m_dimension);
            for (int i = 0; i < m_dimension; i++) {
                DoubleValue d = 
                    (DoubleValue)row.getCell(fixedDataMdsIndices[i]);
                p.setElementAt(i, d.getDoubleValue());
            }
            m_fixedPoints.put(row.getKey(), p);
        }
    }
    
    /**
     * Initializes the lower dimensional data points randomly.
     * 
     * @param seed The random seed to use.
     * @throws CanceledExecutionException If execution was canceled by the user.
     */
    public void init(final long seed) throws CanceledExecutionException {
        m_isInit = true;
        Random rand = new Random(seed);

        ExecutionMonitor exec = m_exec.createSubProgress(0.1);

        // init all data points
        RowIterator it = m_inData.iterator();
        int currRow = 1;
        int maxRows = m_inData.getRowCount();
        while (it.hasNext()) {
            exec.checkCanceled();

            DataRow row = it.next();
            DataPoint p = new DataPoint(m_dimension);
            for (int j = 0; j < m_dimension; j++) {
                p.setElementAt(j, rand.nextDouble());
            }
            m_points.put(row.getKey(), p);

            double prog = (double)currRow / (double)maxRows;
            exec.setProgress(prog, "Initialising data points.");
            currRow++;
        }
    }

    /**
     * Does the training by adjusting the lower dimensional data points 
     * accordant to their distances and the distances of the original data.
     * 
     * @param epochs The number of epochs to train.
     * @param learningrate The learn rate, specifying the step size of 
     * adjustment. 
     * @throws CanceledExecutionException If execution was canceled by the user.
     */
    public void train(final int epochs, final double learningrate)
            throws CanceledExecutionException {
        if (!m_isInit) {
            init(DEFAULT_SEED);
        }

        ExecutionMonitor exec = m_exec.createSubProgress(0.9);

        m_learningrate = learningrate;
        m_initialLearningrate = learningrate;
        m_epochs = epochs;
        for (int e = 1; e <= epochs; e++) {
            exec.setMessage("Start training");
            exec.checkCanceled();
            doEpoch(e, exec);

            double prog = (double)e / (double)epochs;
            exec.setProgress(prog, "Training epoch " + e + " of " + epochs);
        }
    }

    protected void doEpoch(final int epoch, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        // through all data points
        RowIterator it1 = m_inData.iterator();
        while (it1.hasNext()) {
            exec.checkCanceled();

            DataRow r1 = it1.next();
            DataPoint p1 = m_points.get(r1.getKey());

            // first adjust point at the fixed points
            RowIterator fit = m_fixedDataPoints.iterator();
            while (fit.hasNext()) {
                DataRow fixedRow = fit.next();
                DataPoint p2 = m_fixedPoints.get(fixedRow.getKey());
                adjustDataPoint(p1, p2, r1, fixedRow);
            }
            
            // through all data points again
            if (!m_projectOnly) {
                RowIterator it2 = m_inData.iterator();
                while (it2.hasNext()) {
                    DataRow r2 = it2.next();
                    DataPoint p2 = m_points.get(r2.getKey());
                    adjustDataPoint(p1, p2, r1, r2);
                }
            }
        }

        adjustLearningRate(epoch);
    }
        
    protected void adjustDataPoint(final DataPoint p1, final DataPoint p2,
            final DataRow r1, final DataRow r2) {
        if (!p1.equals(p2) && !m_unmodifiablePoints.contains(p1)) {
            double disparity = disparityTransformation(
                    m_distMan.getDistance(r1, r2));
            
            //double distance = m_distMan.getDistance(p1, p2);
            // use only the Euclidean distance for low dimensional data.
            double distance = m_euclideanDistMan.getDistance(p1, p2);
            
            if (disparity <= m_minDistThreshold) {
                // if r1 is equal r2 (or nearly equal, set p1 equal p2
                for (int d = 0; d < m_dimension; d++) {
                    p1.setElementAt(d, p2.getElementAt(d));
                }
                m_unmodifiablePoints.add(p1);
            } else {
                // through all dimensions
                for (int d = 0; d < m_dimension; d++) {
                    double value = p1.getElementAt(d);
                    if (distance != 0) {
                        double delta = m_learningrate
                            * (1 - (disparity / distance)) 
                            * (p2.getElementAt(d) - value);
                            //* activation;
                        p1.setElementAt(d, value + delta);
                    }
                }
            }
        }
    }    
    
    protected double disparityTransformation(final double distance) {
        return distance;
    }

    protected void adjustLearningRate(final int epoch) {
        m_learningrate = m_initialLearningrate * Math.pow(
                (m_finalLearningRate / m_initialLearningrate),
                (double)epoch / (double)m_epochs);
    }

    /**
     * @return a <code>Hashtable</code> containing the <code>RowKey</code>s as
     * as keys and the corresponding lower dimensional <code>DataPoint</code>s
     * as values.
     */
    public Hashtable<RowKey, DataPoint> getDataPoints() {
        return m_points;
    }

    /**
     * Clears the <code>Hashtable</code> containing the high and the 
     * corresponding low dimensional data points.
     */
    public void reset() {
        m_points.clear();
        m_isInit = false;
    }

    /**
     * @return the dimension The dimension of the low dimensionl data points.
     */
    public int getDimension() {
        return m_dimension;
    }

    /**
     * @return the projectOnly
     */
    public boolean getProjectOnly() {
        return m_projectOnly;
    }

    /**
     * @param projectOnly the projectOnly to set
     */
    public void setProjectOnly(final boolean projectOnly) {
        m_projectOnly = projectOnly;
    }

    /**
     * @return the minDistThreshold
     */
    public double getMinDistanceThreshold() {
        return m_minDistThreshold;
    }

    /**
     * @param minDistThreshold the minDistThreshold to set
     */
    public void setMinDistanceThreshold(final double minDistThreshold) {
        m_minDistThreshold = minDistThreshold;
    }
}
