/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 20, 2021 (hornm): created
 */
package org.knime.core.node.workflow;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.def.DefToCoreUtil;
import org.knime.core.workflow.def.JobManagerDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeMessageDef;

/**
 * TODO Do we need caching like in the FileNodeContainerMetaPersistor, which has fields like m_jobManager,
 * m_customDescription, etc.?
 *
 * TODO get rid of this
 *
 * @author hornm
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public class DefNodeContainerMetaPersistor implements NodeContainerMetaPersistor {

    private NodeDef m_def;
    private final WorkflowLoadHelper m_loadHelper;
    private int m_nodeIDSuffix;
    private NodeSettingsRO m_executionJobSettings;
    private boolean m_isDirtyAfterLoad;

    public DefNodeContainerMetaPersistor(final NodeDef def, final WorkflowLoadHelper loadHelper) {
        m_def = def;
        m_loadHelper = loadHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowLoadHelper getLoadHelper() {
        return m_loadHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferencedFile getNodeContainerDirectory() {
        return null;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeAnnotationData getNodeAnnotationData() {
        NodeAnnotationDef annoDef = m_def.getAnnotation();
        NodeAnnotationData annoData = new NodeAnnotationData(annoDef.isAnnotationDefault());
        if (!annoDef.isAnnotationDefault()) {
            DefToCoreUtil.toAnnotationData(annoData, annoDef.getData());
            annoData.copyFrom(annoData, true);
        }
        return annoData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCustomDescription() {
        return m_def.getCustomDescription();
    }

    /**
     * {@inheritDoc}
     * @throws InvalidSettingsException
     */
    @Override
    public NodeExecutionJobManager getExecutionJobManager() {

        JobManagerDef manager = m_def.getJobManager();

        // if no job manager settings are present, the default manager is used, indicated by null
        if(manager == null) {
            return null;
        }

        NodeSettingsRO managerSettings = DefToCoreUtil.toNodeSettings(manager.getSettings());
        return NodeExecutionJobManagerPool.load(manager.getFactory(), managerSettings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeSettingsRO getExecutionJobSettings() {
        // TODO initialize m_executionJobSettings/return non-null if executing remotely
        // e.g., in WorkflowManager#postLoad, we check this using
        // boolean remoteExec = persistor.getMetaPersistor().getExecutionJobSettings() != null
        // FileNodeContainerMetaPersistor#saveNodeExecutionJobReconnectInfo (look for CFG_JOB_CONFIG) doesn't seem to do much?
        return null;
//        return m_executionJobSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalNodeContainerState getState() {
        return InternalNodeContainerState.valueOf(m_def.getState());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeMessage getNodeMessage() {
        NodeMessageDef messageDef = m_def.getMessage();
        return new NodeMessage(NodeMessage.Type.valueOf(messageDef.getType()), messageDef.getMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeLocks getNodeLocks() {
        return DefToCoreUtil.toNodeLocks(m_def.getLocks());
    }

    /** Mark node as dirty. */
    protected void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }
    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    public NodeUIInformation getUIInfo() {
        return DefToCoreUtil.toNodeUIInformation(m_def.getUiInfo());
    }

    /** {@inheritDoc} */
    @Override
    public void setUIInfo(final NodeUIInformation uiInfo) {
        // TODO is this obsolete? required by the interface anyways
        //m_uiInfo = uiInfo;
        throw new IllegalStateException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean load(final NodeSettingsRO settings, final NodeSettingsRO parentSettings,
        final LoadResult loadResult) {
        // TODO where is setUIInfo usually called? Think this is obsolete because we should have everything in the def already
        throw new IllegalStateException("Not implemented");
//        return false;
    }

}
