/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
