/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.base.node.io.database;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Panel which allows to specify an SQL type for each column.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBSQLTypesPanel extends JPanel {
    
    /** Keep column name from spec to text field which contains the SQL type. */
    private final Map<String, JTextField> m_map;

    /**
     * Creates new empty panel.
     */
    DBSQLTypesPanel() {
        super(new GridLayout(0, 1));
        m_map = new LinkedHashMap<String, JTextField>();
    }
    
    /**
     * Reads settings and inits the panel with column name to SQL-type
     * mapping read from spec and settings object. If no type is available
     * to default types are used.
     * @param settings Settings object to read specified SQL-types from.
     * @param specs The data spec to retrieve all column names and types.
     */
    void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        m_map.clear();
        super.removeAll();
        if (specs == null || specs[0] == null) {
            return;
        }
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            String colName = cspec.getName();
            JLabel label = new JLabel(colName.replaceAll("[^a-zA-Z0-9]", "_"));
            int labelHeight = label.getPreferredSize().height;
            label.setIcon(cspec.getType().getIcon());
            label.setPreferredSize(new Dimension(150, labelHeight));
            label.setMinimumSize(new Dimension(200, labelHeight));
            JTextField textFld = new JTextField();
            textFld.setPreferredSize(new Dimension(250, 25));
            textFld.setMinimumSize(new Dimension(250, 25));
            String type = null;
            if (settings != null) {
                type = settings.getString(colName, null);
            }
            if (type != null) {
                textFld.setText(type);
            } else if (cspec.getType().isCompatible(IntValue.class)) {
                textFld.setText(DBWriterNodeModel.SQL_TYPE_INTEGER);
            } else if (cspec.getType().isCompatible(DoubleValue.class)) {
                textFld.setText(DBWriterNodeModel.SQL_TYPE_DOUBLE);
            } else {
                textFld.setText(DBWriterNodeModel.SQL_TYPE_STRING);
            }
            p.add(label);
            p.add(textFld);
            super.add(p);
            m_map.put(colName, textFld);
        }
    }
    
    /**
     * Saves SQL types by column name. 
     * @param settings Save column to SQL mapping to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        for (Map.Entry<String, JTextField> e : m_map.entrySet()) {
            String type = e.getValue().getText().trim();
            settings.addString(e.getKey(), type);
        }   
    }
}
