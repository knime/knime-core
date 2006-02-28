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
 *   28.02.2006 (sieb): created
 */
package de.unikn.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import de.unikn.knime.core.node.NodeLogger;

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
    public void execute() {

        // first create the verification dialog for confirmation of the
        // compound command
        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(),
                SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
        mb.setText("Confirm ...");
        mb.setMessage(m_dialogDisplayText);
        if (mb.open() != SWT.YES) {
            return;
        }

        // in all other cases execute the commands
        LOGGER.debug("Executing <" + size() + "> commands.");
        super.execute();
    }

    /**
     * @param dialogDisplayText the text to display in the verification dialog
     */
    public void setDialogDisplayText(final String dialogDisplayText) {
        m_dialogDisplayText = dialogDisplayText;
    }
}
