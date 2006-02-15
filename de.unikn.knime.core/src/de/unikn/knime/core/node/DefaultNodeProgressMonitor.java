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
 */
package de.unikn.knime.core.node;

import java.util.HashSet;
import java.util.Set;

/**
 * The default node progress monitor which keep a progress flag between 0 and 1,
 * or -1 if no progress is available or set wrong. Furthermore, it holds a flag
 * which indicates that the task during execution was interrupted.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultNodeProgressMonitor implements NodeProgressMonitor {

    /** The cancel requested flag. */
    private boolean m_cancelExecute;

    /** The progress of the excution between 0 and 1, or -1 if not available. */
    private double m_progress;

    /** The progress message. */
    private String m_message;

    /** A set of progress listeners. */
    private final Set<NodeProgressListener> m_set;

    /**
     * Creates a new progress monitor with an empty set of listeners.
     * 
     * @see #DefaultNodeProgressMonitor(NodeProgressListener)
     */
    public DefaultNodeProgressMonitor() {
        m_set = new HashSet<NodeProgressListener>();
        m_cancelExecute = false;
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
            fireProgressChanged();
        }
    }

    /**
     * Sets a new progress value. If the value is not in range, it will be set
     * to -1.
     * 
     * @param progress The value between 0 and 1, or -1 if not available.
     */
    public synchronized void setProgress(final double progress) {
        if (setProgressIntern(progress)) {
            fireProgressChanged();
        }
    }
    
    /**
     * Sets a new message according to the argument.
     * @param message The text message shown in the progress monitor.
     */
    public synchronized void setMessage(final String message) {
        if (setMessageIntern(message)) {
            fireProgressChanged();
        }
    }

    /** Sets progress internally, returns if old value has changed. */
    private boolean setProgressIntern(final double progress) {
        final double oldProgress = m_progress;
        if (progress >= 0.0 && progress <= 1.0) {
            m_progress = progress;
        } else {
            m_progress = -1;
        }
        boolean changed = oldProgress != progress;
        m_progress = progress;
        return changed;
    }

    /** Sets message internally, returns if old value has changed. */
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
     * @return The current progress value.
     */
    public double getProgress() {
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
        if (l != null) {
            m_set.add(l);
        }
    }

    /**
     * @see NodeProgressMonitor#removeProgressListener(NodeProgressListener)
     */
    public void removeProgressListener(final NodeProgressListener l) {
        if (l != null) {
            m_set.remove(l);
        }
    }

    /**
     * @see NodeProgressMonitor#removeAllProgressListener()
     */
    public void removeAllProgressListener() {
        m_set.clear();
    }

    private synchronized void fireProgressChanged() {
        for (NodeProgressListener l : m_set) {
            l.progressChanged(m_progress, m_message);
        }
    }

} // DefaultNodeProgressMonitor
