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
 *   May 7, 2025 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.agentic.tool;

import java.util.List;
import java.util.Map;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.capture.CombinedExecutor;
import org.knime.core.node.workflow.capture.WorkflowSegmentExecutor.ExecutionMode;

/**
 * A {@link ToolValue} that is implemented by a workflow.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface WorkflowToolValue extends ToolValue {

    /**
     * @return -1 if the tool does not have a message output port
     */
    int getMessageOutputPortIndex();

    /**
     * {@inheritDoc} <br>
     * <br>
     * Accepted execution-hints:
     * <ul>
     * <li>{@code execution-mode}: see {@link ExecutionMode} for possible values, defaults to
     * {@link ExecutionMode#DEFAULT}</li>
     * <li>{@code with-view-nodes}: if {@code true}, the tool execution will return the IDs of the view nodes</li>
     * </ul>
     */
    @Override
    WorkflowToolResult execute(String parameters, PortObject[] inputs, ExecutionContext exec,
        Map<String, String> executionHints);

    /**
     * Executes the workflow-tool with the given parameters and input-references while re-using the provided
     * workflow-executor that carries out the tool-execution in the very same workflow.
     *
     * @param workflowExecutor the executor to use for executing the workflow-tool
     * @param parameters the parameters to use for the tool execution
     * @param inputs the input references to use for the tool execution (and connect to component representing the tool)
     * @param exec the execution context for cancellation
     * @param executionHints optional execution hints controlling the tool execution Accepted execution-hints:
     *            <ul>
     *            <li>{@code execution-mode}: see {@link ExecutionMode} for possible values, defaults to
     *            {@link ExecutionMode#DETACHED}</li>
     *            <li>{@code with-view-nodes}: if {@code true}, the tool execution will return the IDs of the view
     *            nodes</li>
     *            </ul>
     * @return the tool result
     *
     * @noreference This method is not intended to be referenced by clients.
     */
    WorkflowToolResult execute(final CombinedExecutor workflowExecutor, final String parameters,
        final List<CombinedExecutor.PortId> inputs, final ExecutionContext exec,
        final Map<String, String> executionHints);

    /**
     * The tool execution result with additional information only relevant for workflow-tool-execution.
     *
     * @param message see {@link ToolValue.ToolResult#message()}
     * @param outputIds port references of the outputs; only present and relevant if executed with a 'combined executor'
     *            - see {@link WorkflowToolValue#execute(CombinedExecutor, String, List, ExecutionContext, Map)} -
     *            otherwise {@code null}
     * @param outputs see {@link ToolValue.ToolResult#outputs()}
     * @param virtualProject the virtual project containing the workflow that has been used for the tool execution. It's
     *            {@code null} if
     *            <ul>
     *            <li>execution-mode is not 'detached' and view-node-ids are not to be included)</li>
     *            <li>if the tool was executed via
     *            {@link WorkflowToolValue#execute(CombinedExecutor, String, List, ExecutionContext, Map)}</li>
     *            </ul>
     *            NOTE: If not {@code null} it requires the caller to take care of disposing the 'virtual workflow'.
     * @param viewNodeIds an array of node IDs of the view nodes in the workflow; {@code null} if view-nodes aren't to
     *            be returned
     * @noreference This record is not intended to be referenced by clients.
     */
    public record WorkflowToolResult(String message, String[] outputIds, PortObject[] outputs,
        WorkflowManager virtualProject, String[] viewNodeIds) implements ToolResult {
        //
    }
}
