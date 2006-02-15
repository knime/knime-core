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
package de.unikn.knime.workbench.ui.wizards.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.unikn.knime.workbench.core.WorkbenchErrorLogger;


/**
 * This is a sample new wizard. Its role is to create a new file resource in the
 * provided container. If the container resource (a folder or a project) is
 * selected in the workspace when the wizard is opened, it will accept it as the
 * target container. The wizard creates one file with the extension "knime". If
 * a sample multi-page editor (also available as a template) is registered for
 * the same extension, it will be able to open it.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewWorkflowWizard extends Wizard implements INewWizard {
    private NewWorkflowPage m_page;

    private ISelection m_selection;

    /**
     * Constructor for NewWorkflowWizard.
     */
    public NewWorkflowWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */
    public void addPages() {
        m_page = new NewWorkflowPage(m_selection);
        addPage(m_page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     * 
     * @return If finished successfully
     */
    public boolean performFinish() {
        final String containerName = m_page.getContainerName();
        final String fileName = m_page.getFileName();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(containerName, fileName, monitor);
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
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
            return false;
        }
        return true;
    }

    /**
     * The worker method. It will find the container, create the file if missing
     * or just replace its contents, and open the editor on the newly created
     * file.
     */
    private void doFinish(final String containerName,
            final String fileName, final IProgressMonitor monitor)
            throws CoreException {
        // create a sample file
        monitor.beginTask("Creating " + fileName, 3);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(new Path(containerName));
        if (!resource.exists() || !(resource instanceof IContainer)) {
            throwCoreException("Container \"" + containerName
                    + "\" does not exist.");
        }
        IContainer container = (IContainer) resource;
        final IFile file = container.getFile(new Path(fileName));
        try {
            InputStream stream = openContentStream();
            if (file.exists()) {
                file.setContents(stream, true, true, monitor);
            } else {
                file.create(stream, true, monitor);
            }
            stream.close();
        } catch (IOException e) {
            WorkbenchErrorLogger.error("Can't create file", e);

        }
        monitor.worked(1);
        monitor.setTaskName("Opening file for editing...");
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                try {
                    IDE.openEditor(page, file, true);
                } catch (PartInitException e) {
                    WorkbenchErrorLogger.error("Can't open file", e);
                }
            }
        });
        monitor.worked(1);
        
        // just to make sure: refresh the new project
        container.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        monitor.worked(1);

    }

    /**
     * We will initialize file contents with a sample text.
     */
    private InputStream openContentStream() {
        String contents = "";
        return new ByteArrayInputStream(contents.getBytes());
    }

    private void throwCoreException(final String message)
            throws CoreException {
        IStatus status = new Status(IStatus.ERROR,
                "de.unikn.knime.workbench.ui", IStatus.OK, message, null);
        throw new CoreException(status);
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize
     * from it.
     * 
     * @see org.eclipse.ui.IWorkbenchWizard# init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {
        this.m_selection = selection;
    }
}

