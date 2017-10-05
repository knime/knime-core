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
 *   Apr 28, 2017 (marcel): created
 */
package org.knime.base.data.aggregation.datetime;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;

/**
 * Date time aggregation operator that calculates the mean date time per group.
 *
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
public final class LocalDateTimeMeanOperator extends AggregationOperator {

    private static final DataType TYPE = LocalDateTimeCellFactory.TYPE;

    private long m_count = 0;

    private double m_meanSeconds = 0;

    private double m_meanNanos = 0;

    /**
     * Empty constructor for extension registration. Calls
     * {@link #LocalDateTimeMeanOperator(GlobalSettings, OperatorColumnSettings)} using {@link GlobalSettings#DEFAULT}
     * and {@link OperatorColumnSettings#DEFAULT_EXCL_MISSING}.
     */
    public LocalDateTimeMeanOperator() {
        this(GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
    }

    /**
     * Constructor for class LocalDateTimeMeanOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public LocalDateTimeMeanOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        super(new OperatorData("Mean " + LocalDateTimeValue.UTILITY.getName(), "Mean", "Mean", false, true,
            LocalDateTimeValue.class, false), globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the mean over all date times per group.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new LocalDateTimeMeanOperator(globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        final LocalDateTime dateTime = ((LocalDateTimeValue)cell).getLocalDateTime();
        final long timestampSeconds = dateTime.toEpochSecond(ZoneOffset.UTC);
        final int timestampNanos = dateTime.getNano();
        m_count++;
        m_meanSeconds = m_meanSeconds + (timestampSeconds - m_meanSeconds) / m_count;
        m_meanNanos = m_meanNanos + (timestampNanos - m_meanNanos) / m_count;
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
        return m_count != 0
            ? LocalDateTimeCellFactory.create(LocalDateTime.ofEpochSecond((long)m_meanSeconds,
                (int)(m_meanSeconds - (long)m_meanSeconds + m_meanNanos + 0.5), ZoneOffset.UTC))
            : DataType.getMissingCell();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_count = 0;
        m_meanSeconds = 0;
        m_meanNanos = 0;
    }
}
