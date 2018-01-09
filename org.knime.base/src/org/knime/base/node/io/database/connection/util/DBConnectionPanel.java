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
 *   08.05.2014 (thor): created
 */
package org.knime.base.node.io.database.connection.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.StringHistoryPanel;

/**
 * A panel for selecting a hostname, port, and a database. The panel has a {@link GridBagLayout} and uses the protected
 * {@link #m_c} {@link GridBagConstraints} for layouting. You should re-use the constraints when extending this panel.
 *
 * @param <T> a subclass of {@link DefaultDatabaseConnectionSettings}
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
public abstract class DBConnectionPanel<T extends DefaultDatabaseConnectionSettings> extends JPanel {
    private static final long serialVersionUID = 5107567313142629692L;

    /**
     * The settings object from which and to which the panel read/writes the settings.
     */
    protected final T m_settings;

    /**
     * An editable drop-down list for entering the hostname.
     */
    protected final StringHistoryPanel m_hostname;

    /**
     * A spinner for the database port. The default value will be taken from the settings.
     */
    protected final JSpinner m_port = new JSpinner(new SpinnerNumberModel(1, 1, 65535, 1));

    /**
     * An editable drop-down list for entering the database name.
     */
    protected final StringHistoryPanel m_databaseName;


    /**
     * Gridbag constraints object used for layouting the panel.
     */
    protected final GridBagConstraints m_c = new GridBagConstraints();

    /**
     * Creates a new connection panel.
     *
     * @param settings the settings object the panel should use
     * @param historyId
     */
    public DBConnectionPanel(final T settings, final String historyId) {
        super(new GridBagLayout());
        m_settings = settings;
        m_hostname = new StringHistoryPanel(historyId + "_hostname");
        m_databaseName = new StringHistoryPanel(historyId + "_databaseName");

        m_c.gridx = 0;
        m_c.gridy = 0;
        m_c.insets = new Insets(2, 2, 2, 2);
        m_c.anchor = GridBagConstraints.WEST;

        add(new JLabel("Hostname   "), m_c);
        m_c.gridx = 1;
        add(new JLabel("Port   "), m_c);


        m_c.gridx = 0;
        m_c.gridy++;
        m_c.fill = GridBagConstraints.HORIZONTAL;
        m_c.weightx = 1;
        add(m_hostname, m_c);

        m_c.weightx = 0;
        m_c.fill = GridBagConstraints.NONE;
        m_c.gridx = 1;
        add(m_port, m_c);

        m_c.gridx = 0;
        m_c.gridy++;
        add(new JLabel("Database name   "), m_c);

        m_c.gridy++;
        m_c.fill = GridBagConstraints.HORIZONTAL;
        m_c.weightx = 1;
        add(m_databaseName, m_c);

        setBorder(BorderFactory.createTitledBorder("Connection"));
    }

    /**
     * Loads the settings into the dialog components.
     *
     * @param specs the incoming port specs.
     * @throws NotConfigurableException if the dialog should not open because necessary information is missing
     */
    public void loadSettings(final PortObjectSpec[] specs) throws NotConfigurableException {
        m_hostname.setSelectedString(m_settings.getHost());
        m_hostname.commitSelectedToHistory();
        m_hostname.updateHistory();

        m_port.setValue(m_settings.getPort() > 0 ?  m_settings.getPort() : 10000);
        m_databaseName.setSelectedString(m_settings.getDatabaseName());
        m_databaseName.commitSelectedToHistory();
        m_databaseName.updateHistory();
    }


    /**
     * Saves the component values into the settings object.
     *
     * @throws InvalidSettingsException if some settings are invalid
     */
    public void saveSettings() throws InvalidSettingsException {
        m_settings.setHost(m_hostname.getSelectedString());
        m_hostname.commitSelectedToHistory();

        m_settings.setPort((Integer)m_port.getValue());
        m_settings.setDatabaseName(m_databaseName.getSelectedString());
        m_settings.setJDBCUrl(getJDBCURL(m_settings.getHost(), m_settings.getPort(), m_settings.getDatabaseName()));
        m_databaseName.commitSelectedToHistory();
    }

    /**
     * Returns the database-specific JDBC URL for the given host and database.
     *
     * @param host the hostname
     * @param port the port
     * @param dbName the database name
     * @return a JDBC URL
     */
    protected abstract String getJDBCURL(String host, int port, String dbName);
}
