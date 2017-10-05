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
package org.knime.base.data.aggregation.general;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;

/**
 * Abstract base class for range aggregation operators that perform calculations using the minimal and the maximal
 * element of a group.
 *
 * @since 3.4
 *
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
public abstract class AbstractRangeOperator extends AggregationOperator {

    private final DataValueComparator m_comparator;

    private DataCell m_min = null;

    private DataCell m_max = null;

    /**
     * Constructor for class AbstractRangeOperator.
     *
     * @param inType the actual (or at least expected) type of the original column spec, used to get a suitable
     *            {@link DataValueComparator}
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected AbstractRangeOperator(final DataType inType, final OperatorData operatorData,
        final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        m_comparator = inType.getComparator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the difference between the largest and the smallest element of the group.";
    }

    /**
     *
     * @param min the group's minimum element, not null
     * @param max the group's maximum element, not null
     * @return the range aggregation operator's result
     */
    protected abstract DataCell getResultInternal(DataCell min, DataCell max);

    /**
     * @return the group's minimum element; note this is only valid after the last call of
     *         {@link #computeInternal(DataCell)}.
     */
    protected DataCell getMin() {
        return m_min;
    }

    /**
     * @return the group's maximum element; note this is only valid after the last call of
     *         {@link #computeInternal(DataCell)}.
     */
    protected DataCell getMax() {
        return m_max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (m_min == null || m_max == null) {
            // this is the first call
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
    protected DataCell getResultInternal() {
        if (m_max == null || m_min == null) {
            // the group contains only missing cells
            return DataType.getMissingCell();
        }
        return getResultInternal(m_min, m_max);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_min = null;
        m_max = null;
    }
}
