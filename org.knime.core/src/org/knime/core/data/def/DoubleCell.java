/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 *   27.02.07 (po): implements ComplexNumberValue now
 */
package org.knime.core.data.def;

import java.io.IOException;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.FuzzyNumberValue;


/**
 * A data cell implementation holding a double value by storing this value in a
 * private <code>double</code> member. It provides a double value and a fuzzy
 * number value, as well as a fuzzy interval value.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class DoubleCell extends DataCell 
    implements DoubleValue, ComplexNumberValue, FuzzyNumberValue, 
    FuzzyIntervalValue, BoundedValue {
    
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
     * {@inheritDoc}
     */
    public double getDoubleValue() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getCore() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxSupport() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinSupport() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxCore() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinCore() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getCenterOfGravity() {
        return m_double;
    }

    /**
     * {@inheritDoc}
     */
    public double getImaginaryValue() {
        return 0.0;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getRealValue() {
        return m_double;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        double o = ((DoubleCell)dc).m_double;
        if (Double.isNaN(m_double) && Double.isNaN(o)) {
            // Double.NaN is not equal to Double.NaN
            return true;
        }
        return o == m_double;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m_double);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Double.toString(m_double);
    }
    
    /** Factory for (de-)serializing a DoubleCell. */
    private static class DoubleSerializer 
        implements DataCellSerializer<DoubleCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(
                final DoubleCell cell, final DataCellDataOutput out) 
            throws IOException {
            out.writeDouble(cell.m_double);
        }
        
        /**
         * {@inheritDoc}
         */
        public DoubleCell deserialize(final DataCellDataInput input) 
            throws IOException {
            double d = input.readDouble();
            return new DoubleCell(d);
        }
    }
}
