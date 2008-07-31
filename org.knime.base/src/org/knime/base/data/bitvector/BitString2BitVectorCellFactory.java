/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   11.07.2006 (Fabian Dill): created
 */
package org.knime.base.data.bitvector;

import java.util.BitSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BitString2BitVectorCellFactory extends BitVectorColumnCellFactory {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BitString2BitVectorCellFactory.class);

    private int m_nrOfSetBits = 0;

    private int m_nrOfNotSetBits = 0;

    private boolean m_hasPrintedWarning = false;
    
    private boolean m_wasSuccessful = false;

    /**
     * Create new cell factory that provides one column given by newColSpec.
     * 
     * @param colSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     */
    public BitString2BitVectorCellFactory(final DataColumnSpec colSpec,
            final int columnIndex) {
        super(colSpec, columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        incrementNrOfRows();
        BitSet bitSet;
        String bitString;
        DataCell cell = row.getCell(getColumnIndex());
        if (!cell.getType().isCompatible(StringValue.class)) {
            LOGGER.warn(cell + " is not a String! "
                    + "Replacing it with missing value");
            return DataType.getMissingCell();
        } else if (cell.isMissing()) {
            return DataType.getMissingCell();
        } else {
            bitString = ((StringValue)cell).getStringValue();
            bitSet = new BitSet(bitString.length());
            int pos = 0;
            int numberOf0s = 0;
            int numberOf1s = 0;
            for (int i = 0; i < bitString.length(); i++) {
                char c = bitString.charAt(i);
                if (c == '0') {
                    pos++;
                    numberOf0s++;
                } else if (c == '1') {
                    bitSet.set(pos++);
                    numberOf1s++;
                } else {
                    if (m_hasPrintedWarning) {
                        LOGGER.debug("Found invalid character: " + c
                                + ". Returning missing cell!");
                    } else {
                        m_hasPrintedWarning = true;
                        LOGGER.warn("Found invalid character: " + c
                                + ". Returning missing cell!"
                                + " (Suppress further warnings!)");
                    }
                    return DataType.getMissingCell();

                }
            }
            m_nrOfNotSetBits += numberOf0s;
            m_nrOfSetBits += numberOf1s;
        }
        m_wasSuccessful = true;
        return new BitVectorCell(bitSet, bitString.length());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasSuccessful() {
        return m_wasSuccessful;
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
        return m_nrOfNotSetBits;
    }
}
