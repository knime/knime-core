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
import java.util.Optional;
import java.util.Set;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeInPortUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.LinkNodesCommand;
import org.knime.workbench.editor2.editparts.ConnectableEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;

/**
 * This provides the 'action' for connecting two or more nodes.
 *
 * @author loki der quaeler
 */
public class LinkNodesAction extends AbstractLinkNodesAction {

    /** actions registry ID for this action. **/
    public static final String ID = "knime.actions.linknodes";
    /** org.eclipse.ui.commands command ID for this action. **/
    private static final String COMMAND_ID = "knime.commands.linknodes";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LinkNodesAction.class);

    private static final ConnectableSpatialComparator CONNECTABLE_SPATIAL_COMPARATOR =
        new ConnectableSpatialComparator();
    private static final BoundsSpatialComparator BOUNDS_SPATIAL_COMPARATOR = new BoundsSpatialComparator();

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
     * We're interested in returning bounds depending on the type of edit part we're dealing with (primarily
     * differentiating between workflow port bars and 'nodes.')
     *
     * @param ep the edit part for which we want UI info
     * @return the spatial bounds for the edit part, or a rectangle of [0, 0, 0, 0] if an underlying edit part of a
     *         selected metanode workflow bar has not yet been fully initialized
     */
    protected static Rectangle getBoundsForConnectable(final ConnectableEditPart ep) {
        if (ep instanceof WorkflowInPortBarEditPart) {
            final WorkflowInPortBarEditPart ipbep = (WorkflowInPortBarEditPart)ep;
            final Optional<Rectangle> bounds = ipbep.getUIBounds();

            return bounds.isPresent() ? bounds.get() : new Rectangle(0, 0, 0, 0);
        } else if (ep instanceof WorkflowOutPortBarEditPart) {
            final WorkflowOutPortBarEditPart opbep = (WorkflowOutPortBarEditPart)ep;
            final Optional<Rectangle> bounds = opbep.getUIBounds();

            return bounds.isPresent() ? bounds.get() : new Rectangle(0, 0, 0, 0);
        }

        final NodeUIInformation uiInfo = ep.getNodeContainer().getUIInformation();
        if (uiInfo != null) {
            int[] bounds = uiInfo.getBounds();

            return new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]);
        }

        return null;
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
        return "Connect selected nodes\t" + getHotkey(COMMAND_ID);
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
        return "Connect selected nodes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Command createCommandForConnectablesInManager(final ConnectableEditPart[] selected,
        final WorkflowManager wm) {
        final Collection<PlannedConnection> plan = generateConnections(selected);

        return new LinkNodesCommand(plan, wm);
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code> if we have:
     *                  • at least two nodes which do not have the same left spatial location
     *                  • that the left most node has at least 1 outport
     *                  • that the right most node has at least 1 inport
     */
    @Override
    protected boolean canEnableForConnectables(final ConnectableEditPart[] selected) {
        final ScreenedSelectionSet sss = screenNodeSelection(selected);

        return sss.setIsConnectable();
    }

    /**
     * This examines the set of nodes provided and creates a list of connections to be performed taking in to account
     * the spatial layout of the nodes and their available ports.
     *
     * @param connectables the set of selected connectables for which connections will be generated
     * @return This returns a collection of PlannedConnection instances which can then be "executed" to form the
     *         resulting connected subset of nodes. The ordering of the connection events is unimportant and so is
     *         returned as a Collection to emphasize that.
     */
    protected Collection<PlannedConnection> generateConnections(final ConnectableEditPart[] connectables) {
        final ArrayList<PlannedConnection> plannedConnections = new ArrayList<>();
        final ScreenedSelectionSet sss = screenNodeSelection(connectables);

        // Should never be the case since this method is only ever called as part of run, which can't
        //      execute unless we're enabled, the crux of which already checks this condition.
        if (!sss.setIsConnectable()) {
            return plannedConnections;
        }


        /*
         * Take first node from sss, get spatially next that:
         *
         *      • has an inport
         *      • is not spatially overlapping
         *      • is not connected to the outport of another node in this set already (not considering the
         *              plan of connections to be made due to this action)
         *
         *
         * This doesn't embody the "on the same y-line" connection logic. (TODO)
         */
        final List<ConnectableEditPart> orderedNodes = sss.getConnectableNodes();
        final boolean[] hasIncomingPlanned = new boolean[orderedNodes.size() - 1]; // indices are shifted by -1

        for (int i = 0; i < (orderedNodes.size() - 1); i++) {
            final ConnectableEditPart source = orderedNodes.get(i);
            final NodeContainerUI sourceNode = source.getNodeContainer();
            final int sourcePortStart = (sourceNode instanceof WorkflowManagerUI) ? 0 : 1;
            int currentIndex = i + 1;

            if (sourceNode.getNrOutPorts() > sourcePortStart) {
                while (currentIndex < orderedNodes.size()) {
                    final ConnectableEditPart destination = orderedNodes.get(currentIndex);
                    final NodeContainerUI destinationNode = destination.getNodeContainer();
                    final int destinationPortStart = (destinationNode instanceof WorkflowManagerUI) ? 0 : 1;

                    if ((destinationNode.getNrInPorts() > destinationPortStart)
                        && (!nodesOverlapInXDomain(sourceNode, destinationNode)
                            && (!nodeHasConnectionWithinSet(destination, orderedNodes)))) {
                        final Optional<PlannedConnection> pc =
                            createConnectionPlan(source, destination, plannedConnections);

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
        final Rectangle zereothBounds = getBoundsForConnectable(orderedNodes.get(0));
        for (int i = 1; i < orderedNodes.size(); i++) {
            final ConnectableEditPart destination = orderedNodes.get(i);
            final Rectangle destinationBounds = getBoundsForConnectable(destination);

            if (!hasIncomingPlanned[i - 1] && (destinationBounds.x > zereothBounds.x)) {
                final NodeContainerUI destinationNode = destination.getNodeContainer();
                final int destinationPortStart = (destinationNode instanceof WorkflowManagerUI) ? 0 : 1;

                if (destinationNode.getNrInPorts() > destinationPortStart) {
                    for (int j = (i - 1); j >= 0; j--) {
                        final ConnectableEditPart source = orderedNodes.get(j);
                        final NodeContainerUI sourceNode = source.getNodeContainer();
                        final int sourcePortStart = (sourceNode instanceof WorkflowManagerUI) ? 0 : 1;

                        if (sourceNode.getNrOutPorts() > sourcePortStart) {
                            final Optional<PlannedConnection> pc =
                                createConnectionPlan(source, destination, plannedConnections);

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
     * @param sourceEP the source, traditionally spatially left, node
     * @param destinationEP the destination, traditionally spatially right, node
     * @param existingPlan the already existing planned connections
     * @return an instance of PlannedConnection as an Optional, by request; this takes into account port types. The
     *         priority is the first (in natural ordering) unused and unplanned port on source and desination side of
     *         match port types; if no such connection possibility exists, then the first unplanned port on each side of
     *         matching port types. If no connection plan could be determined under these rules, an empty() Optional is
     *         returned.
     */
    protected Optional<PlannedConnection> createConnectionPlan(final ConnectableEditPart sourceEP,
        final ConnectableEditPart destinationEP, final List<PlannedConnection> existingPlan) {
        final WorkflowManager wm = getManager();
        final NodeContainerUI source = sourceEP.getNodeContainer();
        final NodeContainerUI destination = destinationEP.getNodeContainer();
        final int sourceStartIndex = (source instanceof WorkflowManagerUI) ? 0 : 1;
        final int sourcePortCount = source.getNrOutPorts();
        final int destinationStartIndex = (destination instanceof WorkflowManagerUI) ? 0 : 1;
        final int destinationPortCount = destination.getNrInPorts();
        final Set<ConnectionContainer> existingOutConnections = getConnectionsForConnectable(sourceEP, true, wm);
        final Set<ConnectionContainer> existingInConnections = getConnectionsForConnectable(destinationEP, false, wm);

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
                            return Optional.of(new PlannedConnection(sourceEP, i, destinationEP, j));
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
                            return Optional.of(new PlannedConnection(sourceEP, i, destinationEP, j));
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

                        return Optional.of(new PlannedConnection(sourceEP, i, destinationEP, j, mustDetach));
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
            final NodeID destinationNID = pc.getDestinationNode().getNodeContainer().getID();
            final NodeID sourceNID = pc.getSourceNode().getNodeContainer().getID();

            if ((inport && destinationNID.equals(nid) && (pc.getDestinationInportIndex() == port))
                || ((!inport) && sourceNID.equals(nid) && (pc.getSourceOutportIndex() == port))) {
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
    protected boolean nodeHasConnectionWithinSet(final ConnectableEditPart node,
        final Collection<ConnectableEditPart> set) {
        final WorkflowManager wm = getManager();
        final Set<ConnectionContainer> incoming = getConnectionsForConnectable(node, true, wm);

        if (!incoming.isEmpty()) {
            for (ConnectionContainer cc : incoming) {
                final NodeID nid = cc.getSource();

                for (ConnectableEditPart setItem : set) {
                    if (nid.equals(setItem.getNodeContainer().getID())) {
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
     * @param connectables the current selection set of connectable parts in the workflow editor
     * @return an instance of ScreenedSelectionSet
     */
    protected ScreenedSelectionSet screenNodeSelection(final ConnectableEditPart[] connectables) {
        final ArrayList<ConnectableEditPart> validLeft = new ArrayList<>();
        final ArrayList<ConnectableEditPart> validRight = new ArrayList<>();
        final WorkflowManager wm = getManager();

        for (int i = 0; i < connectables.length; i++) {
            final NodeContainerUI node = connectables[i].getNodeContainer();
            final NodeID nid = node.getID();
            // We are not guaranteed that the WorkflowManager state still contains these nodes, so check.
            final boolean contains = wm.containsNodeContainer(nid);
            boolean usableMetaNode = false;

            if (!contains && (node instanceof WorkflowManagerWrapper)) {
                final WorkflowManager subWM = ((WorkflowManagerWrapper)node).unwrap();

                usableMetaNode = subWM.equals(wm) && subWM.getParent().containsNodeContainer(nid);
            }

            if (contains || usableMetaNode) {
                final int portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
                final boolean canBeLeftMost = (node.getNrOutPorts() > portLowWaterMark);
                final boolean canBeRightMost = (node.getNrInPorts() > portLowWaterMark);

                if (canBeLeftMost) {
                    validLeft.add(connectables[i]);
                }

                if (canBeRightMost) {
                    validRight.add(connectables[i]);
                }
            }
        }

        if (validLeft.isEmpty() || validRight.isEmpty()) {
            return new ScreenedSelectionSet(Collections.emptySet(), null, null);
        }

        try {
            Collections.sort(validLeft, CONNECTABLE_SPATIAL_COMPARATOR);
            Collections.sort(validRight, CONNECTABLE_SPATIAL_COMPARATOR);
        } catch (NullPointerException e) {
            // This sneaky way assures us that we have UI bounds information for the rest of this screening code
            LOGGER.warn("Some nodes in the current selection returned invalid UI information.", e);

            return new ScreenedSelectionSet(Collections.emptySet(), null, null);
        }

        final ConnectableEditPart[] borderParts = new ConnectableEditPart[2];
        borderParts[0] = validLeft.get(0);
        borderParts[1] = validRight.get(validRight.size() - 1);

        final Rectangle[] spatialBounds = new Rectangle[2];
        spatialBounds[0] = getBoundsForConnectable(borderParts[0]);
        spatialBounds[1] = getBoundsForConnectable(borderParts[1]);

        final ArrayList<ConnectableEditPart> discards = new ArrayList<>();
        for (int i = (validLeft.size() - 1); i >= 0; i--) {
            final ConnectableEditPart cep = validLeft.get(i);

            if (cep == borderParts[1]) {
                continue;
            }

            final Rectangle boundsToo = getBoundsForConnectable(cep);
            if (boundsToo.x < spatialBounds[1].x) {
                break;
            }

            final NodeContainerUI node = cep.getNodeContainer();
            final int portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
            // It's not clear whether there may ever be a node which doesn't have a flow output, <= to be sure
            if (node.getNrOutPorts() <= portLowWaterMark) {
                discards.add(cep);
            }
        }
        validLeft.removeAll(discards);

        discards.clear();
        for (int i = (validRight.size() - 1); i >= 0; i--) {
            final ConnectableEditPart cep = validRight.get(i);

            if (cep == borderParts[0]) {
                continue;
            }

            final Rectangle boundsToo = getBoundsForConnectable(cep);
            if (boundsToo.x < spatialBounds[0].x) {
                break;
            }

            final NodeContainerUI node = cep.getNodeContainer();
            final int portLowWaterMark = (node instanceof WorkflowManagerUI) ? 0 : 1;
            // It's not clear whether there may ever be a node which doesn't have a flow input, <= to be sure
            if (node.getNrInPorts() <= portLowWaterMark) {
                discards.add(cep);
            }
        }
        validRight.removeAll(discards);


        // We should consider changing this to use an approach with maps should the common use case ever become scores
        //  of nodes being selected - this is currently a poly-time solution.
        boolean haveFoundALegalConnection = false;
        for (final ConnectableEditPart left : validLeft) {
            final NodeContainerUI node = left.getNodeContainer();
            final int sourcePortStart = (node instanceof WorkflowManagerUI) ? 0 : 1;
            final Rectangle leftBounds = getBoundsForConnectable(left);

            for (int i = sourcePortStart; (i < node.getNrOutPorts()) && (!haveFoundALegalConnection); i++) {
                final NodeOutPortUI sourcePort = node.getOutPort(i);
                final PortType sourcePortType = sourcePort.getPortType();

                for (final ConnectableEditPart right : validRight) {
                    if (left != right) {
                        final Rectangle rightBounds = getBoundsForConnectable(right);

                        if (leftBounds.x < rightBounds.x) {
                            final NodeContainerUI rightNode = right.getNodeContainer();
                            final int destinationPortStart = (rightNode instanceof WorkflowManagerUI) ? 0 : 1;

                            for (int j = destinationPortStart; j < rightNode.getNrInPorts(); j++) {
                                final NodeInPortUI destinationPort = rightNode.getInPort(j);
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


        final HashSet<ConnectableEditPart> connectableEditParts = new HashSet<>();
        // cramming them into a HashSet rids us of the problem that there will usually be a non-null intersection
        //  between the valid left and valid right sets of nodes.
        connectableEditParts.addAll(validLeft);
        connectableEditParts.addAll(validRight);

        return new ScreenedSelectionSet(connectableEditParts, borderParts[0].getNodeContainer(), borderParts[1].getNodeContainer());
    }


    static class ScreenedSelectionSet {
        private final List<ConnectableEditPart> m_connectableNodes;
        private final NodeContainerUI m_spatiallyLeftMostNode;
        private final NodeContainerUI m_spatiallyRightMostNode;

        ScreenedSelectionSet(final Collection<ConnectableEditPart> connectables, final NodeContainerUI left,
            final NodeContainerUI right) {
            m_spatiallyLeftMostNode = left;
            m_spatiallyRightMostNode = right;

            m_connectableNodes = new ArrayList<>(connectables);
            Collections.sort(m_connectableNodes, CONNECTABLE_SPATIAL_COMPARATOR);
        }

        boolean setIsConnectable() {
            return ((m_spatiallyLeftMostNode != null) && (m_spatiallyRightMostNode != null)
                && (m_spatiallyLeftMostNode != m_spatiallyRightMostNode) && (m_connectableNodes.size() > 1));
        }

        /**
         * @return a spatially in-order list of connectable nodes
         * @see NodeSpatialComparator
         */
        List<ConnectableEditPart> getConnectableNodes() {
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
        private final ConnectableEditPart m_sourceNode;
        private final int m_sourceOutportIndex;

        private final ConnectableEditPart m_destinationNode;
        private final int m_destinationInportIndex;

        private final boolean m_detachDestinationFirst;

        PlannedConnection(final ConnectableEditPart source, final int sourcePort, final ConnectableEditPart destination,
            final int destinationPort) {
            this(source, sourcePort, destination, destinationPort, false);
        }

        PlannedConnection(final ConnectableEditPart source, final int sourcePort, final ConnectableEditPart destination,
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
        public ConnectableEditPart getSourceNode() {
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
        public ConnectableEditPart getDestinationNode() {
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
            return m_sourceNode.getNodeContainer().getNameWithID() + ":" + m_sourceOutportIndex + " -> "
                + m_destinationNode.getNodeContainer().getNameWithID() + ":" + m_destinationInportIndex
                + (m_detachDestinationFirst ? " [DETACH]" : "");
        }
    }


    /**
     * This orders ascending first by x-coordinate and second by y-coordinate.
     */
    static class BoundsSpatialComparator implements Comparator<Rectangle> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final Rectangle bounds1, final Rectangle bounds2) {
            if (bounds1.x != bounds2.x) {
                return bounds1.x - bounds2.x;
            }

            return bounds1.y - bounds2.y;
        }
    }


    /**
     * This orders ascending first by x-coordinate and second by y-coordinate.
     */
    static class ConnectableSpatialComparator implements Comparator<ConnectableEditPart> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final ConnectableEditPart cep1, final ConnectableEditPart cep2) {
            return BOUNDS_SPATIAL_COMPARATOR.compare(getBoundsForConnectable(cep1), getBoundsForConnectable(cep2));
        }
    }
}
