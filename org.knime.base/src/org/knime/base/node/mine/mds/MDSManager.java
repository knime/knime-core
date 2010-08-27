/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
package org.knime.base.node.mine.mds;

import java.util.Hashtable;
import java.util.Random;

import org.knime.base.node.mine.mds.distances.DistanceManager;
import org.knime.base.node.mine.mds.distances.DistanceManagerFactory;
import org.knime.base.node.preproc.filter.row.RowFilterTable;
import org.knime.base.node.preproc.filter.row.rowfilter.MissingCellRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

/**
 * The <code>MDSManager</code> handling the MDS algorithmic. For each row
 * of the given <code>DataTable</code> a <code>DataPoint</code> with the
 * specified dimension is created, representing the higher dimensional row.
 * The <code>DataPoint</code>s are rearranged in a way that their distances
 * to each other approximately match the distances of the corresponding high
 * dimensional points. The rearrangement is an iterative process running as
 * many epochs as specified. The learn rate, specifying the step size is
 * reduced after each epoch, so that the process converges at the end.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSManager {

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
     * The default value of the minimum distance threshold.
     */
    public static final double DEF_MINDIST_THRESHOLD = 0.0001;

    private int m_dimension;

    private DistanceManager m_distMan;

    private DistanceManager m_euclideanDistMan;

    private DataTable m_inData;

    private Hashtable<RowKey, DataPoint> m_points;

    private double m_learningrate;

    private double m_initialLearningrate;

    private double m_epochs;

    private double m_finalLearningRate = 0.001;

    private boolean m_isInit = false;

    private ExecutionMonitor m_exec;

    /**
     * Creates a new instance of <code>MDSManager</code> with the given
     * dimension, distance metric, fuzzy flag and in data to use. If the
     * dimension is less or equals zero a <code>IllegalArgumentException</code>
     * is thrown.
     *
     * @param dimension The output MDS dimension
     * @param distance The distance metric to use.
     * @param fuzzy <code>true</code> if the in data is fuzzy valued data.
     * @param inData The in data to use.
     * @param exec The <code>ExecutionContext</code> to monitor the
     * progress.
     * @throws IllegalArgumentException if the specified dimension is less or
     * equals zero.
     */
    public MDSManager(final int dimension, final String distance,
            final boolean fuzzy, final BufferedDataTable inData,
            final ExecutionContext exec)
    throws IllegalArgumentException {
        if (dimension <= 0) {
            throw new IllegalArgumentException(
                    "Dimension must not be smaller than 1!");
        }
        m_dimension = dimension;
        m_distMan = DistanceManagerFactory.createDistanceManager(distance,
                fuzzy);
        m_euclideanDistMan = DistanceManagerFactory.createDistanceManager(
                DistanceManagerFactory.EUCLIDEAN_DIST, fuzzy);


        RowFilter rf = new MissingCellRowFilter();
        m_inData = new RowFilterTable(inData, rf);
        m_exec = exec.createSubExecutionContext(0.9);

        m_points = new Hashtable<RowKey, DataPoint>();
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

    private void doEpoch(final int epoch, final ExecutionMonitor exec)
    throws CanceledExecutionException {
        // through all data points
        RowIterator it1 = m_inData.iterator();
        while (it1.hasNext()) {
            DataRow r1 = it1.next();
            DataPoint p1 = m_points.get(r1.getKey());

            // through all data points again
            RowIterator it2 = m_inData.iterator();
            while (it2.hasNext()) {
                exec.checkCanceled();

                DataRow r2 = it2.next();
                DataPoint p2 = m_points.get(r2.getKey());

                adjustDataPoint(p1, p2, r1, r2);
            }
        }

        adjustLearningRate(epoch);
    }

    private void adjustDataPoint(final DataPoint p1, final DataPoint p2,
            final DataRow r1, final DataRow r2) {
        if (!p1.equals(p2)) {
            double disparity =
                    disparityTransformation(m_distMan.getDistance(r1, r2));

            // double distance = m_distMan.getDistance(p1, p2);
            // use only the Euclidean distance for low
            // dimensional data.
            double distance = m_euclideanDistMan.getDistance(p1, p2);

            // through all dimensions
            for (int d = 0; d < m_dimension; d++) {
                double value = p1.getElementAt(d);
                if (distance != 0) {
                    double delta =
                            m_learningrate * (1 - (disparity / distance))
                                    * (p2.getElementAt(d) - value);
                    p1.setElementAt(d, value + delta);
                }
            }
        }
    }


    private double disparityTransformation(final double distance) {
        return distance;
    }

    private void adjustLearningRate(final int epoch) {
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
}
