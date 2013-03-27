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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;

/**
 * This class extends the general
 * {@link BasisFunctionLearnerRow} in order to
 * use radial basis function prototypes for training. This prototype keeps an
 * Gaussian functions is internal representation. This function is created
 * infinity which means cover the entry domain. During training the function is
 * shrunk if new conflicting instances are omitted. Therefore two parameters
 * have been introduced. One is <code>m_thetaMinus</code> which is used to
 * describe an upper bound of conflicting instances; and 
 * <code>m_thetaPlus</code>, to lower bound for non-conflicting instances.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RadialBasisFunctionLearnerRow extends BasisFunctionLearnerRow {
    
    /** The upper bound for conflicting instances. */
    private final double m_thetaMinus;
    
    private final double m_thetaMinusSqrtMinusLog;
    private final double m_thetaPlusSqrtMinusLog;

    /** The lower bound for non-conflicting instances. */
    private final double m_thetaPlus;

    /** Row used to predict unknown instances. */
    private final RadialBasisFunctionPredictorRow m_predRow;

    /**
     * Creates a new radial basisfunction using the center vector as the anchor
     * of the Gaussian function and also assigns class label for this prototype.
     * The Gaussian function is be initialized infinity covering the entire
     * domain.
     * 
     * @param key the key of this row
     * @param classInfo the class info assigned to this basisfunction
     * @param center the initial center vector
     * @param thetaMinus upper bound for conflicting instances
     * @param thetaPlus lower bound for non-conflicting instances
     * @param distance choice of the distance function between patterns.
     */
    protected RadialBasisFunctionLearnerRow(final RowKey key, 
            final DataCell classInfo, final DataRow center, 
            final double thetaMinus, final double thetaPlus, 
            final int distance) {
        super(key, center, classInfo);
        m_thetaMinus = thetaMinus;
        m_thetaMinusSqrtMinusLog = Math.sqrt(-Math.log(m_thetaMinus));
        assert (m_thetaMinus >= 0.0 && m_thetaMinus <= 1.0);
        m_thetaPlus = thetaPlus;
        m_thetaPlusSqrtMinusLog = Math.sqrt(-Math.log(m_thetaPlus));
        assert (m_thetaPlus >= 0.0 && m_thetaPlus <= 1.0);
        m_predRow = new RadialBasisFunctionPredictorRow(key, center,
                classInfo, m_thetaMinus, distance);
        addCovered(center, classInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RadialBasisFunctionPredictorRow getPredictorRow() {
        return m_predRow;
    }

    /**
     * Returns the missing double value for the given dimension.
     * @param col the column index.
     * @return The centroid value at the given dimension.
     */
    @Override
    public DoubleValue getMissingValue(final int col) {
        return (DoubleValue)getAnchor().getCell(col);
    }

    /**
     * Checks if the given row is covered by this basis function. If this basis
     * function has not been shrunk before, it will return <b>true</b>
     * immediately, otherwise it checks if the activation is greater theta
     * minus.
     * 
     * @param row the input row to check coverage for
     * @return <code>true</code> if the given row is covered other
     *         <code>false</code>
     * @throws NullPointerException if the given row is <code>null</code>
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public
    final boolean covers(final DataRow row) {
        if (m_predRow.isNotShrunk()) {
            return true;
        }
        return (m_predRow.getStdDev()
            >= m_predRow.computeDistance(row) / m_thetaPlusSqrtMinusLog);
    }

    /**
     * Checks if the given row is explained by this basisfunction. If this basis
     * function has not been shrunk before, it will return <b>true</b>
     * immediately, otherwise it checks if the activation is greater or equal
     * theta plus.
     * 
     * @param row the input row to check coverage for
     * @return <code>true</code> if the given row is explained other
     *         <code>false</code>
     * @throws NullPointerException if the given row is <code>null</code>
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public
    final boolean explains(final DataRow row) {
        if (m_predRow.isNotShrunk()) {
            return true;
        }
        return (computeActivation(row) >= m_thetaPlus);
    }

    /**
     * Compares this basis function with the other one by its standard deviation
     * if the number of covered pattern is equal otherwise use this
     * identification.
     * 
     * @param best the basisfunction with the highest coverage so far
     * @param row the row on which to coverage need to be compared
     * @return <code>true</code> if the coverage of <code>this</code> object
     *         is better than the of the other
     * @throws ClassCastException if the other cell is not a
     *             {@link RadialBasisFunctionLearnerRow}
     */
    @Override
    public
    final boolean compareCoverage(
            final BasisFunctionLearnerRow best, final DataRow row) {
        RadialBasisFunctionLearnerRow rbf 
            = (RadialBasisFunctionLearnerRow) best;
        return m_predRow.getStdDev() > rbf.m_predRow.getStdDev();
    }

    /**
     * Called if a new {@link BasisFunctionLearnerRow} has to be adjusted.
     * 
     * @param row conflicting pattern.
     * @return a value greater zero if a conflict has to be solved. The value
     *         indicates relative loss in coverage for this basisfunction.
     */
    @Override
    public
    final boolean getShrinkValue(final DataRow row) {
        return shrinkIt(row, false);
    }

    /**
     * Basis functions need to be adjusted if they conflict with other ones.
     * Therefore this shrink method computes the new standard deviation based on
     * the <code>m_thetaMinus</code>.
     * 
     * @param row the input row to shrink this basisfunction on
     * @return <code>true</code> if the standard deviation changed due to the
     *         method which happens when either this basisfunction has not be
     *         shrunk before or the new radius is smaller than the old one,
     *         other wise this function return <code>false</code>
     */
    @Override
    public
    final boolean shrink(final DataRow row) {
        return shrinkIt(row, true);
    }

    /**
     * If <code>shrinkIt</code> is true the shrink will be executed otherwise
     * the shrink value is only returned.
     * 
     * @param row the input pattern for shrinking
     * @return 0 if no shrink needed otherwise a value greater zero
     */
    private boolean shrinkIt(final DataRow row, final boolean shrinkIt) {
        // if std dev is max or new std dev less that current
        // compute distance between centroid and given row
        double dist = m_predRow.computeDistance(row);
        //if (m_predRow.isNotShrunk() || (newStdDev < m_predRow.getStdDev())) {
        if (m_predRow.isNotShrunk() 
                || m_predRow.getStdDev() > dist / m_thetaMinusSqrtMinusLog) {
            // remembers old standard deviation for shrink value
            double oldStdDev = m_predRow.getStdDev();
            // new std dev for theta minus
            double newStdDev = dist / m_thetaMinusSqrtMinusLog;
            if (shrinkIt) {
                // set current to new std dev for theta minus
                // std dev was affected, set
                m_predRow.shrinkIt(newStdDev);
            }
            return oldStdDev != newStdDev;
        } else {
            // otherwise no shrink performed
            return false;
        }
    }
    
    /**
     * Method is empty.
     */
    @Override
    public
    final void reset() {
        // empty
    }

    /**
     * Method is empty.
     * 
     * @param row Ignored.
     */
    @Override
    public
    final void cover(final DataRow row) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_predRow.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getFinalCell(final int index) {
        return super.getAnchor().getCell(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeActivation(final DataRow row) {
        return m_predRow.computeActivation(row);
    }
    
    /**
     * @return theta minus
     */
    public final double getThetaMinus() {
        return m_thetaMinus;
    }

    /**
     * @return theta plus
     */
    public final double getThetaPlus() {
        return m_thetaPlus;
    }
    
}
