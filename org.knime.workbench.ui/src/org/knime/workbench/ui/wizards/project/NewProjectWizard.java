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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.wizards.project;

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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.Workbench;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.builder.KNIMEProjectBuilder;
import org.knime.workbench.ui.nature.KNIMEProjectNature;
import org.knime.workbench.ui.navigator.KnimeResourceNavigator;

/**
 * Wizard for the creation of a new modeller project. TODO FIXME not yet
 * implemented
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewProjectWizard extends Wizard implements INewWizard {
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
        KNIME_NATURES = new String[]{KNIMEProjectNature.class.getName()};
        KNIME_BUILDSPECS = new ICommand[]{KNIME_BUILDER};
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
     * @see org.eclipse.jface.wizard.IWizard#performFinish()
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

        // workaround to redraw the knime navigation tree
        // TODO: try to find a better solution
        // update knime resource navigator
        updateResourceNavigator();

        return true;
    }

    private void updateResourceNavigator() {

        // workaround to redraw the knime navigation tree
        // TODO: try to find a better solution
        // update knime resource navigator

        // find all resource navigators and refresh them
        // aditionally exampd all items to remove the + signs
        for (IWorkbenchWindow window : Workbench.getInstance()
                .getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference reference : page.getViewReferences()) {
                    IViewPart viewPart = reference.getView(true);
                    if (viewPart instanceof KnimeResourceNavigator) {
                        TreeViewer viewer =
                                ((KnimeResourceNavigator)viewPart).getViewer();
                        viewer.refresh();
//                        viewer.expandToLevel(2);
                    }
                }
            }
        }
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
                    + "\" does already exist.");
        }
        // Create project description, set the nature IDs and build-commands
        IProject project = root.getProject(projectName.trim());
        ProjectDescription description = new ProjectDescription();
        description.setName(projectName.trim());
        description.setNatureIds(KNIME_NATURES);
        description.setBuildSpec(KNIME_BUILDSPECS);
        // actually create the project in workspace
        project.create(description, monitor);
        // open the project
        project.open(monitor);

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

    private static void throwCoreException(final String message) throws CoreException {
        IStatus status =
                new Status(IStatus.ERROR, "org.knime.workbench.ui", IStatus.OK,
                        message, null);
        throw new CoreException(status);
    }
}
