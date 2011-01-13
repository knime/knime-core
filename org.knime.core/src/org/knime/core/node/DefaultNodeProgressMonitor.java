/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.core.node;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeProgress;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;

/**
 * The default node progress monitor which keep a progress value between 0 and
 * 1, and a progress message. Both can be <code>null</code> if not available
 * or set wrong (progress value out of range). Furthermore, it holds a flag
 * which indicates that the task during execution was interrupted.
 * <p>
 * This progress monitor uses a static timer task looking every 250 milliseconds
 * if progress information has changed. The <code>ProgressEvent</code> is
 * fired if either the value or message has changed only.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultNodeProgressMonitor implements NodeProgressMonitor {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DefaultNodeProgressMonitor.class);

    /** The cancel requested flag. */
    private boolean m_cancelExecute;

    /** Progress of the execution between 0 and 1, or null if not available. */
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
     * timer task iterates over this list and informs the monitors about new
     * progress information.
     */
    private static final List<WeakReference<DefaultNodeProgressMonitor>> PROGMONS =
            new CopyOnWriteArrayList<WeakReference<DefaultNodeProgressMonitor>>();

    /** If progress has changed. */
    private boolean m_changed = false;

    /**
     * The timer task which informs all currently active progress monitors about
     * new progress information.
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
     * Resets this monitor. I. e. no message and no progress is set. The cancel
     * flag is cleared. The list of progress listeners is not affected.<br>
     * NOTE: No notification is send to listeners!
     * {@inheritDoc}
     */
    @Override
    public synchronized void reset() {
        if ((m_progress != null) || (m_message != null)) {
            m_changed = true;
        }
        m_cancelExecute = false;
        m_progress = null;
        m_message = null;
    }

    /**
     * @return <code>true</code> if the execution of the
     *         <code>NodeModel</code> has been canceled.
     */
    boolean isCanceled() {
        return m_cancelExecute || Thread.currentThread().isInterrupted();
    }

    /**
     * Checks if the execution was canceled. If yes, it throws an
     * <code>CanceledExecutionExeption</code>.
     *
     * @see #isCanceled
     *
     * @throws CanceledExecutionException If the execution has been canceled.
     */
    @Override
    public void checkCanceled() throws CanceledExecutionException {
        if (isCanceled()) {
            throw new CanceledExecutionException(
                    "ProgressMonitor has been canceled.");
        }
    }

    /**
     * Sets the cancel requested flag.
     */
    @Override
    public void setExecuteCanceled() {
        m_cancelExecute = true;
    }

    /**
     * Updates the progress value and message if different from the current one.
     *
     * @see #setProgress(double)
     * @param progress The (new) progress value.
     * @param message The text message shown in the progress monitor.
     */
    @Override
    public synchronized void setProgress(final double progress,
            final String message) {
        boolean progressChanged = setProgressIntern(progress);
        boolean messageChanged = setMessageIntern(message, null);
        if (progressChanged || messageChanged) {
            m_changed = true;
        }
    }

    /**
     * Sets a new progress value. If the value is not in range, it will be set
     * to <code>null</code>.
     *
     * @param progress The value between 0 and 1.
     */
    @Override
    public synchronized void setProgress(final double progress) {
        if (setProgressIntern(progress)) {
            m_changed = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setMessage(final String message) {
        setProgress(message);
    }

    /**
     * Sets a new message according to the argument.
     *
     * @param message The text message shown in the progress monitor.
     */
    @Override
    public synchronized void setProgress(final String message) {
        if (setMessageIntern(message, null)) {
            m_changed = true;
        }
    }

    private synchronized void appendMessage(final String append) {
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
        boolean changed =
                oldProgress == null || oldProgress.doubleValue() != progress;
        return changed;
    }

    /**
     * Sets message internally, returns if old value has changed.
     */
    private boolean setMessageIntern(final String message, final String append) {
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
     * @return The current progress value, or <code>null</code> if not yet
     *         set.
     */
    @Override
    public Double getProgress() {
        return m_progress;
    }

    /**
     * @return The current progress message.
     */
    @Override
    public String getMessage() {
        return m_message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProgressListener(final NodeProgressListener l) {
        if ((l != null) && !m_listeners.contains(l)) {
            m_listeners.add(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeProgressListener(final NodeProgressListener l) {
        if (l != null) {
            m_listeners.remove(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllProgressListener() {
        m_listeners.clear();
    }

    private void fireProgressChanged() {
        m_changed = false;
        NodeProgress pe =
                new NodeProgress(getProgress(), createMessage(m_message,
                        m_append));
        for (NodeProgressListener l : m_listeners) {
            try {
                // we can't provide a useful node id here
                // TODO replace by null argument (0 is certainly misleading)
                l.progressChanged(new NodeProgressEvent(new NodeID(0), pe));
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }
        }
    }

    private static String createMessage(final String message,
            final String append) {
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

    /**
     * Progress monitor that is used by "sub-progresses", it doesn't have the
     * range [0, 1] but only [0, b] where b is user-defined.
     */
    static class SubNodeProgressMonitor implements NodeProgressMonitor {

        private final NodeProgressMonitor m_parent;

        private final double m_maxProg;

        private double m_lastProg;

        private String m_message;

        private String m_append;

        /**
         * Creates new sub progress monitor.
         *
         * @param parent The parent of this monitor, i.e. where to report
         *            progress to and get the canceled status from.
         * @param max The maximum progress (w.r.t parent) that this monitor
         *            should report.
         */
        SubNodeProgressMonitor(final NodeProgressMonitor parent,
                final double max) {
            m_maxProg = max;
            m_parent = parent;
            m_message = null;
        }

        /**
         * Must not be called. Throws IllegalStateException.
         * {@inheritDoc}
         */
        @Override
        public void addProgressListener(final NodeProgressListener l) {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Delegates to parent.
         *
         * {@inheritDoc}
         */
        @Override
        public void checkCanceled() throws CanceledExecutionException {
            m_parent.checkCanceled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMessage() {
            if (m_message == null) {
                return "";
            } else {
                return m_message;
            }
        }

        /**
         * Get the subprogress, the value scaled to [0, 1].
         *
         * {@inheritDoc}
         */
        @Override
        public Double getProgress() {
            return m_lastProg;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Must not be called. Throws IllegalStateException.
         *
         * @see NodeProgressMonitor#removeAllProgressListener()
         */
        @Override
        public void removeAllProgressListener() {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Must not be called. Throws IllegalStateException.
         *
         * {@inheritDoc}
         */
        @Override
        public void removeProgressListener(final NodeProgressListener l) {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * Must not be called. Throws IllegalStateException.
         *
         * {@inheritDoc}
         */
        @Override
        public void setExecuteCanceled() {
            throw new IllegalStateException("This method must not be called.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setMessage(final String message) {
            setProgress(message);
        }

        /**
         * Delegates to parent.
         *
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final String message) {
            setProgress(message, true);
        }

        /** Internal setter method, subject to override in silent progress mon.
         * @param message new message
         * @param append whether to append
         */
        void setProgress(final String message, final boolean append) {
            synchronized (m_parent) {
                m_message = message;
                if (append) {
                    m_append = null;
                }
                String create = createMessage(m_message, m_append);
                if (m_parent instanceof DefaultNodeProgressMonitor) {
                    ((DefaultNodeProgressMonitor)m_parent)
                            .appendMessage(create);
                } else if (m_parent instanceof SubNodeProgressMonitor) {
                    ((SubNodeProgressMonitor)m_parent).appendMessage(create);
                } else {
                    m_parent.setMessage(create);
                }
            }
        }

        /** @param append Message to append */
        void appendMessage(final String append) {
            m_append = append;
            setProgress(m_message, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final double progress, final String message) {
            synchronized (m_parent) {
                this.setProgress(progress);
                this.setMessage(message);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final double progress) {
            if (m_maxProg <= 0.0) { // don't report 0-progress ("unknown")
                return;
            }
            // synchronization is imported here: multiple sub progresses may
            // report to the parent. "getOldProgress" and "setNewProgress" must
            // be an atomic operation
            synchronized (m_parent) {
                Double progressOfParent = m_parent.getProgress();
                double boundedProgress = Math.max(0.0, Math.min(progress, 1.0));
                // diff to the last progress update
                double diff = Math.max(0.0, boundedProgress - m_lastProg);
                double subProgress = Math.min(m_maxProg, diff * m_maxProg);
                if (progressOfParent != null) {
                    subProgress += progressOfParent;
                }
                // we silently swallow small progress updates here as a sequence
                // of updates (and all of which are scaled using m_maxProg) may
                // lead to a high accumulated rounding error
                if (diff < 0.001 && progressOfParent != null) {
                    // if the parent has no progress so far, we still set
                    // that small progress, for the parent to show 0%
                    return;
                }
                // the following is sort of a workaround: we know our parent's
                // old progress value, then try(!) to set an updated new value
                // and if the parent's new progress has indeed changed (this
                // object is the only one, which can set the parent's progress),
                // we can also update our internal progress. If
                // m_parent.setProgress did not result in a change of the
                // parent's progress, this object's update was too little to get
                // propagated, that means we stick with the previous propagated
                // progress
                m_parent.setProgress(subProgress);
                Double newProgressOfParent = m_parent.getProgress();
                if (newProgressOfParent != null) {
                    if (progressOfParent == null
                            || progressOfParent.doubleValue()
                                != newProgressOfParent.doubleValue()) {
                        m_lastProg = boundedProgress;
                    }
                }
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
         * @see SubNodeProgressMonitor
         *      #SubNodeProgressMonitor(NodeProgressMonitor, double)
         */
        SilentSubNodeProgressMonitor(final NodeProgressMonitor parent,
                final double max) {
            super(parent, max);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final double prog, final String message) {
            super.setProgress(prog);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setMessage(final String arg0) {
            // do nothing here
        }

        /** {@inheritDoc} */
        @Override
        public void setProgress(final String message) {
            // do nothing here
        }

        /** {@inheritDoc} */
        @Override
        void appendMessage(final String append) {
            // do nothing here
        }

        /** {@inheritDoc} */
        @Override
        void setProgress(final String message, final boolean append) {
            // do nothing here
        }
    }

}
