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
 *   14.04.2009 (gabriel): created
 */
package org.knime.workbench.core;

import javax.crypto.SecretKey;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;

/**
 * Encryption key supplier used to en-/decrypt password (using a master key) in 
 * an eclipse headless mode.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class EclipseEncryptionKeySupplier implements
        EncryptionKeySupplier {
 
    /** Last master entered with the dialog/preference page. */
    public String m_lastMasterKey;

    /** If encryption with master key is enabled. */
    public boolean m_isEnabled;

    /** If a master key has been set already. */
    public boolean m_isSet;

    /**
     * Creates a new encryption key supplier.
     */
    public EclipseEncryptionKeySupplier() {
        init();
    }
    
    /**
     * Read preference store.
     * @return current master key or null, if not set
     */
    private synchronized String init() {
        IPreferenceStore coreStore = 
            KNIMECorePlugin.getDefault().getPreferenceStore();
        m_isSet = coreStore.getBoolean(
                HeadlessPreferencesConstants.P_MASTER_KEY_DEFINED);
        if (m_isSet) {
            m_isEnabled = coreStore.getBoolean(
                    HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED);
            if (!m_isEnabled) {
                m_lastMasterKey = null;
                return null;
            } else {
                if (coreStore.getBoolean(
                        HeadlessPreferencesConstants.P_MASTER_KEY_SAVED)) {
                    try {
                        String mk = coreStore.getString(
                                     HeadlessPreferencesConstants.P_MASTER_KEY);
                        // preference store returns empty string if not set
                        if (mk.isEmpty()) {
                            m_lastMasterKey = null;
                            return null;
                        }
                        SecretKey sk = KnimeEncryption.createSecretKey(
                                HeadlessPreferencesConstants.P_MASTER_KEY);
                        m_lastMasterKey = KnimeEncryption.decrypt(sk, mk);
                    } catch (Exception e) {
                        NodeLogger.getLogger(EclipseEncryptionKeySupplier.class)
                            .warn("Unable to decrypt master key: " 
                                    + e.getMessage(), e);
                        m_lastMasterKey = null;
                    }
                    return m_lastMasterKey;
                }
                return null;
            }
        } else {
            m_lastMasterKey = null;
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getEncryptionKey() {
        return init();
    }
    
}
