/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation.bitvector;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;

/**
 * Returns the intersection of the bit vectors for all members of a group.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class BitVectorIntersectionOperator extends AggregationOperator {

    private final DataType m_type = DenseBitVectorCell.TYPE;

    private DenseBitVectorCell m_v = null;

    /**Constructor for class TrueCountOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public BitVectorIntersectionOperator(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Bit vector intersection", "BV intersection", false, false, BitVectorValue.class, false),
                globalSettings, AggregationOperator.setInclMissingFlag(opColSettings, false));
    }

    /**Constructor for class TrueCountOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public BitVectorIntersectionOperator(final OperatorData operatorData, final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
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
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new BitVectorIntersectionOperator(globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell instanceof BitVectorValue) {
            BitVectorValue val = (BitVectorValue) cell;
            if (m_v == null) {
                final DenseBitVectorCellFactory dbv = new DenseBitVectorCellFactory(val.length());
                long nextSetBit = val.nextSetBit(0);
                while (nextSetBit >= 0) {
                    dbv.set(nextSetBit);
                    nextSetBit = val.nextSetBit(nextSetBit + 1);
                }
                m_v = dbv.createDataCell();
            } else {
                m_v = DenseBitVectorCellFactory.and(m_v, val);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_v == null) {
            return DataType.getMissingCell();
        }
        return m_v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_v = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Computes the intersection of the bit vectors per group.";
    }
}
