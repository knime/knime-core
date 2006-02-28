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
 *   27.02.2006 (sieb): created
 */
package de.unikn.knime.workbench.editor2.actions;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.ui.IWorkbenchPart;

import de.unikn.knime.workbench.editor2.commands.VerifyingCompoundCommand;

/**
 * This class overrides the default delete action of eclipse. This is due to
 * enable collection commands. This means the createDeleteCommand method expects
 * a command that deletes all selected parts.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeConnectionContainerDeleteAction extends DeleteAction {

    /**
     * Constructs a <code>NodeConnectionContainerDeleteAction</code> using the
     * specified part.
     * 
     * @param part The part for this action
     */
    public NodeConnectionContainerDeleteAction(IWorkbenchPart part) {
        super(part);
        setLazyEnablementCalculation(false);
    }

    /**
     * Create one command to remove the selected objects.
     * 
     * @param objects The objects to be deleted.
     * @return The command to remove the selected objects.
     */
    public Command createDeleteCommand(final List objects) {
        if (objects.isEmpty()) {
            return null;
        }

        if (!(objects.get(0) instanceof EditPart)) {
            return null;
        }

        GroupRequest deleteReq = new GroupRequest(RequestConstants.REQ_DELETE);
        deleteReq.setEditParts(objects);

        VerifyingCompoundCommand compoundCmd = new VerifyingCompoundCommand(
                GEFMessages.DeleteAction_ActionDeleteCommandName);

        // set the text to display in the verification dialog
        compoundCmd.setDialogDisplayText("Do you really want to delete "
                + "the selected component(s)?");

        for (int i = 0; i < objects.size(); i++) {
            EditPart object = (EditPart)objects.get(i);
            Command cmd = object.getCommand(deleteReq);
            if (cmd != null) {
                compoundCmd.add(cmd);
            }
        }

        return compoundCmd;
    }
}
