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
 * ---------------------------------------------------------------------
 *
 * Created: Mar 30, 2011
 * Author: ohl
 */
package org.knime.workbench.editor2.commands;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Creates a new node - and may auto connect it to another one.
 *
 * @author ohl, University of Konstanz
 */
public class CreateNewConnectedNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateNewConnectedNodeCommand.class);

    private final EditPartViewer m_viewer;

    private final NodeFactory<? extends NodeModel> m_factory;

    private final Point m_location;

    private final NodeID m_connectTo;

    private NodeID m_newNode;

    /**
     * Creates a new node and connects it to the passed node - if it fits.
     *
     * @param viewer the workflow viewer
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param location Initial visual location of the new node ABSOLTE COORDS!
     * @param connectTo node to which the new node should be connected to
     */
    public CreateNewConnectedNodeCommand(final EditPartViewer viewer,
            final WorkflowManager manager,
            final NodeFactory<? extends NodeModel> factory,
            final Point location, final NodeID connectTo) {
        super(manager);
        m_viewer = viewer;
        m_factory = factory;
        m_location = location;
        m_connectTo = connectTo;

    }

    /**
     * We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_factory != null && m_location != null && super.canExecute();
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {

        m_newNode = createNewNode();
        if (m_newNode != null && m_connectTo != null) {
            autoConnectNewNode();
        }
        // make sure the new node is selected and visible
        m_viewer.deselectAll();
        ((WorkflowRootEditPart)m_viewer.getRootEditPart().getContents())
                .setFutureSelection(new NodeID[]{m_newNode});
    }

    protected NodeID createNewNode() {
        // Add node to workflow and get the container
        NodeID newID = null;
        WorkflowManager hostWFM = getHostWFM();
        try {
            newID = hostWFM.createAndAddNode(m_factory);
        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", t);
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb.setMessage("The node could not be created "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return null;
        }
        // create extra info and set it
        NodeUIInformation info =
                new NodeUIInformation(m_location.x, m_location.y, -1, -1, true);
        NodeContainer nc = hostWFM.getNodeContainer(newID);
        nc.setUIInformation(info);
        return newID;
    }

    protected void autoConnectNewNode() {
        if (m_newNode == null) {
            return;
        }
        if (m_connectTo == null) {
            return;
        }
        WorkflowManager hostWFM = getHostWFM();
        NodeContainer sourceNode = hostWFM.getNodeContainer(m_connectTo);
        if (sourceNode == null) {
            // it's gone...
            return;
        }
        NodeContainer nc = hostWFM.getNodeContainer(m_newNode);
        Map<Integer, Integer> matchingPorts = getMatchingPorts(sourceNode, nc);
        if (matchingPorts == null) {
            LOGGER.info("Can't auto-connect new node (" + m_newNode + "): "
                    + "no matching port type found at node "
                    + sourceNode.getNameWithID());
            return;
        }
        for (Map.Entry<Integer, Integer> entry : matchingPorts.entrySet()) {
            Integer leftPort = entry.getKey();
            Integer rightPort = entry.getValue();
            LOGGER.info("Autoconnect: Connecting new node " + m_newNode + " port "
                    + rightPort + " with existing node " + sourceNode
                    + " port " + leftPort);
            try {
                hostWFM.addConnection(m_connectTo, leftPort, m_newNode,
                        rightPort).getID();
            } catch (Exception e) {
                String from = sourceNode.getNameWithID();
                String to = nc.getNameWithID();
                String msg =
                        "Unable to add connection from " + from + " port "
                                + leftPort + " to " + to + "port "
                                + rightPort + ": " + e.getMessage();
                LOGGER.error(msg);
            }
        }
    }

    private Map<Integer, Integer> getMatchingPorts(final NodeContainer left,
            final NodeContainer right) {
        // don't auto connect to flow var ports - start with port index 1
        int leftFirst = (left instanceof WorkflowManager) ? 0 : 1;
        int rightFirst = (right instanceof WorkflowManager) ? 0 : 1;
        Map<Integer, Integer> matchingPorts = new TreeMap<Integer, Integer>();
        Map<Integer, Integer> possibleMatches = new TreeMap<Integer, Integer>();
        Set<Integer> assignedRight = new HashSet<Integer>();
        for (int rightPidx = rightFirst; rightPidx < right.getNrInPorts(); rightPidx++) {
            for (int leftPidx = leftFirst; leftPidx < left.getNrOutPorts(); leftPidx++) {
                NodeOutPort leftPort = left.getOutPort(leftPidx);
                NodeInPort rightPort = right.getInPort(rightPidx);
                PortType leftPortType = leftPort.getPortType();
                PortType rightPortType = rightPort.getPortType();
                if (leftPortType.isSuperTypeOf(rightPortType)) {
                    if (getHostWFM().getOutgoingConnectionsFor(left.getID(),
                            leftPidx).size() == 0) {
                        if (!matchingPorts.containsKey(leftPidx)
                                && !assignedRight.contains(rightPidx)) {
                            // output not connected: use it.
                            matchingPorts.put(leftPidx, rightPidx);
                            assignedRight.add(rightPidx);
                        }
                    } else {
                        // port already connected - we MAY use it
                        if (!possibleMatches.containsKey(leftPidx)
                                && !assignedRight.contains(rightPidx)) {
                            possibleMatches.put(leftPidx, rightPidx);
                        }
                    }
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : possibleMatches.entrySet()) {
            Integer pl = entry.getKey();
            Integer pr = entry.getValue();
            if(!matchingPorts.containsKey(pl) && !assignedRight.contains(pr)) {
                matchingPorts.put(pl, pr);
                assignedRight.add(pr);
            }
        }
        return matchingPorts;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_newNode != null && getHostWFM().canRemoveNode(m_newNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        try {
            // blows away the connection as well.
            getHostWFM().removeNode(m_newNode);
        } catch (Exception e) {
            String msg =
                    "Undo failed: Node " + m_newNode + " can't be removed: "
                            + e.getMessage();
            LOGGER.error(msg);
            MessageDialog.openInformation(
                    Display.getDefault().getActiveShell(),
                    "Operation not allowed", msg);
        }
    }

}
