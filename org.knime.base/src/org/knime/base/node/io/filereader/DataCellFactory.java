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
 *   05.03.2008 (ohl): created
 *   08.01.2016 (ferry.abt): Fix Bug4957
 */
package org.knime.base.node.io.filereader;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.ConfigurableDataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;

/**
 * Helper class for the file reader node. Creates data cells of a certain type
 * from a provided string.<br>
 *
 * @author ohl, University of Konstanz
 */
public class DataCellFactory {

    private char m_decimalSeparator = '.';

    private char m_thousandsSeparator = '\0';

    private String m_thousandsRegExpr;
    private Pattern m_thousandPattern;

    private String m_lastErrorMessage;

    private String m_missingValuePattern;

    private String m_formatParameter;

    private final Map<DataType, org.knime.core.data.DataCellFactory> m_cellFactoryMap = new HashMap<>();

    private final ExecutionContext m_execContext;


    /**
     * Creates a new factory that can be used to create {@link DataCell}s from
     * a string representation of data. By default the decimal separator for
     * floating point number is a point, thousands grouping is disabled, and no
     * missing value pattern is set.
     * @deprecated use {@link #DataCellFactory(ExecutionContext)} instead
     */
    @Deprecated
    public DataCellFactory() {
        this(null);
    }


    /**
     * Creates a new factory that can be used to create {@link DataCell}s from
     * a string representation of data. By default the decimal separator for
     * floating point number is a point, thousands grouping is disabled, and no
     * missing value pattern is set.
     *
     * @param execContext the current execution context which is required by some data cell implementations
     * @since 3.0
     */
    public DataCellFactory(final ExecutionContext execContext) {
        m_execContext = execContext;
    }

    /**
     * Sets the decimal separator to the specified character. This separator is
     * only honored when converting data into floating point numbers (when
     * creating cells of type {@link DoubleCell}. It is highly discouraged to
     * set 'e' or '-' as a decimal separator.
     *
     * @param decSep the new decimal separator (default is '.')
     */
    public void setDecimalSeparator(final char decSep) {
        if (m_thousandsSeparator != '\0' && m_thousandsSeparator == decSep) {
            throw new IllegalArgumentException("decimal separator and "
                    + "thousands separator can't be the same character.");
        }
        m_decimalSeparator = decSep;
        setNumberPattern();
    }

    /**
     * Returns the current decimal separator of floating point numbers.
     *
     * @return the current decimal separator of floating point numbers.
     */
    public char getDecimalSeparator() {
        return m_decimalSeparator;
    }

    /**
     * Returns true, if grouping of thousands in floating point numbers is
     * currently supported.
     *
     * @return true, if grouping of thousands in floating point numbers is
     *         currently supported.
     */
    public boolean hasThousandsSeparator() {
        return m_thousandsRegExpr != null;
    }

    /**
     * Returns the character currently accepted as thousands separator. The NUL
     * character is returned if thousands grouping is currently not supported.
     *
     * @return the character currently accepted as thousands separator. The NUL
     *         character is returned if thousands grouping is currently not
     *         supported.
     */
    public char getThousandsSeparator() {
        return m_thousandsSeparator;
    }

    /**
     * Sets the separator for groups of thousands to the specified character.
     * This separator is only honored when converting data into floating point
     * numbers (when creating cells of type {@link DoubleCell}. It is highly
     * discouraged to set 'e' or '-' as a decimal separator.
     *
     * @param thousandsSep the character separating groups of thousands. if the
     *            NUL character ('\0') is provided this separator is disabled
     *            (which is the default).
     */
    public void setThousandsSeparator(final char thousandsSep) {
        if (thousandsSep != '\0' && thousandsSep == m_decimalSeparator) {
            throw new IllegalArgumentException("decimal separator and "
                    + "thousands separator can't be the same character.");
        }
        m_thousandsSeparator = thousandsSep;
        // set the regular expression needed for replacement
        if (m_thousandsSeparator != '\0') {
            m_thousandsRegExpr =
                    Pattern.quote(Character.toString(thousandsSep));
        } else {
            m_thousandsRegExpr = null;
        }
        setNumberPattern();
    }


    /**
     * Sets the regex pattern to detect numbers.
     */
    private void setNumberPattern() {
        m_thousandPattern = Pattern.compile("(?i)[+-]?\\d{0,3}(?:" +
                m_thousandsRegExpr + "\\d{3})*(?:" + m_decimalSeparator + "\\d*)?(?:e[+-]?\\d+)?[fd]?");
    }

    /**
     * After a call to this method no grouping of thousands in floating point
     * numbers is supported.
     */
    public void clearThousandsSeparator() {
        setThousandsSeparator('\0');
    }

    /**
     * Sets a new missing value pattern. The method
     * {@link #createDataCellOfType(DataType, String)} creates a
     * {@link DataCell} with a missing value, it the provided data there equals
     * (see {@link String#equals(Object)}) the missing value pattern.
     *
     * @param missingValuePattern the new pattern to set as missing value
     *            pattern. Set to <code>null</code> to clear the missing value
     *            pattern.
     * @see #createDataCellOfType(DataType, String)
     * @see DataType#getMissingCell()
     */
    public void setMissingValuePattern(final String missingValuePattern) {
        m_missingValuePattern = missingValuePattern;
    }

