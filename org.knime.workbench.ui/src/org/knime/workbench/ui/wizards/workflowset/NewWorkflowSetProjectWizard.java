/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.workbench.ui.wizards.workflowset;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.nature.KNIMEWorkflowSetProjectNature;


/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class NewWorkflowSetProjectWizard extends Wizard implements INewWizard {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            NewWorkflowSetProjectWizard.class); 
    
    private WizardNewProjectCreationPage m_page;

    @Override
    public boolean performFinish() {
        final IProject project = m_page.getProjectHandle();
        try {
        PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
                new IRunnableWithProgress() {
                    @Override
                    public void run(final IProgressMonitor monitor)
                            throws InvocationTargetException,
                            InterruptedException {
                        try {
                            project.create(monitor);
                            project.open(monitor);
                        } catch (CoreException e) {
                            LOGGER.error("Failed to create project " 
                                    + project.getName(), e);
                            throw new RuntimeException(e);
                        }
                        monitor.done();
                    }
                });
            IProjectDescription desc = project.getDescription();
            desc.setNatureIds(new String[] {KNIMEWorkflowSetProjectNature.ID});
            project.setDescription(desc, null);
            MetaInfoFile.createMetaInfoFile(new File(project.getLocationURI()), 
                    false);
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
                    new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    try {
                        project.refreshLocal(
                                IResource.DEPTH_ONE, monitor);
                    } catch (CoreException ce) {
                        System.err.println(ce);
                    }
                    
                }
            });
            LOGGER.debug("Project " + project.getName() 
                    + " successfully created!");
            return true;
        } catch (Exception ce) {
            LOGGER.error("Failed to create project " + project.getName(), ce);
        }
        return false;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench, 
            final IStructuredSelection selection) {
        setWindowTitle("Create New Workflow Set Project");
//        m_page.setDescription("Creates a new )
        m_page = new WizardNewProjectCreationPage(
                "Create New Workflow Set Project");
        m_page.setDescription("Creates a new Workflow Set Project, " 
                + "which allows for the grouping of several KNIME workfows.");
        addPage(m_page);
    }

}
