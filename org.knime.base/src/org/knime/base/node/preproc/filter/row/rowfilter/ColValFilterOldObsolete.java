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
 *   19.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A filter selecting rows depending on the content of a column (or cell of the
 * row). The cell content can either be matched against a regular expression or
 * compared to a given range. Matches and rows inside the range can either be
 * included or excluded. RegExpr match can be done case sensitive or
 * insensitive.
 *
 * @author Peter Ohl, University of Konstanz
 * @deprecated This filter contains too much functionality (which can be used
 *             only one at a time anyway). We have split it into three filter
 *             classes: {@link MissingValueRowFilter},
 *             {@link StringCompareRowFilter}, {@link RangeRowFilter}.<br>
 *             This filter is used by the previous row filter node
 *             implementation. If you want to remove it, you must move the old
 *             code in the deprecated package.
 */
@Deprecated
public class ColValFilterOldObsolete extends RowFilter {

    private static final String CFG_COLNAME = "ColValRowFilterColName";

    private static final String CFG_INCLUDE = "ColValRowFilterInclude";

    private static final String CFG_STARTSWITH = "ColValRowFilterStart";

    private static final String CFG_PATTERN = "ColValRowFilterPattern";

    private static final String CFG_CASESENSE = "ColValRowFilterCaseSense";

    private static final String CFG_UPPERBOUND = "ColValRowFilterUpperBound";

    private static final String CFG_LOWERBOUND = "ColValRowFilterLowerBound";

    private static final String CFG_FILTERMISSING = "ColValFilterMissValues";

    private boolean m_include;

    private int m_colIndex;

    private String m_colName;

    /*
     * variables for the regExpr matching
     */
    private Pattern m_pattern;

    private boolean m_caseSensitive;

    private boolean m_startsWith;

    /*
     * variables for the range matching
     */

    private DataCell m_lowerBound;

    private DataCell m_upperBound;

    private DataValueComparator m_dcComp;

    /*
     * variables for missing value filtering
     */
    private boolean m_filterMissingValues = false;

