/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.FileNodePersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.LoadVersion;

class FileNodeContainerMetaLoader {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileNodeContainerMetaLoader.class);

    private static final String CFG_STATE = "state";

    private static final String CFG_IS_DELETABLE = "isDeletable";

    private static final String CFG_HAS_RESET_LOCK = "hasResetLock";

    private static final String CFG_HAS_CONFIGURE_LOCK = "hasConfigureLock";

    private static final String CFG_JOB_MANAGER_CONFIG = "job.manager";

    private static final String CFG_JOB_MANAGER_DIR = "job.manager.dir";

    private static final String CFG_JOB_CONFIG = "execution.job";

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final WorkflowLoadHelper m_loadHelper;

    // -- DEF/RES information --

//    private NodeLocks m_nodeLock = new NodeLocks(false, false, false);

    // -- stuff --

    private final ReferencedFile m_nodeSettingsFile;

    private final LoadVersion m_loadVersion;

    /**
     * @param settingsFile The settings file associated with this node.
     * @param loadHelper The load helper to query for additional information.
     * @param version The load version, not null.
     */
    FileNodeContainerMetaLoader(final ReferencedFile settingsFile, final WorkflowLoadHelper loadHelper,
        final LoadVersion version) {
        m_nodeSettingsFile = settingsFile;
        // the root folder is usually locked during load, one exception
        // is the loading from templates in the node repository (X-Val, e.g.)
        if (!settingsFile.isRootFileLockedForVM()) {
            getLogger().debug(
                "Workflow being loaded (\"" + settingsFile.getParent().getFile().getName() + "\") is not locked");
        }
        m_loadHelper = loadHelper;
        m_loadVersion = version;
    }

    protected NodeLogger getLogger() {
        return m_logger;
    }

    public WorkflowLoadHelper getLoadHelper() {
        return m_loadHelper;
    }

    /** @return the loadVersion */
    protected LoadVersion getLoadVersion() {
        return m_loadVersion;
    }

    public ReferencedFile getNodeContainerDirectory() {
        return m_nodeSettingsFile.getParent();
    }

    ReferencedFile getNodeSettingsFile() {
        return m_nodeSettingsFile;
    }

    public boolean
        load(final NodeSettingsRO settings, final NodeSettingsRO parentSettings, final LoadResult loadResult) {
        boolean isResetRequired = false;

        /**
         * TODO extract try(load)-catch pattern
         */
        try {
            m_nodeAnnotationData = loadNodeAnnotationData(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error = "Can't load node annotation: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            m_nodeAnnotationData = null;
        }
        try {
            m_customDescription = loadCustomDescription(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error = "Invalid custom description in settings: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            m_customDescription = null;
        }
        try {
            m_jobManager = loadNodeExecutionJobManager(settings);
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node execution job manager: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            isResetRequired = true;
            loadResult.setDirtyAfterLoad();
        }

        /**
         * Handling absent job manager
         */
        boolean hasJobManagerLoadFailed = m_jobManager == null;

        try {
            if (!hasJobManagerLoadFailed) {
                m_executionJobSettings = loadNodeExecutionJobSettings(settings);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node execution job manager: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            isResetRequired = true;
            hasJobManagerLoadFailed = true;
        }

        try {
            if (!hasJobManagerLoadFailed) {
                ReferencedFile jobManagerInternalsDirectory =
                    loadJobManagerInternalsDirectory(getNodeContainerDirectory(), settings);
                if (jobManagerInternalsDirectory != null) {
                    m_jobManager.loadInternals(jobManagerInternalsDirectory);
                }
            }
        } catch (Throwable e) {
            String error = "Can't restore node execution job " + "manager internals directory " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            // JobManager Load Failed
        }

        m_nodeLock = loadNodeLocks(settings);
        return isResetRequired;
    }

    /**
     * Read the custom description.
     *
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The custom name or null
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected NodeAnnotationData loadNodeAnnotationData(final NodeSettingsRO settings,
        final NodeSettingsRO parentSettings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V250)) {
            String customName = settings.getString(NodeContainerMetaPersistor.KEY_CUSTOM_NAME, null);
            return NodeAnnotationData.createFromObsoleteCustomName(customName);
        } else {
            if (settings.containsKey("nodeAnnotation")) {
                NodeSettingsRO anno = settings.getNodeSettings("nodeAnnotation");
                NodeAnnotationData result = new NodeAnnotationData(false);
                result.load(anno, getLoadVersion());
                return result;
            }
            return new NodeAnnotationData(true);
        }
    }

    /**
     * Read the custom description.
     *
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The custom name or null
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected String loadCustomDescription(final NodeSettingsRO settings, final NodeSettingsRO parentSettings)
        throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            if (!parentSettings.containsKey(NodeContainerMetaPersistor.KEY_CUSTOM_DESCRIPTION)) {
                return null;
            }
            return parentSettings.getString(NodeContainerMetaPersistor.KEY_CUSTOM_DESCRIPTION);
        } else {
            // custom description was not saved in v2.5.0 (but again in v2.5.1)
            // see bug 3034
            if (!settings.containsKey(NodeContainerMetaPersistor.KEY_CUSTOM_DESCRIPTION)) {
                return null;
            }
            return settings.getString(NodeContainerMetaPersistor.KEY_CUSTOM_DESCRIPTION);
        }
    }

    /**
     * Load the execution manager responsible for this node. This methods is overwritten in the persistor reading the
     * 2.0+ workflows.
     *
     * @param settings To load from.
     * @return null (only this implementation).
     * @throws InvalidSettingsException If that fails.
     */
    protected NodeExecutionJobManager loadNodeExecutionJobManager(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        if (!settings.containsKey(CFG_JOB_MANAGER_CONFIG)) {
            return null;
        }
        return NodeExecutionJobManagerPool.load(settings.getNodeSettings(CFG_JOB_MANAGER_CONFIG));

    }

    /**
     * Load the settings representing the pending execution of this node. Returns null if this node was not saved as
     * being executing.
     *
     * @param settings To load from.
     * @return The execution job.
     * @throws InvalidSettingsException If that fails.
     */
    protected NodeSettingsRO loadNodeExecutionJobSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        if (!settings.containsKey(CFG_JOB_CONFIG)) {
            return null;
        }
        return settings.getNodeSettings(CFG_JOB_CONFIG);
    }

    /**
     * Load the directory name that is used to persist internals of the associated job manager. The default (local) job
     * manager typically does not save any internals, but others (e.g. the grid executor) save the logs of their remote
     * jobs.
     *
     * @param parentDir The parent directory (the node dir).
     * @param settings To load from.
     * @return The file location containing the internals or null.
     * @throws InvalidSettingsException If errors occur.
     */
    protected ReferencedFile loadJobManagerInternalsDirectory(final ReferencedFile parentDir,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        if (!settings.containsKey(CFG_JOB_MANAGER_DIR)) {
            return null;
        }
        String dir = settings.getString(CFG_JOB_MANAGER_DIR);
        if (dir == null) {
            throw new InvalidSettingsException("Job manager internals dir is null");
        }
        return new ReferencedFile(parentDir, dir);
    }

    /**
     * Load the state of the node.
     *
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The state
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected InternalNodeContainerState loadState(final NodeSettingsRO settings, final NodeSettingsRO parentSettings)
        throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            boolean isOldAutoExecutable = false;
            if (parentSettings.containsKey("factory")) {
                String factory = parentSettings.getString("factory");
                int dotLocation = factory.lastIndexOf('.');
                String simpleName = factory;
                if (dotLocation >= 0 && factory.length() > dotLocation + 1) {
                    simpleName = factory.substring(dotLocation + 1);
                }
                isOldAutoExecutable = FileNodePersistor.OLD_AUTOEXECUTABLE_NODEFACTORIES.contains(simpleName);
            }
            boolean isExecuted = parentSettings.getBoolean("isExecuted");
            boolean isConfigured = parentSettings.getBoolean("isConfigured");
            if (isExecuted && !isOldAutoExecutable) {
                return InternalNodeContainerState.EXECUTED;
            } else if (isConfigured) {
                return InternalNodeContainerState.CONFIGURED;
            } else {
                return InternalNodeContainerState.IDLE;
            }
        } else {
            String stateString = settings.getString(CFG_STATE);
            if (stateString == null) {
                throw new InvalidSettingsException("State information is null");
            }
            try {
                return InternalNodeContainerState.valueOf(stateString);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException("Unable to parse state \"" + stateString + "\"");
            }
        }
    }

    /**
     * Load messages that were set on the node.
     *
     * @param settings to load from.
     * @return null in this class, sub-classes overwrite this method.
     * @throws InvalidSettingsException If this fails.
     */
    protected NodeMessage loadNodeMessage(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        } else {
            final String key;
            // in 2.8 we merged the "settings.xml" with the "node.xml". Both files contained the node_message,
            // therefore we write the NC message under a new name to allow old KNIME instances (2.7-) to load
            // the node message in the Node class.
            if (getLoadVersion().ordinal() >= LoadVersion.V280.ordinal()) {
                key = "nodecontainer_message";
            } else {
                key = "node_message";
            }
            if (settings.containsKey(key)) {
                NodeSettingsRO sub = settings.getNodeSettings(key);
                String typeS = sub.getString("type");
                if (typeS == null) {
                    throw new InvalidSettingsException("Message type must not be null");
                }
                Type type;
                try {
                    type = Type.valueOf(typeS);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException("Invalid message type: " + typeS, iae);
                }
                String message = sub.getString("message");
                return new NodeMessage(type, message);
            }
            return null;
        }
    }

    /**
     *
     * @param settings settings to load from
     * @return the node locks
     */
    protected NodeLocks loadNodeLocks(final NodeSettingsRO settings) {
        boolean isDeletable;
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            isDeletable = true;
        } else {
            isDeletable = settings.getBoolean(CFG_IS_DELETABLE, true);
        }
        boolean hasResetLock;
        if(getLoadVersion().isOlderThan(LoadVersion.V3010)) {
            hasResetLock = false;
        } else {
            hasResetLock = settings.getBoolean(CFG_HAS_RESET_LOCK, false);
        }
        boolean hasConfigureLock;
        if (getLoadVersion().isOlderThan(LoadVersion.V3010)) {
            hasConfigureLock = false;
        } else {
            hasConfigureLock = settings.getBoolean(CFG_HAS_CONFIGURE_LOCK, false);
        }
        return new NodeLocks(!isDeletable, hasResetLock, hasConfigureLock);
    }

}
