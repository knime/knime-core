/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 *
 * History
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.wizards.project;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.nature.KNIMEProjectNature;

/**
 * Wizard for the creation of a new modeller project. TODO FIXME not yet
 * implemented
 *
 * @author Florian Georg, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH
 */
public class NewProjectWizard extends Wizard implements INewWizard {

    private NewProjectWizardPage m_page;

    /**
     * Creates the wizard.
     */
    public NewProjectWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {

    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        m_page = new NewProjectWizardPage();
        addPage(m_page);
    }

    /**
     * Perform finish - queries the page and creates the project / file.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        final String projectName = m_page.getProjectName();
        // final boolean addDataset = m_page.getAddDataset();

        // Create new runnable
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    // call the worker method
                    doFinish(projectName, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };

        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // get the exception that issued this async exception
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
            return false;
        }

        // found a better solution to update knime resource navigator
        // @see KnimeResourceChangeListener 
        // Delta.CHANGED is now also processed for every resource

        return true;
    }

    /**
     * Worker method, creates the project using the given options.
     *
     * @param projectName Name of the project to create in workspace
     * @param monitor Progress monitor
     * @throws CoreException if error while creating the project
     */
    public static void doFinish(final String projectName,
            final IProgressMonitor monitor) throws CoreException {

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        //
        // 1. Create the project, throw exception if it exists already
        //
        IResource resource = root.findMember(new Path(projectName.trim()));
        if (resource != null) {
            throwCoreException("Project \"" + projectName.trim()
                    + "\" does already exist.", null);
        }
        // Create project description, set the nature IDs and build-commands
        IProject project = root.getProject(projectName.trim());
        
        // actually create the project in workspace
        project.create(monitor);
        // open the project
        project.open(monitor);

        try {
            IProjectDescription description = project.getDescription();
            description.setName(projectName.trim());
            description.setNatureIds(new String[]{KNIMEProjectNature.ID});
            project.setDescription(description, monitor);
        } catch (CoreException ce) {
            throwCoreException("Error while creating project description for " 
                    + project.getName(), ce);
        }

        //
        // 2. Create the optional files, if wanted
        //
        final IFile defaultFile =
                project.getFile(WorkflowPersistor.WORKFLOW_FILE);

        InputStream is = new ByteArrayInputStream("".getBytes());
        defaultFile.create(is, true, monitor);

        // open the default file, if it was created

        // open the model file in the editor
        monitor.setTaskName("Opening file for editing...");
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page =
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage();
                try {
                    IDE.openEditor(page, defaultFile, true);
                } catch (PartInitException e) {
                    // ignore it
                }
            }
        });

        // just to make sure: refresh the new project
//        project.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
    }

    private static void throwCoreException(final String message, 
            final Throwable t) 
        throws CoreException {
        IStatus status =
                new Status(IStatus.ERROR, "org.knime.workbench.ui", IStatus.OK,
                        message, t);
        throw new CoreException(status);
    }
}
