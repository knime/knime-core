/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   11.01.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.workflow.NodeProgress;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;

/**
 * A runnable which is the abstract super class used by the
 * {@link WorkflowEditor} to Load/save a workflow with a progress bar. NOTE: As
 * the {@link UIManager} holds a reference to this runnable an own class file is
 * necessary sucht that all references to the created workflow manager can be
 * deleted, otherwise the manager can not be deleted later and the memeory can
 * not be freed.
 *
 * @author Christoph Sieb, University of Konstanz
 */
abstract class PersistWorflowRunnable implements IRunnableWithProgress {

    class CheckThread extends Thread {

        private boolean m_finished = false;

        private final IProgressMonitor m_pm;

        private final DefaultNodeProgressMonitor m_progressMonitor;

        private final boolean m_cancelable;

        /**
         * Creates a new cancel execution checker.
         *
         * @param pm
         *            the eclipse progress monitor
         * @param progressMonitor
         *            the knime progress monitor
         * @param cancelable
         *            if true the progress is cancelable by the user if false a
         *            dialog informs the user that the progress is not
         *            cancelable
         */
        public CheckThread(final IProgressMonitor pm,
                final DefaultNodeProgressMonitor progressMonitor,
                final boolean cancelable) {
            super("CheckThread");
            m_pm = pm;
            m_progressMonitor = progressMonitor;
            m_cancelable = cancelable;
        }

        /**
         * Sets the finished flag.
         *
         */
        public void finished() {
            m_finished = true;

        }

        @Override
        public void run() {

            while (!m_finished) {

                if (m_pm.isCanceled()) {

                    if (m_cancelable) {

                        m_progressMonitor.setExecuteCanceled();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // nothing to do here
                }
            }
        }
    }

    class ProgressHandler implements NodeProgressListener {

        private final IProgressMonitor m_progressMonitor;

        private final int m_totalWork;

        private int m_workedSoFar;

        /**
         * Handles progress changes during saving the workflow.
         *
         * @param monitor
         *            the eclipse progressmonitor
         * @param totalWork
         *            the total amount of work to do
         * @param task
         *            the main task name to display
         */
        public ProgressHandler(final IProgressMonitor monitor,
                final int totalWork, final String task) {
            monitor.beginTask(task, totalWork);
            m_progressMonitor = monitor;
            m_totalWork = totalWork;
            m_workedSoFar = 0;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void progressChanged(final NodeProgressEvent evt) {
            final NodeProgress pe = evt.getNodeProgress();
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    if (pe.hasProgress() && pe.getProgress() >= 0) {
                        double progress = pe.getProgress();
                        int worked = (int) (progress * m_totalWork);
                        m_progressMonitor.worked(worked - m_workedSoFar);
                        // remember the work done so far
                        m_workedSoFar = worked;
                    }

                    if (pe.hasMessage()) {
                        m_progressMonitor.subTask(pe.getMessage());
                    } else {
                        m_progressMonitor.subTask("");
                    }
                }
            });
        }
    }
}
