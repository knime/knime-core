/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
