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
 *   Oct 20, 2025 (hornm): created
 */
package org.knime.core.node.workflow.capture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor.BuilderParams;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor.ExecutionMode;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor.WorkflowSegmentNodeMessage;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeModel;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectOutNodeFactory;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectOutNodeModel;
import org.knime.core.node.workflow.virtual.VirtualNodeContext.Restriction;
import org.knime.core.node.workflow.virtual.VirtualNodeInput;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;
import org.knime.core.util.Pair;

import jakarta.json.JsonValue;

/**
 * Adds the workflow segment to be executed to an already existing workflow as a component and connects it to the
 * previously added workflow segments or inputs. And executes it.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.8
 * @noreference This class is not intended to be referenced by clients.
 */
public final class CombinedExecutor {

    /**
     * Creates instances of {@link CombinedExecutor}.
     */
    public static final class Builder {

        final BuilderParams m_params;

        final PortObject[] m_initialInputs;

        final WorkflowManager m_combinedWorkflow;

        Builder(final BuilderParams params, final PortObject[] initialInputs) {
            m_params = params;
            m_initialInputs = initialInputs;
            m_combinedWorkflow = null;
        }

        Builder(final BuilderParams params, final WorkflowManager combinedWorkflow) {
            m_params = params;
            m_combinedWorkflow = combinedWorkflow;
            m_initialInputs = null;
        }

        /**
         * @return a new instance
         */
        public CombinedExecutor build() {
            return new CombinedExecutor(this);
        }
    }

    private WorkflowManager m_wfm;

    private NativeNodeContainer m_hostNode;

    private final boolean m_collectMessages;

    private final ExecutionContext m_exec;

    private List<PortId> m_sourcePortIds;

    private CombinedExecutor(final Builder builder) {
        m_hostNode = builder.m_params.hostNode();
        var mode = builder.m_params.mode();
        if (builder.m_combinedWorkflow == null) {
            assert builder.m_initialInputs != null;
            if (mode == ExecutionMode.DETACHED) {
                var projWfm = m_hostNode.getParent().getProjectWFM();
                m_wfm = WorkflowSegmentExecutor.createTemporaryWorkflowProject(projWfm.getWorkflowDataRepository(),
                    projWfm.getContextV2());
            } else {
                var hostNodeParent = m_hostNode.getParent();
                m_wfm = hostNodeParent.createAndAddSubWorkflow(new PortType[0], new PortType[0],
                    "Debug: " + builder.m_params.workflowName());
                if (mode != ExecutionMode.DEBUG) {
                    m_wfm.hideInUI();
                }
                // position
                NodeUIInformation startUIPlain = m_hostNode.getUIInformation();
                if (startUIPlain != null) {
                    NodeUIInformation startUI =
                        NodeUIInformation.builder(startUIPlain).translate(new int[]{60, -60, 0, 0}).build();
                    m_wfm.setUIInformation(startUI);
                }
            }
            PortType[] inTypes = Stream.of(builder.m_initialInputs)
                .map(i -> PortTypeRegistry.getInstance().getPortType(i.getClass())).toArray(PortType[]::new);
            var virtualInId = m_wfm.createAndAddNode(new DefaultVirtualPortObjectInNodeFactory(inTypes));
            ((DefaultVirtualPortObjectInNodeModel)((NativeNodeContainer)m_wfm.getNodeContainer(virtualInId))
                .getNodeModel())
                    .setVirtualNodeInput(new VirtualNodeInput(builder.m_initialInputs, Collections.emptyList()));
            m_wfm.executeUpToHere(virtualInId);
        } else {
            m_wfm = builder.m_combinedWorkflow;
        }
        m_collectMessages = builder.m_params.collectMessages();
        m_exec = builder.m_params.exec();

        if (builder.m_initialInputs != null) {
            var inputs = builder.m_initialInputs;
            var inputNodeId =
                m_wfm.findNodes(DefaultVirtualPortObjectInNodeModel.class, false).keySet().iterator().next();
            m_sourcePortIds = IntStream.range(0, inputs.length)
                .mapToObj(i -> new PortId(NodeIDSuffix.create(m_wfm.getID(), inputNodeId), (i + 1))).toList();
        } else {
            m_sourcePortIds = null;
        }

    }

    /**
     * @return the workflow used for workflow segment execution
     */
    public WorkflowManager getWorkflow() {
        return m_wfm;
    }

    /**
     * @return the port references of output ports of the source node within the combined workflow
     */
    public List<PortId> getSourcePortIds() {
        return m_sourcePortIds;
    }

