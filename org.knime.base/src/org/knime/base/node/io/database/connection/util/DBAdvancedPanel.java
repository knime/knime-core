/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.05.2014 (thor): created
 */
package org.knime.base.node.io.database.connection.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseUtility;

/**
 * A panel for settings advanced connection information, such as the database type.
 * The panel has a {@link GridBagLayout} and uses the protected {@link #m_c} {@link GridBagConstraints} for layouting.
 * You should re-use the constraints when extending this panel.
 *
 * @param <T> a subclass of {@link DatabaseConnectionSettings}
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class DBAdvancedPanel<T extends DatabaseConnectionSettings> extends JPanel {
    private static final long serialVersionUID = 1;

    /**Automatic db identifier option. If this option is selected the db identifier is chosen based on the jdbc url.*/
    public static final String AUTOMATIC_DB_ID = "<based on database URL>";

    private static final String[] DB_ID_LIST;
    static {
        LinkedList<String> ids = new LinkedList<>(DatabaseUtility.getDatabaseIdentifiers());
        ids.addFirst(AUTOMATIC_DB_ID);
        DB_ID_LIST = ids.toArray(new String[0]);
    }

    /**
     * The settings object from which and to which the panel read/writes the settings.
     */
    protected final T m_settings;

    /**
     * A drop-down list for selecting a database driver. It will be filled with all available drivers.
     */
    protected final JComboBox<String> m_dbIdentifier = new JComboBox<>(DB_ID_LIST);

    /**{@link GridBagConstraints} used in the dialog for placing the elements.*/
    protected GridBagConstraints m_c;

    /**
     * Creates a new panel.
     *
     * @param settings the settings object the panel should use
     */
    public DBAdvancedPanel(final T settings) {
        super(new GridBagLayout());
        m_settings = settings;
        m_c = new GridBagConstraints();
        m_c.gridx = 0;
        m_c.gridy = 0;
        m_c.insets = new Insets(14, 10, 10, 5);
        m_c.anchor = GridBagConstraints.PAGE_START;
        add(new JLabel("Database Type"), m_c);
        m_c.insets = new Insets(10, 0, 10, 10);
        m_c.gridx++;
        m_c.fill = GridBagConstraints.HORIZONTAL;
        m_c.weightx = 1;
        add(m_dbIdentifier, m_c);
        //add empty label to have the component placed at the top of the dialog
        m_c.gridx = 0;
        m_c.gridy++;
        m_c.gridwidth = 2;
        m_c.weighty = 1;
        m_c.fill = GridBagConstraints.VERTICAL;
        add(new JLabel(), m_c);
    }

    /**
     * Loads the settings into the dialog components.
     *
     * @param specs the incoming port specs.
     * @throws NotConfigurableException if the dialog should not open because necessary information is missing
     */
    public void loadSettings(final PortObjectSpec[] specs) throws NotConfigurableException {
        final String dbIdentifier = m_settings.getDatabaseIdentifierValue();
        if (dbIdentifier == null) {
            m_dbIdentifier.setSelectedItem(AUTOMATIC_DB_ID);
        } else {
            m_dbIdentifier.setSelectedItem(dbIdentifier);
        }
    }

    /**
     * Saves the component values into the settings object.
     *
     * @throws InvalidSettingsException if some settings are invalid
     */
    public void saveSettings() throws InvalidSettingsException {
        String dbIdentifier = (String)m_dbIdentifier.getSelectedItem();
        if (AUTOMATIC_DB_ID.equals(dbIdentifier)) {
            m_settings.setDatabaseIdentifier(null);
        } else {
            m_settings.setDatabaseIdentifier(dbIdentifier);
        }
    }
}
