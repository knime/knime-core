/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 */
package org.knime.workbench.core.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.workbench.core.KNIMECorePlugin;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class HeadlessPreferencesInitializer extends
        AbstractPreferenceInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = KNIMECorePlugin.getDefault()
            .getPreferenceStore();
        store.setDefault(HeadlessPreferencesConstants.P_MAXIMUM_THREADS,
                2 * Runtime.getRuntime().availableProcessors());

        store.setDefault(HeadlessPreferencesConstants.P_TEMP_DIR,
                System.getProperty("java.io.tmpdir"));

        store.setDefault(HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE,
                LEVEL.DEBUG.name());

        // set default values
        store.setDefault(KNIMECorePlugin.P_LOGLEVEL_CONSOLE,
                LEVEL.WARN.name());
    }

}
