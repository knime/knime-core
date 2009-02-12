/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.base.node.mine.bfn.fuzzy.membership.MembershipFunction;
import org.knime.base.node.mine.bfn.fuzzy.membership.TrapezoidMembershipFunction;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.util.MutableDouble;

/**
 * Extends the general {@link BasisFunctionLearnerRow} object to act as
 * rectangular fuzzy prototype. Each feature value holds a fuzzy membership
 * function (trapezoid membership function so far) with a assigned anchor
 * retrieved from the input row which commit this rule.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FuzzyBasisFunctionLearnerRow extends BasisFunctionLearnerRow {
    
    /** The choice of shrink procedure. */
    private final int m_shrink;

    /** Underlying predictive fuzzy row. */
    private final FuzzyBasisFunctionPredictorRow m_predRow;

    /**
     * Creates a new learner row for fuzzy rules.
     * @param key the key for the row.
     * @param classInfo the class label.
     * @param centroid the initial center row which forms the anchor of this
     *            fuzzy rule.
     * @param norm A fuzzy norm function.
     * @param shrink A function to shrink rules.
     * @param min An array if minimum bounds, for each input dimension.
     * @param max An array if maximum bounds, for each input dimension.
     */
    protected FuzzyBasisFunctionLearnerRow(final RowKey key, 
            final DataCell classInfo, final DataRow centroid, final int norm, 
            final int shrink, 
            final MutableDouble[] min, final MutableDouble[] max) {
        super(key, centroid, classInfo);
        m_shrink = shrink;
        assert (min.length == centroid.getNumCells());
        assert (max.length == centroid.getNumCells());
        // init trapezoid fuzzy membership functions for each dimension
        MembershipFunction[] mem = new TrapezoidMembershipFunction[centroid
                .getNumCells()];
        for (int i = 0; i < mem.length; i++) {
            DataCell cell = centroid.getCell(i);
            DoubleValue value;
            if (cell.isMissing()) {
                value = null;
            } else {
                value = (DoubleValue)cell;
            }
            mem[i] = new TrapezoidMembershipFunction(value, min[i], max[i]);
        }

        m_predRow = new FuzzyBasisFunctionPredictorRow(key, classInfo,
                mem, norm);
        addCovered(centroid, classInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FuzzyBasisFunctionPredictorRow getPredictorRow() {
        return m_predRow;
    }

    /**
     * @param col the column index
     * @return the center of gravity in this dimension.
     */
    @Override
    public DoubleValue getMissingValue(final int col) {
        // double m = (m_pred.getMemship(col].getMaxCore() +
        // m_pred.getMemship(col].getMinCore()) / 2.0;
        // return new DoubleCell(m);
        return new DoubleCell(m_predRow.getMemship(col).getCenterOfGravity());
    }

    /**
     * Returns <b>true</b> if the given row is covered by this prototype, that
     * is, if {@link #computeActivation(DataRow)} returns a degree greater than
     * <code>MINACT</code>.
     * 
     * @param row the data row to check coverage for
     * @return <code>true</code> if the row is covered
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public boolean covers(final DataRow row) {
        assert (m_predRow.getNrMemships() == row.getNumCells());
        return (computeActivation(row) > m_predRow.getDontKnowClassDegree());
    }

    /**
     * Returns <code>true</code> if the given row is covered by this
     * prototype, that is, if {@link #computeActivation(DataRow)} returns a
     * degree equal 1.
     * 
     * @param row the data row to check coverage for
     * @return <code>true</code> if the row is explained
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public boolean explains(final DataRow row) {
        assert (m_predRow.getNrMemships() == row.getNumCells());
        return (computeActivation(row) == 1.0);
    }

    /**
     * Called if a new {@link BasisFunctionLearnerRow} has to be adjusted.
     * 
     * @param row conflicting pattern
     * @return a value greater zero if a conflict has to be solved. The value
     *         indicates relative loss in coverage for this basis function.
     */
    @Override
    public
    final boolean getShrinkValue(final DataRow row) {
        return shrinkIt(row, false) > 0.0;
    }

    /**
     * If a new prototype has to be adjusted. Goes through all membership
     * function dimensions and looks for this with the smallest loss value. The
     * support shrink has priority for the core region, if no shrink need the
     * function returns false, otherwise true if the shrink was effected.
     * 
     * @param row the input pattern for shrinking
     * @return <code>true</code> if a dimension was effect by this operation
     */
    @Override
    public
    final boolean shrink(final DataRow row) {
        return shrinkIt(row, true) > 1E-10;
    }

    /**
     * If <code>shrinkIt</code> is <code>true</code> the shrink will be
     * executed otherwise the shrink value is only returned.
     * 
     * @param row the input pattern for shrinking @return 0 if no shrink needed
     * otherwise a value greater zero
     */
    private double shrinkIt(final DataRow row, final boolean shrinkIt) {
        assert (m_predRow.getNrMemships() == row.getNumCells());
        // init support loss with maximum and index with dft -1
        double suppLoss = Double.MAX_VALUE;
        int suppK = -1;
        // init core loss with maximum and index with dft -1
        double coreLoss = Double.MAX_VALUE;
        int coreK = -1;
        // overall cells in the vector
        for (int i = 0; i < m_predRow.getNrMemships(); i++) {
            // gets current cell in the vector
            DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                continue;
            }
            // get cell value
            double value = ((DoubleValue)cell).getDoubleValue();
            // if missing dimension continue
            if (m_predRow.getMemship(i).isMissingIntern()) {
                continue;
            }
            // retrieve anchor value
            double a = m_predRow.getMemship(i).getAnchor();
            // if value on the left side of the anchor
            if (value < a) {
                // if left side is unconst. or value truly is in support region
                if (m_predRow.getMemship(i).isSuppLeftMax()
                        || value > m_predRow.getMemship(i).getMinSupport()) {
                    // if the value is in the core region
                    if (value >= m_predRow.getMemship(i).getMinCore()) {
                        // gets the loss in the core region
                        double loss = Shrink.SHRINKS[m_shrink].
                            leftCoreLoss(value, m_predRow.getMemship(i));
                        assert (0.0 <= loss && loss <= 1.0) : loss;
                        // if the new loss is less than the current
                        if (loss < coreLoss) {
                            coreLoss = loss;
                            coreK = i;
                        }
                    } else { // if value is not in the core
                        if (value >= m_predRow.getMemship(i).getMinSupport()) {
                            // gets loss in the support region
                            double loss = Shrink.SHRINKS[m_shrink].
                                leftSuppLoss(value, m_predRow.getMemship(i));
                            assert (0 <= loss && loss <= 1.0) : loss;
                            // if new value less that the current value
                            if (loss < suppLoss) {
                                suppLoss = loss;
                                suppK = i;
                            }
                        }
                    }
                } else { // if not in the support region or left is not
                    // unconst.
                    return 0.0; // return false, no shrink needed
                }
            } else {
                // id value is on the right side of the anchor
                if (value > a) {
                    // if the right side is unconstrained or
                    // value is in support region
                    if (m_predRow.getMemship(i).isSuppRightMax()
                           || value < m_predRow.getMemship(i).getMaxSupport()) {
                        // if the value is in the core region
                        if (value <= m_predRow.getMemship(i).getMaxCore()) {
                            // gets loss value
                            double loss = Shrink.SHRINKS[m_shrink].
                                rightCoreLoss(value, m_predRow.getMemship(i));
                            assert (0.0 <= loss && loss <= 1.0) : loss;
                            // if the new loss is less than the current one
                            if (loss < coreLoss) {
                                coreLoss = loss;
                                coreK = i;
                            }
                        } else { // if value is not in core region,
                            if (value <= m_predRow.getMemship(i)
                                    .getMaxSupport()) {
                                double loss = Shrink.SHRINKS[m_shrink].
                                    rightSuppLoss(value, 
                                            m_predRow.getMemship(i));
                                assert (0.0 <= loss && loss <= 1.0) : loss;
                                // if new loss is less than current
                                if (loss < suppLoss) {
                                    suppLoss = loss;
                                    suppK = i;
                                }
                            }
                        }
                    } else { // if value in not in the support region
                        return 0.0; // ... no shrink needed, returns false
                    }
                } else { // if value equal to the anchor
                    assert (value == a);
                    continue; // no shrink possible, try next dimension
                }
            }
        }
        assert suppK < 0 || (suppK >= 0 && 0.0 <= suppLoss && suppLoss <= 1.0);
        assert coreK < 0 || (coreK >= 0 && 0.0 <= coreLoss && coreLoss <= 1.0);
        // if shrink in the support region is possible and needed
        if (suppK > -1) {
            // get value to shrink support
            double value = ((DoubleValue)row.getCell(suppK)).getDoubleValue();
            // id value left of the anchor
            if (value < m_predRow.getMemship(suppK).getAnchor()) {
                if (shrinkIt) {
                    // shrink support
                    m_predRow.getMemship(suppK).setSuppLeft(value);
                }
                // shrink was made
                return suppLoss;
            } else {
                // if value is on the right side of the anchor
                if (value > m_predRow.getMemship(suppK).getAnchor()) {
                    if (shrinkIt) {
                        // shrink support
                        m_predRow.getMemship(suppK).setSuppRight(value);
                    }
                    // shrink was made
                    return suppLoss;
                } else { // conflict, all values are equal to the anchor
                    assert (value == m_predRow.getMemship(suppK).getAnchor());
                    return 0.0; // shrink not possible
                }
            }
        } else {
            // if shrink in the core regions is only possible
            if (coreK > -1) {
                // get value to shrink core
                double value = ((DoubleValue)row.getCell(coreK))
                        .getDoubleValue();
                // if of the left anchor side
                if (value < m_predRow.getMemship(coreK).getAnchor()) {
                    if (shrinkIt) {
                        // shrink left core and support
                        m_predRow.getMemship(coreK).setCoreLeft(value);
                        m_predRow.getMemship(coreK).setSuppLeft(value);
                    }
                    // shrink was made
                    return coreLoss;
                } else { // if on the right anchor side
                    if (value > m_predRow.getMemship(coreK).getAnchor()) {
                        if (shrinkIt) {
                            // shrink right core and support
                            m_predRow.getMemship(coreK).setCoreRight(value);
                            m_predRow.getMemship(coreK).setSuppRight(value);
                        }
                        // shrink was made
                        return coreLoss;
                    } else { // conflict, all values are equal to the anchor
                        assert (value == m_predRow.getMemship(coreK)
                                .getAnchor());
                        return 0.0; // shrink not possible
                    }
                }
            } else {
                assert (suppK == -1 && coreK == -1);
                return 0.0;
            }
        }
    }

    /**
     * Resets core value of all dimensions to the initial anchor value.
     */
    @Override
    public void reset() {
        for (int i = 0; i < m_predRow.getNrMemships(); i++) {
            if (!m_predRow.getMemship(i).isMissingIntern()) {
                m_predRow.getMemship(i).resetCore();
            }
        }
    }

    /**
     * This basis function covers the given row.
     * 
     * @param row the row to cover
     */
    @Override
    public void cover(final DataRow row) {
        assert (m_predRow.getNrMemships() == row.getNumCells());
        // overall data cells in the vector
        for (int i = 0; i < m_predRow.getNrMemships(); i++) {
            DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                continue;
            }
            // gets current cell in the vector
            double value = ((DoubleValue)cell).getDoubleValue();

            if (m_predRow.getMemship(i).isMissingIntern()) {
                m_predRow.getMemship(i).setAnchor(value);
                continue;
            }

            // if new value is less than current
            if (value < m_predRow.getMemship(i).getMinCore()) {
                // ... replace it
                m_predRow.getMemship(i).setCoreLeft(value);
            } else { // if new is greater than current value
                if (value > m_predRow.getMemship(i).getMaxCore()) {
                    // ... replace it
                    m_predRow.getMemship(i).setCoreRight(value);
                }
            }
        }
    }

    /**
     * Compares this basisfunction with the another one by the fuzzy rule's
     * number of covered pattern.
     * 
     * @param o the other basisfunction to compare with
     * @param r the row to compute activation on
     * @return <code>true</code> if the number covered pattern of
     *         <code>this</code> object is greater
     * @throws NullPointerException if one of the args in <code>null</code>
     */
    @Override
    public boolean compareCoverage(final BasisFunctionLearnerRow o,
            final DataRow r) {
        return m_predRow.getNumAllCoveredPattern() > o.getPredictorRow()
                .getNumAllCoveredPattern();
    }

    /**
     * Returns the aggregated spread of the core.
     * 
     * @return the overall spread of the core regions
     */
    public double computeSpread() {
        double vol = 0.0;
        double dom = 0.0;
        for (int i = 0; i < m_predRow.getNrMemships(); i++) {
            MembershipFunction mem = m_predRow.getMemship(i);
            if (mem.isMissingIntern()) {
                continue;
            }
            double spread = (mem.getMaxSupport() - mem.getMinSupport());
            if (spread > 0.0) {
                if (vol == 0.0) {
                    vol = spread;
                    dom = (mem.getMax().doubleValue() 
                            - mem.getMin().doubleValue());
                } else {
                    vol *= spread;
                    dom *= (mem.getMax().doubleValue() 
                            - mem.getMin().doubleValue());
                }
            }
        }
        return (vol > 0 ? vol / dom : 0);
    }

    /**
     * Returns a string representation of this basis function. Calls the super
     * <code>toString()</code> before adding this fuzzy bfs membership
     * functions.
     * 
     * @return a String summary of this fuzzy bf
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("IF ");
        for (int c = 0; c < m_predRow.getNrMemships(); c++) {
            if (c > 0) {
                buf.append(" AND ");
            }
            buf.append(m_predRow.getMemship(c).toString());
        }
        buf.append(" THEN " + super.toString());
        return buf.toString();
    }

    /**
     * Returns a ARFF string-like summary of this fuzzy bf.
     * 
     * @return a ARFF-style fuzzy bf representation
     */
    public String toStringARFF() {
        StringBuffer buf = new StringBuffer();
        for (int c = 0; c < m_predRow.getNrMemships(); c++) {
            buf.append("mf:\t" + m_predRow.getMemship(c).toString() + "\n");
        }
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getFinalCell(final int index) {
        return m_predRow.getMemship(index).createFuzzyIntervalCell();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeActivation(final DataRow row) {
        return m_predRow.computeActivation(row);
    }

    /**
     * @return shrink method
     */
    public final int getShrink() {
        return m_shrink;
    }
}
