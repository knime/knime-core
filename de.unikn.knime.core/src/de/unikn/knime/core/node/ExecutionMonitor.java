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

/**
 * This node's execution monitor handles the progress and later also memory
 * managment for each node model's execution.
 * <p>
 * This monitor keeps a <code>NodeProgressMonitor</code> and forwards the
 * progress, as well as the cancel request to it.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public final class ExecutionMonitor {

    /** The progress monitor cancel and progress are delegated. */
    private final NodeProgressMonitor m_progress;

    /**
     * Creates a new execution monitor with an empty default progress monitor.
     */
    public ExecutionMonitor() {
        m_progress = new DefaultNodeProgressMonitor(null);
    }

    /**
     * Creates a new execution monitor with the given progress monitor which can
     * be <code>null</code>.
     * 
     * @param progress The progress monitor can be null.
     */
    public ExecutionMonitor(final NodeProgressMonitor progress) {
        if (progress == null) {
            m_progress = new DefaultNodeProgressMonitor(null);
        } else {
            m_progress = progress;
        }
    }

    /**
     * @return The progress monitor used here.
     */
    final NodeProgressMonitor getProgressMonitor() {
        return m_progress;
    }

    /**
     * @see NodeProgressMonitor#checkCanceled()
     * @return <code>true</code> if the execution has been canceled.
     */
    boolean isCanceled() {
        try {
            m_progress.checkCanceled();
            return false;
        } catch (CanceledExecutionException cee) {
            return true;
        }
    }

    /**
     * @see NodeProgressMonitor#checkCanceled()
     * @throws CanceledExecutionException which indicated the execution will be
     *             canceled by this call.
     */
    public void checkCanceled() throws CanceledExecutionException {
        m_progress.checkCanceled();
    }

    /**
     * @see NodeProgressMonitor#setProgress(double)
     * @param progress The progress values to set in the monitor.
     */
    public void setProgress(final double progress) {
        m_progress.setProgress(progress);
    }

    /**
     * @see NodeProgressMonitor#setProgress(double)
     * @param progress The progress values to set in the monitor.
     * @param message The message to be shown in the progress monitor.
     */
    public void setProgress(final double progress, final String message) {
        m_progress.setProgress(progress, message);
    }

    /**
     * @see NodeProgressMonitor#setMessage(String)
     * @param message The message to be shown in the progress monitor.
     */
    public void setMessage(final String message) {
        m_progress.setMessage(message);
    }

    /**
     * Returns the current progress value between 0 and 1.
     * 
     * @return Progress value.
     */
    public double getProgress() {
        return m_progress.getProgress();
    }

    /**
     * @return The progress message currently displayed.
     */
    public String getMessage() {
        return m_progress.getMessage();
    }

} // ExecutionMonitor
