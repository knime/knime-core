/* 
 * 
 * -------------------------------------------------------------------
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
 *   Jan 13, 2006 (wiswedel): created
 */
package org.knime.base.data.bitvector;

import java.util.BitSet;

import org.knime.base.data.replace.ReplacedCellFactory;
import org.knime.base.data.replace.ReplacedColumnsTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;

/**
 * Factory class to transfrom a {@link org.knime.core.data.StringValue}
 * cell into a {@link BitVectorCell}.
 * 
 * @see ReplacedColumnsTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class Hex2BitVectorCellFactory extends ReplacedCellFactory {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Hex2BitVectorCellFactory.class);

    /**
     * If a warning message has been printed, further will be debug messages.
     */
    private boolean m_hasPrintedWarning = false;

    private int m_nrOfSetBits = 0;

    private int m_nrOfNotSetBits = 0;

    /**
     * @see ReplacedCellFactory#getReplacement(DataRow, int)
     */
    @Override
    public DataCell getReplacement(final DataRow row, final int column) {
        DataCell old = row.getCell(column);
        if (old.isMissing()) {
            return DataType.getMissingCell();
        }
        if (old instanceof StringValue) {
            String val = ((StringValue)old).getStringValue();
            DataCell newCell;
            try {
                newCell = new BitVectorCell(((StringValue)old)
                        .getStringValue());
                BitSet set = ((BitVectorCell)newCell).getBitSet();
                m_nrOfSetBits += set.cardinality();
                m_nrOfNotSetBits += ((BitVectorCell)newCell).getNumBits()
                        - set.cardinality();
            } catch (NumberFormatException nfe) {
                String message = "Unable to convert \"" + val + "\" to "
                        + "bit vector: " + nfe.getMessage();
                if (m_hasPrintedWarning) {
                    LOGGER.debug(message);
                } else {
                    LOGGER.warn(message + " (Suppress further warnings!)", nfe);
                    m_hasPrintedWarning = true;
                }
                newCell = DataType.getMissingCell();
            }
            return newCell;
        } else {
            LOGGER.debug("Unable to convert \"" + old + "\" to bit vector, "
                    + "not a string value cell.");
            return DataType.getMissingCell();
        }
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
