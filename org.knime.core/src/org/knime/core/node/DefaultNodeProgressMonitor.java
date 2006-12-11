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
import java.util.List;
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
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(DefaultNodeProgressMonitor.class);
    
    /** The cancel requested flag. */
    private boolean m_cancelExecute;

    /** Progress of the execution between 0 and 1, or null if not available. 
     */
    private Double m_progress;

    /** The progress message. */
    private String m_message;
    
    /** The message to append; used by SubNodeProgressMonitor. */
    private String m_append;
    
    /** A set of progress listeners. */
    private final CopyOnWriteArrayList<NodeProgressListener> m_listeners;
    
    /** Timer period looking for changed progress information. */
    private static final int TIMER_PERIOD = 250;
    
    /**
     * Keeps a static list of these progress monitors if they are active. The
     * timer task iterates over this list and informs the monitors about
     * new progress information. 
     */
    private static final 
        List<WeakReference<DefaultNodeProgressMonitor>> PROGMONS = new 
        CopyOnWriteArrayList<WeakReference<DefaultNodeProgressMonitor>>();
    
    /** If progress has changed. */
    private boolean m_changed = false;
    
    /**
     * The timer task which informs all currently active progress monitors
     * about new progress information.
     */
    private static final TimerTask TASK = new TimerTask() {
        @Override
        public void run() {
            // for maintenance only
            List<WeakReference<DefaultNodeProgressMonitor>> deadList = 
                new ArrayList<WeakReference<DefaultNodeProgressMonitor>>();
            for (Iterator<WeakReference<DefaultNodeProgressMonitor>> it = 
                    PROGMONS.iterator(); it.hasNext();) {
                WeakReference<DefaultNodeProgressMonitor> next = it.next();
                DefaultNodeProgressMonitor p = next.get();

                if (p == null) {
                    deadList.add(next);
                } else if (p.m_changed) {
                    try {
                        p.fireProgressChanged(); // something has changed
                    } catch (Exception e) {
                        LOGGER.warn("Exception (\"" 
                                + e.getClass().getSimpleName() + "\") " 
                                + " during event notification.", e);
                    }
                }
            }
            if (!deadList.isEmpty()) {
                PROGMONS.removeAll(deadList);
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
        // add this progress monitor to the list of active ones
        PROGMONS.add(new WeakReference<DefaultNodeProgressMonitor>(this));
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
     *         <code>NodeModel</code> has been canceled.
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
        if (setProgressIntern(progress) | setMessageIntern(message, null)) {
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
        if (setMessageIntern(message, null)) {
            m_changed = true;
        }
    }
    
    private void appendMessage(final String append) {
        if (setMessageIntern(m_message, append)) {
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
    private boolean setMessageIntern(final String message, 
            final String append) {
        if (message == m_message && append == m_append) {
            return false;
        }
        boolean changed = false;
        if (message == null || m_message == null) {
            changed = true;
        } else {
            changed = !message.equals(m_message);
        }
        m_message = message;
        if (append == null || m_append == null) {
            changed = true;
        } else {
            changed |= !append.equals(m_append);
        }
        m_append = append;
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
        NodeProgressEvent pe = new NodeProgressEvent(getProgress(), 
                createMessage(m_message, m_append));
        for (NodeProgressListener l : m_listeners) {
            try {
                l.progressChanged(pe);
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }                
        }
    }
    
    private static String createMessage(
            final String message, final String append) {
        String result = "";
        if (message != null) {
            result = message;
            if (append != null) {
                result += " - " + append;
            }
        } else if (append != null) {
            result = append;
        }
        return result;
    }
    
    /** Progress monitor that is used by "sub-progresses", it doesn't have
     * the range [0, 1] but only [0, b] where b is user-defined. 
     */
    static class SubNodeProgressMonitor implements NodeProgressMonitor {
        
        private final NodeProgressMonitor m_parent;
        private final double m_maxProg;
        private double m_lastProg;
        private String m_message;
        private String m_append;
        
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
            m_message = null;
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
            if (m_message == null) {
                return "";
            } else {
                return m_message;
            }
        }

        /**
         * Get the subprogress, the value scaled to [0, 1].
         * @see NodeProgressMonitor#getProgress()
         */
        public Double getProgress() {
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
         * @see #setProgress(String)
         */
        public void setMessage(final String message) {
            setProgress(message);
        }
        
        /**
         * Delegates to parent.
         * @see NodeProgressMonitor#setProgress(String)
         */
        public void setProgress(final String message) {
            setProgress(message, true);
        }
        
        private void setProgress(final String message, final boolean append) {
            synchronized (m_parent) {
                m_message = message;
                if (append) {
                    m_append = null;
                }
                String create = createMessage(m_message, m_append);
                if (m_parent instanceof DefaultNodeProgressMonitor) {
                    ((DefaultNodeProgressMonitor) m_parent).appendMessage(
                            create);
                } else if (m_parent instanceof SubNodeProgressMonitor) {
                    ((SubNodeProgressMonitor) m_parent).appendMessage(
                            create);
                } else {
                    m_parent.setMessage(create);
                }            
            }
        }
        
        private void appendMessage(final String append) {
            m_append = append;
            setProgress(m_message, false);
        }

        /**
         * @see NodeProgressMonitor#setProgress(double, String)
         */
        public void setProgress(
                final double progress, final String message) {
            synchronized (m_parent) {
                this.setProgress(progress);
                this.setMessage(message);
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
            Double progressOfParent = m_parent.getProgress();
            // diff to the last progress update
            double diff = progress - m_lastProg;
            m_lastProg = progress;
            if (progressOfParent == null) {
                return Math.min(m_maxProg, diff * m_maxProg);
            } else {
                // scaled to our sub range
                return progressOfParent + Math.min(m_maxProg, diff * m_maxProg);
            }
        }
    }
    
    /**
     * Silent progress monitor which does only forward changed of the progress
     * value rather than progress message.
     */
    static final class SilentSubNodeProgressMonitor extends
            SubNodeProgressMonitor {

        /**
         * @see SubNodeProgressMonitor #SubNodeProgressMonitor(
         *      NodeProgressMonitor, double)
         * @param parent
         * @param max
         */
        SilentSubNodeProgressMonitor(final NodeProgressMonitor parent,
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
    
}
