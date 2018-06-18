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
 *   Jun 13, 2018 (moritz): created
 */
package org.knime.base.expressions.datetime;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

import org.knime.expressions.core.ExpressionJavaMethodProvider;
import org.knime.expressions.core.exceptions.ScriptExecutionException;

/**
 * Provides date/time functionality to the column expression node.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 *
 */
public final class DateTimeExpressionJavaMethodProvider implements ExpressionJavaMethodProvider {

    final static String ID = DateTimeExpressionJavaMethodProvider.class.getName().replaceAll("\\.", "_");

    /**
     * Parses the provided String to LocalDate.
     *
     * @param date String containing the date.
     * @return The LocalDate.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public LocalDate date(final String date) throws ScriptExecutionException {
        if (date == null) {
            throw new ScriptExecutionException("The provided date must not be null.");
        }

        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates a LocalDate from the year, month, and day.
     *
     * @param year Year of the Date.
     * @param month Month of the Date.
     * @param day Day of the Date.
     * @return The LocalDate.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public LocalDate date(final Object year, final Object month, final Object day) throws ScriptExecutionException {
        checkNumberType(year, "year");
        checkNumberType(month, "month");
        checkNumberType(day, "day");

        try {
            return LocalDate.of(((Number)year).intValue(), ((Number)month).intValue(), ((Number)day).intValue());
        } catch (DateTimeException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates LocalDateTime from String.
     *
     * @param dateTime String containing LocalDateTime
     * @return The LocalDateTime.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public LocalDateTime dateTime(final String dateTime) throws ScriptExecutionException {
        if (dateTime == null) {
            throw new ScriptExecutionException("The provided date must not be null.");
        }

        try {
            return LocalDateTime.parse(dateTime);
        } catch (DateTimeParseException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates LocalDateTime.
     *
     * @param year Year.
     * @param month Month.
     * @param day Day.
     * @param hour Hour.
     * @param minute Minute.
     * @param second Second.
     * @return The LocalDateTime.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public LocalDateTime LocalDateTime(final Object year, final Object month, final Object day, final Object hour,
        final Object minute, final Object second) throws ScriptExecutionException {
        checkNumberType(year, "year");
        checkNumberType(month, "month");
        checkNumberType(day, "day");
        checkNumberType(hour, "hours");
        checkNumberType(minute, "minute");
        checkNumberType(second, "seconds");

        try {
            return LocalDateTime.of(((Number)year).intValue(), ((Number)month).intValue(), ((Number)day).intValue(),
                ((Number)hour).intValue(), ((Number)minute).intValue(), ((Number)second).intValue());
        } catch (DateTimeException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates LocalTime from String.
     *
     * @param time String of LocalTime.
     * @return The LocalTime.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public LocalTime time(final String time) throws ScriptExecutionException {
        if (time == null) {
            throw new ScriptExecutionException("The provided time must not be null.");
        }

        try {
            return LocalTime.parse(time);
        } catch (DateTimeParseException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates LocalTime.
     *
     * @param hours Hours.
     * @param minutes Minutes.
     * @param seconds Seconds.
     * @return The LocalTime.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public LocalTime time(final Object hours, final Object minutes, final Object seconds)
        throws ScriptExecutionException {
        checkNumberType(hours, "hours");
        checkNumberType(minutes, "minute");
        checkNumberType(seconds, "seconds");

        try {
            return LocalTime.of(((Number)hours).intValue(), ((Number)minutes).intValue(), ((Number)seconds).intValue());
        } catch (DateTimeException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates Duration of hours.
     *
     * @param hours Hours.
     * @return The Duration.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Duration durationOfHours(final Object hours) throws ScriptExecutionException {
        checkNumberType(hours, "hours");

        try {
            return Duration.ofHours(((Number)hours).longValue());
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates Duration of minutes.
     *
     * @param minutes Minutes.
     * @return The Duration.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Duration durationOfMinutes(final Object minutes) throws ScriptExecutionException {
        checkNumberType(minutes, "minutes");

        try {
            return Duration.ofMinutes(((Number)minutes).longValue());
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates Duration of seconds.
     *
     * @param seconds Seconds.
     * @return The Duration.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Duration durationOfSeconds(final Object seconds) throws ScriptExecutionException {
        checkNumberType(seconds, "seconds");

        try {
            return Duration.ofSeconds(((Number)seconds).longValue());
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates Period of days.
     *
     * @param days Days.
     * @return The Period.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Period periodOfDays(final Object days) throws ScriptExecutionException {
        checkNumberType(days, "days");

        try {
            return Period.ofDays(((Number)days).intValue());
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates Period of months.
     *
     * @param months Months.
     * @return The Period.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Period periodOfMonths(final Object months) throws ScriptExecutionException {
        checkNumberType(months, "months");

        try {
            return Period.ofMonths(((Number)months).intValue());
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates Period of weeks.
     *
     * @param weeks Weeks.
     * @return The Period.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Period periodOfWeeks(final Object weeks) throws ScriptExecutionException {
        checkNumberType(weeks, "weeks");

        try {
            return Period.ofWeeks(((Number)weeks).intValue());
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates Period of years.
     *
     * @param years Years.
     * @return The Period.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Period periodOfYears(final Object years) throws ScriptExecutionException {
        checkNumberType(years, "years");

        try {
            return Period.ofYears(((Number)years).intValue());
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Adds a TemporalAmount to a Temporal.
     *
     * @param temp The Temporal.
     * @param amount The TemporalAmount
     * @return The result.
     *
     * @throws ScriptExecutionException If an error occurs.
     */
    public Temporal plusTemporal(final Object temp, final Object amount) throws ScriptExecutionException {
        if (temp == null) {
            throw new ScriptExecutionException("The provided temporal must not be null.");
        } else if (amount == null) {
            throw new ScriptExecutionException("The provided temporalAmount must not be null.");
        }

        if (!(temp instanceof Temporal)) {
            throw new ScriptExecutionException("Type of temporal ('" + temp.getClass().getSimpleName()
                + "') not valid. Must be one of the following: LocalDate, LocalDateTime, LocalTime.");
        } else if (!(amount instanceof TemporalAmount)) {
            throw new ScriptExecutionException("Type of temporalAmount ('" + temp.getClass().getSimpleName()
                + "') not valid. Must be one of the following: Period, Duration.");
        } else if (temp instanceof LocalDate && amount instanceof Duration) {
            throw new ScriptExecutionException(
                "Temporal of type 'LocalDate' is not compatible with temporalAmount of type "
                    + "'Duration'.Expected temporalAmount to be of type 'Period'.");
        } else if (temp instanceof LocalTime && amount instanceof Period) {
            throw new ScriptExecutionException(
                "Temporal of type 'LocalTime' is not compatible with temporalAmount of type "
                    + "'Period'. Expected temporalAmount to be of type 'Duration'.");
        }

        try {
            return ((Temporal)temp).plus((TemporalAmount)amount);
        } catch (Exception e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Checks if the object is of type {@link Number}.
     *
     * @param obj Object to be checked.
     * @param name The name that shall be shown in the error message.
     *
     * @throws ScriptExecutionException If obj is null or not of type {@link Number}.
     */
    private static void checkNumberType(final Object obj, final String name) throws ScriptExecutionException {
        if (obj == null) {
            throw new ScriptExecutionException("The provided " + name + " must not be null");
        }

        if (!(obj instanceof Number)) {
            throw new ScriptExecutionException(
                "Type of " + name + " ('" + obj.getClass().getSimpleName() + "') not valid. Must be a number.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier() {
        return ID;
    }

}
