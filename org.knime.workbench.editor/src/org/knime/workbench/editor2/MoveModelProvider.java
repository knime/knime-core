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
                workflowFile = project.getFile("workflow.knime");

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
