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
 * ---------------------------------------------------------------------
 *
 * History
 *   07.04.2008 (thor): created
 */
package org.knime.base.node.preproc.sample;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This row filter always includes at least the first and the last row. The
 * remaining rows are taken linearly over the whole table.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LinearSamplingRowFilter extends RowFilter {
    private final int m_rowCount;
    private final double m_rowWeight;
    private int m_count;
    private double m_sum;

    /**
     * Creates a new row filter that selects a certain absolute number of rows.
     *
     * @param rowCount the total number of rows in the input table.
     * @param count the number of rows that should be selected
     */
    public LinearSamplingRowFilter(final int rowCount, final int count) {
        m_rowCount = rowCount;
        m_rowWeight = count / (double) rowCount;
        m_count = count;
    }


    /**
     * Creates a new row filter that selects a certain fraction of rows.
     *
     * @param rowCount the total number of rows in the input table.
     * @param fraction the fraction of rows that should be selected (0 to 1)
     */
    public LinearSamplingRowFilter(final int rowCount, final double fraction) {
        m_rowCount = rowCount;
        m_rowWeight = fraction;
        m_count = (int) Math.round(rowCount * fraction);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {

        m_sum += m_rowWeight;
        if ((rowIndex == 0) || (rowIndex == m_rowCount - 1)) {
            m_count--;
            return true;
        }
        if (m_count == 1) {
            return false;
        }
        if ((int)(m_sum - m_rowWeight) < (int) m_sum) {
            m_count--;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        // nothing to do
    }
}
