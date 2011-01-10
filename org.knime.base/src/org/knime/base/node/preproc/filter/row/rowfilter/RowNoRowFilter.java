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
 * Row filter that includes or excludes a certain range of rows. It will throw a
 * {@link EndOfTableException} if
 * the row number is beyond the include range. An EOT constant is available to
 * use for the range-end-parameter.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class RowNoRowFilter extends RowFilter {

    /**
     * Use this value for the range-end parameter to specify a range reaching to
     * the end of the table.
     */
    public static final int EOT = -1;

    private static final String CFG_START = "RowRangeStart";

    private static final String CFG_END = "RowRangeEnd";

    private static final String CFG_INCL = "RowRangeInclude";

    private int m_start;

    private int m_end;

    private boolean m_include;

    /**
     * Creates a new row filter filtering out all rows (not) included in the
     * specified range. The range includes the specified row numbers. With the
     * include flag specify if rows should be forwarded that are inside or
     * outside the range.
     *
     * @param rangeStart the row index of the first row to (not) match. Must be
     *            a number greater than or equal to zero.
     * @param rangeEnd the row number of the last row to (not) match. Must be a
     *            number greater than the value of <code>rangeStart</code>,
     *            or EOT to indicate a range reaching to the end of the orig
     *            table.
     * @param include flag indicating whether to match or not to match rows
     *            inside the specified range.
     */
    public RowNoRowFilter(final int rangeStart, final int rangeEnd,
            final boolean include) {
        if (rangeStart < 0) {
            throw new IllegalArgumentException("The RowNumberFilter range "
                    + "cannot start at a row number less than 0.");
        }
        if ((rangeEnd != EOT) && (rangeEnd < rangeStart)) {
            throw new IllegalArgumentException("The end of the RowNumberFilter"
                    + " range must be greater than the start.");
        }

        m_start = rangeStart;
        m_end = rangeEnd;
        m_include = include;
    }

    /**
     * Default constructor used by the row filter factory. Don't use it without
     * loading settings.
     */
    public RowNoRowFilter() {
        this(0, EOT, false);
    }

    /**
     * @return the row number of the first row of the filtered range
     */
    public int getFirstRow() {
        return m_start;
    }

    /**
     * @return the row number of the last row of the filtered range
     */
    public int getLastRow() {
        return m_end;
    }

    /**
     * @return <code>true</code> if rows in the range are included (match) or
     *         excluded (won't match)
     */
    public boolean getInclude() {
        return m_include;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {

        if (m_include) {
            if (m_end == EOT) {
                // include everything greater than start (til the end of table)
                if (rowIndex >= m_start) {
                    throw new IncludeFromNowOn();
                } else {
                    return false;
                }
            } else {
                // here we must look at start and end numbers
                if (rowIndex < m_start) {
                    return false;
                } else if (rowIndex <= m_end) {
                    return true;
                } else {
                    // we are beyond the end
                    throw new EndOfTableException();
                }
            }
        } else {
            // m_include is false
            if (m_end != EOT) {
                if (rowIndex < m_start) {
                    return true;
                } else if (rowIndex <= m_end) {
                    return false;
                } else {
                    // we are beyond the exclude range; include the rest
                    throw new IncludeFromNowOn();
                }
            } else {
                // the exclude range goes til the end - flag if we reached the
                // end of our include range
                if (rowIndex < m_start) {
                    return true;
                } else {
                    throw new EndOfTableException();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        m_start = cfg.getInt(CFG_START);
        m_end = cfg.getInt(CFG_END);
        m_include = cfg.getBoolean(CFG_INCL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        cfg.addInt(CFG_START, m_start);
        cfg.addInt(CFG_END, m_end);
        cfg.addBoolean(CFG_INCL, m_include);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        if (m_start < 0) {
            throw new IllegalArgumentException("RowNumberFilter: range "
                    + "start is less than 0.");
        }
        if ((m_end != EOT) && (m_end < m_start)) {
            throw new IllegalArgumentException("RowNumberFilter: range"
                    + "start is larger than range end.");
        }
        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "ROWNo-RANGE-Filter: ";
        result += m_include ? "include " : "exclude ";
        result += "from " + m_start + " to";
        result += (m_end == RowNoRowFilter.EOT) ? " EOT" : " " + m_end;
        return result;
    }
}
