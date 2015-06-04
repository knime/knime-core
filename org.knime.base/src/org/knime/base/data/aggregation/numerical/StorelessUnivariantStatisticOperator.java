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
 *   May 19, 2015 (Lara): created
 */
package org.knime.base.data.aggregation.numerical;

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Computes statistical values without storing observations
 *
 * @author Lara Gorini
 * @since 2.12
 */
public abstract class StorelessUnivariantStatisticOperator extends AggregationOperator {

    private static final DataType TYPE = DoubleCell.TYPE;

    /**
     * Type of statistic
     */
    protected StorelessUnivariateStatistic m_stat;

    /**
     * Constructor for class StorelessUnivariantStatisticOperator.
     *
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     * @param stat type of statistic
     */
    protected StorelessUnivariantStatisticOperator(final OperatorData operatorData,
        final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings,
        final StorelessUnivariateStatistic stat) {
        super(operatorData, globalSettings, opColSettings);
        m_stat = stat;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        double val = ((DoubleValue)cell).getDoubleValue();
        m_stat.increment(val);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return TYPE;
    }

    @Override
    protected DataCell getResultInternal() {
        double result = m_stat.getResult();
        return new DoubleCell(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_stat.clear();
    }

}
