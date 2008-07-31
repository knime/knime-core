/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 *
 * History
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

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
        return "Reset";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/resetNode.gif");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/resetNode_disabled.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Reset the selected node(s)";
    }

    /**
     * Resets all nodes, this is lightweight and does not need to be executed
     * inside an async job.
     *
     * @see org.knime.workbench.editor2.actions. AbstractNodeAction#
     *      runOnNodes(org.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        // the following code has mainly been copied from
        // IDEWorkbenchWindowAdvisor#preWindowShellClose
        IPreferenceStore store = 
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
            MessageDialogWithToggle dialog = 
                MessageDialogWithToggle.openOkCancelConfirm(
                    Display.getDefault().getActiveShell(), 
                    "Confirm reset...", 
                    "Do you really want to reset the selected node(s) ?", 
                    "Do not ask again", false, null, null);
            if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                return;
            }
            if (dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
        }

        LOGGER.debug("Resetting " + nodeParts.length + " node(s)");
        try {
            for (int i = 0; i < nodeParts.length; i++) {
                // skip locked nodes
                if (nodeParts[i].isLocked()) {
                    LOGGER.debug("Node #"
                            + nodeParts[i].getNodeContainer().getID()
                            + " is locked and can't be reset now");
                    continue;
                }

                getManager().resetAndConfigureNode(
                        nodeParts[i].getNodeContainer().getID());
            }

        } catch (Exception ex) {
            MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                    SWT.ICON_INFORMATION | SWT.OK);
            mb.setText("Reset not allowed");
            mb.setMessage("You cannot reset a node while the workflow is in"
                    + " execution. " + ex.getMessage());
            mb.open();
        }

    }

    /**
     * @return <code>true</code> if at least one node is executed
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts = getSelectedNodeParts();

        for (int i = 0; i < parts.length; i++) {
            NodeContainerEditPart part = parts[i];
            if (part.getNodeContainer().getState().equals(
                    NodeContainer.State.EXECUTED)) {
                return true;
            }
        }
        return false;
    }
}
