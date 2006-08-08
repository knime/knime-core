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

import org.knime.base.data.replace.ReplacedCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class IdString2BitVectorCellFactory extends ReplacedCellFactory {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(IdString2BitVectorCellFactory.class);

    private int m_nrOfSetBits = 0;

    private int m_nrOfNotSetBits = 0;

    /**
     * @see ReplacedCellFactory#getReplacement(
     *      org.knime.core.data.DataRow, int)
     */
    @Override
    public DataCell getReplacement(final DataRow row, final int column) {
        if (!row.getCell(column).getType().isCompatible(StringValue.class)) {
            LOGGER.warn(row.getCell(column) + " is not a String value!"
                    + " Replacing it with missing value!");
            return DataType.getMissingCell();
        }
        BitSet currBitSet = new BitSet();
        int maxPos = Integer.MIN_VALUE;
        String toParse = ((StringValue)row.getCell(column)).getStringValue();
        String[] numbers = toParse.split("\\s");
        for (int i = 0; i < numbers.length; i++) {
            int pos = Integer.parseInt(numbers[i].trim());
            maxPos = Math.max(maxPos, pos);
            currBitSet.set(pos);
        }
        m_nrOfSetBits += numbers.length;
        m_nrOfNotSetBits += maxPos - m_nrOfSetBits;
        return new BitVectorCell(currBitSet, maxPos);
    }

    /**
     * 
     * @return the number of set bits.
     */
    public int getNumberOfSetBits() {
        return m_nrOfSetBits;
    }

    /**
     * 
     * @return the number of not set bits.
     */
    public int getNumberOfNotSetBits() {
        return m_nrOfNotSetBits;
    }
}
