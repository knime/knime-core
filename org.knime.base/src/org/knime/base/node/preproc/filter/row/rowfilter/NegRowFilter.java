/*
 * ------------------------------------------------------------------
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
 *   04.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Negates the match results from the filter passed.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class NegRowFilter extends RowFilter {

    private static final String CFG_INFILTER = "ConfigFilterIn";

    private RowFilter m_inFilter;

    /**
     * Default constructor of the NegFilter. Don't use it until you've loaded
     * settings.
     */
    NegRowFilter() {
        m_inFilter = null;
    }

    /**
     * Creates a new row filter negating the match results of the input filter.
     *
     * @param inFilter the input filter to negate
     */
    public NegRowFilter(final RowFilter inFilter) {
        if (inFilter == null) {
            throw new NullPointerException("Can't use null filter as "
                    + "input row filter.");
        }
        m_inFilter = inFilter;
    }

    /**
     * @return the row filter connected to the input of this
     */
    public RowFilter getInput() {
        return m_inFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {

        NodeSettingsRO inCfg = cfg.getNodeSettings(CFG_INFILTER);

        m_inFilter = RowFilterFactory.createRowFilter(inCfg);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        if (m_inFilter != null) {
            NodeSettingsWO inCfg = cfg.addNodeSettings(CFG_INFILTER);
            m_inFilter.saveSettingsTo(inCfg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NEG-Filter:\nINPUT: " + m_inFilter.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        NegRowFilter nrf = (NegRowFilter)super.clone();
        if (m_inFilter != null) {
            nrf.m_inFilter = (RowFilter)m_inFilter.clone();
        }
        return nrf;
    }
}
