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
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

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
import org.knime.core.node.util.StringHistory;
import org.knime.time.Granularity;

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

    private final SettingsModelString m_format = createFormatModel();

    private final SettingsModelString m_locale = createLocaleModel();

    private final SettingsModelBoolean m_startBool = createStartBooleanModel();

    private final SettingsModelBoolean m_endBool = createEndBooleanModel();

    private final SettingsModelString m_dateTimeStart = createDateTimeStartModel();

    private final SettingsModelString m_endSelection = createEndSelectionModel();

    private final SettingsModelString m_dateTimeEnd = createDateTimeEndModel();

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

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createFormatModel() {
        return new SettingsModelString("date_format", null);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createLocaleModel() {
        return new SettingsModelString("locale", Locale.getDefault().toString());
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createStartBooleanModel() {
        return new SettingsModelBoolean("start_boolean", true);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createEndBooleanModel() {
        return new SettingsModelBoolean("end_boolean", true);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createDateTimeStartModel() {
        return new SettingsModelString("date_time_start", null);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createEndSelectionModel() {
        return new SettingsModelString("end_selection", null);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createDateTimeEndModel() {
        return new SettingsModelString("date_time_end", null);
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
     * @return a set of all predefined formats plus the formats added by the user
     */
    static Collection<String> createPredefinedFormats() {
        // unique values
        Set<String> formats = new LinkedHashSet<String>();
        formats.add("yyyy-MM-dd;HH:mm:ss.S");
        formats.add("dd.MM.yyyy;HH:mm:ss.S");
        formats.add("yyyy-MM-dd HH:mm:ss.S");
        formats.add("dd.MM.yyyy HH:mm:ss.S");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS");
        formats.add("yyyy-MM-dd;HH:mm:ssVV");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV'['zzzz']'");
        formats.add("yyyy-MM-dd");
        formats.add("yyyy/dd/MM");
        formats.add("dd.MM.yyyy");
        formats.add("HH:mm:ss");
        // check also the StringHistory....
        final String[] userFormats = StringHistory.getInstance(FORMAT_HISTORY_KEY).getHistory();
        for (String userFormat : userFormats) {
            formats.add(userFormat);
        }
        return formats;
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
        if (m_format.getStringValue() == null) {
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
        final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(m_format.getStringValue(), new Locale(m_locale.getStringValue()));
        final String start;
        if (m_startBool.getBooleanValue()) {
            if (!m_startAlwaysNow.getBooleanValue()) {
                start = m_dateTimeStart.getStringValue();
            } else {
                start = formatter.format(ZonedDateTime.now());
            }
        } else {
            start = null;
        }

        final String end;
        if (m_endBool.getBooleanValue()) {
            if (!m_endAlwaysNow.getBooleanValue()) {
                end = m_dateTimeEnd.getStringValue();
            } else {
                end = formatter.format(ZonedDateTime.now());
            }
        } else {
            end = null;
        }

        // filter rows
        for (final DataRow row : dataTable) {
            exec.checkCanceled();
            if (filterRow(row.getCell(colIdx), start, end, formatter)) {
                container.addRowToTable(row);
            }
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    private boolean filterRow(final DataCell cell, final String start, final String end,
        final DateTimeFormatter formatter) {
        if (cell.isMissing()) {
            return false;
        }
        // local date
        if (cell instanceof LocalDateCell) {
            final LocalDate localDate = ((LocalDateCell)cell).getLocalDate();
            if (start == null) {
                final LocalDate endDate = LocalDate.parse(end, formatter);
                if ((localDate.equals(endDate) && m_endInclusive.getBooleanValue()) || localDate.isBefore(endDate)) {
                    return true;
                }
                return false;
            }
            if (end == null) {
                final LocalDate startDate = LocalDate.parse(start, formatter);
                if ((localDate.equals(startDate) && m_startInclusive.getBooleanValue())
                    || localDate.isAfter(startDate)) {
                    return true;
                }
                return false;
            }
            final LocalDate startDate = LocalDate.parse(start, formatter);
            LocalDate endDate = null;
            if (m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                endDate = LocalDate.parse(end, formatter);
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
            if (endDate == null) {
                throw new IllegalStateException("Option: " + m_endSelection.getStringValue() + " is not defined.");
            }
            if ((localDate.equals(startDate) && m_startInclusive.getBooleanValue())
                || (localDate.equals(endDate) && m_endInclusive.getBooleanValue())
                || (localDate.isAfter(startDate) && localDate.isBefore(endDate))
                || (localDate.isBefore(startDate) && localDate.isAfter(endDate))) {
                return true;

            }
            return false;
        }

        // local time
        if (cell instanceof LocalTimeCell) {
            final LocalTime localTime = ((LocalTimeCell)cell).getLocalTime();
            if (start == null) {
                final LocalTime endTime = LocalTime.parse(end, formatter);
                if ((localTime.equals(endTime) && m_endInclusive.getBooleanValue()) || localTime.isBefore(endTime)) {
                    return true;
                }
                return false;
            }
            if (end == null) {
                final LocalTime startTime = LocalTime.parse(start, formatter);
                if ((localTime.equals(startTime) && m_startInclusive.getBooleanValue())
                    || localTime.isAfter(startTime)) {
                    return true;
                }
                return false;
            }
            final LocalTime startTime = LocalTime.parse(start, formatter);
            LocalTime endTime = null;
            if (m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                endTime = LocalTime.parse(end, formatter);
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
                || (localTime.isAfter(startTime) && localTime.isBefore(endTime))
                || (localTime.isBefore(startTime) && localTime.isAfter(endTime))) {
                return true;

            }
            return false;
        }

        // local date time
        if (cell instanceof LocalDateTimeCell) {
            final LocalDateTime localDateTime = ((LocalDateTimeCell)cell).getLocalDateTime();
            if (start == null) {
                final LocalDateTime endDateTime = LocalDateTime.parse(end, formatter);
                if ((localDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                    || localDateTime.isBefore(endDateTime)) {
                    return true;
                }
                return false;
            }
            if (end == null) {
                final LocalDateTime startDateTime = LocalDateTime.parse(start, formatter);
                if ((localDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                    || localDateTime.isAfter(startDateTime)) {
                    return true;
                }
                return false;
            }
            final LocalDateTime startDateTime = LocalDateTime.parse(start, formatter);
            LocalDateTime endDateTime = null;
            if (m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                endDateTime = LocalDateTime.parse(end, formatter);
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
                || (localDateTime.isBefore(startDateTime) && localDateTime.isAfter(endDateTime))) {
                return true;

            }
            return false;
        }

        // zoned date time
        if (cell instanceof ZonedDateTimeCell) {
            final ZonedDateTime zonedDateTime = ((ZonedDateTimeCell)cell).getZonedDateTime();
            if (start == null) {
                final ZonedDateTime endDateTime = ZonedDateTime.parse(end, formatter);
                if ((zonedDateTime.equals(endDateTime) && m_endInclusive.getBooleanValue())
                    || zonedDateTime.isBefore(endDateTime)) {
                    return true;
                }
                return false;
            }
            if (end == null) {
                final ZonedDateTime startDateTime = ZonedDateTime.parse(start, formatter);
                if ((zonedDateTime.equals(startDateTime) && m_startInclusive.getBooleanValue())
                    || zonedDateTime.isAfter(startDateTime)) {
                    return true;
                }
                return false;
            }
            final ZonedDateTime startDateTime = ZonedDateTime.parse(start, formatter);
            ZonedDateTime endDateTime = null;
            if (m_endSelection.getStringValue().equals(END_OPTION_DATE_TIME)) {
                endDateTime = ZonedDateTime.parse(end, formatter);
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
                || (zonedDateTime.isBefore(startDateTime) && zonedDateTime.isAfter(endDateTime))) {
                return true;

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
                final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(m_format.getStringValue(), new Locale(m_locale.getStringValue()));
                final String start;
                if (m_startBool.getBooleanValue()) {
                    if (!m_startAlwaysNow.getBooleanValue()) {
                        start = m_dateTimeStart.getStringValue();
                    } else {
                        start = formatter.format(ZonedDateTime.now());
                    }
                } else {
                    start = null;
                }

                final String end;
                if (m_endBool.getBooleanValue()) {
                    if (!m_endAlwaysNow.getBooleanValue()) {
                        end = m_dateTimeEnd.getStringValue();
                    } else {
                        end = formatter.format(ZonedDateTime.now());
                    }
                } else {
                    end = null;
                }

                // filter rows
                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    if (filterRow(row.getCell(colIdx), start, end, formatter)) {
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
        m_format.saveSettingsTo(settings);
        m_locale.saveSettingsTo(settings);
        m_startBool.saveSettingsTo(settings);
        m_endBool.saveSettingsTo(settings);
        m_dateTimeStart.saveSettingsTo(settings);
        m_dateTimeEnd.saveSettingsTo(settings);
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
        m_format.validateSettings(settings);
        m_locale.validateSettings(settings);
        m_startBool.validateSettings(settings);
        m_endBool.validateSettings(settings);
        m_dateTimeStart.validateSettings(settings);
        m_dateTimeEnd.validateSettings(settings);
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
        m_format.loadSettingsFrom(settings);
        m_locale.loadSettingsFrom(settings);
        m_startBool.loadSettingsFrom(settings);
        m_endBool.loadSettingsFrom(settings);
        m_dateTimeStart.loadSettingsFrom(settings);
        m_dateTimeEnd.loadSettingsFrom(settings);
        m_startAlwaysNow.loadSettingsFrom(settings);
        m_endAlwaysNow.loadSettingsFrom(settings);
        m_startInclusive.loadSettingsFrom(settings);
        m_endInclusive.loadSettingsFrom(settings);
        m_endSelection.loadSettingsFrom(settings);
        m_periodValueModel.loadSettingsFrom(settings);
        m_numericalValueModel.loadSettingsFrom(settings);
        m_granularityModel.loadSettingsFrom(settings);
        final String dateformat = m_format.getStringValue();
        // if it is not a predefined one -> store it
        if (!createPredefinedFormats().contains(dateformat)) {
            StringHistory.getInstance(FORMAT_HISTORY_KEY).add(dateformat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

}
