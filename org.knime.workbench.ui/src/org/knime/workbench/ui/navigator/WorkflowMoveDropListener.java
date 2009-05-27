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
package org.knime.workbench.ui.navigator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.navigator.actions.MoveWorkflowAction;


/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class WorkflowMoveDropListener implements DropTargetListener {
    
    private final List<IAction> m_actions = new ArrayList<IAction>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(final DropTargetEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(final DropTargetEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOperationChanged(final DropTargetEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
        for (IAction a : m_actions) {
            a.run();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dropAccept(final DropTargetEvent event) {   
        m_actions.clear();
        
        TreeItem item = (TreeItem)event.item;
        IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(
                getPathFor(item));
        IPath targetPath = ResourcesPlugin.getWorkspace().getRoot()
            .getFullPath();
        if (r != null && !(r instanceof IWorkspaceRoot)) {
            // TODO: check this for workspace root
            if (!(r instanceof IFolder || r instanceof IProject)) {
                event.feedback = DND.DROP_NONE;
                return;
            } else  {
                IContainer c = (IContainer)r;
                if (c.findMember(WorkflowPersistor.WORKFLOW_FILE) != null) {
                    // its a KNIME workflow
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openInformation(
                                    Display.getDefault().getActiveShell(), 
                                    "Invalid Target Location", 
                                    "A KNIME workflow cannot contain a KNIME workflow");
                        }
                    });
                    event.feedback = DND.DROP_NONE;
                    return;
                }
            }
            targetPath = r.getFullPath();
        }
        // find source
        Tree tree = (Tree)((DropTarget)event.widget).getControl();
        TreeItem[] selections = tree.getSelection();
        for (TreeItem sourceItem : selections) {
            IPath p = getPathFor(sourceItem);
            m_actions.add(new MoveWorkflowAction(p, targetPath));
        }
        event.feedback = DND.DROP_MOVE;
    }

    
    private IPath getPathFor(final TreeItem item) {
        TreeItem item2 = item;
        String path = "";
        while (item2 != null) {
            path = item2.getText() + "/" + path;
            item2 = item2.getParentItem();
        }
        return new Path(path);
    }
    
}
