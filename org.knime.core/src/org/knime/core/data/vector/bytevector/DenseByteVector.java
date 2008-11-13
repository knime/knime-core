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
 *   25.08.2008 (ohl): created
 */
package org.knime.core.data.vector.bytevector;

import java.util.Arrays;

/**
 * A vector of fixed length holding byte counts at specific positions. Only
 * positive values of counts are supported. Each index can store a number
 * between 0 and 255 (both inclusive). Attempts to store negative numbers or
 * numbers larger than 255 cause an exception. <br />
 * The maximum length is 2147483645.<br />
 * The implementation is not thread-safe.
 *
 * @author ohl, University of Konstanz
 */
public class DenseByteVector {

    private static final int MAX_COUNT = (1 << Byte.SIZE) - 1;

    private final byte[] m_storage;

    /**
     * Creates a new instance with the specified length. All bytes are set to
     * zero.
     *
     * @param length the fixed length of the vector.
     */
    DenseByteVector(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException(
                    "Length of a ByteVector can't be negative.");
        }
        if (length == Integer.MAX_VALUE) {
            // we need MAX_VALUE internally!
            throw new IllegalArgumentException(
                    "Can't create a vector that big!");
        }
        m_storage = new byte[length];

    }

    /**
     * Creates a new vector initialized by the passed counts. The length of the
     * new vector is the length of the argument array. The passed values are
     * interpreted as unsigned positive values in the range of 0 ... 255.
     *
     * @param counts the value to initialize the new vector with.
     */
    DenseByteVector(final byte[] counts) {
        m_storage = counts.clone();
    }

    /**
     * Creates a new vector initialized by the passed vector. The created vector
     * is an identical copy.
     *
     * @param byteVector the byte vector to clone.
     */
    DenseByteVector(final DenseByteVector byteVector) {
        m_storage = byteVector.m_storage.clone();
    }

    /**
     * Returns the length of the vector.
     *
     * @return the length of the vector.
     */
    public int length() {
        return m_storage.length;
    }

    /**
     * Sets the new count value at the specified index.
     *
     * @param index the index of the count value to change.
     * @param count the new value for the specified position.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than or equal to the length of the vector.
     * @throws IllegalArgumentException if the count is negative or larger than
     *             255
     */
    public void set(final int index, final int count) {
        if (count < 0 || count > MAX_COUNT) {
            throw new IllegalArgumentException("Only values 0..." + MAX_COUNT
                    + " can be stored in the vector");
        }
        m_storage[index] = (byte)count;
    }

    /**
     * Sets the new count value at all positions from startIdx (inclusive) to
     * endIdx (exclusive).
     *
     * @param startIdx the first index to store the new value at.
     * @param endIdx the first index the new value should not be stored anymore.
     * @param count the new value that should be stored in the specified index
     *            range
     * @throws ArrayIndexOutOfBoundsException if startIdx is negative or endIdx
     *             is larger than the length of the vector.
     * @throws IllegalArgumentException if the endIdx is smaller than the
     *             startIdx, or if the specified count is negative or larger
     *             than 255.
     */
    public void fill(final int startIdx, final int endIdx, final int count) {
        if (count < 0 || count > MAX_COUNT) {
            throw new IllegalArgumentException("Only values 0..." + MAX_COUNT
                    + " can be stored in the vector");
        }
        Arrays.fill(m_storage, startIdx, endIdx, (byte)count);
    }

    /**
     * Sets the count at the specified index to zero. Equivalent to a call to
     * {@link #set(int, int)} with count zero.
     *
     * @param index the index of the position the count should be reset at.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than or equal to the length of the vector.
     */

    public void clear(final int index) {
        set(index, 0);
    }

    /**
     * Resets the count at all positions from startIdx (inclusive) to endIdx
     * (exclusive). Equivalent to a call to {@link #fill(int, int, int)} with
     * count zero.
     *
     * @param startIdx the first index to set the count to zero.
     * @param endIdx the first index the count not be reset anymore.
     * @throws ArrayIndexOutOfBoundsException if startIdx is negative or endIdx
     *             is larger than the length of the vector.
     * @throws IllegalArgumentException if the endIdx is smaller than the
     *             startIdx.
     */
    public void clear(final int startIdx, final int endIdx) {
        fill(startIdx, endIdx, 0);
    }

    /**
     * Returns the count stored at the specified position.
     *
     * @param index the index of the count to return
     * @return the count stored at the specified index.
     * @throws ArrayIndexOutOfBoundsException if the specified index is negative
     *             or too large.
     */
    public int get(final int index) {
        return m_storage[index] & 0x0FF; // avoid sign extension
    }

    /**
     * Returns the index equal to or larger than the specified index that
     * contains a count larger than zero. If no such index exists (i.e. the
     * vector contains only zeros from <code>idx</code> on) -1 is returned.
     * I.e. if a value not equal -1 is returned, a call to {@link #get(int)}
     * with the result as argument returns a number larger than zero.
     *
     * @param idx the starting index to look for the next count larger than
     *            zero. It is okay to pass an index larger than the length of
     *            this vector (in which case -1 is returned).
     * @return the next index with a count larger than zero that is equal to or
     *         larger than <code>idx</code>, or -1 if the vector contains
     *         only zeros on and after <code>idx</code>.
     */
    public int nextCountIndex(final int idx) {
        for (int i = idx; i < m_storage.length; i++) {
            if (m_storage[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index equal to or larger than the specified index that
     * contains a zero count. If no such index exists (i.e. the vector contains
     * only positive numbers from <code>idx</code> on) -1 is returned. I.e. if
     * a value not equal -1 is returned, a call to {@link #get(int)} with the
     * result as argument returns zero.
     *
     * @param idx the starting index to look for the next zero count. It is okay
     *            to pass an index larger than the length of this vector (in
     *            which case -1 is returned).
     * @return the next index with a zero count that is equal to or larger than
     *         <code>idx</code>, or -1 if the vector contains only counts
     *         larger than zero on and after <code>idx</code>.
     */
    public int nextZeroIndex(final int idx) {
        for (int i = idx; i < m_storage.length; i++) {
            if (m_storage[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a new vector with the sum of the counts at each position. The
     * result's length is the maximum of this' and the argument's length. The
     * value at position i in the result is the sum of the counts in this' and
     * the arguments vector at position i.
     *
     * @param bv the vector to add to this one (position-wise).
     * @param remainder if true and the result of the addition is larger than
     *            255, the value in the result vector will be set to the
     *            remainder when divided by 255 - if false, the result vector is
     *            set to 255 if the sum is larger than 255. (Setting it to true
     *            performs slightly better.)
     * @return a new instance holding at each position the sum of the counts.
     */
    public DenseByteVector add(final DenseByteVector bv, final boolean remainder) {
        DenseByteVector result =
                new DenseByteVector(Math.max(m_storage.length,
                        bv.m_storage.length));
        /*
         * we copy the larger array into the result and then add the shorter
         * onto it.
         */
        DenseByteVector shorter;
        DenseByteVector longer;
        if (m_storage.length < bv.m_storage.length) {
            shorter = this;
            longer = bv;
        } else {
            shorter = bv;
            longer = this;
        }
        System.arraycopy(longer.m_storage, 0, result.m_storage, 0,
                result.m_storage.length);
        if (remainder) {
            for (int i = 0; i < shorter.m_storage.length; i++) {
                result.m_storage[i] += shorter.m_storage[i];
            }
        } else {
            // result must be set to 255 if sum is too large for a byte
            for (int i = 0; i < shorter.m_storage.length; i++) {
                result.m_storage[i] =
                        (byte)Math.min(MAX_COUNT, result.m_storage[i]
                                + shorter.m_storage[i]);
            }
        }

        return result;
    }

    /**
     * Returns a new vector with the maximum of the counts at each position. The
     * result's length is the maximum of this' and the argument's length. The
     * value at position i in the result is the maximum of the counts in this'
     * and the arguments vector at position i.
     *
     * @param bv the vector to compute the maximum of (position-wise).
     * @return a new instance holding at each position the maximum of the
     *         counts.
     */
    public DenseByteVector max(final DenseByteVector bv) {

        DenseByteVector result =
                new DenseByteVector(Math.max(m_storage.length,
                        bv.m_storage.length));

        DenseByteVector longer;
        DenseByteVector shorter;
        if (m_storage.length < bv.m_storage.length) {
            shorter = this;
            longer = bv;
        } else {
            shorter = bv;
            longer = this;
        }

        for (int i = 0; i < shorter.m_storage.length; i++) {
            result.m_storage[i] =
                    longer.m_storage[i] < shorter.m_storage[i] ? shorter.m_storage[i]
                            : longer.m_storage[i];
        }

        // copy the rest of the longer one
        if (shorter.m_storage.length < longer.m_storage.length) {
            System.arraycopy(longer.m_storage, shorter.m_storage.length,
                    result.m_storage, shorter.m_storage.length,
                    longer.m_storage.length - shorter.m_storage.length);
        }
        return result;

    }

    /**
     * Returns a new vector with the minimum of the counts at each position. The
     * result's length is the maximum of this' and the argument's length. The
     * value at position i in the result is the minimum of the counts in this'
     * and the arguments vector at position i.
     *
     * @param bv the vector to compute the minimum of (position-wise).
     * @return a new instance holding at each position the minimum of the
     *         counts.
     */
    public DenseByteVector min(final DenseByteVector bv) {

        DenseByteVector result =
                new DenseByteVector(Math.max(m_storage.length,
                        bv.m_storage.length));

        int i = Math.min(bv.m_storage.length, m_storage.length) - 1;
        while (i >= 0) {
            result.m_storage[i] =
                    bv.m_storage[i] < m_storage[i] ? bv.m_storage[i]
                            : m_storage[i];

        }
        // the rest of the counts stays zero.
        return result;

    }

    /**
     * Appends the argument at the end of this vector. It creates and returns a
     * new vector whose length is the sum of this' and the argument's length.
     * The lower indices in the result contain the counts of this vector, the
     * positions with indices beyond the length of this contain the argument's
     * counts.
     *
     * @param bv the vector to concatenate with this.
     * @return a new instance containing this vector with the argument appended
     */
    public DenseByteVector concatenate(final DenseByteVector bv) {
        DenseByteVector result =
                new DenseByteVector(m_storage.length + bv.m_storage.length);
        System.arraycopy(m_storage, 0, result.m_storage, 0, m_storage.length);
        System.arraycopy(bv.m_storage, 0, result.m_storage, m_storage.length,
                bv.m_storage.length);
        return result;
    }

    /**
     * Creates and returns a new byte vector that contains a subsequence of this
     * vector, beginning with the byte at index <code>startIdx</code> and with
     * its last byte being this' byte at position <code>endIdx - 1</code>.
     * The length of the result vector is <code>endIdx - startIdx</code>. If
     * <code>startIdx</code> equals <code>endIdx</code> a vector of length
     * zero is returned.
     *
     * @param startIdx the startIdx of the subsequence
     * @param endIdx the first byte in this vector after startIdx that is not
     *            included in the result sequence.
     * @return a new vector of length <code>endIdx - startIdx</code>
     *         containing the subsequence of this vector from
     *         <code>startIdx</code> (included) to <code>endIdx</code> (not
     *         included anymore).
     */
    public DenseByteVector subSequence(final int startIdx, final int endIdx) {
        if (startIdx < 0 || endIdx > m_storage.length || endIdx < startIdx) {
            throw new IllegalArgumentException("Illegal range for subsequense."
                    + "(startIdx=" + startIdx + ", endIdx=" + endIdx
                    + ", length = " + m_storage.length + ")");
        }

        DenseByteVector result = new DenseByteVector(endIdx - startIdx);
        if (endIdx == startIdx) {
            return result;
        }
        System.arraycopy(m_storage, startIdx, result.m_storage, 0,
                result.m_storage.length);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(m_storage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DenseByteVector) {
            DenseByteVector bv = ((DenseByteVector)obj);
            return Arrays.equals(m_storage, bv.m_storage);
        }
        return false;
    }

    /**
     * Creates a string containing all counts of this vector (in braces ({ })
     * and comma separated), starting (on the left) with index 0. If the vector
     * is larger than 30000, the result is truncated (and ends with &quot;...
     * }&quot; then).
     *
     * @return a string containing all counts of this vector
     */
    @Override
    public String toString() {
        int max = Math.min(30000, m_storage.length);
        StringBuilder result = new StringBuilder(max * 5);
        result.append("{");
        for (int i = 0; i < max; i++) {
            result.append(m_storage[i] & 0x0FF).append(", ");
        }
        if (max < m_storage.length) {
            result.append("... ");
        } else if (result.length() > 2) {
            result.delete(result.length() - 2, result.length());
        }
        result.append('}');
        return result.toString();
    }

    /**
     * Returns a copy of the internal array of counts. The length of the
     * returned array is the same than this vector's length. Note, byte values
     * are signed in Java, while the values stored in the vector are positive
     * counts. Consider using {@link #getAllCounts()}, which returns an int
     * array only holding positive values from 0 ... 255.
     *
     * @return a copy of the internal byte array.
     */
    public byte[] getAllCountsAsBytes() {
        return m_storage.clone();
    }

    /**
     * Returns a copy of the internal array of counts. The returned array has
     * the same length as this vector and holds values 0 ... 255
     *
     * @return a copy of the internal byte array.
     */
    public int[] getAllCounts() {
        int[] result = new int[m_storage.length];
        for (int i = 0; i < m_storage.length; i++) {
            result[i] = m_storage[i] & 0x0FF; // avoid sign extension
        }
        return result;
    }

    /**
     * Calculates the checksum, the sum of all counts stored.
     *
     * @return the sum of all counts in this vector.
     */
    public long sumOfAllCounts() {
        long result = 0;
        for (byte b : m_storage) {
            result += b & 0x0FF; // avoid sign extension
        }
        return result;
    }

    /**
     * Returns the number of counts larger than zero stored in this vector.
     *
     * @return the number of elements not equal to zero in this vector.
     */
    public int cardinality() {
        int result = 0;
        for (byte b : m_storage) {
            result += (b == 0) ? 0 : 1;
        }
        return result;
    }

    /**
     * Checks all counts and returns true if they are all zero.
     *
     * @return true if all counts are zero.
     */
    public boolean isEmpty() {
        for (int i = 0; i < m_storage.length; i++) {
            if (m_storage[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
