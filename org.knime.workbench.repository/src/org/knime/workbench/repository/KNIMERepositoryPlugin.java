/*
 * ---------------------------------------------------------------------------
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
 * ---------------------------------------------------------------------------
 */
package org.knime.workbench.repository;

import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.workbench.core.WorkbenchErrorLogger;
import org.knime.workbench.preferences.HeadlessPreferencesConstants;
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
            // get the preference store
            // with the preferences for nr threads and tempDir
            IPreferenceStore pStore =
                KNIMERepositoryPlugin.getDefault().getPreferenceStore();
            int maxThreads = pStore.getInt(
                    HeadlessPreferencesConstants.P_MAXIMUM_THREADS);
            String maxTString = System.getProperty("org.knime.core.maxThreads");
            if (maxTString == null) {
                if (maxThreads <= 0) {
                    LOGGER.warn("Can set " + maxThreads
                            + " as number of threads to use");
                } else {
                    KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(maxThreads);
                    LOGGER.debug("Setting KNIME max thread count to "
                            + maxThreads);
                }
            } else {
                LOGGER.debug("Ignoring thread count from preference page (" 
                        + maxThreads + "), since it has set by java property " 
                        + "\"org.knime.core.maxThreads\" (" + maxTString + ")");
            }
            String tmpDir = pStore.getString(
                    HeadlessPreferencesConstants.P_TEMP_DIR);
            // check for existence and if writable
            File tmpDirFile = new File(tmpDir);
            if (!(tmpDirFile.isDirectory() && tmpDirFile.canWrite())) {
                LOGGER.error("Can't set temp directory to \"" + tmpDir + "\", "
                        + "not a directory or not writable");
            } else {
                System.setProperty("java.io.tmpdir", tmpDir);
                LOGGER.debug("Setting temp dir environment variable "
                        + "(java.io.tmpdir) to \"" + tmpDir + "\"");
            }

            // set log file level to stored
            String logLevelFile =
                pStore.getString(HeadlessPreferencesConstants
                        .P_LOGLEVEL_LOG_FILE);
            NodeLogger.setLevel(LEVEL.valueOf(logLevelFile));

            pStore.addPropertyChangeListener(new IPropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {
                    if (event.getProperty().equals(
                            HeadlessPreferencesConstants.P_MAXIMUM_THREADS)) {
                        int count;
                        try {
                            count = (Integer)event.getNewValue();
                            KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(
                                    count);
                        } catch (Exception e) {
                            LOGGER.warn("Unable to get maximum thread count "
                                    + " from preference page.", e);
                        }
                    } else if (event.getProperty().equals(
                            HeadlessPreferencesConstants.P_TEMP_DIR)) {
                        System.setProperty("java.io.tmpdir", (String)event
                                .getNewValue());
                    } else if (event.getProperty().equals(
                            HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE)) {
                        String newName = event.getNewValue().toString();
                        LEVEL level = LEVEL.WARN;
                        try {
                            level = LEVEL.valueOf(newName);
                        } catch (NullPointerException ne) {
                            LOGGER.warn(
                                    "Null is an invalid log level, using WARN");
                        } catch (IllegalArgumentException iae) {
                            LOGGER.warn("Invalid log level " + newName
                                    + ", using WARN");
                        }
                        NodeLogger.setLevelIntern(level);
                    }
                }
            });
            RepositoryManager.INSTANCE.create();
        } catch (Throwable e) {
            WorkbenchErrorLogger.error("FATAL: error initializing KNIME"
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
