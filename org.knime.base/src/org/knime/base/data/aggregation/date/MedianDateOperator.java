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
 */

package org.knime.base.data.aggregation.date;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;


/**
 * Date operator that calculates the median date.
 * @author Tobias Koetter, University of Konstanz
 */
public class MedianDateOperator extends AggregationOperator {

    private static final DataType TYPE = DateAndTimeCell.TYPE;

    private final List<DataCell> m_cells;
    private final Comparator<DataCell> m_comparator = TYPE.getComparator();


    /**Constructor for class MedianDateOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected MedianDateOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        try {
            m_cells = new ArrayList<DataCell>(getMaxUniqueValues());
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException(
            "Maximum unique values number to big");
        }
    }

    /**Constructor for class MedianDateOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public MedianDateOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Median date", true, false,
                DateAndTimeValue.class, false), globalSettings,
                AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new MedianDateOperator(getOperatorData(), globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (m_cells.size() >= getMaxUniqueValues()) {
            setSkipMessage("Group contains to many values");
            return true;
        }
        m_cells.add(cell);
        return false;
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
        final int size = m_cells.size();
        if (size == 0) {
            return DataType.getMissingCell();
        }
        if (size == 1) {
            return m_cells.get(0);
        }
        Collections.sort(m_cells, m_comparator);
        final double middle = size / 2.0;
        if (middle > (int)middle) {
            return m_cells.get((int)middle);
        }
        //the list is even return the middle two
        final DateAndTimeValue date1 =
            (DateAndTimeValue)m_cells.get((int) middle - 1);
        final DateAndTimeValue date2 =
            (DateAndTimeValue)m_cells.get((int) middle);
        final long millis1 =
            date1.getUTCTimeInMillis();
        final long millis2 =
            date2.getUTCTimeInMillis();
        return new DateAndTimeCell((millis1 + millis2) / 2,
                date1.hasDate() || date2.hasDate(),
                date1.hasTime() || date2.hasTime(),
                date1.hasMillis() || date2.hasMillis());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_cells.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the median of all date per group. "
        + "If the number of dates is even the mean is returned with "
        + "date/time/milliseconds if one two middle dates has a "
        + "date/time/milliseconds set.";
    }

}
