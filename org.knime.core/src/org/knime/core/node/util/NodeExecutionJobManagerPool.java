/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   10.10.2008 (ohl): created
 */
package org.knime.core.node.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.exec.ThreadNodeExecutionJobManagerFactory;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;
import org.knime.core.util.ThreadPool;

/**
 * Collects all registered JobManager extensions and holds an instance of each
 * in a set.
 *
 * @author ohl, University of Konstanz
 */
public final class NodeExecutionJobManagerPool {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(NodeExecutionJobManagerPool.class);

    private static final String EXT_POINT_ID =
            "org.knime.core.NodeExecutionJobManagerFactory";

    private static final String EXT_POINT_ATTR_JOBMGR = "JobManagerFactory";

    // stores all managers, mapped to their id.
    private static LinkedHashMap<String, NodeExecutionJobManagerFactory> managerFactories =
            null;

    /**
     * It's a utility class. No reason to instantiate it.
     */
    private NodeExecutionJobManagerPool() {
        // hide the constructor.
    }

    /**
     * Returns the job manager factory with the specified id - or null if it
     * doesn't exists. If the job manager factory is provided by an extension
     * (plug-in) that is currently not installed this returns null, even though
     * in another KNIME installation it may not.
     *
     * @param id the id of the job manager factory to return
     * @return the job manager factory with the specified id, or null if there
     *         is none.
     */
    public static NodeExecutionJobManagerFactory getJobManagerFactory(
            final String id) {
        if (managerFactories == null) {
            collectJobManagerFactories();
        }
        return managerFactories.get(id);
    }

    private static final String CFG_JOB_MANAGER_FACTORY_ID =
            "job.manager.factory.id";

    private static final String CFG_JOB_MANAGER_SETTINGS =
            "job.manager.settings";

    /**
     * Saves the argument job manager to a settings object.
     *
     * @param jobManager The job manager to save.
     * @param settings To save to.
     */
    public static void saveJobManager(final NodeExecutionJobManager jobManager,
            final NodeSettingsWO settings) {
        settings.addString(CFG_JOB_MANAGER_FACTORY_ID, jobManager.getID());
        NodeSettingsWO sub = settings.addNodeSettings(CFG_JOB_MANAGER_SETTINGS);
        jobManager.save(sub);
    }

    /**
     * Restores a job manager given the parameters contained in the argument
     * settings.
     *
     * @param sncSettings To load from.
     * @return A customized job manager or the default one if no settings were
     *         stored.
     * @throws InvalidSettingsException If that fails.
     */
    public static NodeExecutionJobManager load(final NodeSettingsRO sncSettings)
            throws InvalidSettingsException {
        String jobManagerID = sncSettings.getString(CFG_JOB_MANAGER_FACTORY_ID);
        NodeExecutionJobManagerFactory reference =
                getJobManagerFactory(jobManagerID);
        if (reference == null) {
            throw new InvalidSettingsException(
                    "Unknown job manager factory id \""
                            + jobManagerID
                            + "\" (job manager factory possibly not installed?)");
        }
        NodeSettingsRO sub =
                sncSettings.getNodeSettings(CFG_JOB_MANAGER_SETTINGS);
        NodeExecutionJobManager jobManager = reference.getInstance();
        jobManager.load(sub);
        return jobManager;
    }

    /**
     * Updates the settings of the passed job manager - if the settings specify
     * the same type of job manager - or creates and returns a new instance of
     * that new type of job manager.
     *
     * @param instance the "old" job manager that will be updated if its type
     *            fits the type in the settings, or null to create a new
     *            instance.
     * @param ncSettings the new settings to apply
     * @return either the specified instance with new settings, or a new
     *         instance of a new type with the new settings.
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static NodeExecutionJobManager load(
            final NodeExecutionJobManager instance,
            final NodeSettingsRO ncSettings) throws InvalidSettingsException {
        if (instance == null) {
            // create a new instance then.
            return load(ncSettings);
        }
        String jobManagerID = ncSettings.getString(CFG_JOB_MANAGER_FACTORY_ID);
        if (!instance.getID().equals(jobManagerID)) {
            // The settings request a different job manager: create it then.
            return load(ncSettings);
        }
        NodeSettingsRO sub =
                ncSettings.getNodeSettings(CFG_JOB_MANAGER_SETTINGS);
        instance.load(sub);
        return instance;
    }

    /**
     * There is always at least one job manager factory availably.
     *
     * @return the default job manager
     */
    public static NodeExecutionJobManagerFactory getDefaultJobManagerFactory() {
        return ThreadNodeExecutionJobManagerFactory.INSTANCE;
    }

