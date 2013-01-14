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

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.LongCell;


/**
 * Date operator that computes the range between the first and last date
 * in milliseconds.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class MillisRangeOperator extends AggregationOperator {

    private static final DataType TYPE = LongCell.TYPE;

    private DataCell m_min = null;

    private DataCell m_max = null;

    private final DataValueComparator m_comparator =
        DateAndTimeCell.TYPE.getComparator();

    /**Constructor for class MillisRangeOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected MillisRangeOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
    }

    /**Constructor for class MillisRangeOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public MillisRangeOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Date range(ms)", false, false,
                DateAndTimeValue.class, false), globalSettings,
        setInclMissingFlag(opColSettings));
    }

    /**
     * Ensure that the flag is set correctly since this method does not
     * support changing of the missing cell handling option.
     *
     * @param opColSettings the {@link OperatorColumnSettings} to set
     * @return the correct {@link OperatorColumnSettings}
     */
    private static OperatorColumnSettings setInclMissingFlag(
            final OperatorColumnSettings opColSettings) {
        opColSettings.setInclMissing(false);
        return opColSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new MillisRangeOperator(getOperatorData(), globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (m_min == null || m_max == null) {
            //this is the first call
            m_min = cell;
            m_max = cell;
            return false;
        }
        if (m_comparator.compare(m_min, cell) > 0) {
            m_min = cell;
        }
        if (m_comparator.compare(m_max, cell) < 0) {
            m_max = cell;
        }
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
        if (m_max == null || m_min == null) {
            //the group contains only missing cells
            return DataType.getMissingCell();
        }
        final long max = getMax().getUTCTimeInMillis();
        final long min = getMin().getUTCTimeInMillis();
        return new LongCell(max - min);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_min = null;
        m_max = null;
    }


    /**
     * @return the most distant date or <code>null</code> if
     * the group contained only missing cells
     */
    protected DateAndTimeValue getMin() {
        return m_min == null ? null : (DateAndTimeValue)m_min;
    }


    /**
     * @return the most resent date or <code>null</code> if the group contained
     * only missing cells
     */
    protected DateAndTimeValue getMax() {
        return m_max == null ? null : (DateAndTimeValue)m_max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the range between the first and last date "
            + "in milliseconds.";
    }

}
