                           /* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Sep 20, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class NodeContainerMetaPersistorVersion1xx implements NodeContainerMetaPersistor {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SingleNodeContainerPersistorVersion1xx.class);

    private String m_customDescription;

    private String m_customName;

    private int m_nodeIDSuffix;

    private UIInformation m_uiInfo;

    private State m_state = State.IDLE;
    
    private final ReferencedFile m_nodeContainerDirectory;
    
    /** @param baseDir The node container directory (only important while load)
     */
    NodeContainerMetaPersistorVersion1xx(final ReferencedFile baseDir) {
        m_nodeContainerDirectory = baseDir;
    }

    /** {@inheritDoc} */
    public String getCustomDescription() {
        return m_customDescription;
    }

    /** {@inheritDoc} */
    public String getCustomName() {
        return m_customName;
    }

    /** {@inheritDoc} */
    public UIInformation getUIInfo() {
        return m_uiInfo;
    }
    
    /** {@inheritDoc} */
    public void setUIInfo(final UIInformation uiInfo) {
        m_uiInfo = uiInfo;
    }

    /** {@inheritDoc} */
    public int getNodeIDSuffix() {
        return m_nodeIDSuffix;
    }
    
    /** {@inheritDoc} */
    public void setNodeIDSuffix(final int nodeIDSuffix) {
        m_nodeIDSuffix = nodeIDSuffix;
    }
    
    /** {@inheritDoc} */
    public State getState() {
        return m_state;
    }
    
    /** Sets the state. This is needed to load workflows written in 1.x.x as
     * the state information was saved in the node itself (not the node 
     * container).
     * @param state The state as loaded from the node and which is to be 
     *        returned in {@link #getState()}
     */
    void setState(final State state) {
        assert state != null : "State must not be null";
        m_state = state;
    }
    
    /** {@inheritDoc} */
    public ReferencedFile getNodeContainerDirectory() {
        return m_nodeContainerDirectory;
    }

   /** {@inheritDoc} */
    public LoadResult load(final NodeSettingsRO settings) throws IOException {
        LoadResult loadResult = new LoadResult();
        try {
            m_customName = loadCustomName(settings);
        } catch (InvalidSettingsException e) {
            String error = "Invalid custom name in settings: " + e.getMessage();
            loadResult.addError(error);
            LOGGER.debug(error, e);
            m_customName = null;
        }
        try {
            m_customDescription = loadCustomDescription(settings);
        } catch (InvalidSettingsException e) {
            String error = 
                "Invalid custom description in settings: " + e.getMessage();
            loadResult.addError(error);
            LOGGER.debug(error, e);
            m_customDescription = null;
        }
        try {
            m_state = loadState(settings);
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node's state, fallback to " 
                + State.IDLE + ": " + e.getMessage();
            loadResult.addError(error);
            LOGGER.debug(error, e);
            m_state = State.IDLE;
        }
        return loadResult;
    }
    
    protected String loadCustomName(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        if (!settings.containsKey(KEY_CUSTOM_NAME)) {
            return null;
        }
        return settings.getString(KEY_CUSTOM_NAME);
    }

    protected String loadCustomDescription(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (!settings.containsKey(KEY_CUSTOM_DESCRIPTION)) {
            return null;
        }
        return settings.getString(KEY_CUSTOM_DESCRIPTION);
    }

    protected State loadState(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // in 1.x.x the state was not saved in the meta information
        // proper state will be set later on in setState(State)
        return State.IDLE;
    }
    
}
