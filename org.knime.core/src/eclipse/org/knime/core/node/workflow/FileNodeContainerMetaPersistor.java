/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;

import org.knime.core.api.node.workflow.NodeAnnotationData;
import org.knime.core.api.node.workflow.NodeUIInformation;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.FileNodePersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.FileUtil;

class FileNodeContainerMetaPersistor implements NodeContainerMetaPersistor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileNodeContainerMetaPersistor.class);

    private static final String CFG_STATE = "state";

    private static final String CFG_IS_DELETABLE = "isDeletable";

    private static final String CFG_HAS_RESET_LOCK = "hasResetLock";

    private static final String CFG_HAS_CONFIGURE_LOCK = "hasConfigureLock";

    private static final String CFG_JOB_MANAGER_CONFIG = "job.manager";

    private static final String CFG_JOB_MANAGER_DIR = "job.manager.dir";

    private static final String CFG_JOB_CONFIG = "execution.job";

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final WorkflowLoadHelper m_loadHelper;

    private String m_customDescription;

    private NodeAnnotationData m_nodeAnnotationData;

    private int m_nodeIDSuffix;

    private NodeExecutionJobManager m_jobManager;

    private NodeSettingsRO m_executionJobSettings;

    private NodeUIInformation m_uiInfo;

    private InternalNodeContainerState m_state = InternalNodeContainerState.IDLE;

    private NodeMessage m_nodeMessage;

    private NodeLocks m_nodeLock = new NodeLocks(false, false, false);

    private boolean m_isDirtyAfterLoad;

    private final ReferencedFile m_nodeSettingsFile;

    private final LoadVersion m_loadVersion;

    /**
     * @param settingsFile The settings file associated with this node.
     * @param loadHelper The load helper to query for additional information.
     * @param version The load version, not null.
     */
    FileNodeContainerMetaPersistor(final ReferencedFile settingsFile, final WorkflowLoadHelper loadHelper,
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

    /** {@inheritDoc} */
    @Override
    public WorkflowLoadHelper getLoadHelper() {
        return m_loadHelper;
    }

    /** @return the loadVersion */
    protected LoadVersion getLoadVersion() {
        return m_loadVersion;
    }

    /** {@inheritDoc} */
    @Override
    public String getCustomDescription() {
        return m_customDescription;
    }

    /** {@inheritDoc} */
    @Override
    public NodeAnnotationData getNodeAnnotationData() {
        return m_nodeAnnotationData;
    }

    /** {@inheritDoc} */
    @Override
    public NodeUIInformation getUIInfo() {
        return m_uiInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void setUIInfo(final NodeUIInformation uiInfo) {
        m_uiInfo = uiInfo;
    }

    /** {@inheritDoc} */
    @Override
    public int getNodeIDSuffix() {
        return m_nodeIDSuffix;
    }

    /** {@inheritDoc} */
    @Override
    public void setNodeIDSuffix(final int nodeIDSuffix) {
        m_nodeIDSuffix = nodeIDSuffix;
    }

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJobManager getExecutionJobManager() {
        return m_jobManager;
    }

    /** {@inheritDoc} */
    @Override
    public NodeSettingsRO getExecutionJobSettings() {
        return m_executionJobSettings;
    }

    /** {@inheritDoc} */
    @Override
    public InternalNodeContainerState getState() {
        return m_state;
    }

    /** {@inheritDoc} */
    @Override
    public NodeMessage getNodeMessage() {
        return m_nodeMessage;
    }

    /** {@inheritDoc} */
    @Override
    public NodeLocks getNodeLocks() {
        return m_nodeLock;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }

    /** Mark node as dirty. */
    protected void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public ReferencedFile getNodeContainerDirectory() {
        return m_nodeSettingsFile.getParent();
    }

    ReferencedFile getNodeSettingsFile() {
        return m_nodeSettingsFile;
    }

    /** {@inheritDoc} */
    @Override
    public boolean
        load(final NodeSettingsRO settings, final NodeSettingsRO parentSettings, final LoadResult loadResult) {
        boolean isResetRequired = false;

        try {
            m_nodeAnnotationData = loadNodeAnnotationData(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error = "Can't load node annotation: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            m_nodeAnnotationData = null;
        }
        try {
            m_customDescription = loadCustomDescription(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error = "Invalid custom description in settings: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            m_customDescription = null;
        }
        try {
            m_jobManager = loadNodeExecutionJobManager(settings);
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node execution job manager: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            isResetRequired = true;
            setDirtyAfterLoad();
        }
        boolean hasJobManagerLoadFailed = m_jobManager == null;
        try {
            if (!hasJobManagerLoadFailed) {
                m_executionJobSettings = loadNodeExecutionJobSettings(settings);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node execution job manager: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
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
            setDirtyAfterLoad();
            hasJobManagerLoadFailed = true;
        }
        try {
            m_state = loadState(settings, parentSettings);
            switch (m_state) {
                case EXECUTED:
                case EXECUTINGREMOTELY:
                    if (getLoadHelper().isTemplateFlow()) {
                        m_state = InternalNodeContainerState.CONFIGURED;
                    }
                    break;
                default:
                    // leave it as it is
            }
        } catch (InvalidSettingsException e) {
            String error =
                "Can't restore node's state, fallback to " + InternalNodeContainerState.IDLE + ": " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            isResetRequired = true;
            m_state = InternalNodeContainerState.IDLE;
        }
        try {
            if (!getLoadHelper().isTemplateFlow()) {
                m_nodeMessage = loadNodeMessage(settings);
            }
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load node message: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
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
            String customName = settings.getString(KEY_CUSTOM_NAME, null);
            return NodeAnnotationData.createFromObsoleteCustomName(customName);
        } else {
            if (settings.containsKey("nodeAnnotation")) {
                NodeSettingsRO anno = settings.getNodeSettings("nodeAnnotation");
                NodeAnnotationData result = NodeAnnotationData.builder()
                    .copyFrom(FileWorkflowPersistor.loadAnnotationData(anno, getLoadVersion()), true)
                    .setIsDefault(false).build();
                return result;
            }
            return NodeAnnotationData.builder().setIsDefault(true).build();
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
            if (!parentSettings.containsKey(KEY_CUSTOM_DESCRIPTION)) {
                return null;
            }
            return parentSettings.getString(KEY_CUSTOM_DESCRIPTION);
        } else {
            // custom description was not saved in v2.5.0 (but again in v2.5.1)
            // see bug 3034
            if (!settings.containsKey(KEY_CUSTOM_DESCRIPTION)) {
                return null;
            }
            return settings.getString(KEY_CUSTOM_DESCRIPTION);
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
            if (getLoadVersion().ordinal() >= FileWorkflowPersistor.LoadVersion.V280.ordinal()) {
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

    public static void save(final NodeSettingsWO settings, final NodeContainer nc, final ReferencedFile targetDir) {
        synchronized (nc.m_nodeMutex) {
            saveNodeAnnotation(settings, nc);
            saveCustomDescription(settings, nc);
            saveNodeExecutionJobManager(settings, nc);
            boolean mustAlsoSaveExecutorSettings = saveState(settings, nc);
            if (mustAlsoSaveExecutorSettings) {
                saveNodeExecutionJob(settings, nc);
            }
            saveJobManagerInternalsDirectory(settings, nc, targetDir);
            saveNodeMessage(settings, nc);
            saveNodeLocks(settings, nc);
        }
    }

    protected static void saveNodeExecutionJobManager(final NodeSettingsWO settings, final NodeContainer nc) {
        NodeExecutionJobManager jobManager = nc.getJobManager();
        if (jobManager != null) {
            NodeSettingsWO s = settings.addNodeSettings(CFG_JOB_MANAGER_CONFIG);
            NodeExecutionJobManagerPool.saveJobManager(jobManager, s);
        }
    }

    protected static void saveNodeExecutionJob(final NodeSettingsWO settings, final NodeContainer nc) {
        assert nc.findJobManager().canDisconnect(nc.getExecutionJob()) : "Execution job can be saved/disconnected";
        nc.saveNodeExecutionJobReconnectInfo(settings.addNodeSettings(CFG_JOB_CONFIG));
    }

    protected static void saveJobManagerInternalsDirectory(final NodeSettingsWO settings, final NodeContainer nc,
        final ReferencedFile targetDir) {
        NodeExecutionJobManager jobManager = nc.getJobManager();
        if (jobManager != null && jobManager.canSaveInternals()) {
            String dirName = "job_manager_internals";
            File dir = new File(targetDir.getFile(), dirName);
            if (dir.exists()) {
                LOGGER.warn("Directory \"" + dir.getAbsolutePath() + "\"" + " already exists; deleting it");
                FileUtil.deleteRecursively(dir);
            }
            if (!dir.mkdirs()) {
                LOGGER.error("Unable to create directory \"" + dir.getAbsolutePath() + "\"");
                return;
            }
            try {
                jobManager.saveInternals(new ReferencedFile(targetDir, dirName));
                settings.addString(CFG_JOB_MANAGER_DIR, dirName);
            } catch (Throwable e) {
                if (!(e instanceof IOException)) {
                    LOGGER.coding("Saving internals of job manager should " + "only throw IOException, caught "
                        + e.getClass().getSimpleName());
                }
                String error = "Saving job manager internals failed: " + e.getMessage();
                LOGGER.error(error, e);
            }
        }
    }

    protected static boolean saveState(final NodeSettingsWO settings, final NodeContainer nc) {
        String state;
        boolean mustAlsoSaveExecutorSettings = false;
        switch (nc.getInternalState()) {
            case IDLE:
            case UNCONFIGURED_MARKEDFOREXEC:
                state = InternalNodeContainerState.IDLE.toString();
                break;
            case EXECUTED:
                state = InternalNodeContainerState.EXECUTED.toString();
                break;
            case EXECUTINGREMOTELY:
                if (nc.findJobManager().canDisconnect(nc.getExecutionJob())) {
                    // state will also be CONFIGURED only ... we set executing later
                    mustAlsoSaveExecutorSettings = true;
                    state = InternalNodeContainerState.EXECUTINGREMOTELY.toString();
                } else {
                    state = InternalNodeContainerState.IDLE.toString();
                }
                break;
            default:
                state = InternalNodeContainerState.CONFIGURED.toString();
        }
        settings.addString(CFG_STATE, state);
        return mustAlsoSaveExecutorSettings;
    }

    protected static void saveNodeAnnotation(final NodeSettingsWO settings, final NodeContainer nc) {
        NodeAnnotation annotation = nc.getNodeAnnotation();
        if (annotation != null && !annotation.getData().isDefault()) {
            NodeSettingsWO anno = settings.addNodeSettings("nodeAnnotation");
            FileWorkflowPersistor.saveAnnotationData(anno, annotation.getData());
        }
    }

    protected static void saveCustomDescription(final NodeSettingsWO settings, final NodeContainer nc) {
        settings.addString(KEY_CUSTOM_DESCRIPTION, nc.getCustomDescription());
    }

    protected static void saveNodeLocks(final NodeSettingsWO settings, final NodeContainer nc) {
        NodeLocks nl = nc.getNodeLocks();
        if(nl.hasDeleteLock()) {
            settings.addBoolean(CFG_IS_DELETABLE, false);
        }
        if(nl.hasResetLock()) {
            settings.addBoolean(CFG_HAS_RESET_LOCK, true);
        }
        if(nl.hasConfigureLock()) {
            settings.addBoolean(CFG_HAS_CONFIGURE_LOCK, true);
        }

    }

    protected static void saveNodeMessage(final NodeSettingsWO settings, final NodeContainer nc) {
        NodeMessage message = nc.getNodeMessage();
        if (message != null && !message.getMessageType().equals(Type.RESET)) {
            NodeSettingsWO sub = settings.addNodeSettings("nodecontainer_message");
            sub.addString("type", message.getMessageType().name());
            sub.addString("message", message.getMessage());
        }
    }

}
