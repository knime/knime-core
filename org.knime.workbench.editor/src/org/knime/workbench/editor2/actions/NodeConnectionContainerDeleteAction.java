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
 *   27.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.ui.IWorkbenchPart;

import org.knime.workbench.editor2.commands.VerifyingCompoundCommand;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

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
    public NodeConnectionContainerDeleteAction(final IWorkbenchPart part) {
        super(part);
        setLazyEnablementCalculation(false);
    }

    /**
     * Create one command to remove the selected objects.
     * 
     * @param objects The objects to be deleted.
     * @return The command to remove the selected objects.
     */
    @Override
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

        // get the selected nodes and connections
        List<NodeContainerEditPart> nodeParts = getNodeContainerEditParts(getSelectedObjects());
        List<ConnectionContainerEditPart> connParts = getConnectionContainerEditParts(getSelectedObjects());
        if (nodeParts.size() == 1) {

            NodeContainerEditPart nodePart = nodeParts.get(0);
            String name = nodePart.getNodeContainer().getName();
            String customName = nodePart.getNodeContainer().getCustomName();
            String text = "";
            if (customName != null) {
                text = customName + " - ";
            }
            text += name
                + " (#" + nodePart.getNodeContainer().getID() + ")";

            String dialogText = "Do you really want to delete "
                    + "the selected node: " + text + "?";
            compoundCmd.setDialogDisplayText(dialogText);
        } else {

            String dialogText = "Do you really want to delete ";

            if (nodeParts.size() > 0) {
                dialogText += nodeParts.size() + " nodes ";
            }
            if (nodeParts.size() > 0 && connParts.size() > 0) {
                dialogText += "and ";
            }
            if (connParts.size() > 0) {
                dialogText += connParts.size() + " connection(s)";
            }

            dialogText += "?";

            compoundCmd.setDialogDisplayText(dialogText);
        }

        // set the parts into the compound command
        compoundCmd.setNodeParts(nodeParts);

        for (int i = 0; i < objects.size(); i++) {
            EditPart object = (EditPart)objects.get(i);
            Command cmd = object.getCommand(deleteReq);
            if (cmd != null) {
                compoundCmd.add(cmd);
            }
        }

        return compoundCmd;
    }

    private List<NodeContainerEditPart> getNodeContainerEditParts(
            final List objects) {

        List<NodeContainerEditPart> result = new ArrayList<NodeContainerEditPart>();

        for (int i = 0; i < objects.size(); i++) {
            Object obj = objects.get(i);

            if (obj instanceof NodeContainerEditPart) {
                result.add((NodeContainerEditPart)obj);
            }
        }

        return result;
    }

    private List<ConnectionContainerEditPart> getConnectionContainerEditParts(
            final List objects) {

        List<ConnectionContainerEditPart> result = new ArrayList<ConnectionContainerEditPart>();

        for (int i = 0; i < objects.size(); i++) {
            Object obj = objects.get(i);

            if (obj instanceof ConnectionContainerEditPart) {
                result.add((ConnectionContainerEditPart)obj);
            }
        }

        return result;
    }
}
