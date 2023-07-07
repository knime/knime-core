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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow.metadata;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.knime.core.node.NodeLogger;

/**
 * Helper class for the meta info file which contains the meta information entered by the user for workflow groups and
 * workflows, such as author, date, comments.
 *
 * Fabian Dill wrote the original version off which this class is based.
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class MetaInfoFile {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MetaInfoFile.class);

    private static final String DATE_SEPARATOR = "/";

    /**
     * Given the date, return the string representation that we use to store the date within XML.
     *
     * @param calendar an instance of {@link Calendar} containing the date to represent.
     * @return string representation
     */
    public static String dateToStorageString(final Calendar calendar) {
        return dateToStorageString(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH),
            calendar.get(Calendar.YEAR));
    }

    /**
     * Given the date, return the string representation that we use to store the date within XML.
     *
     * @param day 1-31
     * @param month 0-11
     * @param year 2008 - 2015 (for now)
     * @return string representation
     */
    public static String dateToStorageString(final int day, final int month, final int year) {
        return day + DATE_SEPARATOR + (month - 1) + DATE_SEPARATOR + year;
    }

    /**
     * Given a string value created in the correct format, return a correctly populated {@link Calendar} instance;
     * if it's not in the correct format, null is returned.
     *
     * @param value a correctly formatted date string
     * @return a correctly populated calendar or null if the date string is in a wrong format
     */
    public static Calendar calendarFromDateString(final String value) {
        if (value != null) {
            final String[] elements = value.trim().split(DATE_SEPARATOR);

            // Greater than because date strings made elsewhere (server?) can look like:
            //      13/5/2018/10:28:12 +02:00
            if (elements.length >= 3) {
                try {
                    final var day = Math.min(Math.max(1, Integer.parseInt(elements[0])), 31);
                    final var month = Math.min(Math.max(1, Integer.parseInt(elements[1]) + 1), 12);
                    final var year = Math.min(Math.max(Year.MIN_VALUE, Integer.parseInt(elements[2])), Year.MAX_VALUE);
                    return GregorianCalendar.from(LocalDateTime.of(year, month, day, 12, 1) //
                            .atZone(ZoneId.systemDefault()));
                } catch (final NumberFormatException | DateTimeException nfe) {
                    LOGGER.error("Unable to parse date string [" + value + "]", nfe);
                }
            }
        }

        return null;
    }

    private MetaInfoFile() { }
}
