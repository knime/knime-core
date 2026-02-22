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
 *   May 5, 2025 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.agentic.tool;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.agentic.tool.WorkflowToolValue.WorkflowToolResult;
import org.knime.core.node.dialog.ContentType;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.node.dialog.SubNodeDescriptionProvider;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.wizard.page.WizardPageContribution;
import org.knime.core.node.workflow.ComponentMetadata;
import org.knime.core.node.workflow.ComponentMetadata.Port;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.capture.CombinedExecutor;
import org.knime.core.node.workflow.capture.CombinedExecutor.PortId;
import org.knime.core.node.workflow.capture.CombinedExecutor.WorkflowSegmentExecutionResult;
import org.knime.core.node.workflow.capture.IsolatedExecutor;
import org.knime.core.node.workflow.capture.WorkflowSegment;
import org.knime.core.node.workflow.capture.WorkflowSegment.Input;
import org.knime.core.node.workflow.capture.WorkflowSegment.Output;
import org.knime.core.node.workflow.capture.WorkflowSegment.PortID;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor.ExecutionMode;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor.WorkflowSegmentNodeMessage;
import org.knime.core.node.workflow.virtual.VirtualNodeContext.Restriction;
import org.knime.core.util.FileUtil;
import org.knime.core.util.JsonUtil;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Implementation of a {@link ToolValue} where the tool can be provided by a MCP server
 * or as a KNIME workflow.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.11
 */
public final class ToolCell extends FileStoreCell implements ToolValue {

    private static final long serialVersionUID = 1L;

    /**
     * Data type of the {@link ToolCell}.
     */
    public static final DataType TYPE = DataType.getType(ToolCell.class);

    /**
     * Creates a new tool cell instance from a {@link WorkflowManager} instance and(!) removes all the 'Workflow Service
     * Input/Output' nodes and the 'Tool Message Output' node, if present.
     *
     * @param wfm the workflow manager to create the cell from and to modify
     * @param metadata tool-specific workflow metadata, can be {@code null} if there is no extra metadata
     * @param fileStoreFactory creates the file-stores to persist the workflow data
     * @return a new cell instance
     * @throws ToolIncompatibleWorkflowException if the passed workflow manager doesn't comply with the tool conventions
     *             (e.g. multiple tool message outputs or the workflow is executed/executing)
     * @throws IOException if the file stores cannot be created
     */
    public static ToolCell createFromAndModifyWorkflow(final WorkflowManager wfm,
        final ToolWorkflowMetadata metadata, final FileStoreFactory fileStoreFactory)
        throws ToolIncompatibleWorkflowException, IOException {
        return createFromAndModifyWorkflow(wfm, metadata, null, fileStoreFactory);
    }

    /**
     * Creates a new tool cell instance from a {@link WorkflowManager} instance and(!) removes all the 'Workflow Service
     * Input/Output' nodes and the 'Tool Message Output' node, if present.
     *
     * @param wfm the workflow manager to create the cell from and to modify
     * @param metadata tool-specific workflow metadata, can be {@code null} if there is no extra metadata
     * @param dataAreaPath absolute path to the directory used as data area, can be {@code null}
     * @param fileStoreFactory factory to create the file stores to persist the workflow data
     * @return a new cell instance
     * @throws ToolIncompatibleWorkflowException if the passed workflow manager doesn't comply with the tool conventions
     *             (e.g. multiple tool message outputs or the workflow is executed/executing)
     * @throws IOException if the file stores cannot be created
     */
    public static ToolCell createFromAndModifyWorkflow(final WorkflowManager wfm,
        final ToolWorkflowMetadata metadata, final Path dataAreaPath, final FileStoreFactory fileStoreFactory)
        throws ToolIncompatibleWorkflowException, IOException {
        checkThatThereAreNoExecutingOrExecutedNodes(wfm);
        var workflowFileStore = fileStoreFactory.createFileStore("workflow_" + UUID.randomUUID().toString());
        var fileStores = dataAreaPath == null ? new FileStore[]{workflowFileStore} : new FileStore[]{workflowFileStore,
            fileStoreFactory.createFileStore("data_area_" + UUID.randomUUID().toString())};
        var cell = new ToolCell(wfm, metadata == null ? null : metadata.toolMessageOutputNodeID(), dataAreaPath,
            fileStores);
        // AP-24599: flushing is necessary to make sure that the data area cannot be deleted (or modified) after the cell
        // is created but before flushToFileStore is called.
        FileStoreUtil.invokeFlush(cell);
        return cell;
    }

