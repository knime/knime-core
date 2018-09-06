/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.base.node.preproc.cellsplit2;

import org.apache.commons.lang.StringEscapeUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 * Holds all user settings needed for the cell splitter. Provides methods for saving and a constructor taking a
 * NodeSettingRO object.
 * <p>
 * Note: This class replaces the (deprecated) CellSplitterUserSettings.
 * </p>
 *
 * @author ohl, University of Konstanz
 */
class CellSplitter2UserSettings {

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

    private static final String CFG_OUTPUTASLIST = "outputAsList";

    private static final String CFG_OUTPUTASSET = "outputAsSet";

    private static final String CFG_OUTPUTASCOLS = "outputAsColumns";

    private static final String CFG_TRIM = "removeWhitespaces";

    private static final String CFG_SPLIT_COLUMN_NAMES = "splitColumnNames";

    private static final String CFG_HAS_SCAN_LIMIT = "hasScanLimit";

    private static final String CFG_SCAN_LIMIT = "scanLimit";

    private static final String CFG_REMOVE_INPUT_COL = "removeInputColumn";

    private String m_columnName = null;

    private String m_delimiter = null;

    private String m_quotePattern = null;

    private boolean m_removeQuotes = false;

    private int m_numOfCols = -1;

    private boolean m_guessNumOfCols = true;

    private boolean m_useEmptyStrings = false;

    private boolean m_useEscapeCharacter = false;

    private boolean m_outputAsList = false;

    private boolean m_outputAsSet = false;

    private boolean m_outputAsCols = true;

    private boolean m_trim = true;

    private boolean m_splitColumnNames = false;

    private boolean m_hasScanLimit = false;

    private int m_scanLimit = 50;

    private boolean m_removeInputColumn = false;

    /**
     * Creates a new settings object with no (or default) settings.
     */
    CellSplitter2UserSettings() {
        /* empty */
    }

    /**
     * Creates a new settings object with the value from the specified settings object. If the values in there are
     * incomplete it throws an Exception. The values can be validated (checked for consistency and validity) with the
     * getStatus method.
     *
     *
     * @param settings the config object to read the settings values from
     * @throws InvalidSettingsException if the values in the settings object are incomplete.
     */
    CellSplitter2UserSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnName = settings.getString(CFG_COLNAME);
        m_delimiter = settings.getString(CFG_DELIMITER);
        m_guessNumOfCols = settings.getBoolean(CFG_GUESSCOLS);
        m_numOfCols = settings.getInt(CFG_NUMOFCOLS);
        m_quotePattern = settings.getString(CFG_QUOTES);
        m_removeQuotes = settings.getBoolean(CFG_REMOVEQUOTES);

        // the default value is true here for backward compatibility.
        // the node used to create empty cells instead of missing cells.
        m_useEmptyStrings = settings.getBoolean(CFG_USEEMPTYSTRING, true);

        m_useEscapeCharacter = settings.getBoolean(CFG_USEESCAPECHAR, false);

        m_outputAsList = settings.getBoolean(CFG_OUTPUTASLIST, false);
        m_outputAsSet = settings.getBoolean(CFG_OUTPUTASSET, false);
        m_outputAsCols = settings.getBoolean(CFG_OUTPUTASCOLS, true);
        m_trim = settings.getBoolean(CFG_TRIM, true);

        m_splitColumnNames = settings.getBoolean(CFG_SPLIT_COLUMN_NAMES, false);

        m_hasScanLimit = settings.getBoolean(CFG_HAS_SCAN_LIMIT, true);
        m_scanLimit = settings.getInt(CFG_SCAN_LIMIT, 50);

        m_removeInputColumn = settings.getBoolean(CFG_REMOVE_INPUT_COL, false);
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
        settings.addBoolean(CFG_OUTPUTASLIST, m_outputAsList);
        settings.addBoolean(CFG_OUTPUTASSET, m_outputAsSet);
        settings.addBoolean(CFG_OUTPUTASCOLS, m_outputAsCols);
        settings.addBoolean(CFG_TRIM, m_trim);

        settings.addBoolean(CFG_SPLIT_COLUMN_NAMES, m_splitColumnNames);

        settings.addBoolean(CFG_HAS_SCAN_LIMIT, m_hasScanLimit);
        settings.addInt(CFG_SCAN_LIMIT, m_scanLimit);

