/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 *   04.07.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row.rowfilter;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * Negates the match results from the filter passed. Unfortunately EndOfTable
 * exceptions (indicating that matches from now on will always fail) will be
 * caught and swallowed by this filter. Thus using this filter in a filter
 * hierarchy will cause the row filter table to always iterate through the end
 * of the original table before it flags an EOT.
 * 
 * @author ohl, University of Konstanz
 */
public class NegRowFilter extends RowFilter {

    private static final String CFG_INFILTER = "ConfigFilterIn";

    private RowFilter m_inFilter;

    /**
     * default constructor of the NegFilter. Don't use it until you've loaded
     * settings.
     */
    NegRowFilter() {
        m_inFilter = null;
    }

    /**
     * Creates a new row filter negating the match results of the input filter.
     * 
     * @param inFilter the input filter to negate.
     */
    public NegRowFilter(final RowFilter inFilter) {
        if (inFilter == null) {
            throw new NullPointerException("Can't use null filter as "
                    + "input row filter.");
        }
        m_inFilter = inFilter;
    }

    /**
     * @return the row filter connected to the input of this.
     */
    public RowFilter getInput() {
        return m_inFilter;
    }

    /**
     * @see RowFilter#matches(DataRow, int)
     */
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {

        try {
            return !m_inFilter.matches(row, rowIndex);
        } catch (EndOfTableException eot) {
            // if the inFilter tells us no more matches - we always match...
            throw new IncludeFromNowOn(eot.getMessage());
        } catch (IncludeFromNowOn ifno) {
            // if the filter tells us it matches from now on - we don't.
            throw new EndOfTableException(ifno.getMessage());
        }

        /*
         * note: we assume we are not called anymore after we've thrown one of
         * the above exceptions.
         */
    }

    /**
     * @see RowFilter#loadSettingsFrom(NodeSettings)
     */
    public void loadSettingsFrom(final NodeSettings cfg)
            throws InvalidSettingsException {

        NodeSettings inCfg = cfg.getConfig(CFG_INFILTER);

        m_inFilter = RowFilterFactory.createRowFilter(inCfg);
 
    }

    /**
     * @see RowFilter#saveSettings(NodeSettings)
     */
    protected void saveSettings(final NodeSettings cfg) {
        if (m_inFilter != null) {
            NodeSettings inCfg = cfg.addConfig(CFG_INFILTER);
            m_inFilter.saveSettingsTo(inCfg);
        }
    }

    
    /**
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     * #configure(de.unikn.knime.core.data.DataTableSpec)
     */
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        if (m_inFilter == null) {
                throw new InvalidSettingsException(
                        "NEG-rowfilter: no input filter set");
        } else {
            return m_inFilter.configure(inSpec);
        }
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "NEG-Filter:\nINPUT: " + m_inFilter.toString();
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        NegRowFilter nrf = (NegRowFilter)super.clone();
        if (m_inFilter != null) {
            nrf.m_inFilter = (RowFilter)m_inFilter.clone();
        }
        return nrf;
    }
}
