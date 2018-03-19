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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.05.2014 (thor): created
 */
package org.knime.base.node.io.database.connection.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * A panel for settings misc connection information, such as if columns with spaces are allowed or if the connection
 * should be validated before the dialog closes. The panel has a {@link GridBagLayout} and uses the protected
 * {@link #m_c} {@link GridBagConstraints} for layouting. You should re-use the constraints when extending this panel.
 *
 * @param <T> a subclass of {@link DatabaseConnectionSettings}
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
public class DBMiscPanel<T extends DatabaseConnectionSettings> extends JPanel {
    private static final long serialVersionUID = -4521725499938316528L;

    /**
     * The settings object from which and to which the panel read/writes the settings.
     */
    protected final T m_settings;

    /**
     * Checkbox for whether columns with spaces in their names should be allowed.
     */
    protected final JCheckBox m_allowSpacesInColumnNames = new JCheckBox("Allow spaces in column names");

    /**
     * Checkbox for whether the connection should be validated before the dialog is closed.
     */
    protected final JCheckBox m_validateConnection = new JCheckBox("Validate connection on close");

    /**
     * Checkbox for whether the metadata should be retrieved in configure in subsequent nodes.
     */
    protected final JCheckBox m_retrieveMetadataInConfigure = new JCheckBox("Retrieve metadata in configure");


    /**
     * Gridbag constraints object used for layouting the panel.
     */
    protected final GridBagConstraints m_c = new GridBagConstraints();

    /**
     * Creates a new panel.
     *
     * @param settings the settings object the panel should use
     * @param showAllowSpaces <code>true</code> if the option for spaces in column name should be shown,
     *            <code>false</code> otherwise
     */
    public DBMiscPanel(final T settings, final boolean showAllowSpaces) {
        super(new GridBagLayout());
        m_settings = settings;

        m_c.gridx = 0;
        m_c.gridy = 0;
        m_c.insets = new Insets(2, 2, 2, 2);
        m_c.anchor = GridBagConstraints.WEST;
        m_c.fill = GridBagConstraints.HORIZONTAL;
        m_c.weightx = 1;

        if (showAllowSpaces) {
            add(m_allowSpacesInColumnNames, m_c);
            m_c.gridy++;
        }
        add(m_validateConnection, m_c);
        m_c.gridy++;
        add(m_retrieveMetadataInConfigure, m_c);

        setBorder(BorderFactory.createTitledBorder("Misc"));
    }

    /**
     * Loads the settings into the dialog components.
     *
     * @param specs the incoming port specs.
     * @throws NotConfigurableException if the dialog should not open because necessary information is missing
     */
    public void loadSettings(final PortObjectSpec[] specs) throws NotConfigurableException {
        m_allowSpacesInColumnNames.setSelected(m_settings.getAllowSpacesInColumnNames());
        m_validateConnection.setSelected(m_settings.getValidateConnection());
        m_retrieveMetadataInConfigure.setSelected(m_settings.getRetrieveMetadataInConfigure());
    }

    /**
     * Saves the component values into the settings object.
     *
     * @param credentialProvider a credential provider
     * @throws InvalidSettingsException if some settings are invalid
     */
    public void saveSettings(final CredentialsProvider credentialProvider) throws InvalidSettingsException {
        m_settings.setAllowSpacesInColumnNames(m_allowSpacesInColumnNames.isSelected());
        m_settings.setValidateConnection(m_validateConnection.isSelected());
        m_settings.setRetrieveMetadataInConfigure(m_retrieveMetadataInConfigure.isSelected());

        if (m_settings.getValidateConnection()) {
            try {
                m_settings.execute(credentialProvider, conn -> {return conn != null;});
            } catch (SQLException ex) {
                Throwable cause = ExceptionUtils.getRootCause(ex);
                if (cause == null) {
                    cause = ex;
                }
                throw new InvalidSettingsException("Database connection could not be validated: " + cause.getMessage(),
                    ex);
            }
        }
    }
}
