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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.exec.ThreadNodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManager;
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
            "org.knime.core.NodeExecutionJobManager";

    private static final String EXT_POINT_ATTR_JOBMGR = "JobManager";

    // stores all managers, mapped to their id.
    private static LinkedHashMap<String, NodeExecutionJobManager> managers =
            null;

    /**
     * It's a utility class. No reason to instantiate it.
     */
    private NodeExecutionJobManagerPool() {
        // hide the constructor.
    }

    /**
     * Returns the job manager with the specified id - or null if it doesn't
     * exists. If the job manager is provided by an extension (plug-in) that is
     * currently not installed this returns null, even though in another KNIME
     * installation it may not.
     *
     * @param id the id of the job manager to return
     * @return the job manager with the specified id, or null if there is none.
     */
    public static NodeExecutionJobManager getJobManager(final String id) {
        if (managers == null) {
            collectJobExecutors();
        }
        return managers.get(id);
    }

    /**
     * There is always at least one job manager available.
     *
     * @return the default job manager
     */
    public static NodeExecutionJobManager getDefaultJobManager() {
        return ThreadNodeExecutionJobManager.INSTANCE;
    }

    /**
     * Returns the number of job managers registered through the extension
     * point. A call to this method may trigger instantiation of all job
     * managers.
     *
     * @return the number of registered job managers
     */
    public static int getNumberOfJobManagers() {
        if (managers == null) {
            collectJobExecutors();
        }
        return managers.size();
    }

    /**
     * Returns all registered {@link NodeExecutionJobManager}s. If this method
     * is called for the first time it starts instantiating all job managers.
     *
     * @return a set with all registered {@link NodeExecutionJobManager}s
     */
    public static Collection<NodeExecutionJobManager> getAllJobManagers() {
        if (managers == null) {
            collectJobExecutors();
        }
        return Collections.unmodifiableCollection(managers.values());
    }

    /**
     * Same as {@link #getAllJobManagers()}, but returns them as array.
     *
     * @return an array with all registered {@link NodeExecutionJobManager}s.
     */
    public static NodeExecutionJobManager[] getAllJobManagersAsArray() {
        // getAllJobManagers() initializes the set if necessary
        Collection<NodeExecutionJobManager> mgrs = getAllJobManagers();
        return mgrs.toArray(new NodeExecutionJobManager[mgrs.size()]);
    }

    private static void collectJobExecutors() {

        managers = new LinkedHashMap<String, NodeExecutionJobManager>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            // let's throw in the default manager - otherwise things fail badly
            managers.put(getDefaultJobManager().getID(),
                    getDefaultJobManager());
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
            NodeExecutionJobManager instance = null;
            try {
                // TODO: THE THREADED MANAGER NEEDS TO BE RE-WRITTEN!
                if (jobMgr.equals(getDefaultJobManager().getID())) {
                    instance = getDefaultJobManager();
                } else {
                    instance =
                            (NodeExecutionJobManager)elem
                                    .createExecutableExtension(EXT_POINT_ATTR_JOBMGR);
                }
            } catch (UnsatisfiedLinkError ule) {
                // in case an implementation tries to load an external lib
                // when the factory class gets loaded
                LOGGER.error("Unable to load a library required for "
                        + "JobExecutor '" + jobMgr + "'");
                LOGGER.error("Either specify it in the -Djava.library.path "
                        + "option at the program's command line, or");
                LOGGER.error("include it in the LD_LIBRARY_PATH variable.");
                LOGGER.error("Extension " + jobMgr + " ('" + decl
                        + "') ignored.", ule);
            } catch (Throwable t) {
                LOGGER.error("Problems during initialization of "
                        + "JobExecutor '" + jobMgr + "'.");
                LOGGER.error("Extension " + decl + " ignored.", t);
            }

            if (instance != null) {
                /*
                 * make sure the ThreadedJobManager is always the first in the
                 * list
                 */
                if ((instance instanceof ThreadPool) && managers.size() > 0) {
                    Map<String, NodeExecutionJobManager> old = managers;
                    managers =
                            new LinkedHashMap<String, NodeExecutionJobManager>();
                    managers.put(instance.getID(), instance);
                    for (Map.Entry<String, NodeExecutionJobManager> e : old
                            .entrySet()) {
                        managers.put(e.getKey(), e.getValue());
                    }
                } else {
                    managers.put(instance.getID(), instance);
                }
            }
        }

    }
}
