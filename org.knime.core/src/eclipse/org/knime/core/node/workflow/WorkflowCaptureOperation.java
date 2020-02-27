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
 *   Feb 14, 2020 (hornm): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.capture.WorkflowFragment;
import org.knime.core.node.workflow.capture.WorkflowFragment.Input;
import org.knime.core.node.workflow.capture.WorkflowFragment.Output;
import org.knime.core.node.workflow.capture.WorkflowFragment.PortID;
import org.knime.core.util.Pair;

/**
 * Operation that allows one to capture parts of a workflow that is encapsulated by {@link CaptureWorkflowStartNode} and
 * {@link CaptureWorkflowEndNode}s.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class WorkflowCaptureOperation {

    private WorkflowManager m_wfm;

    private NativeNodeContainer m_startNode;

    private NativeNodeContainer m_endNode;

    WorkflowCaptureOperation(final NodeID endNodeID, final WorkflowManager wfm) throws IllegalScopeException {
        m_endNode = wfm.getNodeContainer(endNodeID, NativeNodeContainer.class, true);
        CheckUtils.checkArgument(m_endNode.getNodeModel() instanceof CaptureWorkflowEndNode,
            "Argument must be instance of %s", CaptureWorkflowEndNode.class.getSimpleName());
        NodeID startNodeID = wfm.getWorkflow().getMatchingScopeStart(endNodeID, CaptureWorkflowStartNode.class,
            CaptureWorkflowEndNode.class);
        if (startNodeID == null) {
            throw new IllegalArgumentException(
                "The passed node id doesn't represent a 'capture workflow end node' or it's respective start node is missing.");
        }
        m_startNode = wfm.getNodeContainer(startNodeID, NativeNodeContainer.class, true);
        m_wfm = wfm;
    }

    /**
     * Carries out the actual capture-operation and returns the captured sub-workflow as a {@link WorkflowFragment}.
     *
     * @return the captured sub-workflow
     */
    public WorkflowFragment capture() {
        try (WorkflowLock lock = m_wfm.lock()) {
            NodeID endNodeID = m_endNode.getID();
            WorkflowManager tempParent = WorkflowManager.EXTRACTED_WORKFLOW_ROOT
                .createAndAddProject("Capture-" + endNodeID, new WorkflowCreationHelper());
            // TODO we might have to revisit this when implementing AP-13335
            Set<NodeIDSuffix> addedPortObjectReaderNodes = new HashSet<>();

            // "scope body" -- will copy those nodes later
            List<NodeContainer> nodesInScope = m_wfm.getWorkflow().getNodesInScope(m_endNode);

            // "scope body" and port object ref readers -- will determine bounding box and move them to the top left
            List<NodeContainer> nodesToDetermineBoundingBox = new ArrayList<>(nodesInScope);

            // copy nodes in scope body
            WorkflowCopyContent.Builder copyContent = WorkflowCopyContent.builder();
            NodeID[] allIDs = nodesInScope.stream().map(NodeContainer::getID).toArray(NodeID[]::new);
            HashSet<NodeID> allIDsHashed = new HashSet<>(Arrays.asList(allIDs));
            // map from dest port to src node
            Map<Pair<NodeID, Integer>, NodeID> portObjectReaderConnections = new HashMap<>();
            NodeID[] allButScopeIDs = ArrayUtils.removeElements(allIDs, endNodeID, m_startNode.getID());
            copyContent.setNodeIDs(allButScopeIDs);
            copyContent.setIncludeInOutConnections(false);

            // collect nodes outside the scope body but connected to the scope body (only incoming)
            for (int i = 0; i < allButScopeIDs.length; i++) {
                NodeContainer oldNode = m_wfm.getNodeContainer(allButScopeIDs[i]);
                for (int p = 0; p < oldNode.getNrInPorts(); p++) {
                    ConnectionContainer c = m_wfm.getIncomingConnectionFor(allButScopeIDs[i], p);
                    if (c == null) {
                        // ignore: no incoming connection
                    } else if (allIDsHashed.contains(c.getSource())) {
                        // ignore: connection already retained by paste persistor
                    } else {
                        if (!addedPortObjectReaderNodes.contains(NodeIDSuffix.create(m_wfm.getID(), c.getSource()))) {
                            // only add portObjectReader if not inserted already in previous loop iteration
                            NodeID sourceID = c.getSource();
                            NodeUIInformation sourceUIInformation = m_wfm.getNodeContainer(sourceID).getUIInformation();
                            int sourcePort = c.getSourcePort();
                            NodeContainer sourceNode = m_wfm.getNodeContainer(sourceID);
                            nodesToDetermineBoundingBox.add(sourceNode);
                            NodeOutPort upstreamPort;
                            if (sourceID.equals(m_wfm.getID())) {
                                assert c.getType() == ConnectionType.WFMIN;
                                upstreamPort = m_wfm.getInPort(sourcePort).getUnderlyingPort();
                            } else {
                                upstreamPort = sourceNode.getOutPort(sourcePort);
                            }
                            NodeIDSuffix pastedIDSuffix = NodeIDSuffix.create(tempParent.getID(),
                                PortObjectRepository.addPortObjectReferenceReaderToWorkflow(upstreamPort,
                                    m_wfm.getProjectWFM().getID(), tempParent, sourceID.getIndex()));

                            NodeID pastedID = pastedIDSuffix.prependParent(tempParent.getID());
                            tempParent.getNodeContainer(pastedID).setUIInformation(sourceUIInformation);
                            addedPortObjectReaderNodes.add(pastedIDSuffix);
                        }
                        //TODO deal with WFMIN-connections
                        portObjectReaderConnections.put(Pair.create(c.getDest(), c.getDestPort()), c.getSource());
                    }
                }
            }

            final int[] boundingBox = NodeUIInformation.getBoundingBoxOf(nodesToDetermineBoundingBox);
            final int[] moveUIDist = new int[]{-boundingBox[0] + 50, -boundingBox[1] + 50};
            copyContent.setPositionOffset(moveUIDist);

            tempParent.copyFromAndPasteHere(m_wfm, copyContent.build());

            // connect all new port object readers to the in-scope-nodes
            for (Entry<Pair<NodeID, Integer>, NodeID> connection : portObjectReaderConnections.entrySet()) {
                NodeIDSuffix srcID = NodeIDSuffix.create(m_wfm.getID(), connection.getValue());
                NodeIDSuffix destID = NodeIDSuffix.create(m_wfm.getID(), connection.getKey().getFirst());
                int destIdx = connection.getKey().getSecond();
                tempParent.addConnection(srcID.prependParent(tempParent.getID()), 1,
                    destID.prependParent(tempParent.getID()), destIdx);
            }

            // position port object readers
            for (NodeIDSuffix suffix : addedPortObjectReaderNodes) {
                NodeID srcID = suffix.prependParent(tempParent.getID());
                NodeUIInformation.moveNodeBy(tempParent.getNodeContainer(srcID), moveUIDist);
            }

            //transfer editor settings, too
            tempParent.setEditorUIInformation(m_wfm.getEditorUIInformation());

            List<Input> workflowFragmentInputs = getInputs();
            List<Output> workflowFragmentOutputs = getOutputs();

            return new WorkflowFragment(tempParent, workflowFragmentInputs, workflowFragmentOutputs,
                addedPortObjectReaderNodes);
        }
    }

    /**
     * Returns the input of the (to be) captured sub-workflow, i.e. the same ports {@link #capture()} with a
     * subsequent {@link WorkflowFragment#getConnectedInputs()} would return.
     *
     * @return the inputs of the (to be) captured workflow fragment
     */
    public List<Input> getInputs() {
        List<Input> res = new ArrayList<>();
        for (int i = 0; i < m_startNode.getNrOutPorts(); i++) {
            Set<PortID> connections = m_wfm.getOutgoingConnectionsFor(m_startNode.getID(), i).stream().map(cc -> {
                return new PortID(NodeIDSuffix.create(m_wfm.getID(), cc.getDest()), cc.getDestPort());
            }).collect(Collectors.toSet());
            NodeOutPort outPort = m_startNode.getOutPort(i);
            res.add(new Input(outPort.getPortType(), castToDTSpecOrNull(outPort.getPortObjectSpec()), connections));
        }
        return res;
    }

    /**
     * Returns the outputs of the (to be) captured sub-workflow, i.e. the same ports {@link #capture()} with a
     * subsequent {@link WorkflowFragment#getConnectedOutputs()} would return.
     *
     * @return the outputs of the (to be) captured workflow fragment
     */
    public List<Output> getOutputs() {
        List<Output> res = new ArrayList<>();
        for (int i = 0; i < m_endNode.getNrInPorts(); i++) {
            ConnectionContainer cc = m_wfm.getIncomingConnectionFor(m_endNode.getID(), i);
            PortID connectPort = null;
            PortType type = null;
            DataTableSpec spec = null;
            if (cc != null) {
                connectPort = new PortID(NodeIDSuffix.create(m_wfm.getID(), cc.getSource()), cc.getSourcePort());
                NodeOutPort outPort = m_wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort());
                type = outPort.getPortType();
                spec = castToDTSpecOrNull(outPort.getPortObjectSpec());
            }
            res.add(new Output(type, spec, connectPort));
        }
        return res;
    }

    private static final DataTableSpec castToDTSpecOrNull(final PortObjectSpec spec) {
        return spec instanceof DataTableSpec ? (DataTableSpec)spec : null;
    }

}
