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
 *   Oct 10, 2016 (simon): created
 */
package org.knime.time.util;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

/**
 * An enumeration that contains all different granularities for Date&Time shifting.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public enum Granularity {
        YEAR(ChronoUnit.YEARS), MONTH(ChronoUnit.MONTHS), WEEK(ChronoUnit.WEEKS), DAY(ChronoUnit.DAYS),
        HOUR(ChronoUnit.HOURS), MINUTE(ChronoUnit.MINUTES), SECOND(ChronoUnit.SECONDS), MILLISECOND(ChronoUnit.MILLIS),
        MICROSECOND(ChronoUnit.MICROS), NANOSECOND(ChronoUnit.NANOS);

    private final ChronoUnit m_chronoUnit;

    private Granularity(final ChronoUnit chronoUnit) {
        m_chronoUnit = chronoUnit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_chronoUnit.toString();
    }

    /**
     * @return true if granularity belongs to a date, and false if it belongs to a time
     */
    public boolean isPartOfDate() {
        return m_chronoUnit.isDateBased();
    }

    /**
     * @return a string array containing all string representations of the enums
     */
    public static String[] strings() {
        final Granularity[] granularities = values();
        final String[] strings = new String[granularities.length];

        for (int i = 0; i < granularities.length; i++) {
            strings[i] = granularities[i].toString();
        }

        return strings;
    }

    /**
     * @param name name of the enum
     * @return the {@link Granularity}
     */
    public static Granularity fromString(final String name) {
        if (name != null) {
            for (Granularity granularity : Granularity.values()) {
                if (name.equals(granularity.toString())) {
                    return granularity;
                }
            }
        }
        throw new IllegalArgumentException("No constant with text " + name + " found");
    }

    /**
     * @param value input parameter for {@link Period} or {@link Duration}
     * @return {@link Period} or {@link Duration}
     * @throws ArithmeticException if the input overflows an integer
     */
    public TemporalAmount getPeriodOrDuration(final long value) throws ArithmeticException {
        final String name = name();
        if (name.equals(YEAR.name())) {
            return Period.ofYears(Math.toIntExact(value));
        }
        if (name.equals(MONTH.name())) {
            return Period.ofMonths(Math.toIntExact(value));
        }
        if (name.equals(WEEK.name())) {
            return Period.ofWeeks(Math.toIntExact(value));
        }
        if (name.equals(DAY.name())) {
            return Period.ofDays(Math.toIntExact(value));
        }
        if (name.equals(HOUR.name())) {
            return Duration.ofHours(value);
        }
        if (name.equals(MINUTE.name())) {
            return Duration.ofMinutes(value);
        }
        if (name.equals(SECOND.name())) {
            return Duration.ofSeconds(value);
        }
        if (name.equals(MILLISECOND.name())) {
            return Duration.ofMillis(value);
        }
        if (name.equals(MICROSECOND.name())) {
            return Duration.ofNanos(value * 1000);
        }
        if (name.equals(NANOSECOND.name())) {
            return Duration.ofNanos(value);
        }
        throw new IllegalStateException(name() + " not defined.");
    }

    /**
     * Calculates the amount of time between two temporal objects.
     *
     * @param temporal1Inclusive the base temporal object, not null
     * @param temporal2Exclusive the other temporal object, exclusive, not null
     * @return the amount of time between temporal1Inclusive and temporal2Exclusive in terms of this unit; positive if
     *         temporal2Exclusive is later than temporal1Inclusive, negative if earlier
     */
    public long between(final Temporal temporal1Inclusive, final Temporal temporal2Exclusive) {
        return m_chronoUnit.between(temporal1Inclusive, temporal2Exclusive);
    }

    /**
     * @return the {@link ChronoUnit} of this {@link Granularity}
     */
    public ChronoUnit getChronoUnit() {
        return m_chronoUnit;
    }
}
