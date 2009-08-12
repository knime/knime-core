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
 *   27.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.ui.IWorkbenchPart;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.DeleteCommand;
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
    public NodeConnectionContainerDeleteAction(final WorkflowEditor part) {
        super((IWorkbenchPart)part);
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

        VerifyingCompoundCommand compoundCmd = new VerifyingCompoundCommand(
                GEFMessages.DeleteAction_ActionDeleteCommandName);

        List<NodeContainerEditPart> nodeParts = 
            new ArrayList<NodeContainerEditPart>();
        for (Object o : objects) {
            if (o instanceof NodeContainerEditPart) {
                nodeParts.add((NodeContainerEditPart)o);
            }
        }
        
        WorkflowManager manager = 
            ((WorkflowEditor)getWorkbenchPart()).getWorkflowManager();
        DeleteCommand cmd = new DeleteCommand(objects, manager);
        int nodeCount = cmd.getNodeCount();
        int connCount = cmd.getConnectionCount();
        
        StringBuilder dialogText = 
            new StringBuilder("Do you really want to delete ");
        if (nodeCount > 0) {
            dialogText.append(nodeCount).append(" node");
            dialogText.append(nodeCount > 1 ? "s " : " ");
            dialogText.append(connCount > 0 ? "and " : "");
        }
        if (connCount > 0) {
            dialogText.append(connCount).append(" connection");
            dialogText.append(connCount > 1 ? "s " : " ");
        }
        dialogText.append("?");
        compoundCmd.setDialogDisplayText(dialogText.toString());

        // set the parts into the compound command (for unmarking after cancel)
        compoundCmd.setNodeParts(nodeParts);
        compoundCmd.add(cmd);

        return compoundCmd;
    }

    /** Adds all connections, which do not belong to the current selection
     * to the connParts argument. This is necessary to keep track on which 
     * connections were removed (to allow for undo).
     * @param nodeParts the selected nodes
     * @param connParts the already selected connections. This list will be 
     * modified by this method.
     */
    void addAffectedConnections(
            final Collection<NodeContainerEditPart> nodeParts, 
            final Collection<ConnectionContainerEditPart> connParts) {
        for (NodeContainerEditPart ncep : nodeParts) {
            connParts.addAll(Arrays.asList(ncep.getAllConnections()));
        }
    }
}
