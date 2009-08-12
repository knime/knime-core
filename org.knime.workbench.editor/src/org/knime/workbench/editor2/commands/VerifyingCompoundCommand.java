/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   28.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.commands;

import java.util.List;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Overrides the default <code>CompoundCommand</code> to add a verification
 * dialog. The display text can be specified.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class VerifyingCompoundCommand extends CompoundCommand {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(VerifyingCompoundCommand.class);

    /**
     * The text to display in the verfication dialog.
     */
    private String m_dialogDisplayText;

    /**
     * The selected parts of this compound command.
     */
    private List<NodeContainerEditPart> m_nodeParts;

    /**
     * Constructs an empty CompoundCommand.
     * 
     * @since 2.0
     */
    public VerifyingCompoundCommand() {
    }

    /**
     * Constructs an empty VerifyingCompoundCommand with the specified label.
     * 
     * @param label the label for the Command
     */
    public VerifyingCompoundCommand(final String label) {
        super(label);
    }

    /**
     * Overrides the execute method of <code>CompoundCommand</code> to add a
     * verification dialog with the given message.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        // before showing the confirmation dialog, mark the node part figures
        for (NodeContainerEditPart nodePart : m_nodeParts) {
            nodePart.mark();
        }
        try {
            // the following code has mainly been copied from
            // IDEWorkbenchWindowAdvisor#preWindowShellClose
            IPreferenceStore store = 
                KNIMEUIPlugin.getDefault().getPreferenceStore();
            if (!store.contains(PreferenceConstants.P_CONFIRM_DELETE)
                    || store.getBoolean(PreferenceConstants.P_CONFIRM_DELETE)) {
                MessageDialogWithToggle dialog = 
                    MessageDialogWithToggle.openOkCancelConfirm(
                        Display.getDefault().getActiveShell(), 
                        "Confirm ...", m_dialogDisplayText, 
                        "Do not ask again", false, null, null);
                if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                    return;
                }
                if (dialog.getToggleState()) {
                    store.setValue(PreferenceConstants.P_CONFIRM_DELETE, false);
                    KNIMEUIPlugin.getDefault().savePluginPreferences();
                }
            }
            
            // in all other cases execute the commands
            LOGGER.debug("Executing <" + size() + "> commands.");
            super.execute();
        } finally {
            for (NodeContainerEditPart nodePart : m_nodeParts) {
                nodePart.unmark();
            }
        }
    }

    /**
     * @param dialogDisplayText the text to display in the verification dialog
     */
    public void setDialogDisplayText(final String dialogDisplayText) {
        m_dialogDisplayText = dialogDisplayText;
    }

    /**
     * Sets the node parts affected by this compound command.
     * 
     * @param nodeParts the affected parts.
     */
    public void setNodeParts(final List<NodeContainerEditPart> nodeParts) {
        this.m_nodeParts = nodeParts;
    }
}
