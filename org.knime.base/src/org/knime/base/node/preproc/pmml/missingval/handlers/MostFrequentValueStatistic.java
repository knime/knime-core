/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   14.01.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval.handlers;

import java.util.HashMap;
import java.util.Map;

import org.knime.base.data.statistics.Statistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.util.MutableInteger;

/**
 * Calculates the most frequent value for a single column.
 * @author Alexander Fillbrunn
 */
public class MostFrequentValueStatistic extends Statistic {

    /**
     * Constructor for a new instance of MostFrequentValueStatistic.
     * @param col the column this statistic is calculated for
     */
    @SuppressWarnings("unchecked")
    public MostFrequentValueStatistic(final String col) {
        super(new Class[]{org.knime.core.data.NominalValue.class, org.knime.core.data.IntValue.class}, col);
    }

    private Map<DataCell, MutableInteger> m_nominalValues;
    private int m_colIdx;
    private int m_maxCount = 0;
    private DataCell m_mostFrequent = DataType.getMissingCell();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int amountOfColumns) {
        m_nominalValues = new HashMap<>();
        m_colIdx = spec.findColumnIndex(getColumns()[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void consumeRow(final DataRow dataRow) {
        DataCell cell = dataRow.getCell(m_colIdx);
        if (cell.isMissing()) {
            return;
        }
        MutableInteger i = m_nominalValues.get(cell);
        if (i == null) {
            i = new MutableInteger(1);
            m_nominalValues.put(cell, i);
        } else {
            i.inc();
        }
        if (i.intValue() > m_maxCount) {
            m_maxCount = i.intValue();
            m_mostFrequent = cell;
        }
    }

    /**
     * @return the most frequently observed value in the column.
     */
    public DataCell getMostFrequent() {
        return m_mostFrequent;
    }
}
