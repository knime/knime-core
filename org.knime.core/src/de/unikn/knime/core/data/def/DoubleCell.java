/* 
 * -------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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

/**
 * A data cell implementation holding a double value by storing this value in a
 * private <code>double</code> member. It provides a double value and a fuzzy
 * number value, as well as a fuzzy interval value.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class DoubleCell extends DataCell implements DoubleValue,
        FuzzyNumberValue, FuzzyIntervalValue {
    
    /** Convenience access member for 
     * <code>DataType.getType(DoubleCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = 
        DataType.getType(DoubleCell.class);

    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return DoubleValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return DoubleValue.class;
    }
    
    private static final DataCellSerializer<DoubleCell> SERIALIZER = 
        new DoubleSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<DoubleCell> getCellSerializer() {
        return SERIALIZER;
    }
    
    private final double m_double;

    /**
     * Creates a new cell for a generic double value. Also acting as
     * FuzzyNumberCell and FuzzyIntervalCell.
     * 
     * @param d The double value.
     */
    public DoubleCell(final double d) {
        m_double = d;
    }

    /**
     * @see de.unikn.knime.core.data.DoubleValue#getDoubleValue()
     */
    public double getDoubleValue() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getCore()
     */
    public double getCore() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getMaxSupport()
     */
    public double getMaxSupport() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getMinSupport()
     */
    public double getMinSupport() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMaxCore()
     */
    public double getMaxCore() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMinCore()
     */
    public double getMinCore() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getCenterOfGravity()
     */
    public double getCenterOfGravity() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((DoubleCell)dc).m_double == m_double;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m_double);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Double.toString(m_double);
    }
    
    /** Factory for (de-)serializing a DoubleCell. */
    private static class DoubleSerializer 
        implements DataCellSerializer<DoubleCell> {

        /**
         * @see DataCellSerializer#serialize(DataCell, DataOutput)
         */
        public void serialize(
                final DoubleCell cell, final DataOutput out) 
            throws IOException {
            out.writeDouble(cell.m_double);
        }
        
        /**
         * @see DataCellSerializer#deserialize(DataInput)
         */
        public DoubleCell deserialize(final DataInput input) 
            throws IOException {
            double d = input.readDouble();
            return new DoubleCell(d);
        }
    }
}
