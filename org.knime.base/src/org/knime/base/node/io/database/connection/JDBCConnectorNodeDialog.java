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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   24.04.2014 (thor): created
 */
package org.knime.base.node.io.database.connection;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

import org.knime.base.node.io.database.connection.util.DBAdvancedPanel;
import org.knime.base.node.io.database.connection.util.DBAuthenticationPanel;
import org.knime.base.node.io.database.connection.util.DBGenericConnectionPanel;
import org.knime.base.node.io.database.connection.util.DBMiscPanel;
import org.knime.base.node.io.database.connection.util.DBTimezonePanel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;

/**
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class JDBCConnectorNodeDialog extends NodeDialogPane {

    private final DatabaseConnectionSettings m_settings = new DatabaseConnectionSettings();

    private final DBGenericConnectionPanel<DatabaseConnectionSettings> m_connectionPanel =
        new DBGenericConnectionPanel<>(m_settings);

    private final DBAuthenticationPanel<DatabaseConnectionSettings> m_authPanel = new DBAuthenticationPanel<>(
        m_settings);

    private final DBTimezonePanel<DatabaseConnectionSettings> m_tzPanel = new DBTimezonePanel<>(m_settings);

    private final DBMiscPanel<DatabaseConnectionSettings> m_miscPanel = new DBMiscPanel<>(m_settings, true);

    private final DBAdvancedPanel<DatabaseConnectionSettings> m_advancedPanel = new DBAdvancedPanel<>(m_settings);

    /**
     * Constructor.
     */
    JDBCConnectorNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 4, 0);

        p.add(m_connectionPanel, c);
        c.gridy++;
        p.add(m_authPanel, c);
        c.gridy++;
        p.add(m_tzPanel, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(m_miscPanel, c);

        addTab("Connection settings", p);
        addTab("Advanced", m_advancedPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_settings.loadValidatedConnection(settings, getCredentialsProvider());
        } catch (InvalidSettingsException ex) {
            // too bad, use default values
        }

        m_connectionPanel.loadSettings(specs);
        m_authPanel.loadSettings(specs, getCredentialsProvider());
        m_tzPanel.loadSettings(specs);
        m_miscPanel.loadSettings(specs);
        m_advancedPanel.loadSettings(specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_connectionPanel.saveSettings();
        m_authPanel.saveSettings();
        m_tzPanel.saveSettings();
        m_miscPanel.saveSettings(getCredentialsProvider());
        m_advancedPanel.saveSettings();

        m_settings.saveConnection(settings);
    }
}
