/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
