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
 * Created: Jun 15, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.navigator.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.part.ResourceTransfer;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 *
 * @author ohl, University of Konstanz
 */
public class PasteAction extends SelectionListenerAction  {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PasteAction.class);

    /** ID of this action. */
    public static final String ID = "org.knime.workbench.ui.PasteAction";

    private final Clipboard m_clipboard;

    private final TreeViewer m_viewer;

    private IResource[] m_sources;

    private LinkedList<IContainer> m_lockedFlows;

    private IContainer m_target;

    /**
     * @param viewer the corresponding viewer
     * @param clipboard a clipboard instance to past from
     *
     */
    public PasteAction(final TreeViewer viewer, final Clipboard clipboard) {
        super("&Paste");
        setId(ID);
        m_viewer = viewer;
        m_clipboard = clipboard;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (!isEnabledPrivate()) {
            LOGGER.error(
                    "This action is disabled. Even though it is "
                    + "available through the menu - it is doing nothing "
                    + "with the current selection. This is a know issue. "
                    + "Aka feature.");
            return;
        }

        Object res = m_clipboard.getContents(ResourceTransfer.getInstance());

        if (!(res instanceof IResource[])) {
            LOGGER.debug("Wrong object type in clipboard. Can't paste."
                    + "(Expected: IResource[], Actual: "
                    + res.getClass().getSimpleName() + ")");
            return;
        }
        m_sources = (IResource[])res;
        // can't do it if sources contain each other
        for (int i = 0; i < m_sources.length; i++) {
            if (!m_sources[i].exists()) {
                showSourceVanishedMessage();
                return;
            }
            if (!KnimeResourceUtil.isWorkflow(m_sources[i])
                    && !KnimeResourceUtil.isWorkflowGroup(m_sources[i])) {
                return;
            }
            for (int j = i + 1; j < m_sources.length; j++) {
                IPath ip = m_sources[i].getFullPath();
                IPath jp = m_sources[j].getFullPath();
                if (ip.isPrefixOf(jp) || jp.isPrefixOf(ip)) {
                    return;
                }
            }
        }
        m_target = null;
        // can only paste in one workflow group
        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();
        if (sel == null || sel.size() != 1) {
            return;
        }
        Object o = sel.getFirstElement();
        if (!(o instanceof IContainer)) {
            return;
        }
        m_target = (IContainer)o;
        if (!KnimeResourceUtil.isWorkflowGroup(m_target)) {
            return;
        }

        for (IResource s : m_sources) {
            if (m_target.findMember(s.getName()) != null) {
                showTargetExistsMessage();
                return;
            }
        }

        // lock the workflows to copy
        List<IContainer> wfs =
                KnimeResourceUtil.getContainedWorkflows(Arrays
                        .asList(m_sources));
        m_lockedFlows = new LinkedList<IContainer>();
        if (!lockWorkflows(wfs, m_lockedFlows)) {
            unlockWorkflows(m_lockedFlows);
            showWorkflowInUseMessage();
            return;
        }

        // copy
//        for (IResource src : m_sources) {
//            CopyAction copyAction =
//                    new CopyAction(src.getFullPath(), m_target.getFullPath());
//            copyAction.run();
//        }

        try {
            PlatformUI.getWorkbench().getProgressService()
                    .busyCursorWhile(new IRunnableWithProgress() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void run(final IProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException {
                            String msg;
                            if (m_lockedFlows.size() != 1) {
                                msg = "Copying " + m_lockedFlows.size() + " workflows";
                            } else {
                                msg = "Copying workflow " + m_lockedFlows.getFirst().getName();
                            }
                            // we can't specify work ticks - copy works recursive
                            monitor.beginTask(msg, IProgressMonitor.UNKNOWN);
                            for (IResource src : m_sources) {
                                CopyAction copyAction =
                                        new CopyAction(src.getFullPath(), m_target.getFullPath());
                                // as the target is set in CopyAction we can call this run method
                                copyAction.run(new SubProgressMonitor(monitor, 1));
                                if (monitor.isCanceled()) {
                                    MessageDialog.openInformation(m_viewer.getControl().getShell(),
                                            "Copy Canceled",
                                            "Copy operation canceled. Some workflows or "
                                                    + "workflow groups are already copied and "
                                                    + "will not be deleted.");
                                    monitor.done();
                                    return;
                                }
                            }
                            try {
                                m_target.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                            } catch (CoreException e) {
                                //ignore
                            }
                            monitor.done();
                        }


                    });
        } catch (InvocationTargetException e) {
            LOGGER.error("Error while pasting selection", e);
        } catch (InterruptedException e) {
            LOGGER.error("Error while pasting selection", e);
        }

        unlockWorkflows(m_lockedFlows);
        return;

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


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
    private boolean isEnabledPrivate() {
        Object c = m_clipboard.getContents(ResourceTransfer.getInstance());
        if (c == null || !(c instanceof IResource[])) {
            return false;
        }
        if (((IResource[])c).length == 0) {
            return false;
        }
        // can only paste in one workflow group
        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();
        if (sel == null || sel.size() != 1) {
            return false;
        }
        Object o = sel.getFirstElement();
        if (!(o instanceof IContainer)) {
            return false;
        }
        boolean result = KnimeResourceUtil.isWorkflowGroup((IContainer)o);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean updateSelection(final IStructuredSelection selection) {
        if (!super.updateSelection(selection)) {
            return false;
        }
        boolean result = isEnabledPrivate();
        setEnabled(result); // fires prop change events
        return result;
    }

    private void showTargetExistsMessage() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openWarning(
                        Display.getDefault().getActiveShell(),
                        "Resource already exists",
                        "One of the flows or groups to copy "
                                + " already exists in \""
                                + m_target.getFullPath()
                                + "\".\nPlease rename/remove before copying.");
            }
        });
    }

    private void showSourceVanishedMessage() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openWarning(
                        Display.getDefault().getActiveShell(),
                        "Resource doesn't exist",
                        "One of the flows or groups to copy "
                                + " doesn't exists (anymore).");
            }
        });
    }

    private void showWorkflowInUseMessage() {
        MessageDialog.openError(m_viewer.getControl().getShell(),
                "Can't Copy Workflows", "Some of the workflows pasted are in "
                        + "use by anoter user/instance and can't be copied");
    }

}
