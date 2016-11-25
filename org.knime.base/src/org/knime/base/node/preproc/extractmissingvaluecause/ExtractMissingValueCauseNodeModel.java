/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.extractmissingvaluecause;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingValue;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.util.UniqueNameGenerator;

/**
 * The {@link NodeModel} implementation of the missing value extractor node.
 *
 * @author Simon Schmid
 */
final class ExtractMissingValueCauseNodeModel extends NodeModel {

    private final SettingsModelBoolean m_isFiltered = createIsFilteredModel();

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_suffix = createSuffixModel();

    /** @return the 'is filtered' model for both dialog and model. */
    static SettingsModelBoolean createIsFilteredModel() {
        return new SettingsModelBoolean("isFiltered", true);
    }

    /** @return the column select model, used in both dialog and model. */
    static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select");
    }

    /** @return the suffix model used in both dialog and model. */
    static SettingsModelString createSuffixModel() {
        return new SettingsModelString("suffix", " (error cause)");
    }

    /** One in, one out. */
    ExtractMissingValueCauseNodeModel() {
        super(1, 1);
    }

    /**
     * helper method to compute output
     */
    private void execute(final RowInput inData, final RowOutput output, final ExecutionContext exec,
        final long rowCount) throws Exception {
        final AppendErrorMessageCellFactory cellFactory = createCellFactory(inData.getDataTableSpec());
        DataRow row;
        long currentRowIndex = 0;
        while ((row = inData.poll()) != null) {
            exec.checkCanceled();
            // set progress if not streaming
            if (rowCount >= 0) {
                exec.setProgress(currentRowIndex / (double)rowCount);
            }
            final long currentRowIndexFinal = currentRowIndex;
            exec.setMessage(() -> "Row " + currentRowIndexFinal + "/" + rowCount);
            // compute new cells
            Optional<DataCell[]> newCellsOptional = cellFactory.getCellsOptional(row);
            if (newCellsOptional.isPresent()) {
                DataCell[] newCells= newCellsOptional.get();
                output.push(new AppendedColumnRow(row, newCells));
            }
            currentRowIndex += 1;
        }
        inData.close();
        output.close();
    }

    /**
     * @param inSpec Current input spec
     * @return The CR describing the output
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        AppendErrorMessageCellFactory cellFac = createCellFactory(inSpec);
        rearranger.append(cellFac);
        return rearranger;
    }

    /** @param inSpec Current input spec
     * @return The cell factory for the output cells.
     */
    private AppendErrorMessageCellFactory createCellFactory(final DataTableSpec inSpec) {

        String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        String suffix = m_suffix.getStringValue();

        int[] includeIndexes = Arrays.stream(includeList).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
        UniqueNameGenerator nameGen = new UniqueNameGenerator(inSpec);

        DataColumnSpec[] newCols = Arrays.stream(includeList)
                .map(s -> nameGen.newColumn(s + suffix, StringCell.TYPE))
                .toArray(DataColumnSpec[]::new);
        return new AppendErrorMessageCellFactory(includeIndexes, newCols);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
        throws Exception {

        final ColumnRearranger columnRearranger = createColumnRearranger(inObjects[0].getDataTableSpec());
        if (!m_isFiltered.getBooleanValue()) {
            final BufferedDataTable out = exec.createColumnRearrangeTable(inObjects[0], columnRearranger, exec);

            return new BufferedDataTable[]{out};

        } else {
            exec.setProgress(0);
            final BufferedDataTableRowOutput out =
                new BufferedDataTableRowOutput(exec.createDataContainer(columnRearranger.createSpec()));
            execute(new DataTableRowInput(inObjects[0]), out, exec, inObjects[0].size());

            return new BufferedDataTable[]{out.getDataTable()};
        }
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public StreamableOperatorInternals saveInternals() {
                return null;
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];
                ExtractMissingValueCauseNodeModel.this.execute(in, out, exec, -1);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_isFiltered.saveSettingsTo(settings);
        m_colSelect.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_isFiltered.validateSettings(settings);
        m_colSelect.validateSettings(settings);
        m_suffix.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_isFiltered.loadSettingsFrom(settings);
        m_colSelect.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    final class AppendErrorMessageCellFactory extends AbstractCellFactory {

        private final int[] m_includeIndices;
        private final DataCell[] m_defaultReturnAllMissings;


        /**
         * @param includeIndices
         * @param newCols
         */
        AppendErrorMessageCellFactory(final int[] includeIndices, final DataColumnSpec[] newCols) {
            super(newCols);
            m_includeIndices = includeIndices;
            m_defaultReturnAllMissings = new DataCell[m_includeIndices.length];
            Arrays.fill(m_defaultReturnAllMissings, DataType.getMissingCell());
        }

        @Override
        public DataCell[] getCells(final DataRow row) {
            return getCellsOptional(row).orElseThrow(
                () -> new IllegalStateException("Not supposed to be null at this point"));
        }

        Optional<DataCell[]> getCellsOptional(final DataRow row) {
            // check if the row should be filtered (or the default return value applies)
            boolean hasMissingCells = Arrays.stream(m_includeIndices)
                    .mapToObj(i -> row.getCell(i))
                    .anyMatch(c -> c.isMissing());

            final DataCell[] cells;
            if (hasMissingCells) { // if it has missing -- extract the error messages into new array
                cells = new DataCell[m_includeIndices.length];
                // add error messages
                for (int i = 0; i < m_includeIndices.length; i++) {
                    final DataCell cell = row.getCell(m_includeIndices[i]);
                    if (cell.isMissing()) {
                        String error = ((MissingValue)cell).getError();
                        if (error == null) {
                            cells[i] = new StringCell("");
                        } else {
                            cells[i] = new StringCell(error);
                        }
                    } else {
                        cells[i] = DataType.getMissingCell();
                    }
                }
            } else if (m_isFiltered.getBooleanValue()) { // no missings and rows to be removed ... null return
                cells = null;
            } else { // keep rows (even though non is missing), default return type (all missing cells in error cols)
                cells = m_defaultReturnAllMissings;
            }

            return Optional.ofNullable(cells);
        }

    }
}
