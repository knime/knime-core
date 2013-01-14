/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * History
 *   19.08.2008 (ohl): created
 */
package org.knime.core.data.vector.bitvector;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.renderer.BitVectorValuePixelRenderer;
import org.knime.core.data.renderer.BitVectorValueStringRenderer;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

/**
 * Implementing {@link DataCell}s store '0's and '1's at specific positions in
 * a vector.
 *
 * @author ohl, University of Konstanz
 */
public interface BitVectorValue extends DataValue {

    /**
     * Meta information to bit vector values.
     *
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new BitVectorUtilityFactory();

    /**
     * Returns the length of the bit vector. The number of stored bits.
     *
     * @return the number of bits stored in the vector
     */
    public long length();

    /**
     * Returns the number of set bits (bits with value '1') in the vector.
     *
     * @return the number of set bits (bits with value '1') in the vector.
     */
    public long cardinality();

    /**
     * Returns the value of the specified bit.
     *
     * @param index the index of the bit to test
     * @return true if the bit at the specified index is set, false if it is
     *         zero.
     */
    public boolean get(final long index);

    /**
     * Returns true, if all bits in the vector are cleared.
     *
     * @return true, if all bits are zero, false, if at least one bit is set.
     */
    public boolean isEmpty();

    /**
     * Finds the next bit not set (that is '0') on or after the specified index.
     * Returns an index larger than or equal the provided index, or -1 if no bit
     * is cleared after the startIdx. (It is okay to pass an index larger than
     * the length of the vector.)
     *
     * @param startIdx the first index to look for '0's.
     * @return the index of the next cleared bit, which is on or after the
     *         provided startIdx. Or -1 if the vector contains no zero anymore.
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx negative
     */
    public long nextClearBit(final long startIdx);

    /**
     * Finds the next bit set to one on or after the specified index. Returns an
     * index larger than or equal the provided index, or -1 if no bit is set
     * after the startIdx. (Is okay to pass an index larger than the length of
     * the vector.)
     *
     * @param startIdx the first index to look for '1's. (It is allowed to pass
     *            an index larger then the vector's length.)
     * @return the index of the next bit set to one, which is on or after the
     *         provided startIdx.
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx is
     *             negative
     */
    public long nextSetBit(final long startIdx);

    /**
     * Returns the hex representation of the bits in this vector. Each character
     * in the result represents 4 bits (with the characters <code>'0'</code> -
     * <code>'9'</code> and <code>'A'</code> - <code>'F'</code>). The
     * character at string position <code>(length - 1)</code> holds the lowest
     * bits (bit 0 to 3), the character at position 0 represents the bits with
     * the largest index in the vector. If the length of the vector is larger
     * than ({@link Integer#MAX_VALUE} - 1) * 4 (i.e. 8589934584), the result
     * is truncated (and ends with ...).
     *
     * @return the hex representation of this bit vector.
     */
    public String toHexString();

    /**
     * Returns the binary string representation of the bits in this vector. Each
     * character in the result represents one bit - a '1' stands for a set bit,
     * a '0' represents a cleared bit. The character at string position
     * <code>(length - 1)</code> holds the bit with index 0, the character at
     * position 0 represents the bits with the largest index in the vector. If
     * the length of the vector is larger than ({@link Integer#MAX_VALUE} - 3)
     * (i.e. 2147483644), the result is truncated (and ends with ...).
     *
     * @return the binary (0/1) representation of this bit vector.
     */
    public String toBinaryString();

    /** Implementations of the meta information of this value class. */
    public static class BitVectorUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON =
                loadIcon(BitVectorValue.class, "/bitvectoricon.png");

        private static final DataValueComparator COMPARATOR =
                new DataValueComparator() {
                    /** {@inheritDoc} */
                    @Override
                    protected int compareDataValues(final DataValue v1,
                            final DataValue v2) {
                        long c1 = ((BitVectorValue)v1).cardinality();
                        long c2 = ((BitVectorValue)v2).cardinality();
                        if (c1 < c2) {
                            return -1;
                        } else if (c1 > c2) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                };

        /** Only subclasses are allowed to instantiate this class. */
        protected BitVectorUtilityFactory() {
            // intentionally left empty
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    BitVectorValueStringRenderer.HEX_RENDERER,
                    BitVectorValueStringRenderer.BIN_RENDERER,
                    new BitVectorValuePixelRenderer());
        }
    }

}
