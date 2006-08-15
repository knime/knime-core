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
public class IdString2BitVectorCellFactory extends BitVectorCellFactory {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(IdString2BitVectorCellFactory.class);

    private int m_nrOfSetBits = 0;
    
    private int m_processedRows = 0;
    
    private int m_maxPos = Integer.MIN_VALUE;

    /**
     * @see BitVectorCellFactory#getReplacement(
     *      org.knime.core.data.DataRow, int)
     */
    @Override
    public DataCell getReplacement(final DataRow row, final int column) {
        if (!row.getCell(column).getType().isCompatible(StringValue.class)) {
            LOGGER.warn(row.getCell(column) + " is not a String value!"
                    + " Replacing it with missing value!");
            return DataType.getMissingCell();
        }
        m_processedRows++;
        BitSet currBitSet = new BitSet();
        String toParse = ((StringValue)row.getCell(column)).getStringValue();
        String[] numbers = toParse.split("\\s");
        for (int i = 0; i < numbers.length; i++) {
            int pos = Integer.parseInt(numbers[i].trim());
            m_maxPos = Math.max(m_maxPos, pos);
            currBitSet.set(pos);
        }
        m_nrOfSetBits += numbers.length;
        return new BitVectorCell(currBitSet, m_maxPos);
    }

    /**
     * 
     * @return the number of set bits.
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
        // processedrows * m_maxPos = all possible positions
        int allPositions = m_processedRows * m_maxPos;
        return allPositions - m_nrOfSetBits;
    }
}
