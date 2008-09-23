/* This source code, its documentation and all appendant files
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
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.JPanel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.tableview.TableView;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DataColumnPropertiesView extends JPanel {
    
    private final DataTableSpec m_tableSpec;
    
    private final TableView m_propsView;
    
    private final Object m_lock = new Object();
    
    /**
     * 
     * @param tableSpec data table spec to extract the data column properties 
     *  from
     */
    public DataColumnPropertiesView(final DataTableSpec tableSpec) {
        setLayout(new BorderLayout());
        m_tableSpec = tableSpec;
        m_propsView = new TableView();
        m_propsView.setShowIconInColumnHeader(false);
        m_propsView.getHeaderTable().setShowColorInfo(false);
        add(m_propsView);
        updatePropsTable();
    }

    
    private DataTable createPropsTable() {
        if (m_tableSpec != null) {
            // output has as many cols
            int numOfCols = m_tableSpec.getNumColumns(); 
            String[] colNames = new String[numOfCols];
            DataType[] colTypes = new DataType[numOfCols];
            // colnames are the same as incoming, types are all StringTypes
            for (int c = 0; c < numOfCols; c++) {
                colNames[c] = m_tableSpec.getColumnSpec(c).getName();
                colTypes[c] = StringCell.TYPE;
            }
            // get keys for ALL props in the table. Each will show in one row.
            HashSet<String> allKeys = new HashSet<String>();
            for (int c = 0; c < numOfCols; c++) {
                Enumeration<String> props =
                    m_tableSpec.getColumnSpec(c).getProperties().properties();
                while (props.hasMoreElements()) {
                    allKeys.add(props.nextElement());
                }
            }
            DataContainer result =
                    new DataContainer(new DataTableSpec(colNames, colTypes));

            // now construct the rows we wanna display
            for (String key : allKeys) {
                DataCell[] cells = new DataCell[numOfCols];
                for (int c = 0; c < numOfCols; c++) {
                    String cellValue = "";
                    if (m_tableSpec.getColumnSpec(c).getProperties()
                            .containsProperty(key)) {
                        cellValue =
                            m_tableSpec.getColumnSpec(c).getProperties()
                                        .getProperty(key);
                    }
                    cells[c] = new StringCell(cellValue);
                }
                result.addRowToTable(new DefaultRow(key, cells));
            }
            result.close();
            return result.getTable();

        } else {
            DataContainer result =
                    new DataContainer(new DataTableSpec(
                            new String[]{"No outgoing table spec"},
                            new DataType[]{StringCell.TYPE}));
            result.close();
            return result.getTable();
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        int numOfCols = m_tableSpec.getNumColumns();
        return "DataColumnProperties: " 
            + numOfCols + " Column" + (numOfCols > 1 ? "s" : "");
    }
    
    private void updatePropsTable() {
        synchronized (m_lock) {
            m_propsView.setDataTable(createPropsTable());
        }
    }
}
