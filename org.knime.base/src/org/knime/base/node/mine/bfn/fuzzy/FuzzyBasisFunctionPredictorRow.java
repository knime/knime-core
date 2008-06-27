/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   29.06.2006 (gabriel): created
 */
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.mine.bfn.BasisFunctionPredictorRow;
import org.knime.base.node.mine.bfn.Distance;
import org.knime.base.node.mine.bfn.fuzzy.membership.MembershipFunction;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.util.MutableDouble;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FuzzyBasisFunctionPredictorRow extends BasisFunctionPredictorRow {
    
    private final MembershipFunction[] m_mem;

    private final int m_norm;

    /**
     * Everything below this threshold causes an activation, also don't know
     * class probability.
     */
    private static final double MINACT = 0.0;

    /**
     * Creates a new predictor as fuzzy rule.
     * @param key The id for this rule.
     * @param classLabel The class label of this rule.
     * @param mem An array of membership functions each per dimension. 
     * @param norm A fuzzy norm to combine activations via all dimensions.
     */
    protected FuzzyBasisFunctionPredictorRow(final RowKey key,
            final DataCell classLabel, final MembershipFunction[] mem,
            final int norm) {
        super(key, classLabel, MINACT);
        m_norm = norm;
        m_mem = mem;
    }

    /**
     * Creates a new predictor as fuzzy rule.
     * @param pp Content to read rule from.
     * @throws InvalidSettingsException If the content is invalid.
     */
    public FuzzyBasisFunctionPredictorRow(final ModelContentRO pp)
            throws InvalidSettingsException {
        super(pp);
        m_norm = pp.getInt(Norm.NORM_KEY);
        ModelContentRO memParams = pp.getModelContent("membership_functions");
        m_mem = new MembershipFunction[memParams.keySet().size()];
        int i = 0;
        for (String key : memParams.keySet()) {
            m_mem[i] = new MembershipFunction(memParams.getModelContent(key));
            i++;
        }
    }

    /**
     * Return number of memberships which is equivalent to the number of
     * numeric input dimensions.
     * @return Number of membership functions. 
     */
    public int getNrMemships() {
        return m_mem.length;
    }

    /**
     * Returns the membership for one dimension.
     * @param i Dimension index.
     * @return A fuzzy membership function.
     */
    public MembershipFunction getMemship(final int i) {
        return m_mem[i];
    }
    
    /**
     * @return array of fuzzy membership function
     */
    public MembershipFunction[] getMemships() {
        return m_mem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final ModelContentWO pp) {
        super.save(pp);
        pp.addInt(Norm.NORM_KEY, m_norm);
        ModelContentWO memParams = pp.addModelContent("membership_functions");
        for (int i = 0; i < m_mem.length; i++) {
            m_mem[i].save(memParams.addModelContent("" + i));
        }
    }

    /**
     * Composes the degree of membership by using the disjunction of the
     * tco-norm operator.
     * 
     * @param row row
     * @param act activation
     * @return the new activation array
     * 
     * @see #computeActivation(DataRow)
     * @see Norm#computeTCoNorm(double,double)
     */
    @Override
    public double compose(final DataRow row, final double act) {
        assert (act >= 0.0 && act <= 1.0) : "act=" + act;
        // compute current activation (maximum) of input row
        return Norm.NORMS[m_norm].computeTCoNorm(
                computeActivation(row), act);
    }

    /**
     * Returns the compute activation of this input vector.
     * 
     * @param row input pattern
     * @return membership degree
     */
    @Override
    public double computeActivation(final DataRow row) {
        assert (m_mem.length == row.getNumCells());
        // sets degree to maximum
        double degree = 1.0;
        // overall cells in the vector
        for (int i = 0; i < m_mem.length; i++) {
            DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                continue;
            }
            // gets cell at index i
            double value = ((DoubleValue) cell).getDoubleValue();
            // act in current dimension
            double act = m_mem[i].getActivation(value);
            if (i == 0) {
                degree = act;
                continue;
            }
            // calculates the new (minimum) degree using norm index
            degree = Norm.NORMS[m_norm].computeTNorm(degree, act);
            assert (0.0 <= degree && degree <= 1.0);
        }
        // returns membership degree
        return degree;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public double computeDistance(final DataRow row) {
        assert row.getNumCells() == m_mem.length;
        double[] d1 = new double[m_mem.length];
        double[] d2 = new double[m_mem.length];
        for (int i = 0; i < m_mem.length; i++) {
            DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                d1[i] = Double.NaN;
            } else {
                d1[i] = ((DoubleValue) cell).getDoubleValue();
            }
            if (m_mem[i].isMissingIntern()) {
                d2[i] = Double.NaN;
            } else {
                d2[i] = m_mem[i].getAnchor();
            }
        }
        return Distance.getInstance().compute(d1, d2);
    }
    
    /**
     * Returns the aggregated spread of the core.
     * 
     * @return the overall spread of the core regions
     */
    @Override
    public double computeSpread() {
        double vol = 0.0;
        double dom = 0.0;
        for (int i = 0; i < getNrMemships(); i++) {
            MembershipFunction mem = getMemship(i);
            if (mem.isMissingIntern()) {
                continue;
            }
            double spread = (mem.getMaxCore() - mem.getMinCore());
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
     * 
     * {@inheritDoc}
     */
    @Override
    public int getNrUsedFeatures() {
        int used = 0;
        for (MembershipFunction mem : m_mem) {
            if (mem.isMissingIntern() || !mem.isSuppLeftMax() 
                    || !mem.isSuppRightMax()) {
                used++;
            }
        }
        return used; 
    }
    
    /**
     * @return fuzzy norm
     */
    public final int getNorm() {
        return m_norm;
    }
    
    /**
     * @return array of minimum bounds
     */
    public final MutableDouble[] getMins() {
        MutableDouble[] mins = new MutableDouble[m_mem.length];
        for (int i = 0; i < mins.length; i++) {
            mins[i] = m_mem[i].getMin();
        }
        return mins;
    }
    
    /**
     * @return array of maximum bounds
     */
    public final MutableDouble[] getMaxs() {
        MutableDouble[] maxs = new MutableDouble[m_mem.length];
        for (int i = 0; i < maxs.length; i++) {
            maxs[i] = m_mem[i].getMax();
        }
        return maxs;
    }

}
