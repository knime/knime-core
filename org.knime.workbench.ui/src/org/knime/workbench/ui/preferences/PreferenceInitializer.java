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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Class used to initialize default preference values.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        // get the preference store for the UI plugin
        IPreferenceStore store = KNIMEUIPlugin.getDefault()
                .getPreferenceStore();
        
        store.setDefault(PreferenceConstants.P_CONFIRM_RESET, true);
        
        store.setDefault(PreferenceConstants.P_CONFIRM_DELETE, true);
        
        store.setDefault(PreferenceConstants.P_CONFIRM_RECONNECT, true);
        
        store.setDefault(PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE, 10);
        
        store.setDefault(PreferenceConstants.P_FAV_LAST_USED_SIZE, 10);
        
    }
}
