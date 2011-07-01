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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.part.ResourceTransfer;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 *
 * @author ohl, University of Konstanz
 */
public class CopyToClipboard extends SelectionListenerAction {

    /** The id of this action. */
    public static final String ID = "org.knime.workbench.ui.CopyToClipboard";

    private final Clipboard m_clipboard;

    private final TreeViewer m_viewer;

    private PasteAction m_pasteAction;

    public CopyToClipboard(final TreeViewer viewer, final Clipboard clipboard) {
        super("&Copy");
        m_clipboard = clipboard;
        m_viewer = viewer;
        m_pasteAction = null;
        setId(ID);
        setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
    }

    /**
     * If a paste action is set, it is notified of a new clipboard content. As
     * the paste action's enable state is only updated with selection changes
     * (and the selection doesn't change when content is copied to the
     * clipboard) the paste action must be notified explicitly.
     *
     * @param pasteAction
     */
    public void setPasteAction(final PasteAction pasteAction) {
        m_pasteAction = pasteAction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {

        // set the source from the tree selection
        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();

        if (!isEnabledPrivate(sel)) {
            NodeLogger.getLogger(CopyToClipboard.class).debug(
                    "This action is should have been disabled.");
            return;
        }

        // Add resources, filepaths and filenames to clipboard
        ArrayList<IResource> res = new ArrayList<IResource>();
        ArrayList<String> paths = new ArrayList<String>();
        StringBuilder names = new StringBuilder();

        Iterator iter = sel.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof IResource)) {
                continue;
            }
            IResource r = (IResource)o;
            IPath locPath = r.getLocation();
            if (locPath == null) {
                continue;
            }
            String fName = r.getName();
            res.add(r);
            paths.add(fName);
            if (names.length() > 0) {
                names.append('\n');
            }
            names.append(fName);
        }

        if (res.isEmpty()) {
            return;
        }

        IResource[] resArray = res.toArray(new IResource[res.size()]);
        String[] pathArray = paths.toArray(new String[paths.size()]);
        setContents(new Object[]{resArray, pathArray, names.toString()},
                new Transfer[]{ResourceTransfer.getInstance(),
                FileTransfer.getInstance(), TextTransfer.getInstance()});
        if (m_pasteAction != null) {
            m_pasteAction.updateSelection(sel);
        }
    }

    private void setContents(final Object[] objs, final Transfer[] trans) {
        try {
            m_clipboard.setContents(objs, trans);
        } catch (SWTError e) {
            if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
                throw e;
            }
            if (MessageDialog.openQuestion(m_viewer.getControl().getShell(),
                    "Copy To Clipboard Error",
                    "Unable to copy selection to clipboard. "
                            + "Do you want to try again?")) {
                setContents(objs, trans);
            }
        }

    }

    private boolean isEnabledPrivate(final IStructuredSelection sel) {
        if (m_viewer != null) {
            LinkedList<IResource> selWFs = new LinkedList<IResource>();
            // selection should at least contain one flow or group
            Iterator iter = sel.iterator();
            while (iter.hasNext()) {
                Object s = iter.next();
                if (!(s instanceof IResource)) {
                    continue;
                }
                IResource r = (IResource)s;
                if (KnimeResourceUtil.isWorkflow(r)
                        || KnimeResourceUtil.isWorkflowGroup(r)) {
                    selWFs.add(r);
                }
            }
            if (selWFs.size() <= 0) {
                return false;
            }
            // selection must not contain children of selected items
            for (int i = 0; i < selWFs.size(); i++) {
                for (int j = i + 1; j < selWFs.size(); j++) {
                    IPath ip = selWFs.get(i).getFullPath();
                    IPath jp = selWFs.get(j).getFullPath();
                    if (ip.isPrefixOf(jp) || jp.isPrefixOf(ip)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
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
