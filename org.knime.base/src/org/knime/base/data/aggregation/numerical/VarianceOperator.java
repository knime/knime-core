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

package org.knime.base.data.aggregation.numerical;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Returns the variance per group.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class VarianceOperator extends AggregationOperator {

    private final DataType m_type = DoubleCell.TYPE;

    private double m_sumSquare = 0;
    private double m_sum = 0;
    private int m_validCount = 0;

    /**Constructor for class VarianceOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected VarianceOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
    }

    /**Constructor for class VarianceOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public VarianceOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Variance", false, false, DoubleValue.class, false),
             globalSettings, AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new VarianceOperator(getOperatorData(), globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        final double d = ((DoubleValue)cell).getDoubleValue();
        m_validCount++;
        m_sum += d;
        m_sumSquare += d * d;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_validCount <= 0) {
            return DataType.getMissingCell();
        }
        if (m_validCount == 1) {
            return new DoubleCell(0);
        }
        double variance = (m_sumSquare - ((m_sum * m_sum)
                / m_validCount)) / (m_validCount - 1);
        // unreported bug fix: in cases in which a column contains
        // almost only one value (for instance 1.0) but one single
        // 'outlier' whose value is, for instance 0.9999998, we get
        // round-off errors resulting in negative variance values
        if (variance < 0.0 && variance > -1.0E8) {
            variance = 0.0;
        }
        return new DoubleCell(variance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_sumSquare = 0;
        m_sum = 0;
        m_validCount = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the variance per group.";
    }
}