/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package de.unikn.knime.core.data.def;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataCellSerializer;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValue;
import de.unikn.knime.core.data.StringValue;

/**
 * A data cell implementation holding a string value by storing this value in a
 * private <code>String</code> member.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class StringCell extends DataCell implements StringValue {

    /** Convenience access member for 
     * <code>DataType.getType(StringCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(StringCell.class);
    
    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return StringValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return StringValue.class;
    }
    
    private static final StringSerializer SERIALIZER = 
        new StringSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final StringSerializer getCellSerializer() {
        return SERIALIZER; 
    }

    private final String m_string;

    /**
     * Creates a new String Cell based on the given String value.
     * 
     * <p><b>Note</b>: The serializing technique writes the given String to a
     * <code>DataOutput</code> using the <code>writeUTF(String)</code> method.
     * The implementation is limited to string lengths of at most 64kB - (in 
     * UTF format - which may be not equal to <code>str.length()</code>).
     * 
     * @param str The String value to store.
     * @throws NullPointerException If the given String value is
     *             <code>null</code>.
     */
    public StringCell(final String str) {
        if (str == null) {
            throw new NullPointerException("String value can't be null.");
        }
        m_string = str;
    }

    /**
     * @see de.unikn.knime.core.data.StringValue#getStringValue()
     */
    public String getStringValue() {
        return m_string;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getStringValue();
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    protected boolean equalsDataCell(final DataCell dc) {
        return m_string.equals(((StringCell)dc).m_string);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_string.hashCode();
    }

    /** Factory for (de-)serializing a StringCell. */
    private static class StringSerializer implements 
        DataCellSerializer<StringCell> {
        /**
         * @see DataCellSerializer#serialize(DataCell, DataOutput)
         */
        public void serialize(final StringCell cell, 
                final DataOutput output) throws IOException {
            output.writeUTF(cell.getStringValue());
        }
        
        /**
         * @see DataCellSerializer#deserialize(DataInput)
         */
        public StringCell deserialize(final DataInput input) 
            throws IOException {
            String s = input.readUTF();
            return new StringCell(s);
        }


    }

}
