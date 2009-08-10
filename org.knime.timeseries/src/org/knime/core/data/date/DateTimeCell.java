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
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateTimeCell extends DataCell implements DateTimeValue {
    
    /** {@link DataType} of this cell. */
    public static final DataType TYPE = DataType.getType(DateTimeCell.class);
    
    private static final DataCellSerializer<DateTimeCell>SERIALIZER 
        = new DateTimeCellSerializer();

    /**
     * 
     * @return serializer for this cell
     * @see DataCellSerializer
     * @see DataCell
     */
    public static final DataCellSerializer<DateTimeCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final Calendar m_calendar;
    
    /**
     * 
     * @param calendar the underlying date and time representation
     */
    public DateTimeCell(final Calendar calendar) {
        m_calendar = calendar;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (!(dc instanceof DateTimeValue)) {
            return false;
        }
        DateTimeValue dv = (DateTimeValue)dc;
        return dv.getYear() == getYear()
            && dv.getMonth() == getMonth()
            && dv.getDay() == getDay()
            && dv.getHour() == getHour()
            && dv.getMinute() == getMinute()
            && dv.getSecond() == getSecond()
            && dv.getMilliSecond() == getMilliSecond();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new Integer(
                getYear() + getMonth() + getDay()
                + getHour() + getMinute() + getSecond() + getMilliSecond()
                ).hashCode();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getYear() + "-" + getMonth() + "-" + getDay() + " "
            + getHour() + ":" + getMinute() + ":" + getSecond() + "." 
            + getMilliSecond();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getDay() {
        return m_calendar.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getMonth() {
        return m_calendar.get(Calendar.MONTH);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getYear() {
        return m_calendar.get(Calendar.YEAR);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getHour() {
        return m_calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getMilliSecond() {
        return m_calendar.get(Calendar.MILLISECOND);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getMinute() {
        return m_calendar.get(Calendar.MINUTE);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int getSecond() {
        return m_calendar.get(Calendar.SECOND);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public long getTimeInMillis() {
        return m_calendar.getTimeInMillis();
    }

}
