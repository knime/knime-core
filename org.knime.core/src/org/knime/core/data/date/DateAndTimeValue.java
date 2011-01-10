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
     * @see Calendar#YEAR
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
