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
import org.knime.core.data.IntValue;

/**
 * A data cell implementation holding an integer value by storing this value in
 * a private <code>int</code> member. It provides an int value, a double
 * value, a fuzzy number value, as well as a fuzzy interval value.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class IntCell extends DataCell implements IntValue, DoubleValue,
        ComplexNumberValue, FuzzyNumberValue, 
        FuzzyIntervalValue, BoundedValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(IntCell.class)</code>.
     * 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(IntCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     * 
     * @return IntValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return IntValue.class;
    }

    private static final DataCellSerializer<IntCell> SERIALIZER =
            new IntSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     * 
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
     * {@inheritDoc}
     */
    public int getIntValue() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    public double getDoubleValue() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    public double getCore() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxSupport() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinSupport() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxCore() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinCore() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    public double getCenterOfGravity() {
        return m_int;
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
        return m_int;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((IntCell)dc).m_int == m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Integer.toString(m_int);
    }

    /** Factory for (de-)serializing a IntCell. */
    private static class IntSerializer implements DataCellSerializer<IntCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final IntCell cell, 
                final DataCellDataOutput output) throws IOException {
            output.writeInt(cell.m_int);
        }

        /**
         * {@inheritDoc}
         */
        public IntCell deserialize(
                final DataCellDataInput input) throws IOException {
            int i = input.readInt();
            return new IntCell(i);
        }
    }

}
