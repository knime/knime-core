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
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.SingleNodeContainer;
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
     * {@inheritDoc}
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
                getManager().resetAndConfigureNode(
                        nodeParts[i].getNodeContainer().getID());
            }

        } catch (Exception ex) {
            MessageBox mb = new MessageBox(
                    Display.getDefault().getActiveShell(),
                    SWT.ICON_INFORMATION | SWT.OK);
            mb.setText("Reset not allowed");
            mb.setMessage("You cannot reset a node while the workflow is in"
                    + " execution. " + ex.getMessage());
            mb.open();
        }

    }

    /**
     * @return <code>true</code> if at least one node is resetable
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        for (int i = 0; i < parts.length; i++) {
            NodeContainerEditPart part = parts[i];
            NodeContainer nc = part.getNodeContainer();
            boolean canReset = getManager().canResetNode(nc.getID());
            if (canReset) {
                // SNC#isResetable is a bit too flexible (allows CONFIGURED
                // nodes to be reset)
                if (nc instanceof SingleNodeContainer) {
                    if (State.EXECUTED.equals(nc.getState())) {
                        return true;
                    }
                } else {
                    // meta nodes can always be reset (for now -- to be revised)
                    return true;
                }
            }
        }
        return false;
    }
}
