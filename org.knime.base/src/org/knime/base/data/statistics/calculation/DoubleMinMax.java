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
 *   04.06.2014 (Marcel Hanser): created
 */
package org.knime.base.data.statistics.calculation;

import org.knime.base.data.statistics.Statistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;

/**
 * A double specific min/ max implementation which additionaly has the possibility to filter infinite values from the
 * data.
 *
 * @author Marcel Hanser
 * @since 2.12
 */
public class DoubleMinMax extends Statistic {

    private double[] m_min;

    private double[] m_max;

    private final boolean m_ignoreInfiniteValues;

    /**
     * @param ignoreInfiniteValues if <code>true</code> the computation will ignore infinite values in the computation
     * @param columns to calculate the min/max values
     */
    public DoubleMinMax(final boolean ignoreInfiniteValues, final String... columns) {
        super(DoubleValue.class, columns);
        m_ignoreInfiniteValues = ignoreInfiniteValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int size) {
        m_min = new double[size];
        m_max = new double[size];
        for (int i = 0; i < size; i++) {
            m_min[i] = Double.NaN;
            m_max[i] = Double.NaN;
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
            if (!cell.isMissing()) {
                double val = ((DoubleValue)cell).getDoubleValue();

                if (Double.isNaN(m_min[index]) || val < m_min[index]) {
                    if (!m_ignoreInfiniteValues || !Double.isInfinite(val)) {
                        m_min[index] = val;
                    }
                }
                if (Double.isNaN(m_max[index]) || val > m_max[index]) {
                    if (!m_ignoreInfiniteValues || !Double.isInfinite(val)) {
                        m_max[index] = val;
                    }
                }
            }
            index++;
        }
    }

    /**
     * @param column the columm
     * @return the min value or {@link DataType#getMissingCell()}
     */
    public double getMin(final String column) {
        return m_min[assertIndexForColumn(column)];
    }

    /**
     * @param column the columm
     * @return the max value or {@link DataType#getMissingCell()}
     */
    public double getMax(final String column) {
        return m_max[assertIndexForColumn(column)];
    }
}
