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
 *   06.11.2008 (thiel): created
 */
package org.knime.core.data.vector.bitvector;

/**
 * A utility class providing methods to apply set operations like "AND", "OR",
 * and "XOR" on different kind of {@link BitVectorValue}s, such as
 * {@link SparseBitVectorCell}s or {@link DenseBitVectorCell}s in a convenient
 * way.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public final class BitVectorUtil {

    private BitVectorUtil() { /*empty*/ }

    /**
     * Creates a sparse bit vector cell, in case that one or both given values
     * are sparse bit vector cells (otherwise a dense bit vector cell).
     * The returned cell contains the result of the AND operation on the passed
     * operands. The length of the result vector is the maximum of the lengths
     * of the operands.<br />
     * NOTE: This method performs best if the two arguments are both
     * {@link SparseBitVectorCell}s or {@link DenseBitVectorCell}s.
     * All other implementations need to access the bits through get/set
     * methods which probably performs very poorly.<br />
     *
     * To perform the AND operation the sparse implementation
     * {@link SparseBitVectorCellFactory#and(BitVectorValue, BitVectorValue)},
     * or the dense implementation
     * {@link DenseBitVectorCellFactory#and(BitVectorValue, BitVectorValue)}
     * is called.
     *
     * @param bv1 the first operand to AND with the other
     * @param bv2 the other operand to AND with the first one
     * @return the result of the AND operation
     */
    public static BitVectorValue and(final BitVectorValue bv1,
            final BitVectorValue bv2) {
        if (bv1 == null || bv2 == null) {
            throw new NullPointerException(
                    "Given BitVectorValues may not be null!");
        }

        int noSparseBVC = sparseBitVectorCellCount(bv1, bv2);
        if (noSparseBVC >= 1) {
            return SparseBitVectorCellFactory.and(bv1, bv2);
        }
        return DenseBitVectorCellFactory.and(bv1, bv2);
    }

    /**
     * Creates a sparse bit vector cell, in case that both given values
     * are sparse bit vector cells (otherwise a dense bit vector cell).
     * The returned cell contains the result of the OR operation on the passed
     * operands. The length of the result vector is the maximum of the lengths
     * of the operands.<br />
     * NOTE: This method performs best if the two arguments are both
     * {@link SparseBitVectorCell}s or {@link DenseBitVectorCell}s.
     * All other implementations need to access the bits through get/set
     * methods which probably performs very poorly.<br />
     *
     * To perform the OR operation the sparse implementation
     * {@link SparseBitVectorCellFactory#or(BitVectorValue, BitVectorValue)},
     * or the dense implementation
     * {@link DenseBitVectorCellFactory#or(BitVectorValue, BitVectorValue)}
     * is called.
     *
     * @param bv1 the first operand to OR with the other
     * @param bv2 the other operand to OR with the first one
     * @return the result of the OR operation
     */
    public static BitVectorValue or(final BitVectorValue bv1,
            final BitVectorValue bv2) {
        if (bv1 == null || bv2 == null) {
            throw new NullPointerException(
                    "Given BitVectorValues may not be null!");
        }

        int noSparseBVC = sparseBitVectorCellCount(bv1, bv2);
        if (noSparseBVC == 2) {
            return SparseBitVectorCellFactory.or(bv1, bv2);
        }
        return DenseBitVectorCellFactory.or(bv1, bv2);
    }

    /**
     * Creates a sparse bit vector cell, in case that both given values
     * are sparse bit vector cells (otherwise a dense bit vector cell).
     * The returned cell contains the result of the XOR operation on the passed
     * operands. The length of the result vector is the maximum of the lengths
     * of the operands.<br />
     * NOTE: This method performs best if the two arguments are both
     * {@link SparseBitVectorCell}s or {@link DenseBitVectorCell}s.
     * All other implementations need to access the bits through get/set
     * methods which probably performs very poorly.<br />
     *
     * To perform the XOR operation the sparse implementation
     * {@link SparseBitVectorCellFactory#xor(BitVectorValue, BitVectorValue)},
     * or the dense implementation
     * {@link DenseBitVectorCellFactory#xor(BitVectorValue, BitVectorValue)}
     * is called.
     *
     * @param bv1 the first operand to XOR with the other
     * @param bv2 the other operand to XOR with the first one
     * @return the result of the XOR operation
     */
    public static BitVectorValue xor(final BitVectorValue bv1,
            final BitVectorValue bv2) {
        if (bv1 == null || bv2 == null) {
            throw new NullPointerException(
                    "Given BitVectorValues may not be null!");
        }

        int noSparseBVC = sparseBitVectorCellCount(bv1, bv2);
        if (noSparseBVC == 2) {
            return SparseBitVectorCellFactory.xor(bv1, bv2);
        }
        return DenseBitVectorCellFactory.xor(bv1, bv2);
    }

    private static int sparseBitVectorCellCount(final BitVectorValue bv1,
            final BitVectorValue bv2) {
        int count = 0;
        if (bv1 instanceof SparseBitVectorCell) {
            count++;
        }
        if (bv2 instanceof SparseBitVectorCell) {
            count++;
        }
        return count;
    }
}
