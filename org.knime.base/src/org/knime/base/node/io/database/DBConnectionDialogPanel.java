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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBConnectionDialogPanel extends JPanel {
    
    private final DialogComponentButtonGroup m_group;
    
    /**
     * Options the handle incoming SQL statement.
     */
    enum DBTableOptions implements ButtonGroupEnumInterface {
        /** Wraps the SQL into a new SELECT statement. */
        WRAP_SQL("keep_sql", "Keep SQL query", true),
        /** Creates a new TABLE in the database. */
        CREATE_TABLE("create_table", "Create TABLE", false);
        private final boolean m_isDefault;
        private final String m_label;
        private final String m_id;
        /**
         * Creates a new table option object.
         * @param id the identifier
         * @param label the label of this component
         * @param isDefault if default
         */
        DBTableOptions(final String id, final String label, 
                final boolean isDefault) {
            m_isDefault = isDefault;
            m_label = label;
            m_id = id;
        }
        /**
         * {@inheritDoc}
         */
        public String getActionCommand() {
            return m_id;
        }
        /**
         * {@inheritDoc}
         */
        public String getText() {
            return m_label;
        }
        /**
         * {@inheritDoc}
         */
        public String getToolTip() {
            return null;
        }
        /**
         * {@inheritDoc}
         */
        public boolean isDefault() {
            return m_isDefault;
        }
    }
    
    /**
     * Creates a new panel used to select number of rows to cache and the 
     * method to handle the SQL statement.
     */
    DBConnectionDialogPanel() {
        super(new BorderLayout());
        m_group = new DialogComponentButtonGroup(createTableModel(), null, true,
                DBTableOptions.values());
        super.add(m_group.getComponentPanel(), BorderLayout.NORTH);
    }
    
    /**
     * @return new settings model to create wrapped SQL statement
     */
    static final SettingsModelString createTableModel() {
        return new SettingsModelString("table_option", 
                DBTableOptions.WRAP_SQL.m_label);
    }
    
    /**
     * Loads dialog settings. 
     * @param settings to load
     * @param ports input spec 
     * @throws NotConfigurableException if the settings are not applicable
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] ports) throws NotConfigurableException {
        m_group.loadSettingsFrom(settings, ports);
    }
    
    /**
     * Saves the dialog settings.
     * @param settings to save into
     * @throws InvalidSettingsException if the settings can't be saved
     */
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_group.saveSettingsTo(settings);
    }
    
}
