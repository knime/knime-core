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
package org.knime.core.data.collection.bitvector;

import java.util.Arrays;

/**
 * Stores Zeros and Ones in a vector, i.e. with fixed positions. The vector has
 * a fixed length. <br />
 * Implementation stores the bits in a collection of longs (64 bit words). Thus
 * it can be used for well populated vectors. Its length is restricted to ({@link Integer#MAX_VALUE} -
 * 1) * 64 (i.e. 137438953344, in which case it uses around 16GigaByte of
 * memory).<br />
 * The implementation is not thread-safe.
 *
 * @author ohl, University of Konstanz
 */
public class DenseBitVector {

    // number of bits used per storage object
    private static final int STORAGE_BITS = 64;

    // number of shifts need to go from bit index to storage index
    private static final int STORAGE_ADDRBITS = 6;

    // bits are stored in these objects
    private final long[] m_storage;

    // the first storage address containing a set bit
    private int m_firstAddr;

    // the last storage address containing a set bit
    private int m_lastAddr;

    /*
     * could be different from the actual storage length if some bits are left
     * unused "at the end".
     */
    private final long m_length;

    // lazy hashcode
    private int m_hash;

    /**
     * Creates a new vector of the specified length, with no bits set.
     *
     * @param length the length of the new bit vector.
     */
    public DenseBitVector(final long length) {
        if (length < 0) {
            throw new IllegalArgumentException(
                    "Length of a BitVector can't be negative.");
        }
        if (length >= (long)(Integer.MAX_VALUE - 1) * (long)STORAGE_BITS) {
            // we need MAX_VALUE internally!
            throw new IllegalArgumentException(
                    "Can't create a vector that big!");
        }
        m_length = length;
        assert ((m_length - 1) >> STORAGE_ADDRBITS) + 1 < Integer.MAX_VALUE;

        // shift with sign extension for length zero
        m_storage = new long[(int)(((m_length - 1) >> STORAGE_ADDRBITS) + 1)];
        m_firstAddr = -1;
        m_lastAddr = Integer.MAX_VALUE;

        assert (checkConsistency() == null);
    }

    /**
     * Creates a new instance taking over the initialization of the bits from
     * the passed array. The array must be build like the one returned by the
     * {@link #getAllBits()} method.
     *
     * @param bits the array containing the initial values of the vector
     * @param length the number of bits to use from the array. If the array is
     *            too long (i.e. contains more than length bits) the additional
     *            bits are ignored. If the array is too short, an exception is
     *            thrown.
     * @throws IllegalArgumentException if length is negative or MAX_VALUE, or
     *             if the length of the argument array is less than (length - 1)
     *             &gt;&gt; 6) + 1
     *
     */
    public DenseBitVector(final long[] bits, final long length) {
        if (length < 0) {
            throw new IllegalArgumentException(
                    "Length of a BitVector can't be negative.");
        }
        if (length >= (long)(Integer.MAX_VALUE - 1) * (long)STORAGE_BITS) {
            // we need MAX_VALUE internally!
            throw new IllegalArgumentException(
                    "Can't create a vector that big!");
        }

        long arrayLength = ((length - 1) >> STORAGE_ADDRBITS) + 1;
        assert arrayLength < Integer.MAX_VALUE;

        if (bits.length < arrayLength) {
            throw new IllegalArgumentException(
                    "Bits array is too short (length=" + bits.length
                            + ") to hold " + length + " bits.");
        }

        m_storage = Arrays.copyOf(bits, (int)arrayLength);
        m_length = length;
        // mask off bits beyond m_length
        maskOffBitsAfterEndOfVector();

        m_firstAddr = findFirstBitAddress();
        m_lastAddr = findLastBitAddress();

        assert (checkConsistency() == null);

    }

