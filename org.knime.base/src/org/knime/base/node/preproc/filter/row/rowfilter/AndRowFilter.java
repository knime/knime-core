/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   29.06.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A row filter for the row filter data table ANDing two other row filters.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class AndRowFilter extends RowFilter {

    private static final String CFG_FILTER1 = "ConfigFilter1";

    private static final String CFG_FILTER2 = "ConfigFilter2";

    private RowFilter m_in1;

    private RowFilter m_in2;

    // flags indicating a "IncludeFromNowOn" from the corresp. filter.
    private boolean m_in1True;

    private boolean m_in2True;

    /**
     * Implements a {@link RowFilter} that takes two other
     * {@link RowFilter}s and combines their results with a logical AND. If
     * filter <code>in1</code> returns a no match the
     * {@link RowFilter#matches(DataRow, int)} method of filter <code>in2</code>
     * will not be invoked!
     * 
     * @param in1 row filter as first input into the AND result
     * @param in2 row filter for the second input of the AND result; might be
     *            short cutted
     */
    public AndRowFilter(final RowFilter in1, final RowFilter in2) {
        if (in1 == null) {
            throw new NullPointerException("RowFilter in1 must not be null");
        }
        if (in2 == null) {
            throw new NullPointerException("RowFilter in2 must not be null");
        }
        m_in1 = in1;
        m_in2 = in2;
        m_in1True = false;
        m_in2True = false;
    }

    /**
     * The filter created by this constructor cannot be used without loading
     * settings through a config object.
     */
    public AndRowFilter() {
        m_in1 = null;
        m_in2 = null;
        m_in1True = false;
        m_in2True = false;
    }

    /**
     * @return the row filter connected to one of the inputs of the logical AND.
     *         Returns the one that is not short cutted.
     */
    public RowFilter getInput1() {
        return m_in1;
    }

    /**
     * @return the row filter connected to one of the inputs of the logical AND.
     *         Returns the one that could be short cutted.
     */
    public RowFilter getInput2() {
        return m_in2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {

        /*
         * note: if one of these filters throws an EOTexception, we can let that
         * go through. If that filter will not match any rows anymore, we won't
         * either.
         */

        boolean result1 = true;
        boolean result2 = true;

        if (!m_in1True) {
            try {
                result1 = m_in1.matches(row, rowIndex);
            } catch (IncludeFromNowOn ifno) {
                m_in1True = true;
                if (m_in2True) {
                    // now both inputs are always true - so are we.
                    throw new IncludeFromNowOn();
                }
            }
        }
        if (!m_in2True) {
            try {
                result2 = m_in2.matches(row, rowIndex);
            } catch (IncludeFromNowOn ifno) {
                m_in2True = true;
                if (m_in1True) {
                    // now both inputs are always true - so are we.
                    throw new IncludeFromNowOn();
                }
            }
        }
        return result1 & result2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {

        NodeSettingsRO cfg1 = cfg.getNodeSettings(CFG_FILTER1);
        NodeSettingsRO cfg2 = cfg.getNodeSettings(CFG_FILTER2);

        m_in1 = RowFilterFactory.createRowFilter(cfg1);
        m_in2 = RowFilterFactory.createRowFilter(cfg2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        if (m_in1 != null) {
            NodeSettingsWO cfg1 = cfg.addNodeSettings(CFG_FILTER1);
            m_in1.saveSettingsTo(cfg1);
        }
        if (m_in2 != null) {
            NodeSettingsWO cfg2 = cfg.addNodeSettings(CFG_FILTER2);
            m_in2.saveSettingsTo(cfg2);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AND-Filter:\nINPUT1: " + m_in1.toString() + "\nINPUT2: "
                + m_in2.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        DataTableSpec spec1 = null;
        DataTableSpec spec2 = null;

        if (m_in1 != null) {
            spec1 = m_in1.configure(inSpec);
        } else {
            throw new InvalidSettingsException(
                    "AND-rowfilter: no input filter set");
        }
        if (m_in2 != null) {
            spec2 = m_in2.configure(inSpec);
        } else {
            throw new InvalidSettingsException(
                    "AND-rowfilter: no input filter set");
        }
        if ((spec1 != null) || (spec2 != null)) {
            // TODO: how in the world do we AND two specs?!?
            return null;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        AndRowFilter arf = (AndRowFilter)super.clone();
        if (m_in1 != null) {
            arf.m_in1 = (RowFilter)m_in1.clone();
        }
        if (m_in2 != null) {
            arf.m_in2 = (RowFilter)m_in2.clone();
        }
        arf.m_in1True = false;
        arf.m_in2True = false;
        return arf;
    }
}
