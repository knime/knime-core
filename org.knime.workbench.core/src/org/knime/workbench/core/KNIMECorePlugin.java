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
 *   ${date} (${user}): created
 */
package org.knime.workbench.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
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
public class KNIMECorePlugin extends AbstractUIPlugin {

    /** Make sure that this *always* matches the ID in plugin.xml. */
    public static final String PLUGIN_ID = "org.knime.workbench.core";

    // The shared instance.
    private static KNIMECorePlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            KNIMECorePlugin.class);

    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_CONSOLE = "logging.loglevel.console";
    
    
    /**
     * Keeps list of <code>ConsoleViewAppender</code>. TODO FIXME remove
     * static if you want to have a console for each Workbench
     */
    private static final ArrayList<ConsoleViewAppender> APPENDERS =
            new ArrayList<ConsoleViewAppender>();
    
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
        try {
            // get the preference store
            // with the preferences for nr threads and tempDir
            IPreferenceStore pStore =
                KNIMECorePlugin.getDefault().getPreferenceStore();
            initMaxThreadCountProperty();
            initTmpDirProperty();
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
                        File f = new File(event.getNewValue().toString());
                        LOGGER.debug("Setting temp dir to " 
                                + f.getAbsolutePath());
                        try {
                            KNIMEConstants.setKNIMETempDir(f);
                        } catch (Exception e) {
                            LOGGER.warn("Setting temp dir failed", e);
                        }
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
                        NodeLogger.setLevel(level);
                    } else if (P_LOGLEVEL_CONSOLE.equals(
                            event.getProperty())) {
                        String newName = event.getNewValue().toString();
                        setLogLevel(newName);
                    }
                }
            });
            // end property listener
                            
            String logLevelConsole =
                pStore.getString(P_LOGLEVEL_CONSOLE);
            // TODO: only if awt.headless ==  false
            if (!Boolean.valueOf(
                    System.getProperty("java.awt.headless", "false"))) {
                try {
                    ConsoleViewAppender.WARN_APPENDER.write(
                            KNIMEConstants.WELCOME_MESSAGE);
                    ConsoleViewAppender.WARN_APPENDER.write(
                    "Log file is located at: "
                    + KNIMEConstants.getKNIMEHomeDir() + File.separator
                    + NodeLogger.LOG_FILE + "\n");
                } catch (IOException ioe) {
                    LOGGER.error("Could not print welcome message: ", ioe);
                }
                setLogLevel(logLevelConsole);
            }
            // encryption key supplier registered with the eclipse framework
            // and serves as a master key provider
            KnimeEncryption.setEncryptionKeySupplier(
                    new EclipseEncryptionKeySupplier());
        } catch (Throwable e) {
            LOGGER.error("FATAL: error initializing KNIME"
                    + " repository - check plugin.xml" + " and classpath", e);
        }
        
    }

    private void initMaxThreadCountProperty() {
        IPreferenceStore pStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        int maxThreads = pStore.getInt(
                HeadlessPreferencesConstants.P_MAXIMUM_THREADS);
        String maxTString = 
            System.getProperty(KNIMEConstants.PROPERTY_MAX_THREAD_COUNT);
        if (maxTString == null) {
            if (maxThreads <= 0) {
                LOGGER.warn("Can't set " + maxThreads
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
    }

    private void initTmpDirProperty() {
        IPreferenceStore pStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        String tmpDirPref = pStore.getString(
                HeadlessPreferencesConstants.P_TEMP_DIR);
        String tmpDirSystem = System.getProperty(
                KNIMEConstants.PROPERTY_TEMP_DIR);
        File tmpDir = null;
        if (tmpDirSystem == null) {
            if (tmpDirPref != null) {
                tmpDir = new File(tmpDirPref);
                if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                    LOGGER.warn("Can't set " + tmpDirPref + " as temp dir");
                    tmpDir = null;
                }
            }
        } else {
            tmpDir = new File(tmpDirSystem);
            if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                LOGGER.warn("Can't set " + tmpDirSystem + " as temp dir");
                // try to set path from preference page as fallback
                tmpDir = new File(tmpDirPref);
                if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                    LOGGER.warn("Can't set " + tmpDirPref + " as temp dir");
                    tmpDir = null;
                }
            }
        }
        if (tmpDir != null) {
            LOGGER.debug("Setting KNIME temp dir to \"" 
                    + tmpDir.getAbsolutePath() + "\"");
            KNIMEConstants.setKNIMETempDir(tmpDir);
        }
    }
    
    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        // remove appender listener from "our" NodeLogger
        for (int i = 0; i < APPENDERS.size(); i++) {
            removeAppender(APPENDERS.get(i));
        }
        super.stop(context);
        plugin = null;
        m_resourceBundle = null;
    }
    
    
    /**
     * Register the appenders according to logLevel, i.e.
     * PreferenceConstants.P_LOGLEVEL_DEBUG,
     * PreferenceConstants.P_LOGLEVEL_INFO, etc.
     *
     * @param logLevel The new log level.
     */
    private static void setLogLevel(final String logLevel) {
        // check if can create a console view
        // only possible if we are not "headless"
        if (Boolean.valueOf(System.getProperty("java.awt.headless", "false"))) {
            return;
        }
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
