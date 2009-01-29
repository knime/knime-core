/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   30.03.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.predictor;

import java.util.ArrayList;
import java.util.Iterator;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.sota.distances.DistanceManager;
import org.knime.base.node.mine.sota.distances.DistanceManagerFactory;
import org.knime.base.node.mine.sota.logic.SotaCellFactory;
import org.knime.base.node.mine.sota.logic.SotaManager;
import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPredictorCellFactory implements CellFactory {

    private int[] m_includedColsIndices;
    
    private DistanceManager m_distanceManager;
    
    private SotaTreeCell m_root;
    
    /**
     * Creates new instance of <code>SotaPredictorCellFactory</code> with given
     * <code>SotaManager</code>, array of indices of columns to use for
     * prediction and the distance metric to use.
     * 
     * @param root The <code>SotaManager</code> to set. 
     * @param indicesOfIncludedColumns The array of indices of columns to use 
     * for prediction.
     * @param distance The distance to use.
     */
    public SotaPredictorCellFactory(final SotaTreeCell root, 
            final int[] indicesOfIncludedColumns, final String distance) {
        m_includedColsIndices = indicesOfIncludedColumns;
        m_root = root;
        
        boolean fuzzy = false;
        if (m_root != null) {
            if (m_root.getCellType().equals(SotaCellFactory.FUZZY_TYPE)) {
                fuzzy = true;
            }
        }
        m_distanceManager = 
            DistanceManagerFactory.createDistanceManager(distance, fuzzy, 1);
        //m_distanceManager = new EuclideanDistanceManager(fuzzy);
    }
    
    /**
     * {@inheritDoc}
     */
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

            ArrayList<SotaTreeCell> cells = new ArrayList<SotaTreeCell>();
            SotaManager.getCells(cells, m_root);

            SotaTreeCell winner = null;
            double minDist = Double.MAX_VALUE;

            for (int j = 0; j < cells.size(); j++) {
                double dist = m_distanceManager.getDistance(
                        filteredRow, cells.get(j));
                
                if (dist < minDist) {
                    winner = cells.get(j);
                    minDist = dist;
                }
            }
            String predClass = SotaTreeCell.DEFAULT_CLASS;
            if (winner != null) {
                predClass = winner.getTreeCellClass();
            }

            return new DataCell[]{new StringCell(predClass)};
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        DataColumnSpecCreator creator = 
            new DataColumnSpecCreator("Predicted class", StringCell.TYPE);
        return new DataColumnSpec[]{creator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount, 
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress("predicting classes ...");
    }

}
