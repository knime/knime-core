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
 *   05.03.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Helper class for the file reader node. Creates data cells of a certain type
 * from a provided string.<br>
 *
 * @author ohl, University of Konstanz
 */
public class DataCellFactory {

    private char m_decimalSeparator;

    private char m_thousandsSeparator;

    private String m_thousandsRegExpr;

    private String m_lastErrorMessage;

    private String m_missingValuePattern;

    /**
     * Creates a new factory that can be used to create {@link DataCell}s from
     * a string representation of data. By default the decimal separator for
     * floating point number is a point, thousands grouping is disabled, and no
     * missing value pattern is set.
     */
    public DataCellFactory() {

        m_decimalSeparator = '.';
        m_thousandsSeparator = '\0';
        m_thousandsRegExpr = null;
        m_missingValuePattern = null;
        m_lastErrorMessage = null;

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
     * Creates a {@link DataCell} of the specified type from the data passed.
     * Supported are the types of {@link StringCell}, {@link IntCell}, and
     * {@link DoubleCell}, as well as smiles cells, if the corresponding
     * plug-in is installed (see {@link SmilesTypeHelper}). A {@link DataCell}
     * with a missing value is created if the passed data equals (see
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
    public final DataCell createDataCellOfType(final DataType type,
            final String data) {

        if (type == null || data == null) {
            throw new NullPointerException(
                    "DataType and the data can't be null.");
        }

        // clear any previous error message
        m_lastErrorMessage = null;

        if (data.equals(m_missingValuePattern)) {
            return DataType.getMissingCell();
        }

        if (type.equals(StringCell.TYPE)) {

            try {
                return new StringCell(data);
            } catch (Throwable t) {
                m_lastErrorMessage = t.getMessage();
                if (m_lastErrorMessage == null) {
                    m_lastErrorMessage = "No details.";
                }
                return null;
            }

        } else if (type.equals(IntCell.TYPE)) {

            // for numbers, trim data and accept empty tokens as missing
            // cells
            String trimmed = data.trim();
            if (trimmed.isEmpty()) {
                return DataType.getMissingCell();
            }

            try {
                int val = Integer.parseInt(trimmed);
                return new IntCell(val);
            } catch (NumberFormatException nfe) {
                m_lastErrorMessage =
                        "Wrong data format. Got '" + data + "' for an integer.";
                return null;
            } catch (Throwable t) {
                m_lastErrorMessage = t.getMessage();
                if (m_lastErrorMessage == null) {
                    m_lastErrorMessage = "No details.";
                }
                m_lastErrorMessage += " (Got '" + data + "' for an integer.)";
                return null;
            }

        } else if (type.equals(DoubleCell.TYPE)) {
            // for numbers, trim data and accept empty tokens as missing
            // cells
            String dblData = data.trim();
            if (dblData.isEmpty()) {
                return DataType.getMissingCell();
            }
            // remove thousands grouping
            if (m_thousandsRegExpr != null) {
                dblData = dblData.replaceAll(m_thousandsRegExpr, "");
            }
            // replace decimal separator with java separator '.'
            if (m_decimalSeparator != '.') {
                // we must reject tokens with a '.'.
                if (dblData.indexOf('.') >= 0) {
                    m_lastErrorMessage =
                            "Wrong data format. Got '" + data
                                    + "' for a floating point.";
                    return null;
                }
                dblData = dblData.replace(m_decimalSeparator, '.');
            }
            try {
                double val = Double.parseDouble(dblData);
                return new DoubleCell(val);
            } catch (NumberFormatException nfe) {
                m_lastErrorMessage =
                        "Wrong data format. Got '" + data
                                + "' for a floating point number.";
                return null;
            } catch (Throwable t) {
                m_lastErrorMessage = t.getMessage();
                if (m_lastErrorMessage == null) {
                    m_lastErrorMessage = "No details.";
                }
                m_lastErrorMessage += " (Got '" + data + "' for an integer.)";
                return null;
            }

        } else if (type.equals(SmilesTypeHelper.INSTANCE.getSmilesType())) {
            try {

                return SmilesTypeHelper.INSTANCE.newInstance(data);

            } catch (Throwable t) {
                m_lastErrorMessage = "Error during SMILES cell creation: ";
                if (t.getMessage() != null) {
                    m_lastErrorMessage += t.getMessage();
                } else {
                    m_lastErrorMessage += "<no details available>";
                }
                return null;
            }
        } else {
            throw new IllegalArgumentException(
                    "Cannot create DataCell of type " + type.toString()
                            + ". Looks like an internal error.");
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
