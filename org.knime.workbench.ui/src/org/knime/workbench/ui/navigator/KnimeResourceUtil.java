/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
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

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(KnimeResourceUtil.class);

    /**
     * Path to the meta info file to be used in {@link IContainer#exists(IPath)}
     * .
     */
    public static final IPath META_INFO_FILE =
            new Path(WorkflowPersistor.METAINFO_FILE);

    /**
     * Path to the workflow file to be used in {@link IContainer#exists(IPath)}.
     */
    public static final IPath WORKFLOW_FILE =
            new Path(WorkflowPersistor.WORKFLOW_FILE);

    // This is a not published part of our API - internal use only
    private static final Path NODE_FILE =
            new Path(SingleNodeContainerPersistorVersion200.SETTINGS_FILE_NAME);

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
     * @param r
     * @return true, if the specified resource is a workflow currently opened in
     *         an editor
     */
    public static boolean isOpenedWorkflow(final IResource r) {
        NodeContainer nc = ProjectWorkflowMap.getWorkflow(
                r.getLocationURI());
        if (nc != null) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param r
     * @return true, if the specified resource is a workflow currently opened in
     *         an editor and is dirty
     */
    public static boolean isDirtyWorkflow(final IResource r) {
        NodeContainer nc = ProjectWorkflowMap.getWorkflow(r.getLocationURI());
        if (nc != null) {
            return nc.isDirty();
        }
        return false;
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
        IProject[] projects =
                ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject p : projects) {
            if (KnimeResourceUtil.isWorkflowGroup(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list (never null) with all workflow directories contained in
     * the specified list or in any sub dir (workflow group) contained in the
     * list.
     *
     * @param resources the resources so search for workflows
     * @return a list (never null) with all workflow directories contained in
     *         the specified list or in any sub dir (workflow group) contained
     *         in the list. If a workflow is contained in the parameter list, it
     *         is added to the result.
     */
    public static List<IContainer> getContainedWorkflows(
            final List<? extends IResource> resources) {
        List<IContainer> result = new LinkedList<IContainer>();
        for (IResource c : resources) {
            if (KnimeResourceUtil.isWorkflow(c)) {
                result.add((IContainer)c);
            } else if (KnimeResourceUtil.isWorkflowGroup(c)) {
                try {
                    result.addAll(getContainedWorkflows(Arrays
                            .asList(((IContainer)c).members())));
                } catch (CoreException e) {
                    // ignore - no workflows contained.
                }
            } // else ignore
        }
        return result;
    }

    /**
     * Tries to reveal the newly created resource in the
     * {@link KnimeResourceNavigator}.
     *
     * @param resource resource to reveal
     */
    public static void revealInNavigator(final IResource resource) {
        revealInNavigator(resource, false);
    }

    /**
     * Tries to select and reveal the passed resource in the
     * {@link KnimeResourceNavigator}.
     *
     * @param resource resource to select and reveal
     */
    public static void revealInNavigator(final IResource resource,
            final boolean select) {

        if (resource == null) {
            return;
        }
        IViewPart view =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().findView(KnimeResourceNavigator.ID);
        if (view instanceof KnimeResourceNavigator) {
            KnimeResourceNavigator navigator = (KnimeResourceNavigator)view;
            if (select) {
                navigator.getViewer().setSelection(
                        new StructuredSelection(resource), true);
            } else {
                navigator.getViewer().reveal(resource);
            }
        }
    }

    /**
     *
     * @param parentShell the parent shell for message boxes etc.
     * @param destination the name of the new workflow. If this is an existing
     *            path, it must denote a workflow - which will be overwritten.
     * @param zipFile the archive containing one dir with the workflow. It is
     *            deleted after successful import (only).
     * @return the resource representing the newly imported workflow or null if
     *         things failed
     */
    public static IResource importZipFileAsWorkflow(final Shell parentShell,
            final IPath destination, final File zipFile) {
        IResource destResource =
                ResourcesPlugin.getWorkspace().getRoot()
                        .findMember(destination);
        if (destResource != null) {
            // if the destination exists, it must be a workflow
            if (!isWorkflow(destResource)) {
                MessageDialog.openError(parentShell, "Import Error",
                        "Can only overwrite workflows. (The destination to "
                                + "store the workflow exists but is not "
                                + "a workflow.)");
                MessageDialog.openInformation(parentShell, "Download Info",
                        "Import failed. The downloaded workflow is stored in "
                                + " the file " + zipFile.getAbsolutePath()
                                + " and will NOT be deleted!\n"
                                + "You can import it manually.\n"
                                + "Please delete it, if not needed anymore.");
                return null;
            }
            LOGGER.debug("Deleting existing workflow for "
                    + "import overwrite: " + destination.toString());
            // delete existing
            if (!deleteResource(destResource)) {
                MessageDialog.openError(parentShell, "Deletion Error",
                        "Unable to delete selected workflow "
                                + "(for overwrite).");
                MessageDialog.openInformation(parentShell, "Import Error",
                        "Import failed. The downloaded workflow is stored in "
                                + "the file " + zipFile.getAbsolutePath()
                                + " and will not be deleted.\n"
                                + "You can import it manually.\n"
                                + "Please delete it, if not needed anymore.");
                return null;
            }
            // create new (same) location
            if (!createContainer(destination)) {
                MessageDialog.openError(parentShell, "Creation Error",
                        "Successfully deleted selected workflow (for overwrite)"
                                + "- but unable to create new location again!");
                MessageDialog.openInformation(parentShell, "Import Error",
                        "Import Failed. The downloaded workflow in stored in "
                                + "the file " + zipFile.getAbsolutePath()
                                + " and will not be deleted.\n"
                                + "You can import it manually.\n"
                                + "Please delete it, if not needed anymore");
                return null;
            }
        }

        try {
            importWorkflowIntoWorkspace(destination, zipFile);
            zipFile.delete();
        } catch (Exception e) {
            String msg = "<no details>";
            if (e.getMessage() != null) {
                msg = e.getMessage();
            }
            LOGGER.debug("Exception during import of workflow from zip file."
                    + " Destination: " + destination.toString(), e);
            MessageDialog.openError(parentShell, "Import Error",
                    "Error during import of workflow from zip file.\n" + msg);
            MessageDialog.openInformation(parentShell, "Import Error",
                    "Import Failed. The downloaded workflow in stored in "
                            + "the file " + zipFile.getAbsolutePath()
                            + " and will not be deleted.\n"
                            + "You can import it manually.\n"
                            + "Please delete it, if not needed anymore");
        }
        return ResourcesPlugin.getWorkspace().getRoot().findMember(destination);

    }

    /**
     * Stores the flow in the archive in the local workspace.
     *
     * @param destination the name of the new workflow. Must denote a flow (if
     *            it exists it is overwritten).
     * @param zippedWorkflow
     * @throws Exception
     */
    private static void importWorkflowIntoWorkspace(final IPath destination,
            final File zippedWorkflow) throws Exception {

        ZipFile zFile = new ZipFile(zippedWorkflow);
        ZipLeveledStructProvider importStructureProvider =
                new ZipLeveledStructProvider(zFile);
        importStructureProvider.setStrip(1);

        ZipEntry root = (ZipEntry)importStructureProvider.getRoot();
        List<ZipEntry> rootChild = importStructureProvider.getChildren(root);
        if (rootChild.size() == 1) {
            // the zipped workflow normally contains only one dir
            root = rootChild.get(0);
        }
        LOGGER.debug("Importing workflow. Destination:"
                + destination.toString());
        try {
            final ImportOperation iOper =
                    new ImportOperation(destination, root,
                            importStructureProvider, new IOverwriteQuery() {
                                @Override
                                public String queryOverwrite(
                                        final String pathString) {
                                    return IOverwriteQuery.YES;
                                }
                            });
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
                    new IRunnableWithProgress() {

                        @Override
                        public void run(final IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            iOper.run(monitor);
                        }
                    });
        } finally {
            importStructureProvider.closeArchive();
        }
    }

    private static boolean deleteResource(final IResource res) {
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
                    new IRunnableWithProgress() {
                        @Override
                        public void run(final IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            try {
                                IContainer parent = res.getParent();
                                res.delete(IResource.FORCE
                                       | IResource.ALWAYS_DELETE_PROJECT_CONTENT
                                       | IResource.DEPTH_INFINITE, monitor);
                                parent.refreshLocal(IResource.DEPTH_ONE,
                                        monitor);
                            } catch (CoreException ce) {
                                // we check later
                            }
                        }
                    });
        } catch (InvocationTargetException e) {
            // we check later
        } catch (InterruptedException e) {
            // we check later
        }
        return (ResourcesPlugin.getWorkspace().getRoot().findMember(
                res.getFullPath()) == null);
    }

    /**
     * Creates a container with the specified path (resource must not exist!).
     * Return true if the container exists at the end, false if not. Creates
     * meta info and projects files as required by KNIME.
     *
     * @param dest
     * @return
     */
    private static boolean createContainer(final IPath dest) {

        int newLevel = 0;
        IPath p = dest.removeLastSegments(1);
        while (!ResourcesPlugin.getWorkspace().getRoot().exists(p)) {
            newLevel++;
            p = dest.removeLastSegments(newLevel + 1);
        }

        final ContainerGenerator cg = new ContainerGenerator(dest);
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
                    new IRunnableWithProgress() {
                        @Override
                        public void run(final IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            try {
                                cg.generateContainer(monitor);
                            } catch (CoreException ce) {
                                // we check later
                            }
                        }
                    });
        } catch (InvocationTargetException e) {
            // we check later
        } catch (InterruptedException e) {
            // we check later
        }

        while (newLevel > 0) {
            // we created a new directory, create metainfo files in all levels
            IResource newRes =
                    ResourcesPlugin.getWorkspace().getRoot().findMember(
                            dest.removeLastSegments(newLevel));
            URI rawLoc = newRes.getLocationURI();
            if (rawLoc != null) {
                File projLoc = new File(rawLoc);
                MetaInfoFile.createMetaInfoFile(projLoc,
                        dest.segmentCount() == 1);
            }
            newLevel--;
        }
        try {
            ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
                    IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException e) {
            // don't refresh then
        }
        return true;
    }

    /**
     * Returns the resource denoted by the specified URI - or null, if no such
     * resource exists within the workspace.
     *
     * @param location if inside the workspace and existing the corresponding
     *            resource is returned - otherwise null
     *
     * @return the resource denoted by the specified URI - or null, if no such
     *         resource exists within the workspace.
     *
     */
    public static IResource getResourceForURI(final URI location) {
        if (location == null) {
            return null;
        }
        IWorkspaceRoot rootPath = ResourcesPlugin.getWorkspace().getRoot();
        IContainer[] conts = rootPath.findContainersForLocationURI(location);
        if (conts.length >= 1) {
            return conts[0];
        }
        return null;
    }

}
