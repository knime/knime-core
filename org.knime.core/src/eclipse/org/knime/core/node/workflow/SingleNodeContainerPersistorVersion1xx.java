/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import org.knime.core.node.missing.MissingNodeFactory;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.NodeFactoryUnknownException;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

/**
 *
 * @author wiswedel, University of Konstanz
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class SingleNodeContainerPersistorVersion1xx
    implements SingleNodeContainerPersistor, FromFileNodeContainerPersistor {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final LoadVersion m_version;

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

    private static final Method REPOS_LOAD_METHOD;
    private static final Object REPOS_MANAGER;

    static {
        Class<?> repManClass;
        try {
            repManClass = Class.forName(
                "org.knime.workbench.repository.RepositoryManager");
            Field instanceField = repManClass.getField("INSTANCE");
            REPOS_MANAGER = instanceField.get(null);
            REPOS_LOAD_METHOD = repManClass.getMethod("getRoot");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Load persistor. */
    SingleNodeContainerPersistorVersion1xx(
            final WorkflowPersistorVersion1xx workflowPersistor,
            final ReferencedFile nodeSettingsFile,
            final WorkflowLoadHelper loadHelper,
            final LoadVersion version) {
        this(workflowPersistor, new NodeContainerMetaPersistorVersion1xx(
                nodeSettingsFile, loadHelper, version), version);
    }

    /** Constructor used internally, not used outside this class or its
     * derivates.
     * @param version
     * @param metaPersistor
     * @param wfmPersistor
     */
    SingleNodeContainerPersistorVersion1xx(
            final WorkflowPersistorVersion1xx wfmPersistor,
            final NodeContainerMetaPersistorVersion1xx metaPersistor,
            final LoadVersion version) {
        if (version == null || wfmPersistor == null) {
            throw new NullPointerException();
        }
        m_version = version;
        m_metaPersistor = metaPersistor;
        m_wfmPersistor = wfmPersistor;
    }

    protected final LoadVersion getLoadVersion() {
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
    public SingleNodeContainer getNodeContainer(
            final WorkflowManager wm, final NodeID id) {
        return new SingleNodeContainer(wm, id, this);
    }

    NodePersistorVersion1xx createNodePersistor(final ReferencedFile nodeConfigFile) {
        return new NodePersistorVersion1xx(this, null, nodeConfigFile);
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor,
            final NodeSettingsRO parentSettings, final LoadResult result)
    throws InvalidSettingsException, IOException {
        m_parentPersistor = parentPersistor;
        NodeContainerMetaPersistorVersion1xx meta = getMetaPersistor();
        final ReferencedFile settingsFileRef = meta.getNodeSettingsFile();
        File settingsFile = settingsFileRef.getFile();
        String error;
        if (!settingsFile.isFile()) {
            setDirtyAfterLoad();
            throw new IOException("Can't read node file \""
                    + settingsFile.getAbsolutePath() + "\"");
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
            if (settingsFile.getName().equals(
                    WorkflowPersistor.WORKFLOW_FILE)) {
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
        } catch (Throwable e) {
            error =  "Unable to load additional factory settings for \"" + nodeInfo + "\"";
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        NodeFactory<NodeModel> nodeFactory;
        try {
            nodeFactory = loadNodeFactory(nodeInfo.getFactoryClass());
        } catch (Throwable e) {
            // setDirtyAfterLoad(); // don't set dirty, missing node placeholder will be used instead
            throw new NodeFactoryUnknownException(nodeInfo, additionalFactorySettings, e);
        }

        try {
            if (additionalFactorySettings != null) {
                nodeFactory.loadAdditionalFactorySettings(additionalFactorySettings);
            }
        } catch (Throwable e) {
            error =  "Unable to load additional factory settings into node factory (node \"" + nodeInfo + "\")";
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        m_node = new Node(nodeFactory);
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult result)
    throws InvalidSettingsException, CanceledExecutionException, IOException {
        ReferencedFile nodeFile;
        try {
            nodeFile = loadNodeFile(m_nodeSettings);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load node settings file for node with ID suffix "
                    + m_metaPersistor.getNodeIDSuffix() + " (node \"" + m_node.getName() + "\"): " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            return;
        }
        m_settingsFailPolicy = translateToFailPolicy(m_metaPersistor.getState());
        NodePersistorVersion1xx nodePersistor = createNodePersistor(nodeFile);
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
            m_flowObjects = loadFlowObjects(m_nodeSettings);
        } catch (Exception e) {
            m_flowObjects = Collections.emptyList();
            String error = "Error loading flow variables: "
                + e.getMessage();
            getLogger().warn(error, e);
            result.addError(error);
            setDirtyAfterLoad();
            needsResetAfterLoad();
        }
        try {
            m_sncSettings = loadSNCSettings(m_nodeSettings, nodePersistor);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load SNC settings: " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            m_sncSettings = new SingleNodeContainerSettings();
            setDirtyAfterLoad();
            return;
        }
        if (nodePersistor.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }
        if (nodePersistor.needsResetAfterLoad()) {
            setNeedsResetAfterLoad();
        }
        exec.setProgress(1.0);
    }

    /** Load factory name.
     * @param parentSettings settings of outer workflow (old style workflows
     *        have it in there)
     * @param settings settings of this node, ignored in this implementation
     * @return Factory information.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    NodeAndBundleInformation loadNodeFactoryInfo(
            final NodeSettingsRO parentSettings, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String factoryName = parentSettings.getString("factory");

        // This is a hack to load old J48 Nodes Model from pre-2.0 workflows
        if ("org.knime.ext.weka.j48_2.WEKAJ48NodeFactory2".equals(factoryName)
                || "org.knime.ext.weka.j48.WEKAJ48NodeFactory".equals(factoryName)) {
            factoryName = "org.knime.ext.weka.knimeJ48.KnimeJ48NodeFactory";
        }
        return new NodeAndBundleInformation(factoryName, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    final NodeFactory<NodeModel> loadNodeFactory(
            final String factoryClassName) throws InvalidSettingsException,
            InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        // use global Class Creator utility for Eclipse "compatibility"
        try {
            NodeFactory<NodeModel> f = (NodeFactory<NodeModel>)((GlobalClassCreator
                    .createClass(factoryClassName)).newInstance());
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

    /** Reads sub settings object.
     * @param settings ...
     * @return child settings (here: null)
     * @throws InvalidSettingsException ...
     */
    NodeSettingsRO loadAdditionalFactorySettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }

    /** Load file containing node settings.
     * @param settings to load from, used in sub-classes, ignored here.
     * @return pointer to "settings.xml"
     * @throws InvalidSettingsException If that fails for any reason.
     */
    ReferencedFile loadNodeFile(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        ReferencedFile nodeDir = getMetaPersistor().getNodeContainerDirectory();
        return new ReferencedFile(nodeDir, SETTINGS_FILE_NAME);
    }

    /** Load configuration of node.
     * @param settings to load from (used in sub-classes)
     * @param nodePersistor persistor to allow this implementation to load
     *        old-style
     * @return node config
     * @throws InvalidSettingsException if that fails for any reason.
     */
    SingleNodeContainerSettings loadSNCSettings(
            final NodeSettingsRO settings,
            final NodePersistorVersion1xx nodePersistor)
        throws InvalidSettingsException {
        NodeSettingsRO s = nodePersistor.getSettings();
        SingleNodeContainerSettings sncs = new SingleNodeContainerSettings();
        // in versions before KNIME 1.2.0, there were no misc settings
        // in the dialog, we must use caution here: if they are not present
        // we use the default, i.e. small data are kept in memory
        MemoryPolicy p;
        if (s.containsKey(Node.CFG_MISC_SETTINGS)
                && s.getNodeSettings(Node.CFG_MISC_SETTINGS).containsKey(
                        SingleNodeContainer.CFG_MEMORY_POLICY)) {
            NodeSettingsRO sub =
                    s.getNodeSettings(Node.CFG_MISC_SETTINGS);
            String memoryPolicy =
                    sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY,
                            MemoryPolicy.CacheSmallInMemory.toString());
            if (memoryPolicy == null) {
                throw new InvalidSettingsException(
                        "Can't use null memory policy.");
            }
            try {
                p = MemoryPolicy.valueOf(memoryPolicy);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException(
                        "Invalid memory policy: " + memoryPolicy);
            }
        } else {
            p = MemoryPolicy.CacheSmallInMemory;
        }
        sncs.setMemoryPolicy(p);
        return sncs;
    }

    /** Load from variables.
     * @param settings to load from, ignored in this implementation
     *        (flow variables added in later versions)
     * @return an empty list.
     * @throws InvalidSettingsException if that fails for any reason.
     */
    List<FlowObject> loadFlowObjects(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return Collections.emptyList();
    }

    boolean shouldFixModelPortOrder() {
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

    static final LoadNodeModelSettingsFailPolicy translateToFailPolicy(final State nodeState) {
        switch (nodeState) {
        case IDLE:
            return LoadNodeModelSettingsFailPolicy.IGNORE;
        case EXECUTED:
            return LoadNodeModelSettingsFailPolicy.WARN;
        default:
            return LoadNodeModelSettingsFailPolicy.FAIL;
        }
    }

    /** {@inheritDoc}
     * @since 2.7 */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformation nodeInfo,
             final NodeSettingsRO additionalFactorySettings,
             final ArrayList<PersistorWithPortIndex> upstreamNodes,
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
                NodePersistorVersion1xx nodePersistor = createNodePersistor(loadNodeFile(m_nodeSettings));
                LoadResult guessLoadResult = new LoadResult("Port type guessing for missing node \"" + nodeName + "\"");
                outPortTypes = nodePersistor.guessOutputPortTypes(m_parentPersistor, guessLoadResult, nodeName);
                if (guessLoadResult.hasErrors()) {
                    getLogger().debug("Errors guessing port types for missing node \"" + nodeName + "\": "
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
            MissingNodeFactory nodefactory = new MissingNodeFactory(
                    nodeInfo, additionalFactorySettings, inPortTypes, outPortTypes);
            if (getLoadVersion().ordinal() < WorkflowPersistorVersion200.VERSION_LATEST.ordinal()) {
                nodefactory.setCopyInternDirForWorkflowVersionChange(true);
            }
            nodefactory.init();
            m_node = new Node((NodeFactory)nodefactory); // unclear why this needs a cast
        }
    }

    /** {@inheritDoc}
     * @since 2.7 */
    @Override
    public PortType getDownstreamPortType(final int index) {
        if (m_node != null && index < m_node.getNrInPorts()) {
            return m_node.getInputType(index);
        }
        return null;
    }

    /** {@inheritDoc}
     * @since 2.7 */
    @Override
    public PortType getUpstreamPortType(final int index) {
        if (m_node != null && index < m_node.getNrOutPorts()) {
            return m_node.getOutputType(index);
        }
        return null;
    }
}
