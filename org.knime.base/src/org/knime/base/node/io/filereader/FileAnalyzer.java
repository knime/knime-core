/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   10.03.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Vector;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

import org.knime.base.node.io.filetokenizer.Comment;
import org.knime.base.node.io.filetokenizer.Delimiter;
import org.knime.base.node.io.filetokenizer.FileTokenizer;
import org.knime.base.node.io.filetokenizer.Quote;

/**
 * Provides functionality for analyzing an ASCII data file to create default
 * settings. It tries to figure out what kind of delimiters and comments the
 * file contains - honoring fixed presettings passed in.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public final class FileAnalyzer {

    /** The number of lines the analyzer examines. */
    static final int NUMOFLINES = 1000;

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FileAnalyzer.class);

    private FileAnalyzer() {
    }

    /**
     * Tries to guess FileReader settings for the passed data file. It will use
     * the settings in the settings object (if any - but the file location is
     * required), and will read in the first lines from the file. It will first
     * detect comment characters (if the first lines start with '#' or '%'), and
     * then guess the delimiter (',', ';', or space) depending on which cuts a
     * line into (more than one) tokens.
     * 
     * @param userSettings containing the URL of the file to examin and settings
     *            that should be used and considered fixed.
     * @return settings that supposably provide more or less useful results. It
     *         will always be a non-null object - but may not contain any
     *         settings if guessing was just too hard.
     * @throws IOException if there was an error reading from the URL
     */
    public static FileReaderNodeSettings analyze(
            final FileReaderNodeSettings userSettings) throws IOException {
        if (userSettings.getDataFileLocation() == null) {
            throw new IllegalArgumentException("Must specify a valid file "
                    + "location for the file anaylizer");
        }

        // create the new and empty settings
        FileReaderNodeSettings result = new FileReaderNodeSettings();
        result.setDataFileLocationAndUpdateTableName(userSettings
                .getDataFileLocation());
        result.setTableName(userSettings.getTableName());
        result.setDecimalSeparator(userSettings.getDecimalSeparator());

        result.setAnalyzeUsedAllRows(true); // default is true, to avoid warning

        if (!userSettings.isCommentUserSet()) {
            // only guess comment patterns if user didn't provide any
            addComments(result);
            result.setCommentUserSet(false);
        } else {
            // take over user settings.
            for (Comment comment : userSettings.getAllComments()) {
                result.addBlockCommentPattern(comment.getBegin(), comment
                        .getEnd(), comment.returnAsSeparateToken(), comment
                        .includeInToken());
            }
            result.setCommentUserSet(true);
        }

        if (!userSettings.isQuoteUserSet()) {
            // only guess quotes if user didn't specify any
            addQuotes(result);
            result.setQuoteUserSet(false);
        } else {
            // take over user settings.
            for (Quote quote : userSettings.getAllQuotes()) {
                if (quote.hasEscapeChar()) {
                    result.addQuotePattern(quote.getLeft(), quote.getRight(),
                            quote.getEscape());
                } else {
                    result.addQuotePattern(quote.getLeft(), quote.getRight());
                }
            }
            result.setQuoteUserSet(true);
        }

        // if user provided whitespace characters, we need to add them.
        if (userSettings.isWhiteSpaceUserSet()) {
            for (String ws : userSettings.getAllWhiteSpaces()) {
                result.addWhiteSpaceCharacter(ws);
            }
            result.setWhiteSpaceUserSet(true);
        } else {
            result.addWhiteSpaceCharacter(" ");
            result.addWhiteSpaceCharacter("\t");
            result.setWhiteSpaceUserSet(false);
        }

        // sets delimiter and column numbers (as many columns as it gets with
        // the delimiters - regardless of any row headers); honors user settings
        setDelimitersAndColNum(userSettings, result);
        assert result.getNumberOfColumns() > 0;

        if (userSettings.isFileHasRowHeadersUserSet()) {
            result.setFileHasRowHeaders(userSettings.getFileHasRowHeaders());
            result.setFileHasRowHeadersUserSet(true);
        } else {
            boolean hasRowHeaders;
            if (result.getNumberOfColumns() > 1) {
                // only if we have at least 2 cols, one of them could be headers
                hasRowHeaders = checkRowHeader(result);
            } else {
                hasRowHeaders = false;
            }
            result.setFileHasRowHeaders(hasRowHeaders);
            result.setFileHasRowHeadersUserSet(false);
        }

        // we must correct the colnumber we've guessed
        if (result.getFileHasRowHeaders()) {
            result.setNumberOfColumns(result.getNumberOfColumns() - 1);
        }

        // guesses (copies) column types and names.
        Vector<ColProperty> columnProps = createColumnProperties(userSettings,
                result);
        result.setColumnProperties(columnProps);

        // set a default row header prefix
        if (userSettings.getRowHeaderPrefix() != null) {
            result.setRowHeaderPrefix(userSettings.getRowHeaderPrefix());
        } else {
            result.setRowHeaderPrefix("Row");
        }

        if (userSettings.isIgnoreEmptyLinesUserSet()) {
            result.setIgnoreEmptyLines(userSettings.getIgnoreEmtpyLines());
            result.setIgnoreEmptyLinesUserSet(true);
        } else {
            result.setIgnoreEmptyLines(true);
            result.setIgnoreEmptyLinesUserSet(false);
        }

        return result;

    }

    /**
     * Determines the type and name of each column. It tries to figure out if
     * there are column headers in the file or otherwise generates names for the
     * columns. <br>
     * We read from the first line one token per column (plus one for the row
     * header if we have row headers in the file). Then we do three checks:
     * first, if we have row headers and are missing one token we assume the
     * column header for the "row-header-column" is missing, thus we must have
     * column headers. Second, we check the types of the tokens read. If one of
     * the tokens (except the first if we have row headers) cannot be converted
     * into the column's type, we assume its a column header. Last, if all
     * tokens (except the first if we have row headers) start with the same
     * prefix followed by an increasing number, then that looks like column
     * headers to us. Otherwise we say we have no column headers.
     * 
     * @param userSettings settings user provided. Must be honored!
     * @param result the settings so far, must contain data url, delimiters,
     *            comments, quotes, colNumber, and rowHeader flag
     * @return a vector of colProperty objects, having the columnSpec set and
     *         the useFileHeader flag
     * @throws IOException if an I/O error occurs
     */
    private static Vector<ColProperty> createColumnProperties(
            final FileReaderNodeSettings userSettings,
            final FileReaderNodeSettings result) throws IOException {

        // first detect the type of each column
        DataType[] columnTypes = createColumnTypes(userSettings, result);
        // number of columns must be set accordingly
        assert result.getNumberOfColumns() == columnTypes.length;
        // store the first line here to analyze the tokens - depending on the
        // row header flag expect one more token to come.
        String rowHeader = null;
        String[] columnHeaders = new String[result.getNumberOfColumns()];

        BufferedReader reader;
        try {
            reader = result.createNewInputReader();
        } catch (NullPointerException npe) {
            // with Windows the openStream throws a NullPointerException if
            // we specify ' ' as a sub-dir. Bummer.
            throw new IOException("Can't access '"
                    + result.getDataFileLocation() + "'.");
        } catch (Exception e) {
            throw new IOException("Can't access '"
                    + result.getDataFileLocation() + "'.");
        }
        FileTokenizer tokenizer = new FileTokenizer(reader);
        tokenizer.setSettings(result);

        // the first token is supposed to be the header for the "row column"
        if (result.getFileHasRowHeaders()) {
            rowHeader = tokenizer.nextToken();
        }
        // now read the (possible) data column headers
        for (int c = 0; c < columnHeaders.length; c++) {
            String token = tokenizer.nextToken();
            if (token == null) {
                // end of file... already?!?
                break;
            }
            if (result.isRowDelimiter(token)) {
                // end of line - a bit early, huh??
                break;
            }
            columnHeaders[c] = token;
        }
        tokenizer.closeSourceStream();

        Vector<ColProperty> userColProps = userSettings.getColumnProperties();
        if (userColProps == null) {
            // that saves us quite some checkings later
            userColProps = new Vector<ColProperty>();
        }
        if (!userSettings.isFileHasColumnHeadersUserSet()) {
            // we must try to figure out if the file has col headers.

            // check first if we get off easy: If we didn't get enough column
            // headers, we assume the rowHeader is a data column header.
            if (result.getFileHasRowHeaders()
            // && (the last token is empty)
                    && (columnHeaders.length > 0)
                    && (columnHeaders[columnHeaders.length - 1] == null)) {
                result.setFileHasColumnHeaders(true);
                // discard the last (=null) token
                String[] colNames = new String[result.getNumberOfColumns()];
                colNames[0] = rowHeader;
                System.arraycopy(columnHeaders, 0, colNames, 1,
                        colNames.length - 1);
                return createColProps(colNames, userColProps, columnTypes);
            }

            // another indication for a column_headers_must_have is when the
            // first line contains tokens that are not type compliant with all
            // other lines (e.g. all items in the column are integers except in
            // the first line).
            for (int c = 0; c < columnHeaders.length; c++) {
                if (columnHeaders[c] == null) {
                    // the first line ended early - could be anything...
                    continue;
                }
                if (columnHeaders[c].equals("?")) {
                    continue;
                }
                if (columnTypes[c].equals(IntCell.TYPE)) {
                    try {
                        Integer.parseInt(columnHeaders[c]);
                        // that column header has data format - may be it IS
                        // data...
                        continue;
                    } catch (NumberFormatException nfe) {
                        // fall through
                    }
                } else if (columnTypes[c].equals(DoubleCell.TYPE)) {
                    try {
                        Double.parseDouble(columnHeaders[c]);
                        // that column header has data format - may be it IS
                        // data...
                        continue;
                    } catch (NumberFormatException nfe) {
                        // fall through
                    }
                } else if (columnTypes[c].equals(StringCell.TYPE)) {
                    // we can always convert to string...
                    continue;
                } else {
                    assert false;
                    // internal error - who the hell created that type???
                    break;
                }
                // we didn't hit a 'continue' meaning the parsing failed, so we
                // must use those headers.
                result.setFileHasColumnHeaders(true);
                return createColProps(columnHeaders, userColProps, columnTypes);
            }
            // and now, see if the headers to be are nicely formatted - that is
            // all have the same prefix and a growing index.
            if ((columnHeaders.length > 0) && consecutiveHeaders(columnHeaders)) {
                result.setFileHasColumnHeaders(true);
                return createColProps(columnHeaders, userColProps, columnTypes);
            }
            // otherwise we assume the first line doesn't contain headers.
            // pass an array with null strings and it will create headers for us
            result.setFileHasColumnHeaders(false);
            String[] colNames = new String[columnHeaders.length]; // null
            // array
            return createColProps(colNames, userColProps, columnTypes);
        } else {
            // user set fileHasColHeaders - see if it's true or false
            result.setFileHasColumnHeaders(userSettings
                    .getFileHasColumnHeaders());
            result.setFileHasColumnHeadersUserSet(true);
            if (userSettings.getFileHasColumnHeaders()) {
                // use the headers we read in
                if ((columnHeaders.length > 0)
                        && (columnHeaders[columnHeaders.length - 1] == null)) {
                    // okay, we got one too few, use row header
                    String[] colNames = new String[result.getNumberOfColumns()];
                    colNames[0] = rowHeader;
                    System.arraycopy(columnHeaders, 0, colNames, 1,
                            colNames.length - 1);
                    return createColProps(colNames, userColProps, columnTypes);
                } else {
                    return createColProps(columnHeaders, userColProps,
                            columnTypes);
                }
            } else {
                // don't read col headers - create null array to generate names
                String[] colNames = new String[columnHeaders.length];
                return createColProps(colNames, userColProps, columnTypes);
            }
        }
    }

    /**
     * Looks at the first token of each line (except the first line) and returns
     * true if they are all prefixed by the same (possibly empty) string
     * followed by a constantly incremented number.
     * 
     * @param settings the file to look at with corresponding settings
     * @return true if it's reasonable to assume the file has row headers
     * @throws IOException if an I/O error occurs
     */
    private static boolean checkRowHeader(final FileReaderNodeSettings settings)
            throws IOException {

        // read in all row headers to examin (plus one, the first line could
        // contain column headers - we don't examine them yet).
        String[] rowHeaders = new String[NUMOFLINES + 1];

        BufferedReader reader;
        try {
            reader = settings.createNewInputReader();
        } catch (NullPointerException npe) {
            // with Windows the openStream throws a NullPointerException if
            // we specify ' ' as a sub-dir. Bummer.
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        } catch (Exception e) {
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        }

        FileTokenizer tokenizer = new FileTokenizer(reader);
        tokenizer.setSettings(settings);
        String token;

        int hdrIdx = 0;
        while (hdrIdx < rowHeaders.length) {

            token = tokenizer.nextToken();
            if (token == null) {
                // end of file
                break;
            }

            if (!settings.isRowDelimiter(token)) {
                if (rowHeaders[hdrIdx] == null) {
                    rowHeaders[hdrIdx] = token;
                }
            } else {
                if (rowHeaders[hdrIdx] != null) {
                    hdrIdx++;
                } // else ignore that empty line
            }

        }
        tokenizer.closeSourceStream();

        String[] rowHeadersWOfirstLine = new String[rowHeaders.length - 1];
        System.arraycopy(rowHeaders, 1, rowHeadersWOfirstLine, 0,
                rowHeadersWOfirstLine.length);
        return consecutiveHeaders(rowHeadersWOfirstLine);
    }

    /**
     * Returns <code>true</code> if all items in the array are prefixed by the
     * same (possibly empty) string followed by a constantly incremented number.
     * 
     * @param headers the headers to look at, an emtpy item is considered end of
     *            list
     * @return <code>true</code> if it's reasonable to assume that these are
     *         headers
     */
    private static boolean consecutiveHeaders(final String[] headers) {

        // examin the first header first - it is the template for the rest
        String header = headers[0];
        if ((header == null) || header.equals("")) {
            return false;
        }

        int p;
        for (p = header.length() - 1; p >= 0; p--) {
            if ((header.charAt(p) < '0') || (header.charAt(p) > '9')) {
                break;
            }
        }
        // p is now the first position from the end with a not-a-number-char

        if (p == header.length() - 1) {
            // we want a running index at the end of the row identifier
            return false;
        }

        String prefix = "";
        boolean hasPrefix = false;
        if (p >= 0) {
            prefix = header.substring(0, p);
            hasPrefix = true;
        }

        String index = header.substring(p + 1);
        long oldIdx;
        try {
            oldIdx = Long.parseLong(index); // to make sure idx increases
        } catch (NumberFormatException nfe) {
            // it is a number - but too big for a long. Can't do it.
            return false;
        }

        // now expect all headers starting with the same prefix and
        // incrementing the index.
        for (int h = 1; h < headers.length; h++) {
            if (headers[h] == null) {
                // we've reached the end of the list.
                return true;
            }
            if ((headers[h].length() - 1) <= p) {
                // we want that running index. This header is too short.
                return false;
            }
            if (hasPrefix) {
                if (!headers[h].substring(0, p).equals(prefix)) {
                    // this header doesn't start with the prefix. Bummer.
                    return false;
                }
            }
            long newIdx;
            try {
                newIdx = Long.parseLong(headers[h].substring(p + 1));
            } catch (NumberFormatException nfe) {
                // this header doesn't have an index. ByeBye.
                return false;
            }
            if (newIdx <= oldIdx) {
                // we want the index to grow. This way we ensure unique headers.
                return false;
            }

            oldIdx = newIdx;

        }

        return true;
    }

    /**
     * Creates colProperty objects from the names and types passed in. If the
     * specified names are not unique it will uniquify them, setting the
     * 'useFileHeader' flag to false - if the file has colHeaders. If the
     * specified name is null it will create a name like 'col' + colNumber (also
     * falseing the flag, if the file has col headers).
     * 
     * @param colNames the names of the columns. Items can be <code>null</code>.
     * @param userProps ColProperty objects preset by the user. Used, if not
     *            null, instead of a generated col property.
     * @param colTypes the type of the columns. Items can NOT be
     *            <code>null</code>.
     * @return a Vector of ColProperties. colTypes.length in number.
     */
    private static Vector<ColProperty> createColProps(final String[] colNames,
            final Vector<ColProperty> userProps, final DataType[] colTypes) {

        assert colNames.length == colTypes.length;

        // extract user preset column names top make uniquifying faster/easier
        String[] userNames = new String[colNames.length];
        for (int c = 0; c < colNames.length; c++) {
            if (c >= userProps.size()) {
                break;
            }
            ColProperty cProp = userProps.get(c);
            if (cProp != null) {
                DataColumnSpec cSpec = cProp.getColumnSpec();
                if (cSpec != null) {
                    userNames[c] = cSpec.getName().toString();
                }
            }
        }

        Vector<ColProperty> colProps = new Vector<ColProperty>();

        for (int c = 0; c < colNames.length; c++) {
            ColProperty colProp;

            if ((userProps.size() > c) && (userProps.get(c) != null)) {

                // user specifed col properties - take them
                colProp = userProps.get(c);

            } else {
                // create one from the name and type we got
                colProp = new ColProperty();
                // take over or create a name
                String name;
                if (colNames[c] != null) {
                    name = colNames[c];
                } else {
                    name = "Col" + c;
                }
                // create and set the column spec
                DataColumnSpecCreator dcsc = new DataColumnSpecCreator(name,
                        colTypes[c]);
                colProp.setColumnSpec(dcsc.createSpec());
                // set the missing value pattern
                colProp.setMissingValuePattern("?");
                // With IntValues we give the user the choice of reading nominal
                // values (in the domain dialog). Default is: don't
                if (colTypes[c].equals(IntCell.TYPE)) {
                    // flag is used/read for int types only
                    colProp.setReadPossibleValuesFromFile(false);
                }
            }

            // make ColName unique
            boolean unique;
            int count = 2; // used to count duplicates. Added as postfix to
            // name
            String name = colProp.getColumnSpec().getName().toString();

            do {
                unique = true;
                for (int i = 0; i < colNames.length; i++) {
                    // iterate through all cols to look at user preset names,
                    // which we don't wanna change.
                    if (i < c) {
                        // look at the already existing col names
                        if (colProps.get(i).getColumnSpec().getName()
                                .toString().equals(name)) {
                            unique = false;
                        }
                    } else if (i > c) {
                        // compare it with the preset names
                        if ((userNames[i] != null)
                                && (userNames[i].equals(name))) {
                            unique = false;
                        }
                    }
                    if (!unique) {
                        name = colProp.getColumnSpec().getName().toString()
                                + "(" + count + ")";
                        count++;
                        // start all over, compare the new name with all others
                        break;
                    }
                }

            } while (!unique);

            // set the new name - if we had to change it
            if (!name.equals(colProp.getColumnSpec().getName().toString())) {
                colProp.changeColumnName(name);
            }

            // added to the result vector
            colProps.add(colProp);
        }

        return colProps;

    }

    private static DataType[] createColumnTypes(
            final FileReaderNodeSettings userSettings,
            final FileReaderNodeSettings result) throws IOException {
        BufferedReader reader;
        try {
            reader = result.createNewInputReader();
        } catch (NullPointerException npe) {
            // with Windows the openStream throws a NullPointerException if
            // we specify ' ' as a sub-dir. Bummer.
            throw new IOException("Can't access '"
                    + result.getDataFileLocation() + "'.");
        } catch (Exception e) {
            throw new IOException("Can't access '"
                    + result.getDataFileLocation() + "'.");
        }
        FileTokenizer tokenizer = new FileTokenizer(reader);
        tokenizer.setSettings(result);
        // extract user preset type - if we got any
        DataType[] userTypes = new DataType[result.getNumberOfColumns()];
        Vector<ColProperty> userColProps = userSettings.getColumnProperties();
        if (userColProps != null) {
            for (int t = 0; t < userTypes.length; t++) {
                if (t >= userColProps.size()) {
                    break;
                }
                ColProperty cProp = userColProps.get(t);
                if (cProp != null) {
                    DataColumnSpec cSpec = cProp.getColumnSpec();
                    if (cSpec != null) {
                        userTypes[t] = cSpec.getType();
                    }
                }
            }
        }
        DataType[] types = new DataType[result.getNumberOfColumns()];
        for (int t = 0; t < types.length; t++) {
            // set user type - if set.
            if (userTypes[t] != null) {
                types[t] = userTypes[t];
            }
        }
        int linesRead = 0;
        int colIdx = -1;
        while (true) {
            String token = tokenizer.nextToken();
            if (token == null) {
                // reached EOF
                break;
            }
            colIdx++;

            if (result.getFileHasRowHeaders() && (colIdx == 0)
                    && (!result.isRowDelimiter(token))) {
                // ignore the row header - get the next token/column
                token = tokenizer.nextToken();
                if (token == null) { // EOF
                    break;
                }
            }
            if (!result.isRowDelimiter(token)) {
                if ((linesRead < 1)
                        && (!userSettings.isFileHasColumnHeadersUserSet() || userSettings
                                .getFileHasColumnHeaders())) {
                    // skip the first line - could be column headers - unless
                    // we know it's not
                    continue;
                }
                if (colIdx >= result.getNumberOfColumns()) {
                    // the line contains more tokens than columns.
                    // Ignore the extra columns.
                    continue;
                }
                // Allow missing pattern "?" and empty tokens
                if ((token.length() == 0) || (token.equals("?"))) {
                    continue;
                }
                if (userTypes[colIdx] == null) {
                    // no user preset type - figure out the right type
                    if (types[colIdx] == null) {
                        // we come accross this columns for the first time:
                        // start with INT type
                        types[colIdx] = IntCell.TYPE;
                    }
                    if (types[colIdx].isCompatible(IntValue.class)) {
                        // first try integer as "most restrictive" type
                        try {
                            Integer.parseInt(token);
                            continue;
                        } catch (NumberFormatException nfe) {
                            // it's not an integer - could be a double
                            types[colIdx] = DoubleCell.TYPE;
                        }
                    } // no else here, we immediately want to check if it's a
                    // double
                    if (types[colIdx].isCompatible(DoubleValue.class)) {
                        try {
                            String dblData = token;
                            if (result.getDecimalSeparator() != '.') {
                                // we must reject tokens with a '.'.
                                if (token.indexOf('.') >= 0) {
                                    throw new NumberFormatException();
                                }
                                dblData = token.replace(result
                                        .getDecimalSeparator(), '.');
                            }
                            /*
                             * weird thing: parseDouble accepts strings with
                             * leading spaces. We don't.
                             */
                            if (dblData.indexOf(' ') >= 0) {
                                throw new NumberFormatException();
                            }
                            Double.parseDouble(dblData);
                            continue;
                        } catch (NumberFormatException nfe) {
                            // it's not a double, lets accept everything:
                            // StringCell
                            types[colIdx] = StringCell.TYPE;
                        }
                    }
                }
            } else {
                // it's a row delimiter.
                // we could check if we got enough tokens for the row from the
                // file. But if not - what would we do...
                if (colIdx > 0) {
                    linesRead++; // only count not empty lines
                }
                colIdx = -1;
                if (linesRead == NUMOFLINES) {
                    // we've seen enough
                    result.setAnalyzeUsedAllRows(false);
                    break;
                }
            }
        }
        tokenizer.closeSourceStream();
        // if there is still a type set to null we got only missig values
        // in that column: warn user
        String cols = "";
        for (int t = 0; t < types.length; t++) {
            if (types[t] == null) {
                cols += "#" + t + ", ";
                types[t] = StringCell.TYPE;

            }
        }
        if (cols.length() > 0) {
            LOGGER.warn("Didn't get any value for column(s) with index "
                    + cols.substring(0, cols.length() - 2) // cut off the comma
                    + ". Please verify column type(s).");
        }
        return types;
    }

    /**
     * Looks at the first character of the first lines of the file and
     * determines what kind of single line comment we should support. If no
     * suspect character appears it adds C-style comment to the settings object
     * passed in.
     * 
     * @param settings object containing the data file location. The method will
     *            add comment patterns to this object.
     * @throws IOException if an I/O error occurs
     */
    private static void addComments(final FileReaderNodeSettings settings)
            throws IOException {

        assert settings != null;
        assert settings.getDataFileLocation() != null;
        assert settings.getAllComments().size() == 0;

        BufferedReader reader;
        try {
            reader = settings.createNewInputReader();
        } catch (NullPointerException npe) {
            // with Windows the openStream throws a NullPointerException if
            // we specify ' ' as a sub-dir. Bummer.
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        } catch (Exception e) {
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        }

        boolean commentFound = false;
        String line;
        int linesRead = 0;

        while (linesRead < NUMOFLINES) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.equals("")) {
                // empty lines are boring.
                continue;
            }
            linesRead++;

            if (line.charAt(0) == '#') {
                settings.addSingleLineCommentPattern("#", false, false);
                commentFound = true;
                break;
            }
            if (line.charAt(0) == '%') {
                settings.addSingleLineCommentPattern("%", false, false);
                commentFound = true;
                break;
            }

            if ((line.charAt(0) == '/') && (line.charAt(1) == '/')) {
                settings.addSingleLineCommentPattern("//", false, false);
                settings.addBlockCommentPattern("/*", "*/", false, false);
                commentFound = true;
                break;
            }

        }

        if (reader.read() != -1) {
            // if the next char is EOF we've seen all rows.
            settings.setAnalyzeUsedAllRows(false);
        }

        if (!commentFound) {
            // haven't seen any line comment, set C-style comment.
            settings.addSingleLineCommentPattern("//", false, false);
            settings.addBlockCommentPattern("/*", "*/", false, false);
        }
    }

    /**
     * Adds quotes to the settings object. For now we always add double quotes
     * with an escape character and single quotes without it.
     * 
     * @param settings the object to add quote settings to. Must contain file
     *            location and possibly comments - but no delimiters yet!
     * @throws IOException if an I/O error occurs
     */
    private static void addQuotes(final FileReaderNodeSettings settings)
            throws IOException {
        assert settings != null;
        assert settings.getAllQuotes().size() == 0;
        assert settings.getDataFileLocation() != null;
        assert settings.getAllDelimiters().size() == 0;

        BufferedReader reader;
        FileTokenizer tokenizer;
        try {
            reader = settings.createNewInputReader();
        } catch (NullPointerException npe) {
            // with Windows the openStream throws a NullPointerException if
            // we specify ' ' as a sub-dir. Bummer.
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        } catch (Exception e) {
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        }
        tokenizer = new FileTokenizer(reader);

        // add '\n' as the only delimiter, so we get one line per token
        settings.addDelimiterPattern("\n", true, false, false);
        tokenizer.setSettings(settings);
        settings.removeAllDelimiters(); // reconstruct original settings.

        int linesRead = 0;
        // by default we support " and ' as quotes both with escape character \
        boolean useDoubleQuotes = true;
        boolean escapeDoubleQuotes = true;
        boolean useSingleQuotes = true;
        boolean escapeSingleQuotes = true;

        String token;
        while (true) {

            token = tokenizer.nextToken();
            if (token == null) {
                // seen end of file.
                break;
            }
            if (token.length() == 0) {
                continue; // ignore empty lines
            }
            linesRead++;

            // Count the number of quote characters. If an odd number appears
            // don't support this quote character.
            int dq = 0; // double quote count
            int edq = 0; // escaped double quotes
            int sq = 0; // single quote count
            int esq = 0; // escaped single quote count
            boolean esc = false;
            for (int c = 0; c < token.length(); c++) {
                char ch = token.charAt(c);
                if (ch == '\\') {
                    if (esc) {
                        // it's a double backslash, leave esc mode
                        esc = false;
                    } else {
                        esc = true;
                    }
                } else {
                    if (ch == '"') {
                        if (!esc) {
                            dq++;
                        } else {
                            edq++; // previous char was escape char.
                        }
                    }
                    if (ch == '\'') {
                        if (!esc) {
                            sq++;
                        } else {
                            esq++;
                        }
                    }
                    esc = false;
                }
            } // end of for loop

            // now figure out what to do...
            if (dq % 2 != 0) { // odd number of quotes
                if (edq % 2 != 0) {
                    // we can fix that by using the odd number of esc quotes
                    escapeDoubleQuotes = false;
                } else {
                    // nothing to do but not using double quotes as quotes
                    useDoubleQuotes = false;
                    if (!useSingleQuotes) {
                        break; // final decision made
                    }
                }
            }
            if (sq % 2 != 0) { // odd number of quotes
                if (esq % 2 != 0) {
                    // we can fix that by using the odd number of esc quotes
                    escapeSingleQuotes = false;
                } else {
                    // nothing to do but not using single quotes as quotes
                    useSingleQuotes = false;
                    if (!useDoubleQuotes) {
                        break; // final decision made
                    }
                }
            }

            if (linesRead > NUMOFLINES) {
                settings.setAnalyzeUsedAllRows(false);
                break;
            }
        }

        tokenizer.closeSourceStream();

        if (useDoubleQuotes) {
            if (escapeDoubleQuotes) {
                settings.addQuotePattern("\"", "\"", '\\');
            } else {
                settings.addQuotePattern("\"", "\"");
            }
        }
        if (useSingleQuotes) {
            if (escapeSingleQuotes) {
                settings.addQuotePattern("'", "'", '\\');
            } else {
                settings.addQuotePattern("'", "'");
            }
        }
    }

    /*
     * Tokenizes the first lines of the file (honouring the settings in the
     * settings object), and tries to guess which delimiters create the best
     * results. It'll try out comma-whitespace, whitespace, or
     * semicolon-whitespace delimiters; in this order. Whatever produces more
     * than one column will be set. If no settings create more than one column
     * no column delimiters will be set. A row delimiter ('\n') will always be
     * set.
     */
    private static void setDelimitersAndColNum(
            final FileReaderNodeSettings userSettings,
            final FileReaderNodeSettings result) throws IOException {

        assert result != null;
        assert userSettings != null;
        assert result.getDataFileLocation() != null;

        if (!userSettings.isDelimiterUserSet()) {

            //
            // Try out comma delimiter
            //
            // make sure '\n' is a row delimiter. Always.
            try {
                result.addRowDelimiter("\n", true);
                result.addDelimiterPattern(",", false, false, false);

                if (testDelimiterSettingsSetColNum(result)) {
                    return;
                }
            } catch (IllegalArgumentException iae) {
                // seems they've added ',' as comment before - alright then.
            }

            //
            // try tab seperated columns
            //
            try {
                result.removeAllDelimiters();
                // make sure '\n' is a row delimiter. Always.
                result.addRowDelimiter("\n", true);

                result.addDelimiterPattern("\t", false, false, false);

                if (testDelimiterSettingsSetColNum(result)) {
                    return;
                }
            } catch (IllegalArgumentException iae) {
                // seems they've added '\t' as comment before - alright then.
            }

            // Try space, ignoring additional tabs at the end of each line
            try {
                result.removeAllDelimiters();
                // make sure '\n' is a row delimiter. Always.
                result.addRowDelimiter("\n", true);

                result.addDelimiterPattern(" ", true, false, false);
                result.setIgnoreEmptyTokensAtEndOfRow(true);

                if (testDelimiterSettingsSetColNum(result)) {
                    return;
                }
            } catch (IllegalArgumentException iae) {
                // seems they've added ' ' as comment before - alright then.
            }
            // restore it to false
            result.setIgnoreEmptyTokensAtEndOfRow(false);

            //
            // try space seperated columns
            //
            try {
                result.removeAllDelimiters();
                // make sure '\n' is a row delimiter. Always.
                result.addRowDelimiter("\n", true);
                result.addDelimiterPattern(" ", true, false, false);

                if (testDelimiterSettingsSetColNum(result)) {
                    return;
                }
            } catch (IllegalArgumentException iae) {
                // seems we've added ' ' as comment before - alright then.
            }

            //
            // now also try the semicolon seperated columns, if
            // it's not already a single line comment character
            //
            try {
                result.removeAllDelimiters();
                // make sure '\n' is a row delimiter. Always.
                result.addRowDelimiter("\n", true);
                result.addDelimiterPattern(";", false, false, false);

                if (testDelimiterSettingsSetColNum(result)) {
                    return;
                }
            } catch (IllegalArgumentException iae) {
                // seems we've added ';' as comment before - alright then.
            }

            // well - none of the above settings made sense - return without
            // delims
            result.removeAllDelimiters();
            // but always have one row per line
            result.addRowDelimiter("\n", true);
            result.setNumberOfColumns(1);
            return;

        } else {
            // user provided delimiters copy them
            for (Delimiter delim : userSettings.getAllDelimiters()) {
                if (userSettings.isRowDelimiter(delim.getDelimiter())) {
                    result.addRowDelimiter(delim.getDelimiter(), delim
                            .combineConsecutiveDelims());
                } else {
                    result.addDelimiterPattern(delim.getDelimiter(), delim
                            .combineConsecutiveDelims(), delim.returnAsToken(),
                            delim.includeInToken());
                }
            }

            result.setDelimiterUserSet(true);
            result.setIgnoreEmptyTokensAtEndOfRow(userSettings
                    .ignoreEmptyTokensAtEndOfRow());
            if (userSettings.ignoreDelimsAtEORUserSet()) {
                result.setIgnoreDelimsAtEndOfRowUserValue(userSettings
                        .ignoreDelimsAtEORUserValue());
            }
            // set the number of cols that we read in with user presets.
            // take the maximum if rows have different num of cols.
            result.setNumberOfColumns(getMaximumNumberOfColumns(result));
        }

        return;

    }

    /*
     * With the new "ignore empty tokens at end of row" option this got a bit
     * more complicated: We need to keep a range of numberOfColumns that we can
     * accept. The lower bound will be the number of non-empty columns we read
     * so far (because this is the minimum all rows must have), the maximum will
     * be the non-empty plus empty columns we have seen so far. The reason for
     * that is, we may need some of these empty tokens at the end of a row to
     * fill the row, in case a later row has more (non-empty) tokens.
     */
    private static boolean testDelimiterSettingsSetColNum(
            final FileReaderNodeSettings settings) throws IOException {

        BufferedReader reader;
        FileTokenizer tokenizer;
        // first see how many columns we get with comma and whitespaces.
        try {
            reader = settings.createNewInputReader();
        } catch (NullPointerException npe) {
            // with Windows the openStream throws a NullPointerException if
            // we specify ' ' as a sub-dir. Bummer.
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        } catch (Exception e) {
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        }

        tokenizer = new FileTokenizer(reader);

        tokenizer.setSettings(settings);

        int linesRead = 0;
        int columns = 0; // column counter per line
        int numOfCols = -1; // num of cols with these settings
        int maxNumOfCols = -1; // num of cols including some emtpy tokens at
        // EOR
        boolean useSettings = false; // set it true to use these settings.
        int consEmptyTokens = 0; // consecutive empty tokens read

        while (true) {

            String token = tokenizer.nextToken();
            if (!settings.isRowDelimiter(token)) {

                columns++;

                // keep track of the empty tokens read.
                if (token.equals("") && !tokenizer.lastTokenWasQuoted()) {
                    consEmptyTokens++;
                } else {
                    consEmptyTokens = 0;
                }

            } else {
                if (columns > 0) {
                    // ignore empty lines

                    linesRead++;

                    if (linesRead > 1) {
                        if (numOfCols < 1) {
                            // this is the first line we are counting columns
                            // for
                            if (settings.ignoreEmptyTokensAtEndOfRow()) {
                                // these are the "hard" columns we need
                                numOfCols = columns - consEmptyTokens;
                                // we could fill up to this number with empty
                                // tokens
                                maxNumOfCols = columns;
                            } else {
                                numOfCols = columns;
                            }
                            if (numOfCols > 1) {
                                // if we get more than one col settings look
                                // reasonable
                                useSettings = true;
                            }
                        } else {
                            if (settings.ignoreEmptyTokensAtEndOfRow()) {
                                if ((columns - consEmptyTokens) > maxNumOfCols) {
                                    // we read more non-emtpy columns than we
                                    // could
                                    // fill (in other rows) with emtpy tokens
                                    useSettings = false;
                                    break;
                                }
                                if (columns < numOfCols) {
                                    // even with empty tokens this line has not
                                    // ebough columns
                                    useSettings = false;
                                    break;
                                }
                                if (columns < maxNumOfCols) {
                                    // "maxNumOfCols" is the maximum number all
                                    // rows
                                    // can deliver.
                                    maxNumOfCols = columns;
                                }
                                if ((columns - consEmptyTokens) > numOfCols) {
                                    // Adjust the number of "hard" columns
                                    numOfCols = columns - consEmptyTokens;
                                }
                                // "hard" columns must be less than the soft
                                // cols
                                assert numOfCols <= maxNumOfCols;
                            } else {
                                // make sure we always get the same number of
                                // cols
                                if (columns != numOfCols) {
                                    // not good. Getting different number of
                                    // cols in
                                    // different lines.
                                    useSettings = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                consEmptyTokens = 0;
                columns = 0;

                if (linesRead == NUMOFLINES) {
                    // seen enough lines.
                    settings.setAnalyzeUsedAllRows(false);
                    break;
                }
                if (token == null) {
                    // seen end of file.
                    break;
                }
            }

        }

        tokenizer.closeSourceStream();

        if (useSettings) {
            settings.setNumberOfColumns(numOfCols);
        }

        return useSettings;

    }

    private static int getMaximumNumberOfColumns(
            final FileReaderNodeSettings settings) throws IOException {

        BufferedReader reader;
        FileTokenizer tokenizer;
        // first see how many columns we get with the provided settings.
        try {
            reader = settings.createNewInputReader();
        } catch (NullPointerException npe) {
            // with Windows the openStream throws a NullPointerException if
            // we specify ' ' as a sub-dir. Bummer.
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        } catch (Exception e) {
            throw new IOException("Can't access '"
                    + settings.getDataFileLocation() + "'.");
        }

        tokenizer = new FileTokenizer(reader);

        tokenizer.setSettings(settings);

        int linesRead = 0;
        int colCount = 0; // the counter per line
        int numOfCols = 0; // the maximum
        int consEmptyTokens = 0; // consecutive emtpy tokens

        while (true) {

            String token = tokenizer.nextToken();

            if (!settings.isRowDelimiter(token)) {

                colCount++;

                // keep track of the empty tokens read.
                if (token.equals("") && !tokenizer.lastTokenWasQuoted()) {
                    consEmptyTokens++;
                } else {
                    consEmptyTokens = 0;
                }

            } else {
                // null token (=EOF) is a row delimiter
                if (colCount > 0) {
                    // ignore empty lines
                    linesRead++;
                }

                if (settings.ignoreEmptyTokensAtEndOfRow()) {
                    // we are looking for the maximum - those emtpy tokens
                    // should not contribute to it.
                    colCount -= consEmptyTokens;
                }
                if (colCount > numOfCols) {
                    // we are supposed to return the maximum
                    numOfCols = colCount;
                }

                colCount = 0;
                consEmptyTokens = 0;

                if (token == null) {
                    break;
                }
                if (linesRead == NUMOFLINES) {
                    // read enough lines.
                    settings.setAnalyzeUsedAllRows(false);
                    break;
                }
            }

        }
        tokenizer.closeSourceStream();

        return numOfCols;
    }
}
