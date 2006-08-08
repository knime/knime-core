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

/**
 * This node's execution monitor handles the progress and later also memory
 * managment for each node model's execution.
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
        if (maxProg > 1.0 || maxProg < 0.0) {
            throw new IllegalArgumentException(
                    "Invalid sub progress size: " + maxProg);
        }
        SubNodeProgressMonitor subProgress = 
            new SubNodeProgressMonitor(m_progress, maxProg);
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
        if (maxProg > 1.0 || maxProg < 0.0) {
            throw new IllegalArgumentException(
                    "Invalid sub progress size: " + maxProg);
        }
        SilentSubNodeProgressMonitor subProgress = 
            new SilentSubNodeProgressMonitor(m_progress, maxProg);
        return new ExecutionMonitor(subProgress);
    }
    
    /** Progress monitor that is used by "sub-progresses", it doesn't have
     * the range [0, 1] but only [0, b] where b is user-defined. 
     */
    private static class SubNodeProgressMonitor 
        implements NodeProgressMonitor {
        
        private final NodeProgressMonitor m_parent;
        private final double m_maxProg;
        private double m_lastProg;
        private final String m_parentMessage;
        private String m_lastMessage;
        
        /**
         * Creates new sub progress monitor.
         * @param parent The parent of this monitor, i.e. where to report
         * progress to and get the canceled status from.
         * @param max The maximum progress (w.r.t parent) that this monitor
         * should report.
         */
        SubNodeProgressMonitor(
                final NodeProgressMonitor parent, final double max) {
            m_maxProg = max;
            m_parent = parent;
            m_parentMessage = parent.getMessage();
        }

        /**
         * Must not be called. Throws IllegalStateException.
         * @see NodeProgressMonitor#addProgressListener(NodeProgressListener)
         */
        public void addProgressListener(final NodeProgressListener l) {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Delegates to parent.
         * @see NodeProgressMonitor#checkCanceled()
         */
        public void checkCanceled() throws CanceledExecutionException {
            m_parent.checkCanceled();
        }

        /**
         * @see NodeProgressMonitor#getMessage()
         */
        public String getMessage() {
            return m_lastMessage;
        }

        /**
         * Get the subprogress, the value scaled to [0, 1].
         * @see NodeProgressMonitor#getProgress()
         */
        public double getProgress() {
            return m_lastProg;
        }

        /**
         * Must not be called. Throws IllegalStateException.
         * @see NodeProgressMonitor#removeAllProgressListener()
         */
        public void removeAllProgressListener() {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Must not be called. Throws IllegalStateException.
         * @see NodeProgressMonitor#removeProgressListener(NodeProgressListener)
         */
        public void removeProgressListener(final NodeProgressListener l) {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Must not be called. Throws IllegalStateException.
         * @see NodeProgressMonitor#setExecuteCanceled()
         */
        public void setExecuteCanceled() {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Delegates to parent.
         * @see NodeProgressMonitor#setMessage(String)
         */
        public void setMessage(final String message) {
            String subMessage = calcNewMessage(message);
            m_parent.setMessage(m_parentMessage + " - " + subMessage);
        }

        /**
         * @see NodeProgressMonitor#setProgress(double, String)
         */
        public void setProgress(
                final double progress, final String message) {
            synchronized (m_parent) {
                double subProgress = calcSubProgress(progress);
                String subMessage = calcNewMessage(message);
                m_parent.setProgress(subProgress, subMessage);
            }
        }

        /**
         * @see NodeProgressMonitor#setProgress(double)
         */
        public void setProgress(final double progress) {
            synchronized (m_parent) {
                double subProgress = calcSubProgress(progress);
                m_parent.setProgress(subProgress);
            }
        }
        
        
        private double calcSubProgress(final double progress) {
            double progressOfParent = m_parent.getProgress();
            // diff to the last progress update
            double diff = progress - m_lastProg;
            m_lastProg = progress;
            // scaled to our sub range
            return progressOfParent + diff * m_maxProg;
        }
        
        private String calcNewMessage(final String message) {
            m_lastMessage = m_parentMessage + " - " + message;
            return m_lastMessage;
        }
    }
    
    private static final class SilentSubNodeProgressMonitor extends
            SubNodeProgressMonitor {

        /**
         * @see SubNodeProgressMonitor#SubNodeProgressMonitor(
         *      NodeProgressMonitor,double)
         * @param parent
         * @param max
         */
        private SilentSubNodeProgressMonitor(final NodeProgressMonitor parent,
                final double max) {
            super(parent, max);
        }
        
        /**
         * @see NodeProgressMonitor#setProgress(double, java.lang.String)
         */
        @Override
        public void setProgress(final double prog, final String message) {
            super.setProgress(prog);
        }
        
        /**
         * @see NodeProgressMonitor#setMessage(java.lang.String)
         */
        @Override
        public void setMessage(final String arg0) {
            // do nothing here
        }
    }

} // ExecutionMonitor