    private static void checkThatThereAreNoExecutingOrExecutedNodes(final WorkflowManager wfm)
        throws ToolIncompatibleWorkflowException {
        final var partiallyExecuted = wfm.canResetAll() && !wfm.getNodeContainers().isEmpty();
        if (partiallyExecuted) {
            throw new ToolIncompatibleWorkflowException(
                "Tool can't be created from an executed or partially executed workflow");
        }
        final var executing = wfm.canCancelAll();
        if (executing) {
            throw new ToolIncompatibleWorkflowException("Tool can't be created from an executing workflow");
        }
    }

    private final ToolType m_toolType;

    private final String m_name;

    private final String m_description;

    private final String m_parameterSchema;

    // Workflow-specific fields (null for MCP tools)
    private final ToolPort[] m_inputs;

    private final ToolPort[] m_outputs;

    private final int m_messageOutputPortIndex;

    byte[] m_workflow;

    private Path m_dataAreaPath;

    // MCP-specific fields (null for Workflow tools)
    private final String m_serverUri;

    private final String m_toolName;

    private ToolCell(final WorkflowManager wfm, final NodeID toolMessageOutputNodeID, final Path dataAreaPath,
        final FileStore... fileStores) throws ToolIncompatibleWorkflowException {
        super(fileStores);
        m_toolType = ToolType.WORKFLOW;
        var wsInputs = new ArrayList<WorkflowSegment.Input>();
        var wsOutputs = new ArrayList<WorkflowSegment.Output>();
        var toolInputs = new ArrayList<ToolPort>();
        var toolOutputs = new ArrayList<ToolPort>();
        try (var unused = wfm.lock()) {
            m_messageOutputPortIndex = removeAndCollectInputsAndOutputs(wfm, wsInputs, wsOutputs, toolInputs,
                toolOutputs, toolMessageOutputNodeID);
        }
        var ws = new WorkflowSegment(wfm, wsInputs, wsOutputs, Set.of());
        m_name = wfm.getName();
        m_description = wfm.getMetadata().getDescription().orElse("");
        m_parameterSchema = extractParameterSchemaFromConfigNodes(wfm);
        m_inputs = toolInputs.toArray(new ToolPort[0]);
        m_outputs = toolOutputs.toArray(new ToolPort[0]);
        m_workflow = serializeAndDisposeWorkflowSegment(ws);
        m_dataAreaPath = dataAreaPath;
        // MCP fields are null
        m_serverUri = null;
        m_toolName = null;
    }

    // Workflow deserialization constructor
    ToolCell(final String name, final String description, final String parameterSchema, final ToolPort[] inputs,
        final ToolPort[] outputs, final int messageOutputPortIndex) {
        m_toolType = ToolType.WORKFLOW;
        m_name = name;
        m_description = description;
        m_parameterSchema = parameterSchema;
        m_inputs = inputs;
        m_outputs = outputs;
        m_messageOutputPortIndex = messageOutputPortIndex;
        // MCP fields are null
        m_serverUri = null;
        m_toolName = null;
    }

    /**
     * MCP tool constructor.
     * 
     * @param name the tool name
     * @param description the tool description
     * @param parameterSchema the parameter schema as JSON
     * @param serverUri the MCP server URI
     * @param toolName the tool name on the MCP server
     */
    public ToolCell(final String name, final String description, final String parameterSchema,
        final String serverUri, final String toolName) {
        m_toolType = ToolType.MCP;
        m_name = name;
        m_description = description;
        m_parameterSchema = parameterSchema;
        m_serverUri = serverUri;
        m_toolName = toolName;
        // Workflow fields are null/empty
        m_inputs = new ToolPort[0];
        m_outputs = new ToolPort[0];
        m_messageOutputPortIndex = -1;
        m_workflow = null;
        m_dataAreaPath = null;
    }

    /**
     * @return the tool type (WORKFLOW or MCP)
     */
    public ToolType getToolType() {
        return m_toolType;
    }

    /**
     * @return the MCP server URI (null for Workflow tools)
     */
    public String getServerUri() {
        return m_serverUri;
    }

    /**
     * @return the MCP tool name (null for Workflow tools)
     */
    public String getToolName() {
        return m_toolName;
    }

