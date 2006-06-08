/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
package de.unikn.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.workbench.editor2.ImageRepository;
import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to reset a node.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ResetAction extends AbstractNodeAction {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ResetAction.class);

    /** unique ID for this action. * */
    public static final String ID = "knime.action.reset";

    /**
     * 
     * @param editor The workflow editor
     */
    public ResetAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    public String getId() {
        return ID;
    }

    /**
     * @see org.eclipse.jface.action.IAction#getText()
     */
    public String getText() {
        return "Reset";
    }

    /**
     * @see org.eclipse.jface.action.IAction#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/resetNode.gif");
    }

    /**
     * @see org.eclipse.jface.action.IAction#getToolTipText()
     */
    public String getToolTipText() {
        return "Reset the selected node(s)";
    }

    /**
     * Resets all nodes, this is lightweight and does not need to be executed
     * inside an async job.
     * 
     * @see de.unikn.knime.workbench.editor2.actions. AbstractNodeAction#
     *      runOnNodes(de.unikn.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
        mb.setText("Confirm reset...");
        mb.setMessage("Do you really want to reset the selected node(s) ?");
        if (mb.open() != SWT.YES) {
            return;
        }

        LOGGER.debug("Resetting " + nodeParts.length + " node(s)");
        for (int i = 0; i < nodeParts.length; i++) {
            // skip locked nodes
            if (nodeParts[i].isLocked()) {
                LOGGER.debug("Node #"
                        + nodeParts[i].getNodeContainer().getID()
                        + " is locked and can't be resetted now");
                continue;
            }

            // TODO FIXME This should be executed on the WFM, and result in a
            // "workflow changed event" that triggers calculateEnabled !
            nodeParts[i].getNodeContainer().reset();
        }
    }

    /**
     * @return <code>true</code> if at least one node is executed
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getSelectedNodeParts();
        
        for (int i = 0; i < parts.length; i++) {
            NodeContainerEditPart part = parts[i];
            if (part.getNodeContainer().isExecuted()) {
                return true;
            }
        }
        return false;
    }

}
