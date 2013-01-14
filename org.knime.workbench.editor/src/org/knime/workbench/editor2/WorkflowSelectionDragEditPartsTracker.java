/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
     * Additionally the method creates a command to adapt connections where both
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

    /**
     * Returns the connections whose source and target is contained in the argument list.
     * @param parts list of selected nodes
     * @return the connections whose source and target is contained in the argument list.
     */
    public static ConnectionContainerEditPart[] getEmbracedConnections(
            final List<EditPart> parts) {

        // result list
        List<ConnectionContainerEditPart> result =
                new ArrayList<ConnectionContainerEditPart>();

        for (EditPart part : parts) {
            if (part instanceof NodeContainerEditPart
                    || part instanceof AbstractWorkflowPortBarEditPart) {
                EditPart containerPart = part;

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

    @SuppressWarnings("unchecked")
    private static ConnectionContainerEditPart[] getOutportConnections(
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
