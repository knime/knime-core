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
 *   Sep 19, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

interface NodeContainerMetaPersistor {

    /** Key for this node's user description. */
    static final String KEY_CUSTOM_DESCRIPTION = "customDescription";

    /** Key for this node's user name. */
    static final String KEY_CUSTOM_NAME = "customName";

    ReferencedFile getNodeContainerDirectory();
    
    int getNodeIDSuffix();

    void setNodeIDSuffix(final int nodeIDSuffix);
    
    String getCustomName();

    String getCustomDescription();

    State getState();

    UIInformation getUIInfo();
    
    NodeMessage getNodeMessage();
    
    boolean isDeletable();
    
    boolean isDirtyAfterLoad();
    
    void setUIInfo(final UIInformation uiInfo);
    
    /** Load content, gets both the current settings (first argument) and
     * the "parent settings", which are only used in 1.3.x flows and will be
     * ignored in any version after that.
     * @param settings The settings object that is usually read from
     * @param parentSettings The parent settings, mostly ignored. 
     * @return The load result representing the load task.
     */
    LoadResult load(final NodeSettingsRO settings, 
            final NodeSettingsRO parentSettings);
    
}