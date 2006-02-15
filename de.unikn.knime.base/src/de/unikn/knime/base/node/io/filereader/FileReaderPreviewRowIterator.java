/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   11.01.2006 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.data.def.DefaultStringCell;

/**
 * The iterator for the <code>FileReaderPreviewTable</code>. Wraps the
 * iterator of the underlying file table. Catches exceptions thrown by the
 * underlying iterator, creates an "error row" instead and will end the table.
 * 
 * @author ohl, University of Konstanz
 */
class FileReaderPreviewRowIterator extends RowIterator {

    // the underlying iterator we wrap and catch the exceptions from
    private RowIterator m_rowIter;

    private FileReaderPreviewTable m_table;

    // we end it after an error occured, this flag indicates that.
    private boolean m_done;

    /**
     * a new non-failing file row iterator. It wrappes the passed iterator and
     * catches its exceptions, converting them into rows with error messages.
     * 
     * @param rowIterator the row iterator to wrap
     * @param previewTable the corrensponding talbe this is the iterator from.
     *            Needed to pull the sprc from for creating the row in case of
     *            an error/exception and to tell the table an errro occured.
     */
    FileReaderPreviewRowIterator(final RowIterator rowIterator,
            final FileReaderPreviewTable previewTable) {
        m_rowIter = rowIterator;
        m_table = previewTable;
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return !m_done && m_rowIter.hasNext();
    }

    /**
     * @see java.util.Iterator#next()
     */
    @Override
    public DataRow next() {
        DataRow nextRow = null;

        try {
            nextRow = m_rowIter.next();
        } catch (FileReaderException fre) {
            // after an error occured all missing elements read would be
            // junk. Thus we end the table after the first exception.
            m_done = true;
            m_table.setErrorFlag();
            nextRow = createErrorRow(fre.getMessage());
        }

        return nextRow;
    }

    /**
     * Creates a row, somehow displaying the error message. Uses the table spec
     * to create missing cells for the data cells.
     * 
     * @param errMsg
     * @param rowNum
     * @return a row with a rowKey
     */
    private DataRow createErrorRow(final String errMsg) {

        DataCell rowID = new DefaultStringCell("ERROR!!");
        DataCell[] cells = new DataCell[m_table.getDataTableSpec()
                .getNumColumns()];
        if (cells.length > 0) {
            // the first column displays the error message,
            cells[0] = new ErrorCell(errMsg, m_table.getDataTableSpec()
                    .getColumnSpec(0).getType());
            // all other cells are just missing cells
            for (int c = 1; c < cells.length; c++) {
                cells[c] = m_table.getDataTableSpec().getColumnSpec(0)
                        .getType().getMissingCell();
            }
        }
        return new DefaultRow(rowID, cells);
    }

    private class ErrorCell extends DataCell {

        private final DataType m_type;

        private final String m_msg;

        /**
         * Creates another instance of a missing cell for the specified type.
         * This cell will return the specified message in the
         * <code>toString</code> method.
         * 
         * @param msg
         * @param type
         */
        ErrorCell(final String msg, final DataType type) {
            m_msg = msg;
            m_type = type;
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            // guaranteed not to be called on and with a missing cell.....
            return false;
        }

        @Override
        public DataType getType() {
            return m_type;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean isMissing() {
            return true;
        }

        @Override
        public String toString() {
            return m_msg;
        }

    }

}
