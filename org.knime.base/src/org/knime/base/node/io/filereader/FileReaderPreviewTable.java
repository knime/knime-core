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
