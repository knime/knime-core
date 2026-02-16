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
 *   Feb 16, 2026 (Carsten Haubold, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.agentic.tool;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.v2.filestore.AbstractFileStoreValueFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.agentic.tool.ToolValue.ToolPort;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.capture.CombinedExecutor;
import org.knime.core.node.workflow.capture.CombinedExecutor.PortId;
import org.knime.core.table.access.ByteAccess.ByteReadAccess;
import org.knime.core.table.access.ByteAccess.ByteWriteAccess;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.ByteDataSpec;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * Unified ValueFactory for reading and writing Tool instances (both WorkflowTool and MCPTool).
 * 
 * Uses a discriminator pattern with byte-indexed enum to support both tool types in a single column.
 * 
 * Arrow schema (9 fields):
 * - Field 0: tool_type (BYTE) - Enum index: 0=WORKFLOW, 1=MCP
 * - Field 1: name (STRING) - Common
 * - Field 2: description (STRING) - Common  
 * - Field 3: parameter_schema (STRING) - Common (JSON)
 * - Field 4: input_spec (LIST) - Workflow ports or null for MCP
 * - Field 5: output_spec (LIST) - Workflow ports or null for MCP
 * - Field 6: message_output_port_index (INT) - Workflow only (-1 for MCP)
 * - Field 7: workflow_filestore (unused for now, reserved for future use)
 * - Field 8: server_uri (STRING) - MCP only (null for Workflow)
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public class ToolValueFactory extends AbstractFileStoreValueFactory {

    @Override
    public UnifiedToolReadValue createReadValue(final StructReadAccess access) {
        return new UnifiedToolReadValue(access);
    }

    @Override
    public UnifiedToolWriteValue createWriteValue(final StructWriteAccess access) {
        return new UnifiedToolWriteValue(access);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataSpec getTableDataSpec() {
        return new StructDataSpec(
            ByteDataSpec.INSTANCE,           // 0: tool_type discriminator
            StringDataSpec.INSTANCE,         // 1: name
            StringDataSpec.INSTANCE,         // 2: description
            StringDataSpec.INSTANCE,         // 3: parameter_schema (JSON)
            new ListDataSpec(portDataSpec()),// 4: input_spec (Workflow ports or null)
            new ListDataSpec(portDataSpec()),// 5: output_spec (Workflow ports or null)
            IntDataSpec.INSTANCE,            // 6: message_output_port_index
            StringDataSpec.INSTANCE,         // 7: reserved (unused for now)
            StringDataSpec.INSTANCE          // 8: server_uri (MCP only)
        );
    }

    private static DataSpec portDataSpec() {
        return new StructDataSpec(//
            StringDataSpec.INSTANCE, // name
            StringDataSpec.INSTANCE, // description
            StringDataSpec.INSTANCE, // type
            StringDataSpec.INSTANCE  // optional spec
        );
    }

    /**
     * Unified ReadValue that can handle both WorkflowTool and MCPTool types.
     */
    protected final class UnifiedToolReadValue extends FileStoreReadValue<StructReadAccess>
        implements WorkflowToolValue {

        private final ByteReadAccess m_toolType;
        private final StringReadAccess m_name;
        private final StringReadAccess m_description;
        private final StringReadAccess m_parameterSchema;
        private final ListReadAccess m_inputPorts;
        private final ListReadAccess m_outputPorts;
        private final IntReadAccess m_messageOutputPortIndex;
        private final StringReadAccess m_serverUri;

        private UnifiedToolReadValue(final StructReadAccess access) {
            super(access);
            var tableDataAccess = getTableDataAccess();
            m_toolType = tableDataAccess.getAccess(0);
            m_name = tableDataAccess.getAccess(1);
            m_description = tableDataAccess.getAccess(2);
            m_parameterSchema = tableDataAccess.getAccess(3);
            m_inputPorts = tableDataAccess.getAccess(4);
            m_outputPorts = tableDataAccess.getAccess(5);
            m_messageOutputPortIndex = tableDataAccess.getAccess(6);
            // Field 7 is reserved/unused
            m_serverUri = tableDataAccess.getAccess(8);
        }

        /**
         * @return the tool type (WORKFLOW or MCP)
         */
        public ToolType getToolType() {
            return ToolType.fromIndex(m_toolType.getByteValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell createCell(final StructReadAccess tableDataAccess) {
            ByteReadAccess toolType = tableDataAccess.getAccess(0);
            StringReadAccess name = tableDataAccess.getAccess(1);
            StringReadAccess description = tableDataAccess.getAccess(2);
            StringReadAccess parameterSchema = tableDataAccess.getAccess(3);
            ListReadAccess inputPorts = tableDataAccess.getAccess(4);
            ListReadAccess outputPorts = tableDataAccess.getAccess(5);
            IntReadAccess messageOutputPortIndex = tableDataAccess.getAccess(6);
            
            ToolType type = ToolType.fromIndex(toolType.getByteValue());
            
            if (type == ToolType.WORKFLOW) {
                return new WorkflowToolCell(
                    name.getStringValue(),
                    description.getStringValue(),
                    parameterSchema.getStringValue(),
                    readToolPorts(inputPorts),
                    readToolPorts(outputPorts),
                    messageOutputPortIndex.getIntValue()
                );
            } else {
                // MCP tool - create appropriate cell
                // For now, return a simple WorkflowToolCell with empty ports
                // This should be replaced with proper MCPToolCell when available
                return new WorkflowToolCell(
                    name.getStringValue(),
                    description.getStringValue(),
                    parameterSchema.getStringValue(),
                    new ToolPort[0],
                    new ToolPort[0],
                    -1
                );
            }
        }

        @Override
        public String getName() {
            return m_name.getStringValue();
        }

        @Override
        public String getDescription() {
            return m_description.getStringValue();
        }

        @Override
        public String getParameterSchema() {
            return m_parameterSchema.getStringValue();
        }

        @Override
        public ToolPort[] getInputs() {
            if (getToolType() == ToolType.MCP) {
                return new ToolPort[0];
            }
            return readToolPorts(m_inputPorts);
        }

        /**
         * Read tool ports from accessor.
         * 
         * @param access the list access
         * @return array of tool ports
         */
        protected static ToolPort[] readToolPorts(final ListReadAccess access) {
            final StructReadAccess portAccess = access.getAccess();
            return IntStream.range(0, access.size())//
                .mapToObj(i -> {
                    access.setIndex(i);
                    return readToolPort(portAccess);
                })//
                .toArray(ToolPort[]::new);
        }

        private static ToolPort readToolPort(final StructReadAccess access) {
            final String name = access.<StringReadAccess> getAccess(0).getStringValue();
            final String description = access.<StringReadAccess> getAccess(1).getStringValue();
            final String type = access.<StringReadAccess> getAccess(2).getStringValue();
            var specAccess = access.<StringReadAccess> getAccess(3);
            final String spec = specAccess.isMissing() ? null : specAccess.getStringValue();
            return new ToolPort(type, name, description, spec);
        }

        @Override
        public ToolPort[] getOutputs() {
            if (getToolType() == ToolType.MCP) {
                return new ToolPort[0];
            }
            return readToolPorts(m_outputPorts);
        }

        @Override
        public int getMessageOutputPortIndex() {
            return m_messageOutputPortIndex.getIntValue();
        }

        /**
         * @return the MCP server URI (null for Workflow tools)
         */
        public String getServerUri() {
            return m_serverUri.isMissing() ? null : m_serverUri.getStringValue();
        }

        @Override
        public WorkflowToolResult execute(final String parameters, final PortObject[] inputs,
            final ExecutionContext exec, final Map<String, String> executionHints) {
            return ((WorkflowToolValue)super.getDataCell()).execute(parameters, inputs, exec, executionHints);
        }

        @Override
        public WorkflowToolResult execute(final CombinedExecutor workflowExecutor, final String parameters,
            final List<PortId> inputs, final ExecutionContext exec, final Map<String, String> executionHints) {
            return ((WorkflowToolValue)super.getDataCell()).execute(workflowExecutor, parameters, inputs, exec,
                executionHints);
        }
    }

    /**
     * Unified WriteValue that can handle both WorkflowTool and MCPTool types.
     */
    protected final class UnifiedToolWriteValue extends FileStoreWriteValue<WorkflowToolValue, StructWriteAccess> {

        private UnifiedToolWriteValue(final StructWriteAccess access) {
            super(access);
        }

        /**
         * Write tool ports to accessor.
         * 
         * @param access the list access
         * @param ports the tool ports to write
         */
        protected static void writeToolPorts(final ListWriteAccess access, final ToolPort[] ports) {
            final StructWriteAccess portAccess = access.getWriteAccess();
            access.create(ports.length);
            for (int i = 0; i < ports.length; i++) {
                final ToolPort port = ports[i];
                access.setWriteIndex(i);
                writeToolPort(portAccess, port);
            }
        }

        private static void writeToolPort(final StructWriteAccess access, final ToolPort port) {
            access.<StringWriteAccess> getWriteAccess(0).setStringValue(port.name());
            access.<StringWriteAccess> getWriteAccess(1).setStringValue(port.description());
            access.<StringWriteAccess> getWriteAccess(2).setStringValue(port.type());
            var specAccess = access.<StringWriteAccess> getWriteAccess(3);
            if (port.spec() == null) {
                specAccess.setMissing();
            } else {
                specAccess.setStringValue(port.spec());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isCorrespondingReadValue(final WorkflowToolValue value) {
            return value instanceof UnifiedToolReadValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected FileStoreCell getFileStoreCell(final WorkflowToolValue value) throws IOException {
            if (value instanceof WorkflowToolCell wtc) {
                return wtc;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void setTableData(final WorkflowToolValue value, final StructWriteAccess access) {
            // Set tool type (always WORKFLOW for now, as we only have WorkflowToolValue)
            ((ByteWriteAccess)access.getWriteAccess(0)).setByteValue(ToolType.WORKFLOW.getIndex());
            
            // Common fields
            ((StringWriteAccess)access.getWriteAccess(1)).setStringValue(value.getName());
            ((StringWriteAccess)access.getWriteAccess(2)).setStringValue(value.getDescription());
            ((StringWriteAccess)access.getWriteAccess(3)).setStringValue(value.getParameterSchema());
            
            // Workflow-specific fields
            writeToolPorts((ListWriteAccess)access.getWriteAccess(4), value.getInputs());
            writeToolPorts((ListWriteAccess)access.getWriteAccess(5), value.getOutputs());
            ((IntWriteAccess)access.getWriteAccess(6)).setIntValue(value.getMessageOutputPortIndex());
            
            // Field 7 is reserved/unused
            ((StringWriteAccess)access.getWriteAccess(7)).setMissing();
            
            // MCP field (null for Workflow)
            ((StringWriteAccess)access.getWriteAccess(8)).setMissing();
        }
    }
}
