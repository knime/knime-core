/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
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
        if (bv1 == null && bv2 == null) {
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
        if (bv1 == null && bv2 == null) {
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
        if (bv1 == null && bv2 == null) {
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
