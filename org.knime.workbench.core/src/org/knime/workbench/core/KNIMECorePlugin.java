/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   ${date} (${user}): created
 */
package org.knime.workbench.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;


/**
 * The core plugin, basically a holder for the framework's jar and some minor
 * workbench componentes that are needed everywhere (ErrorDialog,...).
 * 
 * NOTE: Plugins need to depend upon this, as this plugin exports the underlying
 * framework API !!
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMECorePlugin extends Plugin {

    /** Make sure that this *always* matches the ID in plugin.xml. */
    public static final String PLUGIN_ID = "org.knime.workbench.core";

    // The shared instance.
    private static KNIMECorePlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    /**
     * The constructor.
     */
    public KNIMECorePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
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
     * @return Singleton instance of the Core Plugin
     */
    public static KNIMECorePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     * 
     * @param key The resource key
     * @return The resource value, or the key if not found in the resource
     *         bundle
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = KNIMECorePlugin.getDefault()
                .getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle.
     * 
     * @return The resource bundle, or <code>null</code>
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle.getBundle(plugin
                        .getClass().getName());
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
            WorkbenchErrorLogger
                    .warning("Could not locate resource bundle for "
                            + plugin.getClass().getName());
        }
        return m_resourceBundle;
    }

}
