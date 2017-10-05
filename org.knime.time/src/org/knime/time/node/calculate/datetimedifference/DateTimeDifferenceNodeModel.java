/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 5, 2017 (simon): created
 */
package org.knime.time.node.calculate.datetimedifference;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.util.Granularity;
import org.knime.time.util.SettingsModelDateTime;

/**
 * The node model of the node which calculates differences between two date&time values.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DateTimeDifferenceNodeModel extends NodeModel {
    private final SettingsModelString m_col1stSelectModel = createColSelectModel(1);

    private final SettingsModelString m_col2ndSelectModel = createColSelectModel(2);

    private final SettingsModelString m_modusSelectModel = createModusSelection();

    private final SettingsModelDateTime m_fixedDateTimeModel = createDateTimeModel();

    private final SettingsModelString m_calculationSelectModel = createCalculationSelection();

    private final SettingsModelString m_granularityModel = createGranularityModel(m_calculationSelectModel);

    private final SettingsModelString m_newColNameModel = createNewColNameModel();

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createColSelectModel(final int i) {
        return new SettingsModelString("col_select" + i, null);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createModusSelection() {
        return new SettingsModelString("modus", ModusOptions.Use2ndColumn.name());
    }

    /** @return the date time model, used in both dialog and model. */
    static SettingsModelDateTime createDateTimeModel() {
        return new SettingsModelDateTime("fixed_date_time", ZonedDateTime.now());
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createCalculationSelection() {
        return new SettingsModelString("output", OutputMode.Granularity.name());
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createGranularityModel(final SettingsModelString calcSelectionModel) {
        final SettingsModelString granularityModel = new SettingsModelString("granularity", Granularity.DAY.toString());
        calcSelectionModel.addChangeListener(
            l -> granularityModel.setEnabled(calcSelectionModel.getStringValue().equals(OutputMode.Granularity.name())));
        return granularityModel;
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createNewColNameModel() {
        return new SettingsModelString("new_col_name", "date&time diff");
    }

    DateTimeDifferenceNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final String colName1 = m_col1stSelectModel.getStringValue();
        final String colName2 = m_col2ndSelectModel.getStringValue();
        if (colName1 == null) {
            throw new InvalidSettingsException("Node must be configured!");
        }
        if (inSpecs[0].findColumnIndex(colName1) < 0) {
            throw new InvalidSettingsException("Column " + colName1 + " not found in input table!");
        }
        final DataType type1 = inSpecs[0].getColumnSpec(colName1).getType();
        if (!(type1.isCompatible(LocalDateValue.class) || type1.isCompatible(LocalTimeValue.class)
            || type1.isCompatible(LocalDateTimeValue.class) || type1.isCompatible(ZonedDateTimeValue.class))) {
            throw new InvalidSettingsException("Column " + colName1 + " is not compatible!");
        }
        if (m_modusSelectModel.getStringValue().equals(ModusOptions.Use2ndColumn.name())) {
            if (inSpecs[0].findColumnIndex(colName2) < 0) {
                throw new InvalidSettingsException("Column " + colName2 + " not found in input table!");
            }
            final DataType type2 = inSpecs[0].getColumnSpec(colName2).getType();
            if (!(type2.isCompatible(LocalDateValue.class) || type2.isCompatible(LocalTimeValue.class)
                || type2.isCompatible(LocalDateTimeValue.class) || type2.isCompatible(ZonedDateTimeValue.class))) {
                throw new InvalidSettingsException("Column " + colName1 + " is not compatible!");
            }
        }
        return new DataTableSpec[]{
            new DataTableSpecCreator(inSpecs[0]).addColumns(createColumnSpec(inSpecs[0])).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        if (m_modusSelectModel.getStringValue().equals(ModusOptions.UsePreviousRow.name())) {
            final BufferedDataTableRowOutput out = new BufferedDataTableRowOutput(
                exec.createDataContainer(new DataTableSpecCreator(inData[0].getDataTableSpec())
                    .addColumns(createColumnSpec(inData[0].getDataTableSpec())).createSpec()));
            execute(new DataTableRowInput(inData[0]), out, exec, inData[0].size());
            return new BufferedDataTable[]{out.getDataTable()};
        } else {
            final ColumnRearranger r = createColumnRearranger(inData[0].getDataTableSpec());
            final BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], r, exec);
            return new BufferedDataTable[]{out};
        }
    }

    /**
     * helper method to compute output
     */
    private void execute(final RowInput inData, final RowOutput output, final ExecutionContext exec,
        final long rowCount) throws Exception {
        DataRow row;
        DataRow previousRow = inData.poll();

        final int colIdx1 = inData.getDataTableSpec().findColumnIndex(m_col1stSelectModel.getStringValue());
        final DataType type = inData.getDataTableSpec().getColumnSpec(colIdx1).getType();
        long currentRowIndex = 0;

        output.push(new AppendedColumnRow(previousRow,
            new MissingCell("No previous row for calculating difference available.")));

        while ((row = inData.poll()) != null) {
            exec.checkCanceled();
            // set progress if not streaming
            if (rowCount >= 0) {
                exec.setProgress(currentRowIndex / (double)rowCount);
            }
            final long currentRowIndexFinal = currentRowIndex;
            exec.setMessage(() -> "Row " + currentRowIndexFinal + "/" + rowCount);
            // compute new cells
            DataCell newCell;

            if (type.isCompatible(LocalDateValue.class)) {
                newCell = calculateDate(previousRow.getCell(colIdx1), row.getCell(colIdx1), null);
            } else {
                newCell = calculateTime(previousRow.getCell(colIdx1), row.getCell(colIdx1), null);
            }
            output.push(new AppendedColumnRow(row, newCell));

            previousRow = row;
            currentRowIndex++;
        }
        inData.close();
        output.close();
    }

    private DataColumnSpec createColumnSpec(final DataTableSpec inSpec) {
        if (m_calculationSelectModel.getStringValue().equals(OutputMode.Granularity.name())) {
            return new UniqueNameGenerator(inSpec).newColumn(m_newColNameModel.getStringValue(), LongCellFactory.TYPE);
        }
        final DataType type = inSpec.getColumnSpec(m_col1stSelectModel.getStringValue()).getType();
        if (type.isCompatible(LocalDateValue.class)) {
            return new UniqueNameGenerator(inSpec).newColumn(m_newColNameModel.getStringValue(),
                PeriodCellFactory.TYPE);
        } else {
            return new UniqueNameGenerator(inSpec).newColumn(m_newColNameModel.getStringValue(),
                DurationCellFactory.TYPE);
        }
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(spec);

        final ZonedDateTime fixedDateTime;
        if (m_modusSelectModel.getStringValue().equals(ModusOptions.UseExecutionTime.name())) {
            fixedDateTime = ZonedDateTime.now();
        } else if (m_modusSelectModel.getStringValue().equals(ModusOptions.UseFixedTime.name())) {
            fixedDateTime = m_fixedDateTimeModel.getZonedDateTime();
        } else {
            fixedDateTime = null;
        }

        final int colIdx1 = spec.findColumnIndex(m_col1stSelectModel.getStringValue());
        final int colIdx2 = spec.findColumnIndex(m_col2ndSelectModel.getStringValue());

        final AbstractCellFactory cellFac;
        final DataType type = spec.getColumnSpec(colIdx1).getType();

        if (type.isCompatible(LocalDateValue.class)) {
            cellFac = new DateDifferenceCellFactory(colIdx1, colIdx2,
                fixedDateTime == null ? null : fixedDateTime.toLocalDate(), createColumnSpec(spec));
        } else {
            cellFac = new TimeDifferenceCellFactory(colIdx1, colIdx2, fixedDateTime, createColumnSpec(spec));
        }
        rearranger.append(cellFac);
        return rearranger;
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
        m_col1stSelectModel.saveSettingsTo(settings);
        m_col2ndSelectModel.saveSettingsTo(settings);
        m_modusSelectModel.saveSettingsTo(settings);
        m_fixedDateTimeModel.saveSettingsTo(settings);
        m_calculationSelectModel.saveSettingsTo(settings);
        m_granularityModel.saveSettingsTo(settings);
        m_newColNameModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_col1stSelectModel.validateSettings(settings);
        m_col2ndSelectModel.validateSettings(settings);
        m_modusSelectModel.validateSettings(settings);

        SettingsModelString temp = createModusSelection();
        temp.loadSettingsFrom(settings);
        try {
            ModusOptions.valueOf(temp.getStringValue());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException("Unknown difference modus '" + temp.getStringValue() + "'");
        }

        m_fixedDateTimeModel.validateSettings(settings);
        m_calculationSelectModel.validateSettings(settings);
        m_granularityModel.validateSettings(settings);
        m_newColNameModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_col1stSelectModel.loadSettingsFrom(settings);
        m_col2ndSelectModel.loadSettingsFrom(settings);
        m_modusSelectModel.loadSettingsFrom(settings);
        m_fixedDateTimeModel.loadSettingsFrom(settings);
        m_calculationSelectModel.loadSettingsFrom(settings);
        m_granularityModel.loadSettingsFrom(settings);
        m_newColNameModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{m_modusSelectModel.getStringValue().equals(ModusOptions.UsePreviousRow.name())
            ? InputPortRole.NONDISTRIBUTED_STREAMABLE : InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        if (!m_modusSelectModel.getStringValue().equals(ModusOptions.UsePreviousRow.name())) {
            return createColumnRearranger((DataTableSpec)inSpecs[0]).createStreamableFunction(0, 0);
        } else {
            return new StreamableOperator() {
                @Override
                public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                    throws Exception {
                    execute((RowInput)inputs[0], (RowOutput)outputs[0], exec, -1);
                }
            };
        }
    }

    private final class DateDifferenceCellFactory extends SingleCellFactory {
        private final int m_colIdx1;

        private final int m_colIdx2;

        private LocalDate m_fixedDateTime;

        public DateDifferenceCellFactory(final int colIdx1, final int colIdx2, final LocalDate fixedDateTime,
            final DataColumnSpec newColSpec) {
            super(newColSpec);
            m_colIdx1 = colIdx1;
            m_colIdx2 = colIdx2;
            m_fixedDateTime = fixedDateTime;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIdx1);
            return calculateDate(cell, row.getCell(m_colIdx2), m_fixedDateTime);
        }
    }

    private DataCell calculateDate(final DataCell cell, final DataCell referenceCell, LocalDate fixedDateTime) {
        if (cell.isMissing()) {
            return new MissingCell("Cell for calculating difference is missing.");
        } else if (fixedDateTime == null && referenceCell.isMissing()) {
            return new MissingCell("Reference cell for calculating difference is missing.");
        }

        fixedDateTime = fixedDateTime == null ? ((LocalDateValue)referenceCell).getLocalDate() : fixedDateTime;

        if (m_calculationSelectModel.getStringValue().equals(OutputMode.Duration.name())) {
            final Period diffPeriod = Period.between(((LocalDateValue)cell).getLocalDate(), fixedDateTime);
            return PeriodCellFactory.create(diffPeriod);
        } else {
            final Granularity granularity = Granularity.fromString(m_granularityModel.getStringValue());
            if (granularity.isPartOfDate()) {
                return LongCellFactory
                    .create(granularity.between(((LocalDateValue)cell).getLocalDate(), fixedDateTime));
            } else {
                final long days = Granularity.DAY.between(((LocalDateValue)cell).getLocalDate(), fixedDateTime);
                if (granularity.equals(Granularity.HOUR)) {
                    return LongCellFactory.create(days * 24);
                } else if (granularity.equals(Granularity.MINUTE)) {
                    return LongCellFactory.create(days * 24 * 60);
                } else if (granularity.equals(Granularity.SECOND)) {
                    return LongCellFactory.create(days * 24 * 60 * 60);
                } else if (granularity.equals(Granularity.MILLISECOND)) {
                    return LongCellFactory.create(days * 24 * 60 * 60 * 1_000);
                } else if (granularity.equals(Granularity.NANOSECOND)) {
                    return LongCellFactory.create(days * 24 * 60 * 60 * 1_000_000_000);
                } else {
                    throw new IllegalStateException("Unknow granularity: " + granularity.toString());
                }
            }
        }
    }

    private final class TimeDifferenceCellFactory extends SingleCellFactory {
        private final int m_colIdx1;

        private final int m_colIdx2;

        private ZonedDateTime m_fixedDateTime;

        public TimeDifferenceCellFactory(final int colIdx1, final int colIdx2, final ZonedDateTime fixedDateTime,
            final DataColumnSpec newColSpecs) {
            super(newColSpecs);
            m_colIdx1 = colIdx1;
            m_colIdx2 = colIdx2;
            m_fixedDateTime = fixedDateTime;
        }

        /**
         * {@inheritDoc}m_col1stSelectModel.getStringValue()
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIdx1);
            return calculateTime(cell, row.getCell(m_colIdx2), m_fixedDateTime);
        }
    }

    private DataCell calculateTime(final DataCell cell, final DataCell referenceCell,
        final ZonedDateTime fixedDateTime) {
        if (cell.isMissing()) {
            return new MissingCell("Cell for calculating difference is missing.");
        } else if (fixedDateTime == null && referenceCell.isMissing()) {
            return new MissingCell("Reference cell for calculating difference is missing.");
        }

        final Temporal temporal1;
        final Temporal temporal2;
        if (cell instanceof ZonedDateTimeValue) {
            temporal1 = ((ZonedDateTimeValue)cell).getZonedDateTime();
            temporal2 = fixedDateTime == null ? ((ZonedDateTimeValue)referenceCell).getZonedDateTime() : fixedDateTime;
        } else if (cell instanceof LocalDateTimeValue) {
            temporal1 = ((LocalDateTimeValue)cell).getLocalDateTime();
            temporal2 = fixedDateTime == null ? ((LocalDateTimeValue)referenceCell).getLocalDateTime()
                : fixedDateTime.toLocalDateTime();
        } else {
            temporal1 = ((LocalTimeValue)cell).getLocalTime();
            temporal2 =
                fixedDateTime == null ? ((LocalTimeValue)referenceCell).getLocalTime() : fixedDateTime.toLocalTime();
        }

        if (m_calculationSelectModel.getStringValue().equals(OutputMode.Duration.name())) {
            final Duration diffDuration = Duration.between(temporal1, temporal2);
            return DurationCellFactory.create(diffDuration);
        } else {
            final Granularity granularity = Granularity.fromString(m_granularityModel.getStringValue());
            return LongCellFactory.create(granularity.between(temporal1, temporal2));
        }
    }
}
