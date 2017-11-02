
/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.knime.base.node.io.database.util.DBDialogPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Dialog pane of the Database Delete node.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.7
 */
final class DBDeleteRowsDialogPane extends NodeDialogPane {

    private final DataColumnSpecFilterPanel m_columnsInWhereClause;

    private final DBDialogPane m_loginPanel = new DBDialogPane(false);

    private final JTextField m_tableName = new JTextField();

    private final JTextField m_batchSize = new JTextField();

    /** Creates new dialog. */
    DBDeleteRowsDialogPane() {
// tab with database login and column selection
        final JPanel columnPanel = new JPanel(new GridLayout(1, 1));
        m_columnsInWhereClause = new DataColumnSpecFilterPanel();
        m_columnsInWhereClause.setBorder(BorderFactory.createTitledBorder(" Select WHERE Columns "));
        columnPanel.add(m_columnsInWhereClause);

        final JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(" Table Name "));
        tablePanel.setFont(DBDialogPane.FONT);
        tablePanel.add(m_tableName, BorderLayout.CENTER);

        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        panel.add(m_loginPanel, c);
        c.gridy++;
        panel.add(tablePanel, c);
        c.gridy++;
        panel.add(columnPanel, c);
        final JScrollPane scroll = new JScrollPane(panel);
        super.addTab("Settings", scroll);

// advanced tab with batch size
        final JPanel batchSizePanel = new JPanel(new FlowLayout());
        batchSizePanel.add(new JLabel("Batch Size: "));
        m_batchSize.setPreferredSize(new Dimension(100, 20));
        batchSizePanel.add(m_batchSize);
        super.addTab("Advanced", batchSizePanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        DataTableSpec tableSpec = (DataTableSpec)specs[0];

        // load login setting
        m_loginPanel.loadSettingsFrom(settings, specs, getCredentialsProvider());
        // load table name
        m_tableName.setText(settings.getString(DBDeleteRowsNodeModel.KEY_TABLE_NAME, ""));
        // load WHERE column panel
        DataColumnSpecFilterConfiguration configWhere = new DataColumnSpecFilterConfiguration(
            DBDeleteRowsNodeModel.KEY_WHERE_FILTER_COLUMN);
        configWhere.loadConfigurationInDialog(settings, tableSpec);
        m_columnsInWhereClause.loadConfiguration(configWhere, tableSpec);
        // load batch size
        final int batchSize = settings.getInt(DBDeleteRowsNodeModel.KEY_BATCH_SIZE, 1);
        m_batchSize.setText(Integer.toString(batchSize));

        if ((specs.length > 1) && (specs[1] instanceof DatabaseConnectionPortObjectSpec)) {
            m_loginPanel.setVisible(false);
        } else {
            m_loginPanel.setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // save login settings
        m_loginPanel.saveSettingsTo(settings, getCredentialsProvider());
        // save table name
        settings.addString(DBDeleteRowsNodeModel.KEY_TABLE_NAME, m_tableName.getText().trim());
        // save WHERE columns
        DataColumnSpecFilterConfiguration configWHERE = new DataColumnSpecFilterConfiguration(
            DBDeleteRowsNodeModel.KEY_WHERE_FILTER_COLUMN);
        m_columnsInWhereClause.saveConfiguration(configWHERE);
        configWHERE.saveConfiguration(settings);
        // save batch size
        final String strBatchSite = m_batchSize.getText().trim();
        if (strBatchSite.isEmpty()) {
            throw new InvalidSettingsException("Batch size must not be empty.");
        }
        try {
            final int intBatchSize = Integer.parseInt(strBatchSite);
            settings.addInt(DBDeleteRowsNodeModel.KEY_BATCH_SIZE, intBatchSize);
        } catch (final NumberFormatException nfe) {
            throw new InvalidSettingsException("Can't parse batch size \"" + strBatchSite
                                               + "\", reason: " + nfe.getMessage(), nfe);
        }
    }
}
