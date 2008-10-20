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
 *   Sep 18, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class SingleNodeContainerPersistorVersion1xx 
    implements SingleNodeContainerPersistor {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private Node m_node;
    
    private NodeContainerMetaPersistorVersion1xx m_metaPersistor;
    private final WorkflowPersistorVersion1xx m_wfmPersistor;
    
    private NodeSettingsRO m_nodeSettings;
    private NodeSettingsRO m_sncSettings; 
    private ReferencedFile m_nodeDir;
    private boolean m_needsResetAfterLoad;
    private boolean m_isDirtyAfterLoad;
    private List<ScopeObject> m_scopeObjects;
    private LoadNodeModelSettingsFailPolicy m_settingsFailPolicy;
    
    SingleNodeContainerPersistorVersion1xx(
            WorkflowPersistorVersion1xx workflowPersistor) {
        m_wfmPersistor = workflowPersistor;
    }
    
    protected NodeLogger getLogger() {
        return m_logger;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }
    
    /** Indicate that node should be reset after load (due to load problems). */
    public void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }
    
    /** Mark as dirty. */
    protected void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }
    
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /** {@inheritDoc} */
    public Node getNode() {
        return m_node;
    }
    
    /** {@inheritDoc} */
    @Override
    public NodeSettingsRO getSNCSettings() {
        return m_sncSettings;
    }
    
    /** {@inheritDoc} */
    public List<ScopeObject> getScopeObjects() {
        return m_scopeObjects;
    }
    
    /** {@inheritDoc} */
    public SingleNodeContainer getNodeContainer(
            final WorkflowManager wm, final NodeID id) {
        return new SingleNodeContainer(wm, id, this);
    }
    
    protected NodePersistorVersion1xx createNodePersistor() {
        return new NodePersistorVersion1xx(this);
    }

    /** {@inheritDoc} */
    @Override
    public LoadResult preLoadNodeContainer(final ReferencedFile settingsFileRef,
            final NodeSettingsRO parentSettings) 
    throws InvalidSettingsException, IOException {
        LoadResult result = new LoadResult();
        File settingsFile = settingsFileRef.getFile();
        String error;
        if (!settingsFile.isFile()) {
            setDirtyAfterLoad();
            throw new IOException("Can't read node file \"" 
                    + settingsFile.getAbsolutePath() + "\"");
        }
        NodeSettingsRO settings;
        try {
            settings = NodeSettings.loadFromXML(
                    new BufferedInputStream(new FileInputStream(settingsFile)));
        } catch (IOException ioe) {
            setDirtyAfterLoad();
            throw ioe;
        }
        String nodeFactoryClassName;
        try {
            nodeFactoryClassName = loadNodeFactoryClassName(
                    parentSettings, settings);
        } catch (InvalidSettingsException e) {
            if (settingsFile.getName().equals(
                    WorkflowPersistor.WORKFLOW_FILE)) {
                error = "Can't load meta flows in this version";
            } else {
                error = "Can't load node factory class name";
            }
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        NodeFactory<NodeModel> nodeFactory;
        
        try {
            nodeFactory = loadNodeFactory(nodeFactoryClassName);
        } catch (Throwable e) {
            error =  "Unable to load factory class \"" 
                + nodeFactoryClassName + "\"";
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        m_node = new Node(nodeFactory);
        m_metaPersistor = createNodeContainerMetaPersistor(
                settingsFileRef.getParent());
        LoadResult metaResult = m_metaPersistor.load(settings);
        result.addError(metaResult);
        m_nodeSettings = settings;
        m_nodeDir = settingsFileRef.getParent();
        if (result.hasErrors() || m_metaPersistor.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    public LoadResult loadNodeContainer(
            final Map<Integer, BufferedDataTable> tblRep, 
            final ExecutionMonitor exec) throws InvalidSettingsException, 
            CanceledExecutionException, IOException {
        LoadResult result = new LoadResult();
        
        try {
            m_sncSettings = loadSNCSettings(m_nodeSettings);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load SNC settings: " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            return result;
        }
        String nodeFileName;
        try {
            nodeFileName = loadNodeFile(m_nodeSettings);
        } catch (InvalidSettingsException e) {
            String error = 
                "Unable to load node settings file for node with ID suffix "
                    + m_metaPersistor.getNodeIDSuffix() + " (node \""
                    + m_node.getName() + "\"): " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            return result;
        }
        ReferencedFile nodeFile = new ReferencedFile(m_nodeDir, nodeFileName);
        m_settingsFailPolicy = 
            translateToFailPolicy(m_metaPersistor.getState());
        NodePersistorVersion1xx nodePersistor = createNodePersistor();
        try {
            HashMap<Integer, ContainerTable> globalTableRepository =
                getWorkflowManagerPersistor().getGlobalTableRepository();
            LoadResult nodeLoadResult = nodePersistor.load(
                    m_node, nodeFile, exec, tblRep, globalTableRepository);
            result.addError(nodeLoadResult);
        } catch (final Exception e) {
            String error = "Error loading node content: " + e.getMessage();
            getLogger().warn(error, e);
            needsResetAfterLoad();
            result.addError(error);
        }
        try {
            m_scopeObjects = loadScopeObjects(m_nodeSettings);
        } catch (InvalidSettingsException e) {
            m_scopeObjects = Collections.emptyList();
            String error = "Error loading scope objects (flow variables): "
                + e.getMessage();
            getLogger().warn(error, e);
            result.addError(error);
            needsResetAfterLoad();
        }
        if (nodePersistor.needsResetAfterLoad()) {
            setNeedsResetAfterLoad();
        }
        loadNodeStateIntoMetaPersistor(nodePersistor);
        exec.setProgress(1.0);
        if (result.hasErrors()) {
            setDirtyAfterLoad();
        }
        return result;
    }
    
    protected NodeContainerMetaPersistorVersion1xx 
        createNodeContainerMetaPersistor(final ReferencedFile baseDir) {
        return new NodeContainerMetaPersistorVersion1xx(baseDir);
    }
    
    protected void loadNodeStateIntoMetaPersistor(
            final NodePersistorVersion1xx nodePersistor) {
        State nodeState;
        if (nodePersistor.isExecuted()) {
            nodeState = State.EXECUTED;
        } else if (nodePersistor.isConfigured()) {
            nodeState = State.CONFIGURED;
        } else {
            nodeState = State.IDLE;
        }
        m_metaPersistor.setState(nodeState);
    }
    
    protected String loadNodeFactoryClassName(
            final NodeSettingsRO parentSettings, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return parentSettings.getString(KEY_FACTORY_NAME);
    }

    @SuppressWarnings("unchecked")
    protected NodeFactory<NodeModel> loadNodeFactory(
            final String factoryClassName) throws InvalidSettingsException,
            InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        // use global Class Creator utility for Eclipse "compatibility"
        try {
            NodeFactory<NodeModel> f = (NodeFactory<NodeModel>)((GlobalClassCreator
                    .createClass(factoryClassName)).newInstance());
            return f;
        } catch (ClassNotFoundException ex) {
            String[] x = factoryClassName.split("\\.");
            String simpleClassName = x[x.length - 1];

            for (String s : NodeFactory.getLoadedNodeFactories()) {
                if (s.endsWith("." + simpleClassName)) {
                    NodeFactory<NodeModel> f = (NodeFactory<NodeModel>)((GlobalClassCreator
                            .createClass(s)).newInstance());
                    getLogger().warn("Substituted '" + f.getClass().getName()
                            + "' for unknown factory '" + factoryClassName
                            + "'");
                    return f;
                }
            }
            throw ex;
        }
    }
    
    protected String loadNodeFile(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return SETTINGS_FILE_NAME;
    }
    
    protected NodeSettingsRO loadSNCSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return settings;
    }
    
    protected List<ScopeObject> loadScopeObjects(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return Collections.emptyList();
    }
    
    protected boolean shouldFixModelPortOrder() {
        return true;
    }
    
    WorkflowPersistorVersion1xx getWorkflowManagerPersistor() {
        return m_wfmPersistor;
    }
    
    public boolean mustWarnOnDataLoadError() {
        return getWorkflowManagerPersistor().mustWarnOnDataLoadError();
    }
    
    /**
     * @return the settingsFailPolicy
     */
    public LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy() {
        return m_settingsFailPolicy;
    }
    
    static final LoadNodeModelSettingsFailPolicy translateToFailPolicy(
            final State nodeState) { 
        switch (nodeState) {
        case IDLE:
            return LoadNodeModelSettingsFailPolicy.IGNORE;
        case EXECUTED:
            return LoadNodeModelSettingsFailPolicy.WARN;
        default:
            return LoadNodeModelSettingsFailPolicy.FAIL;
        }
    }
}
