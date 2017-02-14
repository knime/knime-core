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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
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
import org.knime.time.Granularity;
import org.knime.time.util.SettingsModelDateTime;

/**
 * The node model of the node which filters rows based on a time window on one of the new date&time columns.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class DateTimeBasedRowFilterNodeModel extends NodeModel {

    static final String FORMAT_HISTORY_KEY = "time_based_row_filter_formats";

    static final String END_OPTION_DATE_TIME = "Date&Time";

    static final String END_OPTION_PERIOD_DURATION = "Period/Duration";

    static final String END_OPTION_NUMERICAL = "Numerical";

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

    private boolean m_startAfterEnd;

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

    /**
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createPeriodValueModel() {
        return new SettingsModelString("period_value", "");
    }

    /**
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelInteger createNumericalValueModel() {
        return new SettingsModelInteger("numerical_value", 1);
    }

    /**
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createNumericalGranularityModel() {
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
        if (inSpecs[0].findColumnIndex(m_colSelect.getStringValue()) < 0){
            throw new InvalidSettingsException("No configuration available!");
        }
        m_startAfterEnd = false;
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
            if (filterRow(row.getCell(colIdx), executionStartTime, executionEndTime)) {
                container.addRowToTable(row);
            }
        }
        container.close();
        if (m_startAfterEnd) {
            setWarningMessage("Start date is after end date! Node created an empty data table.");
        }
        return new BufferedDataTable[]{container.getTable()};
    }

    private boolean filterRow(final DataCell cell, final ZonedDateTime executionStartTime,
        final ZonedDateTime executionEndTime) {
        if (cell.isMissing()) {
            return false;
        }
        // local date
        if (cell instanceof LocalDateCell) {
            final LocalDate localDate = ((LocalDateCell)cell).getLocalDate();
            LocalDate endDate =
                    executionEndTime == null ? m_endDateTime.getLocalDate() : executionEndTime.toLocalDate();
            if (!m_startBool.getBooleanValue()) {
                if ((localDate.equals(endDate) && m_endInclusive.getBooleanValue()) || localDate.isBefore(endDate)) {
                    return true;
                }
                return false;
            }
            final LocalDate startDate =
                executionStartTime == null ? m_startDateTime.getLocalDate() : executionStartTime.toLocalDate();
            if (!m_endBool.getBooleanValue()) {
                if ((localDate.equals(startDate) && m_startInclusive.getBooleanValue())
                    || localDate.isAfter(startDate)) {
                    return true;
                }
                return false;
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_PERIOD_DURATION)) {
                try {
                    endDate = startDate.plus(Period.parse(m_periodValueModel.getStringValue()));
                } catch (DateTimeException e) {
                    throw new IllegalStateException("Period could not be parsed.");
                }
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_NUMERICAL)) {
                try {
                    final Period period = (Period)Granularity.fromString(m_granularityModel.getStringValue())
                        .getPeriodOrDuration(m_numericalValueModel.getIntValue());
                    endDate = startDate.plus(period);
                } catch (Exception e) {
                    throw new IllegalStateException("Period could not be parsed.");
                }
            }
            if ((localDate.equals(startDate) && m_startInclusive.getBooleanValue())
                || (localDate.equals(endDate) && m_endInclusive.getBooleanValue())
                || (localDate.isAfter(startDate) && localDate.isBefore(endDate)) || (localDate.isBefore(startDate)
                    && localDate.isAfter(endDate) && !m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME))) {
                return true;
            }
            if (startDate.isAfter(endDate) && m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                m_startAfterEnd = true;
            }
            return false;
        }

        // local time
        if (cell instanceof LocalTimeCell) {
            final LocalTime localTime = ((LocalTimeCell)cell).getLocalTime();
            if (!m_startBool.getBooleanValue()) {
                final LocalTime endTime =
                    executionEndTime == null ? m_endDateTime.getLocalTime() : executionEndTime.toLocalTime();
                if ((localTime.equals(endTime) && m_endInclusive.getBooleanValue()) || localTime.isBefore(endTime)) {
                    return true;
                }
                return false;
            }
            final LocalTime startTime =
                executionStartTime == null ? m_startDateTime.getLocalTime() : executionStartTime.toLocalTime();
            if (!m_endBool.getBooleanValue()) {
                if ((localTime.equals(startTime) && m_startInclusive.getBooleanValue())
                    || localTime.isAfter(startTime)) {
                    return true;
                }
                return false;
            }
            LocalTime endTime = null;
            if (m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                endTime = executionEndTime == null ? m_endDateTime.getLocalTime() : executionEndTime.toLocalTime();
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_PERIOD_DURATION)) {
                try {
                    endTime = startTime.plus(Duration.parse(m_periodValueModel.getStringValue()));
                } catch (DateTimeException e) {
                    throw new IllegalStateException("Duration could not be parsed.");
                }
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_NUMERICAL)) {
                try {
                    final Duration duration = (Duration)Granularity.fromString(m_granularityModel.getStringValue())
                        .getPeriodOrDuration(m_numericalValueModel.getIntValue());
                    endTime = startTime.plus(duration);
                } catch (Exception e) {
                    throw new IllegalStateException("Duration could not be parsed.");
                }
            }
            if (endTime == null) {
                throw new IllegalStateException("Option: " + m_endSelection.getStringValue() + " is not defined.");
            }
            if ((localTime.equals(startTime) && m_startInclusive.getBooleanValue())
                || (localTime.equals(endTime) && m_endInclusive.getBooleanValue())
                || (localTime.isAfter(startTime) && localTime.isBefore(endTime)) || (localTime.isBefore(startTime)
                    && localTime.isAfter(endTime) && !m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME))) {
                return true;
            }
            if (startTime.isAfter(endTime) && m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                m_startAfterEnd = true;
            }
            return false;
        }

        // local date time
        if (cell instanceof LocalDateTimeCell) {
            final LocalDateTime localDateTime = ((LocalDateTimeCell)cell).getLocalDateTime();
            if (!m_startBool.getBooleanValue()) {
                final LocalDateTime endDateTime =
                    executionEndTime == null ? m_endDateTime.getLocalDateTime() : executionEndTime.toLocalDateTime();
                if ((localDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                    || localDateTime.isBefore(endDateTime)) {
                    return true;
                }
                return false;
            }
            final LocalDateTime startDateTime =
                executionStartTime == null ? m_startDateTime.getLocalDateTime() : executionStartTime.toLocalDateTime();
            if (!m_endBool.getBooleanValue()) {
                if ((localDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                    || localDateTime.isAfter(startDateTime)) {
                    return true;
                }
                return false;
            }
            LocalDateTime endDateTime = null;
            if (m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                endDateTime =
                    executionEndTime == null ? m_endDateTime.getLocalDateTime() : executionEndTime.toLocalDateTime();
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_PERIOD_DURATION)) {
                try {
                    endDateTime = startDateTime.plus(Period.parse(m_periodValueModel.getStringValue()));
                } catch (DateTimeException e) {
                    try {
                        endDateTime = startDateTime.plus(Duration.parse(m_periodValueModel.getStringValue()));
                    } catch (DateTimeException e2) {
                        throw new IllegalStateException("Duration or Period could not be parsed.");
                    }
                }
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_NUMERICAL)) {
                try {
                    final Duration duration = (Duration)Granularity.fromString(m_granularityModel.getStringValue())
                        .getPeriodOrDuration(m_numericalValueModel.getIntValue());
                    endDateTime = startDateTime.plus(duration);
                } catch (Exception e) {
                    try {
                        final Period period = (Period)Granularity.fromString(m_granularityModel.getStringValue())
                            .getPeriodOrDuration(m_numericalValueModel.getIntValue());
                        endDateTime = startDateTime.plus(period);
                    } catch (Exception e2) {
                        throw new IllegalStateException("Duration or Period could not be parsed.");
                    }
                }
            }
            if (endDateTime == null) {
                throw new IllegalStateException("Option: " + m_endSelection.getStringValue() + " is not defined.");
            }
            if ((localDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                || (localDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                || (localDateTime.isAfter(startDateTime) && localDateTime.isBefore(endDateTime))
                || (localDateTime.isBefore(startDateTime) && localDateTime.isAfter(endDateTime)
                    && !m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME))) {
                return true;
            }
            if (startDateTime.isAfter(endDateTime) && m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                m_startAfterEnd = true;
            }
            return false;
        }

        // zoned date time
        if (cell instanceof ZonedDateTimeCell) {
            final ZonedDateTime zonedDateTime = ((ZonedDateTimeCell)cell).getZonedDateTime();
            if (!m_startBool.getBooleanValue()) {
                final ZonedDateTime endDateTime =
                    executionEndTime == null ? m_endDateTime.getZonedDateTime() : executionEndTime;
                if ((zonedDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                    || zonedDateTime.isBefore(endDateTime)) {
                    return true;
                }
                return false;
            }
            final ZonedDateTime startDateTime =
                executionStartTime == null ? m_startDateTime.getZonedDateTime() : executionStartTime;
            if (!m_endBool.getBooleanValue()) {
                if ((zonedDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                    || zonedDateTime.isAfter(startDateTime)) {
                    return true;
                }
                return false;
            }
            ZonedDateTime endDateTime = null;
            if (m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                endDateTime = executionEndTime == null ? m_endDateTime.getZonedDateTime() : executionEndTime;
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_PERIOD_DURATION)) {
                try {
                    endDateTime = startDateTime.plus(Period.parse(m_periodValueModel.getStringValue()));
                } catch (DateTimeException e) {
                    try {
                        endDateTime = startDateTime.plus(Duration.parse(m_periodValueModel.getStringValue()));
                    } catch (DateTimeException e2) {
                        throw new IllegalStateException("Duration or Period could not be parsed.");
                    }
                }
            }
            if (m_endSelection.getStringValue().equals(END_OPTION_NUMERICAL)) {
                try {
                    final Duration duration = (Duration)Granularity.fromString(m_granularityModel.getStringValue())
                        .getPeriodOrDuration(m_numericalValueModel.getIntValue());
                    endDateTime = startDateTime.plus(duration);
                } catch (Exception e) {
                    try {
                        final Period period = (Period)Granularity.fromString(m_granularityModel.getStringValue())
                            .getPeriodOrDuration(m_numericalValueModel.getIntValue());
                        endDateTime = startDateTime.plus(period);
                    } catch (Exception e2) {
                        throw new IllegalStateException("Duration or Period could not be parsed.");
                    }
                }
            }
            if (endDateTime == null) {
                throw new IllegalStateException("Option: " + m_endSelection.getStringValue() + " is not defined.");
            }
            if ((zonedDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                || (zonedDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                || (zonedDateTime.isAfter(startDateTime) && zonedDateTime.isBefore(endDateTime))
                || (zonedDateTime.isBefore(startDateTime) && zonedDateTime.isAfter(endDateTime)
                    && !m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME))) {
                return true;
            }
            if (startDateTime.isAfter(endDateTime) && m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                m_startAfterEnd = true;
            }
            return false;
        }
        throw new IllegalStateException("Unexpected data type: " + cell.getClass());
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
                RowInput in = (RowInput)inputs[0];
                RowOutput out = (RowOutput)outputs[0];

                // read input
                final int colIdx = in.getDataTableSpec().findColumnIndex(m_colSelect.getStringValue());
                final ZonedDateTime executionStartTime =
                    m_startAlwaysNow.getBooleanValue() ? ZonedDateTime.now() : null;
                final ZonedDateTime executionEndTime = m_endAlwaysNow.getBooleanValue() ? ZonedDateTime.now() : null;
                // filter rows
                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    if (filterRow(row.getCell(colIdx), executionStartTime, executionEndTime)) {
                        out.push(row);
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
        m_startAfterEnd = false;
    }

}
