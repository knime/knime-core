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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.io.filereader;

import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MutableInteger;

import org.knime.base.node.io.filetokenizer.FileTokenizer;
import org.knime.base.node.io.filetokenizer.FileTokenizerException;

/**
 * Row iterator for the {@link FileTable}.
 * <p>
 * The iterator provides a method
 * 
 * @author Peter Ohl, University of Konstanz
 * 
 * @see org.knime.core.data.RowIterator
 */
final class FileRowIterator extends RowIterator {

    /* The tokenizer reads the next token from the input stream. */
    private final FileTokenizer m_tokenizer;

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

    // if true we need to replace the char in the double tokens with a '.'
    private boolean m_customDecimalSeparator;

    private char m_decSeparator;
    
    /* the execution context the progress is reported to */
    private ExecutionContext m_exec;
    
    /* counts the progress reportings */
    private long m_lastReport;

    /**
     * The RowIterator for the FileTable.
     * 
     * @param tableSpec the spec defining the structure of the rows to create
     * @param frSettings object containing the wheres and hows to read the data
     * @param exec the execution context to report the progess to
     * @throws IOException if it couldn't open the data file
     */
    FileRowIterator(final FileReaderSettings frSettings,
            final DataTableSpec tableSpec, final ExecutionContext exec)
            throws IOException {

        m_tableSpec = tableSpec;
        m_frSettings = frSettings;

        m_exec = exec;
        m_lastReport = 0;

        m_tokenizer = new FileTokenizer(m_frSettings.createNewInputReader());

        // set the tokenizer related settings in the tokenizer
        m_tokenizer.setSettings(frSettings);

        m_decSeparator = frSettings.getDecimalSeparator();
        m_customDecimalSeparator = m_decSeparator != '.';

        m_rowNumber = 1;
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
     * @see org.knime.core.data.RowIterator#hasNext()
     */
    @Override
    public boolean hasNext() {

        if (m_exceptionThrown) {
            // after we've thrown an exception don't even try to read more.
            return false;
        }
        String token;

        // we must eat all empty lines to see if there is more meat in the file
        // DO NOT call this function if you are not at the beginning of the row
        while (true) {
            token = m_tokenizer.nextToken();

            if (token == null) {
                // Reading the EOF closes the stream.
                return false;
            }

            if (m_frSettings.getIgnoreEmtpyLines()
                    && m_frSettings.isRowDelimiter(token)) {
                // get the next token.
                continue;
            }

            m_tokenizer.pushBack();
            return true;
        }
    }

    /**
     * @see org.knime.core.data.RowIterator#next()
     */
    @Override
    public DataRow next() {
        int noOfCols = m_tableSpec.getNumColumns();
        String token = null;
        boolean isMissingCell;
        DataCell rowHeader;
        DataCell[] row = new DataCell[noOfCols];

        // before anything else: check if there is more in the stream
        // this MUST be called, because it is the procedure that removes empty
        // lines (if we are supposed to).
        if (!hasNext()) {
            throw new NoSuchElementException(
                    "The row iterator proceeded beyond the last line of '"
                            + m_frSettings.getDataFileLocation().toString()
                            + "'.");
        }

        // counts the number of columns we've read and created
        int createdCols = 0;

        // first, create a row header. This will also read it from file, if any.
        try {
            rowHeader = createRowHeader(m_rowNumber);
        } catch (FileTokenizerException fte) {
            throw prepareForException(fte.getMessage() + " (line: "
                    + m_tokenizer.getLineNumber() 
                    + " source: '" + m_frSettings.getDataFileLocation()
                    + "')", m_tokenizer.getLineNumber(), new StringCell("ERR"), 
                    row);
        }

        // we made sure before that there is at least one token in the stream
        assert rowHeader != null;

        // Now, read the columns until we have enough or see a row delimiter
        while (createdCols < noOfCols) {

            try {
                token = m_tokenizer.nextToken();
            } catch (FileTokenizerException fte) {
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

            DataColumnSpec cSpec = m_tableSpec.getColumnSpec(createdCols);
            // now get that new cell (it throws something at us if it couldn't)
            row[createdCols] = createNewDataCellOfType(cSpec.getType(), token,
                    isMissingCell, rowHeader, row);

            createdCols++;

        } // end of while(createdCols < noOfCols)

        // In case we've seen a row delimiter before the row was complete:
        // puke and die
        int lineNr = m_tokenizer.getLineNumber();
        if ((lineNr > 0) && (token != null) && (token.equals("\n"))) {
            lineNr--;
        }
        if (createdCols < noOfCols) {
            throw prepareForException("Too few data elements in row "
                    + "(line: " + lineNr + " (" + rowHeader + "), source: '"
                    + m_frSettings.getDataFileLocation() + "')", lineNr,
                    rowHeader, row);
        }

        token = m_tokenizer.nextToken();

        // eat all empty tokens til the end of the row, if we're supposed to
        while (m_frSettings.ignoreEmptyTokensAtEndOfRow()
                && !m_frSettings.isRowDelimiter(token) && token.equals("")
                && (!m_tokenizer.lastTokenWasQuoted())) {
            try {
                token = m_tokenizer.nextToken();
            } catch (FileTokenizerException fte) {
                throw prepareForException(fte.getMessage() + "(line: " + lineNr
                        + " (" + rowHeader + "), source: '"
                        + m_frSettings.getDataFileLocation() + "')", lineNr,
                        rowHeader, row);
            }
        }
        // now read the row delimiter from the file, and in case there are more
        // data items in the file than we needed for one row: barf and die.
        if (!m_frSettings.isRowDelimiter(token)) {
            throw prepareForException("Too many data elements in row "
                    + "(line: " + lineNr + " (" + rowHeader + "), source: '"
                    + m_frSettings.getDataFileLocation() + "')", lineNr,
                    rowHeader, row);
        }
        m_rowNumber++;

        // report progress
        // only if an execution context exists an if the underlying
        // URL is a file whose size can be determined
        double readBytes = (double)m_tokenizer.getReadBytes();
        if (m_exec != null && m_frSettings.getDataFileSize() > 0
                && readBytes / PROGRESS_JUNK_SIZE > m_lastReport) {
            // assert readBytes <= m_frSettings.getDataFileSize();
            m_exec.setProgress((double)readBytes
                    / (double)m_frSettings.getDataFileSize());
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
            final DataCell rowHeader, final DataCell[] row) {

        if (createMissingCell) {
            return DataType.getMissingCell();
        }
        if (type.equals(StringCell.TYPE)) {
            return new StringCell(data);
            // also timming strings now. As per TG.
        } else if (type.equals(IntCell.TYPE)) {
            try {
                int val = Integer.parseInt(data);
                return new IntCell(val);
            } catch (NumberFormatException nfe) {
                int col = 0;
                while (col < row.length && row[col] != null) {
                    col++;
                }
                throw prepareForException("Wrong data format. In line "
                        + m_tokenizer.getLineNumber() + " (" + rowHeader 
                        + ") read '" + data
                        + "' for an integer (in column #" + col 
                        + ")." , m_tokenizer.getLineNumber(),
                        rowHeader, row);
            }
        } else if (type.equals(DoubleCell.TYPE)) {
            String dblData = data;
            if (m_customDecimalSeparator) {
                // we must reject tokens with a '.'.
                if (data.indexOf('.') >= 0) {
                    int col = 0;
                    while (col < row.length && row[col] != null) {
                        col++;
                    }
                    throw prepareForException("Wrong data format. In line "
                            + m_tokenizer.getLineNumber() + " (" + rowHeader
                            + ") read '" + data
                            + "' for a floating point (in column #" + col 
                            + ").", m_tokenizer.getLineNumber(), rowHeader, 
                            row);
                }
                dblData = data.replace(m_decSeparator, '.');
            }
            try {
                double val = Double.parseDouble(dblData);
                return new DoubleCell(val);
            } catch (NumberFormatException nfe) {
                int col = 0;
                while (col < row.length && row[col] != null) {
                    col++;
                }
                throw prepareForException("Wrong data format. In line "
                        + m_tokenizer.getLineNumber() + " (" + rowHeader 
                        + ") read '" + data
                        + "' for a floating point (in column #" + col
                        + ").", m_tokenizer
                        .getLineNumber(), rowHeader, row);
            }
        } else if (type.equals(SmilesTypeHelper.INSTANCE.getSmilesType())) {
            try {
                
                return SmilesTypeHelper.INSTANCE.newInstance(data);
            
            } catch (Throwable t) {
                int col = 0;
                while (col < row.length && row[col] != null) {
                    col++;
                }
                String msg = "Error during SMILES cell creation: ";
                if (t.getMessage() != null) {
                    msg += t.getMessage();
                } else {
                    msg += "<no details available>";
                }
                msg += " (line: " + m_tokenizer.getLineNumber() + "("
                                + rowHeader + "), column #" + col + ").";
                throw prepareForException(msg, m_tokenizer.getLineNumber(), 
                        rowHeader, row);
            }
        } else {
            throw prepareForException("Cannot create DataCell of type "
                    + type.toString() + ". Looks like an internal error.",
                    m_tokenizer.getLineNumber(), rowHeader, row);
        }
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
    private StringCell createRowHeader(final int rowNumber) {

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
            // see if it's unique - and if not make it unique.
            newRowHeader = uniquifyRowHeader(newRowHeader);

            return new StringCell(newRowHeader);

        } else {

            return new StringCell(m_rowHeaderPrefix + rowNumber);

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
                // put back the old (incr.) suffix (overriden with NOSUFFIX).
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
            final int lineNumber, final DataCell rowHeader,
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

        DataCell errRowHeader = new StringCell("ERROR_ROW ("
                + rowHeader.toString() + ")");

        DataRow errRow = new DefaultRow(errRowHeader, errCells);

        return new FileReaderException(msg, errRow, lineNumber);

    }
}
