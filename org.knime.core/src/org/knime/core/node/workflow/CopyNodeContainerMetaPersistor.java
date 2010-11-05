/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Jun 9, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 *
 * @author wiswedel, University of Konstanz
 */
final class CopyNodeContainerMetaPersistor
implements NodeContainerMetaPersistor {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(CopyNodeContainerMetaPersistor.class);

    private final String m_customName;
    private final String m_customDescription;
    private int m_nodeIDSuffix;
    private final NodeSettingsRO m_jobManagerSettings;
    private final State m_state;
    private final NodeMessage m_nodeMessage;
    private final UIInformation m_uiInformation;
    private final boolean m_isDeletable;
    private final ReferencedFile m_ncDirRef;

    /** Create copy persistor.
     * @param original To copy from
     * @param preserveDeletableFlag Whether to keep the "is-deletable" flags
     *        in the target.
     * @param isUndoableDeleteCommand If to keep the location of the node
     *        directories (important for undo of delete commands, see
     *        {@link WorkflowManager#copy(boolean, WorkflowCopyContent)}
     *        for details.)
     */
    CopyNodeContainerMetaPersistor(final NodeContainer original,
            final boolean preserveDeletableFlag,
            final boolean isUndoableDeleteCommand) {
        m_customName = original.getCustomName();
        m_customDescription = original.getCustomDescription();
        m_nodeIDSuffix = original.getID().getIndex();
        NodeExecutionJobManager orig = original.getJobManager();
        NodeSettings jobMgrSettings = null;
        if (orig != null) {
            jobMgrSettings = new NodeSettings("job_manager_clone_config");
            NodeExecutionJobManagerPool.saveJobManager(orig, jobMgrSettings);
        }
        m_jobManagerSettings = jobMgrSettings;
        switch (original.getState()) {
        case IDLE:
        case UNCONFIGURED_MARKEDFOREXEC:
            m_state = State.IDLE;
            break;
        default:
            m_state = State.CONFIGURED;
        }
        m_nodeMessage = original.getNodeMessage();
        if (original.getUIInformation() != null) {
            m_uiInformation = original.getUIInformation().clone();
        } else {
            m_uiInformation = null;
        }
        m_isDeletable = !preserveDeletableFlag || original.isDeletable();
        if (isUndoableDeleteCommand) {
            m_ncDirRef = original.getNodeContainerDirectory();
        } else {
            m_ncDirRef = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getCustomDescription() {
        return m_customDescription;
    }

    /** {@inheritDoc} */
    @Override
    public String getCustomName() {
        return m_customName;
    }

    /** {@inheritDoc} */
    @Override
    public ReferencedFile getNodeContainerDirectory() {
        return m_ncDirRef;
    }

    /** {@inheritDoc} */
    @Override
    public int getNodeIDSuffix() {
        return m_nodeIDSuffix;
    }

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJobManager getExecutionJobManager() {
        if (m_jobManagerSettings == null) {
            return null;
        }
        try {
            return NodeExecutionJobManagerPool.load(m_jobManagerSettings);
        } catch (InvalidSettingsException ise) {
            LOGGER.error("Cannot clone job manager", ise);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public NodeSettingsRO getExecutionJobSettings() {
        // return null here (non-null value means that the
        // node is still executing, i.e. m_original is executing)
        return null;
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
    public UIInformation getUIInfo() {
        return m_uiInformation;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeletable() {
        return m_isDeletable;
    }

    /** {@inheritDoc} */
    @Override
    public boolean load(final NodeSettingsRO settings,
            final NodeSettingsRO parentSettings, final LoadResult loadResult) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setNodeIDSuffix(final int nodeIDSuffix) {
    }

    /** {@inheritDoc} */
    @Override
    public void setUIInfo(final UIInformation uiInfo) {
    }

}
