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
 * ---------------------------------------------------------------------
 *
 * History
 *   04.11.2008 (ohl): created
 */
package org.knime.core.data.vector.bytevector;

import org.knime.core.data.DataCell;

/**
 * Used to created {@link DataCell}s holding a {@link DenseByteVector}. As
 * data cells are read only this factory can be used to initialize the byte
 * vector accordingly and then create a data cell from it. <br />
 * This factory also provides methods for performing basic operations
 * ({@link #min(ByteVectorValue, ByteVectorValue)},
 * {@link #max(ByteVectorValue, ByteVectorValue)}, etc.) on two data cells
 * holding byte vectors.
 *
 * @author ohl, University of Konstanz
 */
public class DenseByteVectorCellFactory {

    private DenseByteVector m_vector;

    /**
     * Initializes the factory to the specified length, all counts set to zero.
     *
     * @param length the length of the count vector
     */
    public DenseByteVectorCellFactory(final int length) {
        m_vector = new DenseByteVector(length);
    }

    /**
     * A copy of the specified vector is stored in the created byte vector cell.
     *
     * @param vector used to initialize the counts.
     */
    public DenseByteVectorCellFactory(final DenseByteVector vector) {
        m_vector = new DenseByteVector(vector);
    }

    /**
     * Initializes the vector from a subsequence of the specified cell. The
     * bytes used are the ones from <code>startIdx</code> to
     * <code>endIdx - 1</code>. The length of the resulting vector is
     * <code>startIdx - endIdx</code>.
     *
     * @param cell the byte vector cell to take the subsequence from.
     * @param startIdx the first byte to include in the created bit vector
     * @param endIdx the first byte NOT to include in the result vector
     *
     */
    public DenseByteVectorCellFactory(final DenseByteVectorCell cell,
            final int startIdx, final int endIdx) {
        m_vector = cell.getByteVectorCopy().subSequence(startIdx, endIdx);
    }

    /**
     * Appends the argument at the end of the vector currently stored in the
     * factory. The new length is the sum of the current and the argument
     * vector. The bytes of the argument are inserted at the highest positions,
     * beyond the current vector's length.
     *
     * @param bvCell the data cell containing the vector to concatenate with
     *            this.
     */
    public void concatenate(final DenseByteVectorCell bvCell) {
        m_vector = m_vector.concatenate(bvCell.getByteVectorCopy());
    }

    /**
     * Inserts the bytes at the lower positions of the current vector. The bytes
     * currently stored are shifted to higher positions. The new length is the
     * sum of the current and the argument vector.
     *
     * @param bvCell the data cell containing the vector to insert into this
     */
    public void insert(final DenseByteVectorCell bvCell) {
        m_vector = bvCell.getByteVectorCopy().concatenate(m_vector);
    }

    /**
     * Sets the new count value at the specified index.
     *
     * @param byteIndex the index of the count value to change.
     * @param byteVal the new value for the specified position.
     * @throws ArrayIndexOutOfBoundsException if the index is negative or larger
     *             than or equal to the length of the vector.
     * @throws IllegalArgumentException if the count is negative or larger than
     *             255
     */
    public void set(final int byteIndex, final byte byteVal) {
        m_vector.set(byteIndex, byteVal);
    }

