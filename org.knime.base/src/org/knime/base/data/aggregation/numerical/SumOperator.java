/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;

/**
 * Returns the sum per group.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SumOperator extends AggregationOperator {

    private final DataType m_type;
    private boolean m_valid = false;
    private double m_sum = 0;

    /**Constructor for class SumOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public SumOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Sum_V2.5.2", "Sum", "Sum", false, false,
                DoubleValue.class, false), globalSettings, opColSettings);
    }

    /**Constructor for class SumOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected SumOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        final DataColumnSpec origSpec = opColSettings.getOriginalColSpec();
        if (origSpec != null) {
            //the spec is null during registration of the operator
            m_type = getResultType(origSpec.getType());
        } else {
            m_type = DoubleCell.TYPE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return getResultType(origType);
    }

    private DataType getResultType(final DataType origType) {
        if (origType.isCompatible(IntValue.class)) {
            return IntCell.TYPE;
        } else if (origType.isCompatible(LongValue.class)) {
            return LongCell.TYPE;
        } else {
            return DoubleCell.TYPE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new SumOperator(globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        m_valid = true;
        final double d = ((DoubleValue)cell).getDoubleValue();
        m_sum += d;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (!m_valid) {
            return DataType.getMissingCell();
        }
        if (IntCell.TYPE.equals(m_type)) {
            //check if the double value is to big for an integer
            if (m_sum > Integer.MAX_VALUE) {
                setSkipped(true);
                setSkipMessage("Sum > maximum int value. "
                        + "Convert column to long.");
                return DataType.getMissingCell();
            }
            return new IntCell((int)m_sum);
        } else if (LongCell.TYPE.equals(m_type)) {
            //check if the double value is to big for a long
            if (m_sum > Long.MAX_VALUE) {
                setSkipped(true);
                setSkipMessage("Sum > maximum long value. "
                        + "Convert column to double.");
                return DataType.getMissingCell();
            }
            return new LongCell((long)m_sum);
        }
        return new DoubleCell(m_sum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_valid = false;
        m_sum = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the sum per group. For int and long cells, the "
            + "operator might return a missing cell and mark the column as "
            + "skipped if the sum exceeds the limit of int (2<sup>31</sup>-1)"
            + " resp. long (2<sup>62</sup>-1).";
    }
}