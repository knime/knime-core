/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
import org.knime.base.node.mine.mds.distances.RowDistanceManager;
import org.knime.base.node.preproc.filter.row.RowFilterTable;
import org.knime.base.node.preproc.filter.row.rowfilter.MissingCellRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
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
 * data points and their corresponding lower dimensional representation.
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

    /**
     * Threshold of minimum distance.
     */
    protected double m_minDistThreshold = MDSManager.DEF_MINDIST_THRESHOLD;

    /**
     * The set of unmodifyable data points.
     */
    protected Set<DataPoint> m_unmodifiablePoints = new HashSet<DataPoint>();

    /**
     * The dimension of the target space.
     */
    protected int m_dimension;

    /**
     * The distance manager to use.
     */
    protected RowDistanceManager m_distMan;

    /**
     * The Euclidean distance manager used in the target space.
     */
    protected DistanceManager m_euclideanDistMan;

    /**
     * The input data table.
     */
    protected DataTable m_inData;

    /**
     * A hashtable holding keys of input rows and related points of the target
     * space.
     */
    protected Hashtable<RowKey, DataPoint> m_points;

    /**
     * The input data table storing the fixed data points.
     */
    protected DataTable m_fixedDataPoints;

    /**
     * A hashtable holding row keys of fixed points and related points of
     * the target space.
     */
    protected Hashtable<RowKey, DataPoint> m_fixedPoints;

    /**
     * The learning rate.
     */
    protected double m_learningrate;

    /**
     * The initial learning rate.
     */
    protected double m_initialLearningrate;

    /**
     * The number of epochs to train.
     */
    protected double m_epochs;

    /**
     * The final learning rate.
     */
    protected double m_finalLearningRate = 0.001;

    /**
     * Flag, indicating if data points in target space have been initialized
     * (if <code>true</code>) or not (if <code>false</code>).
     */
    protected boolean m_isInit = false;

    /**
     * The execution context to show progress information an enable cancel.
     */
    protected ExecutionContext m_exec;

    /**
     * Flag, indicating if data points have to be projected only according to
     * the fixed points (if <code>true</code>) or adjusted according to the
     * other (not fixed) points as well (if <code>false</code>).
     */
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
     * @param exec The <code>ExecutionContext</code> to monitor the
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
            final ExecutionContext exec)
    throws IllegalArgumentException, CanceledExecutionException {
        this(dimension, DistanceManagerFactory.createDistanceManager(
                distance, fuzzy), fuzzy, inData, fixedDataPoints,
                fixedDataMdsIndices, exec);
    }

    /**
     * Creates a new instance of <code>MDSProjectionManager</code> with the
     * given dimension, distance metric (manager), fuzzy flag, in data and
     * fixed data to use. If the dimension is less or equals zero, the
     * fixedDataPoints or the distance manager is <code>null</code>, the low
     * dimension of the fixed data is not equal the specified dimension or
     * the high dimension of the fixed data is not equal to the dimension of
     * the input data an <code>IllegalArgumentException</code> is thrown. The
     * fixed data is used to project the input data. First the in data is placed
     * with respect to the fixed data and than it is moved by means of mds.
     *
     * @param dimension The output MDS dimension
     * @param distManager The distance metric (manager) to use.
     * @param fuzzy <code>true</code> if the in data is fuzzy valued data.
     * @param inData The in data to use.
     * @param exec The <code>ExecutionContext</code> to monitor the
     * progress.
     * @param fixedDataPoints The fixed data points to project the in data at.
     * @param fixedDataMdsIndices Array, containing the indices of the
     * fixed mds data points according to the fixedDataPoints data table.
     * @throws IllegalArgumentException if the specified dimension is less or
     * equals zero or dimension incompatibilities of in data and fixed data
     * occur.
     * @throws CanceledExecutionException If execution was canceled by the user.
     * @since 2.6
     */
    public MDSProjectionManager(final int dimension,
            final RowDistanceManager distManager, final boolean fuzzy,
            final BufferedDataTable inData,
            final BufferedDataTable fixedDataPoints,
            final int[] fixedDataMdsIndices, final ExecutionContext exec)
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
        if (distManager == null) {
            throw new IllegalArgumentException(
                    "Distance Manager may not be null!");
        }

        m_dimension = dimension;
        m_distMan = distManager;
        m_euclideanDistMan = DistanceManagerFactory.createDistanceManager(
                DistanceManagerFactory.EUCLIDEAN_DIST, fuzzy);
        m_exec = exec;

        // handle data table with fixed data
        m_fixedDataPoints = fixedDataPoints;
        m_fixedPoints = new Hashtable<RowKey, DataPoint>();
        preprocFixedDataPoints(fixedDataMdsIndices);
        m_fixedDataPoints = new FilterColumnTable(m_fixedDataPoints, false,
                        fixedDataMdsIndices);

        RowFilter rf = new MissingCellRowFilter();
        m_inData = new RowFilterTable(inData, rf);

        m_points = new Hashtable<RowKey, DataPoint>();
    }

    /**
     * Initializes for each of the fixed data points a point in the
     * target space. Which of the columns of the data table containing the
     * fixed points have to be considered (according to the non fixed points)
     * is specified by the given array of indices.
     *
     *
     * @param fixedDataMdsIndices The indices specifying the columns of
     * the data table containing the fixed data points, to consider.
     * @throws CanceledExecutionException If the process is canceled.
     */
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
                final DataCell cell = row.getCell(fixedDataMdsIndices[i]);
                if (!cell.isMissing()) {
                    final Double d = ((DoubleValue) cell).getDoubleValue();
                    p.setElementAt(i, d);
                }
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
        while (it.hasNext()) {
            exec.checkCanceled();

            DataRow row = it.next();
            DataPoint p = new DataPoint(m_dimension);
            for (int j = 0; j < m_dimension; j++) {
                p.setElementAt(j, rand.nextDouble());
            }
            m_points.put(row.getKey(), p);

            exec.setProgress("Initialising data points.");
        }
    }

    /**
     * Does the training by adjusting the lower dimensional data points
     * according to their distances and the distances of the original data.
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
        exec.setMessage("Start training");
        for (int e = 1; e <= epochs; e++) {
            exec.checkCanceled();
            doEpoch(e, exec);

            double prog = (double)e / (double)epochs;
            exec.setProgress(prog, "Training epoch " + e + " of " + epochs);
        }
    }

    /**
     * Computing one epoch if the iterative mds. In one epoch all points are
     * adjusted according to all fixed points and if <code>projectOnly</code>
     * is set <code>false</code> to all other points too.
     *
     * @param epoch The current epoch.
     * @param exec The execution monitor to show the progress and enable
     * canceling.
     * @throws CanceledExecutionException If the process was canceled.
     */
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

    /**
     * Adjusts the low dimensional mapping of the first data point according
     * to the second data point and its mapping.
     *
     * @param p1 The mapping of the first data point in the target space.
     * @param p2 The mapping of the second data point in the target space.
     * @param r1 The first data point in the original space.
     * @param r2 The second data point in the original space.
     */
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

    /**
     * Computes the disparity value for the given distance value.
     * @param distance The distance value to compute the disparity value for.
     * @return The disparity value according to the given distance value.
     */
    protected double disparityTransformation(final double distance) {
        return distance;
    }

    /**
     * Adjusts learning rate according to the given epoch. The learning rate
     * is decreased over time.
     *
     * @param epoch The epoch for which the learning rate has to be computed.
     * The higher the given epoch (according to the maximum epochs) the more
     * is the learning rate decreased.
     */
    protected void adjustLearningRate(final int epoch) {
        m_learningrate = m_initialLearningrate * Math.pow(
                (m_finalLearningRate / m_initialLearningrate),
                epoch / m_epochs);
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
