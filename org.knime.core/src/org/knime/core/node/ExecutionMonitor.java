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
 */
package org.knime.core.node;

import org.knime.core.node.DefaultNodeProgressMonitor.SilentSubNodeProgressMonitor;
import org.knime.core.node.DefaultNodeProgressMonitor.SubNodeProgressMonitor;

/**
 * This node's execution monitor handles the progress and later also memory
 * management for each node model's execution.
 * <p>
 * This monitor keeps a <code>NodeProgressMonitor</code> and forwards the
 * progress, as well as the cancel request to it.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ExecutionMonitor {

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
    public final NodeProgressMonitor getProgressMonitor() {
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
     * @see NodeProgressMonitor#setProgress(String)
     * @param message The message to be shown in the progress monitor.
     */
    public void setProgress(final String message) {
        m_progress.setProgress(message);
    }
    
    /** Creates an execution monitor with a partial progress range.
     * Classes that use a progress monitor and report in the range of [0,1]
     * should get such a sub-progress monitor when their job is only partially
     * contributing to the entire progress. The progress of such sub-jobs is
     * then automatically scaled to the "right" range. 
     * @param maxProg The fraction of the progress this sub progress
     * contributes to the whole progress 
     * @return A new execution monitor ready to use in sub jobs.
     * @throws IllegalArgumentException If the argument is not in (0, 1].
     */ 
    public ExecutionMonitor createSubProgress(final double maxProg) {
        NodeProgressMonitor subProgress = createSubProgressMonitor(maxProg);
        return new ExecutionMonitor(subProgress);
    }
    
    /** Creates an execution monitor with a partial progress range,  which
     * ignores any message set.
     * Classes that use a progress monitor and report in the range of [0,1]
     * should get such a sub-progress monitor when their job is only partially
     * contributing to the entire progress. The progress of such sub-jobs is
     * then automatically scaled to the "right" range. This method
     * differs from the {@link #createSubProgress(double)} message in that it
     * does not report any message but rather ignores any new string message.  
     * @param maxProg The fraction of the progress this sub progress
     * contributes to the whole progress 
     * @return A new execution monitor ready to use in sub jobs.
     * @throws IllegalArgumentException If the argument is not in (0, 1].
     */ 
    public ExecutionMonitor createSilentSubProgress(final double maxProg) {
        NodeProgressMonitor subProgress = 
            createSilentSubProgressMonitor(maxProg);
        return new ExecutionMonitor(subProgress);
    }
    
    /** 
     * Factory method to create a new sub progress monitor. Only for
     * internal use (i.e. here and in ExecutionContext).
     * @param maxProg The fraction of the progress this sub progress
     * contributes to the whole progress 
     * @return A new sub node progress monitor.
     * @throws IllegalArgumentException If the argument is not in (0, 1].
     */ 
    NodeProgressMonitor createSubProgressMonitor(final double maxProg) {
        if (maxProg > 1.0 || maxProg < 0.0) {
            throw new IllegalArgumentException(
                    "Invalid sub progress size: " + maxProg);
        }
        return new SubNodeProgressMonitor(m_progress, maxProg);
    }
    
    /**
     * Factory method to create a new silent sub progress monitor. Only for
     * internal use (i.e. here an in ExecutionContext).
     * @param maxProg The fraction of the progress this sub progress
     * contributes to the whole progress 
     * @return A new silent sub node progress monitor.
     */
    NodeProgressMonitor createSilentSubProgressMonitor(final double maxProg) {
        if (maxProg > 1.0 || maxProg < 0.0) {
            throw new IllegalArgumentException(
                    "Invalid sub progress size: " + maxProg);
        }
        return new SilentSubNodeProgressMonitor(m_progress, maxProg);
    }
    
} // ExecutionMonitor