    private static String extractParameterSchemaFromConfigNodes(final WorkflowManager wfm)
        throws ToolIncompatibleWorkflowException {
        var configNodes = wfm.getConfigurationNodes(false);
        if (configNodes.isEmpty()) {
            return "";
        }
        var paramSchema = JsonUtil.getProvider().createObjectBuilder();
        var paramNameSet = new HashSet<String>();
        for (var configNodeEntry : configNodes.entrySet()) {
            var paramName = configNodeEntry.getKey();
            // remove "-<nodeId>" suffix from config node name
            var dashIdx = paramName.lastIndexOf('-');
            if (dashIdx > 0) {
                paramName = paramName.substring(0, dashIdx);
            }
            if (paramNameSet.contains(paramName)) {
                throw new ToolIncompatibleWorkflowException(
                    "Duplicate parameter name '" + paramName + "' found in configuration nodes.");
            } else {
                paramNameSet.add(paramName);
            }
            var dialogNode = configNodeEntry.getValue();
            var value = dialogNode.getDefaultValue().toJson();
            var valueWithDescription = JsonUtil.getProvider().createObjectBuilder((JsonObject)value);
            valueWithDescription.add("description",
                ((SubNodeDescriptionProvider)dialogNode.getDialogRepresentation()).getDescription());
            paramSchema.add(paramName, valueWithDescription);
        }
        return paramSchema.build().toString();
    }

    private static byte[] serializeAndDisposeWorkflowSegment(final WorkflowSegment workflowSegment) {
        try (var byteOut = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(byteOut)) {
            workflowSegment.serializeAndDisposeWorkflow();
            workflowSegment.save(zipOut);
            return byteOut.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(
                "Problem saving workflow '" + workflowSegment.loadWorkflow().getName() + "'", ex);
        }
    }

    @Override
    protected void postConstruct() throws IOException {
        // @ToolTypeDispatch - Update when adding new ToolType values
        switch (m_toolType) {
            case WORKFLOW -> {
                var fileStores = getFileStores();
                m_workflow = FileUtils.readFileToByteArray(fileStores[0].getFile());
                if (fileStores.length == 2) {
                    m_dataAreaPath = fileStores[1].getFile().toPath();
                } else {
                    m_dataAreaPath = null;
                }
            }
            case MCP -> {
                // MCP tools have no workflow data to load
            }
        }
    }

    @Override
    protected void flushToFileStore() throws IOException {
        // @ToolTypeDispatch - Update when adding new ToolType values
        switch (m_toolType) {
            case WORKFLOW -> {
                var fileStores = getFileStores();
                var fileStore = fileStores[0];
                FileUtils.writeByteArrayToFile(fileStore.getFile(), m_workflow);
                if (m_dataAreaPath != null && fileStores.length == 2
                    && !m_dataAreaPath.equals(fileStores[1].getFile().toPath())) {
                    // copy the data area to the file store
                    var dataAreaDir = fileStores[1].getFile();
                    FileUtils.copyDirectory(m_dataAreaPath.toFile(), dataAreaDir);
                    m_dataAreaPath = dataAreaDir.toPath();
                }
            }
            case MCP -> {
                // MCP tools have no workflow data to flush
            }
        }
    }

    private WorkflowSegment deserializeWorkflowSegment() {
        try (var byteIn = new ByteArrayInputStream(m_workflow); var zipIn = new ZipInputStream(byteIn)) {
            return WorkflowSegment.load(zipIn);
        } catch (IOException ex) {
            throw new IllegalStateException("Problem loading workflow", ex);
        }
    }

    private static int removeAndCollectInputsAndOutputs(final WorkflowManager wfm,
        final List<WorkflowSegment.Input> wsInputs, final List<WorkflowSegment.Output> wsOutputs,
        final List<ToolPort> toolInputs, final List<ToolPort> toolOutputs, final NodeID toolMessageOutputNodeID) {
        List<NodeID> nodesToRemove = new ArrayList<>();
        var messageOutputPortIndex = new AtomicInteger(-1);
        wfm.getNodeContainers().stream().sorted(Comparator.comparingInt(nc -> nc.getUIInformation().getBounds()[1]))
            .forEach(nc -> {
                if (nc instanceof NativeNodeContainer nnc) {
                    if (collectInputs(wfm, wsInputs, toolInputs, nnc)) {
                        nodesToRemove.add(nnc.getID());
                        collectNodesUpstreamOf(List.of(nnc.getID()), wfm, nodesToRemove);
                    } else if (collectOutputs(wfm, wsOutputs, messageOutputPortIndex, toolOutputs, nnc,
                        toolMessageOutputNodeID)) {
                        nodesToRemove.add(nnc.getID());
                        collectNodesDownstreamOf(List.of(nnc.getID()), wfm, nodesToRemove);
                    }
                }
            });
        nodesToRemove.forEach(wfm::removeNode);
        return messageOutputPortIndex.get();
    }

