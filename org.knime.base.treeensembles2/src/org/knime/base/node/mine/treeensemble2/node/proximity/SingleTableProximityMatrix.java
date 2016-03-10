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
 *   07.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity;

import org.knime.base.util.HalfDoubleMatrix;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;

/**
 *
 * @author Adrian Nembach
 */
public class SingleTableProximityMatrix extends ProximityMatrix {

    private final HalfDoubleMatrix m_data;

    private final RowKey[] m_index2RowKey;

    public SingleTableProximityMatrix(final BufferedDataTable table) {
        long lengthLong = table.size();
        if (lengthLong > 65500) {
            throw new IllegalArgumentException("This proximity matrix supports a maximal column/row count of 65500.");
        }
        int length = (int)lengthLong;
        m_data = new HalfDoubleMatrix(length, true);
        m_index2RowKey = new RowKey[length];
        fillIndexMap(m_index2RowKey, table);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double getEntryAt(final int row, final int col) {
        return m_data.get(row, col);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double[] getRowAt(final int row) {
        final double[] result = new double[m_index2RowKey.length];
        for (int c = 0; c < result.length; c++) {
            result[c] = m_data.get(row, c);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RowKey getRowKeyForTable(final int tableIndex, final int row) {
        if (tableIndex < 0 || tableIndex > 1) {
            throw new IndexOutOfBoundsException();
        }
        return m_index2RowKey[row];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNumRows() {
        return m_index2RowKey.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNumCols() {
        return m_index2RowKey.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final int[][] indices) {
        for (int[] indexPair : indices) {
            m_data.add(indexPair[0], indexPair[1], 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void normalize(final double normalizer) {
        for (int r = 0; r < m_index2RowKey.length; r++) {
            for (int c = r; c < m_index2RowKey.length; c++) {
                double val = m_data.get(r, c);
                m_data.set(r, c, val * normalizer);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final double[][] incrementMatrix) {
        // this assumes that incrementMatrix is also a half matrix
        for (int r = 0; r < m_data.getRowCount(); r++) {
            for (int c = r; c < incrementMatrix[r].length; c++) {
                m_data.add(r, c, incrementMatrix[r][c]);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final int[] indexPair, final double value) {
        if (indexPair.length != 2) {
            throw new IllegalArgumentException("The given index pair must two entries.");
        }
        m_data.add(indexPair[0], indexPair[1], value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final int rowIdx, final double[] rowValue) {
        int rowCount = m_data.getRowCount();
        if (rowIdx >= rowCount) {
            throw new IllegalArgumentException(rowIdx + " is not a valid row index.");
        }
        if (rowValue.length != rowCount) {
            throw new IllegalArgumentException(
                "The given row must have the same number of columns as the proximity matrix.");
        }
        // the half matrix only stores half of the entries
        for (int c = rowIdx; c < rowValue.length; c++) {
            m_data.add(rowIdx, c, rowValue[c]);
        }
    }

}
