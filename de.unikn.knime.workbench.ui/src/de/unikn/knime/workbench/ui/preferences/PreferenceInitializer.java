/* 
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
 *   12.01.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.unikn.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Class used to initialize default preference values.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class PreferenceInitializer extends
        AbstractPreferenceInitializer {

    /**
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
     *      #initializeDefaultPreferences()
     */
    public void initializeDefaultPreferences() {
        // get the preference store for the UI plugin
        IPreferenceStore store = KNIMEUIPlugin.getDefault()
                .getPreferenceStore();

        // set default values
        store.setDefault(PreferenceConstants.P_CHOICE_VIEWMODE,
                PreferenceConstants.P_CHOICE_VIEWMODE_JFRAME);

        store.setDefault(PreferenceConstants.P_LOGLEVEL_CONSOLE,
                PreferenceConstants.P_LOGLEVEL_WARN);

        store.setDefault(PreferenceConstants.P_LOGLEVEL_LOG_FILE,
                PreferenceConstants.P_LOGLEVEL_DEBUG);
        
        store.setDefault(PreferenceConstants.P_MAXIMUM_THREADS, 
                2 * Runtime.getRuntime().availableProcessors());
                
    }

}
