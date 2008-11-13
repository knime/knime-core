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

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class NodeContainerMetaPersistorVersion1xx implements NodeContainerMetaPersistor {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private String m_customDescription;

    private String m_customName;

    private int m_nodeIDSuffix;

    private UIInformation m_uiInfo;

    private State m_state = State.IDLE;
    
    private NodeMessage m_nodeMessage;
    
    private boolean m_isDeletable = true;
    
    private boolean m_isDirtyAfterLoad;
    
    private final ReferencedFile m_nodeContainerDirectory;
    
    /** @param baseDir The node container directory (only important while load)
     */
    NodeContainerMetaPersistorVersion1xx(final ReferencedFile baseDir) {
        m_nodeContainerDirectory = baseDir;
    }
    
    protected NodeLogger getLogger() {
        return m_logger;
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
    
    /** {@inheritDoc} */
    @Override
    public NodeMessage getNodeMessage() {
        return m_nodeMessage;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isDeletable() {
        return m_isDeletable;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }
    
    /** Mark node as dirty. */
    protected void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }
    
    /** {@inheritDoc} */
    public ReferencedFile getNodeContainerDirectory() {
        return m_nodeContainerDirectory;
    }

   /** {@inheritDoc} */
    public LoadResult load(final NodeSettingsRO settings, 
            final NodeSettingsRO parentSettings) {
        LoadResult loadResult = new LoadResult();
        try {
            m_customName = loadCustomName(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error = "Invalid custom name in settings: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            m_customName = null;
        }
        try {
            m_customDescription = 
                loadCustomDescription(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error = 
                "Invalid custom description in settings: " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            m_customDescription = null;
        }
        try {
            m_state = loadState(settings, parentSettings);
        } catch (InvalidSettingsException e) {
            String error = "Can't restore node's state, fallback to " 
                + State.IDLE + ": " + e.getMessage();
            loadResult.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            m_state = State.IDLE;
        }
        try {
            m_nodeMessage = loadNodeMessage(settings);
        } catch (InvalidSettingsException ise) {
            String e = "Unable to load node message: " + ise.getMessage();
            loadResult.addError(e);
            getLogger().warn(e, ise);
        }
        m_isDeletable = loadIsDeletable(settings);
        return loadResult;
    }
    
    /** Read the custom name.
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The custom name or null
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected String loadCustomName(final NodeSettingsRO settings, 
            final NodeSettingsRO parentSettings) 
        throws InvalidSettingsException {
        if (!parentSettings.containsKey(KEY_CUSTOM_NAME)) {
            return null;
        }
        return parentSettings.getString(KEY_CUSTOM_NAME);
    }

    /** Read the custom description.
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The custom name or null
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected String loadCustomDescription(final NodeSettingsRO settings, 
            final NodeSettingsRO parentSettings)
        throws InvalidSettingsException {
        if (!parentSettings.containsKey(KEY_CUSTOM_DESCRIPTION)) {
            return null;
        }
        return parentSettings.getString(KEY_CUSTOM_DESCRIPTION);
    }

    /**
     * Load the state of the node.
     * @param settings The settings associated with the node (used in 2.0+)
     * @param parentSettings The parent settings (workflow.knime, used in 1.3x)
     * @return The state
     * @throws InvalidSettingsException In case of errors reading the argument
     */
    protected State loadState(final NodeSettingsRO settings, 
            final NodeSettingsRO parentSettings)
            throws InvalidSettingsException {
        boolean isOldAutoExecutable = false;
        if (parentSettings.containsKey("factory")) {
            String factory = parentSettings.getString("factory");
            int dotLocation = factory.lastIndexOf('.');
            String simpleName = factory;
            if (dotLocation >= 0 && factory.length() > dotLocation + 1) {
                simpleName = factory.substring(dotLocation + 1);
            }
            isOldAutoExecutable = NodePersistorVersion1xx.
                OLD_AUTOEXECUTABLE_NODEFACTORIES.contains(simpleName);
        }
        boolean isExecuted = parentSettings.getBoolean("isExecuted");
        boolean isConfigured = parentSettings.getBoolean("isConfigured");
        if (isExecuted && !isOldAutoExecutable) {
            return State.EXECUTED;
        } else if (isConfigured) {
            return State.CONFIGURED;
        } else {
            return State.IDLE;
        }
    }
    
    protected NodeMessage loadNodeMessage(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return null;
    }


    
    protected boolean loadIsDeletable(final NodeSettingsRO settings) {
        return true;
    }
}
