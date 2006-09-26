/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 */
package org.knime.core.node;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The default node progress monitor which keep a progress value between 0 and 
 * 1, and a progress message. Both can be <code>null</code> if not available or 
 * set wrong (progress value out of range). Furthermore, it holds a flag which 
 * indicates that the task during execution was interrupted.
 * <p>
 * This progress monitor uses a static timer task looking every 250 milliseconds
 * if progress information has changed. The <code>ProgressEvent</code> is fired
 * if either the value or message has changed only.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultNodeProgressMonitor implements NodeProgressMonitor {

    /** The cancel requested flag. */
    private boolean m_cancelExecute;

    /** The progress of the excution between 0 and 1, or null if not available. 
     */
    private Double m_progress;

    /** The progress message. */
    private String m_message;

    /** A set of progress listeners. */
    private final CopyOnWriteArrayList<NodeProgressListener> m_listeners;
    
    /** Timer period looking for changed progress information. */
    private static final int TIMER_PERIOD = 250;
    
    /**
     * Keeps a static list of these progress monitors if they are active. The
     * timer task iterates over this list and informs the monitors about
     * new progress information. 
     */
    private static final ArrayList<WeakReference<DefaultNodeProgressMonitor>> 
       PROGMONS = new ArrayList<WeakReference<DefaultNodeProgressMonitor>>();
    
    /** If progress has changed. */
    private boolean m_changed = false;
    
    /**
     * The timer task which informs all currently active progress monitors
     * about new progress information.
     */
    private static final TimerTask TASK = new TimerTask() {
        @Override
        public void run() {
            synchronized (PROGMONS) {
                for (Iterator<WeakReference<DefaultNodeProgressMonitor>> it = 
                        PROGMONS.iterator(); it.hasNext();) {
                    DefaultNodeProgressMonitor p = it.next().get();
                    if (p == null) {
                        it.remove(); // not active anymore
                    } else if (p.m_changed) {
                        p.fireProgressChanged(); // something has changed
                    }
                }
            }
        }  
    };
    
    /** Timer used to schedule the task. */
    private static final Timer TIMER = new Timer("KNIME Progress Timer", true);
        
    static {
        // start timer once with the given task, starting time, and time period
        TIMER.scheduleAtFixedRate(TASK, 0, TIMER_PERIOD);
    }
    
    /**
     * Creates a new progress monitor with an empty set of listeners.
     * 
     * @see #DefaultNodeProgressMonitor(NodeProgressListener)
     */
    public DefaultNodeProgressMonitor() {
        m_listeners = new CopyOnWriteArrayList<NodeProgressListener>();
        m_cancelExecute = false;
        m_progress = null;
        m_message = null;
        synchronized (PROGMONS) {
            // add this progress monitor to the list of active ones
            PROGMONS.add(new WeakReference<DefaultNodeProgressMonitor>(this));
        }
    }

    /**
     * Creates a new node progress monitor, with the cancel requested false, and
     * no progress.
     * 
     * @param l Initial node progress listener.
     */
    public DefaultNodeProgressMonitor(final NodeProgressListener l) {
        this();
        addProgressListener(l);

    }

    /**
     * @return <code>true</code> if the execution of the
     *         <code>NodeModel</code> has been cancled.
     */
    synchronized boolean isCanceled() {
        return m_cancelExecute;
    }

    /**
     * Checks if the execution was canceled. If yes, it throws an
     * <code>CanceledExecutionExeption</code>.
     * 
     * @see #isCanceled
     * 
     * @throws CanceledExecutionException If the execution has been canceled.
     */
    public synchronized void checkCanceled() throws CanceledExecutionException {
        if (isCanceled()) {
            throw new CanceledExecutionException(
                    "ProgressMonitor has been canceled.");
        }
    }

    /**
     * Sets the cancel requested flag.
     */
    public synchronized void setExecuteCanceled() {
        m_cancelExecute = true;
    }
    
    /**
     * Updates the progress value and message if different from the current one.
     * @see #setProgress(double)
     * @param progress The (new) progress value.
     * @param message The text message shown in the progress monitor.
     */
    public synchronized void setProgress(final double progress,
            final String message) {
        if (setProgressIntern(progress) | setMessageIntern(message)) {
            m_changed = true;
        }
    }

    /**
     * Sets a new progress value. If the value is not in range, it will be set
     * to <code>null</code>.
     * 
     * @param progress The value between 0 and 1.
     */
    public synchronized void setProgress(final double progress) {
        if (setProgressIntern(progress)) {
            m_changed = true;
        }
    }

    /**
     * @see #setProgress(String)
     */
    public synchronized void setMessage(final String message) {
        setProgress(message);
    }

    /**
     * Sets a new message according to the argument.
     * @param message The text message shown in the progress monitor.
     */
    public void setProgress(final String message) {
        if (setMessageIntern(message)) {
            m_changed = true;
        }
    }

    /**
     * Sets progress internally, returns <code>true</code> if old value has 
     * changed.
     */
    private boolean setProgressIntern(final double progress) {
        final Double oldProgress = m_progress;
        if (progress >= 0.0 && progress <= 1.0) {
            m_progress = progress;
        }
        boolean changed = oldProgress == null 
            || oldProgress.doubleValue() != progress;
        return changed;
    }

    /**
     * Sets message internally, returns if old value has changed.
     */
    private boolean setMessageIntern(final String message) {
        if (message == m_message) {
            return false;
        }
        boolean changed = (message != null 
                ? !message.equals(m_message) : !m_message.equals(message));
        m_message = message;
        return changed;
    }
    
    /**
     * @return The current progress value, or <code>null</code> if not yet set.
     */
    public Double getProgress() {
        return m_progress;
    }

    /**
     * @return The current progress message.
     */
    public String getMessage() {
        return m_message;
    }
    
    /**
     * @see NodeProgressMonitor#addProgressListener(NodeProgressListener)
     */
    public void addProgressListener(final NodeProgressListener l) {
        if ((l != null) && !m_listeners.contains(l)) {
            m_listeners.add(l);
        }
    }

    /**
     * @see NodeProgressMonitor#removeProgressListener(NodeProgressListener)
     */
    public void removeProgressListener(final NodeProgressListener l) {
        if (l != null) {
            m_listeners.remove(l);
        }
    }

    /**
     * @see NodeProgressMonitor#removeAllProgressListener()
     */
    public void removeAllProgressListener() {
        m_listeners.clear();
    }

    private void fireProgressChanged() {
        m_changed = false;
        NodeProgressEvent pe = new NodeProgressEvent(m_progress, m_message);
        for (NodeProgressListener l : m_listeners) {
            l.progressChanged(pe);
        }
    }
    
}
