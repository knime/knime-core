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

import java.io.IOException;
import java.util.Map;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.v2.filestore.AbstractFileStoreValueFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.agentic.tool.ToolValue.ToolPort;
import org.knime.core.node.port.PortObject;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * De-/serialization of {@link WorkflowToolValue}s.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class WorkflowToolValueFactory extends AbstractFileStoreValueFactory {

    @Override
    public WorkflowToolReadValue createReadValue(final StructReadAccess access) {
        return new WorkflowToolReadValue(access);
    }

    @Override
    public WorkflowToolWriteValue createWriteValue(final StructWriteAccess access) {
        return new WorkflowToolWriteValue(access);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataSpec getTableDataSpec() {
        return new StructDataSpec(StringDataSpec.INSTANCE, // name
            StringDataSpec.INSTANCE, // description
            StringDataSpec.INSTANCE, // parameter schema
            new ListDataSpec(portDataSpec()), // input ports
            new ListDataSpec(portDataSpec()), // output ports
            IntDataSpec.INSTANCE // message output port index
        );
    }

    private static DataSpec portDataSpec() {
        return new StructDataSpec(//
            StringDataSpec.INSTANCE, // name
            StringDataSpec.INSTANCE, // description
            StringDataSpec.INSTANCE, // type
            StringDataSpec.INSTANCE // optional spec
        );
    }

    private final class WorkflowToolReadValue extends FileStoreReadValue<StructReadAccess>
        implements WorkflowToolValue {

        private final StringReadAccess m_name;

        private final StringReadAccess m_description;

        private final StringReadAccess m_parameterSchema;

        private final ListReadAccess m_inputPorts;

        private final ListReadAccess m_outputPorts;

        private final IntReadAccess m_messageOutputPortIndex;

        private WorkflowToolReadValue(final StructReadAccess access) {
            super(access);
            var tableDataAccess = getTableDataAccess();
            m_name = tableDataAccess.getAccess(0);
            m_description = tableDataAccess.getAccess(1);
            m_parameterSchema = tableDataAccess.getAccess(2);
            m_inputPorts = tableDataAccess.getAccess(3);
            m_outputPorts = tableDataAccess.getAccess(4);
            m_messageOutputPortIndex = tableDataAccess.getAccess(5);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell createCell(final StructReadAccess tableDataAccess) {
            StringReadAccess name = tableDataAccess.getAccess(0);
            StringReadAccess description = tableDataAccess.getAccess(1);
            StringReadAccess parameterSchema = tableDataAccess.getAccess(2);
            ListReadAccess inputPorts = tableDataAccess.getAccess(3);
            ListReadAccess outputPorts = tableDataAccess.getAccess(4);
            IntReadAccess messageOutputPortIndex = tableDataAccess.getAccess(5);
            return new WorkflowToolCell(name.getStringValue(), description.getStringValue(),
                parameterSchema.getStringValue(), readToolPorts(inputPorts), readToolPorts(outputPorts),
                messageOutputPortIndex.getIntValue());
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
            return readToolPorts(m_inputPorts);
        }

        private static ToolPort[] readToolPorts(final ListReadAccess access) {
            final StructReadAccess portAccess = access.getAccess();
            return IntStream.range(0, access.size())//
                .mapToObj(i -> readToolPort(portAccess))//
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
            return readToolPorts(m_outputPorts);
        }

        @Override
        public int getMessageOutputPortIndex() {
            return m_messageOutputPortIndex.getIntValue();
        }

        @Override
        public WorkflowToolResult execute(final String parameters, final PortObject[] inputs,
            final ExecutionContext exec, final Map<String, String> executionHints) {
            return ((WorkflowToolValue)super.getDataCell()).execute(parameters, inputs, exec, executionHints);
        }

    }

    private final class WorkflowToolWriteValue extends FileStoreWriteValue<WorkflowToolValue, StructWriteAccess> {

        private WorkflowToolWriteValue(final StructWriteAccess access) {
            super(access);
        }

        private static void writeToolPorts(final ListWriteAccess access, final ToolPort[] ports) {
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
            return value instanceof WorkflowToolReadValue;
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
            ((StringWriteAccess)access.getWriteAccess(0)).setStringValue(value.getName());
            ((StringWriteAccess)access.getWriteAccess(1)).setStringValue(value.getDescription());
            ((StringWriteAccess)access.getWriteAccess(2)).setStringValue(value.getParameterSchema());
            writeToolPorts((ListWriteAccess)access.getWriteAccess(3), value.getInputs());
            writeToolPorts((ListWriteAccess)access.getWriteAccess(4), value.getOutputs());
            ((IntWriteAccess)access.getWriteAccess(5)).setIntValue(value.getMessageOutputPortIndex());
        }

    }

}
