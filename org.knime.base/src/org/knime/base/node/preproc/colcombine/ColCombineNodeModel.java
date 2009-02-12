/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.preproc.colcombine;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of ColCombine. Takes the contents of a set
 * of columns and combines them into one string column.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ColCombineNodeModel extends NodeModel {

    /** Config identifier: Included columns. */
    static final String CFG_COLUMNS = "columns";
    /** Config identifier: Name of new column. */
    static final String CFG_NEW_COLUMN_NAME = "new_column_name";
    /** Config identifier: delimiter string. */
    static final String CFG_DELIMITER_STRING = "delimiter";
    /** Config identifier: if to use quoting. */
    static final String CFG_IS_QUOTING = "is_quoting";
    /** Config identifier: if is to quote always. */
    static final String CFG_IS_QUOTING_ALWAYS = "is_quoting_always";
    /** Config identifier: quote character. */
    static final String CFG_QUOTE_CHAR = "quote_char";
    /** Config identifier: delimiter replacement string. */
    static final String CFG_REPLACE_DELIMITER_STRING = "replace_delimiter";

    private String[] m_columns;
    private String m_newColName;
    private String m_delimString;
    private char m_quoteChar;
    private boolean m_isQuoting;
    private boolean m_isQuotingAlways;
    private String m_replaceDelimString;

    /**
     * Constructor for the node model.
     */
    protected ColCombineNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger arranger =
            createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(
                inData[0], arranger, exec);
        return new BufferedDataTable[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_columns == null) {
            throw new InvalidSettingsException("No settings available");
        }
        DataTableSpec spec = inSpecs[0];
        for (String s : m_columns) {
            if (!spec.containsName(s)) {
                throw new InvalidSettingsException("No such column: " + s);
            }
        }
        if (spec.containsName(m_newColName)) {
            throw new InvalidSettingsException(
                    "Column already exits: " + m_newColName);
        }
        ColumnRearranger arranger = createColumnRearranger(spec);
        return new DataTableSpec[]{arranger.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) {
        ColumnRearranger result = new ColumnRearranger(spec);
        DataColumnSpec append = new DataColumnSpecCreator(
                m_newColName, StringCell.TYPE).createSpec();
        final int[] indices = new int[m_columns.length];
        List<String> colNames = Arrays.asList(m_columns);
        int j = 0;
        for (int k = 0; k < spec.getNumColumns(); k++) {
            DataColumnSpec cs = spec.getColumnSpec(k);
            if (colNames.contains(cs.getName())) {
                indices[j] = k;
                j++;
            }
        }
        
        // ", " -> ","
        // "  " -> "  " (do not let the resulting string be empty)
        // " bla bla " -> "bla bla"
        final String delimTrim = trimDelimString(m_delimString);
        result.append(new SingleCellFactory(append) {
           @Override
            public DataCell getCell(final DataRow row) {
               String[] cellContents = new String[indices.length];
               for (int i = 0; i < indices.length; i++) {
                   DataCell c = row.getCell(indices[i]);
                   String s = c instanceof StringValue
                       ? ((StringValue)c).getStringValue() : c.toString();
                   cellContents[i] = s;
               }
               return new StringCell(handleContent(cellContents, delimTrim));
            }
        });
        return result;
    }

    /** Concatenates the elements of the array, used from cell factory.
     * @param cellContents The cell contents
     * @param delimTrim The trimmed delimiter (used as argument to not do the
     * trimming over and over again.)
     * @return The concatenated string.
     */
    private String handleContent(final String[] cellContents,
            final String delimTrim) {

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < cellContents.length; i++) {

            b.append(i > 0 ? delimTrim : "");
            String s = cellContents[i];

            if (m_isQuoting) {
                if (m_isQuotingAlways
                        || (delimTrim.length() > 0 && s.contains(delimTrim))
                        || s.contains(Character.toString(m_quoteChar))) {
                    // quote the cell content
                    b.append(m_quoteChar);

                    for (int j = 0; j < s.length(); j++) {
                        char tempChar = s.charAt(j);
                        if (tempChar == m_quoteChar || tempChar == '\\') {
                            b.append('\\');
                        }
                        b.append(tempChar);
                    }

                    b.append(m_quoteChar);
                } else {
                    b.append(s);
                }
            } else {
                // replace occurrences of the delimiter
                if (delimTrim.length() > 0) {
                    b.append(s.replace(delimTrim, m_replaceDelimString));
                } else {
                    b.append(s);
                }
            }

        }
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_columns != null) {
            settings.addStringArray(CFG_COLUMNS, m_columns);
            settings.addString(CFG_DELIMITER_STRING, m_delimString);
            settings.addBoolean(CFG_IS_QUOTING, m_isQuoting);
            if (m_isQuoting) {
                settings.addChar(CFG_QUOTE_CHAR, m_quoteChar);
                settings.addBoolean(CFG_IS_QUOTING_ALWAYS, m_isQuotingAlways);
            } else {
                settings.addString(
                        CFG_REPLACE_DELIMITER_STRING, m_replaceDelimString);
            }
            settings.addString(CFG_NEW_COLUMN_NAME, m_newColName);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columns = settings.getStringArray(CFG_COLUMNS);
        m_newColName = settings.getString(CFG_NEW_COLUMN_NAME);
        m_delimString = settings.getString(CFG_DELIMITER_STRING);
        m_isQuoting = settings.getBoolean(CFG_IS_QUOTING);
        if (m_isQuoting) {
            m_quoteChar = settings.getChar(CFG_QUOTE_CHAR);
            m_isQuotingAlways = settings.getBoolean(CFG_IS_QUOTING_ALWAYS);
        } else {
            m_replaceDelimString = settings.getString(
                    CFG_REPLACE_DELIMITER_STRING);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.getStringArray(CFG_COLUMNS).length == 0) {
            throw new InvalidSettingsException("No columns selected");
        }
        String newColName = settings.getString(CFG_NEW_COLUMN_NAME);
        if (newColName == null || newColName.trim().length() == 0) {
            throw new InvalidSettingsException(
                    "Name of new column must not be empty");
        }
        String delim = settings.getString(CFG_DELIMITER_STRING);
        if (delim == null) {
            throw new InvalidSettingsException("Delimiter must not be null");
        }
        delim = trimDelimString(delim);

        boolean isQuote = settings.getBoolean(CFG_IS_QUOTING);
        if (isQuote) {
            char quote = settings.getChar(CFG_QUOTE_CHAR);
            if (Character.isWhitespace(quote)) {
                throw new InvalidSettingsException(
                        "Can't use white space as quote char");
            }
            if (delim.contains(Character.toString(quote))) {
                throw new InvalidSettingsException("Delimiter String \""
                        + delim + "\" must not contain quote character ('"
                        + quote + "')");
            }
            settings.getBoolean(CFG_IS_QUOTING_ALWAYS);
        } else {
            String replace = settings.getString(CFG_REPLACE_DELIMITER_STRING);
            if ((delim.length() > 0) && (replace.contains(delim))) {
                throw new InvalidSettingsException("Replacement string \""
                        + replace + "\" must not contain delimiter string \""
                        + delim + "\"");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }


    /**
     * ', ' gets ','.
     * ' ' gets ' ' (do not let the resulting string be empty)
     * ' blah blah ' gets 'blah blah'
     *
     * @param delimString string to trim
     * @return the trimmed string
     */
    static String trimDelimString(final String delimString) {
        return delimString.trim().length() == 0 ? delimString : delimString
                .trim();

    }
}
