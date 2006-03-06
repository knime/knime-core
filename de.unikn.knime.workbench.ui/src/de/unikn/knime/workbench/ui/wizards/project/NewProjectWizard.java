/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   12.01.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.ui.wizards.project;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.workbench.ui.builder.KNIMEProjectBuilder;
import de.unikn.knime.workbench.ui.nature.KNIMEProjectNature;

/**
 * Wizard for the creation of a new modeller project. TODO FIXME not yet
 * implemented
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewProjectWizard extends Wizard implements INewWizard {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NewProjectWizard.class);

    /**
     * Build command invoking KNIME project builder.
     */
    public static final ICommand KNIME_BUILDER;

    /** List of natures that should be given to the new project. */
    public static final String[] KNIME_NATURES;

    /** List of build commands that should be given to the new project. * */
    public static final ICommand[] KNIME_BUILDSPECS;

    static {
        KNIME_BUILDER = new BuildCommand();
        KNIME_BUILDER.setBuilderName(KNIMEProjectBuilder.BUILDER_ID);
        KNIME_NATURES = new String[] {KNIMEProjectNature.class.getName()};
        KNIME_BUILDSPECS = new ICommand[] {KNIME_BUILDER};
    }

    private NewProjectWizardPage m_page;

    /**
     * Creates the wizard.
     */
    public NewProjectWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    /**
     * @see org.eclipse.ui.IWorkbenchWizard #init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {

    }

    /**
     * Adding the page to the wizard.
     */
    public void addPages() {
        m_page = new NewProjectWizardPage();
        addPage(m_page);
    }

    /**
     * Perform finish - queries the page and creates the project / file.
     * 
     * @see org.eclipse.jface.wizard.IWizard#performFinish()
     */
    public boolean performFinish() {

        final String projectName = m_page.getProjectName();
        final boolean addWorkflowFile = m_page.getCreateWorkflowFile();
        //final boolean addDataset = m_page.getAddDataset();

        // Create new runnable
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    // call the worker method
                    doFinish(projectName, addWorkflowFile, false, monitor);
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
        return true;

    }

    /**
     * Worker method, creates the project using the given options.
     * 
     * @param projectName Name of the project to create in workspace
     * @param addWorkflowFile flag, indicating that a default.knime should be
     *            created
     * @param addDataset flag, indicating that an example dataset (IRIS) should
     *            be copied into the new project
     * @param monitor Progress monitor
     * @throws CoreException if error while creating the project
     */
    protected void doFinish(final String projectName,
            final boolean addWorkflowFile, final boolean addDataset,
            final IProgressMonitor monitor) throws CoreException {

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        //
        // 1. Create the project, throw exception if it exists already
        //
        IResource resource = root.findMember(new Path(projectName.trim()));
        if (resource != null) {
            throwCoreException("Project \"" + projectName.trim()
                    + "\" does already exist.");
        }
        // Create project description, set the nature IDs and build-commands
        IProject project = root.getProject(projectName.trim());
        ProjectDescription description = new ProjectDescription();
        description.setNatureIds(KNIME_NATURES);
        description.setBuildSpec(KNIME_BUILDSPECS);
        // actually create the project in workspace
        project.create(description, monitor);
        // open the project
        project.open(monitor);

        //
        // 2. Create the optional files, if wanted
        //
        final IFile defaultFile = project.getFile("Workflow.knime");
        if (addWorkflowFile) {
            InputStream is = new ByteArrayInputStream("".getBytes());
            defaultFile.create(is, true, monitor);
        }

        if (addDataset) {
            // TODO copy iris dataset from plugin into new project.
            LOGGER.warn("Add dataset: not implemented yet");
        }

        // open the default file, if it was created
        if (addWorkflowFile) {
            // open the model file in the editor
            monitor.setTaskName("Opening file for editing...");
            getShell().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage page = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    try {
                        IDE.openEditor(page, defaultFile, true);
                    } catch (PartInitException e) {
                    }
                }
            });
        }

        // just to make sure: refresh the new project
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    }

    private void throwCoreException(final String message) throws CoreException {
        IStatus status = new Status(IStatus.ERROR,
                "de.unikn.knime.workbench.ui", IStatus.OK, message, null);
        throw new CoreException(status);
    }

}
