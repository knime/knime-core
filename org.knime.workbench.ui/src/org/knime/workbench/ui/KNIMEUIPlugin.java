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
 * -------------------------------------------------------------------
 *
 * History
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
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
@SuppressWarnings("restriction")
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

        // create a knime encryption supplier that reads in an encryption key
        // from the user via a dialog or directly from the preference page
        KnimeEncryption.setEncryptionKeySupplier(
                MasterKeyPreferencePage.SUPPLIER);

        if (Display.getCurrent() != null) {
            // do not load UI stuff if we run, e.g. the batch executor
            getImageRegistry().put("knime",
                    imageDescriptorFromPlugin(PLUGIN_ID,
                            "/icons/knime_default.png"));
        }

        IPreferenceStore prefStore = getPreferenceStore();
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

        // hide already installed IU by default in install wizard
        // its a bit dirty but there is no API to set the option
        // the constants are taken from AvailableIUsPage
        IDialogSettings ds = ProvUIActivator.getDefault().getDialogSettings();
        IDialogSettings ds2 = ds.getSection("AvailableIUsPage");
        if (ds2 == null) {
           ds2 = ds.addNewSection("AvailableIUsPage");
           ds2.put("HideInstalledContent", true);
        } else if (ds2.get("HideInstalledContent") == null) {
            ds2.put("HideInstalledContent", true);
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     * @param context The bundle context
     * @throws Exception If failed
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
        IJobManager jobMan = Job.getJobManager();
        jobMan.cancel(getBundle().getSymbolicName());
        jobMan.join(getBundle().getSymbolicName(), null);
        plugin = null;
        m_resourceBundle = null;
        super.stop(context);
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
