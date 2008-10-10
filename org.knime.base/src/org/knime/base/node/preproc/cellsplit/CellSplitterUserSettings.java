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
 *   Jun 19, 2007 (ohl): created
 *   Oct 06, 2008 (ohl): added missing value/empty cell option
 */
package org.knime.base.node.preproc.cellsplit;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Holds all user settings needed for the cell splitter. Provides methods for
 * saving and a constructor taking a NodeSettingRO object.
 *
 * @author ohl, University of Konstanz
 */
public class CellSplitterUserSettings {

    /**
     * keys to store user settings with.
     */
    private static final String CFG_COLNAME = "colName";

    private static final String CFG_QUOTES = "quotePattern";

    private static final String CFG_REMOVEQUOTES = "removeQuotes";

    private static final String CFG_NUMOFCOLS = "numberOfCols";

    private static final String CFG_GUESSCOLS = "guessNumOfCols";

    private static final String CFG_DELIMITER = "delimiter";

    private static final String CFG_USEEMPTYSTRING = "useEmptyString";

    private String m_columnName = null;

    private String m_delimiter = null;

    private String m_quotePattern = null;

    private boolean m_removeQuotes = false;

    private int m_numOfCols = -1;

    private boolean m_guessNumOfCols = true;

    private boolean m_useEmptyStrings = false;

    /**
     * Creates a new settings object with no (or default) settings.
     */
    CellSplitterUserSettings() {

    }

    /**
     * Creates a new settings object with the value from the specified settings
     * object. If the values in there are incomplete it throws an Exception. The
     * values can be validated (checked for consistency and validity) with the
     * getStatus method.
     *
     *
     * @param settings the config object to read the settings values from
     * @throws InvalidSettingsException if the values in the settings object are
     *             incomplete.
     */
    CellSplitterUserSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName = settings.getString(CFG_COLNAME);
        m_delimiter = settings.getString(CFG_DELIMITER);
        m_guessNumOfCols = settings.getBoolean(CFG_GUESSCOLS);
        m_numOfCols = settings.getInt(CFG_NUMOFCOLS);
        m_quotePattern = settings.getString(CFG_QUOTES);
        m_removeQuotes = settings.getBoolean(CFG_REMOVEQUOTES);

        // the default value is true here for backward compatibility.
        // the node used to create empty cells instead of missing cells.
        m_useEmptyStrings = settings.getBoolean(CFG_USEEMPTYSTRING, true);
    }

    /**
     * Stores the settings values in the specified object.
     *
     * @param settings the config object to save the values in
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_COLNAME, m_columnName);
        settings.addString(CFG_DELIMITER, m_delimiter);
        settings.addBoolean(CFG_GUESSCOLS, m_guessNumOfCols);
        settings.addInt(CFG_NUMOFCOLS, m_numOfCols);
        settings.addString(CFG_QUOTES, m_quotePattern);
        settings.addBoolean(CFG_REMOVEQUOTES, m_removeQuotes);
        settings.addBoolean(CFG_USEEMPTYSTRING, m_useEmptyStrings);
    }

    /**
     * @param spec the spec to check the settings against. Can be null.
     * @return null, if the settings are okay, or an user error message if some
     *         settings are missing or invalid.
     */
    String getStatus(final DataTableSpec spec) {

        if ((m_columnName == null) || (m_columnName.length() == 0)) {
            return "Specify the column to split.";
        }

        if (spec != null) {
            if (!spec.containsName(m_columnName)) {
                return "Input table doesn't contain specified column name.";
            }
            if (!spec.getColumnSpec(m_columnName).getType().isCompatible(
                    StringValue.class)) {
                return "Selected split column must be of type string";
            }
        }
        if ((m_delimiter == null) || (m_delimiter.length() == 0)) {
            return "Specify the delimiter to perform splitting with.";
        }

        if ((m_quotePattern != null) && (m_quotePattern.length() == 0)) {
            return "Specify a quotation character (or disable it).";
        }

        if (!m_guessNumOfCols && (m_numOfCols < 1)) {
            return "Specify the number of columns to add (or enable guessing).";
        }

        if ((m_quotePattern != null)
                && (m_quotePattern.startsWith(m_delimiter) || m_delimiter
                        .startsWith(m_quotePattern))) {
            return "The quote and delimiter can't be the same and can't"
                    + " prefix each other.";
        }

        return null;
    }

    /**
     * @return the columnName
     */
    String getColumnName() {
        return m_columnName;
    }

    /**
     * @param columnName the columnName to set
     */
    void setColumnName(final String columnName) {
        m_columnName = columnName;
    }

    /**
     * @return the delimiter
     */
    String getDelimiter() {
        return m_delimiter;
    }

    /**
     * @param delimiter the delimiter to set
     */
    void setDelimiter(final String delimiter) {
        m_delimiter = delimiter;
    }

    /**
     * @return the guessNumOfCols
     */
    boolean isGuessNumOfCols() {
        return m_guessNumOfCols;
    }

    /**
     * @param guessNumOfCols the guessNumOfCols to set
     */
    void setGuessNumOfCols(final boolean guessNumOfCols) {
        m_guessNumOfCols = guessNumOfCols;
    }

    /**
     * @return the numOfCols
     */
    int getNumOfCols() {
        return m_numOfCols;
    }

    /**
     * @param numOfCols the numOfCols to set
     */
    void setNumOfCols(final int numOfCols) {
        m_numOfCols = numOfCols;
    }

    /**
     * @return the quotePattern
     */
    String getQuotePattern() {
        return m_quotePattern;
    }

    /**
     * @param quotePattern the quotePattern to set
     */
    void setQuotePattern(final String quotePattern) {
        m_quotePattern = quotePattern;
    }

    /**
     * @return the removeQuotes
     */
    boolean isRemoveQuotes() {
        return m_removeQuotes;
    }

    /**
     * @param removeQuotes the removeQuotes to set
     */
    void setRemoveQuotes(final boolean removeQuotes) {
        m_removeQuotes = removeQuotes;
    }

    /**
     * @return true, if an empty string cell is introduced instead of a missing
     *         cell (in case of in missing input cell, or missing split
     *         results).
     */
    boolean isUseEmptyString() {
        return m_useEmptyStrings;
    }

    /**
     * If set to true, the node creates an empty cell in case of a missing input
     * cell or a missing split. Otherwise, if set false, it introduces a missing
     * cell instead.
     *
     * @param useEmptyString set to true to create empty string cells instead of
     *            missing cells.
     */
    void setUseEmptyString(final boolean useEmptyString) {
        m_useEmptyStrings = useEmptyString;
    }

}
