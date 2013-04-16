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
