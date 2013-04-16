/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * A PNN rule used to predict unknown data.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RadialBasisFunctionPredictorRow extends BasisFunctionPredictorRow {
    
    /** Center vector of this radial function. */
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

    /**
     * Creates a new predictor for PNN rules. 
     * @param key The id for this rule.
     * @param center The center vector.
     * @param classLabel The class label.
     * @param thetaMinus Theta minus.
     * @param distance Distance measurement.
     */
    protected RadialBasisFunctionPredictorRow(final RowKey key, 
            final DataRow center, final DataCell classLabel, 
            final double thetaMinus, final int distance) {
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

    /**
     * Creates a new predictor row based on the given model content.
     * @param pp Model content to read this rule from.
     * @throws InvalidSettingsException If properties can't be read.
     */
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
    
    /**
     * Computes the overlapping based on the standard deviation of both radial
     * basisfunctions.
     * 
     * @param symmetric if the result is proportional to both basis functions,
     *            and thus symmetric, or if it is proportional to the area of 
     *            the basis function on which the function is called.
     * @param bf the other radial basisfunction to compute the overlap with
     * @return <code>true</code> if both radial basisfunctions overlap
     */
    @Override
    public double overlap(final BasisFunctionPredictorRow bf,
            final boolean symmetric) {
        RadialBasisFunctionPredictorRow rbf = 
            (RadialBasisFunctionPredictorRow) bf;
        assert (m_center.length == rbf.m_center.length);
        double overlap = 1.0;
        for (int i = 0; i < m_center.length; i++) {
            if (Double.isNaN(m_center[i]) || Double.isNaN(rbf.m_center[i])) {
                continue;
            }
            double a = m_center[i];
            double b = rbf.m_center[i];
            double overlapping = overlapping(a - getStdDev(), a
                    + getStdDev(), b - rbf.getStdDev(), b
                    + rbf.getStdDev(), symmetric);
            if (overlapping == 0.0) {
                return 0.0;
            } else {
                overlap *= overlapping;
            }
        }
        return overlap;
    }

    /**
     * @return <code>true</code> If not yet shrunken.
     */
    final boolean isNotShrunk() {
        return m_notShrunk;
    }

    /**
     * @return The standard deviation of this radial basisfunction rule.
     */
    final double getStdDev() {
        return m_stdDev;
    }
    
    /**
     * Returns the standard deviation of this radial basisfunction.
     * 
     * @return the standard deviation
     */
    @Override
    public double computeSpread() {
        return getStdDev();
    }

    /**
     * Shrinks this rules standard deviation by the new value.
     * @param newStdDev The new value for the standard deviation.
     */
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
    @Override
    public final double computeDistance(final DataRow row) {
        Distance dist = BasisFunctionLearnerNodeModel.DISTANCES[m_distance];
        return dist.compute(m_center, row);
    }

    /**
     * Sum of the given activation plus the newly calculated one for the given
     * row.
     * 
     * @param row row to get activation
     * @param act activation
     * @return the sum of both activations; greater or equal to zero
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public final double compose(final DataRow row, final double act) {
        return act + (getNumCorrectCoveredPattern() * computeActivation(row));
    }

    /**
     * Calculates the current activation of this basis function given a input
     * row which is always between <code>0.0</code> and <code>1.0</code>
     * using the the hereinafter called distance function.
     * 
     * @param row the row to compute activation for
     * @return activation for the given input row
     */
    @Override
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
            double a = Math.exp(-(dist * dist / (m_stdDev * m_stdDev)));
            assert (a >= 0.0 && a <= 1.0) : "d=" + d + ",a=" + a + ",stddev="
                    + m_stdDev;
            return a;
        }
    }

    /**
     * {@inheritDoc}
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
     * Returns a string representation of this basisfunction and the super
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

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getNrUsedFeatures() {
        return m_center.length; 
    }
    
    /**
     * @return distance measure
     */
    public final int getDistance() {
        return m_distance;
    }
    
}
