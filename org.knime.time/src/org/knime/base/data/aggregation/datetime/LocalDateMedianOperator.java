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
 *   Apr 26, 2017 (marcel): created
 */
package org.knime.base.data.aggregation.datetime;

import java.time.LocalDate;
import java.util.List;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.AbstractMedianOperator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;

/**
 * Median aggregation operator for {@link LocalDateValue}.
 *
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
public final class LocalDateMedianOperator extends AbstractMedianOperator {

    private static final DataType TYPE = LocalDateCellFactory.TYPE;

    private static EvenListMedianMethod createCustomMeanMedianMethod() {
        return new EvenListMedianMethod() {

            @Override
            public DataCell extractMedian(final List<DataCell> cells, final int lowerCandidateIdx,
                final int upperCandidateIdx) {
                final LocalDate date1 = ((LocalDateValue)cells.get(lowerCandidateIdx)).getLocalDate();
                final LocalDate date2 = ((LocalDateValue)cells.get(upperCandidateIdx)).getLocalDate();

                // calculate mean, truncate/round down fraction of day
                final long meanTimestamp = (long)((date1.toEpochDay() + date2.toEpochDay()) / 2.0);
                final LocalDate meanDate = LocalDate.ofEpochDay(meanTimestamp);
                return LocalDateCellFactory.create(meanDate);
            }
        };
    }

    /**
     * Empty constructor for extension registration.
     */
    public LocalDateMedianOperator() {
        this(GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
    }

    /**
     * Constructor for class LocalDateMedianOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public LocalDateMedianOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        super(formatId(LocalDateValue.UTILITY), true, LocalDateValue.class, createCustomMeanMedianMethod(),
            globalSettings, opColSettings);
    }

    private LocalDateMedianOperator(final EvenListMedianMethodDescription[] methodDescs,
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
        final LocalDateMedianOperator operator = new LocalDateMedianOperator(getMedianMethodDescriptions(),
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
