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
 * -------------------------------------------------------------------
 *
 * History
 *   21.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 *   25.09.2006 (ohl): using SettingsModel
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * A component that allows a user to enter username/password or select credentials variable.
 * @author Lara Gorini
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
public final class DialogComponentAuthentication extends DialogComponent implements ActionListener {

    private static final Insets NEUTRAL_INSET = new Insets(0, 0, 0, 0);

    private static final int LEFT_INSET = 23;

    private final ButtonGroup m_authenticationType = new ButtonGroup();

    private final JRadioButton m_typeNone =
            createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.NONE, m_authenticationType, this);
    private final JRadioButton m_typeUser =
            createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.USER, m_authenticationType, this);
    private final JRadioButton m_typeUserPwd =
            createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.USER_PWD, m_authenticationType, this);
    private final JRadioButton m_typeCredential =
            createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.CREDENTIALS, m_authenticationType, this);
    private final JRadioButton m_typeKerberos =
            createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.KERBEROS, m_authenticationType, this);

    private final JComboBox<String> m_credentialField = new JComboBox<>();

    private final JTextField m_usernameOnlyField = new JTextField(20);

    private final JTextField m_usernameField = new JTextField(20);

    private final JPasswordField m_passwordField = new JPasswordField(20);

    private final JLabel m_usernameLabel = new JLabel("Username:", SwingConstants.LEFT);
    private final JLabel m_passwordLabel = new JLabel("Password:", SwingConstants.LEFT);

    private final Component m_credentialPanel = getCredentialPanel();

    private final Component m_userPanel = getUserPanel();

    private final Component m_userPwdPanel = getUserPwdPanel();

    private final String m_label;

    private JPanel m_rootPanel;

    private HashSet<AuthenticationType> m_supportedTypes;

    private static JRadioButton createAuthenticationTypeButton(final AuthenticationType type, final ButtonGroup group,
        final ActionListener l) {
        final JRadioButton button = new JRadioButton(type.getText());
        button.setActionCommand(type.getActionCommand());
        if (type.isDefault()) {
            button.setSelected(true);
        }
        if (type.getToolTip() != null) {
            button.setToolTipText(type.getToolTip());
        }
        if (l != null) {
            button.addActionListener(l);
        }
        group.add(button);
        return button;
    }

    /**
     * Constructor for this dialog component with default authentication types.
     *
     * @param authModel The {@link SettingsModel}.
     *
     */
    public DialogComponentAuthentication(final SettingsModelAuthentication authModel) {
        this(authModel, null, AuthenticationType.CREDENTIALS, AuthenticationType.USER_PWD);
    }

    /**
     * Constructor for this dialog component.
     *
     * @param authModel The {@link SettingsModel}.
     * @param label The label.
     * @param supportedTypes the authentication {@link AuthenticationType}s to display
     */
    public DialogComponentAuthentication(final SettingsModelAuthentication authModel, final String label,
        final AuthenticationType... supportedTypes) {
        super(authModel);
        m_supportedTypes = new HashSet<>(Arrays.asList(supportedTypes));
        m_label = label;
        m_rootPanel = getRootPanel();
        getComponentPanel().setLayout(new GridBagLayout());
        getComponentPanel().add(m_rootPanel);

        //add all the change listeners
        // update the model, if the user changes the component
        m_credentialField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // if a new item is selected update the model
                    updateModel();
                }
            }
        });

        m_usernameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateModel();
            }
            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateModel();
            }
            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateModel();
            }
        });

        m_usernameOnlyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateModel();
            }
            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateModel();
            }
            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateModel();
            }
        });

        m_passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateModel();
            }
            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateModel();
            }
            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateModel();
            }
        });

        // update the checkbox, whenever the model changes - make sure we get
        // notified first.
        getModel().prependChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });


        //call this method to be in sync with the settings model
        updateComponent();
    }

    private JPanel getRootPanel() {
        final JPanel authBox = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = NEUTRAL_INSET;
        if (m_label != null) {
            authBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " " + m_label + " "));
        }

        if (m_supportedTypes.contains(AuthenticationType.NONE)) {
            authBox.add(m_typeNone, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.CREDENTIALS)) {
            gbc.gridy++;
            if (m_supportedTypes.size() > 1) {
                authBox.add(m_typeCredential, gbc);
            }
            gbc.gridy++;
            authBox.add(m_credentialPanel, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.USER)) {
            gbc.gridy++;
            if (m_supportedTypes.size() > 1) {
                authBox.add(m_typeUser, gbc);
            }
            gbc.gridy++;
            authBox.add(m_userPanel, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.USER_PWD)) {
            if (m_supportedTypes.size() > 1) {
                gbc.gridy++;
                authBox.add(m_typeUserPwd, gbc);
            }
            gbc.gridy++;
            authBox.add(m_userPwdPanel, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.KERBEROS)) {
            gbc.gridy++;
            authBox.add(m_typeKerberos, gbc);
    //        gbc.gridy++;
    //        authBox.add(m_kerberosPanel, gbc);
        }
        final Dimension origSize = authBox.getPreferredSize();
        Dimension preferredSize = getMaxDim(m_credentialPanel.getPreferredSize(), m_userPwdPanel.getPreferredSize());
        preferredSize = getMaxDim(preferredSize, m_userPanel.getPreferredSize());
        final Dimension maxSize = getMaxDim(preferredSize, origSize);
        authBox.setMinimumSize(maxSize);
        authBox.setPreferredSize(maxSize);
        return authBox;
    }

    /**
     * @param preferredSize
     * @param preferredSize2
     * @return
     */
    private Dimension getMaxDim(final Dimension d1, final Dimension d2) {
        return new Dimension(Math.max(d1.width, d2.width), Math.max(d1.height, d2.height));
    }

    private JPanel getUserPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_usernameLabel, gbc);
        gbc.gridx = 1;
        gbc.insets = NEUTRAL_INSET;
        gbc.ipadx = 10;
        panel.add(m_usernameOnlyField, gbc);
        return panel;
    }

    private JPanel getUserPwdPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_usernameLabel, gbc);
        gbc.gridx = 1;
        gbc.insets = NEUTRAL_INSET;
        gbc.ipadx = 10;
        gbc.weightx = 1;
        panel.add(m_usernameField, gbc);
        gbc.ipadx = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_passwordLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = NEUTRAL_INSET;
        panel.add(m_passwordField, gbc);
        return panel;
    }

    private Component getCredentialPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 0);
        panel.add(m_credentialField, gbc);
        return panel;
    }

    private void updateModel() {
        final AuthenticationType type = AuthenticationType.get(m_authenticationType.getSelection().getActionCommand());
        String userName = null;
        String credential = null;
        String pwd = null;
        switch (type) {
            case CREDENTIALS:
                credential = (String)m_credentialField.getSelectedItem();
                break;
            case KERBEROS:
                //nothing to store
                break;
            case NONE:
                //nothing to store
                break;
            case USER:
                userName = m_usernameOnlyField.getText();
                break;
            case USER_PWD:
                userName = m_usernameField.getText();
                final char[] password = m_passwordField.getPassword();
                if (password != null && password.length > 1) {
                    pwd = new String(password);
                }
                break;
            default:
                throw new IllegalStateException("Unimplemented authentication type found");
        }
        final SettingsModelAuthentication model = (SettingsModelAuthentication)getModel();
        model.setValues(type, credential, userName, pwd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // only update component if values are off
        final SettingsModelAuthentication model = (SettingsModelAuthentication)getModel();
        setEnabledComponents(model.isEnabled());

        //select the correct radio button
        final Enumeration<AbstractButton> buttons = m_authenticationType.getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand().equals(model.getAuthenticationType().getActionCommand())) {
                button.setSelected(true);
            }
        }

        if (model.getAuthenticationType().equals(AuthenticationType.CREDENTIALS)) {
            //update the credential information
            if (m_credentialField.getSelectedItem() != null
                && !((String)m_credentialField.getSelectedItem()).equals(model.getCredential())) {
                ItemListener[] itemListeners = m_credentialField.getItemListeners();
                for (ItemListener listener : itemListeners) {
                    m_credentialField.removeItemListener(listener);
                }
                m_credentialField.setSelectedItem(model.getCredential());
                for (ItemListener listener : itemListeners) {
                    m_credentialField.addItemListener(listener);
                }
            }

        } else if (model.getAuthenticationType().equals(AuthenticationType.USER)) {
          //update the user name only field
            if (!m_usernameOnlyField.getText().equals(model.getUsername())) {
                updateNoListener(m_usernameOnlyField, model.getUsername());
            }

        } else if (model.getAuthenticationType().equals(AuthenticationType.USER_PWD)) {
            //update the user name field
            if (!m_usernameField.getText().equals(model.getUsername())) {
                updateNoListener(m_usernameField, model.getUsername());
            }

            //update the password field
            if (model.getPassword() != null) {
                String modelPwd = model.getPassword();
                char[] password = m_passwordField.getPassword();
                String componentPwd = null;
                if (password != null && password.length > 1) {
                    componentPwd = new String(password);
                }
                if (!Objects.equals(componentPwd, modelPwd)) {
                    updateNoListener(m_passwordField, modelPwd);
                }
            }
        }

        updatePanel();
    }

    /**
     *
     */
    private void updatePanel() {
        final boolean credentialsAvailable = m_credentialField.getItemCount() > 0;
        m_typeCredential.setEnabled(credentialsAvailable);
        m_credentialPanel.setVisible(m_typeCredential.isSelected());
        m_userPwdPanel.setVisible(m_typeUserPwd.isSelected());
        m_userPanel.setVisible(m_typeUser.isSelected());
    }

    private static void updateNoListener(final JTextField txtField, final String text) {
        final AbstractDocument doc = (AbstractDocument)txtField.getDocument();
        DocumentListener[] listeners = doc.getDocumentListeners();
        for (DocumentListener listener : listeners) {
            doc.removeDocumentListener(listener);
        }
        txtField.setText(text);
        for (DocumentListener listener : listeners) {
            doc.addDocumentListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // we're always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_credentialField.setEnabled(enabled);
        m_usernameOnlyField.setEnabled(enabled);
        m_usernameField.setEnabled(enabled);
        m_passwordField.setEnabled(enabled);
        m_typeCredential.setEnabled(enabled);
        m_typeUser.setEnabled(enabled);
        m_typeUserPwd.setEnabled(enabled);
        m_typeKerberos.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        //TODO
    }

    /**
     * @param settings
     * @param specs
     * @param cp
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final CredentialsProvider cp) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        ItemListener[] itemListeners = m_credentialField.getItemListeners();
        for (ItemListener listener : itemListeners) {
            m_credentialField.removeItemListener(listener);
        }
        loadCredentials(cp);
        for (ItemListener listener : itemListeners) {
            m_credentialField.addItemListener(listener);
        }
        updateComponent();
    }

    /** Loads items in credentials select box. */
    public void loadCredentials(final CredentialsProvider cp) {
        final SettingsModelAuthentication model = (SettingsModelAuthentication)getModel();
        m_credentialField.removeAllItems();
        final Collection<String> names = cp.listNames();
        if (names != null) {
            for (final String option : names) {
                m_credentialField.addItem(option);
            }
        }
        m_credentialField.setSelectedItem(model.getCredential());
    }

    /**
     * Called whenever the authentication type has changed.
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        updateModel();
        updatePanel();
    }

    /**
     * Set the text displayed in the usernameLabel
     *
     * @param usernameLabel the label text to be set
     */
    public void setUsernameLabel(final String usernameLabel) {
        m_usernameLabel.setText(usernameLabel);
        updateComponent();
    }

    /**
     * Set the text displayed in the passwordLabel
     *
     * @param passwordLabel the label text to be set
     */
    public void setPasswordLabel(final String passwordLabel) {
        m_passwordLabel.setText(passwordLabel);
        updateComponent();
    }
}
