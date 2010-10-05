/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 */
package org.knime.workbench.ui.navigator;

import java.io.File;
import java.net.URI;
import java.util.Comparator;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ResourceTransfer;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.navigator.actions.MoveWorkflowAction;

/**
 *
 * @author Fabian Dill, KNIME.com GmbH
 * @author Peter Ohl, KNIME.com GmbH
 */
public class WorkflowMoveDropListener extends ViewerDropAdapter {

    /**
     * @param viewer
     */
    protected WorkflowMoveDropListener(final Viewer viewer) {
        super(viewer);
        // disable the insertion feature. We don't sort workflows.
        setFeedbackEnabled(false);
    }

    private static final Path WORKFLOW_FILE =
            new Path(WorkflowPersistor.WORKFLOW_FILE);

    private boolean isNode(final IResource source) {
        if (source instanceof IFolder) {
            IFolder dir = (IFolder)source;
            return dir.getParent().exists(WORKFLOW_FILE);
        }
        return false;
    }

    private boolean isWorkflow(final IResource r) {
        if (r instanceof IContainer) {
            return (((IContainer)r).findMember(WorkflowPersistor.WORKFLOW_FILE) != null);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data) {
        Object t = getCurrentTarget();
        IPath targetPath = null;
        if ((t instanceof IResource) && !(t instanceof IWorkspaceRoot)) {
            IResource r = (IResource)t;
            if (!(r instanceof IFolder || r instanceof IProject)) {
                return false;
            } else {
                // the target must not be a node
                if (isNode(r)) {
                    return false;
                }
            }
            targetPath = r.getFullPath();
        } else if (t == null) {
            // no drop target selected: drop it in the root.
            targetPath = new Path("/");
        }

        if (data instanceof IResource[]) {
            // move should only allow drops on groups
            if (isWorkflow(ResourcesPlugin.getWorkspace().getRoot().findMember(
                    targetPath))) {
                return false;
            }
            // thats a move of workflows/groups inside the navigator
            // (ResourceTransfer)
            // elements to move are in a sorted map to move short paths first
            TreeSet<IPath> moves = new TreeSet<IPath>(new Comparator<IPath>() {
                public int compare(final IPath o1, final IPath o2) {
                    if (o1.segmentCount() < o2.segmentCount()) {
                        return -1;
                    }
                    if (o2.segmentCount() > o1.segmentCount()) {
                        return 1;
                    }
                    // returning 0 prevents items from being added!!!!
                    return o1.equals(o2) ? 0 : 1;
                };
            });
            IResource[] selections = (IResource[])data;
            boolean showOpenFlowError = false;
            for (IResource source : selections) {
                // move only workflows and groups - ignore the rest.
                if (source instanceof IFolder || source instanceof IProject) {
                    if (KnimeResourceUtil.isOpenedWorkflow(source)) {
                        // can't move currently open flows - fail
                        showOpenFlowError = true;
                        break;
                    }
                    if (isNode(source)) {
                        // ignore selected nodes
                        continue;
                    }
                    moves.add(source.getFullPath());
                }
            }
            if (showOpenFlowError) {
                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Open Workflow(s)",
                        "Workflows curently opened in an editor can't "
                                + "be moved\nPlease save and close the open "
                                + "workflow editor(s) and try again.");
                return false;
            }
            if (selections.length > 0 && moves.size() == 0) {
                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Move Error",
                        "Only workflows and workflow groups can be moved.");
                return false;
            }

            for (IPath src : moves) {
                // not existing paths (already moved ones) are ignored
                new MoveWorkflowAction(src, targetPath).run();
            }
            getViewer().setSelection(
                    new StructuredSelection(moves.iterator().next()), true);
        }

