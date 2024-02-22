/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 4, 2013 by wiswedel
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.FileNodePersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactoryClassMapper;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.extension.InvalidNodeFactoryExtensionException;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.message.Message;
import org.knime.core.node.missing.MissingNodeFactory;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.NodeFactoryUnknownException;
import org.knime.core.util.LoadVersion;

/**
 *
 * @author wiswedel
 */
public class FileNativeNodeContainerPersistor extends FileSingleNodeContainerPersistor
    implements NativeNodeContainerPersistor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileNativeNodeContainerPersistor.class);

    public static final String NODE_FILE = "node.xml";

    private WorkflowPersistor m_parentPersistor;

    private FileNodePersistor m_nodePersistor;

    private Node m_node;

    private NodeAndBundleInformationPersistor m_nodeAndBundleInformation;

    /**
     * @param nodeSettingsFile
     * @param loadHelper
     * @param version
     */
    FileNativeNodeContainerPersistor(final ReferencedFile nodeSettingsFile, final WorkflowLoadHelper loadHelper,
        final LoadVersion version, final WorkflowDataRepository workflowDataRepository,
        final boolean mustWarnOnDataLoadError) {
        super(nodeSettingsFile, loadHelper, version, workflowDataRepository, mustWarnOnDataLoadError);
    }

    /** Called by {@link Node} to update the message field in the {@link NodeModel} class.
     * @return the msg or null.
     */
    public Message getNodeMessage() {
        NodeMessage nodeMessage = getMetaPersistor().getNodeMessage();
        if (nodeMessage != null && !nodeMessage.getMessageType().equals(NodeMessage.Type.RESET)) {
            return Message.fromNodeMessage(nodeMessage);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Node getNode() {
        return m_node;
    }

    /** {@inheritDoc} */
    @Override
    public NodeAndBundleInformationPersistor getNodeAndBundleInformation() {
        return m_nodeAndBundleInformation;
    }

    FileNodePersistor createNodePersistor(final NodeSettingsRO settings) {
        return new FileNodePersistor(this, getLoadVersion(), settings);
    }

    /** {@inheritDoc} */
    @Override
    public NativeNodeContainer getNodeContainer(final WorkflowManager wm, final NodeID id) {
        return new NativeNodeContainer(wm, id, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult result) throws InvalidSettingsException, IOException {
        super.preLoadNodeContainer(parentPersistor, parentSettings, result);
        m_parentPersistor = parentPersistor;
        NodeSettingsRO settings = getNodeSettings();
        String error;
        NodeAndBundleInformationPersistor nodeInfo;
        try {
            nodeInfo = loadNodeFactoryInfo(parentSettings, settings);
        } catch (InvalidSettingsException e) {
            setDirtyAfterLoad();
            throw e;
        }
        NodeSettingsRO additionalFactorySettings;
        try {
            additionalFactorySettings = loadAdditionalFactorySettings(settings);
        } catch (InvalidSettingsException | RuntimeException e) {
            error = "Unable to load additional factory settings for \"" + nodeInfo + "\"";
            setDirtyAfterLoad();
            throw new InvalidSettingsException(error, e);
        }
        NodeFactory<NodeModel> nodeFactory;
        try {
            nodeFactory = loadNodeFactory(nodeInfo.getFactoryClassNotNull());
        } catch (Exception e) {
            // setDirtyAfterLoad(); // don't set dirty, missing node placeholder will be used instead
            throw new NodeFactoryUnknownException(nodeInfo, additionalFactorySettings, e);
        }

        ModifiableNodeCreationConfiguration creationConfig = null;
        if (additionalFactorySettings != null) {
            try {
                nodeFactory.loadAdditionalFactorySettings(additionalFactorySettings);
            } catch (InvalidSettingsException | RuntimeException e) {
                handleExceptionPostNodeFactoryInstantiation(result, nodeInfo, additionalFactorySettings, nodeFactory,
                    "Unable to load additional factory settings into", e);
            }
        }
        try {
            creationConfig = loadCreationConfig(settings, nodeFactory).orElse(null);
        } catch (InvalidSettingsException | RuntimeException e) {
            handleExceptionPostNodeFactoryInstantiation(result, nodeInfo, additionalFactorySettings, nodeFactory,
                "Unable to instantiate creation config for", e);
        }
        m_nodeAndBundleInformation = nodeInfo;
        m_node = new Node(nodeFactory, creationConfig);
    }

    private void handleExceptionPostNodeFactoryInstantiation(final LoadResult result,
        final NodeAndBundleInformationPersistor nodeInfo, final NodeSettingsRO additionalFactorySettings,
        final NodeFactory<NodeModel> nodeFactory, final String summaryMsg, final Exception e)
        throws NodeFactoryUnknownException {
        final var nodeName = nodeInfo.getNodeName().orElseGet(nodeFactory.getClass()::getSimpleName);
        final var error = String.format("%s node \"%s\": %s", summaryMsg, nodeName, e.getMessage());
        getLogger().error(error);

        // AP-21738 - Missing Plotly extension leads to workflow/component error on load with deleted node
        // The node instance can be loaded but it fails to load additional settings; two cases to distinguish:
        // (1) - The workflow file denotes as contributing plug-in the same plug-in the concrete class of the factory
        //       lives in, for instance, a Component Output node lives in org.knime.core but it fails to load the input
        //       port type definition
        // (2) - The concrete factory instance lives in a different plug-in than what is stored in the workflow, e.g.
        //       DynamicJSNodeFactory is installed but it's concrete instance (plotly) is not installed -- in
        //       this case fail with a unknown factory exception and offer the installation of the missing extension
        final var instanceInfo = NodeAndBundleInformationPersistor.create(nodeFactory);
        if (Objects.equals(instanceInfo.getBundleSymbolicName(), nodeInfo.getBundleSymbolicName())) {
            result.addError(error);
            setDirtyAfterLoad();
        } else {
            // don't set dirty, missing node placeholder
            throw new NodeFactoryUnknownException(error, nodeInfo, additionalFactorySettings, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    NodeSettingsRO loadNCAndModelSettings(final NodeSettingsRO settingsForNode,
        final NodeSettingsRO modelSettings,
        final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec, final LoadResult result)
                throws InvalidSettingsException, CanceledExecutionException, IOException {
        m_nodePersistor = createNodePersistor(settingsForNode);
        m_nodePersistor.preLoad(m_node, result);
        try {
            if (modelSettings != null) { // null if the node never had settings - no reason to load them
                m_node.validateModelSettings(modelSettings);
                m_node.loadModelSettingsFrom(modelSettings);
            }
        } catch (Exception e) {
            final String error;
            if (e instanceof InvalidSettingsException) {
                error = "Loading model settings failed: " + e.getMessage();
            } else {
                error = "Caught \"" + e.getClass().getSimpleName() + "\", " + "Loading model settings failed: "
                        + e.getMessage();
            }
            final LoadNodeModelSettingsFailPolicy pol = getModelSettingsFailPolicy(
                getMetaPersistor().getState(), m_nodePersistor.isInactive());
            switch (pol) {
                case IGNORE:
                    if (!(e instanceof InvalidSettingsException)) {
                        getLogger().coding(error, e);
                    }
                    break;
                case FAIL:
                    result.addError(error);
                    m_node.createErrorMessageAndNotify(Message.fromSummary(error), e);
                    setNeedsResetAfterLoad();
                    break;
                case WARN:
                    m_node.createWarningMessageAndNotify(Message.fromSummary(error), e);
                    result.addWarning(error);
                    setDirtyAfterLoad();
                    break;
            }
        }
        try {
            WorkflowDataRepository dataRepository = getWorkflowDataRepository();
            m_nodePersistor.load(m_node, getParentPersistor(), exec, tblRep, dataRepository,
                result);
        } catch (final Exception e) {
            String error = "Error loading node content: " + e.getMessage();
            getLogger().warn(error, e);
            needsResetAfterLoad();
            result.addError(error);
        }
        if (m_nodePersistor.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }
        if (m_nodePersistor.needsResetAfterLoad()) {
            setNeedsResetAfterLoad();
        }
        return modelSettings;
    }

    /**
     * Load factory name.
     *
     * @param parentSettings settings of outer workflow (old style workflows have it in there)
     * @param settings settings of this node, ignored in this implementation
     * @return Factory information.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    NodeAndBundleInformationPersistor loadNodeFactoryInfo(final NodeSettingsRO parentSettings, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            String factoryName = parentSettings.getString("factory");
            // This is a hack to load old J48 Nodes Model from pre-2.0 workflows
            if ("org.knime.ext.weka.j48_2.WEKAJ48NodeFactory2".equals(factoryName)
                || "org.knime.ext.weka.j48.WEKAJ48NodeFactory".equals(factoryName)) {
                factoryName = "org.knime.ext.weka.knimeJ48.KnimeJ48NodeFactory";
            }
            return new NodeAndBundleInformationPersistor(factoryName);
        } else {
            return NodeAndBundleInformationPersistor.load(settings, getLoadVersion());
        }
    }

    /**
     * Creates the node factory instance for the given fully-qualified factory class name.
     * Otherwise a respective exception will be thrown.
     * @param factoryClassName
     * @return
     * @throws InvalidSettingsException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvalidNodeFactoryExtensionException
     *
     * @since 3.5
     */
    @SuppressWarnings("unchecked")
    public static final NodeFactory<NodeModel> loadNodeFactory(final String factoryClassName)
            throws InvalidSettingsException, InstantiationException, IllegalAccessException,
            InvalidNodeFactoryExtensionException{
        Optional<NodeFactory<NodeModel>> facOptional =
                NodeFactoryProvider.getInstance().getNodeFactory(factoryClassName);
        if (facOptional.isPresent()) {
            return facOptional.get();
        }
        List<NodeFactoryClassMapper> classMapperList = NodeFactoryClassMapper.getRegisteredMappers();
        for (NodeFactoryClassMapper mapper : classMapperList) {
            @SuppressWarnings("rawtypes")
            NodeFactory factory = mapper.mapFactoryClassName(factoryClassName);
            if (factory != null) {
                LOGGER.debug(String.format("Replacing stored factory class name \"%s\" by actual factory "
                    + "class \"%s\" (defined by class mapper \"%s\")", factoryClassName, factory.getClass().getName(),
                    mapper.getClass().getName()));
                return factory;
            }
        }

//        for (String s : NodeFactory.getLoadedNodeFactories()) {
//            if (s.endsWith("." + simpleClassName)) {
//                NodeFactory<NodeModel> f =
//                    (NodeFactory<NodeModel>)((GlobalClassCreator.createClass(s)).newInstance());
//                LOGGER.warn(
//                    "Substituted '" + f.getClass().getName() + "' for unknown factory '" + factoryClassName + "'");
//                return f;
//            }
//        }

        throw new InvalidSettingsException(String.format(
            "Unknown factory class \"%s\" -- not registered via extension point", factoryClassName));
    }

    /**
     * Helper to load a nodes {@link NodeCreationConfiguration}.
     *
     * @param settings the settings the node creation configuration will be initialized with
     * @param factory the node factory get the node creation config from
     * @return the node creation config or an empty optional of the node factory is not of type
     *         {@link ConfigurableNodeFactory}
     * @throws InvalidSettingsException
     * @since 4.2
     */
    public static Optional<ModifiableNodeCreationConfiguration> loadCreationConfig(final NodeSettingsRO settings,
        final NodeFactory<NodeModel> factory) throws InvalidSettingsException {
        if (factory instanceof ConfigurableNodeFactory) {
            final ModifiableNodeCreationConfiguration creationConfig =
                (((ConfigurableNodeFactory<NodeModel>)factory).createNodeCreationConfig());
            creationConfig.loadSettingsFrom(settings);
            return Optional.of(creationConfig);
        }
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    NodeSettingsRO loadSettingsForNode(final LoadResult loadResult) throws IOException {
        NodeSettingsRO nodeSettings = getNodeSettings();
        if (getLoadVersion().ordinal() < LoadVersion.V280.ordinal()) {
            FileNodeContainerMetaPersistor metaPersistor = getMetaPersistor();

            ReferencedFile nodeFile;
            try {
                nodeFile = loadNodeFile(nodeSettings);
            } catch (InvalidSettingsException e) {
                String error = "Unable to load node settings file for node with ID suffix "
                        + metaPersistor.getNodeIDSuffix() + " (node \"" + m_node.getName() + "\"): " + e.getMessage();
                loadResult.addError(error);
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                return new NodeSettings("empty");
            }
            File configFile = nodeFile.getFile();
            if (configFile == null || !configFile.isFile() || !configFile.canRead()) {
                String error = "Unable to read node settings file for node with ID suffix "
                        + metaPersistor.getNodeIDSuffix() + " (node \"" + m_node.getName() + "\"), file \""
                        + configFile + "\"";
                loadResult.addError(error);
                setNeedsResetAfterLoad(); // also implies dirty
                return new NodeSettings("empty");
            } else {
                InputStream in = new FileInputStream(configFile);
                try {
                    in = getParentPersistor().decipherInput(in);
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
            return nodeSettings;
        }
    }

    /**
     * @return the parentPersistor
     */
    WorkflowPersistor getParentPersistor() {
        return m_parentPersistor;
    }

    /**
     * @return the nodePersistor
     */
    FileNodePersistor getNodePersistor() {
        return m_nodePersistor;
    }

    /** {@inheritDoc} */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformationPersistor nodeInfo,
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
                FileNodePersistor nodePersistor = createNodePersistor(settingsForNode);
                outPortTypes = nodePersistor.guessOutputPortTypes(guessLoadResult, nodeName);
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
                            type = PortObject.TYPE;
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
            if (getLoadVersion().ordinal() < FileWorkflowPersistor.VERSION_LATEST.ordinal()) {
                nodefactory.setCopyInternDirForWorkflowVersionChange(true);
            }
            nodefactory.init();
            m_node = new Node((NodeFactory)nodefactory);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PortType getDownstreamPortType(final int index) {
        if (m_node != null && index < m_node.getNrInPorts()) {
            return m_node.getInputType(index);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PortType getUpstreamPortType(final int index) {
        if (m_node != null && index < m_node.getNrOutPorts()) {
            return m_node.getOutputType(index);
        }
        return null;
    }

    static void save(final NativeNodeContainer nnc, final NodeSettingsWO settings,
        final ExecutionMonitor execMon, final ReferencedFile nodeDirRef,
        final boolean isSaveData) throws IOException, CanceledExecutionException {
        saveNodeFactory(settings, nnc);
        saveCreationConfig(settings, nnc.getNode());
        FileNodePersistor.save(nnc, settings, execMon, nodeDirRef,
            isSaveData && nnc.getInternalState().equals(InternalNodeContainerState.EXECUTED));
    }

    private static void saveNodeFactory(final NodeSettingsWO settings, final NativeNodeContainer nnc) {
        final Node node = nnc.getNode();
        // node info to missing node is the info to the actual instance, not MissingNodeFactory
        NodeAndBundleInformationPersistor nodeInfo = nnc.getNodeAndBundleInformation();
        nodeInfo.save(settings);

        // The factory-id-uniquifier is only saved but never loaded! It's restored by the
        // respective node-implementation based on the 'additional factory settings'.
        // It's persisted such that the complete factory-id can be re-created without the need to
        // instantiate the node factory but just by reading the node settings.
        var factory = node.getFactory();
        if (factory instanceof DynamicNodeFactory dynamicNodeFactory) {
            settings.addString("factory-id-uniquifier", dynamicNodeFactory.getFactoryIdUniquifier());
        }

        NodeSettingsWO subSets = settings.addNodeSettings("factory_settings");
        factory.saveAdditionalFactorySettings(subSets);
    }

    private static void saveCreationConfig(final NodeSettingsWO settings, final Node node) {
        node.getCopyOfCreationConfig().ifPresent(config -> config.saveSettingsTo(settings));
    }

}
