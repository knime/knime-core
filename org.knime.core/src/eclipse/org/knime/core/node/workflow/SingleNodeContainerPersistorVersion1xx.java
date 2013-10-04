/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 * History
 *   Sep 18, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.missing.MissingNodeFactory;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowLoopContext.RestoredFlowLoopContext;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.NodeFactoryUnknownException;
import org.knime.core.util.FileUtil;

/**
 *
 * @author wiswedel, University of Konstanz
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class SingleNodeContainerPersistorVersion1xx implements SingleNodeContainerPersistor,
    FromFileNodeContainerPersistor {

    private static final Method REPOS_LOAD_METHOD;
    private static final Object REPOS_MANAGER;
    static {
        Class<?> repManClass;
        try {
            repManClass = Class.forName("org.knime.workbench.repository.RepositoryManager");
            Field instanceField = repManClass.getField("INSTANCE");
            REPOS_MANAGER = instanceField.get(null);
            REPOS_LOAD_METHOD = repManClass.getMethod("getRoot");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final NodeLogger SAVE_LOGGER = NodeLogger.getLogger(SingleNodeContainerPersistorVersion1xx.class);
    public static final String NODE_FILE = "node.xml";

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final WorkflowPersistorVersion1xx.LoadVersion m_version;

    private Node m_node;

    /** Meta persistor, only set when used to load a workflow. */
    private final NodeContainerMetaPersistorVersion1xx m_metaPersistor;

    /** WFM persistor, only set when used to load a workflow. */
    private final WorkflowPersistorVersion1xx m_wfmPersistor;

    private WorkflowPersistor m_parentPersistor;

    private NodeSettingsRO m_nodeSettings;

    private SingleNodeContainerSettings m_sncSettings;

    private boolean m_needsResetAfterLoad;

    private boolean m_isDirtyAfterLoad;

    private List<FlowObject> m_flowObjects;

    private LoadNodeModelSettingsFailPolicy m_settingsFailPolicy;


    /** Load persistor. */
    SingleNodeContainerPersistorVersion1xx(final WorkflowPersistorVersion1xx workflowPersistor,
        final ReferencedFile nodeSettingsFile, final WorkflowLoadHelper loadHelper, final WorkflowPersistorVersion1xx.LoadVersion version) {
        this(workflowPersistor, new NodeContainerMetaPersistorVersion1xx(nodeSettingsFile, loadHelper, version),
            version);
    }

    /**
     * Constructor used internally, not used outside this class or its derivates.
     *
     * @param version
     * @param metaPersistor
     * @param wfmPersistor
     */
    SingleNodeContainerPersistorVersion1xx(final WorkflowPersistorVersion1xx wfmPersistor,
        final NodeContainerMetaPersistorVersion1xx metaPersistor, final WorkflowPersistorVersion1xx.LoadVersion version) {
        if (version == null || wfmPersistor == null) {
            throw new NullPointerException();
        }
        m_version = version;
        m_metaPersistor = metaPersistor;
        m_wfmPersistor = wfmPersistor;
    }

    protected final WorkflowPersistorVersion1xx.LoadVersion getLoadVersion() {
        return m_version;
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

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return true;
    }

    /** Mark as dirty. */
    protected void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }

    @Override
    public NodeContainerMetaPersistorVersion1xx getMetaPersistor() {
        return m_metaPersistor;
    }

    /** @return Directory associated with node.
     * @since 2.8 */
    public ReferencedFile getNodeContainerDirectory() {
        return m_metaPersistor.getNodeContainerDirectory();
    }

    /** Called by {@link Node} to update the message field in the {@link NodeModel} class.
     * @return the msg or null.
     * @since 2.8
     */
    public String getNodeMessage() {
        NodeMessage nodeMessage = getMetaPersistor().getNodeMessage();
        if (nodeMessage != null && !nodeMessage.getMessageType().equals(NodeMessage.Type.RESET)) {
            return nodeMessage.getMessage();
        }
        return null;
    }

    public WorkflowLoadHelper getLoadHelper() {
        return m_metaPersistor.getLoadHelper();
    }

    /** {@inheritDoc} */
    @Override
    public Node getNode() {
        return m_node;
    }

    /** {@inheritDoc} */
    @Override
    public SingleNodeContainerSettings getSNCSettings() {
        return m_sncSettings;
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowObject> getFlowObjects() {
        return m_flowObjects;
    }

    /** {@inheritDoc} */
    @Override
    public SingleNodeContainer getNodeContainer(final WorkflowManager wm, final NodeID id) {
        return new NativeNodeContainer(wm, id, this);
    }

    NodePersistorVersion1xx createNodePersistor(final NodeSettingsRO settings) {
        return new NodePersistorVersion1xx(this, getLoadVersion(), settings);
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult result) throws InvalidSettingsException, IOException {
        m_parentPersistor = parentPersistor;
        NodeContainerMetaPersistorVersion1xx meta = getMetaPersistor();
        final ReferencedFile settingsFileRef = meta.getNodeSettingsFile();
        File settingsFile = settingsFileRef.getFile();
        String error;
        if (!settingsFile.isFile()) {
            setDirtyAfterLoad();
            throw new IOException("Can't read node file \"" + settingsFile.getAbsolutePath() + "\"");
        }
        NodeSettingsRO settings;
        try {
            InputStream in = new FileInputStream(settingsFile);
            in = parentPersistor.decipherInput(in);
            settings = NodeSettings.loadFromXML(new BufferedInputStream(in));
        } catch (IOException ioe) {
            setDirtyAfterLoad();
            throw ioe;
        }

        boolean resetRequired = meta.load(settings, parentSettings, result);
        m_nodeSettings = settings;
        if (resetRequired) {
            setNeedsResetAfterLoad();
            setDirtyAfterLoad();
        }
        if (meta.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }

        NodeAndBundleInformation nodeInfo;
        try {
            nodeInfo = loadNodeFactoryInfo(parentSettings, settings);
        } catch (InvalidSettingsException e) {
            if (settingsFile.getName().equals(WorkflowPersistor.WORKFLOW_FILE)) {
                error = "Can't load meta flows in this version";
            } else {
                error = "Can't load node factory class name";
            }
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        NodeSettingsRO additionalFactorySettings;
        try {
            additionalFactorySettings = loadAdditionalFactorySettings(settings);
        } catch (Exception e) {
            error = "Unable to load additional factory settings for \"" + nodeInfo + "\"";
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        NodeFactory<NodeModel> nodeFactory;
        try {
            nodeFactory = loadNodeFactory(nodeInfo.getFactoryClass());
        } catch (Exception e) {
            // setDirtyAfterLoad(); // don't set dirty, missing node placeholder will be used instead
            throw new NodeFactoryUnknownException(nodeInfo, additionalFactorySettings, e);
        }

        try {
            if (additionalFactorySettings != null) {
                nodeFactory.loadAdditionalFactorySettings(additionalFactorySettings);
            }
        } catch (Exception e) {
            error = "Unable to load additional factory settings into node factory (node \"" + nodeInfo + "\")";
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        m_node = new Node(nodeFactory);
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final LoadResult result) throws InvalidSettingsException, CanceledExecutionException, IOException {
        final NodeSettingsRO settingsForNode = loadSettingsForNode(result);
        m_settingsFailPolicy = translateToFailPolicy(m_metaPersistor.getState());
        NodePersistorVersion1xx nodePersistor = createNodePersistor(settingsForNode);
        m_sncSettings = new SingleNodeContainerSettings();
        try {
            m_sncSettings.setMemoryPolicy(loadMemoryPolicySettings(m_nodeSettings, nodePersistor));
        } catch (InvalidSettingsException e) {
            String error = "Unable to load SNC settings: " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            return;
        }
        try {
            NodeSettings modelSettings = loadModelSettings(settingsForNode);
            if (modelSettings != null) { // null if the node never had settings - no reason to load them
                // this also validates the settings
                m_node.loadModelSettingsFrom(modelSettings);

                // previous versions of KNIME (2.7 and before) kept the model settings only in the node;
                // NodeModel#saveSettingsTo was always called before the dialog was opened (some dialog implementations
                // rely on the exact structure of the NodeSettings ... which may change between versions).
                // We wash the settings through the node so that the model settings are updated (they possibly
                // no longer map to the variable settings loaded further down below - if so, the inconsistency
                // is warned later during configuration)
                NodeSettings washedSettings = new NodeSettings("model");
                m_node.saveModelSettingsTo(washedSettings);
                m_sncSettings.setModelSettings(washedSettings);
            }
        } catch (Exception e) {
            final String error;
            if (e instanceof InvalidSettingsException) {
                error = "Loading model settings failed: " + e.getMessage();
            } else {
                error = "Caught \"" + e.getClass().getSimpleName() + "\", "
                        + "Loading model settings failed: " + e.getMessage();
            }
            LoadNodeModelSettingsFailPolicy pol = getModelSettingsFailPolicy();
            if (pol == null) {
                if (!nodePersistor.isConfigured()) {
                    pol = LoadNodeModelSettingsFailPolicy.IGNORE;
                } else if (nodePersistor.isExecuted()) {
                    pol = LoadNodeModelSettingsFailPolicy.WARN;
                } else {
                    pol = LoadNodeModelSettingsFailPolicy.FAIL;
                }
            }
            switch (pol) {
                case IGNORE:
                    if (!(e instanceof InvalidSettingsException)) {
                        m_logger.coding(error, e);
                    }
                    break;
                case FAIL:
                    result.addError(error);
                    m_node.createErrorMessageAndNotify(error, e);
                    setNeedsResetAfterLoad();
                    break;
                case WARN:
                    m_node.createWarningMessageAndNotify(error, e);
                    result.addWarning(error);
                    setDirtyAfterLoad();
                    break;
            }
        }
        try {
            WorkflowPersistorVersion1xx wfmPersistor = getWorkflowManagerPersistor();
            HashMap<Integer, ContainerTable> globalTableRepository = wfmPersistor.getGlobalTableRepository();
            WorkflowFileStoreHandlerRepository fileStoreHandlerRepository =
                wfmPersistor.getFileStoreHandlerRepository();
            nodePersistor.load(m_node, m_parentPersistor, exec, tblRep, globalTableRepository,
                fileStoreHandlerRepository, result);
        } catch (final Exception e) {
            String error = "Error loading node content: " + e.getMessage();
            getLogger().warn(error, e);
            needsResetAfterLoad();
            result.addError(error);
        }
        try {
            m_sncSettings.setVariablesSettings(loadVariableSettings(settingsForNode));
        } catch (InvalidSettingsException e) {
            String msg = "Could load variable settings: " + e.getMessage();
            result.addError(msg);
            setDirtyAfterLoad();
            setNeedsResetAfterLoad();
        }
        try {
            m_flowObjects = loadFlowObjects(m_nodeSettings);
        } catch (Exception e) {
            m_flowObjects = Collections.emptyList();
            String error = "Error loading flow variables: " + e.getMessage();
            getLogger().warn(error, e);
            result.addError(error);
            setDirtyAfterLoad();
            setNeedsResetAfterLoad();
        }
        if (nodePersistor.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }
        if (nodePersistor.needsResetAfterLoad()) {
            setNeedsResetAfterLoad();
        }
        exec.setProgress(1.0);
    }

    /** Loads the settings passed to the NodePersistor class. It includes node settings, variable settings,
     * file store information (but not node factory information). In 2.7 and before it was contained in a separate
     * node.xml, in 2.8+ it's all in one file.
     * @param loadResult ...
     * @return ...
     * @throws FileNotFoundException ...
     * @throws IOException ...
     */
    private NodeSettingsRO loadSettingsForNode(final LoadResult loadResult) throws FileNotFoundException, IOException {
        if (getLoadVersion().ordinal() < WorkflowPersistorVersion1xx.LoadVersion.V280.ordinal()) {
            ReferencedFile nodeFile;
            try {
                nodeFile = loadNodeFile(m_nodeSettings);
            } catch (InvalidSettingsException e) {
                String error = "Unable to load node settings file for node with ID suffix "
                        + m_metaPersistor.getNodeIDSuffix() + " (node \"" + m_node.getName() + "\"): " + e.getMessage();
                loadResult.addError(error);
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                return new NodeSettings("empty");
            }
            File configFile = nodeFile.getFile();
            if (configFile == null || !configFile.isFile() || !configFile.canRead()) {
                String error = "Unable to read node settings file for node with ID suffix "
                        + m_metaPersistor.getNodeIDSuffix() + " (node \"" + m_node.getName() + "\"), file \""
                        + configFile + "\"";
                loadResult.addError(error);
                setNeedsResetAfterLoad(); // also implies dirty
                return new NodeSettings("empty");
            } else {
                InputStream in = new FileInputStream(configFile);
                try {
                    in = m_parentPersistor.decipherInput(in);
                    return NodeSettings.loadFromXML(new BufferedInputStream(in));
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        getLogger().error("Failed to close input stream on \""
                                + configFile.getAbsolutePath() + "\"", e);
                    }
                }
            }
        } else {
            return m_nodeSettings;
        }
    }

    /**
     * Load factory name.
     *
     * @param parentSettings settings of outer workflow (old style workflows have it in there)
     * @param settings settings of this node, ignored in this implementation
     * @return Factory information.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    NodeAndBundleInformation loadNodeFactoryInfo(final NodeSettingsRO parentSettings, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(WorkflowPersistorVersion1xx.LoadVersion.V200)) {
            String factoryName = parentSettings.getString("factory");
            // This is a hack to load old J48 Nodes Model from pre-2.0 workflows
            if ("org.knime.ext.weka.j48_2.WEKAJ48NodeFactory2".equals(factoryName)
                || "org.knime.ext.weka.j48.WEKAJ48NodeFactory".equals(factoryName)) {
                factoryName = "org.knime.ext.weka.knimeJ48.KnimeJ48NodeFactory";
            }
            return new NodeAndBundleInformation(factoryName, null, null, null, null);
        } else {
            return NodeAndBundleInformation.load(settings, getLoadVersion());
        }
    }

    @SuppressWarnings("unchecked")
    final NodeFactory<NodeModel> loadNodeFactory(final String factoryClassName) throws InvalidSettingsException,
        InstantiationException, IllegalAccessException, ClassNotFoundException {
        // use global Class Creator utility for Eclipse "compatibility"
        try {
            NodeFactory<NodeModel> f =
                (NodeFactory<NodeModel>)((GlobalClassCreator.createClass(factoryClassName)).newInstance());
            return f;
        } catch (ClassNotFoundException ex) {
            try {
                // Because of the changed startup process, not all factories
                // may be loaded. Therefore in order to search for a matching
                // factory below we need to initialize the whole repository
                // first
                REPOS_LOAD_METHOD.invoke(REPOS_MANAGER);
            } catch (Exception ex1) {
                getLogger().error("Could not load repository manager", ex1);
            }

            String[] x = factoryClassName.split("\\.");
            String simpleClassName = x[x.length - 1];

            for (String s : NodeFactory.getLoadedNodeFactories()) {
                if (s.endsWith("." + simpleClassName)) {
                    NodeFactory<NodeModel> f =
                        (NodeFactory<NodeModel>)((GlobalClassCreator.createClass(s)).newInstance());
                    getLogger().warn(
                        "Substituted '" + f.getClass().getName() + "' for unknown factory '" + factoryClassName + "'");
                    return f;
                }
            }
            throw ex;
        }
    }

    /**
     * Called during load by the Node instance to determine whether or not the flow variable output port is to be
     * populated with the FlowVariablePortObjectSpec singleton.
     *
     * @return true if node's state is not idle
     * @since 2.7
     */
    public boolean hasConfiguredState() {
        if (m_metaPersistor == null) {
            throw new IllegalStateException("preloadnodecontainer hasn't been called yet");
        }
        switch (m_metaPersistor.getState()) {
            case IDLE:
            case UNCONFIGURED_MARKEDFOREXEC:
                return false;
            default:
                return true;
        }
    }

    /**
     * Reads sub settings object.
     *
     * @param settings ...
     * @return child settings (here: null)
     * @throws InvalidSettingsException ...
     */
    NodeSettingsRO loadAdditionalFactorySettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // added in v2.6 (during hittisau 2012) without changing the version
        // number (current load version is V250("2.5.0"))
        if (settings.containsKey("factory_settings")) {
            return settings.getNodeSettings("factory_settings");
        }
        return null;
    }

    /**
     * Load file containing node settings.
     *
     * @param settings to load from, used in sub-classes, ignored here.
     * @return pointer to "settings.xml"
     * @throws InvalidSettingsException If that fails for any reason.
     */
    ReferencedFile loadNodeFile(final NodeSettingsRO settings) throws InvalidSettingsException {
        ReferencedFile nodeDir = getMetaPersistor().getNodeContainerDirectory();
        if (getLoadVersion().isOlderThan(WorkflowPersistorVersion1xx.LoadVersion.V200)) {
            return new ReferencedFile(nodeDir, SETTINGS_FILE_NAME);
        } else {
            return new ReferencedFile(nodeDir, settings.getString("node_file"));
        }
    }

    /**
     * Load configuration of node.
     *
     * @param settings to load from (used in sub-classes)
     * @param nodePersistor persistor to allow this implementation to load old-style
     * @return node config
     * @throws InvalidSettingsException if that fails for any reason.
     */
    MemoryPolicy loadMemoryPolicySettings(final NodeSettingsRO settings,
        final NodePersistorVersion1xx nodePersistor) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(WorkflowPersistorVersion1xx.LoadVersion.V210_Pre)) {
            // in versions before KNIME 1.2.0, there were no misc settings
            // in the dialog, we must use caution here: if they are not present
            // we use the default, i.e. small data are kept in memory
            if (settings.containsKey(Node.CFG_MISC_SETTINGS)
                && settings.getNodeSettings(Node.CFG_MISC_SETTINGS).containsKey(SingleNodeContainer.CFG_MEMORY_POLICY)) {
                NodeSettingsRO sub = settings.getNodeSettings(Node.CFG_MISC_SETTINGS);
                String memoryPolicy =
                    sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY, MemoryPolicy.CacheSmallInMemory.toString());
                if (memoryPolicy == null) {
                    throw new InvalidSettingsException("Can't use null memory policy.");
                }
                try {
                    return MemoryPolicy.valueOf(memoryPolicy);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException("Invalid memory policy: " + memoryPolicy);
                }
            } else {
                return MemoryPolicy.CacheSmallInMemory;
            }
        } else {
            // any version after 2.0 saves the snc settings in the settings.xml
            // (previously these settings were saves as part of the node.xml)
            NodeSettingsRO sub = settings.getNodeSettings(Node.CFG_MISC_SETTINGS);
            String memoryPolicy =
                sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY, MemoryPolicy.CacheSmallInMemory.toString());
            if (memoryPolicy == null) {
                throw new InvalidSettingsException("Can't use null memory policy.");
            }
            try {
                return MemoryPolicy.valueOf(memoryPolicy);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid memory policy: " + memoryPolicy);
            }
        }
    }

    /** The settings passed to the NodeModel's validate and load method.
     * @param settings The whole settings tree.
     * @return the settings child {@link SingleNodeContainer#CFG_MODEL}.
     * @throws InvalidSettingsException ... */
    NodeSettings loadModelSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // settings not present if node never had settings (different since 2.8 -- before the node always had settings,
        // defined by NodeModel#saveSettings -- these settings were never confirmed through validate/load though)
        if (settings.containsKey(SingleNodeContainer.CFG_MODEL)) {
            return (NodeSettings)settings.getNodeSettings(SingleNodeContainer.CFG_MODEL);
        }
        return null;
    }

    /** The variable settings (whatever overwrites user parameters).
     * @param settings The whole settings tree.
     * @return the settings child {@link SingleNodeContainer#CFG_VARIABLES} if present, or null.
     * @throws InvalidSettingsException ... */
    NodeSettings loadVariableSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(SingleNodeContainer.CFG_VARIABLES)) {
            return (NodeSettings)settings.getNodeSettings(SingleNodeContainer.CFG_VARIABLES);
        }
        return null;
    }

    /**
     * Load from variables.
     *
     * @param settings to load from, ignored in this implementation (flow variables added in later versions)
     * @return an empty list.
     * @throws InvalidSettingsException if that fails for any reason.
     */
    List<FlowObject> loadFlowObjects(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(WorkflowPersistorVersion1xx.LoadVersion.V200)) {
            return Collections.emptyList();
        }
        List<FlowObject> result = new ArrayList<FlowObject>();
        NodeSettingsRO stackSet;
        if (getLoadVersion().isOlderThan(WorkflowPersistorVersion1xx.LoadVersion.V220)) {
            stackSet = settings.getNodeSettings("scope_stack");
        } else {
            stackSet = settings.getNodeSettings("flow_stack");
        }
        for (String key : stackSet.keySet()) {
            NodeSettingsRO sub = stackSet.getNodeSettings(key);
            String type = sub.getString("type");
            if ("variable".equals(type)) {
                FlowVariable v = FlowVariable.load(sub);
                result.add(v);
            } else if ("loopcontext".equals(type)) {
                result.add(new RestoredFlowLoopContext());
                //                int tailID = sub.getInt("tailID");
            } else if ("loopcontext_execute".equals(type)) {
                result.add(new InnerFlowLoopContext());
            } else if ("loopcontext_inactive".equals(type)) {
                FlowLoopContext flc = new FlowLoopContext();
                flc.inactiveScope(true);
                result.add(flc);
            } else if ("scopecontext".equals(type)) {
                result.add(new FlowScopeContext());
            } else if ("scopecontext_inactive".equals(type)) {
                FlowScopeContext slc = new FlowScopeContext();
                slc.inactiveScope(true);
                result.add(slc);
            } else {
                throw new InvalidSettingsException("Unknown flow object type: " + type);
            }
        }
        return result;
    }

    boolean shouldFixModelPortOrder() {
        return getLoadVersion().isOlderThan(WorkflowPersistorVersion1xx.LoadVersion.V200);
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

    static final LoadNodeModelSettingsFailPolicy translateToFailPolicy(final InternalNodeContainerState nodeState) {
        switch (nodeState) {
            case IDLE:
                return LoadNodeModelSettingsFailPolicy.IGNORE;
            case EXECUTED:
                return LoadNodeModelSettingsFailPolicy.WARN;
            default:
                return LoadNodeModelSettingsFailPolicy.FAIL;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.7
     */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformation nodeInfo,
        final NodeSettingsRO additionalFactorySettings, final ArrayList<PersistorWithPortIndex> upstreamNodes,
        final ArrayList<List<PersistorWithPortIndex>> downstreamNodes) {
        if (m_node == null) {
            /* Input ports from the connection table. */
            // first is flow var port
            PortType[] inPortTypes = new PortType[Math.max(upstreamNodes.size() - 1, 0)];
            Arrays.fill(inPortTypes, BufferedDataTable.TYPE); // default to BDT for unconnected ports
            for (int i = 0; i < inPortTypes.length; i++) {
                PersistorWithPortIndex p = upstreamNodes.get(i + 1); // first is flow var port
                if (p != null) {
                    PortType portTypeFromUpstreamNode = p.getPersistor().getUpstreamPortType(p.getPortIndex());
                    if (portTypeFromUpstreamNode != null) { // null if upstream is missing, too
                        inPortTypes[i] = portTypeFromUpstreamNode;
                    }
                }
            }

            /* Output ports from node settings (saved ports) -- if possible (executed) */
            String nodeName = nodeInfo.getNodeNameNotNull();
            PortType[] outPortTypes;
            try {
                LoadResult guessLoadResult = new LoadResult("Port type guessing for missing node \"" + nodeName + "\"");
                NodeSettingsRO settingsForNode = loadSettingsForNode(guessLoadResult);
                NodePersistorVersion1xx nodePersistor = createNodePersistor(settingsForNode);
                outPortTypes = nodePersistor.guessOutputPortTypes(m_parentPersistor, guessLoadResult, nodeName);
                if (guessLoadResult.hasErrors()) {
                    getLogger().debug(
                        "Errors guessing port types for missing node \"" + nodeName + "\": "
                            + guessLoadResult.getFilteredError("", LoadResultEntryType.Error));
                }
            } catch (Exception e) {
                getLogger().debug("Unable to guess port types for missing node \"" + nodeName + "\"", e);
                outPortTypes = null;
            }
            if (outPortTypes == null) { // couldn't guess port types from looking at node settings (e.g. not executed)
                // default to BDT for unconnected ports
                outPortTypes = new PortType[Math.max(downstreamNodes.size() - 1, 0)];
            }
            for (int i = 0; i < outPortTypes.length; i++) {
                PortType type = outPortTypes[i];
                // output types may be partially filled by settings guessing above, list may be empty or too short
                List<PersistorWithPortIndex> list = i < downstreamNodes.size() - 1 ? downstreamNodes.get(i + 1) : null;
                if (list != null) {
                    assert !list.isEmpty();
                    for (PersistorWithPortIndex p : list) {
                        PortType current = p.getPersistor().getDownstreamPortType(p.getPortIndex());
                        if (current == null) {
                            // ignore, downstream node is also missing
                        } else if (type == null) {
                            type = current;
                        } else if (type.equals(current)) {
                            // keep type
                        } else {
                            // this shouldn't really happen - someone changed port types between versions
                            type = new PortType(PortObject.class);
                        }
                    }
                    outPortTypes[i] = type;
                }
                if (outPortTypes[i] == null) {
                    // might still be null if missing node is only connected to missing node, fallback: BDT
                    outPortTypes[i] = BufferedDataTable.TYPE;
                }
            }
            MissingNodeFactory nodefactory =
                new MissingNodeFactory(nodeInfo, additionalFactorySettings, inPortTypes, outPortTypes);
            if (getLoadVersion().ordinal() < WorkflowPersistorVersion1xx.VERSION_LATEST.ordinal()) {
                nodefactory.setCopyInternDirForWorkflowVersionChange(true);
            }
            nodefactory.init();
            m_node = new Node((NodeFactory)nodefactory); // unclear why this needs a cast
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.7
     */
    @Override
    public PortType getDownstreamPortType(final int index) {
        if (m_node != null && index < m_node.getNrInPorts()) {
            return m_node.getInputType(index);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.7
     */
    @Override
    public PortType getUpstreamPortType(final int index) {
        if (m_node != null && index < m_node.getNrOutPorts()) {
            return m_node.getOutputType(index);
        }
        return null;
    }

    protected static String save(final NativeNodeContainer nnc, final ReferencedFile nodeDirRef,
        final ExecutionMonitor exec, final boolean isSaveData) throws CanceledExecutionException, IOException {
        String settingsDotXML = nnc.getParent().getCipherFileName(SETTINGS_FILE_NAME);
        ReferencedFile sncWorkingDirRef = nnc.getNodeContainerDirectory();
        if (nodeDirRef.equals(sncWorkingDirRef) && !nnc.isDirty()) {
            return settingsDotXML;
        }
        File nodeDir = nodeDirRef.getFile();
        boolean nodeDirExists = nodeDir.exists();
        boolean nodeDirDeleted = deleteChildren(nodeDir, SingleNodeContainer.DROP_DIR_NAME);
        nodeDir.mkdirs();
        if (!nodeDir.isDirectory() || !nodeDir.canWrite()) {
            throw new IOException("Unable to write or create directory \"" + nodeDirRef + "\"");
        }
        String debug;
        if (nodeDirExists) {
            if (nodeDirDeleted) {
                debug = "Replaced node directory \"" + nodeDirRef + "\"";
            } else {
                debug = "Failed to replace node directory \"" + nodeDirRef + "\" -- writing into existing directory";
            }
        } else {
            debug = "Created node directory \"" + nodeDirRef + "\"";
        }
        SAVE_LOGGER.debug(debug);

        // get drop directory in "home" (the designated working dir)
        ReferencedFile nodeDropDirInWDRef =
            sncWorkingDirRef == null ? null : new ReferencedFile(sncWorkingDirRef, SingleNodeContainer.DROP_DIR_NAME);

        ReferencedFile nodeDropDirRef = new ReferencedFile(nodeDirRef, SingleNodeContainer.DROP_DIR_NAME);

        // if node container directory is set and we write into a new location
        if (nodeDropDirInWDRef != null && !nodeDropDirRef.equals(nodeDropDirInWDRef)) {

            // this code is executed in either of the two cases:
            // - Node was copy&paste from node with drop folder
            //   (its (freshly copied) drop folder is currently in /tmp)
            // - Node is saved into new location (saveAs) -- need to copy
            //   the drop folder there (either from /tmp or from working dir)
            File dropInSource = nodeDropDirRef.getFile();
            File dropInTarget = new File(nodeDir, SingleNodeContainer.DROP_DIR_NAME);
            if (dropInSource.exists()) {
                FileUtil.copyDir(dropInSource, dropInTarget);
            }
        }
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveNodeFactory(settings, nnc);
        saveNodeFileName(nnc, settings, nodeDirRef); // only to allow 2.7- clients to load 2.8+ workflows
        saveFlowObjectStack(settings, nnc);
        saveSNCSettings(settings, nnc);
        NodeContainerMetaPersistorVersion1xx.save(settings, nnc, nodeDirRef);
        NodePersistorVersion1xx.save(nnc, settings, exec, nodeDirRef,
            isSaveData && nnc.getInternalState().equals(InternalNodeContainerState.EXECUTED));
        File nodeSettingsXMLFile = new File(nodeDir, settingsDotXML);
        OutputStream os = new FileOutputStream(nodeSettingsXMLFile);
        os = nnc.getParent().cipherOutput(os);
        settings.saveToXML(os);
        if (sncWorkingDirRef == null) {
            // set working dir so that we can unset the dirty flag
            sncWorkingDirRef = nodeDirRef;
            nnc.setNodeContainerDirectory(sncWorkingDirRef);
        }
        if (nodeDirRef.equals(sncWorkingDirRef)) {
            nnc.unsetDirty();
        }
        exec.setProgress(1.0);
        return settingsDotXML;
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    protected static void saveNodeFactory(final NodeSettingsWO settings, final NativeNodeContainer nnc) {
        final Node node = nnc.getNode();
        // node info to missing node is the info to the actual instance, not MissingNodeFactory
        NodeAndBundleInformation nodeInfo = node.getNodeAndBundleInformation();
        nodeInfo.save(settings);

        NodeSettingsWO subSets = settings.addNodeSettings("factory_settings");
        node.getFactory().saveAdditionalFactorySettings(subSets);
    }

    protected static ReferencedFile saveNodeFileName(final SingleNodeContainer snc, final NodeSettingsWO settings,
        final ReferencedFile nodeDirectoryRef) {
        // KNIME 2.7- reads from this file. It used to be "node.xml", which was removed in 2.8 and now the settings.xml
        // (contains the settings from this method argument) contains everything. We save the node_file so that
        // old KNIME instances can read new workflows
        String fileName = SETTINGS_FILE_NAME;
        fileName = snc.getParent().getCipherFileName(fileName);
        settings.addString("node_file", fileName);
        return new ReferencedFile(nodeDirectoryRef, fileName);
    }

    protected static void saveSNCSettings(final NodeSettingsWO settings, final SingleNodeContainer snc) {
        // if no settings are stored in the SNCSettings, use the ones from the NodeModel as
        // default (node uses default configuration after new instantiation), fixes bug 4364
        snc.saveSNCSettings(settings, true);
    }

    protected static void saveFlowObjectStack(final NodeSettingsWO settings, final SingleNodeContainer nc) {
        NodeSettingsWO stackSet = settings.addNodeSettings("flow_stack");
        FlowObjectStack stack = nc.getOutgoingFlowObjectStack();
        @SuppressWarnings("unchecked")
        Iterable<FlowObject> myObjs = stack == null ? Collections.EMPTY_LIST : stack.getFlowObjectsOwnedBy(nc.getID(), /*exclude*/
            Scope.Local);
        int c = 0;
        for (FlowObject s : myObjs) {
            if (s instanceof FlowVariable) {
                FlowVariable v = (FlowVariable)s;
                NodeSettingsWO sub = stackSet.addNodeSettings("Variable_" + c);
                sub.addString("type", "variable");
                v.save(sub);
            } else if (s instanceof FlowLoopContext) {
                if (!((FlowLoopContext)s).isInactiveScope()) {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Loop_" + c);
                    sub.addString("type", "loopcontext");
                } else {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Inactive_Loop_" + c);
                    sub.addString("type", "loopcontext_inactive");
                }
            } else if (s instanceof InnerFlowLoopContext) {
                NodeSettingsWO sub = stackSet.addNodeSettings("Loop_Execute_" + c);
                sub.addString("type", "loopcontext_execute");
            } else if (s instanceof FlowScopeContext) {
                if (!((FlowScopeContext)s).isInactiveScope()) {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Scope_" + c);
                    sub.addString("type", "scopecontext");
                } else {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Inactive_Scope_" + c);
                    sub.addString("type", "scopecontext_inactive");
                }
            } else {
                SAVE_LOGGER.error("Saving of flow objects of type \"" + s.getClass().getSimpleName()
                    + "\" not implemented");
            }
            c += 1;
        }
    }

    /**
     * Delete content of directory, skipping (direct) childs as given in 2nd argument. Use case is: to delete a node
     * directory but skip its drop folder.
     *
     * @param directory The directory whose content is to be deleted
     * @param exclude A list of direct child names that are to be skipped
     * @return false if directory does not exist, true if non-listed children are deleted
     */
    private static boolean deleteChildren(final File directory, final String... exclude) {
        if (!directory.isDirectory()) {
            return false;
        }
        HashSet<String> excludeSet = new HashSet<String>(Arrays.asList(exclude));
        File[] children = directory.listFiles();
        if (children == null) {
            return true;
        }
        boolean success = true;
        for (File f : children) {
            if (!excludeSet.contains(f.getName())) {
                boolean s = FileUtil.deleteRecursively(f);
                success &= s;
            }
        }
        return success;
    }

}
