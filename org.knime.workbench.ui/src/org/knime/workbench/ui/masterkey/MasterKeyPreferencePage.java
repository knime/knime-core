/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
        private Boolean m_isDefined;
        /**
         * {@inheritDoc}
         */
        public String getEncryptionKey() {
            if (m_isDefined != null) {
                 return (m_isDefined.booleanValue() ? m_lastMasterKey : null);
            }
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    m_lastMasterKey = openDialogAndReadKey();
                }
            });
            m_isDefined = (m_lastMasterKey != null);
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
                PreferenceConstants.P_MASTER_KEY_DEFINED, 
                "Enable password en-/decryption", parent);
        m_isMasterKey.load();
        super.addField(m_isMasterKey);
        m_masterKey = new StringFieldEditor(
                PreferenceConstants.P_MASTER_KEY, "Master key: ", 20, parent);
        m_masterKey.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKey);
        m_masterKeyConfirm = new StringFieldEditor(
                PreferenceConstants.P_MASTER_KEY, "Confirm: ", 20, parent);
        m_masterKeyConfirm.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKeyConfirm);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        if (SUPPLIER.m_isDefined != null) {
            getPreferenceStore().setValue(
                    PreferenceConstants.P_MASTER_KEY_DEFINED, 
                    SUPPLIER.m_isDefined);
        }
        super.initialize();
        m_masterKey.setStringValue(SUPPLIER.m_lastMasterKey);
        m_masterKeyConfirm.setStringValue(SUPPLIER.m_lastMasterKey);
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
        if (m_isMasterKey.getBooleanValue()) {
            String masterKey = m_masterKey.getStringValue();
            boolean valid = 
                checkMasterKeys(masterKey, m_masterKeyConfirm.getStringValue());
            if (!valid) {
                SUPPLIER.m_isDefined = false;
                return false;
            }
            SUPPLIER.m_lastMasterKey = m_masterKey.getStringValue();
        } else {
            SUPPLIER.m_lastMasterKey = null;
        }
        SUPPLIER.m_isDefined = m_isMasterKey.getBooleanValue();
        getPreferenceStore().setValue(PreferenceConstants.P_MASTER_KEY_DEFINED, 
                SUPPLIER.m_isDefined);
        return true;
    }
    
    /**
     * 
     * @return the entered master key
     */
    String getMasterKey() {
        return m_masterKey.getStringValue();
    }
    
    private static boolean checkMasterKeys(final String masterKey, 
            final String confirmMasterKey) {
        if (masterKey == null || masterKey.isEmpty()) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Empty master key...");
            mb.setMessage("The master key must not be empty.");
            mb.open();
            return false;
        }
        if (!masterKey.equals(confirmMasterKey)) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Confirm master key...");
            mb.setMessage("Make sure both master keys are the same.");
            mb.open();
            return false;
        }
        return true;
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
            + "and each node individually. To avoid storing those passwords\n"
            + "in plain text, the central encryption key is used.\n\n"

            + "Up to now no key has been supplied which can be used to\n"
            + "encrypt/decrypt the passwords.\n\n"

            + "Note: if you have entered a key in a previous session which\n"
            + "has been used to encrypt passwords, those passwords can\n"
            + "obviously only be decrypted with the same key. \n\n";

    private static String openDialogAndReadKey() {
        try {
        Shell shell = Display.getDefault().getActiveShell();
        if (shell == null) {
            shell = new Shell();
        }
        MasterKeyDialog dialog = new MasterKeyDialog(shell); 
        dialog.open();
        return dialog.getMasterKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    

}
