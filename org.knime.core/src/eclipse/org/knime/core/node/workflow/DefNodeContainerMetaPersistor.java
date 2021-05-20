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
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeMessageDef;

/**
 *
 * @author hornm
 */
public class DefNodeContainerMetaPersistor implements NodeContainerMetaPersistor {

    private NodeDef m_def;

    public DefNodeContainerMetaPersistor(final NodeDef def) {
        m_def = def;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowLoadHelper getLoadHelper() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferencedFile getNodeContainerDirectory() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNodeIDSuffix() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeIDSuffix(final int nodeIDSuffix) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeAnnotationData getNodeAnnotationData() {
        NodeAnnotationDef annoDef = m_def.getNodeAnnotation();
        NodeAnnotationData annoData = new NodeAnnotationData(annoDef.isDefault());
        if (!annoDef.isDefault()) {
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
     */
    @Override
    public NodeExecutionJobManager getExecutionJobManager() {
        NodeSettingsRO settings = DefToCoreUtil.toNodeSettings(m_def.getNodeExecutionJobManagerSettings());
        try {
            return NodeExecutionJobManagerPool.load(settings);
        } catch (InvalidSettingsException ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeSettingsRO getExecutionJobSettings() {
        // TODO
        return null;
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
    public NodeUIInformation getUIInfo() {
        return DefToCoreUtil.toNodeUIInformation(m_def.getNodeUIInfo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeMessage getNodeMessage() {
        NodeMessageDef messageDef = m_def.getNodeMessage();
        return new NodeMessage(NodeMessage.Type.valueOf(messageDef.getType()), messageDef.getMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeLocks getNodeLocks() {
        return DefToCoreUtil.toNodeLocks(m_def.getNodeLocks());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirtyAfterLoad() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUIInfo(final NodeUIInformation uiInfo) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean load(final NodeSettingsRO settings, final NodeSettingsRO parentSettings,
        final LoadResult loadResult) {
        // TODO Auto-generated method stub
        return false;
    }

}
