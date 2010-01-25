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
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.FuzzyNumberValue;

/**
 * A data cell implementation holding a Fuzzy number by storing this value in
 * three private <code>double</code> members, that is one for the core and two
 * for the min/max of the support. It also provides a fuzzy interval value.
 * 
 * <p>
 * The height of the membership value at the core value is assumed to be 1.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public final class FuzzyNumberCell extends DataCell implements
        FuzzyNumberValue, FuzzyIntervalValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(FuzzyNumberCell.class)</code>.
     * 
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(FuzzyNumberCell.class);

    private static final FuzzyNumberSerializer SERIALIZER =
            new FuzzyNumberSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     * 
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final FuzzyNumberSerializer getCellSerializer() {
        return SERIALIZER;
    }

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     * 
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
     *             <code>minSupp <= core <= maxSupp</code>.
     */
    public FuzzyNumberCell(final double minSupp, final double core,
            final double maxSupp) {
        if (!(minSupp <= core && core <= maxSupp)) {
            throw new IllegalArgumentException("Illegal FuzzyNumber: <"
                    + minSupp + "," + core + "," + maxSupp + "> these values"
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
     *         (left triangle, right triangle). This value is divided by the
     *         overall membership function volume.
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
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        FuzzyNumberCell fc = (FuzzyNumberCell)dc;
        if (Double.isNaN(fc.m_minSupp) && Double.isNaN(m_minSupp) 
                && Double.isNaN(fc.m_core) && Double.isNaN(m_core)
                && Double.isNaN(fc.m_maxSupp) && Double.isNaN(m_maxSupp)) {
            return true;
        }
        return (fc.m_minSupp == m_minSupp) && (fc.m_core == m_core)
                && (fc.m_maxSupp == m_maxSupp);
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "<" + m_minSupp + "," + m_core + "," + m_maxSupp + ">";
    }

    /** Factory for (de-)serializing a FuzzyNumberCell. */
    private static class FuzzyNumberSerializer implements
            DataCellSerializer<FuzzyNumberCell> {
        /**
         * {@inheritDoc}
         */
        public void serialize(final FuzzyNumberCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeDouble(cell.getMinSupport());
            output.writeDouble(cell.getCore());
            output.writeDouble(cell.getMaxSupport());
        }

        /**
         * {@inheritDoc}
         */
        public FuzzyNumberCell deserialize(final DataCellDataInput input)
                throws IOException {
            double minSupp = input.readDouble();
            double core = input.readDouble();
            double maxSupp = input.readDouble();
            return new FuzzyNumberCell(minSupp, core, maxSupp);
        }
    }
}
