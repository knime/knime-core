
/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.io.database;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Dialog pane of the database writer.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBWriterDialogPane extends NodeDialogPane {
    
    private final DBDialogPane m_loginPane = new DBDialogPane();
    
    private final JTextField m_table = new JTextField("<table_name>");
    
    private final JCheckBox m_append = 
        new JCheckBox("... to existing table (if any!)");

    private final DBSQLTypesPanel m_typePanel;
    
    /**
     * Creates new dialog.
     */
    DBWriterDialogPane() {
        super();
        
        JPanel tablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tablePanel.setBorder(BorderFactory.createTitledBorder(" Table name "));
        m_table.setPreferredSize(new Dimension(400, 20));
        m_table.setFont(DBDialogPane.FONT);
        tablePanel.add(m_table);
        m_loginPane.add(tablePanel);
        
        JPanel appendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        appendPanel.setBorder(
                BorderFactory.createTitledBorder(" Append data "));
        m_append.setPreferredSize(new Dimension(400, 20));
        m_append.setFont(DBDialogPane.FONT);
        m_append.setToolTipText("Table structure from input and database table"
                + " must match!");
        appendPanel.add(m_append);
        m_loginPane.add(appendPanel);
        
        super.addTab("Settings", m_loginPane);
        
        // add SQL type panel
        m_typePanel = new DBSQLTypesPanel();
        JScrollPane scroll = new JScrollPane(m_typePanel);
        scroll.setPreferredSize(m_loginPane.getPreferredSize());
        super.addTab("SQL types", scroll);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_loginPane.loadSettingsFrom(settings, specs);
        // table name
        m_table.setText(settings.getString("table", "<table_name>"));
        // append data flag
        m_append.setSelected(settings.getBoolean("append_data", false));
        
        // load sql type for each column
        try {
            NodeSettingsRO typeSett = settings.getNodeSettings(
                    DBWriterNodeModel.CFG_SQL_TYPES);
            m_typePanel.loadSettingsFrom(typeSett, specs);
        } catch (InvalidSettingsException ise) {
            m_typePanel.loadSettingsFrom(null, specs);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_loginPane.saveSettingsTo(settings);
        
        settings.addString("table", m_table.getText().trim());
        settings.addBoolean("append_data", m_append.isSelected());
        
        // save sql type for each column
        NodeSettingsWO typeSett = settings.addNodeSettings(
                DBWriterNodeModel.CFG_SQL_TYPES);
        m_typePanel.saveSettingsTo(typeSett);
    }
}
