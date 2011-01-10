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
 * ---------------------------------------------------------------------
 *
 * History
 *   29.08.2008 (ohl): created
 */
package org.knime.core.data.vector.bytevector;

import java.util.Arrays;

import org.knime.core.data.vector.bitvector.SparseBitVector;

/**
 * A vector of fixed length holding byte counts at specific positions. Only
 * positive values of counts are supported. Each index can store a number
 * between 0 and 255 (both inclusive). Attempts to store negative numbers or
 * numbers larger than 255 cause an exception. This implementation stores only
 * the counts not equal to zero, thus it is suitable for large and sparsely
 * populated vectors. <br />
 * The maximum length is {@link Long#MAX_VALUE}(i.e. 9223372036854775807). The
 * maximum number of counts larger than zero that can be stored is
 * {@link Integer#MAX_VALUE} (i.e. 2147483647).<br />
 * The implementation is not thread-safe.
 *
 * @author ohl, University of Konstanz
 */
public class SparseByteVector {

    // make sure the length of the array is always a power of 2.
    private long[] m_idxStorage;

    /*
     * the corresponding counts. Count value stored at m_count[i] belongs to
     * vector index stored in m_idxStorage[i].
     */
    private byte[] m_count;

    // the index of the last used position in the arrays. -1 if no count is set.
    private int m_lastIdx;

    private final long m_length;

    /**
     * Creates a new vector with (initially) space for 64 counts and of the
     * specified length.
     *
     * @param length the length of the vector to create
     */
    public SparseByteVector(final long length) {
        this(length, 64);
    }

    /**
     * Creates a new vector of the specified length and with (initially) space
     * for the specified number of counts.
     *
     * @param length the length of the vector to create
     * @param initialCapacity space will be allocated to store that many numbers
     */
    public SparseByteVector(final long length, final int initialCapacity) {
        if (length < 0) {
            throw new IllegalArgumentException(
                    "Length of a ByteVector can't be negative.");
        }
        m_length = length;

        // be sure to start with a power of two (and not zero!)
        int c = 1;
        while (c < initialCapacity && c < m_length) {
            c <<= 1;
        }
        m_idxStorage = new long[c];
        m_count = new byte[c];
        m_lastIdx = -1;
    }

    /**
     * Creates a new instance by taking over the initialization from the passed
     * arrays. The numbers in the first argument array
     * (<code>countIndices</code>)
     * are considered indices of the positions a number is stored at. The second
     * array (<code>counts</code>) contains the corresponding number to
     * store. Both arrays must have the same length.<br />
     * The <code>countIndices</code> array must be sorted! The lowest index
     * must be stored at array index zero. The arrays must be build like the one
     * returned by the {@link #getAllCountIndices()} and {@link #getAllCounts()}
     * methods.
     *
     * @param length the length of the vector. Indices must be smaller than this
     *            number.
     * @param countIndices the array containing the indices of the counts to
     *            store. MUST be sorted (lowest index first).
     * @param counts the numbers to store. Note, even though Java handles
     *            <code>byte</code> as signed numbers, the passed counts are
     *            interpreted as positive counts in the range of 0 ... 255.
     * @throws IllegalArgumentException if length is negative or if the array
     *             contains negative indices or indices larger than length - or
     *             if the array is not sorted or the arrays do not have the same
     *             length!
     *
     */
    public SparseByteVector(final long length, final long[] countIndices,
            final byte[] counts) {
        this(length, countIndices.length);

        if (length < countIndices.length) {
            throw new IllegalArgumentException("Can't init a vector of length "
                    + length + " with " + countIndices.length + " counts.");
        }
        if (countIndices.length != counts.length) {
            throw new IllegalArgumentException("The initialization arrays must"
                    + " have the same length");
        }
        int idx = -1;
        long lastVal = -1;
        int maxArrayIdx = countIndices.length - 1;

        while (idx < maxArrayIdx) {
            idx++;
            if (countIndices[idx] >= length) {
                throw new IllegalArgumentException("Initialization array"
                        + " contains index out range at array index " + idx
                        + " (vector length=" + length + ", index="
                        + countIndices[idx] + ")");
            }
            if (countIndices[idx] < 0) {
                throw new IllegalArgumentException("Initialization array"
                        + " contains a negative index at array index " + idx
                        + "(index=" + countIndices[idx] + ")");
            }
            if (countIndices[idx] <= lastVal) {
                throw new IllegalArgumentException("Initialization array"
                        + " is not sorted at array index " + idx
                        + " (previousVal=" + lastVal + ", indexVal="
                        + countIndices[idx] + ")");
            }

            m_idxStorage[idx] = countIndices[idx];
            m_count[idx] = counts[idx];

            lastVal = countIndices[idx];
        }

        m_lastIdx = idx;

    }

