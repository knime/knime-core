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
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.FuzzyIntervalValue;
import de.unikn.knime.core.data.FuzzyNumberValue;
import de.unikn.knime.core.data.IntValue;

/**
 * A data cell implementation holding an integer value by storing this value in
 * a private <code>int</code> member. It provides an int value, a double
 * value, a fuzzy number value, as well as a fuzzy interval value.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class IntCell extends DataCell implements IntValue,
        DoubleValue, FuzzyNumberValue, FuzzyIntervalValue {

    /** Convenience access member for 
     * <code>DataType.getType(IntCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(IntCell.class);
    
    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return IntValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return IntValue.class;
    }
    
    private static final DataCellSerializer<IntCell> SERIALIZER = 
        new IntSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<IntCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final int m_int;

    /**
     * Creates new cell for a generic int value.
     * 
     * @param i The integer value to store.
     */
    public IntCell(final int i) {
        m_int = i;
    }

    /**
     * @see de.unikn.knime.core.data.IntValue#getIntValue()
     */
    public int getIntValue() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.DoubleValue#getDoubleValue()
     */
    public double getDoubleValue() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getCore()
     */
    public double getCore() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMaxSupport()
     */
    public double getMaxSupport() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMinSupport()
     */
    public double getMinSupport() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMaxCore()
     */
    public double getMaxCore() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMinCore()
     */
    public double getMinCore() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getCenterOfGravity()
     */
    public double getCenterOfGravity() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((IntCell)dc).m_int == m_int;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return m_int;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Integer.toString(m_int);
    }

    /** Factory for (de-)serializing a IntCell. */
    private static class IntSerializer implements 
        DataCellSerializer<IntCell> {

        /**
         * @see DataCellSerializer#serialize(DataCell, DataOutput)
         */
        public void serialize(final IntCell cell, 
                final DataOutput output) throws IOException {
            output.writeInt(cell.m_int);
        }
        
        /**
         * @see DataCellSerializer#deserialize(DataInput)
         */
        public IntCell deserialize(
                final DataInput input) throws IOException {
            int i = input.readInt();
            return new IntCell(i);
        }
    }

}
