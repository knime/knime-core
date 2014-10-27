/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   11.01.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.UIManager;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowSaveHelper;
import org.knime.core.util.FileUtil;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * A runnable which is used by the {@link WorkflowEditor} to save a workflow with a progress bar. NOTE: As the
 * {@link UIManager} holds a reference to this runnable an own class file is necessary sucht that all references to the
 * created workflow manager can be deleted, otherwise the manager can not be deleted later and the memeory can not be
 * freed.
 *
 * @author Christoph Sieb, University of Konstanz
 */
class SaveWorkflowRunnable extends PersistWorkflowRunnable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SaveWorkflowRunnable.class);

    private WorkflowEditor m_editor;

    private File m_workflowFile;

    private StringBuilder m_exceptionMessage;

    private IProgressMonitor m_monitor;

    private final WorkflowSaveHelper m_saveHelper;

    /**
     * Creates a runnable that saves the worfklow.
     *
     * @param editor the editor holding the workflow to save
     * @param workflowFile the file to save the workflow to
     * @param exceptionMessage holding an exception message
     * @param saveHelper Save options.
     * @param monitor the progress monitor to report the progress to
     */
    public SaveWorkflowRunnable(final WorkflowEditor editor, final File workflowFile,
        final StringBuilder exceptionMessage, final WorkflowSaveHelper saveHelper, final IProgressMonitor monitor) {
        m_editor = editor;
        m_workflowFile = workflowFile;
        m_exceptionMessage = exceptionMessage;
        m_saveHelper = saveHelper;
        m_monitor = monitor;
    }

    /** {@inheritDoc} */
    @Override
    public void run(final IProgressMonitor pm) {
        try {
            final WorkflowManager wfm = m_editor.getWorkflowManager();
            ProgressHandler progressHandler =
                new ProgressHandler(pm, wfm.getNodeContainers().size(), "Saving workflow... (can not be canceled)");
            final CheckCancelNodeProgressMonitor progressMonitor = new CheckCancelNodeProgressMonitor(pm);

            progressMonitor.addProgressListener(progressHandler);

            final File workflowPath = m_workflowFile.getParentFile();
            final ReferencedFile oldWorkflowPathRef = wfm.getWorkingDir();
            final File oldWorkflowPath = oldWorkflowPathRef == null ? null : oldWorkflowPathRef.getFile();
            final ExecutionMonitor exec = new ExecutionMonitor(progressMonitor);
            if (!m_saveHelper.isAutoSave() && oldWorkflowPath != null && !oldWorkflowPath.equals(workflowPath)) {
                wfm.saveAs(workflowPath, exec);
            } else {
                wfm.save(workflowPath, m_saveHelper, exec);
            }
            // copy report files, see bug 5717
            if (m_saveHelper.isAutoSave() && oldWorkflowPath != null) {
                File reportDesignFile = new File(oldWorkflowPath, "default_report.rptdesign");
                File reportConfigFile = new File(oldWorkflowPath, "default_report.rptconfig");
                if (reportDesignFile.isFile()) {
                    FileUtil.copy(reportDesignFile, new File(workflowPath, reportDesignFile.getName()));
                }
                if (reportConfigFile.isFile()) {
                    FileUtil.copy(reportConfigFile, new File(workflowPath, reportConfigFile.getName()));
                }
            }

            // the refresh used to take place in WorkflowEditor#saveTo but
            // was moved to this runnable as part of bug fix 3028
            IResource r = KnimeResourceUtil.getResourceForURI(workflowPath.toURI());
            if (r != null) {
                String pName = r.getName();
                pm.setTaskName("Refreshing " + pName + "...");
                r.refreshLocal(IResource.DEPTH_INFINITE, pm);
            }
        } catch (FileNotFoundException fnfe) {
            LOGGER.fatal("File not found", fnfe);
            m_exceptionMessage.append("File access problems: " + fnfe.getMessage());

            m_monitor.setCanceled(true);
        } catch (IOException ioe) {
            if (m_workflowFile.length() == 0) {
                LOGGER.info("New workflow created.");
            } else {
                LOGGER.error("Could not save workflow: " + m_workflowFile.getName(), ioe);
                m_exceptionMessage.append("File access problems: " + ioe.getMessage());
                m_monitor.setCanceled(true);
            }
        } catch (CanceledExecutionException cee) {
            LOGGER.info("Canceled saving worflow: " + m_workflowFile.getName());
            m_exceptionMessage.append("Saving workflow" + " was canceled.");
            m_monitor.setCanceled(true);
        } catch (Error e) {
            LOGGER.error("Could not save workflow", e);

            m_exceptionMessage.append("Could not save workflow: " + e.getMessage());
            m_monitor.setCanceled(true);

        } catch (Exception e) {
            LOGGER.error("Could not save workflow", e);

            m_exceptionMessage.append("Could not save workflow: " + e.getMessage());
            m_monitor.setCanceled(true);
        } finally {
            pm.subTask("Finished.");
            pm.done();
            m_editor = null;
            m_workflowFile = null;
            m_exceptionMessage = null;
            m_monitor = null;
        }

    }
}
