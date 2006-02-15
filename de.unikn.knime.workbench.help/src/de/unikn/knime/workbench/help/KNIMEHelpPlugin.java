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
 *   19.01.2005 (georg): created
 */
package de.unikn.knime.workbench.help;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * Help plugin class
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEHelpPlugin extends Plugin {
    // Make sure that this *always* matches the ID in plugin.xml
    /** The plugin ID */
    public static final String PLUGIN_ID = "de.unikn.knime.workbench.help";

    // The shared instance.
    private static KNIMEHelpPlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    /**
     * The constructor.
     */
    public KNIMEHelpPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     * 
     * @param context The bundle context
     * @throws Exception If failed
     */
    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * This method is called when the plug-in is stopped
     * 
     * @param context The bundle context
     * @throws Exception If failed
     */
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        m_resourceBundle = null;
    }

    /**
     * @return Returns the shared instance.
     */
    public static KNIMEHelpPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     * 
     * @param key The resource key
     * @return The resource String
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = KNIMEHelpPlugin.getDefault()
                .getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * @return Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle
                        .getBundle("de.unikn.knime.workbench.help.Resources");
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
        }
        return m_resourceBundle;
    }
}