    /**
     * Creates a dense byte vector cell containing the result of the min
     * operation on the passed operands (that is each position holds the minimum
     * of the values of the operands). The length of the result vector is the
     * maximum of the lengths of the operands.<br />
     * NOTE: This method performs best, if the two arguments are both
     * {@link DenseByteVectorCell}s. All other implementations need to access
     * the byte counts through get/set methods and will probably perform poorly.
     * <br />
     * See also
     * {@link SparseByteVectorCellFactory#min(ByteVectorValue, ByteVectorValue)}
     * for calculating the minimum on sparce byte vector cells.
     *
     * @param bv1 the first operand to build the minimum with the second
     * @param bv2 the second operand to build the minimum with the first one
     * @return the result of the min operation
     */
    public static DenseByteVectorCell min(final ByteVectorValue bv1,
            final ByteVectorValue bv2) {
        if (bv1 instanceof DenseByteVectorCell
                && bv2 instanceof DenseByteVectorCell) {
            DenseByteVectorCell cell1 = (DenseByteVectorCell)bv1;
            DenseByteVectorCell cell2 = (DenseByteVectorCell)bv2;
            // TODO: don't create three new instances...
            return new DenseByteVectorCell(cell1.getByteVectorCopy().min(
                    cell2.getByteVectorCopy()));
        }

        if (bv1.length() >= Integer.MAX_VALUE
                || bv2.length() >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Operand is too long to create"
                    + " a dense byte vector from.");
        }
        DenseByteVector result =
                new DenseByteVector((int)Math.max(bv1.length(), bv2.length()));
        long bv1Idx = bv1.nextCountIndex(0);
        long bv2Idx = bv2.nextCountIndex(0);
        while (bv1Idx >= 0 && bv2Idx >= 0) {
            if (bv1Idx == bv2Idx) {
                // vectors have a count at the same index - so will the result
                result.set((int)bv1Idx, Math.min(bv1.get(bv1Idx), bv2
                        .get(bv2Idx)));
            }
            // if one if the vectors is zero, the result stays zero
            if (bv1Idx <= bv2Idx) {
                bv1Idx = bv1.nextCountIndex(bv1Idx + 1);
            }
            if (bv1Idx >= bv2Idx) {
                bv2Idx = bv2.nextCountIndex(bv2Idx + 1);
            }
        }
        return new DenseByteVectorCell(result);

    }

    /**
     * Creates a dense byte vector cell containing the result of the max
     * operation on the passed operands (that is each position holds the maximum
     * of the values of the operands). The length of the result vector is the
     * maximum of the length of the operands.<br />
     * NOTE: This method performs best, if the two arguments are both
     * {@link DenseByteVectorCell}s. All other implementations need to access
     * the byte counts through get/set methods and will probably perform poorly.
     * <br />
     * See also
     * {@link SparseByteVectorCellFactory#max(ByteVectorValue, ByteVectorValue)}
     * for calculating the maximum on sparce byte vector cells.
     *
     * @param bv1 the first operand to build the maximum with the second
     * @param bv2 the second operand to build the maximum with the first one
     * @return the result of the max operation
     */
    public static DenseByteVectorCell max(final ByteVectorValue bv1,
            final ByteVectorValue bv2) {
        if (bv1 instanceof DenseByteVectorCell
                && bv2 instanceof DenseByteVectorCell) {
            DenseByteVectorCell cell1 = (DenseByteVectorCell)bv1;
            DenseByteVectorCell cell2 = (DenseByteVectorCell)bv2;
            // TODO: don't create three new instances...
            return new DenseByteVectorCell(cell1.getByteVectorCopy().max(
                    cell2.getByteVectorCopy()));
        }

        if (bv1.length() >= Integer.MAX_VALUE
                || bv2.length() >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Operand is too long to create"
                    + " a dense byte vector from.");
        }
        DenseByteVector result =
                new DenseByteVector((int)Math.max(bv1.length(), bv2.length()));
        long bv1Idx = bv1.nextCountIndex(0);
        long bv2Idx = bv2.nextCountIndex(0);
        while (bv1Idx >= 0 && bv2Idx >= 0) {
            if (bv1Idx == bv2Idx) {
                // vectors have a count at the same index - max goes in result
                result.set((int)bv1Idx, Math.max(bv1.get(bv1Idx), bv2
                        .get(bv2Idx)));
                bv1Idx = bv1.nextCountIndex(bv1Idx + 1);
                bv2Idx = bv2.nextCountIndex(bv2Idx + 1);
            }
            // if one has a count, the result gets it
            if (bv1Idx < bv2Idx) {
                result.set((int)bv1Idx, bv1.get(bv1Idx));
                bv1Idx = bv1.nextCountIndex(bv1Idx + 1);
            }
            if (bv1Idx > bv2Idx) {
                result.set((int)bv2Idx, bv2.get(bv2Idx));
                bv2Idx = bv2.nextCountIndex(bv2Idx + 1);
            }
        }
        // take over the counts from the longer argument
        while (bv1Idx >= 0) {
            result.set((int)bv1Idx, bv1.get(bv1Idx));
            bv1Idx = bv1.nextCountIndex(bv1Idx + 1);
        }
        while (bv2Idx >= 0) {
            result.set((int)bv2Idx, bv2.get(bv2Idx));
            bv2Idx = bv2.nextCountIndex(bv2Idx + 1);
        }

        return new DenseByteVectorCell(result);

    }

    /**
     * Creates a dense byte vector cell containing the sum of the passed
     * operands (that is each position holds the sum of the values of the
     * operands). The length of the result vector is the maximum of the length
     * of the operands. If the sum of both counts is larger than the maximum
     * count (i.e. 255) the result is set to this maximum count (255) <br />
     * NOTE: This method performs best, if the two arguments are both
     * {@link DenseByteVectorCell}s. All other implementations need to access
     * the byte counts through get/set methods and will probably perform poorly.
     * <br />
     * See also
     * {@link SparseByteVectorCellFactory#sum(ByteVectorValue, ByteVectorValue)}
     * for calculating the sum on sparce byte vector cells.
     *
     * @param bv1 the first operand to build the sum with the second
     * @param bv2 the second operand to build the sum with the first one
     * @return the result of the add operation
     */
    public static DenseByteVectorCell sum(final ByteVectorValue bv1,
            final ByteVectorValue bv2) {
        if (bv1 instanceof DenseByteVectorCell
                && bv2 instanceof DenseByteVectorCell) {
            DenseByteVectorCell cell1 = (DenseByteVectorCell)bv1;
            DenseByteVectorCell cell2 = (DenseByteVectorCell)bv2;
            // TODO: don't create three new instances...
            return new DenseByteVectorCell(cell1.getByteVectorCopy().add(
                    cell2.getByteVectorCopy(), false));
        }

        if (bv1.length() >= Integer.MAX_VALUE
                || bv2.length() >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Operand is too long to create"
                    + " a dense byte vector from.");
        }
        DenseByteVector result =
                new DenseByteVector((int)Math.max(bv1.length(), bv2.length()));
        long bv1Idx = bv1.nextCountIndex(0);
        long bv2Idx = bv2.nextCountIndex(0);
        while (bv1Idx >= 0 && bv2Idx >= 0) {
            if (bv1Idx == bv2Idx) {
                // vectors have a count at the same index - sum goes in result
                result.set((int)bv1Idx, Math.min(255, bv1.get(bv1Idx)
                        + bv2.get(bv2Idx)));
                bv1Idx = bv1.nextCountIndex(bv1Idx + 1);
                bv2Idx = bv2.nextCountIndex(bv2Idx + 1);
            }
            // if only one has a count, the result gets it
            if (bv1Idx < bv2Idx) {
                result.set((int)bv1Idx, bv1.get(bv1Idx));
                bv1Idx = bv1.nextCountIndex(bv1Idx + 1);
            }
            if (bv1Idx > bv2Idx) {
                result.set((int)bv2Idx, bv2.get(bv2Idx));
                bv2Idx = bv2.nextCountIndex(bv2Idx + 1);
            }
        }
        // take over the counts from the longer argument
        while (bv1Idx >= 0) {
            result.set((int)bv1Idx, bv1.get(bv1Idx));
            bv1Idx = bv1.nextCountIndex(bv1Idx + 1);
        }
        while (bv2Idx >= 0) {
            result.set((int)bv2Idx, bv2.get(bv2Idx));
            bv2Idx = bv2.nextCountIndex(bv2Idx + 1);
        }

        return new DenseByteVectorCell(result);

    }

    /**
     * Creates a {@link DataCell} from the currently stored byte vector.
     *
     * @return a {@link DataCell} containing the current value of the vector
     */
    public DenseByteVectorCell createDataCell() {
        return new DenseByteVectorCell(m_vector);
    }
}
