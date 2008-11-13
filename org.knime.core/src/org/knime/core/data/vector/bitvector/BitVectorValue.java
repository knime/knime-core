/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 * a vector.<br />
 * The default implementation uses the {@link BitVectorCellFactory} to create
 * new instances of {@link BitVectorDataCell}s.<br />
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
