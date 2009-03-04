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
 *   09.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.Vector;

import org.knime.base.node.io.filetokenizer.FileTokenizer;
import org.knime.base.node.io.filetokenizer.FileTokenizerSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFTable implements DataTable {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ARFFTable.class);

    private final URL m_file;

    private final DataTableSpec m_tSpec;

    private final String m_rowPrefix;

    /**
     * Create a new DataTable reading its content from an ARFF file at the
     * specified location.
     *
     * @param arffFileLocation valid URL which points to the ARFF file to read
     * @param tSpec the structure of the table to create
     * @param rowKeyPrefix row keys are constructed like rowKeyPrefix + lineNo
     */
    public ARFFTable(final URL arffFileLocation, final DataTableSpec tSpec,
            final String rowKeyPrefix) {
        if (arffFileLocation == null) {
            throw new NullPointerException("Can't pass null ARFF "
                    + "file location.");
        }
        if (tSpec == null) {
            throw new NullPointerException("Can't handle null table spec.");
        }
        m_file = arffFileLocation;
        m_tSpec = tSpec;
        m_rowPrefix = rowKeyPrefix;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_tSpec;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        try {
            return new ARFFRowIterator(m_file, m_tSpec, m_rowPrefix);
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Reads in the header of the specified ARFF file and returns a
     * corresponding table spec object.
     *
     * @param fileLoc the location of the ARFF file to read
     * @param exec to enable users to cancel this process
     * @return a table spec reflecting the settings in the file header
     * @throws IOException if the file location couldn't be opened
     * @throws InvalidSettingsException if the file contains an invalid format
     * @throws CanceledExecutionException if user canceled
     */
    public static DataTableSpec createDataTableSpecFromARFFfile(
            final URL fileLoc, final ExecutionMonitor exec) throws IOException,
            InvalidSettingsException, CanceledExecutionException {

        // create a tokenizer to read the header
        InputStream inStream = fileLoc.openStream();

        FileTokenizer tokenizer = new FileTokenizer(new BufferedReader(
                new InputStreamReader(inStream)));
        // create tokenizer settings that will deliver us the attributes and
        // arguments as tokens.
        tokenizer.setSettings(getTokenizerHeaderSettings());
        // prepare for creating a column spec for each "@attribute" read
        Vector<DataColumnSpec> colSpecs = new Vector<DataColumnSpec>();
        String tableName = null;
        String token;
        // now we collect the header information - until we see the EOF or
        // the data section begins.
        while (true) {
            if (exec != null) {
                exec.checkCanceled(); // throws exception if user canceled.
            }
            DataCell[] possVals = null;
            DataType type;
            token = tokenizer.nextToken();
            if (token == null) {
                throw new InvalidSettingsException("Incorrect/Incomplete "
                        + "ARFF file. No data section found.");
            }
            if (token.length() == 0) {
                // ignore empty lines
                continue;
            }
            if (token.equalsIgnoreCase("@DATA")) {
                // this starts the data section: we are done.
                break;
            }
            if (token.equalsIgnoreCase("@ATTRIBUTE")) {
                // defines a new data column
                String colName = tokenizer.nextToken();
                String colType = null;
                if (tokenizer.lastTokenWasQuoted()
                        && tokenizer.getLastQuoteBeginPattern().equals("{")) {
                    // Weka allows the nominal value list to be appended without
                    // a space delimiter. We will get it then hanging at the
                    // name. Extract it from there and set it in the 'colType'
                    if (colName.charAt(0) == '{') {
                        // seems we only got a value list.
                        // The col name must be empty/missing then...
                        colType = colName;
                        colName = null;
                    } else {
                        int openBraceIdx = colName.indexOf('{');
                        int closeBraceIdx = colName.lastIndexOf('}');
                        colType = colName.substring(openBraceIdx + 1,
                                closeBraceIdx);
                        colName = colName.substring(0, openBraceIdx);
                        // we ignore everything after the nominal value list
                    }
                } else {
                    colType = tokenizer.nextToken();
                }
                if ((colName == null) || (colType == null)) {
                    throw new InvalidSettingsException(
                            "Incomplete '@attribute' statement at line "
                                    + tokenizer.getLineNumber()
                                    + " in ARFF file '" + fileLoc + "'.");
                }
                // make sure 'colType' is the last token we read before we
                // start the 'if' thing here.
                if (colType.equalsIgnoreCase("NUMERIC")
                        || colType.equalsIgnoreCase("REAL")) {
                    type = DoubleCell.TYPE;
                    // ignore whatever still comes in that line, warn though
                    readUntilEOL(tokenizer, fileLoc.toString());
                } else if (colType.equalsIgnoreCase("INTEGER")) {
                    type = IntCell.TYPE;
                    // ignore whatever still comes in that line, warn though
                    readUntilEOL(tokenizer, fileLoc.toString());
                } else if (colType.equalsIgnoreCase("STRING")) {
                    type = StringCell.TYPE;
                    // ignore whatever still comes in that line, warn though
                    readUntilEOL(tokenizer, fileLoc.toString());
                } else if (colType.equalsIgnoreCase("DATE")) {
                    // we use string cell for date ...
                    type = StringCell.TYPE;
                    // ignore whatever date format is specified
                    readUntilEOL(tokenizer, null);
                } else if (tokenizer.lastTokenWasQuoted()
                        && tokenizer.getLastQuoteBeginPattern().equals("{")) {
                    // the braces should be still in the string
                    int openBraceIdx = colType.indexOf('{');
                    int closeBraceIdx = colType.lastIndexOf('}');
                    if ((openBraceIdx >= 0) && (closeBraceIdx > 0)
                            && (openBraceIdx < closeBraceIdx)) {
                        colType = colType.substring(openBraceIdx + 1,
                                        closeBraceIdx);
                    }
                    // the type was a list of nominal values
                    possVals = extractNominalVals(colType, fileLoc.toString(),
                                    tokenizer.getLineNumber());
                    // KNIME uses string cells for nominal values.
                    type = StringCell.TYPE;
                    readUntilEOL(tokenizer, fileLoc.toString());
                } else {
                    throw new InvalidSettingsException("Invalid column type"
                            + " '" + colType + "' in attribute control "
                            + "statement in ARFF file '" + fileLoc
                            + "' at line " + tokenizer.getLineNumber() + ".");
                }
                DataColumnSpecCreator dcsc = new DataColumnSpecCreator(colName,
                        type);
                if (possVals != null) {
                    dcsc.setDomain(new DataColumnDomainCreator(possVals)
                            .createDomain());
                }
                colSpecs.add(dcsc.createSpec());

            } else if (token.equalsIgnoreCase("@RELATION")) {
                tableName = tokenizer.nextToken();
                if (tableName == null) {
                    throw new InvalidSettingsException(
                            "Incomplete '@relation' statement at line "
                                    + tokenizer.getLineNumber()
                                    + " in ARFF file '" + fileLoc + "'.");
                }
                // we just ignore the name of the data set.
                readUntilEOL(tokenizer, null);
            } else if (token.charAt(0) == '@') {
                // OOps. What's that?!?
                LOGGER.warn("ARFF reader WARNING: Unsupported control "
                        + "statement '" + token + "' in line "
                        + tokenizer.getLineNumber() + ". Ignoring it! File: "
                        + fileLoc);
                readUntilEOL(tokenizer, null);
            } else if (!token.equals("\n")) {
                LOGGER.warn("ARFF reader WARNING: Unsupported " + "statement '"
                        + token + "' in header of ARFF file '" + fileLoc
                        + "', line " + tokenizer.getLineNumber()
                        + ". Ignoring it!");
                readUntilEOL(tokenizer, null);
            } // else ignore empty lines

        } // end of while (not EOF)

        // check uniqueness of column names
        for (int c = 0; c < colSpecs.size(); c++) {
            // compare it with all specs with higher index
            for (int h = c + 1; h < colSpecs.size(); h++) {
                if (colSpecs.get(c).getName().equals(
                        colSpecs.get(h).getName())) {
                    throw new InvalidSettingsException("Two attributes with "
                            + "equal names defined in header of file '"
                            + fileLoc + "'.");
                }
            }
        }
        return new DataTableSpec(tableName, colSpecs
                .toArray(new DataColumnSpec[colSpecs.size()]));
    } // createDataTableSpecFromARFFfile(URL)

    /*
     * returns a settings object used to read the ARFF file header.
     */
    private static FileTokenizerSettings getTokenizerHeaderSettings() {
        FileTokenizerSettings settings = new FileTokenizerSettings();
        // add the ARFF single line comment
        settings.addSingleLineCommentPattern("%", false, false);
        // LF is a row seperator - add it as delimiter
        settings.addDelimiterPattern("\n", /* combine multiple= */true,
        /* return as token= */true, /* include in token= */false);
        // ARFF knows single and double quotes
        settings.addQuotePattern("'", "'");
        settings.addQuotePattern("\"", "\"");
        // the nominal values list will be quoted into one token (but the
        // braces must stay in)
        settings.addQuotePattern("{", "}", true);
        // the attribute statement and arguments are separated by space(s)
        settings.addDelimiterPattern(" ", true, false, false);
        // or tabs
        settings.addDelimiterPattern("\t", true, false, false);
        // and a combination of them
        settings.setCombineMultipleDelimiters(true);

        return settings;
    }

    /*
     * expects the list of nominal values (in curely braces and comma separated)
     * from the "@attribute" line to be next in the tokenizer (including the
     * beginning of the list with the iopening brace). Will return an array of
     * StringsCells with the different values extracted (and removed) from the
     * tokenizer. It will leave the EOL at the end of the list in the tokenizer.
     * Pass in also file name for nice error messages.
     */
    private static DataCell[] extractNominalVals(final String valList,
            final String fileName, final int lineNo)
            throws InvalidSettingsException {

        Vector<DataCell> vals = new Vector<DataCell>();

        // we must support quotes and stuff - let's use another tokenizer.
        StringReader strReader = new StringReader(valList);
        FileTokenizer tokizer = new FileTokenizer(strReader);
        FileTokenizerSettings tokSets = new FileTokenizerSettings();
        tokSets.addDelimiterPattern(",", false, false, false);
        tokSets.addQuotePattern("'", "'");
        tokSets.addQuotePattern("\"", "\"");
        tokizer.setSettings(tokSets);

        for (String val = tokizer.nextToken(); val != null; val = tokizer
                .nextToken()) {

            String newval = val;
            // trimm off any whitespaces.
            if (!tokizer.lastTokenWasQuoted()) {
                newval = val.trim();
            }

            // make sure we don't add the same value twice.
            StringCell newValCell = new StringCell(newval);
            if (!vals.contains(newValCell)) {
                vals.add(newValCell);
            } else {
                LOGGER.warn("ARFF reader WARNING: The list of nominal "
                        + "values in the header of file '" + fileName
                        + "' line " + lineNo + " contains the value '" + newval
                        + "' twice. Ignoring one appearance.");
            }
        }
        return vals.toArray(new DataCell[vals.size()]);
    }

    /*
     * reads from the tokenizer until it reads a token containing '\n'. The
     * tokenizer must be set up to consider \n as a token delimiter and to
     * return it as separate token. If a filename is passed its considered being
     * a flag to indicate that we are not really expecting anything and this
     * method will print a warning if the first token it reads is NOT the EOL.
     */
    private static void readUntilEOL(final FileTokenizer tizer,
            final String filename) {
        boolean msgPrinted = false;
        String token = tizer.nextToken();

        while ((token != null) && !token.equals("\n")) { // EOF is also EOL
            token = tizer.nextToken();
            if (!msgPrinted && (filename != null)) {
                LOGGER.warn("ARFF reader WARNING: Ignoring extra "
                        + "characters in header of file '" + filename
                        + "' line " + tizer.getLineNumber() + ".");
            }
        }
    }
}
