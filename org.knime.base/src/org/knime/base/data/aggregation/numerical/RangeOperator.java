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
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class RangeOperator extends AggregationOperator {

    private DataCell m_min = null;

    private DataCell m_max = null;


    private final DataValueComparator m_comparator;

    private final DataType m_type;

    /**Constructor for class RangeOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected RangeOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        if (opColSettings.getOriginalColSpec() != null) {
            m_type = opColSettings.getOriginalColSpec().getType();
            m_comparator = m_type.getComparator();
        } else {
            m_type = DoubleCell.TYPE;
            m_comparator = DoubleCell.TYPE.getComparator();
        }
    }

    /**Constructor for class RangeOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public RangeOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Range", false, false, DoubleValue.class, false),
                globalSettings, setInclMissingFlag(opColSettings));
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
        return new RangeOperator(getOperatorData(), globalSettings, opColSettings);
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
        //if the input is an int/long return also an int/long
        if (m_type == IntCell.TYPE || m_type == LongCell.TYPE) {
            return origType;
        }
        //if not always return a double
        return DoubleCell.TYPE;
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
        if (m_type == IntCell.TYPE) {
            final int range = ((IntValue)m_max).getIntValue()
                    - ((IntValue)m_min).getIntValue();
            return new IntCell(range);
        } else if (m_type == LongCell.TYPE) {
            final long range = ((LongValue)m_max).getLongValue()
                - ((LongValue)m_min).getLongValue();
            return new LongCell(range);
        }
        final double range = ((DoubleValue)m_max).getDoubleValue()
                - ((DoubleValue)m_min).getDoubleValue();
        return new DoubleCell(range);
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
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the difference between the "
        + "largest and smallest value.";
    }

}
