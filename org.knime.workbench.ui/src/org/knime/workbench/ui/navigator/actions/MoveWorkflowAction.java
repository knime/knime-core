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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class MoveWorkflowAction extends Action 
    implements IRunnableWithProgress {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MoveWorkflowAction.class);
    
    private final IPath m_source;

    private final IPath m_target;
    
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
            LOGGER.error("Error while moving resource " + getSource(), e);
        } catch (InterruptedException e) {
            LOGGER.error("Error while moving resource " + getSource(), e);
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
        IPath target = getTarget();
        IResource targetRes = root.findMember(target);
        if (source == null || target == null) {
            return;
        }
        // check whether the target is contained in source 
        if (getSource().isPrefixOf(getTarget())) {
            LOGGER.debug("Operation not allowed. " + source.getName() 
                    + " is parent resource of target " 
                    + getTarget());
            showIsParent(source.getName(), targetRes.getName());
            return;            
        }
        // check if the source is an opened workflow
        if (isOpenedWorkflow(source)) {
            showWorkflowIsOpenMessage();
            return;
        }
        
        if (source != null && !source.isLinked()) {
            final File sourceFile = new File(source.getLocationURI());
            if (!sourceFile.exists()) {
                showUnsupportedLinkedProject(sourceFile.getName());
                return;
            }
            File targetFile = new File(targetRes.getLocationURI());
            LOGGER.debug("target path: " + targetFile);
            // create path here
            target = target.append(getSource());
            File targetDir = new File(targetFile, getSource().toFile()
                    .getName());
            if (!targetDir.mkdir()) {
                LOGGER.debug("target dir could not be created!");
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
                     newProject = MetaInfoFile.createWorkflowSetProject(
                                    newProject.getName());
                }
                // exception handling
                targetRes.refreshLocal(IResource.DEPTH_ONE, monitor);
                source.delete(true, monitor);
            } catch (Exception e) {
                LOGGER.error("Error while moving resource " + source,  e);
                throw new InvocationTargetException(e);
            }
        }
    }
    
    private void showWorkflowIsOpenMessage() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(
                        Display.getDefault().getActiveShell(),
                        "Open Workflow",
                        "Cannot move opened workflows. Please save and close " 
                        + "the open workflow editor.");
            }
        });    
    }

    private boolean isOpenedWorkflow(final IResource source) {
        NodeContainer nc = ProjectWorkflowMap.getWorkflow(
                source.getFullPath().toString());
        if (nc != null) {
            return true;
        }
        return false;
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
    
    private void showIsParent(final String source, final String target) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openWarning(Display.getDefault()
                        .getActiveShell(), "Cannot Move Resource",
                        "Operation not allowed. \"" + source 
                    + "\" is parent resource of target \"" + target + "\"");
            }
        });        
    }


}
