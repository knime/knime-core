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
 *   02.03.2006 (sieb): created
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.commands.UnexecutableCommand;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.knime.workbench.editor2.editparts.AbstractPortEditPart;
import org.knime.workbench.editor2.editparts.AbstractWorkflowPortBarEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Adjusts the default <code>DragEditPartsTracker</code> to create commands
 * that also move bendpoints.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowSelectionDragEditPartsTracker extends DragEditPartsTracker {

    /**
     * Constructs a new WorkflowSelectionDragEditPartsTracker with the given
     * source edit part.
     * 
     * @param sourceEditPart the source edit part
     */
    public WorkflowSelectionDragEditPartsTracker(final EditPart sourceEditPart) {
        super(sourceEditPart);
    }

    /**
     * Asks each edit part in the
     * {@link org.eclipse.gef.tools.AbstractTool#getOperationSet() operation set}
     * to contribute to a {@link CompoundCommand} after first setting the
     * request type to either {@link org.eclipse.gef.RequestConstants#REQ_MOVE}
     * or {@link org.eclipse.gef.RequestConstants#REQ_ORPHAN}, depending on the
     * result of {@link #isMove()}.
     * 
     * Aditionally the method creats a command to adapt connections where both
     * node container are include in the drag operation.
     * 
     * @see org.eclipse.gef.tools.AbstractTool#getCommand()
     */
    @Override
    protected Command getCommand() {

        CompoundCommand command = new CompoundCommand();
        command.setDebugLabel("Drag Object Tracker");

        Iterator iter = getOperationSet().iterator();

        Request request = getTargetRequest();

        if (isCloneActive()) {
            request.setType(REQ_CLONE);
        } else if (isMove()) {
            request.setType(REQ_MOVE);
        } else {
            request.setType(REQ_ORPHAN);
        }

        if (!isCloneActive()) {
            while (iter.hasNext()) {
                EditPart editPart = (EditPart)iter.next();
                command.add(editPart.getCommand(request));
            }
        }

        // now add the commands for the node-embraced connections
        ConnectionContainerEditPart[] connectionsToAdapt =
                getEmbracedConnections(getOperationSet());
        for (ConnectionContainerEditPart connectionPart : connectionsToAdapt) {

            command.add(connectionPart.getBendpointAdaptionCommand(request));
        }

        if (!isMove() || isCloneActive()) {

            if (!isCloneActive()) {
                request.setType(REQ_ADD);
            }

            if (getTargetEditPart() == null) {
                command.add(UnexecutableCommand.INSTANCE);
            } else {
                command.add(getTargetEditPart().getCommand(getTargetRequest()));
            }
        }

        return command;
    }

    // TODO: rewrite to handle also workflow ports!
    private ConnectionContainerEditPart[] getEmbracedConnections(
            final List<EditPart> parts) {

        // result list
        List<ConnectionContainerEditPart> result =
                new ArrayList<ConnectionContainerEditPart>();

        for (EditPart part : parts) {
            if (part instanceof NodeContainerEditPart 
                    || part instanceof AbstractWorkflowPortBarEditPart) {
                EditPart containerPart =
                        (EditPart)part;

                ConnectionContainerEditPart[] outPortConnectionParts =
                        getOutportConnections(containerPart);

                // if one of the connections in-port-node is included in the
                // selected list, the connections bendpoints must be adapted
                for (ConnectionContainerEditPart connectionPart 
                            : outPortConnectionParts) {

                    // get the in-port-node part of the connection and check
                    AbstractPortEditPart inPortPart = null;
                    if (connectionPart.getTarget() != null
                            && ((AbstractPortEditPart)connectionPart
                                    .getTarget()).isInPort()) {
                        inPortPart =
                                (AbstractPortEditPart)connectionPart
                                        .getTarget();
                    } else if (connectionPart.getSource() != null) {
                        inPortPart =
                                (AbstractPortEditPart)connectionPart
                                        .getSource();
                    }

                    if (inPortPart != null
                            && isPartInList(inPortPart.getParent(), parts)) {
                        result.add(connectionPart);
                    }
                }

            }
        }
        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    private ConnectionContainerEditPart[] getOutportConnections(
            final EditPart containerPart) {

        // result list
        List<ConnectionContainerEditPart> result =
                new ArrayList<ConnectionContainerEditPart>();
        List<EditPart> children = containerPart.getChildren();

        for (EditPart part : children) {
            if (part instanceof AbstractPortEditPart) {
//                    && !((AbstractPortEditPart)part).isInPort()) {
                AbstractPortEditPart outPortPart = (AbstractPortEditPart)part;

                // append all connection edit parts
                result.addAll(outPortPart.getSourceConnections());
            }
        }

        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    private static boolean isPartInList(final EditPart partToCheck,
            final List<EditPart> parts) {

        for (EditPart part : parts) {

            if (part == partToCheck) {

                return true;
            }
        }

        return false;
    }
}
