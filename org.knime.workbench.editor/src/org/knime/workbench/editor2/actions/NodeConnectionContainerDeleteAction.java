/* 
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
