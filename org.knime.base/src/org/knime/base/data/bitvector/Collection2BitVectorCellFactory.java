/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.06.2014 (koetter): created
 */
package org.knime.base.data.bitvector;

import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.vector.bitvector.BitVectorType;
import org.knime.core.node.NodeLogger;

/**
 * Creates a bit vector for a given {@link CollectionDataValue}.
 * @author Tobias Koetter
 * @since 2.10
 */
public class Collection2BitVectorCellFactory extends BitVectorColumnCellFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Collection2BitVectorCellFactory.class);

    private int m_nrOfSetBits = 0;

    private int m_nrOfNotSetBits = 0;

    private final Map<String, Integer> m_idxMap;

    /**
     * @param vectorType {@link BitVectorType}
     * @param columnSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     * @param idxMap {@link Map} that maps a {@link DataCell} string representation to a unique index starting by 0
     */
    public Collection2BitVectorCellFactory(final BitVectorType vectorType, final DataColumnSpec columnSpec,
        final int columnIndex, final Map<String, Integer> idxMap) {
        super(vectorType, columnSpec, columnIndex);
        m_idxMap = idxMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        incrementNrOfRows();
        final DataCell cell = row.getCell(getColumnIndex());
        if (cell.isMissing()) {
            return DataType.getMissingCell();
        }
        if (cell instanceof CollectionDataValue) {
            org.knime.core.data.vector.bitvector.BitVectorCellFactory<? extends DataCell> factory =
                    getVectorType().getCellFactory(m_idxMap.size());
            final CollectionDataValue collCell = (CollectionDataValue) cell;
            for (final DataCell valCell : collCell) {
                if (valCell.isMissing()) {
                    continue;
                }
                final Integer bitIdx = m_idxMap.get(valCell.toString());
                if (bitIdx != null) {
                    factory.set(bitIdx.intValue());
                } else {
                    printError(LOGGER, row, "No bit index found for cell " + valCell.toString());
                    return DataType.getMissingCell();
                }
            }
            m_nrOfSetBits += collCell.size();
            m_nrOfNotSetBits += m_idxMap.size() - collCell.size();
            return factory.createDataCell();
        } else {
            printError(LOGGER, row, "Incompatible type found");
            return DataType.getMissingCell();
        }
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
