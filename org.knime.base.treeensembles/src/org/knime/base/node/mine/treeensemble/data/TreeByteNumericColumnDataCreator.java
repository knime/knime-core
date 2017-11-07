/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   20.10.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.vector.bytevector.ByteVectorValue;

/**
 *
 * @author Adrian Nembach
 */
public class TreeByteNumericColumnDataCreator implements TreeAttributeColumnDataCreator {

    private static final int MAX_COUNT = (1 << Byte.SIZE) - 1;

    private List<ByteTuple>[] m_byteTupleLists;

    private int m_index;

    /**
     * @param column
     */
    public TreeByteNumericColumnDataCreator(final DataColumnSpec column) {
        if (!column.getType().isCompatible(ByteVectorValue.class)) {
            throw new IllegalStateException("Can't derive byte vector data " + "from non-bytevector column: " + column);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptsMissing() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public void add(final RowKey rowKey, final DataCell cell) {
        if (cell.isMissing()) {
            throw new IllegalStateException("Missing values not supported");
        }
        ByteVectorValue v = (ByteVectorValue)cell;
        final long lengthLong = v.length();
        if (lengthLong > Integer.MAX_VALUE) {
            throw new IllegalStateException("Sparse byte vectors not supported");
        }
        final int length = (int)lengthLong;

        if (m_byteTupleLists == null) {
            m_byteTupleLists = new ArrayList[length];
            for (int i = 0; i < length; i++) {
                m_byteTupleLists[i] = new ArrayList<ByteTuple>();
            }
        } else if (m_byteTupleLists.length != length) {
            throw new IllegalArgumentException("Byte vectors in table have different length, expected "
                + m_byteTupleLists.length + " bytes but got " + length + " bytes in row \"" + rowKey + "\"");
        }

        for (int attrIndex = 0; attrIndex < length; attrIndex++) {
            ByteTuple tuple = new ByteTuple();
            int val = v.get(attrIndex);
            if (val > MAX_COUNT) {
                throw new IllegalArgumentException(
                    "The value \"" + val + "\" is larger than the maximum value \"" + MAX_COUNT + "\".");
            } else if (val < 0) {
                throw new IllegalArgumentException("Negative values are not allowed.");
            }
            tuple.m_value = (byte)val;
            tuple.m_indexInColumn = m_index;
            m_byteTupleLists[attrIndex].add(tuple);
        }
        m_index++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeAttributeColumnData createColumnData(final int attributeIndex,
        final TreeEnsembleLearnerConfiguration configuration) {
        ByteTuple[] tuples =
            m_byteTupleLists[attributeIndex].toArray(new ByteTuple[m_byteTupleLists[attributeIndex].size()]);
        Arrays.sort(tuples);
        byte[] sortedData = new byte[tuples.length];
        int[] sortIndex = new int[tuples.length];
        for (int i = 0; i < tuples.length; i++) {
            ByteTuple t = tuples[i];
            sortedData[i] = t.m_value;
            sortIndex[i] = t.m_indexInColumn;
        }
        final String n = TreeNumericColumnMetaData.getAttributeName(attributeIndex);
        TreeNumericColumnMetaData metaData = new TreeNumericColumnMetaData(n);
        return new TreeByteNumericColumnData(metaData, configuration, sortedData, sortIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrAttributes() {
        if (m_byteTupleLists == null) {
            throw new IllegalStateException("No rows have been added or " + "all were rejected due to missing values");
        }
        return m_byteTupleLists.length;
    }

    private class ByteTuple implements Comparable<ByteTuple> {

        private byte m_value;

        private int m_indexInColumn;

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final ByteTuple o) {
            int val1 = m_value & 0xFF;
            int val2 = o.m_value & 0xFF;
            int comp = Integer.compare(val1, val2);
            if (comp == 0) {
                return m_indexInColumn - o.m_indexInColumn;
            }
            return comp;
        }

    }

}
