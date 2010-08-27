/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   16.11.2005 (gdf): created
 */

package org.knime.core.node.defaultnodesettings;

import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.KnimeEncryption;

/**
 * Provide a standard component for a dialog that allows to edit a text field.
 *
 * @author Thomas Gabriel, University of Konstanz
 *
 */
public final class DialogComponentPasswordField extends DialogComponent {

    // the min, max and default width of the edit field, if not set explicitly
    private static final int FIELD_MINWIDTH = 5;

    private static final int FIELD_DEFWIDTH = 15;

    private static final int FIELD_MAXWIDTH = 30;

    private final JPasswordField m_pwField;

    private final JLabel m_label;

    private boolean m_containsDefaultValue;

    /**
     * Constructor put label and JTextField into panel.
     *
     * @param label label for dialog in front of JTextField
     * @param stringModel the model that stores the value for this component.
     */
    public DialogComponentPasswordField(final SettingsModelString stringModel,
            final String label) {
        this(stringModel, label,
                calcDefaultWidth(stringModel.getStringValue()));
    }

    /**
     * Constructor put label and JTextField into panel.
     *
     * @param label label for dialog in front of JTextField
     * @param stringModel the model that stores the value for this component.
     * @param compWidth the width of the component (in columns/characters)
     */
    public DialogComponentPasswordField(final SettingsModelString stringModel,
            final String label, final int compWidth) {
        super(stringModel);

        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_pwField = new JPasswordField();
        m_pwField.setColumns(compWidth);

        m_pwField.addFocusListener(new FocusListener() {
            public void focusLost(final FocusEvent e) {
                // not doing anything
            }

            public void focusGained(final FocusEvent e) {
                // if the password field contains the value we've set, we
                // clear the field (because the user can't know what's in there)
                if (m_containsDefaultValue) {
                    m_containsDefaultValue = false;
                    m_pwField.setText("");
                    // this triggers the master key dialog.
                    // Otherwise it shows after the first char is entered
                    try {
                        encrypt("foo".toCharArray());
                    } catch (Exception e1) {
                        // ignore
                    }
                }
            }

        });
        m_pwField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void insertUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void changedUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }
        });

        // password fields will not notify model listeners when the password
        // gets changed.

        // update the pw field, whenever the model changes
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        getComponentPanel().add(m_pwField);
        m_containsDefaultValue = true;

        // call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * @param defaultValue the default value in the component
     * @return the width of the spinner, derived from the defaultValue.
     */
    private static int calcDefaultWidth(final String defaultValue) {
        if ((defaultValue == null) || (defaultValue.length() == 0)) {
            // no default value, return the default width of 15
            return FIELD_DEFWIDTH;
        }
        if (defaultValue.length() < FIELD_MINWIDTH) {
            // the editfield should be at least 15 columns wide
            return FIELD_MINWIDTH;
        }
        if (defaultValue.length() > FIELD_MAXWIDTH) {
            return FIELD_MAXWIDTH;
        }
        return defaultValue.length();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {

        clearError(m_pwField);

        if (m_containsDefaultValue) {
            // if this is the loaded/unchanged password, put the encrypted
            // password in, to not show the user the length of the set password
            String pw = ((SettingsModelString)getModel())
                    .getStringValue();
            if (!new String(m_pwField.getPassword()).equals(pw)) {
                m_pwField.setText(pw);
            }
            // update the enable status too
            setEnabledComponents(getModel().isEnabled());
            return;
        }

        char[] componentPw = new char[0];
        char[] modelPw = new char[0];

        try {
            // keep the decrypted password in the component!
            componentPw = m_pwField.getPassword();
            final String str =
                    ((SettingsModelString)getModel()).getStringValue();
            if (str != null && !str.isEmpty()) {
                modelPw = decrypt(str).toCharArray();
            }
            if (!Arrays.equals(componentPw, modelPw)) {
                // only update component if values are different
                m_pwField.setText(decrypt(str));
            }

            // update the enable status too
            setEnabledComponents(getModel().isEnabled());
        } catch (Throwable t) {
            NodeLogger.getLogger(DialogComponentPasswordField.class).debug(
                    "Couldn't update password component.", t);
            // no update then...
        } finally {
            Arrays.fill(componentPw, '\0');
            Arrays.fill(modelPw, '\0');
        }
    }

    /**
     * Transfers the value from the component into the settings model.
     *
     * @throws InvalidSettingsException if there was a problem encrypting the
     *             password
     */
    private void updateModel() throws InvalidSettingsException {
        // we transfer the value from the field into the model...
        if (!m_containsDefaultValue) {
            // ...only if user changed it
            final char[] pw = m_pwField.getPassword();
            try {
                if (pw.length == 0) {
                    // don't encrypt an empty password
                    ((SettingsModelString)getModel()).setStringValue("");
                } else {
                    ((SettingsModelString)getModel())
                            .setStringValue(encrypt(pw));
                }
            } catch (final Exception e) {
                showError(m_pwField);
                throw new InvalidSettingsException(
                        "Could not encrypt password.");
            } finally {
                Arrays.fill(pw, '\0');
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // passwords are always valid
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // called before a new value is loaded (when the dialog opens)
        m_containsDefaultValue = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_pwField.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_pwField.setPreferredSize(new Dimension(width, height));
    }

    /**
     * Encrypts password.
     *
     * @param password Char array.
     * @return The password encrypt.
     * @throws Exception If something goes wrong.
     */
    public static final String encrypt(final char[] password) throws Exception {
        return KnimeEncryption.encrypt(password);
    }

    /**
     * Decrypts password.
     *
     * @param password The password to decrypt.
     * @return The decrypted password.
     * @throws Exception If something goes wrong.
     */
    public static final String decrypt(final String password) throws Exception {
        return KnimeEncryption.decrypt(password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_pwField.setToolTipText(text);
    }

}
