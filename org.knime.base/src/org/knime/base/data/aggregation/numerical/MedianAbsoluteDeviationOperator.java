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
 *   Jun 23, 2015 (Lara): created
 */
package org.knime.base.data.aggregation.numerical;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Calculates the median absolute deviation per group.
 *
 * @author Lara Gorini
 * @since 2.12
 */
public class MedianAbsoluteDeviationOperator extends StoreResizableDoubleArrayOperator {

    private static final DataType TYPE = DoubleCell.TYPE;

    /**
     * Constructor for class MedianAbsoluteDeviationOperator.
     *
     * @param operatorData
     * @param globalSettings
     * @param opColSettings
     */
    protected MedianAbsoluteDeviationOperator(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
    }

    /**
     * Constructor for class MedianAbsoluteDeviationOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public MedianAbsoluteDeviationOperator(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Median absolute deviation", true, false, DoubleValue.class, false), globalSettings,
            setInclMissingFlag(opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the median absolute deviation per group.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new MedianAbsoluteDeviationOperator(getOperatorData(), globalSettings, opColSettings);
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
    public String getColumnLabel() {
        return "Median abs. dev.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        final double[] cells = super.getCells().getElements();
        if (cells.length == 0) {
            return DataType.getMissingCell();
        }
        final Median median = new Median();
        double medianValue = median.evaluate(cells);
        for (int i = 0; i < cells.length; i++) {
            cells[i] = Math.abs(medianValue - cells[i]);
        }
        medianValue = median.evaluate(cells);
        return new DoubleCell(medianValue);
    }

}
