/* This source code, its documentation and all appendant files
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
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BufferedDataTableView extends JComponent {

    private final TableView m_dataView;

    private DataTable m_table;

    /** 
     * Updates are synchronized on this object. Declaring the methods
     * as synchronized (i.e. using "this" as mutex) does not work as swing
     * also acquires locks on this graphical object.
     */
    private final Object m_updateLock = new Object();
    
    /**
     * A view showing the data stored in the specified output port.
     * @param table table to display
     */
    public BufferedDataTableView(final DataTable table) {
        m_table = table;

        setLayout(new BorderLayout());
        setBackground(NodeView.COLOR_BACKGROUND);

        m_dataView = new TableView();
        m_dataView.getHeaderTable().setShowColorInfo(false);
        updateDataTable();
    }

    private void updateDataTable() {
        synchronized (m_updateLock) {
            m_dataView.setDataTable(m_table);
            add(m_dataView);
            revalidate();
        }
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        if (m_table == null) {
            return "No Table";
        }
        StringBuilder result = new StringBuilder("");
        String tableName = m_table.getDataTableSpec().getName(); 
        if (tableName != null) {
            result.append("Table \"" + tableName + "\"");
        } else {
            result.append("");
        }
        if (m_table instanceof BufferedDataTable) {
            String numOfRows = null;
            if (m_table != null) {
                numOfRows = "" + ((BufferedDataTable) m_table).getRowCount();
            }
            if (numOfRows != null) {
                result.append(" - Rows: " + numOfRows);
            }
        }
        return result.toString();
    }


    /**
     * Rest internal data table and reset data out-port view.
     */
    public void dispose() {
        m_table = null;
        m_dataView.setDataTable(null);
    }
    
}
