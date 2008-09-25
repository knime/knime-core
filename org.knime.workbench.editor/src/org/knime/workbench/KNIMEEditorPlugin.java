/* 
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
package org.knime.workbench;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class for the editor.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEEditorPlugin extends AbstractUIPlugin {
    // Make sure that this *always* matches the ID in plugin.xml
    /** The Plugin ID. */
    public static final String PLUGIN_ID = "org.knime.workbench.editor";

    // The shared instance.
    private static KNIMEEditorPlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;
    
    
    /**
     * Type of this port.
     */
    public static final PortType PMML_PORT_TYPE = new PortType(PMMLPortObject.class);

    /**
     * The constructor.
     */
    public KNIMEEditorPlugin() {
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
        KNIMEUIPlugin.getDefault().getPreferenceStore();
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
     * @return The shared instance of this plugin
     */
    public static KNIMEEditorPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     * 
     * @param key The resourc key
     * @return The resource string
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle =
                KNIMEEditorPlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle.
     * 
     * @return The resource bundle
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle.getBundle(
                        "org.knime.workbench.editor.Resources");
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
        }
        return m_resourceBundle;
    }
}
