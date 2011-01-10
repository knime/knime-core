/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
