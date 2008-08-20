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
 *   02.07.2006 (sieb): created
 */
package org.knime.workbench.ui.wizards.export;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.dialogs.ExportWizard;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileExportOperation;
import org.knime.core.node.NodePersistorVersion200;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 * This wizard is intended to export a knime workflow project.
 * 
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowExportWizard extends ExportWizard implements IExportWizard {
    private WorkflowExportPage m_page;

    private ISelection m_selection;

    /**
     * Constructor.
     */
    public WorkflowExportWizard() {
        super();
        setWindowTitle("Export");
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        m_page = new WorkflowExportPage(m_selection);
        addPage(m_page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     * 
     * @return If finished successfully
     */
    @Override
    public boolean performFinish() {

        // first save dirty editors
        boolean canceled =
                !IDEWorkbenchPlugin.getDefault().getWorkbench().saveAllEditors(
                        true);
        if (canceled) {
            return false;
        }
        final String containerName = m_page.getContainerName();
        final String fileName = m_page.getFileName().trim();
        final boolean excludeData = m_page.excludeData();

        // if the specified export file already exist ask the user
        // for confirmation

        final File exportFile = new File(fileName);
        if (exportFile.exists()) {

            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
            mb.setText("File already exists...");
            mb.setMessage("File already exists.\nDo you want to "
                    + "overwrite the specified file ?");
            if (mb.open() != SWT.YES) {
                return false;
            }
        }

        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(containerName, exportFile, monitor, excludeData);
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
            String message = realException.getMessage();

            message = "Problem during export: " + e.getMessage();

            MessageDialog.openError(getShell(), "Error", message);
            return false;
        }
        return true;
    }

    /**
     * Implements the exclude policy. At the moment the "intern" folder and
     * "*.zip" files are excluded.
     * 
     * @param resource the resource to check
     */
    private boolean excludeResource(final IResource resource) {

        String name = resource.getName();

        if (name.equals("internal")) {
            return true;
        }

        if (resource.getType() == IResource.FILE) {
            if (name.startsWith("model_")) {
                return true;
            }
            if (name.equals("data.xml")) {
                return true;
            }
        }
        
        // exclusion list for workflows in format of 2.x
        switch (resource.getType()) {
        case IResource.FOLDER: 
            if (name.startsWith(NodePersistorVersion200.PORT_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(
                    NodePersistorVersion200.INTERNAL_TABLE_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(NodePersistorVersion200.INTERN_FILE_DIR)) {
                return true;
            }
            break;
        case IResource.FILE:
            if (name.startsWith(WorkflowPersistor.SAVED_WITH_DATA_FILE)) {
                return true;
            }
            break;
        default:
        }

        // get extension to check if this resource is a zip file
        return name.toLowerCase().endsWith(".zip");
    }

    private void addNonExcludingFiles(final List resouceList,
            final IResource resource) {

        // if this resource must be excluded do not add to resource list and
        // return
        if (excludeResource(resource)) {
            return;
        }

        // if this is a file add it to the list
        if (resource.getType() == IResource.FILE) {
            resouceList.add(resource);
            return;
        }

        IResource[] resources;
        try {
            resources = ((IContainer)resource).members();
        } catch (Exception e) {
            throw new IllegalStateException("Members of folder "
                    + resource.getName() + " could not be retrieved.");
        }
        // else vistit all child resources
        for (IResource currentResource : resources) {
            addNonExcludingFiles(resouceList, currentResource);
        }
    }

    /**
     * The worker method. It will find the container, create the export file if
     * missing or just replace its contents.
     */
    private void doFinish(final String containerName, final File fileName,
            final IProgressMonitor monitor, final boolean excludeData)
            throws CoreException {

        // start zipping
        monitor.beginTask("Collect resources... ", 3);
        
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(new Path(containerName));
        if (!resource.exists() || !(resource instanceof IContainer)) {
            throwCoreException("Container \"" + containerName
                    + "\" does not exist.");
        }

        IContainer container = (IContainer)resource;
        container.refreshLocal(IResource.DEPTH_INFINITE, monitor);

        // if the data should be excluded from the export
        // iterate over the resources and add only the wanted stuff
        // i.e. the "intern" folder and "*.zip" files are excluded
        List resourceList = new ArrayList();
        if (excludeData) {
            addNonExcludingFiles(resourceList, container);
        } else {
            resourceList.add(container);
        }
        
        monitor.worked(1);

        ArchiveFileExportOperation exportOperation =
                new ArchiveFileExportOperation(resourceList, fileName.getPath());

        monitor.beginTask("Write to file... " + fileName, 3);

        try {
            exportOperation.run(monitor);
        } catch (Throwable t) {
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_WARNING | SWT.OK);
            mb.setText("Export could not be completed...");
            mb.setMessage("Knime project could not be exported.\n Reason: "
                    + t.getMessage());
        }

        // final IFile file = container.getFile(new Path(fileName));
        // try {
        // InputStream stream = openContentStream();
        // if (file.exists()) {
        // file.setContents(stream, true, true, monitor);
        // } else {
        // file.create(stream, true, monitor);
        // }
        // stream.close();
        // } catch (IOException e) {
        // WorkbenchErrorLogger.error("Can't create file", e);
        //
        // }

        // getShell().getDisplay().asyncExec(new Runnable() {
        // public void run() {
        // IWorkbenchPage page = PlatformUI.getWorkbench()
        // .getActiveWorkbenchWindow().getActivePage();
        // try {
        // IDE.openEditor(page, file, true);
        // } catch (PartInitException e) {
        // WorkbenchErrorLogger.error("Can't open file", e);
        // }
        // }
        // });
        monitor.worked(1);

    }

    /**
     * We will initialize file contents with a sample text.
     */
    InputStream openContentStream() {
        String contents = "";
        return new ByteArrayInputStream(contents.getBytes());
    }

    private void throwCoreException(final String message) throws CoreException {
        IStatus status =
                new Status(IStatus.ERROR, "org.knime.workbench.ui", IStatus.OK,
                        message, null);
        throw new CoreException(status);
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize
     * from it.
     * 
     * @see org.eclipse.ui.IWorkbenchWizard# init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {
        this.m_selection = selection;
    }
}
