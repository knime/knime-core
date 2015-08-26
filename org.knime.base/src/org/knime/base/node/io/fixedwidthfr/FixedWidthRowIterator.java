/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   15.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.base.node.io.filereader.FileReaderException;
import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Tim-Oliver Buchholz
 */
public class FixedWidthRowIterator extends CloseableRowIterator {

    private DataTableSpec m_tableSpec;

    private FixedWidthFRSettings m_nodeSettings;

    private ExecutionContext m_exec;

    private BufferedFileReader m_inputStream;

    private FixedWidthTokenizer m_tokenizer;

    private String[] m_missingValuePatterns;

    private String[] m_formatParameters;

    private DataCellFactory m_dataCellFactory;

    private int m_lineNumber;

    // The junk size after which a new progress is reported. (yet 512 KByte)
    private static final long PROGRESS_JUNK_SIZE = 1024 * 512;

    /* counts the progress reports */
    private long m_lastReport;

    private boolean m_exceptionThrown;

    /**
     *
     * @param nodeSettings the current node settings
     * @param tableSpec the DataTableSpec
     * @param exec the execution context
     * @throws IOException thrown by the BufferedFileReader
     */
    FixedWidthRowIterator(final FixedWidthFRSettings nodeSettings, final DataTableSpec tableSpec,
        final ExecutionContext exec) throws IOException {

        m_tableSpec = tableSpec;
        m_nodeSettings = nodeSettings;
        m_exec = exec;
        m_lastReport = 0;

        m_inputStream = m_nodeSettings.createNewInputReader();
        m_tokenizer = new FixedWidthTokenizer(m_inputStream, m_nodeSettings);
        m_missingValuePatterns = m_nodeSettings.getMissingValuePatterns();
        m_formatParameters = m_nodeSettings.getFormatParameters();

        m_dataCellFactory = new DataCellFactory(exec);

        m_lineNumber = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        if (m_exceptionThrown) {
            return false;
        } else {
            String token;

            while (true) {
                token = m_tokenizer.nextToken();

                if (token == null) {
                    // Reading the EOF closes the stream.
                    return false;
                }

                m_tokenizer.pushBack();
                return true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {

        int rowLength = m_tableSpec.getNumColumns();
        int createdCols = 0;

        String token = null;
        String rowHeader;

        if (!hasNext()) {
            throw new NoSuchElementException("The row iterator proceeded beyond the last line of '"
                + m_nodeSettings.getFileLocation().toString() + "'.");
        }

        DataCell[] row;
        if (!m_nodeSettings.getHasRowHeader()) {
            rowHeader = "Row" + m_lineNumber++;
            row = new DataCell[rowLength];
        } else {
            rowHeader = m_tokenizer.nextToken();
            row = new DataCell[rowLength];
        }

        DataColumnSpec cSpec = null;
        while (createdCols < rowLength) {
            m_dataCellFactory.setMissingValuePattern(m_missingValuePatterns[createdCols]);
            m_dataCellFactory.setFormatParameter(m_formatParameters[createdCols]);
            token = m_tokenizer.nextToken();
            if (!m_tokenizer.getReachedEndOfLine()) {
                cSpec = m_tableSpec.getColumnSpec(createdCols);
                DataCell result = m_dataCellFactory.createDataCellOfType(cSpec.getType(), token);

                if (result != null) {
                    row[createdCols] = result;
                } else {
                    // something went wrong during cell creation.

                    // figure out which column we were trying to read
                    int errCol = 0;
                    while (errCol < row.length && row[errCol] != null) {
                        errCol++;
                    }
                    // create an error message
                    String errorMsg = m_dataCellFactory.getErrorMessage();
                    errorMsg +=
                        " In line " + m_tokenizer.getLineNumber() + " (" + rowHeader + ") at column #" + errCol + " ('"
                            + m_tableSpec.getColumnSpec(errCol).getName() + "').";

                    assert rowHeader != null;
                    // create a data row showing where things went
                    // wrong, and close the stream
                    throw prepareForException(errorMsg, m_tokenizer.getLineNumber(), rowHeader, row);
                }
            } else {
                // no more characters in this line but we need more columns
                // just add missing cells
                row[createdCols] = new MissingCell(null);
            }
            createdCols++;
        }

        double readBytes = m_inputStream.getNumberOfBytesRead();
        if (m_exec != null && m_inputStream.getFileSize() > 0 && readBytes / PROGRESS_JUNK_SIZE > m_lastReport) {
            // assert readBytes <= m_frSettings.getDataFileSize();
            m_exec.setProgress(readBytes / m_inputStream.getFileSize());
            m_lastReport++;
        }
        return new DefaultRow(rowHeader, row);
    }

    /**
     * Call this before releasing the last reference to this iterator. It closes the underlying source. Especially if
     * the iterator didn't run to the end of the table, it is required to call this method. Otherwise the file handle is
     * not released until the garbage collector cleans up. A call to {@link #next()} after disposing of the iterator has
     * undefined behavior.
     */
    @Override
    public void close() {
        try {
            m_inputStream.close();
        } catch (IOException ioe) {
            // then don't close it
        }
    }

    /*
     * !!!!!!!!!! Creates the exception object (storing the last read items in
     * the row of the exception), sets the global "exception thrown" flag, and
     * closes the input stream. !!!!!!!!!!
     */
    private FileReaderException prepareForException(final String msg, final int lineNumber, final String rowHeader,
        final DataCell[] cellsRead) {

        /*
         * indicate we have thrown (actually will throw...) an exception, and
         * close the stream as we will not read anymore from the stream after
         * the exception.
         */
        m_exceptionThrown = true;

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
}