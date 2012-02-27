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

    private static final String CFG_USEESCAPECHAR = "useEscapeCharacter";

    private String m_columnName = null;

    private String m_delimiter = null;

    private String m_quotePattern = null;

    private boolean m_removeQuotes = false;

    private int m_numOfCols = -1;

    private boolean m_guessNumOfCols = true;

    private boolean m_useEmptyStrings = false;

    private boolean m_useEscapeCharacter = false;

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

        /** @since 2.6 */
        m_useEscapeCharacter = settings.getBoolean(CFG_USEESCAPECHAR, false);
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
        settings.addBoolean(CFG_USEESCAPECHAR, m_useEscapeCharacter);
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

    /**
     * @return if "\" should be used as escape character
     */
    boolean isUseEscapeCharacter() {
        return m_useEscapeCharacter;
    }

    /**
     * @param b <code>true</code> if "\" should be used as escape character
     */
    void setUseEscapeCharacter(final boolean b) {
        m_useEscapeCharacter = b;
    }
}
