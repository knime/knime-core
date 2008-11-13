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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class IdString2BitVectorCellFactory extends BitVectorColumnCellFactory {

    /**
     * Create new cell factory that provides one column given by newColSpec.
     *
     * @param colSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     */
    public IdString2BitVectorCellFactory(final DataColumnSpec colSpec,
            final int columnIndex) {
        super(colSpec, columnIndex);
    }

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(IdString2BitVectorCellFactory.class);

    private int m_nrOfSetBits = 0;

    private int m_maxPos = Integer.MIN_VALUE;

    private boolean m_wasSuccessful = true;

    /**
     *
     * @param maxPos the actual length of the bit set - the max position
     */
    public void setMaxPos(final int maxPos) {
        m_maxPos = maxPos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        incrementNrOfRows();
        if (!row.getCell(getColumnIndex()).getType().isCompatible(
                StringValue.class)) {
            m_wasSuccessful = false;
            printError(LOGGER, "Cell in column " + getColumnIndex()
                    + " is not a string value!");
            return DataType.getMissingCell();
        }
        if (row.getCell(getColumnIndex()).isMissing()) {
            return DataType.getMissingCell();
        }
        String toParse =
                ((StringValue)row.getCell(getColumnIndex())).getStringValue();
        toParse = toParse.trim();

        try {
            int newlySetBits = 0;
            DenseBitVector currBitSet = new DenseBitVector(m_maxPos);
            if (!toParse.isEmpty()) {
                String[] numbers = toParse.split("\\s");
                for (int i = 0; i < numbers.length; i++) {
                    int pos = Integer.parseInt(numbers[i].trim());
                    if (pos < 0) {
                        m_wasSuccessful = false;
                        printError(LOGGER,
                                "Invalid negative index in index string: "
                                        + toParse);
                        return DataType.getMissingCell();
                    }
                    if (!currBitSet.get(pos)) {
                        currBitSet.set(pos);
                        newlySetBits++;
                    }
                }
            }
            m_nrOfSetBits += newlySetBits;
            DenseBitVectorCellFactory fact =
                    new DenseBitVectorCellFactory(currBitSet);
            return fact.createDataCell();
        } catch (NumberFormatException nfe) {
            String nfeMsg = nfe.getMessage();
            if (nfeMsg == null) {
                nfeMsg = "<sorry, no further details>";
            }
            String message =
                    "Unable to convert \"" + toParse + "\" to "
                            + "bit vector: " + nfeMsg;
            m_wasSuccessful = false;
            printError(LOGGER, message);
            return DataType.getMissingCell();
        }
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
        int allPositions = getNrOfProcessedRows() * m_maxPos;
        return allPositions - m_nrOfSetBits;
    }
}
