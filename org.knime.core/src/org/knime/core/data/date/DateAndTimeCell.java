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
 *   13.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 * Cell storing a time and/or date. Time is represented by a {@link Calendar} 
 * set to UTC time. No support for time zones. A {@link DateAndTimeCell} can be 
 * created by passing only the date fields (year, month, day), only the time 
 * fields (hour, minute, seconds, and milliseconds).
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateAndTimeCell extends DataCell 
    implements DateAndTimeValue, BoundedValue {
    
    /** The UTC time zone used to represent the time. */
    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            DateAndTimeCell.class);
    
    /**
     * 
     * @param cal the calendar containing time fields and for which the date 
     *  fields should be reset
     *  {@link Calendar#YEAR}, {@link Calendar#MONTH}, 
     *  {@link Calendar#DAY_OF_MONTH}
     */
    public static void resetDateFields(final Calendar cal) {
        // do it the hard way, since resetting single fields does not work
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int millis = cal.get(Calendar.MILLISECOND);
        cal.clear();
        cal.setTimeZone(UTC_TIMEZONE);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millis);
    }
    
    /**
     * 
     * @param cal the calendar containing date fields and for which the time 
     *  fields should be reset
     *  {@link Calendar#HOUR}, {@link Calendar#HOUR_OF_DAY}, 
     *  {@link Calendar#MINUTE}, {@link Calendar#SECOND}, 
     *  {@link Calendar#MILLISECOND}
     */
    public static void resetTimeFields(final Calendar cal) {
        // do it the hard way, since resetting single fields does not work
        // before resetting the time fields we have to set it to 12 o'clock
        // otherwise we might get a date switch...
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }
    
    
    /** {@link DataType} of this cell. */
    public static final DataType TYPE = DataType.getType(DateAndTimeCell.class);
    
    private static final DataCellSerializer<DateAndTimeCell>SERIALIZER 
            = new DateAndTimeCellSerializer();
    
    /**
     * 
     * @return serializer for this cell
     * @see DataCellSerializer
     * @see DataCell
     */
    public static final DataCellSerializer<DateAndTimeCell> getCellSerializer() {
        return SERIALIZER;
    }
    
    private final Calendar m_utcCalendar;
    
    private final boolean m_hasDate;
    
    private final boolean m_hasTime;
    
    private final boolean m_hasMillis;
    
    // ***************************************************************
    // **************      Constructors     **************************
    // ***************************************************************
    
    /**
     * A date timestamp without time.
     * 
     * @param year the year
     * @param month the month (1-12)
     * @param dayOfMonth the day of the month (1-31)
     */
    public DateAndTimeCell(final int year, final int month, 
            final int dayOfMonth) {
        m_utcCalendar = getUTCCalendar();
        m_utcCalendar.clear();
        m_utcCalendar.set(year, month, dayOfMonth);
        m_hasDate = true;
        m_hasTime = false;
        m_hasMillis = false;
    }
    
    /**
     * A timestamp without date.
     * 
     * @param hourOfDay the hour of the day
     * @param minute the minute
     * @param second the second
     * @param milliseconds the milliseconds (or <0 if they should not be set) 
     */
    public DateAndTimeCell(final int hourOfDay, final int minute, 
            final int second, final int milliseconds) {
        m_utcCalendar = getUTCCalendar();
        m_utcCalendar.clear();
        m_utcCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        m_utcCalendar.set(Calendar.MINUTE, minute);
        m_utcCalendar.set(Calendar.SECOND, second);
        if (milliseconds < 0) {
            m_hasMillis = false;            
        } else {
            m_hasMillis = true;
            m_utcCalendar.set(Calendar.MILLISECOND, milliseconds);
        }
        m_hasDate = false;
        m_hasTime = true;
    }
   
    /**
     * 
     * @param year the year {@link Calendar#YEAR}
     * @param month the month {@link Calendar#MONTH}
     * @param dayOfMonth day of month {@link Calendar#DAY_OF_MONTH}
     * @param hourOfDay hour of day {@link Calendar#HOUR_OF_DAY}
     * @param minute minute {@link Calendar#MINUTE}
     * @param second second {@link Calendar#SECOND}
     */
    public DateAndTimeCell(final int year, final int month, final int dayOfMonth,
            final int hourOfDay, final int minute, final int second) {
        this(year, month, dayOfMonth, hourOfDay, minute, second, -1);
    }

    /**
     * 
     * @param year the year {@link Calendar#YEAR}
     * @param month the month {@link Calendar#MONTH}
     * @param dayOfMonth day of month {@link Calendar#DAY_OF_MONTH}
     * @param hourOfDay hour of day {@link Calendar#HOUR_OF_DAY}
     * @param minute minute {@link Calendar#MINUTE}
     * @param second second {@link Calendar#SECOND}
     * @param millisecond milliseconds {@link Calendar#MILLISECOND}
     *    (or <0 if they should not be set)
     */
    public DateAndTimeCell(final int year, final int month, final int dayOfMonth,
            final int hourOfDay, final int minute, final int second, 
            final int millisecond) {
        m_utcCalendar = getUTCCalendar();
        m_utcCalendar.clear();
        m_utcCalendar.set(year, month, dayOfMonth, hourOfDay, minute, second);
        m_hasDate = true;
        m_hasTime = true;
        if (millisecond >= 0) {
            m_utcCalendar.set(Calendar.MILLISECOND, millisecond);
            m_hasMillis = true;   
        } else {
            m_hasMillis = false;
        }
    }
    
    /**
     * For internal use only!
     * 
     * @param utcTime milliseconds in UTC time 
     * @param hasDate true if the date (year, month ,day) is available
     * @param hasTime true if the time is available (hour, minute, second)
     * @param hasMillis true if milliseconds are available 
     */
    public DateAndTimeCell(final long utcTime, final boolean hasDate, 
            final boolean hasTime, final boolean hasMillis) {
        if (hasMillis & !hasTime) {
            throw new IllegalArgumentException("Timestamp with Millis but "
                    + " without time not allowed!");
        }
        m_utcCalendar = getUTCCalendar();
        m_utcCalendar.setTimeInMillis(utcTime);
        m_hasDate = hasDate;
        m_hasTime = hasTime;
        m_hasMillis = hasMillis;
        if (!m_hasDate) {
            resetDateFields(m_utcCalendar);
        }
        if (!m_hasTime) {
            resetTimeFields(m_utcCalendar);
        }
    }
    
    // ***************************************************************
    // **************      Date Fields     ***************************
    // ***************************************************************
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getYear() {
        if (!checkDateFieldAccess("Year")) {
            return -1;
        }
        return m_utcCalendar.get(Calendar.YEAR);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getMonth() {
        if (!checkDateFieldAccess("Month")) {
            return -1;
        }
        return m_utcCalendar.get(Calendar.MONTH);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getDayOfMonth() {
        if (!checkDateFieldAccess("Day of month")) {
            return -1;
        }
        return m_utcCalendar.get(Calendar.DAY_OF_MONTH);
    }
    
    // ***************************************************************
    // **************      Time Fields     ***************************
    // ***************************************************************
    

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHourOfDay() {
        if (!checkTimeFieldAccess("Hour")) {
            return -1;
        }
        return m_utcCalendar.get(Calendar.HOUR_OF_DAY);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinute() {
        if (!checkTimeFieldAccess("Minute")) {
            return -1;
        }
        return m_utcCalendar.get(Calendar.MINUTE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSecond() {
        if (!checkTimeFieldAccess("Second")) {
            return -1;
        }
        return m_utcCalendar.get(Calendar.SECOND);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getMillis() {
        if (!m_hasMillis) {
            LOGGER.warn(
                    "Milliseconds are not supported by this instance!");
            return 0;
        }
        return m_utcCalendar.get(Calendar.MILLISECOND);
    }

    
    // ***************************************************************
    // **************         Utility      ***************************
    // ***************************************************************
    
    /**
     * @return an emptycalendar where the time zone is set to UTC (as it
     * us used in the timestamp cell) and all fields are empty.
     */
    public static final Calendar getUTCCalendar() { 
        Calendar cal = Calendar.getInstance(UTC_TIMEZONE);
        cal.clear();
        return cal;
    }
    
    private boolean checkDateFieldAccess(final String fieldName) {
        if (!m_hasDate) {
            LOGGER.warn(fieldName
                    + " is a date field, which is not supported by " 
                    + "this instance!");
            return false;
        }
        return true;
    }
    
    private boolean checkTimeFieldAccess(final String fieldName) {
        if (!m_hasTime) {
            LOGGER.warn(fieldName
                    + " is a time field, which is not supported by " 
                    + "this instance!");
            return false;
        }
        return true;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Calendar getUTCCalendarClone() {
        Calendar newCal = getUTCCalendar();
        newCal.setTimeInMillis(m_utcCalendar.getTimeInMillis());
        return newCal;
    }

    /** Convenience method to access member UTC Calendar (for faster
     * comparisons/equal tests).
     * 
     * @return underlying UTC calendar
     */
    Calendar getInternalUTCCalendarMember() {
        return m_utcCalendar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUTCTimeInMillis() {
        return m_utcCalendar.getTimeInMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDate() {
        return m_hasDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasTime() {
        return m_hasTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasMillis() {
        return m_hasMillis;
    }

    // ***************************************************************
    // **************         DataCell      **************************
    // ***************************************************************
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (dc != null && dc.getType().isCompatible(DateAndTimeValue.class)) {
            DateAndTimeValue tv = (DateAndTimeValue)dc;
            boolean isEqual = (tv.getUTCTimeInMillis() 
                == m_utcCalendar.getTimeInMillis());
            isEqual &= (this.m_hasDate == tv.hasDate());
            isEqual &= (this.m_hasTime == tv.hasTime());
            isEqual &= (this.m_hasMillis == tv.hasMillis());
            return isEqual;
        }
        return false;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // based on the recommendations of Joshua Bloch 
        // how to generate a hashCode for longs. 
        // Note: ignores flags!
        return (int)(m_utcCalendar.getTimeInMillis() 
                ^ (m_utcCalendar.getTimeInMillis() >>> 32));
    }

    /** a date formatter. */
    static final SimpleDateFormat DATEONLYFORMAT =
        new SimpleDateFormat("dd.MMM.yyyy");
    /** a date formatter. */
    static final SimpleDateFormat TIMEONLYFORMAT =
        new SimpleDateFormat("hh:mm:ss");
    /** a date formatter. */
    static final SimpleDateFormat TIMEANDMILLISONLYFORMAT =
        new SimpleDateFormat("hh:mm:ss.SSS");
    /** a date formatter. */
    static final SimpleDateFormat DATEANDTIMEFORMAT =
        new SimpleDateFormat("dd.MMM.yyyy hh:mm:ss");
    /** a date formatter. */
    static final SimpleDateFormat DATEANDTIMEANDMILLISFORMAT =
        new SimpleDateFormat("dd.MMM.yyyy hh:mm:ss.SSS");

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        SimpleDateFormat sdf = null;
        if (hasDate()) {
            if (hasTime()) {
                if (hasMillis()) {
                    sdf = DATEANDTIMEANDMILLISFORMAT;
                } else {
                    sdf = DATEANDTIMEFORMAT;
                }
            } else {
                sdf = DATEONLYFORMAT;
            }
        } else {
            if (hasTime()) {
                if (hasMillis()) {
                    sdf = TIMEANDMILLISONLYFORMAT;
                } else {
                    sdf = TIMEONLYFORMAT;
                }
            } else {
                return "n/a";
            }
        }
        assert sdf != null;
        return sdf.format(m_utcCalendar.getTime());
    }

}
