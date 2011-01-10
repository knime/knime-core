/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------------
 */
package org.knime.workbench.repository;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleContext;

/**
 * Repository Plugin.
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMERepositoryPlugin extends AbstractUIPlugin {
    // Make sure that this *always* matches the ID in plugin.xml
    /** The plugin-id. */
    public static final String PLUGIN_ID = "org.knime.workbench."
            + "repository";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            KNIMERepositoryPlugin.class);

    // The shared instance.
    private static KNIMERepositoryPlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    // image registry
    private ImageRegistry m_imageRegistry;

    /**
     * The constructor.
     */
    public KNIMERepositoryPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation. We're showing a little
     * opening splashScreen wit a progress bar, while building up the
     * repository.
     *
     * @param context The context
     * @throws Exception some startup exception
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        // Do the actual work: load the repository
        try {
            RepositoryManager.INSTANCE.create();
        } catch (Throwable e) {
            LOGGER.error("FATAL: error initializing KNIME"
                    + " repository - check plugin.xml" + " and classpath", e);
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The context
     * @throws Exception some stopping exception
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
     * @return The plugin instance (singleton)
     */
    public static KNIMERepositoryPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     *
     * @param key The key
     * @return The string
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = KNIMERepositoryPlugin.getDefault()
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
     * @return The bundle
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle
                        .getBundle("org.knime.workbench.repository."
                                + "Resources");
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
        }
        return m_resourceBundle;
    }

    /**
     * Returns a (cached) image from the image registry.
     *
     * @param descriptor The image descriptor
     * @return The image, or a default image if missing.
     */
    public Image getImage(final ImageDescriptor descriptor) {

        if (descriptor == null) {
            return null;
        }

        // create the registry if needed
        if (m_imageRegistry == null) {
            // if the imageRegistry is not created within the UI thread
            // then the UI thread has to be invoked with Display.getDefault();
            // this has to be done when KNIME is started without the eclipse GUI
            if (Display.getCurrent() == null) {
                Display.getDefault();
                assert Display.getCurrent() != null;
            }
            m_imageRegistry = new ImageRegistry();
        }
        // try to lookup previously cached image

        Image img = m_imageRegistry.get(descriptor.toString());

        // if null, create the image and store it in the registry for further
        // requests
        if (img == null) {
            img = descriptor.createImage(true);
            m_imageRegistry.put(descriptor.toString(), img);
        }

        return img;
    }

    /**
     * This only works for images located in the KNIMERepositry Plugin !
     *
     * @param filename The filename, relative to the KNIMERepositryPlugin root
     * @return The image, default will be supplied if missing.
     */
    public Image getImage(final String filename) {
        return this.getImage(PLUGIN_ID, filename);
    }

    /**
     * Load a image from the given location from within the plugin.
     *
     * @param pluginID The ID of the hosting plugin
     * @param filename The elative filename
     * @return The image, a default will be returned if file was missing.
     */
    public Image getImage(final String pluginID, final String filename) {
        return this.getImage(this.getImageDescriptor(pluginID, filename));

    }

    /**
     * Returns a image descriptor.
     *
     * @param pluginID The plugin ID
     * @param filename Th relative filename
     * @return The descriptor, or null
     */
    public ImageDescriptor getImageDescriptor(final String pluginID,
            final String filename) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(pluginID, filename);
    }
}
