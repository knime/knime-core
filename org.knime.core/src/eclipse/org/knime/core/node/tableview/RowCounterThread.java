/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Nov 11, 2005 (wiswedel): created
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.tableview;

import java.util.Iterator;

/**
 * Low priority thread, which counts rows in a table as background process.
 * This thread will inform the
 * {@link org.knime.core.node.tableview.TableContentModel}, from which this
 * process has been started, that there are more rows and, thus, the table
 * can fire events and the number of rows can be shown in the gui, for instance.
 * 
 * @see TableContentModel#countRowsInBackground()
 * @author Bernd Wiswedel, University of Konstanz
 */
final class RowCounterThread extends Thread {
    
    /** Delay time between two successive "there are new rows" events. */  
    private static final int NOTIFY_DELAY = 1000;
    
    /** The underlying table, from which we need to count rows. */
    private final TableContentModel m_contentModel;
    
    /**
     * Creates a new thread for the table as given in the argument.
     *  
     * @param contentModel The table whose rows need to be counted.
     */
    RowCounterThread(final TableContentModel contentModel) {
        super("RowCounter for Table " + contentModel.hashCode());
        setPriority(Thread.MIN_PRIORITY);
        m_contentModel = contentModel;
    }
    
    /**
     * Starts the thread and calls <code>setRowCount</code> in the content 
     * model from time to time.
     */
    @Override
    public void run() {
        long lastNotify = System.currentTimeMillis();
        int rowCount = 0;
        Iterator<?> it = m_contentModel.getDataTable().iterator();
        while (it.hasNext()) {
            if (isInterrupted()) {
                return;
            }
            // do this before reading the next row, otherwise
            // the flag in setRowCount may be wrong: We don't know if there
            // are more rows to come.
            final long now = System.currentTimeMillis();
            if (now - lastNotify >= NOTIFY_DELAY) {
                lastNotify = now;
                m_contentModel.setRowCount(rowCount, false);
            }
            it.next();
            rowCount++;
        }
        m_contentModel.setRowCount(rowCount, true);
    }
}
