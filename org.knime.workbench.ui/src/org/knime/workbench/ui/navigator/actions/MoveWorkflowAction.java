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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class MoveWorkflowAction extends Action 
    implements IRunnableWithProgress {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MoveWorkflowAction.class);

    private IPath m_source;

    private IPath m_target;

    /**
     * 
     * @param source path to the source (which sould be moved)
     * @param target target to which the source should be moved
     */
    public MoveWorkflowAction(final IPath source, final IPath target) {
        m_source = source;
        m_target = target;
    }

    /**
     * 
     * @return path to the source (which sould be transfered)
     */
    public IPath getSource() {
        return m_source;
    }

    /**
     * 
     * @return target to which the source should be transfered
     */
    public IPath getTarget() {
        return m_target;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            PlatformUI.getWorkbench().getProgressService()
                    .busyCursorWhile(this);
        } catch (InvocationTargetException e) {
            LOGGER.error(e);
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }

    }

    private void copyFiles(final File source, final File target) {
        for (File f : source.listFiles()) {
            f.renameTo(new File(target, f.getName()));
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void run(final IProgressMonitor monitor) 
        throws InvocationTargetException, InterruptedException {
        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        final IResource source = root.findMember(getSource());
        if (source != null && !source.isLinked()) {
            final File sourceFile = new File(source.getLocationURI());
            if (!sourceFile.exists()) {
                showUnsupportedLinkedProject(sourceFile.getName());
                return;
            }
            IPath target = getTarget();
            IResource targetRes = root.findMember(target);
            File targetFile = new File(targetRes.getLocationURI());
            LOGGER.debug("target path: " + targetFile);
            // create path here
            target = target.append(getSource());
            File targetDir = new File(targetFile, getSource().toFile()
                    .getName());
            if (!targetDir.mkdir()) {
                LOGGER.error("target dir could not be created!");
                showAlreadyExists(targetDir.getName(), targetFile.getName());
                return;
            }
            copyFiles(sourceFile, targetDir);
            try {
                if (targetRes instanceof IWorkspaceRoot) {
                    IProject newProject = ((IWorkspaceRoot)targetRes)
                            .getProject(sourceFile.getName());
                    if (newProject.exists()) {
                        // exception handling -> project already exists
                        LOGGER.warn("A workflow " + sourceFile.getName()
                                + " already exists in /");
                        showAlreadyExists(newProject.getName(), 
                                "workspace root");
                        return;
                    }
                    newProject.create(monitor);
                    newProject.open(monitor);
                }
                // TODO: exception handling
                targetRes.refreshLocal(IResource.DEPTH_ONE, monitor);
                source.delete(true, monitor);
            } catch (CoreException e) {
                LOGGER.error(e);
                throw new InvocationTargetException(e);
            }
        }
    }
    
    private void showUnsupportedLinkedProject(final String name) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(
                        Display.getDefault().getActiveShell(),
                        "Unsupported Linked Project",
                        "\"" + name + "\" is a linked resource. " 
                        + "Linked resources are only linked to the workspace " 
                        + "but located elsewhere. They are not supported by " 
                        + "this operation.");
            }
        });    
    }
    
    private void showAlreadyExists(final String name, final String target) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openWarning(Display.getDefault()
                        .getActiveShell(), "Resource already exists", 
                        "A folder \"" + name + "\" already exists in \""
                        + target + "\". Please rename before moving.");
            }
        });
    }
    

}
