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
package org.knime.testing.node.blob.verify;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.testing.data.blob.LargeBlobValue;

/**
 * Model for "Verify Test Blobs" nodes.
 * @author wiswedel
 */
final class VerifyTestBlobNodeModel extends NodeModel {

    private final SettingsModelColumnName m_blobColumnModel = createBlobColumnModel();

    private final SettingsModelColumnName m_verifyColumnModel = createVerifyColumnModel();

    VerifyTestBlobNodeModel() {
        super(1, 1);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
        String blobColumn = m_blobColumnModel.getColumnName();
        int blobIndex;
        if (blobColumn == null) {
            blobIndex = IntStream.range(0, spec.getNumColumns())
                .filter(i -> spec.getColumnSpec(i).getType().isCompatible(LargeBlobValue.class)).findFirst()
                .orElseThrow(() -> new InvalidSettingsException("No blob column in input"));
            m_blobColumnModel.setStringValue(spec.getColumnSpec(blobIndex).getName()); // auto-guess only if null
        } else {
            blobIndex = spec.findColumnIndex(blobColumn);
            CheckUtils.checkSetting(blobIndex >= 0, "no column \"%s\" in input", blobColumn);
            CheckUtils.checkSetting(spec.getColumnSpec(blobIndex).getType().isCompatible(LargeBlobValue.class),
                "Column \"%s\" is not a blob column", blobColumn);
        }

        String verifyColumn = m_verifyColumnModel.getColumnName();
        int verifyIndex;
        if (verifyColumn == null) {
            verifyIndex = IntStream.range(0, spec.getNumColumns())
                .filter(i -> spec.getColumnSpec(i).getType().isCompatible(StringValue.class)).findFirst()
                .orElseThrow(() -> new InvalidSettingsException("No string column in input"));
            m_verifyColumnModel.setStringValue(spec.getColumnSpec(verifyIndex).getName()); // auto-guess only if null
        } else {
            verifyIndex = spec.findColumnIndex(verifyColumn);
            CheckUtils.checkSetting(verifyIndex >= 0, "no column \"%s\" in input", verifyColumn);
            CheckUtils.checkSetting(spec.getColumnSpec(verifyIndex).getType().isCompatible(StringValue.class),
                "Column \"%s\" is not a string column", verifyColumn);
        }
        return new DataTableSpec[]{spec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable data = inData[0];
        final int blobColumn = data.getDataTableSpec().findColumnIndex(m_blobColumnModel.getColumnName());
        final int verifyColumn = data.getDataTableSpec().findColumnIndex(m_verifyColumnModel.getColumnName());
        long rowIndex = 0L;
        final long rowCount = data.size();
        for (DataRow r : data) {
            exec.checkCanceled();
            exec.setProgress(rowIndex / (double)rowCount,
                String.format("Row \"%s\" (%d/%d)", r.getKey(), rowIndex, rowCount));
            checkRow(r, blobColumn, verifyColumn);
            rowIndex++;
        }
        return inData;
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                RowInput input = (RowInput)inputs[0];
                RowOutput output = (RowOutput)outputs[0];
                long counter = 0L;
                DataRow row;
                final int blobColumn = input.getDataTableSpec().findColumnIndex(m_blobColumnModel.getColumnName());
                final int verifyColumn = input.getDataTableSpec().findColumnIndex(m_verifyColumnModel.getColumnName());
                while ((row = input.poll()) != null) {
                    exec.checkCanceled();
                    exec.setMessage(String.format("Row %d (\"%s\")", counter++, row.getKey()));
                    checkRow(row, blobColumn, verifyColumn);
                    output.push(row);
                }
                input.close();
                output.close();
            }
        };
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[] {InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[] {OutputPortRole.DISTRIBUTED};
    }

    private void checkRow(final DataRow input, final int blobColumn, final int verifyColumn) throws Exception {
        DataCell blobCell = input.getCell(blobColumn);
        DataCell verifyCell = input.getCell(verifyColumn);
        CheckUtils.checkArgument(!blobCell.isMissing(), "Blob column must not contain missing cells");
        CheckUtils.checkArgument(!verifyCell.isMissing(), "Verification column must not contain missing cells");
        LargeBlobValue blobValue = (LargeBlobValue)blobCell;
        String verifyString = ((StringValue)verifyCell).getStringValue();
        CheckUtils.checkArgument(Objects.equals(blobValue.getIdentifier(), verifyString),
            "Verification string doesn't match: \"%s\" vs. \"%s\"", blobValue.getIdentifier(), verifyString);
    }

    @Override
    protected void reset() {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_blobColumnModel.saveSettingsTo(settings);
        m_verifyColumnModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        SettingsModelColumnName cloneBlobModel = m_blobColumnModel.createCloneWithValidatedValue(settings);
        CheckUtils.checkSettingNotNull(cloneBlobModel.getColumnName(), "Column name must not be null");
        SettingsModelColumnName cloneVerifyModel = m_verifyColumnModel.createCloneWithValidatedValue(settings);
        CheckUtils.checkSettingNotNull(cloneVerifyModel.getColumnName(), "Column name must not be null");
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_blobColumnModel.loadSettingsFrom(settings);
        m_verifyColumnModel.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * @return
     */
    static SettingsModelColumnName createBlobColumnModel() {
        return new SettingsModelColumnName("blobColumn", null);
    }

    /**
     * @return
     */
    static final SettingsModelColumnName createVerifyColumnModel() {
        return new SettingsModelColumnName("verifyColumn", null);
    }

}