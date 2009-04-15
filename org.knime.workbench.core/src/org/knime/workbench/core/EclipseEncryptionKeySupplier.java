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

import org.knime.core.node.NodeLogger;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;

/**
 * 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class EclipseEncryptionKeySupplier implements
        EncryptionKeySupplier {
    
    public String m_lastMasterKey;

    public boolean m_isEnabled;

    public boolean m_isSet;

    public EclipseEncryptionKeySupplier() {
        // load stored or current master key
        if (m_lastMasterKey == null) {
            try {
                String mk = KNIMECorePlugin.getDefault().getPreferenceStore().getString(
                        HeadlessPreferencesConstants.P_MASTER_KEY);
                SecretKey sk = KnimeEncryption.createSecretKey(
                        HeadlessPreferencesConstants.P_MASTER_KEY);
                m_lastMasterKey = KnimeEncryption.decrypt(sk, mk);
            } catch (Exception e) {
                NodeLogger.getLogger(EclipseEncryptionKeySupplier.class).warn(
                        "Can't load MasterKey: " + e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getEncryptionKey() {
        m_isSet = KNIMECorePlugin.getDefault().getPreferenceStore().getBoolean(
                HeadlessPreferencesConstants.P_MASTER_KEY_DEFINED);
        if (m_isSet) {
            m_isEnabled = KNIMECorePlugin.getDefault().getPreferenceStore().getBoolean(
                    HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED);
            if (!m_isEnabled) {
                return null;
            } else {
                if (KNIMECorePlugin.getDefault().getPreferenceStore().getBoolean(
                        HeadlessPreferencesConstants.P_MASTER_KEY_SAVED)) {
                    try {
                        String mk = KNIMECorePlugin.getDefault().getPreferenceStore().getString(
                                     HeadlessPreferencesConstants.P_MASTER_KEY);
                        SecretKey sk = KnimeEncryption.createSecretKey(
                                HeadlessPreferencesConstants.P_MASTER_KEY);
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
        return m_lastMasterKey;
    }
    
}
