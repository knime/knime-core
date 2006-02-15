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

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;

/**
 * The data table displayed in the file reader's dialog's preview. We need an
 * extra incarnation of a data table (different from from the
 * <code>FileReaderTable</code>), because if settings are not correct yet,
 * the table in the preview must not throw any exception on unexpected or
 * invalid data it reads (which the "normal" file table does). Thus, this table
 * returns a row iterator that will create an error row when a error occurs
 * during file reading. It will end the table after the errornous element was
 * read.
 * 
 * @author ohl, University of Konstanz
 */
public class FileReaderPreviewTable extends FileTable {

    private boolean m_errorOccured;

    /**
     * Creates a new table, its like the "normal" <code>FileTable</code>,
     * just not failing on invalid data files.
     * 
     * @param settings settings for the underlying <code>FileTable</code>.
     * @param tableSpec table spec for the underlying <code>FileTable</code>.
     * @see FileTable
     */
    FileReaderPreviewTable(final DataTableSpec tableSpec,
            final FileReaderNodeSettings settings) {
        super(tableSpec, settings);
        m_errorOccured = false;
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public RowIterator iterator() {
        return new FileReaderPreviewRowIterator(super.iterator(), this);
    }

    /**
     * this sets the flag indicating that the row iterator ended the table with
     * an error.
     */
    void setErrorFlag() {
        m_errorOccured = true;
    }

    /**
     * @return true if an error occured in an underlying row iterator. Meaning
     *         the table contains invalid data. NOTE: if false is returned it is
     *         not guaranteed that all data in the table is valid. It could be
     *         that no row iterator reached the invalid data yet. 
     */
    boolean getErrorOccured() {
        return m_errorOccured;
    }
}
