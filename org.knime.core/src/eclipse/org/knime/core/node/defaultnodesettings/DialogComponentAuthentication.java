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
 * -------------------------------------------------------------------
 *
 * History
 *   21.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 *   25.09.2006 (ohl): using SettingsModel
 */
package org.knime.core.node.defaultnodesettings;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer.FlowVariableCell;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.Pair;

/**
 * A component that allows a user to enter username/password or select credentials variable.
 * @author Lara Gorini
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
public final class DialogComponentAuthentication extends DialogComponent implements ActionListener {

    private static final Insets NEUTRAL_INSET = new Insets(0, 0, 0, 0);

    private final ButtonGroup m_authenticationType = new ButtonGroup();

    private final JRadioButton m_typeNone;
    private final JRadioButton m_typeUser;
    private final JRadioButton m_typeUserPwd;
    private final JRadioButton m_typePwd;
    private final JRadioButton m_typeCredential;
    private final JRadioButton m_typeKerberos;

    private final JComboBox<FlowVariableCell> m_credentialField = new JComboBox<>();

    private final JTextField m_usernameOnlyField = new JTextField(20);

    private final JTextField m_usernameField = new JTextField(20);

    private final JPasswordField m_passwordField = new JPasswordField(20);

    private final JLabel m_usernameLabel = new JLabel("Username:", SwingConstants.LEFT);
    private final JLabel m_passwordLabel = new JLabel("Password:", SwingConstants.LEFT);

    private final JPasswordField m_passwordOnlyField = new JPasswordField(20);

    private final JLabel m_passwordOnlyLabel = new JLabel("Password:", SwingConstants.LEFT);

    private final Component m_credentialPanel;

    private final Component m_userPanel;

    private final Component m_userPwdPanel;

    private final Component m_pwdPanel;

    private final String m_label;

    private JPanel m_rootPanel;

    private HashSet<AuthenticationType> m_supportedTypes;
    private Map<AuthenticationType, Pair<String, String>> m_namingMap = new HashMap<>();

    private JRadioButton createAuthenticationTypeButton(final AuthenticationType type, final ButtonGroup group,
        final ActionListener l) {
        boolean contains = m_namingMap.containsKey(type);
        String buttonLabel = contains ? m_namingMap.get(type).getFirst() : type.getText();
        String toolTip = contains ? m_namingMap.get(type).getSecond() : type.getToolTip();

        final JRadioButton button = new JRadioButton(buttonLabel);
        button.setActionCommand(type.getActionCommand());
        if (type.isDefault()) {
            button.setSelected(true);
        }
        if (type.getToolTip() != null) {
            button.setToolTipText(toolTip);
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
        this(authModel, label, Arrays.asList(supportedTypes), null);
    }

    /** Constructor for this dialog component
     *
     * @param authModel The {@link SettingsModel}
     * @param label The label.
     * @param namingMap The map containing the {@link AuthenticationType} as key and a pair
     *          consisting of the label and the tooltip String for the radio buttons for authentication types
     * @param supportedTypes the authentication {@link AuthenticationType}s to display
     * @since 3.3
     */
    public DialogComponentAuthentication(final SettingsModelAuthentication authModel, final String label,
        final HashMap<AuthenticationType, Pair<String, String>> namingMap, final AuthenticationType... supportedTypes) {
        this(authModel, label, Arrays.asList(supportedTypes), namingMap);
    }


    /** Constructor for this dialog component
     *
     * @param authModel The {@link SettingsModel}
     * @param label The label.
     * @param namingMap The map containing the {@link AuthenticationType} as key and a pair
     *          consisting of the label and the tooltip String for the radio buttons for authentication types
     * @param supportedTypes the authentication {@link AuthenticationType}s to display
     * @since 4.0
     */
    public DialogComponentAuthentication(final SettingsModelAuthentication authModel, final String label,
        final Collection<AuthenticationType> supportedTypes,
        final Map<AuthenticationType, Pair<String, String>> namingMap) {
        this(authModel, label, supportedTypes, namingMap, false);
    }

    /** Constructor for this dialog component
     *
     * @param authModel The {@link SettingsModel}
     * @param label The label.
     * @param namingMap The map containing the {@link AuthenticationType} as key and a pair
     *          consisting of the label and the tooltip String for the radio buttons for authentication types
     * @param supportedTypes the authentication {@link AuthenticationType}s to display
     * @param horizontal <code>true</code> if the different options should be placed horizontal otherwise vertical
     * @since 5.0
     */
    public DialogComponentAuthentication(final SettingsModelAuthentication authModel, final String label,
        final Collection<AuthenticationType> supportedTypes,
        final Map<AuthenticationType, Pair<String, String>> namingMap, final boolean horizontal) {
        super(authModel);
        m_supportedTypes = new HashSet<>(supportedTypes);
        m_label = label;

        if (namingMap != null) {
            m_namingMap = namingMap;
        }
        m_typeNone = createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.NONE, m_authenticationType, this);
        m_typeUser = createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.USER, m_authenticationType, this);
        m_typeUserPwd = createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.USER_PWD, m_authenticationType, this);
        m_typePwd = createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.PWD, m_authenticationType, this);
        m_typeCredential = createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.CREDENTIALS, m_authenticationType, this);
        m_typeKerberos = createAuthenticationTypeButton(SettingsModelAuthentication.AuthenticationType.KERBEROS, m_authenticationType, this);

        final int leftInset;
        if (horizontal) {
            leftInset = 0;
        } else {
            leftInset = 23;
        }
        m_credentialPanel = getCredentialPanel(leftInset);
        m_userPanel = getUserPanel(leftInset);
        m_userPwdPanel = getUserPwdPanel(leftInset);
        m_pwdPanel = getPwdPanel(leftInset);

        if (horizontal) {
            m_rootPanel = getHorizontalRootPanel();
        } else {
            m_rootPanel = getRootPanel();
        }
        getComponentPanel().setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        // connection
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;


        getComponentPanel().add(m_rootPanel, gbc);

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

        m_passwordOnlyField.getDocument().addDocumentListener(new DocumentListener() {
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
            gbc.insets = new Insets(0,23,0,0);
            authBox.add(m_userPanel, gbc);
            gbc.insets = NEUTRAL_INSET;
        }

        if (m_supportedTypes.contains(AuthenticationType.USER_PWD)) {
            if (m_supportedTypes.size() > 1) {
                gbc.gridy++;
                authBox.add(m_typeUserPwd, gbc);
            }
            gbc.gridy++;
            authBox.add(m_userPwdPanel, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.PWD)) {
            if (m_supportedTypes.size() > 1) {
                gbc.gridy++;
                authBox.add(m_typePwd, gbc);
            }
            gbc.gridy++;
            authBox.add(m_pwdPanel, gbc);
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

    private JPanel getHorizontalRootPanel() {
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
            gbc.gridx++;
            if (m_supportedTypes.size() > 1) {
                authBox.add(m_typeCredential, gbc);
            }
            gbc.gridx++;
            authBox.add(m_credentialPanel, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.USER)) {
            gbc.gridx++;
            if (m_supportedTypes.size() > 1) {
                authBox.add(m_typeUser, gbc);
            }
            gbc.gridx++;
            gbc.insets = new Insets(0,23,0,0);
            authBox.add(m_userPanel, gbc);
            gbc.insets = NEUTRAL_INSET;
        }

        if (m_supportedTypes.contains(AuthenticationType.USER_PWD)) {
            if (m_supportedTypes.size() > 1) {
                gbc.gridx++;
                authBox.add(m_typeUserPwd, gbc);
            }
            gbc.gridx++;
            authBox.add(m_userPwdPanel, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.PWD)) {
            if (m_supportedTypes.size() > 1) {
                gbc.gridx++;
                authBox.add(m_typePwd, gbc);
            }
            gbc.gridx++;
            authBox.add(m_pwdPanel, gbc);
        }

        if (m_supportedTypes.contains(AuthenticationType.KERBEROS)) {
            gbc.gridx++;
            authBox.add(m_typeKerberos, gbc);
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

    private JPanel getUserPanel(final int leftInset) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, leftInset, 0, 5);
        panel.add(m_usernameLabel, gbc);
        gbc.gridx = 1;
        gbc.insets = NEUTRAL_INSET;
        gbc.ipadx = 10;
        panel.add(m_usernameOnlyField, gbc);
        return panel;
    }

    private JPanel getUserPwdPanel(final int leftInset) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, leftInset, 0, 5);
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
        gbc.insets = new Insets(0, leftInset, 0, 5);
        panel.add(m_passwordLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = NEUTRAL_INSET;
        panel.add(m_passwordField, gbc);
        return panel;
    }

    private JPanel getPwdPanel(final int leftInset) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, leftInset, 0, 5);
        panel.add(m_passwordOnlyLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = NEUTRAL_INSET;
        gbc.ipadx = 10;
        panel.add(m_passwordOnlyField, gbc);
        return panel;
    }

    private Component getCredentialPanel(final int leftInset) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, leftInset, 0, 0);
        m_credentialField.setRenderer(new FlowVariableListCellRenderer());
        m_credentialField.setEditable(false);
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
                credential = getCredentialFromField().orElse(null);
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
                if (isNotEmpty(password)) {
                    pwd = new String(password);
                }
                break;
            case PWD:
                final char[] passwordOnly = m_passwordOnlyField.getPassword();
                if (isNotEmpty(passwordOnly)) {
                    pwd = new String(passwordOnly);
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

        if (model.getAuthenticationType() == AuthenticationType.CREDENTIALS) {
            //update the credential information
            if (getCredentialFromField().filter(name -> !Objects.equals(name, model.getCredential())).isPresent()) {
                ItemListener[] itemListeners = m_credentialField.getItemListeners();
                for (ItemListener listener : itemListeners) {
                    m_credentialField.removeItemListener(listener);
                }
                m_credentialField.setSelectedItem(model.getCredential());
                for (ItemListener listener : itemListeners) {
                    m_credentialField.addItemListener(listener);
                }
            }

        } else if (model.getAuthenticationType() == AuthenticationType.USER) {
          //update the user name only field
            if (!m_usernameOnlyField.getText().equals(model.getUsername())) {
                updateNoListener(m_usernameOnlyField, model.getUsername());
            }

        } else if (model.getAuthenticationType() == AuthenticationType.USER_PWD) {
            //update the user name field
            if (!m_usernameField.getText().equals(model.getUsername())) {
                updateNoListener(m_usernameField, model.getUsername());
            }

            //update the password field
            final var fieldPassword = String.valueOf(m_passwordField.getPassword());
            if (!fieldPassword.equals(model.getPassword())) {
                String modelPwd = model.getPassword();
                String componentPwd = null;
                if (StringUtils.isNotEmpty(fieldPassword)) {
                    componentPwd = fieldPassword;
                }
                if (!Objects.equals(componentPwd, modelPwd)) {
                    updateNoListener(m_passwordField, modelPwd);
                }
            }

        } else if (model.getAuthenticationType() == AuthenticationType.PWD) {
            //update the password field
            final var fieldPassword = String.valueOf(m_passwordOnlyField.getPassword());
            if (!fieldPassword.equals(model.getPassword())) {
                String modelPwd = model.getPassword();
                String componentPwd = null;
                if (StringUtils.isNotEmpty(fieldPassword)) {
                    componentPwd = fieldPassword;
                }
                if (!Objects.equals(componentPwd, modelPwd)) {
                    updateNoListener(m_passwordOnlyField, modelPwd);
                }
            }
        }

        updatePanel();
    }

    /**
     *
     */
    private void updatePanel() {
        final var modelEnabled = getModel().isEnabled();
        final var credentialsAvailable = m_credentialField.getItemCount() > 0 && modelEnabled;
        m_credentialField.setEnabled(credentialsAvailable);
        m_typeCredential.setEnabled(credentialsAvailable);
        m_credentialPanel.setVisible(m_typeCredential.isSelected());
        m_userPwdPanel.setVisible(m_typeUserPwd.isSelected());
        m_userPanel.setVisible(m_typeUser.isSelected());
        m_pwdPanel.setVisible(m_typePwd.isSelected());
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
        clearDeselectedFields();
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
        m_passwordOnlyField.setEnabled(enabled);
        m_typeCredential.setEnabled(enabled);
        m_typeUser.setEnabled(enabled);
        m_typeUserPwd.setEnabled(enabled);
        m_typePwd.setEnabled(enabled);
        m_typeKerberos.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        super.getComponentPanel().setToolTipText(text);
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

    /**
     * Loads items in credentials select box. This implementation caches the last
     * available credentials name. This way, if the selected credential becomes unavailable,
     * it remains cached in the model. Allows for easy re-selection on re-execute.
     *
     * This also means that clients using this method have to check for the existence of the
     * cache credential since it no longer implies that. However, since the credentials are
     * rendered as {@link FlowVariableCell}s, an invalid variable is also marked in the dialog
     * with a red border for the user to see.
     *
     * Note: other dialog components (e.g. the {@link DialogComponentCredentialSelection}) or
     * nodes (e.g. Hub Authenticator, SSH Connector, ...) that allow for credential selection
     * do not cache the last credential name. I.e. everywhere else, after a re-configure, the
     * selected credential name is lost if the implementation does not cache.
     *
     * @param cp CredentialsProvider
     */
    public void loadCredentials(final CredentialsProvider cp) {
        final var model = (SettingsModelAuthentication)getModel();
        m_credentialField.removeAllItems();
        if (cp != null) {
            cp.listVariables().stream() //
                .map(FlowVariableCell::new) //
                .forEach(m_credentialField::addItem);
        }
        // if the credential was lost due to re-configure, it might become available again
        final var selectedCredential = model.getCredential();
        if (cp != null && !cp.listNames().contains(selectedCredential)) {
            // this FlowVariableCell(String) constructor creates an invalid-marked cell
            m_credentialField.addItem(new FlowVariableCell(selectedCredential));
        }
        setCredentialFieldTo(selectedCredential);
    }

    private Optional<String> getCredentialFromField() {
        final var selectedType = AuthenticationType.get(m_authenticationType.getSelection().getActionCommand());
        if (selectedType != AuthenticationType.CREDENTIALS) {
            return Optional.empty();
        }
        return Optional.ofNullable((FlowVariableCell)m_credentialField.getSelectedItem()) //
                .map(FlowVariableCell::getName);
    }

    private void setCredentialFieldTo(final String name) {
        for (var i = 0; i < m_credentialField.getItemCount(); i++) {
            var cell = m_credentialField.getItemAt(i);
            if (cell != null && Objects.equals(cell.getName(), name)) {
                m_credentialField.setSelectedIndex(i);
                return;
            }
        }
        // explicitly deselect the chosen selection if nothing matched, for example
        // if the name is null (JComboBox#setSelectedItem(null) is a no-op)
        m_credentialField.setSelectedIndex(-1);
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
     * @since 3.3
     */
    public void setUsernameLabel(final String usernameLabel) {
        m_usernameLabel.setText(usernameLabel);
        updateComponent();
    }

    /**
     * Set the text displayed in the passwordLabel
     *
     * @param passwordLabel the label text to be set
     * @since 3.3
     */
    public void setPasswordLabel(final String passwordLabel) {
        m_passwordLabel.setText(passwordLabel);
        updateComponent();
    }

    /**
     * Set the text displayed in the passwordOnlyLabel
     *
     * @param passwordOnlyLabel the label text to be set
     * @since 4.1
     */
    public void setPasswordOnlyLabel(final String passwordOnlyLabel) {
        m_passwordOnlyLabel.setText(passwordOnlyLabel);
        updateComponent();
    }

    /**
     * Clears the data of the 'other' authentication types. Added as part of AP-21887 to
     * only store authentication data when needed. To specifically clear the settings model,
     * call {@link SettingsModelAuthentication#clear()}.
     * <p>
     * This method clears the text fields in the dialog, whose cleared contents are then
     * propagated to the settings model on UI action events.
     *
     * @param type authentication type
     */
    private void clearDeselectedFields() {
        final var selectedType = AuthenticationType.get(m_authenticationType.getSelection().getActionCommand());
        for (var otherType : m_supportedTypes) {
            if (otherType == selectedType) {
                continue;
            }
            switch (otherType) {
                case USER -> m_usernameOnlyField.setText("");
                case PWD -> m_passwordOnlyField.setText("");
                case USER_PWD -> {
                    m_usernameField.setText("");
                    m_passwordField.setText("");
                }
                case CREDENTIALS -> m_credentialField.setSelectedItem(-1);
                case NONE, KERBEROS -> { } // NOSONAR avoiding incomplete-switch warning
            }
        }
    }
}
