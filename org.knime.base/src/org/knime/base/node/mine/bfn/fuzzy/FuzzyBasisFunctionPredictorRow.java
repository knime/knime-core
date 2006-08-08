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
import org.knime.base.node.mine.bfn.fuzzy.membership.MembershipFunction;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * 
 * @author Thoams Gabriel, University of Konstanz
 */
public class FuzzyBasisFunctionPredictorRow extends BasisFunctionPredictorRow {
    private final MembershipFunction[] m_mem;

    private final int m_norm;

    /**
     * Everything below this threshold causes an activation, also don't know
     * class propability.
     */
    private static final double MINACT = 0.0;

    FuzzyBasisFunctionPredictorRow(final DataCell key,
            final DataCell classLabel, final MembershipFunction[] mem,
            final int norm) {
        super(key, classLabel, MINACT);
        m_norm = norm;
        m_mem = mem;
    }

    FuzzyBasisFunctionPredictorRow(final ModelContentRO pp)
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

    public int getNrMemships() {
        return m_mem.length;
    }

    public MembershipFunction getMemship(final int i) {
        return m_mem[i];
    }

    /**
     * @see BasisFunctionPredictorRow
     *      #save(org.knime.core.node.ModelContentWO)
     */
    @Override
    protected void save(final ModelContentWO pp) {
        super.save(pp);
        pp.addInt(Norm.NORM_KEY, m_norm);
        ModelContentWO memParams = pp.addModelContent("membership_functions");
        for (int i = 0; i < m_mem.length; i++) {
            m_mem[i].save(memParams.addModelContent("" + i));
        }
    }

    /**
     * Composes the degree of membership by using the disjunktion of the
     * tco-norm operator.
     * 
     * @param row the row to compute the activation from
     * @param act the activation to compromise
     * @return the new activation array
     * 
     * @see #computeActivation(DataRow)
     * @see Norm#computeTCoNorm(double,double)
     */
    @Override
    public double compose(final DataRow row, final double act) {
        assert (row != null);
        assert (0 <= act && act <= 1.0);
        // already maximum reached
        if (act == 1.0) {
            return act;
        }
        // compute current activation of input row
        return Norm.NORMS[m_norm].computeTCoNorm(act, computeActivation(row));
    }

    /**
     * Returns the compute activation of this input vector.
     * 
     * @param row input pattern
     * @return membership degree
     */
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
            double value = ((DoubleValue)cell).getDoubleValue();
            // act in current dimension
            double act = m_mem[i].getActivation(value);
            // shortes, lowest poss. memship degree already reached, return
            if (act == 0.0) {
                return act;
            }
            // calculates the new degree using inorm
            degree = Norm.NORMS[m_norm].computeTNorm(degree, act);
            assert (0.0 <= degree && degree <= 1.0);
        }
        // returns membership degree
        return degree;
    }
}
