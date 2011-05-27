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
 *
 */
package org.knime.workbench.editor2;

import java.util.StringTokenizer;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeProgress;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry;
import org.knime.workbench.KNIMEEditorPlugin;

/**
 * A runnable which is the abstract super class used by the
 * {@link WorkflowEditor} to Load/save a workflow with a progress bar. NOTE: As
 * the {@link UIManager} holds a reference to this runnable an own class file is
 * necessary such that all references to the created workflow manager can be
 * deleted, otherwise the manager can not be deleted later and the memory can
 * not be freed.
 *
 * @author Christoph Sieb, University of Konstanz
 */
abstract class PersistWorkflowRunnable implements IRunnableWithProgress {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    /** A KNIME progress monitor that's using an eclipse progress monitor to
     * check for cancelation. */
    static class CheckCancelNodeProgressMonitor
        extends DefaultNodeProgressMonitor {

        private final IProgressMonitor m_pm;


        /** @param pm The prog to monitor for cancelation. */
        public CheckCancelNodeProgressMonitor(final IProgressMonitor pm) {
            m_pm = pm;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isCanceled() {
            return super.isCanceled() || m_pm.isCanceled();
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void reset() {
            throw new IllegalStateException("Reset not supported");
        }
    }

    static class ProgressHandler implements NodeProgressListener {

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
        @Override
        public synchronized void progressChanged(final NodeProgressEvent evt) {
            final NodeProgress pe = evt.getNodeProgress();
            Display.getDefault().asyncExec(new Runnable() {
                @Override
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

    /** @return instance logger. */
    protected NodeLogger getLogger() {
        return m_logger;
    }

    /** Logs the argument error to logger, preserving line breaks.
     * This method will hopefully go into the NodeLogger facilities (and hence
     * be public API).
     * @param isError Whether to report to LOGGER.error (otherwise warn only).
     * @param error The error string to log.
     */
    protected final void logPreseveLineBreaks(
            final String error, final boolean isError) {
        StringTokenizer t = new StringTokenizer(error, "\n");
        NodeLogger logger = getLogger();
        while (t.hasMoreTokens()) {
            if (isError) {
                logger.error(t.nextToken());
            } else {
                logger.warn(t.nextToken());
            }
        }
    }

    /** Create IStatus from load result.
     *
     * @param loadResult Load result.
     * @param treatDataLoadErrorsAsOK data loading is OK (exported with no data)
     * @return The IStatus object to be shown. */
    protected IStatus createStatus(final LoadResultEntry loadResult,
            final boolean treatDataLoadErrorsAsOK) {
        LoadResultEntry[] children = loadResult.getChildren();
        if (children.length == 0) {
            int severity;
            switch (loadResult.getType()) {
            case DataLoadError:
                severity = treatDataLoadErrorsAsOK
                ? IStatus.OK : IStatus.ERROR;
                break;
            case Error:
                severity = IStatus.ERROR;
                break;
            case Warning:
                severity = IStatus.WARNING;
                break;
            default:
                severity = IStatus.OK;
            }
            return new Status(severity, KNIMEEditorPlugin.PLUGIN_ID,
                    loadResult.getMessage(), null);
        }
        IStatus[] subStatus = new IStatus[children.length];
        for (int i = 0; i < children.length; i++) {
            subStatus[i] = createStatus(children[i], treatDataLoadErrorsAsOK);
        }
        return createMultiStatus(loadResult.getMessage(), subStatus);
    }

    protected IStatus createMultiStatus(
            final String message, final IStatus[] stats) {
        return new MultiStatus(KNIMEEditorPlugin.PLUGIN_ID, Status.OK,
                stats, message, null);
    }

}
