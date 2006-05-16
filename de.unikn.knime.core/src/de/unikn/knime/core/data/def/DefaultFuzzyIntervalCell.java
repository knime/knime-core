/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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

/**
 * A data cell implementation holding a fuzzy interval as value by storing this
 * value in four private <code>double</code> members, two for the min/max
 * values of the support, and two for the min/max values of the core of the
 * fuzzy interval.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class DefaultFuzzyIntervalCell extends DataCell implements
        FuzzyIntervalValue {

    /** Convenience access member for 
     * <code>DataType.getType(DefaultFuzzyIntervalCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = 
        DataType.getType(DefaultFuzzyIntervalCell.class);

    /** Returns the preferred value class of this cell implementation. 
     * This method is called per reflection to determine which is the 
     * preferred renderer, comparator, etc.
     * @return FuzzyIntervalValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return FuzzyIntervalValue.class;
    }
    
    private static final FuzzyIntervalSerializer SERIALIZER = 
        new FuzzyIntervalSerializer();
    
    /** Returns the factory to read/write DataCells of this class from/to
     * a DataInput/DataOutput. This method is called via reflection.
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final FuzzyIntervalSerializer getCellSerializer() {
        return SERIALIZER;
    }
    
    /** Minimum support value. */
    private final double m_a;

    /** Minimum core value. */
    private final double m_b;

    /** Maximum core value. */
    private final double m_c;

    /** Maximum support value. */
    private final double m_d;

    /**
     * Creates a new fuzzy interval cell based on the min/max of support and of
     * core.
     * 
     * @param a Minimum support value.
     * @param b Minumum core value.
     * @param c Maximum core value.
     * @param d Maximum support value.
     * @throws IllegalArgumentException If not <code>a <= b <= c <= d</code>.
     */
    public DefaultFuzzyIntervalCell(final double a, final double b,
            final double c, final double d) {
        if (!((a < b || Math.abs(a - b) < 1e-9)
                && (b < c || Math.abs(b - c) < 1e-9) && (c < d || Math.abs(c
                - d) < 1e-9))) {
            throw new IllegalArgumentException("Illegal FuzzyInterval: <" + a
                    + "," + b + "," + c + "," + d + "> these numbers"
                    + " must be ascending from left to right!");
        }
        m_a = a;
        m_b = b;
        m_c = c;
        m_d = d;
    }

    /**
     * @return Minimum support value.
     */
    public double getMinSupport() {
        return m_a;
    }

    /**
     * @return Minimum core value.
     */
    public double getMinCore() {
        return m_b;
    }

    /**
     * @return Maximum core value.
     */
    public double getMaxCore() {
        return m_c;
    }

    /**
     * @return Maximum support value.
     */
    public double getMaxSupport() {
        return m_d;
    }

    /**
     * @return The center of gravity of this trapezoid membership function which
     *         are the weighted (by the area) gravities of each of the three
     *         areas (left triangle, core rectangle, right triangle) whereby the
     *         triangles' gravity point is 2/3 and 1/3 resp. is computed by the
     *         product of the gravity point and area for each interval. This
     *         value is divided by the overall membership function volume.
     */
    public double getCenterOfGravity() {
        if (isMissing()) {
            assert false;
            return Double.NaN;
        }
        // left support
        double a1 = (getMinCore() - getMinSupport()) / 2.0;
        double s1 = getMinSupport() + (getMinCore() - getMinSupport()) * 2.0
                / 3.0;
        // core
        double a2 = getMaxCore() - getMinCore();
        double s2 = getMinCore() + (getMaxCore() - getMinCore()) / 2.0;
        // right support
        double a3 = (getMaxSupport() - getMaxCore()) / 2.0;
        double s3 = getMaxCore() + (getMaxSupport() - getMaxCore()) / 3.0;
        if (a1 + a2 + a3 == 0.0) {
            assert (s1 == s2 && s2 == s3);
            return s1;
        }
        return (a1 * s1 + a2 * s2 + a3 * s3) / (a1 + a2 + a3);
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    protected boolean equalsDataCell(final DataCell dc) {
        DefaultFuzzyIntervalCell fc = (DefaultFuzzyIntervalCell)dc;
        return (fc.m_a == m_a) && (fc.m_b == m_b) && (fc.m_c == m_c)
                && (fc.m_d == m_d);
    }

    /**
     * Computes hash code based on the center of gravity of the fuzzy interval.
     * 
     * @return A hash code depending on the CoG of this cell.
     * 
     * @see java.lang.Double#hashCode()
     */
    public int hashCode() {
        long bits = Double.doubleToLongBits(getCenterOfGravity());
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "<" + m_a + "," + m_b + "," + m_c + "," + m_d + ">";
    }

    /** Factory for (de-)serializing a DefaultFuzzyIntervalCell. */
    private static class FuzzyIntervalSerializer 
        implements DataCellSerializer<DefaultFuzzyIntervalCell> {
        
        /**
         * @see DataCellSerializer#serialize(DataCell, DataOutput)
         */
        public void serialize(final DefaultFuzzyIntervalCell cell, 
                final DataOutput output) throws IOException {
            output.writeDouble(cell.getMinSupport());
            output.writeDouble(cell.getMinCore());
            output.writeDouble(cell.getMaxCore());
            output.writeDouble(cell.getMaxSupport());
        }
        
        /**
         * @see DataCellSerializer#deserialize(DataInput)
         */
        public DefaultFuzzyIntervalCell deserialize(
                final DataInput input) throws IOException {
            double minSupp = input.readDouble();
            double minCore = input.readDouble();
            double maxCore = input.readDouble();
            double maxSupp = input.readDouble();
            return new DefaultFuzzyIntervalCell(
                    minSupp, minCore, maxCore, maxSupp);
        }
    }

}
