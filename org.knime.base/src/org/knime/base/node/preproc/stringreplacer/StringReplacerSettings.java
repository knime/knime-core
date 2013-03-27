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
 *   18.06.2007 (thor): created
 */
package org.knime.base.node.preproc.stringreplacer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the string replacer node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class StringReplacerSettings {
    private boolean m_caseSensitive;

    private String m_colName;

    private boolean m_createNewCol;

    private String m_newColName;

    private String m_pattern;

    private boolean m_replaceAllOccurrences;

    private String m_replacement;

    /** @since 2.8 */
    private boolean m_enableEscaping;
    /** @since 2.8 */
    private boolean m_patternIsRegex;


    /**
     * Returns whether the pattern is a regular expression or a simple wildcard pattern.
     *
     * @return <code>true</code> if it is a regular expression, <code>false</code> if it contains wildcards
     * @since 2.8
     */
    public boolean patternIsRegex() {
        return m_patternIsRegex;
    }

    /**
     * Sets whether the pattern is a regular expression or a simple wildcard pattern.
     *
     * @param regex <code>true</code> if it is a regular expression, <code>false</code> if it contains wildcards
     * @since 2.8
     */
    public void patternIsRegex(final boolean regex) {
        m_patternIsRegex = regex;
    }


    /**
     * Returns whether escaping via a backslash is enabled.
     *
     * @return <code>true</code> if the backslash is an escape character, <code>false</code> otherwise
     * @since 2.8
     */
    public boolean enableEscaping() {
        return m_enableEscaping;
    }

    /**
     * Sets whether escaping via a backslash is enabled.
     *
     * @param enable <code>true</code> if the backslash is an escape character, <code>false</code> otherwise
     * @since 2.8
     */
    public void enableEscaping(final boolean enable) {
        m_enableEscaping = enable;
    }

    /**
     * Returns if the pattern should match case sensitive or not.
     *
     * @return <code>true</code> if the matches should be case sensitive,
     *         <code>false</code> otherwise
     */
    public boolean caseSensitive() {
        return m_caseSensitive;
    }

    /**
     * Sets if the pattern should match case sensitive or not.
     *
     * @param b <code>true</code> if the matches should be case sensitive,
     *            <code>false</code> otherwise
     */
    public void caseSensitive(final boolean b) {
        m_caseSensitive = b;
    }

    /**
     * Returns the name of the column that should be processed.
     *
     * @return the column's name
     */
    public String columnName() {
        return m_colName;
    }

    /**
     * Sets the name of the column that should be processed.
     *
     * @param colName the column's name
     */
    public void columnName(final String colName) {
        m_colName = colName;
    }

    /**
     * Returns if a new column should be created instead of overriding the
     * values in the target column.
     *
     * @return <code>true</code> if a new column should be created,
     *         <code>false</code> otherwise
     * @see #newColumnName()
     */
    public boolean createNewColumn() {
        return m_createNewCol;
    }

    /**
     * Sets if a new column should be created instead of overriding the values
     * in the target column.
     *
     * @param b <code>true</code> if a new column should be created,
     *            <code>false</code> otherwise
     * @see #newColumnName(String)
     */
    public void createNewColumn(final boolean b) {
        m_createNewCol = b;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings node settings
     * @throws InvalidSettingsException if settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_caseSensitive = settings.getBoolean("caseSensitive");
        m_colName = settings.getString("colName");
        m_createNewCol = settings.getBoolean("createNewCol");
        m_newColName = settings.getString("newColName");
        m_pattern = settings.getString("pattern");
        m_replaceAllOccurrences = settings.getBoolean("replaceAllOccurences");
        m_replacement = settings.getString("replacement");

        /** @since 2.8 */
        m_enableEscaping = settings.getBoolean("enableEscaping", false);
        m_patternIsRegex = settings.getBoolean("patternIsRegex", false);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_caseSensitive = settings.getBoolean("caseSensitive", false);
        m_colName = settings.getString("colName", null);
        m_createNewCol = settings.getBoolean("createNewCol", false);
        m_newColName = settings.getString("newColName", null);
        m_pattern = settings.getString("pattern", "");
        m_replaceAllOccurrences =
                settings.getBoolean("replaceAllOccurences", false);
        m_replacement = settings.getString("replacement", "");
        m_enableEscaping = settings.getBoolean("enableEscaping", false);
        m_patternIsRegex = settings.getBoolean("patternIsRegex", false);
    }

    /**
     * Returns the name of the new column.
     *
     * @return the new column's name
     * @see #createNewColumn()
     */
    public String newColumnName() {
        return m_newColName;
    }

    /**
     * Sets the name of the new column.
     *
     * @param colName the new column's name
     * @see #createNewColumn(boolean)
     */
    public void newColumnName(final String colName) {
        m_newColName = colName;
    }

    /**
     * Returns the pattern.
     *
     * @return the pattern
     */
    public String pattern() {
        return m_pattern;
    }

    /**
     * Sets the pattern.
     *
     * @param pattern the pattern
     */
    public void pattern(final String pattern) {
        m_pattern = pattern;
    }

    /**
     * Returns if the whole string or all occurrences of the pattern should be
     * replaced.
     *
     * @return <code>true</code> if all occurrences should be replaced,
     *         <code>false</code> if the whole string should be replaced
     *
     */
    public boolean replaceAllOccurrences() {
        return m_replaceAllOccurrences;
    }

    /**
     * Sets if the whole string or all occurrences of the pattern should be
     * replaced.
     *
     * @param b <code>true</code> if all occurrences should be replaced,
     *         <code>false</code> if the whole string should be replaced
     *
     */
    public void replaceAllOccurrences(final boolean b) {
        m_replaceAllOccurrences = b;
    }

    /**
     * Returns the replacement text.
     *
     * @return the replacement text
     */
    public String replacement() {
        return m_replacement;
    }

    /**
     * Sets the replacement text.
     *
     * @param replacement the replacement text
     */
    public void replacement(final String replacement) {
        m_replacement = replacement;
    }

    /**
     * Save the settings into the node settings object.
     *
     * @param settings node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean("caseSensitive", m_caseSensitive);
        settings.addString("colName", m_colName);
        settings.addBoolean("createNewCol", m_createNewCol);
        settings.addString("newColName", m_newColName);
        settings.addString("pattern", m_pattern);
        settings.addBoolean("replaceAllOccurences", m_replaceAllOccurrences);
        settings.addString("replacement", m_replacement);
        settings.addBoolean("enableEscaping", m_enableEscaping);
        settings.addBoolean("patternIsRegex", m_patternIsRegex);
    }
}
