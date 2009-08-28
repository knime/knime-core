/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
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
        IPreferenceStore prefStore = getPreferenceStore();
        // for backward compatibility we need to ensure that the master key
        // pref. store values are copied from the ui into the core plugin
        // FIXME: remove with 2.1+ version of KNIME
        // fix: master key settings are saved in core plugin only
        IPreferenceStore corePrefStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        if (!corePrefStore.contains(HeadlessPreferencesConstants.P_MASTER_KEY_DEFINED)
             && prefStore.contains(HeadlessPreferencesConstants.P_MASTER_KEY_DEFINED)) {
            corePrefStore.setValue(
                    HeadlessPreferencesConstants.P_MASTER_KEY_DEFINED,
                    prefStore.getString(HeadlessPreferencesConstants.P_MASTER_KEY_DEFINED));
            corePrefStore.setValue(HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED,
                    prefStore.getString(HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED));
            corePrefStore.setValue(HeadlessPreferencesConstants.P_MASTER_KEY_SAVED,
                    prefStore.getString(HeadlessPreferencesConstants.P_MASTER_KEY_SAVED));
            corePrefStore.setValue(HeadlessPreferencesConstants.P_MASTER_KEY,
                    prefStore.getString(HeadlessPreferencesConstants.P_MASTER_KEY));
        }
        // create a knime encryption supplier that reads in an encryption key
        // from the user via a dialog or directly from the preference page
        KnimeEncryption.setEncryptionKeySupplier(
                MasterKeyPreferencePage.SUPPLIER);

        getImageRegistry().put("knime",
                imageDescriptorFromPlugin(PLUGIN_ID,
                        "/icons/knime_default.png"));
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
                    if (!(event.getNewValue() instanceof Integer)) {
                        // when preferences are imported and this value is
                        // not set, they send an empty string
                        return;
                    }
                    int count;
                    try {
                        count = (Integer)event.getNewValue();
                        NodeUsageRegistry.setMaxFrequentSize(count);
                    } catch (Exception e) {
                        LOGGER.error("Unable to set maximum number of "
                                + "frequently used nodes", e);
                    }
                } else if (PreferenceConstants.P_FAV_LAST_USED_SIZE.equals(
                        prop)) {
                    if (!(event.getNewValue() instanceof Integer)) {
                        // when preferences are imported and this value is
                        // not set, they send an empty string
                        return;
                    }
                    int count;
                    try {
                        count = (Integer)event.getNewValue();
                        NodeUsageRegistry.setMaxLastUsedSize(count);
                    } catch (Exception e) {
                        LOGGER.error("Unable to set maximum number of "
                                + "last used nodes", e);
                    }
                }

            }
        });

        try {
            NodeUsageRegistry.setMaxFrequentSize(freqHistorySize);
            NodeUsageRegistry.setMaxLastUsedSize(usedHistorySize);
        } catch (Exception e) {
            LOGGER.error("Error during loading of node usage history: ", e);
        }
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
     * @param filename The relative filename
     * @return The image, a default will be returned if file was missing.
     */
    public Image getImage(final String pluginID, final String filename) {
        return this.getImage(this.getImageDescriptor(pluginID, filename));

    }

    /**
     * Returns a image descriptor.
     *
     * @param pluginID The plugin ID
     * @param filename The relative filename
     * @return The descriptor, or null
     */
    public ImageDescriptor getImageDescriptor(final String pluginID,
            final String filename) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(pluginID, filename);
    }

}
