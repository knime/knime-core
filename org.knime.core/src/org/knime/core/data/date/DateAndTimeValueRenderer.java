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
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import java.text.SimpleDateFormat;

import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * Renders the a {@link DateAndTimeValue}.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateAndTimeValueRenderer extends DefaultDataValueRenderer {
    
    private static final String DATE = "dd.MMM.yyyy";
    private static final String TIME = "HH:mm:ss";
    private static final String ISO_DATE = "yyyy-MM-dd";
    private static final String US_DATE = "MM/dd/yyyy";
    private static final String US_TIME = "hh:mm:ss";
    private static final String MILLIS = ".SSS";
    
    /** MM/dd/yyyy hh:mm:ss a. */
    static final SimpleDateFormat US_DATE_TIME_FORMAT 
        = new SimpleDateFormat(US_DATE + " " + US_TIME + " a");
    /** MM/dd/yyyy hh:mm:ss.SSS a. */
    static final SimpleDateFormat US_DATE_TIME_MILLIS_FORMAT 
        = new SimpleDateFormat(US_DATE + " " + US_TIME + MILLIS + " a");
    /** MM/dd/yyyy. */
    static final SimpleDateFormat US_DATE_FORMAT 
        = new SimpleDateFormat(US_DATE);
    /** hh:mm:ss a. */
    static final SimpleDateFormat US_TIME_FORMAT 
        = new SimpleDateFormat(US_TIME + " a");
    /** hh:mm:ss.SSS a. */
    static final SimpleDateFormat US_TIME_MILLIS_FORMAT 
        = new SimpleDateFormat(US_TIME + MILLIS + " a");
    /** dd.MMM.yyyy HH:mm:ss.*/
    static final SimpleDateFormat DATE_TIME_FORMAT 
        = new SimpleDateFormat(DATE + " " + TIME);
    /** dd.MMM.yyyy HH:mm:ss.SSS.*/
    static final SimpleDateFormat DATE_TIME_MILLIS_FORMAT 
        = new SimpleDateFormat(DATE + " " + TIME + MILLIS);
    /** dd.MMM.yyyy.*/
    static final SimpleDateFormat DATE_FORMAT 
        = new SimpleDateFormat(DATE);
    /** HH:mm:ss.*/
    static final SimpleDateFormat TIME_FORMAT 
        = new SimpleDateFormat(TIME);
    /** HH:mm:ss.SSS.*/
    static final SimpleDateFormat TIME_MILLIS_FORMAT 
        = new SimpleDateFormat(TIME + MILLIS);
    /** yyyy-MM-dd'T'HH:mm:ss. */
    static final SimpleDateFormat ISO8601_DATE_TIME_FORMAT
        = new SimpleDateFormat(ISO_DATE + "'T'" + TIME);
    /** yyyy-MM-dd'T'HH:mm:ss.SSS. */
    static final SimpleDateFormat ISO8601_DATE_TIME_MILLIS_FORMAT 
        = new SimpleDateFormat(ISO_DATE + "'T'" + TIME + MILLIS);
    /** yyyy-MM-dd. */
    static final SimpleDateFormat ISO8601_DATE_FORMAT 
        = new SimpleDateFormat(ISO_DATE);
    /** HH:mm:ss. */
    static final SimpleDateFormat ISO8601_TIME_FORMAT 
        = new SimpleDateFormat(TIME);
    /** yyyy-MM-dd'T'HH:mm:ss.SSS. */
    static final SimpleDateFormat ISO8601_TIME_MILLIS_FORMAT 
        = new SimpleDateFormat(TIME + MILLIS);    
    
    /**
     * Renders the datetime as yyyy/dd/MM hh:mm:ss.SSS.
     */
    public static final DateAndTimeValueRenderer US 
        = new DateAndTimeValueRenderer(US_DATE_TIME_MILLIS_FORMAT,
                US_DATE_TIME_FORMAT, US_DATE_FORMAT, US_TIME_FORMAT,
                US_TIME_MILLIS_FORMAT) {
        @Override
        public String getDescription() {
            return "US: MM/dd/yyyy hh:mm:ss.SSS";
        };
    };

    /**
     * Renders the datetime as dd.MMM.yyyy hh:mm:ss.SSS.
     */
    public static final DateAndTimeValueRenderer DEFAULT 
        = new DateAndTimeValueRenderer(DATE_TIME_MILLIS_FORMAT, 
                DATE_TIME_FORMAT, DATE_FORMAT, TIME_FORMAT, 
                TIME_MILLIS_FORMAT) {
        @Override
        public String getDescription() {
            return "dd.MMM.yyyy hh:mm:ss.SSS";
        };
    };
    
    /**
     * Renders the datetime as yyyy-MM-ddTHH:mm:ss.SSS.
     */
    public static final DateAndTimeValueRenderer ISO8061 
        = new DateAndTimeValueRenderer(ISO8601_DATE_TIME_MILLIS_FORMAT,
                ISO8601_DATE_TIME_FORMAT, ISO8601_DATE_FORMAT,
                ISO8601_TIME_FORMAT, ISO8601_TIME_MILLIS_FORMAT) {
        @Override
        public String getDescription() {
            return "ISO8601: yyyy-MM-ddTHH:mm:ss.SSS";
        };
    };
    

    private final SimpleDateFormat m_dateTimeMillis;
    private final SimpleDateFormat m_dateTime;
    private final SimpleDateFormat m_date;
    private final SimpleDateFormat m_time;
    private final SimpleDateFormat m_timeMillis;
    
    /**
     * 
     * @param dateTimeMillis format if all values are set
     * @param dateTime format for date and time but no millis
     * @param date only date, no time, no millis
     * @param time only time, no date, no millis
     * @param timeMillis time, millis but no date
     */
    public DateAndTimeValueRenderer(final SimpleDateFormat dateTimeMillis,
            final SimpleDateFormat dateTime, final SimpleDateFormat date,
            final SimpleDateFormat time, final SimpleDateFormat timeMillis) {
        m_dateTimeMillis = dateTimeMillis;
        // necessary to set the time zone of the formatter to UTC
        // otherwise the string is converted to the local time zone
        m_dateTimeMillis.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);
        m_dateTime = dateTime;
        m_dateTime.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);
        m_date = date;
        m_date.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);
        m_time = time;
        m_time.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);
        m_timeMillis = timeMillis;
        m_timeMillis.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof DateAndTimeValue) {
            super.setValue(getStringRepresentationFor((DateAndTimeValue)value));
        } else {
            super.setValue(value);
        }
    }
    
    /**
     * 
     * @param value the date and time value to render
     * @return a string representation of the date and time value
     */
    protected String getStringRepresentationFor(final DateAndTimeValue value) {
        SimpleDateFormat sdf = null;
        if (value.hasDate()) {
            if (value.hasTime()) {
                if (value.hasMillis()) {
                    sdf = m_dateTimeMillis;
                } else {
                    sdf = m_dateTime;
                }
            } else {
                sdf = m_date;
            }
        } else {
            if (value.hasTime()) {
                if (value.hasMillis()) {
                    sdf = m_timeMillis;
                } else {
                    sdf = m_time;
                }
            } else {
                return "n/a";
            }
        }
        assert sdf != null;
        return sdf.format(value.getUTCCalendarClone().getTime());
    }

}
