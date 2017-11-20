/*
 * ------------------------------------------------------------------------
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
 *   02.09.2008 (thor): created
 */
package org.knime.time.node.window;

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
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.time.node.window.LoopStartWindowConfiguration.Trigger;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.SettingsModelDateTime;

/**
 * Loop start node that outputs a set of rows at a time. Used to implement a streaming (or chunking approach) where only
 * a set of rows is processed at a time
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
final class LoopStartWindowNodeModel extends NodeModel implements LoopStartNodeTerminator {

    // Configuration used to get the settings
    private LoopStartWindowConfiguration m_windowConfig;

    // Input iterator
    private CloseableRowIterator m_rowIterator;

    // number of columns
    private int m_nColumns;

    // index of current row
    private long m_currRow;

    // number of rows
    private long m_rowCount;

    // buffered rows used for overlapping
    private LinkedList<DataRow> m_bufferedRows;

    // Name of the chosen time column
    private String m_timeColumnName;

    // Next start of the window
    private Temporal m_nextStartTemporal;

    // Next end of the window
    private Temporal m_windowEndTemporal;

    // Used to check if table is sorted
    private Temporal m_prevTemporal;

    // To check if an overflow occurred concerning next starting temporal.
    private boolean m_lastWindow;

    // To ensure that warning message will be printed only once
    private boolean m_printedMissingWarning;

    private final SettingsModelDateTime m_timeConfig = createStartModel();

    private final SettingsModelString settingsModel = LoopStartWindowNodeModel.createColumnModel();

    /**
     * Creates a new model.
     */
    public LoopStartWindowNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_windowConfig == null) {
            m_windowConfig = new LoopStartWindowConfiguration();
            setWarningMessage("Using default: " + m_windowConfig);
        }

        DataTableSpec tableSpec = inSpecs[0];

        if (m_windowConfig.getTrigger() == Trigger.TIME) {
            if (!tableSpec.containsName(m_timeColumnName)) {
                throw new InvalidSettingsException(
                    "Selected time column '" + m_timeColumnName + "' does not exist in input table.");
            }

            DataColumnSpec columnSpec = tableSpec.getColumnSpec(m_timeColumnName);

            /* Check if the cells have the same type as the specified start time. */
            if (m_windowConfig.useSpecifiedStartTime()) {
                Temporal specifiedStartTime = m_timeConfig.getSelectedDateTime();

                if (specifiedStartTime == null) {
                    throw new InvalidSettingsException(
                        "Specified start time is not compatible with selected time column '" + m_timeColumnName + "'");
                }
            }

            /* Check if period is set for LocalTime */
            TemporalAmount start =
                getTemporalAmount(m_windowConfig.getTimeStepSize() + m_windowConfig.getTimeStepUnit().getUnitLetter());
            if (m_windowConfig.getTimeStepSize() == null) {
                throw new InvalidSettingsException("No temporal step size set.");
            }
            if (start == null) {
                throw new InvalidSettingsException("Given step size couldn't be matched to type Duration or Period.");
            } else if (start instanceof Period && columnSpec.getType().equals(DataType.getType(LocalTimeCell.class))) {
                throw new InvalidSettingsException("Step size: Period type not allowed for LocalTime");
            }

            TemporalAmount window = getTemporalAmount(
                m_windowConfig.getTimeWindowSize() + m_windowConfig.getTimeWindowUnit().getUnitLetter());
            if (m_windowConfig.getTimeWindowSize() == null) {
                throw new InvalidSettingsException("No window size set.");
            }
            if (window == null) {
                throw new InvalidSettingsException("Given window size couldn't be machted to type Duration or Period.");
            } else if (start instanceof Period && columnSpec.getType().equals(DataType.getType(LocalTimeCell.class))) {
                throw new InvalidSettingsException("Window size: Period type not allowed for LocalTime");
            }
        }

        return inSpecs;
    }

    /**
     * Gets the temporal amount from the given string which can be either a Duration or a Period.
     *
     * @param amount string that shall be parsed
     * @return TemporalAmount of the string or {@code null} if it cannot be parsed to Duration or Period.
     */
    private TemporalAmount getTemporalAmount(final String amount) {
        try {
            return DurationPeriodFormatUtils.parseDuration(amount);
        } catch (DateTimeParseException e) {
            try {
                return DurationPeriodFormatUtils.parsePeriod(amount);
            } catch (DateTimeException e2) {

            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable table = inData[0];
        m_rowCount = table.size();

        if (m_currRow == 0) {
            m_rowIterator = table.iterator();
            m_bufferedRows = new LinkedList<>();

            m_nColumns = table.getSpec().getNumColumns();

            if (m_rowCount == 0) {
                BufferedDataContainer container = exec.createDataContainer(table.getSpec());
                container.close();

                return new BufferedDataTable[]{container.getTable()};
            }
        }

        switch (m_windowConfig.getWindowDefinition()) {
            case BACKWARD:
                if (m_windowConfig.getTrigger().equals(Trigger.ROW)) {
                    return executeBackward(table, exec);
                }

                return executeTemporalBackward(table, exec);

            case CENTRAL:
                if (m_windowConfig.getTrigger().equals(Trigger.ROW)) {
                    return executeCentral(table, exec);
                }

                return executeTemporalCentral(table, exec);

            case FORWARD:
                if (m_windowConfig.getTrigger().equals(Trigger.ROW)) {
                    return executeForward(table, exec);
                }

                return executeTemporalForward(table, exec);

            default:
                return executeForward(table, exec);

        }
    }

    /**
     * Computes the next window that shall be returned for time triggered events using forward windowing.
     *
     * @param table that holds the data.
     * @param exec context of the execution.
     * @return Next window.
     */
    private BufferedDataTable[] executeTemporalForward(final BufferedDataTable table, final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(table.getSpec());
        int column = table.getDataTableSpec().findColumnIndex(m_timeColumnName);
        TemporalAmount startInterval =
            getTemporalAmount(m_windowConfig.getTimeStepSize() + m_windowConfig.getTimeStepUnit().getUnitLetter());
        TemporalAmount windowDuration =
            getTemporalAmount(m_windowConfig.getTimeWindowSize() + m_windowConfig.getTimeWindowUnit().getUnitLetter());

        /* To check if an overflow occurred concerning the current window */
        boolean overflow = false;
        // To check if an overflow occurred concerning next starting temporal.
        m_lastWindow = false;

        /* Compute end duration of window and beginning of next duration*/
        if (m_nextStartTemporal == null && m_rowIterator.hasNext()) {
            DataRow first = m_rowIterator.next();

            /* Check if column only consists of missing values. */
            while (first.getCell(column).isMissing() && m_rowIterator.hasNext()) {
                first = m_rowIterator.next();

                printMissingWarning();
            }

            if (first.getCell(column).isMissing()) {
                getLogger().warn("Column '" + m_timeColumnName + "' only contains missing values.");
                container.close();

                return new BufferedDataTable[]{container.getTable()};
            }

            Temporal firstStart = getTemporal(first.getCell(column));

            m_prevTemporal = getTemporal(first.getCell(column));

            /* Check if user specified start shall be used. */
            if (m_windowConfig.useSpecifiedStartTime()) {
                firstStart = m_timeConfig.getSelectedDateTime();

                /* Current start temporal, m_nextStartTemporal is used to re-use the skipTemporalWindow method. */
                m_nextStartTemporal = firstStart;

                m_windowEndTemporal = m_nextStartTemporal.plus(windowDuration);

                /* Check if overflow of current window occurs. If this is the case simply find first row of the given window. */
                if (compareTemporal(m_windowEndTemporal, m_nextStartTemporal) <= 0) {
                    while (m_rowIterator.hasNext()
                        && compareTemporal(getTemporal(first.getCell(column)), m_nextStartTemporal) < 0) {
                        first = m_rowIterator.next();
                    }

                    if (compareTemporal(getTemporal(first.getCell(column)), m_nextStartTemporal) >= 0) {
                        m_bufferedRows.addFirst(first);
                    }
                } else {
                    /* We may have to skip the temporal window and or the current rows to find a window containing at least one row. */
                    skipTemporalWindow(first, column, startInterval, windowDuration);
                    firstStart = m_nextStartTemporal;
                }

                if (m_bufferedRows.isEmpty()) {
                    container.close();

                    return new BufferedDataTable[]{container.getTable()};
                }
            }

            m_nextStartTemporal = firstStart.plus(startInterval);

            /* Check if the next starting temporal lies beyond the maximum temporal value. */
            if (compareTemporal(m_nextStartTemporal, firstStart) <= 0) {
                m_lastWindow = true;
            }

            /* Checks if window overflow occurs. */
            Temporal temp = firstStart.plus(windowDuration);
            if (compareTemporal(temp, firstStart) <= 0) {
                overflow = true;
            } else {
                m_windowEndTemporal = temp;
            }

            /* Add the first row if no user specified start is used. */
            if (!m_windowConfig.useSpecifiedStartTime()) {
                m_bufferedRows.add(first);
            }
        } else {
            m_prevTemporal = getTemporal(m_bufferedRows.getFirst().getCell(column));

            /* Checks if temporal overflow occurs. */
            Temporal temp = m_nextStartTemporal.plus(windowDuration);
            if (compareTemporal(temp, m_nextStartTemporal.minus(startInterval)) <= 0) {
                overflow = true;
            } else {
                m_windowEndTemporal = temp;
                temp = m_nextStartTemporal.plus(startInterval);
                /* Check if the next starting temporal lies beyond the maximum temporal value. */
                if (compareTemporal(temp, m_nextStartTemporal) <= 0) {
                    m_lastWindow = true;
                } else {
                    m_nextStartTemporal = temp;
                }
            }
        }

        Iterator<DataRow> bufferedIterator = m_bufferedRows.iterator();

        boolean allBufferedRowsInWindow = true;

        /* Add buffered rows. */
        while (bufferedIterator.hasNext()) {
            DataRow row = bufferedIterator.next();

            Temporal temp = getTemporal(row.getCell(column));

            /* Checks if all buffered rows are in the specified window. */
            if (!overflow && compareTemporal(temp, m_windowEndTemporal) > 0) {
                allBufferedRowsInWindow = false;
                break;
            }

            container.addRowToTable(row);
            m_currRow++;

            if (overflow || m_lastWindow
                || compareTemporal(getTemporal(row.getCell(column)), m_nextStartTemporal) < 0) {
                bufferedIterator.remove();
            }
        }

        boolean lastEntryMissing = false;
        boolean addedNewToBuffer = false;

        /* Add newly read rows. */
        while (m_rowIterator.hasNext() && allBufferedRowsInWindow) {
            DataRow row = m_rowIterator.next();

            if (row.getCell(column).isMissing()) {
                printMissingWarning();
                lastEntryMissing = true;
                continue;
            }

            lastEntryMissing = false;

            Temporal currTemporal = getTemporal(row.getCell(column));

            /* Check if table is sorted in non-descending order according to temporal column. */
            if (compareTemporal(currTemporal, m_prevTemporal) < 0) {
                throw new IllegalStateException(
                    "Table not in ascending order concerning chosen temporal column (use Sorter prior to Windo Loop Start).");
            }

            m_prevTemporal = currTemporal;

            /* Add rows for next window into the buffer. */
            if (!m_lastWindow && compareTemporal(currTemporal, m_nextStartTemporal) >= 0 && !overflow) {
                m_bufferedRows.add(row);
                addedNewToBuffer = true;
            }

            /* Add row to current output. */
            if (overflow || compareTemporal(currTemporal, m_windowEndTemporal) <= 0) {
                container.addRowToTable(row);

                /* The last entry has been in the current window, thus it is the last one. */
                if (!m_rowIterator.hasNext()) {
                    m_lastWindow = true;
                }

                m_currRow++;
            } else {
                break;
            }
        }

        /* Checks if the last row we saw had a missing value. If this is the case the current window is the last window. */
        if (lastEntryMissing) {
            m_lastWindow = true;
        }

        /* Find next entry that lies in a following window. */
        DataRow row = null;

        /* Close iterator if last window has been filled. */
        if (m_lastWindow) {
            m_rowIterator.close();
        } else if (!allBufferedRowsInWindow) {
            /* Not all previously buffered rows are in the current window. */
            row = m_bufferedRows.remove();
        } else if (!m_rowIterator.hasNext() && !addedNewToBuffer) {
            /* We already returned the last row, but it would be in the next window. Nevertheless, terminate. */
            m_rowIterator.close();
            m_lastWindow = true;
        } else if (m_bufferedRows.size() == 0) {
            /* Buffer is empty, so get next row. */
            row = m_rowIterator.next();

            while (row.getCell(column).isMissing() && m_rowIterator.hasNext()) {
                row = m_rowIterator.next();

                printMissingWarning();
            }

            if (row.getCell(column).isMissing()) {
                row = null;

                printMissingWarning();
            }
        } else if (!overflow && !m_bufferedRows.isEmpty()) {
            /* Checks if the next buffered row lies within the given window */
            if (compareTemporal(m_windowEndTemporal.plus(startInterval), m_windowEndTemporal) > 0) {
                Temporal temp = getTemporal(m_bufferedRows.getFirst().getCell(column));

                if (compareTemporal(temp, m_windowEndTemporal.plus(startInterval)) >= 0) {
                    row = m_bufferedRows.removeFirst();
                }
            }
        }

        skipTemporalWindow(row, column, startInterval, windowDuration);

        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * Prints a warning concerning missing values.
     */
    private void printMissingWarning() {
        if (!m_printedMissingWarning) {
            m_printedMissingWarning = true;
            getLogger().warn("Detected missing values for specified column; rows have been skipped.");
        }

    }

    /**
     * Computes the next window that shall be returned for time triggered events using backward windowing.
     *
     * @param table that holds the data.
     * @param exec context of the execution.
     * @return Next window.
     */
    private BufferedDataTable[] executeTemporalBackward(final BufferedDataTable table, final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(table.getSpec());
        int column = table.getDataTableSpec().findColumnIndex(m_timeColumnName);
        TemporalAmount startInterval =
            getTemporalAmount(m_windowConfig.getTimeStepSize() + m_windowConfig.getTimeStepUnit().getUnitLetter());
        TemporalAmount windowDuration =
            getTemporalAmount(m_windowConfig.getTimeWindowSize() + m_windowConfig.getTimeStepUnit().getUnitLetter());

        /* To check if an overflow occurred concerning the current window */
        boolean overflow = false;
        // To check if an overflow occurred concerning next starting temporal.
        m_lastWindow = false;

        /* Compute end duration of window and beginning of next duration*/
        if (m_nextStartTemporal == null && m_rowIterator.hasNext()) {
            DataRow first = m_rowIterator.next();

            /* Check if column only consists of missing values. */
            while (first.getCell(column).isMissing() && m_rowIterator.hasNext()) {
                first = m_rowIterator.next();

                printMissingWarning();
            }

            if (first.getCell(column).isMissing()) {
                getLogger().warn("Column '" + m_timeColumnName + "' only contains missing values.");
                container.close();

                return new BufferedDataTable[]{container.getTable()};
            }

            /* First entry is the end of the window. Compute next starting point by adding start interval minus the size of the window. */
            Temporal firstEnd = getTemporal(first.getCell(column));

            /* Check if user specified start shall be used. */
            if (m_windowConfig.useSpecifiedStartTime()) {
                firstEnd = m_timeConfig.getSelectedDateTime();

                /* Move the window until the current end is greater or equal than the first row. */
                while (compareTemporal(getTemporal(first.getCell(column)), firstEnd) > 0) {
                    Temporal tempNextEnd = firstEnd.plus(startInterval);

                    /* Check if next window yields an overflow. In this case we return an empty table. */
                    if (compareTemporal(tempNextEnd, firstEnd) <= 0) {
                        container.close();
                        getLogger().warn("No row lies within any of the possible windows.");

                        return new BufferedDataTable[]{container.getTable()};
                    }

                    firstEnd = tempNextEnd;
                }

                /* Current start temporal, m_nextStartTemporal is used to re-use the skipTemporalWindow method. */
                m_nextStartTemporal = firstEnd.minus(windowDuration);

                /* Check for underflow of the current start. */
                if (compareTemporal(m_nextStartTemporal, firstEnd) >= 0) {
                    m_nextStartTemporal = getMin(m_nextStartTemporal);
                    m_bufferedRows.add(first);
                } else {
                    /* Skip window until we find one which contains at least one row. */
                    skipTemporalWindow(first, column, startInterval, windowDuration);
                    firstEnd = m_nextStartTemporal.plus(windowDuration);

                    /* Check if overflow of window occurred. In this case we return an empty table. */
                    if (compareTemporal(firstEnd, m_nextStartTemporal) <= 0) {
                        container.close();
                        getLogger().warn("No row lies within any of the possible windows.");

                        return new BufferedDataTable[]{container.getTable()};
                    }

                }

                /* Check if we found a window which contains at least one row. */
                if (m_bufferedRows.isEmpty()) {
                    container.close();
                    getLogger().warn("No row lies within any of the possible windows.");

                    return new BufferedDataTable[]{container.getTable()};
                }
            }

            m_windowEndTemporal = firstEnd;

            m_prevTemporal = getTemporal(first.getCell(column));

            if (!m_windowConfig.useSpecifiedStartTime()) {
                m_bufferedRows.add(first);
            }

            Temporal tempNextEnd = m_windowEndTemporal.plus(startInterval);

            /* Check if the current window is the last window. */
            if (compareTemporal(tempNextEnd, m_windowEndTemporal) <= 0) {
                m_lastWindow = true;
            } else {
                m_nextStartTemporal = tempNextEnd.minus(windowDuration);

                if (compareTemporal(m_nextStartTemporal, tempNextEnd) >= 0) {
                    m_nextStartTemporal = getMin(tempNextEnd);
                }
            }

        } else {
            Temporal tempNextEnd = m_windowEndTemporal.plus(startInterval);

            /* Check if the current window is the last window. */
            if (compareTemporal(tempNextEnd, m_windowEndTemporal) <= 0) {
                m_lastWindow = true;
            } else {
                m_nextStartTemporal = tempNextEnd.minus(windowDuration).plus(startInterval);

                if (compareTemporal(m_nextStartTemporal, tempNextEnd) >= 0) {
                    m_nextStartTemporal = getMin(tempNextEnd);
                }
            }

            m_windowEndTemporal = tempNextEnd;
        }

        Temporal tempNextEnd = m_windowEndTemporal.plus(startInterval);

        /* Check if the current window is the last window. */
        if (compareTemporal(tempNextEnd, m_windowEndTemporal) <= 0) {
            m_lastWindow = true;
        } else {
            m_nextStartTemporal = tempNextEnd.minus(windowDuration);

            if (compareTemporal(m_nextStartTemporal, tempNextEnd) >= 0) {
                m_nextStartTemporal = getMin(tempNextEnd);
            }
        }

        Iterator<DataRow> bufferedIterator = m_bufferedRows.iterator();
        boolean allBufferedRowsInWindow = true;

        /* Add buffered rows. */
        while (bufferedIterator.hasNext()) {
            DataRow row = bufferedIterator.next();

            Temporal temp = getTemporal(row.getCell(column));

            /* Checks if all buffered rows are in the specified window. */
            if (!overflow && compareTemporal(temp, m_windowEndTemporal) > 0) {
                allBufferedRowsInWindow = false;
                break;
            }

            container.addRowToTable(row);
            m_currRow++;

            if (overflow || m_lastWindow
                || compareTemporal(getTemporal(row.getCell(column)), m_nextStartTemporal) < 0) {
                bufferedIterator.remove();
            }
        }

        boolean lastEntryMissing = false;
        boolean addedNewToBuffer = false;

        /* Add newly read rows. */
        while (m_rowIterator.hasNext() && allBufferedRowsInWindow) {
            DataRow row = m_rowIterator.next();

            if (row.getCell(column).isMissing()) {
                printMissingWarning();
                lastEntryMissing = true;
                continue;
            }

            lastEntryMissing = false;

            Temporal currTemporal = getTemporal(row.getCell(column));

            /* Check if table is sorted in non-descending order according to temporal column. */
            if (compareTemporal(currTemporal, m_prevTemporal) < 0) {
                throw new IllegalStateException("Table not in ascending order concerning chosen temporal column.");
            }

            m_prevTemporal = currTemporal;

            /* Add rows for next window into the buffer. */
            if (!m_lastWindow && compareTemporal(currTemporal, m_nextStartTemporal) >= 0 && !overflow) {
                m_bufferedRows.add(row);
                addedNewToBuffer = true;
            }

            /* Add row to current output. */
            if (overflow || compareTemporal(currTemporal, m_windowEndTemporal) <= 0) {
                container.addRowToTable(row);

                /* The last entry has been in the current window, thus it is the last one. */
                if (!m_rowIterator.hasNext()) {
                    m_lastWindow = true;
                }

                m_currRow++;
            } else {
                break;
            }
        }

        /* Checks if the last row we saw had a missing value. If this is the case the current window is the last window. */
        if (lastEntryMissing) {
            m_lastWindow = true;
        }

        /* Find next entry that lies in a following window. */
        DataRow row = null;

        /* Close iterator if last window has been filled. */
        if (m_lastWindow) {
            m_rowIterator.close();
        } else if (!allBufferedRowsInWindow) {
            /* Not all previously buffered rows are in the current window. */
            row = m_bufferedRows.remove();
        } else if (!m_rowIterator.hasNext() && !addedNewToBuffer) {
            /* We already returned the last row, but it would be in the next window. Nevertheless, terminate. */
            m_rowIterator.close();
            m_lastWindow = true;
        } else if (m_bufferedRows.size() == 0) {
            /* Buffer is empty, so get next row. */
            row = m_rowIterator.next();

            while (row.getCell(column).isMissing() && m_rowIterator.hasNext()) {
                row = m_rowIterator.next();

                printMissingWarning();
            }

            if (row.getCell(column).isMissing()) {
                row = null;

                printMissingWarning();
            }
        } else if (!overflow && !m_bufferedRows.isEmpty()) {
            /* Checks if the next buffered row lies within the given window */
            if (compareTemporal(m_windowEndTemporal.plus(startInterval), m_windowEndTemporal) > 0) {
                Temporal temp = getTemporal(m_bufferedRows.getFirst().getCell(column));

                if (compareTemporal(temp, m_windowEndTemporal.plus(startInterval)) >= 0) {
                    row = m_bufferedRows.removeFirst();
                }
            }
        }

        skipTemporalWindow(row, column, startInterval, windowDuration);

        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * Computes the next window that shall be returned for time triggered events using central windowing.
     *
     * @param table that holds the data.
     * @param exec context of the execution.
     * @return Next window.
     */
    private BufferedDataTable[] executeTemporalCentral(final BufferedDataTable table, final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(table.getSpec());
        int column = table.getDataTableSpec().findColumnIndex(m_timeColumnName);
        Duration startInterval = (Duration)getTemporalAmount(
            m_windowConfig.getTimeStepSize() + m_windowConfig.getTimeStepUnit().getUnitLetter());
        Duration windowDuration = (Duration)getTemporalAmount(
            m_windowConfig.getTimeWindowSize() + m_windowConfig.getTimeWindowUnit().getUnitLetter());

        /* To check if an overflow occurred concerning the current window */
        boolean overflow = false;
        // To check if an overflow occurred concerning next starting temporal.
        m_lastWindow = false;

        /* Compute end duration of window and beginning of next duration*/
        if (m_nextStartTemporal == null && m_rowIterator.hasNext()) {
            DataRow first = m_rowIterator.next();

            /* Check if column only consists of missing values. */
            while (first.getCell(column).isMissing() && m_rowIterator.hasNext()) {
                first = m_rowIterator.next();

                printMissingWarning();
            }

            if (first.getCell(column).isMissing()) {
                getLogger().warn("Column '" + m_timeColumnName + "' only contains missing values.");
                container.close();

                return new BufferedDataTable[]{container.getTable()};
            }

            Temporal firstStart = getTemporal(first.getCell(column));

            m_prevTemporal = firstStart;

            /* Check if user specified start shall be used. */
            if (m_windowConfig.useSpecifiedStartTime()) {
                firstStart = m_timeConfig.getSelectedDateTime();

                Temporal firstEnd = firstStart.plus(windowDuration.dividedBy(2));

                /* Check for overflow. */
                if (compareTemporal(firstEnd, firstStart) <= 0) {
                    overflow = true;
                }

                /* Move the window until the current end is greater or equal than the first row. */
                while (!overflow && compareTemporal(getTemporal(first.getCell(column)), firstEnd) > 0) {
                    Temporal tempNextEnd = firstEnd.plus(startInterval);

                    /* Check if next window yields an overflow. */
                    if (compareTemporal(tempNextEnd, firstEnd) <= 0) {
                        //                       overflow = true;
                        break;
                    }

                    firstEnd = tempNextEnd;
                }

                /* Current start temporal, m_nextStartTemporal is used to re-use the skipTemporalWindow method. */
                m_nextStartTemporal = firstEnd.minus(windowDuration);
                firstStart = firstEnd.minus(windowDuration.dividedBy(2));

                /* Check for underflow of the current start. */
                if (compareTemporal(m_nextStartTemporal, firstEnd) >= 0 && !overflow) {
                    m_nextStartTemporal = getMin(m_nextStartTemporal);
                    m_bufferedRows.add(first);
                } else {
                    /* Skip window until we find one which contains at least one row. */
                    skipTemporalWindow(first, column, startInterval, windowDuration);
                    firstEnd = m_nextStartTemporal.plus(windowDuration);
                }

                /* Check if we found a window which contains at least one row. */
                if (m_bufferedRows.isEmpty()) {
                    container.close();
                    getLogger().warn("No row lies within any of the possible windows.");

                    return new BufferedDataTable[]{container.getTable()};
                }
            }

            m_windowEndTemporal = firstStart.plus(windowDuration.dividedBy(2));
            /* Might yield an underflow but is used to check if the current window is the last window.*/
            m_nextStartTemporal = firstStart.minus(windowDuration.dividedBy(2));

            if (compareTemporal(m_windowEndTemporal, firstStart) <= 0) {
                overflow = true;
                m_lastWindow = true;
            } else {
                Temporal tempNextEnd = m_windowEndTemporal.plus(startInterval);
                Temporal tempNextStart = tempNextEnd.minus(windowDuration);
                Temporal tempNextMid = tempNextEnd.minus(windowDuration.dividedBy(2));

                /* Check if the current window is the last window. */
                boolean nextEndOverflow = compareTemporal(tempNextEnd, m_windowEndTemporal) <= 0;
                boolean tempNextMidOverflow = nextEndOverflow && compareTemporal(tempNextMid, tempNextEnd) < 0;

                if (tempNextMidOverflow) {
                    m_lastWindow = true;
                } else if (compareTemporal(tempNextEnd, m_windowEndTemporal) > 0
                    && compareTemporal(tempNextStart, tempNextEnd) >= 0) {
                    /* Underflow occurred; set next start to minimum. */
                    m_nextStartTemporal = getMin(tempNextStart);
                } else {
                    m_nextStartTemporal = tempNextStart;
                }
            }

            if (!m_windowConfig.useSpecifiedStartTime()) {
                m_bufferedRows.add(first);
            }
        } else {
            m_prevTemporal = getTemporal(m_bufferedRows.getFirst().getCell(column));

            Temporal tempEnd = m_windowEndTemporal.plus(startInterval);

            /* Check for overflow of the window. */
            if (compareTemporal(tempEnd, m_windowEndTemporal) <= 0) {
                overflow = true;
            } else {
                m_windowEndTemporal = tempEnd;

                Temporal tempNextEnd = m_windowEndTemporal.plus(startInterval);
                Temporal tempNextStart = tempNextEnd.minus(windowDuration);
                Temporal currMid = m_windowEndTemporal.minus(windowDuration.dividedBy(2));
                Temporal tempNextMid = currMid.plus(startInterval);

                /* Check if the current window is the last window. */
                boolean nextEndOverflow = compareTemporal(tempNextEnd, m_windowEndTemporal) <= 0;
                boolean tempNextMidOverflow = nextEndOverflow && compareTemporal(tempNextMid, tempNextEnd) < 0;

                if (tempNextMidOverflow) {
                    m_lastWindow = true;
                } else if (compareTemporal(tempNextEnd, m_windowEndTemporal) > 0
                    && compareTemporal(tempNextStart, tempNextEnd) >= 0) {
                    /* Underflow occurred; set next start to minimum. */
                    m_nextStartTemporal = getMin(tempNextStart);
                } else {
                    m_nextStartTemporal = tempNextStart;
                }
            }
        }

        Iterator<DataRow> bufferedIterator = m_bufferedRows.iterator();
        boolean allBufferedRowsInWindow = true;

        /* Add buffered rows. */
        while (bufferedIterator.hasNext()) {
            DataRow row = bufferedIterator.next();

            Temporal temp = getTemporal(row.getCell(column));

            /* Checks if all buffered rows are in the specified window. */
            if (!overflow && compareTemporal(temp, m_windowEndTemporal) > 0) {
                allBufferedRowsInWindow = false;
                break;
            }

            container.addRowToTable(row);
            m_currRow++;

            if (overflow || m_lastWindow
                || compareTemporal(getTemporal(row.getCell(column)), m_nextStartTemporal) < 0) {
                bufferedIterator.remove();
            }
        }

        boolean lastEntryMissing = false;
        boolean addedNewToBuffer = false;

        /* Add newly read rows. */
        while (m_rowIterator.hasNext() && allBufferedRowsInWindow) {
            DataRow row = m_rowIterator.next();

            if (row.getCell(column).isMissing()) {
                printMissingWarning();
                lastEntryMissing = true;
                continue;
            }

            lastEntryMissing = false;

            Temporal currTemporal = getTemporal(row.getCell(column));

            /* Check if table is sorted in non-descending order according to temporal column. */
            if (compareTemporal(currTemporal, m_prevTemporal) < 0) {
                throw new IllegalStateException("Table not in ascending order concerning chosen temporal column.");
            }

            m_prevTemporal = currTemporal;

            /* Add rows for next window into the buffer. */
            if (!m_lastWindow && compareTemporal(currTemporal, m_nextStartTemporal) >= 0 && !overflow) {
                m_bufferedRows.add(row);
                addedNewToBuffer = true;
            }

            /* Add row to current output. */
            if (overflow || compareTemporal(currTemporal, m_windowEndTemporal) <= 0) {
                container.addRowToTable(row);

                /* The last entry has been in the current window, thus it is the last one. */
                if (!m_rowIterator.hasNext()) {
                    m_lastWindow = true;
                }

                m_currRow++;
            } else {
                break;
            }
        }

        /* Checks if the last row we saw had a missing value. If this is the case the current window is the last window. */
        if (lastEntryMissing) {
            m_lastWindow = true;
        }

        /* Find next entry that lies in a following window. */
        DataRow row = null;

        /* Close iterator if last window has been filled. */
        if (m_lastWindow) {
            m_rowIterator.close();
        } else if (!allBufferedRowsInWindow) {
            /* Not all previously buffered rows are in the current window. */
            row = m_bufferedRows.remove();
        } else if (!m_rowIterator.hasNext() && !addedNewToBuffer) {
            /* We already returned the last row, but it would be in the next window. Nevertheless, terminate. */
            m_rowIterator.close();
            m_lastWindow = true;
        } else if (m_bufferedRows.size() == 0) {
            /* Buffer is empty, so get next row. */
            row = m_rowIterator.next();

            while (row.getCell(column).isMissing() && m_rowIterator.hasNext()) {
                row = m_rowIterator.next();

                printMissingWarning();
            }

            if (row.getCell(column).isMissing()) {
                row = null;

                printMissingWarning();
            }
        } else if (!overflow && !m_bufferedRows.isEmpty()) {
            /* Checks if the next buffered row lies within the given window */
            if (compareTemporal(m_windowEndTemporal.plus(startInterval), m_windowEndTemporal) > 0) {
                Temporal temp = getTemporal(m_bufferedRows.getFirst().getCell(column));

                if (compareTemporal(temp, m_windowEndTemporal.plus(startInterval)) >= 0) {
                    row = m_bufferedRows.removeFirst();
                }
            }
        }

        skipTemporalWindow(row, column, startInterval, windowDuration);

        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * @param firstStart
     * @return
     */
    private Temporal getMin(final Temporal t1) {
        if (t1 instanceof LocalTime) {
            return LocalTime.MIN;
        } else if (t1 instanceof LocalDateTime) {
            return LocalDateTime.MIN;
        } else if (t1 instanceof LocalDate) {
            return LocalDate.MIN;
        }

        throw new IllegalArgumentException(
            "Data must be of type LocalDate, LocalDateTime, LocalTime, or ZonedDateTime");
    }

    /**
     * Skips the window for temporal data types until we obtain a window containing at least one row.
     *
     * @param row that is currently considered.
     * @param column index of the time column.
     * @param startInterval starting interval of the windows.
     * @param windowDuration duration of the window.
     */
    private void skipTemporalWindow(DataRow row, final int column, final TemporalAmount startInterval,
        final TemporalAmount windowDuration) {
        while (row != null) {
            /* Check if current row lies beyond next starting temporal. */
            while (compareTemporal(getTemporal(row.getCell(column)), m_nextStartTemporal) < 0
                && m_rowIterator.hasNext()) {
                DataRow temp = m_rowIterator.next();

                if (temp.getCell(column).isMissing()) {
                    printMissingWarning();

                    continue;
                } else if (compareTemporal(m_prevTemporal, getTemporal(temp.getCell(column))) > 0) {
                    throw new IllegalStateException(
                        "Table not in ascending order concerning chosen temporal column (use Sorter prior to Windo Loop Start).");
                }

                m_prevTemporal = getTemporal(row.getCell(column));

                row = temp;
            }

            /* Check for overflow of the window. Necessary for the three different window definitions.*/
            if (compareTemporal(m_nextStartTemporal.plus(windowDuration), m_nextStartTemporal) < 0) {
                switch (m_windowConfig.getWindowDefinition()) {
                    case FORWARD:
                        m_bufferedRows.addFirst(row);
                        break;
                    case BACKWARD:
                        break;
                    case CENTRAL:
                        /* Check if currently considered central time point lies before the overflow. */
                        if (compareTemporal(m_nextStartTemporal.plus(windowDuration),
                            getMin(m_nextStartTemporal).plus(((Duration)windowDuration).dividedBy(2))) < 0) {
                            /* Check if the current row lies after the current starting point. */
                            if (compareTemporal(getTemporal(row.getCell(column)), m_nextStartTemporal) >= 0) {
                                m_bufferedRows.addFirst(row);
                            }
                        }
                        break;
                    default:
                }

                break;
            }

            /* Checks if current row lies within next temporal window */
            if (compareTemporal(getTemporal(row.getCell(column)),
                m_windowEndTemporal.plus(startInterval)/*m_nextStartTemporal.plus(windowDuration)*/) <= 0) {
                m_bufferedRows.addFirst(row);
                break;
            } else if (compareTemporal(getTemporal(row.getCell(column)), m_nextStartTemporal) <= 0
                && !m_rowIterator.hasNext()) {
                /* There are no more rows that could lie within an upcoming window. */
                break;
            }

            /* If next row lies beyond the defined next window move it until the rows lies within an upcoming window or the window passed said row. */
            Temporal tempNextEnd = m_windowEndTemporal.plus(startInterval).plus(startInterval);
            Temporal nextTemporalStart = tempNextEnd.minus(windowDuration);

            /* Checks for overflow. */
            if (compareTemporal(tempNextEnd, m_windowEndTemporal) <= 0
                && compareTemporal(nextTemporalStart, tempNextEnd) <= 0) {
                m_rowIterator.close();
                break;
            } else if (compareTemporal(nextTemporalStart, tempNextEnd) >= 0) {
                m_nextStartTemporal = getMin(m_nextStartTemporal);
            } else {
                m_nextStartTemporal = nextTemporalStart;
            }

            m_windowEndTemporal = m_windowEndTemporal.plus(startInterval);

            //            Temporal nextTemporalStart = m_nextStartTemporal.plus(startInterval);
            //
            //            /* Check for overflow of the next starting interval. */
            //            if (compareTemporal(nextTemporalStart, m_nextStartTemporal) <= 0) {
            //                m_rowIterator.close();
            //                break;
            //            } else {
            //                m_nextStartTemporal = nextTemporalStart;
            //            }
        }

    }

    /**
     * Compares the temporal.
     *
     * @param t1 first temporal
     * @param t2 second temporal
     * @return the comparator value, negative if less, positive if greater
     */
    private int compareTemporal(final Temporal t1, final Temporal t2) {
        if (t1 instanceof LocalTime) {
            return ((LocalTime)t1).compareTo((LocalTime)t2);
        } else if (t1 instanceof LocalDateTime) {
            return ((LocalDateTime)t1).compareTo((LocalDateTime)t2);
        } else if (t1 instanceof LocalDate) {
            return ((LocalDate)t1).compareTo((LocalDate)t2);
        } else if (t1 instanceof ZonedDateTime) {
            return ((ZonedDateTime)t1).compareTo((ZonedDateTime)t2);
        }

        throw new IllegalArgumentException(
            "Data must be of type LocalDate, LocalDateTime, LocalTime, or ZonedDateTime");
    }

    /**
     * Returns the content of the given DataCell.
     *
     * @param cell which holds the content
     * @return temporal object of the cell, null if the DataCell does not contain a temporal object.
     */
    private Temporal getTemporal(final DataCell cell) {
        if (cell instanceof LocalTimeCell) {
            return ((LocalTimeCell)cell).getLocalTime();
        } else if (cell instanceof LocalDateCell) {
            LocalDate date = ((LocalDateCell)cell).getLocalDate();
            LocalDateTime dateTime = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0);
            return dateTime;
        } else if (cell instanceof LocalDateTimeCell) {
            return ((LocalDateTimeCell)cell).getLocalDateTime();
        } else if (cell instanceof ZonedDateTimeCell) {
            return ((ZonedDateTimeCell)cell).getZonedDateTime();
        }

        return null;
    }

    /**
     * Executes backward windowing.
     *
     * @param table input data
     * @param exec ExecutionContext
     * @return BufferedDataTable containing the current loop.
     */
    private BufferedDataTable[] executeBackward(final BufferedDataTable table, final ExecutionContext exec) {
        int windowSize = m_windowConfig.getEventWindowSize();
        int stepSize = m_windowConfig.getEventStepSize();
        int currRowCount = 0;

        BufferedDataContainer container = exec.createDataContainer(table.getSpec());

        /* Jump to next following row if step size is greater than the window size. */
        if (stepSize > windowSize && m_currRow > 0) {
            int diff = stepSize - windowSize;

            while (diff > 0 && m_rowIterator.hasNext()) {
                m_rowIterator.next();
                diff--;
            }
        }

        /* If window is limited, i.e. no missing rows shall be inserted, move the window until there are no missing rows. */
        if (m_windowConfig.getLimitWindow() && m_currRow < windowSize) {
            /* windowSize-1 are the number of rows we have in front of the considered row. */
            long bufferStart = -(windowSize - 1);
            long nextRow = m_currRow;

            while (bufferStart < 0) {
                bufferStart += stepSize;
                nextRow += stepSize;
            }

            while (m_currRow < bufferStart && m_rowIterator.hasNext()) {
                m_rowIterator.next();
                m_currRow++;
            }

            while (m_currRow < nextRow && m_rowIterator.hasNext()) {
                m_bufferedRows.add(m_rowIterator.next());
                m_currRow++;
            }
        }

        /* Add missing preceding rows to fill up the window at the beginning of the loop. */
        while (container.size() < windowSize - (m_currRow + 1)) {
            container.addRowToTable(new MissingRow(m_nColumns));
            currRowCount++;
        }

        /* Add buffered rows that overlap. */
        Iterator<DataRow> bufferedIterator = m_bufferedRows.iterator();

        while (bufferedIterator.hasNext()) {
            container.addRowToTable(bufferedIterator.next());

            if (currRowCount < stepSize) {
                bufferedIterator.remove();
            }

            currRowCount++;
        }

        /* Add newly read rows. */
        for (; container.size() < windowSize && m_rowIterator.hasNext(); currRowCount++) {
            DataRow dRow = m_rowIterator.next();

            if (currRowCount >= stepSize) {
                m_bufferedRows.add(dRow);
            }

            container.addRowToTable(dRow);
        }

        m_currRow += stepSize;

        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * Executes central windowing
     *
     * @param table input data
     * @param exec ExecutionContext
     * @return BufferedDataTable containing the current loop.
     */
    private BufferedDataTable[] executeCentral(final BufferedDataTable table, final ExecutionContext exec) {
        int windowSize = m_windowConfig.getEventWindowSize();
        int stepSize = m_windowConfig.getEventStepSize();
        int currRowCount = 0;

        BufferedDataContainer container = exec.createDataContainer(table.getSpec());

        /* Jump to next following row if step size is greater than the window size.*/
        if (stepSize > windowSize && m_currRow > 0) {
            int diff = stepSize - windowSize;

            while (diff > 0 && m_rowIterator.hasNext()) {
                m_rowIterator.next();
                diff--;
            }
        }

        /* If window is limited, i.e. no missing rows shall be inserted, move the window until there are no missing rows. */
        if (m_windowConfig.getLimitWindow() && m_currRow < Math.floorDiv(windowSize, 2)) {
            long bufferStart = -Math.floorDiv(windowSize, 2);
            long nextRow = m_currRow;

            while (bufferStart < 0) {
                bufferStart += stepSize;
                nextRow += stepSize;
            }

            while (m_currRow < bufferStart && m_rowIterator.hasNext()) {
                m_rowIterator.next();
                m_currRow++;
            }

            while (m_currRow < nextRow && m_rowIterator.hasNext()) {
                m_bufferedRows.add(m_rowIterator.next());
                m_currRow++;
            }
        }

        /* Fill missing preceding rows with missing values. Only needed at the start of*/
        while (container.size() < Math.floorDiv(windowSize, 2) - (m_currRow)) {
            container.addRowToTable(new MissingRow(m_nColumns));
            currRowCount++;
        }

        Iterator<DataRow> bufferedIterator = m_bufferedRows.iterator();

        /* Add buffered rows that overlap. */
        while (bufferedIterator.hasNext()) {
            container.addRowToTable(bufferedIterator.next());

            if (currRowCount < stepSize) {
                bufferedIterator.remove();
            }

            currRowCount++;
        }

        /* Add newly read rows. */
        for (; container.size() < windowSize && m_rowIterator.hasNext(); currRowCount++) {
            DataRow dRow = m_rowIterator.next();

            if (currRowCount >= stepSize) {
                m_bufferedRows.add(dRow);
            }

            container.addRowToTable(dRow);
        }

        /* Add missing rows to fill up the window. */
        while (container.size() < windowSize) {
            container.addRowToTable(new MissingRow(m_nColumns));
        }

        m_currRow += stepSize;

        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * Executes forward windowing
     *
     * @param table input data
     * @param exec ExecutionContext
     * @return BufferedDataTable containing the current loop.
     */
    private BufferedDataTable[] executeForward(final BufferedDataTable table, final ExecutionContext exec) {
        int windowSize = m_windowConfig.getEventWindowSize();
        int stepSize = m_windowConfig.getEventStepSize();
        int currRowCount = 0;

        /* Jump to next following row if step size is greater than the window size.*/
        if (stepSize > windowSize && m_currRow > 0) {
            int diff = stepSize - windowSize;

            while (diff > 0 && m_rowIterator.hasNext()) {
                m_rowIterator.next();
                diff--;
            }
        }

        BufferedDataContainer container = exec.createDataContainer(table.getSpec());
        Iterator<DataRow> bufferedIterator = m_bufferedRows.iterator();

        /* Add buffered rows that overlap. */
        while (bufferedIterator.hasNext()) {
            container.addRowToTable(bufferedIterator.next());

            if (currRowCount < stepSize) {
                bufferedIterator.remove();
            }

            currRowCount++;
        }

        /* Add newly read rows. */
        for (; container.size() < windowSize && m_rowIterator.hasNext(); currRowCount++) {
            DataRow dRow = m_rowIterator.next();

            if (currRowCount >= stepSize) {
                m_bufferedRows.add(dRow);
            }

            container.addRowToTable(dRow);
        }

        /* Add missing rows to fill up the window. */
        while (container.size() < windowSize) {
            container.addRowToTable(new MissingRow(m_nColumns));
        }

        m_currRow += stepSize;

        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currRow = 0;
        MissingRow.rowCounter = 0;
        m_lastWindow = false;
        m_printedMissingWarning = false;

        if (m_rowIterator != null) {
            m_rowIterator.close();
        }

        m_rowIterator = null;
        m_nextStartTemporal = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean terminateLoop() {
        if (m_windowConfig.getTrigger().equals(Trigger.ROW)) {
            /* If we limit the window to fit in the table we might terminate earlier. */
            if (m_windowConfig.getLimitWindow()) {
                /* Given window is too large. */
                if (m_rowCount < m_windowConfig.getEventWindowSize()) {
                    return true;
                }

                switch (m_windowConfig.getWindowDefinition()) {
                    case FORWARD:
                        return m_rowCount - m_currRow < m_windowConfig.getEventWindowSize();
                    case BACKWARD:
                        return m_currRow >= m_rowCount;
                    case CENTRAL:
                        return m_rowCount - m_currRow < Math.ceil(((double)m_windowConfig.getEventWindowSize()) / 2)
                            - 1;
                    default:
                        return true;
                }
            }

            return m_currRow >= m_rowCount;
        }

        return m_lastWindow || !m_rowIterator.hasNext() && (m_bufferedRows == null || m_bufferedRows.isEmpty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_windowConfig != null) {
            m_windowConfig.saveSettingsTo(settings);

            if (m_windowConfig.getTrigger() == Trigger.TIME) {
                settingsModel.saveSettingsTo(settings);
                //                m_timeColumnName = settingsModel.getStringValue();

                if (m_windowConfig.useSpecifiedStartTime()) {
                    createStartModel().saveSettingsTo(settings);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new LoopStartWindowConfiguration().loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        LoopStartWindowConfiguration config = new LoopStartWindowConfiguration();
        config.loadSettingsInModel(settings);
        m_windowConfig = config;

        if (m_windowConfig.getTrigger() == Trigger.TIME) {
            settingsModel.loadSettingsFrom(settings);
            m_timeColumnName = settingsModel.getStringValue();

            m_timeConfig.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to save
    }

    /**
     * An InputRow with solely missing data cells, needed for different window definitions. Copied:
     * org.knime.base.node.preproc.joiner.DataHiliteOutputContainer.Missing
     *
     * @author Heiko Hofer
     */
    static class MissingRow implements DataRow {
        private DataCell[] m_cells;

        private static int rowCounter = 0;

        private static String rowName = "LSW_Missing_Row";

        /**
         * @param numCells The number of cells in the {@link DataRow}
         */
        public MissingRow(final int numCells) {
            m_cells = new DataCell[numCells];
            for (int i = 0; i < numCells; i++) {
                m_cells[i] = DataType.getMissingCell();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowKey getKey() {
            return new RowKey(rowName + (rowCounter++));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final int index) {
            return m_cells[index];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumCells() {
            return m_cells.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<DataCell> iterator() {
            return Arrays.asList(m_cells).iterator();
        }
    }

    /**
     * @return settings model for column selection
     */
    static final SettingsModelString createColumnModel() {
        return new SettingsModelString("selectedTimeColumn", null);
    }

    /**
     * @return the date&time model, used in both dialog and model.
     */
    static SettingsModelDateTime createStartModel() {
        final SettingsModelDateTime settingsModelDateTime =
            new SettingsModelDateTime("start", LocalDateTime.now().withNano(0).minusYears(1).minusHours(1));
        return settingsModelDateTime;
    }
}
