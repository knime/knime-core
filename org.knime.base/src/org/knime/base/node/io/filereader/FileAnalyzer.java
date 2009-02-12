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
 *
 * History
 *   10.03.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.knime.base.node.io.filetokenizer.Comment;
import org.knime.base.node.io.filetokenizer.Delimiter;
import org.knime.base.node.io.filetokenizer.FileTokenizer;
import org.knime.base.node.io.filetokenizer.Quote;
import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.tableview.TableContentModel;

/**
 * Provides functionality for analyzing an ASCII data file to create default
 * settings. It tries to figure out what kind of delimiters and comments the
 * file contains - honoring fixed pre-settings passed in.
 *
 * @author Peter Ohl, University of Konstanz
 */
public final class FileAnalyzer {

    /**
     * If the analysis is cut short, we look only at the first couple of lines.
     * We look at as many lines as the preview table will read in as its first
     * chunk.
     */
    static final int NUMOFLINES = TableContentModel.CHUNK_SIZE;

    // the part of the entire analysis each task takes. Must add up to 1.0.
    private static final double COMMENT_SUB = 0.1;

    private static final double QUOTES_SUB = 0.1;

    private static final double DELIMS_SUB = 0.3;

    private static final double ROWHDR_SUB = 0.05;

    private static final double TYPES_SUB = 0.4;

    private static final double COLHDR_SUB = 0.05;

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(FileAnalyzer.class);

    private FileAnalyzer() {
        // use static methods only
    }

