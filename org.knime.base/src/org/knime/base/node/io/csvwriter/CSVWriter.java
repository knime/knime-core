/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * -------------------------------------------------------------------
 *
 * History
 *   Dec 17, 2005 (wiswedel): created
 *   Mar  7, 2007 (ohl): extended with more options
 */
package org.knime.base.node.io.csvwriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * Class to write a {@link org.knime.core.data.DataTable} to an output stream.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class CSVWriter extends BufferedWriter {

    private final FileWriterSettings m_settings;

    private String m_lastWarning;

    /**
     * Creates a new writer with default settings.
     *
     * @param writer the writer to write the table to.
     */
    public CSVWriter(final Writer writer) {
        this(writer, new FileWriterSettings());
        m_lastWarning = null;
    }

    /**
     * Creates new instance which writes tables to the given writer class. An
     * immediate write operation, will write the table headers (both column and
     * row headers) and will write missing values as "" (empty string).
     *
     * @param writer to write to
     * @param settings the object holding all settings, influencing how data
     *            tables are written to file.
     */
    public CSVWriter(final Writer writer, final FileWriterSettings settings) {
        super(writer);
        if (settings == null) {
            throw new NullPointerException(
                    "The CSVWriter doesn't accept null settings.");
        }

        m_lastWarning = null;
        m_settings = settings;

        // change all null strings to empty strings
        if (m_settings.getColSeparator() == null) {
            m_settings.setColSeparator("");
        }
        if (m_settings.getMissValuePattern() == null) {
            m_settings.setMissValuePattern("");
        }
        if (m_settings.getQuoteBegin() == null) {
            m_settings.setQuoteBegin("");
        }
        if (m_settings.getQuoteEnd() == null) {
            m_settings.setQuoteEnd("");
        }
        if (m_settings.getQuoteReplacement() == null) {
            m_settings.setQuoteReplacement("");
        }
        if (m_settings.getSeparatorReplacement() == null) {
            m_settings.setSeparatorReplacement("");
        }
    }

    /**
     * @return the settings object that configures this writer. Modifying it
     *         influences its behavior.
     */
    protected FileWriterSettings getSettings() {
        return m_settings;
    }

    /**
     * Writes <code>table</code> with current settings.
     *
     * @param table the table to write to the file
     * @param exec an execution monitor where to check for canceled status and
     *            report progress to. (In case of cancellation, the file will be
     *            deleted.)
     * @throws IOException if any related I/O error occurs
     * @throws CanceledExecutionException if execution in <code>exec</code>
     *             has been canceled
     * @throws NullPointerException if table is <code>null</code>
     */
    public void write(final DataTable table, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

        DataTableSpec inSpec = table.getDataTableSpec();
        final int colCount = inSpec.getNumColumns();
        boolean first; // if first entry in the row (skip separator then)
        m_lastWarning = null; // reset any previous warning

        // write column names
        if (m_settings.writeColumnHeader()) {

            if (m_settings.writeRowID()) {
                write(quoteString("row ID", false)); // RowHeader header
                first = false;
            } else {
                first = true;
            }
            for (int i = 0; i < colCount; i++) {
                String cName = inSpec.getColumnSpec(i).getName();
                if (!first) {
                    write(m_settings.getColSeparator());
                }
                first = false;
                write(quoteString(cName, false));
            }
            newLine();
        } // end of if write column names

        // write each row of the data
        int i = 0;
        int rowCnt = -1;
        if (table instanceof BufferedDataTable) {
            rowCnt = ((BufferedDataTable)table).getRowCount();
        }

        for (DataRow row : table) {

            String rowKey = row.getKey().toString();
            String msg;

            // set the progress
            if (rowCnt <= 0) {
                msg = "Writing row " + (i + 1) + " (\"" + rowKey + "\")";
            } else {
                msg =
                        "Writing row " + (i + 1) + " (\"" + rowKey + "\") of "
                                + rowCnt;
                exec.setProgress(i / (double)rowCnt, msg);
            }
            // Check if execution was canceled !
            exec.checkCanceled();

            // write the columns
            first = true;
            // first, the row id
            if (m_settings.writeRowID()) {
                write(quoteString(row.getKey().getString(), false));
                first = false;
            }
            // now all data cells
            for (int c = 0; c < colCount; c++) {

                DataCell colValue = row.getCell(c);
                if (!first) {
                    write(m_settings.getColSeparator());
                }
                first = false;

                if (colValue.isMissing()) {
                    // never quote missing patterns.
                    write(m_settings.getMissValuePattern());
                } else {
                    boolean isNumerical = false;
                    DataType type = inSpec.getColumnSpec(c).getType();
                    String strVal = colValue.toString();

                    if (type.isCompatible(DoubleValue.class)) {
                        isNumerical = true;
                    }
                    if (isNumerical
                            && (m_settings.getDecimalSeparator() != '.')) {
                        // use the new separator only if it is not already
                        // contained in the value.
                        if (strVal.indexOf(m_settings.getDecimalSeparator())
                                < 0) {
                            strVal =
                                    replaceDecimalSeparator(strVal, m_settings
                                            .getDecimalSeparator());
                        } else {
                            if (m_lastWarning == null) {
                                m_lastWarning = "Specified decimal separator ('"
                                    + m_settings.getDecimalSeparator() + "') is"
                                    + " contained in the numerical value. "
                                    + "Not replacing decimal separator (e.g. "
                                    + "in row #" + i + " column #" + c + ").";
                            }
                        }
                    }
                    write(quoteString(strVal, isNumerical));

                }
            }
            newLine();
            i++;
        }

    }

    /**
     * If the specified string contains exactly one dot it is replaced by the
     * specified character.
     *
     * @param val the string to replace the standard decimal separator ('.') in
     * @param newSeparator the new separator
     * @return a string with the dot replaced by the new separator. Could be the
     *         passed argument itself.
     */
    private String replaceDecimalSeparator(final String val,
            final char newSeparator) {

        int dotIdx = val.indexOf('.');

        // not a floating point number
        if (dotIdx < 0) {
            return val;
        }
        if (val.indexOf('.', dotIdx + 1) >= 0) {
            // more than one dot in val: not a floating point
            return val;
        }
        return val.replace('.', newSeparator);
    }

    /**
     * Returns a string that can be written out to the file that is treated
     * (with respect to quotes) according to the current settings.
     *
     * @param data the string to quote/replaceQuotes/notQuote/etc.
     * @param isNumerical set true, if the data comes from a numerical data cell
     * @return the string correctly quoted according to the current settings.
     */
    protected String quoteString(final String data, final boolean isNumerical) {

        String result = data;

        switch (m_settings.getQuoteMode()) {
        case ALWAYS:
            if (m_settings.replaceSeparatorInStrings() && !isNumerical) {
                result = replaceSeparator(data);
                result = replaceAndQuote(result);
            } else {
                result = replaceAndQuote(data);
            }
            break;
        case IF_NEEDED:
            boolean needsQuotes = false;
            // we need quotes if the data contains the separator, equals the
            // missing value pattern.
            if (m_settings.getColSeparator().length() > 0) {
                needsQuotes = data.contains(m_settings.getColSeparator());
            } else {
                needsQuotes = true;
            }
            needsQuotes |= data.equals(m_settings.getMissValuePattern());

            result = data;
            if (m_settings.replaceSeparatorInStrings() && !isNumerical) {
                result = replaceSeparator(result);
            }
            if (needsQuotes) {
                result = replaceAndQuote(result);
            }
            break;
        case REPLACE:
            result = replaceSeparator(data);
            break;
        case STRINGS:
            if (isNumerical) {
                result = data;
            } else {
                result = data;
                if (m_settings.replaceSeparatorInStrings()) {
                    result = replaceSeparator(result);
                }
                result = replaceAndQuote(result);
            }
            break;
        }

        return result;
    }

    /**
     * Replaces the quote end pattern contained in the string and puts quotes
     * around the string.
     *
     * @param data the string to examine and change
     * @return the input string with quotes around it and either replaced or
     *         escaped quote end patterns in the string.
     */
    protected String replaceAndQuote(final String data) {

        if (m_settings.getQuoteEnd().length() == 0) {
            return m_settings.getQuoteBegin() + data;
        }

        // start with the opening quotes
        StringBuilder result = new StringBuilder(m_settings.getQuoteBegin());
        int examined = 0; // index up to which the input string is handled

        do {
            int quoteIdx = data.indexOf(m_settings.getQuoteEnd(), examined);
            if (quoteIdx < 0) {
                // no (more) quote end pattern in the string. Copy the rest.
                result.append(data.substring(examined));
                // done.
                break;
            }

            // copy the part up to the quote pattern
            result.append(data.substring(examined, quoteIdx));

            // replace the quote pattern with the specified string
            result.append(m_settings.getQuoteReplacement());

            examined = quoteIdx + m_settings.getQuoteEnd().length();

        } while (examined < data.length());

        // finally append the closing quote
        result.append(m_settings.getQuoteEnd());

        return result.toString();

    }

    /**
     * Derives a string from the input string that has all appearances of the
     * separator replaced with the specified replacer string.
     *
     * @param data the string to examine and to replace the separator in.
     * @return the input string with all appearances of the separator replaced.
     */
    protected String replaceSeparator(final String data) {

        if (m_settings.getColSeparator().length() == 0) {
            return data;
        }

        boolean changed = false;
        StringBuilder result = new StringBuilder();
        int examined = 0; // index up to which the input string is handled

        do {
            int sepIdx = data.indexOf(m_settings.getColSeparator(), examined);
            if (sepIdx < 0) {
                // no (more) separator in the string. Copy the rest.
                result.append(data.substring(examined));
                // done.
                break;
            }

            changed = true;

            // copy the part up to the separator
            result.append(data.substring(examined, sepIdx));

            // replace the separator with the specified string
            result.append(m_settings.getSeparatorReplacement());

            examined = sepIdx + m_settings.getColSeparator().length();

        } while (examined < data.length());

        if (changed) {
            return result.toString();
        } else {
            return data;
        }

    }

    /**
     * @return true if a warning message is available
     */
    public boolean hasWarningMessage() {
        return m_lastWarning != null;
    }

    /**
     * Returns a warning message from the last write action. Or null, if there
     * is no warning set.
     *
     * @return a warning message from the last write action. Or null, if there
     * is no warning set.
     */
    public String getLastWarningMessage() {
        return m_lastWarning;
    }
}