    /**
     * Returns the number of job manager factories registered through the
     * extension point. A call to this method may trigger instantiation of all
     * job manager factories.
     *
     * @return the number of registered job manager factories
     */
    public static int getNumberOfJobManagersFactories() {
        if (managerFactories == null) {
            collectJobManagerFactories();
        }
        return managerFactories.size();
    }

    /**
     * Returns names of all registered {@link NodeExecutionJobManagerFactory}s.
     * If this method is called for the first time it starts instantiating all
     * job manager factories.
     *
     * @return names of all registered {@link NodeExecutionJobManagerFactory}s
     */
    public static Collection<String> getAllJobManagerFactoryIDs() {
        if (managerFactories == null) {
            collectJobManagerFactories();
        }
        return Collections.unmodifiableCollection(managerFactories.keySet());
    }

    private static void collectJobManagerFactories() {

        managerFactories =
                new LinkedHashMap<String, NodeExecutionJobManagerFactory>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            // let's throw in the default manager - otherwise things fail badly
            managerFactories.put(getDefaultJobManagerFactory().getID(),
                    getDefaultJobManagerFactory());
            LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
            throw new IllegalStateException("ACTIVATION ERROR: "
                    + " --> Invalid extension point: " + EXT_POINT_ID);
        }

        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String jobMgr = elem.getAttribute(EXT_POINT_ATTR_JOBMGR);
            String decl = elem.getDeclaringExtension().getUniqueIdentifier();

            if (jobMgr == null || jobMgr.isEmpty()) {
                LOGGER.error("The extension '" + decl
                        + "' doesn't provide the required attribute '"
                        + EXT_POINT_ATTR_JOBMGR + "'");
                LOGGER.error("Extension " + decl + " ignored.");
                continue;
            }

            // try instantiating the job manager.
            NodeExecutionJobManagerFactory instance = null;
            try {
                // TODO: THE THREADED MANAGER NEEDS TO BE RE-WRITTEN!
                if (jobMgr.equals(getDefaultJobManagerFactory().getID())) {
                    instance = getDefaultJobManagerFactory();
                } else {
                    instance =
                            (NodeExecutionJobManagerFactory)elem
                                    .createExecutableExtension(EXT_POINT_ATTR_JOBMGR);
                }
            } catch (UnsatisfiedLinkError ule) {
                // in case an implementation tries to load an external lib
                // when the factory class gets loaded
                LOGGER.error("Unable to load a library required for '" + jobMgr
                        + "'");
                LOGGER.error("Either specify it in the -Djava.library.path "
                        + "option at the program's command line, or");
                LOGGER.error("include it in the LD_LIBRARY_PATH variable.");
                LOGGER.error("Extension " + jobMgr + " ('" + decl
                        + "') ignored.", ule);
            } catch (Throwable t) {
                LOGGER.error("Problems during initialization of "
                        + "job manager (with id '" + jobMgr + "'.)");
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.", t);
                }
            }

            if (instance != null) {
                /*
                 * make sure the ThreadedJobManagerFactory is always the first
                 * in the list
                 */
                if ((instance instanceof ThreadPool)
                        && managerFactories.size() > 0) {
                    Map<String, NodeExecutionJobManagerFactory> old =
                            managerFactories;
                    managerFactories =
                            new LinkedHashMap<String, NodeExecutionJobManagerFactory>();
                    managerFactories.put(instance.getID(), instance);
                    for (Map.Entry<String, NodeExecutionJobManagerFactory> e : old
                            .entrySet()) {
                        managerFactories.put(e.getKey(), e.getValue());
                    }
                } else {
                    managerFactories.put(instance.getID(), instance);
                }
            }
        }

    }
}
