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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.capture.WorkflowSegment;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class WorkflowToolCell extends DataCell implements WorkflowToolValue {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Data type of the {@link WorkflowToolCell}.
     */
    public static final DataType TYPE = DataType.getType(WorkflowToolCell.class);

    private final String m_name;

    private final String m_description;

    private final String m_parameterSchema;

    private final Input[] m_inputs;

    private final Output[] m_outputs;

    private final byte[] m_workflow;

    /**
     * TODO
     *
     * @param name
     * @param description
     * @param parameterSchema
     * @param inputs
     * @param outputs
     * @param workflowSegment
     */
    public WorkflowToolCell(final String name, final String description, final String parameterSchema,
        final Input[] inputs, final Output[] outputs, final WorkflowSegment workflowSegment) {
        this(name, description, parameterSchema, inputs, outputs, serializeWorkflowSegment(workflowSegment));
    }

    private static byte[] serializeWorkflowSegment(final WorkflowSegment workflowSegment) {
        try (var byteOut = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(byteOut)) {
            workflowSegment.save(zipOut);
            return byteOut.toByteArray();
        } catch (IOException ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }

    WorkflowToolCell(final String name, final String description, final String parameterSchema, final Input[] inputs,
        final Output[] outputs, final byte[] workflow) {
        m_name = name;
        m_description = description;
        m_parameterSchema = parameterSchema;
        m_inputs = inputs;
        m_outputs = outputs;
        m_workflow = workflow;
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
    public Input[] getInputs() {
        return m_inputs;
    }

    @Override
    public Output[] getOutputs() {
        return m_outputs;
    }

    @Override
    public byte[] getWorkflow() {
        return m_workflow;
    }

    @Override
    public ToolResult execute(final String parameters, final PortObject[] inputs) {
        System.out.println("Executing with parameters: " + parameters);
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        // TODO
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int hashCode() {
        // TODO
        return HashCodeBuilder.reflectionHashCode(this);
    }

    // TODO needed for the registry to properly register the cell type. Looks more like a bug in the registry, though.
    public static final class WorkflowToolCellSerializer implements DataCellSerializer<WorkflowToolCell> {
        @Override
        public void serialize(final WorkflowToolCell cell, final DataCellDataOutput output) throws IOException {
            output.writeUTF(cell.getName());
            output.writeUTF(cell.getDescription());
            output.writeUTF(cell.getParameterSchema());
            var wfBytes = cell.getWorkflow();
            output.writeInt(wfBytes.length);
            output.write(wfBytes);
        }

        @Override
        public WorkflowToolCell deserialize(final DataCellDataInput input) throws IOException {
            final String name = input.readUTF();
            final String description = input.readUTF();
            final String parameterSchema = input.readUTF();
            final byte[] workflow = new byte[input.readInt()];
            input.readFully(workflow);
            return new WorkflowToolCell(name, description, parameterSchema, new Input[0], new Output[0], workflow);
        }
    }

}
