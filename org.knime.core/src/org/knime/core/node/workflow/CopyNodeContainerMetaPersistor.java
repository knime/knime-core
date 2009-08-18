/*
 * ------------------------------------------------------------------ *
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
    
    /**
     * 
     */
    CopyNodeContainerMetaPersistor(final NodeContainer original, 
            final boolean preserveDeletableFlag) {
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
        return null;
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
