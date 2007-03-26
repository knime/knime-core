/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   12.01.2007 (mb): created
 */
package org.knime.timeseries.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

/**
 * Implementation of a <code>DataCell</code> holding day/time
 * information.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class TimestampCell extends DataCell implements TimestampValue {

    /** Convenience access member for 
     * <code>DataType.getType(TimestampCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(TimestampCell.class);
    
    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return TimestampValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return TimestampValue.class;
    }
    
    /*
     * local members holding date/time information
     */
    private Date m_date;
    private boolean m_intraDay = false;
    
    /**
     * Creates a new Timestamp Cell based on the given value.
     *
     * @param d date to be stored.
     */
    public TimestampCell(final Date d) {
        m_date = d;
    }

    /** Parse string and create new date object. Use predefined format
     * 
     * @param s the input string
     * @throws ParseException if parsing failed
     */
    public TimestampCell(final String s) throws ParseException {
        // define format
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd;HH:mm:ss.S");
        // parse string
        m_date = df.parse(s);
    }
    
    /** Parse string and create new date object. Format is also provided.
     * 
     * @param s the input string
     * @param df the format specification for the date in s
     * @throws ParseException if parsing failed
     */
    public TimestampCell(final String s, final SimpleDateFormat df)
    throws ParseException {
        m_date = new Date();
        // parse string
        m_date = df.parse(s);
    }

    /** Getter methods for internals.
     * 
     * @return date.
     */
    public Date getDate() {
        return m_date;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      SimpleDateFormat formatter;
      
      if (m_intraDay) {
                formatter = new SimpleDateFormat("hh:mm:ss");          
        } else {
        formatter = 
            new SimpleDateFormat("yyyy.MMM.dd");
        }
      
        String dateString = formatter.format(m_date);
 //       return m_date.toString();
        return dateString;
    }

    /**
     * @see org.knime.core.data.DataCell
     *      #equalsDataCell(org.knime.core.data.DataCell)
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return m_date.equals(((TimestampValue)dc).getDate());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return m_date.hashCode();
    }

}
