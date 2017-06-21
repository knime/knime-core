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
 *   Feb 16, 2017 (simon): created
 */
package org.knime.time.util;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;

/**
 * Utilities to format and parse {@link Duration}s and {@link Period}s.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public final class DurationPeriodFormatUtils {
    /**
     * Formats the given duration into a time string of format H:mm:ss.S.
     *
     * @param duration the duration to format
     * @return formatted string
     */
    public static String formatDurationShort(final Duration duration) {
        String s = duration.toString();
        // remove leading 'PT'
        s = StringUtils.replaceOnce(s, "PT", " ");

        // replace ISO letters with time letters
        s = StringUtils.replaceOnce(s, "H", "H ");
        s = StringUtils.replaceOnce(s, "M", "m ");
        s = StringUtils.replaceOnce(s, "S", "s ");

        return s.trim();
    }

    /**
     * Formats the given duration into a pluralization correct string.
     *
     * @param duration the duration to format
     * @return formatted string
     */
    public static String formatDurationLong(final Duration duration) {
        String s = duration.toString();
        // remove leading 'PT'
        s = StringUtils.replaceOnce(s, "PT", " ");

        // replace ISO letters with words
        s = StringUtils.replaceOnce(s, "H", " hours ");
        s = StringUtils.replaceOnce(s, "M", " minutes ");
        s = StringUtils.replaceOnce(s, "S", " seconds ");

        // handle plurals
        s = StringUtils.replaceOnce(s, " 1 seconds", " 1 second");
        s = StringUtils.replaceOnce(s, " 1 minutes", " 1 minute");
        s = StringUtils.replaceOnce(s, " 1 hours", " 1 hour");
        s = StringUtils.replaceOnce(s, " -1 seconds", " -1 second");
        s = StringUtils.replaceOnce(s, " -1 minutes", " -1 minute");
        s = StringUtils.replaceOnce(s, " -1 hours", " -1 hour");
        return s.trim();
    }

    /**
     * Formats the given period into a string of format y'y' m'M' d'd'.
     *
     * @param period the period to format
     * @return formatted string
     */
    public static String formatPeriodShort(final Period period) {
        String s = period.toString();
        // remove leading 'P'
        s = StringUtils.replaceOnce(s, "P", " ");

        // replace ISO letters with date letters
        s = StringUtils.replaceOnce(s, "Y", "y ");
        s = StringUtils.replaceOnce(s, "M", "M ");
        s = StringUtils.replaceOnce(s, "W", "W ");
        s = StringUtils.replaceOnce(s, "D", "d ");

        return s.trim();
    }

    /**
     * Formats the given period into a pluralization correct string.
     *
     * @param period the period to format
     * @return formatted string
     */
    public static String formatPeriodLong(final Period period) {
        String s = period.toString();
        // remove leading 'P'
        s = StringUtils.replaceOnce(s, "P", " ");

        // replace ISO letters with words
        s = StringUtils.replaceOnce(s, "Y", " years ");
        s = StringUtils.replaceOnce(s, "M", " months ");
        s = StringUtils.replaceOnce(s, "W", " weeks ");
        s = StringUtils.replaceOnce(s, "D", " days ");

        // handle plurals
        s = StringUtils.replaceOnce(s, " 1 years", " 1 year");
        s = StringUtils.replaceOnce(s, " -1 years", " -1 year");
        s = StringUtils.replaceOnce(s, " 1 months", " 1 month");
        s = StringUtils.replaceOnce(s, " 1 weeks", " 1 week");
        s = StringUtils.replaceOnce(s, " -1 months", " -1 month");
        s = StringUtils.replaceOnce(s, " 1 days", " 1 day");
        s = StringUtils.replaceOnce(s, " -1 days", " -1 day");

        return s.trim();
    }

    /**
     * Complement of {@link #formatDurationShort(Duration)} and {@link #formatDurationLong(Duration)}. If you enter a
     * string in ISO-8601, it will not be modified.
     *
     * @param text the text to parse, not null
     * @return the parsed duration, not null
     * @throws DateTimeParseException if the text cannot be parsed to a duration
     */
    public static Duration parseDuration(final String text) throws DateTimeParseException {
        String s = text;
        // check for correct ISO usage
        if (s.startsWith("P") && !s.startsWith("PT")) {
            throw new DateTimeParseException("A leading 'P' indicates a date-based duration, not a time-based duration.", s, 0);
        }

        // replace words with ISO letters
        s = StringUtils.replaceOnce(s, "hours", "H");
        s = StringUtils.replaceOnce(s, "minutes", "m");
        s = StringUtils.replaceOnce(s, "seconds", "s");
        s = StringUtils.replaceOnce(s, "hour", "H");
        s = StringUtils.replaceOnce(s, "minute", "m");
        s = StringUtils.replaceOnce(s, "second", "s");

        // add leading 'PT'
        if (!s.startsWith("PT") && !s.startsWith("-PT")) {
            // check for correct usage of 'm' and 'M'
            final int idx = StringUtils.indexOf(s, "M");
            if (idx >= 0) {
                throw new DateTimeParseException(
                    "'M' stands for months and cannot be parsed as part of a time-based duration. Use 'm' for minutes instead.", s,
                    idx);
            }
            s = "PT" + s;
        }

        // remove whitespaces
        s = s.replaceAll("\\s+", "");

        return Duration.parse(s);
    }

    /**
     * Complement of {@link #formatPeriodShort(Period)} and {@link #formatPeriodLong(Period)}. If you enter a string in
     * ISO-8601, it will not be modified.
     *
     * @param text the text to parse, not null
     * @return the parsed period, not null
     * @throws DateTimeParseException if the text cannot be parsed to a period
     */
    public static Period parsePeriod(final String text) throws DateTimeParseException {
        String s = text;
        // check for correct ISO usage
        if (s.startsWith("PT")) {
            throw new DateTimeParseException("A leading 'PT' indicates a time-based duration, not a date-based duration.", s, 0);
        }

        // replace words with ISO letters
        s = StringUtils.replaceOnce(s, "years", "y");
        s = StringUtils.replaceOnce(s, "months", "M");
        s = StringUtils.replaceOnce(s, "weeks", "w");
        s = StringUtils.replaceOnce(s, "days", "d");
        s = StringUtils.replaceOnce(s, "year", "y");
        s = StringUtils.replaceOnce(s, "month", "M");
        s = StringUtils.replaceOnce(s, "week", "w");
        s = StringUtils.replaceOnce(s, "day", "d");

        // add leading 'P'
        if (!s.startsWith("P") && !s.startsWith("-P")) {
            // check for correct usage of 'm' and 'M'
            final int idx = StringUtils.indexOf(s, "m");
            if (idx >= 0) {
                throw new DateTimeParseException(
                    "'m' stands for minutes and cannot be parsed as part of a date-based duration. Use 'M' for months instead.",
                    s, idx);
            }
            s = "P" + s;
        }

        // remove whitespaces
        s = s.replaceAll("\\s+", "");

        return Period.parse(s);
    }

}
