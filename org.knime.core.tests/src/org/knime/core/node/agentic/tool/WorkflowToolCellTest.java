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
 *   May 23, 2025 (hornm): created
 */
package org.knime.core.node.agentic.tool;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.LongUTFDataInputStream;
import org.knime.core.data.container.LongUTFDataOutputStream;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.agentic.tool.WorkflowToolCell.ToolIncompatibleWorkflowException;
import org.knime.core.node.agentic.tool.WorkflowToolCell.WorkflowToolCellSerializer;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainerMetadata.ContentType;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowMetadata;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.capture.WorkflowSegment;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor.ExecutionMode;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.virtual.VirtualNodeContext;
import org.knime.core.node.workflow.virtual.VirtualNodeContext.Restriction;
import org.knime.core.util.FileUtil;
import org.knime.testing.core.ExecutionContextExtension;
import org.knime.testing.data.filestore.LargeFileStoreValue;
import org.knime.testing.util.WorkflowManagerUtil;

import jakarta.json.JsonString;

/**
 * Tests for {@link WorkflowToolCell}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class WorkflowToolCellTest {

    @RegisterExtension
    static ExecutionContextExtension executionContextExtension = ExecutionContextExtension.create();

    private WorkflowManager m_toolWfm;

    @TempDir
    Path m_tempDir;

    @BeforeEach
    void createEmptyWorkflow() throws IOException {
        var dir = FileUtil.createTempDir("workflow", m_tempDir.toFile());
        var workflowFile = new File(dir, WorkflowPersistor.WORKFLOW_FILE);
        if (workflowFile.createNewFile()) {
            m_toolWfm =
                WorkflowManager.EXTRACTED_WORKFLOW_ROOT.createAndAddProject("workflow", new WorkflowCreationHelper(
                    WorkflowContextV2.forTemporaryWorkflow(workflowFile.getParentFile().toPath(), null)));
        } else {
            throw new IllegalStateException("Creating empty workflow failed");
        }
    }

    @AfterEach
    void disposeWorkflow() {
        var id = m_toolWfm.getID();
        if (WorkflowManager.EXTRACTED_WORKFLOW_ROOT.containsNodeContainer(id)) {
            WorkflowManager.EXTRACTED_WORKFLOW_ROOT.removeProject(id);
        }
    }

    @Test
    void testCellCreatedFromEmptyWorkflow() throws IOException, ToolIncompatibleWorkflowException {
        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null,
            FileStoreFactory.createNotInWorkflowFileStoreFactory());
        assertThat(cell.getName()).isEqualTo("workflow");
        assertThat(cell.getDescription()).isBlank();
        assertThat(cell.getParameterSchema()).isEmpty();
        assertThat(cell.getInputs()).isEmpty();
        assertThat(cell.getOutputs()).isEmpty();
        assertThat(cell.m_workflow).isNotEmpty();
        assertThat(cell.getMessageOutputPortIndex()).isEqualTo(-1);

        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("", new PortObject[0], exec, Map.of());
        assertThat(res.message()).startsWith("Tool executed successfully (no custom tool message output");
        assertThat(res.outputs()).isNotNull();
    }

    @Test
    void testCellCreatedFromFullFledgedToolWorkflow() throws IOException, ToolIncompatibleWorkflowException {
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowInputTestNodeFactory(), 0, 0); // un-connected input node
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowOutputTestNodeFactory(), 0, 0); // un-connected input node
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new ConfigurationTestNodeFactory(), 0, 0); // configuration node
        var messageOutput = addAndConnectNodes(m_toolWfm, inData -> {
            var projectWfm = NodeContext.getContext().getWorkflowManager();
            assertThat(projectWfm.getName()).isEqualTo("JUnit-ExecutionContextExtension");
        }, true);
        m_toolWfm.setContainerMetadata(WorkflowMetadata.fluentBuilder().withContentType(ContentType.PLAIN)
            .withLastModifiedNow().withDescription("tool description").build());

        // create and check cell
        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm,
            new ToolWorkflowMetadata(messageOutput.getID()), FileStoreFactory.createNotInWorkflowFileStoreFactory());
        assertThat(cell.getName()).isEqualTo("workflow");
        assertThat(cell.getDescription()).isEqualTo("tool description");
        assertThatJson(cell.getParameterSchema()).isEqualTo(
            """
                    {"configuration-parameter-name-3":{"type":"string","default":"default config value","description":"config decription"}}""");
        assertThat(cell.getInputs()).hasSize(1);
        var toolInput = cell.getInputs()[0];
        assertThat(toolInput.name()).isEqualTo("test-input-parameter");
        assertThat(toolInput.description()).isEqualTo("workflow input description");
        assertThat(toolInput.type()).isEqualTo("Table");
        assertThatJson(toolInput.spec()).isEqualTo(
            """
                    {"columns":[{"type":"String","name":"col1"},{"type":"String","name":"col2"},{"type":"LargeFileStoreCell","name":"large-file-store"}],"name":"default"}""");
        assertThat(cell.getOutputs()).hasSize(1);
        var toolOutput = cell.getOutputs()[0];
        assertThat(toolOutput.name()).isEqualTo("test-output-parameter");
        assertThat(toolOutput.description()).isEqualTo("workflow output description");
        assertThat(toolOutput.type()).isEqualTo("Table");
        assertThatJson(toolOutput.spec()).isEqualTo(
            """
                    {"columns":[{"type":"String","name":"col1"},{"type":"String","name":"col2"},{"type":"LargeFileStoreCell","name":"large-file-store"}],"name":"default"}""");
        assertThat(cell.m_workflow).isNotEmpty();
        assertThat(cell.getMessageOutputPortIndex()).isEqualTo(1);

        // check execution
        ConfigurationTestNodeModel.jsonValue = null;
        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("""
                {"configuration-parameter-name-3": "config value" }
                 """, new PortObject[]{TestNodeModel.createTable(exec)}, exec, Map.of());
        assertThat(res.message()).isEqualTo("val1");
        assertThat(res.outputs()[0]).isNotNull();
        assertThat(((JsonString)ConfigurationTestNodeModel.jsonValue).getString()).isEqualTo("config value");
        assertThat(res.virtualProject()).isNull();
        assertThat(res.viewNodeIds()).isNull();
    }

    @Test
    void testCellCreatedFromWorkflowWithoutToolMessageOutput() throws ToolIncompatibleWorkflowException, IOException {
        addAndConnectNodes(m_toolWfm, false); // without message output node

        // create and check cell
        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null,
            FileStoreFactory.createNotInWorkflowFileStoreFactory());
        assertThat(cell.getMessageOutputPortIndex()).isEqualTo(-1);

        // check execution
        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("", new PortObject[]{TestNodeModel.createTable(exec)}, exec, Map.of());
        assertThat(res.message()).startsWith("Tool executed successfully (no custom tool message output");
        assertThat(res.outputs()[0]).isNotNull();
    }

    @Test
    void testCellCreatedFromExecutedWorkflow() {
        addAndConnectNodes(m_toolWfm, true);
        m_toolWfm.executeAllAndWaitUntilDone();

        var message = Assertions
            .assertThrows(ToolIncompatibleWorkflowException.class, () -> WorkflowToolCell
                .createFromAndModifyWorkflow(m_toolWfm, null, FileStoreFactory.createNotInWorkflowFileStoreFactory()))
            .getMessage();
        assertThat(message).isEqualTo("Tool can't be created from an executed or partially executed workflow");
    }

    @Test
    void testExecuteToolWorkflowWithFailure() throws ToolIncompatibleWorkflowException, IOException {
        addAndConnectNodes(m_toolWfm, inData -> {
            throw new RuntimeException("Purposely fail on execute");
        }, true);

        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null,
            FileStoreFactory.createNotInWorkflowFileStoreFactory());

        // check execution
        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("", new PortObject[]{TestNodeModel.createTable(exec)}, exec, Map.of());
        assertThat(res.message())
            .startsWith("Tool execution failed with: Workflow contains one node with execution failure:"
                + System.lineSeparator() + "TestNodeFactory #2: Purposely fail on execute");
        assertThat(res.outputs()).isNull();
    }

    /**
     * Tests that nodes connected upstream/downstream to workflow input/output nodes are removed when the cell is
     * created and the workflow modified.
     *
     * @throws ToolIncompatibleWorkflowException
     * @throws IOException
     */
    @Test
    void testRemoveNodesConnectedToWorkflowIONodes() throws ToolIncompatibleWorkflowException, IOException {
        var upstreamOfInput1 = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new TestNodeFactory(), 1, 0);
        var upstreamOfInput0 = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new TestNodeFactory(), 1, 0);
        var input = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowInputTestNodeFactory(), 1, 0);
        var node = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new TestNodeFactory(), 35, 23);
        var output = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowOutputTestNodeFactory(), 1, 1);
        var downstreamOfOutput0 = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new TestNodeFactory(), 0, 0);
        var downstreamOfOutput1 = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new TestNodeFactory(), 0, 0);
        m_toolWfm.addConnection(input.getID(), 1, node.getID(), 1);
        m_toolWfm.addConnection(node.getID(), 1, output.getID(), 1);
        m_toolWfm.addConnection(upstreamOfInput0.getID(), 0, input.getID(), 0);
        m_toolWfm.addConnection(upstreamOfInput1.getID(), 0, upstreamOfInput0.getID(), 0);
        m_toolWfm.addConnection(output.getID(), 0, downstreamOfOutput0.getID(), 0);
        m_toolWfm.addConnection(downstreamOfOutput0.getID(), 0, downstreamOfOutput1.getID(), 0);

        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null,
            FileStoreFactory.createNotInWorkflowFileStoreFactory());
        var ws = WorkflowSegment.load(new ZipInputStream(new ByteArrayInputStream(cell.m_workflow)));
        var wfm = ws.loadWorkflow();
        assertThat(wfm.getNodeContainers().size()).isOne();
        assertThat(wfm.getNodeContainers().iterator().next().getUIInformation().getBounds())
            .isEqualTo(new int[]{35, 23, 10, 10});
        ws.disposeWorkflow();
    }

    /**
     * Tests to execute a workflow tool with a data area.
     *
     * @throws ToolIncompatibleWorkflowException
     * @throws IOException
     */
    @Test
    void testExecuteToolWorkflowWithDataArea(@TempDir final Path tempDataAreaPath)
        throws ToolIncompatibleWorkflowException, IOException {
        var virtualDataAreaPath = new AtomicReference<Path>();
        Consumer<BufferedDataTable[]> onExecute = inData -> {
            var vContext = VirtualNodeContext.getContext().orElse(null);
            assertThat(vContext).isNotNull();
            assertThat(vContext.hasRestriction(Restriction.WORKFLOW_RELATIVE_RESOURCE_ACCESS)).isTrue();
            assertThat(vContext.hasRestriction(Restriction.WORKFLOW_DATA_AREA_ACCESS)).isTrue();
            var dataAreaPath = vContext.getVirtualDataAreaPath().orElse(null);
            assertThat(dataAreaPath).isNotNull();
            try {
                var content = FileUtils.readFileToString(dataAreaPath.resolve("file").toFile(), StandardCharsets.UTF_8);
                assertThat(content).isEqualTo("file content");
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            virtualDataAreaPath.set(dataAreaPath);

            var message = assertThrows(IOException.class, () -> new URL("knime://knime.workflow/test.txt").openStream())
                .getMessage();
            assertThat(message).isEqualTo(
                "Node is not allowed to access workflow-relative resources because it's executed within in a restricted (virtual) scope.");
            message = assertThrows(IOException.class, () -> new URL("knime://knime.mountpoint/test.txt").openStream())
                .getMessage();
            assertThat(message).isEqualTo("Mountpoint or space relative URL needs a mountpoint.");

        };
        addAndConnectNodes(m_toolWfm, onExecute, false);
        var dataAreaPath = Files.createDirectory(tempDataAreaPath.resolve("testDataArea"));
        FileUtils.writeStringToFile(dataAreaPath.resolve("file").toFile(), "file content", StandardCharsets.UTF_8);
        final var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null, dataAreaPath,
            FileStoreFactory.createNotInWorkflowFileStoreFactory());

        var readCell = writeAndReadWorkflowToolCell(cell);

        var exec = executionContextExtension.getExecutionContext();
        var res = readCell.execute("", new PortObject[]{TestNodeModel.createTable(exec)}, exec, Map.of());
        assertThat(res.message()).startsWith("Tool executed successfully (no custom tool message output");
        assertThat(res.outputs()[0]).isNotNull();
        assertThat(Files.notExists(virtualDataAreaPath.get())).as("virtual data area not deleted").isTrue();
    }

    private WorkflowToolCell writeAndReadWorkflowToolCell(final WorkflowToolCell cell) throws IOException {
        byte[] serializedCell;
        try (var bout = new ByteArrayOutputStream(); var out = new TestDataCellOutput(new DataOutputStream(bout))) {
            FileStoreUtil.invokeFlush(cell);
            new WorkflowToolCellSerializer().serialize(cell, out);
            serializedCell = bout.toByteArray();
        }

        WorkflowToolCell readCell;
        try (var in = new TestDataCellInput(new DataInputStream(new ByteArrayInputStream(serializedCell)))) {
            readCell = new WorkflowToolCellSerializer().deserialize(in);
            var fileStores = FileStoreUtil.getFileStores(cell);
            var fsKeys = FileStoreUtil.getFileStoreKeys(cell);
            var dataRepo = FileStoreUtil.getFileStoreHandler(fileStores[0]).getDataRepository();
            FileStoreUtil.retrieveFileStoreHandlersFrom(readCell, fsKeys, dataRepo);
        }
        return readCell;
    }

    /**
     * Tests {@link WorkflowToolCell#execute(String, PortObject[], org.knime.core.node.ExecutionContext, String...)} in
     * the {@link ExecutionMode#DETACHED}.
     *
     * @throws ToolIncompatibleWorkflowException
     * @throws IOException
     */
    @Test
    void testExecuteToolWorkflowWithDetachedMode() throws ToolIncompatibleWorkflowException, IOException {
        var detachedWorkflowSegmentPath = new AtomicReference<Path>();
        Consumer<BufferedDataTable[]> onExecute = inData -> {
            var projectWfm = NodeContext.getContext().getWorkflowManager();
            assertThat(projectWfm.getName()).startsWith("workflow_segment_executor");
            testFileStores(inData[0]);
            detachedWorkflowSegmentPath.set(projectWfm.getContextV2().getExecutorInfo().getLocalWorkflowPath());
        };
        addAndConnectNodes(m_toolWfm, onExecute, false);

        final var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null,
            FileStoreFactory.createNotInWorkflowFileStoreFactory());

        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("", new PortObject[]{TestNodeModel.createTable(exec)}, exec,
            Map.of("execution-mode", "DETACHED"));
        assertThat(res.message()).startsWith("Tool executed successfully (no custom tool message output");
        testFileStores((BufferedDataTable)res.outputs()[0]);
        assertThat(Files.exists(detachedWorkflowSegmentPath.get())).isFalse();
    }

    private static void testFileStores(final BufferedDataTable inData) {
        try (var rowCursor = inData.cursor()) {
            var fileStoreColIndex = inData.getSpec().findColumnIndex("large-file-store");
            while (rowCursor.canForward()) {
                var row = rowCursor.forward();
                LargeFileStoreValue v = row.getValue(fileStoreColIndex);
                var lf = v.getLargeFile();
                assertThat(lf).isNotNull();

                long seed;
                try {
                    seed = lf.read();
                } catch (IOException e) {
                    throw new AssertionError("Failed to read from large file store", e);
                }
                assertThat(seed).isEqualTo(v.getSeed());
            }
        }
    }

    /**
     * Tests {@link WorkflowToolCell#execute(String, PortObject[], org.knime.core.node.ExecutionContext, Map)} for tools
     * with view nodes.
     *
     * @throws IOException
     * @throws ToolIncompatibleWorkflowException
     */
    @Test
    void testExecuteToolWorkflowWithViewNodes() throws ToolIncompatibleWorkflowException, IOException {
        var input = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowInputTestNodeFactory(), 1, 0);
        var node = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new TestNodeFactory(null), 0, 0);
        var output = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowOutputTestNodeFactory(), 1, 1);
        m_toolWfm.addConnection(input.getID(), 1, node.getID(), 1);
        m_toolWfm.addConnection(node.getID(), 1, output.getID(), 1);
        var nodeWithViewID =
            WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WithViewTestNodeFactory(), 1, 1).getID();
        m_toolWfm.addConnection(node.getID(), 1, nodeWithViewID, 1);

        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null,
            FileStoreFactory.createNotInWorkflowFileStoreFactory());
        var exec = executionContextExtension.getExecutionContext();
        var inputs = new PortObject[]{TestNodeModel.createTable(exec)};
        var res = cell.execute("", inputs, exec, Map.of("with-views", "true"));
        assertThat(res.viewNodeIds()).isNull();
        assertThat(res.virtualProject()).isNull();

        exec = executionContextExtension.getExecutionContext();
        res = cell.execute("", inputs, exec, Map.of("with-view-nodes", "true", "execution-mode", "DETACHED"));
        var virtualProject = res.virtualProject();
        assertThat(virtualProject).isNotNull();
        assertThat(res.viewNodeIds()).isEqualTo(new String[]{NodeIDSuffix
            .create(m_toolWfm.getParent().getID(), nodeWithViewID).prependParent(virtualProject.getID()).toString()});

        res = cell.execute("", inputs, exec, Map.of("with-view-nodes", "true", "execution-mode", "DEBUG"));
        assertThat(res.virtualProject()).isNull();
        assertThat(res.viewNodeIds()).hasSize(1);
    }

    private static NativeNodeContainer addAndConnectNodes(final WorkflowManager wfm, final boolean addMessageOutput) {
        return addAndConnectNodes(wfm, null, addMessageOutput);
    }

    private static NativeNodeContainer addAndConnectNodes(final WorkflowManager wfm,
        final Consumer<BufferedDataTable[]> onExecute, final boolean addMessageOutput) {
        var input = WorkflowManagerUtil.createAndAddNode(wfm, new WorkflowInputTestNodeFactory(), 1, 0);
        String nodeKey = null;
        if (onExecute != null) {
            nodeKey = String.valueOf(System.identityHashCode(onExecute));
            TestNodeModel.onExecuteMap.put(nodeKey, onExecute);
        }
        var node = WorkflowManagerUtil.createAndAddNode(wfm, new TestNodeFactory(nodeKey), 0, 0);
        var output = WorkflowManagerUtil.createAndAddNode(wfm, new WorkflowOutputTestNodeFactory(), 1, 1);
        wfm.addConnection(input.getID(), 1, node.getID(), 1);
        wfm.addConnection(node.getID(), 1, output.getID(), 1);
        if (addMessageOutput) {
            var messageOutput = WorkflowManagerUtil.createAndAddNode(wfm, new WorkflowOutputTestNodeFactory(), 1, 2);
            wfm.addConnection(node.getID(), 1, messageOutput.getID(), 1);
            return messageOutput;
        }
        return null;
    }

    @AfterEach
    void clearOnExecuteMap() {
        TestNodeModel.onExecuteMap.clear();
    }

    private class TestDataCellOutput extends LongUTFDataOutputStream implements DataCellDataOutput {

        TestDataCellOutput(final DataOutputStream output) {
            super(output);
        }

        @Override
        public void writeDataCell(final DataCell cell) throws IOException {
            throw new UnsupportedOperationException("Not implemented in test");
        }

    }

    private class TestDataCellInput extends LongUTFDataInputStream implements DataCellDataInput {

        TestDataCellInput(final DataInputStream input) {
            super(input);
        }

        @Override
        public DataCell readDataCell() throws IOException {
            throw new UnsupportedOperationException("Not implemented in test");
        }

    }

    public static class TestToolMessageCell extends DataCell implements ToolMessage {
        private final String m_content;

        public TestToolMessageCell(final String content) {
            m_content = content;
        }

        @Override
        public String getToolMessageText() {
            return m_content;
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return dc instanceof TestToolMessageCell;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            return "ToolMessageCell";
        }
    }

    @Test
    void testExtractToolMessageText() {
        DataCell messageCell = new TestToolMessageCell("TestMessage");

        DataTableSpec spec = new DataTableSpec(new DataColumnSpecCreator("msg", messageCell.getType()).createSpec());
        var exec = executionContextExtension.getExecutionContext();
        BufferedDataContainer container = exec.createDataContainer(spec);
        container.addRowToTable(new DefaultRow("row1", messageCell));
        container.close();
        BufferedDataTable messageTable = container.getTable();

        String extracted = WorkflowToolCell.extractToolMessageContent(messageTable);

        assertThat(extracted).isEqualTo("TestMessage");
    }
}