    /**
     * References a port within the combined workflow used as inputs or outputs for the execution of a workflow segment.
     *
     * @param nodeIDSuffix
     * @param portIndex
     */
    public record PortId(NodeIDSuffix nodeIDSuffix, int portIndex) {

    }

    /**
     * Represents the result of combined workflow segment execution.
     *
     * @param outputs the outputs of the executed workflow segment or {@code null} if execution failed
     * @param flowVariables the flow variables after execution
     * @param nodeMessages a hierarchical list of error and warning messages of all nodes of the executed workflow
     *            segment
     * @param outputIds port references of the outputs of the executed workflow segment within the combined workflow
     * @param component the component encapsulating the executed workflow segment
     */
    public record WorkflowSegmentExecutionResult(PortObject[] outputs, List<FlowVariable> flowVariables,
        List<WorkflowSegmentNodeMessage> nodeMessages, PortId[] outputIds, SubNodeContainer component) {
    }

    /**
     * Loads, configures and executes the given workflow segment within a combined workflow.
     *
     * @param ws the workflow segment to be executed
     * @param inputs the port references used as inputs for the execution of the workflow segment (connections will be
     *            added accordingly)
     * @param parameters a map of parameter names to the new to be set configuration value as json. Sets the
     *            configuration of the (config) nodes referenced by the given parameter name. Only considers nodes on
     *            the top level (cp. {@link WorkflowManager#setConfigurationNodes(Map)}). Can be {@code null}.
     * @param dataAreaPath absolute path to the workflow's data area or {@code null} if none
     * @param restrictions restrictions on the execution of the workflow segment
     * @return the execution result
     * @throws Exception if the workflow segment loading, configuration or execution fails
     */
    public WorkflowSegmentExecutionResult execute(final WorkflowSegment ws, final List<PortId> inputs,
        final Map<String, JsonValue> parameters, final Path dataAreaPath, final Restriction... restrictions)
        throws Exception {
        var segmentWorkflow = ws.loadWorkflow();

        var orgNodeIds = segmentWorkflow.getNodeContainers().stream().map(NodeContainer::getID).toArray(NodeID[]::new);
        var persistor = segmentWorkflow.copy(WorkflowCopyContent.builder().setNodeIDs(orgNodeIds)
            .setAnnotationIDs(segmentWorkflow.getWorkflowAnnotationIDs().toArray(WorkflowAnnotationID[]::new)).build());
        var copyContent = m_wfm.paste(persistor);
        ws.disposeWorkflow();
        var nodeIdMapping = new HashMap<NodeIDSuffix, NodeID>();
        for (int i = 0; i < orgNodeIds.length; i++) {
            nodeIdMapping.put(NodeIDSuffix.create(segmentWorkflow.getID(), orgNodeIds[i]), copyContent.getNodeIDs()[i]);
        }

        // connect inputs
        var wsInputs = ws.getConnectedInputs();
        assert wsInputs.size() == inputs.size();
        for (int i = 0; i < inputs.size(); i++) {
            for (var portId : wsInputs.get(i).getConnectedPorts()) {
                var nodeId = nodeIdMapping.get(portId.getNodeIDSuffix());
                m_wfm.addConnection(toNodeID(inputs.get(i)), inputs.get(i).portIndex(), nodeId, portId.getIndex());
            }
        }

        // connect outputs
        List<PortType> outTypes = new ArrayList<>();
        List<Pair<NodeID, Integer>> outPorts = new ArrayList<>();
        for (var outputNodeId : m_wfm.findNodes(DefaultVirtualPortObjectOutNodeModel.class, false).keySet()) {
            // collect outports that are already connected to the output node
            m_wfm.getIncomingConnectionsFor(outputNodeId).forEach(cc -> {
                outTypes.add(m_wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort()).getPortType());
                outPorts.add(Pair.create(cc.getSource(), cc.getSourcePort()));
            });
            m_wfm.removeNode(outputNodeId);
        }
        var wsOutputs = ws.getConnectedOutputs();
        wsOutputs.stream().filter(o -> o.getConnectedPort().isPresent() && o.getType().isPresent()).forEach(o -> {
            outTypes.add(o.getType().orElseThrow());
            var portId = o.getConnectedPort();
            var nodeId = nodeIdMapping.get(portId.get().getNodeIDSuffix());
            outPorts.add(Pair.create(nodeId, portId.get().getIndex()));
        });
        var outputNodeId =
            m_wfm.createAndAddNode(new DefaultVirtualPortObjectOutNodeFactory(outTypes.toArray(PortType[]::new)));
        for (int i = 0; i < outPorts.size(); i++) {
            m_wfm.addConnection(outPorts.get(i).getFirst(), outPorts.get(i).getSecond(), outputNodeId, i + 1);
        }