    private static void collectNodesUpstreamOf(final List<NodeID> ids, final WorkflowManager wfm,
        final List<NodeID> result) {
        var upstreamNodes =
            ids.stream().flatMap(id -> wfm.getIncomingConnectionsFor(id).stream().map(cc -> cc.getSource())).toList();
        result.addAll(upstreamNodes);
        if (!upstreamNodes.isEmpty()) {
            collectNodesUpstreamOf(upstreamNodes, wfm, result);
        }
    }

    private static void collectNodesDownstreamOf(final List<NodeID> ids, final WorkflowManager wfm,
        final List<NodeID> result) {
        var downstreamNodes =
            ids.stream().flatMap(id -> wfm.getOutgoingConnectionsFor(id).stream().map(cc -> cc.getDest())).toList();
        result.addAll(downstreamNodes);
        if (!downstreamNodes.isEmpty()) {
            collectNodesDownstreamOf(downstreamNodes, wfm, result);
        }
    }

    private static boolean collectOutputs(final WorkflowManager wfm, final List<WorkflowSegment.Output> wsOutputs,
        final AtomicInteger messageOutputPortIndex, final List<ToolPort> toolOutputs, final NativeNodeContainer nnc,
        final NodeID toolMessageOutputNodeID) {
        if (nnc.getNodeModel() instanceof OutputNode outputNode) {
            var cc = wfm.getConnection(new ConnectionID(nnc.getID(), 1));
            if (cc == null) {
                return false;
            }
            var outPort = wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort());
            var wsOutput = new Output(outPort.getPortType(), null,
                new PortID(NodeIDSuffix.create(wfm.getID(), cc.getSource()), cc.getSourcePort()));
            var isToolMessageOutput = nnc.getID().equals(toolMessageOutputNodeID);
            if (isToolMessageOutput) {
                messageOutputPortIndex.set(wsOutputs.size());
                wsOutputs.add(wsOutput);
                return true;
            } else if (isWorkflowOutput(outputNode)) {
                var outputData = outputNode.getExternalOutput();
                var outputId = outputData.getID();
                wsOutputs.add(wsOutput);
                toolOutputs.add(new ToolPort(outPort.getPortType().getName(), outputId,
                    outputData.getDescription().orElse(""), specToString(outPort.getPortObjectSpec())));
                return true;
            }
        }
        return false;
    }

    private static boolean collectInputs(final WorkflowManager wfm, final List<WorkflowSegment.Input> wsInputs,
        final List<ToolPort> toolInputs, final NativeNodeContainer nnc) {
        if (nnc.getNodeModel() instanceof InputNode inputNode) {
            var inputData = inputNode.getInputData();
            if (isWorkflowInput(inputNode)) {
                Set<PortID> ports = wfm.getOutgoingConnectionsFor(nnc.getID(), 1).stream()
                    .map(cc -> new PortID(NodeIDSuffix.create(wfm.getID(), cc.getDest()), cc.getDestPort()))
                    .collect(Collectors.toSet());
                if (!ports.isEmpty()) {
                    var outPort = nnc.getOutPort(1);
                    wsInputs.add(new Input(outPort.getPortType(), null, ports));
                    toolInputs.add(new ToolPort(outPort.getPortType().getName(), inputData.getID(),
                        inputData.getDescription().orElse(""), specToString(outPort.getPortObjectSpec())));
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isWorkflowInput(final InputNode inputNode) {
        // only Workflow Input nodes with a table port are supported for now
        return hasContentType(inputNode.getInputData(),
            ContentType.CONTENT_TYPE_DEF_PREFIX + BufferedDataTable.class.getName());
    }

    private static boolean isWorkflowOutput(final OutputNode outputNode) {
        // only Workflow Output nodes with a table port are supported for now
        return hasContentType(outputNode.getExternalOutput(),
            ContentType.CONTENT_TYPE_DEF_PREFIX + BufferedDataTable.class.getName());
    }

    private static boolean hasContentType(final ExternalNodeData externalNodeData, final String contentType) {
        return externalNodeData.getContentType().map(ct -> ct.startsWith(contentType)).orElse(Boolean.FALSE);
    }

    private static String specToString(final PortObjectSpec spec) {
        if (spec instanceof DataTableSpec tableSpec) {
            var map = Map.of( //
                "name", tableSpec.getName(), //
                "columns", tableSpec.stream().map(colSpec -> Map.of( //
                    "name", colSpec.getName(), //
                    "type", colSpec.getType().getName())).toList() //
            );
            return JsonUtil.getProvider().createObjectBuilder(map).build().toString();
        } else {
            return "";
        }
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public String getDescription() {
        return m_description;
    }

    @Override
    public String getParameterSchema() {
        return m_parameterSchema;
    }

    @Override
    public ToolPort[] getInputs() {
        return m_inputs;
    }

    @Override
    public ToolPort[] getOutputs() {
        return m_outputs;
    }

//    @Override
    public int getMessageOutputPortIndex() {
        return m_messageOutputPortIndex;
    }

    @Override
    public WorkflowToolResult execute(final String parameters, final PortObject[] inputs, final ExecutionContext exec,
        final Map<String, String> executionHints) {
        // @ToolTypeDispatch - Update when adding new ToolType values
        return switch (m_toolType) {
            case WORKFLOW -> executeWorkflowTool(parameters, inputs, exec, executionHints);
            case MCP -> executeMCPTool(parameters, inputs, exec, executionHints);
        };
    }

    private WorkflowToolResult executeWorkflowTool(final String parameters, final PortObject[] inputs,
        final ExecutionContext exec, final Map<String, String> executionHints) {
        var ws = deserializeWorkflowSegment();
        var name = ws.loadWorkflow().getName();
        var hostNode = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();
        IsolatedExecutor wsExecutor = null;
        var execMode =
            ExecutionMode.valueOf(Optional.ofNullable(executionHints.get("execution-mode")).orElse("DEFAULT"));
        var isDebugMode = execMode == ExecutionMode.DEBUG;
        Path dataAreaPath = null;
        boolean disposeWorkflowSegmentExecutor = false;
        try {
            var workflowName = (isDebugMode ? "Debug: " : "") + name;
            dataAreaPath = copyDataAreaToTempDir().orElse(null);
            wsExecutor = WorkflowSegmentExecutor.builder( //
                hostNode, //
                execMode, //
                workflowName, //
                warning -> {
                }, //
                exec, //
                true).isolated(true).build();
            var result = wsExecutor.execute( //
                ws, //
                inputs, //
                StringUtils.isBlank(parameters) ? null : parseParameters(parameters), //
                dataAreaPath, //
                Restriction.WORKFLOW_RELATIVE_RESOURCE_ACCESS, Restriction.WORKFLOW_DATA_AREA_ACCESS);
            disposeWorkflowSegmentExecutor = !isDebugMode || result.outputs() != null;
            var wfm = wsExecutor.getWorkflow();
            String[] viewNodeIds = null;
            WorkflowManager virtualProject = null;
            if (Boolean.parseBoolean(executionHints.get("with-view-nodes"))) {
                viewNodeIds = wfm.getNodeContainers().stream()
                    .filter(nc -> nc instanceof NativeNodeContainer nnc
                        && nnc.getNode().getFactory() instanceof WizardPageContribution wpc && wpc.hasNodeView()) //
                    .map(nc -> nc.getID().toString()).toArray(String[]::new);
                if (viewNodeIds.length > 0 && execMode == ExecutionMode.DETACHED) {
                    virtualProject = wfm.getParent();
                    disposeWorkflowSegmentExecutor = false;
                }
            }
            return new WorkflowToolResult(
                extractMessage(result.outputs(),
                    () -> WorkflowSegmentNodeMessage.compileSingleErrorMessage(result.nodeMessages())),
                null, removeMessageOutput(result.outputs()), virtualProject, viewNodeIds);
        } catch (Exception ex) {
            var message = "Failed to execute tool: " + name + ": " + ex.getMessage();
            NodeLogger.getLogger(getClass()).error(message, ex);
            disposeWorkflowSegmentExecutor = !isDebugMode;
            return new WorkflowToolResult(message, null, null, null, null);
        } finally {
            if (wsExecutor != null && disposeWorkflowSegmentExecutor) {
                wsExecutor.dispose();
            }
            if (dataAreaPath != null) {
                FileUtils.deleteQuietly(dataAreaPath.toFile());
            }
        }
    }

    private WorkflowToolResult executeMCPTool(final String parameters, final PortObject[] inputs,
        final ExecutionContext exec, final Map<String, String> executionHints) {
        // TODO: Implement MCP tool execution
        // This should call the MCP server at m_serverUri with the tool m_toolName and parameters
        var message = "MCP tool execution not yet implemented for tool: " + m_name;
        NodeLogger.getLogger(getClass()).warn(message);
        return new WorkflowToolResult(message, null, null, null, null);
    }

//    @Override
//    public WorkflowToolResult execute(final CombinedExecutor workflowExecutor, final String parameters,
//        final List<CombinedExecutor.PortId> inputs, final ExecutionContext exec,
//        final Map<String, String> executionHints) {
//        var ws = deserializeWorkflowSegment();
//        var name = ws.loadWorkflow().getName();
//        Path dataAreaPath = null;
//        try {
//            dataAreaPath = copyDataAreaToTempDir().orElse(null);
//            var result = workflowExecutor.execute(ws, inputs, parseParameters(parameters), dataAreaPath,
//                Restriction.WORKFLOW_RELATIVE_RESOURCE_ACCESS, Restriction.WORKFLOW_DATA_AREA_ACCESS);
//
//            var viewNodeIds = getViewNodeIds(workflowExecutor, executionHints, result);
//            setComponentMetadata(workflowExecutor, inputs, result.outputIds(), result.component());
//            var outputIds = result.outputIds() == null ? null : Stream.of(result.outputIds())
//                .map(id -> id.nodeIDSuffix().toString() + "#" + id.portIndex()).toArray(String[]::new);
//            return new WorkflowToolResult(
//                extractMessage(result.outputs(),
//                    () -> WorkflowSegmentNodeMessage.compileSingleErrorMessage(result.nodeMessages())),
//                removeMessageOutput(outputIds), removeMessageOutput(result.outputs()), null, viewNodeIds);
//        } catch (Exception ex) {
//            var message = "Failed to execute tool: " + name + ": " + ex.getMessage();
//            NodeLogger.getLogger(getClass()).error(message, ex);
//            return new WorkflowToolResult(message, null, null, null, null);
//        } finally {
//            if (dataAreaPath != null) {
//                FileUtils.deleteQuietly(dataAreaPath.toFile());
//            }
//        }
//    }

    private static String[] getViewNodeIds(final CombinedExecutor workflowExecutor,
        final Map<String, String> executionHints, final WorkflowSegmentExecutionResult result) {
        if (result.outputs() == null || result.component() == null
            || !Boolean.parseBoolean(executionHints.get("with-view-nodes"))) {
            return null;
        }
        var component = result.component();
        return component.getWorkflowManager().getNodeContainers().stream()
            .filter(nc -> nc instanceof NativeNodeContainer nnc
                && nnc.getNode().getFactory() instanceof WizardPageContribution wpc && wpc.hasNodeView()) //
            .map(nc -> NodeIDSuffix.create(workflowExecutor.getWorkflow().getID(), nc.getID()).toString())
            .toArray(String[]::new);

    }

    private void setComponentMetadata(final CombinedExecutor workflowExecutor, final List<PortId> inputs,
        final PortId[] outputs, final SubNodeContainer component) {
        if (component == null) {
            return;
        }
        var metadataBuilder = ComponentMetadata.fluentBuilder();
        var wfm = workflowExecutor.getWorkflow();
        var inPortMetadata = new Port[component.getNrInPorts() - 1];
        for (int i = 0; i < inPortMetadata.length; i++) {
            var portId = inputs.get(i);
            var toolInput = m_inputs[i];
            wfm.getOutgoingConnectionsFor(portId.nodeIDSuffix().prependParent(wfm.getID()), portId.portIndex()).stream()
                .filter(cc -> cc.getDest().equals(component.getID())).forEach(
                    cc -> inPortMetadata[cc.getDestPort() - 1] = new Port(toolInput.name(), toolInput.description()));
        }
        for (var pm : inPortMetadata) {
            metadataBuilder.withInPort(pm.name(), pm.description());
        }
        var outPortMetadata = new Port[outputs == null ? 0 : component.getNrOutPorts() - 1];
        for (int i = 0; i < outPortMetadata.length; i++) {
            var portId = outputs[i];
            if (i == m_messageOutputPortIndex) {
                outPortMetadata[portId.portIndex() - 1] = new Port("tool-message", "Output port for the tool message");
            } else {
                var toolOutput = m_outputs[i > m_messageOutputPortIndex && m_messageOutputPortIndex >= 0 ? (i - 1) : i];
                assert portId.nodeIDSuffix().prependParent(wfm.getID()).equals(component.getID());
                outPortMetadata[portId.portIndex() - 1] = new Port(toolOutput.name(), toolOutput.description());
            }
        }
        for (var pm : outPortMetadata) {
            metadataBuilder.withOutPort(pm.name(), pm.description());
        }
        var metadata = metadataBuilder.withHtmlContent().withLastModifiedNow().withDescription(m_description).build();
        component.setMetadata(metadata);
    }

    private Optional<Path> copyDataAreaToTempDir() throws IOException {
        if (m_dataAreaPath != null) {
            var dataAreaPath = FileUtil.createTempDir("workflow_tool_data_area").toPath();
            FileUtils.copyDirectory(m_dataAreaPath.toFile(), dataAreaPath.toFile());
            return Optional.of(dataAreaPath);
        } else {
            return Optional.empty();
        }
    }

    private static Map<String, JsonValue> parseParameters(final String parameters) {
        if (StringUtils.isBlank(parameters)) {
            return null;
        }
        try (var reader = JsonUtil.getProvider().createReader(new StringReader(parameters))) {
            var jsonObject = reader.readObject();
            return jsonObject.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }
    }

    private <T> T[] removeMessageOutput(final T[] outputs) {
        if (outputs == null) {
            return null;
        }
        if (m_messageOutputPortIndex == -1) {
            return outputs;
        }
        return ArrayUtils.remove(outputs, m_messageOutputPortIndex);
    }

    /**
     * Extracts the message from the workflow execution result. If the message output is a {@link ToolMessage}, its text
     * content parts are extracted. Otherwise, a default string representation of the output cell is used.
     *
     * @param result the workflow segment execution result.
     * @return the extracted tool message as a single string.
     */
    private String extractMessage(final PortObject[] outputs, final Supplier<String> singleErrorMessages) {
        if (outputs == null) {
            return "Tool execution failed with: " + singleErrorMessages.get();
        }
        if (m_messageOutputPortIndex == -1) {
            return "Tool executed successfully (no custom tool message output)";
        }
        var messageTable = (BufferedDataTable)outputs[m_messageOutputPortIndex];
        if (messageTable.size() == 0 || messageTable.getDataTableSpec().getNumColumns() == 0) {
            return "Tool executed successfully (empty custom tool message)";
        }
        return extractToolMessageContent(messageTable);
    }

    /**
     * Utility method to extract the tool message content from the first cell of the first row of a
     * {@link BufferedDataTable}. If the cell is a {@link ToolMessage}, its content is returned, otherwise the cell's
     * string representation is returned.
     *
     * @param messageTable the table containing the message
     * @return the extracted message content
     * @since 5.6
     */
    public static String extractToolMessageContent(final BufferedDataTable messageTable) {
        try (var cursor = messageTable.cursor()) {
            DataCell messageCell = cursor.forward().getAsDataCell(0);
            if (messageCell instanceof ToolMessage toolMessage) {
                return toolMessage.getToolMessageText();
            } else {
                return messageCell.toString();
            }
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        append("name", m_name, sb);
        if (isNotBlank(m_description)) {
            append("description", m_description, sb);
        }
        if (isNotBlank(m_parameterSchema)) {
            append("parameters", m_parameterSchema, sb);
        }
        if (m_inputs.length > 0) {
            append("input", m_inputs, sb);
        }
        if (m_outputs.length > 0) {
            append("output", m_outputs, sb);
        }
        return sb.toString();
    }

    private static void append(final String name, final String value, final StringBuilder sb) {
        sb.append(name).append("=").append(value).append(",\n");
    }

    private static void append(final String name, final ToolPort[] toolPorts, final StringBuilder sb) {
        var indent = 3;
        for (int i = 0; i < toolPorts.length; i++) {
            sb.append(name + " " + (i + 1)).append("=[\n");
            var toolPort = toolPorts[i];
            append("name", toolPort.name(), sb, indent);
            append("type", toolPort.type(), sb, indent);
            if (isNotBlank(toolPort.description())) {
                append("description", toolPort.description(), sb, indent);
            }
            if (isNotBlank(toolPort.spec())) {
                append("spec", toolPort.spec(), sb, indent);
            }
            sb.append("],\n");
        }
    }

    private static void append(final String name, final String value, final StringBuilder sb, final int indent) {
        sb.append(" ".repeat(indent)).append(name).append("=").append(value).append(",\n");
    }

    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (dc instanceof ToolCell toolCell) {
            return new EqualsBuilder() //
                .append(m_name, toolCell.m_name) //
                .append(m_description, toolCell.m_description) //
                .append(m_parameterSchema, toolCell.m_parameterSchema) //
                .append(m_inputs, toolCell.m_inputs) //
                .append(m_outputs, toolCell.m_outputs) //
                .append(m_workflow, toolCell.m_workflow) //
                .build();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder() //
            .append(m_name) //
            .append(m_description) //
            .append(m_parameterSchema) //
            .append(m_inputs)//
            .append(m_outputs) //
            .append(m_workflow) //
            .toHashCode();
    }

    @SuppressWarnings("javadoc")
    // needed for the registry to properly register the cell type. Looks more like a bug in the registry, though.
    public static final class WorkflowToolCellSerializer implements DataCellSerializer<ToolCell> {
        @Override
        public void serialize(final ToolCell cell, final DataCellDataOutput output) throws IOException {
            // Write tool type discriminator
            output.writeByte(cell.getToolType().getIndex());
            
            // Write common fields
            output.writeUTF(cell.getName());
            output.writeUTF(cell.getDescription());
            output.writeUTF(cell.getParameterSchema());
            
            // @ToolTypeDispatch - Update when adding new ToolType values
            switch (cell.getToolType()) {
                case WORKFLOW -> {
                    // Write workflow-specific fields
                    output.writeInt(cell.getInputs().length);
                    for (var toolPort : cell.getInputs()) {
                        writeToolPort(toolPort, output);
                    }
                    output.writeInt(cell.getOutputs().length);
                    for (var toolPort : cell.getOutputs()) {
                        writeToolPort(toolPort, output);
                    }
                    output.writeInt(cell.getMessageOutputPortIndex());
                }
                case MCP -> {
                    // Write MCP-specific fields
                    output.writeUTF(cell.getServerUri());
                    output.writeUTF(cell.getToolName());
                }
            }
        }

        private static void writeToolPort(final ToolPort toolPort, final DataCellDataOutput output) throws IOException {
            output.writeUTF(toolPort.type());
            output.writeUTF(toolPort.name());
            output.writeUTF(toolPort.description());
            output.writeUTF(toolPort.spec());
        }

        @Override
        public ToolCell deserialize(final DataCellDataInput input) throws IOException {
            // Read tool type discriminator
            final byte toolTypeIndex = input.readByte();
            final ToolType toolType = ToolType.fromIndex(toolTypeIndex);
            
            // Read common fields
            final String name = input.readUTF();
            final String description = input.readUTF();
            final String parameterSchema = input.readUTF();
            
            // @ToolTypeDispatch - Update when adding new ToolType values
            return switch (toolType) {
                case WORKFLOW -> {
                    // Read workflow-specific fields
                    final var inputs = new ToolPort[input.readInt()];
                    for (int i = 0; i < inputs.length; i++) {
                        inputs[i] = readToolPort(input);
                    }
                    final var outputs = new ToolPort[input.readInt()];
                    for (int i = 0; i < outputs.length; i++) {
                        outputs[i] = readToolPort(input);
                    }
                    final int messageOutputPortIndex = input.readInt();
                    yield new ToolCell(name, description, parameterSchema, inputs, outputs, messageOutputPortIndex);
                }
                case MCP -> {
                    // Read MCP-specific fields
                    final String serverUri = input.readUTF();
                    final String toolName = input.readUTF();
                    yield new ToolCell(name, description, parameterSchema, serverUri, toolName);
                }
            };
        }

        private static ToolPort readToolPort(final DataCellDataInput input) throws IOException {
            var type = input.readUTF();
            var name = input.readUTF();
            var description = input.readUTF();
            var spec = input.readUTF();
            return new ToolPort(type, name, description, spec);
        }
    }

    /**
     * Exception thrown when a workflow is not compatible with the tool conventions.
     */
    public static final class ToolIncompatibleWorkflowException extends Exception {

        private static final long serialVersionUID = 1L;

        private ToolIncompatibleWorkflowException(final String message) {
            super(message);
        }

    }

}
