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
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;


/**
 * Date operator that calculates the mean date per group. The resulting
 * date has date/time/milliseconds set if at least one member of the group
 * had date/time/milliseconds set.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class DateMeanOperator extends AggregationOperator {

    private int m_count = 0;
    private double m_mean = 0;

    private boolean m_hasDate = false;
    private boolean m_hasTime = false;
    private boolean m_hasMilis = false;

    /**Constructor for class DateMeanOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     *
     */
    protected DateMeanOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
    }

    /**Constructor for class DateMeanOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public DateMeanOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Mean date", false, true, DateAndTimeValue.class,
                false), globalSettings, setInclMissingFlag(opColSettings));
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
        return new DateMeanOperator(getOperatorData(), globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell instanceof DateAndTimeValue) {
            //skip missing cells
            final DateAndTimeValue dateCell = (DateAndTimeValue)cell;
            m_hasDate |= dateCell.hasDate();
            m_hasTime |= dateCell.hasTime();
            m_hasMilis |= dateCell.hasMillis();
            final double d = dateCell.getUTCTimeInMillis();
            m_mean = m_mean * ((double)m_count / (m_count  + 1))
              + d * (1.0 / (m_count + 1));
            m_count++;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return origType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_count == 0) {
            return DataType.getMissingCell();
        }
        return new DateAndTimeCell(
                (long)m_mean, m_hasDate, m_hasTime, m_hasMilis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_mean = 0;
        m_count = 0;
        m_hasDate = false;
        m_hasTime = false;
        m_hasMilis = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the mean over all dates per group. "
        + "The result date contains a date/time/milliseconds if at least "
        + "one dates has a date/time/milliseconds set per group";
    }

}
