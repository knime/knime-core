/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   29.06.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row.rowfilter;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class OrRowFilter extends RowFilter {

    private static final String CFG_FILTER1 = "ConfigFilter1";

    private static final String CFG_FILTER2 = "ConfigFilter2";

    private RowFilter m_in1;

    private RowFilter m_in2;

    private boolean m_eotIn1;

    private boolean m_eotIn2;

    /**
     * Implements a RowFilter that takes two other RowFilters and combines their
     * results with a logical OR. If filter in1 returns a match the matches()
     * method of filter in2 is not invoked!
     * 
     * @param in1 RowFilter as first input into the OR result
     * @param in2 RowFilter for the second input of the OR result. Might be
     *            short cutted.
     */
    public OrRowFilter(final RowFilter in1, final RowFilter in2) {
        if (in1 == null) {
            throw new NullPointerException("RowFilter in1 must not be null");
        }
        if (in2 == null) {
            throw new NullPointerException("RowFilter in2 must not be null");
        }
        m_in1 = in1;
        m_in2 = in2;
        m_eotIn1 = false;
        m_eotIn2 = false;
    }

    /**
     * The row filter created by this contructor can not be used without setting
     * two input filters by loading settings from a congif object.
     */
    public OrRowFilter() {
        m_in1 = null;
        m_in2 = null;
        m_eotIn1 = false;
        m_eotIn2 = false;
    }

    /**
     * @return the row filter connected to one of the inputs of the logical OR.
     *         Returns the one that is not short cutted.
     */
    public RowFilter getInput1() {
        return m_in1;
    }

    /**
     * @return the row filter connected to one of the inputs of the logical OR.
     *         Returns the one that could be short cutted.
     */
    public RowFilter getInput2() {
        return m_in2;
    }

    /**
     * @see RowFilter#matches(DataRow, int)
     */
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        /*
         * we must keep flags storing which of the filters threw an eot. Only
         * after both filters are at the eot we can also indicate an eot. But a
         * IncludeFromNowOn exception can just fly through.
         */

        boolean resultIn1;
        boolean resultIn2;
        if (!m_eotIn1) {
            try {
                resultIn1 = m_in1.matches(row, rowIndex);
            } catch (EndOfTableException eot) {
                resultIn1 = false;
                m_eotIn1 = true;
                if (m_eotIn2) {
                    // now both are at eot - we are then too
                    throw new EndOfTableException();
                }
            }
        } else {
            // don't call in1 when it indicated eot before
            resultIn1 = false;
        }

        if (!m_eotIn2) {
            try {
                resultIn2 = m_in2.matches(row, rowIndex);
            } catch (EndOfTableException eot) {
                resultIn2 = false;
                m_eotIn2 = true;
                if (m_eotIn1) {
                    // now both are at eot - we are then too
                    throw new EndOfTableException();
                }
            }
        } else {
            // don't call in2 when it indicated eot before
            resultIn2 = false;
        }

        return resultIn1 || resultIn2;
    }

    /**
     * @see RowFilter#loadSettingsFrom(NodeSettingsRO)
     */
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {

        NodeSettingsRO cfg1 = cfg.getNodeSettings(CFG_FILTER1);
        NodeSettingsRO cfg2 = cfg.getNodeSettings(CFG_FILTER2);

        m_in1 = RowFilterFactory.createRowFilter(cfg1);
        m_in2 = RowFilterFactory.createRowFilter(cfg2);
    }

    /**
     * @see RowFilter#saveSettings(NodeSettingsWO)
     */
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
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *      #configure(de.unikn.knime.core.data.DataTableSpec)
     */
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        DataTableSpec spec1 = null;
        DataTableSpec spec2 = null;

        if (m_in1 != null) {
            spec1 = m_in1.configure(inSpec);
        } else {
            throw new InvalidSettingsException(
                    "OR-rowfilter: no input filter set");
        }
        if (m_in2 != null) {
            spec2 = m_in2.configure(inSpec);
        } else {
            throw new InvalidSettingsException(
                    "OR-rowfilter: no input filter set");
        }
        if ((spec1 != null) || (spec2 != null)) {
            // TODO: how in the world do we OR two specs?!?
            return null;
        }
        return null;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "OR-Filter:\nINPUT1: " + m_in1.toString() + "\nINPUT2: "
                + m_in2.toString();
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        OrRowFilter orf = (OrRowFilter)super.clone();
        if (m_in1 != null) {
            orf.m_in1 = (RowFilter)m_in1.clone();
        }
        if (m_in2 != null) {
            orf.m_in2 = (RowFilter)m_in2.clone();
        }
        orf.m_eotIn1 = false;
        orf.m_eotIn2 = false;
        return orf;
    }
}
