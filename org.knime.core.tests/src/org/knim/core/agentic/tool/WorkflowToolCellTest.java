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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.agentic.tool.ToolWorkflowMetadata;
import org.knime.core.node.agentic.tool.WorkflowToolCell;
import org.knime.core.node.agentic.tool.WorkflowToolCell.ToolIncompatibleWorkflowException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.core.ExecutionContextExtension;
import org.knime.testing.util.WorkflowManagerUtil;

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
        assertThat(res.message()).startsWith("Failed to execute tool:");
        assertThat(res.outputs()).isNull();
    }

    @Test
    void testCellCreatedFromFullFledgedToolWorkflow() throws IOException, ToolIncompatibleWorkflowException {
        var exec = executionContextExtension.getExecutionContext();
        var dc =
            exec.createDataContainer(new DataTableSpec(new DataColumnSpecCreator("col1", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("col2", StringCell.TYPE).createSpec()));
        dc.addRowToTable(new DefaultRow("1", "val1", "val2"));
        dc.addRowToTable(new DefaultRow("2", "val3", "val4"));
        dc.close();
        var table = dc.getTable();
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowInputTestNodeFactory(table), 0, 0); // un-connected input node
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowOutputTestNodeFactory(), 0, 0); // un-connected input node
        WorkflowManagerUtil.createAndAddNode(m_toolWfm, new ConfigurationTestNodeFactory(), 0, 0); // configuration node
        var input = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowInputTestNodeFactory(table), 1, 0);
        var node = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new TestNodeFactory(), 0, 0);
        var output = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowOutputTestNodeFactory(), 1, 1);
        var messageOutput = WorkflowManagerUtil.createAndAddNode(m_toolWfm, new WorkflowOutputTestNodeFactory(), 1, 2);
        m_toolWfm.addConnection(input.getID(), 1, node.getID(), 1);
        m_toolWfm.addConnection(node.getID(), 1, output.getID(), 1);
        m_toolWfm.addConnection(node.getID(), 1, messageOutput.getID(), 1);
        var cell =
            WorkflowToolCell.createFromAndModifyWorkflow(m_toolWfm, new ToolWorkflowMetadata(messageOutput.getID()));
        assertThat(cell.getName()).isEqualTo("workflow");
        assertThat(cell.getDescription()).isBlank(); // TODO
        assertThat(cell.getParameterSchema()).isEqualTo(
            """
                    {"configuration-parameter-name-3":{"type":"string","default":"default config value","description":"config decription"}}""");
        assertThat(cell.getInputs()).hasSize(1);
        var toolInput = cell.getInputs()[0];
        assertThat(toolInput.name()).isEqualTo("test-input-parameter");
        assertThat(toolInput.description()).isEqualTo("workflow input description");
        assertThat(toolInput.type()).isEqualTo("Table");
        assertThat(toolInput.spec()).isEqualTo(""); // TODO
        assertThat(cell.getOutputs()).hasSize(1);
        var toolOutput = cell.getOutputs()[0];
        assertThat(toolOutput.name()).isEqualTo("test-output-parameter");
        assertThat(toolOutput.description()).isEqualTo("workflow output description");
        assertThat(toolOutput.type()).isEqualTo("Table");
        assertThat(toolOutput.spec()).isEqualTo(""); // TODO

        assertThat(cell.getWorkflow()).isNotEmpty();
        assertThat(cell.getMessageOutputPortIndex()).isEqualTo(1);

        var res = cell.execute("", new PortObject[0], exec);
        assertThat(res.message()).startsWith("Failed to execute tool:"); // TODO
    }

    @Test
    void testCellCreatedFromWorkflowWithoutToolMessageOutput() {
        // TODO
    }

    @Test
    void testCellCreatedFromPartiallyExecutedWorkflow() {
        // TODO
    }

}
