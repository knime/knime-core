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
 *   Apr 27, 2017 (marcel): created
 */
package org.knime.base.data.aggregation.datetime;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.AbstractMedianOperator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;

/**
 * Median aggregation operator for {@link LocalDateTimeValue}.
 *
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
public final class LocalDateTimeMedianOperator extends AbstractMedianOperator {

    private static DataType TYPE = LocalDateTimeCellFactory.TYPE;

    private static EvenListMedianMethod createCustomMeanMedianMetod() {
        return new EvenListMedianMethod() {

            @Override
            public DataCell extractMedian(final List<DataCell> cells, final int lowerCandidateIdx,
                final int upperCandidateIdx) {
                final LocalDateTime dateTime1 = ((LocalDateTimeValue)cells.get(lowerCandidateIdx)).getLocalDateTime();
                final LocalDateTime dateTime2 = ((LocalDateTimeValue)cells.get(upperCandidateIdx)).getLocalDateTime();

                // calculate mean (seconds), keep decimals for further calculations
                final double meanTimestampSeconds =
                    (dateTime1.toEpochSecond(ZoneOffset.UTC) + dateTime2.toEpochSecond(ZoneOffset.UTC)) / 2.0;
                // calculate mean (nanos), round fraction of nano
                final int meanTimestampNanos = (int)(meanTimestampSeconds - (long)meanTimestampSeconds
                    + (dateTime1.getNano() + dateTime2.getNano()) / 2.0 + 0.5);
                final LocalDateTime meanDateTime =
                    LocalDateTime.ofEpochSecond((long)meanTimestampSeconds, meanTimestampNanos, ZoneOffset.UTC);
                return LocalDateTimeCellFactory.create(meanDateTime);
            }
        };
    }

    /**
     * Empty constructor for extension registration.
     */
    public LocalDateTimeMedianOperator() {
        this(GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
    }

    /**
     * Constructor for class LocalDateTimeMedianOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public LocalDateTimeMedianOperator(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(formatId(LocalDateTimeValue.UTILITY), true, LocalDateTimeValue.class, createCustomMeanMedianMetod(),
            globalSettings, opColSettings);
    }

    private LocalDateTimeMedianOperator(final EvenListMedianMethodDescription[] methodDescs,
        final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(methodDescs, operatorData, globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        final LocalDateTimeMedianOperator operator = new LocalDateTimeMedianOperator(getMedianMethodDescriptions(),
            getOperatorData(), globalSettings, opColSettings);
        operator.setMedianMethod(getMedianMethod());
        return operator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return TYPE;
    }
}
