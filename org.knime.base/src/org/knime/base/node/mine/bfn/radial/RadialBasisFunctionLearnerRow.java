/*
 * --------------------------------------------------------------------- *
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.base.node.mine.bfn.BasisFunctionPredictorRow;
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
 * shrinked if new conflicting instances are obmitted. Therefore two parameters
 * have been introduced. One is <code>m_thetaMinus</code> which is used to
 * describe an upper bound of conflicting instances; and 
 * <code>m_thetaPlus</code>, to lower bound for non-conflicting instances.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class RadialBasisFunctionLearnerRow extends BasisFunctionLearnerRow {
    
    /** The upper bound for conflicting instances. */
    private final double m_thetaMinus;

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
     * @param numPat The overall number of pattern used for training. 
     * @param isHierarchical true if the rule is hierarchical, false otherwise.
     */
    RadialBasisFunctionLearnerRow(final RowKey key, final DataCell classInfo,
            final DataRow center, final double thetaMinus,
            final double thetaPlus, final int distance,
            final int numPat,
            final boolean isHierarchical) {
        super(key, center, classInfo, isHierarchical);
        m_thetaMinus = thetaMinus;
        assert (m_thetaMinus >= 0.0 && m_thetaMinus <= 1.0);
        m_thetaPlus = thetaPlus;
        assert (m_thetaPlus >= 0.0 && m_thetaPlus <= 1.0);
        m_predRow = new RadialBasisFunctionPredictorRow(key.getId(), center,
                classInfo, m_thetaMinus, distance, numPat);
        m_predRow.addCovered(center.getKey().getId(), classInfo);
    }

    /**
     * @see BasisFunctionLearnerRow#getPredictorRow()
     */
    @Override
    public BasisFunctionPredictorRow getPredictorRow() {
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
    protected final boolean covers(final DataRow row) {
        if (m_predRow.isNotShrunk()) {
            return true;
        }
        double act = computeActivation(row);
        return (act >= m_thetaPlus);
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
    protected final boolean explains(final DataRow row) {
        if (m_predRow.isNotShrunk()) {
            return true;
        }
        return (computeActivation(row) >= m_thetaPlus);
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
    public double overlap(final BasisFunctionLearnerRow bf,
            final boolean symmetric) {
        assert (bf instanceof RadialBasisFunctionLearnerRow);
        RadialBasisFunctionLearnerRow rbf = (RadialBasisFunctionLearnerRow)bf;
        assert (this.getAnchor().getNumCells() 
                == rbf.getAnchor().getNumCells());
        double overlap = 1.0;
        for (int i = 0; i < this.getAnchor().getNumCells(); i++) {
            double a = ((DoubleValue)this.getAnchor().getCell(i))
                    .getDoubleValue();
            double b = ((DoubleValue)rbf.getAnchor().getCell(i))
                    .getDoubleValue();
            double overlapping = overlapping(a - m_predRow.getStdDev(), a
                    + m_predRow.getStdDev(), b - rbf.m_predRow.getStdDev(), b
                    + rbf.m_predRow.getStdDev(), symmetric);
            if (overlapping == 0.0) {
                return 0.0;
            } else {
                overlap *= overlapping;
            }
        }
        return overlap;
    }

    /**
     * Returns the standard deviation of this radial basisfunction.
     * 
     * @return the standard deviation
     */
    @Override
    public double computeCoverage() {
        return m_predRow.getStdDev();
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
    protected final boolean compareCoverage(
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
    protected final double getShrinkValue(final DataRow row) {
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
    protected final boolean shrink(final DataRow row) {
        return shrinkIt(row, true) > 0.0;
    }

    /**
     * If <code>shrinkIt</code> is true the shrink will be executed otherwise
     * the shrink value is only returned.
     * 
     * @param row the input pattern for shrinking
     * @return 0 if no shrink needed otherwise a value greater zero
     */
    private double shrinkIt(final DataRow row, final boolean shrinkIt) {
        // compute distance between centroid and given row
        double dist = m_predRow.computeDistance(row);
        // new std dev for theta minus
        double newStdDev = dist / Math.sqrt(-Math.log(m_thetaMinus));
        // if std dev is max or new std dev less that current
        if (m_predRow.isNotShrunk() || (newStdDev < m_predRow.getStdDev())) {
            // remembers old standard deviation for shrink value
            double oldStdDev = m_predRow.getStdDev();
            if (shrinkIt) {
                // set current to new std dev for theta minus
                // std dev was affected, set
                m_predRow.shrinkIt(newStdDev);
            }
            return oldStdDev - newStdDev;
        } else {
            // otherwise no shrink performed
            return 0.0;
        }
    }

    /**
     * Method is empty.
     */
    @Override
    protected final void reset() {
        // empty
    }

    /**
     * Method is empty.
     * 
     * @param row Ignored.
     */
    @Override
    protected final void cover(final DataRow row) {
        // empty
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return m_predRow.toString();
    }

    /**
     * @see BasisFunctionLearnerRow#getFinalCell(int)
     */
    @Override
    protected DataCell getFinalCell(final int index) {
        return super.getAnchor().getCell(index);
    }

    /**
     * @see BasisFunctionLearnerRow
     *      #computeActivation(org.knime.core.data.DataRow)
     */
    @Override
    public double computeActivation(final DataRow row) {
        return m_predRow.computeActivation(row);
    }
}
