/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   11.09.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.workflowgroup;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.nature.KNIMEWorkflowSetProjectNature;

/**
 * Operation which actually creates the workflow group with its meta info file
 * at the given workspace relative path.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowGroupCreationOperation extends WorkspaceModifyOperation {
    
    private final IPath m_targetPath;
    
    private IContainer m_resultContainer;
    
    /**
     * 
     * @param targetPath the workspace realtive path of the to be created 
     *  resource
     */
    public WorkflowGroupCreationOperation(final IPath targetPath) {
        m_targetPath = targetPath;
    }
    
    /**
     * 
     * @return the created workflow group resource or <code>null</code>, if 
     * this operation was not yet executed
     */
    public IContainer getCreatedWorkflowGroup() {
        return m_resultContainer;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute(final IProgressMonitor monitor) throws CoreException,
            InvocationTargetException, InterruptedException {
        ContainerGenerator generator = new ContainerGenerator(m_targetPath);
        m_resultContainer = generator.generateContainer(monitor);
        if (m_resultContainer instanceof IProject) {
            // get project description
            // set nature
            // set description
            final IProject project = (IProject)m_resultContainer;
            IProjectDescription desc = project.getDescription();
            desc.setNatureIds(new String[] {KNIMEWorkflowSetProjectNature.ID});
            project.setDescription(desc, monitor);
            // only one workflow group can be created at a time
            // -> thus, only necessary to create this meta info file 
        }
        // this has to be done in any case!
        MetaInfoFile.createMetaInfoFile(
                new File(m_resultContainer.getLocationURI()), false);
        m_resultContainer.refreshLocal(
                IResource.DEPTH_ONE, monitor);
    }

}
