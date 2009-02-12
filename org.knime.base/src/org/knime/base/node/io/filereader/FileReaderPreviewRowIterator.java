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
 *   11.01.2006 (ohl): created
 */
package org.knime.base.node.io.filereader;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;

/**
 * The iterator for the
 * {@link FileReaderPreviewTable}. Wraps
 * the iterator of the underlying file table. Catches exceptions thrown by the
 * underlying iterator, returns an "error row" instead and will end the table.
 * If an error occurres it sets an error message in the underlying table.
 *
 * @author Peter, University of Konstanz
 */
class FileReaderPreviewRowIterator extends RowIterator {

    // the underlying iterator we wrap and catch the exceptions from
    private FileRowIterator m_rowIter;

    private FileReaderPreviewTable m_table;

    // we end it after an error occured, this flag indicates that.
    private boolean m_done;

    /**
     * A new non-failing file row iterator. It wraps the passed iterator and
     * catches its exceptions, converting them into rows with error messages.
     *
     * @param rowIterator the row iterator to wrap
     * @param previewTable the corresponding table this is the iterator from.
     *            Needed to pull the sprc from for creating the row in case of
     *            an error/exception and to tell the table an error occurred.
     */
    FileReaderPreviewRowIterator(final FileRowIterator rowIterator,
            final FileReaderPreviewTable previewTable) {
        m_rowIter = rowIterator;
        m_table = previewTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        dispose();
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
        m_rowIter.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return !m_done && m_rowIter.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        DataRow nextRow = null;

        try {
            nextRow = m_rowIter.next();
        } catch (FileReaderException fre) {
            // after an error occurred all missing elements read would be
            // junk. Thus we end the table after the first exception.
            m_done = true;
            m_table.setError(fre);
            nextRow = fre.getErrorRow();

        }

        return nextRow;
    }
}