    /**
     * Tries to guess FileReader settings for the passed data file. It will use
     * the settings in the settings object (if any - but the file location is
     * required), and will read in the first lines from the file. It will first
     * detect comment characters (if the first lines start with '#' or '%'), and
     * then guess the delimiter (',', ';', or space) depending on which cuts a
     * line into (more than one) tokens.
     *
     * @param userSettings containing the URL of the file to examine and
     *            settings that should be used and considered fixed.
     * @param exec used to check for cancellations and to report progress. Could
     *            be null. If a {@link FileReaderExecutionMonitor} is provided
     *            it is distinguished between user cancellations cutting the
     *            analysis short, and interrupts that return immediately and
     *            return null as result.
     * @return settings that supposably provide more or less useful results. It
     *         will always be a non-null object - but may not contain any
     *         settings if guessing was just too hard.
     * @throws IOException if there was an error reading from the URL
     */
    public static FileReaderNodeSettings analyze(
            final FileReaderNodeSettings userSettings,
            final ExecutionMonitor exec) throws IOException {
        if (userSettings.getDataFileLocation() == null) {
            throw new IllegalArgumentException("Must specify a valid file "
                    + "location for the file analyzer");
        }

        ExecutionMonitor execMon = exec;
        if (execMon == null) {
            // we create a default exec monitor. Doesn't hurt, because that
            // will never be canceled.
            execMon = new FileReaderExecutionMonitor();
        }

        // create the new and empty settings
        FileReaderNodeSettings result = new FileReaderNodeSettings();

        execMon.setProgress(0.0);
        try {
            result.setDataFileLocationAndUpdateTableName(userSettings
                    .getDataFileLocation());
            result.setTableName(userSettings.getTableName());
            result.setDecimalSeparator(userSettings.getDecimalSeparator());
            result.setThousandsSeparator(userSettings.getThousandsSeparator());
            result.setDecimalSeparatorUserSet(userSettings
                    .decimalSeparatorUserSet());
            result.setUniquifyRowIDs(userSettings.uniquifyRowIDs());
            result.setMaximumNumberOfRowsToRead(userSettings
                    .getMaximumNumberOfRowsToRead());
            result.setCharsetName(userSettings.getCharsetName());
            result.setAnalyzeUsedAllRows(true);
            result.setMissValuePatternStrCols(userSettings
                    .getMissValuePatternStrCols());

            ExecutionMonitor subExec = execMon.createSubProgress(COMMENT_SUB);
            if (!userSettings.isCommentUserSet()) {
                // only guess comment patterns if user didn't provide any
                addComments(result, subExec);
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
            subExec.setProgress(1.0);
            checkInterrupt(execMon);

            subExec = execMon.createSubProgress(QUOTES_SUB);
            if (!userSettings.isQuoteUserSet()) {
                // only guess quotes if user didn't specify any
                addQuotes(result, subExec);
                result.setQuoteUserSet(false);
            } else {
                // take over user settings.
                for (Quote quote : userSettings.getAllQuotes()) {
                    if (quote.hasEscapeChar()) {
                        result.addQuotePattern(quote.getLeft(), quote
                                .getRight(), quote.getEscape());
                    } else {
                        result.addQuotePattern(quote.getLeft(), quote
                                .getRight());
                    }
                }
                result.setQuoteUserSet(true);
            }
            subExec.setProgress(1.0);
            checkInterrupt(execMon);

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

            // for now we just take over this flag:
            result.setSupportShortLines(userSettings.getSupportShortLines());

            // sets delimiter and column numbers (as many columns as it gets
            // with the delimiters - regardless of any row headers);
            // honors user settings
            subExec = execMon.createSubProgress(DELIMS_SUB);

            setDelimitersAndColNum(userSettings, result, subExec);

            assert result.getNumberOfColumns() > 0;
            subExec.setProgress(1.0);
            checkInterrupt(execMon);

            // the number of column set as of now does not take into account the
            // skipped columns.
            subExec = execMon.createSubProgress(ROWHDR_SUB);
            if (userSettings.isFileHasRowHeadersUserSet()) {
                result
                        .setFileHasRowHeaders(userSettings
                                .getFileHasRowHeaders());
                result.setFileHasRowHeadersUserSet(true);
            } else {
                boolean hasRowHeaders;
                if (result.getNumberOfColumns() > 1) {
                    // if we have at least 2 cols, one of them could be headers
                    hasRowHeaders = checkRowHeader(result, subExec);
                } else {
                    hasRowHeaders = false;
                }
                result.setFileHasRowHeaders(hasRowHeaders);
                result.setFileHasRowHeadersUserSet(false);
            }
            subExec.setProgress(1.0);
            checkInterrupt(execMon);

            // we must correct the column number we've guessed
            if (result.getFileHasRowHeaders()) {
                result.setNumberOfColumns(result.getNumberOfColumns() - 1);
            }

            // guesses (or copies) column types and names.
            subExec = execMon.createSubProgress(TYPES_SUB + COLHDR_SUB);
            Vector<ColProperty> columnProps =
                    createColumnProperties(userSettings, result, subExec);
            result.setColumnProperties(columnProps);
            subExec.setProgress(1.0);

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

            execMon.setProgress(1.0);

        } catch (InterruptedExecutionException iee) {
            return null;
        }

        return result;

    }

    /**
     * If the specified {@link ExecutionMonitor} is a
     * {@link FileReaderExecutionMonitor} this method throws an exception, if
     * the execution is supposed to be interrupted. Otherwise it just returns.
     *
     * @param exec the monitor to check for interrupts
     */
    private static void checkInterrupt(final ExecutionMonitor exec)
            throws InterruptedExecutionException {
        if (exec instanceof FileReaderExecutionMonitor) {
            ((FileReaderExecutionMonitor)exec).checkInterrupted();
        }
    }

    /**
     * If the specified {@link ExecutionMonitor} is a
     * {@link FileReaderExecutionMonitor}, it is asked if the user canceled the
     * operation and wants to cut analysis short. Also, if the execution is
     * supposed to be interrupted, it throws an exception.
     *
     * @param exec the monitor to check for interrupts
     * @return true, if analysis should be cut short, false otherwise.
     * @throws InterruptedExecutionException if the execution should be
     *             interrupted immediately
     */
    private static boolean cutItShort(final ExecutionMonitor exec)
            throws InterruptedExecutionException {
        if (exec instanceof FileReaderExecutionMonitor) {
            FileReaderExecutionMonitor m = (FileReaderExecutionMonitor)exec;
            m.checkInterrupted();
            return m.wasCanceled();
        }
        try {
            exec.checkCanceled();
        } catch (CanceledExecutionException cee) {
            return true;
        }
        return false;
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
     * @param exec to check for cancellations and report progress to
     * @return a vector of colProperty objects, having the columnSpec set and
     *         the useFileHeader flag
     * @throws IOException if an I/O error occurs
     */
    private static Vector<ColProperty> createColumnProperties(
            final FileReaderNodeSettings userSettings,
            final FileReaderNodeSettings result, final ExecutionMonitor exec)
            throws IOException, InterruptedExecutionException {

        // first detect the type of each column
        ExecutionMonitor subExec = exec.createSubProgress(TYPES_SUB);
        ColProperty[] colProps =
                createColumnTypes(userSettings, result, subExec);
        // extract the column types and column missing values from the result
        // of the above method call
        DataType[] columnTypes = new DataType[colProps.length];
        String[] missValues = new String[colProps.length];
        for (int c = 0; c < colProps.length; c++) {
            columnTypes[c] = colProps[c].getColumnSpec().getType();
            missValues[c] = colProps[c].getMissingValuePattern();
        }
        subExec.setProgress(1.0);
        checkInterrupt(exec);

        // number of columns must be set accordingly (including skipped cols)
        assert result.getNumberOfColumns() == columnTypes.length;
        // store the first line here to analyze the tokens - depending on the
        // row header flag expect one more token to come.
        String rowHeader = null;
        String scndLineRowHeader = null;
        String[] columnHeaders = new String[result.getNumberOfColumns()];

        BufferedReader reader = result.createNewInputReader();
        FileTokenizer tokenizer = new FileTokenizer(reader);
        tokenizer.setSettings(result);

        exec.setProgress("Guessing column headers");

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
                scndLineRowHeader = tokenizer.nextToken();
                break;
            }
            columnHeaders[c] = token;
            try {
                checkInterrupt(exec);
            } catch (InterruptedExecutionException iee) {
                tokenizer.closeSourceStream();
                throw iee;
            }
        }
        // the next token is the row header in the next row (could be...)
        scndLineRowHeader = tokenizer.nextToken();
        tokenizer.closeSourceStream();

        Vector<ColProperty> userColProps = userSettings.getColumnProperties();
        if (userColProps == null) {
            // that saves us quite some checking later
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
                return createColProps(colNames, userColProps, columnTypes,
                        missValues, exec);
            }

            // another indication for a column_headers_must_have is when the
            // first line contains tokens that are not type compliant with all
            // other lines (e.g. all items in the column are integers except in
            // the first line).
            DataCellFactory cellFactory = new DataCellFactory();
            cellFactory.setDecimalSeparator(result.getDecimalSeparator());
            cellFactory.setThousandsSeparator(result.getThousandsSeparator());

            for (int c = 0; c < columnHeaders.length; c++) {
                checkInterrupt(exec);

                if (columnHeaders[c] == null) {
                    // the first line ended early - could be anything...
                    continue;
                }
                cellFactory.setMissingValuePattern(missValues[c]);

                DataCell dc =
                        cellFactory.createDataCellOfType(columnTypes[c],
                                columnHeaders[c]);

                if (dc != null) {
                    // this column header could be data - try the others...
                    continue;
                }

                // header is not data: must be column header
                result.setFileHasColumnHeaders(true);
                return createColProps(columnHeaders, userColProps, columnTypes,
                        missValues, exec);
            }

            // if all column headers fit the type of the column, the rowHeader
            // should also fit in - if we have row headers in the file
            if (!result.isFileHasRowHeadersUserSet()) {
                // do it only if user didn't specify rowHeader existence,
                // if the user said we have row headers they may not fit our
                // prefix+index pattern, so we have nothing to test against.
                if (rowHeader != null && scndLineRowHeader != null) {
                    HeaderHelper hh =
                            HeaderHelper
                                    .extractPrefixAndIndexFromHeader(rowHeader);
                    if (hh == null || !hh.testNextHeader(scndLineRowHeader)) {
                        // this first line row header isn't a good row header
                        // all the other lines have nice ones - create col hdrs
                        // also create colHdrs if they don't fit to each other
                        // header is not data: must be column header
                        result.setFileHasColumnHeaders(true);
                        return createColProps(columnHeaders, userColProps,
                                columnTypes, missValues, exec);
                    }
                }
            }

            // and now, see if the headers to be are nicely formatted - that is
            // all have the same prefix and a growing index.
            if ((columnHeaders.length > 0)
                    && consecutiveHeaders(columnHeaders, exec)) {
                result.setFileHasColumnHeaders(true);
                return createColProps(columnHeaders, userColProps, columnTypes,
                        missValues, exec);
            }
            // otherwise we assume the first line doesn't contain headers.
            // pass an array with null strings and it will create headers for us
            result.setFileHasColumnHeaders(false);
            String[] nulls = new String[columnHeaders.length]; // null array
            return createColProps(nulls, userColProps, columnTypes, missValues,
                    exec);
        } else {
            // user set fileHasColHeaders - see if it's true or false
            result.setFileHasColumnHeaders(userSettings
                    .getFileHasColumnHeaders());
            result.setFileHasColumnHeadersUserSet(true);
            if (userSettings.getFileHasColumnHeaders()) {
                // use the headers we read in
                if ((columnHeaders.length > 0)
                        && (columnHeaders[columnHeaders.length - 1] == null)
                        && rowHeader != null) {
                    // okay, we got one too few, use row header
                    String[] colNames = new String[result.getNumberOfColumns()];
                    colNames[0] = rowHeader;
                    System.arraycopy(columnHeaders, 0, colNames, 1,
                            colNames.length - 1);
                    return createColProps(colNames, userColProps, columnTypes,
                            missValues, exec);
                } else {
                    return createColProps(columnHeaders, userColProps,
                            columnTypes, missValues, exec);
                }
            } else {
                // don't read col headers - create null array to generate names
                String[] colNames = new String[columnHeaders.length];
                return createColProps(colNames, userColProps, columnTypes,
                        missValues, exec);
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
     * @throws InterruptedExecutionException if analysis should be interrupted
     *             immediately
     */
    private static boolean checkRowHeader(
            final FileReaderNodeSettings settings, final ExecutionMonitor exec)
            throws IOException, InterruptedExecutionException {

        BufferedFileReader reader = settings.createNewInputReader();
        long fileSize = reader.getFileSize();
        long linesRead = 0;

        exec.setProgress("Guessing row IDs");

        FileTokenizer tokenizer = new FileTokenizer(reader);
        tokenizer.setSettings(settings);
        String token;

        HeaderHelper helper = null;
        boolean firstTokenInRow = true;
        try {
            while (true) {

                token = tokenizer.nextToken();
                if (token == null) {
                    // end of file
                    break;
                }

                if (firstTokenInRow && settings.isRowDelimiter(token)) {
                    // ignore empty rows
                    continue;
                }

                if (firstTokenInRow) {
                    firstTokenInRow = false;
                    if (linesRead > 0) {
                        // we ignore the first line (could be col header line)
                        if (helper == null) {
                            // the first row ID we see
                            helper =
                                    HeaderHelper
                                            .extractPrefixAndIndexFromHeader(token);
                            if (helper == null) {
                                // that's not row header material
                                return false;
                            }
                        } else {
                            // all other header must match the first one
                            if (!helper.testNextHeader(token)) {
                                return false;
                            }
                        }
                    }
                } else {
                    // swallow all tokens except new line delimiters
                    if (settings.isRowDelimiter(token)) {
                        firstTokenInRow = true; // the next token is the first
                        linesRead++;
                        if (cutItShort(exec)) {
                            if (linesRead > NUMOFLINES) {
                                break;
                            }
                            exec.setProgress(linesRead / (double)NUMOFLINES);
                        } else {
                            if (fileSize > 0) {
                                exec.setProgress(reader.getNumberOfBytesRead()
                                        / fileSize);
                            }
                        }
                    }
                }

            }
        } finally {
            tokenizer.closeSourceStream();
        }

        return true;
    }

    /**
     * Returns <code>true</code> if all items in the array are prefixed by the
     * same (possibly empty) string followed by a constantly increasing number.
     *
     * @param headers the headers to look at, an empty item is considered end of
     *            list
     * @param exec to check for cancellations or interruptions.
     * @return <code>true</code> if it's reasonable to assume that these are
     *         headers
     * @throws InterruptedExecutionException if the execution is interrupted
     *             immediately
     */
    private static boolean consecutiveHeaders(final String[] headers,
            final ExecutionMonitor exec) {

        // examine the first header first - it is the template for the rest
        HeaderHelper helper =
                HeaderHelper.extractPrefixAndIndexFromHeader(headers[0]);
        if (helper == null) {
            return false;
        }

        // now test all other headers starting with the same prefix and
        // incrementing the running index.
        for (int h = 1; h < headers.length; h++) {
            if (!helper.testNextHeader(headers[h])) {
                return false;
            }
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
            final Vector<ColProperty> userProps, final DataType[] colTypes,
            final String[] missValuePattern, final ExecutionMonitor exec)
            throws InterruptedExecutionException {

        assert colNames.length == colTypes.length;
        assert colNames.length == missValuePattern.length;

        // keep the actually used col names in a set for fast look up
        Set<String> resultNames = new HashSet<String>();

        // extract user preset column names to make uniquifying faster/easier
        Map<Integer, String> userNames = new HashMap<Integer, String>();
        for (int c = 0; c < colNames.length; c++) {
            if (c >= userProps.size()) {
                break;
            }
            ColProperty cProp = userProps.get(c);
            if ((cProp != null) && !cProp.getSkipThisColumn()) {
                DataColumnSpec cSpec = cProp.getColumnSpec();
                if (cSpec != null) {
                    userNames.put(c, cSpec.getName());
                }
            }
        }

        Vector<ColProperty> colProps = new Vector<ColProperty>();
        for (int c = 0; c < colNames.length; c++) {
            checkInterrupt(exec);

            ColProperty colProp;

            if ((userProps.size() > c) && (userProps.get(c) != null)) {

                // user specified col properties - take them
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
                DataColumnSpecCreator dcsc =
                        new DataColumnSpecCreator(name, colTypes[c]);
                colProp.setColumnSpec(dcsc.createSpec());
                // set the missing value pattern
                colProp.setMissingValuePattern(missValuePattern[c]);
                // With IntValues we give the user the choice of reading nominal
                // values (in the domain dialog). Default is: don't
                if (colTypes[c].equals(IntCell.TYPE)) {
                    // flag is used/read for int types only
                    colProp.setReadPossibleValuesFromFile(false);
                }
            }

            // make ColName unique
            int cnt = 2; // used to count duplicates. Postfix to name
            String name = colProp.getColumnSpec().getName();
            boolean changed = false;
            // remove the username for this index to not compare against
            // itself!
            String currentUserName = userNames.remove(c);

            while (resultNames.contains(name) || userNames.containsValue(name)) {
                name = colProp.getColumnSpec().getName() + "(" + cnt++ + ")";
                changed = true;
            }

            // set the new name - if we had to change it
            if (changed) {
                colProp.changeColumnName(name);
            }

            // added to the result vector
            colProps.add(colProp);

            // add the name to the set of used names
            resultNames.add(name);
            // put in again the username for this index
            if (currentUserName != null) {
                userNames.put(c, currentUserName);
            }
        }

        return colProps;

    }

    private static ColProperty[] createColumnTypes(
            final FileReaderNodeSettings userSettings,
            final FileReaderNodeSettings result, final ExecutionMonitor exec)
            throws IOException, InterruptedExecutionException {
        BufferedFileReader reader = result.createNewInputReader();
        long fileSize = reader.getFileSize();

        exec.setProgress("Guessing column types");

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
        // if we find a number that can't be parsed,
        // we set it as missing value pattern
        String[] missValPattern = new String[result.getNumberOfColumns()];
        // we can use this missing value pattern only if we also got a real
        // value for that same column
        boolean[] gotValue = new boolean[result.getNumberOfColumns()];

        for (int t = 0; t < types.length; t++) {
            // set user type - if set.
            if (userTypes[t] != null) {
                types[t] = userTypes[t];
            } else {
                types[t] = IntCell.TYPE;
            }
            // initialize the data structures:
            missValPattern[t] = null;
            gotValue[t] = false;
        }

        FileTokenizer tokenizer = new FileTokenizer(reader);
        tokenizer.setSettings(result);
        int linesRead = 0;
        int colIdx = -1;

        DataCellFactory cellFactory = new DataCellFactory();
        cellFactory.setDecimalSeparator(result.getDecimalSeparator());
        cellFactory.setThousandsSeparator(result.getThousandsSeparator());

        try {
            // close the stream on an exception
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
                checkInterrupt(exec);

                if (result.isRowDelimiter(token)) {
                    // it's a row delimiter.
                    // we could check if we got enough tokens for the row from
                    // the file. But if not - what would we do...
                    if (colIdx > 0) {
                        linesRead++; // only count not empty lines
                        exec.setProgress("Verifying column types");
                    }
                    colIdx = -1;
                    if (cutItShort(exec)) {
                        if (linesRead >= NUMOFLINES) {
                            result.setAnalyzeUsedAllRows(false);
                            break;
                        }
                        exec.setProgress(linesRead / (double)NUMOFLINES);
                    } else {
                        if (fileSize > 0) {
                            exec.setProgress(reader.getNumberOfBytesRead()
                                    / (double)fileSize);
                        }
                    }
                    continue;
                }
                if ((linesRead < 1)
                        && (!userSettings.isFileHasColumnHeadersUserSet()
                                || userSettings.getFileHasColumnHeaders())) {
                    // skip the first line - could be column headers -
                    // unless we know it's not
                    continue;
                }
                if (colIdx >= result.getNumberOfColumns()) {
                    // the line contains more tokens than columns.
                    // Ignore the extra columns.
                    continue;
                }

                if (userTypes[colIdx] != null) {
                    // user preset type - nothing to do for us in this column
                    continue;
                }

                cellFactory.setMissingValuePattern(missValPattern[colIdx]);

                // for numbers we trim tokens and allow empty for missValue
                token = token.trim();

                if (types[colIdx].isCompatible(IntValue.class)) {
                    DataCell dc =
                            cellFactory.createDataCellOfType(IntCell.TYPE,
                                    token);
                    if (dc != null) {
                        gotValue[colIdx] = gotValue[colIdx] || !dc.isMissing();
                        continue;
                    }
                    // not an integer - could it be the missing value?
                    if (missValPattern[colIdx] == null) {
                        // we accept one token that can't be
                        // parsed per column - but we don't use doubles
                        // as missing value! Would be odd.
                        dc = cellFactory.createDataCellOfType(
                                DoubleCell.TYPE, token);
                        if (dc == null) {
                            missValPattern[colIdx] = token;
                            continue;
                        }
                    }
                    // not an integer, not the missing value
                    // - could be a double
                    types[colIdx] = DoubleCell.TYPE;
                } // no else, we immediately check if it's a double

                if (types[colIdx].isCompatible(DoubleValue.class)) {
                    DataCell dc =
                            cellFactory.createDataCellOfType(DoubleCell.TYPE,
                                    token);
                    if (dc != null) {
                        gotValue[colIdx] = gotValue[colIdx] || !dc.isMissing();
                        continue;
                    }
                    // not a double - missing value maybe?
                    if (missValPattern[colIdx] == null) {
                        // we accept one token that can't be parsed
                        // per column as missing value pattern
                        missValPattern[colIdx] = token;
                        continue;
                    }
                    // not a double, not a missing value,
                    // lets accept everything: StringCell
                    types[colIdx] = StringCell.TYPE;
                    gotValue[colIdx] = true;
                }

            }
        } finally {
            tokenizer.closeSourceStream();
        }

        // set all columns we didn't see any real value for to String.
        // Discard any (possible) missing value pattern (that works,
        // because we don't accept doubles as missing value patterns).
        // Warn the user.
        String cols = "";
        int cnt = 0;
        for (int t = 0; t < types.length; t++) {
            if (userTypes[t] == null && !gotValue[t]) {
                // do it only for types not set by the user
                assert types[t].equals(IntCell.TYPE);
                types[t] = StringCell.TYPE;
                missValPattern[t] = null;
                if ((cnt < 21)
                        && ((userColProps == null)
                                || (userColProps.size() <= t)
                                || (userColProps.get(t) == null)
                                || (!userColProps
                                        .get(t).getSkipThisColumn()))) {
                    if (cnt < 20) {
                        cols += "#" + t + ", ";
                        cnt++;
                    } else if (cnt == 20) {
                        cols += "...and more..., ";
                        cnt++;
                    }
                }
            }
        }
        if (cols.length() > 0) {
            LOGGER.warn("Didn't get any value for column(s) with index "
                    // cut off the comma
                    + cols.substring(0, cols.length() - 2)
                    + ". Please verify column type(s).");
        }

        // pack column types and column missing values in one object
        ColProperty[] colPropResult = new ColProperty[types.length];
        for (int c = 0; c < colPropResult.length; c++) {
            ColProperty cp = new ColProperty();
            DataColumnSpecCreator dcsc =
                    new DataColumnSpecCreator("Foo", types[c]);
            cp.setColumnSpec(dcsc.createSpec());
            if (types[c].equals(StringCell.TYPE)) {
                // for string columns we don't have a missing value.
                // use the global one, if set, otherwise '?'
                if (result.getMissValuePatternStrCols() != null) {
                    cp.setMissingValuePattern(result
                            .getMissValuePatternStrCols());
                } else {
                    cp.setMissingValuePattern("?");
                }
            } else {
                // for int or double, use the one we figured out (or none)
                cp.setMissingValuePattern(missValPattern[c]);
            }
            colPropResult[c] = cp;
        }
        return colPropResult;
    }

    /**
     * Looks at the first character of the first lines of the file and
     * determines what kind of single line comment we should support. Comments
     * are usually at the beginning of the file - so this method looks only at
     * the first couple of lines (even if we are not supposed to cut the
     * analysis short).
     *
     * @param settings object containing the data file location. The method will
     *            add comment patterns to this object.
     * @param exec to check for cancellations and to report progress
     * @throws IOException if an I/O error occurs
     */
    private static void addComments(final FileReaderNodeSettings settings,
            final ExecutionMonitor exec) throws IOException,
            InterruptedExecutionException {

        assert settings != null;
        assert settings.getDataFileLocation() != null;
        assert settings.getAllComments().size() == 0;

        BufferedReader reader = settings.createNewInputReader();

        exec.setProgress("Guessing comment pattern");

        String line;
        int linesRead = 0;

        try {
            while (true) {

                checkInterrupt(exec);

                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.equals("")) {
                    // empty lines are boring.
                    continue;
                }
                linesRead++;

                exec.setProgress(linesRead / (double)NUMOFLINES);

                if (line.charAt(0) == '#') {
                    settings.addSingleLineCommentPattern("#", false, false);
                    break;
                }
                if (line.charAt(0) == '%') {
                    settings.addSingleLineCommentPattern("%", false, false);
                    break;
                }

                if ((line.charAt(0) == '/') && (line.charAt(1) == '/')) {
                    settings.addSingleLineCommentPattern("//", false, false);
                    settings.addBlockCommentPattern("/*", "*/", false, false);
                    break;
                }
                if (cutItShort(exec)) {
                    if (linesRead >= NUMOFLINES) {
                        settings.setAnalyzeUsedAllRows(false);
                        break;
                    }
                    exec.setProgress(linesRead / (double)NUMOFLINES);
                }
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Adds quotes to the settings object. It counts the occurrence of double
     * and single quotes in each line. If it's an odd number it will not
     * consider this being a quote (unless it has an odd number of escaped
     * character of this type).
     *
     * @param settings the object to add quote settings to. Must contain file
     *            location and possibly comments - but no delimiters yet!
     * @param exec to check for cancellations and to report progress
     * @throws IOException if an I/O error occurs
     * @throws InterruptedExecutionException if analysis was interrupted
     */
    private static void addQuotes(final FileReaderNodeSettings settings,
            final ExecutionMonitor exec) throws IOException,
            InterruptedExecutionException {
        assert settings != null;
        assert settings.getAllQuotes().size() == 0;
        assert settings.getDataFileLocation() != null;
        assert settings.getAllDelimiters().size() == 0;

        BufferedFileReader reader = settings.createNewInputReader();
        FileTokenizer tokenizer = new FileTokenizer(reader);
        double fileSize = reader.getFileSize();
        exec.setProgress("Guessing quotes");

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
        try {
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
                // cutItShort also checks for interrupt
                if (cutItShort(exec) && (linesRead > NUMOFLINES)) {
                    settings.setAnalyzeUsedAllRows(false);
                    break;
                }

                if (cutItShort(exec)) {
                    exec.setProgress(linesRead / (double)NUMOFLINES);
                } else if (fileSize > 0) {
                    exec.setProgress(reader.getNumberOfBytesRead() / fileSize);
                }

                // Count the number of quote characters. If an odd number
                // appears don't support this quote character.
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
            }

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

        } finally {
            // do this even if analysis is interrupted
            tokenizer.closeSourceStream();
        }

    }

    /**
     * Splits the lines of the file (honoring the settings in the settings
     * object), and tries to guess which delimiters create the best results.
     * It'll try out semicolon, comma, tab, or space delimiters; in this order.
     * Whatever produces more than one column (consistently) will be set. If no
     * settings create more than one column no column delimiters will be set. A
     * row delimiter ('\n') is always set.
     */
    private static void setDelimitersAndColNum(
            final FileReaderNodeSettings userSettings,
            final FileReaderNodeSettings result, final ExecutionMonitor exec)
            throws IOException, InterruptedExecutionException {

        assert result != null;
        assert userSettings != null;
        assert result.getDataFileLocation() != null;

        if (!userSettings.isDelimiterUserSet()) {

            exec.setProgress("Guessing column separator");
            exec.setProgress(0.0);
            // Start with a semicolon delimiter. This way we catch the German
            // version of CSV files (they use semicolons, because comma is the
            // decimal separator - which might not yet be specified by the user)
            //
            if ((userSettings.getThousandsSeparator() != ';')
                    && (userSettings.getDecimalSeparator() != ';')) {
                ExecutionMonitor subExec = createSubExecWithRemainder(exec);
                try {
                    result.removeAllDelimiters();
                    // make sure '\n' is a row delimiter. Always.
                    result.addRowDelimiter("\n", true);
                    result.addDelimiterPattern(";", false, false, false);

                    if (testDelimiterSettingsSetColNum(result, subExec)) {
                        return;
                    }
                } catch (IllegalArgumentException iae) {
                    // seems we've added ';' as comment before - alright then.
                }
            }
            //
            // Try out comma delimiter
            // - but only if its not the decimal or thousand separator
            //
            if ((userSettings.getThousandsSeparator() != ',')
                    && (userSettings.getDecimalSeparator() != ',')) {
                // make sure '\n' is a row delimiter. Always.
                ExecutionMonitor subExec = createSubExecWithRemainder(exec);
                try {
                    result.removeAllDelimiters();
                    result.addRowDelimiter("\n", true);
                    result.addDelimiterPattern(",", false, false, false);

                    if (testDelimiterSettingsSetColNum(result, subExec)) {
                        return;
                    }
                } catch (IllegalArgumentException iae) {
                    // seems they've added ',' as comment before - alright then.
                }
            }
            //
            // try tab separated columns
            // - but only if its not the decimal or thousand separator
            //
            if ((userSettings.getThousandsSeparator() != '\t')
                    && (userSettings.getDecimalSeparator() != '\t')) {
                ExecutionMonitor subExec = createSubExecWithRemainder(exec);
                try {
                    result.removeAllDelimiters();
                    // make sure '\n' is a row delimiter. Always.
                    result.addRowDelimiter("\n", true);

                    result.addDelimiterPattern("\t", false, false, false);

                    if (testDelimiterSettingsSetColNum(result, subExec)) {
                        return;
                    }
                } catch (IllegalArgumentException iae) {
                    // seems they've added '\t' as comment before - alright
                    // then.
                }
            }
            //
            // Try space, ignoring additional tabs at the end of each line
            // - but only if its not the decimal or thousand separator
            //
            if ((userSettings.getThousandsSeparator() != ' ')
                    && (userSettings.getDecimalSeparator() != ' ')) {
                ExecutionMonitor subExec = createSubExecWithRemainder(exec);
                try {
                    result.removeAllDelimiters();
                    // make sure '\n' is a row delimiter. Always.
                    result.addRowDelimiter("\n", true);

                    result.addDelimiterPattern(" ", true, false, false);
                    result.setIgnoreEmptyTokensAtEndOfRow(true);

                    if (testDelimiterSettingsSetColNum(result, subExec)) {
                        return;
                    }
                } catch (IllegalArgumentException iae) {
                    // seems they've added ' ' as comment before - alright then.
                }
                // restore it to false
                result.setIgnoreEmptyTokensAtEndOfRow(false);

                //
                // try space separated columns
                //
                subExec = createSubExecWithRemainder(exec);
                try {
                    result.removeAllDelimiters();
                    // make sure '\n' is a row delimiter. Always.
                    result.addRowDelimiter("\n", true);
                    result.addDelimiterPattern(" ", true, false, false);

                    if (testDelimiterSettingsSetColNum(result, subExec)) {
                        return;
                    }
                } catch (IllegalArgumentException iae) {
                    // seems we've added ' ' as comment before - alright then.
                }
            }

            // well - none of the above settings made sense - return without
            // delimiter
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
            result.setNumberOfColumns(getMaximumNumberOfColumns(result, exec));
        }

        return;

    }

    /**
     * Creates a sub-exec with the progress part (100%) set to the unused
     * remainder of the passed monitor.
     *
     * @param exec the monitor to use the unused progress from
     * @return a sub-progress that uses up the entire remainder from the
     *         argument
     */
    private static ExecutionMonitor createSubExecWithRemainder(
            final ExecutionMonitor exec) {
        Double progress = exec.getProgressMonitor().getProgress();
        if (progress == null) {
            progress = new Double(0.0);
        }
        double remainder = 1.0 - progress.doubleValue();
        return exec.createSubProgress(remainder);
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
            final FileReaderNodeSettings settings, final ExecutionMonitor exec)
            throws IOException, InterruptedExecutionException {

        BufferedFileReader reader = settings.createNewInputReader();
        FileTokenizer tokenizer = new FileTokenizer(reader);
        long fileSize = reader.getFileSize();

        tokenizer = new FileTokenizer(reader);

        tokenizer.setSettings(settings);

        int linesRead = 0;
        int columns = 0; // column counter per line
        int numOfCols = -1; // num of cols with these settings
        int maxNumOfCols = -1; // num of cols incl. some empty tokens at EOR
        boolean useSettings = false; // set it true to use these settings.
        int consEmptyTokens = 0; // consecutive empty tokens read

        while (true) {

            String token = tokenizer.nextToken();
            if (fileSize > 0) {
                exec.setProgress(reader.getNumberOfBytesRead()
                        / (double)fileSize);
            }
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
                    try {
                        if (cutItShort(exec) && (linesRead > NUMOFLINES)) {
                            // cutItShort also checks for interrupts
                            settings.setAnalyzeUsedAllRows(false);
                            break;
                        }
                    } catch (InterruptedExecutionException iee) {
                        tokenizer.closeSourceStream();
                        throw iee;
                    }

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
                                if (numOfCols > 1) {
                                    // if we get more than one col settings
                                    // look reasonable
                                    useSettings = true;
                                }
                            } else {
                                numOfCols = columns;
                                if (numOfCols <= 1) {
                                    // we don't need this delimiter if we put
                                    // everything in one column
                                    useSettings = false;
                                    break;
                                }
                                useSettings = true;
                            }
                        } else {
                            if (settings.ignoreEmptyTokensAtEndOfRow()) {
                                if ((columns - consEmptyTokens) > maxNumOfCols) {
                                    // we read more non-empty columns than we
                                    // could
                                    // fill (in other rows) with empty tokens
                                    useSettings = false;
                                    break;
                                }
                                if (columns < numOfCols) {
                                    // even with empty tokens this line has not
                                    // enough columns
                                    useSettings = false;
                                    break;
                                }
                                if (columns < maxNumOfCols) {
                                    // "maxNumOfCols" is the maximum number all
                                    // rows can deliver.
                                    maxNumOfCols = columns;
                                }
                                if ((columns - consEmptyTokens) > numOfCols) {
                                    // Adjust the number of "hard" columns
                                    numOfCols = columns - consEmptyTokens;
                                    if (numOfCols > 1) {
                                        useSettings = true;
                                    }
                                }
                                // "hard" columns must be less than the soft
                                // cols
                                assert numOfCols <= maxNumOfCols;
                            } else {
                                // make sure we always get the same number of
                                // cols
                                if (columns != numOfCols) {
                                    // not good. Getting different number of
                                    // columns in different lines.
                                    useSettings = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                consEmptyTokens = 0;
                columns = 0;

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
            final FileReaderNodeSettings settings, final ExecutionMonitor exec)
            throws IOException, InterruptedExecutionException {

        BufferedFileReader reader = settings.createNewInputReader();
        FileTokenizer tokenizer = new FileTokenizer(reader);
        tokenizer.setSettings(settings);
        double fileSize = reader.getFileSize();

        int linesRead = 0;
        int colCount = 0; // the counter per line
        int numOfCols = 0; // the maximum
        int consEmptyTokens = 0; // consecutive empty tokens

        try {
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
                        // we are looking for the maximum - those empty tokens
                        // should not contribute to it.
                        colCount -= consEmptyTokens;
                    }
                    if (colCount > numOfCols) {
                        // we are supposed to return the maximum
                        numOfCols = colCount;
                        settings.setColumnNumDeterminingLineNumber(tokenizer
                                .getLineNumber());
                    }

                    colCount = 0;
                    consEmptyTokens = 0;

                    if (token == null) {
                        break;
                    }

                    if (cutItShort(exec)) {
                        // cutItShort also checks for interrupts
                        if (linesRead >= NUMOFLINES) {
                            settings.setAnalyzeUsedAllRows(false);
                            break;
                        }
                        exec.setProgress(linesRead / (double)NUMOFLINES);
                    } else {
                        if (fileSize > 0) {
                            exec.setProgress(reader.getNumberOfBytesRead()
                                    / fileSize);
                        }
                    }
                }

            }
        } finally {
            tokenizer.closeSourceStream();
        }
        return numOfCols;
    }

    /**
     * Little helper class for the check header method. Holds the prefix and the
     * running index of possible row or column headers.
     *
     * @author ohl, University of Konstanz
     */
    static final class HeaderHelper {
        /* the prefix of all row/col headers (could be empty) */
        private final String m_prefix;

        /* the index of the first character of the running index */
        private final int m_indexStartIdx;

        /* the last index seen (to ensure increasing indices) */
        private long m_lastIndex;

        /**
         * Stores the prefix all headers should start with (could be empty), the
         * index in the string, where the running index of the headers start and
         * the first running index seen (to ensure increasing indices).
         *
         * @param prefix the prefix of all headers (could be empty but not
         *            null).
         * @param indexStartIdx the index in the header the running index starts
         *            at. Must not be negative.
         * @param firstRunningIndex the first running index of the first header
         */
        private HeaderHelper(final String prefix, final int indexStartIdx,
                final long firstRunningIndex) {
            if (prefix == null) {
                throw new NullPointerException(
                        "The row ID prefix can't be null.");
            }
            if (indexStartIdx < 0) {
                throw new IllegalArgumentException(
                        "The start index can't be negative.");
            }
            m_prefix = prefix;
            m_indexStartIdx = indexStartIdx;
            m_lastIndex = firstRunningIndex;
        }

        /**
         * Checks if the specified header fits in the sequence of consecutive
         * headers. That is, it must consist of the stored prefix followed by a
         * number (the running index) that must be larger than the last one
         * seen. It is assumed that all headers are checked through this method
         * in the order they are read (because this method stores the last
         * running index seen).<br>
         * If false is returned, this method doesn't change its last running
         * index seen.
         *
         * @param headerToTest the next header in the file to test.
         * @return true, if this header fits in the sequence of header - with
         *         its prefix and its running index.
         */
        boolean testNextHeader(final String headerToTest) {
            if (headerToTest == null || headerToTest.isEmpty()) {
                return false;
            }
            if (headerToTest.length() <= m_indexStartIdx) {
                // we need that prefix
                return false;
            }
            if (!headerToTest.substring(0, m_indexStartIdx).equals(m_prefix)) {
                // the (possibly empty) prefix must match
                return false;
            }

            // see if it has a running index after the prefix
            long newIdx;
            try {
                newIdx =
                        Long.parseLong(headerToTest.substring(m_indexStartIdx));
            } catch (NumberFormatException nfe) {
                // this header doesn't have an index. ByeBye.
                return false;
            }
            if (newIdx <= m_lastIndex) {
                // we want the index to grow. This way we ensure unique headers.
                return false;
            }

            m_lastIndex = newIdx;
            return true;
        }

        /**
         * from the header passed it extracts the prefix and the running index
         * and stores them in the returned helper class. If the specified header
         * has no index at the end, it returns null.
         *
         * @param header the (possible) header to examine
         * @return a class holding the (possibly empty) prefix and the running
         *         index of the specified header, or null, if the header has no
         *         prefix
         */
        static HeaderHelper extractPrefixAndIndexFromHeader(final String header) {
            if (header == null || header.isEmpty()) {
                return null;
            }
            int p;
            for (p = header.length() - 1; p >= 0; p--) {
                if (!Character.isDigit(header.charAt(p))) {
                    break;
                }
            }
            // p is now the first idx from the end with a not-a-number-char, or
            // -1

            p = p + 1; // now it points at the first number of the header index

            if (p == header.length()) {
                // we want a running index at the end of the header
                return null;
            }

            String prefix = header.substring(0, p);
            String index = header.substring(p);

            long idx;
            try {
                idx = Long.parseLong(index); // to make sure idx increases
            } catch (NumberFormatException nfe) {
                // it is a number - but too big for a long. Can't do it.
                return null;
            }

            return new HeaderHelper(prefix, p, idx);

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            String result = "";
            if (m_prefix.isEmpty()) {
                result += "No prefix. ";
            } else {
                result += "Prefix: '" + m_prefix + "' ";
            }
            result += "Index starts at " + m_indexStartIdx;
            result += ". Last Index: " + m_lastIndex;
            return result;
        }
    }

}
