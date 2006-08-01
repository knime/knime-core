/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;

import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to cancel all nodes that are running.
 * 
 * @author Christoph sieb, University of Konstanz
 */
public class CancelAllAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CancelAllAction.class);

    /** unique ID for this action. * */
    public static final String ID = "knime.action.cancelall";

    /**
     * 
     * @param editor The workflow editor
     */
    public CancelAllAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    @Override
    public String getText() {
        return "Cancel all";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/executeAll.PNG");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return "Cancel all running nodes.";
    }

    /**
     * @return <code>true</code>, if at least one node is running
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        if (getManager() == null) {
            return false;
        }
        return getManager().executionInProgress();
    }

    /**
     * This cancels all running jobs.
     * 
     * @see org.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(org.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
        mb.setText("Confirm cancel all...");
        mb.setMessage("Do you really want to cancel all running node(s) ?");
        if (mb.open() != SWT.YES) {
            return;
        }

        LOGGER.debug("(Cancel all)  cancel all running jobs.");
        getManager().cancelExecution();
    }
}
