/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   04.02.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval.handlers.timeseries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * HashMap based statistic for linear interpolation.
 * @author Alexander Fillbrunn
 */
public class LinearInterpolationStatisticMB extends MappingStatistic {

    private List<DataCell> m_values;
    private int m_numMissing = 0;
    private boolean m_isDateColumn;
    private DataCell m_previous = DataType.getMissingCell();
    private int m_colIdx;

    /**
     * @param column the column this statistic is created for
     * @param isDateColumn true if the statistic is used on a date column
     */
    public LinearInterpolationStatisticMB(final String column,
                                            final boolean isDateColumn) {
        super(isDateColumn ? DateAndTimeValue.class : DoubleValue.class, column);
        m_isDateColumn = isDateColumn;
        m_values = new ArrayList<DataCell>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<DataCell> iterator() {
        return m_values.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int amountOfColumns) {
        m_colIdx = spec.findColumnIndex(getColumns()[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void consumeRow(final DataRow dataRow) {
        DataCell cell = dataRow.getCell(m_colIdx);
        if (cell.isMissing()) {
            m_numMissing++;
        } else {
            for (int i = 0; i < m_numMissing; i++) {
                DataCell res;
                if (m_previous.isMissing()) {
                    res = cell;
                } else {
                    if (m_isDateColumn) {
                        DateAndTimeValue val = (DateAndTimeValue)cell;
                        DateAndTimeValue prevVal = (DateAndTimeValue)m_previous;

                        boolean hasDate = val.hasDate() | prevVal.hasDate();
                        boolean hasTime = val.hasTime() | prevVal.hasTime();
                        boolean hasMilis = val.hasMillis() | prevVal.hasMillis();

                        long prev = prevVal.getUTCTimeInMillis();
                        long next = val.getUTCTimeInMillis();
                        long lin = Math.round(prev + 1.0 * (i + 1) / (1.0 * (m_numMissing + 1)) * (next - prev));
                        res = new DateAndTimeCell(lin, hasDate, hasTime, hasMilis);
                    } else {
                        DoubleValue val = (DoubleValue)cell;
                        double prev = ((DoubleValue)m_previous).getDoubleValue();
                        double next = val.getDoubleValue();
                        double lin = prev + 1.0 * (i + 1) / (1.0 * (m_numMissing + 1)) * (next - prev);

                        if (m_previous instanceof IntValue) {
                            // get an int, create an int
                            res = new IntCell((int)Math.round(lin));
                        } else if (m_previous instanceof LongValue) {
                            // get an long, create an long
                            res = new LongCell(Math.round(lin));
                        } else {
                            res = new DoubleCell(lin);
                        }
                    }
                }
                m_values.add(res);
            }
            m_numMissing = 0;
            m_previous = cell;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String afterEvaluation() {
        // Rest cannot be calculated and therefore is set to the previous value
        for (int i = 0; i < m_numMissing; i++) {
            m_values.add(m_previous);
        }
        return super.afterEvaluation();
    }
}
