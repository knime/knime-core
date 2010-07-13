/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * History
 *   11.01.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.workbench.KNIMEEditorPlugin;


/**
 * A runnable which is used by the {@link WorkflowEditor} to load a workflow
 * with a progress bar. NOTE: As the {@link UIManager} holds a reference to this
 * runnable an own class file is necessary such that all references to the
 * created workflow manager can be deleted, otherwise the manager can not be
 * deleted later and the memory can not be freed.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
class LoadWorkflowRunnable extends PersistWorkflowRunnable {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LoadWorkflowRunnable.class);

    private WorkflowEditor m_editor;

    private File m_workflowFile;
    
    private Throwable m_throwable = null;

    /**
     * 
     * @param editor the {@link WorkflowEditor} for which the workflow should
     * be loaded
     * @param workflowFile the workflow file from which the workflow should be 
     * loaded (or created = empty workflow file)
     */
    public LoadWorkflowRunnable(final WorkflowEditor editor, 
            final File workflowFile) {
        m_editor = editor;
        m_workflowFile = workflowFile;
    }
    
    /**
     * 
     * @return the throwable which was thrown during the loading of the workflow
     * or null, if no throwable was thrown
     */
    Throwable getThrowable() {
        return m_throwable;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    public void run(final IProgressMonitor pm) {
        CheckThread checkThread = null;
        // indicates whether to create an empty workflow
        // this is done if the file is empty
        boolean createEmptyWorkflow = false;
        
        // name of workflow will be null (uses directory name then)
        String name = null;

        // set the loading canceled variable to false
        m_editor.setLoadingCanceled(false);
        m_throwable = null;

        try {
            // create progress monitor
            ProgressHandler progressHandler = new ProgressHandler(pm, 101,
                    "Loading workflow...");
            final DefaultNodeProgressMonitor progressMonitor 
                = new DefaultNodeProgressMonitor();
            progressMonitor.addProgressListener(progressHandler);

            checkThread = new CheckThread(pm, progressMonitor, true);

            checkThread.start();

            final WorkflowLoadResult result = WorkflowManager.loadProject(
                    m_workflowFile.getParentFile(), 
                    new ExecutionMonitor(progressMonitor));
            m_editor.setWorkflowManager(result.getWorkflowManager());
            pm.subTask("Finished.");
            pm.done();
            if (result.getWorkflowManager().isDirty()) {
                m_editor.markDirty();
            }
            
            final IStatus status = createStatus(result, 
                    !result.getGUIMustReportDataLoadErrors());
            final String message;
            switch (status.getSeverity()) {
            case IStatus.OK:
                message = "No problems during load.";
                break;
            case IStatus.WARNING:
                message = "Warnings during load";
                logPreseveLineBreaks("Warning during load: " 
                        + result.getFilteredError(
                                "", LoadResultEntryType.Warning), false);
                break;
            default:
                message = "Errors during load";
                logPreseveLineBreaks("Errors during load: " 
                        + result.getFilteredError(
                                "", LoadResultEntryType.Warning), true);
            }
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    // will not open if status is OK.
                    ErrorDialog.openError(
                            Display.getDefault().getActiveShell(), 
                            "Workflow Load", message, status);
                }
                
            });
        } catch (FileNotFoundException fnfe) {
            m_throwable = fnfe;
            LOGGER.fatal("File not found", fnfe);
        } catch (IOException ioe) {
            m_throwable = ioe;
            if (m_workflowFile.length() == 0) {
                LOGGER.info("New workflow created.");
                // this is the only place to set this flag to true: we have an 
                // empty workflow file, i.e. a new project was created
                // bugfix 1555: if an exception is thrown DO NOT create empty 
                // workflow 
                createEmptyWorkflow = true;
            } else {
                LOGGER.error("Could not load workflow from: "
                        + m_workflowFile.getName(), ioe);
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.error("Could not load workflow from: "
                    + m_workflowFile.getName(), ise);
            m_throwable = ise;
        } catch (CanceledExecutionException cee) {
            LOGGER.info("Canceled loading workflow: " 
                    + m_workflowFile.getName());
            m_editor.setWorkflowManager(null);
            m_editor.setLoadingCanceled(true);
            m_editor.setLoadingCanceledMessage(cee.getMessage());
        } catch (Throwable e) {
            m_throwable = e;
            LOGGER.error("Workflow could not be loaded. " + e.getMessage(), e);
            m_editor.setWorkflowManager(null);
        } finally {
            // terminate the check thread
            checkThread.finished();
            // create empty WFM if a new workflow is created 
            // (empty workflow file)
            if (createEmptyWorkflow) {
                m_editor.setWorkflowManager(WorkflowManager.ROOT
                        .createAndAddProject(name));
                // save empty project immediately
                // bugfix 1341 -> see WorkflowEditor line 1294 
                // (resource delta visitor movedTo)
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {                        
                        m_editor.doSave(new NullProgressMonitor());
                    }
                });
                m_editor.setIsDirty(false);
                
            }
            // IMPORTANT: Remove the reference to the file and the
            // editor!!! Otherwise the memory can not be freed later
            m_editor = null;
            m_workflowFile = null;
        }
    }
    
    /** Logs the argument error to LOGGER, preserving line breaks.
     * This method will hopefully go into the NodeLogger facilities (and hence
     * be public API).
     * @param isError Whether to report to LOGGER.error (otherwise warn only).
     * @param error The error string to log.
     */
    private static final void logPreseveLineBreaks(
            final String error, final boolean isError) {
        StringTokenizer t = new StringTokenizer(error, "\n");
        while (t.hasMoreTokens()) {
            if (isError) {
                LOGGER.error(t.nextToken());
            } else {
                LOGGER.warn(t.nextToken());
            }
        }
    }
    
    private static IStatus createStatus(final LoadResultEntry loadResult, 
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
        return new MultiStatus(KNIMEEditorPlugin.PLUGIN_ID, Status.OK, 
                subStatus, loadResult.getMessage(), null);
    }
    
}

