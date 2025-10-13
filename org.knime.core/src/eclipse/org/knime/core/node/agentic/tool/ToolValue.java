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

import java.util.Map;

import org.knime.core.data.DataValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

/**
 * A ToolValue that represents a tool that can be used by an AI Agent.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.5
 */
public interface ToolValue extends DataValue {

    @SuppressWarnings("javadoc")
    UtilityFactory UTILITY = new ExtensibleUtilityFactory(ToolValue.class) {

        @Override
        public String getName() {
            return "Tool";
        }
    };

    /**
     * @return Name of the tool used by an AI Agent to pick a tool.
     */
    String getName();

    /**
     * @return Description of the tool used by an AI Agent to pick a tool; can be {@code null} if there is no
     *         description
     */
    String getDescription();

    /**
     * @return the JSON schema of the parameters used by the tool; can be {@code null} if there are no parameters
     */
    String getParameterSchema();

    /**
     * @return data inputs of the tool which the agent has to provide in {@link #execute(String, PortObject[])}
     */
    ToolPort[] getInputs();

    /**
     * @return data outputs that the tool produces in {@link #execute(String, PortObject[])}
     */
    ToolPort[] getOutputs();

    /**
     * Represents a port of the tool. Data inputs are the inputs that the agent has to provide in order to execute the
     * tool.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param type the type of the port
     * @param name the name of the port which the agent can use to identify the port
     * @param description the description of the port which the agent can use to pick the port
     * @param spec optional spec of the port if available which gives more context to the agent
     */
    record ToolPort(String type, String name, String description, String spec) {
    }

    /**
     * Represents the result of executing a tool. The result contains a message that is presented to the agent, as well
     * as output data that the agent can use to execute subsequent tools with.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany execution failed
     */
    interface ToolResult {

        /**
         *
         * @return message the message that is presented to the agent (e.g. success or error message)
         */
        String message();

        /**
         * @return the output data that the agent can use to execute subsequent tools with, {@code null} if the
         *         execution failed
         */
        PortObject[] outputs();

    }

    /**
     * Executes the tool with the given parameters and inputs.
     *
     * @param parameters the parameters to use for the tool execution
     * @param inputs the input data to use for the tool execution
     * @param exec the execution context for cancellation and to create tables
     * @param executionHints optional hints controlling the tool execution - doesn't need to be respected by the
     *            implementation
     * @return the tool result
     */
    ToolResult execute(String parameters, PortObject[] inputs, ExecutionContext exec,
        Map<String, String> executionHints);

}
