/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   29.06.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * A {@link RowFilter} that matches the row ID against a regular expression. It
 * allows for including or excluding matching rows, supports case sensitivity,
 * and supports entire row ID matches vs. starts with.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class RowIDRowFilter extends RowFilter {

    private static final String CFG_INCLUDE = "RegExprRowFilterInclude";

    private static final String CFG_STARTSWITH = "RegExprRowFilterStart";

    private static final String CFG_PATTERN = "RegExprRowFilterPattern";

    private static final String CFG_CASESENSE = "RegExprRowFilterCaseSense";

    private boolean m_include;

    private boolean m_startsWith;

    private boolean m_caseSensitive;

    private Pattern m_pattern;

    /**
     * Creates a new RowFilter that matches the row ID against a regular
     * expression.
     *
     * @param regExpr the regular expression
     * @param include flag inverting the match if set <code>false</code>
     * @param caseSensitive case ignoring match if set <code>false</code>
     * @param startsWith if <code>false</code>, the entire row ID must match
     *            the reg expr, if set <code>true</code> it only has to start
     *            with the reg expr
     */
    public RowIDRowFilter(final String regExpr, final boolean include,
            final boolean caseSensitive, final boolean startsWith) {

        m_include = include;
        m_startsWith = startsWith;
        m_caseSensitive = caseSensitive;

        try {
            m_pattern = compileRegExpr(regExpr, caseSensitive);
        } catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException("Error in regular expression ('"
                    + pse.getMessage() + "')");
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
        int flags = Pattern.DOTALL | Pattern.MULTILINE;
        if (!caseSensitive) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return Pattern.compile(regExpr, flags);

    }

    /**
     * The filter created by this contructor matches everything. The settings
     * are ment to be overloaded by settings from a config object.
     */
    public RowIDRowFilter() {
        m_include = true;
        m_startsWith = false;
        m_pattern = Pattern.compile(".*");
        m_caseSensitive = false;
    }

    /**
     * @return the regular expression row IDs are matched against
     */
    public String getRegExpr() {
        return m_pattern.pattern();
    }

    /**
     * @return <code>true</code> if the rowID must start with the regExpr
     *         pattern, <code>false</code> if it only has to start with it
     */
    public boolean getStartsWith() {
        return m_startsWith;
    }

    /**
     * @return <code>true</code> if matching row IDs are included (match
     *         method returns true) or <code>false</code>, if they are
     *         excluded (matches method returns false, if rowID matches the reg
     *         expr.)
     */
    public boolean getInclude() {
        return m_include;
    }

    /**
     * @return <code>true</code> if the match is case sensitive,
     *         <code>false</code> if not
     */
    public boolean getCaseSensitivity() {
        return m_caseSensitive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex) {
        assert row != null;
        Matcher matcher = m_pattern.matcher(row.getKey().getString());

        boolean match;
        if (m_startsWith) {
            match = matcher.lookingAt();
        } else {
            match = matcher.matches();
        }
        return ((m_include && match) || (!m_include && !match));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        m_include = cfg.getBoolean(CFG_INCLUDE);
        m_startsWith = cfg.getBoolean(CFG_STARTSWITH);
        m_caseSensitive = cfg.getBoolean(CFG_CASESENSE);
        String regExpr = cfg.getString(CFG_PATTERN);

        try {
            m_pattern = compileRegExpr(regExpr, m_caseSensitive);
        } catch (PatternSyntaxException pse) {
            throw new InvalidSettingsException("Error in regular expression"
                    + " (" + regExpr + ") read from config object: '"
                    + pse.getMessage() + "'");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        cfg.addBoolean(CFG_INCLUDE, m_include);
        cfg.addBoolean(CFG_STARTSWITH, m_startsWith);
        cfg.addString(CFG_PATTERN, m_pattern.pattern());
        cfg.addBoolean(CFG_CASESENSE, m_caseSensitive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        if (m_pattern == null) {
            throw new InvalidSettingsException("RowIDFilter: no pattern set");
        }
        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "ROW-ID-FILTER:";
        result += m_include ? "include " : "exclude ";
        result += "row IDs matching '" + m_pattern.pattern() + "' (";
        result += m_caseSensitive ? "case, " : "nocase, ";
        result += m_startsWith ? "prefixed)" : "entire)";
        return result;
    }
}