    /**
     * Initializes the created bit vector from the hex representation in the
     * passed string. Only characters <code>'0' - '9'</code> and
     * <code>'A' - 'F'</code> (or <code>'a' to 'f'</code>) are allowed. The
     * character at string position <code>(length - 1)</code> represents the
     * bits with index 0 to 3 in the vector. The character at position 0
     * represents the bits with the highest indices. The length of the created
     * vector is the length of the string times 4 (as each character represents
     * four bits).
     *
     * @param hexString containing the hex value to initialize the vector
     * @throws IllegalArgumentException if <code>hexString</code> contains
     *             characters other then the hex characters (i.e.
     *             <code>0 - 9, A - F, a - f</code>)
     */
    public DenseBitVector(final String hexString) {
        this(hexString.length() << 2); // four bits for each character

        int maxAddr = ((hexString.length() - 1) << 2) >> STORAGE_ADDRBITS;

        /*
         * take chunks of 16 character and store them in one storage qword.
         */
        for (int i = 0; i <= maxAddr; i += 16) {
            long value = 0;
            for (int n = 0; n < 16; n++) {
                if ((i * 16) + n >= hexString.length()) {
                    // the string is not a multiple of 16 characters.
                    // we leave the high-bits zero/cleared
                    break;
                }
                long cVal =
                        Character.toUpperCase(hexString.charAt((i * 16) + n));
                if (cVal < '0' || cVal > 'F' || (cVal > '9' && cVal < 'A')) {
                    throw new IllegalArgumentException(
                            "Invalid character in hex" + " number ('"
                                    + hexString.charAt(i + n) + "')");
                }
                if (cVal > '9') {
                    cVal -= 'A' - 10;
                } else {
                    cVal -= '0';
                }
                // cVal must only use the lower four bits
                assert (cVal & 0xFFFFFFFFFFFFFFF0L) == 0L;
                // shift the value in its nibble position
                cVal <<= (n << 2);
                // OR it onto the previous nibbles
                value |= cVal;

            }
            m_storage[i] = value;
        }
        assert checkConsistency() == null;
    }

    /**
     * Creates a new instance as copy of the passed argument.
     *
     * @param clone the vector to copy into the new instance
     */
    public DenseBitVector(final DenseBitVector clone) {
        if (clone == null) {
            throw new NullPointerException(
                    "Can't initialize from a null vector");
        }
        assert clone.checkConsistency() == null;
        m_storage = Arrays.copyOf(clone.m_storage, clone.m_storage.length);
        m_length = clone.m_length;
        m_firstAddr = clone.m_firstAddr;
        m_lastAddr = clone.m_lastAddr;
        m_hash = clone.m_hash;
        assert checkConsistency() == null;
    }

    /**
     * Returns the number of bits stored in this vector.
     *
     * @return the length of the vector.
     */
    public long length() {
        return m_length;
    }

    /**
     * Sets all bits in the storage array beyond m_length to zero. Normally
     * there should be no need to call this, as we ensure that no index is
     * specified beyond m_length. Only in {@link #invert()} and when
     * initializing from a long array this might be called.
     */
    private void maskOffBitsAfterEndOfVector() {
        if (m_length % STORAGE_BITS != 0) {
            long mask = ~(-1L << m_length);
            m_storage[m_storage.length - 1] &= mask;
        }
    }

