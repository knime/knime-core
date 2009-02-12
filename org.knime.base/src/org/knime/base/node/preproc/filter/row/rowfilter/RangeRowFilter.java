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
 *   04.02.2008 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Filters rows based on the value of a cell in a certain column. It includes
 * (or excludes) rows if their cell value is in a certain range. The type of the
 * column is not checked and not restricted to numerical types - the result of a
 * string range check (for example) might not be as expected though. The filter
 * uses the comparator provided by the column (see
 * {@link org.knime.core.data.DataValueComparator}).
 * <p>
 * NOTE: Before the filter instance is applied it must be configured to find the
 * column index to the specified column name and to set the appropriate
 * comparator.
 *
 * @author ohl, University of Konstanz
 */
public class RangeRowFilter extends AttrValueRowFilter {

    private static final String CFGKEY_LOWERBOUND = "lowerBound";

    private static final String CFGKEY_UPPERBOUND = "upperBound";

    private DataCell m_lowerBound;

    private DataCell m_upperBound;

    /*
     * the following two variables are set during configure
     */
    private DataValueComparator m_comparator;

    /**
     * Creates a filter that compares the value of a data cell in the specified
     * column with the specified range. If the value is in that range the row is
     * included (or excluded, depending on the corresponding flag). It is
     * possible to set only one bound of the range, an open interval is assumed
     * then (an exception will fly, if no bound is set though).
     * <p>
     * NOTE: This filter must be configured before it can be applied (there, the
     * index of the selected column is determined and the comparator is
     * retrieved from the column. The comparator is used to do the range
     * checking).
     *
     * @see #configure(DataTableSpec)
     *
     * @param colName the column name of the cell to match
     * @param include if true, matching rows are included, if false, they are
     *            excluded.
     * @param lowerBound the lower bound of the range the value will be checked
     *            against. Could be null (if the upper bound is not null) which
     *            indicates that there is no lower bound for the range.
     * @param upperBound the upper bound of the range the value will be checked
     *            against. Could be null (if the lower bound is not null) which
     *            indicates that there is no upper bound for the range.
     *
     */
    public RangeRowFilter(final String colName, final boolean include,
            final DataCell lowerBound, final DataCell upperBound) {
        super(colName, include);

        if (lowerBound == null && upperBound == null) {
            throw new NullPointerException("At least one bound of the range"
                    + " must be specified.");
        }

        m_lowerBound = lowerBound;
        m_upperBound = upperBound;
        m_comparator = null;
    }

    /**
     * Don't use created filter without loading settings before.
     */
    RangeRowFilter() {
        super();
        m_lowerBound = null;
        m_upperBound = null;
        m_comparator = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        super.configure(inSpec);
        DataType colType = inSpec.getColumnSpec(getColIdx()).getType();
        if (m_lowerBound != null) {
            if (!colType.isASuperTypeOf(m_lowerBound.getType())) {
                throw new InvalidSettingsException("Column value filter: "
                        + "Specified lower bound of range doesn't fit "
                        + "column type. (Col#:" + getColIdx() + ",ColType:"
                        + colType + ",RangeType:" + m_lowerBound.getType());
            }
        }
        if (m_upperBound != null) {
            if (!colType.isASuperTypeOf(m_upperBound.getType())) {
                throw new InvalidSettingsException("Column value filter: "
                        + "Specified upper bound of range doesn't fit "
                        + "column type. (Col#:" + getColIdx() + ",ColType:"
                        + colType + ",RangeType:" + m_upperBound.getType());
            }
        }

        m_comparator = colType.getComparator();

        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        super.loadSettingsFrom(cfg);
        m_upperBound = cfg.getDataCell(CFGKEY_UPPERBOUND);
        m_lowerBound = cfg.getDataCell(CFGKEY_LOWERBOUND);
        m_comparator = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        super.saveSettings(cfg);
        cfg.addDataCell(CFGKEY_LOWERBOUND, m_lowerBound);
        cfg.addDataCell(CFGKEY_UPPERBOUND, m_upperBound);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        // if one of these goes off, configure was probably not called after
        // loading filter's settings
        assert getColIdx() >= 0;
        assert m_comparator != null;

        DataCell theCell = row.getCell(getColIdx());
        boolean match = false;

        if (theCell.isMissing()) {
            match = false;

        } else {
            if (m_lowerBound != null) {
                match = (m_comparator.compare(m_lowerBound, theCell) <= 0);
            } else {
                // if no lowerBound is specified - its always above the minimum
                match = true;
            }
            if (m_upperBound != null) {
                match &= (m_comparator.compare(theCell, m_upperBound) <= 0);
            }
        }
        return ((getInclude() && match) || (!getInclude() && !match));
    }

    /**
     * @return the lowerBound
     */
    public DataCell getLowerBound() {
        return m_lowerBound;
    }

    /**
     * @return the upperBound
     */
    public DataCell getUpperBound() {
        return m_upperBound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "RangeRowFilter: ColName='" + getColName() + "', LowerBound='"
                + m_lowerBound + "', upperBound='" + m_upperBound
                + "' " + (getInclude() ? " includes" : "excludes") + " rows.";
    }
}
