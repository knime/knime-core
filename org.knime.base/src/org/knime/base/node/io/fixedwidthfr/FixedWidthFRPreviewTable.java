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
 *   17.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.io.filereader.FileReaderException;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * The data table displayed in the file reader's dialog's preview. We need an extra incarnation of a data table
 * (different from from the {@link FileTable}) because if settings are not correct yet, the table in the preview must
 * not throw any exception on unexpected or invalid data it reads (which the "normal" file table does). Thus, this table
 * returns a row iterator that will create an error row when a error occurs during file reading. It will end the table
 * after the erroneous element was read.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class FixedWidthFRPreviewTable extends FixedWidthFRTable {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(FixedWidthFRPreviewTable.class);

    private final FixedWidthFRTable m_table;

    private String m_errorMsg;

    private String m_errorDetail;

    private final CopyOnWriteArrayList<ChangeListener> m_listeners;

    /**
     * Creates a new table, its like the "normal" {@link FileTable}, just not failing on invalid data files.
     *
     * @param settings settings for the underlying <code>FileTable</code>
     * @param tableSpec table spec for the underlying <code>FileTable</code>
     * @param exec the execution context the progress is reported to
     * @see FileTable
     */
    FixedWidthFRPreviewTable(final DataTableSpec tableSpec, final FixedWidthFRSettings settings,
        final ExecutionContext exec) {
        super(tableSpec, settings, exec);
        m_table = new FixedWidthFRTable(tableSpec, settings, exec);

        m_listeners = new CopyOnWriteArrayList<ChangeListener>();
        m_errorMsg = null;
        m_errorDetail = null;
    }

    @Override
    protected CloseableRowIterator createRowIterator(final FixedWidthFRSettings nodeSettings,
        final DataTableSpec tableSpec, final ExecutionContext exec) throws IOException {
        return new FixedWidthPreviewRowIterator(nodeSettings, tableSpec, exec, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_table.getDataTableSpec();
    }

    /**
     * This sets the flag indicating that the row iterator ended the table with an error.
     *
     * @param fre the exception thrown by the error.
     */
    void setError(final FileReaderException fre) {
        final String msg = fre.getMessage();
        final int lineNumber = fre.getErrorLineNumber();
        final String errDetail = fre.getDetailedMessage();

        if (msg == null) {
            throw new IllegalArgumentException("Set a nice error message");
        }
        if (lineNumber < 0) {
            throw new IllegalArgumentException("Line numbers must be larger " + "than zero.");
        }
        m_errorMsg = msg;
        m_errorDetail = errDetail;

        // notify all interested
        fireErrorOccuredEvent();
    }

    /**
     * @return <code>true</code> if an error occurred in an underlying row iterator. Meaning the table contains invalid
     *         data. NOTE: if <code>false</code> is returned it is not guaranteed that all data in the table is valid.
     *         It could be that no row iterator reached the invalid data yet.
     */
    boolean getErrorOccurred() {
        return m_errorMsg != null;
    }

    /**
     * @return the error msg set by a row iterator that came across an error in the table. This is <code>null</code> if
     *         not set.
     */
    String getErrorMsg() {
        return m_errorMsg;
    }

    /**
     * @return a message containing more details about the error occurred. Could be null if no details are available.
     */
    String getErrorDetail() {
        return m_errorDetail;
    }

    /**
     * If someone wants to be notified if an error occurred he should register through this method.
     *
     * @param listener the object being notified when an error occurs.
     */
    void addChangeListener(final ChangeListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * Clears the list of change listeners.
     *
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