        // collapse into component
        var componentId = m_wfm.convertMetaNodeToSubNode(
            m_wfm.collapseIntoMetaNode(copyContent.getNodeIDs(), copyContent.getAnnotationIDs(), ws.getName())
                .getCollapsedMetanodeID())
            .getConvertedNodeID();
        var component = (SubNodeContainer)m_wfm.getNodeContainer(componentId);

        configureComponent(parameters, component);

        var flowVirtualScopeContext = new FlowVirtualScopeContext(m_hostNode.getID(), dataAreaPath, restrictions);
        component.getWorkflowManager().setInitialScopeContext(flowVirtualScopeContext);

        layout(m_wfm);

        // execute and extract tool outputs
        var exception = new AtomicReference<Exception>();
        WorkflowSegmentExecutor.executeAndWait(m_hostNode, m_wfm, m_exec, null, exception);
        if (exception.get() != null) {
            throw exception.get();
        }

        var outputs = new PortObject[wsOutputs.size()];
        var outputIds = new PortId[wsOutputs.size()];
        var outIdx = 0;
        for (int i = 0; i < outPorts.size(); i++) {
            var cc = m_wfm.getIncomingConnectionFor(outputNodeId, i + 1);
            if (cc.getSource().equals(componentId)) {
                outputs[outIdx] = m_wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort()).getPortObject();
                outputIds[outIdx] = new PortId(NodeIDSuffix.create(m_wfm.getID(), cc.getSource()), cc.getSourcePort());
                outIdx++;
            }
        }
        var flowVars =
            WorkflowSegmentExecutor.getFlowVariablesFromNC((SingleNodeContainer)m_wfm.getNodeContainer(outputNodeId));
        boolean executionSuccessful = m_wfm.getNodeContainerState().isExecuted();
        if (!executionSuccessful) {
            outputs = null;
        }
        List<WorkflowSegmentNodeMessage> nodeMessages = List.of();
        if (m_collectMessages) {
            nodeMessages = WorkflowSegmentExecutor.recursivelyExtractNodeMessages(component.getWorkflowManager());
        }
        return new WorkflowSegmentExecutionResult(outputs, flowVars, nodeMessages, outputIds, component);

    }

    private static void configureComponent(final Map<String, JsonValue> parameters, final SubNodeContainer component)
        throws InvalidSettingsException {
        if (parameters != null && !parameters.isEmpty()) {
            var wfm = component.getParent();
            var componentWfm = component.getWorkflowManager();
            componentWfm.setConfigurationNodes(parameters);
            var settings = new NodeSettings("component_settings");
            wfm.saveNodeSettings(component.getID(), settings);
            var modelSettings = settings.addNodeSettings("model");
            var dialogNodes = componentWfm.getConfigurationNodes(false);
            for (var entry : dialogNodes.entrySet()) {
                var dialogValue = entry.getValue().getDialogValue();
                if (dialogValue != null) {
                    var subSettings = modelSettings.addNodeSettings(entry.getKey());
                    dialogValue.saveToNodeSettings(subSettings);
                }
            }
            wfm.loadNodeSettings(component.getID(), settings);
        }
    }

    private static void layout(final WorkflowManager wfm) {
        var ncs = wfm.getNodeContainers();
        var countPerDepth = new int[ncs.size()];
        for (var nc : ncs) {
            var depth = wfm.getNodeGraphAnnotation(nc.getID()).iterator().next().getDepth();
            nc.setUIInformation(
                NodeUIInformation.builder().setNodeLocation(depth * 100, countPerDepth[depth] * 100, 0, 0).build());
            countPerDepth[depth]++;
        }
    }

    private NodeID toNodeID(final PortId portId) {
        return portId.nodeIDSuffix().prependParent(m_wfm.getID());
    }

    /**
     * Disposes the combined executor and the underlying workflow used for execution.
     */
    public void dispose() {
        dispose(true);
    }

    /**
     * Disposes the combined executor and optionally the underlying workflow used for execution.
     *
     * @param disposeWorkflow whether to dispose the underlying workflow
     */
    public void dispose(final boolean disposeWorkflow) {
        if (disposeWorkflow) {
            WorkflowSegmentExecutor.cancel(m_wfm);
            if (m_wfm.isProject()) {
                m_wfm.getParent().removeProject(m_wfm.getID());
            } else {
                m_wfm.getParent().removeNode(m_wfm.getID());
            }
        }
        m_wfm = null;
        m_hostNode = null;
        m_sourcePortIds = null;
    }

}