        if (data instanceof URI[]) {
            // thats an import of a remote workflow in a zip archive file
            // (RemoteFileTransfer)
            URI[] uriData = (URI[])data;
            // we should accept drops on workflow groups only
            IResource r =
                    ResourcesPlugin.getWorkspace().getRoot().findMember(
                            targetPath);
            assert r != null;
            assert (r instanceof IWorkspaceRoot)
                    || KnimeResourceUtil.isWorkflowGroup(r)
                    || KnimeResourceUtil.isWorkflow(r);
            IPath newWF;
            String wfSimplename = new Path(uriData[0].getPath()).lastSegment();
            if (KnimeResourceUtil.isWorkflow(r)) {
                // if dropped on a workflow we store it in the parent group
                newWF = targetPath.removeLastSegments(1).append(wfSimplename);
            } else {
                // add the workflow name to the target path
                newWF = targetPath.append(wfSimplename);
            }
            IResource newWFres =
                    ResourcesPlugin.getWorkspace().getRoot().findMember(newWF);
            if (newWFres != null) {
                // ask if we are about to overwrite
                String msg;
                String[] labels;
                int defaultIdx;
                int dlgType;
                assert KnimeResourceUtil.isWorkflow(newWFres);
                IPath renamedFlow = createNewName(newWF);
                if (!KnimeResourceUtil.isDirtyWorkflow(newWFres)) {
                    labels = new String[]{"Overwrite", "New Name", "Cancel"};
                    defaultIdx = 0;
                    msg =
                            "The target workflow exists\n\t'"
                                    + newWF.toString()
                                    + "'.\n\nPlease select:\n"
                                    + "\n- \"Overwrite\" to replace the "
                                    + "existing with the downloaded flow\n"
                                    + "\n- \"New Name\" to save downloaded "
                                    + "flow with a new name '"
                                    + renamedFlow.lastSegment()
                                    + "'\n"
                                    + "\n- \"Cancel\" to abort the download of "
                                    + "the flow";
                    dlgType = MessageDialog.QUESTION;
                } else {
                    labels = new String[]{"Discard and Overwrite", "New Name", "Cancel"};
                    defaultIdx = 1;
                    msg =
                            "The target workflow exists\n\t'"
                                    + newWF.toString()
                                    + "'\n and is modified in an editor!\n\n"
                                    + "Please select:\n"
                                    + "\n- \"Discard and Overwrite\" to discard"
                                    + " your changes and replace the existing"
                                    + " flow with the downloaded one\n"
                                    + "\n- \"New Name\" to save downloaded "
                                    + "flow with a new name '"
                                    + renamedFlow.lastSegment()
                                    + "'\n"
                                    + "\n- \"Cancel\" to abort the download of "
                                    + "the flow";
                    dlgType = MessageDialog.WARNING;
                }
                MessageDialog md =
                        new MessageDialog(
                                Display.getDefault().getActiveShell(),
                                "Confirm Overwrite", null, msg, dlgType,
                                labels, defaultIdx);
                int result = md.open();
                if (result != 0 && result != 1) {
                    // do not overwrite and do not save with new name: Cancel.
                    return true;
                }
                if (result == 1) {
                    // save with new name
                    newWF = renamedFlow;
                    newWFres =
                            ResourcesPlugin.getWorkspace().getRoot()
                                    .findMember(newWF);
                }
            }
            // let the target download the flow now
            String[] fileNames =
                    RemoteFileTransfer.getInstance().requestFileContent(
                            (URI[])data);
            if (fileNames.length != 1) {
                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Drop Error",
                        "Only one remote workflow can be dropped at a time.");
                return false;
            }
            IResource wf =
                    KnimeResourceUtil.importZipFileAsWorkflow(Display
                            .getDefault().getActiveShell(), newWF, new File(
                            fileNames[0]));
            KnimeResourceUtil.revealInNavigator(wf, true);
            return true;
        }

        // We support RemoteFileTransfer and ResourceTransfer only
        return false;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final Object target, final int operation,
            final TransferData transferType) {
        if (target == null) {
            // drop on the root
            return true;
        }
        if (!(target instanceof IResource) || target instanceof IWorkspaceRoot) {
            return false;
        }
        IResource r = (IResource)target;
        if (!(r instanceof IFolder || r instanceof IProject)) {
            return false;
        }
        IContainer c = (IContainer)r;
        // the target must not be a node
        if (isNode(r)) {
            return false;
        }

        if (ResourceTransfer.getInstance().isSupportedType(transferType)) {
            // Resources are dragged when moving flows or groups
            // they can only be dropped on groups
            return !isWorkflow(c);
        }
        if (RemoteFileTransfer.getInstance().isSupportedType(transferType)) {
            // remote flows can be dropped on flows and groups
            return true;
        }

        return false;
    }

    /**
     * If a path of an existing resource is passed, it adds a suffix to the last
     * segment to make the path not existing. The root is not made unique.
     *
     * @param name
     * @return a not existing path possibly with a suffix like &quot;_(2)&quot;
     */
    private IPath createNewName(final IPath name) {
        if (name == null || name.isEmpty() || name.isRoot()) {
            return name;
        }
        String simpleName = name.lastSegment();
        IPath p = name;
        int suffix = 1;
        while (ResourcesPlugin.getWorkspace().getRoot().findMember(p) != null) {
            String newSimpleName = simpleName + "_(" + (++suffix) + ")";
            p = name.removeLastSegments(1).append(newSimpleName);
        }
        return p;
    }
}
