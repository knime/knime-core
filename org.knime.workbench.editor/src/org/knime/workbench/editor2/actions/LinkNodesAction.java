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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeInPortUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.LinkNodesCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This provides the 'action' for connecting two or more nodes.
 *
 * @author loki der quaeler
 */
public class LinkNodesAction extends AbstractNodeAction {

    /** actions registry ID for this action. **/
    public static final String ID = "knime.actions.linknodes";
    /** org.eclipse.ui.commands command ID for this action. **/
    private static final String COMMAND_ID = "knime.commands.linknodes";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LinkNodesAction.class);
    private static final NodeSpatialComparator SPATIAL_COMPARATOR = new NodeSpatialComparator();

    /**
     * This method determines whether the UI representation of the two nodes have an overlap in the x-domain.
     *
     * @param node1 the first node, must not be <code>null</code>
     * @param node2 the second node, must not be <code>null</code>
     * @return <code>true</code> is the two nodes overlap in the x-domain, <code>false</code> otherwise
     */
   protected static boolean nodesOverlapInXDomain(final NodeContainerUI node1, final NodeContainerUI node2) {
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
     * Constructs an instance of <code>LinkNodesAction</code>.
     *
     * @param editor the workflow editor which this action will work within
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
    public String getText() {
        return "Link selected nodes\t" + getHotkey(COMMAND_ID);
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
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/link_nodes_disabled.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Link selected nodes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        final WorkflowManager wm = getManager();
        try (WorkflowLock lock = wm.lock()) {
            final Collection<PlannedConnection> plan = generateConnections(nodeParts);
            final LinkNodesCommand command = new LinkNodesCommand(plan, wm);

            execute(command);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code> if we have:
     *                  • at least two nodes which do not have the same left spatial location
     *                  • that the left most node has at least 1 outport
     *                  • that the right most node has at least 1 inport
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        final NodeContainerEditPart[] selected = getSelectedParts(NodeContainerEditPart.class);
        WorkflowManager manager = getManager();

        if (selected.length < 2) {
            return false;
        }

        try (WorkflowLock lock = manager.lock()) {
            // disable if there are nodes that are not (= no longer) contained in the workflow
            // this is a very common case when nodes get deleted and actions in the toolbar are
            // updated in response to that
            if (!Arrays.stream(selected).map(NodeContainerEditPart::getNodeContainer) //
                .map(NodeContainerUI::getID) //
                .anyMatch(manager::containsNodeContainer)) {
                return false;
            }

            final ScreenedSelectionSet sss = screenNodeSelection(selected);

            return sss.setIsConnectable();
        }

    }

    /**
     * This examines the set of nodes provided and creates a list of connections to be performed taking in to account
     * the spatial layout of the nodes and their available ports.
     *
     * @param nodes the set of selected nodes for which connections will be generated
     * @return This returns a collection of PlannedConnection instances which can then be "executed" to form the
     *         resulting connected subset of nodes. The ordering of the connection events is unimportant and so is
     *         returned as a Collection to emphasize that.
     */
    protected Collection<PlannedConnection> generateConnections(final NodeContainerEditPart[] nodes) {
        final ArrayList<PlannedConnection> plannedConnections = new ArrayList<>();
        final ScreenedSelectionSet sss = screenNodeSelection(nodes);

        // Should never be the case since this method is only ever called as part of run, which can't
        //      execute unless we're enabled, the crux of which already checks this condition.
        if (!sss.setIsConnectable()) {
            return plannedConnections;
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
        final boolean[] hasIncomingPlanned = new boolean[orderedNodes.size() - 1]; // indices are shifted by -1

        for (int i = 0; i < (orderedNodes.size() - 1); i++) {
            final NodeContainerUI sourceNode = orderedNodes.get(i);
            final int sourcePortStart = (sourceNode instanceof WorkflowManagerUI) ? 0 : 1;
            int currentIndex = i + 1;

            if (sourceNode.getNrOutPorts() > sourcePortStart) {
                while (currentIndex < orderedNodes.size()) {
                    final NodeContainerUI destinationNode = orderedNodes.get(currentIndex);
                    final int destinationPortStart = (destinationNode instanceof WorkflowManagerUI) ? 0 : 1;

                    if ((destinationNode.getNrInPorts() > destinationPortStart)
                        && (!nodesOverlapInXDomain(sourceNode, destinationNode)
                            && (!nodeHasConnectionWithinSet(destinationNode, orderedNodes)))) {
                        final Optional<PlannedConnection> pc =
                            createConnectionPlan(sourceNode, destinationNode, plannedConnections);

                        if (pc.isPresent()) {
                            hasIncomingPlanned[currentIndex - 1] = true;
                            plannedConnections.add(pc.get());
                            break;
                        }
                    }

                    currentIndex++;
                }
            }
        }

        /**
         * Now, find any with inports that are not already connected in the plan and that are not the first node or have
         * the same X location as the first node and connect to the first previous node which has an outport of the
         * appropriate port type.
         *
         * Such situations may arise due certain spatial configurations.
         */
        final int zereothX = orderedNodes.get(0).getUIInformation().getBounds()[0];
        for (int i = 1; i < orderedNodes.size(); i++) {
            final NodeContainerUI destinationNode = orderedNodes.get(i);

            if (!hasIncomingPlanned[i - 1] && (destinationNode.getUIInformation().getBounds()[0] > zereothX)) {
                final int destinationPortStart = (destinationNode instanceof WorkflowManagerUI) ? 0 : 1;

                if (destinationNode.getNrInPorts() > destinationPortStart) {
                    for (int j = (i - 1); j >= 0; j--) {
                        final NodeContainerUI sourceNode = orderedNodes.get(j);
                        final int sourcePortStart = (sourceNode instanceof WorkflowManagerUI) ? 0 : 1;

                        if (sourceNode.getNrOutPorts() > sourcePortStart) {
                            final Optional<PlannedConnection> pc =
                                createConnectionPlan(sourceNode, destinationNode, plannedConnections);

                            if (pc.isPresent()) {
                                // there's no existing reason to set this to true (it's not used afterwards)
                                //      but i'm a fan of consistent state
                                hasIncomingPlanned[i - 1] = true;
                                plannedConnections.add(pc.get());
                                break;
                            }
                        }
                    }
                }
            }
        }

        return plannedConnections;
    }

    /**
     * This is a variant on the logic that is in AbstractCreateNewConnectedNodeCommand.getMatchingPorts(...) - consider
     * making a generalized variation of that method accepting an instance of WorkflowManager as method parameter, and
     * putting that into a utilities class and potentially changing this to act as a second round processing on the
     * returned Map.
     *
     * @param source the source, traditionally spatially left, node
     * @param destination the destination, traditionally spatially right, node
     * @param existingPlan the already existing planned connections
     * @return an instance of PlannedConnection as an Optional, by request; this takes into account port types. The
     *         priority is the first (in natural ordering) unused and unplanned port on source and desination side of
     *         match port types; if no such connection possibility exists, then the first unplanned port on each side of
     *         matching port types. If no connection plan could be determined under these rules, an empty() Optional is
     *         returned.
     */
    protected Optional<PlannedConnection> createConnectionPlan(final NodeContainerUI source,
        final NodeContainerUI destination, final List<PlannedConnection> existingPlan) {
        final WorkflowManager wm = getManager();
        final int sourceStartIndex = (source instanceof WorkflowManagerUI) ? 0 : 1;
        final int sourcePortCount = source.getNrOutPorts();
        final int destinationStartIndex = (destination instanceof WorkflowManagerUI) ? 0 : 1;
        final int destinationPortCount = destination.getNrInPorts();
        final Set<ConnectionContainer> existingOutConnections = wm.getOutgoingConnectionsFor(source.getID());
        final Set<ConnectionContainer> existingInConnections = wm.getIncomingConnectionsFor(destination.getID());

        for (int i = sourceStartIndex; i < sourcePortCount; i++) {
            if (!portAlreadyHasConnection(source, i, false, existingPlan, existingOutConnections)) {
                final NodeOutPortUI sourcePort = source.getOutPort(i);
                final PortType sourcePortType = sourcePort.getPortType();

                for (int j = destinationStartIndex; j < destinationPortCount; j++) {
                    if (!portAlreadyHasConnection(destination, j, true, existingPlan, existingInConnections)) {
                        final NodeInPortUI destinationPort = destination.getInPort(j);
                        final PortType destinationPortType = destinationPort.getPortType();

                        if (sourcePortType.isSuperTypeOf(destinationPortType)
                            || destinationPortType.isSuperTypeOf(sourcePortType)) {
                            return Optional.of(new PlannedConnection(source, i, destination, j));
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
            if (!portAlreadyHasConnection(source, i, false, existingPlan, Collections.emptySet())) {
                final NodeOutPortUI sourcePort = source.getOutPort(i);
                final PortType sourcePortType = sourcePort.getPortType();

                for (int j = destinationStartIndex; j < destinationPortCount; j++) {
                    if (!portAlreadyHasConnection(destination, j, true, existingPlan, existingInConnections)) {
                        final NodeInPortUI destinationPort = destination.getInPort(j);
                        final PortType destinationPortType = destinationPort.getPortType();

                        if (sourcePortType.isSuperTypeOf(destinationPortType)
                            || destinationPortType.isSuperTypeOf(sourcePortType)) {
                            return Optional.of(new PlannedConnection(source, i, destination, j));
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
            final NodeOutPortUI sourcePort = source.getOutPort(i);
            final PortType sourcePortType = sourcePort.getPortType();

            for (int j = destinationStartIndex; j < destinationPortCount; j++) {
                if (!portAlreadyHasConnection(destination, j, true, existingPlan, Collections.emptySet())) {
                    final NodeInPortUI destinationPort = destination.getInPort(j);
                    final PortType destinationPortType = destinationPort.getPortType();

                    if (sourcePortType.isSuperTypeOf(destinationPortType)
                        || destinationPortType.isSuperTypeOf(sourcePortType)) {
                        final boolean mustDetach = portAlreadyHasConnection(destination, j, true,
                            Collections.emptyList(), existingInConnections);

                        return Optional.of(new PlannedConnection(source, i, destination, j, mustDetach));
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * This detrmines whether we are already tracking (either via a planned connection, or a connection already existant
     * in the workflow) a connection of the given port of the given node.
     *
     * @param node the node which either the source or destination of this connection (as clarified via the
     *            <code>inport</code> parameter value.) This value is ignored if existingPlan is null
     * @param port the port index for the connection in question
     * @param inport <code>true</code> if we're considering the inport side (destination) of the connection, false for
     *            the outport
     * @param existingPlan this will be consulted in the existence determination; must be non-null.
     * @param existingConnections this will be consulted in the existence determination; must be non-null.
     * @return true if the port,inport couplet exists in the existing connections set and <code>false</code> otherwise.
     * @throws IllegalArgumentException if either existingPlan or existingConnections is null
     */
    protected boolean portAlreadyHasConnection(final NodeContainerUI node, final int port, final boolean inport,
        final List<PlannedConnection> existingPlan, final Set<ConnectionContainer> existingConnections) {
        if (existingPlan == null) {
            throw new IllegalArgumentException("existingPlan cannot be null.");
        }
        if (existingConnections == null) {
            throw new IllegalArgumentException("existingConnections cannot be null.");
        }

        for (final ConnectionContainer cc : existingConnections) {
            if ((inport && (cc.getDestPort() == port)) || ((!inport) && (cc.getSourcePort() == port))) {
                return true;
            }
        }

        final NodeID nid = node.getID();

        for (final PlannedConnection pc : existingPlan) {
            if ((inport && pc.getDestinationNode().getID().equals(nid) && (pc.getDestinationInportIndex() == port))
                || ((!inport) && pc.getSourceNode().getID().equals(nid) && (pc.getSourceOutportIndex() == port))) {
                return true;
            }
        }

        return false;
    }

    /**
     * This method determines whether the specified node has a connection to one of its imports by a node contained in
     * the <code>set</code>.
     *
     * @param node a node, must not be <code>null</code>
     * @param set a set of nodes, must not be <code>null</code>
     * @return <code>true</code> if the specified node already has an inport connected to the outport of a node in the
     *         set, <code>false</code> otherwise
     */
    protected boolean nodeHasConnectionWithinSet(final NodeContainerUI node, final Collection<NodeContainerUI> set) {
        final WorkflowManager wm = getManager();
        final Set<ConnectionContainer> incoming = wm.getIncomingConnectionsFor(node.getID());

        if (!incoming.isEmpty()) {
            for (ConnectionContainer cc : incoming) {
                final NodeID nid = cc.getSource();

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
     * We desire this method in particular to be as computationally unintensive as possible as it is called per node
     * selection change; unfortunately in addition to spatial arrangement and port cardinality, we need also gate upon
     * port type which brings this functionality ever closer to the actual full plan creation logic performed at action
     * execution time.
     *
     * This performs the first pass screening which consists of determining the spatially leftmost and rightmost nodes
     * in the selection, that are valid, and discards any invalid nodes (nodes which are spatially leftmost but have
     * only inports or are spatially rightmost but have only outports.)
     *
     * @param nodes the current selection set of nodes in the workflow editor
     * @return an instance of ScreenedSelectionSet
     */
    protected ScreenedSelectionSet screenNodeSelection(final NodeContainerEditPart[] nodes) {
        final ArrayList<NodeContainerUI> validLeft = new ArrayList<>();
        final ArrayList<NodeContainerUI> validRight = new ArrayList<>();
        final WorkflowManager wm = getManager();

        for (int i = 0; i < nodes.length; i++) {
            final NodeContainerUI node = nodes[i].getNodeContainer();

            // We are not guaranteed that the WorkflowManager state still contains these nodes, so check.
            if (wm.containsNodeContainer(node.getID())) {
                final int portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
                final boolean canBeLeftMost = (node.getNrOutPorts() > portLowWaterMark);
                final boolean canBeRightMost = (node.getNrInPorts() > portLowWaterMark);

                if (canBeLeftMost) {
                    validLeft.add(node);
                }

                if (canBeRightMost) {
                    validRight.add(node);
                }
            }
        }

        if (validLeft.isEmpty() || validRight.isEmpty()) {
            return new ScreenedSelectionSet(Collections.emptySet(), null, null);
        }

        try {
            Collections.sort(validLeft, SPATIAL_COMPARATOR);
            Collections.sort(validRight, SPATIAL_COMPARATOR);
        } catch (NullPointerException e) {
            // This sneaky way assures us that we have UI bounds information for the rest of this screening code
            LOGGER.warn("Some nodes in the current selection returned invalid UI information.");

            return new ScreenedSelectionSet(Collections.emptySet(), null, null);
        }

        final NodeContainerUI[] spatialBounds = new NodeContainerUI[2];
        spatialBounds[0] = validLeft.get(0);
        spatialBounds[1] = validRight.get(validRight.size() - 1);

        final ArrayList<NodeContainerUI> discards = new ArrayList<>();
        NodeUIInformation uiInfo = spatialBounds[1].getUIInformation();
        for (int i = (validLeft.size() - 1); i >= 0; i--) {
            final NodeContainerUI node = validLeft.get(i);
            final NodeUIInformation uiInfoToo = node.getUIInformation();

            if (node == spatialBounds[1]) {
                continue;
            }

            if (uiInfoToo.getBounds()[0] < uiInfo.getBounds()[0]) {
                break;
            }

            final int portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
            // It's not clear whether there may ever be a node which doesn't have a flow input, <= to be sure
            if (node.getNrInPorts() <= portLowWaterMark) {
                discards.add(node);
            }
        }
        validLeft.removeAll(discards);

        discards.clear();
        uiInfo = spatialBounds[0].getUIInformation();
        for (int i = (validRight.size() - 1); i >= 0; i--) {
            final NodeContainerUI node = validRight.get(i);
            final NodeUIInformation uiInfoToo = node.getUIInformation();

            if (node == spatialBounds[0]) {
                continue;
            }

            if (uiInfoToo.getBounds()[0] > uiInfo.getBounds()[0]) {
                break;
            }

            final int portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
            // It's not clear whether there may ever be a node which doesn't have a flow output, <= to be sure
            if (node.getNrOutPorts() <= portLowWaterMark) {
                discards.add(node);
            }
        }
        validRight.removeAll(discards);


        // We should consider changing this to use an approach with maps should the common use case ever become scores
        //  of nodes being selected - this is currently a poly-time solution.
        boolean haveFoundALegalConnection = false;
        for (final NodeContainerUI left : validLeft) {
            final int sourcePortStart = (left instanceof WorkflowManagerUI) ? 0 : 1;
            final NodeUIInformation leftUIInfo = left.getUIInformation();

            for (int i = sourcePortStart; (i < left.getNrOutPorts()) && (!haveFoundALegalConnection); i++) {
                final NodeOutPortUI sourcePort = left.getOutPort(i);
                final PortType sourcePortType = sourcePort.getPortType();

                for (final NodeContainerUI right : validRight) {
                    if (left != right) {
                        final NodeUIInformation rightUIInfo = right.getUIInformation();

                        if (leftUIInfo.getBounds()[0] < rightUIInfo.getBounds()[0]) {
                            final int destinationPortStart = (right instanceof WorkflowManagerUI) ? 0 : 1;

                            for (int j = destinationPortStart; j < right.getNrInPorts(); j++) {
                                final NodeInPortUI destinationPort = right.getInPort(j);
                                final PortType destinationPortType = destinationPort.getPortType();

                                if (sourcePortType.isSuperTypeOf(destinationPortType)
                                    || destinationPortType.isSuperTypeOf(sourcePortType)) {
                                    haveFoundALegalConnection = true;

                                    break;
                                }
                            }

                            if (haveFoundALegalConnection) {
                                break;
                            }
                        }
                    }
                }
            }

            if (haveFoundALegalConnection) {
                break;
            }
        }

        if (!haveFoundALegalConnection) {
            return new ScreenedSelectionSet(Collections.emptySet(), null, null);
        }


        final HashSet<NodeContainerUI> connectableNodes = new HashSet<>();
        // cramming them into a HashSet rids us of the problem that there will usually be a non-null intersection
        //  between the valid left and valid right sets of nodes.
        connectableNodes.addAll(validLeft);
        connectableNodes.addAll(validRight);

        return new ScreenedSelectionSet(connectableNodes, spatialBounds[0], spatialBounds[1]);
    }


    static class ScreenedSelectionSet {
        private final List<NodeContainerUI> m_connectableNodes;
        private final NodeContainerUI m_spatiallyLeftMostNode;
        private final NodeContainerUI m_spatiallyRightMostNode;

        ScreenedSelectionSet(final Collection<NodeContainerUI> connectables, final NodeContainerUI left,
            final NodeContainerUI right) {
            m_connectableNodes = new ArrayList<>(connectables);
            m_spatiallyLeftMostNode = left;
            m_spatiallyRightMostNode = right;

            Collections.sort(m_connectableNodes, SPATIAL_COMPARATOR);
        }

        boolean setIsConnectable() {
            return ((m_spatiallyLeftMostNode != null) && (m_spatiallyRightMostNode != null)
                && (m_spatiallyLeftMostNode != m_spatiallyRightMostNode) && (m_connectableNodes.size() > 1));
        }

        /**
         * @return a spatially in-order list of connectable nodes
         * @see NodeSpatialComparator
         */
        List<NodeContainerUI> getConnectableNodes() {
            return m_connectableNodes;
        }
    }


    /**
     * This class is a close representation of the ConnectionContainer class; that plus its limited use gives me pause
     * in extracting this out as its own first class class.
     *
     * @author loki der quaeler
     */
    public static class PlannedConnection {
        private final NodeContainerUI m_sourceNode;
        private final int m_sourceOutportIndex;

        private final NodeContainerUI m_destinationNode;
        private final int m_destinationInportIndex;

        private final boolean m_detachDestinationFirst;

        PlannedConnection(final NodeContainerUI source, final int sourcePort, final NodeContainerUI destination,
            final int destinationPort) {
            this(source, sourcePort, destination, destinationPort, false);
        }

        PlannedConnection(final NodeContainerUI source, final int sourcePort, final NodeContainerUI destination,
            final int destinationPort, final boolean destinationRequiresDetachEvent) {
            m_sourceNode = source;
            m_sourceOutportIndex = sourcePort;

            m_destinationNode = destination;
            m_destinationInportIndex = destinationPort;

            m_detachDestinationFirst = destinationRequiresDetachEvent;
        }

        /**
         * @return the sourceNode
         */
        public NodeContainerUI getSourceNode() {
            return m_sourceNode;
        }

        /**
         * @return the sourceOutportIndex
         */
        public int getSourceOutportIndex() {
            return m_sourceOutportIndex;
        }

        /**
         * @return the destinationNode
         */
        public NodeContainerUI getDestinationNode() {
            return m_destinationNode;
        }

        /**
         * @return the destinationInportIndex
         */
        public int getDestinationInportIndex() {
            return m_destinationInportIndex;
        }

        /**
         * @return the detachDestinationFirst
         */
        public boolean shouldDetachDestinationFirst() {
            return m_detachDestinationFirst;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_sourceNode.getNameWithID() + ":" + m_sourceOutportIndex + " -> "
                + m_destinationNode.getNameWithID() + ":" + m_destinationInportIndex
                + (m_detachDestinationFirst ? " [DETACH]" : "");
        }
    }


    /**
     * This orders ascending first by x-coordinate and second by y-coordinate.
     */
    static class NodeSpatialComparator implements Comparator<NodeContainerUI> {
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
