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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.io.filereader;

import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MutableInteger;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerException;

/**
 * Row iterator for the {@link FileTable}.
 * <p>
 * The iterator provides a method
 *
 * @author Peter Ohl, University of Konstanz
 *
 * @see org.knime.core.data.RowIterator
 */
class FileRowIterator extends RowIterator {

    /* The tokenizer reads the next token from the input stream. */
    private final Tokenizer m_tokenizer;

    private final BufferedFileReader m_source;

    // keep a reference for the filereader settings.
    private final FileReaderSettings m_frSettings;

    /* Keep a reference to the spec defining the table's strucutre. */
    private final DataTableSpec m_tableSpec;

    /* Counts the number of rows read. */
    private int m_rowNumber;

    // the resolved row header prefix used for each row, if set. The constructor
    // resolves all possible user settings and default values and sets this.
    private final String m_rowHeaderPrefix;

    // a hash set where we store row header read in - to ensure ID uniquity, and
    // we associate with it the last used suffix, to make it unique
    private final HashMap<String, Number> m_rowIDhash;

    // Used in the above hash to indicate that duplicate of that row was found.
    private static final Integer NOSUFFIX = new Integer(0);

    // The junk size after which a new progress is reported. (yet 512 KByte)
    private static final long PROGRESS_JUNK_SIZE = 1024 * 512;

    // if that is true we don't return any more rows.
    private boolean m_exceptionThrown;

    // the maximum number of rows this iterator will produce
    private final long m_maxNumOfRows;

    private boolean m_fileWasNotCompletelyRead;

    private final boolean[] m_skipColumns;

    // factory used to create the cells from the string data read in
    private final DataCellFactory m_cellFactory;

    /* the execution context the progress is reported to */
    private ExecutionContext m_exec;

    /* counts the progress reports */
    private long m_lastReport;

