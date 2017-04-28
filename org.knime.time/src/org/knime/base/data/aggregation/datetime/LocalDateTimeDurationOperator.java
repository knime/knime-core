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
 *   Apr 27, 2017 (marcel): created
 */
package org.knime.base.data.aggregation.datetime;

import java.time.Duration;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.AbstractRangeOperator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;

/**
 * Aggregation operator for {@link LocalDateTimeValue} that computes the {@link DurationValue} between the first and the
 * last date time of a group.
 *
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
public final class LocalDateTimeDurationOperator extends AbstractRangeOperator {

    private static final DataType IN_TYPE = LocalDateTimeCellFactory.TYPE;

    private static final DataType OUT_TYPE = DurationCellFactory.TYPE;

    /**
     * Empty constructor for extension registration. Calls
     * {@link #LocalDateTimeDurationOperator(GlobalSettings, OperatorColumnSettings)} using
     * {@link GlobalSettings#DEFAULT} and {@link OperatorColumnSettings#DEFAULT_EXCL_MISSING}.
     */
    public LocalDateTimeDurationOperator() {
        this(GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
    }

    /**
     * Constructor for class LocalDateTimeDurationOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public LocalDateTimeDurationOperator(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(
            IN_TYPE, new OperatorData("Duration date time", "Duration", "Duration", false, false,
                LocalDateTimeValue.class, false),
            globalSettings, AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the duration between the first and the last date time of a group.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new LocalDateTimeDurationOperator(globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return OUT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal(final DataCell min, final DataCell max) {
        return DurationCellFactory.create(Duration.between(((LocalDateTimeValue)min).getLocalDateTime(),
            ((LocalDateTimeValue)max).getLocalDateTime()));
    }
}
