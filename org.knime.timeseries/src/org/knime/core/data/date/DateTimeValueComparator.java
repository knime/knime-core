/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   26.07.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import java.util.Calendar;

import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateTimeValueComparator extends DataValueComparator {
    
    /**
     * Different comparator types for the different date and time values.
     * 
     * @author Fabian Dill, KNIME.com, Zurich, Switzerland
     */
    public enum Type {
        /** Comparator type for {@link DateValue}s. */
        Date,
        /** Comparator type for {@link TimeValue}s. */
        Time,
        /** Comparator type for {@link DateTimeValue}s. */
        DateTime
    }

    private final Type m_type;
    
    /**
     * 
     * @param type type of comparator either for date, time or date time
     */
    public DateTimeValueComparator(final Type type) {
        m_type = type;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected int compareDataValues(final DataValue v1, final DataValue v2) {
        switch (m_type) {
        case Date:
            return compareDateValues((DateValue)v1, (DateValue)v2);
        case Time:
            return compareTimeValues((TimeValue)v1, (TimeValue)v2);
        case DateTime:
            return compareDateTimeValues((DateTimeValue)v1, 
                    (DateTimeValue)v2);
        default:
            // should never happen
            assert false;
            return 0;
        }
    }

    private int compareDateTimeValues(final DateTimeValue v1, 
            final DateTimeValue v2) {
        Calendar c1 = Calendar.getInstance();
        c1 = prepareCalendarFor(c1, (DateValue)v1);
        c1 = prepareCalendarFor(c1, (TimeValue)v1);
        Calendar c2 = Calendar.getInstance();
        c2 = prepareCalendarFor(c2, (DateValue)v2);
        c2 = prepareCalendarFor(c2, (TimeValue)v2);
        return c1.compareTo(c2);
    }
    
    private int compareDateValues(final DateValue v1, final DateValue v2) {
        Calendar c1 = Calendar.getInstance();
        c1 = prepareCalendarFor(c1, v1);
        // prepare calendar 2
        Calendar c2 = Calendar.getInstance();
        c2 = prepareCalendarFor(c2, v2);
        return c1.compareTo(c2);
    }
    
    private int compareTimeValues(final TimeValue v1, final TimeValue v2) {
        Calendar c1 = Calendar.getInstance();
        c1 = prepareCalendarFor(c1, v1);
        // prepare calendar 2
        Calendar c2 = Calendar.getInstance();
        c2 = prepareCalendarFor(c2, v2);
        return c1.compareTo(c2);        
    }
    
    private Calendar prepareCalendarFor(final Calendar c, final DateValue v) {
        c.set(Calendar.YEAR, v.getYear());
        c.set(Calendar.MONTH, v.getMonth());
        c.set(Calendar.DAY_OF_MONTH, v.getDay());
        return c;
    }
    
    private Calendar prepareCalendarFor(final Calendar c, final TimeValue v) {
        c.set(Calendar.HOUR, v.getHour());
        c.set(Calendar.MINUTE, v.getMinute());
        c.set(Calendar.SECOND, v.getSecond());
        c.set(Calendar.MILLISECOND, v.getMilliSecond());
        return c;
    }

}
