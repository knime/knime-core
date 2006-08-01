/* 
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
 *   29.06.2006 (gabriel): created
 */
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeModel;
import org.knime.base.node.mine.bfn.BasisFunctionPredictorRow;
import org.knime.base.node.mine.bfn.Distance;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

public class RadialBasisFunctionPredictorRow extends BasisFunctionPredictorRow {
    private final double[] m_center;

    /** Standard deviation of theta_minus to the center vector. */
    private double m_stdDev;

    /**
     * The choice of distance function which is used to measure the distance
     * between pattern.
     */
    private final int m_distance;

    /** Max radius is true if radius was not adapted so far otherwise false. */
    private boolean m_notShrunk;

    RadialBasisFunctionPredictorRow(final DataCell key, final DataRow center,
            final DataCell classLabel, final double thetaMinus,
            final int distance) {
        super(key, classLabel, thetaMinus);
        m_distance = distance;
        m_center = new double[center.getNumCells()];
        for (int i = 0; i < m_center.length; i++) {
            DataCell cell = center.getCell(i);
            if (cell.isMissing()) {
                m_center[i] = Double.NaN;
            } else {
                m_center[i] = ((DoubleValue)cell).getDoubleValue();
            }
        }
        m_notShrunk = true; // infinite radius covering everything
        m_stdDev = Double.MAX_VALUE; // set max radius
    }

    RadialBasisFunctionPredictorRow(final ModelContentRO pp)
            throws InvalidSettingsException {
        super(pp);
        m_center = pp.getDoubleArray("center");
        m_distance = pp.getInt("distance");
        m_notShrunk = pp.getBoolean("not_shrunk");
        if (!m_notShrunk) {
            m_stdDev = pp.getDouble("standard_deviation");
        } else {
            m_stdDev = Double.MAX_VALUE;
        }
    }

    final boolean isNotShrunk() {
        return m_notShrunk;
    }

    final double getStdDev() {
        return m_stdDev;
    }

    final void shrinkIt(final double newStdDev) {
        // set current to new std dev for theta minus
        m_stdDev = newStdDev;
        // std dev was affected, set
        m_notShrunk = false;
    }

    /**
     * Computes the distance between this prototype's center vector and the
     * given row.
     * 
     * @param row the row to compute distance to
     * @return the distance between prototype and given row
     */
    final double computeDistance(final DataRow row) {
        Distance dist = BasisFunctionLearnerNodeModel.DISTANCES[m_distance];
        return dist.compute(m_center, row);
    }

    /**
     * Sum of the given activation plus then newly calculated one for the given
     * row.
     * 
     * @param row the new activation computed on
     * @param act the activation to combine with
     * @return the sum of both activations; greater or equal to zero
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public final double compose(final DataRow row, final double act) {
        assert (row != null);
        assert (act >= 0.0);
        // compute current activation of input row
        // TODO incorporate weights
        return act + computeActivation(row);
    }

    /**
     * Calculates the current activation of this basis function given a input
     * row which is always between <code>0.0</code> and <code>1.0</code>
     * using the the hereinafter called distance function.
     * 
     * @param row the row to compute activation for
     * @return activation for the given input row
     */
    public final double computeActivation(final DataRow row) {
        // if this basisfunction has not been shrunk yet
        if (m_notShrunk) {
            // activation is maximum
            return 1.0;
        } else {
            double dist = computeDistance(row);
            if (m_stdDev == 0.0) {
                return (dist == 0.0 ? 1.0 : 0.0);
            }
            double d = dist / m_stdDev;
            double a = Math.exp(-(d * d));
            assert (a >= 0.0 && a <= 1.0) : "d=" + d + ",a=" + a + ",stddev="
                    + m_stdDev;
            // return activation
            return a;
        }
    }

    /**
     * @see de.unikn.knime.dev.node.mine.bfn.BasisFunctionPredictorRow
     *      #save(org.knime.core.node.ModelContentWO)
     */
    @Override
    public void save(final ModelContentWO pp) {
        super.save(pp);
        pp.addDoubleArray("center", m_center);
        pp.addInt("distance", m_distance);
        pp.addBoolean("not_shrunk", m_notShrunk);
        if (!m_notShrunk) {
            pp.addDouble("standard_deviation", m_stdDev);
        }
    }

    /**
     * Returns a string represenation of this basisfunction and the super
     * implementation. Here, the method adds properties such as the center
     * vector and the standard deviation.
     * 
     * @return a string representation for this radial basisfunction cell
     *         including the initial center vector and the radius of standard
     *         deviation
     */
    @Override
    public final String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("center=[");
        for (int c = 0; c < m_center.length; c++) {
            if (c > 0) {
                buf.append(",");
            }
            buf.append("" + m_center[c]);
        }
        buf.append("],");
        buf.append("stddev=");
        buf.append(m_notShrunk ? "Infinity" : "" + m_stdDev);
        buf.append(" -> " + super.toString());
        return buf.toString();
    }
}
