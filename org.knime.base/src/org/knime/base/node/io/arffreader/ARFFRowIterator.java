/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 *
 * History
 *   09.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFRowIterator extends RowIterator {
    /** The node logger fot this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ARFFRowIterator.class);

    private final DataTableSpec m_tSpec;

    private final URL m_file;

    private final String m_rowPrefix;

    private final Tokenizer m_tokenizer;

    private int m_rowNo; // we count the rows read so far

    private int m_numMsgExtraCol;

    private int m_numMsgMissCol;

    private int m_numMsgWrongFormat;

    private int m_numMsgMissVal;

    private static final int MAX_ERR_MSG = 10;

    /**
     * Create a new row iterator reading the rows from an ARFF file at the
     * specified location.
     *
     * @param fileLocation valid URL of the file to read
     * @param tSpec the structure of the table to create
     * @param rowKeyPrefix row keys are constructed like rowKeyPrefix + lineNo
     * @throws IOException if the ARFF file location couldn't be opened
     */
    public ARFFRowIterator(final URL fileLocation, final DataTableSpec tSpec,
            final String rowKeyPrefix) throws IOException {
        if (fileLocation == null) {
            throw new NullPointerException("Can't pass null ARFF file "
                    + "location");
        }
        m_file = fileLocation;
        m_tSpec = tSpec;
        m_rowNo = 1;

        if (rowKeyPrefix == null) {
            m_rowPrefix = "";
        } else {
            m_rowPrefix = rowKeyPrefix;
        }
        m_numMsgExtraCol = 0;
        m_numMsgMissCol = 0;
        m_numMsgWrongFormat = 0;
        m_numMsgMissVal = 0;

        InputStream inStream = m_file.openStream();
        BufferedReader fReader =
                new BufferedReader(new InputStreamReader(inStream));

        // eat the ARFF header
        String line;
        while ((line = fReader.readLine()) != null) {
            if (line.trim().toUpperCase().equals("@DATA")) {
                // we ate the "data" declaration token. Data starts from here.
                break;
            }
        }

        // setup the tokenizer to read the file
        m_tokenizer = new Tokenizer(fReader);
        // create settings for the tokenizer
        TokenizerSettings settings = new TokenizerSettings();
        // add the ARFF single line comment
        settings.addSingleLineCommentPattern("%", false, false);
        // LF is a row seperator - add it as delimiter
        settings.addDelimiterPattern("\n", /* combine multiple= */true,
        /* return as token= */true, /* include in token= */false);
        // ARFF knows single and double quotes
        settings.addQuotePattern("'", "'");
        settings.addQuotePattern("\"", "\"");
        // the data in the data section is separated by comma.
        settings.addDelimiterPattern(",", false, false, false);

        settings.addWhiteSpaceCharacter(' ');
        settings.addWhiteSpaceCharacter('\t');

        m_tokenizer.setSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        String token = null;
        try {
            token = m_tokenizer.nextToken();
            // skip empty lines.
            while ((token != null)
                    && (token.equals("\n") || (!m_tokenizer
                            .lastTokenWasQuoted() && token.isEmpty()))) {
                token = m_tokenizer.nextToken();
            }
            m_tokenizer.pushBack();
        } catch (Throwable t) {
            token = null;
        }
        return (token != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        // before anything else: check if there is more in the stream
        // skips empty lines!
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "The row iterator proceeded beyond the last line of '"
                            + m_file + "'.");
        }

        // create a row ID cell
        String rowID = m_rowPrefix + (m_rowNo - 1);

        // Now, read the columns until we have enough or see a row delimiter
        DataCell[] rowCells = new DataCell[m_tSpec.getNumColumns()];
        String token;
        int noOfCols = m_tSpec.getNumColumns();
        int createdCols = 0;
        while (createdCols < noOfCols) {

            token = m_tokenizer.nextToken();

            if (token == null) {
                // file ended early.
                break;
            }

            // EOL is returned as token
            if (token.equals("\n")) {
                if (createdCols == 0) {
                    /*
                     * this is a bit of a hack. The tokenizer doesn't combine
                     * delimiters if there is comment between them. That's why a
                     * comment line would create a row filled with missing
                     * values. This avoids that.
                     */
                    continue;
                }
                // line ended early.
                m_tokenizer.pushBack();
                // we need the row delim in the file, for after the loop
                break;
            }

            if (createdCols == 0) {
                // if the token in the first column starts with a '{' then
                // this is a sparce ARFF file. We are not supporting this. Yet.
                if (!token.isEmpty() && (token.charAt(0) == '{')
                        && (!m_tokenizer.lastTokenWasQuoted())) {
                    throw new IllegalStateException("ARFF Reader: Sparce ARFF "
                            + "files not supported yet.");
                }
            }

            // figure out if its a missing value
            boolean isMissingCell = false;
            if (token.equals("") && (!m_tokenizer.lastTokenWasQuoted())) {
                if (m_numMsgMissVal < MAX_ERR_MSG) {
                    LOGGER.warn("ARFF reader WARNING: No value for"
                            + " column " + (createdCols + 1) + "("
                            + m_tSpec.getColumnSpec(createdCols) + "), file '"
                            + m_file + "' line " + m_tokenizer.getLineNumber()
                            + ". Creating missing value for it.");
                    m_numMsgMissVal++;
                }
                if (m_numMsgMissVal == MAX_ERR_MSG) {
                    LOGGER.warn("   (last message of this kind)");
                    m_numMsgMissVal++;
                }
                isMissingCell = true;
            } else if ((token.equals("?"))
                    && (!m_tokenizer.lastTokenWasQuoted())) {
                // the ARFF pattern for missing values
                isMissingCell = true;
            }

            // now get that new cell (it throws something at us if it couldn't)
            rowCells[createdCols] =
                    createNewDataCellOfType(m_tSpec.getColumnSpec(createdCols)
                            .getType(), token, isMissingCell);

            createdCols++;

        } // end of while(createdCols < noOfCols)

        // In case we've seen a row delimiter before the row was complete:
        // fill the row with missing cells
        if (createdCols < noOfCols) {
            if (m_numMsgMissCol < MAX_ERR_MSG) {
                LOGGER.warn("ARFF reader WARNING: Too few columns in "
                        + "file '" + m_file + "' line "
                        + m_tokenizer.getLineNumber()
                        + ". Creating missing values for the missing columns.");
                m_numMsgMissCol++;
            }
            if (m_numMsgMissCol == MAX_ERR_MSG) {
                LOGGER.warn("   (last message of this kind)");
                m_numMsgMissCol++;
            }
            while (createdCols < noOfCols) {
                rowCells[createdCols] =
                        createNewDataCellOfType(m_tSpec.getColumnSpec(
                                createdCols).getType(), null, true);
                createdCols++;
            }
        }
        // now read the row delimiter from the file - and ignore whatever is
        // before it.
        readUntilEOL();

        m_rowNo++;

        return new DefaultRow(rowID, rowCells);

    }

    /*
     * reads from the tokenizer until it reads a token containing '\n'.
     */
    private void readUntilEOL() {

        boolean msgPrinted = false;
        String token = m_tokenizer.nextToken();

        while ((token != null) && !token.equals("\n")) { // EOF is also EOL
            if (!msgPrinted && (m_numMsgExtraCol < MAX_ERR_MSG)) {
                LOGGER.warn("ARFF reader WARNING: Ignoring extra "
                        + "columns in the data section of file '" + m_file
                        + "' line " + m_tokenizer.getLineNumber() + ".");
                m_numMsgExtraCol++;
            }
            if (m_numMsgExtraCol == MAX_ERR_MSG) {
                LOGGER.warn("   (last message of this kind.)");
                m_numMsgExtraCol++;
            }
            token = m_tokenizer.nextToken();
        }

    }

    /*
     * The function creates a default <code> DataCell </code> of a type
     * depending on the <code> type </code> passed in, and initializes the value
     * of this data cell from the <code> data </code> string (converting the
     * string to the corresponding type). It will create a missing cell and
     * print a warning if it couldn't convert the string into the appropreate
     * format (to int or double). It throws a <code> IllegalStateException
     * </code> if the <code> type </code> passed in is not supported. @param
     * type Specifies the type of DataCell that is to be created, supported are
     * DoubleCell, IntCell, and StringCell. @param data the string
     * representation of the value that will be set in the DataCell created. It
     * gets trimmed before it's converted into a number. @param
     * createMissingCell If set true the default ' <code> missing </code> '
     * value of that cell type will be set indicating that the data in that cell
     * was not specified. The <code> data </code> parameter is ignored then.
     *
     * @return <code> DataCell </code> of the type specified in <code> type
     * </code> .
     */
    private DataCell createNewDataCellOfType(final DataType type,
            final String data, final boolean createMissingCell) {

        if (type.equals(StringCell.TYPE)) {
            if (createMissingCell) {
                return DataType.getMissingCell();
            } else {
                return new StringCell(data);
            }
        } else if (type.equals(IntCell.TYPE)) {
            if (createMissingCell) {
                return DataType.getMissingCell();
            } else {
                try {
                    int val = Integer.parseInt(data.trim());
                    return new IntCell(val);
                } catch (NumberFormatException nfe) {
                    if (m_numMsgWrongFormat < MAX_ERR_MSG) {
                        LOGGER.warn("ARFF reader WARNING: Wrong data "
                                + "format. In line "
                                + m_tokenizer.getLineNumber() + " read '"
                                + data + "' for an integer.");
                        LOGGER.warn("    Creating missing cell for it.");
                        m_numMsgWrongFormat++;
                    }
                    if (m_numMsgWrongFormat == MAX_ERR_MSG) {
                        LOGGER.warn("    (last message of " + "this kind.)");
                        m_numMsgWrongFormat++;
                    }
                    return DataType.getMissingCell();
                }
            }
        } else if (type.equals(DoubleCell.TYPE)) {
            if (createMissingCell) {
                return DataType.getMissingCell();
            } else {
                try {
                    double val = Double.parseDouble(data.trim());
                    return new DoubleCell(val);
                } catch (NumberFormatException nfe) {
                    if (m_numMsgWrongFormat < MAX_ERR_MSG) {
                        LOGGER.warn("ARFF reader WARNING: Wrong data "
                                + "format. In line "
                                + m_tokenizer.getLineNumber() + " read '"
                                + data + "' for a floating point.");
                        LOGGER.warn("    Creating missing cell for it.");
                        m_numMsgWrongFormat++;
                    }
                    if (m_numMsgWrongFormat == MAX_ERR_MSG) {
                        m_numMsgWrongFormat++;
                        LOGGER.warn("    (last message of this kind.)");
                    }
                    return DataType.getMissingCell();
                }
            }
        } else {
            throw new IllegalStateException("Cannot create DataCell of type"
                    + type.toString());
        }
    }
}
