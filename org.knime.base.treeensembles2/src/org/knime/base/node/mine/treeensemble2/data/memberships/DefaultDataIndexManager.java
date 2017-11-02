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
 *   07.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;

/**
 * This class is the {@link IDataIndexManager} implementation for all column types in which the
 * original positions differ from the positions in the individual columns. <br>
 * Currently this includes all column types except for the BitVector type.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class DefaultDataIndexManager implements IDataIndexManager {

    private final int[][] m_original2Column;
    private final int[][] m_column2Original;

    /**
     * Constructs a DataIndexManager from the given TreeData object
     * @param data
     */
    public DefaultDataIndexManager(final TreeData data) {
        int numRows = data.getNrRows();
        int numCols = data.getNrAttributes();
        m_original2Column = new int[numCols][numRows];
        m_column2Original = new int[numCols][numRows];
        TreeAttributeColumnData[] columnData = data.getColumns();

        for (int c = 0; c < numCols; c++) {
            TreeAttributeColumnData column = columnData[c];
            int[] originalIndices = column.getOriginalIndicesInColumnList();

            for (int i = 0; i < numRows; i++) {
                m_original2Column[c][originalIndices[i]] = i;
            }
            m_column2Original[c] = originalIndices;
        }
    }

    @Override
    public int[] getPositionsInColumn(final int colIndex) {
        return m_original2Column[colIndex];
    }

    @Override
    public int[] getOriginalPositions(final int colIndex) {
        return m_column2Original[colIndex];
    }

    @Override
    public int getPositionInColumn(final int colIndex, final int originalPosition) {
        return m_original2Column[colIndex][originalPosition];
    }

    @Override
    public int getOriginalPosition(final int colIndex, final int positionInColumn) {
        return m_column2Original[colIndex][positionInColumn];
    }
}
