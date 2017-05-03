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
 *   Feb 23, 2017 (simon): created
 */
package org.knime.time.node.create.createdatetime;

import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelLongBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.time.node.convert.DateTimeTypes;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.SettingsModelDateTime;

/**
 * The node model of the node which creates date and time cells.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class CreateDateTimeNodeModel extends NodeModel {
    private final SettingsModelString m_columnName = createColumnNameModel();

    private final SettingsModelString m_rowNrOptionSelection = createRowNrOptionSelectionModel();

    private final SettingsModelLong m_rowNrFixed = createRowNrFixedModel(m_rowNrOptionSelection);

    private final SettingsModelBoolean m_startUseExecTime = createStartUseExecTimeModel();

    private final SettingsModelDateTime m_start = createStartModel(m_startUseExecTime);

    private final SettingsModelString m_durationOrEnd = createDurationOrEndSelectionModel();

    private final SettingsModelBoolean m_endUseExecTime =
        createEndUseExecTimeModel(m_rowNrOptionSelection, m_durationOrEnd);

    private final SettingsModelString m_duration = createDurationModel(m_rowNrOptionSelection, m_durationOrEnd);

    private final SettingsModelDateTime m_end =
        createEndModel(m_rowNrOptionSelection, m_durationOrEnd, m_endUseExecTime);

    private DateTimeTypes m_selectedNewType = DateTimeTypes.LOCAL_DATE_TIME;

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createColumnNameModel() {
        return new SettingsModelString("column_name", "Date&Time");
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createRowNrOptionSelectionModel() {
        return new SettingsModelString("rownr_option_selection", RowNrMode.Fixed.name());
    }

    /** @return the long model, used in both dialog and model. */
    static SettingsModelLong createRowNrFixedModel(final SettingsModelString rowNrOptionSelectionModel) {
        final SettingsModelLong settingsModelLong = new SettingsModelLongBounded("nr_rows", 1000, 1, Long.MAX_VALUE);
        rowNrOptionSelectionModel.addChangeListener(l -> settingsModelLong
            .setEnabled(rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Fixed.name())));
        return settingsModelLong;
    }

    /** @return the date&time model, used in both dialog and model. */
    static SettingsModelDateTime createStartModel(final SettingsModelBoolean useExecTimeModel) {
        final SettingsModelDateTime settingsModelDateTime =
            new SettingsModelDateTime("start", LocalDateTime.now().withNano(0).minusYears(1).minusHours(1));
        useExecTimeModel.addChangeListener(l -> settingsModelDateTime.setEnabled(!useExecTimeModel.getBooleanValue()));
        return settingsModelDateTime;
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createDurationOrEndSelectionModel() {
        return new SettingsModelString("duration_or_end", EndMode.End.name());
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createDurationModel(final SettingsModelString rowNrOptionSelectionModel,
        final SettingsModelString durationOrEndSelectionModel) {
        final SettingsModelString settingsModelString = new SettingsModelString("duration", null);
        final ChangeListener changeListener = e -> {
            if (durationOrEndSelectionModel.getStringValue() != null
                && rowNrOptionSelectionModel.getStringValue() != null) {
                settingsModelString
                    .setEnabled(rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Variable.name())
                        || (rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Fixed.name())
                            && durationOrEndSelectionModel.getStringValue().equals(EndMode.Duration.name())));
            }
        };
        rowNrOptionSelectionModel.addChangeListener(changeListener);
        durationOrEndSelectionModel.addChangeListener(changeListener);
        changeListener.stateChanged(new ChangeEvent(rowNrOptionSelectionModel));
        return settingsModelString;
    }

    /** @return the date&time model, used in both dialog and model. */
    static SettingsModelDateTime createEndModel(final SettingsModelString rowNrOptionSelectionModel,
        final SettingsModelString durationOrEndSelectionModel, final SettingsModelBoolean useExecTimeModel) {
        final SettingsModelDateTime settingsModelDateTime =
            new SettingsModelDateTime("end", LocalDateTime.now().withNano(0));
        final ChangeListener changeListener = e -> {
            if (durationOrEndSelectionModel.getStringValue() != null
                && rowNrOptionSelectionModel.getStringValue() != null) {
                settingsModelDateTime
                    .setEnabled((rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Variable.name())
                        || (rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Fixed.name())
                            && durationOrEndSelectionModel.getStringValue().equals(EndMode.End.name())))
                        && !useExecTimeModel.getBooleanValue());
            }
        };
        rowNrOptionSelectionModel.addChangeListener(changeListener);
        durationOrEndSelectionModel.addChangeListener(changeListener);
        useExecTimeModel.addChangeListener(changeListener);
        changeListener.stateChanged(new ChangeEvent(rowNrOptionSelectionModel));
        return settingsModelDateTime;
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createStartUseExecTimeModel() {
        return new SettingsModelBoolean("start_use_exec_time", false);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createEndUseExecTimeModel(final SettingsModelString rowNrOptionSelectionModel,
        final SettingsModelString durationOrEndSelectionModel) {
        SettingsModelBoolean settingsModelBoolean = new SettingsModelBoolean("end_use_exec_time", false);
        final ChangeListener changeListener = e -> {
            if (durationOrEndSelectionModel.getStringValue() != null
                && rowNrOptionSelectionModel.getStringValue() != null) {
                settingsModelBoolean
                    .setEnabled(rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Variable.name())
                        || (rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Fixed.name())
                            && durationOrEndSelectionModel.getStringValue().equals(EndMode.End.name())));
            }
        };
        rowNrOptionSelectionModel.addChangeListener(changeListener);
        durationOrEndSelectionModel.addChangeListener(changeListener);
        changeListener.stateChanged(new ChangeEvent(rowNrOptionSelectionModel));
        return settingsModelBoolean;
    }

    CreateDateTimeNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createOutSpec()};
    }

    private DataTableSpec createOutSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator(m_columnName.getStringValue(), m_selectedNewType.getDataType()).createSpec());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataContainer container = exec.createDataContainer(createOutSpec());

        // check and parse duration/period (may be wrong, if controlled by flow variables)
        TemporalAmount durationOrPeriod = null;
        if (m_duration.getStringValue() != null) {
            try {
                durationOrPeriod = DurationPeriodFormatUtils.parseDuration(m_duration.getStringValue());
            } catch (DateTimeParseException ex1) {
                try {
                    durationOrPeriod = DurationPeriodFormatUtils.parsePeriod(m_duration.getStringValue());
                } catch (DateTimeParseException ex2) {
                    throw new InvalidSettingsException(
                        "'" + m_duration.getStringValue() + "' could not be parsed as duration or period!");
                }
            }
        }

        // check start and end input (may be wrong, if controlled by flow variables)
        final Class<? extends Temporal> classStart = m_start.getSelectedDateTime().getClass();
        final Class<? extends Temporal> classEnd = m_end.getSelectedDateTime().getClass();
        if (!classStart.equals(classEnd)
            && !(classStart.equals(ZonedDateTime.class) && classEnd.equals(LocalDateTime.class)
                && m_selectedNewType.getDataType().equals(ZonedDateTimeCellFactory.TYPE))) {
            throw new InvalidSettingsException("The type of start and end time are not compatible: start is "
                + classStart.getSimpleName() + " but end is " + classEnd.getSimpleName());
        }

        // in case the end time is controlled by a flow variable holding a zoned date time, remove the zone
        m_end.setUseZone(false);

        // create date&time rows depending on settings
        final Temporal start;
        final Temporal end;
        if (m_start.getSelectedDateTime() instanceof ZonedDateTime) {
            start = m_startUseExecTime.getBooleanValue()
                ? getTemporalExecTimeWithFormat(((LocalDateTime)m_end.getSelectedDateTime()).atZone(m_start.getZone()))
                : m_start.getSelectedDateTime();
            end = m_endUseExecTime.getBooleanValue() ? getTemporalExecTimeWithFormat(m_start.getSelectedDateTime())
                : ZonedDateTime.of((LocalDateTime)m_end.getSelectedDateTime(), m_start.getZone());
        } else {
            start = m_startUseExecTime.getBooleanValue() ? getTemporalExecTimeWithFormat(m_end.getSelectedDateTime())
                : m_start.getSelectedDateTime();
            end = m_endUseExecTime.getBooleanValue() ? getTemporalExecTimeWithFormat(m_start.getSelectedDateTime())
                : m_end.getSelectedDateTime();
        }

        if (m_rowNrOptionSelection.getStringValue().equals(RowNrMode.Fixed.name())) {
            if (m_durationOrEnd.getStringValue().equals(EndMode.Duration.name())) {
                createByFixedRowNrAndDuration(container, m_rowNrFixed.getLongValue(), start, durationOrPeriod, false,
                    hasMillis(start, start) || hasDurationMillis());
            } else {
                createByFixedRowNrAndEnd(container, m_rowNrFixed.getLongValue(), start, end);
            }
        } else {
            createByVariableRowNr(container, start, end, durationOrPeriod);
        }
        container.close();

        return new BufferedDataTable[]{exec.createBufferedDataTable(container.getTable(), exec)};
    }

    static Temporal getTemporalExecTimeWithFormat(final Temporal formatTemporal) {
        if (formatTemporal instanceof LocalDate) {
            return LocalDate.now();
        }
        if (formatTemporal instanceof LocalTime) {
            return ((LocalTime)formatTemporal).getNano() > 0 ? LocalTime.now() : LocalTime.now().withNano(0);
        }
        if (formatTemporal instanceof LocalDateTime) {
            return ((LocalDateTime)formatTemporal).getNano() > 0 ? LocalDateTime.now()
                : LocalDateTime.now().withNano(0);
        }
        return ((ZonedDateTime)formatTemporal).getNano() > 0 ? ZonedDateTime.now() : ZonedDateTime.now().withNano(0);
    }

    /**
     * Create date&time row with a fixed number of rows and a given starting point and duration/period.
     */
    private void createByFixedRowNrAndDuration(final BufferedDataContainer container, final long nrRows,
        final Temporal startDateTime, final TemporalAmount durationOrPeriod, final boolean wasLocalDate,
        final boolean hasMillis) throws DateTimeException, ArithmeticException {
        Temporal intervalDateTime = startDateTime.minus(durationOrPeriod);
        for (long rowIdx = 0; rowIdx < nrRows; rowIdx++) {
            intervalDateTime = intervalDateTime.plus(durationOrPeriod);

            final DataCell dataCell;
            // local date
            if (intervalDateTime instanceof LocalDate) {
                dataCell = LocalDateCellFactory.create((LocalDate)intervalDateTime);
            }
            // local time
            else if (intervalDateTime instanceof LocalTime) {
                if (hasMillis) {
                    dataCell =
                        LocalTimeCellFactory.create(((LocalTime)intervalDateTime).truncatedTo(ChronoUnit.MILLIS));
                } else if (((LocalTime)intervalDateTime).getNano() >= 500_000_000) {
                    // rounding
                    dataCell =
                        LocalTimeCellFactory.create(((LocalTime)intervalDateTime).plusSeconds(1).withNano(0));
                } else {
                    dataCell = LocalTimeCellFactory.create(((LocalTime)intervalDateTime).withNano(0));
                }
            }
            // local date time
            else if (intervalDateTime instanceof LocalDateTime) {
                if (wasLocalDate) {
                    LocalDate localDate = ((LocalDateTime)intervalDateTime).toLocalDate();
                    // rounding
                    if (((LocalDateTime)intervalDateTime).toLocalTime().isAfter(LocalTime.NOON)) {
                        dataCell = LocalDateCellFactory.create(localDate.plusDays(1));
                    } else {
                        dataCell = LocalDateCellFactory.create(localDate);
                    }
                } else if (hasMillis) {
                    dataCell = LocalDateTimeCellFactory
                        .create(((LocalDateTime)intervalDateTime).truncatedTo(ChronoUnit.MILLIS));
                } else if (((LocalDateTime)intervalDateTime).getNano() >= 500_000_000) {
                    // rounding
                    dataCell = LocalDateTimeCellFactory
                        .create(((LocalDateTime)intervalDateTime).plusSeconds(1).withNano(0));
                } else {
                    dataCell = LocalDateTimeCellFactory.create(((LocalDateTime)intervalDateTime).withNano(0));
                }
            }
            // zoned date time
            else {
                if (hasMillis) {
                    dataCell = ZonedDateTimeCellFactory
                        .create(((ZonedDateTime)intervalDateTime).truncatedTo(ChronoUnit.MILLIS));
                } else if (((ZonedDateTime)intervalDateTime).getNano() >= 500_000_000) {
                    // rounding
                    dataCell = ZonedDateTimeCellFactory
                        .create(((ZonedDateTime)intervalDateTime).plusSeconds(1).withNano(0));
                } else {
                    dataCell = ZonedDateTimeCellFactory.create(((ZonedDateTime)intervalDateTime).withNano(0));
                }
            }

            container.addRowToTable(new DefaultRow(new RowKey("Row" + rowIdx), dataCell));
        }
    }

    /**
     * Create date&time row with a fixed number of rows and a given starting point ending point.
     */
    private void createByFixedRowNrAndEnd(final BufferedDataContainer container, final long nrRows,
        final Temporal startDateTime, final Temporal endDateTime) {
        Temporal start = startDateTime;
        Temporal end = endDateTime;

        // if start or end time contains millis, remember this to create the output accordingly
        final boolean hasMillis = hasMillis(start, end);

        // because a period cannot be divided, convert a local date temporarily to a local date time
        final boolean wasLocalDate = start instanceof LocalDate;
        if (wasLocalDate) {
            start = LocalDateTime.of((LocalDate)start, LocalTime.ofNanoOfDay(0));
            end = LocalDateTime.of((LocalDate)end, LocalTime.ofNanoOfDay(0));
        }

        // === create all rows except the last one ===
        if (nrRows > 1) {
            final Duration durationInterval = Duration.between(start, end).dividedBy(nrRows - 1);
            createByFixedRowNrAndDuration(container, nrRows - 1, start, durationInterval, wasLocalDate, hasMillis);
        } else {
            end = start;
        }

        // === last row needs to be end date&time ===
        final DataCell dataCell;
        if (end instanceof LocalTime) {
            dataCell = LocalTimeCellFactory
                .create(hasMillis ? ((LocalTime)end).truncatedTo(ChronoUnit.MILLIS) : ((LocalTime)end).withNano(0));
        } else if (end instanceof LocalDateTime) {
            if (wasLocalDate) {
                dataCell = LocalDateCellFactory.create(((LocalDateTime)end).toLocalDate());
            } else {
                dataCell = LocalDateTimeCellFactory.create(
                    hasMillis ? ((LocalDateTime)end).truncatedTo(ChronoUnit.MILLIS) : ((LocalDateTime)end).withNano(0));
            }
        } else {
            dataCell = ZonedDateTimeCellFactory.create(
                hasMillis ? ((ZonedDateTime)end).truncatedTo(ChronoUnit.MILLIS) : ((ZonedDateTime)end).withNano(0));
        }
        container.addRowToTable(new DefaultRow(new RowKey("Row" + (nrRows - 1)), dataCell));
    }

    /**
     * @return true, if either start or end has milliseconds
     */
    private boolean hasMillis(final Temporal start, final Temporal end) {
        if (start instanceof LocalDate) {
            return false;
        } else if (start instanceof LocalTime) {
            return ((LocalTime)start).getNano() > 0 || ((LocalTime)end).getNano() > 0;
        } else if (start instanceof LocalDateTime) {
            return ((LocalDateTime)start).getNano() > 0 || ((LocalDateTime)end).getNano() > 0;
        } else if (start instanceof ZonedDateTime) {
            return ((ZonedDateTime)start).getNano() > 0 || ((ZonedDateTime)end).getNano() > 0;
        } else {
            throw new IllegalStateException("Unexpected data type: " + start.getClass().getSimpleName());
        }
    }

    private boolean hasDurationMillis() {
        try {
            final Duration duration = DurationPeriodFormatUtils.parseDuration(m_duration.getStringValue());
            return duration.getNano() > 0;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Create date&time row with a variable number of rows depending on a given starting point, a duration/period and an
     * ending point.
     */
    private void createByVariableRowNr(final BufferedDataContainer container, final Temporal startDateTime,
        final Temporal endDateTime, final TemporalAmount durationOrPeriod) throws InvalidSettingsException {
        Temporal start = startDateTime;
        Temporal end = endDateTime;

        // check if duration is zero
        if ((durationOrPeriod instanceof Period) && ((Period)durationOrPeriod).isZero()) {
            setWarningMessage("Interval is zero! Node created an empty table.");
            return;
        } else if ((durationOrPeriod instanceof Duration) && ((Duration)durationOrPeriod).isZero()) {
            setWarningMessage("Interval is zero! Node created an empty table.");
            return;
        }

        // === check if start date is after end ===
        boolean wasLocalTime = start instanceof LocalTime;
        final boolean isStartAfterEnd;
        if (start instanceof LocalDate) {
            isStartAfterEnd = ((LocalDate)start).isAfter((LocalDate)end);
        } else if (start instanceof LocalTime) {
            // because of the problem that 00:00 is before 23:59, a local time needs to be temporarily converted
            // to a local date time
            boolean isLocalTimeStartAfterEnd = ((LocalTime)start).isAfter((LocalTime)end);
            final boolean isDurationNegative = ((Duration)durationOrPeriod).isNegative();
            int daysAddedToEnd = 0;
            if (isLocalTimeStartAfterEnd && !isDurationNegative) {
                daysAddedToEnd = 1;
            }
            if (!isLocalTimeStartAfterEnd && isDurationNegative) {
                daysAddedToEnd = -1;
            }
            if (start.equals(end)) {
                daysAddedToEnd = 0;
            }
            final int dayOfYear = 10;
            start = LocalDateTime.of(LocalDate.ofYearDay(2010, dayOfYear), (LocalTime)start);
            end = LocalDateTime.of(LocalDate.ofYearDay(2010, dayOfYear + daysAddedToEnd), (LocalTime)end);

            isStartAfterEnd = ((LocalDateTime)start).isAfter((LocalDateTime)end);
        } else if (start instanceof LocalDateTime) {
            isStartAfterEnd = ((LocalDateTime)start).isAfter((LocalDateTime)end);
        } else {
            isStartAfterEnd = ((ZonedDateTime)start).isAfter((ZonedDateTime)end);
        }

        // === check if input is legal: duration/period needs to be positive if end is after start and vice versa ===
        final String warningMsgPos =
            "Interval must be positive, if end is after start date! Node created an empty table.";
        final String warningMsgNeg =
            "Interval must be negative, if end is before start date! Node created an empty table.";
        if (start instanceof LocalDate) {
            if ((((LocalDate)end).isAfter((LocalDate)start))
                && (((LocalDate)start.plus(durationOrPeriod)).isBefore((LocalDate)start))) {
                setWarningMessage(warningMsgPos);
                return;
            }
            if ((((LocalDate)end).isBefore((LocalDate)start))
                && (((LocalDate)start.plus(durationOrPeriod)).isAfter((LocalDate)start))) {
                setWarningMessage(warningMsgNeg);
                return;
            }
        } else if (start instanceof LocalDateTime) {
            if ((((LocalDateTime)end).isAfter((LocalDateTime)start))
                && (((LocalDateTime)start.plus(durationOrPeriod)).isBefore((LocalDateTime)start))) {
                setWarningMessage(warningMsgPos);
                return;
            }
            if ((((LocalDateTime)end).isBefore((LocalDateTime)start))
                && (((LocalDateTime)start.plus(durationOrPeriod)).isAfter((LocalDateTime)start))) {
                setWarningMessage(warningMsgNeg);
                return;
            }
        } else if (start instanceof ZonedDateTime) {
            if ((((ZonedDateTime)end).isAfter((ZonedDateTime)start))
                && (((ZonedDateTime)start.plus(durationOrPeriod)).isBefore((ZonedDateTime)start))) {
                setWarningMessage(warningMsgPos);
                return;
            }
            if ((((ZonedDateTime)end).isBefore((ZonedDateTime)start))
                && (((ZonedDateTime)start.plus(durationOrPeriod)).isAfter((ZonedDateTime)start))) {
                setWarningMessage(warningMsgNeg);
                return;
            }
        }

        // === create rows ===
        Temporal currentDateTime = start;
        long row_idx = 0;
        while (true) {
            final DataCell dataCell;
            final boolean isEqual = currentDateTime.equals(end);
            final boolean isCurrentAfterEnd;
            if (currentDateTime instanceof LocalDate) {
                isCurrentAfterEnd = ((LocalDate)currentDateTime).isAfter((LocalDate)end);
                dataCell = LocalDateCellFactory.create((LocalDate)currentDateTime);
            } else if (currentDateTime instanceof LocalDateTime) {
                isCurrentAfterEnd = ((LocalDateTime)currentDateTime).isAfter((LocalDateTime)end);
                if (wasLocalTime) {
                    dataCell = LocalTimeCellFactory
                        .create((((LocalDateTime)currentDateTime).truncatedTo(ChronoUnit.MILLIS)).toLocalTime());
                } else {
                    dataCell = LocalDateTimeCellFactory
                        .create(((LocalDateTime)currentDateTime).truncatedTo(ChronoUnit.MILLIS));
                }
            } else {
                isCurrentAfterEnd = ((ZonedDateTime)currentDateTime).isAfter((ZonedDateTime)end);
                dataCell =
                    ZonedDateTimeCellFactory.create(((ZonedDateTime)currentDateTime).truncatedTo(ChronoUnit.MILLIS));
            }
            if ((isCurrentAfterEnd && !isStartAfterEnd) || (!isCurrentAfterEnd && !isEqual && isStartAfterEnd)) {
                break;
            }
            container.addRowToTable(new DefaultRow(new RowKey("Row" + row_idx++), dataCell));
            if (isEqual) {
                break;
            }
            currentDateTime = currentDateTime.plus(durationOrPeriod);
        }
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
        m_columnName.saveSettingsTo(settings);
        m_rowNrOptionSelection.saveSettingsTo(settings);
        m_rowNrFixed.saveSettingsTo(settings);
        m_start.saveSettingsTo(settings);
        m_durationOrEnd.saveSettingsTo(settings);
        m_duration.saveSettingsTo(settings);
        m_end.saveSettingsTo(settings);
        m_startUseExecTime.saveSettingsTo(settings);
        m_endUseExecTime.saveSettingsTo(settings);
        if (m_selectedNewType != null) {
            settings.addString("type", m_selectedNewType.name());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnName.validateSettings(settings);
        m_rowNrFixed.validateSettings(settings);
        m_start.validateSettings(settings);
        m_durationOrEnd.validateSettings(settings);
        m_duration.validateSettings(settings);
        m_rowNrOptionSelection.validateSettings(settings);
        m_end.validateSettings(settings);
        m_startUseExecTime.validateSettings(settings);
        m_endUseExecTime.validateSettings(settings);
        settings.getString("type");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnName.loadSettingsFrom(settings);
        m_selectedNewType = DateTimeTypes.valueOf(settings.getString("type"));
        m_rowNrFixed.loadSettingsFrom(settings);
        m_start.loadSettingsFrom(settings);
        m_duration.loadSettingsFrom(settings);
        m_rowNrOptionSelection.loadSettingsFrom(settings);
        m_durationOrEnd.loadSettingsFrom(settings);
        m_end.loadSettingsFrom(settings);
        m_startUseExecTime.loadSettingsFrom(settings);
        m_endUseExecTime.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }
}
