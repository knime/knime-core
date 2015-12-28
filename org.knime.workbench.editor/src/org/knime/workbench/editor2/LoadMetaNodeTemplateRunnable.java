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
 *   13.04.2011 (wiswedel): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.net.URI;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.TemplateNodeContainerPersistor;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * A runnable which is used by the {@link WorkflowEditor} to load a workflow
 * with a progress bar. NOTE: As the {@link UIManager} holds a reference to this
 * runnable an own class file is necessary such that all references to the
 * created workflow manager can be deleted, otherwise the manager cannot be
 * deleted later and the memory cannot be freed.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public class LoadMetaNodeTemplateRunnable extends PersistWorkflowRunnable {
    private WorkflowManager m_parentWFM;

    private AbstractExplorerFileStore m_templateKNIMEFolder;

    private MetaNodeLinkUpdateResult m_result;

    /**
     *
     * @param wfm target workflow (where to insert)
     * @param templateKNIMEFolder the workflow dir from which the template
     *            should be loaded
     */
    public LoadMetaNodeTemplateRunnable(final WorkflowManager wfm,
            final AbstractExplorerFileStore templateKNIMEFolder) {
        m_parentWFM = wfm;
        m_templateKNIMEFolder = templateKNIMEFolder;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("null")
    @Override
    public void run(final IProgressMonitor pm) {
        try {
            // create progress monitor
            ProgressHandler progressHandler = new ProgressHandler(pm, 101, "Loading metanode template...");
            final CheckCancelNodeProgressMonitor progressMonitor = new CheckCancelNodeProgressMonitor(pm);
            progressMonitor.addProgressListener(progressHandler);

            URI sourceURI = m_templateKNIMEFolder.toURI();
            File parentFile = ResolverUtil.resolveURItoLocalOrTempFile(sourceURI, pm);
            if (pm.isCanceled()) {
                throw new InterruptedException();
            }

            Display d = Display.getDefault();
            GUIWorkflowLoadHelper loadHelper =
                    new GUIWorkflowLoadHelper(d, parentFile.getName(), parentFile, null, true);
            TemplateNodeContainerPersistor loadPersistor =
                    loadHelper.createTemplateLoadPersistor(parentFile, sourceURI);
            MetaNodeLinkUpdateResult loadResult =
                    new MetaNodeLinkUpdateResult("Template from \"" + sourceURI + "\"");
            m_parentWFM.load(loadPersistor, loadResult, new ExecutionMonitor(progressMonitor), false);
            m_result = loadResult;
            if (pm.isCanceled()) {
                throw new InterruptedException();
            }
            pm.subTask("Finished.");
            pm.done();

            final IStatus status =
                    createStatus(m_result,
                            !m_result.getGUIMustReportDataLoadErrors());
            final String message;
            switch (status.getSeverity()) {
                case IStatus.OK:
                    message = "No problems during load.";
                    break;
                case IStatus.WARNING:
                    message = "Warnings during load";
                    logPreseveLineBreaks(
                            "Warnings during load: "
                                    + m_result.getFilteredError("",
                                            LoadResultEntryType.Warning), false);
                    break;
                default:
                    message = "Errors during load";
                    logPreseveLineBreaks(
                            "Errors during load: "
                                    + m_result.getFilteredError("",
                                            LoadResultEntryType.Warning), true);
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            // IMPORTANT: Remove the reference to the file and the
            // editor!!! Otherwise the memory cannot be freed later
            m_parentWFM = null;
            m_templateKNIMEFolder = null;
        }
    }

    /** @return the result */
    public MetaNodeLinkUpdateResult getLoadResult() {
        return m_result;
    }
}
