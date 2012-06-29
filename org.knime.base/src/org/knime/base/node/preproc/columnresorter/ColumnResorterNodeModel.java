/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *   28.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.columnresorter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The node model of the column resorter node, re sorting columns based on
 * dialog settings.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class ColumnResorterNodeModel extends NodeModel {

    private String[] m_order = new String[] {};
    
    /**
     * Creates new instance of <code>ColumnResorterNodeModel</code>.
     */
    ColumnResorterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // if no order has been specified
        if (m_order.length <= 0) {
            setWarningMessage("All columns stay in same order.");
            return inSpecs;
        }

        DataTableSpec original = inSpecs[0];
        ColumnRearranger rearranger = createColumnRearranger(original);
        return new DataTableSpec[]{rearranger.createSpec()};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        DataTableSpec original = in.getDataTableSpec();
        
        // if node has not been configured, just pass input data table through
        if (m_order.length <= 0) {
            return new BufferedDataTable[] {in};
        }
        
        ColumnRearranger rearranger = createColumnRearranger(original);
        return new BufferedDataTable[] {exec.createColumnRearrangeTable(
                in, rearranger, exec)};
    }
    
    /**
     * Creates and returns an instance of the column rearranger which re sorts
     * the input columns in a user specified way.
     * @param original The data table spec of the original input table.
     * @return The rearranger to resort the columns.
     */
    private ColumnRearranger createColumnRearranger(
            final DataTableSpec original) {
        ColumnRearranger rearranger = new ColumnRearranger(original);
        String[] newColOder = getNewOrder(original);
        rearranger.permute(newColOder);
        return rearranger;
    }
    
    /**
     * Returns <code>true</code> if given column name is contained in order
     * array, otherwise <code>false</code>.
     * @param col The column name to search for
     * @return <code>true</code> if given column name is contained in order
     * array, otherwise <code>false</code>.
     */
    private boolean isContainedInOrder(final String col) {
        for (String s : m_order) {
            if (s.equals(col)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Creates and returns arrays of column names in new order, based on the
     * dialog settings.
     * @param orig The original table spec.
     * @return Array of column names in new order.
     */
    private String[] getNewOrder(final DataTableSpec orig) {
        String[] newOrder = new String[orig.getNumColumns()];
        
        // collect unknown columns which are not in order settings so far
        List<String> unknownNewColumns = new ArrayList<String>();
        for (int i = 0; i < orig.getNumColumns(); i++) {
            DataColumnSpec col = orig.getColumnSpec(i);
            if (!isContainedInOrder(col.getName())) {
                unknownNewColumns.add(col.getName());
            }
        }
        int delta = unknownNewColumns.size();
        
        // walk through cols and (re)define order
        int j = 0;
        for (int i = 0; i < newOrder.length; i++) {
            // get next column from dialog to order
            String colName = m_order[j];
            j++;
            
            // if col exists in orig spec, put it at index i
            if (orig.containsName(colName)) {
                newOrder[i] = colName;
                
            // if col to resort is the dummy place holder
            } else if (DataColumnSpecListDummyCellRenderer.UNKNOWN_COL_DUMMY
                    .getName().equals(colName)) {
                // only add new cols to place holder place if there are any
                if (delta > 0) {
                    for (int x = 0; x < delta; x++) {
                        newOrder[i] = unknownNewColumns.get(x);
                        // for more then one column to add at place holder
                        // increment counter
                        if (x < delta - 1) {
                            i++;
                        }
                    }
                // if no columns to insert for place holder decrease insert 
                // index
                } else {
                    i--;
                }
            // if column does not exist and is not the place holder decrease
            // insert index
            } else {
                i--;
            }
        }
        return newOrder;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addStringArray(ColumnResorterConfigKeys.COLUMN_ORDER, m_order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(ColumnResorterConfigKeys.COLUMN_ORDER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_order = settings.getStringArray(
                ColumnResorterConfigKeys.COLUMN_ORDER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }    
}
