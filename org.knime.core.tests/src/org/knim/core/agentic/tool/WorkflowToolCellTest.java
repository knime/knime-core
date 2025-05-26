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
package org.knim.core.agentic.tool;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.knime.core.node.agentic.tool.ToolWorkflowMetadata;
import org.knime.core.node.agentic.tool.WorkflowToolCell;
import org.knime.core.node.agentic.tool.WorkflowToolCell.ToolIncompatibleWorkflowException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainerMetadata.ContentType;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowMetadata;
import org.knime.core.node.workflow.capture.WorkflowSegment;
import org.knime.testing.core.ExecutionContextExtension;
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

    @BeforeEach
    void createEmptyWorkflow() throws IOException {
        m_toolWfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @AfterEach
    void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_toolWfm);
    }

    @Test
    void testCellCreatedFromEmptyWorkflow() throws IOException, ToolIncompatibleWorkflowException {
        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null);
        assertThat(cell.getName()).isEqualTo("workflow");
        assertThat(cell.getDescription()).isBlank();
        assertThat(cell.getParameterSchema()).isEmpty();
        assertThat(cell.getInputs()).isEmpty();
        assertThat(cell.getOutputs()).isEmpty();
        assertThat(cell.getWorkflow()).isNotEmpty();
        assertThat(cell.getMessageOutputPortIndex()).isEqualTo(-1);

        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("", new PortObject[0], exec);
        assertThat(res.message()).startsWith("Tool executed successfully (no custom tool message output");
        assertThat(res.outputs()).isNotNull();
    }

    @Test
    void testCellCreatedFromFullFledgedToolWorkflow() throws IOException, ToolIncompatibleWorkflowException {
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowInputTestNodeFactory(), 0, 0); // un-connected input node
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowOutputTestNodeFactory(), 0, 0); // un-connected input node
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new ConfigurationTestNodeFactory(), 0, 0); // configuration node
        var messageOutput = addAndConnectNodes(m_toolWfm, true);
        m_toolWfm.setContainerMetadata(WorkflowMetadata.fluentBuilder().withContentType(ContentType.PLAIN)
            .withLastModifiedNow().withDescription("tool description").build());

        // create and check cell
        var cell =
            WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, new ToolWorkflowMetadata(messageOutput.getID()));
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
        assertThatJson(toolInput.spec()).isEqualTo("""
                {"columns":[{"type":"String","name":"col1"},{"type":"String","name":"col2"}],"name":"default"}""");
        assertThat(cell.getOutputs()).hasSize(1);
        var toolOutput = cell.getOutputs()[0];
        assertThat(toolOutput.name()).isEqualTo("test-output-parameter");
        assertThat(toolOutput.description()).isEqualTo("workflow output description");
        assertThat(toolOutput.type()).isEqualTo("Table");
        assertThatJson(toolOutput.spec()).isEqualTo("""
                {"columns":[{"type":"String","name":"col1"},{"type":"String","name":"col2"}],"name":"default"}""");
        assertThat(cell.getWorkflow()).isNotEmpty();
        assertThat(cell.getMessageOutputPortIndex()).isEqualTo(1);

        // check execution
        ConfigurationTestNodeModel.jsonValue = null;
        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("""
                {"configuration-parameter-name-3": "config value" }
                 """, new PortObject[]{TestNodeModel.createTable(exec)}, exec);
        assertThat(res.message()).isEqualTo("val1");
        assertThat(res.outputs()[0]).isNotNull();
        assertThat(((JsonString)ConfigurationTestNodeModel.jsonValue).getString()).isEqualTo("config value");
    }

    @Test
    void testCellCreatedFromWorkflowWithoutToolMessageOutput() throws ToolIncompatibleWorkflowException {
        addAndConnectNodes(m_toolWfm, false); // without message output node

        // create and check cell
        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null);
        assertThat(cell.getMessageOutputPortIndex()).isEqualTo(-1);

        // check execution
        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("", new PortObject[]{TestNodeModel.createTable(exec)}, exec);
        assertThat(res.message()).startsWith("Tool executed successfully (no custom tool message output");
        assertThat(res.outputs()[0]).isNotNull();
    }

    @Test
    void testCellCreatedFromExecutedWorkflow() {
        addAndConnectNodes(m_toolWfm, true);
        m_toolWfm.executeAllAndWaitUntilDone();

        var message = Assertions.assertThrows(ToolIncompatibleWorkflowException.class,
            () -> WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null)).getMessage();
        assertThat(message).isEqualTo("Tool can't be created from an executed or partially executed workflow");
    }

    @Test
    void testExecuteToolWorkflowWithFailure() throws ToolIncompatibleWorkflowException {
        addAndConnectNodes(m_toolWfm, true, true);

        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null);

        // check execution
        var exec = executionContextExtension.getExecutionContext();
        var res = cell.execute("", new PortObject[]{TestNodeModel.createTable(exec)}, exec);
        assertThat(res.message())
            .startsWith("Tool execution failed with: Workflow contains one node with execution failure:\n"
                + "TestNodeFactory #2: Purposely fail on execute");
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

        var cell = WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, null);
        var ws = WorkflowSegment.load(new ZipInputStream(new ByteArrayInputStream(cell.getWorkflow())));
        var wfm  = ws.loadWorkflow();
        assertThat(wfm.getNodeContainers().size()).isOne();
        assertThat(wfm.getNodeContainers().iterator().next().getUIInformation().getBounds())
            .isEqualTo(new int[]{35, 23, 10, 10});
        ws.disposeWorkflow();
    }

    private static NativeNodeContainer addAndConnectNodes(final WorkflowManager wfm, final boolean addMessageOutput) {
        return addAndConnectNodes(wfm, false, addMessageOutput);
    }

    private static NativeNodeContainer addAndConnectNodes(final WorkflowManager wfm, final boolean addFailingNode,
        final boolean addMessageOutput) {
        var input = WorkflowManagerUtil.createAndAddNode(wfm, new WorkflowInputTestNodeFactory(), 1, 0);
        var node = WorkflowManagerUtil.createAndAddNode(wfm, new TestNodeFactory(addFailingNode), 0, 0);
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

}
