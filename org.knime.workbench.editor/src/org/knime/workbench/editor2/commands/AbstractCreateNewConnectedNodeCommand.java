/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * History
 *   20.06.2012 (meinl): created
 */
package org.knime.workbench.editor2.commands;

import static org.knime.core.ui.wrapper.Wrapper.unwrapNC;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeInPortUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.util.SWTUtilities;
import org.knime.workbench.ui.async.AsyncUtil;

/**
 * Abstract super class for commands that insert new nodes into a workflow and potentially auto-connect them
 *  to an existing node.
 *
 * TODO ldq sez: The naïve me says that this should be a subclass of CreateConnectionCommand
 *          and that hierarchy downwards should be modified to suit descending from that class.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public abstract class AbstractCreateNewConnectedNodeCommand extends
        AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractCreateNewConnectedNodeCommand.class);

    private final EditPartViewer m_viewer;

    private final Point m_location;

    private final NodeID m_connectTo;

    private NodeID m_newNode;

    /**
     * Creates a new node and connects it to the passed node - if it fits.
     *
     * @param viewer the workflow viewer
     * @param manager the workflow manager that should host the new node
     * @param location the absolute coordinates of the initial visual location
     *            of the new node
     * @param connectTo node to which the new node should be connected to
     */
    public AbstractCreateNewConnectedNodeCommand(final EditPartViewer viewer,
            final WorkflowManagerUI manager, final Point location,
            final NodeID connectTo) {
        super(manager);
        m_viewer = viewer;
        m_location = location;
        m_connectTo = connectTo;

    }

    /**
     * We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_location != null && super.canExecute();
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // create extra info and pass it
        NodeUIInformation uiInfo =
            NodeUIInformation.builder().setNodeLocation(m_location.x, m_location.y, -1, -1).build();
        m_newNode = createNewNode(uiInfo);
        if (m_newNode != null) {
            if (m_connectTo != null) {
                autoConnectNewNode();
            }
        }
        // make sure the new node is selected and visible
        m_viewer.deselectAll();
    }

    /**
     * Creates a new node in the current workflow manager (which can be
     * retrieved with {@link #getHostWFM()}) and returns its node id.
     * @param uiInfo the ui info to be passed to the new node
     *
     * @return the node id of the new node
     */
    protected abstract NodeID createNewNode(NodeUIInformation uiInfo);

    private void autoConnectNewNode() {
        if (m_newNode == null) {
            return;
        }
        if (m_connectTo == null) {
            return;
        }
        WorkflowManagerUI hostWFM = getHostWFMUI();
        NodeContainerUI sourceNode = hostWFM.getNodeContainer(m_connectTo);
        NodeContainerUI nc = hostWFM.getNodeContainer(m_newNode);
        Map<Integer, Integer> matchingPorts = getMatchingPorts(sourceNode, nc);
        if (matchingPorts.size() == 0) {
            LOGGER.info("Can't auto-connect new node (" + m_newNode + "): "
                    + "no matching port type found at node "
                    + sourceNode.getNameWithID());
            return;
        }
        for (Map.Entry<Integer, Integer> entry : matchingPorts.entrySet()) {
            Integer leftPort = entry.getKey();
            Integer rightPort = entry.getValue();
            LOGGER.info("Autoconnect: Connecting new node " + m_newNode
                    + " port " + rightPort + " with existing node "
                    + sourceNode + " port " + leftPort);
            try {
                AsyncUtil.wfmAsyncSwitchRethrow(wfm -> {
                    return wfm.addConnection(m_connectTo, leftPort, m_newNode, rightPort);
                }, wfm -> {
                    //TODO do parallel not sequential
                    return wfm.addConnectionAsync(m_connectTo, leftPort, m_newNode, rightPort);
                }, hostWFM, "Creating connection ...");
                if (wraps(sourceNode, NodeContainer.class)) {
                    NodeTimer.GLOBAL_TIMER.addConnectionCreation(unwrapNC(sourceNode), unwrapNC(nc));
                }
            } catch (Exception e) {
                String from = sourceNode.getNameWithID();
                String to = nc.getNameWithID();
                String msg =
                        "Unable to add connection from " + from + " port "
                                + leftPort + " to " + to + "port " + rightPort
                                + ": " + e.getMessage();
                LOGGER.error(msg);
            }
        }
    }

    private Map<Integer, Integer> getMatchingPorts(final NodeContainerUI left,
            final NodeContainerUI right) {
        // don't auto connect to flow var ports - start with port index 1
        int leftFirst = (left instanceof WorkflowManagerUI) ? 0 : 1;
        int rightFirst = (right instanceof WorkflowManagerUI) ? 0 : 1;
        Map<Integer, Integer> matchingPorts = new TreeMap<Integer, Integer>();
        Map<Integer, Integer> possibleMatches = new TreeMap<Integer, Integer>();
        Set<Integer> assignedRight = new HashSet<Integer>();
        for (int rightPidx = rightFirst; rightPidx < right.getNrInPorts(); rightPidx++) {
            for (int leftPidx = leftFirst; leftPidx < left.getNrOutPorts(); leftPidx++) {
                NodeOutPortUI leftPort = left.getOutPort(leftPidx);
                NodeInPortUI rightPort = right.getInPort(rightPidx);
                PortType leftPortType = leftPort.getPortType();
                PortType rightPortType = rightPort.getPortType();
                if (leftPortType.isSuperTypeOf(rightPortType)) {
                    if (getHostWFMUI().getOutgoingConnectionsFor(left.getID(),
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
            if (!matchingPorts.containsKey(pl) && !assignedRight.contains(pr)) {
                matchingPorts.put(pl, pr);
                assignedRight.add(pr);
            }
        }
        return matchingPorts;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_newNode != null && getHostWFMUI().canRemoveNode(m_newNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        try {
            // blows away the connection as well.
            AsyncUtil.wfmAsyncSwitchRethrow(wfm -> {
                wfm.remove(new NodeID[]{m_newNode}, null, null);
                return null;
            }, wfm -> {
                return wfm.removeAsync(new NodeID[]{m_newNode}, null, null);
            }, getHostWFMUI(), "Removing node ...");
        } catch (Exception e) {
            String msg =
                    "Undo failed: Node " + m_newNode + " can't be removed: "
                            + e.getMessage();
            LOGGER.error(msg);
            MessageDialog.openInformation(SWTUtilities.getActiveShell(), "Operation not allowed", msg);
        }
    }
}
