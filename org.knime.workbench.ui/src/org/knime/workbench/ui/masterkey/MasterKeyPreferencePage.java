/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   31.03.2008 (gabriel): created
 */
package org.knime.workbench.ui.masterkey;

import javax.crypto.SecretKey;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Preference page used to enter (or not) a master key for KNIME.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MasterKeyPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {
    
    private BooleanFieldEditor m_isMasterKey;
    private StringFieldEditor m_masterKey;
    private StringFieldEditor m_masterKeyConfirm;
    private BooleanFieldEditor m_saveMasterKey;
    
    /**
     * Static encryption key supplier registered with the eclipse framework
     * and serves as a master key provider when the preference page is opened
     * the first time.
     */
    public static final EclipseEncryptionKeySupplier SUPPLIER = 
        new EclipseEncryptionKeySupplier();    
    
    private static class EclipseEncryptionKeySupplier 
            implements EncryptionKeySupplier {
        private String m_lastMasterKey;
        private boolean m_isEnabled;
        private boolean m_isSet;

        /**
         * {@inheritDoc}
         */
        public synchronized String getEncryptionKey() {
            m_isSet = KNIMEUIPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.P_MASTER_KEY_DEFINED);
            if (m_isSet) {
                m_isEnabled = KNIMEUIPlugin.getDefault().getPreferenceStore()
                    .getBoolean(PreferenceConstants.P_MASTER_KEY_ENABLED);
                if (!m_isEnabled) {
                    return null;
                } else {
                    if (KNIMEUIPlugin.getDefault().getPreferenceStore()
                          .getBoolean(PreferenceConstants.P_MASTER_KEY_SAVED)) {
                        try {
                            String mk = KNIMEUIPlugin.getDefault().
                                getPreferenceStore().getString(
                                    PreferenceConstants.P_MASTER_KEY);
                            SecretKey sk = KnimeEncryption.createSecretKey(
                                    PreferenceConstants.P_MASTER_KEY);
                            m_lastMasterKey = KnimeEncryption.decrypt(sk, mk);
                        } catch (Exception e) {
                            m_lastMasterKey = null;
                        } 
                    }
                    if (m_lastMasterKey != null) {
                        return m_lastMasterKey;
                    }
                }
            }
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    m_lastMasterKey = openDialogAndReadKey();
                    m_isSet = true;
                }
            });
            return m_lastMasterKey;
        }
    }
   
    /**
     * 
     */
    public MasterKeyPreferencePage() {
        super(GRID);
        setDescription(DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        final Composite parent = getFieldEditorParent();
        m_isMasterKey = new BooleanFieldEditor(
                PreferenceConstants.P_MASTER_KEY_ENABLED, 
                "Enable password en-/decryption", parent);
        m_isMasterKey.load();
        super.addField(m_isMasterKey);
        m_masterKey = new StringFieldEditor(
                "master_key_field", "Master Key: ", 20, parent);
        m_masterKey.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKey);
        m_masterKeyConfirm = new StringFieldEditor(
                "master_key_field", "Confirm: ", 20, parent);
        m_masterKeyConfirm.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKeyConfirm);
        m_saveMasterKey = new BooleanFieldEditor(
                PreferenceConstants.P_MASTER_KEY_SAVED, 
                "Save Master Key and don't ask again on restart (unsafe)", 
                parent);
        m_saveMasterKey.load();
        super.addField(m_saveMasterKey);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        super.initialize();
        // load stored or current master key
        if (SUPPLIER.m_lastMasterKey == null) {
            m_masterKey.load();
            try {
                String mk = KNIMEUIPlugin.getDefault().
                getPreferenceStore().getString(
                    PreferenceConstants.P_MASTER_KEY);
                SecretKey sk = KnimeEncryption.createSecretKey(
                    PreferenceConstants.P_MASTER_KEY);
                SUPPLIER.m_lastMasterKey = KnimeEncryption.decrypt(sk, mk);
            } catch (Exception e) {
                m_masterKey.setErrorMessage("Could not encrypt Master Key:\n"
                        + e.getMessage());
            }
            m_masterKey.setStringValue(SUPPLIER.m_lastMasterKey);
            m_masterKeyConfirm.setStringValue(SUPPLIER.m_lastMasterKey);
        } else {
            m_masterKey.setStringValue(SUPPLIER.m_lastMasterKey);
            m_masterKeyConfirm.setStringValue(SUPPLIER.m_lastMasterKey);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performOk() {
        getPreferenceStore().setValue(
                PreferenceConstants.P_MASTER_KEY, "");
        if (m_isMasterKey.getBooleanValue()) {
            String masterKey = m_masterKey.getStringValue();
            String encryptedMasterKey = 
                checkMasterKeys(masterKey, m_masterKeyConfirm.getStringValue());
            if (encryptedMasterKey == null) {
                SUPPLIER.m_isSet = false;
                return false;
            }
            SUPPLIER.m_isEnabled = true;
            SUPPLIER.m_lastMasterKey = masterKey;
            if (m_saveMasterKey.getBooleanValue()) {
                getPreferenceStore().setValue(
                    PreferenceConstants.P_MASTER_KEY, encryptedMasterKey);
             }
        } else {
            SUPPLIER.m_isEnabled = false;
            SUPPLIER.m_lastMasterKey = null;
        }
        SUPPLIER.m_isSet = true;
        getPreferenceStore().setValue(PreferenceConstants.P_MASTER_KEY_DEFINED,
                Boolean.toString(SUPPLIER.m_isSet));
        getPreferenceStore().setValue(PreferenceConstants.P_MASTER_KEY_ENABLED, 
                Boolean.toString(SUPPLIER.m_isEnabled));
        getPreferenceStore().setValue(PreferenceConstants.P_MASTER_KEY_SAVED, 
                Boolean.toString(m_saveMasterKey.getBooleanValue()));
        return true;        
    }
    
    
    private static String checkMasterKeys(final String masterKey, 
            final String confirmMasterKey) {
        if (masterKey == null || masterKey.isEmpty()) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Empty master key...");
            mb.setMessage("The master key must not be empty.");
            mb.open();
            return null;
        }
        if (!masterKey.equals(confirmMasterKey)) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Confirm master key...");
            mb.setMessage("Make sure both master keys are the same.");
            mb.open();
            return null;
        }
        try {
            SecretKey secretKey = KnimeEncryption.createSecretKey(
                        PreferenceConstants.P_MASTER_KEY);
            return KnimeEncryption.encrypt(secretKey, masterKey.toCharArray());
        } catch (Exception e) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Master Key Encryption...");
            mb.setMessage("Master Key Encryption failed:\n" + e.getMessage());
            mb.open();
            return null; 
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void performApply() {
        this.performOk();
    }
    
    private static final String DESCRIPTION = 
              "KNIME requires an encryption key to encrypt/decrypt passwords,\n"
            + "mainly for database passwords in nodes connecting to databases\n"
            + "(e.g. database reader/writer nodes).\n"
            + "This avoids having to enter passwords for each new session\n"
            + "and each node individually. In order to avoid storing those\n"
            + "passwords in plain text, a central encryption key is used.\n\n"

            + "No key has been supplied yet to encrypt/decrypt passwords.\n\n"

            + "Note: if you have entered a key in a previous session which\n"
            + "has been used to encrypt passwords, those passwords can\n"
            + "obviously only be decrypted with the same key.\n\n";
    

    private static String openDialogAndReadKey() {
        Shell shell = Display.getDefault().getActiveShell();
        if (shell == null) {
            shell = new Shell();
        }
        new MasterKeyDialog(shell).open();
        return SUPPLIER.m_lastMasterKey;
    }
    

}
