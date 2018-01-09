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
 *   05.09.2016 (Jonathan Hale): created
 */
package org.knime.base.node.jsnippet.type;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.datacell.ArrayToCollectionConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.datacell.SimpleJavaToDataCellConverterFactory;
import org.knime.core.data.convert.java.CollectionConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.java.SimpleDataCellToJavaConverterFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;

/**
 * Contains utilities and converter factories for the deprecated {@link DateAndTimeValue} and {@link DateAndTimeCell}.
 *
 * Intended to keep all references to deprecated code in one place for easy removal.
 *
 * @author Jonathan Hale, KNIME.com, Konstanz, Germany
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This field is not intended to be referenced by clients.
 */
public class DeprecatedDateAndTimeUtil {

    public static final SimpleDataCellToJavaConverterFactory<DateAndTimeValue, Calendar> toCalendarConverterFactory =
        new SimpleDataCellToJavaConverterFactory<>(DateAndTimeValue.class, Calendar.class,
            (val) -> val.getUTCCalendarClone(), "Calendar");

    public static final SimpleDataCellToJavaConverterFactory<DateAndTimeValue, Date> toDateConverterFactory =
        new SimpleDataCellToJavaConverterFactory<>(DateAndTimeValue.class, Date.class,
            (val) -> new Date(val.getUTCTimeInMillis()), "Date");

    public static final SimpleJavaToDataCellConverterFactory<Calendar> calendarConverterFactory =
        new SimpleJavaToDataCellConverterFactory<>(Calendar.class, DateAndTimeCell.TYPE,
            (val) -> new DateAndTimeCell(val.getTimeInMillis(), calendarHasDate(val), calendarHasTime(val),
                calendarHasMillis(val)),
            "Calendar");

    public static final SimpleJavaToDataCellConverterFactory<Date> dateConverterFactory =
        new SimpleJavaToDataCellConverterFactory<>(Date.class, DateAndTimeCell.TYPE,
            (val) -> new DateAndTimeCell(val.getTime(), true, true, true), "Date");

    /**
     * Return true when given calendar has time in the sense of hasDate from {@link DateAndTimeCell}.
     */
    private static boolean calendarHasDate(final Calendar calendar) {
        // see Calendar for field documentation
        return calendarIsSet(calendar, 0, 8);
    }

    /**
     * Return true when given calendar has date in the sense of hasTime from {@link DateAndTimeCell}.
     */
    private static boolean calendarHasTime(final Calendar calendar) {
        // see Calendar for field documentation
        return calendarIsSet(calendar, 9, 13);
    }

    /**
     * Return true when given calendar has milliseconds in the sense of hasMillis from {@link DateAndTimeCell}.
     */
    private static boolean calendarHasMillis(final Calendar calendar) {
        // see Calendar for field documentation
        return calendarIsSet(calendar, 14, 14);
    }

