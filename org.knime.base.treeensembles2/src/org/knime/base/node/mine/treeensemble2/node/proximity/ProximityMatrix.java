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
 *   05.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity;

import java.util.Arrays;
import java.util.Comparator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Adrian Nembach
 */
public abstract class ProximityMatrix {

    protected abstract double getEntryAt(int row, int col);

    protected abstract double[] getRowAt(int row);

    protected abstract RowKey getRowKeyForTable(int tableIndex, int row);

    protected abstract int getNumRows();

    protected abstract int getNumCols();

    public BufferedDataTable createTable(final ExecutionContext exec) throws CanceledExecutionException {
        int numCols = getNumCols();
        int numRows = getNumRows();
        DataColumnSpec[] colSpecs = new DataColumnSpec[numCols];
        for (int i = 0; i < colSpecs.length; i++) {
            colSpecs[i] = new DataColumnSpecCreator(getRowKeyForTable(1, i).getString(), DoubleCell.TYPE).createSpec();
        }
        DataTableSpec tableSpec = new DataTableSpec(colSpecs);
        BufferedDataContainer container = exec.createDataContainer(tableSpec);
        for (int i = 0; i < numRows; i++) {
            exec.checkCanceled();
            exec.setProgress(((double)i) / numRows, "Row " + i + "/" + numRows);
            DataCell[] cells = new DataCell[numCols];
            for (int j = 0; j < numCols; j++) {
                cells[j] = new DoubleCell(getEntryAt(i, j));
            }
            container.addRowToTable(new DefaultRow(getRowKeyForTable(0, i), cells));
        }
        container.close();
        return container.getTable();
    }

    public BufferedDataTable[] getNearestNeighbors(final ExecutionContext exec, final int k)
        throws CanceledExecutionException {

        int numCols = getNumCols();
        int numRows = getNumRows();
        if (k < 0 || k >= numCols) {
            throw new IllegalArgumentException("k must be within the range of the proximity matrix.");
        }
        DataTableSpec[] tableSpecs = createNearestNeighborOutSpecs(k);
        BufferedDataContainer containerNeighbors = exec.createDataContainer(tableSpecs[0]);
        BufferedDataContainer containerProximities = exec.createDataContainer(tableSpecs[1]);
        Integer[] idx = new Integer[numCols];
        for (int i = 0; i < numCols; i++) {
            idx[i] = i;
        }
        for (int i = 0; i < numRows; i++) {
            exec.checkCanceled();
            exec.setProgress(((double)i + 1) / numRows, "Row " + (i + 1) + "/" + numRows);
            final double[] matRow = getRowAt(i);
            Arrays.sort(idx, new Comparator<Integer>() {
                @Override
                public int compare(final Integer arg0, final Integer arg1) {
                    return Double.compare(matRow[arg1], matRow[arg0]);
                }
            });
            DataCell[][] cells = new DataCell[2][k];
            for (int j = 0; j < k; j++) {
                cells[0][j] = new StringCell(getRowKeyForTable(1, idx[j]).getString());
                cells[1][j] = new DoubleCell(getEntryAt(i, j));
            }
            RowKey key = getRowKeyForTable(0, i);
            containerNeighbors.addRowToTable(new DefaultRow(key, cells[0]));
            containerProximities.addRowToTable(new DefaultRow(key, cells[1]));
        }
        containerNeighbors.close();
        containerProximities.close();
        return new BufferedDataTable[]{containerNeighbors.getTable(), containerProximities.getTable()};
    }

    public static DataTableSpec[] createNearestNeighborOutSpecs(final int k) {
        DataColumnSpec[][] colSpecs = new DataColumnSpec[2][k];
        for (int i = 0; i < k; i++) {
            colSpecs[0][i] = new DataColumnSpecCreator((i + 1) + ". neighbor", StringCell.TYPE).createSpec();
            colSpecs[1][i] =
                new DataColumnSpecCreator("Proximity " + (i + 1) + ". neighbor", DoubleCell.TYPE).createSpec();
        }
        return new DataTableSpec[]{new DataTableSpec(colSpecs[0]), new DataTableSpec(colSpecs[1])};
    }

    protected void fillIndexMap(final RowKey[] indexMap, final BufferedDataTable table) {
        int index = 0;
        for (DataRow row : table) {
            indexMap[index++] = row.getKey();
        }
    }

    /**
     * This method MUST be declared synchronized in a subclass.
     *
     * @param indices (a int[][2]) the indices that should be incremented by 1
     */
    public abstract void incrementSync(final int[][] indices);

    /**
     * This method MUST be declared synchronized in a subclass.
     *
     * @param incrementMatrix a matrix with increments for each entry of the proximity matrix (must have the same
     *            dimensions as the proximity matrix) such that proximityMatrix = proximityMatrix + incrementMatrix
     */
    public abstract void incrementSync(final double[][] incrementMatrix);

    public abstract void incrementSync(final int[] indexPair, final double value);

    public abstract void incrementSync(final int rowIdx, final double[] rowValue);

    /**
     * Normalize the matrix by multiplying each entry with <b>normalizer</b>
     *
     * @param normalizer
     */
    public abstract void normalize(final double normalizer);

}
