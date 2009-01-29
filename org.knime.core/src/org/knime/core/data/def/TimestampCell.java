/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.core.data.def;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.TimestampValue;

/**
 * Implementation of a <code>DataCell</code> holding day/time
 * information as {@link Date}.
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
    
    /** Internal representation as {@link java.lang.Date}. */
    private final Date m_date;

    
    /**
     * Creates a new <code>TimestampCell</code> based on the given value.
     *
     * @param d date to be stored.
     */
    public TimestampCell(final Date d) {
        m_date = d;
    }

    /** 
     * Parse string and create new date object. Use predefined format as
     * defined as <code>yyyy-MM-dd;HH:mm:ss.S</code>.
     * @param s the input string date
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
        this(df.parse(s));
    }
    
    /** {@inheritDoc} */
    @Override
    public Date getDate() {
        return m_date;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_date.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return m_date.equals(((TimestampValue)dc).getDate());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_date.hashCode();
    }
    
    private static final TimestampCellSerializer SERIALIZER = 
        new TimestampCellSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final TimestampCellSerializer getCellSerializer() {
        return SERIALIZER;
    }
    
    /** Factory for (de-)serializing a TimestampCell. */
    private static class TimestampCellSerializer 
            implements DataCellSerializer<TimestampCell> {
        /** {@inheritDoc} */
        @Override
        public void serialize(final TimestampCell cell, 
                final DataCellDataOutput output) throws IOException {
            output.writeLong(cell.getDate().getTime());

        }
        /** {@inheritDoc} */
        @Override
        public TimestampCell deserialize(
                final DataCellDataInput input) throws IOException {
            return new TimestampCell(new Date(input.readLong()));
        }
    }

}
