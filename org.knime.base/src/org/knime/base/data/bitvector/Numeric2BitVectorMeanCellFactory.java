/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   18.01.2007 (dill): created
 */
package org.knime.base.data.bitvector;

import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.vector.bitvector.BitVectorType;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Fabian Dill, University of Konstanz
 * @author Tobias Koetter
 */
public class Numeric2BitVectorMeanCellFactory extends BitVectorCellFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Numeric2BitVectorMeanCellFactory.class);

    private double[] m_meanValues;

    private double m_meanFactor;

    private int m_totalNrOf0s;

    private int m_totalNrOf1s;

    private final int[] m_columns;

    private final BitVectorType m_vectorType;

    /**
     *
     * @param bitColSpec the column spec of the column containing the bit vector
     * @param meanValues the mean values of the numeric columns
     * @param meanThreshold threshold above which the bits should be set
     *            (percentage of the mean)
     * @param colIndices list of column indices used to create bit vector from
     */
    public Numeric2BitVectorMeanCellFactory(final DataColumnSpec bitColSpec,
            final double[] meanValues, final double meanThreshold,
            final List<Integer> colIndices) {
        this(BitVectorType.DENSE, bitColSpec, meanThreshold, meanValues, convert2Array(colIndices));
    }

    /**
     * @param vectorType {@link BitVectorType}
     * @param bitColSpec the column spec of the column containing the bit vector
     * @param meanThreshold threshold above which the bits should be set
     *            (percentage of the mean)
     * @param meanValues the mean values of the numeric columns defined by the given column indices. The mean
     * values need to be in the same order as their corresponding column indices
     * @param colIndices list of column indices used to create bit vector from in the same order as their corresponding
     * mean values
     * @since 2.10
     */
    public Numeric2BitVectorMeanCellFactory(final BitVectorType vectorType, final DataColumnSpec bitColSpec,
        final double meanThreshold, final double[] meanValues, final int[] colIndices) {
        super(bitColSpec);
        m_vectorType = vectorType;
        m_meanValues = meanValues;
        m_meanFactor = meanThreshold;
        m_columns = colIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfNotSetBits() {
        return m_totalNrOf0s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSetBits() {
        return m_totalNrOf1s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        incrementNrOfRows();
        org.knime.core.data.vector.bitvector.BitVectorCellFactory<? extends DataCell> factory =
                m_vectorType.getCellFactory(m_columns.length);
        for (int i = 0; i < m_columns.length; i++) {
            final DataCell cell = row.getCell(m_columns[i]);
            if (cell.isMissing()) {
                m_totalNrOf0s++;
                continue;
            }
            if (cell instanceof DoubleValue) {
                double currValue = ((DoubleValue)cell).getDoubleValue();
                if (currValue >= (m_meanFactor * m_meanValues[i])) {
                    factory.set(i);
                    m_totalNrOf1s++;
                } else {
                    m_totalNrOf0s++;
                }
            } else {
                printError(LOGGER, row, "Incompatible type found.");
                return DataType.getMissingCell();
            }
        }
        return factory.createDataCell();
    }
}
