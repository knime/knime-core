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
package org.knime.base.node.io.database.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.KnimeEncryption;

/**
 * Creates a panel to select database driver, enter database URL, user and
 * password - optionally from credentials.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DBDialogPane extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBDialogPane.class);

    private final JComboBox<String> m_driver = new JComboBox<>();

    private final JComboBox<String> m_db = new JComboBox<>();

    private final JTextField m_user = new JTextField("");

    private final JPasswordField m_pass = new JPasswordField();

    private boolean m_passwordChanged = false;

    private final JCheckBox m_credCheckBox = new JCheckBox();
    private final JCheckBox m_allowSpacesInColumnNames = new JCheckBox();
    private final JCheckBox m_validateConnection = new JCheckBox();
    private final JCheckBox m_retrieveMetadataInConfigure = new JCheckBox();
    private final JComboBox<String> m_credBox = new JComboBox<>();
    private final JComboBox<String> m_timezone; // filled with all time zones (sorted by name)

    private final JRadioButton m_noCorrectionTZ = new JRadioButton("No Correction (use UTC)");
    private final JRadioButton m_currentTZ = new JRadioButton("Use local TimeZone");
    private final JRadioButton m_selectTZ = new JRadioButton("TimeZone:");

    /** Default font used for all components within the database dialogs. */
    public static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

    /**
     * Creates a new dialog.
     *
     * @param showConnectionOptions <code>true</code> the options affecting the connection in subsequent nodes should be
     *            shown, <code>false</code> otherwise
     */
    public DBDialogPane(final boolean showConnectionOptions) {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

// create and driver component
        m_driver.setEditable(false);
        m_driver.setFont(FONT);
        final JPanel driverPanel = new JPanel(new BorderLayout());
        driverPanel.setBorder(BorderFactory
                .createTitledBorder(" Database Driver "));
        driverPanel.add(m_driver, BorderLayout.CENTER);
        driverPanel.add(new JLabel(" (Additional Database Drivers can be loaded"
                + " in the KNIME preference page.) "), BorderLayout.SOUTH);
        super.add(driverPanel);

// create and add database URL
        m_db.setFont(FONT);
        m_db.setEditable(true);
        m_driver.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent ie) {
                String url =
                    DatabaseDriverLoader.getURLForDriver((String) ie.getItem());
                m_db.setSelectedItem(url);
            }
        });
        final JPanel dbPanel = new JPanel(new BorderLayout());
        dbPanel.setBorder(BorderFactory.createTitledBorder(
                " Database URL "));
        dbPanel.add(m_db, BorderLayout.CENTER);
        super.add(dbPanel);

// create and add credential box
        final JPanel credPanel = new JPanel(new BorderLayout());
        credPanel.setBorder(BorderFactory.createTitledBorder(
            " Workflow Credentials "));
        credPanel.add(m_credCheckBox);
        m_credCheckBox.addItemListener(new ItemListener() {
           @Override
           public void itemStateChanged(final ItemEvent ie) {
                enableCredentials(m_credCheckBox.isSelected());
           }
        });
        m_credBox.setEditable(false);
        m_credBox.setFont(FONT);
        credPanel.add(m_credCheckBox, BorderLayout.WEST);
        credPanel.add(m_credBox, BorderLayout.CENTER);
        super.add(credPanel);

// create and user name field
        final JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(BorderFactory.createTitledBorder(" User Name "));
        m_user.setFont(FONT);
        userPanel.add(m_user, BorderLayout.CENTER);
        super.add(userPanel);

// create and add password panel
        final JPanel passPanel = new JPanel(new BorderLayout());
        passPanel.setBorder(BorderFactory.createTitledBorder(" Password "));
        m_pass.setFont(FONT);
        m_pass.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
            @Override
            public void insertUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
            @Override
            public void removeUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
        });
        m_pass.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent fe) {
                if (!m_passwordChanged) {
                    m_pass.setText("");
                }
            }
        });
        passPanel.add(m_pass, BorderLayout.CENTER);
        super.add(passPanel);

