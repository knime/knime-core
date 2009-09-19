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

import java.util.Calendar;
import java.util.TimeZone;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 * Cell storing a time and/or date. Time is represented by a {@link Calendar} 
 * set to UTC time. No support for time zones. A {@link TimestampCell} can be 
 * created by passing only the date fields (year, month, day), only the time 
 * fields (hour, minute, seconds, and milliseconds).
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimestampCell extends DataCell 
    implements TimestampValue, BoundedValue {
    
    /** The UTC time zone used to represent the time. */
    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            TimestampCell.class);
    
    /**
     * 
     * @param time the calendar containing time fields and for which the date 
     *  fields should be reset
     * @return a calendar with the same time fields but with clear date fields 
     *  {@link Calendar#YEAR}, {@link Calendar#MONTH}, 
     *  {@link Calendar#DAY_OF_MONTH}
     */
    public static Calendar resetDateFields(final Calendar time) {
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        int millis = time.get(Calendar.MILLISECOND);
        // do it the hard way, since resetting single fields does not work
        time.clear();
        time.setTimeZone(UTC_TIMEZONE);
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, second);
        time.set(Calendar.MILLISECOND, millis);
        return time;
    }
    
    /**
     * 
     * @param date the calendar containing date fields and for which the time 
     *  fields should be reset
     * @return a calendar with the same date fields but with clear time fields 
     *  {@link Calendar#HOUR}, {@link Calendar#HOUR_OF_DAY}, 
     *  {@link Calendar#MINUTE}, {@link Calendar#SECOND}, 
     *  {@link Calendar#MILLISECOND}
     */
    public static Calendar resetTimeFields(final Calendar date) {
        // before resetting the time fields we have to set it to 12 o clock
        // otherwise we might get a date switch...
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int dayOfMonth = date.get(Calendar.DAY_OF_MONTH);
        date.clear();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return date;
    }
    
    
    /** {@link DataType} of this cell. */
    public static final DataType TYPE = DataType.getType(TimestampCell.class);
    
    private static final DataCellSerializer<TimestampCell>SERIALIZER 
            = new TimestampCellSerializer();
    
    /**
     * 
     * @return serializer for this cell
     * @see DataCellSerializer
     * @see DataCell
     */
    public static final DataCellSerializer<TimestampCell> getCellSerializer() {
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
    public TimestampCell(final int year, final int month, 
            final int dayOfMonth) {
        m_utcCalendar = getUTCCalendar();
        m_utcCalendar.set(year, month, dayOfMonth);
        m_hasDate = true;
        m_hasTime = false;
        m_hasMillis = false;
        resetTimeFields(m_utcCalendar);
    }
    
    /**
     * A timestamp without date.
     * 
     * @param hourOfDay the hour of the day
     * @param minute the minute
     * @param second the second
     * @param milliseconds the milliseconds (or -1 if they should not be set) 
     */
    public TimestampCell(final int hourOfDay, final int minute, 
            final int second, final int milliseconds) {
        m_utcCalendar = getUTCCalendar();
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
        resetDateFields(m_utcCalendar);
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
    public TimestampCell(final int year, final int month, final int dayOfMonth,
            final int hourOfDay, final int minute, final int second) {
        m_utcCalendar = getUTCCalendar();
        m_utcCalendar.set(year, month, dayOfMonth, hourOfDay, minute, second);
        m_hasDate = true;
        m_hasTime = true;        
        m_hasMillis = false;            
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
     */
    public TimestampCell(final int year, final int month, final int dayOfMonth,
            final int hourOfDay, final int minute, final int second, 
            final int millisecond) {
        m_utcCalendar = getUTCCalendar();
        m_utcCalendar.set(year, month, dayOfMonth, hourOfDay, minute, second);
        m_utcCalendar.set(Calendar.MILLISECOND, millisecond);
        m_hasDate = true;
        m_hasTime = true;
        m_hasMillis = true;   
    }
    
    /**
     * For internal use only!
     * 
     * @param utcTime milliseconds in UTC time 
     * @param hasDate true if the date (year, month ,day) is available
     * @param hasTime true if the time is available (hour, minute, second)
     * @param hasMillis true if milliseconds are available 
     */
    public TimestampCell(final long utcTime, final boolean hasDate, 
            final boolean hasTime, final boolean hasMillis) {
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
        // the month fields is stored in [0,11] -> we convert it to [1,12]
        return m_utcCalendar.get(Calendar.MONTH) + 1;
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
        checkTimeFieldAccess("Milliseconds");
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
     * @return a calendar where the time zone is set to UTC (as it us used in 
     * the timestamp cell)
     */
    public static final Calendar getUTCCalendar() { 
        Calendar cal = Calendar.getInstance(UTC_TIMEZONE);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUTCTime() {
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
    public boolean hasMillis() {
        return m_hasMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasTime() {
        return m_hasTime;
    }
    
    // ***************************************************************
    // **************         DataCell      **************************
    // ***************************************************************
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (dc != null && dc.getType().isCompatible(TimestampValue.class)) {
            return ((TimestampValue)dc).getUTCTime() 
                == m_utcCalendar.getTimeInMillis();
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
        // how to generate a hashCode for longs 
        return (int)(m_utcCalendar.getTimeInMillis() 
                ^ (m_utcCalendar.getTimeInMillis() >>> 32));
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "";
        if (m_hasDate) {
         result = TimeRenderUtil.getStringForDateField(getYear()) 
             + "-" + TimeRenderUtil.getStringForDateField(getMonth()) 
             + "-" + TimeRenderUtil.getStringForDateField(getDayOfMonth()) 
             + " ";
        } 
        if (m_hasTime) {
            result += TimeRenderUtil.getStringForDateField(getHourOfDay())
                + ":" + TimeRenderUtil.getStringForDateField(getMinute())
                + ":" + TimeRenderUtil.getStringForDateField(getSecond());
            if (m_hasMillis) {
                result += "." + getMillis();
            }
        }
        return result;
    }

}
