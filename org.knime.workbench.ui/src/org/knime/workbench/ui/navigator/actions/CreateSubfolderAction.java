/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.nature.KNIMEProjectNature;
import org.knime.workbench.ui.navigator.KnimeResourceNavigator;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class CreateSubfolderAction extends Action {
    
    private IContainer m_parent;
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Create Subfolder...";
    }
    
    private KnimeResourceNavigator getNavigator() {
        IWorkbenchPage page = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage();
        IViewPart view = page.findView(KnimeResourceNavigator.ID);
        if (view == null || !(view instanceof KnimeResourceNavigator)) {
            // someone else has registered a view with the same ID
            throw new IllegalArgumentException(
                    "ID of Workflow Group Navigator ("
                    + KnimeResourceNavigator.ID + ") "
                    + " is also used by " + view.getClass());
        }
        return (KnimeResourceNavigator)view;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        // getSelection from navigator
        IWorkbenchPage page = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage();
        ISelection sel = page.getSelection(KnimeResourceNavigator.ID);
        if (!(sel instanceof IStructuredSelection)) {
            return false;
        }
        IStructuredSelection strucSel = (IStructuredSelection)sel;
        if (strucSel.size() > 1 || strucSel.size() == 0) {
            // multiple selection or empty
            return false;
        } else {
            Object o = strucSel.getFirstElement();    
            if (o instanceof IFolder) {
                IFolder folder = (IFolder)o;
                if (folder.findMember(
                        WorkflowPersistor.WORKFLOW_FILE) != null) {
                    return false;
                }
            } else if (o instanceof IProject) {
                // check that it is not a KNIME project
                IProject p = (IProject)o;
                try {
                    if (!p.isOpen() 
                            || p.getNature(KNIMEProjectNature.ID) != null) {
                        return false;
                    }
                } catch (CoreException e) {
                    e.printStackTrace();
                    return false;
                }
            } else if (o instanceof IFile) {
                return false;
            }
            m_parent = (IContainer)o;
            return true;
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void run() {
        InputDialog dialog = new InputDialog(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                "Enter Subfolder Name",
                "Please enter the name of the subfolder: ",
                "folder name...", 
                new IInputValidator() {

                    @Override
                    public String isValid(final String newText) {
                        if (newText.trim().length() == 0) {
                            return "Please enter the name of the subfolder";
                        }
                        if (m_parent.findMember(newText) != null) {
                            return "File " + newText + " already exists in "
                            + m_parent.getName();
                        }
                        return null;
                    }
                    
                });
        if (dialog.open() == Window.OK) {
            String name = dialog.getValue();
            if (name == null) {
                throw new IllegalArgumentException(
                        "Name of the the subfolder must not be null!");
            }
            // create the subfolder here
            File parent = m_parent.getLocation().toFile();
            if (parent == null) {
                throw new RuntimeException("parent must not be null!");
            }
            File subFolder = new File(parent, name);
            subFolder.mkdir();
            try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
                    new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    try {
                        m_parent.refreshLocal(
                                IResource.DEPTH_INFINITE, monitor);
                    } catch (CoreException ce) {
                        System.err.println(ce);
                    }
                    
                }
            });
            } catch (Exception e) {
                System.err.println(e);
            }
            getNavigator().getTreeViewer().refresh(m_parent);
        }
    }

}
