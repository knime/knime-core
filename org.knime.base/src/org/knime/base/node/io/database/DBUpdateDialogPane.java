
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Dialog pane of the Database Update node.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
final class DBUpdateDialogPane extends NodeDialogPane {

    private final DataColumnSpecFilterPanel m_columnsInSetClause;
    private final DataColumnSpecFilterPanel m_columnsInWhereClause;

    private final DBDialogPane m_loginPanel;
    private final JTextField m_tableName;

    /** Creates new dialog. */
    DBUpdateDialogPane() {
        final JPanel columnPanel = new JPanel(new BorderLayout());
        m_columnsInSetClause = new DataColumnSpecFilterPanel();
        m_columnsInSetClause.setBorder(BorderFactory.createTitledBorder(" Select SET Columns "));
        columnPanel.add(m_columnsInSetClause, BorderLayout.NORTH);
        m_columnsInWhereClause = new DataColumnSpecFilterPanel();
        m_columnsInWhereClause.setBorder(BorderFactory.createTitledBorder(" Select WHERE Columns "));
        columnPanel.add(m_columnsInWhereClause, BorderLayout.SOUTH);

        m_loginPanel = new DBDialogPane();
        final JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(" Table Name "));
        tablePanel.setFont(DBDialogPane.FONT);
        m_tableName = new JTextField("");
        tablePanel.add(m_tableName, BorderLayout.CENTER);
        m_loginPanel.add(tablePanel);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(m_loginPanel, BorderLayout.NORTH);
        panel.add(columnPanel, BorderLayout.CENTER);
        super.addTab("Settings", panel);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // load login setting
        m_loginPanel.loadSettingsFrom(settings, specs, getCredentialsNames());
        // load table name
        m_tableName.setText(settings.getString("table_name", ""));
        // load SET column panel
        DataColumnSpecFilterConfiguration configSet = new DataColumnSpecFilterConfiguration(
            DBUpdateNodeModel.KEY_SET_FILTER_COLUMN);
        configSet.loadConfigurationInDialog(settings, specs[0]);
        m_columnsInSetClause.loadConfiguration(configSet, specs[0]);
        // load WHERE column panel
        DataColumnSpecFilterConfiguration configWhere = new DataColumnSpecFilterConfiguration(
            DBUpdateNodeModel.KEY_WHERE_FILTER_COLUMN);
        configWhere.loadConfigurationInDialog(settings, specs[0]);
        m_columnsInWhereClause.loadConfiguration(configWhere, specs[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // save login settings
        m_loginPanel.saveSettingsTo(settings);
        // save table name
        settings.addString("table_name", m_tableName.getText().trim());
        // save SET columns
        DataColumnSpecFilterConfiguration configSET = new DataColumnSpecFilterConfiguration(
            DBUpdateNodeModel.KEY_SET_FILTER_COLUMN);
        m_columnsInSetClause.saveConfiguration(configSET);
        configSET.saveConfiguration(settings);
        // save WHERE columns
        DataColumnSpecFilterConfiguration configWHERE = new DataColumnSpecFilterConfiguration(
            DBUpdateNodeModel.KEY_WHERE_FILTER_COLUMN);
        m_columnsInWhereClause.saveConfiguration(configWHERE);
        configWHERE.saveConfiguration(settings);
    }
}
