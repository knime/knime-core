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
import de.unikn.knime.core.data.FuzzyIntervalValue;
import de.unikn.knime.core.data.FuzzyNumberValue;

/**
 * A data cell implementation holding a Fuzzy number by storing this value in 
 * three private <code>double</code> members, that is one for the core and
 * two for the min/max of the support. It also provides a fuzzy interval value.
 * 
 * <p>The height of the membership value at the core value is assumed to be 1.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class FuzzyNumberCell extends DataCell implements
        FuzzyNumberValue, FuzzyIntervalValue {

    /** Convenience access member for 
     * <code>DataType.getType(FuzzyNumberCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = 
        DataType.getType(FuzzyNumberCell.class);
    
    private static final FuzzyNumberSerializer SERIALIZER = 
        new FuzzyNumberSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final FuzzyNumberSerializer getCellSerializer() {
        return SERIALIZER;
    }

    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return FuzzyNumberValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return FuzzyNumberValue.class;
    }

    /** Minimum support value. */
    private final double m_minSupp;

    /** Core value. */
    private final double m_core;

    /** Maximum support value. */
    private final double m_maxSupp;

    /**
     * Creates a new fuzzy number cell based on min, max support, and core.
     * 
     * @param minSupp Minimum support value.
     * @param core Core value.
     * @param maxSupp Maximum support value.
     * @throws IllegalArgumentException If not 
     * <code>minSupp <= core <= maxSupp</code>.
     */
    public FuzzyNumberCell(final double minSupp, final double core, 
            final double maxSupp) {
        if (!(minSupp <= core && core <= maxSupp)) {
            throw new IllegalArgumentException("Illegal FuzzyInterval: <" 
                    + minSupp + "," + core + "," + maxSupp + "> these numbers"
                    + " must be ascending from left to right!");
        }
        m_minSupp = minSupp;
        m_core = core;
        m_maxSupp = maxSupp;
    }

    /**
     * @return Minimum support value.
     */
    public double getMinSupport() {
        return m_minSupp;
    }

    /**
     * @return Core value.
     */
    public double getCore() {
        return m_core;
    }

    /**
     * @return Maximum support value.
     */
    public double getMaxSupport() {
        return m_maxSupp;
    }

    //
    // for compatibility with FuzzyIntervalValue
    //

    /**
     * @return <code>#getCore()</code>
     */
    public double getMinCore() {
        return getCore();
    }

    /**
     * @return <code>#getCore()</code>
     */
    public double getMaxCore() {
        return getCore();
    }

    /**
     * @return The center of gravity of this trapezoid membership function which
     *         are the weighted (by the area) gravities of each of the two areas
     *         (left triangle, right triangle). This value is divided
     *         by the overall membership function volume.
     */
    public double getCenterOfGravity() {
        // left support
        double a1 = (getCore() - getMinSupport()) / 2.0;
        double s1 = getMinSupport() + (getCore() - getMinSupport()) * 2.0 / 3.0;

        // right support
        double a3 = (getMaxSupport() - getCore()) / 2.0;
        double s3 = getCore() + (getMaxSupport() - getCore()) / 3.0;
        if (a1 + a3 == 0.0) {
            assert (s1 == s3);
            return s1;
        }
        return (a1 * s1 + a3 * s3) / (a1 + a3);
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        FuzzyNumberCell fc = (FuzzyNumberCell)dc;
        return (fc.m_minSupp == m_minSupp) && (fc.m_core == m_core) 
            && (fc.m_maxSupp == m_maxSupp);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        long minSuppBits = Double.doubleToLongBits(m_minSupp);
        long coreBits = Double.doubleToLongBits(m_core);
        long maxSuppBits = Double.doubleToLongBits(m_maxSupp);
        long bits = minSuppBits ^ (coreBits >> 1) ^ (maxSuppBits << 1);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "<" + m_minSupp + "," + m_core + "," + m_maxSupp + ">";
    }
    
    /** Factory for (de-)serializing a FuzzyNumberCell. */
    private static class FuzzyNumberSerializer 
        implements DataCellSerializer<FuzzyNumberCell> {
        /**
         * @see DataCellSerializer#serialize(DataCell, DataOutput)
         */
        public void serialize(final FuzzyNumberCell cell, 
                final DataOutput output) throws IOException {
            output.writeDouble(cell.getMinSupport());
            output.writeDouble(cell.getCore());
            output.writeDouble(cell.getMaxSupport());
        }
        
        /**
         * @see DataCellSerializer#deserialize(DataInput)
         */
        public FuzzyNumberCell deserialize(
                final DataInput input) throws IOException {
            double minSupp = input.readDouble();
            double core = input.readDouble();
            double maxSupp = input.readDouble();
            return new FuzzyNumberCell(minSupp, core, maxSupp);
        }
    }

}
