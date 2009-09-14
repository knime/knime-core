/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   17.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

/**
 * Convenience class to be used when dealing with KNIME resources, such as 
 * workflows and workflow groups.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public final class KnimeResourceUtil {
    
    /**
     * Path to the meta info file to be used in 
     *  {@link IContainer#exists(IPath)}.
     */
    public static final IPath META_INFO_FILE = new Path(
            MetaInfoFile.METAINFO_FILE);
    /**
     * Path to the workflow file to be used in 
     *  {@link IContainer#exists(IPath)}.
     */    
    public static final IPath WORKFLOW_FILE = new Path(
            WorkflowPersistor.WORKFLOW_FILE); 
    
    // This is a not published part of our API - internal use only 
    private static final Path NODE_FILE = new Path(
            SingleNodeContainerPersistorVersion200.SETTINGS_FILE_NAME);
    
    private KnimeResourceUtil() {
        // utility class
    }
    
    /**
     * 
     * @param resource the resource to check
     * @return true if the resource is a node
     */
    public static boolean isNode(final IResource resource) {
        if (resource == null) {
            return false;
        }
        if (resource instanceof IFile) {
            return false;
        }
        if (!(resource instanceof IContainer)) {
            return false;
        }
        IContainer container = (IContainer)resource;
        return container.exists(NODE_FILE);
    }

    /**
     * 
     * @param resource the resource to test
     * @return true if the resource contains a workflow
     */
    public static boolean isWorkflow(final IResource resource) {
        if (resource == null) {
            return false;
        }
        if (resource instanceof IFile) {
            return false;
        }
        if (!(resource instanceof IContainer)) {
            return false;
        }
        IContainer container = (IContainer)resource;
        // if container contains a workflow file but not its parent
        IContainer parent = container.getParent();
        if (parent != null) {
            if (parent.exists(WORKFLOW_FILE)) {
                return false;
            }
        }
        return container.exists(WORKFLOW_FILE);
    }
    
    /**
     * 
     * @param resource resource to test
     * @return true if the resource is a meta node
     */
    public static boolean isMetaNode(final IResource resource) {
        // contains workflow file AND parent also has workflow file
        if (resource == null) {
            return false;
        }
        if (resource instanceof IFile) {
            return false;
        }
        if (!(resource instanceof IContainer)) {
            return false;
        }
        IContainer container = (IContainer)resource;
        // if container contains a workflow file but not its parent
        IContainer parent = container.getParent();
        if (parent != null) {
            if (!parent.exists(WORKFLOW_FILE)) {
                return false;
            }
        }
        return container.exists(WORKFLOW_FILE);
    }
    
    /**
     * 
     * @param resource resource to test
     * @return true if the resource is a a workflow group 
     */
    public static boolean isWorkflowGroup(final IResource resource) {
        if (resource == null) {
            return false;
        }
        if (resource instanceof IFile) {
            return false;
        }
        if (!(resource instanceof IContainer)) {
            return false;
        }
        IContainer container = (IContainer)resource;
        // contains no workflow file but a meta info file 
        return !container.exists(WORKFLOW_FILE) 
            && container.exists(META_INFO_FILE);
    }
        
    public static boolean existsWorkflowGroupInWorkspace() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
            .getProjects();
        for (IProject p : projects) {
            if (KnimeResourceUtil.isWorkflowGroup(p)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tries to reveal the newly created resource in the 
     *  {@link KnimeResourceNavigator}.
     *  
     * @param resource resource to reveal 
     */
    public static void revealInNavigator(final IResource resource) {
        if (resource == null) {
            return;
        }
        IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
            .getActivePage().findView(KnimeResourceNavigator.ID);
        if (view instanceof KnimeResourceNavigator) {
            KnimeResourceNavigator navigator = (KnimeResourceNavigator)view;
            navigator.getViewer().reveal(resource);            
        }
    }
    
}
