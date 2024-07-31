/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   20 Apr 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.def;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactoryClassMapper;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.extension.InvalidNodeFactoryExtensionException;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.missing.MissingNodeFactory;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.node.workflow.ComponentMetadata;
import org.knime.core.node.workflow.ComponentMetadata.ComponentNodeType;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.core.node.workflow.FileNativeNodeContainerPersistor;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeContainerMetadata.ContentType;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.shared.workflow.def.AnnotationDataDef;
import org.knime.shared.workflow.def.AuthorInformationDef;
import org.knime.shared.workflow.def.BaseNodeDef;
import org.knime.shared.workflow.def.BaseNodeDef.NodeTypeEnum;
import org.knime.shared.workflow.def.ComponentMetadataDef;
import org.knime.shared.workflow.def.ComponentNodeDef;
import org.knime.shared.workflow.def.ConfigDef;
import org.knime.shared.workflow.def.ConfigMapDef;
import org.knime.shared.workflow.def.ConnectionDef;
import org.knime.shared.workflow.def.ConnectionUISettingsDef;
import org.knime.shared.workflow.def.FlowVariableDef;
import org.knime.shared.workflow.def.JobManagerDef;
import org.knime.shared.workflow.def.MetaNodeDef;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.knime.shared.workflow.def.NodeLocksDef;
import org.knime.shared.workflow.def.NodeUIInfoDef;
import org.knime.shared.workflow.def.PortDef;
import org.knime.shared.workflow.def.PortTypeDef;
import org.knime.shared.workflow.def.WorkflowUISettingsDef;
import org.knime.shared.workflow.storage.multidir.util.LoaderUtils;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
public class DefToCoreUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefToCoreUtil.class);

    /**
     * Sets annotation data def to AnnotationData
     *
     * @param annoData a {@link AnnotationData}
     * @param def {@link AnnotationDataDef}
     * @return an {@link AnnotationData}
     */
    public static <E extends AnnotationData> E toAnnotationData(final E annoData, final AnnotationDataDef def) {
        annoData.setAlignment(TextAlignment.valueOf(def.getTextAlignment()));
        annoData.setBgColor(def.getBgcolor());
        annoData.setBorderColor(def.getBorderColor());
        annoData.setBorderSize(def.getBorderSize());
        annoData.setDefaultFontSize(def.getDefaultFontSize());
        annoData.setX(def.getLocation().getX());
        annoData.setY(def.getLocation().getY());
        annoData.setWidth(def.getWidth());
        annoData.setHeight(def.getHeight());
        annoData.setText(def.getText());
        annoData.setContentType(def.getContentType());
        annoData.setStyleRanges(def.getStyles());
        return annoData;
    }

    /**
     * Converts a native node def to Node
     *
     * @param def a {@link NativeNodeDef}
     * @return a {@link Node}
     * @throws RuntimeException if the node factory is not found or there was an error during instantiation
     */
    public static Node toNode(final NativeNodeDef def) {
        try {
            return instantiateNode(def, loadNodeFactory(def.getFactory()));
        } catch (InstantiationException | IllegalAccessException | InvalidSettingsException
                | InvalidNodeFactoryExtensionException ex) {
            // TODO currently not doing any type of error handling (unknown node), done as part of AP-18960
            throw new RuntimeException(ex);
        }
    }

    /**
     * Tries to convert a native node def to a Node
     * @param def the definition of the node
     * @return a {@link Node} instance, or {@link Optional#empty()} if the node factory cannot be found (missing node)
     * @throws RuntimeException if there was an error instantiating the node factory, but the node factory was found
     */
    public static Optional<Node> tryToNode(final NativeNodeDef def) {
        try {
            final var optFac = tryLoadNodeFactory(def.getFactory());
            return optFac.map(fac -> instantiateNode(def, fac));
        } catch (InstantiationException | IllegalAccessException | InvalidNodeFactoryExtensionException ex) {
            // TODO currently not doing any type of error handling (unknown node), done as part of AP-18960
            throw new RuntimeException(ex);
        }
    }

    private static Node instantiateNode(final NativeNodeDef def, final NodeFactory<NodeModel> fac) {
        NodeSettingsRO nodeCreationSettings;
        try {
            NodeSettingsRO additionalFactorySettings = toNodeSettings(def.getFactorySettings());
            nodeCreationSettings = toNodeSettings(def.getNodeCreationConfig());
            fac.loadAdditionalFactorySettings(additionalFactorySettings);
        } catch (InvalidSettingsException e) {
            // TODO currently not doing any type of error handling (unknown node), done as part of AP-18960
            throw new RuntimeException(e);
        }
        Node node;
        try {
            node = new Node(fac, loadCreationConfig(nodeCreationSettings, fac).orElse(null));
        } catch (InvalidSettingsException e) {
            throw new RuntimeException(e);
        }
        node = FileNativeNodeContainerPersistor.replaceNodeByMissingNodeIfUsageIsDisallowed(node, nodeCreationSettings);
        return node;
    }

    /**
     *  Converts the job manager def to NodeExceution job manager.
     *
     * @param def a {@link JobManagerDef}.
     * @return a {@link NodeExecutionJobManager}.
     */
    public static NodeExecutionJobManager toJobManager(final JobManagerDef def) {
        if (def == null) {
            return null;
        }
        var jobManagerId = def.getFactory();
        NodeExecutionJobManagerFactory reference = NodeExecutionJobManagerPool.getJobManagerFactory(jobManagerId);
        if (reference == null) {
            final var msg = String
                .format("Unknown job manager factory id %s (job manager factory possibly not installed?", jobManagerId);
            LOGGER.error(msg);
            return null;
        }
        var settings = toNodeSettings(def.getSettings());

        NodeExecutionJobManager jobManager = reference.getInstance();
        try {
            jobManager.load(settings);
        } catch (InvalidSettingsException ex) {
            final var msg =
                String.format("Can't load the job manager with factory id %s: %s", jobManagerId, ex.getMessage());
            LOGGER.error(msg, ex);
        }
        return jobManager;
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
    private static Optional<ModifiableNodeCreationConfiguration> loadCreationConfig(final NodeSettingsRO settings,
        final NodeFactory<NodeModel> factory) throws InvalidSettingsException {
        if (factory instanceof ConfigurableNodeFactory) {
            final ModifiableNodeCreationConfiguration creationConfig =
                (((ConfigurableNodeFactory<NodeModel>)factory).createNodeCreationConfig());
            try {
                creationConfig.loadSettingsFrom(settings);
            } catch (final InvalidSettingsException e) {
                throw new InvalidSettingsException("Unable to load creation context", e.getCause());
            }
            return Optional.of(creationConfig);
        }
        return Optional.empty();
    }

    /**
     * Creates the node factory instance for the given fully-qualified factory class name.
     * Otherwise a respective exception will be thrown.
     *
     * @since 3.5
     */
    private static final NodeFactory<NodeModel> loadNodeFactory(final String factoryClassName)
        throws InvalidSettingsException, InstantiationException, IllegalAccessException,
        InvalidNodeFactoryExtensionException {
        return tryLoadNodeFactory(factoryClassName).orElseThrow(() -> new InvalidSettingsException(
            String.format("Unknown factory class \"%s\" -- not registered via extension point", factoryClassName)));
    }

    /**
     * Creates the node factory instance for the given fully-qualified factory class name if possible.
     * Otherwise, an empty {@linkplain Optional} is returned.
     *
     * @since 5.4
     */
    @SuppressWarnings("unchecked") // Casting the node factory to NodeFactory<NodeModel> to comply with return type
    private static final Optional<NodeFactory<NodeModel>> tryLoadNodeFactory(final String factoryClassName)
        throws InstantiationException, IllegalAccessException, InvalidNodeFactoryExtensionException {
        return NodeFactoryProvider.getInstance().getNodeFactory(factoryClassName) //
            .or(() -> { // NOSONAR
                for (final var mapper : NodeFactoryClassMapper.getRegisteredMappers()) {
                    final var factory = mapper.mapFactoryClassName(factoryClassName);
                    if (factory != null) {
                        LOGGER.debug(String.format(
                            "Replacing stored factory class name \"%s\" by actual factory "
                                + "class \"%s\" (defined by class mapper \"%s\")",
                            factoryClassName, factory.getClass().getName(), mapper.getClass().getName()));
                        return Optional.of((NodeFactory<NodeModel>)factory);
                    }
                }
                return Optional.empty();
            });
    }

    /**
     * Converts a port type def to PortType.
     *
     * @param portType a {@link PortTypeDef}
     * @return a {@link PortType}
     */
    public static PortType toPortType(final PortTypeDef portType) {
        var objectClassString = portType.getPortObjectClass();
        if (objectClassString == null) {
            // TODO VWR proper error handling or eliminate code paths that lead here
            throw new RuntimeException("No port object class found to create PortType object");
        }
        Class<? extends PortObject> obClass =
            PortTypeRegistry.getInstance().getObjectClass(objectClassString).orElseThrow(() -> new RuntimeException(
                "Unable to restore port type, " + "can't load class \"" + objectClassString + "\""));
        return PortTypeRegistry.getInstance().getPortType(obClass, portType.isOptional());

    }

    /**
     * Converts a Component MetaData def to ComponentMetadata.
     *
     * @param def {@link ComponentMetadataDef}.
     * @return a {@link ComponentMetadata}.
     */
    public static ComponentMetadata toComponentMetadata(final ComponentMetadataDef def) {
        final var builder = ComponentMetadata.fluentBuilder()
                .withIcon(def.getIcon())
                .withComponentType(toComponentNodeType(def.getComponentType()));

        if (def.getInPortNames() != null) {
            for (var i = 0; i < def.getInPortNames().size(); i++) {
                builder.withInPort(def.getInPortNames().get(i), def.getInPortDescriptions().get(i));
            }
        }

        if (def.getOutPortNames() != null) {
            for (var i = 0; i < def.getOutPortNames().size(); i++) {
                builder.withOutPort(def.getOutPortNames().get(i), def.getOutPortDescriptions().get(i));
            }
        }

        final var contentType = switch (def.getContentType()) {
            case PLAIN -> ContentType.PLAIN;
            case HTML -> ContentType.HTML;
        };

        final var baseBuilder = builder //
                .withContentType(contentType) //
                .withLastModified(def.getLastModified().toZonedDateTime()) //
                .withDescription(def.getDescription()) //
                .withAuthor(def.getAuthor()) //
                .withCreated(Optional.ofNullable(def.getCreated()).map(OffsetDateTime::toZonedDateTime).orElse(null));

        Optional.ofNullable(def.getLinks()).orElse(List.of()).stream() //
                .forEach(link -> baseBuilder.addLink(link.getUrl(), link.getText()));
        Optional.ofNullable(def.getTags()).orElse(List.of()).stream().forEach(baseBuilder::addTag);

        return baseBuilder.build();
    }

    /**
     * @param defType nullable
     * @return equivalent core enum value or null if given null
     */
    public static ComponentNodeType toComponentNodeType(final ComponentMetadataDef.ComponentTypeEnum defType) {
        if(defType == null) {
            return null;
        }
        return ComponentNodeType.valueOf(defType.toString());
    }

    /**
     * Converts a Workflow UI settings def to EditorUIInformation.
     *
     * @param def {@link WorkflowUISettingsDef}.
     * @return a {@link EditorUIInformation}.
     */
    public static EditorUIInformation toEditorUIInformation(final WorkflowUISettingsDef def) {

        if(def == null) {
            return EditorUIInformation.builder().build();
        }

        return EditorUIInformation.builder()//
            .setGridX(def.getGridX())//
            .setGridY(def.getGridY())//
            .setConnectionLineWidth(def.getConnectionLineWidth())
            .setHasCurvedConnections(def.isCurvedConnections())//
            .setShowGrid(def.isShowGrid())//
            .setSnapToGrid(def.isSnapToGrid())//
            .setZoomLevel(def.getZoomLevel().doubleValue())//
            .build();
    }

    /**
     * Converts an author information def to Author information.
     *
     * @param def {@link AuthorInformationDef}.
     * @return a {@link AuthorInformation}.
     */
    public static AuthorInformation toAuthorInformation(final AuthorInformationDef def) {
        final var lastEdited =
            Optional.ofNullable(def.getLastEditedWhen());
        final var authored = Optional.ofNullable(def.getAuthoredWhen());
        return new AuthorInformation(def.getAuthoredBy(), authored.orElse(null), def.getLastEditedBy(),
            lastEdited.orElse(null));
    }

    /**
     * Create a node settings tree (comprising {@link AbstractConfigEntry}s) from a {@link ConfigDef} tree.
     *
     * @param def an entity containing the recursive node settings
     * @return the same data in the format that, e.g., node models expect
     */
    public static NodeSettings toNodeSettings(final ConfigMapDef def) {
        return toNodeSettings(def, PasswordRedactor.unsafe());
    }

    /**
     *
     * @param def an entity containing the recursive node settings
     * @param passwordHandler for restoring passwords that have been redacted during
     *            {@link LoaderUtils#toConfigBase(ConfigDef, PasswordRedactor)}
     * @return the same data in the format that, e.g., node models expect
     */
    public static NodeSettings toNodeSettings(final ConfigMapDef def, final PasswordRedactor passwordHandler) {
        // TODO should def be allowed to be null here?
        return def == null ? null : LoaderUtils.toSettings(def, def.getKey(), NodeSettings::new, passwordHandler);
    }

    /**
     * Returning null makes sense here, because a metanode's workflow manager expects null in case the position of the
     * in/outport bars should be automatically chosen.
     *
     * @param uiInfoDef def describing the location and size of a node or metanode in/outport bar
     * @return null if the given def is null, the converted information otherwise.
     * @see WorkflowManager#setInPortsBarUIInfo(NodeUIInformation)
     */
    public static NodeUIInformation toNodeUIInformation(final NodeUIInfoDef uiInfoDef) {
        if (uiInfoDef == null) {
            // if the node ui information is null on a workflow manager that represents a metanode, the ui will figure
            // out sensible default values.
            return null;
        }

        var boundsDef = uiInfoDef.getBounds();
        return NodeUIInformation.builder() //
            .setHasAbsoluteCoordinates(uiInfoDef.hasAbsoluteCoordinates()) //
            .setIsDropLocation(false) // is only used to specify the location of a node and the shape of metanode bars
            .setIsSymbolRelative(uiInfoDef.isSymbolRelative())//
            .setNodeLocation(//
                boundsDef.getLocation().getX(), boundsDef.getLocation().getY(), //
                boundsDef.getWidth(), boundsDef.getHeight()) //
            .setSnapToGrid(false) // is only used to specify the location of a node and the shape of metanode bars
            .build();
    }

    public static NodeLocks toNodeLocks(final NodeLocksDef def) {
        return new NodeLocks(def.hasDeleteLock(), def.hasResetLock(), def.hasConfigureLock());
    }

    /**
     * TODO duplicated code
     *
     * @param flowContextDef
     * @return
     */
//    public static FlowScopeContext toFlowContext(final FlowContextDef def) {
//        if ("loopcontext".equals(def.getContextType())) {
//            return new RestoredFlowLoopContext();
//            //  TODO              int tailID = sub.getInt("tailID");
//        } else if ("loopcontext_execute".equals(def.getContextType())) {
//            return null; // TODO new InnerFlowLoopContext());
//        } else if ("loopcontext_inactive".equals(def.getContextType())) {
//            FlowLoopContext flc = new FlowLoopContext();
//            flc.inactiveScope(true);
//            return flc;
//        } else if ("flowcapturecontext".equals(def.getContextType())) {
//            return new FlowCaptureContext();
//        } else if ("flowcapturecontext_inactive".equals(def.getContextType())) {
//            FlowScopeContext slc = new FlowCaptureContext();
//            slc.inactiveScope(true);
//            return slc;
//        } else if ("scopecontext".equals(def.getContextType())) {
//            return new FlowScopeContext();
//        } else if ("scopecontext_inactive".equals(def.getContextType())) {
//            FlowScopeContext slc = new FlowScopeContext();
//            slc.inactiveScope(true);
//            return slc;
//        } else {
//            throw new IllegalArgumentException("Unknown flow object type: " + def.getContextType());
//        }
//
//    }

    /**
     * @param def
     * @return
     */
    public static FlowVariable toFlowVariable(final FlowVariableDef def) {
        // FlowVariable subtypes (e.g. VariableValue) and constructors (new FlowVariable(name, value, scope)
        // are not visible, so we can't build a more explicit schema like this w/o changing visibility
        //        final String identifier = def.getObjectClass();
        //        VariableType<?>[] a = VariableTypeRegistry.getInstance().getAllTypes();
        //
        //        final VariableType<?> type = Arrays.stream(a)//
        //            .filter(t -> identifier.equals(t.getIdentifier()))//
        //            .findFirst()//
        //            .orElseThrow(() -> new InvalidSettingsException(
        //                String.format("No flow variable type for identifier/class '%s'", identifier)));
        //        var value = type.loadValue(toNodeSettings(def.getValue()));
        //        FlowVariable.lo
        try {
            // TODO fix to ConfigMapDef (AP-18944)
//            return FlowVariable.load(toNodeSettings(def.getValue()));
            throw new InvalidSettingsException("Loading flow variables not supported");
        } catch (InvalidSettingsException ex) {
            throw new IllegalArgumentException("Can not load flow variable from " + def, ex);
        }
    }

    public static AnnotationData toAnnotationData(final AnnotationDataDef dataDef) {
        var annotationData = new AnnotationData();
        return toAnnotationData(annotationData, dataDef);
    }

    /**
     * @param uiSettings
     * @return connection settings that describe the bend points of the line that is used to represent it
     */
    public static ConnectionUIInformation toConnectionUIInformation(final ConnectionUISettingsDef uiSettings) {
        var resultBuilder = ConnectionUIInformation.builder();
        if (uiSettings != null && uiSettings.getBendPoints() != null) {
            int[][] bendPoints = uiSettings.getBendPoints().stream()//
                .map(coordinate -> new int[]{coordinate.getX(), coordinate.getY()})//
                .toArray(int[][]::new);
            resultBuilder.setBendpoints(bendPoints);
        }
        return resultBuilder.build();
    }

    /**
     * @param baseNodeDef description of a native node, metanode, or component
     * @return the configured input port types if the node is a component or metanode, an empty array otherwise.
     */
    public static PortType[] extractInPortTypes(final BaseNodeDef baseNodeDef) {
        Supplier<List<PortDef>> inPortsGetter = null;
        if (NodeTypeEnum.METANODE.equals(baseNodeDef.getNodeType())) {
            inPortsGetter = ((MetaNodeDef)baseNodeDef)::getInPorts;
        } else if (NodeTypeEnum.COMPONENT.equals(baseNodeDef.getNodeType())) {
            inPortsGetter = ((ComponentNodeDef)baseNodeDef)::getInPorts;
        }
        var inportsListSupplier = Optional.ofNullable(inPortsGetter).orElse(List::of);
        return inportsListSupplier.get().stream()//
            .map(pd -> pd.getPortType())//
            .map(DefToCoreUtil::toPortType)//
            .toArray(PortType[]::new);

    }

    /**
     * @param baseNodeDef description of a native node, metanode, or component
     * @return the configured output port types if the node is a component or metanode, an empty array otherwise.
     */
    public static PortType[] extractOutPortTypes(final BaseNodeDef baseNodeDef) {
        Supplier<List<PortDef>> outPortsGetter = null;
        if (NodeTypeEnum.METANODE.equals(baseNodeDef.getNodeType())) {
            outPortsGetter = ((MetaNodeDef)baseNodeDef)::getOutPorts;
        } else if (NodeTypeEnum.COMPONENT.equals(baseNodeDef.getNodeType())) {
            outPortsGetter = ((ComponentNodeDef)baseNodeDef)::getOutPorts;
        }
        var outportsListSupplier = Optional.ofNullable(outPortsGetter).orElse(List::of);
        return outportsListSupplier.get().stream()//
            .map(pd -> pd.getPortType())//
            .map(DefToCoreUtil::toPortType)//
            .toArray(PortType[]::new);

    }

    /**
     * Create a missing node factory from a pasted {@link NativeNodeDef}.
     * The port number and port types are guessed from a list of connections.
     *
     * @param parent the {@link WorkflowManager} that provides context to the up- and downstream nodes.
     * @param nodeId for the node that shall be added
     * @param def the spec of the node
     * @param connectionDefs list of connections
     * @param translationMap translation between former and pasted nodeIDs
     * @return a {@link MissingNodeFactory} that is a stand-in for the provided node spec
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // cast of return value
    public static NodeFactory<NodeModel> createMissingNodeFactory(final WorkflowManager parent, // NOSONAR
        final NodeID nodeId, final NativeNodeDef def, final Iterable<ConnectionDef> connectionDefs,
        final Map<Integer, NodeID> translationMap) {
        // Collect in and out connections of the node in question
        final SortedMap<Integer, List<ConnectionDef>> outConnections = new TreeMap<>();
        final SortedMap<Integer, ConnectionDef> inConnections = new TreeMap<>();
        for (final var conn : connectionDefs) {
            if (conn.getSourceID().equals(def.getId())) {
                outConnections.computeIfAbsent(conn.getSourcePort(), k -> new ArrayList<>()).add(conn);
            } else if (conn.getDestID().equals(def.getId())) {
                inConnections.put(conn.getDestPort(), conn);
            }
        }

        final var parentId = nodeId.getPrefix();

        // Guess in port types
        final var nrInPorts = inConnections.isEmpty() ? 0 : inConnections.lastKey();
        final var inPortTypes = new PortType[nrInPorts]; // does not include flow var port
        Arrays.fill(inPortTypes, BufferedDataTable.TYPE); // the file node persistor also does it this way.
                                                                // Furthermore, if there are multiple, chained, missing
                                                                // nodes, the default out- and in connections should
                                                                // be the same to preserve connections.
        for (final var inConnEntry : inConnections.tailMap(1).entrySet()) { // all connections after the flow var port
            final var portIdxHere = inConnEntry.getKey() - 1; // port index (first data port is 0)
            final var connection = inConnEntry.getValue();
            final var sourceId = connection.getSourceID();
            final var portIdxUpstream = connection.getSourcePort();
            final var upstream = translationMap.getOrDefault(sourceId, new NodeID(parentId, sourceId));
            final var optPortType = parent.tryGetNodeContainer(upstream) //
                .filter(node -> portIdxUpstream >= 0 && portIdxUpstream < node.getNrOutPorts()) //
                .map(node -> node.getOutPort(portIdxUpstream)) //
                .map(NodePort::getPortType);
            if (optPortType.isPresent()) {
                inPortTypes[portIdxHere] = optPortType.get();
            }
        }

        // Guess out port types
        final var nrOutPorts = outConnections.isEmpty() ? 0 : outConnections.lastKey();
        final var outPortTypes = new PortType[nrOutPorts];
        Arrays.fill(outPortTypes, BufferedDataTable.TYPE); // default to BDT for unconnected ports
        for (final var outConnEntry : outConnections.tailMap(1).entrySet()) { // all connections after the flow var port
            final var portIdxHere = outConnEntry.getKey() - 1; // port index (first data port is 0)
            final var list = outConnEntry.getValue();
            final Set<PortType> candidates = new HashSet<>();
            // iterate over all downstream nodes from the out port
            for (ConnectionDef connection : list) {
                final var destinationId = connection.getDestID();
                final var portIdxDownstream = connection.getDestPort();
                final var downstream = translationMap.getOrDefault(destinationId, new NodeID(parentId, destinationId));
                parent.tryGetNodeContainer(downstream) //
                    .filter(node -> portIdxDownstream >= 0 && portIdxDownstream < node.getNrOutPorts()) //
                    .map(node -> node.getOutPort(portIdxDownstream)) //
                    .map(NodePort::getPortType) //
                    .ifPresent(candidates::add);
            }

            final var compatibleWithAll = candidates.stream() //
                .filter(tp -> candidates.stream().allMatch(other -> other.isSuperTypeOf(tp))) //
                .findAny();
            if (compatibleWithAll.isPresent()) {
                outPortTypes[portIdxHere] = compatibleWithAll.get();
            }
        }

        // collect node and bundle information stored with the node
        final var nodeInfo = NodeAndBundleInformationPersistor.load(def.getNodeName(), def.getFeature(),
            def.getBundle(), def.getFactory());
        NodeSettingsRO additionalFactorySettings = DefToCoreUtil.toNodeSettings(def.getFactorySettings());
        final var nodeFactory = new MissingNodeFactory(nodeInfo, additionalFactorySettings, inPortTypes, outPortTypes);
        nodeFactory.init();
        return (NodeFactory)nodeFactory; // cast of MissingNodeFactory to NodeFactory
    }
}
