/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
