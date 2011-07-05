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
 * Created: May 13, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.nature.KNIMEProjectNature;
import org.knime.workbench.ui.nature.KNIMEWorkflowSetProjectNature;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class RenameAction extends SelectionListenerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RenameAction.class);

    /**
     * The id for this action.
     */
    public static final String ID = "Rename...";

    private final TreeViewer m_viewer;

    /**
     *
     * @param viewer the viewer.
     */
    public RenameAction(final TreeViewer viewer) {
        super("Rename...");
        m_viewer = viewer;
        setId(ID);
        setToolTipText("Renames a KNIME workflow or workflow group.");
    }

    private boolean isEnabledPrivate(final IStructuredSelection selection) {
        if (selection.size() != 1) {
            return false;
        }
        if (!(selection.getFirstElement() instanceof IContainer)) {
            return false;
        }
        return true;
    }

    /**
     *
     */
    @Override
    public void run() {

        IStructuredSelection selection =
            (IStructuredSelection)m_viewer.getSelection();

        if (!isEnabledPrivate(selection)) {
            LOGGER.debug("This action should have been is disabled.");
            return;
        }

        if (selection.size() != 1) {
            return;
        }
        if (!(selection.getFirstElement() instanceof IContainer)) {
            return;
        }
        IContainer cont = (IContainer)selection.getFirstElement();
        boolean isWfGroup = false;
        if (KnimeResourceUtil.isWorkflow(cont)) {
            isWfGroup = false;
        } else if (KnimeResourceUtil.isWorkflowGroup(cont)) {
            isWfGroup = true;
        } else {
            // can't rename anything else
            return;
        }

        // check if the source is/contains opened workflow(s)
        List<IContainer> containedWFs =
                KnimeResourceUtil.getContainedWorkflows(Collections
                        .singletonList(cont));
        LinkedList<IContainer> openWFs = new LinkedList<IContainer>();
        for (IContainer wf : containedWFs) {
            if (KnimeResourceUtil.isOpenedWorkflow(wf)) {
                openWFs.add(wf);
            }
        }
        if (openWFs.size() > 0) {
            String flowNames = "";
            if (openWFs.size() > 0) {
                flowNames += openWFs.get(0).getName();
            }
            if (openWFs.size() > 1) {
                flowNames += ", " + openWFs.get(1).getName();
            }
            if (openWFs.size() > 2) {
                flowNames += ", ...";
            }
            showWorkflowIsOpenMessage(isWfGroup, flowNames);
            return;
        }

        // get new name through input dialog
        String title =
                (isWfGroup ? "Rename Workflow Group" : "Rename Workflow");
        InputDialog input =
                new InputDialog(m_viewer.getControl().getShell(), title,
                        "Enter new name:", cont.getName(), null);
        input.setBlockOnOpen(true);
        int ok = input.open();
        if (ok != Window.OK) {
            return;
        }
        String newName = input.getValue().trim();
        if (newName.isEmpty()) {
            // empty name is as good as cancel
            return;
        }
        if (newName.indexOf('\\') >= 0 || newName.indexOf('/') >= 0) {
            showUnsupportedCharacter(newName, isWfGroup);
            return;
        }

        // check if new name already exists
        IContainer parent = cont.getParent();
        IResource newRes = parent.findMember(newName);
        if (newRes != null) {
            boolean destIsFlow = KnimeResourceUtil.isWorkflow(newRes);
            showAlreadyExists(newName, destIsFlow);
            return;
        }

        // try to lock all flows before renaming it
        List<IContainer> lockedWFs = new LinkedList<IContainer>();
        if (!lockWorkflows(containedWFs, lockedWFs)) {
            unlockWorkflows(lockedWFs);
            showWorkflowInUseMessage();
            return;
        }
        File srcFile = new File(cont.getLocationURI());
        File destFile = new File(new File(parent.getLocationURI()), newName);
        // now release locks and rename
        unlockWorkflows(lockedWFs);

        if (srcFile.renameTo(destFile)) {
            // rename succeeded: refresh, delete obsolete resource
            try {
                if (cont instanceof IProject) {
                    // the .project file must be adapted
                    String natureId = KNIMEProjectNature.ID;
                    if (isWfGroup) {
                        natureId = KNIMEWorkflowSetProjectNature.ID;
                    }
                    IProject newProj =
                            MetaInfoFile.createKnimeProject(newName, natureId);
                    newProj.refreshLocal(IResource.DEPTH_INFINITE, null);
                } else {
                    // else refresh parent (no resource for dest available yet)
                    parent.refreshLocal(IResource.DEPTH_INFINITE, null);
                }
                // remove the old stuff (as resource)
                cont.delete(true, null);
            } catch (Exception e) {
                LOGGER.error("Error while renaming " + cont.getName() + " to "
                        + newName, e);
            }
        } else {
            showRenameFailed(isWfGroup);
        }

    }

    private boolean lockWorkflows(final List<IContainer> toBeLockedWFs,
            final List<IContainer> lockedWF) {
        boolean result = true;
        assert lockedWF.size() == 0;
        // open workflows can be locked multiple times.
        for (IContainer wf : toBeLockedWFs) {
            assert KnimeResourceUtil.isWorkflow(wf);
            if (VMFileLocker.lockForVM(new File(wf.getLocationURI()))) {
                lockedWF.add(wf);
            } else {
                result = false;
            }
        }
        return result;
    }

    private void unlockWorkflows(final List<IContainer> workflows) {
        for (IContainer wf : workflows) {
            assert KnimeResourceUtil.isWorkflow(wf);
            VMFileLocker.unlockForVM(new File(wf.getLocationURI()));
        }
    }

    private void showUnsupportedCharacter(final String newInvalidName,
            final boolean isGroup) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Illegal Characters in Name",
                        "The entered name (" + newInvalidName
                                + ") contains unsupported characters"
                                + " ( / and \\ )."
                                + "\nIf you wish to move the workflow "
                                + (isGroup ? "group " : "")
                                + "to a new location, "
                                + "please use the Move command.");
            }
        });
    }

    private void showWorkflowIsOpenMessage(final boolean isGroup,
            final String flowNames) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                String msg = "";
                if (isGroup) {
                    msg =
                            "Cannot rename workflow groups containing open "
                                    + "workflows.\nPlease save and close the "
                                    + "corresponding workflow editor ("
                                    + flowNames + ").";
                } else {
                    msg =
                            "Cannot rename open workflows.\nPlease save and "
                                    + "close the corresponding workflow "
                                    + "editor (" + flowNames + ").";
                }

                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Open Workflow", msg);
            }
        });
    }

    private void showWorkflowInUseMessage() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Locked Workflow",
                        "The selected workflow is locked by another "
                                + "user/instance and can't be renamed.");
            }
        });
    }

    private void showAlreadyExists(final String newName, final boolean isFlow) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Destination Already Exists",
                        "Cannot rename workflow " + (isFlow ? "" : "group ")
                                + "to \"" + newName
                                + "\". An item with the same "
                                + "name already exists.");
            }
        });
    }

    private void showRenameFailed(final boolean isGroup) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(Display.getDefault()
                        .getActiveShell(), "Rename Failed",
                        "KNIME was unable to rename the workflow"
                                + (isGroup ? " group" : "") + ".");
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean updateSelection(final IStructuredSelection selection) {
        super.updateSelection(selection);
        boolean enable = isEnabledPrivate(selection);
        setEnabled(enable); // fires prop change events
        return enable;
    }

}
