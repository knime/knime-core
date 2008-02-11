 /* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.State;

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
    public void setUIInfo(UIInformation uiInfo) {
        m_uiInfo = uiInfo;
    }

    /** {@inheritDoc} */
    public int getNodeIDSuffix() {
        return m_nodeIDSuffix;
    }
    
    /** {@inheritDoc} */
    public void setNodeIDSuffix(int nodeIDSuffix) {
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
    public void load(final NodeSettingsRO settings)
            throws InvalidSettingsException, IOException,
            CanceledExecutionException {
        m_customName = loadCustomName(settings);
        m_customDescription = loadCustomDescription(settings);
        m_state = loadState(settings);
    }
    
    protected String loadCustomName(final NodeSettingsRO settings) {
        if (!settings.containsKey(KEY_CUSTOM_NAME)) {
            return null;
        }
        try {
            return settings.getString(KEY_CUSTOM_NAME);
        } catch (InvalidSettingsException e) {
            LOGGER.warn("Invalid custom name in settings, expected string");
            return null;
        }
    }

    protected String loadCustomDescription(final NodeSettingsRO settings) {
        if (!settings.containsKey(KEY_CUSTOM_DESCRIPTION)) {
            return null;
        }
        try {
            return settings.getString(KEY_CUSTOM_DESCRIPTION);
        } catch (InvalidSettingsException e) {
            LOGGER.warn(
                    "Invalid custom description in settings, expected string");
            return null;
        }
    }

    protected State loadState(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // in 1.x.x the state was not saved in the meta information
        // proper state will be set later on in setState(State)
        return State.IDLE;
    }
    
}
