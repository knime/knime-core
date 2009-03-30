/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 *
 * History
 *   21.01.2009 (meinl): created
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
import org.knime.core.data.LongValue;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LongCell extends DataCell implements LongValue, DoubleValue,
        ComplexNumberValue, FuzzyNumberValue, FuzzyIntervalValue, BoundedValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(LongCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(LongCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     * @return LongValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return LongValue.class;
    }

    private static final DataCellSerializer<LongCell> SERIALIZER =
            new LongSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<LongCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final long m_long;

    /**
     * Creates new cell for a generic long value.
     *
     * @param l The long value to store.
     */
    public LongCell(final long l) {
        m_long = l;
    }

    /**
     * {@inheritDoc}
     */
    public long getLongValue() {
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    public double getDoubleValue() {
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    public double getCore() {
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxSupport() {
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinSupport() {
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxCore() {
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinCore() {
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    public double getCenterOfGravity() {
        return m_long;
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
        return m_long;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((LongCell)dc).m_long == m_long;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (int)(m_long ^ (m_long >>> 32));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Long.toString(m_long);
    }

    /** Factory for (de-)serializing a LongCell. */
    private static class LongSerializer implements DataCellSerializer<LongCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final LongCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeLong(cell.m_long);
        }

        /**
         * {@inheritDoc}
         */
        public LongCell deserialize(final DataCellDataInput input)
                throws IOException {
            long l = input.readLong();
            return new LongCell(l);
        }
    }
}
