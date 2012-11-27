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
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.exec.ThreadNodeExecutionJobManagerFactory;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;
import org.knime.core.node.workflow.WorkflowManager;
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

    /** Get the common settings for a set of job managers.
     * Used from {@link WorkflowManager#getCommonSettings(org.knime.core.node.workflow.NodeID...)}.
     * @param jobManagers ...
     * @return ...
     * @since 2.7
     */
    public static NodeContainerSettings merge(final NodeExecutionJobManager[] jobManagers) {
        String factoryID = null;
        NodeSettings mgrSettings = null;
        boolean isFirst = true;
        for (NodeExecutionJobManager jobManager : jobManagers) {
            String curFactoryID;
            NodeSettings curMgrSettings;
            if (jobManager == null) {
                curFactoryID = null;
                curMgrSettings = null;
            } else {
                curFactoryID = jobManager.getID();
                NodeSettings temp = new NodeSettings(CFG_JOB_MANAGER_SETTINGS);
                jobManager.save(temp);
                curMgrSettings = temp;
            }
            if (isFirst) {
                isFirst = false;
                factoryID = curFactoryID;
                mgrSettings = curMgrSettings;
            } else if (ConvenienceMethods.areEqual(factoryID, curFactoryID)) {
                if (!ConvenienceMethods.areEqual(mgrSettings, curMgrSettings)) {
                    mgrSettings = null;
                }
            } else {
                // different job managers
                curFactoryID = null; // unassigned
            }
        }
        if (factoryID == null) {
            return null;
        }
        NodeExecutionJobManagerFactory jobManagerFactory = getJobManagerFactory(factoryID);
        assert jobManagerFactory != null : "Factory ID " + factoryID + " unknown although job manager present";
        NodeExecutionJobManager instance = jobManagerFactory.getInstance();
        if (mgrSettings != null) {
            try {
                instance.load(mgrSettings);
            } catch (InvalidSettingsException e) {
                LOGGER.error("Settings could not be applied to job manager although "
                        + "they retrieved from another identical instance.", e);
            }
        }
        NodeContainerSettings result = new NodeContainerSettings();
        result.setJobManager(instance);
        return result;
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
                        + "job manager (with id '" + jobMgr + "'.)", t);
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.");
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
