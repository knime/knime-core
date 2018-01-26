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
 *   Jan 25, 2018 (wiswedel): created
 */
package org.knime.testing.node.blob.create;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.testing.data.blob.LargeBlobCell;

/**
 * Model for "Create Test Blobs" nodes.
 *
 * @author wiswedel
 */
final class CreateTestBlobNodeModel extends NodeModel {

    private final SettingsModelIntegerBounded m_countModel = createCountModel();

    CreateTestBlobNodeModel() {
        super(0, 1);
    }

    /** @return settings model to enter count. */
    static final SettingsModelIntegerBounded createCountModel() {
        return new SettingsModelIntegerBounded("count", 100, 1, Integer.MAX_VALUE);
    }

    private DataTableSpec createTableSpec() {
        return new DataTableSpec(new DataColumnSpecCreator("test-blob", LargeBlobCell.TYPE).createSpec(),
            new DataColumnSpecCreator("blob-identifier", StringCell.TYPE).createSpec());
    }

    private static void fillOutput(final RowOutput output, final int start, final int count,
        final ExecutionContext exec) throws Exception {
        for (int i = 0; i < count; i++) {
            exec.setProgress(i / (double)count, String.format("Row %d", start + i + 1));
            exec.checkCanceled();
            String identifier = String.format("identifier-%03d", start + i);
            output.push(new DefaultRow(RowKey.createRowKey((long)(start + i)), new LargeBlobCell(identifier),
                new StringCell(identifier)));
        }
        output.close();
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createTableSpec()};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        // create two table and concatenate them -- both tables will then not be directly in the output and this
        // runs extra code paths in the core.
        BufferedDataContainer container1 = exec.createDataContainer(createTableSpec());
        BufferedDataContainer container2 = exec.createDataContainer(createTableSpec());

        BufferedDataTableRowOutput rowOut1 = new BufferedDataTableRowOutput(container1);
        BufferedDataTableRowOutput rowOut2 = new BufferedDataTableRowOutput(container2);
        int totalCount = m_countModel.getIntValue();
        fillOutput(rowOut1, 0, totalCount / 2, exec.createSubExecutionContext(1 / 3.0));

        // weird math? : in case it's an odd number
        fillOutput(rowOut2, totalCount / 2, totalCount - totalCount / 2, exec.createSubExecutionContext(1 / 3.0));
        BufferedDataTable tableOut1Global = rowOut1.getDataTable(); // table will be (indirectly) contained in output
        BufferedDataTable tableOut2Local = rowOut2.getDataTable();  // table will be copied and not be put in output

        BufferedDataContainer container3 = exec.createDataContainer(createTableSpec());
        ExecutionContext copyExec = exec.createSubExecutionContext(1 / 3.0);
        long l = 0L;
        long tableCount = tableOut2Local.size();
        for (DataRow r : tableOut2Local) {
            copyExec.checkCanceled();
            copyExec.setProgress(l++ / (double)tableCount, String.format("Copying 2nd table %d/%d", l, tableCount));
            container3.addRowToTable(r);
        }
        container3.close();
        BufferedDataTable tableOut2Global = container3.getTable();

        BufferedDataTable outputTable = exec.createConcatenateTable(
            exec, Optional.empty(), false, tableOut1Global, tableOut2Global);
        return new BufferedDataTable[]{outputTable};

    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return super.getInputPortRoles();
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                fillOutput((RowOutput)outputs[0], 0, m_countModel.getIntValue(), exec);
            }
        };
    }

    @Override
    protected void reset() {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_countModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_countModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_countModel.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

}
