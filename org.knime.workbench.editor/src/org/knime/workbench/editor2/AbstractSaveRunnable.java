/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   17.11.2016 (thor): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CastUtil;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.LockFailedException;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
abstract class AbstractSaveRunnable extends PersistWorkflowRunnable {
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private WorkflowEditor m_editor;

    private StringBuilder m_exceptionMessage;

    private IProgressMonitor m_monitor;

    /**
     * Creates a runnable that saves the worfklow.
     *
     * @param editor the editor holding the workflow to save
     * @param exceptionMessage holding an exception message
     * @param monitor the progress monitor to report the progress to
     */
    public AbstractSaveRunnable(final WorkflowEditor editor, final StringBuilder exceptionMessage,
        final IProgressMonitor monitor) {
        m_editor = editor;
        m_exceptionMessage = exceptionMessage;
        m_monitor = monitor;
    }

    @Override
    public final void run(final IProgressMonitor pm) {
        File workflowDir = getSaveLocation();
        try {
            final WorkflowManager wfm = CastUtil.castWFM(m_editor.getWorkflowManager());
            ProgressHandler progressHandler =
                new ProgressHandler(pm, wfm.getNodeContainers().size(), "Saving workflow... (cannot be canceled)");
            final CheckCancelNodeProgressMonitor progressMonitor = new CheckCancelNodeProgressMonitor(pm);

            progressMonitor.addProgressListener(progressHandler);
            final ExecutionMonitor exec = new ExecutionMonitor(progressMonitor);

             save(wfm, exec);

            // the refresh used to take place in WorkflowEditor#saveTo but
            // was moved to this runnable as part of bug fix 3028
            IResource r = KnimeResourceUtil.getResourceForURI(workflowDir.toURI());
            if (r != null) {
                String pName = r.getName();
                pm.setTaskName("Refreshing " + pName + "...");
                r.refreshLocal(IResource.DEPTH_INFINITE, pm);
            }
        } catch (FileNotFoundException fnfe) {
            m_logger.fatal("File not found", fnfe);
            m_exceptionMessage.append("File access problems: " + fnfe.getMessage());
            m_monitor.setCanceled(true);
        } catch (IOException ioe) {
            if (new File(workflowDir, WorkflowPersistor.WORKFLOW_FILE).length() == 0) {
                m_logger.info("New workflow created.");
            } else {
                m_logger.error("Could not save workflow: " + workflowDir.getName(), ioe);
                m_exceptionMessage.append("File access problems: " + ioe.getMessage());
                m_monitor.setCanceled(true);
            }
        } catch (CanceledExecutionException cee) {
            m_logger.info("Canceled saving workflow: " + workflowDir.getName());
            m_exceptionMessage.append("Saving workflow" + " was canceled.");
            m_monitor.setCanceled(true);
        } catch (Exception e) {
            m_logger.error("Could not save workflow", e);
            m_exceptionMessage.append("Could not save workflow: " + e.getMessage());
            m_monitor.setCanceled(true);
        } finally {
            pm.subTask("Finished.");
            pm.done();
            m_editor = null;
            m_exceptionMessage = null;
            m_monitor = null;
        }
    }

    protected abstract File getSaveLocation();

    protected abstract void save(WorkflowManager wfm, ExecutionMonitor exec) throws IOException, CanceledExecutionException, LockFailedException;
}
