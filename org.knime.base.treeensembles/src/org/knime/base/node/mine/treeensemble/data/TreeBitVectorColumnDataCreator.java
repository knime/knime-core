/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 12, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.util.BitSet;

import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.vector.bitvector.BitVectorValue;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeBitVectorColumnDataCreator implements TreeAttributeColumnDataCreator {

    private int m_index = 0;

    private BitSet[] m_bitSets;

    /**
     * @param column  */
    public TreeBitVectorColumnDataCreator(final DataColumnSpec column) {
        if (!column.getType().isCompatible(BitVectorValue.class)) {
            throw new IllegalStateException("Can't derive bit vector data " + "from non-bitvector column: " + column);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean acceptsMissing() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void add(final RowKey rowKey, final DataCell cell) {
        if (cell.isMissing()) {
            throw new IllegalStateException("Missing values not supported");
        }
        BitVectorValue v = (BitVectorValue)cell;
        final long lengthLong = v.length();
        if (lengthLong > Integer.MAX_VALUE) {
            throw new IllegalStateException("Sparse bit vectors not supported");
        }
        final int length = (int)lengthLong;
        if (m_bitSets == null) {
            m_bitSets = new BitSet[length];
            for (int i = 0; i < length; i++) {
                m_bitSets[i] = new BitSet();
            }
        } else if (m_bitSets.length != length) {
            throw new IllegalArgumentException("Bit vectors in table have different length, expected "
                + m_bitSets.length + " bits but got " + length + " bits in row \"" + rowKey + "\"");
        }
        for (int attrIndex = 0; attrIndex < length; attrIndex++) {
            m_bitSets[attrIndex].set(m_index, v.get(attrIndex));
        }
        m_index++;
    }

    /** {@inheritDoc} */
    @Override
    public TreeAttributeColumnData createColumnData(final int attributeIndex,
        final TreeEnsembleLearnerConfiguration configuration) {
        BitSet columnBitSet = m_bitSets[attributeIndex];
        String attName = TreeBitColumnMetaData.getAttributeName(attributeIndex);
        TreeBitColumnMetaData metaData = new TreeBitColumnMetaData(attName);
        return new TreeBitVectorColumnData(metaData, configuration, columnBitSet, m_index);
    }

    /** {@inheritDoc} */
    @Override
    public int getNrAttributes() {
        if (m_bitSets == null) {
            throw new IllegalStateException("No rows have been added or " + "all were rejected due to missing values");
        }
        return m_bitSets.length;
    }

}
