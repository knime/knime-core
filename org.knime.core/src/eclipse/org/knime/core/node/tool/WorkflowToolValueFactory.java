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
package org.knime.core.node.tool;

import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.node.port.PortObject;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WorkflowToolValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    @Override
    public ReadValue createReadValue(final StructReadAccess access) {
        return new DefaultWorkflowToolReadValue(access);
    }

    @Override
    public WriteValue<?> createWriteValue(final StructWriteAccess access) {
        return new DefaultWorkflowToolWriteValue(access);
    }

    @Override
    public DataSpec getSpec() {
        return new StructDataSpec(
            StringDataSpec.INSTANCE,// name
            StringDataSpec.INSTANCE,// description
            StringDataSpec.INSTANCE,// parameter schema
            VarBinaryDataSpec.INSTANCE); // binary representation
    }

    interface WorkflowToolReadValue extends ReadValue, ToolValue {

    }

    private static final class DefaultWorkflowToolReadValue implements WorkflowToolReadValue {


        private final StringReadAccess m_name;

        private final StringReadAccess m_description;

        private final StringReadAccess m_parameterSchema;

        private final VarBinaryReadAccess m_binaryRepresentation;

        private DefaultWorkflowToolReadValue(final StructReadAccess access) {
            m_name = access.getAccess(0);
            m_description = access.getAccess(1);
            m_parameterSchema = access.getAccess(2);
            m_binaryRepresentation = access.getAccess(3);
        }

        @Override
        public WorkflowToolCell getDataCell() {
            throw new UnsupportedOperationException();
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
        public Input[] getInputs() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Output[] getOutputs() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public ToolResult execute(final String parameters, final PortObject[] inputs) {
            return getDataCell().execute(parameters, inputs);
        }

    }

    interface WorkflowToolWriteValue extends WriteValue<WorkflowToolCell> {

    }

    private static final class DefaultWorkflowToolWriteValue implements WorkflowToolWriteValue {

        private final StringWriteAccess m_name;
        private final StringWriteAccess m_description;
        private final StringWriteAccess m_parameterSchema;
        private final VarBinaryWriteAccess m_binaryRepresentation;

        private DefaultWorkflowToolWriteValue(final StructWriteAccess access) {
            m_name = access.getWriteAccess(0);
            m_description = access.getWriteAccess(1);
            m_parameterSchema = access.getWriteAccess(2);
            m_binaryRepresentation = access.getWriteAccess(3);
        }

        @Override
        public void setValue(final WorkflowToolCell cell) {
            m_name.setStringValue(cell.getName());
            m_description.setStringValue(cell.getDescription());
            m_parameterSchema.setStringValue(cell.getParameterSchema());
            m_binaryRepresentation.setByteArray(cell.getWorkflow());
        }

    }

}
