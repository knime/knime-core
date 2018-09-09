/*
 * ------------------------------------------------------------------------
 *
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
 *   Apr 5, 2018 (loki): created
 */
package org.knime.workbench.editor2.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.ConnectionManifest;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This is the Command functionality invoked by the <code>NodeSupplantDragListener</code> under the appropriate mouse-up
 * conditions.
 *
 * @author loki der quaeler
 */
public class SupplantationCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(SupplantationCommand.class);


    private ConnectionID m_edgeTargetId;

    private int[] m_redoNodeBounds;

    private final NodeContainerEditPart m_nodeTarget;
    private WorkflowPersistor m_undoWorkflowPersitor;

    private Set<ConnectionContainer> m_originalEdges;
    private Set<ConnectionContainer> m_replacementEdges;

    private final NodeContainerEditPart m_supplantingNode;
    private final int[] m_originalSupplantingNodeBounds;
    private final ConnectionManifest m_supplantingNodeInportManifest;
    private final ConnectionManifest m_supplantingNodeOutportManifest;

    /**
     * This constructor should be used when a node is being dropped on a connection.
     *
     * @param dragNode the node which is being dropped on a connection
     * @param mouseDownLocation the location the node was when the drag began (captured for undo purposes)
     * @param targetEdge the connection onto which the node is being dropped
     * @param dragInportManifest the <code>ConnectionManifest</code> instance representing the potential inports of the
     *            dropped node
     * @param dragOutportManifest the <code>ConnectionManifest</code> instance representing the potential outorts of the
     *            dropped node
     * @param wm the <code>WorkflowManager</code> representing the workflow in which the node sits
     */
    public SupplantationCommand(final NodeContainerEditPart dragNode, final int[] mouseDownLocation,
        final ConnectionContainerEditPart targetEdge, final ConnectionManifest dragInportManifest,
        final ConnectionManifest dragOutportManifest, final WorkflowManager wm) {
        this(dragNode, mouseDownLocation, targetEdge, null, dragInportManifest, dragOutportManifest, wm);
    }

    /**
     * This constructor should be used when a node is being dropped on another node as a replacement.
     *
     * @param dragNode the node which is being dropped on a connection
     * @param mouseDownLocation the location the node was when the drag began (captured for undo purposes)
     * @param targetNode the node onto which the <code>dragNode</code> is being dropped
     * @param dragInportManifest the <code>ConnectionManifest</code> instance representing the potential inports of the
     *            dropped node
     * @param dragOutportManifest the <code>ConnectionManifest</code> instance representing the potential outorts of the
     *            dropped node
     * @param wm the <code>WorkflowManager</code> representing the workflow in which the node sits
     */
    public SupplantationCommand(final NodeContainerEditPart dragNode, final int[] mouseDownLocation,
        final NodeContainerEditPart targetNode, final ConnectionManifest dragInportManifest,
        final ConnectionManifest dragOutportManifest, final WorkflowManager wm) {
        this(dragNode, mouseDownLocation, null, targetNode, dragInportManifest, dragOutportManifest, wm);
    }

    private SupplantationCommand(final NodeContainerEditPart dragNode, final int[] mouseDownLocation,
        final ConnectionContainerEditPart edge, final NodeContainerEditPart target,
        final ConnectionManifest dragInportManifest, final ConnectionManifest dragOutportManifest,
        final WorkflowManager wm) {
        super(wm);

        m_supplantingNode = dragNode;
        m_originalSupplantingNodeBounds = mouseDownLocation;
        m_supplantingNodeInportManifest = dragInportManifest;
        m_supplantingNodeOutportManifest = dragOutportManifest;

        m_edgeTargetId = (edge != null) ? edge.getModel().getID() : null;
        m_nodeTarget = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return (m_supplantingNode != null) && ((m_edgeTargetId != null) || (m_nodeTarget != null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final WorkflowManager wm = getHostWFM();
        if (m_edgeTargetId != null) {
            final ConnectionContainer targetCC = wm.getConnection(m_edgeTargetId);

            if (!ReplaceHelper.executedStateAllowsReplace(wm, null, targetCC)) {
                return;
            }

            final NodeID sourceNodeId = targetCC.getSource();
            final WorkflowManagerUI wmUI = getHostWFMUI();
            final NodeContainerUI sourceNodeUI =
                sourceNodeId.equals(wm.getID()) ? wmUI : wmUI.getNodeContainer(sourceNodeId);
            final PortType portType = sourceNodeUI.getOutPort(targetCC.getSourcePort()).getPortType();
            final ConnectionManifest inportManifest = m_supplantingNodeInportManifest.clone();
            final ConnectionManifest outportManifest = m_supplantingNodeOutportManifest.clone();
            final int dragNodeInPortToUse = inportManifest.consumePortForPortType(portType, true);
            final int dragNodeOutPortToUse = outportManifest.consumePortForPortType(portType, false);

            m_originalEdges = new HashSet<>();

            final NodeID dragNodeId = m_supplantingNode.getNodeContainer().getID();
            if (dragNodeInPortToUse != -1) {
                for (ConnectionContainer cc : wm.getIncomingConnectionsFor(dragNodeId)) {
                    if (cc.getDestPort() == dragNodeInPortToUse) {
                        wm.removeConnection(cc);
                        m_originalEdges.add(cc);
                    }
                }
            }

            wm.removeConnection(targetCC);
            m_originalEdges.add(targetCC);


            m_replacementEdges = new HashSet<>();
            if (dragNodeInPortToUse != -1) {
                m_replacementEdges
                    .add(wm.addConnection(sourceNodeId, targetCC.getSourcePort(), dragNodeId, dragNodeInPortToUse));
            }
            if (dragNodeOutPortToUse != -1) {
                m_replacementEdges.add(
                    wm.addConnection(dragNodeId, dragNodeOutPortToUse, targetCC.getDest(), targetCC.getDestPort()));
            }

            final NodeContainerUI dragNodeUI = m_supplantingNode.getNodeContainer();
            final NodeID targetNodeId = targetCC.getDest();
            final NodeContainerUI destinationNodeUI =
                targetNodeId.equals(wm.getID()) ? wmUI : wmUI.getNodeContainer(targetCC.getDest());

            // If this is a first execute, and not a redo, grab the dragged node's end location so that
            //  we can set it on a redo as seen in the else block.
            if (m_redoNodeBounds == null) {
                m_redoNodeBounds = dragNodeUI.getUIInformation().getBounds();
            } else {
                final NodeUIInformation.Builder builder =
                    NodeUIInformation.builder(m_supplantingNode.getNodeContainer().getUIInformation());

                builder.setNodeLocation(m_redoNodeBounds[0], m_redoNodeBounds[1], m_redoNodeBounds[2],
                    m_redoNodeBounds[3]);
                m_supplantingNode.getNodeContainer().setUIInformation(builder.build());
            }

            NodeTimer.GLOBAL_TIMER.addConnectionCreation(Wrapper.unwrapNC(sourceNodeUI),
                Wrapper.unwrapNC(dragNodeUI));
            NodeTimer.GLOBAL_TIMER.addConnectionCreation(Wrapper.unwrapNC(dragNodeUI),
                Wrapper.unwrapNC(destinationNodeUI));
        } else {
            final NodeContainerUI toRemoveNodeUI = m_nodeTarget.getNodeContainer();
            final NodeID toRemoveNodeId = toRemoveNodeUI.getID();
            final ArrayList<ConnectionContainer> ccs = new ArrayList<>(wm.getOutgoingConnectionsFor(toRemoveNodeId));
            final ConnectionContainer[] connections = ccs.toArray(new ConnectionContainer[ccs.size()]);

            if (!ReplaceHelper.executedStateAllowsReplace(wm, wm.getNodeContainer(toRemoveNodeId), connections)) {
                return;
            }

            final NodeID[] removeIds = new NodeID[1];
            final Set<ScheduledConnection> pendingConnections = new HashSet<>();

            removeIds[0] = toRemoveNodeUI.getID();

            m_originalEdges = new HashSet<>();
            try {
                final ConnectionManifest inportManifest = m_supplantingNodeInportManifest.clone();
                final ConnectionManifest outportManifest = m_supplantingNodeOutportManifest.clone();
                final HashMap<Integer, Integer> outportMap;

                for (ConnectionContainer cc : wm.getIncomingConnectionsFor(removeIds[0])) {
                    try {
                        pendingConnections
                            .add(new ScheduledConnection(toRemoveNodeUI, wm, cc, true, inportManifest, null));
                    } catch (IllegalStateException e) { } // NOPMD  acceptable for no ports on drag node available

                    m_originalEdges.add(cc);
                }

                outportMap = new HashMap<>();
                for (ConnectionContainer cc : wm.getOutgoingConnectionsFor(removeIds[0])) {
                    try {
                        pendingConnections
                            .add(new ScheduledConnection(toRemoveNodeUI, wm, cc, false, outportManifest, outportMap));
                    } catch (IllegalStateException e) { } // NOPMD  acceptable for no ports on drag node available

                    m_originalEdges.add(cc);
                }
            } catch (Exception e) {
                LOGGER.error("We were unable to generate replacement connection descriptions due to: " + e.getMessage(),
                    e);

                return;
            }

            final NodeID dragNodeId = m_supplantingNode.getNodeContainer().getID();
            for (ConnectionContainer cc : wm.getIncomingConnectionsFor(dragNodeId)) {
                wm.removeConnection(cc);
                m_originalEdges.add(cc);
            }


            final WorkflowCopyContent.Builder content = WorkflowCopyContent.builder();
            content.setNodeIDs(removeIds);
            m_undoWorkflowPersitor = wm.copy(true, content.build());

            wm.removeNode(removeIds[0]);

            // If this is a first execute, and not a redo, set the dragged node's location as the replacement node's
            //  location, and hang on to it so that we can set it on a redo as seen in the else block.
            if (m_redoNodeBounds == null) {
                final NodeUIInformation.Builder builder = NodeUIInformation.builder(toRemoveNodeUI.getUIInformation());

                m_supplantingNode.getNodeContainer().setUIInformation(builder.build());
                m_redoNodeBounds = m_supplantingNode.getNodeContainer().getUIInformation().getBounds();
            } else {
                final NodeUIInformation.Builder builder =
                    NodeUIInformation.builder(m_supplantingNode.getNodeContainer().getUIInformation());

                builder.setNodeLocation(m_redoNodeBounds[0], m_redoNodeBounds[1], m_redoNodeBounds[2],
                    m_redoNodeBounds[3]);
                m_supplantingNode.getNodeContainer().setUIInformation(builder.build());
            }

            m_replacementEdges = new HashSet<>();

            final WorkflowManagerUI wmUI = getHostWFMUI();
            for (ScheduledConnection sc : pendingConnections) {
                final NodeID sourceNID;
                final int sourcePort;
                final NodeID destinationNID;
                final int destinationPort;
                final ConnectionUIInformation uiInfo;

                if (sc.isInputConnection()) {
                    sourceNID = sc.getOtherNodeId();
                    sourcePort = sc.getOtherPort();
                    destinationNID = dragNodeId;
                    destinationPort = sc.getNodeInDragPort();
                    uiInfo = sc.getOriginalUIInfo();
                } else {
                    sourceNID = dragNodeId;
                    sourcePort = sc.getNodeInDragPort();
                    destinationNID = sc.getOtherNodeId();
                    destinationPort = sc.getOtherPort();
                    uiInfo = null;
                }

                final ConnectionContainer newCC =
                    wm.addConnection(sourceNID, sourcePort, destinationNID, destinationPort);

                if (uiInfo != null) {
                    newCC.setUIInfo(uiInfo);
                }
                m_replacementEdges.add(newCC);

                final NodeContainerUI sourceNodeUI = wmUI.getNodeContainer(sourceNID);
                final NodeContainerUI destinationNodeUI = wmUI.getNodeContainer(destinationNID);
                NodeTimer.GLOBAL_TIMER.addConnectionCreation(Wrapper.unwrapNC(sourceNodeUI),
                    Wrapper.unwrapNC(destinationNodeUI));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        final WorkflowManager wm = getHostWFM();

        if (m_replacementEdges != null) {
            for (final ConnectionContainer cc : m_replacementEdges) {
                wm.removeConnection(cc);
            }
        }

        if (m_undoWorkflowPersitor != null) {
            wm.paste(m_undoWorkflowPersitor);
        }

        if (m_originalEdges != null) {
            for (final ConnectionContainer cc : m_originalEdges) {
                final ConnectionContainer newCC =
                    wm.addConnection(cc.getSource(), cc.getSourcePort(), cc.getDest(), cc.getDestPort());

                newCC.setUIInfo(cc.getUIInfo());

                // We need to capture the new connection id to support redo
                if (m_edgeTargetId != null) {
                    m_edgeTargetId = newCC.getID();
                }
            }
        }

        final NodeUIInformation.Builder builder =
            NodeUIInformation.builder(m_supplantingNode.getNodeContainer().getUIInformation());

        builder.setNodeLocation(m_originalSupplantingNodeBounds[0], m_originalSupplantingNodeBounds[1],
            m_originalSupplantingNodeBounds[2], m_originalSupplantingNodeBounds[3]);
        m_supplantingNode.getNodeContainer().setUIInformation(builder.build());
    }


    private static class ScheduledConnection {
        private final int m_otherPort;
        private final NodeID m_otherNodeId;

        private final ConnectionUIInformation m_originalConnectionUIInfo;

        private final int m_nodeInDragPort;

        private final boolean m_inputConnection;

        /**
         * @param node this should be the node being targetted, not the node in drag
         * @param wm the workflow manager
         * @param connection this should be a connection which is being replaced
         * @param input this should be <code>true</code> if the connection is an input
         * @param nidConnectionManifest the connection manifest for the node if drag (the output or the input one, as
         *            appropriate to this construction)
         * @param outportMap this should be provided if <code>input == false</code> and the same instance should be
         *            provided for all ConnectionContainer gotten via the WorkflowManager's
         *            getOutgoingConnectionsFor(...) for the node being targetted.
         * @throws IllegalStateException if there is no free compatible port
         * @throws IllegalArgumentException if <code>input == false</code> and <code>outportMap == null</code>, or
         *             <code>input == true</code> and <code>outportMap != null</code>
         */
        private ScheduledConnection(final NodeContainerUI node, final WorkflowManager wm,
            final ConnectionContainer connection, final boolean input, final ConnectionManifest nidConnectionManifest,
            final Map<Integer, Integer> outportMap) {
            if ((!input) && (outportMap == null)) {
                throw new IllegalArgumentException("outportMap cannot be null if input is false.");
            }
            if (input && (outportMap != null)) {
                throw new IllegalArgumentException("outportMap cannot be non-null if input is true.");
            }

            final Integer nodePort = new Integer(input ? connection.getDestPort() : connection.getSourcePort());
            final PortType portType = input ? node.getInPort(nodePort.intValue()).getPortType()
                : node.getOutPort(nodePort.intValue()).getPortType();
            final int port;

            if (portType.equals(FlowVariablePortObject.TYPE)) {
                port = nodePort.intValue();
            } else if (outportMap != null) {
                // The purpose of this whole thing is to address the scenario in which the targetted node has
                //      an outport with, for example, 2 connections on it and the node-in-drag has 2 port-type
                //      compatible outports where each of them gets assigned one of the connections.
                final Integer portI = outportMap.get(nodePort);

                if (portI == null) {
                    port = nidConnectionManifest.consumePortForPortType(portType, false);

                    if (port != -1) {
                        outportMap.put(nodePort, new Integer(port));
                    }
                } else {
                    port = portI.intValue();
                }
            } else {
                port = nidConnectionManifest.consumePortForPortType(portType, true);
            }

            if (port == -1) {
                throw new IllegalStateException("Node in drag had no " + (input ? "input" : "output")
                    + " ports of type " + portType + " available.");
            }

            m_otherPort = input ? connection.getSourcePort() : connection.getDestPort();
            m_otherNodeId = input ? connection.getSource() : connection.getDest();

            m_originalConnectionUIInfo = connection.getUIInfo();

            m_nodeInDragPort = port;

            m_inputConnection = input;
        }

        /**
         * @return the m_originalConnectionUIInfo
         */
        ConnectionUIInformation getOriginalUIInfo() {
            return m_originalConnectionUIInfo;
        }

        /**
         * @return the m_otherPort
         */
        int getOtherPort() {
            return m_otherPort;
        }

        /**
         * @return the m_otherNodeId
         */
        NodeID getOtherNodeId() {
            return m_otherNodeId;
        }

        /**
         * @return the m_nodeInDragPort
         */
        int getNodeInDragPort() {
            return m_nodeInDragPort;
        }

        /**
         * @return the m_inputConnection
         */
        boolean isInputConnection() {
            return m_inputConnection;
        }
    }
}