    /** Test if the fields from min to max are set on the calendar. */
    private static boolean calendarIsSet(final Calendar calendar, final int min, final int max) {
        for (int i = min; i <= max; i++) {
            if (calendar.isSet(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get one of the hidden/no registered deprecated converter factories provided by this utility.
     *
     * @param source Source type
     * @param dest Destination type
     * @return an optional converter factory
     */
    public static Optional<DataCellToJavaConverterFactory<?, ?>>
        getConverterFactory(final Class<? extends DataValue> source, final Class<?> dest) {
        if (DateAndTimeValue.class.isAssignableFrom(source)) {
            if (dest.isAssignableFrom(Date.class)) {
                return Optional.of(toDateConverterFactory);
            } else if (dest.isAssignableFrom(Calendar.class)) {
                return Optional.of(toCalendarConverterFactory);
            }
        } else if (dest.isArray() && CollectionDataValue.class.isAssignableFrom(source)) {
            if (Date.class.isAssignableFrom(dest.getComponentType())) {
                return Optional.of(DataCellToJavaConverterRegistry.getInstance().getCollectionConverterFactory(toDateConverterFactory));
            } else if (Calendar.class.isAssignableFrom(dest.getComponentType())) {
                return Optional.of(DataCellToJavaConverterRegistry.getInstance().getCollectionConverterFactory(toCalendarConverterFactory));
            }
        }

        return Optional.empty();
    }

    /**
     * Get one of the hidden/no registered deprecated converter factories provided by this utility.
     *
     * @param source Source type
     * @param dest Destination type
     * @return an optional converter factory
     */
    public static Optional<JavaToDataCellConverterFactory<?>> getConverterFactory(final Class<?> source,
        final DataType dest) {
        if (dest == DateAndTimeCell.TYPE) {
            if (Date.class.isAssignableFrom(source)) {
                return Optional.of(dateConverterFactory);
            } else if (Calendar.class.isAssignableFrom(source)) {
                return Optional.of(calendarConverterFactory);
            }
        } else if (dest == ListCell.getCollectionType(DateAndTimeCell.TYPE) && source.isArray()) {
            if (Date.class.isAssignableFrom(source.getComponentType())) {
                return Optional.of(JavaToDataCellConverterRegistry.getInstance().getArrayConverterFactory(dateConverterFactory));
            } else if (Calendar.class.isAssignableFrom(source.getComponentType())) {
                return Optional.of(JavaToDataCellConverterRegistry.getInstance().getArrayConverterFactory(calendarConverterFactory));
            }
        }

        return Optional.empty();
    }

    /**
     * Get one of the hidden/no registered deprecated converter factories provided by this utility using its identifier.
     *
     * @param id
     * @return an optional converter factory.
     */
    public static Optional<DataCellToJavaConverterFactory<?, ?>> getDataCellToJavaConverterFactory(final String id) {
        if (id == null)  {
            return Optional.empty();
        }

        if (id.startsWith(CollectionConverterFactory.class.getName())) {
            final String elementConverterId = id.substring(CollectionConverterFactory.class.getName().length()+1, id.length()-1);
            final Optional<DataCellToJavaConverterFactory<?, ?>> elementFactory = getDataCellToJavaConverterFactory(elementConverterId);
            if (elementFactory.isPresent()) {
                return Optional.of(DataCellToJavaConverterRegistry.getInstance().getCollectionConverterFactory(elementFactory.get()));
            }
        } else if (id.equals(toDateConverterFactory.getIdentifier())) {
            return Optional.of(toDateConverterFactory);
        } else if (id.equals(toCalendarConverterFactory.getIdentifier())) {
            return Optional.of(toCalendarConverterFactory);
        }

        return Optional.empty();
    }

    /**
     * Get one of the hidden/no registered deprecated converter factories provided by this utility using its identifier.
     *
     * @param id
     * @return an optional converter factory.
     */
    public static Optional<JavaToDataCellConverterFactory<?>> getJavaToDataCellConverterFactory(final String id) {
        if (id == null)  {
            return Optional.empty();
        }

       if (id.startsWith(ArrayToCollectionConverterFactory.class.getName())) {
            final String elementConverterId = id.substring(ArrayToCollectionConverterFactory.class.getName().length()+1, id.length()-1);
            final Optional<JavaToDataCellConverterFactory<?>> elementFactory = getJavaToDataCellConverterFactory(elementConverterId);
            if (elementFactory.isPresent()) {
                return Optional.of(JavaToDataCellConverterRegistry.getInstance().getArrayConverterFactory(elementFactory.get()));
            }
        } else if (id.equals(calendarConverterFactory.getIdentifier())) {
            return Optional.of(calendarConverterFactory);
        } else if (id.equals(dateConverterFactory.getIdentifier())) {
            return Optional.of(dateConverterFactory);
        }

        return Optional.empty();
    }

    /**
     * @return All factories in this util
     */
    public static Collection<JavaToDataCellConverterFactory<?>> getAllJavaToDataCellConverterFactories() {
        return Arrays.asList(dateConverterFactory, calendarConverterFactory);
    }

}
