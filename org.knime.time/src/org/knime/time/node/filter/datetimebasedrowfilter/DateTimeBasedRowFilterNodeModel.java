/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 20, 2017 (simon): created
 */
package org.knime.time.node.filter.datetimebasedrowfilter;

import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.Granularity;
import org.knime.time.util.SettingsModelDateTime;

/**
 * The node model of the node which filters rows based on a time window on one of the new date&time columns.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DateTimeBasedRowFilterNodeModel extends NodeModel {

    static final String FORMAT_HISTORY_KEY = "time_based_row_filter_formats";

    static final String WARNING_MESSAGE_START_AFTER_END =
        "Start date is after end date! Node created an empty data table.";

    private final SettingsModelString m_colSelect = createColSelectModel();

    private final SettingsModelBoolean m_startBool = createStartBooleanModel();

    private final SettingsModelBoolean m_endBool = createEndBooleanModel();

    private final SettingsModelDateTime m_startDateTime = createStartDateTimeModel();

    private final SettingsModelString m_endSelection = createEndSelectionModel();

    private final SettingsModelDateTime m_endDateTime = createEndDateTimeModel();

    private final SettingsModelString m_periodValueModel = createPeriodValueModel();

    private final SettingsModelInteger m_numericalValueModel = createNumericalValueModel();

    private final SettingsModelString m_granularityModel = createNumericalGranularityModel();

    private final SettingsModelBoolean m_startInclusive = createStartInclusiveModel();

    private final SettingsModelBoolean m_endInclusive = createEndInclusiveModel();

    private final SettingsModelBoolean m_startAlwaysNow = createStartAlwaysNowModel();

    private final SettingsModelBoolean m_endAlwaysNow = createEndAlwaysNowModel();

    /** @return the column select model, used in both dialog and model. */
    static SettingsModelString createColSelectModel() {
        return new SettingsModelString("col_select", null);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createStartBooleanModel() {
        return new SettingsModelBoolean("start_boolean", true);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createEndBooleanModel() {
        return new SettingsModelBoolean("end_boolean", true);
    }

    /** @return the date time model, used in both dialog and model. */
    static SettingsModelDateTime createStartDateTimeModel() {
        return new SettingsModelDateTime("start_date_time", ZonedDateTime.now().withNano(0));
    }

    /** @return the date time model, used in both dialog and model. */
    static SettingsModelDateTime createEndDateTimeModel() {
        return new SettingsModelDateTime("end_date_time", ZonedDateTime.now().withNano(0));
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createEndSelectionModel() {
        return new SettingsModelString("end_selection", null);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createPeriodValueModel() {
        return new SettingsModelString("period_value", "");
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelInteger createNumericalValueModel() {
        return new SettingsModelInteger("numerical_value", 1);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createNumericalGranularityModel() {
        return new SettingsModelString("numerical_granularity", "");
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createStartInclusiveModel() {
        return new SettingsModelBoolean("start_inclusive", true);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createEndInclusiveModel() {
        return new SettingsModelBoolean("end_inclusive", true);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createStartAlwaysNowModel() {
        return new SettingsModelBoolean("start_always_now", false);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createEndAlwaysNowModel() {
        return new SettingsModelBoolean("end_always_now", false);
    }

    /**
     */
    protected DateTimeBasedRowFilterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[0].findColumnIndex(m_colSelect.getStringValue()) < 0) {
            throw new InvalidSettingsException("No configuration available!");
        }
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable dataTable = inData[0];
        final BufferedDataContainer container = exec.createDataContainer(dataTable.getDataTableSpec());

        // read input
        final int colIdx = dataTable.getDataTableSpec().findColumnIndex(m_colSelect.getStringValue());
        final ZonedDateTime executionStartTime = m_startAlwaysNow.getBooleanValue() ? ZonedDateTime.now() : null;
        final ZonedDateTime executionEndTime = m_endAlwaysNow.getBooleanValue() ? ZonedDateTime.now() : null;

        // filter rows
        for (final DataRow row : dataTable) {
            exec.checkCanceled();
            final DataCell cell = row.getCell(colIdx);
            if (!cell.isMissing()) {
                if (cell instanceof LocalDateValue
                    && filterRowLocalDate(((LocalDateValue)cell).getLocalDate(), executionStartTime, executionEndTime)) {
                    container.addRowToTable(row);
                } else if (cell instanceof LocalTimeValue
                    && filterRowLocalTime(((LocalTimeValue)cell).getLocalTime(), executionStartTime, executionEndTime)) {
                    container.addRowToTable(row);
                } else if (cell instanceof LocalDateTimeValue && filterRowLocalDateTime(
                    ((LocalDateTimeValue)cell).getLocalDateTime(), executionStartTime, executionEndTime)) {
                    container.addRowToTable(row);
                } else if (cell instanceof ZonedDateTimeValue && filterRowZonedDateTime(
                    ((ZonedDateTimeValue)cell).getZonedDateTime(), executionStartTime, executionEndTime)) {
                    container.addRowToTable(row);
                }
            }
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * Helper method used in both execution modes streaming and non-streaming for LocalDate
     *
     * @param localDate value of the cell
     * @param executionStartTime execution zoned date time if execution time shall be used, null otherwise
     * @param executionEndTime execution zoned date time if execution time shall be used, null otherwise
     * @return true if row shall be in the output, otherwise false
     */
    private boolean filterRowLocalDate(final LocalDate localDate, final ZonedDateTime executionStartTime,
        final ZonedDateTime executionEndTime) throws ArithmeticException, DateTimeException {
        LocalDate endDate = executionEndTime == null ? m_endDateTime.getLocalDate() : executionEndTime.toLocalDate();
        // if only an end date is given, look if given date is before
        if (!m_startBool.getBooleanValue()) {
            if ((localDate.equals(endDate) && m_endInclusive.getBooleanValue()) || localDate.isBefore(endDate)) {
                return true;
            }
            return false;
        }

        final LocalDate startDate =
            executionStartTime == null ? m_startDateTime.getLocalDate() : executionStartTime.toLocalDate();
        // if only a start date is given, look if given date is afterwards
        if (!m_endBool.getBooleanValue()) {
            if ((localDate.equals(startDate) && m_startInclusive.getBooleanValue()) || localDate.isAfter(startDate)) {
                return true;
            }
            return false;
        }

        // end date is calculated, if end date is given by a period or a granularity
        endDate = (LocalDate)calculateEndDateTime(startDate, endDate);

        // return true if date equals start and start is inclusive
        if (localDate.equals(startDate) && m_startInclusive.getBooleanValue()) {
            return true;
        }
        // return true if date equals end and end is inclusive
        if (localDate.equals(endDate) && m_endInclusive.getBooleanValue()) {
            return true;
        }
        // return true if date is after start and before end
        if (localDate.isAfter(startDate) && localDate.isBefore(endDate)) {
            return true;
        }
        // return true if date is before start and after end, but only if ending point is defined by a period or granularity
        if (localDate.isBefore(startDate) && localDate.isAfter(endDate)
            && !m_endSelection.getStringValue().equals(EndMode.DateTime.name())) {
            return true;
        }

        // this can be true, if the start or end date is defined by execution time
        if (startDate.isAfter(endDate) && m_endSelection.getStringValue().equals(EndMode.DateTime.name())
            && (getWarningMessage() == null || getWarningMessage().isEmpty())) {
            setWarningMessage(WARNING_MESSAGE_START_AFTER_END);
        }
        return false;
    }

    /**
     * Helper method used in both execution modes streaming and non-streaming for LocalTime
     *
     * @param localTime value of the cell
     * @param executionStartTime execution zoned date time if execution time shall be used, null otherwise
     * @param executionEndTime execution zoned date time if execution time shall be used, null otherwise
     * @return true if row shall be in the output, otherwise false
     */
    private boolean filterRowLocalTime(final LocalTime localTime, final ZonedDateTime executionStartTime,
        final ZonedDateTime executionEndTime) throws ArithmeticException, DateTimeException {
        LocalTime endTime = executionEndTime == null ? m_endDateTime.getLocalTime() : executionEndTime.toLocalTime();
        // if only an end time is given, look if given time is before
        if (!m_startBool.getBooleanValue()) {
            if ((localTime.equals(endTime) && m_endInclusive.getBooleanValue()) || localTime.isBefore(endTime)) {
                return true;
            }
            return false;
        }

        final LocalTime startTime =
            executionStartTime == null ? m_startDateTime.getLocalTime() : executionStartTime.toLocalTime();
        // if only a start time is given, look if given time is afterwards
        if (!m_endBool.getBooleanValue()) {
            if ((localTime.equals(startTime) && m_startInclusive.getBooleanValue()) || localTime.isAfter(startTime)) {
                return true;
            }
            return false;
        }

        // end time is calculated, if end time is given by a duration or a granularity
        endTime = (LocalTime)calculateEndDateTime(startTime, endTime);

        // return true if time equals start and start is inclusive
        if (localTime.equals(startTime) && m_startInclusive.getBooleanValue()) {
            return true;
        }
        // return true if time equals end and end is inclusive
        if (localTime.equals(endTime) && m_endInclusive.getBooleanValue()) {
            return true;
        }
        // return true if time is after start and before end
        if (localTime.isAfter(startTime) && localTime.isBefore(endTime)) {
            return true;
        }
        // return true if time is before start and after end, but only if ending point is defined by a duration or granularity
        if (localTime.isBefore(startTime) && localTime.isAfter(endTime)
            && !m_endSelection.getStringValue().equals(EndMode.DateTime.name())) {
            return true;
        }

        // this can be true, if the start or end date is defined by execution time
        if (startTime.isAfter(endTime) && m_endSelection.getStringValue().equals(EndMode.DateTime.name())
            && (getWarningMessage() == null || getWarningMessage().isEmpty())) {
            setWarningMessage(WARNING_MESSAGE_START_AFTER_END);
        }
        return false;
    }

    /**
     * Helper method used in both execution modes streaming and non-streaming for LocalDateTime
     *
     * @param localDateTime value of the cell
     * @param executionStartTime execution zoned date time if execution time shall be used, null otherwise
     * @param executionEndTime execution zoned date time if execution time shall be used, null otherwise
     * @return true if row shall be in the output, otherwise false
     */
    private boolean filterRowLocalDateTime(final LocalDateTime localDateTime, final ZonedDateTime executionStartTime,
        final ZonedDateTime executionEndTime) throws ArithmeticException, DateTimeException {
        LocalDateTime endDateTime =
            executionEndTime == null ? m_endDateTime.getLocalDateTime() : executionEndTime.toLocalDateTime();
        // if only an end time is given, look if given time is before
        if (!m_startBool.getBooleanValue()) {
            if ((localDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                || localDateTime.isBefore(endDateTime)) {
                return true;
            }
            return false;
        }

        final LocalDateTime startDateTime =
            executionStartTime == null ? m_startDateTime.getLocalDateTime() : executionStartTime.toLocalDateTime();
        // if only a start time is given, look if given time is afterwards
        if (!m_endBool.getBooleanValue()) {
            if ((localDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                || localDateTime.isAfter(startDateTime)) {
                return true;
            }
            return false;
        }

        // end time is calculated, if end time is given by a duration or a granularity
        endDateTime = (LocalDateTime)calculateEndDateTime(startDateTime, endDateTime);

        // return true if time equals start and start is inclusive
        if (localDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue()) {
            return true;
        }
        // return true if time equals end and end is inclusive
        if (localDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue()) {
            return true;
        }
        // return true if time is after start and before end
        if (localDateTime.isAfter(startDateTime) && localDateTime.isBefore(endDateTime)) {
            return true;
        }
        // return true if time is before start and after end, but only if ending point is defined by a duration or granularity
        if (localDateTime.isBefore(startDateTime) && localDateTime.isAfter(endDateTime)
            && !m_endSelection.getStringValue().equals(EndMode.DateTime.name())) {
            return true;
        }

        // this can be true, if the start or end date is defined by execution time
        if (startDateTime.isAfter(endDateTime) && m_endSelection.getStringValue().equals(EndMode.DateTime.name())
            && (getWarningMessage() == null || getWarningMessage().isEmpty())) {
            setWarningMessage(WARNING_MESSAGE_START_AFTER_END);
        }
        return false;
    }

    /**
     * Helper method used in both execution modes streaming and non-streaming for ZonedDateTime
     *
     * @param zonedDateTime value of the cell
     * @param executionStartTime execution zoned date time if execution time shall be used, null otherwise
     * @param executionEndTime execution zoned date time if execution time shall be used, null otherwise
     * @return true if row shall be in the output, otherwise false
     */
    private boolean filterRowZonedDateTime(final ZonedDateTime zonedDateTime, final ZonedDateTime executionStartTime,
        final ZonedDateTime executionEndTime) throws ArithmeticException, DateTimeException {
        ZonedDateTime endDateTime = executionEndTime == null ? m_endDateTime.getZonedDateTime() : executionEndTime;
        // if only an end time is given, look if given time is before
        if (!m_startBool.getBooleanValue()) {
            if ((zonedDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                || zonedDateTime.isBefore(endDateTime)) {
                return true;
            }
            return false;
        }

        final ZonedDateTime startDateTime =
            executionStartTime == null ? m_startDateTime.getZonedDateTime() : executionStartTime;
        // if only a start time is given, look if given time is afterwards
        if (!m_endBool.getBooleanValue()) {
            if ((zonedDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                || zonedDateTime.isAfter(startDateTime)) {
                return true;
            }
            return false;
        }

        // end time is calculated, if end time is given by a duration or a granularity
        endDateTime = (ZonedDateTime)calculateEndDateTime(startDateTime, endDateTime);

        // return true if time equals start and start is inclusive
        if (zonedDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue()) {
            return true;
        }
        // return true if time equals end and end is inclusive
        if (zonedDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue()) {
            return true;
        }
        // return true if time is after start and before end
        if (zonedDateTime.isAfter(startDateTime) && zonedDateTime.isBefore(endDateTime)) {
            return true;
        }
        // return true if time is before start and after end, but only if ending point is defined by a duration or granularity
        if (zonedDateTime.isBefore(startDateTime) && zonedDateTime.isAfter(endDateTime)
            && !m_endSelection.getStringValue().equals(EndMode.DateTime.name())) {
            return true;
        }

        // this can be true, if the start or end date is defined by execution time
        if (startDateTime.isAfter(endDateTime) && m_endSelection.getStringValue().equals(EndMode.DateTime.name())
            && (getWarningMessage() == null || getWarningMessage().isEmpty())) {
            setWarningMessage(WARNING_MESSAGE_START_AFTER_END);
        }
        return false;
    }

    /**
     * Calculates the ending point if period/duration or granularity is selected. Otherwise output will be the same as
     * input (endDateTime).
     *
     * @param startDateTime starting point
     * @param endDateTime current ending point
     * @return
     */
    private Temporal calculateEndDateTime(final Temporal startDateTime, final Temporal endDateTime)
        throws ArithmeticException, DateTimeException {
        Temporal end = endDateTime;
        if (m_endSelection.getStringValue().equals(EndMode.Duration.name())) {
            try {
                end = startDateTime.plus(DurationPeriodFormatUtils.parsePeriod(m_periodValueModel.getStringValue()));
            } catch (DateTimeException e1) {
                end = startDateTime.plus(DurationPeriodFormatUtils.parseDuration(m_periodValueModel.getStringValue()));
            }
        }
        if (m_endSelection.getStringValue().equals(EndMode.Numerical.name())) {
            final TemporalAmount amount = Granularity.fromString(m_granularityModel.getStringValue())
                .getPeriodOrDuration(m_numericalValueModel.getIntValue());
            end = startDateTime.plus(amount);
        }
        return end;
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
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];

                // read input
                final int colIdx = in.getDataTableSpec().findColumnIndex(m_colSelect.getStringValue());
                final ZonedDateTime executionStartTime =
                    m_startAlwaysNow.getBooleanValue() ? ZonedDateTime.now() : null;
                final ZonedDateTime executionEndTime = m_endAlwaysNow.getBooleanValue() ? ZonedDateTime.now() : null;

                // filter rows
                final DataType colDataType = in.getDataTableSpec().getColumnSpec(colIdx).getType();
                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    final DataCell cell = row.getCell(colIdx);
                    if (!cell.isMissing()) {
                        if (cell instanceof LocalDateValue
                            && filterRowLocalDate(((LocalDateValue)cell).getLocalDate(), executionStartTime, executionEndTime)) {
                            out.push(row);
                        } else if (cell instanceof LocalTimeValue
                            && filterRowLocalTime(((LocalTimeValue)cell).getLocalTime(), executionStartTime, executionEndTime)) {
                            out.push(row);
                        } else if (cell instanceof LocalDateTimeValue && filterRowLocalDateTime(
                            ((LocalDateTimeValue)cell).getLocalDateTime(), executionStartTime, executionEndTime)) {
                            out.push(row);
                        } else if (cell instanceof ZonedDateTimeValue && filterRowZonedDateTime(
                            ((ZonedDateTimeValue)cell).getZonedDateTime(), executionStartTime, executionEndTime)) {
                            out.push(row);
                        }
                    }
                }
                in.close();
                out.close();
            }
        };
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
        m_colSelect.saveSettingsTo(settings);
        m_startBool.saveSettingsTo(settings);
        m_endBool.saveSettingsTo(settings);
        m_startDateTime.saveSettingsTo(settings);
        m_endDateTime.saveSettingsTo(settings);
        m_startAlwaysNow.saveSettingsTo(settings);
        m_endAlwaysNow.saveSettingsTo(settings);
        m_startInclusive.saveSettingsTo(settings);
        m_endInclusive.saveSettingsTo(settings);
        m_endSelection.saveSettingsTo(settings);
        m_periodValueModel.saveSettingsTo(settings);
        m_numericalValueModel.saveSettingsTo(settings);
        m_granularityModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_startBool.validateSettings(settings);
        m_endBool.validateSettings(settings);
        m_startDateTime.validateSettings(settings);
        m_endDateTime.validateSettings(settings);
        m_startAlwaysNow.validateSettings(settings);
        m_endAlwaysNow.validateSettings(settings);
        m_startInclusive.validateSettings(settings);
        m_endInclusive.validateSettings(settings);
        m_endSelection.validateSettings(settings);
        m_periodValueModel.validateSettings(settings);
        m_numericalValueModel.validateSettings(settings);
        m_granularityModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_startBool.loadSettingsFrom(settings);
        m_endBool.loadSettingsFrom(settings);
        m_startDateTime.loadSettingsFrom(settings);
        m_endDateTime.loadSettingsFrom(settings);
        m_startAlwaysNow.loadSettingsFrom(settings);
        m_endAlwaysNow.loadSettingsFrom(settings);
        m_startInclusive.loadSettingsFrom(settings);
        m_endInclusive.loadSettingsFrom(settings);
        m_endSelection.loadSettingsFrom(settings);
        m_periodValueModel.loadSettingsFrom(settings);
        m_numericalValueModel.loadSettingsFrom(settings);
        m_granularityModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

}
