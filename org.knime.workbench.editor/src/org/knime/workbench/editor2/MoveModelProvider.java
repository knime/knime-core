/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Dec 19, 2006 (sieb): created
 */
package org.knime.workbench.editor2;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ModelStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 * This model provider is registered in the plugin.xml and checks if a KNIIME
 * workflow project is intended to be renamed. The reason is, that at the moment
 * the KNIME core saves an absolute path for storage reasons. If renamed save
 * actions would fail.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class MoveModelProvider extends ModelProvider {

    /**
     * Returns the id of this provider.
     *
     * @return the id of this provider
     */
    public String getModelProviderId() {
        return "org.knime.workbench.editor2.MoveModelProvider";
    }

    /**
     * Checks if a KNIIME workflow project is intended to be renamed. The reason
     * is, that at the moment the KNIME core saves an absolute path for storage
     * reasons. If renamed save actions would fail.
     *
     * @see org.eclipse.core.resources.mapping.ModelProvider#
     *      validateChange(org.eclipse.core.resources.IResourceDelta,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public IStatus validateChange(final IResourceDelta delta,
            final IProgressMonitor monitor) {

        try {

            // check whether this is a knime project
            // boolean existsworkflow =
            // delta.getAffectedChildren()[1].getResource().getProject()
            // .exists(new Path("workflow.knime"));
            IProject project = null;
            IFile workflowFile = null;
            for (IResourceDelta affectedChild : delta.getAffectedChildren()) {
                project = affectedChild.getResource().getProject();
                workflowFile = project.getFile(
                        WorkflowPersistor.WORKFLOW_FILE);

                // break if we found the project with a knime workflow
                if (workflowFile.exists()) {
                    break;
                }
            }

            // check whether this is a move delta
            IResourceDelta[] deltas = delta.getAffectedChildren();
            boolean moveAction = false;
            for (IResourceDelta curDelta : deltas) {

                if ((curDelta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
                    moveAction = true;
                    break;
                }
            }

            if (workflowFile.exists() && moveAction) {

                // check if an editor is opened on this resource
                boolean editorOpen = false;
                IWorkbenchWindow[] windows =
                        PlatformUI.getWorkbench().getWorkbenchWindows();

                for (IWorkbenchWindow window : windows) {
                    IWorkbenchPage[] pages = window.getPages();
                    for (IWorkbenchPage page : pages) {
                        IEditorPart editorPart =
                                page.findEditor(new FileEditorInput(
                                        workflowFile));

                        if (editorPart != null) {
                            editorOpen = true;
                            break;
                        }
                    }
                }

                if (editorOpen) {
                    return new ModelStatus(
                            IStatus.WARNING,
                            ResourcesPlugin.PI_RESOURCES,
                            getModelProviderId(),
                            "Renaming "
                                    + project.getName()
                                    + " may have undesirable side effects "
                                    + "since this workflow is currently open."
                                    + " You may loose some or all of your "
                                    + "data if you continue (although the "
                                    + "settings will"
                                    + " not be affected). Consider closing the "
                                    + "workflow before renaming it.");
                }
            }
        } catch (Throwable t) {
            // do nothing
        }

        // else performe the default behavior
        return super.validateChange(delta, monitor);
    }
}