    /**
     * Creates a new filter which matches the string representation of the
     * specified column against a given regular expression.
     *
     * @param regExpr a valid regular expression; causes an exception to fly if
     *            its not a valid reg expr
     * @param colName the name of the column to test in the row
     * @param include flag indicating whether to include or exclude rows with a
     *            matching value
     * @param caseSensitive if true the match will be done case sensitive, or if
     *            false case insensitive
     * @param startsWith determines whether the value must match entirely the
     *            regular expression or can only start with it.
     * @throws IllegalArgumentException if the pattern passed as regular
     *             expression is not a valid regExpr
     */
    public ColValFilterOldObsolete(final String regExpr, final String colName,
            final boolean include, final boolean caseSensitive,
            final boolean startsWith) {
        // clear all variables
        this();

        m_colIndex = -1;
        m_colName = colName;
        m_include = include;
        m_startsWith = startsWith;
        m_caseSensitive = caseSensitive;

        try {
            m_pattern = compileRegExpr(regExpr, caseSensitive);
        } catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException("Error in regular expression ('"
                    + pse.getMessage() + "')");
        }
        if ((colName == null) || (colName.length() == 0)) {
            throw new IllegalArgumentException("Column value filter: "
                    + "the column name must be provided and not emtpy.");
        }

    }

    /**
     * We need to compile the pattern in two places (in the constructor and in
     * loadSettings). Both places call this method.
     *
     * @param regExpr the pattern to compile.
     * @param caseSensitive if true, matching is case sensitive
     * @return the RegExprMachine compiled from the reg expr passed
     * @throws PatternSyntaxException if the pattern is invalid
     */
    private Pattern compileRegExpr(final String regExpr,
            final boolean caseSensitive) throws PatternSyntaxException {
        // support \n in the data and weird international characters.
        int flags = Pattern.DOTALL | Pattern.UNICODE_CASE | Pattern.MULTILINE;
        if (!caseSensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return Pattern.compile(regExpr, flags);

    }

    /**
     * Creates a new filter that tests the value of the specified cell against
     * the specified range.
     *
     * @param comp the comparator that will be used to compare the column's cell
     *            with the bounds of the range.
     * @param lowerBound if the comparator doesn't return a negative number when
     *            the cell is compared with this value, the cell is above the
     *            lower bound. If <code>null</code> no minimum is set.
     * @param upperBound if the comparator doesn't return a positive number when
     *            the cell is compared with this value, the cell is below the
     *            upper bound. If <code>null</code>, no maximum is set.
     * @param colName the name of the column to test in the row
     * @param include determines whether to include or exclude rows with a value
     *            inside the range
     */
    public ColValFilterOldObsolete(final DataValueComparator comp,
            final DataCell lowerBound, final DataCell upperBound,
            final String colName, final boolean include) {
        // clear all variables
        this();

        m_include = include;
        m_colIndex = -1;
        m_colName = colName;

        m_dcComp = comp;
        m_lowerBound = lowerBound;
        m_upperBound = upperBound;

        if (comp == null) {
            throw new IllegalArgumentException("Column value filter: "
                    + "the datacell comparator must not be null.");
        }
        if ((upperBound == null) && (lowerBound == null)) {
            throw new IllegalArgumentException("Column value filter: "
                    + "only one of the range bounds can be null.");
        }
        if ((upperBound != null) && (lowerBound != null)) {
            if (comp.compare(m_lowerBound, m_upperBound) > 0) {
                throw new IllegalArgumentException(
                        "Column value filter: the lower bound of the range "
                                + "must not be larger than the upper bound");
            }
        }
        if ((colName == null) || (colName.length() == 0)) {
            throw new IllegalArgumentException("Column value filter: "
                    + "the column name must provided and not emtpy");
        }
    }

    /**
     * Creates a new filter that filters missing values of the specified column.
     *
     * @param colName the name of the column to test the values of
     * @param include if true, rows with a missing value in the specified column
     *            are included, otherwise excluded
     */
    public ColValFilterOldObsolete(final String colName,
            final boolean include) {
        m_include = include;
        m_colName = colName;
        m_colIndex = -1;
        m_filterMissingValues = true;
    }

    /**
     * Default constructor. Don't use without loading settings before.
     */
    ColValFilterOldObsolete() {
        m_include = false;
        m_colIndex = -1;
        m_colName = null;
        m_pattern = null;
        m_caseSensitive = false;
        m_startsWith = false;
        m_lowerBound = null;
        m_upperBound = null;
        m_dcComp = null;
    }

    /**
     * Sets a new comparator used to check the range if lower and upper bounds
     * are set. This MUST be called after settings have been loaded from a
     * config object and upper/lower bounds were set. It is always save to set a
     * comparator. Its only used if at least one bound is set.
     *
     * @param dcComp the comparator used to compare the column's value with the
     *            upper and lower range
     */
    public void setDataValueComparator(final DataValueComparator dcComp) {
        m_dcComp = dcComp;
    }

    /**
     * @return <code>true</code> if rows inside the specified range or
     *         matching the specified regular expression will be included,
     *         <code>false</code> if they are excluded
     */
    public boolean includeMatchingLines() {
        return m_include;
    }

    /**
     * @return the index of the column whose value is tested
     */
    public String getColumnName() {
        return m_colName;
    }

    /**
     * @return true if rows with missing values in the specified attribute are
     *         filtered.
     */
    public boolean getFilterMissingValues() {
        return m_filterMissingValues;
    }

    /**
     * @return true if the range of the column is tested
     */
    public boolean rangeSet() {
        // if we have a comparator and at least one bound is specified
        return ((m_lowerBound != null) || (m_upperBound != null));
    }

    /**
     * @return the lower bound of the range the value of the specified column is
     *         tested against. <code>null</code> if no range testing is
     *         happening or if no minimum is set
     */
    public DataCell getLowerBound() {
        if (rangeSet()) {
            return m_lowerBound;
        } else {
            return null;
        }
    }

    /**
     * @return the upper bound of the range the value of the specified column is
     *         tested against. <code>null</code> if no range testing is
     *         happening or if no maximum is set
     */
    public DataCell getUpperBound() {
        if (rangeSet()) {
            return m_upperBound;
        } else {
            return null;
        }
    }

    /**
     * @return <code>true</code> if the string representation of the column's
     *         value is tested against a regular expression
     */
    public boolean testingStringPattern() {
        return (m_pattern != null);
    }

    /**
     * @return the reg expr the column's value is tested against, or
     *         <code>null</code> if no pattern matching is happening
     */
    public String getRegExpr() {
        if (m_pattern != null) {
            return m_pattern.pattern();
        } else {
            return null;
        }
    }

    /**
     * @return <code>true</code> if the value pattern must entirely match the
     *         reg expr, <code>false</code> if it can also only start with the
     *         expression
     */
    public boolean mustEntirelyMatch() {
        return !m_startsWith;
    }

    /**
     * @return <code>true</code> if the reg expr match is done in a case
     *         sensitive way
     */
    public boolean caseSensitiveMatch() {
        return m_caseSensitive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {

        /*
         * if this goes off you probably didn't set a comparator after loading a
         * range from a config object. Which is not good.
         */
        assert (((m_lowerBound == null) && (m_upperBound == null))
                || m_dcComp != null);

        DataCell theCell = row.getCell(m_colIndex);
        boolean match = false;

        if (theCell.isMissing()) {
            // missing cells never match any range or boundaries - only when
            // we filter missing values we may return true
            match = m_filterMissingValues;
        } else if (rangeSet() && (m_dcComp != null)) {
            // do range checking if we have a comparator and at least one bound
            if (m_lowerBound != null) {
                match = (m_dcComp.compare(m_lowerBound, theCell) <= 0);
            } else {
                // if no lowerBound is specified - its always above the minimum
                match = true;
            }
            if (m_upperBound != null) {
                match &= (m_dcComp.compare(theCell, m_upperBound) <= 0);
            }
        } else if (m_pattern != null) {

            Matcher matcher = m_pattern.matcher(theCell.toString());
            if (m_startsWith) {
                match = matcher.lookingAt();
            } else {
                match = matcher.matches();
            }
        }
        return ((m_include && match) || (!m_include && !match));
    }

    /**
     * A comparator MUST be set if a range is specified in the config object!!
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {

        m_colName = cfg.getString(CFG_COLNAME);
        m_colIndex = -1;
        if ((m_colName == null) || (m_colName.length() < 1)) {
            throw new InvalidSettingsException("Column value filter: "
                    + "NodeSettings object contains invalid column name");
        }
        m_include = cfg.getBoolean(CFG_INCLUDE);

        m_caseSensitive = cfg.getBoolean(CFG_CASESENSE);
        m_startsWith = cfg.getBoolean(CFG_STARTSWITH);
        String regExpr = cfg.getString(CFG_PATTERN, null);
        if (regExpr != null) {
            try {
                m_pattern = compileRegExpr(regExpr, m_caseSensitive);
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Column value filter: "
                        + "NodeSettings object contains invalid reg expr.");
            }
        } else {
            m_pattern = null;
        }

        m_lowerBound = cfg.getDataCell(CFG_LOWERBOUND, null);
        m_upperBound = cfg.getDataCell(CFG_UPPERBOUND, null);

        // we introduce this feature with ver1.2, have a default for compatib.
        m_filterMissingValues = cfg.getBoolean(CFG_FILTERMISSING, false);

        if ((m_lowerBound == null) && (m_upperBound == null)
                && (m_pattern == null) && !m_filterMissingValues) {
            throw new InvalidSettingsException("Column value filter: "
                    + "NodeSettings object contains no matching criteria");
        }

        if ((m_pattern != null) && (m_filterMissingValues)) {
            throw new InvalidSettingsException("Can't filter missing values"
                    + " and do pattern matching at the same time.");
        }
        if (m_filterMissingValues
                && ((m_lowerBound != null) || (m_upperBound != null))) {
            throw new InvalidSettingsException("Can't filter missing values"
                    + " and do range checking at the same time");
        }
        if ((m_pattern != null)
                && ((m_lowerBound != null) || (m_upperBound != null))) {
            throw new InvalidSettingsException("Column value filter: "
                    + "Invalid NodeSettings object; "
                    + "can't match range and pattern.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        cfg.addBoolean(CFG_INCLUDE, m_include);
        cfg.addString(CFG_COLNAME, m_colName);
        if (m_pattern == null) {
            cfg.addString(CFG_PATTERN, null);
        } else {
            cfg.addString(CFG_PATTERN, m_pattern.pattern());
        }
        cfg.addBoolean(CFG_CASESENSE, m_caseSensitive);
        cfg.addBoolean(CFG_STARTSWITH, m_startsWith);
        cfg.addDataCell(CFG_LOWERBOUND, m_lowerBound);
        cfg.addDataCell(CFG_UPPERBOUND, m_upperBound);
        cfg.addBoolean(CFG_FILTERMISSING, m_filterMissingValues);
    }

    /**
     * The column value filter grabs the comparator from the table spec (if
     * available) and checks settings against the latest spec.
     *
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {

        if ((inSpec == null) || (inSpec.getNumColumns() <= 0)) {
            m_dcComp = null;
            return null;
        }
        if ((m_colName == null) || (m_colName.length() < 1)) {
            throw new InvalidSettingsException("Column value filter: "
                    + "No column specified");
        }
        if (!inSpec.containsName(m_colName)) {
            throw new InvalidSettingsException("Column value filter: "
                    + "Input table doesn't contain specified column name");
        }

        m_colIndex = inSpec.findColumnIndex(m_colName);

        DataType colType = inSpec.getColumnSpec(m_colIndex).getType();
        if (m_lowerBound != null) {
            if (!colType.isASuperTypeOf(m_lowerBound.getType())) {
                throw new InvalidSettingsException("Column value filter: "
                        + "Specified lower bound of range doesn't fit "
                        + "column type. (Col#:" + m_colIndex + ",ColType:"
                        + colType + ",RangeType:" + m_lowerBound.getType());
            }
        }
        if (m_upperBound != null) {
            if (!colType.isASuperTypeOf(m_upperBound.getType())) {
                throw new InvalidSettingsException("Column value filter: "
                        + "Specified upper bound of range doesn't fit "
                        + "column type. (Col#:" + m_colIndex + ",ColType:"
                        + colType + ",RangeType:" + m_upperBound.getType());
            }
        }

        m_dcComp = colType.getComparator();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "ColVal-Filter: Col# " + m_colIndex;
        result += m_include ? ", include " : ", exclude ";
        if (testingStringPattern()) {
            result += " pattern matching '" + m_pattern.pattern() + "' (";
            result += m_caseSensitive ? "case, " : "nocase, ";
            result += m_startsWith ? "prefix)" : "entire)";
        }
        if (rangeSet()) {
            result += " values from '";
            result +=
                    (m_lowerBound == null) ? "<open>" : m_lowerBound.toString();
            result += "' to '";
            result +=
                    (m_upperBound == null) ? "<open>" : m_upperBound.toString();
            result += "'";
            if (m_dcComp == null) {
                result += " NO COMPARATOR SET!!!";
            }
        }
        if (m_filterMissingValues) {
            result += " missing values.";
        }
        return result;
    }
}
