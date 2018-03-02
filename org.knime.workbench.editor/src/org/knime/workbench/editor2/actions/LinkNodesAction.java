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
 *   Mar 4, 2018 (loki): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeInPortUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This provides the 'action' for connecting two or more nodes.
 */
public class LinkNodesAction extends AbstractNodeAction {

    /** org.eclipse.ui.commands command ID for this action. **/
    static public final String ID = "knime.commands.linknodes";

    static private final NodeLogger LOGGER = NodeLogger.getLogger(LinkNodesAction.class);
    static private final NodeSpatialComparator SPATIAL_COMPARATOR = new NodeSpatialComparator();


    /**
     * @param editor The workflow editor which this action will work within
     */
    public LinkNodesAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/link_nodes.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        final Collection<PlannedConnection> plan = this.generateConnections(nodeParts);
        final WorkflowManager wm = this.getManager();
        NodeContainer sourceNode;
        NodeID sourceNodeID;
        NodeContainer destinationNode;
        NodeID destinationNodeID;
        ConnectionContainer cc;

        for (final PlannedConnection planAction : plan) {
            sourceNodeID = planAction.getSourceNode().getID();
            sourceNode = wm.getNodeContainer(sourceNodeID);
            destinationNodeID = planAction.getDestinationNode().getID();
            destinationNode = wm.getNodeContainer(destinationNodeID);

            if (planAction.shouldDetachDestinationFirst()) {
                cc = wm.getIncomingConnectionFor(destinationNodeID, planAction.getDestinationInportIndex());

                try {
                    wm.removeConnection(cc);
                }
                catch (Exception e) {
                    LOGGER.error("Could not delete existing inport connection for "
                                    + destinationNodeID + ":" + planAction.getDestinationInportIndex()
                                    + "; skipping new connection task from " + sourceNodeID + ":"
                                    + planAction.getSourceOutportIndex() + " to " + destinationNodeID + ":"
                                    + planAction.getDestinationInportIndex(),
                                 e);

                    continue;
                }
            }

            try {
                wm.addConnection(sourceNodeID, planAction.getSourceOutportIndex(),
                                 destinationNodeID, planAction.getDestinationInportIndex());

                NodeTimer.GLOBAL_TIMER.addConnectionCreation(sourceNode, destinationNode);
            }
            catch (Exception e) {
                LOGGER.error("Failed to connect " + sourceNodeID + ":" + planAction.getSourceOutportIndex()
                                + " to " + destinationNodeID + ":" + planAction.getDestinationInportIndex(),
                             e);
            }
        }
    }

    /**
     * @return <code>true</code> if we have:
     *                  • at least two nodes which do not have the same left spatial location
     *                  • that the left most node has at least 1 outport
     *                  • that the right most node has at least 1 inport
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        final NodeContainerEditPart[] selected = this.getSelectedParts(NodeContainerEditPart.class);

        if (selected.length < 2) {
            return false;
        }

        final ScreenedSelectionSet sss = this.screenNodeSelection(selected);

        return (sss.setIsConnectable());
    }

    /**
     *
     * @param nodes
     * @return This returns a collection of PlannedConnection instances which can then be "executed" to
     *                  form the resulting connected subset of nodes. The ordering of the connection
     *                  events is unimportant and so is returned as a Collection to emphasize that.
     */
    protected Collection<PlannedConnection> generateConnections(final NodeContainerEditPart[] nodes) {
        final ArrayList<PlannedConnection> rhett = new ArrayList<>();
        final ScreenedSelectionSet sss = this.screenNodeSelection(nodes);

        // Should never be the case since this method is only ever called as part of run, which can't
        //      execute unless we're enabled, the crux of which already checks this condition.
        if (!sss.setIsConnectable()) {
            return rhett;
        }


        /*
         * Take first node from sss, get spatially next that:
         *
         *      . has an inport
         *      . is not spatially overlapping
         *      . is not connected to the outport of another node in this set already (not considering the
         *              plan of connections to be made due to this action)
         *
         *
         * This doesn't embody the "on the same y-line" connection logic. (TODO)
         */
        final List<NodeContainerUI> orderedNodes = sss.getConnectableNodes();
        NodeContainerUI sourceNode = null;
        NodeContainerUI destinationNode;
        boolean[] hasDestination = new boolean[orderedNodes.size() - 1]; // indices are shifted by -1
        int currentIndex;
        int portLowWaterMark;

        for (int i = 0; i < (orderedNodes.size() - 1); i++) {
            currentIndex = i + 1;

            sourceNode = orderedNodes.get(i);
            portLowWaterMark = (sourceNode instanceof WorkflowManagerUI) ? 0 : 1;
            if (sourceNode.getNrOutPorts() > portLowWaterMark) {
                while (currentIndex < orderedNodes.size()) {
                    destinationNode = orderedNodes.get(currentIndex);
                    portLowWaterMark = (destinationNode instanceof WorkflowManagerUI) ? 0 : 1;

                    if ((destinationNode.getNrInPorts() > portLowWaterMark)
                                        && (!this.nodesOverlapInXDomain(sourceNode, destinationNode)
                                        && (!this.nodeHasConnectionWithinSet(destinationNode,
                                                                             orderedNodes)))) {
                        final PlannedConnection pc = this.createConnectionPlan(sourceNode, destinationNode,
                                                                               rhett);

                        if (pc != null) {
                            hasDestination[currentIndex - 1] = true;
                            rhett.add(pc);

                            break;
                        }
                    }

                    currentIndex++;
                }
            }
        }

        /*
         * Now, find any with inports that are not connected in the plan and that are not the first node;
         *  connect to the first previous node which has an outport of the appropriate port type.
         *
         * Such situations may arise due certain spatial configurations.
         */
        for (int i = 1; i < orderedNodes.size(); i++) {
            if (!hasDestination[i - 1]) {
                destinationNode = orderedNodes.get(i);
                portLowWaterMark = (destinationNode instanceof WorkflowManagerUI) ? 0 : 1;

                if (destinationNode.getNrInPorts() > portLowWaterMark) {
                    for (int j = (i - 1); j >= 0; j--) {
                        sourceNode = orderedNodes.get(j);
                        portLowWaterMark = (sourceNode instanceof WorkflowManagerUI) ? 0 : 1;

                        if (sourceNode.getNrOutPorts() > portLowWaterMark) {
                            final PlannedConnection pc = this.createConnectionPlan(sourceNode,
                                                                                   destinationNode,
                                                                                   rhett);

                            if (pc != null) {
                                // there's no existing reason to set this to true (it's not used afterwards)
                                //      but i'm a fan of consistent state
                                hasDestination[i - 1] = true;
                                rhett.add(pc);

                                break;
                            }
                        }
                    }
                }
            }
        }

        return rhett;
    }

    /**
     * This is a variant on the logic that is in AbstractCreateNewConnectedNodeCommand.getMatchingPorts(...) -
     *  consider making a generalized variation of that method accepting an instance of WorkflowManager as
     *  method parameter, and putting that into a utilities class and potentially changing this to
     *  act as a second round processing on the returned Map.
     *
     * @param source the source, traditionally spatially left, node
     * @param destination the destination, traditionally spatially right, node
     * @param existingPlan the already existing planned connections
     * @return an instance of PlannedConnection; this takes into account port types. The priority is the
     *              first (in natural ordering) unused and unplanned port on source and desination side of
     *              match port types; if no such connection possibility exists, then the first unplanned port
     *              on each side of matching port types. If no connection plan could be determined under
     *              these rules, a null is returned.
     */
    protected PlannedConnection createConnectionPlan(final NodeContainerUI source,
                                                     final NodeContainerUI destination,
                                                     final List<PlannedConnection> existingPlan) {
        final WorkflowManager wm = this.getManager();
        final int sourceStartIndex = (source instanceof WorkflowManagerUI) ? 0 : 1;
        final int sourcePortCount = source.getNrOutPorts();
        final int destinationStartIndex = (destination instanceof WorkflowManagerUI) ? 0 : 1;
        final int destinationPortCount = destination.getNrInPorts();
        final Set<ConnectionContainer> existingOutConnections = wm.getOutgoingConnectionsFor(source.getID());
        final Set<ConnectionContainer> existingInConnections
                                                         = wm.getIncomingConnectionsFor(destination.getID());
        NodeOutPortUI sourcePort;
        PortType sourcePortType;
        NodeInPortUI destinationPort;
        PortType destinationPortType;

        for (int i = sourceStartIndex; i < sourcePortCount; i++) {
            if (!this.portAlreadyHasConnection(source, i, false, existingPlan, existingOutConnections)) {
                sourcePort = source.getOutPort(i);
                sourcePortType = sourcePort.getPortType();

                for (int j = destinationStartIndex; j < destinationPortCount; j++) {
                    if (!this.portAlreadyHasConnection(destination, j, true, existingPlan,
                                                       existingInConnections)) {
                        destinationPort = destination.getInPort(j);
                        destinationPortType = destinationPort.getPortType();

                        if (sourcePortType.isSuperTypeOf(destinationPortType)) {
                            return new PlannedConnection(source, i, destination, j);
                        }
                    }
                }
            }
        }


        /*
         * If we've made it to here, nothing was found in, taking "available existing" on source and
         *  destination into account; try again ignoring existing on source.
         */
        for (int i = sourceStartIndex; i < sourcePortCount; i++) {
            if (!this.portAlreadyHasConnection(source, i, false, existingPlan, null)) {
                sourcePort = source.getOutPort(i);
                sourcePortType = sourcePort.getPortType();

                for (int j = destinationStartIndex; j < destinationPortCount; j++) {
                    if (!this.portAlreadyHasConnection(destination, j, true, existingPlan,
                                                       existingInConnections)) {
                        destinationPort = destination.getInPort(j);
                        destinationPortType = destinationPort.getPortType();

                        if (sourcePortType.isSuperTypeOf(destinationPortType)) {
                            return new PlannedConnection(source, i, destination, j);
                        }
                    }
                }
            }
        }



        /*
         * If we've made it to here, nothing was found in, taking "available existing" on source and
         *  destination into account, and also ignoring existing on source but not destination; now
         *  just try only taking the existing plan into affect and allowing for multiple outport
         *  assignments as a last ditch effort.
         */
        for (int i = sourceStartIndex; i < sourcePortCount; i++) {
            sourcePort = source.getOutPort(i);
            sourcePortType = sourcePort.getPortType();

            for (int j = destinationStartIndex; j < destinationPortCount; j++) {
                if (!this.portAlreadyHasConnection(destination, j, true, existingPlan, null)) {
                    destinationPort = destination.getInPort(j);
                    destinationPortType = destinationPort.getPortType();

                    if (sourcePortType.isSuperTypeOf(destinationPortType)) {
                        final boolean mustDetach = this.portAlreadyHasConnection(destination, j, true, null,
                                                                                 existingInConnections);

                        return new PlannedConnection(source, i, destination, j, mustDetach);
                    }
                }
            }
        }


        return null;
    }

    /**
     * @param node the node which either the source or destination of this connection (as clarified via
     *                  the <code>inport</code> parameter value.) This value is ignored if existingPlan
     *                  is null
     * @param port
     * @param inport true if we're considering the inport side (destination) of the connection,
     *                  false for the outport
     * @param existingPlan if non-null, these will be consulted in the existence determination
     * @param existingConnections if non-null, these will be consulted in the existence determination
     * @return true if the port,inport couplet exists in the existing connections set, false otherwise.
     */
    protected boolean portAlreadyHasConnection (final NodeContainerUI node,
                                                final int port, final boolean inport,
                                                final List<PlannedConnection> existingPlan,
                                                final Set<ConnectionContainer> existingConnections) {
        if (existingConnections != null) {
            for (final ConnectionContainer cc : existingConnections) {
                if ((inport && (cc.getDestPort() == port)) || ((!inport) && (cc.getSourcePort() == port))) {
                    return true;
                }
            }
        }

        if (existingPlan != null) {
            final NodeID nid = node.getID();

            for (final PlannedConnection pc : existingPlan) {
                if ((inport
                            && pc.getDestinationNode().getID().equals(nid)
                            && (pc.getDestinationInportIndex() == port))
                     || ((!inport)
                            && pc.getSourceNode().getID().equals(nid)
                            && (pc.getSourceOutportIndex() == port))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param node1
     * @param node2
     * @return true is the two nodes overlap in the x-domain
     */
    protected boolean nodesOverlapInXDomain(final NodeContainerUI node1, final NodeContainerUI node2) {
        final NodeUIInformation ui1 = node1.getUIInformation();
        final NodeUIInformation ui2 = node2.getUIInformation();
        final int[] bounds1 = ui1.getBounds();
        final int[] bounds2 = ui2.getBounds();
        final int node1x2 = bounds1[0] + bounds1[2];
        final int node2x2 = bounds2[0] + bounds2[2];

        return (((bounds1[0] <= bounds2[0]) && (bounds2[0] <= node1x2))
                    || ((bounds2[0] <= bounds1[0]) && (bounds1[0] <= node2x2)));
    }

    /**
     * @param node
     * @param set
     * @return true if the specified node already has an inport connected to the outport of a node in the set
     */
    protected boolean nodeHasConnectionWithinSet(final NodeContainerUI node,
                                                 final Collection<NodeContainerUI> set) {
        final WorkflowManager wm = this.getManager();
        final Set<ConnectionContainer> incoming = wm.getIncomingConnectionsFor(node.getID());

        if (incoming.size() > 0) {
            NodeID nid;

            for (ConnectionContainer cc : incoming) {
                nid = cc.getSource();

                for (NodeContainerUI setItem : set) {
                    if (nid.equals(setItem.getID())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * This performs the first pass screening which consists of determining the spatially leftmost and
     *  rightmost nodes in the selection, that are valid, and discards any invalid nodes (nodes which
     *  are spatially leftmost but have only inports or are spatially rightmost but have only outports.)
     *
     * @param nodes the current selection set of nodes in the workflow editor
     * @return an instance of ScreenedSelectionSet
     */
    protected ScreenedSelectionSet screenNodeSelection(final NodeContainerEditPart[] nodes) {
        final NodeContainerUI[] spatialBounds = new NodeContainerUI[2];
        final ArrayList<NodeContainerUI> validLeft = new ArrayList<>();
        final ArrayList<NodeContainerUI> validRight = new ArrayList<>();
        ArrayList<NodeContainerUI> discards;
        NodeContainerUI node;
        NodeUIInformation uiInfo;
        NodeUIInformation uiInfoToo;
        boolean canBeLeftMost;
        boolean canBeRightMost;
        int portLowWaterMark;

        for (int i = 0; i < nodes.length; i++) {
            node = nodes[i].getNodeContainer();
            portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;

            canBeLeftMost = (node.getNrOutPorts() > portLowWaterMark);
            canBeRightMost = (node.getNrInPorts() > portLowWaterMark);

            if (canBeLeftMost) {
                validLeft.add(node);
            }

            if (canBeRightMost) {
                validRight.add(node);
            }
        }

        final HashSet<NodeContainerUI> connectableNodes = new HashSet<>();
        if ((validLeft.size() == 0) || (validRight.size() == 0)) {
            return new ScreenedSelectionSet(connectableNodes, null, null);
        }

        try {
            Collections.sort(validLeft, SPATIAL_COMPARATOR);
            Collections.sort(validRight, SPATIAL_COMPARATOR);
        }
        catch (NullPointerException e) {
            // This sneaky way assures us that we have UI bounds information for the rest of this screening
            //          code
            LOGGER.warn("Some nodes in the current selection returned invalid UI information.");

            return new ScreenedSelectionSet(connectableNodes, null, null);
        }

        spatialBounds[0] = validLeft.get(0);
        spatialBounds[1] = validRight.get(validRight.size() - 1);

        discards = new ArrayList<>();
        uiInfo = spatialBounds[1].getUIInformation();
        for (int i = (validLeft.size() - 1); i >= 0; i--) {
            node = validLeft.get(i);
            uiInfoToo = node.getUIInformation();

            if (uiInfoToo.getBounds()[0] < uiInfo.getBounds()[0]) {
                break;
            }

            portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
            if (node.getNrOutPorts() == portLowWaterMark) {
                discards.add(node);
            }
        }
        validLeft.removeAll(discards);

        discards = new ArrayList<>();
        uiInfo = spatialBounds[0].getUIInformation();
        for (int i = (validRight.size() - 1); i >= 0; i--) {
            node = validRight.get(i);
            uiInfoToo = node.getUIInformation();

            if (uiInfoToo.getBounds()[0] > uiInfo.getBounds()[0]) {
                break;
            }

            portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
            if (node.getNrInPorts() == portLowWaterMark) {
                discards.add(node);
            }
        }
        validRight.removeAll(discards);

        // cramming them into a HashSet rids us of the problem that there will usually be a non-null
        //      intersection between the valid left and valid right sets of nodes.
        connectableNodes.addAll(validLeft);
        connectableNodes.addAll(validRight);

        return new ScreenedSelectionSet(connectableNodes, spatialBounds[0], spatialBounds[1]);
    }


    static private class ScreenedSelectionSet {

        final private List<NodeContainerUI> connectableNodes;
        final private NodeContainerUI spatiallyLeftMostNode;
        final private NodeContainerUI spatiallyRightMostNode;

        private ScreenedSelectionSet(final Collection<NodeContainerUI> connectables,
                                     final NodeContainerUI left, final NodeContainerUI right) {
            this.connectableNodes = new ArrayList<>(connectables);
            this.spatiallyLeftMostNode = left;
            this.spatiallyRightMostNode = right;

            Collections.sort(this.connectableNodes, SPATIAL_COMPARATOR);
        }

        private boolean setIsConnectable() {
            return ((this.spatiallyLeftMostNode != null)
                        && (this.spatiallyRightMostNode != null)
                        && (this.connectableNodes.size() > 0));
        }

        /**
         * @return a spatially in-order list of connectable nodes
         * @see NodeSpatialComparator
         */
        private List<NodeContainerUI> getConnectableNodes() {
            return this.connectableNodes;
        }

        @SuppressWarnings("unused")  // have a feeling like this might be of use in the future
        private NodeContainerUI getSpatiallyLeftMostNode() {
            return this.spatiallyLeftMostNode;
        }

        @SuppressWarnings("unused")  // have a feeling like this might be of use in the future
        private NodeContainerUI getSpatiallyRightMostNode() {
            return this.spatiallyRightMostNode;
        }

    }


    static private class PlannedConnection {

        final private NodeContainerUI sourceNode;
        final private int sourceOutportIndex;

        final private NodeContainerUI destinationNode;
        final private int destinationInportIndex;

        final private boolean detachDestinationFirst;

        private PlannedConnection(final NodeContainerUI source, final int sourcePort,
                                  final NodeContainerUI destination, final int destinationPort) {
            this(source, sourcePort, destination, destinationPort, false);
        }

        private PlannedConnection(final NodeContainerUI source, final int sourcePort,
                                  final NodeContainerUI destination, final int destinationPort,
                                  final boolean destinationRequiresDetachEvent) {
            this.sourceNode = source;
            this.sourceOutportIndex = sourcePort;

            this.destinationNode = destination;
            this.destinationInportIndex = destinationPort;

            this.detachDestinationFirst = destinationRequiresDetachEvent;
        }

        /**
         * @return the sourceNode
         */
        private NodeContainerUI getSourceNode() {
            return this.sourceNode;
        }

        /**
         * @return the sourceOutportIndex
         */
        private int getSourceOutportIndex() {
            return this.sourceOutportIndex;
        }

        /**
         * @return the destinationNode
         */
        private NodeContainerUI getDestinationNode() {
            return this.destinationNode;
        }

        /**
         * @return the destinationInportIndex
         */
        private int getDestinationInportIndex() {
            return this.destinationInportIndex;
        }

        /**
         * @return the detachDestinationFirst
         */
        private boolean shouldDetachDestinationFirst() {
            return this.detachDestinationFirst;
        }

        @Override
        public String toString() {
            return this.sourceNode.getNameWithID() + ":" + this.sourceOutportIndex + " -> "
                        + this.destinationNode.getNameWithID() + ":" + this.destinationInportIndex
                        + (this.detachDestinationFirst ? " [DETACH]" : "");
        }

    }


    /**
     * This orders ascending first by x-coordinate and second by y-coordinate.
     */
    static private class NodeSpatialComparator
            implements Comparator<NodeContainerUI> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final NodeContainerUI node1, final NodeContainerUI node2) {
            final NodeUIInformation ui1 = node1.getUIInformation();
            final NodeUIInformation ui2 = node2.getUIInformation();
            final int[] bounds1 = ui1.getBounds();
            final int[] bounds2 = ui2.getBounds();

            if (bounds1[0] != bounds2[0]) {
                return bounds1[0] - bounds2[0];
            }

            return bounds1[1] - bounds2[1];
        }

    }

}