        settings.addBoolean(CFG_REMOVE_INPUT_COL, m_removeInputColumn);
    }

    /**
     * @param spec the spec to check the settings against. Can be null.
     * @return null, if the settings are okay, or an user error message if some settings are missing or invalid.
     */
    String getStatus(final DataTableSpec spec) {

        if ((m_columnName == null) || (m_columnName.length() == 0)) {
            return "Specify the column to split.";
        }

        if (spec != null) {
            if (!spec.containsName(m_columnName)) {
                return "Input table doesn't contain specified column name.";
            }
            if (!spec.getColumnSpec(m_columnName).getType().isCompatible(StringValue.class)) {
                return "Selected split column must be of type string";
            }
        }
        if ((m_delimiter == null) || (m_delimiter.length() == 0)) {
            return "Specify the delimiter to perform splitting with.";
        }

        if ((m_quotePattern != null) && (m_quotePattern.length() == 0)) {
            return "Specify a quotation character (or disable it).";
        }

        if (m_outputAsCols && !m_guessNumOfCols && (m_numOfCols < 1)) {
            return "Specify the number of columns to add (or enable guessing).";
        }

        if ((m_quotePattern != null)
            && (m_quotePattern.startsWith(m_delimiter) || m_delimiter.startsWith(m_quotePattern))) {
            return "The quote and delimiter can't be the same and can't" + " prefix each other.";
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
     * @return true, if an empty string cell is introduced instead of a missing cell (in case of in missing input cell,
     *         or missing split results).
     */
    boolean isUseEmptyString() {
        return m_useEmptyStrings;
    }

    /**
     * If set to true, the node creates an empty cell in case of a missing input cell or a missing split. Otherwise, if
     * set false, it introduces a missing cell instead.
     *
     * @param useEmptyString set to true to create empty string cells instead of missing cells.
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

    /**
     * @return the outputAsList <code>true</code> if output is list, otherwise <code>false</code>.
     */
    boolean isOutputAsList() {
        return m_outputAsList;
    }

    /**
     * @param outputAsList the outputAsList to set
     */
    void setOutputAsList(final boolean outputAsList) {
        m_outputAsList = outputAsList;
    }

    /**
     * @return the outputAsSet <code>true</code> if output is set, otherwise <code>false</code>.
     */
    boolean isOutputAsSet() {
        return m_outputAsSet;
    }

    /**
     * @param outputAsSet the outputAsSet to set
     */
    void setOutputAsSet(final boolean outputAsSet) {
        m_outputAsSet = outputAsSet;
    }

    /**
     * @return the outputAsCols <code>true</code> if output are columns, otherwise <code>false</code>.
     */
    boolean isOutputAsCols() {
        return m_outputAsCols;
    }

    /**
     * @param outputAsCols the outputAsCols to set
     */
    void setOutputAsCols(final boolean outputAsCols) {
        m_outputAsCols = outputAsCols;
    }

    /**
     * @return the trim <code>true</code> if leading and trailing white spaces need to be removed, otherwise
     *         <code>false</code>.
     */
    boolean isTrim() {
        return m_trim;
    }

    /**
     * @param trim the trim to set
     */
    void setTrim(final boolean trim) {
        m_trim = trim;
    }

    /**
     * @return <code>true</code> if the input column name should be split with the same pattern as the columns contents
     *         and be used as column names.
     */
    boolean isSplitColumnNames() {
        return m_splitColumnNames;
    }

    /**
     * Set whether to split the input column's name for use as output column names.
     *
     * @param split Whether to split
     */
    void setSplitColumnNames(final boolean split) {
        m_splitColumnNames = split;
    }

    /**
     * @return whether to use a scan limit while guessing the amount of output columns
     */
    boolean hasScanLimit() {
        return m_hasScanLimit;
    }

    /**
     * @param b whether to use a scan limit while guessing the amount of output columns
     */
    void setHasScanLimit(final boolean b) {
        m_hasScanLimit = b;
    }

    /**
     * @return the number of rows to scan to guess the number of output columns
     */
    int scanLimit() {
        return m_scanLimit;
    }

    /**
     * @param n Amount of numbers to use when guessing amount of output columns. Only has effect if
     *            {@link #hasScanLimit()}.
     */
    void setScanLimit(final int n) {
        m_scanLimit = n;
    }

    /**
     * @return whether to remove the input column
     */
    boolean isRemoveInputColumn() {
        return m_removeInputColumn;
    }

    /**
     * @param b Whether to remove the input column
     */
    void setRemoveInputColumn(final boolean b) {
        m_removeInputColumn = b;
    }

    /**
     * Creates the TokenizerSettings from these CellSpliterUserSettings, which can be <code>null</code>.
     *
     * @return the tokenizer settings.
     */
    TokenizerSettings createTokenizerSettings() {
        if ((getDelimiter() == null) || (getDelimiter().length() == 0)) {
            return null;
        }

        final TokenizerSettings result = new TokenizerSettings();

        String delim = getDelimiter();
        if (isUseEscapeCharacter()) {
            delim = StringEscapeUtils.unescapeJava(delim);
        }
        result.addDelimiterPattern(delim, /* combineConsecutive */false, /* returnAsSeperateToken */false,
            /* includeInToken */false);

        final String quote = getQuotePattern();
        if ((quote != null) && (quote.length() > 0)) {
            result.addQuotePattern(quote, quote, '\\', isRemoveQuotes());
        }

        return result;
    }
}