    /**
     * Creates a clone of the passed vector.
     *
     * @param byteVector the vector to clone.
     */
    public SparseByteVector(final SparseByteVector byteVector) {
        m_count = byteVector.m_count;
        m_idxStorage = byteVector.m_idxStorage;
        m_lastIdx = byteVector.m_lastIdx;
        m_length = byteVector.m_length;
        assert checkConsistency() == null;
    }

    /**
     * Returns the number of numbers stored in this vector.
     *
     * @return the length of the vector.
     */
    public long length() {
        return m_length;
    }

    /**
     * Frees unused memory in the vector. If a vector loses a lot of ones the
     * used storage could be reduced (as only the indices of ones are stores).
     */
    public void shrink() {
        int newLength = m_idxStorage.length;
        if (m_lastIdx <= 0) {
            // never create a zero-length array (wouldn't grow anymore!)
            newLength = 1;
        } else {
            assert Long.bitCount(newLength) == 1;
            while (newLength > m_lastIdx) {
                newLength >>>= 1;
            }
            newLength <<= 1;
        }
        if (newLength < m_idxStorage.length) {
            m_idxStorage = Arrays.copyOf(m_idxStorage, newLength);
            m_count = Arrays.copyOf(m_count, newLength);
        }
    }

    /**
     * Stores the number at the specified index.
     *
     * @param index the index of the position where the count will be stored.
     * @param value the number to store at the specified index. Must be in the
     *            range of 0 ... 255.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than the size of the vector.
     * @throws IllegalArgumentException if the specified value is negative or
     *             larger than 255.
     */
    public void set(final long index, final int value) {
        if (value == 0) {
            // zero is different - needs to remove count and index
            clear(index);
        }

        assert (checkConsistency() == null);
        if (index >= m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + index
                    + "') too large for vector of length " + m_length);
        }
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("Index can't be negative");
        }
        int storageIdx;
        if (m_lastIdx == -1 || m_idxStorage[0] > index) {
            // no count stored yet - or only at higher indices
            storageIdx = 0;
        } else if (index > m_idxStorage[m_lastIdx]) {
            // might be faster when they set the counts in order
            storageIdx = m_lastIdx + 1;
        } else {
            storageIdx =
                    Arrays.binarySearch(m_idxStorage, 0, m_lastIdx + 1, index);
            if (storageIdx >= 0) {
                // already set - override it
                m_count[storageIdx] = (byte)value;
                return;
            }
            // the position is not set yet - insert it
            storageIdx = -(storageIdx + 1);
        }

        m_lastIdx++;
        long[] newStorage = m_idxStorage;
        byte[] newCount = m_count;

        // ensure capacity
        if (m_idxStorage.length <= m_lastIdx) {
            if (m_idxStorage.length == Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "The capacity of the sparce bit vector is exceeded."
                                + " Too many numbers stored.");
            }
            assert m_idxStorage.length > 0;
            newStorage = new long[m_idxStorage.length << 1];
            newCount = new byte[m_idxStorage.length << 1];
            // copy the content up to the new index
            System.arraycopy(m_idxStorage, 0, newStorage, 0, storageIdx);
            System.arraycopy(m_count, 0, newCount, 0, storageIdx);
        }

        // shift the old content
        System.arraycopy(m_idxStorage, storageIdx, newStorage, storageIdx + 1,
                m_lastIdx - storageIdx);
        System.arraycopy(m_count, storageIdx, newCount, storageIdx + 1,
                m_lastIdx - storageIdx);

        // insert the new count index
        newStorage[storageIdx] = index;
        newCount[storageIdx] = (byte)value;

        // in case we re-allocated
        m_idxStorage = newStorage;
        m_count = newCount;

        assert (checkConsistency() == null);
    }

    /**
     * Resets the count at the specified index (sets it to zero).
     *
     * @param index the index of the position to clear.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than the size of the vector
     */
    public void clear(final long index) {
        assert (checkConsistency() == null);
        if (index >= m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + index
                    + "') too large for vector of length " + m_length);
        }
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("Index can't be negative");
        }
        if (m_lastIdx == -1 || m_idxStorage[m_lastIdx] < index
                || m_idxStorage[0] > index) {
            // no counts stored yet - or only lower or higher indices
            return;
        }

        int storageIdx =
                Arrays.binarySearch(m_idxStorage, 0, m_lastIdx + 1, index);
        if (storageIdx < 0) {
            // nothing to clear
            return;
        }

        if (storageIdx < m_lastIdx) {
            // shift the higher content down over the count to clear
            System.arraycopy(m_idxStorage, storageIdx + 1, m_idxStorage,
                    storageIdx, m_lastIdx - storageIdx);
            System.arraycopy(m_count, storageIdx + 1, m_count, storageIdx,
                    m_lastIdx - storageIdx);
        }

        m_lastIdx--;
        assert (checkConsistency() == null);
    }

    /**
     * Returns the number of counts larger than zero stored in this vector.
     *
     * @return the number of elements not equal to zero in this vector.
     */
    public int cardinality() {
        return m_lastIdx + 1;
    }

    /**
     * Checks all counts and returns true if they are all zero.
     *
     * @return true if all counts are zero.
     */
    public boolean isEmpty() {
        return m_lastIdx == -1;
    }

    /**
     * Returns the number stored at the specified index.
     *
     * @param index the index of the number to return.
     * @return the number (in the range of 0 ... 255) stored at the specified
     *         index.
     * @throws ArrayIndexOutOfBoundsException if the index is larger than the
     *             length of the vector
     */
    public int get(final long index) {
        assert (checkConsistency() == null);
        if (index >= m_length) {
            throw new ArrayIndexOutOfBoundsException("Index ('" + index
                    + "') too large for vector of length " + m_length);
        }
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("Index can't be negative");
        }
        if (m_lastIdx == -1 || m_idxStorage[0] > index
                || m_idxStorage[m_lastIdx] < index) {
            // no value set yet - or only higher or lower indices
            return 0;
        }
        int storageIdx =
                Arrays.binarySearch(m_idxStorage, 0, m_lastIdx + 1, index);
        if (storageIdx < 0) {
            // no count set at that position
            return 0;
        } else {
            // avoid sign extension
            return m_count[storageIdx] & 0x0FF;
        }
    }

    /**
     * Finds the next count not equal to zero on or after the specified index.
     * Returns an index larger than or equal the provided index, or -1 if no
     * count larger than zero exists after the startIdx. (This is the only
     * method (and the #nextZeroIndex) where it is okay to pass an index larger
     * than the length of the vector.)
     *
     * @param startIdx the first index to look for non-zero counts. (It is
     *            allowed to pass an index larger then the vector's length.)
     * @return the index of the next count larger than zero, which is on or
     *         after the provided startIdx, or -1 if there isn't any
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx is
     *             negative
     */
    public long nextCountIndex(final long startIdx) {
        if (startIdx < 0) {
            throw new ArrayIndexOutOfBoundsException("Starting index"
                    + " can't be negative");
        }
        if (m_lastIdx < 0 || m_idxStorage[m_lastIdx] < startIdx) {
            // no counts stored - or none above or on startIdx
            return -1;
        }
        if (startIdx <= m_idxStorage[0]) {
            return m_idxStorage[0];
        }

        int startAddr =
                Arrays.binarySearch(m_idxStorage, 0, m_lastIdx + 1, startIdx);
        if (startAddr < 0) {
            startAddr = -(startAddr + 1);
            if (startAddr == m_lastIdx + 1) {
                // there are only smaller bit addresses in the storage
                return -1;
            }
            // otherwise it points to the "insert point"
            return m_idxStorage[startAddr];
        } else {
            // found the startIdx.
            return startIdx;
        }
    }

    /**
     * Finds the next index whose value is zero on or after the specified index.
     * Returns an index larger than or equal the provided index, or -1 if no
     * such index exists. (This is the only method (and the #nextCountIndex)
     * where it is okay to pass an index larger than the length of the vector.)
     *
     * @param startIdx the first index to look for zero values.
     * @return the index of the next index with value zero, which is on or after
     *         the provided startIdx. Or -1 if the vector contains no zeros
     *         there after.
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx negative
     */
    public long nextZeroIndex(final long startIdx) {
        if (startIdx < 0) {
            throw new ArrayIndexOutOfBoundsException("Starting index"
                    + " can't be negative");
        }
        if (m_lastIdx < 0 || startIdx >= m_length) {
            return -1;
        }
        if (m_idxStorage[m_lastIdx] < startIdx || startIdx < m_idxStorage[0]) {
            // startIdx outside the non-zero range
            return startIdx;
        }

        int startAddr =
                Arrays.binarySearch(m_idxStorage, 0, m_lastIdx + 1, startIdx);

        if (startAddr < 0) {
            // startIdx not contained - return it as zero address
            return startIdx;
        }

        long idx = m_idxStorage[startAddr];
        // now find the next index not in the storage
        while (startAddr < m_lastIdx) {
            idx++;
            startAddr++;
            if (idx != m_idxStorage[startAddr]) {
                return idx;
            }
        }
        // all numbers from startAddr are continuously stored
        assert (idx == m_idxStorage[m_lastIdx]);
        return idx++;
    }

    /**
     * Calculates the checksum, the sum of all counts stored.
     *
     * @return the sum of all counts in this vector.
     */
    public long sumOfAllCounts() {
        long result = 0;
        for (int i = 0; i < m_lastIdx; i++) {
            result += m_count[i] & 0x0FF; // avoid sign extension
        }
        return result;
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
    public SparseByteVector add(final SparseByteVector bv,
            final boolean remainder) {
        SparseByteVector result =
                new SparseByteVector(Math.max(m_length, bv.m_length), Math.max(
                        cardinality(), bv.cardinality()));

        int thisIdx = 0;
        int bvIdx = 0;
        while (thisIdx < m_lastIdx && bvIdx < bv.m_lastIdx) {
            // the set method is pretty fast, if we add sorted indices
            if (m_idxStorage[thisIdx] == bv.m_idxStorage[bvIdx]) {
                if (remainder) {
                    result.set(m_idxStorage[thisIdx], m_count[thisIdx]
                            + bv.m_count[bvIdx]);
                } else {
                    result.set(m_idxStorage[thisIdx], Math.min(255,
                            m_count[thisIdx] + bv.m_count[bvIdx]));
                }
                thisIdx++;
                bvIdx++;
            } else if (m_idxStorage[thisIdx] < bv.m_idxStorage[bvIdx]) {
                result.set(m_idxStorage[thisIdx], m_count[thisIdx]);
                thisIdx++;
            } else {
                result.set(bv.m_idxStorage[bvIdx], bv.m_count[bvIdx]);
                bvIdx++;
            }
        }
        // copy the longer vector into the result - only one while loop executes
        while (thisIdx < m_lastIdx) {
            result.set(m_idxStorage[thisIdx], m_count[thisIdx]);
            thisIdx++;
        }
        while (bvIdx < bv.m_lastIdx) {
            result.set(bv.m_idxStorage[bvIdx], bv.m_count[bvIdx]);
            bvIdx++;
        }

        assert result.checkConsistency() == null;
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
    public SparseByteVector min(final SparseByteVector bv) {
        SparseByteVector result =
                new SparseByteVector(Math.max(m_length, bv.m_length), Math.min(
                        cardinality(), bv.cardinality()));

        int thisIdx = 0;
        int bvIdx = 0;
        while (thisIdx < m_lastIdx && bvIdx < bv.m_lastIdx) {
            // the set method is pretty fast, if we add sorted indices
            if (m_idxStorage[thisIdx] == bv.m_idxStorage[bvIdx]) {
                result.set(m_idxStorage[thisIdx], Math.min(m_count[thisIdx],
                        bv.m_count[bvIdx]));
                thisIdx++;
                bvIdx++;
            } else if (m_idxStorage[thisIdx] < bv.m_idxStorage[bvIdx]) {
                thisIdx++;
            } else {
                bvIdx++;
            }
        }
        // the other indices are zero

        assert result.checkConsistency() == null;
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
    public SparseByteVector max(final SparseByteVector bv) {
        SparseByteVector result =
                new SparseByteVector(Math.max(m_length, bv.m_length), Math.max(
                        cardinality(), bv.cardinality()));

        int thisIdx = 0;
        int bvIdx = 0;
        while (thisIdx < m_lastIdx && bvIdx < bv.m_lastIdx) {
            // the set method is pretty fast, if we add sorted indices
            if (m_idxStorage[thisIdx] == bv.m_idxStorage[bvIdx]) {
                result.set(m_idxStorage[thisIdx], Math.max(m_count[thisIdx],
                        bv.m_count[bvIdx]));
                thisIdx++;
                bvIdx++;
            } else if (m_idxStorage[thisIdx] < bv.m_idxStorage[bvIdx]) {
                result.set(m_idxStorage[thisIdx], m_count[thisIdx]);
                thisIdx++;
            } else {
                result.set(bv.m_idxStorage[bvIdx], bv.m_count[bvIdx]);
                bvIdx++;
            }
        }
        // copy the longer vector into the result - only one loop executes
        while (thisIdx < m_lastIdx) {
            result.set(m_idxStorage[thisIdx], m_count[thisIdx]);
            thisIdx++;
        }
        while (bvIdx < bv.m_lastIdx) {
            result.set(bv.m_idxStorage[bvIdx], bv.m_count[bvIdx]);
            bvIdx++;
        }

        assert result.checkConsistency() == null;
        return result;
    }

    /**
     * Creates and returns a new byte vector that contains copies of both (this
     * and the argument vector). The argument vector is appended at the end of
     * this vector, i.e. its value with index zero will be stored at index
     * "length-of-this-vector" in the result vector. The length of the result is
     * the length of this plus the length of the argument vector.
     *
     * @param bv the vector to append at the end of this
     * @return a new instance containing both vectors concatenated
     */
    public SparseByteVector concatenate(final SparseByteVector bv) {
        SparseByteVector result =
                new SparseByteVector(m_length + bv.m_length, cardinality()
                        + bv.cardinality());
        System
                .arraycopy(m_idxStorage, 0, result.m_idxStorage, 0,
                        m_lastIdx + 1);

        // copy bv's indices and add the length of this to each
        int resIdx = m_lastIdx + 1;
        for (int bvIdx = 0; bvIdx <= bv.m_lastIdx; bvIdx++) {
            result.m_idxStorage[resIdx] = bv.m_idxStorage[bvIdx] + m_length;
            resIdx++;
        }

        assert resIdx == m_lastIdx + bv.m_lastIdx + 1 + 1;
        result.m_lastIdx = m_lastIdx + bv.m_lastIdx + 1;

        // the values can all be just copied.
        System.arraycopy(m_count, 0, result.m_count, 0, m_lastIdx + 1);
        System.arraycopy(bv.m_count, 0, result.m_count, m_lastIdx + 1,
                bv.m_lastIdx + 1);

        assert result.checkConsistency() == null;

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
     * @param startIdx the first index included in the subsequence
     * @param endIdx the first byte in this vector after startIdx that is not
     *            included in the result sequence.
     * @return a new vector of length <code>endIdx - startIdx</code>
     *         containing the subsequence of this vector from
     *         <code>startIdx</code> (included) to <code>endIdx</code> (not
     *         included anymore).
     */
    public SparseByteVector subSequence(final long startIdx,
            final long endIdx) {
        if (startIdx < 0 || endIdx > m_length || endIdx < startIdx) {
            throw new IllegalArgumentException("Illegal range for subsequense."
                    + "(startIdx=" + startIdx + ", endIdx=" + endIdx
                    + ", length = " + m_length + ")");
        }

        SparseByteVector result = new SparseByteVector(endIdx - startIdx);

        if (m_lastIdx < 0 || startIdx == endIdx || m_idxStorage[0] >= endIdx
                || m_idxStorage[m_lastIdx] < startIdx) {
            // no bits set, or not in the specified range - or range is null
            return result;
        }

        // find the address to start copying
        int storageIdx =
                Arrays.binarySearch(m_idxStorage, 0, m_lastIdx + 1, startIdx);
        if (storageIdx < 0) {
            // it points to the next index in the array
            storageIdx = -(storageIdx + 1);
        }
        // copy the indexes
        while (storageIdx < m_lastIdx && m_idxStorage[storageIdx] < endIdx) {
            result.set(m_idxStorage[storageIdx], m_count[storageIdx] & 0x0FF);
            storageIdx++;
        }

        assert result.checkConsistency() == null;
        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = (int)(m_length ^ (m_length >>> 32));
        for (int i = 0; i <= m_lastIdx; i++) {
            long idx = m_idxStorage[i];
            long val = m_count[i];
            hash = 31 * hash + ((int)(idx ^ (idx >>> 32)));
            hash = 31 * hash + ((int)(val ^ (val >>> 32)));
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof SparseBitVector) {
            SparseByteVector s = (SparseByteVector)obj;
            if (s.m_length != m_length) {
                return false;
            }
            if (s.m_lastIdx != m_lastIdx) {
                return false;
            }
            if (m_lastIdx == -1) {
                return true;
            }
            for (int i = 0; i <= m_lastIdx; i++) {
                if (m_idxStorage[i] != s.m_idxStorage[i]) {
                    return false;
                }
                if (m_count[i] != s.m_count[i]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns a string containing (comma separated) all numbers stored in this
     * vector. The number of values added to the string is limited to 30000. If
     * the output is truncated, the string ends on &quot;... }&quot;
     *
     * @return a string containing (comma separated) the values in this vector.
     */
    @Override
    public String toString() {
        int max = (int)Math.min(30000, m_length - 1);
        StringBuilder result = new StringBuilder(max * 4);

        int storageIdx = 0;
        result.append("{");
        for (int i = 0; i < max; i++) {
            if (storageIdx <= m_lastIdx && i == m_idxStorage[storageIdx]) {
                result.append(m_count[i] & 0x0FF).append(", ");
                storageIdx++;
            } else {
                result.append("0, ");
            }
        }
        if (max < m_length) {
            result.append("... ");
        } else if (result.length() > 2) {
            result.delete(result.length() - 2, result.length());
        }
        result.append('}');
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
        if (m_length < 0) {
            return "vector's length is negative!";
        }
        if (m_lastIdx >= m_length) {
            return "m_lastIdx not less than m_length!";
        }

        if (m_count.length != m_idxStorage.length) {
            return "arrays are not of same size.";
        }

        // make sure array is sorted
        long lastVal = -1;
        for (int i = 0; i <= m_lastIdx; i++) {
            if (m_idxStorage[i] <= lastVal) {
                return "Index at position " + i + "(" + m_idxStorage[i]
                        + ") is not larger then the one at " + (i - 1) + "("
                        + lastVal + ")";
            }
            lastVal = m_idxStorage[i];
        }

        // only counts larger than zero are stored. Normally.
        for (int i = 0; i <= m_lastIdx; i++) {
            if (m_count[i] == 0) {
                return "count zero is stored at index " + m_idxStorage[i];
            }
        }

        return null;
    }

    /**
     * Returns a copy of the internal storage of all values. The array contains
     * only the values larger than zero. The position of these values in the
     * vector can be retrieved from the array returned by
     * {@link #getAllCountIndices()}. The arrays returned by these two methods
     * are of same length. The count at index i in the result array is located
     * in the vector at the index stored in the other array at the same index i.
     * Note, even though Java stores signed numbers in <code>byte</code>, the
     * returned number are values in the range of 0... 255.<br />
     * The length of the returned array is the cardinality of the vector.
     *
     * @return a copy of the internal representation of the bits in this vector.
     */
    public byte[] getAllCounts() {
        return Arrays.copyOf(m_count, m_lastIdx + 1);
    }

    /**
     * Returns a copy of the internal storage of all values. The array contains
     * the sorted indices of all '1's in the vector. The length of the returned
     * array is the cardinality of the vector.
     *
     * @return a copy of the internal representation of the bits in this vector.
     */
    public long[] getAllCountIndices() {
        return Arrays.copyOf(m_idxStorage, m_lastIdx + 1);
    }

}
