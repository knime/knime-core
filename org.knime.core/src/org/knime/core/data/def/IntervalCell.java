/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.IntervalValue;

/**
 * A <code>DataCell</code> implementation holding a numeric interval as value
 * by storing left and right bound.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class IntervalCell extends DataCell implements FuzzyIntervalValue,
        IntervalValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(IntervalCell.class)</code>.
     * 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(IntervalCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     * 
     * @return IntervalValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return IntervalValue.class;
    }

    private static final IntervalSerializer SERIALIZER =
            new IntervalSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     * 
     * @return a serializer for reading/writing cells of this kind
     */
    public static final IntervalSerializer getCellSerializer() {
        return SERIALIZER;
    }

    /** Left interval bound. */
    private final double m_left;

    /** Right interval bound. */
    private final double m_right;

    /** Whether to include the left bound. */
    private final boolean m_includeLeft;

    /** Whether to include the right bound. */
    private final boolean m_includeRight;

    /**
     * Creates a new interval cell based on the minimum and maximum value.
     * 
     * @param left bound
     * @param right bound
     * @param includeLeft whether the left bound is included
     * @param includeRight whether the right bound is included
     * @throws IllegalArgumentException if <code>min</code> &gt;
     *             <code>max</code>
     */
    public IntervalCell(final double left, final double right,
            final boolean includeLeft, final boolean includeRight) {
        if (left > right) {
            throw new IllegalArgumentException("Illegal Interval: "
                    + getLeftBracket() + left + ", " + right
                    + getRightBracket() + " left must be less or equal to "
                    + "right interval value!");
        }
        
        m_left = left;
        m_right = right;
        m_includeLeft = includeLeft;
        m_includeRight = includeRight;
    }

    /**
     * Creates a new interval cell based on the minimum and maximum value, 
     * while both bounds are included.
     * 
     * @param left bound
     * @param right bound
     * @throws IllegalArgumentException if <code>min</code> &gt;
     *             <code>max</code>
     */
    public IntervalCell(final double left, final double right) {
        this(left, right, true, true);
    }

    private String getLeftBracket() {
        if (m_includeLeft) {
            return "[";
        } else {
            return "(";
        }
    }

    private String getRightBracket() {
        if (m_includeRight) {
            return "]";
        } else {
            return ")";
        }
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
     * 
     * @return the mean of both value
     */
    public double getCenterOfGravity() {
        return (getRightBound() + getLeftBound()) / 2.0;
    }

    /**
     * Checks if this and the given cell have equal values.
     * 
     * @param dc the other <code>IntervalCell</code> to check values
     * @return true if both, minimum and maximum border have equal values,
     *         otherwise false
     * @see org.knime.core.data.DataCell
     *      #equalsDataCell(org.knime.core.data.DataCell)
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        IntervalCell ic = (IntervalCell)dc;
        if (Double.isNaN(ic.getLeftBound()) && Double.isNaN(getLeftBound())
                && Double.isNaN(ic.getRightBound()) 
                && Double.isNaN(getRightBound())) {
            return true;
        }
        return ic.getLeftBound() == getLeftBound()
                && ic.getRightBound() == getRightBound()
                && ic.m_includeLeft == m_includeLeft
                && ic.m_includeRight == m_includeRight;
    }

    /**
     * Computes hash code based on all private members.
     * 
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long minBits = Double.doubleToLongBits(getLeftBound());
        long maxBits = Double.doubleToLongBits(getRightBound());
        long leftIncludeBits = 0L;
        if (m_includeLeft) {
            leftIncludeBits = 1L;
        }
        long rightIncludeBits = 0L;
        if (m_includeRight) {
            rightIncludeBits = 1L;
        }
        long bits = minBits ^ maxBits ^ leftIncludeBits ^ rightIncludeBits;
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * Return a string summary of this object.
     * 
     * @return <code>[left,right]</code> string
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getLeftBracket() + getLeftBound() + "," + getRightBound()
                + getRightBracket();
    }

    /**
     * Factory for (de-)serializing a <code>IntervalCell</code>.
     */
    private static class IntervalSerializer implements
            DataCellSerializer<IntervalCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final IntervalCell cell, 
                final DataCellDataOutput output) throws IOException {
            output.writeDouble(cell.getLeftBound());
            output.writeDouble(cell.getRightBound());
            output.writeBoolean(cell.leftBoundIncluded());
            output.writeBoolean(cell.rightBoundIncluded());
        }

        /**
         * {@inheritDoc}
         */
        public IntervalCell deserialize(final DataCellDataInput input)
                throws IOException {
            return new IntervalCell(input.readDouble(), input.readDouble(),
                    input.readBoolean(), input.readBoolean());
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getRightBound() {
        return m_right;
    }

    /**
     * {@inheritDoc}
     */
    public double getLeftBound() {
        return m_left;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(final double value) {
        if (value < m_left) {
            return -1;
        }
        if (value > m_right) {
            return 1;
        }
        int result = 0;
        if (value == m_left && !m_includeLeft) {
            result = -1;
        }
        if (value == m_right && !m_includeRight) {
            result = 1;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(final DoubleValue value) {
        return compare(value.getDoubleValue());
    }

    /**
     * {@inheritDoc}
     */
    public boolean includes(final IntervalValue value) {
        if (value.getLeftBound() < m_left) {
            return false;
        }
        if (value.getRightBound() > m_right) {
            return false;
        }
        if (value.getLeftBound() == m_left && !m_includeLeft
                && value.leftBoundIncluded()) {
            return false;
        }
        if (value.getRightBound() == m_right && !m_includeRight
                && value.rightBoundIncluded()) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean leftBoundIncluded() {
        return m_includeLeft;
    }

    /**
     * {@inheritDoc}
     */
    public boolean rightBoundIncluded() {
        return m_includeRight;
    }

}
