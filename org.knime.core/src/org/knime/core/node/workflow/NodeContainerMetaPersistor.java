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
 *   Sep 19, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.State;

interface NodeContainerMetaPersistor {

    /** Key for this node's user description. */
    static final String KEY_CUSTOM_DESCRIPTION = "customDescription";

    /** Key for this node's user name. */
    static final String KEY_CUSTOM_NAME = "customName";

    int getNodeIDSuffix();

    void setNodeIDSuffix(final int nodeIDSuffix);
    
    String getCustomName();

    String getCustomDescription();

    State getState();

    UIInformation getUIInfo();
    
    void setUIInfo(final UIInformation uiInfo);
    
    void load(final NodeSettingsRO settings)
            throws InvalidSettingsException, IOException,
            CanceledExecutionException;
}