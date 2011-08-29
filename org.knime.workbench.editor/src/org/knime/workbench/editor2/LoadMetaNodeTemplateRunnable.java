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
 * History
 *   13.04.2011 (wiswedel): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import javax.swing.UIManager;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.WorkflowPersistorVersion1xx;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;


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
public class LoadMetaNodeTemplateRunnable extends PersistWorkflowRunnable {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LoadMetaNodeTemplateRunnable.class);

    private WorkflowManager m_parentWFM;

    private LocalExplorerFileStore m_templateKNIMEFolder;

    private Throwable m_throwable = null;

    /** Message, which is non-null if the user canceled to the load. */
    private String m_loadingCanceledMessage;

    private WorkflowLoadResult m_result;

    /**
     *
     * @param wfm target workflow (where to insert)
     * @param templateKNIMEFolder the workflow dir from which the template
     *        should be loaded
     */
    public LoadMetaNodeTemplateRunnable(final WorkflowManager wfm,
            final LocalExplorerFileStore templateKNIMEFolder) {
        m_parentWFM = wfm;
        m_templateKNIMEFolder = templateKNIMEFolder;
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
     * {@inheritDoc}
     */
    @Override
    public void run(final IProgressMonitor pm) {
        m_throwable = null;

        try {
            // create progress monitor
            ProgressHandler progressHandler = new ProgressHandler(pm, 101,
                    "Loading meta node template...");
            final CheckCancelNodeProgressMonitor progressMonitor
                = new CheckCancelNodeProgressMonitor(pm);
            progressMonitor.addProgressListener(progressHandler);

            File parentFile = m_templateKNIMEFolder.toLocalFile(EFS.NONE, null);
            Display d = Display.getDefault();

            GUIWorkflowLoadHelper loadHelper =
                new GUIWorkflowLoadHelper(d, parentFile.getName(), true);
            WorkflowPersistorVersion1xx loadPersistor =
                WorkflowManager.createLoadPersistor(parentFile, loadHelper);
            URI sourceURI = m_templateKNIMEFolder.toURI();
            loadPersistor.setTemplateInformationLinkURI(sourceURI);
            loadPersistor.setNameOverwrite(parentFile.getName());
            m_result = m_parentWFM.load(loadPersistor,
                    new ExecutionMonitor(progressMonitor), false);
            pm.subTask("Finished.");
            pm.done();

            final IStatus status = createStatus(m_result,
                    !m_result.getGUIMustReportDataLoadErrors());
            final String message;
            switch (status.getSeverity()) {
            case IStatus.OK:
                message = "No problems during load.";
                break;
            case IStatus.WARNING:
                message = "Warnings during load";
                logPreseveLineBreaks("Warnings during load: "
                        + m_result.getFilteredError(
                                "", LoadResultEntryType.Warning), false);
                break;
            default:
                message = "Errors during load";
                logPreseveLineBreaks("Errors during load: "
                        + m_result.getFilteredError(
                                "", LoadResultEntryType.Warning), true);
            }
            Display.getDefault().asyncExec(new Runnable() {
                @Override
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
            LOGGER.error("Could not load meta node from: "
                        + m_templateKNIMEFolder.getName(), ioe);
        } catch (InvalidSettingsException ise) {
            LOGGER.error("Could not load meta node from: "
                    + m_templateKNIMEFolder.getName(), ise);
            m_throwable = ise;
        } catch (UnsupportedWorkflowVersionException uve) {
            m_loadingCanceledMessage =
                "Canceled meta node load due to incompatible version";
            LOGGER.info(m_loadingCanceledMessage, uve);
        } catch (CanceledExecutionException cee) {
            m_loadingCanceledMessage =
                "Canceled loading meta node: " + m_templateKNIMEFolder.getName();
            LOGGER.info(m_loadingCanceledMessage, cee);
        } catch (Throwable e) {
            m_throwable = e;
            LOGGER.error("Meta node could not be loaded. " + e.getMessage(), e);
        } finally {
            // IMPORTANT: Remove the reference to the file and the
            // editor!!! Otherwise the memory can not be freed later
            m_parentWFM = null;
            m_templateKNIMEFolder = null;
        }
    }

    /** @return True if the load process has been interrupted. */
    public boolean hasLoadingBeenCanceled() {
        return m_loadingCanceledMessage != null;
    }

    /** @return the loadingCanceledMessage, non-null if
     * {@link #hasLoadingBeenCanceled()}. */
    public String getLoadingCanceledMessage() {
        return m_loadingCanceledMessage;
    }

    /** @return the result */
    public WorkflowLoadResult getWorkflowLoadResult() {
        return m_result;
    }

    /** Set fields to null so that they can get GC'ed. */
    public void discard() {
        m_result = null;
        m_templateKNIMEFolder = null;
        m_parentWFM = null;
    }

}

