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
 * ---------------------------------------------------------------------
 *
 * History
 *   04.03.2008 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Super class for all row filters that test an attribute value (like
 * {@link StringCompareRowFilter}, {@link MissingValueRowFilter}, and
 * {@link RangeRowFilter}).
 *
 * @author ohl, University of Konstanz
 */
public abstract class AttrValueRowFilter extends RowFilter {

    private static final String CFGKEY_INCLUDE = "include";

    private static final String CFGKEY_COLNAME = "ColumnName";

    private boolean m_include;

    private String m_colName;

    private int m_colIdx;

    /**
     * Don't use an instance created by this constructor until calling
     * loadSettings and configure.
     */
    AttrValueRowFilter() {
        this("", false);
    }

    /**
     * The super class stores the class name and the include flag for all
     * filters checking the attribute value in some way.
     *
     * @param colName the name of the column to test
     * @param include flag indicating whether to include or exclude matching
     *            rows
     */
    protected AttrValueRowFilter(final String colName, final boolean include) {
        if (colName == null) {
            throw new NullPointerException("Column name can't be null");
        }
        m_colName = colName;
        m_colIdx = -1;
        m_include = include;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {

        if (!inSpec.containsName(m_colName)) {
            throw new InvalidSettingsException("Column range filter: "
                    + "Input table doesn't contain specified column name");
        }

        m_colIdx = inSpec.findColumnIndex(m_colName);

        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        m_colName = cfg.getString(CFGKEY_COLNAME);
        m_colIdx = -1;

        if ((m_colName == null) || (m_colName.length() < 1)) {
            throw new InvalidSettingsException("String compare filter: "
                    + "NodeSettings object contains no column name");
        }
        m_include = cfg.getBoolean(CFGKEY_INCLUDE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        cfg.addString(CFGKEY_COLNAME, m_colName);
        cfg.addBoolean(CFGKEY_INCLUDE, m_include);
    }

    /**
     * @return the colName
     */
    public String getColName() {
        return m_colName;
    }

    /**
     * @return the index of the column with the column name. Not valid until
     *         configure is called.
     */
    protected int getColIdx() {
        return m_colIdx;
    }

    /**
     * @return the include
     */
    public boolean getInclude() {
        return m_include;
    }

}
