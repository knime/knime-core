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
 *   Nov 11, 2005 (wiswedel): created
 */
package de.unikn.knime.core.node.tableview;

import java.util.Iterator;

/**
 * Low priority thread, which counts rows in a table as background process.
 * This thread will inform the <code>TableContentModel</code>, from which this
 * process has been started, that there are more rows and, thus, the table
 * can fire events and the number of rows can be shown in the gui, for instance.
 * @see TableContentModel#countRowsInBackground()
 * @author Bernd Wiswedel, University of Konstanz
 */
final class RowCounterThread extends Thread {
    
    /** Delay time between two successive "there are new rows" events. */  
    private static final int NOTIFY_DELAY = 1000;
    
    /** Cancel flag set the process needs to abort. */
    /* Note: I don't have much of a clue if the keyword volatile makes much
     * sense here. If someone knows better, please let me know. It can't 
     * be wrong, though.
     */
    private volatile boolean m_isCanceled = false;
    
    /** The underlying table, from which we need to count rows. */
    private final TableContentModel m_contentModel;
    
    /** The point in time where we last notified the table. */
    private long m_lastNotify;
    
    /** Creates new thread for the table as given in the argument. 
     * @param contentModel The table whose rows need to be counted.
     */
    RowCounterThread(final TableContentModel contentModel) {
        super("RowCounter for Table " + contentModel.hashCode());
        setPriority(Thread.MIN_PRIORITY);
        m_contentModel = contentModel;
    }
    
    /** 
     * Cancels this thread, no further notification will be sent. Counting
     * is stopped.
     */
    public void setCanceled() {
        m_isCanceled = true;
    }
    
    /** Starts the thread and calls <code>setRowCount</code> in the content 
     * model from time time. */
    @Override
    public void run() {
        m_lastNotify = System.currentTimeMillis();
        int rowCount = 0;
        Iterator it = m_contentModel.getDataTable().iterator();
        while (it.hasNext()) {
            if (m_isCanceled) {
                return;
            }
            // do this before reading the next row, otherwise
            // the flag in setRowCount may be wrong: We don't know if there
            // are more rows to come.
            final long now = System.currentTimeMillis();
            if (now - m_lastNotify >= NOTIFY_DELAY && !m_isCanceled) {
                m_lastNotify = now;
                m_contentModel.setRowCount(rowCount, false);
            }
            it.next();
            rowCount++;
        }
        m_contentModel.setRowCount(rowCount, true);
    }

}
