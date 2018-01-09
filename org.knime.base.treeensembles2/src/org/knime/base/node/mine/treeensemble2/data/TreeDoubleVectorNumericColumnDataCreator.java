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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   16.03.2016 (adrian): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class TreeDoubleVectorNumericColumnDataCreator implements TreeAttributeColumnDataCreator {

    private static class DoubleTuple implements Comparable<DoubleTuple> {
        private final double m_value;

        private final int m_indexInColumn;

        public DoubleTuple(final double value, final int indexInOriginal) {
            m_value = value;
            m_indexInColumn = indexInOriginal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final DoubleTuple o) {
            return Double.compare(m_value, o.m_value);
        }

    }

    private List<DoubleTuple>[] m_doubleTupleLists;

    private int[] m_missingCounts;

    private int m_index;

    /**
     * @param column DataColumnSpec of the column from which the tree ensemble should be learned
     *
     */
    public TreeDoubleVectorNumericColumnDataCreator(final DataColumnSpec column) {
        if (!column.getType().isCompatible(DoubleVectorValue.class)) {
            throw new IllegalStateException("Can't derive double vector data from non-doublevector column: " + column);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptsMissing() {
        // missing means in this case that the whole vector is missing
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final RowKey rowKey, final DataCell cell) {
        if (cell.isMissing()) {
            throw new IllegalStateException("Missing values not supported");
        }
        final DoubleVectorValue v = (DoubleVectorValue)cell;
        final long lengthLong = v.getLength();
        if (lengthLong > Integer.MAX_VALUE) {
            throw new IllegalStateException("Sparse double vectors not supported");
        }
        final int length = (int)lengthLong;

        if (m_doubleTupleLists == null) {
            m_doubleTupleLists = new ArrayList[length];
            m_missingCounts = new int[length];
            for (int i = 0; i < length; i++) {
                m_doubleTupleLists[i] = new ArrayList<DoubleTuple>();
            }
        } else if (m_doubleTupleLists.length != length) {
            throw new IllegalArgumentException("Double vectors in table have different length, expected "
                + m_doubleTupleLists.length + " Doubles but got " + length + " Doubles in row \"" + rowKey + "\"");
        }

        for (int attrIndex = 0; attrIndex < length; attrIndex++) {
            double val = v.getValue(attrIndex);
            DoubleTuple tuple = new DoubleTuple(val, m_index);
            m_doubleTupleLists[attrIndex].add(tuple);
            if (val == Double.NaN) {
                m_missingCounts[attrIndex]++;
            }
        }
        m_index++;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeAttributeColumnData createColumnData(final int attributeIndex,
        final TreeEnsembleLearnerConfiguration configuration) {
        DoubleTuple[] tuples =
            m_doubleTupleLists[attributeIndex].toArray(new DoubleTuple[m_doubleTupleLists[attributeIndex].size()]);
        Arrays.sort(tuples);
        double[] sortedData = new double[tuples.length];
        int[] sortIndex = new int[tuples.length];
        for (int i = 0; i < tuples.length; i++) {
            DoubleTuple t = tuples[i];
            sortedData[i] = t.m_value;
            sortIndex[i] = t.m_indexInColumn;
        }
        final String n = TreeNumericColumnMetaData.getAttributeNameDouble(attributeIndex);
        TreeNumericColumnMetaData metaData = new TreeNumericColumnMetaData(n);
        final int missingCount = m_missingCounts[attributeIndex];
        return new TreeDoubleVectorNumericColumnData(metaData, configuration, sortIndex, sortedData,
            sortedData.length - missingCount, missingCount > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrAttributes() {
        if (m_doubleTupleLists == null) {
            throw new IllegalStateException("No rows have been added or all were rejected due to missing values");
        }
        return m_doubleTupleLists.length;
    }

}
