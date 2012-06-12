/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.FuzzyIntervalValue;


/**
 * A data cell implementation holding a fuzzy interval as value by storing this
 * value in four private <code>double</code> members, two for the min/max
 * values of the support, and two for the min/max values of the core of the
 * fuzzy interval.
 * 
 * <p>The height of the membership value in the core region is assumed to be 1.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class FuzzyIntervalCell extends DataCell implements
        FuzzyIntervalValue {

    /** Convenience access member for 
     * <code>DataType.getType(FuzzyIntervalCell.class)</code>. 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = 
        DataType.getType(FuzzyIntervalCell.class);

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
    private final double m_minSupp;

    /** Minimum core value. */
    private final double m_minCore;

    /** Maximum core value. */
    private final double m_maxCore;

    /** Maximum support value. */
    private final double m_maxSupp;

    /**
     * Creates a new fuzzy interval cell based on the min/max of support and of
     * core.
     * 
     * @param minSupp Minimum support value.
     * @param minCore Minimum core value.
     * @param maxCore Maximum core value.
     * @param maxSupp Maximum support value.
     * @throws IllegalArgumentException If not <code>a <= b <= c <= d</code>.
     */
    public FuzzyIntervalCell(final double minSupp, final double minCore,
            final double maxCore, final double maxSupp) {
        if (!(minSupp <= minCore && minCore <= maxCore && maxCore <= maxSupp)) {
            throw new IllegalArgumentException("Illegal FuzzyInterval: <"
                    + minSupp + ", " + minCore + ", " + maxCore + ", " + maxSupp
                    + "> these numbers"
                    + " must be ascending from left to right!");
        }
        m_minSupp = minSupp;
        m_minCore = minCore;
        m_maxCore = maxCore;
        m_maxSupp = maxSupp;
    }

    /**
     * @return Minimum support value.
     */
    public double getMinSupport() {
        return m_minSupp;
    }

    /**
     * @return Minimum core value.
     */
    public double getMinCore() {
        return m_minCore;
    }

    /**
     * @return Maximum core value.
     */
    public double getMaxCore() {
        return m_maxCore;
    }

    /**
     * @return Maximum support value.
     */
    public double getMaxSupport() {
        return m_maxSupp;
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
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        FuzzyIntervalCell fc = (FuzzyIntervalCell)dc;
        if (Double.isNaN(fc.m_minSupp) && Double.isNaN(m_minSupp) 
                && Double.isNaN(fc.m_minCore) && Double.isNaN(m_minCore)
                && Double.isNaN(fc.m_maxCore) && Double.isNaN(m_maxCore)
                && Double.isNaN(fc.m_maxSupp) && Double.isNaN(m_maxSupp)) {
            return true;
        }
        return (fc.m_minSupp == m_minSupp) && (fc.m_minCore == m_minCore) 
            && (fc.m_maxCore == m_maxCore) && (fc.m_maxSupp == m_maxSupp);
    }

    /**
     * Computes hash code based on all private members.
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long minSuppBits = Double.doubleToLongBits(m_minSupp);
        long minCoreBits = Double.doubleToLongBits(m_minCore);
        long maxCoreBits = Double.doubleToLongBits(m_maxCore);
        long maxSuppBits = Double.doubleToLongBits(m_maxSupp);
        long bits = minSuppBits ^ (minCoreBits >> 1) 
            ^ (maxCoreBits >> 2) ^ (maxSuppBits << 1);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "<" + m_minSupp + "," + m_minCore + "," 
            + m_maxCore + "," + m_maxSupp + ">";
    }

    /** Factory for (de-)serializing a FuzzyIntervalCell. */
    private static class FuzzyIntervalSerializer 
        implements DataCellSerializer<FuzzyIntervalCell> {
        
        /**
         * {@inheritDoc}
         */
        public void serialize(final FuzzyIntervalCell cell, 
                final DataCellDataOutput output) throws IOException {
            output.writeDouble(cell.getMinSupport());
            output.writeDouble(cell.getMinCore());
            output.writeDouble(cell.getMaxCore());
            output.writeDouble(cell.getMaxSupport());
        }
        
        /**
         * {@inheritDoc}
         */
        public FuzzyIntervalCell deserialize(
                final DataCellDataInput input) throws IOException {
            double minSupp = input.readDouble();
            double minCore = input.readDouble();
            double maxCore = input.readDouble();
            double maxSupp = input.readDouble();
            return new FuzzyIntervalCell(
                    minSupp, minCore, maxCore, maxSupp);
        }
    }
}
