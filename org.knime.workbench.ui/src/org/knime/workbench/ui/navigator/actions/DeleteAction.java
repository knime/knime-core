/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: May 16, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;

/**
 *
 * @author ohl, University of Konstanz
 */
public class DeleteAction extends Action {

    /** ID of this action. */
    public static final String ID = "org.knime.deleteAction";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DeleteAction.class);

    private final Shell m_shell;

    private final TreeViewer m_viewer;

    public DeleteAction(final Shell shell, final TreeViewer viewer) {
        super("Delete...");
        setId(ID);
        m_shell = shell;
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {

        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();
        List<IContainer> toDel = getTopLevelResources(sel);
        if (toDel.size() <= 0) {
            LOGGER.debug("No workflow or workflow "
                    + "group selected for deletion.");
            return;
        }
        List<IContainer> toDelWorkflows =
                KnimeResourceUtil.getContainedWorkflows(toDel);
        LinkedList<IContainer> lockedWFs = new LinkedList<IContainer>();
        if (toDelWorkflows.size() > 0) {
            LinkedList<IContainer> unlockableWFs = new LinkedList<IContainer>();
            lockWorkflows(toDelWorkflows, unlockableWFs, lockedWFs);
            if (unlockableWFs.size() > 0) {
                // release locks acquired for deletion
                unlockWorkflows(lockedWFs);
                showCantDeleteMessage();
                return;
            }
        }
        assert lockedWFs.size() == toDelWorkflows.size();
        if (!confirmDeletion(toDel, toDelWorkflows)) {
            // release locks acquired for deletion
            unlockWorkflows(lockedWFs);
            return;
        }

        closeOpenWorkflows(toDelWorkflows);

        // delete Workflows first (must be unlocked first)
        deleteWorkflows(toDelWorkflows);
        deleteWorkflowGroups(toDel);

    }

    private void closeOpenWorkflows(final List<IContainer> allWorkflows) {
        IWorkbenchPage page =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
        for (IContainer wf : allWorkflows) {
            NodeContainer wfm =
                    ProjectWorkflowMap.getWorkflow(wf.getLocationURI());
            if (wfm != null) {
                for (IEditorReference editRef : page.getEditorReferences()) {
                    IEditorPart editor = editRef.getEditor(false);
                    if (editor == null) {
                        // got closed in the mean time
                        continue;
                    }
                    WorkflowEditorAdapter wea =
                            (WorkflowEditorAdapter)editor
                                    .getAdapter(WorkflowEditorAdapter.class);
                    NodeContainer editWFM = null;
                    if (wea != null) {
                        editWFM = wea.getWorkflowManager();
                    }
                    if (wfm == editWFM) {
                        page.closeEditor(editor, false);
                    }
                }

            }
        }
    }

    private void deleteWorkflows(final List<IContainer> toDelWFs) {
        for (IContainer wf : toDelWFs) {
            assert VMFileLocker.isLockedForVM(new File(wf.getLocationURI()));
            assert KnimeResourceUtil.isWorkflow(wf);
            try {
                // delete the workflow file first
                IResource wfFile =
                        wf.findMember(WorkflowPersistor.WORKFLOW_FILE);
                wfFile.delete(true, null);

                for (IResource child : wf.members()) {
                    if (VMFileLocker.LOCK_FILE.equals(child.getName())) {
                        // delete the lock file last
                        continue;
                    }
                    child.delete(true, null);
                }

                // release lock in order to delete lock file
                VMFileLocker.unlockForVM(new File(wf.getLocationURI()));
                // lock file resource may not exist
                File lockFile =
                        new File(new File(wf.getLocationURI()),
                                VMFileLocker.LOCK_FILE);
                lockFile.delete();
                // delete the workflow directory itself
                wf.delete(true, null);
            } catch (CoreException e) {
                LOGGER.error(
                        "Error while deleting workflow " + wf.getFullPath()
                                + ": " + e.getMessage(), e);
                // continue with next workflow...
            }
        }
    }

    private void deleteWorkflowGroups(final List<IContainer> toDelGrpsAndWFs) {
        for (IContainer toDel : toDelGrpsAndWFs) {
            // workflows might be gone already
            if (toDel.exists() && KnimeResourceUtil.isWorkflowGroup(toDel)) {
                try {
                    toDel.delete(true, null);
                } catch (CoreException e) {
                    LOGGER.error(
                            "Error while deleting workflow group "
                                    + toDel.getFullPath() + ": "
                                    + e.getMessage(), e);
                    // continue with next workflow group...
                }
            }
        }
    }

    private List<IContainer> getTopLevelResources(
            final IStructuredSelection selection) {

        LinkedList<IContainer> toDelete = new LinkedList<IContainer>();

        Iterator selIter = selection.iterator();
        while (selIter.hasNext()) {
            Object o = selIter.next();
            if (o instanceof IContainer) {
                IContainer c = (IContainer)o;
                // we only delete workflows and workspaces
                if (!KnimeResourceUtil.isWorkflow(c)
                        && !KnimeResourceUtil.isWorkflowGroup(c)) {
                    continue;
                }
                String p = c.getFullPath().toString();
                boolean isChild = false;
                // remove all elements contained in the new resource
                Iterator<IContainer> listIter = toDelete.iterator();
                while (listIter.hasNext()) {
                    IContainer listCont = listIter.next();
                    String listP = listCont.getFullPath().toString();
                    if (p.startsWith(listP)) {
                        // sub element of an already added element: ignore
                        isChild = true;
                        break;
                    }
                    if (listP.startsWith(p)) {
                        // list element is sub element of new element: delete it
                        listIter.remove();
                    }
                }
                if (!isChild) {
                    // only add resources not a child of already added ones
                    toDelete.add(c);
                }
            } else {
                LOGGER.debug("Ignoring selection in delete action: " + o + " ("
                        + o.getClass().getSimpleName() + ")");
            }
        }

        return toDelete;
    }

    private boolean confirmDeletion(final List<IContainer> toDel,
            final List<IContainer> toDelWFs) {

        String msg = "";
        if (toDel.size() == 1) {
            boolean isGroup = toDelWFs.size() != 1;
            msg = "Do you want to delete the workflow ";
            if (isGroup) {
                msg += "group ";
            }
            msg += "\"" + toDel.get(0).getName() + "\"?\n";
        } else {
            int groups = toDel.size() - toDelWFs.size();
            if (groups == 0) {
                msg =
                        "Do you want to delete all " + toDel.size()
                                + " selected workflows?\n";
            } else {
                if (groups == 1) {
                    msg =
                            "Do you want to delete the workflow group \""
                                    + toDel.get(0).getName() + "\"?\n";
                } else {
                    msg =
                            "Do you want to delete all " + toDel.size()
                                    + " workflow groups?\n";
                }
                if (toDelWFs.size() > 0) {
                    if (groups == 1) {
                        msg += "It contains ";
                    } else {
                        msg += "They contain ";
                    }
                    msg += toDelWFs.size() + " workflow";
                    if (toDelWFs.size() > 1) {
                        msg += "s";
                    }
                    msg += ".\n";
                }
            }
        }
        msg += "\nThis operation cannot be undone.";

        MessageBox mb =
                new MessageBox(m_shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
        mb.setMessage(msg);
        mb.setText("Confirm Deletion");
        if (mb.open() != SWT.OK) {
            LOGGER.debug("Deletion of " + toDel.size()
                    + " items canceled by user.");
            return false;
        } else {
            LOGGER.debug("Deletion of " + toDel.size()
                    + " items confirmed by user.");
            return true;
        }

    }

    private void lockWorkflows(final List<IContainer> toDelWorkflows,
            final List<IContainer> unlockableWF, final List<IContainer> lockedWF) {
        assert unlockableWF.size() == 0; // the result lists should be empty
        assert lockedWF.size() == 0;
        // open workflows can be locked multiple times.
        for (IContainer wf : toDelWorkflows) {
            assert KnimeResourceUtil.isWorkflow(wf);
            if (VMFileLocker.lockForVM(new File(wf.getLocationURI()))) {
                lockedWF.add(wf);
            } else {
                unlockableWF.add(wf);
            }
        }
    }

    private void showCantDeleteMessage() {
        MessageBox mb = new MessageBox(m_shell, SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Deletion");
        mb.setMessage("At least one of the workflows " + "can't be deleted.\n"
                + " It is probably in use by another user/instance.\n"
                + "Cancel deletion.");
        mb.open();
    }

    private void unlockWorkflows(final List<IContainer> lockedWFs) {
        for (IContainer lwf : lockedWFs) {
            File wfFile = new File(lwf.getLocationURI());
            assert VMFileLocker.isLockedForVM(wfFile);
            assert KnimeResourceUtil.isWorkflow(lwf);
            VMFileLocker.unlockForVM(wfFile);
        }
    }
}
