/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   11.09.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface WorkflowPersistor extends NodeContainerPersistor {

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    public static final String KEY_UI_INFORMATION = "extraInfoClassName";

    /** Links the node settings file name. */
    static final String KEY_NODE_SETTINGS_FILE = "node_settings_file";

    /** Key for this node's internal ID. */
    static final String KEY_ID = "id";
    
    /** Identifier for KNIME workflows when saved to disc. */
    public static final String WORKFLOW_FILE = "workflow.knime";
    
    /** File used to signal that workflow was saved in usual manner. It will
     * always be present in the workflow directory unless the workflow is 
     * exported with the "exclude data" flag being set. */
    public static final String SAVED_WITH_DATA_FILE = ".savedWithData";

    String getLoadVersion();

    Map<Integer, NodeContainerPersistor> getNodeLoaderMap();

    Set<ConnectionContainerTemplate> getConnectionSet();
    
    HashMap<Integer, ContainerTable> getGlobalTableRepository();
    
    String getName();
    
    WorkflowPortTemplate[] getInPortTemplates();
    
    WorkflowPortTemplate[] getOutPortTemplates();
    
    /** Get UI information for workflow input ports.
     * @return the ui info or null if not set.
     */
    public UIInformation getInPortsBarUIInfo();
    
    /** Get UI information for workflow output ports.
     * @return the ui info or null if not set.
     */
    public UIInformation getOutPortsBarUIInfo();
    
    static class ConnectionContainerTemplate {
        private final int m_sourceSuffix;
        private final int m_sourcePort;
        private final int m_destSuffix;
        // not final, may be fixed later (puzzling port IDs in 1.x.x)
        private int m_destPort;
        private final boolean m_isDeletable;
        private final UIInformation m_uiInfo;
        
        ConnectionContainerTemplate(final int source, final int sourcePort, 
                final int dest, final int destPort, final boolean isDeletable,
                final UIInformation uiInfo) {
            m_sourceSuffix = source;
            m_sourcePort = sourcePort;
            m_destSuffix = dest;
            m_destPort = destPort;
            m_isDeletable = isDeletable;
            m_uiInfo = uiInfo;
        }
        
        /** Copies an existing connection (used for copy&paste).
         * @param original To copy. */
        ConnectionContainerTemplate(final ConnectionContainer original) {
            m_sourcePort = original.getSourcePort();
            m_destPort = original.getDestPort();
            switch (original.getType()) {
            case STD:
                m_sourceSuffix = original.getSource().getIndex();
                m_destSuffix = original.getDest().getIndex();
                break;
            case WFMIN:
                m_sourceSuffix = -1;
                m_destSuffix = original.getDest().getIndex();
                break;
            case WFMOUT:
                m_sourceSuffix = original.getSource().getIndex();
                m_destSuffix = -1;
                break;
            case WFMTHROUGH:
                m_sourceSuffix = -1;
                m_destSuffix = -1;
                break;
            default:
                throw new InternalError("Unknown type " + original.getType());
            }
            m_isDeletable = original.isDeletable();
            UIInformation origUIInfo = original.getUIInfo();
            m_uiInfo = origUIInfo == null ? null : origUIInfo.clone();
        }

        /** @return the source identifier */
        int getSourceSuffix() {
            return m_sourceSuffix;
        }

        /** @return the source port */
        int getSourcePort() {
            return m_sourcePort;
        }

        /** @return the destination identifier. */
        int getDestSuffix() {
            return m_destSuffix;
        }

        /** @return the destination port */
        int getDestPort() {
            return m_destPort;
        }
        
        /**
         * @return the isDeletable
         */
        boolean isDeletable() {
            return m_isDeletable;
        }
        
        /** @param destPort the destPort to set */
        public void setDestPort(final int destPort) {
            m_destPort = destPort;
        }

        /** @return the uiInfo */
        UIInformation getUiInfo() {
            return m_uiInfo;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "[" + getSourceSuffix() + "(" 
                + getSourcePort() + ") -> " + getDestSuffix() 
                + "( " + getDestPort() + ")]";
        }

    }
    
    static final class WorkflowPortTemplate {
        private final int m_portIndex;
        private final PortType m_portType;
        private String m_portName;
        WorkflowPortTemplate(final int index, final PortType type) {
            m_portIndex = index;
            m_portType = type;
        }
        /** @return the portIndex */
        int getPortIndex() {
            return m_portIndex;
        }
        /** @return the portType */
        PortType getPortType() {
            return m_portType;
        }
        /** @param name the name to set */
        void setPortName(final String name) {
            m_portName = name;
        }
        /** @return the name */
        String getPortName() {
            return m_portName;
        }
    }
    
    public static class LoadResult {
        
        private final StringBuilder m_errors = new StringBuilder();
        private boolean m_hasErrorDuringNonDataLoad = false;
        
        public void addError(final String error) {
            addError(error, false);
        }
        
        public void addError(final String error, 
                final boolean isErrorDuringDataLoad) {
            m_errors.append(error);
            m_errors.append('\n');
            if (!isErrorDuringDataLoad) {
                m_hasErrorDuringNonDataLoad = true;
            }
        }
        
        public void addError(final String parentName, final LoadResult loadResult) {
            m_errors.append(parentName);
            m_errors.append('\n');
            StringTokenizer tokenizer = 
                new StringTokenizer(loadResult.toString(), "\n");
            while (tokenizer.hasMoreTokens()) {
                m_errors.append("  ");
                m_errors.append(tokenizer.nextToken());
                m_errors.append('\n');
            }
            if (loadResult.hasErrorDuringNonDataLoad()) {
                m_hasErrorDuringNonDataLoad = true;
            }
        }
        
        public void addError(final LoadResult loadResult) {
            m_errors.append(loadResult.m_errors);
            if (loadResult.hasErrorDuringNonDataLoad()) {
                m_hasErrorDuringNonDataLoad = true;
            }
        }
        
        public boolean hasErrors() {
            return m_errors.length() != 0;
        }
        
        public String getErrors() {
            return m_errors.toString();
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_errors.toString();
        }
        
        /**
         * @return the hasErrorDuringNonDataLoad
         */
        public boolean hasErrorDuringNonDataLoad() {
            return m_hasErrorDuringNonDataLoad;
        }
        
    }
    
    public static final class WorkflowLoadResult extends LoadResult {
        
        private WorkflowManager m_workflowManager;
        private boolean m_guiMustReportError = false;
        
        /**
         * @param workflowManager the workflowManager to set
         */
        void setWorkflowManager(final WorkflowManager workflowManager) {
            m_workflowManager = workflowManager;
        }
        
        /**
         * @return the workflowManager
         */
        public WorkflowManager getWorkflowManager() {
            return m_workflowManager;
        }
        
        /**
         * @param guiMustReportError the guiMustReportError to set
         */
        void setGUIMustReportError(final boolean guiMustReportError) {
            m_guiMustReportError = guiMustReportError;
        }
        
        /**
         * @return the guiMustReportError
         */
        public boolean getGUIMustReportError() {
            return m_guiMustReportError;
        }
        
    }
    
    
}
