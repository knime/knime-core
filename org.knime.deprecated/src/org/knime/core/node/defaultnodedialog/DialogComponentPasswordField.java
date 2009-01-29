/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   16.11.2005 (gdf): created
 */

package org.knime.core.node.defaultnodedialog;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPasswordField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.KnimeEncryption;

/**
 * Provide a standard component for a dialog that allows to edit a text field.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * 
 */
public final class DialogComponentPasswordField extends DialogComponent {

    private final JPasswordField m_pwField;

    private final String m_configName;
    
    /**
     * Constructor put label and JTextField into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of JTextField
     */
    public DialogComponentPasswordField(
            final String configName, final String label) {
        this.add(new JLabel(label));
        m_pwField = new JPasswordField();
        this.add(m_pwField);
        m_configName = configName;
    }

    /**
     * The password should never be stored into the settings by the
     * NodeModel. The password field stays empty on update.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        String pw = settings.getString(m_configName, "");
        try {
            m_pwField.setText(decrypt(pw));
        } catch (Exception e) {
            m_pwField.setText("");
        }
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        char[] pw = m_pwField.getPassword();
        try {
            String password = encrypt(pw);
            settings.addString(m_configName, password);
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not encryt password.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_pwField.setEnabled(enabled);
    }
    
    /**
     * Sets the preferred size of the internal component.
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_pwField.setPreferredSize(new Dimension(width, height));
    }
    
    /**
     * Enrypts password.
     * 
     * @param password Char array.
     * @return The password encrypt.
     * @throws Exception If something goes wrong.
     * @see KnimeEncryption#encrypt(char[])
     * @deprecated call {@link KnimeEncryption#encrypt(char[])} directly
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
     * @see KnimeEncryption#decrypt(String)
     * @deprecated call {@link KnimeEncryption#decrypt(String)} directly
     */
    public static final String decrypt(final String password) throws Exception {
        return KnimeEncryption.decrypt(password);
    }
    
}
