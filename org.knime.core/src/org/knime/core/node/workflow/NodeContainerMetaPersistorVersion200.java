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
 *   Sep 25, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class NodeContainerMetaPersistorVersion200 extends
        NodeContainerMetaPersistorVersion1xx {
    
    private static final String CFG_STATE = "state";
    private static final String CFG_IS_DELETABLE = "isDeletable";
    
    /** @param baseDir The node container directory (only important while load)
     */
    NodeContainerMetaPersistorVersion200(final ReferencedFile baseDir) {
        super(baseDir);
    }

    /** {@inheritDoc} */
    @Override
    protected State loadState(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String stateString = settings.getString(CFG_STATE);
        if (stateString == null) {
            throw new InvalidSettingsException("State information is null");
        }
        try {
            return State.valueOf(stateString);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("Unable to parse state \""
                    + stateString + "\"");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean loadIsDeletable(final NodeSettingsRO settings) {
        return settings.getBoolean(CFG_IS_DELETABLE, true);
    }

    public void save(final NodeContainer nc, final NodeSettingsWO settings)
        throws IOException {
        saveCustomName(settings, nc);
        saveCustomDescription(settings, nc);
        saveState(settings, nc);
        saveIsDeletable(settings, nc);
    }

    protected void saveState(final NodeSettingsWO settings,
            final NodeContainer nc) {
        String state;
        switch (nc.getState()) {
        case IDLE:
        case UNCONFIGURED_MARKEDFOREXEC:
            state = State.IDLE.toString();
            break;
        case EXECUTED:
            state = State.EXECUTED.toString();
            break;
        default:
            state = State.CONFIGURED.toString();
        }
        settings.addString(CFG_STATE, state);
    }
    
    protected void saveCustomName(final NodeSettingsWO settings,
            final NodeContainer nc) {
        settings.addString(KEY_CUSTOM_NAME, nc.getCustomName());
    }

    protected void saveCustomDescription(final NodeSettingsWO settings,
            final NodeContainer nc) {
        settings.addString(KEY_CUSTOM_DESCRIPTION, nc.getCustomDescription());
    }
    
    protected void saveIsDeletable(final NodeSettingsWO settings, 
            final NodeContainer nc) {
        if (!nc.isDeletable()) {
            settings.addBoolean(CFG_IS_DELETABLE, false);
        }
    }

}
