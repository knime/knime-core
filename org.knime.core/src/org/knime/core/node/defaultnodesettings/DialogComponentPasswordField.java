/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

package org.knime.core.node.defaultnodesettings;

import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Provide a standard component for a dialog that allows to edit a text field.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 */
public final class DialogComponentPasswordField extends DialogComponent {

    private final JPasswordField m_pwField;

    private boolean m_containsDefaultValue;

    /**
     * Secret for the password de- and encyption.
     */
    private static SecretKey mSECRETKEY;
    static {
        try {
            mSECRETKEY = KeyGenerator.getInstance("DES").generateKey();
        } catch (NoSuchAlgorithmException e) {
            NodeLogger.getLogger("Password").warn("Could not generate DES key",
                    e);
        }
    }

    /**
     * Constructor put label and JTextField into panel.
     * 
     * @param label label for dialog in front of JTextField
     * @param stringModel the model that stores the value for this component.
     */
    public DialogComponentPasswordField(final SettingsModelString stringModel,
            final String label) {
        super(stringModel);

        this.add(new JLabel(label));
        m_pwField = new JPasswordField();
        m_pwField.setColumns(30);

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

        this.add(m_pwField);
        m_containsDefaultValue = true;

    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #updateComponent()
     */
    @Override
    void updateComponent() {
        final String str = ((SettingsModelString)getModel()).getStringValue();
        m_pwField.setText(str);
        m_containsDefaultValue = true;
    }

    /**
     * Transfers the value from the component into the settingsmodel.
     * 
     * @throws InvalidSettingsException if there was a problem encrypting the
     *             password
     */
    private void updateModel() throws InvalidSettingsException {
        // we transfer the value from the field into the model...
        if (!m_containsDefaultValue) {
            // ...only if user changed it
            char[] pw = m_pwField.getPassword();
            try {
                ((SettingsModelString)getModel()).setStringValue(encrypt(pw));
            } catch (Exception e) {
                showError(m_pwField);
                throw new InvalidSettingsException(
                        "Could not encrypt password.");
            } finally {
                Arrays.fill(pw, '\0');
            }
        }
    }

    /**
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // we are always good.
    }

    /**
     * @see DialogComponent #setEnabledComponents(boolean)
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
     * Enrypts password.
     * 
     * @param password Char array.
     * @return The password encrypt.
     * @throws Exception If something goes wrong.
     */
    public static final String encrypt(final char[] password) throws Exception {
        // Create Cipher
        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, mSECRETKEY);
        byte[] ciphertext = desCipher.doFinal(new String(password).getBytes());
        return new BASE64Encoder().encode(ciphertext);
    }

    /**
     * Decrypts password.
     * 
     * @param password The password to decrypt.
     * @return The decrypted password.
     * @throws Exception If something goes wrong.
     */
    public static final String decrypt(final String password) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, mSECRETKEY);
        // perform the decryption
        byte[] pw = new BASE64Decoder().decodeBuffer(password);
        byte[] decryptedText = cipher.doFinal(pw);
        String result = new String(decryptedText);
        Arrays.fill(pw, (byte)0);
        Arrays.fill(decryptedText, (byte)0);
        return result;
    }

}
