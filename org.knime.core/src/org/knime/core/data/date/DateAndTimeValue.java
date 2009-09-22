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

import org.knime.core.data.DataValue;

/**
 * Interface supporting the representation of time and date independent of the
 * user's time zone and location. Times in KNIME are always UTC times!
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public interface DateAndTimeValue extends DataValue {
    
    /** Utility implementation for timestamp values. */
    public static final DateAndTimeUtility UTILITY = new DateAndTimeUtility();
    
    /**
     * @return the year of this date
     * @see {@link Calendar#YEAR}
     */
    public int getYear();
    
    /**
     * 
     * @return the month of the year, **STARTING WITH 0** for the first month
     * @see Calendar#MONTH
     */
    public int getMonth();
    
    /**
     * 
     * @return the day of the month in the interval 1-31
     * @see Calendar#DAY_OF_MONTH
     */
    public int getDayOfMonth();
    
    /**
     * 
     * @return the hour of day represented in the interval 0-23
     * @see Calendar#HOUR_OF_DAY
     */
    public int getHourOfDay();
    
    /**
     * 
     * @return the minute in the interval 0-59
     * @see Calendar#MINUTE
     */
    public int getMinute();
    
    /**
     * 
     * @return the second in the interval 0-59
     * @see Calendar#SECOND
     */
    public int getSecond();
    
    /**
     * 
     * @return the milliseconds in the interval 0-999
     * @see Calendar#MILLISECOND
     */
    public int getMillis();
    
    /**
     * 
     * @return true if the date is available and it is legal to access the 
     * date fields (year, month, day)
     */
    public boolean hasDate();
    
    /**
     * 
     * @return true if the time is available and it is legal to access the time 
     * fields (hour, minute, second)
     */
    public boolean hasTime();
    
    /**
     * 
     * @return true if the milliseconds are available and it is legal to access
     * the milliseconds
     */
    public boolean hasMillis();

    /**
     * 
     * @return the milliseconds in UTC time
     * @see Calendar#getTimeInMillis()
     */
    public long getUTCTimeInMillis();
    
    
    /**
     * 
     * @return a clone of the underlying UTC calendar 
     */
    public Calendar getUTCCalendarClone();
    
    
}
