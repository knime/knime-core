/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.product;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class ProductPlugin extends AbstractUIPlugin {
    // The shared instance.
    private static ProductPlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    /**
     * The constructor.
     */
    public ProductPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context BundleContext
     * @throws Exception on error
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context BundleContext
     * @throws Exception on error
     * 
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        m_resourceBundle = null;
    }

    /**
     * @return Returns the shared instance.
     */
    public static ProductPlugin getDefault() {
        return plugin;
    }

    /**
     * @param key The resource key
     * @return the string from the plugin's resource bundle, or 'key' if not
     *         found.
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = ProductPlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * @return the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle
                        .getBundle("org.knime.product."
                                + "ProductPluginResources");
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
        }
        return m_resourceBundle;
    }
}
