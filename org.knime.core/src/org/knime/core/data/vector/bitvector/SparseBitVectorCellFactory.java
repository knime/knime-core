/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   04.09.2008 (ohl): created
 */
package org.knime.core.data.vector.bitvector;

import java.util.Arrays;

import org.knime.core.data.DataCell;

/**
 *
 * @author ohl, University of Konstanz
 */
public class SparseBitVectorCellFactory {

    private SparseBitVector m_vector;

    /**
     * Initializes the factory to the specified length, all bits cleared.
     *
     * @param length of the vector in the cell to create
     */
    public SparseBitVectorCellFactory(final long length) {
        m_vector = new SparseBitVector(length);
    }

    /**
     * Initializes the factory to the specified length, all bits cleared. The
     * capacity of the vector (i.e. the storage for the indices of the '1's) is
     * set to the specified value.
     *
     * @param length of the vector in the cell to create
     * @param initialCapacity the number of expected '1's in the vector
     */
    public SparseBitVectorCellFactory(final long length,
            final int initialCapacity) {
        m_vector = new SparseBitVector(length, initialCapacity);
    }

    /**
     * Initializes the factory to the specified length, all bits whose index
     * appear in the passed array are set.
     *
     * @param length of the vector in the cell to create
     * @param oneIdxs the array containing the indices of the ones. Should be
     *            sorted (lowest index first).
     */
    public SparseBitVectorCellFactory(final long length, final long[] oneIdxs) {
        try {
            m_vector = new SparseBitVector(length, oneIdxs);
        } catch (IllegalArgumentException iae) {
            // sort the index array and try again
            long[] sorted = Arrays.copyOf(oneIdxs, oneIdxs.length);
            Arrays.sort(sorted);
            m_vector = new SparseBitVector(length, sorted);
        }
    }

    /**
     * A copy of the specified vector is stored in the created bit vector cell.
     *
     * @param vector used to initialize the bits.
     */
    public SparseBitVectorCellFactory(final SparseBitVector vector) {
        m_vector = new SparseBitVector(vector);
    }

    /**
     * Initializes the vector from a subsequence of the specified cell. The bits
     * used are the ones from <code>startIdx</code> to <code>endIdx - 1</code>.
     * The length of the resulting vector is <code>startIdx - endIdx</code>.
     *
     * @param cell the bit vector cell to take the subsequence from.
     * @param startIdx the first bit to include in the created bit vector
     * @param endIdx the first bit NOT to include in the result vector
     *
     */
    public SparseBitVectorCellFactory(final SparseBitVectorCell cell,
            final long startIdx, final long endIdx) {
        m_vector = cell.getBitVectorCopy().subSequence(startIdx, endIdx);
    }

    /**
     * Initializes the created bit vector from the hex representation in the
     * passed string. Only characters <code>'0' - '9'</code> and
     * <code>'A' - 'F'</code> are allowed. The character at string position
     * <code>(length - 1)</code> represents the bits with index 0 to 3 in the
     * vector. The character at position 0 represents the bits with the highest
     * indices. The length of the vector created is the length of the string
     * times 4 (as each character represents four bits).
     *
     * @param hexString containing the hex value to initialize the vector with
     * @throws IllegalArgumentException if <code>hexString</code> contains
     *             characters other then the hex characters (i.e.
     *             <code>0 - 9, A - F</code>)
     */
    public SparseBitVectorCellFactory(final String hexString) {
        m_vector = new SparseBitVector(hexString);
    }

    /**
     * Sets the bit with the specified index in the vector.
     *
     * @param bitIndex the index of the bit to set to one.
     */
    public void set(final long bitIndex) {
        m_vector.set(bitIndex);
    }

    /**
     * Sets the bit at the specified index to the new value.
     *
     * @param bitIdx the index of the bit to set or clear
     * @param value if true, the specified bit will be set, otherwise it will be
     *            cleared.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than the size of the vector
     */
    public void set(final long bitIdx, final boolean value) {
        m_vector.set(bitIdx, value);
    }

    /**
     * Clears the bit with the specified index in the vector.
     *
     * @param bitIndex the index of the bit to set to zero.
     */
    public void clear(final long bitIndex) {
        m_vector.clear(bitIndex);
    }

    /**
     * Creates a {@link DataCell} from the currently stored bit vector.
     *
     * @return a {@link DataCell} containing the current value of the vector
     */
    public SparseBitVectorCell createDataCell() {
        return new SparseBitVectorCell(m_vector);
    }

