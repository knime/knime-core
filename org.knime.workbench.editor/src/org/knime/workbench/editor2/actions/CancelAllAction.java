/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
import org.knime.core.node.workflow.NodeContainer;
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
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Cancel all";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/cancelAll.PNG");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/cancelAll_disabled.PNG");
    }

    /**
     * {@inheritDoc}
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
        NodeContainerEditPart[] parts = getAllNodeParts();
        // enable if we have at least one cancel-able node in our selection
        for (int i = 0; i < parts.length; i++) {
            NodeContainer nc = parts[i].getNodeContainer();
            if (nc.getState().executionInProgress()) {
                return true;
            }

        }
        return false;
    }

    /**
     * This cancels all running jobs.
     *
     * {@inheritDoc}
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
        // bugfix 1386 -> get all parts (not only selected ones)
        for (NodeContainerEditPart part : getAllNodeParts()) {
            getManager().cancelExecution(part.getNodeContainer());
        }
        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }
}
