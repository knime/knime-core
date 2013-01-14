/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   31.01.2008 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Filters rows based on the string representation of the value in a certain
 * column. The type of the column is not checked and not restricted to the type
 * of {@link org.knime.core.data.StringValue}. The filter uses the String
 * representation of the value (by calling
 * {@link org.knime.core.data.DataCell#toString()})
 * <p>
 * NOTE: Before the filter instance is applied it must be configured to find the
 * column index to the specified column name.
 *
 * @author ohl, University of Konstanz
 */
public class StringCompareRowFilter extends AttrValueRowFilter {

    public static final String CFGKEY_PATTERN = "Pattern";

    private static final String CFGKEY_CASE = "CaseSensitive";

    private static final String CFGKEY_WILD = "hasWildCards";

    private static final String CFGKEY_REGEXPR = "isRegExpr";

    private String m_pattern;

    private Pattern m_regExpr;

    private boolean m_caseSensitive;

    private boolean m_hasWildcards;

    private boolean m_isRegExpr;

    /**
     * Creates a row filter that compares the string representation of the cell
     * in the specified column with the given string pattern. Matching rows are
     * included or excluded, depending on the corresponding argument. The
     * pattern will either be matched exactly, or case insensitive, or may
     * contain wildcards. A wildcard is the asterisk (*) matching any number
     * (including zero) of any character, and the question mark (?) matching any
     * (but exactly one) character. Cells with missing values never match!
     *
     *
     * @param strPattern the pattern that is matched against the string
     *            representation of the data cell
     * @param colName the column name of the cell to match
     * @param include if true, matching rows are included, if false, they are
     *            excluded.
     * @param caseSensitive if true a case sensitive match is performed,
     *            otherwise characters of different case match, too.
     * @param hasWildcards if true, '*' and '?' is interpreted as wildcard
     *            matching any character sequence or any character respectively.
     *            If false, '*' and '?' are treated as regular characters and
     *            match '*' and '?' in the value.
     * @param isRegExpr if true, the pattern argument is treated as regular
     *            expression. Can't be true when the hasWildcard argument is
     *            true
     *
     */
    public StringCompareRowFilter(final String strPattern,
            final String colName, final boolean include,
            final boolean caseSensitive, final boolean hasWildcards,
            final boolean isRegExpr) {
        super(colName, include);
        if (strPattern == null) {
            throw new NullPointerException("Pattern to match can't be null.");
        }
        if (hasWildcards && isRegExpr) {
            throw new IllegalArgumentException("Arguments hasWildcards and"
                    + " isRegExpr can't be true at the same time");
        }
        m_pattern = strPattern;
        m_caseSensitive = caseSensitive;
        m_hasWildcards = hasWildcards;
        m_isRegExpr = isRegExpr;

        compileRegularExpression();

    }

    /**
     * Don't use created filter without loading settings before.
     */
    StringCompareRowFilter() {
        super();
        m_pattern = null;
        m_regExpr = null;
        m_caseSensitive = false;
        m_hasWildcards = false;
    }

    /**
     * Evaluates the flags hasWildcards and isRegExpression and compiles the
     * provided pattern into a {@link Pattern} - if needed. If the corresponding
     * member is null after a call to this method, use the pattern directly for
     * a simple string compare.
     */
    private void compileRegularExpression() {

        // can't be both!
        assert !(m_hasWildcards && m_isRegExpr);

        String regExprToUse = null;

        if (m_hasWildcards) {
            regExprToUse = WildcardMatcher.wildcardToRegex(m_pattern);
        } else if (m_isRegExpr) {
            regExprToUse = m_pattern;
        } else {
            regExprToUse = null;
        }

        if (regExprToUse != null) {
            // allow - and match - LF and international chars in the data
            int flags =
                Pattern.DOTALL | Pattern.MULTILINE;
            if (!m_caseSensitive) {
                flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }
            m_regExpr = Pattern.compile(regExprToUse, flags);
        } else {
            m_regExpr = null;
        }

        /*
         * if m_regExpr is not null, use it for comparison - otherwise use the
         * m_pattern string directly.
         */

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        super.loadSettingsFrom(cfg);
        m_caseSensitive = cfg.getBoolean(CFGKEY_CASE);
        m_hasWildcards = cfg.getBoolean(CFGKEY_WILD);
        m_isRegExpr = cfg.getBoolean(CFGKEY_REGEXPR);
        m_pattern = cfg.getString(CFGKEY_PATTERN);

        if ((m_pattern == null) || (m_pattern.isEmpty())) {
            throw new InvalidSettingsException("String compare filter: "
                    + "NodeSettings object contains invalid (empty) pattern");
        }
        if (m_isRegExpr && m_hasWildcards) {
            throw new InvalidSettingsException("hasWildcards and"
                    + " isRegExpr can't be true at the same time");
        }

        compileRegularExpression();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        super.saveSettings(cfg);
        cfg.addBoolean(CFGKEY_CASE, m_caseSensitive);
        cfg.addString(CFGKEY_PATTERN, m_pattern);
        cfg.addBoolean(CFGKEY_WILD, m_hasWildcards);
        cfg.addBoolean(CFGKEY_REGEXPR, m_isRegExpr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        // if this goes off, configure was probably not called after
        // loading filter's settings
        assert getColIdx() >= 0;

        DataCell theCell = row.getCell(getColIdx());
        boolean match = false;

        if (theCell.isMissing()) {
            match = false;

        } else if (m_regExpr != null) {
            // if we have a regular expression - use it
            Matcher matcher = m_regExpr.matcher(theCell.toString());
            match = matcher.matches();
        } else {
            // otherwise use the string pattern directly
            if (m_caseSensitive) {
                match = m_pattern.equals(theCell.toString());
            } else {
                match = m_pattern.equalsIgnoreCase(theCell.toString());
            }

        }
        return ((getInclude() && match) || (!getInclude() && !match));
    }

    /*
     * getter methods for parameters
     */

    /**
     * @return the caseSensitive
     */
    public boolean getCaseSensitive() {
        return m_caseSensitive;
    }

    /**
     * @return the hasWildcards
     */
    public boolean getHasWildcards() {
        return m_hasWildcards;
    }

    /**
     * @return the isRegExpr
     */
    public boolean getIsRegExpr() {
        return m_isRegExpr;
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return m_pattern;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "StringCompareFilter: ColName='" + getColName() + "', Pattern='"
                + m_pattern + "', CaseSensitive=" + m_caseSensitive
                + ", hasWildCards=" + m_hasWildcards + ", isRegExpr="
                + m_isRegExpr + (getInclude() ? " includes" : "excludes")
                + " rows.";
    }

}
