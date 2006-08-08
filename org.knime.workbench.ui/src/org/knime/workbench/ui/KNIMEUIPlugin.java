/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
package org.knime.workbench.ui;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Plugin class for the eclipse UI contributions.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEUIPlugin extends AbstractUIPlugin {
    // Make sure that this *always* matches the ID in plugin.xml

    /** The plugin ID. */
    public static final String PLUGIN_ID = "org.knime.workbench.ui";

    // The shared instance.
    private static KNIMEUIPlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    /**
     * The constructor.
     */
    public KNIMEUIPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context The bundle context
     * @throws Exception If failed
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        getImageRegistry().put("knime",
                imageDescriptorFromPlugin(PLUGIN_ID, "/icons/knime.png"));

    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The bundle context
     * @throws Exception If failed
     * 
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        m_resourceBundle = null;
    }

    /**
     * Returns the shared instance.
     * 
     * @return The shared plugin instance
     */
    public static KNIMEUIPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     * 
     * @param key The resource key
     * @return The resource string
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = KNIMEUIPlugin.getDefault().getResourceBundle();
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
                        .getBundle("org.knime.workbench.ui.Resources");
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
        }
        return m_resourceBundle;
    }

}
