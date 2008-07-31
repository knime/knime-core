/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * The data table displayed in the file reader's dialog's preview. We need an
 * extra incarnation of a data table (different from from the {@link FileTable})
 * because if settings are not correct yet, the table in the preview must not
 * throw any exception on unexpected or invalid data it reads (which the
 * "normal" file table does). Thus, this table returns a row iterator that will
 * create an error row when a error occurs during file reading. It will end the
 * table after the erroneous element was read.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class FileReaderPreviewTable implements DataTable {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(FileReaderPreviewTable.class);

    private final FileTable m_table;

    private String m_errorMsg;

    private String m_errorDetail;

    private int m_errorLine;

    private final CopyOnWriteArrayList<ChangeListener> m_listeners;

    private final
            LinkedList<WeakReference<FileReaderPreviewRowIterator>> m_iterators;

    /**
     * Creates a new table, its like the "normal" {@link FileTable}, just not
     * failing on invalid data files.
     *
     * @param settings settings for the underlying <code>FileTable</code>
     * @param tableSpec table spec for the underlying <code>FileTable</code>
     * @param exec the execution context the progress is reported to
     * @see FileTable
     */
    FileReaderPreviewTable(final DataTableSpec tableSpec,
            final FileReaderNodeSettings settings,
            final ExecutionContext exec) {
        m_table = new FileTable(tableSpec, settings, exec);
        m_listeners = new CopyOnWriteArrayList<ChangeListener>();
        m_iterators =
                new LinkedList<WeakReference<FileReaderPreviewRowIterator>>();
        m_errorMsg = null;
        m_errorDetail = null;
        m_errorLine = -1;
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
     * Call this before releasing the last reference to this table and all its
     * iterators. It closes the underlying source. Especially if the iterators
     * don't run to the end of the table, it is required to call this method.
     * Otherwise the file handle is not released until the garbage collector
     * cleans up. A call to <code>next()</code> on any of the iterators of this
     * table after disposing of the table has undefined behavior.
     */
    public void dispose() {
        synchronized (m_iterators) {
            for (WeakReference<FileReaderPreviewRowIterator> i : m_iterators) {
                FileReaderPreviewRowIterator iter = i.get();
                if (iter != null) {
                    iter.dispose();
                }
            }
            m_iterators.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        FileReaderPreviewRowIterator i =
                new FileReaderPreviewRowIterator(m_table.iterator(), this);
        synchronized (m_iterators) {
            m_iterators.add(new WeakReference<FileReaderPreviewRowIterator>(i));
        }
        return i;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_table.getDataTableSpec();
    }

    /**
     * This sets the flag indicating that the row iterator ended the table with
     * an error.
     *
     * @param fre the exception thrown by the error.
     */
    void setError(final FileReaderException fre) {
        final String msg = fre.getMessage();
        final int lineNumber = fre.getErrorLineNumber();
        final String errDetail = fre.getDetailedMessage();

        if (msg == null) {
            throw new NullPointerException("Set a nice error message");
        }
        if (lineNumber < 0) {
            throw new IllegalArgumentException("Line numbers must be larger "
                    + "than zero.");
        }
        m_errorMsg = msg;
        m_errorLine = lineNumber;
        m_errorDetail = errDetail;

        // notify all interested
        fireErrorOccuredEvent();
    }

    /**
     * @return <code>true</code> if an error occurred in an underlying row
     *         iterator. Meaning the table contains invalid data. NOTE: if
     *         <code>false</code> is returned it is not guaranteed that all
     *         data in the table is valid. It could be that no row iterator
     *         reached the invalid data yet.
     */
    boolean getErrorOccurred() {
        return m_errorMsg != null;
    }

    /**
     * @return the error msg set by a row iterator that came across an error in
     *         the table. This is <code>null</code> if not set.
     */
    String getErrorMsg() {
        return m_errorMsg;
    }

    /**
     * @return a message containing more details about the error occurred. Could
     *         be null if no details are available.
     */
    String getErrorDetail() {
        return m_errorDetail;
    }

    /**
     * @return the line number where the error occurred - if an error occurred
     *         and an error line number was set. Otherwise -1 is returned.
     */
    int getErrorLine() {
        return m_errorLine;
    }

    /**
     * If someone wants to be notified if an error occurred he should register
     * through this method.
     *
     * @param listener the object being notified when an error occurs.
     */
    void addChangeListener(final ChangeListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * Clears the list of change listeners
     * @see #addChangeListener(ChangeListener)
     */
    void removeAllChangeListeners() {
        m_listeners.clear();
    }

    private void fireErrorOccuredEvent() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener l : m_listeners) {
            try {
                l.stateChanged(event);
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }
        }
    }
}
