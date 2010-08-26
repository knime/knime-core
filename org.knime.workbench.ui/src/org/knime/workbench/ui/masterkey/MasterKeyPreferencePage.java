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
 * --------------------------------------------------------------------- *
 */
package org.knime.workbench.ui.masterkey;

import javax.crypto.SecretKey;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.EclipseEncryptionKeySupplier;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;

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
     * Static encryption key supplier registered with the eclipse framework and
     * serves as a master key provider when the preference page is opened the
     * first time.
     */
    public static final EclipseEncryptionKeySupplier SUPPLIER =
        new EclipseEncryptionKeySupplier() {
            /**
             * Derived method to open a dialog, if the master key is not
             * set. {@inheritDoc}
             */
            @Override
            public synchronized String getEncryptionKey() {
                super.getEncryptionKey();
                if (m_isEnabled) {
                    if (m_lastMasterKey == null || m_lastMasterKey.isEmpty()) {
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                MasterKeyDialog.openDialogAndReadKey();
                            }
                        });
                    }
                    return m_lastMasterKey;
                } else {
                    return null;
                }
            }
        };
        
    private static final String OBSOLETE_MASTERKEY =
        "Starting with KNIME 2.2 the master key becomes obsolete and is\n"
        + "replaced by the Workflow Credentials. Those credentials with\n"
        + "user name and password can be defined independently for each\n"
        + "workflow (available in the Workflow Projects' context menu).\n\n";

    /** Description used when a master key was already used. */
    private static final String NEW_DESCRIPTION =
        "A master key was entered in a previous session which\n"
        + "has been used to encrypt passwords, those passwords can\n"
        + "only be decrypted with the same master key.\n\n";

    /** Description used within the master key preference page and dialog. */
    private static final String DESCRIPTION =
        "The master key is used to encrypt/decrypt passwords,\n"
        + "mainly for database passwords in nodes connecting to databases\n"
        + "(e.g. database reader/writer nodes).\n"
        + "This avoids having to enter passwords for each new session\n"
        + "and each node individually. In order to avoid storing those\n"
        + "passwords in plain text, a central master key is used.\n\n";

    /**
     * Create a new master key preference page.
     */
    public MasterKeyPreferencePage() {
        super(GRID);
        setDescription(OBSOLETE_MASTERKEY + DESCRIPTION);
    }

    /**
     * Create a new master key preference page composite used inside an dialog.
     * @param flag ignored
     */
    MasterKeyPreferencePage(final boolean flag) {
        super(GRID);
        if (SUPPLIER.m_wasSet) {
            setDescription(OBSOLETE_MASTERKEY + NEW_DESCRIPTION + DESCRIPTION);
        } else {
            setDescription(OBSOLETE_MASTERKEY + DESCRIPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        final Composite parent = getFieldEditorParent();
        m_isMasterKey = new BooleanFieldEditor(
                HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED,
                "Enable password en-/decryption", parent) {
            /** {@inheritDoc}  */
            @Override
            protected void valueChanged(final boolean old, final boolean neu) {
                enableFields(neu);
            }
        };
        m_isMasterKey.load();
          super.addField(m_isMasterKey);
        m_masterKey = new StringFieldEditor("master_key_field", "Master Key: ",
                20, parent);
        m_masterKey.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKey);
        m_masterKeyConfirm = new StringFieldEditor("master_key_field",
                "Confirm: ", 20, parent);
        m_masterKeyConfirm.getTextControl(parent).setEchoChar('*');
        super.addField(m_masterKeyConfirm);
        m_saveMasterKey = new BooleanFieldEditor(
            HeadlessPreferencesConstants.P_MASTER_KEY_SAVED,
            "Save Master Key and don't ask again on restart (unsafe)", parent);
        m_saveMasterKey.load();
        super.addField(m_saveMasterKey);
    }

    private void enableFields(final boolean enabled) {
        Composite parent = super.getFieldEditorParent();
        m_isMasterKey.setEnabled(true, parent);
        m_masterKey.setEnabled(enabled, parent);
        m_masterKey.setEmptyStringAllowed(!enabled);
        m_masterKeyConfirm.setEnabled(enabled, parent);
        m_masterKeyConfirm.setEmptyStringAllowed(!enabled);
        m_saveMasterKey.setEnabled(enabled, parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        super.initialize();
        initPrefStore();
        // init dialog options from preference store
        if (SUPPLIER.m_lastMasterKey == null
                || SUPPLIER.m_lastMasterKey.isEmpty()) {
            try {
                String mk = getPreferenceStore().getString(
                        HeadlessPreferencesConstants.P_MASTER_KEY);
                SecretKey sk =
                        KnimeEncryption.createSecretKey(
                                HeadlessPreferencesConstants.P_MASTER_KEY);
                SUPPLIER.m_lastMasterKey = KnimeEncryption.decrypt(sk, mk);
            } catch (Exception e) {
                m_masterKey.setErrorMessage("Could not encrypt Master Key:\n"
                        + e.getMessage());
            }
            m_masterKey.setStringValue(SUPPLIER.m_lastMasterKey);
            m_masterKeyConfirm.setStringValue(SUPPLIER.m_lastMasterKey);
            if (m_isMasterKey.getBooleanValue()) {
                setErrorMessage("Master key must not be empty.");
            }
        } else {
            m_masterKey.setStringValue(SUPPLIER.m_lastMasterKey);
            m_masterKeyConfirm.setStringValue(SUPPLIER.m_lastMasterKey);
        }
        enableFields(SUPPLIER.m_isEnabled);
    }

    /**
     * Called from the master key dialog to enable/disable the master key.
     */
    final void enableMasterKey() {
        Composite parent = super.getFieldEditorParent();
        if (SUPPLIER.m_wasSet) {
            m_isMasterKey.setEnabled(false, parent);
        } else {
            m_isMasterKey.setEnabled(true, parent);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        initPrefStore();
    }

    /**
     * Preference store is initialized by the org.knime.workbench.core
     * store.
     */
    public void initPrefStore() {
        IPreferenceStore corePrefStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        setPreferenceStore(corePrefStore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performOk() {
        IPreferenceStore pstore =
                KNIMECorePlugin.getDefault().getPreferenceStore();
        pstore.setValue(HeadlessPreferencesConstants.P_MASTER_KEY, "");
        if (m_isMasterKey.getBooleanValue()) {
            String masterKey = m_masterKey.getStringValue();
            String encryptedMasterKey =
                    checkMasterKeys(masterKey, m_masterKeyConfirm
                            .getStringValue());
            if (encryptedMasterKey == null) {
                return false;
            }
            SUPPLIER.m_isEnabled = true;
            SUPPLIER.m_lastMasterKey = masterKey;
            if (m_saveMasterKey.getBooleanValue()) {
                pstore.setValue(HeadlessPreferencesConstants.P_MASTER_KEY,
                        encryptedMasterKey);
            }
        } else {
            SUPPLIER.m_isEnabled = false;
            SUPPLIER.m_lastMasterKey = null;
        }
        pstore.setValue(HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED,
                Boolean.toString(SUPPLIER.m_isEnabled));
        pstore.setValue(HeadlessPreferencesConstants.P_MASTER_KEY_SAVED,
                Boolean.toString(m_saveMasterKey.getBooleanValue()));
        return true;
    }

    private String checkMasterKeys(final String masterKey,
            final String confirmMasterKey) {
        setErrorMessage(null);
        if (masterKey == null || masterKey.isEmpty()) {
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setText("Empty master key...");
            mb.setMessage("Master Key must not be empty.");
            setErrorMessage(mb.getMessage());
            mb.open();
            return null;
        }
        if (!masterKey.equals(confirmMasterKey)) {
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setMessage("Make sure both master keys are the same.");
            setErrorMessage(mb.getMessage());
            mb.open();
            return null;
        }
        try {
            SecretKey secretKey = KnimeEncryption.createSecretKey(
                    HeadlessPreferencesConstants.P_MASTER_KEY);
            return KnimeEncryption.encrypt(secretKey, masterKey.toCharArray());
        } catch (Exception e) {
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setText("Master Key Encryption...");
            mb.setMessage("Master Key Encryption failed:\n" + e.getMessage());
            setErrorMessage(mb.getMessage());
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

}
