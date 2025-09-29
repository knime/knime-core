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
 *   Feb 15, 2021 (hornm): created
 */
package org.knime.core.node.workflow.capture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.capture.WorkflowSegment.Input;
import org.knime.core.node.workflow.capture.WorkflowSegment.Output;
import org.knime.core.node.workflow.capture.WorkflowSegment.PortID;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.LocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeModel;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectOutNodeFactory;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectOutNodeModel;
import org.knime.core.node.workflow.virtual.VirtualNodeContext.Restriction;
import org.knime.core.node.workflow.virtual.VirtualNodeInput;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;
import org.knime.core.util.Pair;
import org.knime.core.util.ThreadPool;

import jakarta.json.JsonException;
import jakarta.json.JsonValue;

/**
 * Represents an executable {@link WorkflowSegment}. The execution is done by embedding the workflow segment as a
 * metanode into the currently opened worflow.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class WorkflowSegmentExecutor {

    private WorkflowManager m_wfm;

    private WorkflowManager m_hostWfm;

    private final boolean m_shallDisposeHostWfm;
    private final NativeNodeContainer m_hostNode;

    private FlowVirtualScopeContext m_flowVirtualScopeContext;

    private NodeID m_virtualStartID;

    private NodeID m_virtualEndID;

    private final boolean m_executeAllNodes;

    /**
     * Controls how the workflow segment is executed.
     */
    public enum ExecutionMode {
            /**
             * Executed as a visible metanode next to the host node. Usually used for debugging purposes.
             */
            DEBUG,
            /**
             * Executed as invisible metanode next to the host node.
             */
            DEFAULT,
            /**
             * Executed as a metanode in a separate, temporary, workflow project. This execution mode doesn't have any
             * impact on the execution state of the parent workflow (e.g. component workflow) of the host node.
             */
            DETACHED;

    }

    /**
     * @see #WorkflowSegmentExecutor(WorkflowSegment, String, NodeContainer, boolean, boolean, Consumer)
     */
    public WorkflowSegmentExecutor(final WorkflowSegment ws, final String workflowName, final NodeContainer hostNode,
        final boolean debug, final Consumer<String> warningConsumer) throws KNIMEException {
        // executeAll option was newly introduced, using old behavior here
        this(ws, workflowName, hostNode, debug ? ExecutionMode.DEBUG : ExecutionMode.DEFAULT, false, warningConsumer);
    }

    /**
     * @param ws the workflow segment to execute
     * @param workflowName the name of the metanode to be created (which will only be visible if 'debug' is
     *            <code>true</code>)
     * @param hostNode the node which is responsible for the execution of the workflow segment (which provides the input
     *            and receives the output data, supplies the file store, etc.)
     * @param mode the workflow segment {@link ExecutionMode}
     * @param executeAll if <code>true</code> all nodes in the segment are executed (which is new behavior), previously
     *            and if <code>false</code> only the output nodes would be executed
     * @param warningConsumer callback for warning if there have while loading the workflow from the workflow segment
     * @throws KNIMEException If the workflow can't be instantiated from the segment.
     * @since 5.5
     */
    public WorkflowSegmentExecutor(final WorkflowSegment ws, final String workflowName, final NodeContainer hostNode,
        final ExecutionMode mode, final boolean executeAll, final Consumer<String> warningConsumer)
        throws KNIMEException {
        this(ws, workflowName, hostNode, mode, executeAll, warningConsumer, null, new Restriction[0]);
    }

    /**
     * @param ws the workflow segment to execute
     * @param workflowName the name of the metanode to be created (which will only be visible if 'debug' is
     *            <code>true</code>)
     * @param hostNode the node which is responsible for the execution of the workflow segment (which provides the input
     *            and receives the output data, supplies the file store, etc.)
     * @param mode the workflow segment {@link ExecutionMode}
     * @param executeAll if <code>true</code> all nodes in the segment are executed (which is new behavior), previously
     *            and if <code>false</code> only the output nodes would be executed
     * @param warningConsumer callback for warning if there have while loading the workflow from the workflow segment
     * @param dataAreaPath absolute path to the workflow's data area or {@code null} if none
     * @param restrictions restrictions on the execution of the workflow segment
     * @throws KNIMEException If the workflow can't be instantiated from the segment.
     * @since 5.5
     */
    public WorkflowSegmentExecutor(final WorkflowSegment ws, final String workflowName, final NodeContainer hostNode,
        final ExecutionMode mode, final boolean executeAll, final Consumer<String> warningConsumer,
        final Path dataAreaPath, final Restriction... restrictions) throws KNIMEException {
        m_hostNode = (NativeNodeContainer)hostNode;
        if (mode == ExecutionMode.DETACHED) {
            var projWfm = m_hostNode.getParent().getProjectWFM();
            m_hostWfm = createTemporaryWorkflowProject(projWfm.getWorkflowDataRepository(), projWfm.getContextV2());
            m_shallDisposeHostWfm = true;
        } else {
            m_hostWfm = hostNode.getParent();
            m_shallDisposeHostWfm = false;
        }

        try (var unused = m_hostWfm.lock()) {
            if (mode != ExecutionMode.DETACHED && !m_hostWfm.canModifyStructure()) {
                throw new KNIMEException(
                    "Cannot execute workflow segment in %s mode because it's part of an already executed component."
                        .formatted(mode.name().toLowerCase(Locale.ENGLISH)));
            }
            m_wfm = m_hostWfm.createAndAddSubWorkflow(new PortType[0], new PortType[0], workflowName);
        }

        m_flowVirtualScopeContext = new FlowVirtualScopeContext(hostNode.getID(), dataAreaPath, restrictions);
        m_wfm.setInitialScopeContext(m_flowVirtualScopeContext);
        if (mode != ExecutionMode.DEBUG) {
            m_wfm.hideInUI();
        }
        m_executeAllNodes = executeAll;

        // position
        NodeUIInformation startUIPlain = hostNode.getUIInformation();
        if (startUIPlain != null) {
            NodeUIInformation startUI =
                NodeUIInformation.builder(startUIPlain).translate(new int[]{60, -60, 0, 0}).build();
            m_wfm.setUIInformation(startUI);
        }

        // copy workflow segment into metanode
        WorkflowManager segmentWorkflow = BuildWorkflowsUtil.loadWorkflow(ws, warningConsumer);
        NodeID[] ids = segmentWorkflow.getNodeContainers().stream().map(NodeContainer::getID).toArray(NodeID[]::new);
        m_wfm.copyFromAndPasteHere(segmentWorkflow, WorkflowCopyContent.builder().setNodeIDs(ids).build());
        ws.disposeWorkflow();
        addVirtualIONodes(ws);
    }

    private static WorkflowManager createTemporaryWorkflowProject(final WorkflowDataRepository workflowDataRepository,
        final WorkflowContextV2 orgContext) throws KNIMEException {
        var mountpoint = ClassUtils.castOptional(AnalyticsPlatformExecutorInfo.class, orgContext.getExecutorInfo())//
            .flatMap(info -> info.getMountpoint()) //
            .map(mp -> Pair.create(mp.getFirst().getAuthority(), mp.getSecond())).orElse(Pair.create(null, null));
        var creationHelper =
            new WorkflowCreationHelper(createContext(orgContext.getExecutorInfo().getLocalWorkflowPath(),
                mountpoint.getFirst(), mountpoint.getSecond(), orgContext.getLocationInfo()));
        creationHelper.setWorkflowDataRepository(workflowDataRepository);
        return WorkflowManager.ROOT.createAndAddProject("workflow_segment_executor", creationHelper);
    }

    private static WorkflowContextV2 createContext(final Path workflowFolderPath, final String mountId,
        final Path mountpointRoot, final LocationInfo locationInfo) {
        return WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> {
                var res = exec //
                    .withCurrentUserAsUserId() //
                    .withLocalWorkflowPath(workflowFolderPath);
                if (mountId != null) {
                    res.withMountpoint(mountId, mountpointRoot);
                }
                return res;
            }) //
            .withLocation(locationInfo) //
            .build();
    }

    private void addVirtualIONodes(final WorkflowSegment wf) {

        //add virtual in node
        List<Input> inputs = wf.getConnectedInputs();
        PortType[] inTypes =
            inputs.stream().map(i -> getNonOptionalType(i.getType().get())).toArray(s -> new PortType[s]);
        int[] wfBounds = NodeUIInformation.getBoundingBoxOf(m_wfm.getNodeContainers());
        m_virtualStartID = m_wfm.createAndAddNode(new DefaultVirtualPortObjectInNodeFactory(inTypes));
        Pair<Integer, int[]> pos = BuildWorkflowsUtil.getInputOutputNodePositions(wfBounds, 1, true);
        m_wfm.getNodeContainer(m_virtualStartID).setUIInformation(
            NodeUIInformation.builder().setNodeLocation(pos.getFirst(), pos.getSecond()[0], -1, -1).build());

        //add virtual out node
        List<Output> outputs = wf.getConnectedOutputs();
        PortType[] outTypes =
            outputs.stream().map(o -> getNonOptionalType(o.getType().get())).toArray(s -> new PortType[s]);
        m_virtualEndID = m_wfm.createAndAddNode(new DefaultVirtualPortObjectOutNodeFactory(outTypes));
        pos = BuildWorkflowsUtil.getInputOutputNodePositions(wfBounds, 1, false);
        m_wfm.getNodeContainer(m_virtualEndID).setUIInformation(
            NodeUIInformation.builder().setNodeLocation(pos.getFirst(), pos.getSecond()[0], -1, -1).build());

        //connect virtual in
        for (int i = 0; i < inputs.size(); i++) {
            for (PortID p : inputs.get(i).getConnectedPorts()) {
                m_wfm.addConnection(m_virtualStartID, i + 1, p.getNodeIDSuffix().prependParent(m_wfm.getID()),
                    p.getIndex());
            }
        }

        //connect virtual out
        for (int i = 0; i < outputs.size(); i++) {
            PortID p = outputs.get(i).getConnectedPort().orElse(null);
            if (p != null) {
                m_wfm.addConnection(p.getNodeIDSuffix().prependParent(m_wfm.getID()), p.getIndex(), m_virtualEndID,
                    i + 1);
            }
        }
    }

    private static PortType getNonOptionalType(final PortType p) {
        return PortTypeRegistry.getInstance().getPortType(p.getPortObjectClass());
    }

    /**
     * Sets the configuration of the (config) nodes referenced by the given parameter name. Only considers nodes on the
     * top level (cp. {@link WorkflowManager#setConfigurationNodes(Map)}).
     *
     * @param parameters a map of parameter names to the new to be set configuration value as json
     * @throws InvalidSettingsException if there is no node for a given parameter name or the validation of the new
     *             configuration value failed
     * @throws JsonException if configuration couldn't be parsed from the json object
     */
    public void configureWorkflow(final Map<String, JsonValue> parameters)
        throws JsonException, InvalidSettingsException {
        checkWfmNonNull();
        m_wfm.setConfigurationNodes(parameters);
    }

    /**
     * Executes the workflow segment.
     *
     * @param inputData the input data to be used for execution
     * @param exec for cancellation
     * @return the resulting port objects and flow variables
     * @throws Exception if workflow execution fails
     * @throws IllegalStateException if the underlying workflow has been disposed already
     */
    public Pair<PortObject[], List<FlowVariable>> executeWorkflow(final PortObject[] inputData,
        final ExecutionContext exec) throws Exception { // NOSONAR
        checkWfmNonNull();
        NativeNodeContainer virtualInNode = ((NativeNodeContainer)m_wfm.getNodeContainer(m_virtualStartID));
        DefaultVirtualPortObjectInNodeModel inNM = (DefaultVirtualPortObjectInNodeModel)virtualInNode.getNodeModel();

        m_flowVirtualScopeContext.registerHostNode(m_hostNode, exec);

        inNM.setVirtualNodeInput(
            new VirtualNodeInput(inputData, collectOutputFlowVariablesFromUpstreamNodes(m_hostNode)));
        NativeNodeContainer nnc = (NativeNodeContainer)m_wfm.getNodeContainer(m_virtualEndID);

        AtomicReference<Exception> exception = new AtomicReference<>();
        executeAndWait(exec, exception);

        if (exception.get() != null) {
            throw exception.get();
        }

        DefaultVirtualPortObjectOutNodeModel outNM = (DefaultVirtualPortObjectOutNodeModel)nnc.getNodeModel();
        PortObject[] portObjectCopies = copyPortObjects(outNM.getOutObjects(), exec);
        // if (portObjectCopies != null) {
        //     removeSuperfluousFileStores(Stream.concat(stream(portObjectCopies), outputData.stream()));
        // }
        return Pair.create(portObjectCopies, getFlowVariablesFromNC(nnc));
    }

    /**
     * Executes the workflow segment and additionally returns a hierarchical list containing the error and warning
     * messages of all nodes. Reset messages are filtered out if they don't contain nested error or warning messages.
     * The method is used in
     * org.knime.python3.nodes/src/main/java/org/knime/python3/nodes/ports/PythonWorkflowPortObject.java to construct an
     * exception message for python nodes.
     *
     * @param inputData the input data to be used for execution
     * @param exec for cancellation
     * @return the resulting port objects, flow variables and a hierarchical list of node error and warning messages
     * @throws Exception if workflow execution fails
     * @throws IllegalStateException if the underlying workflow has been disposed already
     *
     * @since 5.4
     */
    public WorkflowSegmentExecutionResult executeWorkflowAndCollectNodeMessages(final PortObject[] inputData,
        final ExecutionContext exec) throws Exception {
        var executionResult = executeWorkflow(inputData, exec);
        boolean executionSuccessful = m_wfm.getNodeContainerState().isExecuted();
        return new WorkflowSegmentExecutionResult(executionSuccessful ? executionResult.getFirst() : null,
            executionResult.getSecond(), recursivelyExtractNodeMessages(m_wfm));
    }

    private void checkWfmNonNull() {
        if (m_wfm == null) {
            throw new IllegalStateException(
                "Can't extract error messages from workflow segment. Workflow has been disposed already.");
        }
    }

    private static List<WorkflowSegmentNodeMessage> recursivelyExtractNodeMessages(final NodeContainer nc) {
        if (nc instanceof NativeNodeContainer) {
            return Collections.emptyList();
        }
        WorkflowManager wfm = null;
        if (nc instanceof WorkflowManager w) {
            wfm = w;
        } else if (nc instanceof SubNodeContainer snc) {
            wfm = snc.getWorkflowManager();
        } else {
            throw new IllegalStateException("Received unexpected NodeContainer.");
        }
        return wfm.getNodeContainers().stream()
            .map(n -> new WorkflowSegmentNodeMessage(n.getName(), n.getID(), n.getNodeMessage(),
                recursivelyExtractNodeMessages(n)))
            .filter(msg -> (!msg.recursiveMessages().isEmpty()) || (msg.message().getMessageType() == Type.ERROR)
                || (msg.message().getMessageType() == Type.WARNING))
            .toList();
    }

    /**
     * Represents messages of nodes containing name, node ID, error or warning message and WorkflowSegmentNodeMessages
     * of nested nodes.
     *
     * @param nodeName the node name
     * @param nodeID the node ID
     * @param message the error message of the node
     * @param recursiveMessages a list of node messages for nested nodes if the node is a container, otherwise an empty
     *            list
     *
     * @since 5.4
     */
    public record WorkflowSegmentNodeMessage(String nodeName, NodeID nodeID, NodeMessage message,
        List<WorkflowSegmentNodeMessage> recursiveMessages) {
    }

    /**
     * Represents the result of the executed workflow including a hierarchical list of the error and warning messages
     * of all nodes.
     *
     * @param portObjectCopies the resulting port objects or {@code null} if the execution failed
     * @param flowVariables the resulting flow variables
     * @param nodeMessages a hierarchical list of the error and warning messages of all nodes
     *
     * @since 5.4
     */
    public record WorkflowSegmentExecutionResult(PortObject[] portObjectCopies, List<FlowVariable> flowVariables,
        List<WorkflowSegmentNodeMessage> nodeMessages) {
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            WorkflowSegmentExecutionResult result = (WorkflowSegmentExecutionResult) obj;
            return Arrays.equals(portObjectCopies, result.portObjectCopies) &&
                   Objects.equals(flowVariables, result.flowVariables) &&
                   Objects.equals(nodeMessages, result.nodeMessages);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((portObjectCopies == null) ? 0 : Arrays.hashCode(portObjectCopies));
            result = prime * result + ((flowVariables == null) ? 0 : flowVariables.hashCode());
            result = prime * result + ((nodeMessages == null) ? 0 : nodeMessages.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "WorkflowSegmentExecutionResult{" +
                    "portObjectCopies=" + Arrays.toString(portObjectCopies) +
                    ", flowVariables=" + flowVariables +
                    ", nodeMessages=" + nodeMessages +
                    '}';
        }

        /**
         * Helper to aggregate a single error message based on the node error message in execution result.
         *
         * @return the compiled error message
         *
         * @since 5.5
         */
        public String compileSingleErrorMessage() {
            var errorMessages =
                nodeMessages.stream().toList();
            // determine the number of failed nodes that are not containers
            List<WorkflowSegmentNodeMessage> leafErrorMessages = new ArrayList<>();
            for (WorkflowSegmentNodeMessage message : errorMessages) {
                recursivelyExtractLeafNodeErrorMessages(message, leafErrorMessages);
            }
            return constructErrorMessage(leafErrorMessages.size(), errorMessages);
        }

        private static void recursivelyExtractLeafNodeErrorMessages(final WorkflowSegmentNodeMessage message,
            final List<WorkflowSegmentNodeMessage> result) {
            if (message.message().getMessageType() == Type.RESET) {
                return;
            }
            if (message.recursiveMessages().isEmpty()) {
                result.add(message);
            } else {
                for (WorkflowSegmentNodeMessage nestedMessage : message.recursiveMessages()) {
                    recursivelyExtractLeafNodeErrorMessages(nestedMessage, result);
                }
            }
        }

        private static String constructErrorMessage(final int numberOfFailedNodes,
            final List<WorkflowSegmentNodeMessage> errorMessages) {
            String iNodes;
            if (numberOfFailedNodes == 1) {
                iNodes = "one node";
            } else {
                iNodes = String.valueOf(numberOfFailedNodes) + " nodes";
            }
            return String.format("Workflow contains %s with execution failure:%n%s",
                iNodes,
                errorMessages.stream()//
                    .map(msg -> recursivelyConstructErrorMessage(msg, ""))//
                    .collect(Collectors.joining(",\n")));
        }

        private static String recursivelyConstructErrorMessage(final WorkflowSegmentNodeMessage message,
            final String prefix) {
            if (message.recursiveMessages().isEmpty()) {
                return prefix + message.nodeName() + " #" + message.nodeID().getIndex() + ": "
                    + removeErrorPrefix(message.message().getMessage());
            } else {
                String newPrefix = prefix + message.nodeName() + " #" + message.nodeID().getIndex() + " > ";
                return message.recursiveMessages().stream().filter(msg -> msg.message().getMessageType() == Type.ERROR)
                    .map(ms -> recursivelyConstructErrorMessage(ms, newPrefix))
                    .collect(Collectors.joining(",\n"));
                // WorkflowSegmentNodeMessage of type ERROR can contain messages of type WARNING
            }
        }

        private static String removeErrorPrefix(final String msg) {
            if (msg.startsWith(Node.EXECUTE_FAILED_PREFIX)) {
                return StringUtils.removeStart(msg, Node.EXECUTE_FAILED_PREFIX);
            }
            return msg;
        }
    }

    private void executeAndWait(final ExecutionContext exec, final AtomicReference<Exception> exception) {
        // code copied from SubNodeContainer#executeWorkflowAndWait
        final Runnable inBackgroundRunner = () -> {
            executeThisSegmentWithoutWaiting();
            try {
                waitWhileInExecution(m_wfm, exec);
            } catch (InterruptedException | CanceledExecutionException e) { // NOSONAR
                m_wfm.cancelExecution(m_hostNode);
                Thread.currentThread().interrupt();
            }
        };
        final var currentPool = ThreadPool.currentPool();
        if (currentPool != null) {
            // ordinary workflow execution
            try {
                currentPool.runInvisible(Executors.callable(inBackgroundRunner::run));
            } catch (ExecutionException ee) {
                exception.compareAndSet(null, ee);
                NodeLogger.getLogger(this.getClass()).error(
                    ee.getCause().getClass().getSimpleName() + " while waiting for to-be-executed workflow to complete",
                    ee);
            } catch (final InterruptedException e) { // NOSONAR interrupt is handled by WFM cancellation
                m_wfm.cancelExecution();
            }
        } else {
            // streaming execution
            inBackgroundRunner.run();
        }
    }

    private void executeThisSegmentWithoutWaiting() {
        if (m_executeAllNodes) {
            m_wfm.executeAll();
        } else {
            m_wfm.executeUpToHere(m_virtualEndID);
        }
    }

    private static void waitWhileInExecution(final WorkflowManager wfm, final ExecutionContext exec)
        throws InterruptedException, CanceledExecutionException {
        while (wfm.getNodeContainerState().isExecutionInProgress()) {
            wfm.waitWhileInExecution(1, TimeUnit.SECONDS);
            exec.checkCanceled();
        }
    }

    /**
     * Cancels the execution if it is running and removes the virtual node containing the workflow segment from the
     * hosting workflow.
     */
    public void dispose() {
        cancel();
        m_wfm.getParent().removeNode(m_wfm.getID());
        m_wfm = null;
        if (m_shallDisposeHostWfm) {
            WorkflowManager.ROOT.removeProject(m_hostWfm.getID());
        }
        m_hostWfm = null;
    }

    /**
     * Cancels the execution of the workflow segment.
     * @throws IllegalStateException if the underlying workflow has been disposed already
     */
    public void cancel() {
        checkWfmNonNull();
        if (m_wfm.getNodeContainerState().isExecutionInProgress()) {
            m_wfm.cancelExecution(m_wfm);
        }
    }

    private static List<FlowVariable> getFlowVariablesFromNC(final SingleNodeContainer nc) {
        Stream<FlowVariable> res;
        if (nc instanceof NativeNodeContainer) {
            res = ((NativeNodeContainer)nc).getNodeModel()
                .getAvailableFlowVariables(VariableTypeRegistry.getInstance().getAllTypes()).values().stream();
        } else {
            res = nc.createOutFlowObjectStack().getAllAvailableFlowVariables().values().stream();
        }
        return res.filter(fv -> fv.getScope() == Scope.Flow).collect(Collectors.toList());
    }

    private static PortObject[] copyPortObjects(final PortObject[] portObjects, final ExecutionContext exec)
        throws IOException, CanceledExecutionException {
        if (portObjects == null) {
            return null; // NOSONAR
        }
        PortObject[] portObjectCopies = new PortObject[portObjects.length];
        for (int i = 0; i < portObjects.length; i++) {
            if (portObjects[i] != null) {
                portObjectCopies[i] = PortObjectRepository.copy(portObjects[i], exec, exec);
            }
        }
        return portObjectCopies;
    }

    /*
     * Remove file stores that aren't needed anymore because they aren't part of any of the port objects
     * (either as file store cell or file store port object).
     */
    //private static void removeSuperfluousFileStores(final Stream<PortObject> portObjects) {
    // TODO
    // see ticket https://knime-com.atlassian.net/browse/AP-14414
    // m_thisNode.getNode().getFileStoreHandler();
    // ...
    //}

    /*
     * Essentially only take the flow variables coming in via the 2nd to nth input port (and ignore flow var (0th)
     * and workflow (1st) port). Otherwise those will always take precedence and can possibly
     * interfere with the workflow being executed.
     */
    private static List<FlowVariable> collectOutputFlowVariablesFromUpstreamNodes(final NodeContainer thisNode) {
        // skip flow var (0th) and workflow (1st) input port
        WorkflowManager wfm = thisNode.getParent();
        List<FlowVariable> res = new ArrayList<>();
        for (int i = 2; i < thisNode.getNrInPorts(); i++) {
            ConnectionContainer cc = wfm.getIncomingConnectionFor(thisNode.getID(), i);
            NodeID sourceId = cc.getSource();
            SingleNodeContainer snc;
            if (sourceId.equals(wfm.getID())) {
                // if upstream port is the 'inner' output port of a metanode
                snc = wfm.getWorkflowIncomingPort(cc.getSourcePort()).getConnectedNodeContainer();
            } else {
                NodeContainer nc = wfm.getNodeContainer(sourceId);
                if (nc instanceof WorkflowManager) {
                    // if the upstream node is a metanode
                    snc = ((WorkflowManager)nc).getOutPort(cc.getSourcePort()).getConnectedNodeContainer();
                } else {
                    snc = (SingleNodeContainer)nc;
                }
            }
            List<FlowVariable> vars = getFlowVariablesFromNC(snc);
            // reverse the order of the flow variables in order to preserve the original order
            ListIterator<FlowVariable> reverseIter = vars.listIterator(vars.size());
            while (reverseIter.hasPrevious()) {
                res.add(reverseIter.previous());
            }
        }
        return res;
    }

    /**
     * @return the workflow manager of the (to be) executed workflow segment
     * @throws IllegalStateException if the workflow segment executor has been disposed already
     */
    public WorkflowManager getWorkflowManager() {
        checkWfmNonNull();
        return m_wfm;
    }

}