// create and timezone field
        final String[] timezones = TimeZone.getAvailableIDs();
        Arrays.sort(timezones);
        m_timezone = new JComboBox<String>(timezones);
        m_timezone.setFont(FONT);
        m_timezone.setSelectedItem(TimeZone.getDefault().getID());
        m_timezone.setEnabled(false);
        m_selectTZ.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent ie) {
                if (ie != null) {
                    m_timezone.setEnabled(ie.getStateChange() == ItemEvent.SELECTED);
                }
            }
        });
        final ButtonGroup bg = new ButtonGroup();
        bg.add(m_noCorrectionTZ);
        bg.add(m_currentTZ);
        bg.add(m_selectTZ);
        final JPanel tzPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tzPanel1.add(m_noCorrectionTZ);
        tzPanel1.add(m_currentTZ);
        final JPanel tzPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tzPanel2.add(m_selectTZ);
        tzPanel2.add(m_timezone);
        final JPanel timezonePanel = new JPanel(new BorderLayout());
        timezonePanel.setBorder(BorderFactory.createTitledBorder(" TimeZone "));
        timezonePanel.add(tzPanel1, BorderLayout.NORTH);
        timezonePanel.add(tzPanel2, BorderLayout.SOUTH);
        super.add(timezonePanel);

        // misc options (validate, spaces, ...)
        final JPanel miscPanel = new JPanel(new GridLayout(0, 1));
        miscPanel.setBorder(BorderFactory.createTitledBorder("Misc"));

        final JPanel spacesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        spacesPanel.add(m_allowSpacesInColumnNames);
        spacesPanel.add(new JLabel("Allow spaces in column names"));
        miscPanel.add(spacesPanel);

        if (showConnectionOptions) {
            final JPanel validatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            validatePanel.add(m_validateConnection);
            validatePanel.add(new JLabel("Validate connection on close"));
            miscPanel.add(validatePanel);

            final JPanel metadataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            metadataPanel.add(m_retrieveMetadataInConfigure);
            metadataPanel.add(new JLabel("Retrieve metadata in configure"));
            miscPanel.add(metadataPanel);
        }


        super.add(miscPanel);
    }

    private void enableCredentials(final boolean flag) {
        m_credBox.setEnabled(flag);
        m_pass.setEnabled(!flag);
        m_user.setEnabled(!flag);
    }

    /**
     * Load settings.
     * @param settings to load
     * @param specs input spec
     * @param credProvider a credentials provider, must not be <code>null</code>
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final CredentialsProvider credProvider) {
        DatabaseConnectionSettings s = new DatabaseConnectionSettings();
        try {
            s.loadValidatedConnection(settings, null);
        } catch (InvalidSettingsException ex) {
            // use the available settings as they are
        }

        // update credentials
        m_credBox.removeAllItems();
        if (credProvider == null || credProvider.listNames().isEmpty()) {
            m_credCheckBox.setEnabled(false);
            m_credBox.setEnabled(false);
        } else {
            m_credCheckBox.setEnabled(true);
            m_credBox.setEnabled(true);
            for (String c : credProvider.listNames()) {
                m_credBox.addItem(c);
            }
        }
        // database driver and name
        m_driver.removeAllItems();
        // update list of registered driver
        updateDriver();
        // check if at least one driver is selected and the list is not empty
        final Object selectedDriver = m_driver.getSelectedItem();
        if (selectedDriver != null) {
            m_driver.setSelectedItem(s.getDriver());
        }
        // update list of urls
        m_db.removeAllItems();
        for (String databaseURL : DatabaseConnectionSettings.DATABASE_URLS.getHistory()) {
            m_db.addItem(databaseURL);
        }
        if (s.getJDBCUrl() == null) {
            m_db.setSelectedItem("jdbc:odbc:<database_name>");
        } else {
            m_db.setSelectedItem(s.getJDBCUrl());
        }

        boolean useCredential = (s.getCredentialName() != null);
        enableCredentials(useCredential);
        if (useCredential) {
            m_credBox.setSelectedItem(s.getCredentialName());
            m_credCheckBox.setSelected(true);
        } else {
            // user
            String user = s.getUserName(null);
            m_user.setText(user == null ? "" : user);
            // password
            String password = s.getPassword(null);
            m_pass.setText(password == null ? "" : password);
            m_passwordChanged = false;
            m_credCheckBox.setSelected(false);
        }

        // read timezone
        final String timezone = s.getTimezone();
        if ("none".equals(timezone) || (timezone == null)) {
            m_noCorrectionTZ.setSelected(true);
        } else if ("current".equals(timezone)) {
            m_currentTZ.setSelected(true);
        } else {
            m_selectTZ.setSelected(true);
            m_timezone.setSelectedItem(timezone);
        }

        m_allowSpacesInColumnNames.setSelected(s.getAllowSpacesInColumnNames());
        m_validateConnection.setSelected(s.getValidateConnection());
        m_retrieveMetadataInConfigure.setSelected(s.getRetrieveMetadataInConfigure());
    }

    private void updateDriver() {
        m_driver.removeAllItems();
        Set<String> driverNames = new LinkedHashSet<>(DatabaseUtility.getJDBCDriverClasses());
        for (String driverName
                : DatabaseConnectionSettings.DRIVER_ORDER.getHistory()) {
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
     * Save settings.
     * @param settings to save into
     * @param credProvider a credentials provider, must not be <code>null</code>
     * @throws InvalidSettingsException if the connection could not be validated
     */
    public void saveSettingsTo(final NodeSettingsWO settings, final CredentialsProvider credProvider)
        throws InvalidSettingsException {
        DatabaseConnectionSettings s = new DatabaseConnectionSettings();

        String driverName = m_driver.getSelectedItem().toString();
        s.setDriver(driverName);

        String url = m_db.getEditor().getItem().toString();
        s.setJDBCUrl(url);

        boolean useCredential = m_credCheckBox.isSelected();
        if (useCredential) {
            s.setCredentialName((String) m_credBox.getSelectedItem());
        } else {
            s.setUserName(m_user.getText().trim());
            if (m_passwordChanged) {
                try {
                    s.setPassword(KnimeEncryption.encrypt(m_pass.getPassword()));
                } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException
                        | UnsupportedEncodingException ex) {
                    LOGGER.error("Could not encrypt password, reason: " + ex.getMessage(), ex);
                }
            } else {
                s.setPassword(new String(m_pass.getPassword()));
            }
        }
        if (m_noCorrectionTZ.isSelected()) {
            s.setTimezone("none");
        } else if (m_currentTZ.isSelected()) {
            s.setTimezone("current");
        } else {
            final String timezone = (String) m_timezone.getSelectedItem();
            s.setTimezone(timezone);
        }

        s.setAllowSpacesInColumnNames(m_allowSpacesInColumnNames.isSelected());
        s.setValidateConnection(m_validateConnection.isSelected());
        s.setRetrieveMetadataInConfigure(m_retrieveMetadataInConfigure.isSelected());
        if (s.getValidateConnection()) {
            try {
                s.createConnection(credProvider);
            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | SQLException
                    | IOException ex) {
                Throwable cause = ExceptionUtils.getRootCause(ex);
                if (cause == null) {
                    cause = ex;
                }

                throw new InvalidSettingsException("Database connection could not be validated: " + cause.getMessage(),
                    ex);
            }
        }

        s.saveConnection(settings);
    }

    /**
     * Settings object holding the current database connection properties.
     * @return a <code>DatabaseConnectionSettings</code> object
     */
    public DatabaseConnectionSettings getConnectionSettings() {
        return new DatabaseConnectionSettings(
                m_driver.getSelectedItem().toString(),
                m_db.getSelectedItem().toString(),
                m_user.getText(),
                new String(m_pass.getPassword()),
                m_credCheckBox.isSelected() ? m_credBox.getSelectedItem().toString() : null,
                (String) m_timezone.getSelectedItem());
    }
}

