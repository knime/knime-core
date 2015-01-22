/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.05.2014 (Marcel Hanser): created
 */
package org.knime.base.data.statistics.calculation;

import java.util.Arrays;

import org.knime.base.data.statistics.StatisticSorted;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Calculates the median of the given column.
 *
 * @author Marcel Hanser
 * @since 2.12
 */
public class Median extends StatisticSorted {

    private int m_currentIndex = 0;

    private DataCell[] m_medians;

    private boolean[] m_isDouble;

    private double[] m_medianIndex;

    /**
     * @param columns to calculate the median
     */
    public Median(final String... columns) {
        super(DataValue.class, columns);
    }

    /**
     * @param column the column
     * @return the computed median of the column
     */
    public DataCell getMedian(final String column) {
        return m_medians[assertIndexForColumn(column)];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int size) {
        m_medians = new DataCell[size];
        m_medianIndex = new double[size];
        m_isDouble = new boolean[size];
        Arrays.fill(m_medians, DataType.getMissingCell());
        int index = 0;
        for (int i : getIndices()) {
            m_isDouble[index++] = spec.getColumnSpec(i).getType().isCompatible(DoubleValue.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void consumeRow(final DataRow dataRow) {
        int index = 0;
        for (int i : getIndices()) {
            DataCell cell = dataRow.getCell(i);
            if (cell.isMissing()) {
                m_medianIndex[index] += 0.5;
            } else if (m_currentIndex == Math.ceil(m_medianIndex[index]) - 1) {
                m_medians[index] = cell;

            } else if (m_isDouble[index] && isInteger(m_medianIndex[index]) && m_currentIndex == m_medianIndex[index]) {
                // this means we are in situation of double value columns with even amount of valid doubles
                // so return the middle of them.
                m_medians[index] =
                    new DoubleCell(
                        (((DoubleValue)m_medians[index]).getDoubleValue() + ((DoubleValue)cell).getDoubleValue()) / 2);
            }
            index++;
        }
        m_currentIndex++;
    }

    /**
     * @param d
     * @return
     */
    private static boolean isInteger(final double d) {
        return d == Math.floor(d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeEvaluation(final int amountOfRows) {
        Arrays.fill(m_medianIndex, amountOfRows / 2d);
    }
}
