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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 *
 * @author Lara Gorini
 * @since 3.2
 */
public final class DialogComponentAuthentication extends DialogComponent {

    private final JRadioButton m_checkCredential;

    private final JComboBox<String> m_credentialField;

    private final JRadioButton m_checkUser;

    private final JTextField m_usernameField;

    private final JPasswordField m_passwordField;

    /**
     * @param authModel
     */
    public DialogComponentAuthentication(final SettingsModelAuthentication authModel) {
        super(authModel);

        final Box authBox = Box.createVerticalBox();

        m_checkCredential = new JRadioButton("Use credentials");
        authBox.add(m_checkCredential);

        m_credentialField = new JComboBox<>();
        authBox.add(m_credentialField);

        m_checkUser = new JRadioButton("Use username & password");
        authBox.add(m_checkUser);

        authBox.add(new JLabel("Username:"));
        m_usernameField = new JTextField();
        authBox.add(m_usernameField);

        authBox.add(new JLabel("Password:"));
        m_passwordField = new JPasswordField();
        authBox.add(m_passwordField);

        m_checkCredential.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setSelected(m_checkCredential.isSelected());
            }

        });

        m_checkUser.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setSelected(!m_checkUser.isSelected());
            }
        });

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

        getComponentPanel().add(authBox);

        //call this method to be in sync with the settings model
        updateComponent();
    }

    private void updateModel() {

        final SettingsModelAuthentication model = (SettingsModelAuthentication)getModel();

//        model.setCredentialSelected(m_checkCredential.isSelected());
//
//        model.setCredential((String)m_credentialField.getSelectedItem());
//        model.setUsername(m_usernameField.getText());
        char[] password = m_passwordField.getPassword();
        String pwd = null;
        if (password != null && password.length > 1) {
            pwd = new String(password);
        }
//        model.setPassword(pwd);

        model.setValues((String)m_credentialField.getSelectedItem(), m_checkCredential.isSelected(), m_usernameField.getText(), pwd);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // only update component if values are off
        final SettingsModelAuthentication model = (SettingsModelAuthentication)getModel();
        setEnabledComponents(model.isEnabled());
        setSelected(model.isCredentialSelected());
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

        if (!m_usernameField.getText().equals(model.getUsername())) {
            updateNoListener(m_usernameField, model.getUsername());
        }

        if (model.getPassword() != null) {
            String modelPwd = model.getPassword();
            char[] password = m_passwordField.getPassword();
            String componentPwd = null;
            if (password != null && password.length > 1) {
                componentPwd = new String(password);
            }
            if ((componentPwd == null && modelPwd != null) || !componentPwd.equals(modelPwd)) {
                updateNoListener(m_passwordField, modelPwd);
            }
        }



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

    private void setSelected(final boolean credentialIsSelected) {
        m_checkCredential.setSelected(credentialIsSelected);
        m_credentialField.setEnabled(credentialIsSelected);
        m_checkUser.setSelected(!credentialIsSelected);
        m_usernameField.setEnabled(!credentialIsSelected);
        m_passwordField.setEnabled(!credentialIsSelected);
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
        m_usernameField.setEnabled(enabled);
        m_passwordField.setEnabled(enabled);
        m_checkCredential.setEnabled(enabled);
        m_checkUser.setEnabled(enabled);
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
        final SettingsModelAuthentication model = (SettingsModelAuthentication)getModel();
        for (ItemListener listener : itemListeners) {
            m_credentialField.removeItemListener(listener);
        }
        m_credentialField.removeAllItems();
        final Collection<String> names = cp.listNames();
        if (names != null) {
            for (final String option : names) {
                m_credentialField.addItem(option);
            }
        }
        m_credentialField.setSelectedItem(model.getCredential());
        for (ItemListener listener : itemListeners) {
            m_credentialField.addItemListener(listener);
        }
    }
}
