/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   11.09.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface WorkflowPersistor extends NodeContainerPersistor {

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    public static final String KEY_SOURCE_ID = "sourceID";

    public static final String KEY_SOURCE_PORT = "sourcePort";

    public static final String KEY_TARGET_ID = "targetID";

    public static final String KEY_TARGET_PORT = "targetPort";

    public static final String KEY_UI_INFORMATION = "extraInfoClassName";

    /** Links the node settings file name. */
    static final String KEY_NODE_SETTINGS_FILE = "node_settings_file";

    /** Key for this node's internal ID. */
    static final String KEY_ID = "id";
    
    /** Identifier for KNIME workflows when saved to disc. */
    public static final String WORKFLOW_FILE = "workflow.knime";


    Map<Integer, NodeContainerPersistor> getNodeLoaderMap();

    Set<ConnectionContainerTemplate> getConnectionSet();
    
    HashMap<Integer, ContainerTable> getGlobalTableRepository();
    
    String getName();
    
    WorkflowInPort[] getInPorts();
    
    WorkflowOutPort[] getOutPorts();
    
    public void loadWorkflow(NodeSettingsRO settings, 
            final File workflowDirectory, ExecutionMonitor exec, int loadID)
            throws CanceledExecutionException, IOException,
            InvalidSettingsException;
    
    static class ConnectionContainerTemplate {
        private final int m_sourceID;
        private final int m_sourcePort;
        private final int m_targetID;
        private final int m_targetPort;
        private final UIInformation m_uiInfo;
        
        ConnectionContainerTemplate(final int sourceID, final int sourcePort, 
                final int targetID, final int targetPort, 
                final UIInformation uiInfo) {
            m_sourceID = sourceID;
            m_sourcePort = sourcePort;
            m_targetID = targetID;
            m_targetPort = targetPort;
            m_uiInfo = uiInfo;
        }

        /** @return the sourceID */
        int getSourceID() {
            return m_sourceID;
        }

        /** @return the sourcePort */
        int getSourcePort() {
            return m_sourcePort;
        }

        /** @return the targetID */
        int getTargetID() {
            return m_targetID;
        }

        /** @return the targetPort */
        int getTargetPort() {
            return m_targetPort;
        }

        /** @return the uiInfo */
        UIInformation getUiInfo() {
            return m_uiInfo;
        }
    }
}
