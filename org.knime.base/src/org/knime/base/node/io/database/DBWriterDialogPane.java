
/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
    
    private final JTextField m_table = new JTextField("");
    
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
