/*
 * ------------------------------------------------------------------------
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
 *   30.03.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.predictor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.sota.distances.DistanceManager;
import org.knime.base.node.mine.sota.distances.DistanceManagerFactory;
import org.knime.base.node.mine.sota.logic.SotaCellFactory;
import org.knime.base.node.mine.sota.logic.SotaManager;
import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPredictorCellFactory implements CellFactory {

    /**
     *
     */
    private static final double EPSILON = 1E-9;

    private int[] m_includedColsIndices;

    private DistanceManager m_distanceManager;

    private SotaTreeCell m_root;

    private boolean m_appendProbs;

    private DataTableSpec m_newColumns;

    private ArrayList<SotaTreeCell> m_cells = new ArrayList<SotaTreeCell>();

    private final DataColumnSpec m_targetColumn;

    private final String m_suffix;

    /**
     * Creates new instance of <code>SotaPredictorCellFactory</code> with given <code>SotaManager</code>, array of
     * indices of columns to use for prediction and the distance metric to use.
     *
     * @param root The <code>SotaManager</code> to set.
     * @param indicesOfIncludedColumns The array of indices of columns to use for prediction.
     * @param distance The distance to use.
     * @deprecated Use the other constructor,
     *             {@link #SotaPredictorCellFactory(SotaTreeCell, int[], String, boolean, DataColumnSpec, String, DataColumnSpec[])}
     */
    @Deprecated
    public SotaPredictorCellFactory(final SotaTreeCell root,
            final int[] indicesOfIncludedColumns, final String distance) {
        this(root, indicesOfIncludedColumns, distance, false, null, "", new DataColumnSpec[] {new DataColumnSpecCreator("Predicted class", StringCell.TYPE).createSpec()});
    }

    /**
     * Creates new instance of <code>SotaPredictorCellFactory</code> with given
     * <code>SotaManager</code>, array of indices of columns to use for
     * prediction and the distance metric to use.
     *
     * @param root The <code>SotaManager</code> to set.
     * @param indicesOfIncludedColumns The array of indices of columns to use
     * for prediction.
     * @param distance The distance to use.
     * @param appendProbabilities Add the probability columns.
     * @param targetColumn The target column spec from the learned model.
     * @param suffix The suffix for the probability columns (not {@code null}).
     * @param newColumns The specs for the new columns.
     * @since 2.10
     */
    public SotaPredictorCellFactory(final SotaTreeCell root, final int[] indicesOfIncludedColumns,
        final String distance, final boolean appendProbabilities, final DataColumnSpec targetColumn,
        final String suffix, final DataColumnSpec[] newColumns) {
        m_includedColsIndices = indicesOfIncludedColumns;
        m_root = root;
        this.m_targetColumn = targetColumn;
        this.m_suffix = suffix;
        SotaManager.getCells(m_cells, m_root);

        boolean fuzzy = false;
        if (m_root != null) {
            if (m_root.getCellType().equals(SotaCellFactory.FUZZY_TYPE)) {
                fuzzy = true;
            }
        }
        m_distanceManager =
            DistanceManagerFactory.createDistanceManager(distance, fuzzy, 1);
        //m_distanceManager = new EuclideanDistanceManager(fuzzy);
        m_appendProbs = appendProbabilities && targetColumn != null;
        m_newColumns = new DataTableSpec(newColumns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        if (row != null) {

            DataRow filteredRow =
                new FilterColumnRow(row, m_includedColsIndices);

            Iterator<DataCell> it = filteredRow.iterator();
            while (it.hasNext()) {
                if (it.next().isMissing()) {
                    return new DataCell[]{DataType.getMissingCell()};
                }
            }

            SotaTreeCell winner = null;
            double minDist = Double.MAX_VALUE;

            Map<String, Double> minDists = new HashMap<String, Double>();
            for (int j = 0; j < m_cells.size(); j++) {
                double dist = m_distanceManager.getDistance(
                        filteredRow, m_cells.get(j));

                String treeCellClass = m_cells.get(j).getTreeCellClass();
                if (minDists.containsKey(treeCellClass)) {
                    Double old = minDists.get(treeCellClass);
                    if (old.doubleValue() > dist) {
                        minDists.put(treeCellClass, dist);
                    }
                } else {
                    minDists.put(treeCellClass, dist);
                }
                if (dist < minDist) {
                    winner = m_cells.get(j);
                    minDist = dist;
                }
            }
            String predClass = SotaTreeCell.DEFAULT_CLASS;
            if (winner != null) {
                predClass = winner.getTreeCellClass();
            }
            DataCell[] ret;
            ret = new DataCell[m_newColumns.getNumColumns()];
            if (m_appendProbs) {
                double sumDists = 0d;
                for (Double d : minDists.values()) {
                    sumDists += 1d / Math.max(EPSILON, d.doubleValue());
                }
                for (int i = ret.length; i-->0;) {
                    ret[i] = DataType.getMissingCell();
                }
                for (Entry<String, Double> entry : minDists.entrySet()) {
                    final String target = entry.getKey();
                    final String colName =
                        PredictorHelper.getInstance().probabilityColumnName(m_targetColumn.getName(), target, m_suffix);
                    int colIndex = m_newColumns.findColumnIndex(colName);
                    if (colIndex >= 0) {
                        ret[colIndex] =
                            new DoubleCell(1d / Math.max(EPSILON, entry.getValue().doubleValue()) / sumDists);
                    }
                }
                ret[ret.length - 1] = new StringCell(predClass);
            } else {
                ret = new DataCell[]{new StringCell(predClass)};
            }
            return ret;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        final DataColumnSpec[] ret = new DataColumnSpec[m_newColumns.getNumColumns()];
        for (int i = ret.length; i-->0;) {
            ret[i] = m_newColumns.getColumnSpec(i);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress("predicting classes ...");
    }

}