    /**
     * The RowIterator for the FileTable.
     *
     * @param frSettings object containing the wheres and hows to read the data
     * @param tableSpec the spec defining the structure of the rows to create
     *            (the result spec after applying the next argument
     *            'skipColumns')
     * @param skipColumns array with the element set to true if the
     *            corresponding column should be skipped (i.e. read but not be
     *            included in the row). The array must have the length of the
     *            'original' column number (in the file), the specified table
     *            spec is the new one (with less columns).
     * @param exec the execution context to report the progress to
     * @throws IOException if it couldn't open the data file
     */
    FileRowIterator(final FileReaderSettings frSettings,
            final DataTableSpec tableSpec, final boolean[] skipColumns,
            final ExecutionContext exec) throws IOException {

        if (skipColumns.length < tableSpec.getNumColumns()) {
            throw new IllegalArgumentException("The number of columns can't"
                    + " be larger than the spec for columns to skip");
        }
        int cols = 0;
        for (boolean b : skipColumns) {
            if (!b) {
                cols++;
            }
        }
        if (cols != tableSpec.getNumColumns()) {
            throw new IllegalArgumentException("The number of columns to "
                    + "include is different from the number of columns in the"
                    + " table spec.");
        }

        m_tableSpec = tableSpec;
        m_frSettings = frSettings;

        m_exec = exec;
        m_lastReport = 0;

        m_source = m_frSettings.createNewInputReader();
        m_tokenizer = new Tokenizer(m_source);

        // set the tokenizer related settings in the tokenizer
        m_tokenizer.setSettings(frSettings);

        // cell factory used to create the cells of each row
        m_cellFactory = new DataCellFactory();
        m_cellFactory.setDecimalSeparator(frSettings.getDecimalSeparator());
        m_cellFactory.setThousandsSeparator(frSettings.getThousandsSeparator());

        m_rowNumber = 1;
        if (m_frSettings.getMaximumNumberOfRowsToRead() < 0) {
            m_maxNumOfRows = Long.MAX_VALUE;
        } else {
            m_maxNumOfRows = m_frSettings.getMaximumNumberOfRowsToRead();
        }
        m_fileWasNotCompletelyRead = false; // normally we fully read the file

        m_skipColumns = skipColumns;

        m_exceptionThrown = false;

        // set the row prefix here (so we don't have to go through this for each
        // row separately). If this prefix is set it will be used - otherwise
        // it's safe to assume the file contains row headers!
        if (frSettings.getFileHasRowHeaders()) {
            // settings tell us to use the first column as row headers. We will.
            m_rowHeaderPrefix = null;
        } else {
            // Won't get them from the file. Get the user settings or the
            // default from the settings structure
            if (frSettings.getRowHeaderPrefix() != null) {
                m_rowHeaderPrefix = frSettings.getRowHeaderPrefix();
            } else {
                m_rowHeaderPrefix = FileReaderSettings.DEF_ROWPREFIX;
            }
        }

        m_rowIDhash = new HashMap<String, Number>();

        // if the column headers are stored in the data file, we must read
        // them (the first line) and discard them (if they are actually used
        // from the file they should have been stored in the table spec).
        if (frSettings.getFileHasColumnHeaders()) {
            if (hasNext()) { // call this first to eat up empty lines
                String token = m_tokenizer.nextToken();
                while (!frSettings.isRowDelimiter(token)) {
                    token = m_tokenizer.nextToken();
                }
            }
        }

    } // FileRowIterator(FileTableSpec)

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        m_source.close();
        super.finalize();
    }

    /**
     * Call this before releasing the last reference to this iterator. It closes
     * the underlying source. Especially if the iterator didn't run to the end
     * of the table, it is required to call this method. Otherwise the file
     * handle is not released until the garbage collector cleans up. A call to
     * {@link #next()} after disposing of the iterator has undefined behavior.
     */
    public void dispose() {
        try {
            m_source.close();
        } catch (IOException ioe) {
            // then don't close it
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {

        boolean result;

        if (m_exceptionThrown) {
            // after we've thrown an exception don't even try to read more.
            result = false;
        } else {

            String token;

            // we must eat all empty lines to see if there is more meat in the
            // file
            // DO NOT call this function if you are not at the beginning of the
            // row
            while (true) {
                token = m_tokenizer.nextToken();

                if (token == null) {
                    // Reading the EOF closes the stream.
                    result = false;
                    break;
                }

                if (m_frSettings.getIgnoreEmtpyLines()
                        && m_frSettings.isRowDelimiter(token)) {
                    // get the next token.
                    continue;
                }

                m_tokenizer.pushBack();
                result = true;
                break;
            }
        }

        // rowNumber is number of the next row!
        if (m_rowNumber > m_maxNumOfRows) {
            m_fileWasNotCompletelyRead = result; // incorrect on exception
            result = false;
        }

        if (!result) {
            m_tokenizer.closeSourceStream();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        int rowLength = m_tableSpec.getNumColumns();
        int colsToRead = m_skipColumns.length;

        assert rowLength <= colsToRead;

        String token = null;
        boolean isMissingCell;
        String rowHeader;
        DataCell[] row = new DataCell[rowLength];

        // before anything else: check if there is more in the stream
        // this MUST be called, because it is the procedure that removes empty
        // lines (if we are supposed to).
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "The row iterator proceeded beyond the last line of '"
                            + m_frSettings.getDataFileLocation().toString()
                            + "'.");
        }
        // counts the columns (tokens) read from the file
        int readCols = 0;
        // counts the number of columns we've created (excl. skipped columns)
        int createdCols = 0;

        // first, create a row header.
        // This will also read it from file, if supposed to.
        try {
            rowHeader = createRowHeader(m_rowNumber - 1);
        } catch (TokenizerException fte) {
            throw prepareForException(fte.getMessage() + " (line: "
                    + m_tokenizer.getLineNumber() + " source: '"
                    + m_frSettings.getDataFileLocation() + "')", m_tokenizer
                    .getLineNumber(), "ERR", row);
        }
        // we made sure before that there is at least one token in the stream
        assert rowHeader != null;
        // Now, read the columns until we have enough or see a row delimiter
        while (readCols < colsToRead) {

            try {
                token = m_tokenizer.nextToken();
            } catch (TokenizerException fte) {
                throw prepareForException(fte.getMessage() + " (line: "
                        + m_tokenizer.getLineNumber() + " (" + rowHeader
                        + ") source: '" + m_frSettings.getDataFileLocation()
                        + "')", m_tokenizer.getLineNumber(), rowHeader, row);
            }
            // row delims are returned as token
            if ((token == null) || m_frSettings.isRowDelimiter(token)) {
                // line ended early.
                m_tokenizer.pushBack();
                // we need the row delim in the file, for after the loop
                break;
            }
            // check if we have a missing cell (i.e. nothing between two
            // column delimiters).
            if (token.equals("") && (!m_tokenizer.lastTokenWasQuoted())) {
                isMissingCell = true;
            } else if (token.equals(m_frSettings
                    .getMissingValueOfColumn(createdCols))) {
                // equals(null) if it was not specified - which is fine.
                isMissingCell = true;
            } else {
                isMissingCell = false;
            }
            if (!m_skipColumns[readCols]) {
                DataColumnSpec cSpec = m_tableSpec.getColumnSpec(createdCols);
                // now get that new cell
                // (it throws an exception at us if it couldn't)
                row[createdCols] =
                        createNewDataCellOfType(cSpec.getType(), token,
                                isMissingCell, rowHeader, row);
                createdCols++;
            }
            readCols++;
        } // end of while(readCols < colsToRead)

        int lineNr = m_tokenizer.getLineNumber();
        if ((lineNr > 0) && (token != null) && (token.equals("\n"))) {
            lineNr--;
        }
        // In case we've seen a row delimiter before the row was complete:
        // puke and die - unless we are told otherwise
        if (m_frSettings.getSupportShortLines()) {
            // pad the row with missing values
            while (createdCols < rowLength) {
                row[createdCols++] = DataType.getMissingCell();
            }
        } else {
            if (createdCols < rowLength) {
                FileReaderException ex =
                        prepareForException("Too few data elements "
                                + "(line: " + lineNr + " (" + rowHeader
                                + "), source: '"
                                + m_frSettings.getDataFileLocation() + "')",
                                lineNr, rowHeader, row);
                if (m_frSettings.getColumnNumDeterminingLineNumber() >= 0) {
                    ex.setDetailsMessage("The number of columns was "
                            + "determined by the entries above line no."
                            + m_frSettings.getColumnNumDeterminingLineNumber());
                }
                throw ex;
            }
        }

        token = m_tokenizer.nextToken();

        // eat all empty tokens til the end of the row, if we're supposed to
        while (m_frSettings.ignoreEmptyTokensAtEndOfRow()
                && !m_frSettings.isRowDelimiter(token) && token.equals("")
                && (!m_tokenizer.lastTokenWasQuoted())) {
            try {
                token = m_tokenizer.nextToken();
            } catch (TokenizerException fte) {
                throw prepareForException(fte.getMessage() + "(line: " + lineNr
                        + " (" + rowHeader + "), source: '"
                        + m_frSettings.getDataFileLocation() + "')", lineNr,
                        rowHeader, row);
            }
        }
        // now read the row delimiter from the file, and in case there are more
        // data items in the file than we needed for one row: barf and die.
        if (!m_frSettings.isRowDelimiter(token)) {
            FileReaderException ex =
                    prepareForException("Too many data elements " + "(line: "
                            + lineNr + " (" + rowHeader + "), source: '"
                            + m_frSettings.getDataFileLocation() + "')",
                            lineNr, rowHeader, row);
            if (m_frSettings.getColumnNumDeterminingLineNumber() >= 0) {
                ex.setDetailsMessage("The number of columns was "
                        + "determined by line no."
                        + m_frSettings.getColumnNumDeterminingLineNumber());
            }
            throw ex;
        }
        m_rowNumber++;

        // report progress
        // only if an execution context exists an if the underlying
        // URL is a file whose size can be determined
        double readBytes = m_source.getNumberOfBytesRead();
        if (m_exec != null && m_source.getFileSize() > 0
                && readBytes / PROGRESS_JUNK_SIZE > m_lastReport) {
            // assert readBytes <= m_frSettings.getDataFileSize();
            m_exec.setProgress(readBytes / m_source.getFileSize());
            m_lastReport++;
        }
        return new DefaultRow(rowHeader, row);
    } // next()

    /**
     * The method creates a default {@link DataCell} of the type passed in, and
     * initializes its value from the <code>data</code> string (converting it
     * to the corresponding type). Throws a {@link NumberFormatException} if the
     * <code>data</code> argument couldn't be converted to the appropriate
     * type or a {@link IllegalStateException} if the <code> type </code> passed
     * in is not supported.
     *
     * @param type the type of DataCell to be created, supported are Double-,
     *            Int-, and StringTypes
     * @param data the string representation of the value that will be set in
     *            the DataCell created
     * @param createMissingCell If set <code>true</code> a missing cell of the
     *            passed type will be created. The <code>data</code> parameter
     *            is ignored then.
     * @param rowHeader the rowID - for nice error messages only
     * @param row the cells of the row created so far. Used for messages only.
     * @return data cell of the type specified in <code> type </code>
     *
     */
    private DataCell createNewDataCellOfType(final DataType type,
            final String data, final boolean createMissingCell,
            final String rowHeader, final DataCell[] row) {

        if (createMissingCell) {
            return DataType.getMissingCell();
        }

        DataCell result = m_cellFactory.createDataCellOfType(type, data);

        if (result != null) {

            return result;

        }

        // something went wrong during cell creation.

        // figure out which column we were trying to read
        int errCol = 0;
        while (errCol < row.length && row[errCol] != null) {
            errCol++;
        }
        // create an error message
        String errorMsg = m_cellFactory.getErrorMessage();
        errorMsg +=
                " In line " + m_tokenizer.getLineNumber() + " (" + rowHeader
                        + ") at column #" + errCol + " ('"
                        + m_tableSpec.getColumnSpec(errCol).getName() + "').";

        // create a data row showing where things went
        // wrong, and close the stream
        throw prepareForException(errorMsg, m_tokenizer.getLineNumber(),
                rowHeader, row);

    } // createNewDataCellOfType(Class,String,boolean)

    /*
     * Creates a StringCell containing the row header. If the filereader
     * settings tell us that there is one in the file - it will be read. The
     * header actually created depends on the member 'rowHeaderPrefix'. If it's
     * set (not null) it will be used to create the row header (plus the row
     * number) overriding any file row header just read. If it's null then the
     * one read from the file will be used (and there better be one). If the
     * file returns a missing token we create a row header like "
     * <missing>+RowNo". Returns null if EOF was reached before a row header (or
     * a delimiter) was read.
     */
    private String createRowHeader(final int rowNumber) {

        // the constructor sets m_rowHeaderPrefix if the file doesn't have one
        assert (m_frSettings.getFileHasRowHeaders()
                || (m_rowHeaderPrefix != null));

        // if there is a row header in the file we must read it - independend
        // of if we are going to use it or not.
        String fileHeader = null;
        if (m_frSettings.getFileHasRowHeaders()) {
            // read it away.
            fileHeader = m_tokenizer.nextToken();
            if (fileHeader == null) {
                return null; // seen EOF
            }

            if (m_frSettings.isRowDelimiter(fileHeader)) {
                // Oops, we've read an empty line. Push the delimiter back,
                // others need to see it too.
                m_tokenizer.pushBack();
                fileHeader = "";
            }
        }

        if (m_rowHeaderPrefix == null) {
            assert fileHeader != null;
            String newRowHeader;
            if (fileHeader.equals("") && !m_tokenizer.lastTokenWasQuoted()) {
                // seems we got a missing row delimiter. Let's build one.
                newRowHeader = DataType.getMissingCell().toString() + rowNumber;
            } else {
                newRowHeader = fileHeader;
            }

            if (m_frSettings.uniquifyRowIDs()) {
                // see if it's unique - and if not make it unique.
                newRowHeader = uniquifyRowHeader(newRowHeader);
            }

            return newRowHeader;

        } else {

            return m_rowHeaderPrefix + rowNumber;

        }
    }

    /*
     * checks if the newRowHeader is already in the hash set of all created row
     * headers and if so it adds some suffix to make it unique. It will return a
     * unique row header, which could be the same than the one passed in (and
     * adds any rowheader returned to the hash set).
     */
    private String uniquifyRowHeader(final String newRowHeader) {

        Number oldSuffix = m_rowIDhash.put(newRowHeader, NOSUFFIX);

        if (oldSuffix == null) {
            // haven't seen the rowID so far.
            return newRowHeader;
        }

        String result = newRowHeader;
        while (oldSuffix != null) {

            // we have seen this rowID before!
            int idx = oldSuffix.intValue();

            assert idx >= NOSUFFIX.intValue();

            idx++;

            if (oldSuffix == NOSUFFIX) {
                // until now the NOSUFFIX placeholder was in the hash
                assert idx - 1 == NOSUFFIX.intValue();
                m_rowIDhash.put(result, new MutableInteger(idx));
            } else {
                assert oldSuffix instanceof MutableInteger;
                ((MutableInteger)oldSuffix).inc();
                assert idx == oldSuffix.intValue();
                // put back the old (incr.) suffix (overridden with NOSUFFIX).
                m_rowIDhash.put(result, oldSuffix);
            }

            result = result + "_" + idx;
            oldSuffix = m_rowIDhash.put(result, NOSUFFIX);

        }

        return result;
    }

    /*
     * !!!!!!!!!! Creates the exception object (storing the last read items in
     * the row of the exception), sets the global "exception thrown" flag, and
     * closes the input stream. !!!!!!!!!!
     */
    private FileReaderException prepareForException(final String msg,
            final int lineNumber, final String rowHeader,
            final DataCell[] cellsRead) {

        /*
         * indicate we have thrown (actually will throw...) an exception, and
         * close the stream as we will not read anymore from the stream after
         * the exception.
         */
        m_exceptionThrown = true;
        m_tokenizer.closeSourceStream();

        DataCell[] errCells = new DataCell[cellsRead.length];
        System.arraycopy(cellsRead, 0, errCells, 0, errCells.length);

        for (int c = 0; c < errCells.length; c++) {
            if (errCells[c] == null) {
                errCells[c] = DataType.getMissingCell();
            }
        }

        String errRowHeader = "ERROR_ROW (" + rowHeader.toString() + ")";

        DataRow errRow = new DefaultRow(errRowHeader, errCells);

        return new FileReaderException(msg, errRow, lineNumber);

    }

    /**
     * The settings allow for specifying a maximum number of rows. This method
     * can be used to find out, if the source has more data than actually
     * returned by the iterator. The result is only accurate, after the
     * {@link #hasNext()} method returned <code>false</code>.
     *
     * @return true, if the iterator didn't return all rows of the source (due
     *         to its settings). Only accurate after the iterator finished
     *         ({@link #hasNext()} returned false).
     */
    public boolean iteratorEndedEarly() {
        return m_fileWasNotCompletelyRead;
    }

    /**
     * If the source read was a ZIP archive this method tests if there are more
     * than one entry in the archive. If the source was not compressed or a gzip
     * file, it always returns false. If the EOF has not been read from the
     * source, the result is always false.
     *
     * @return true, if the source was read til the end and it is a ZIP archive
     *         with more than one entry
     */
    public boolean zippedSourceHasMoreEntries() {
        return m_source.hasMoreZipEntries();
    }

    /**
     * @return if the underlying source is a ZIP archive it returns the entry
     *         read. Null if not a ZIP source.
     */
    public String getZipEntryName() {
        return m_source.getZipEntryName();
    }


}
