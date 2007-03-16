/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.IntervalValue;


/**
 * A <code>DataCell</code> implementation holding a numeric interval as value 
 * by storing left and right bound.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class IntervalCell extends DataCell implements
        FuzzyIntervalValue, IntervalValue {

    /** 
     * Convenience access member for 
     * <code>DataType.getType(IntervalCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = 
        DataType.getType(IntervalCell.class);

    /** 
     * Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return IntervalValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return IntervalValue.class;
    }
    
    private static final IntervalSerializer SERIALIZER = 
        new IntervalSerializer();
    
    /** 
     * Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return a serializer for reading/writing cells of this kind
     */
    public static final IntervalSerializer getCellSerializer() {
        return SERIALIZER;
    }
    
    /** Left interval bound. */
    private final double m_left;

    /** Right interval bound. */
    private final double m_right;

    /**
     * Creates a new interval cell based on the minimum and maximum value.
     * @param left bound
     * @param right bound
     * @throws IllegalArgumentException if <code>min</code> &gt; 
     *         <code>max</code>
     */
    public IntervalCell(final double left, final double right) {
        if (left > right) {
            throw new IllegalArgumentException("Illegal Interval: ["
                    + left + ", " + right + "] left must be less or equal to "
                    + "right interval value!");
        }
        m_left = left;
        m_right = right;
    }

    /**
     * @return Minimum support value.
     */
    public double getMinSupport() {
        return getLeftBound();
    }

    /**
     * @return Minimum core value.
     */
    public double getMinCore() {
        return getLeftBound();
    }

    /**
     * @return Maximum core value.
     */
    public double getMaxCore() {
        return getRightBound();
    }

    /**
     * @return Maximum support value.
     */
    public double getMaxSupport() {
        return getRightBound();
    }

    /**
     * Returns the mean of minimum and maximum border.
     * @return the mean of both value
     */
    public double getCenterOfGravity() {
        return (getRightBound() + getLeftBound()) / 2.0;
    }

    /**
     * Checks if this and the given cell have equal values.
     * @param dc the other <code>IntervalCell</code> to check values
     * @return true if both, minimum and maximum border have equal values,
     *         otherwise false
     * @see org.knime.core.data.DataCell
     *      #equalsDataCell(org.knime.core.data.DataCell)
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        IntervalCell ic = (IntervalCell) dc;
        return ic.getLeftBound() == getLeftBound() 
                && ic.getRightBound() == getRightBound();
    }

    /**
     * Computes hash code based on all private members.
     * @see DataCell#hashCode()
     */
    @Override
    public int hashCode() {
        long minBits = Double.doubleToLongBits(getLeftBound());
        long maxBits = Double.doubleToLongBits(getRightBound());
        long bits = minBits ^ maxBits;
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * Return a string summary of this object.
     * @return <code>[left,right]</code> string
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "[" + getLeftBound() + "," + getRightBound() + "]";
    }

    /** 
     * Factory for (de-)serializing a <code>IntervalCell</code>. 
     */
    private static class IntervalSerializer 
        implements DataCellSerializer<IntervalCell> {
        
        /**
         * @see DataCellSerializer#serialize(DataCell, DataOutput)
         */
        public void serialize(final IntervalCell cell, 
                final DataOutput output) throws IOException {
            output.writeDouble(cell.getLeftBound());
            output.writeDouble(cell.getRightBound());
        }
        
        /**
         * @see DataCellSerializer#deserialize(DataInput)
         */
        public IntervalCell deserialize(
                final DataInput input) throws IOException {
            return new IntervalCell(input.readDouble(), input.readDouble());
        }
    }

    /**
     * @see org.knime.core.data.IntervalValue#getRightBound()
     */
    public double getRightBound() {
        return m_right;
    }

    /**
     * @see org.knime.core.data.IntervalValue#getLeftBound()
     */
    public double getLeftBound() {
        return m_left;
    }

}
