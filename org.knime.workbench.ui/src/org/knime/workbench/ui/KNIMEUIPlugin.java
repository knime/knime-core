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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.ui.favorites.FavoriteNodesManager;
import org.knime.workbench.ui.masterkey.MasterKeyPreferencePage;
import org.knime.workbench.ui.metanodes.MetaNodeTemplateRepositoryView;
import org.knime.workbench.ui.preferences.PreferenceConstants;
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

    // image registry
    private ImageRegistry m_imageRegistry;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(KNIMEUIPlugin.class);


    /**
     * Keeps list of <code>ConsoleViewAppender</code>. TODO FIXME remove
     * static if you want to have a console for each Workbench
     */
    private static final ArrayList<ConsoleViewAppender> APPENDERS =
            new ArrayList<ConsoleViewAppender>();

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

        // create a knime encryption supplier that reads in an encryption key
        // from the user via a dialog or directly from the preference page
        KnimeEncryption.setEncryptionKeySupplier(
                MasterKeyPreferencePage.SUPPLIER);

        IPreferenceStore prefStore = getPreferenceStore();
        getImageRegistry().put("knime",
                imageDescriptorFromPlugin(PLUGIN_ID, "/icons/knime.png"));
        int freqHistorySize = prefStore.getInt(
                PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE);
        int usedHistorySize = prefStore.getInt(
                PreferenceConstants.P_FAV_LAST_USED_SIZE);

        prefStore.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                String prop = event.getProperty();
                if (PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE.equals(
                        prop)) {
                    int count;
                    try {
                        count = (Integer)event.getNewValue();
                        NodeUsageRegistry.setMaxFrequentSize(count);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to set maximum number of "
                                + "frequently used nodes", e);
                    }
                } else if (PreferenceConstants.P_FAV_LAST_USED_SIZE.equals(
                        prop)) {
                    int count;
                    try {
                        count = (Integer)event.getNewValue();
                        NodeUsageRegistry.setMaxLastUsedSize(count);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to set maximum number of "
                                + "last used nodes", e);
                    }
                } else if (PreferenceConstants.P_LOGLEVEL_CONSOLE.equals(
                        prop)) {
                    String newName = event.getNewValue().toString();
                    setLogLevel(newName);
                }

            }
        });

        try {
            NodeUsageRegistry.setMaxFrequentSize(freqHistorySize);
            NodeUsageRegistry.setMaxLastUsedSize(usedHistorySize);
        } catch (Exception e) {
            LOGGER.error("Error during loading of node usage history: ", e);
        }

        String logLevelConsole =
            prefStore.getString(PreferenceConstants.P_LOGLEVEL_CONSOLE);
        try {
            ConsoleViewAppender.WARN_APPENDER
                .write(KNIMEConstants.WELCOME_MESSAGE);
            ConsoleViewAppender.WARN_APPENDER.write("Log file is located at: "
                    + KNIMEConstants.getKNIMEHomeDir() + File.separator
                    + NodeLogger.LOG_FILE + "\n");
        } catch (IOException ioe) {
            LOGGER.error("Could not print welcome message: ", ioe);
        }
        setLogLevel(logLevelConsole);
    }


    /**
     * Register the appenders according to logLevel, i.e.
     * PreferenceConstants.P_LOGLEVEL_DEBUG,
     * PreferenceConstants.P_LOGLEVEL_INFO, etc.
     *
     * @param logLevel The new log level.
     */
    private static void setLogLevel(final String logLevel) {
        boolean changed = false;
        if (logLevel.equals(LEVEL.DEBUG.name())) {
            changed |= addAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.INFO.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.WARN.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.ERROR.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else {
            LOGGER.warn("Invalid log level " + logLevel + "; setting to "
                    + LEVEL.WARN.name());
            setLogLevel(LEVEL.WARN.name());
        }
        if (changed) {
            LOGGER.info("Setting console view log level to " + logLevel);
        }
    }


    /**
     * Add the given Appender to the NodeLogger.
     *
     * @param app Appender to add.
     * @return If the given appender was not previously registered.
     */
    static boolean addAppender(final ConsoleViewAppender app) {
        if (!APPENDERS.contains(app)) {
            NodeLogger.addWriter(app, app.getLevel(), app.getLevel());
            APPENDERS.add(app);
            return true;
        }
        return false;
    }

    /**
     * Removes the given Appender from the NodeLogger.
     *
     * @param app Appender to remove.
     * @return If the given appended was previously registered.
     */
    static boolean removeAppender(final ConsoleViewAppender app) {
        if (APPENDERS.contains(app)) {
            NodeLogger.removeWriter(app);
            APPENDERS.remove(app);
            return true;
        }
        return false;
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
        // if the FavoriteNodesManager was not initialized
        // then we do not need to save it, since no changes happened
        // to the last used and most frequent used has not changed
        // which is the case, when the workflow is only loaded and not
        // structurally changed.
        // @see FavoritesView#usedHistoryChanged
        // @see FavoritesView#frequentHistoryChanged
        if (FavoriteNodesManager.wasInitialized()) {
            FavoriteNodesManager.getInstance().saveFavoriteNodes();
        }
        // remove appender listener from "our" NodeLogger
        for (int i = 0; i < APPENDERS.size(); i++) {
            removeAppender(APPENDERS.get(i));
        }
        if (MetaNodeTemplateRepositoryView.wasInitialized()) {
            MetaNodeTemplateRepositoryView.getInstance().dispose();
        }
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
                m_resourceBundle =
                        ResourceBundle
                                .getBundle("org.knime.workbench.ui.Resources");
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
