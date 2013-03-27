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
