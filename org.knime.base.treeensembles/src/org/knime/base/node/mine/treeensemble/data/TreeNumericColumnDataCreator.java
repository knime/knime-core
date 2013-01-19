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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 27, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeNumericColumnDataCreator implements TreeAttributeColumnDataCreator {
    private final DataColumnSpec m_column;
    private final List<Tuple> m_tuples;

    TreeNumericColumnDataCreator(final DataColumnSpec column) {
        m_column = column;
        m_tuples = new ArrayList<Tuple>();
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
            throw new UnsupportedOperationException(
                    "missing vals not supported");
        }
        Tuple t = new Tuple();
        t.m_value = ((DoubleValue)cell).getDoubleValue();
        t.m_indexInColumn = m_tuples.size();
        m_tuples.add(t);
    }

    /** {@inheritDoc} */
    @Override
    public int getNrAttributes() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNumericColumnData createColumnData(final int attributeIndex,
            final TreeEnsembleLearnerConfiguration configuration) {
        assert attributeIndex == 0;
        Tuple[] tuples = m_tuples.toArray(new Tuple[m_tuples.size()]);
        Arrays.sort(tuples);
        double[] sortedData = new double[tuples.length];
        int[] sortIndex = new int[tuples.length];
        for (int i = 0; i < tuples.length; i++) {
            Tuple t = tuples[i];
            sortedData[i] = t.m_value;
            sortIndex[i] = t.m_indexInColumn;
        }
        final String n = m_column.getName();
        TreeNumericColumnMetaData metaData = new TreeNumericColumnMetaData(n);
        return new TreeNumericColumnData(metaData,
                configuration, sortedData, sortIndex);
    }

    private static class Tuple implements Comparable<Tuple> {
        private double m_value;
        private int m_indexInColumn;
        /** {@inheritDoc} */
        @Override
        public int compareTo(final Tuple o) {
            int comp = Double.compare(m_value, o.m_value);
            if (comp == 0) {
                return m_indexInColumn - o.m_indexInColumn;
            }
            return comp;
        }
    };


}
