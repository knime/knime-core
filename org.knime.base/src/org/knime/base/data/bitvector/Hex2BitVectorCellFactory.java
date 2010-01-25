/*
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
