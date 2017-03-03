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
package org.knime.time;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;

import org.knime.core.data.DataValue;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;

/**
 * An enumeration that contains all different granularities for Date&Time shifting.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public enum Granularity {

    YEAR("Year", PeriodValue.class),
    MONTH("Month", PeriodValue.class),
    WEEK("Week", PeriodValue.class),
    DAY("Day", PeriodValue.class),
    HOUR("Hour", DurationValue.class),
    MINUTE("Minute", DurationValue.class),
    SECOND("Second", DurationValue.class),
    MILLISECOND("Millisecond", DurationValue.class),
    NANOSECOND("Nanosecond", DurationValue.class);

    private final String m_name;

    private final Class<? extends DataValue> m_dataValue;

    private Granularity(final String name, final Class<? extends DataValue> dataValue) {
        m_name = name;
        m_dataValue = dataValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_name;
    }

    /**
     * @return true if granularity belongs to a date, and false if it belongs to a time
     */
    public boolean isPartOfDate() {
        return m_dataValue.equals(PeriodValue.class);
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
                if (name.equalsIgnoreCase(granularity.name())) {
                    return granularity;
                }
            }
        }
        throw new IllegalArgumentException("No constant with text " + name + " found");
    }

    /**
     * @param value input parameter for {@link Period} or {@link Duration}
     * @return {@link Period} or {@link Duration}
     */
    public TemporalAmount getPeriodOrDuration(final int value) {
        final String name = name();
        if (name.equals(YEAR.name())) {
            return Period.ofYears(value);
        }
        if (name.equals(MONTH.name())) {
            return Period.ofMonths(value);
        }
        if (name.equals(WEEK.name())) {
            return Period.ofWeeks(value);
        }
        if (name.equals(DAY.name())) {
            return Period.ofDays(value);
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
        if (name.equals(NANOSECOND.name())) {
            return Duration.ofNanos(value);
        }
        throw new IllegalStateException(name() + " not defined.");
    }
}
