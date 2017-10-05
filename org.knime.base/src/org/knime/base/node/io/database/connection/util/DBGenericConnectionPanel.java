/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.05.2014 (thor): created
 */
package org.knime.base.node.io.database.connection.util;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.core.node.port.database.DatabaseUtility;

/**
 * A panel for selecting a JDBC driver and a full JDBC URL to the database. The panel has a {@link GridBagLayout} and
 * uses the protected {@link #m_c} {@link GridBagConstraints} for layouting. You should re-use the constraints when
 * extending this panel.
 *
 * @param <T> a subclass of {@link DatabaseConnectionSettings}
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DBGenericConnectionPanel<T extends DatabaseConnectionSettings> extends JPanel {
    private static final long serialVersionUID = 3255468317035158516L;

    /**
     * The settings object from which and to which the panel read/writes the settings.
     */
    protected final T m_settings;

    /**
     * A drop-down list for selecting a database driver. It will be filled with all available drivers.
     */
    protected final JComboBox<String> m_driver = new JComboBox<>();

    /**
     * An editable drop-down list for entering a database URL.
     */
    protected final JComboBox<String> m_db = new JComboBox<>();

    /**
     * Gridbag constraints object used for layouting the panel.
     */
    protected final GridBagConstraints m_c = new GridBagConstraints();

    /**
     *
     * @param settings the settings object the panel should use
     */
    public DBGenericConnectionPanel(final T settings) {
        super(new GridBagLayout());
        m_settings = settings;

        m_c.gridx = 0;
        m_c.gridy = 0;
        m_c.insets = new Insets(2, 2, 2, 2);
        m_c.anchor = GridBagConstraints.WEST;

        add(new JLabel("Database driver   "), m_c);
        m_c.gridx = 1;
        m_c.fill = GridBagConstraints.HORIZONTAL;
        m_c.weightx = 1;
        add(m_driver, m_c);

        m_c.gridx = 0;
        m_c.gridy++;
        m_c.weightx = 0;
        add(new JLabel("Database URL   "), m_c);
        m_c.gridx = 1;
        m_c.weightx = 1;
        m_db.setPreferredSize(new Dimension(200, m_db.getPreferredSize().height));
        add(m_db, m_c);

        m_driver.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent ie) {
                String url = DatabaseDriverLoader.getURLForDriver((String)ie.getItem());
                m_db.setSelectedItem(url);
            }
        });

        m_db.setEditable(true);

        setBorder(BorderFactory.createTitledBorder("Connection"));
    }

    private void updateDriver() {
        m_driver.removeAllItems();
        Set<String> driverNames = new LinkedHashSet<>(DatabaseUtility.getJDBCDriverClasses());
        for (String driverName : DatabaseConnectionSettings.DRIVER_ORDER.getHistory()) {
            if (driverNames.contains(driverName)) {
                m_driver.addItem(driverName);
                driverNames.remove(driverName);
            }
        }
        for (String driverName : driverNames) {
            m_driver.addItem(driverName);
        }
    }

    /**
     * Loads the settings into the dialog components.
     *
     * @param specs the incoming port specs.
     * @throws NotConfigurableException if the dialog should not open because necessary information is missing
     */
    public void loadSettings(final PortObjectSpec[] specs) throws NotConfigurableException {
        // update list of registered driver
        updateDriver();
        // check if at least one driver is selected and the list is not empty
        final Object selectedDriver = m_driver.getSelectedItem();
        if (selectedDriver != null) {
            m_driver.setSelectedItem(m_settings.getDriver());
        }

        // update list of urls
        m_db.removeAllItems();
        for (String databaseURL : DatabaseConnectionSettings.DATABASE_URLS.getHistory()) {
            m_db.addItem(databaseURL);
        }
        if (m_settings.getJDBCUrl() == null) {
            m_db.setSelectedItem("jdbc:odbc:<database_name>");
        } else {
            m_db.setSelectedItem(m_settings.getJDBCUrl());
        }
    }

    /**
     * Saves the component values into the settings object.
     *
     * @throws InvalidSettingsException if some settings are invalid
     */
    public void saveSettings() throws InvalidSettingsException {
        m_settings.setDriver((String)m_driver.getSelectedItem());
        m_settings.setJDBCUrl((String) m_db.getEditor().getItem());
    }
}
