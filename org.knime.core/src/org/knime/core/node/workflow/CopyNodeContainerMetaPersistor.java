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
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class CopyNodeContainerMetaPersistor implements
        NodeContainerMetaPersistor {
    
    private final NodeContainer m_original;
    private final boolean m_preserveDeletableFlag;
    
    /**
     * 
     */
    CopyNodeContainerMetaPersistor(final NodeContainer original, 
            final boolean preserveDeletableFlag) {
        m_original = original;
        m_preserveDeletableFlag = preserveDeletableFlag;
    }

    /** {@inheritDoc} */
    @Override
    public String getCustomDescription() {
        return m_original.getCustomDescription();
    }

    /** {@inheritDoc} */
    @Override
    public String getCustomName() {
        return m_original.getCustomName();
    }

    /** {@inheritDoc} */
    @Override
    public ReferencedFile getNodeContainerDirectory() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getNodeIDSuffix() {
        return m_original.getID().getIndex();
    }
    
    /** {@inheritDoc} */
    @Override
    public NodeExecutionJobManager getExecutionJobManager() {
        NodeExecutionJobManager orig = m_original.getJobManager();
        if (orig == null) {
            return null;
        }
        NodeSettings s = new NodeSettings("job_manager_clone_config");
        NodeExecutionJobManagerPool.saveJobManager(orig, s);
        try {
            return NodeExecutionJobManagerPool.load(s);
        } catch (InvalidSettingsException ise) {
            throw new IllegalStateException("Cannot clone job manager", ise);
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
        switch (m_original.getState()) {
        case IDLE:
        case UNCONFIGURED_MARKEDFOREXEC:
            return State.IDLE;
        default:
            return State.CONFIGURED;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public NodeMessage getNodeMessage() {
        return m_original.getNodeMessage();
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getUIInfo() {
        if (m_original.getUIInformation() != null) {
            return m_original.getUIInformation().clone();
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isDeletable() {
        return !m_preserveDeletableFlag || m_original.isDeletable();
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
