/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 * History
 *   11.07.2006 (Fabian Dill): created
 */
package org.knime.base.data.bitvector;

import java.util.BitSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BitString2BitVectorCellFactory extends BitVectorCellFactory {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BitString2BitVectorCellFactory.class);

    private int m_nrOfSetBits = 0;

    private int m_nrOfNotSetBits = 0;

    /**
     * @see BitVectorCellFactory#getReplacement(
     *      org.knime.core.data.DataRow, int)
     */
    @Override
    public DataCell getReplacement(final DataRow row, final int column) {
        BitSet bitSet = new BitSet();
        DataCell newCell = DataType.getMissingCell();
        for (DataCell cell : row) {
            if (!cell.getType().isCompatible(StringValue.class)) {
                LOGGER.warn(cell + " is not a String! "
                        + "Replacing it with missing value");
                return newCell;
            } else {
                String bitString = ((StringValue)cell).getStringValue();
                bitSet = new BitSet(bitString.length());
                int pos = 0;
                for (int i = 0; i < bitString.length(); i++) {
                    char c = bitString.charAt(i);
                    if (c == '0') {
                        pos++;
                        m_nrOfNotSetBits++;
                    } else if (c == '1') {
                        bitSet.set(pos++);
                        m_nrOfSetBits++;
                    } else {
                        LOGGER.warn("Found invalid character: " + c
                                + ". Returning missing cell!");
                        return newCell;

                    }
                }
            }
        }
        return new BitVectorCell(bitSet, bitSet.size());
    }

    /**
     * 
     * @return the number of se bits.
     */
    @Override
    public int getNumberOfSetBits() {
        return m_nrOfSetBits;
    }

    /**
     * 
     * @return the number of not set bits.
     */
    @Override
    public int getNumberOfNotSetBits() {
        return m_nrOfNotSetBits;
    }
}
