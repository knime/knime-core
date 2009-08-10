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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimeCell extends DataCell implements TimeValue {
    
    /** {@link DataType} of this cell. */
    public static final DataType TYPE = DataType.getType(TimeCell.class);
    
    private static final DataCellSerializer<TimeCell> SERIALIZER 
        = new TimeCellSerializer();
    
    /**
     * 
     * @return serializer for this field
     * @see DataCell
     */
    public static DataCellSerializer<TimeCell>getCellSerializer() {
        return SERIALIZER;
    }
    
    
    /**
     * 
     * @param time the calendar containing time fields and for which the date 
     *  fields should be reset
     * @return a calendar with the same time fields but with clear date fields 
     *  {@link Calendar#YEAR}, {@link Calendar#MONTH}, 
     *  {@link Calendar#DAY_OF_MONTH}
     */
    public static Calendar resetDateFields(final Calendar time) {
        time.clear(Calendar.YEAR);
        time.clear(Calendar.MONTH);
        time.clear(Calendar.DAY_OF_MONTH);
        return time;
    }
    
    private final Calendar m_calendar;

    /**
     * 
     * @param calendar time representation, only the fields 
     *  {@link Calendar#HOUR_OF_DAY}, {@link Calendar#MINUTE}, 
     *  {@link Calendar#SECOND}, and {@link Calendar#MILLISECOND} are taken 
     *  into account
     */
    public TimeCell(final Calendar calendar) {
        m_calendar = calendar;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (!(dc instanceof TimeValue)) {
            return false;
        }
        TimeValue tv = (TimeValue)dc;
        return tv.getHour() == getHour()
            && tv.getMinute() == getMinute()
            && tv.getSecond() == getSecond()
            && tv.getMilliSecond() == getMilliSecond();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new Integer(getHour() + getMinute() + getSecond() 
                + getMilliSecond()).hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getHour() + ":" + getMinute() + ":" + getSecond() + "." 
            + getMilliSecond();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHour() {
        return m_calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMilliSecond() {
        return m_calendar.get(Calendar.MILLISECOND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinute() {
        return m_calendar.get(Calendar.MINUTE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSecond() {
        return m_calendar.get(Calendar.SECOND);
    }

}