    /**
     * Returns the index of the first storage location that contains a one.
     * Doesn't rely on m_firstAddr or m_lastAddr.
     *
     * @return the index of the first storage object that is not zero
     */
    private int findFirstBitAddress() {
        for (int i = 0; i < m_storage.length; i++) {
            if (m_storage[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first storage location that contains a one.
     * Doesn't rely on m_firstAddr or m_lastAddr.
     *
     * @return the index of the first storage object that is not zero
     */
    private int findLastBitAddress() {
        for (int i = m_storage.length - 1; i >= 0; i--) {
            if (m_storage[i] != 0) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
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
        if (value) {
            set(bitIdx);
        } else {
            clear(bitIdx);
        }
    }

    /**
     * Sets the bit at the specified index to zero.
     *
     * @param bitIdx the index of the bit to clear.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than the size of the vector
     */
    public void set(final long bitIdx) {
        assert (checkConsistency() == null);
        if (bitIdx >= m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + bitIdx
                    + "') too large for vector of length " + m_length);
        }
        assert bitIdx >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        int storageAddr = (int)(bitIdx >> STORAGE_ADDRBITS);
        int storageIdx = (int)(bitIdx % STORAGE_BITS);

        m_storage[storageAddr] = m_storage[storageAddr] | (1L << storageIdx);

        if (m_firstAddr < 0 || m_firstAddr > storageAddr) {
            m_firstAddr = storageAddr;
        }
        if (m_lastAddr >= m_storage.length || m_lastAddr < storageAddr) {
            m_lastAddr = storageAddr;
        }
        assert (checkConsistency() == null);
    }

    /**
     * Sets all bits in the specified range to the new value. The bit at index
     * startIdx is included, the endIdx is not included in the change. The
     * endIdx can't be smaller than the startIdx. If the indices are equal, no
     * change is made.
     *
     * @param startIdx the index of the first bit to set to the new value
     * @param endIdx the index of the last bit to set to the new value
     * @param value if set to true the bits are set to one, otherwise to zero
     */
    public void set(final long startIdx, final long endIdx, final boolean value) {
        if (value) {
            set(startIdx, endIdx);
        } else {
            clear(startIdx, endIdx);
        }
    }

    /**
     * Sets all bits in the specified range. The bit at index startIdx is
     * included, the endIdx is not included in the change. The endIdx can't be
     * smaller than the startIdx. If the indices are equal, no change is made.
     *
     * @param startIdx the index of the first bit to set to one
     * @param endIdx the index of the last bit to set to one
     */
    public void set(final long startIdx, final long endIdx) {
        assert (checkConsistency() == null);
        if (endIdx < startIdx) {
            throw new IllegalArgumentException("The end index can't be smaller"
                    + " than the start index.");
        }
        if (endIdx > m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + endIdx
                    + "') too large for vector of length " + m_length);
        }
        if (endIdx == startIdx) {
            return;
        }
        assert startIdx >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        assert endIdx - 1 >> STORAGE_ADDRBITS < Integer.MAX_VALUE;

        int storageStartAddr = (int)(startIdx >> STORAGE_ADDRBITS);
        // endIdx is not supposed to be changed
        int storageEndAddr = (int)(endIdx - 1 >> STORAGE_ADDRBITS);

        long firstMask = -1L << startIdx;
        long lastMask = ~(-1L << endIdx);
        if (endIdx % STORAGE_BITS == 0) {
            lastMask = -1L;
        }
        if (storageStartAddr == storageEndAddr) {
            // range fully lies in one storage object
            m_storage[storageStartAddr] |= firstMask & lastMask;
            if (m_firstAddr < 0 || m_firstAddr > storageStartAddr) {
                m_firstAddr = storageStartAddr;
            }
            if (m_lastAddr >= m_storage.length || m_lastAddr < storageEndAddr) {
                m_lastAddr = storageEndAddr;
            }

        } else {
            int addr = storageStartAddr;
            // apply first mask to first storage address
            m_storage[addr++] |= firstMask;
            // set all addresses in-between to all '1's
            while (addr < storageEndAddr) {
                m_storage[addr++] = -1L;
            }
            // apply last mask to last storage address
            m_storage[addr] |= lastMask;

            if (m_firstAddr < 0 || m_firstAddr > storageStartAddr) {
                m_firstAddr = storageStartAddr;
            }
            if (m_lastAddr >= m_storage.length || m_lastAddr < storageEndAddr) {
                m_lastAddr = storageEndAddr;
            }
        }
        assert (checkConsistency() == null);
    }

    /**
     * Sets the bit at the specified index to one.
     *
     * @param bitIdx the index of the bit to set.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than the size of the vector
     */
    public void clear(final long bitIdx) {
        assert (checkConsistency() == null);
        if (bitIdx >= m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + bitIdx
                    + "') too large for vector of length " + m_length);
        }

        assert bitIdx >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        int storageAddr = (int)(bitIdx >> STORAGE_ADDRBITS);
        int storageIdx = (int)(bitIdx % STORAGE_BITS);
        m_storage[storageAddr] = m_storage[storageAddr] & (~(1L << storageIdx));

        if (storageAddr == m_firstAddr && m_storage[storageAddr] == 0) {
            // we just cleared the first bit...
            m_firstAddr = findFirstBitAddress();
        }
        if (storageAddr == m_lastAddr && m_storage[storageAddr] == 0) {
            // we just cleared the last bit...
            m_lastAddr = findLastBitAddress();
        }
        assert (checkConsistency() == null);

    }

    /**
     * Clears all bits in the specified range. The bit at index startIdx is
     * included, the endIdx is not included in the change. The endIdx can't be
     * smaller than the startIdx. If the indices are equal, no change is made.
     *
     * @param startIdx the index of the first bit to set to zero
     * @param endIdx the index of the last bit to set to zero
     */
    public void clear(final long startIdx, final long endIdx) {
        assert (checkConsistency() == null);
        if (endIdx < startIdx) {
            throw new IllegalArgumentException("The end index can't be smaller"
                    + " than the start index.");
        }
        if (endIdx > m_length) {
            throw new ArrayIndexOutOfBoundsException("Endindex ('" + endIdx
                    + "') too large for vector of length " + m_length);
        }
        if (endIdx == startIdx) {
            return;
        }

        assert startIdx >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        assert endIdx - 1 >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        int storageStartAddr = (int)(startIdx >> STORAGE_ADDRBITS);
        // last index is not supposed to be changed
        int storageEndAddr = (int)(endIdx - 1 >> STORAGE_ADDRBITS);

        long firstMask = -1L << startIdx;
        long lastMask = ~(-1L << endIdx);
        if (endIdx % STORAGE_BITS == 0) {
            lastMask = -1L;
        }
        if (storageStartAddr == storageEndAddr) {
            // range fully lies in one storage object
            m_storage[storageStartAddr] &= ~(firstMask & lastMask);
        } else {
            int addr = storageStartAddr;
            // apply first mask to first storage address
            m_storage[addr++] &= ~firstMask;
            // set all addresses in-between to all '0's
            while (addr < storageEndAddr) {
                m_storage[addr++] = 0;
            }
            // apply last mask to last storage address
            m_storage[addr] &= ~lastMask;
        }
        if (storageStartAddr <= m_firstAddr && storageEndAddr >= m_firstAddr) {
            m_firstAddr = findFirstBitAddress();
        }
        if (storageStartAddr <= m_lastAddr && storageEndAddr >= m_lastAddr) {
            m_lastAddr = findLastBitAddress();
        }

        assert (checkConsistency() == null);
    }

    /**
     * Number of bits set in this bit vector.
     *
     * @return the number of ones in this vector
     */
    public long cardinality() {
        assert (checkConsistency() == null);
        long result = 0;
        // because we make sure no bits are set beyond the length of the vector
        // we can just count all ones
        if (m_firstAddr == -1) {
            return 0;
        }
        for (int i = m_firstAddr; i <= m_lastAddr; i++) {
            result += Long.bitCount(m_storage[i]);
        }
        return result;
    }

    /**
     * Returns true if no bits are set in this bit vector.
     *
     * @return true if no bits are set in this bit vector.
     */
    public boolean isEmpty() {
        assert (checkConsistency() == null);
        return m_firstAddr == -1;
    }

    /**
     * Returns true, if this and the argument vector have at least one bit set
     * at the same position.
     *
     * @param bv the vector to test
     * @return true, if this and the argument vector have at least one bit set
     *         at the same position.
     */
    public boolean intersects(final DenseBitVector bv) {
        assert (checkConsistency() == null);

        if (bv.isEmpty() || isEmpty()) {
            return false;
        }
        int startIdx = Math.max(m_firstAddr, bv.m_firstAddr);
        int endIdx = Math.min(m_lastAddr, bv.m_lastAddr);
        if (startIdx > endIdx) {
            return false;
        }

        for (int i = startIdx; i <= endIdx; i++) {
            if ((m_storage[i] & bv.m_storage[i]) != 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the bit at the specified index is set. False otherwise.
     *
     * @param bitIdx the index of the bit to test.
     * @return <code>true</code> if the specified bit is set,
     *         <code>false</code> otherwise
     * @throws ArrayIndexOutOfBoundsException if the index is larger than the
     *             length of the vector
     */
    public boolean get(final long bitIdx) {
        assert (checkConsistency() == null);
        if (bitIdx >= m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + bitIdx
                    + "') too large for vector of length " + m_length);
        }
        assert bitIdx >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        int storageAddr = (int)(bitIdx >> STORAGE_ADDRBITS);
        int storageIdx = (int)(bitIdx % STORAGE_BITS);
        return (m_storage[storageAddr] & (1L << storageIdx)) != 0;
    }

    /**
     * Finds the next bit set to one on or after the specified index. Returns an
     * index larger than or equal the provided index, or -1 if no bit is set
     * after the startIdx. (This is the only method (and the #nextClearBit)
     * where it is okay to pass an index larger than the length of the vector.)
     *
     * @param startIdx the first index to look for '1's. (It is allowed to pass
     *            an index larger then the vector's length.)
     * @return the index of the next bit set to one, which is on or after the
     *         provided startIdx.
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx is
     *             negative
     */
    public long nextSetBit(final long startIdx) {
        assert (checkConsistency() == null);
        if (startIdx >= m_length) {
            return -1;
        }
        if (startIdx < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    "Starting index can't be negative.");
        }

        if (m_firstAddr == -1) {
            // there is no bit set in this vector
            return -1;
        }

        assert startIdx >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        int storageAddr = (int)(startIdx >> STORAGE_ADDRBITS);
        int storageIdx;

        if (storageAddr >= m_firstAddr) {
            storageIdx = (int)(startIdx % STORAGE_BITS);
        } else {
            // lets start with the first used storage object
            storageAddr = m_firstAddr;
            storageIdx = 0;
        }

        // mask off the bits before the startIdx
        long bits = m_storage[storageAddr] & (-1L << storageIdx);

        while (true) {
            if (bits != 0) {
                return ((long)STORAGE_BITS * (long)storageAddr)
                        + Long.numberOfTrailingZeros(bits);
            }
            storageAddr++;
            if (storageAddr > m_lastAddr) {
                break;
            }
            bits = m_storage[storageAddr];
        }
        // no further '1's in this vector
        return -1;
    }

    /**
     * Finds the next bit not set (that is '0') on or after the specified index.
     * Returns an index larger than or equal the provided index, or -1 if no bit
     * is cleared after the startIdx. (This is the only method (and the
     * #nextSetBit) where it is okay to pass an index larger than the length of
     * the vector.)
     *
     * @param startIdx the first index to look for '0's.
     * @return the index of the next cleared bit, which is on or after the
     *         provided startIdx. Or -1 if the vector contains no zero anymore.
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx negative
     */
    public long nextClearBit(final long startIdx) {
        assert (checkConsistency() == null);
        if (startIdx >= m_length) {
            return -1;
        }
        if (startIdx < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    "Starting index can't be negative.");
        }

        assert startIdx >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
        int storageAddr = (int)(startIdx >> STORAGE_ADDRBITS);
        int storageIdx = (int)(startIdx % STORAGE_BITS);

        if (m_firstAddr == -1 || storageAddr < m_firstAddr
                || storageAddr > m_lastAddr) {
            /*
             * there is no bit set in this vector, or we are outside the range
             * where bits are set
             */
            return startIdx;
        }

        /*
         * As java.lang.Long only counts trailing zeros (and not ones) we invert
         * the bits and look for ones.
         */

        // mask off the bits before the startIdx
        long bits = ~m_storage[storageAddr] & (-1L << storageIdx);

        while (m_lastAddr > storageAddr) {
            if (bits != 0) {
                return ((long)STORAGE_BITS * (long)storageAddr)
                        + Long.numberOfTrailingZeros(bits);
            }
            bits = ~m_storage[++storageAddr];
        }

        // bits contains the last used storage object.
        // (note, if it is fully set 'bits' will be zero and
        // numberOfTrailingZeros returns 64 - which is fine.)
        long result =
                ((long)STORAGE_BITS * (long)storageAddr)
                        + Long.numberOfTrailingZeros(bits);
        if (result >= m_length) {
            return -1;
        } else {
            return result;
        }

    }

    /**
     * Creates and returns a new bit vector whose bits are set at positions
     * where both, this and the argument vector have their bits set. The length
     * of the new vector is the maximum of the length of this and the argument.
     *
     * @param bv the vector to AND this one with
     * @return a new instance containing the result of the AND operation
     */
    public DenseBitVector and(final DenseBitVector bv) {
        assert (checkConsistency() == null);
        DenseBitVector result =
                new DenseBitVector(Math.max(m_length, bv.m_length));

        if (isEmpty() || bv.isEmpty()) {
            assert (result.checkConsistency() == null);
            return result;
        }

        int startAddr = Math.max(m_firstAddr, bv.m_firstAddr);
        int endAddr = Math.min(m_lastAddr, bv.m_lastAddr);
        if (endAddr < startAddr) {
            // no intersection of ones
            assert (result.checkConsistency() == null);
            return result;
        }

        for (int i = startAddr; i <= endAddr; i++) {
            result.m_storage[i] = m_storage[i] & bv.m_storage[i];
        }
        result.m_firstAddr = result.findFirstBitAddress();
        result.m_lastAddr = result.findLastBitAddress();

        assert (result.checkConsistency() == null);
        return result;

    }

    /**
     * Creates and returns a new bit vector whose bits are set at positions
     * where at least one of the vectors (this or the argument vector) have a
     * bit set. The length of the new vector is the maximum of the length of
     * this and the argument.
     *
     * @param bv the vector to OR this one with
     * @return a new instance containing the result of the OR operation
     */
    public DenseBitVector or(final DenseBitVector bv) {
        assert (checkConsistency() == null);
        DenseBitVector result =
                new DenseBitVector(Math.max(m_length, bv.m_length));

        // check if one of them is empty
        if (isEmpty()) {
            if (bv.isEmpty()) {
                return result;
            }
            // just copy bv's array
            System.arraycopy(bv.m_storage, 0, result.m_storage, 0,
                    bv.m_storage.length);
            result.m_firstAddr = bv.m_firstAddr;
            result.m_lastAddr = bv.m_lastAddr;
            assert (result.checkConsistency() == null);
            return result;
        } else {
            if (bv.isEmpty()) {
                // just copy this' array
                System.arraycopy(m_storage, 0, result.m_storage, 0,
                        m_storage.length);
                result.m_firstAddr = m_firstAddr;
                result.m_lastAddr = m_lastAddr;
                assert (result.checkConsistency() == null);
                return result;
            }
        }

        /*
         * TODO: Only the intersection of both used address spaces actually
         * needs to be ORed. The non-intersecting regions (from the one
         * firstAddr to the firstAddr of the other operand and from the one
         * lastAddr to the other lastAddr of the other operand) could be just
         * copied.
         */
        int startAddr = Math.min(m_firstAddr, bv.m_firstAddr);
        int endAddr = Math.max(m_lastAddr, bv.m_lastAddr);

        for (int i = startAddr; i <= endAddr; i++) {
            result.m_storage[i] = m_storage[i] | bv.m_storage[i];
        }
        result.m_firstAddr = startAddr;
        result.m_lastAddr = endAddr;

        assert (result.checkConsistency() == null);
        return result;

    }

    /**
     * Creates and returns a new bit vector whose bits are set at positions
     * where (exactly) one of the vectors (this or the argument vector) have a
     * bit set. The length of the new vector is the maximum of the length of
     * this and the argument.
     *
     * @param bv the vector to XOR this one with
     * @return a new instance containing the result of the XOR operation
     */
    public DenseBitVector xor(final DenseBitVector bv) {
        assert (checkConsistency() == null);

        DenseBitVector result =
                new DenseBitVector(Math.max(m_length, bv.m_length));

        // check if one of them is empty
        if (isEmpty()) {
            if (bv.isEmpty()) {
                return result;
            }
            // just copy bv's array
            System.arraycopy(bv.m_storage, 0, result.m_storage, 0,
                    bv.m_storage.length);
            result.m_firstAddr = bv.m_firstAddr;
            result.m_lastAddr = bv.m_lastAddr;
            assert (result.checkConsistency() == null);
            return result;
        } else {
            if (bv.isEmpty()) {
                // just copy this' array
                System.arraycopy(m_storage, 0, result.m_storage, 0,
                        m_storage.length);
                result.m_firstAddr = m_firstAddr;
                result.m_lastAddr = m_lastAddr;
                assert (result.checkConsistency() == null);
                return result;
            }
        }

        /*
         * TODO: Only the intersection of both used address spaces actually
         * needs to be XORed. The non-intersecting regions (from the one
         * firstAddr to the firstAddr of the other operand and from the one
         * lastAddr to the other lastAddr of the other operand) could be just
         * copied.
         */
        int startAddr = Math.min(m_firstAddr, bv.m_firstAddr);
        int endAddr = Math.max(m_lastAddr, bv.m_lastAddr);

        for (int i = startAddr; i <= endAddr; i++) {
            result.m_storage[i] = m_storage[i] ^ bv.m_storage[i];
        }
        result.m_firstAddr = result.findFirstBitAddress();
        result.m_lastAddr = result.findLastBitAddress();

        assert (result.checkConsistency() == null);
        return result;
    }

    /**
     * Creates and returns a new bit vector whose bits are inverted compared to
     * this vector. The bits of the result are set at positions where this
     * vector has a cleared bit and vice versa. The result vector has the same
     * length as this vector.
     *
     * @return a new instance containing the inverted bits of this vector
     */
    public DenseBitVector invert() {
        assert (checkConsistency() == null);
        DenseBitVector result = new DenseBitVector(m_length);

        if (isEmpty()) {
            // this might be faster
            if (m_length > 0) {
                Arrays.fill(result.m_storage, -1L);
                result.m_firstAddr = 0;
                result.m_lastAddr = result.m_storage.length - 1;
                // mask off the bits beyond m_length
                result.maskOffBitsAfterEndOfVector();

            }
            assert (result.checkConsistency() == null);
            return result;
        }
        for (int i = 0; i < m_storage.length; i++) {
            result.m_storage[i] = ~m_storage[i];
        }
        result.maskOffBitsAfterEndOfVector();
        result.m_firstAddr = result.findFirstBitAddress();
        result.m_lastAddr = result.findLastBitAddress();
        assert (result.checkConsistency() == null);
        return result;
    }

    /**
     * Creates and returns a new bit vector that contains copies of both (this
     * and the argument vector). The argument vector is appended at the end of
     * this vector, i.e. it's bit with index zero will be stored at index
     * "length-of-this-vector" in the result vector. The length of the result is
     * the length of this plus the length of the argument vector.
     *
     * @param bv the vector to append at the end of this
     * @return a new instance containing both vectors concatenated
     */
    public DenseBitVector concatenate(final DenseBitVector bv) {
        assert (checkConsistency() == null);
        DenseBitVector result = new DenseBitVector(m_length + bv.m_length);

        // first we always copy this' storage - unless its all zeros
        if (!isEmpty()) {
            System.arraycopy(m_storage, 0, result.m_storage, 0,
                    m_storage.length);
            result.m_firstAddr = m_firstAddr;
        }

        if (bv.isEmpty()) {
            result.m_lastAddr = m_lastAddr;
            assert (result.checkConsistency() == null);
            return result;
        }

        if (m_length % STORAGE_BITS == 0) {
            // this' array is aligned - just copy bv's array
            System.arraycopy(bv.m_storage, 0, result.m_storage,
                    m_storage.length, bv.m_storage.length);
            // we know bv is not empty (that's handled above)
            if (isEmpty()) {
                result.m_firstAddr = bv.m_firstAddr + m_storage.length;
            }
            result.m_lastAddr = bv.m_lastAddr + m_storage.length;
            assert (result.checkConsistency() == null);
            return result;
        }

        // cut a piece from bv's storage that fills the "rest" of our storage
        int leftover = (int)(m_length % STORAGE_BITS);

        int resultAddr = m_storage.length - 1;
        int bvAddr = 0;
        while (bvAddr <= bv.m_lastAddr) {
            result.m_storage[resultAddr] |= bv.m_storage[bvAddr] << leftover;
            if (resultAddr < result.m_storage.length - 1) {
                result.m_storage[resultAddr + 1] =
                        bv.m_storage[bvAddr] >>> (STORAGE_BITS - leftover);
            }
            bvAddr++;
            resultAddr++;
        }

        result.m_firstAddr = result.findFirstBitAddress();
        result.m_lastAddr = result.findLastBitAddress();
        assert (result.checkConsistency() == null);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        assert (checkConsistency() == null);
        if (m_hash == 0) {
            long h = 1234;
            if (m_firstAddr > -1) {
                for (int i = m_lastAddr; i >= m_firstAddr; i--) {
                    h ^= m_storage[i] * (i + 1);
                }
            }
            m_hash = (int)((h >> 32) ^ h);
        }
        return m_hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        assert (checkConsistency() == null);
        if (!(obj instanceof DenseBitVector)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        DenseBitVector o = (DenseBitVector)obj;

        assert (o.checkConsistency() == null);
        if (o.m_firstAddr != m_firstAddr || o.m_lastAddr != m_lastAddr) {
            return false;
        }
        if (m_firstAddr == -1) {
            // all empty.
            return true;
        }
        for (int i = m_firstAddr; i <= m_lastAddr; i++) {
            if (o.m_storage[i] != m_storage[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a string containing (comma separated) indices of the bits set in
     * this vector. The number of bit indices added to the string is limited to
     * 300000. If the output is truncated, the string ends on &quot;... }&quot;
     *
     * @return a string containing (comma separated) indices of the bits set in
     *         this vector.
     */
    @Override
    public String toString() {
        assert (checkConsistency() == null);
        long ones = cardinality();

        int use = (int)Math.min(ones, 300000);

        StringBuilder result = new StringBuilder(use * 7);
        result.append('{');
        for (long i = nextSetBit(0); i > -1; i = nextSetBit(++i)) {
            result.append(i).append(", ");
        }
        if (use < ones) {
            result.append("... ");
        } else if (result.length() > 2) {
            result.delete(result.length() - 2, result.length());
        }
        result.append('}');
        return result.toString();
    }

    /**
     * Returns the hex representation of the bits in this vector. Each character
     * in the result represents 4 bits (with the characters <code>'0'</code> -
     * <code>'9'</code> and <code>'A'</code> - <code>'F'</code>). The
     * character at string position <code>(length - 1)</code> holds the lowest
     * bits (bit 0 to 3), the character at position 0 represents the bits with
     * the largest index in the vector. If the length of the vector is larger
     * than ({@link Integer#MAX_VALUE} - 1) * 4 (i.e. 8589934584), the result is
     * truncated (and ends with ...).
     *
     * @return the hex representation of this bit vector.
     */
    public String toHexString() {
        // the number of bits we store in the string
        long max = (int)Math.min(m_length, (Integer.MAX_VALUE - 1) << 2);

        // 4 bits are combined to one character
        StringBuilder result = new StringBuilder((int)(max >> 2));

        // the last storage might not be fully used
        int leftOver = (int)(m_length % STORAGE_BITS);

        // start with the highest bits

        int storageAddr = (int)((max - 1) >> STORAGE_ADDRBITS);
        assert storageAddr <= m_storage.length;

        int nibbleIdx;
        if (leftOver == 0) {
            nibbleIdx = 15;
        } else {
            nibbleIdx = leftOver >> 2;
        }
        while (storageAddr >= 0) {

            while (nibbleIdx >= 0) {
                int value = (int)(m_storage[storageAddr] >>> (nibbleIdx << 2));
                value &= 0x0f;

                value += '0';
                if (value > '9') {
                    value += ('A' - ('9' + 1));
                }
                // add character to string
                result.append((char)(value));

                nibbleIdx--;
            }
            // a 64bit word stores 16 nibbles
            nibbleIdx = 15;
            storageAddr--;
        }

        if (max < m_length) {
            result.append("...");
        }
        return result.toString();

    }

    /**
     * There are certain conditions the implementation depends on. They are all
     * checked in here. Normally the method should return null. If it doesn't
     * something is fishy and a error message is returned. NOTE: This method is
     * not cheap! It should be called in an assert statement only.
     *
     * @return the error message, or null if everything is alright
     */
    private String checkConsistency() {
        if (m_firstAddr != findFirstBitAddress()) {
            return "m_firstAddress is not set properly";
        }

        if (m_lastAddr != findLastBitAddress()) {
            return "m_lastAddress is not set properly";
        }

        if (m_length > 0) {
            assert (m_length - 1) >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
            int highestIdx = (int)((m_length - 1) >> STORAGE_ADDRBITS);
            if (m_storage.length <= highestIdx) {
                return "Storage array is too short";
            }
            // storage shouldn't be too long either
            if (m_storage.length > ((m_length - 1) >> STORAGE_ADDRBITS) + 1) {
                return "Storage array is too long";
            }
            // make sure there are no bits set "above" m_length
            assert m_length >> STORAGE_ADDRBITS < Integer.MAX_VALUE;
            int addr = (int)(m_length >> STORAGE_ADDRBITS);
            int idx = (int)(m_length % STORAGE_BITS);
            while (addr < m_storage.length) {
                if ((m_storage[addr] & (1L << idx)) != 0) {
                    return "A bit is set outside the vector's length";
                }
                idx++;
                addr += idx / STORAGE_BITS;
                idx = idx % STORAGE_BITS;
            }
        } else {
            // if length is zero, the storage should be of length zero too
            if (m_storage.length > 0) {
                return "Vector length is zero, but has a storage allocated";
            }

        }
        return null;
    }

    /**
     * Returns a copy of the internal storage of all bits. The vector's bit with
     * index zero is stored in the array at index zero and at the right-most bit
     * (LSB) of the long word. In general bit with index i is stored in the long
     * word at index (i &gt;&gt;&gt; 6) in the array and in this long word at
     * bit position (index) i % 64. The length of the returned array is
     * ((vector_length - 1) &gt;&gt; 6) + 1.
     *
     * @return a copy of the internal representation of the bits in this vector.
     */
    public long[] getAllBits() {
        return m_storage.clone();
    }

    /**
     * Returns a multi-line dump of the internal storage.
     *
     * @return a multi-line dump of the internal storage.
     */
    public String dumpBits() {
        assert (checkConsistency() == null);
        if (m_length == 0) {
            return "<bitvector of length zero>";
        }
        StringBuilder result =
                new StringBuilder(m_storage.length * (STORAGE_BITS + 15));
        for (int i = m_storage.length - 1; i >= 0; i--) {
            result.append("[");
            String s = Long.toBinaryString(m_storage[i]);
            if (s.length() < STORAGE_BITS) {
                char[] z = new char[STORAGE_BITS - s.length()];
                Arrays.fill(z, '0');
                result.append(z);
            }
            result.append(s);
            result.append("] ");

            result.append(((i + 1) * STORAGE_BITS) - 1);
            result.append("-");
            result.append(i * STORAGE_BITS);
            result.append("\n");
        }
        return result.toString();
    }

}
