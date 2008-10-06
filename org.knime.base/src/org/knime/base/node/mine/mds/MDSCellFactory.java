/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   10.03.2008 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds;

import java.util.Hashtable;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * A {@link CellFactory} adding the MDS values as cells to the corresponding 
 * rows.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MDSCellFactory implements CellFactory {

    private static final NodeLogger LOGGER = NodeLogger
    .getLogger(MDSCellFactory.class); 
    
    private Hashtable<RowKey, DataPoint> m_points;
    
    private int m_dimension;
    
    /**
     * Creates a new instance of <code>MDSCellFactory</code> with given
     * <code>Hashtable</code> containing the MDS points to add and the related
     * row keys.
     * 
     * @param points The MDS points to add and the row keys.
     * @param dimension The dimension of the MDS points.
     */
    public MDSCellFactory(final Hashtable<RowKey, DataPoint> points,
            final int dimension) {
        m_points = points;
        m_dimension = dimension;
    }
    
    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        DataCell[] cells = new DataCell[m_dimension];
        if (m_points.containsKey(row.getKey())) {
            DataPoint p = m_points.get(row.getKey());
            for (int i = 0; i < m_dimension; i++) {
                cells[i] = new DoubleCell(p.getElementAt(i));
            }
        } else {
            LOGGER.warn("No MDS data point found for \"" 
                   + row.getKey().getString() + "\", inserting missing cells.");
            for (int i = 0; i < m_dimension; i++) {
                cells[i] = DataType.getMissingCell();
            }
        }
        return cells;
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        return MDSNodeModel.getColumnSpecs(m_dimension);
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount, 
            final RowKey lastKey, final ExecutionMonitor exec) {
        double prog = (double)curRowNr / (double)rowCount;
        exec.setProgress(prog, "Creating row " + curRowNr + " of " + rowCount);
    }
}
