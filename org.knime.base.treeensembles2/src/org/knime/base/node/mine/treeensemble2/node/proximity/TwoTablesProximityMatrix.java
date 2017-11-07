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
 *   07.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity;

import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;

/**
 *
 * @author Adrian Nembach
 */
public class TwoTablesProximityMatrix extends ProximityMatrix {

    private final double[][] m_data;

    private final RowKey[] m_index2RowKeyTable1;

    private final RowKey[] m_index2RowKeyTable2;

    /**
     * @param table1
     * @param table2
     */
    public TwoTablesProximityMatrix(final BufferedDataTable table1, final BufferedDataTable table2) {
        final long longLength1 = table1.size();
        final long longLength2 = table2.size();
        if (longLength1 > Integer.MAX_VALUE || longLength2 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Tables larger than Integer.MAX_VALUE can currently not be handled.");
        } else if (longLength1 <= 0 || longLength2 <= 0) {
            throw new IllegalArgumentException("Empty tables are not permitted.");
        }
        final int length1 = (int)longLength1;
        final int length2 = (int)longLength2;

        m_data = new double[length1][length2];
        m_index2RowKeyTable1 = new RowKey[length1];
        m_index2RowKeyTable2 = new RowKey[length2];
        fillIndexMap(m_index2RowKeyTable1, table1);
        fillIndexMap(m_index2RowKeyTable2, table2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double getEntryAt(final int row, final int col) {
        return m_data[row][col];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RowKey getRowKeyForTable(final int tableIndex, final int row) {
        switch (tableIndex) {
            case 0:
                return m_index2RowKeyTable1[row];
            case 1:
                return m_index2RowKeyTable2[row];
        }
        throw new IndexOutOfBoundsException("The index " + tableIndex + " is not allowed.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNumRows() {
        return m_index2RowKeyTable1.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNumCols() {
        return m_index2RowKeyTable2.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double[] getRowAt(final int row) {
        return m_data[row];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void normalize(final double normalizer) {
        for (int i = 0; i < m_data.length; i++) {
            for (int j = 0; j < m_data[0].length; j++) {
                m_data[i][j] = m_data[i][j] * normalizer;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final int[][] indices) {
        for (int[] indexPair : indices) {
            m_data[indexPair[0]][indexPair[1]]++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final double[][] incrementMatrix) {
        if (incrementMatrix.length != m_data.length) {
            throw new IllegalArgumentException(
                "The increment matrix must have the same dimensions as the proximity matrix");
        }

        for (int r = 0; r < m_data.length; r++) {
            if (incrementMatrix[r].length != m_data[r].length) {
                throw new IllegalArgumentException(
                    "The increment matrix must have the same dimensions as the proximity matrix");
            }
            for (int c = 0; c < m_data[r].length; c++) {
                m_data[r][c] += incrementMatrix[r][c];
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final int[] indexPair, final double value) {
        m_data[indexPair[0]][indexPair[1]] += value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void incrementSync(final int rowIdx, final double[] rowValue) {
        if (rowIdx >= m_data.length) {
            throw new IllegalArgumentException("rowIdx is too large");
        }

        double[] row = m_data[rowIdx];
        if (row.length != rowValue.length) {
            throw new IllegalArgumentException(
                "The provided row must have the same dimension as the rows of the proximity matrix.");
        }

        for (int c = 0; c < row.length; c++) {
            row[c] += rowValue[c];
        }
    }

}
