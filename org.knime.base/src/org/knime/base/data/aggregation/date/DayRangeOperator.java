/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */

package org.knime.base.data.aggregation.date;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.DoubleCell;

import org.knime.base.data.aggregation.AggregationOperator;


/**
 * Date operator that returns the range between the last and first
 * date per group in days.
 * @author Tobias Koetter, University of Konstanz
 */
public class DayRangeOperator extends MillisRangeOperator {

    private static final DataType TYPE = DoubleCell.TYPE;

    /**The number of milliseconds per day.
     * 24 hour * 60 minutes * 60 seconds * 1000 milliseconds.*/
    private static final double MS_PER_DAY = 24 * 60 * 60 * 1000;

    /**Constructor for class DayRangeOperator.
     *
     */
    public DayRangeOperator() {
        super("Date range(day)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final DataColumnSpec origColSpec,
            final int maxUniqueValues) {
        return new DayRangeOperator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        final DateAndTimeValue min = getMin();
        final DateAndTimeValue max = getMax();
        if (min == null || max == null) {
            return DataType.getMissingCell();
        }
        final long range = max.getUTCTimeInMillis() - min.getUTCTimeInMillis();
        if (range == 0) {
            return new DoubleCell(0);
        }
        return new DoubleCell(range / MS_PER_DAY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the range between the first and last date "
        + "in days.";
    }
}
