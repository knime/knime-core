/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
