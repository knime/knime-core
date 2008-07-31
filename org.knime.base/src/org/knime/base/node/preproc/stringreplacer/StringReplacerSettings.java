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
    }
}
