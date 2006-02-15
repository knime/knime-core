/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   29.06.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row.rowfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * A RowFilter that matches the row ID against a regular expression. It allows
 * for including or excluding matching rows, supports case sensitivity, and
 * supports entire row ID matches vs. starts with.
 * 
 * @author ohl, University of Konstanz
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
     * @param include flag inverting the match if set false
     * @param caseSensitive case ignoring match if set false
     * @param startsWith if false, the entire row ID must match the reg expr, if
     *            set true it only has to start with the reg expr.
     */
    public RowIDRowFilter(final String regExpr, final boolean include,
            final boolean caseSensitive, final boolean startsWith) {

        m_include = include;
        m_startsWith = startsWith;
        m_caseSensitive = caseSensitive;

        try {
            if (caseSensitive) {
                m_pattern = Pattern.compile(regExpr);
            } else {
                m_pattern = Pattern.compile(regExpr, Pattern.CASE_INSENSITIVE);
            }
        } catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException("Error in regular expression ('"
                    + pse.getMessage() + "')");
        }

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
     * @return true if the rowID must start with the regExpr pattern, false if
     *         it only has to start with it
     */
    public boolean getStartsWith() {
        return m_startsWith;
    }

    /**
     * @return true if matching row IDs are included (match method returns true)
     *         or false, if they are excluded (matches method returns false, if
     *         rowID matches the reg expr.)
     */
    public boolean getInclude() {
        return m_include;
    }

    /**
     * @return true if the match is case sensitive, false if not.
     */
    public boolean getCaseSensitivity() {
        return m_caseSensitive;
    }

    /**
     * @see RowFilter#matches(DataRow, int)
     */
    public boolean matches(final DataRow row, final int rowIndex) {
        assert row != null;
        Matcher matcher = m_pattern.matcher(row.getKey().getId().toString());

        boolean match;
        if (m_startsWith) {
            match = matcher.lookingAt();
        } else {
            match = matcher.matches();
        }
        return ((m_include && match) || (!m_include && !match));
    }

    /**
     * @see RowFilter#loadSettingsFrom(NodeSettings)
     */
    public void loadSettingsFrom(final NodeSettings cfg)
            throws InvalidSettingsException {
        m_include = cfg.getBoolean(CFG_INCLUDE);
        m_startsWith = cfg.getBoolean(CFG_STARTSWITH);
        m_caseSensitive = cfg.getBoolean(CFG_CASESENSE);
        String regExpr = cfg.getString(CFG_PATTERN);

        try {
            if (m_caseSensitive) {
                m_pattern = Pattern.compile(regExpr);
            } else {
                m_pattern = Pattern.compile(regExpr, Pattern.CASE_INSENSITIVE);
            }
        } catch (PatternSyntaxException pse) {
            throw new InvalidSettingsException("Error in regular expression"
                    + " (" + regExpr + ") read from config object: '"
                    + pse.getMessage() + "'");
        }

    }

    /**
     * @see RowFilter#saveSettings(NodeSettings)
     */
    protected void saveSettings(final NodeSettings cfg) {
        cfg.addBoolean(CFG_INCLUDE, m_include);
        cfg.addBoolean(CFG_STARTSWITH, m_startsWith);
        cfg.addString(CFG_PATTERN, m_pattern.pattern());
        cfg.addBoolean(CFG_CASESENSE, m_caseSensitive);
    }
    
    /**
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *  #configure(de.unikn.knime.core.data.DataTableSpec)
     */
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        if (m_pattern == null) {
            throw new InvalidSettingsException("RowIDFilter: no pattern set");
        }
        return inSpec;
    }
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
       String result = "ROW-ID-FILTER:";
       result += m_include ? "include " : "exclude ";
       result += "row IDs matching '" + m_pattern.pattern() + "' (";
       result += m_caseSensitive ? "case, " : "nocase, ";
       result += m_startsWith ? "prefixed)" : "entire)";
       return result;
    }

}
