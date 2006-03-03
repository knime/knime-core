/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   16.11.2005 (gdf): created
 */

package de.unikn.knime.core.node.defaultnodedialog;

import java.awt.Dimension;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.JLabel;
import javax.swing.JPasswordField;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;

/**
 * Provide a standard component for a dialog that allows to edit a text field.
 * 
 * @author Thomas Gabriel, Konstanz University
 * 
 */
public final class DialogComponentPasswordField extends DialogComponent {

    private final JPasswordField m_pwField;

    private final String m_configName;
    
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
    };
    
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
    public void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) {
        m_pwField.setText("");
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    public void saveSettingsTo(final NodeSettings settings)
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
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #setEnabledComponents(boolean)
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
        return new String(decryptedText);
    }
    
}
