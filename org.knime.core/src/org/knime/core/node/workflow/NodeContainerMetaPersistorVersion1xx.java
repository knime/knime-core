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
 * --------------------------------------------------------------------- *
 *
 * History
 *   Sep 20, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

/**
 *
 * @author wiswedel, University of Konstanz
 */
class NodeContainerMetaPersistorVersion1xx implements NodeContainerMetaPersistor {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final WorkflowLoadHelper m_loadHelper;

    private String m_customName;

    private NodeAnnotationData m_nodeAnnotationData;

    private int m_nodeIDSuffix;

    private NodeExecutionJobManager m_jobManager;

    private NodeSettingsRO m_executionJobSettings;

    private NodeUIInformation m_uiInfo;

    private State m_state = State.IDLE;

    private NodeMessage m_nodeMessage;

    private boolean m_isDeletable = true;

    private boolean m_isDirtyAfterLoad;

    private final ReferencedFile m_nodeSettingsFile;

    private final LoadVersion m_loadVersion;

    /** @param settingsFile The settings file associated with this node.
     * @param loadHelper The load helper to query for additional information.
     * @param version The load version, not null.
     */
    NodeContainerMetaPersistorVersion1xx(final ReferencedFile settingsFile,
            final WorkflowLoadHelper loadHelper, final LoadVersion version) {
        m_nodeSettingsFile = settingsFile;
        // the root folder is usually locked during load, one exception
        // is the loading from templates in the node repository (X-Val, e.g.)
        if (!settingsFile.isRootFileLockedForVM()) {
            getLogger().debug("Workflow being loaded (\""
                    + settingsFile.getParent().getFile().getName()
                    + "\") is not locked");
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
    public State getState() {
        return m_state;
    }

    /** {@inheritDoc} */
    @Override
    public NodeMessage getNodeMessage() {
        return m_nodeMessage;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeletable() {
        return m_isDeletable;
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
    public boolean load(final NodeSettingsRO settings,
            final NodeSettingsRO parentSettings, final LoadResult loadResult) {
        boolean isResetRequired = false;

        try {
            m_nodeAnnotationData =
                loadNodeAnnotationData(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error =
                "Can't load node annotation: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            m_nodeAnnotationData = null;
        }
        try {
            m_jobManager = loadNodeExecutionJobManager(settings);
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node execution job manager: "
                + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            isResetRequired = true;
            setDirtyAfterLoad();
        }
        boolean hasJobManagerLoadFailed = m_jobManager == null;
        try {
            if (!hasJobManagerLoadFailed) {
                m_executionJobSettings =
                    loadNodeExecutionJobSettings(settings);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node execution job manager: "
                + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            isResetRequired = true;
            hasJobManagerLoadFailed = true;
        }
        try {
            if (!hasJobManagerLoadFailed) {
                ReferencedFile jobManagerInternalsDirectory =
                    loadJobManagerInternalsDirectory(
                            getNodeContainerDirectory(), settings);
                if (jobManagerInternalsDirectory != null) {
                    m_jobManager.loadInternals(jobManagerInternalsDirectory);
                }
            }
        } catch (Throwable e) {
            String error = "Can't restore node execution job "
                + "manager internals directory " + e.getMessage();
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
                    m_state = NodeContainer.State.CONFIGURED;
                }
                break;
            default:
                // leave it as it is
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node's state, fallback to "
                + State.IDLE + ": " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            isResetRequired = true;
            m_state = State.IDLE;
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
        m_isDeletable = loadIsDeletable(settings);
        return isResetRequired;
    }

    /** Read the custom description.
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The custom name or null
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected NodeAnnotationData loadNodeAnnotationData(
            final NodeSettingsRO settings, final NodeSettingsRO parentSettings)
        throws InvalidSettingsException {
        String customName = parentSettings.getString(KEY_CUSTOM_NAME, null);

        String customDescr =
            parentSettings.getString(KEY_CUSTOM_DESCRIPTION, null);

        return NodeAnnotationData.createFromObsoleteCustomDescription(
                customName, customDescr);
    }

    /** Load the execution manager responsible for this node. This methods
     * is overwritten in the persistor reading the 2.0+ workflows.
     * @param settings To load from.
     * @return null (only this implementation).
     * @throws InvalidSettingsException If that fails.
     */
    protected NodeExecutionJobManager loadNodeExecutionJobManager(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }

    /** Load the settings representing the pending execution of this node.
     * Returns null if this node was not saved as being executing.
     * @param settings To load from.
     * @return The execution job.
     * @throws InvalidSettingsException If that fails.
     */
    protected NodeSettingsRO loadNodeExecutionJobSettings(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }

    /** Load the directory name that is used to persist internals of the
     * associated job manager. The default (local) job manager typically does
     * not save any internals, but others (e.g. the grid executor) save
     * the logs of their remote jobs.
     * @param parentDir The parent directory (the node dir).
     * @param settings To load from.
     * @return The file location containing the internals or null.
     * @throws InvalidSettingsException If errors occur.
     */
    protected ReferencedFile loadJobManagerInternalsDirectory(
            final ReferencedFile parentDir, final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return null;
    }

    /**
     * Load the state of the node.
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The state
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected State loadState(final NodeSettingsRO settings,
            final NodeSettingsRO parentSettings)
            throws InvalidSettingsException {
        boolean isOldAutoExecutable = false;
        if (parentSettings.containsKey("factory")) {
            String factory = parentSettings.getString("factory");
            int dotLocation = factory.lastIndexOf('.');
            String simpleName = factory;
            if (dotLocation >= 0 && factory.length() > dotLocation + 1) {
                simpleName = factory.substring(dotLocation + 1);
            }
            isOldAutoExecutable = NodePersistorVersion1xx.
                OLD_AUTOEXECUTABLE_NODEFACTORIES.contains(simpleName);
        }
        boolean isExecuted = parentSettings.getBoolean("isExecuted");
        boolean isConfigured = parentSettings.getBoolean("isConfigured");
        if (isExecuted && !isOldAutoExecutable) {
            return State.EXECUTED;
        } else if (isConfigured) {
            return State.CONFIGURED;
        } else {
            return State.IDLE;
        }
    }

    /** Load messages that were set on the node.
     * @param settings to load from.
     * @return null in this class, sub-classes overwrite this method.
     * @throws InvalidSettingsException If this fails.
     */
    protected NodeMessage loadNodeMessage(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return null;
    }

    /** Load whether this node is deletable.
     * @param settings to load from
     * @return true in this implementation, sub-classes overwrite this behavior
     */
    protected boolean loadIsDeletable(final NodeSettingsRO settings) {
        return true;
    }
}
