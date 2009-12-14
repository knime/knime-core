/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 */
package org.knime.base.node.preproc.cellsplit;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 * Creates the data cells for the new columns of the cell splitter.
 *
 * @author ohl, University of Konstanz
 */
class CellSplitterCellFactory implements CellFactory {

    private final CellSplitterSettings m_settings;

    private final DataTableSpec m_inSpec;

    private DataColumnSpec[] m_outSpecs = null;

    private final int m_colIdx;

    private final TokenizerSettings m_tokenizerSettings;

    private static final StringCell EMPTY_STRINGCELL = new StringCell("");

    /**
     * Constructor.
     *
     * @param inSpec the spec from the underlying input table
     * @param settings the settings object containing the user settings.
     */
    public CellSplitterCellFactory(final DataTableSpec inSpec,
            final CellSplitterSettings settings) {
        m_settings = settings;
        m_inSpec = inSpec;

        if ((m_inSpec != null) && (m_settings.getColumnName() != null)) {
            m_colIdx = m_inSpec.findColumnIndex(m_settings.getColumnName());
        } else {
            m_colIdx = -1;
        }

        m_tokenizerSettings =
                CellSplitterCellFactory.createTokenizerSettings(m_settings);
    }

    private static TokenizerSettings createTokenizerSettings(
            final CellSplitterUserSettings userSettings) {

        if (userSettings == null) {
            return null;
        }
        if ((userSettings.getDelimiter() == null)
                || (userSettings.getDelimiter().length() == 0)) {
            return null;
        }

        TokenizerSettings result = new TokenizerSettings();

        result.addDelimiterPattern(userSettings.getDelimiter(),
        /* combineConsecutive */false,
        /* returnAsSeperateToken */false,
        /* includeInToken */false);

        String quote = userSettings.getQuotePattern();
        if ((quote != null) && (quote.length() > 0)) {
            result.addQuotePattern(quote, quote, '\\', userSettings
                    .isRemoveQuotes());
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        String msg = m_settings.getStatus(m_inSpec);
        if (msg != null) {
            throw new IllegalStateException(msg);
        }
        if (m_settings.isGuessNumOfCols()
                && m_settings.getNumOfColsGuessed() < 1) {
            // guess the number of columns before creating the table
            assert false;
            throw new IllegalStateException("Number of new columns is not set");
        }
        if (m_colIdx < 0) {
            // the status should have checked this!
            assert false;
            throw new IllegalStateException(
                    "Input table doesn't contain selected column");
        }
        if (m_tokenizerSettings == null) {
            throw new IllegalStateException("Incorrect user settings");
        }

        int numOfCols = m_settings.getNumOfCols();
        if (m_settings.isGuessNumOfCols()) {
            numOfCols = m_settings.getNumOfColsGuessed();
        }
        DataCell[] result = new DataCell[numOfCols];

        DataCell inputCell = row.getCell(m_colIdx);
        if (inputCell.isMissing()) {

            Arrays.fill(result, DataType.getMissingCell());

            if (m_settings.isUseEmptyString()) {
                // replace cells for string columns with empty string cells
                for (int c = 0; c < result.length; c++) {
                    if (m_settings.getTypeOfColumn(c).equals(StringCell.TYPE)) {
                        result[c] = EMPTY_STRINGCELL;
                    }
                }
            }
            return result;
        }

        final String inputString;
        if (inputCell instanceof StringValue) {
            inputString = ((StringValue)inputCell).getStringValue();
        } else {
            inputString = inputCell.toString();
        }

        // init the tokenizer
        StringReader inputReader = new StringReader(inputString);
        // the reader is no good if it doesn't support the mark operation
        assert inputReader.markSupported();

        Tokenizer tokenizer = new Tokenizer(inputReader);
        tokenizer.setSettings(m_tokenizerSettings);

        // tokenize the column value and create new output cells
        for (int col = 0; col < result.length; col++) {

            String token = null;

            if (col == result.length - 1) {
                /*
                 * this is the last column - if there is more than one token
                 * left in the stream we need to store the entire rest
                 * (including this token) in the column.
                 */
                // mark the stream in case we need to read the rest of it
                try {

                    inputReader.mark(0);
                    token = tokenizer.nextToken();

                    // see if there is more in the stream
                    if (inputReader.read() != -1) {
                        // go back to before the token
                        inputReader.reset();
                        token = readAll(inputReader);
                    }
                } catch (IOException ioe) {
                    // reading a string won't cause an IOException.
                }
            } else {

                token = tokenizer.nextToken();

            }

            if (token == null) {
                if (m_settings.isUseEmptyString()
                        && m_settings.getTypeOfColumn(col).equals(
                                StringCell.TYPE)) {
                    // create empty string cells - not missing cells.
                    result[col] = EMPTY_STRINGCELL;
                } else {
                    result[col] = DataType.getMissingCell();
                }
            } else {
                result[col] =
                        createDataCell(token.trim(), m_settings
                                .getTypeOfColumn(col));
            }
        }

        return result;
    }

    private DataCell createDataCell(final String token, final DataType type) {

        if (type.equals(StringCell.TYPE)) {
            return new StringCell(token);

        } else if (type.equals(DoubleCell.TYPE)) {
            if (token.length() == 0) {
                return DataType.getMissingCell();
            }
            try {
                double val = Double.parseDouble(token);
                return new DoubleCell(val);
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException(
                        "Guessed the wrong type guessed " + "(got '" + token
                                + "' for a double.");
            }

        } else if (type.equals(IntCell.TYPE)) {
            if (token.length() == 0) {
                return DataType.getMissingCell();
            }
            try {
                int val = Integer.parseInt(token);
                return new IntCell(val);
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException(
                        "Guessed the wrong type guessed " + "(got '" + token
                                + "' for an integer.");
            }
        } else {
            throw new IllegalStateException("Guessed an unsupported type ...");
        }

    }

    /*
     * Reads all characters left in the reader and returns them as string.
     * Always instantiates a StringBuilder. Call only with chars left in the
     * stream (otherwise rewrite it).
     */
    private String readAll(final Reader src) {

        StringBuilder temp = new StringBuilder();

        try {
            int c = src.read();
            while (c != -1) {
                temp.append((char)c);
                c = src.read();
            }
        } catch (IOException ioe) {
            // return the stuff we got so far
        }

        return temp.toString();
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {

        // make sure number of column is set or guessed
        if (m_outSpecs == null) {
            int colNum = m_settings.getNumOfCols();
            if (m_settings.isGuessNumOfCols()) {
                colNum = m_settings.getNumOfColsGuessed();
            }

            if (colNum < 1) {
                // user settings not set yet, or col number not guessed yet.

                // don't call getColumnSpec on this rearranger
                // with incomplete settings
                assert false;
                m_outSpecs = new DataColumnSpec[0];
            }
        }
        // make sure settings are correct and complete
        if (m_outSpecs == null) {
            String msg = m_settings.getStatus(m_inSpec);
            if (msg != null) {
                // don't call getColumnSpec on this rearranger
                // with incomplete settings
                assert false;
                m_outSpecs = new DataColumnSpec[0];
            }
        }
        // no input spec, no output spec. tit for tat.
        if (m_outSpecs == null) {
            if (m_inSpec == null) {
                // don't call getColumnSpec on this rearranger
                // if you don't have an input spec.
                assert false;
                m_outSpecs = new DataColumnSpec[0];
            }
        }

        // now, create the output specs
        if (m_outSpecs == null) {

            int colNum = m_settings.getNumOfCols();
            if (m_settings.isGuessNumOfCols()) {
                colNum = m_settings.getNumOfColsGuessed();
            }

            m_outSpecs = new DataColumnSpec[colNum];
            String selColName = m_settings.getColumnName();

            for (int col = 0; col < colNum; col++) {
                String colName = selColName + "_Arr[" + col + "]";
                colName = uniquifyName(colName, m_inSpec);
                DataType colType = m_settings.getTypeOfColumn(col);

                DataColumnSpecCreator dcsc =
                        new DataColumnSpecCreator(colName, colType);
                m_outSpecs[col] = dcsc.createSpec();
            }

        }

        return m_outSpecs;

    }

    /**
     * Changes the specified col name to a name not contained in the table spec.
     *
     * @param colName the name to change
     * @param tableSpec the spec to check the name against
     * @return the same string, if the spec doesn't contain a column named
     *         <code>colName</code>, or <code>colName</code> with a suffix
     *         added to make it unique (e.g. (2)).
     */
    private String uniquifyName(final String colName,
            final DataTableSpec tableSpec) {

        String result = colName;
        int suffixIdx = 1;

        while (tableSpec.containsName(result)) {
            result = result + "(" + ++suffixIdx + ")";
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount,
                "processing row #" + curRowNr + " of " + rowCount + " ("
                        + lastKey.getString() + ")");
    }

    /**
     * Analyzes the values in the user selected column and tries to figure out
     * how many columns are needed to hold the splitted values and of which type
     * the new resulting column have to be. <br>
     * If the "guess" flag in the settings object is NOT set, it returns the
     * column number entered by the user and string type for all columns.
     * Otherwise it runs once through the entire table, splits the value of the
     * selected column, stores the maximum number of parts received, and tries
     * to convert each part into an int (first), then into a double, and if both
     * fails it sets string type for the corresponding column.
     *
     * @param table the table with the column to examine (can be null, if no
     *            type guessing is required)
     * @param userSettings user settings
     * @param exec the execution context to set progress and check for cancel
     *            (can be null)
     * @return a settings object containing the same settings as the ones passed
     *         in and in addition the type (and number) of each column to add
     * @throws CanceledExecutionException if user cancels
     */
    static CellSplitterSettings createNewColumnTypes(
            final BufferedDataTable table,
            final CellSplitterUserSettings userSettings,
            final ExecutionContext exec) throws CanceledExecutionException {

        // make sure we have settings we can deal with
        DataTableSpec spec = null;
        if (table != null) {
            spec = table.getDataTableSpec();
        }
        String msg = userSettings.getStatus(spec);
        if (msg != null) {
            // don't call this with invalid settings
            assert false;
            throw new IllegalStateException(msg);
        }

        // transfer the user settings into a new settings object (the result)
        CellSplitterSettings result;
        NodeSettings tmp = new NodeSettings("tmp");
        userSettings.saveSettingsTo(tmp);

        try {
            result = new CellSplitterSettings(tmp);
        } catch (InvalidSettingsException ise) {
            // the getStatus should have covered any invalidities
            throw new IllegalStateException(ise.getMessage());
        }

        /*
         * not guessing types:
         */
        if (!userSettings.isGuessNumOfCols()) {
            // we are not supposed to analyze the file.
            for (int col = 0; col < userSettings.getNumOfCols(); col++) {
                // create as many string columns as the user set
                result.addColumnOfType(StringCell.TYPE);
            }
            return result;
        }

        /*
         * analyze table
         */
        int colIdx =
                table.getDataTableSpec().findColumnIndex(
                        userSettings.getColumnName());
        if (colIdx < 0) {
            // the status should have checked this!
            assert false;
            throw new IllegalStateException(
                    "Input table doesn't contain selected column");
        }
        TokenizerSettings tokenizerSettings =
                createTokenizerSettings(userSettings);
        if (tokenizerSettings == null) {
            throw new IllegalStateException("Incorrect user settings");
        }

        long rowCnt = 0;
        long numOfRows = table.getRowCount();

        for (DataRow row : table) {
            rowCnt++;

            String inputString = "";
            DataCell inputCell = row.getCell(colIdx);

            if (inputCell.isMissing()) {
                // missing cells don't help determining the target types
                continue;
            }

            if (inputCell instanceof StringValue) {
                inputString = ((StringValue)inputCell).getStringValue();
            } else {
                inputString = inputCell.toString();
            }

            // init the tokenizer
            StringReader inputReader = new StringReader(inputString);
            // the reader is no good if it doesn't support the mark operation
            assert inputReader.markSupported();

            Tokenizer tokenizer = new Tokenizer(inputReader);
            tokenizer.setSettings(tokenizerSettings);
            int addedColIdx = -1;

            // read tokens from the input, analyze the tokens and set the type
            while (true) {
                String token = tokenizer.nextToken();
                addedColIdx++;
                if (token == null) {
                    // done with that input string from that row
                    break;
                }

                token = token.trim();

                DataType colType = IntCell.TYPE;

                // if we already got that many columns, verify the type
                if (addedColIdx < result.getNumOfColsGuessed()) {
                    colType = result.getTypeOfColumn(addedColIdx);
                } else {
                    // otherwise init the type with int
                    result.addColumnOfType(colType);
                }

                if (colType.equals(IntCell.TYPE)) {
                    // try converting it to an integer
                    try {
                        Integer.parseInt(token);
                    } catch (NumberFormatException nfe) {
                        // that wasn't really an integer. Try double.
                        colType = DoubleCell.TYPE;
                    }
                } // fall through. No else here.

                if (colType.equals(DoubleCell.TYPE)) {
                    // try converting it to a double
                    try {
                        Double.parseDouble(token);
                    } catch (NumberFormatException nfe) {
                        // that wasn't really a double. Use string.
                        colType = StringCell.TYPE;
                    }
                }

                // write back the type
                result.replaceTypeOfColumn(addedColIdx, colType);

            }
            if (exec != null) {
                exec.checkCanceled();
                exec.setProgress((double)rowCnt / (double)numOfRows,
                        "Analyzing row #" + rowCnt + " of " + numOfRows);
            }
        }

        /*
         * if the input table contained missing values only, we end up with no
         * column to add. Throw an exception.
         */
        if (result.getNumOfColsGuessed() < 1) {
            throw new IllegalStateException("Data analysis computed no "
                    + "columns to add (happens if input table is empty or "
                    + "has only missing values).\n"
                    + "Please set the array size manually.");
        }

        return result;
    }
}