    /**
     * Returns the currently set missing value pattern. If <code>null</code>
     * is returned, the missing value pattern is disabled.
     *
     * @return the missingValuePattern
     * @see #setMissingValuePattern(String)
     */
    public String getMissingValuePattern() {
        return m_missingValuePattern;
    }


    /**
     * Sets the optional format parameter required by the underlying {@link org.knime.core.data.DataCellFactory}.
     *
     * @param formatParameter format for the data cell factory, may be <code>null</code>
     * @since 3.0
     */
    public void setFormatParameter(final String formatParameter) {
        m_formatParameter = formatParameter;
    }

    /**
     * Creates a {@link DataCell} of the specified type from the data passed.
     * A {@link DataCell} with a missing value is created if the passed data equals (see
     * {@link String#equals(Object)} the currently set missing value pattern
     * (disabled by default, see {@link #setMissingValuePattern(String)}).<br>
     * A {@link StringCell} can always be created, returns only
     * <code>null</code>, if <code>null</code> was provided as data (a
     * {@link StringCell} can't hold <code>null</code>). <br>
     * Creating an {@link IntCell} fails, if the provided data is not a valid
     * string representation of an integer number (see
     * {@link Integer#parseInt(String)}).<br>
     * Creation of a {@link DoubleCell} fails, if the provided data is not a
     * valid string representation of a double number (see
     * {@link Double#parseDouble(String)}, with respect to the currently set
     * decimal and thousands separators.
     *
     *
     * @param type the type of the data cell to create. If the provided data
     *            can't be translated to the corresponding type, null is
     *            returned. The error message can be retrieved through the
     *            {@link #getErrorMessage()}.
     * @param data the string representation of the data to store in the newly
     *            created cell. Can't be null.
     * @return a data cell of the specified type carrying the provided data.
     *         <code>Null</code> is returned if the cell couldn't be created.
     *         Mostly due to incompatible string data, probably. The error
     *         message can then be retrieved through the
     *         {@link #getErrorMessage()} method.
     * @see #setMissingValuePattern(String)
     * @throws IllegalArgumentException if the passed type is not supported.
     * @throws NullPointerException if the passed data or type is null.
     * @see #getErrorMessage()
     * @see DataType#getMissingCell()
     */
    public final DataCell createDataCellOfType(final DataType type, String data) {

        if (type == null || data == null) {
            throw new NullPointerException(
                    "DataType and the data can't be null.");
        }

        // clear any previous error message
        m_lastErrorMessage = null;

        if (data.equals(m_missingValuePattern)) {
            return DataType.getMissingCell();
        }

        org.knime.core.data.DataCellFactory cellFactory = m_cellFactoryMap.get(type);
        if (cellFactory == null) {
            cellFactory = type.getCellFactory(m_execContext)
                    .orElseThrow(() -> new IllegalArgumentException("No data cell factory for data type '" + type
                        + "' found"));
            m_cellFactoryMap.put(type, cellFactory);
        }

        if (cellFactory instanceof ConfigurableDataCellFactory) {
            ((ConfigurableDataCellFactory) cellFactory).configure(m_formatParameter);
        }

        if (type.equals(DoubleCell.TYPE)) {
            // for numbers, trim data and accept empty tokens as missing
            // cells
            // remove thousands grouping
            if (m_thousandsRegExpr != null) {
                Matcher thousandMatcher = m_thousandPattern.matcher(data);
                if (thousandMatcher.matches()) {
                    //Only continue processing if input is a valid number (wrong thousands separators are targeted to identify dates
                    data = data.replaceAll(m_thousandsRegExpr, "");
                } else {
                    m_lastErrorMessage = "Wrong data format. Got '" + data + "' for a floating point.";
                    return null;
                }
            }

            // replace decimal separator with java separator '.'
            if (m_decimalSeparator != '.') {
                // we must reject tokens with a '.'.
                if (data.indexOf('.') >= 0) {
                    m_lastErrorMessage =
                            "Wrong data format. Got '" + data
                                    + "' for a floating point.";
                    return null;
                }
                data = data.replace(m_decimalSeparator, '.');
            }
        }

        try {
            return ((FromString) cellFactory).createCell(data);
        } catch (Throwable t) {
            m_lastErrorMessage = t.getMessage();
            if (m_lastErrorMessage == null) {
                m_lastErrorMessage = "No details.";
            }
            return null;
        }
    }

    /**
     * Get the error message when
     * {@link #createDataCellOfType(DataType, String)} returned
     * <code>null</code>.
     *
     * @return the error message of the last attempt to create a
     *         {@link DataCell}. Will be null, if creation succeeded.
     */
    public String getErrorMessage() {
        return m_lastErrorMessage;
    }
}
