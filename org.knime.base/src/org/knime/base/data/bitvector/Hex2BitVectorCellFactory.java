/*
 *
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Jan 13, 2006 (wiswedel): created
 */
package org.knime.base.data.bitvector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.NodeLogger;

/**
 * Factory class to transform a {@link org.knime.core.data.StringValue} cell
 * into a {@link DenseBitVectorCell}. If, during conversion an error occurs it
 * logs an error message and returns a missing cell. A flag
 * &quot;wasSuccessful&quot; (see {@link #wasSuccessful()}) can be queried to
 * see if this missing cell was created due to an error or due to a missing
 * input cell. This flag is only set (and not reset after successful conversion
 * of the next input cell).
 *
 * @see org.knime.base.data.replace.ReplacedColumnsTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class Hex2BitVectorCellFactory extends BitVectorColumnCellFactory {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(Hex2BitVectorCellFactory.class);

    private int m_nrOfSetBits = 0;

    private int m_nrOfNotSetBits = 0;

    private boolean m_wasSuccessful = true;

    /**
     * Create new cell factory that provides one column given by newColSpec.
     *
     * @param colSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     */
    public Hex2BitVectorCellFactory(final DataColumnSpec colSpec,
            final int columnIndex) {
        super(colSpec, columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        incrementNrOfRows();
        DataCell old = row.getCell(getColumnIndex());
        if (old.isMissing()) {
            return DataType.getMissingCell();
        }
        if (old instanceof StringValue) {
            String val = ((StringValue)old).getStringValue();
            DataCell newCell;
            try {
                String hexString = val.trim();
                DenseBitVectorCell cell =
                        new DenseBitVectorCellFactory(hexString)
                                .createDataCell();
                int card = (int)cell.cardinality(); // hopefully int does it
                m_nrOfSetBits += card;
                m_nrOfNotSetBits += cell.length() - card;
                newCell = cell;
            } catch (IllegalArgumentException nfe) {
                String nfeMsg = nfe.getMessage();
                if (nfeMsg == null) {
                    nfeMsg = "<sorry, no further details>";
                }
                String message =
                        "Unable to convert \"" + val + "\" to "
                                + "bit vector: " + nfeMsg;
                printError(LOGGER, message);
                newCell = DataType.getMissingCell();
                m_wasSuccessful = false;
            }
            return newCell;
        } else {
            m_wasSuccessful = false;
            printError(LOGGER, "Unable to convert \"" + old
                    + "\" to bit vector, not a string value cell.");
            return DataType.getMissingCell();
        }
    }

    /**
     *
     * @return <code>true</code> if all conversions of all cells were
     *         successful. <code>false</code> if one conversion failed.
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