    /**
     * Creates a sparse bit vector cell containing the result of the AND
     * operation on the passed operands. The length of the result vector is the
     * maximum of the lengths of the operands.<br />
     * NOTE: This method performs best if the two arguments are both
     * {@link SparseBitVectorCell}s. All other implementations need to access
     * the bits through get/set methods which probably performs very poorly.
     * <br />
     * See also
     * {@link DenseBitVectorCellFactory#and(BitVectorValue, BitVectorValue)}
     * for ANDing dense bit vector cells.
     *
     * @param bv1 the first operand to AND with the other
     * @param bv2 the other operand to AND with the first one
     * @return the result of the AND operation
     */
    public static SparseBitVectorCell and(final BitVectorValue bv1,
            final BitVectorValue bv2) {
        if (bv1 instanceof SparseBitVectorCell
                && bv2 instanceof SparseBitVectorCell) {
            SparseBitVectorCell cell1 = (SparseBitVectorCell)bv1;
            SparseBitVectorCell cell2 = (SparseBitVectorCell)bv2;
            // TODO: don't create three new instances...
            return new SparseBitVectorCell(cell1.getBitVectorCopy().and(
                    cell2.getBitVectorCopy()));
        }
        // for all other implementations we need to go through get/set.
        SparseBitVector result =
                new SparseBitVector(Math.max(bv1.length(), bv2.length()));
        long bv1Idx = bv1.nextSetBit(0);
        long bv2Idx = bv2.nextSetBit(0);
        while (bv1Idx >= 0 && bv2Idx >= 0) {
            if (bv1Idx == bv2Idx) {
                // both vectors have a 1 at the same index - so will the result
                result.set(bv1Idx);
            }
            if (bv1Idx <= bv2Idx) {
                bv1Idx = bv1.nextSetBit(bv1Idx + 1);
            }
            if (bv1Idx >= bv2Idx) {
                bv2Idx = bv2.nextSetBit(bv2Idx + 1);
            }
        }
        return new SparseBitVectorCell(result);
    }

    /**
     * Creates a sparse bit vector cell containing the result of the OR
     * operation on the passed operands. The length of the result vector is the
     * maximum of the lengths of the operands.<br />
     * NOTE: This method performs best if the two arguments are both
     * {@link SparseBitVectorCell}s. All other implementations need to access
     * the bits through get/set methods which probably performs very poorly.
     * <br />
     * See also
     * {@link DenseBitVectorCellFactory#and(BitVectorValue, BitVectorValue)}
     * for ORing dense bit vector cells.
     *
     * @param bv1 the first operand to OR with the other
     * @param bv2 the other operand to OR with the first one
     * @return the result of the OR operation
     */
    public static SparseBitVectorCell or(final BitVectorValue bv1,
            final BitVectorValue bv2) {
        if (bv1 instanceof SparseBitVectorCell
                && bv2 instanceof SparseBitVectorCell) {
            SparseBitVectorCell cell1 = (SparseBitVectorCell)bv1;
            SparseBitVectorCell cell2 = (SparseBitVectorCell)bv2;
            // TODO: don't create three new instances...
            return new SparseBitVectorCell(cell1.getBitVectorCopy().or(
                    cell2.getBitVectorCopy()));
        }
        // for all other implementations we need to go through get/set.
        SparseBitVector result =
                new SparseBitVector(Math.max(bv1.length(), bv2.length()));
        for (long bv1Idx = bv1.nextSetBit(0); bv1Idx >= 0; bv1Idx =
                bv1.nextSetBit(bv1Idx + 1)) {
            result.set(bv1Idx);
        }
        for (long bv2Idx = bv2.nextSetBit(0); bv2Idx >= 0; bv2Idx =
                bv2.nextSetBit(bv2Idx + 1)) {
            result.set(bv2Idx);
        }
        return new SparseBitVectorCell(result);
    }

    /**
     * Creates a sparse bit vector cell containing the result of the XOR
     * operation on the passed operands. The length of the result vector is the
     * maximum of the lengths of the operands.<br />
     * NOTE: This method performs best if the two arguments are both
     * {@link SparseBitVectorCell}s. All other implementations need to access
     * the bits through get/set methods which probably performs very poorly.
     * <br />
     * See also
     * {@link DenseBitVectorCellFactory#and(BitVectorValue, BitVectorValue)}
     * for XORing dense bit vector cells.
     *
     * @param bv1 the first operand to XOR with the other
     * @param bv2 the other operand to XOR with the first one
     * @return the result of the XOR operation
     */
    public static SparseBitVectorCell xor(final BitVectorValue bv1,
            final BitVectorValue bv2) {
        if (bv1 instanceof SparseBitVectorCell
                && bv2 instanceof SparseBitVectorCell) {
            SparseBitVectorCell cell1 = (SparseBitVectorCell)bv1;
            SparseBitVectorCell cell2 = (SparseBitVectorCell)bv2;
            // TODO: don't create three new instances...
            return new SparseBitVectorCell(cell1.getBitVectorCopy().xor(
                    cell2.getBitVectorCopy()));
        }
        // for all other implementations we need to go through get/set.
        SparseBitVector result =
                new SparseBitVector(Math.max(bv1.length(), bv2.length()));
        long bv1Idx = bv1.nextSetBit(0);
        long bv2Idx = bv2.nextSetBit(0);
        while (bv1Idx >= 0 && bv2Idx >= 0) {

            if (bv1Idx == bv2Idx) {
                bv1Idx = bv1.nextSetBit(bv1Idx + 1);
                bv2Idx = bv2.nextSetBit(bv2Idx + 1);
            }
            if (bv1Idx < bv2Idx) {
                result.set(bv1Idx);
                bv1Idx = bv1.nextSetBit(bv1Idx + 1);
            }
            if (bv1Idx > bv2Idx) {
                result.set(bv2Idx);
                bv2Idx = bv2.nextSetBit(bv2Idx + 1);
            }
        }
        while (bv1Idx >= 0) {
            result.set(bv1Idx);
            bv1Idx = bv1.nextSetBit(bv1Idx + 1);
        }
        while (bv2Idx >= 0) {
            result.set(bv2Idx);
            bv2Idx = bv2.nextSetBit(bv2Idx + 1);
        }

        return new SparseBitVectorCell(result);
    }

}
