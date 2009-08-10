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
import org.knime.core.data.DataValue;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateCell extends DataCell implements DateValue {
    
    /** DataType for this cell. */
    public static final DataType TYPE = DataType.getType(DateCell.class);
    
    private static final DataCellSerializer<DateCell> SERIALIZER 
        = new DateCellSerializer();
    
    /**
     * 
     * @return date value class
     */
    public static Class<DateValue> getPreferredValueClass() {
        return DateValue.class;
    }
    
    /**
     * 
     * @return serializer for this cell
     * @see DataCell
     */
    public static final DataCellSerializer<DateCell> getCellSerializer() {
        return SERIALIZER;
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
        date.clear(Calendar.HOUR);
        date.clear(Calendar.HOUR_OF_DAY);
        date.clear(Calendar.MINUTE);
        date.clear(Calendar.SECOND);
        date.clear(Calendar.MILLISECOND);
        return date;
    }
    
    private final Calendar m_calendar;
    
    /**
     * 
     * @param calendar the representation of the date to be set. Only the fields
     * {@link Calendar#YEAR}, {@link Calendar#MONTH}, 
     * {@link Calendar#DAY_OF_MONTH} are relevant for this date.
     * 
     */
    public DateCell(final Calendar calendar) {
        m_calendar = calendar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (!(dc instanceof DataValue)) {
            return false;
        }
        DateValue dv = (DateValue)dc;
        return dv.getYear() == getYear() &&  dv.getMonth() == getMonth()
            && dv.getDay() == getDay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new Integer(
                m_calendar.get(Calendar.YEAR)
                + m_calendar.get(Calendar.MONTH)
                + m_calendar.get(Calendar.DAY_OF_MONTH)).hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getYear() + "-" + getMonth() + "-" + getDay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDay() {
        return m_calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMonth() {
        // increment the value since the months are zero-based 
        return m_calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getYear() {
        return m_calendar.get(Calendar.YEAR);
    }
    

}
