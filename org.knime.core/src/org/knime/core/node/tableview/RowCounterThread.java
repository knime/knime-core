/* 
 * -------------------------------------------------------------------
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
