/*
 * ------------------------------------------------------------------------
 *
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   13.07.2015 (Alexander): created
 */
package org.knime.base.data.statistics;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Statistic for calculating the mean of columns. This statistic returns a DataCell
 * as mean that can be a MissingCell if all cells in the column are missing or the table is empty.
 * @author Alexander Fillbrunn
 * @since 2.12
 */
public class CellMean extends Statistic {

    private double[] m_sums;
    private int[] m_counts;
    private int[] m_indices;

    /**
     * @param columns the columns this statistic is for
     */
    public CellMean(final String... columns) {
        super(DoubleValue.class, columns);
        m_sums = new double[columns.length];
        m_counts = new int[columns.length];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void consumeRow(final DataRow dataRow) {
        for (int i = 0; i < m_indices.length; i++) {
            int idx = m_indices[i];
            DataCell cell = dataRow.getCell(idx);
            if (!cell.isMissing()) {
                m_counts[i]++;
                m_sums[i] += ((DoubleValue)cell).getDoubleValue();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int amountOfColumns) {
        m_indices = new int[amountOfColumns];
        for (int i = 0; i < m_indices.length; i++) {
            m_indices[i] = spec.findColumnIndex(getColumns()[i]);
        }
    }

    /**
     * @param col the index of the column as it was given in the constructor of the statistic
     * @return Returns a double cell that is missing if all values in the table are missing and the mean otherwise.
     */
    public DataCell getResult(final int col) {
        if (m_counts[col] == 0) {
            return DataType.getMissingCell();
        } else {
            return new DoubleCell(m_sums[col] / m_counts[col]);
        }
    }

}
