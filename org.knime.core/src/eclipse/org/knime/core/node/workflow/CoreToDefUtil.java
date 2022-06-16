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
 *   May 19, 2021 (hornm): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ComponentMetadata.ComponentNodeType;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.WorkflowPersistor.ConnectionContainerTemplate;
import org.knime.core.util.Version;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.shared.workflow.def.AnnotationDataDef;
import org.knime.shared.workflow.def.AuthorInformationDef;
import org.knime.shared.workflow.def.BaseNodeDef;
import org.knime.shared.workflow.def.BoundsDef;
import org.knime.shared.workflow.def.ComponentMetadataDef;
import org.knime.shared.workflow.def.ConnectionDef;
import org.knime.shared.workflow.def.ConnectionUISettingsDef;
import org.knime.shared.workflow.def.CoordinateDef;
import org.knime.shared.workflow.def.CredentialPlaceholderDef;
import org.knime.shared.workflow.def.JobManagerDef;
import org.knime.shared.workflow.def.NodeAnnotationDef;
import org.knime.shared.workflow.def.NodeLocksDef;
import org.knime.shared.workflow.def.NodeUIInfoDef;
import org.knime.shared.workflow.def.PortDef;
import org.knime.shared.workflow.def.PortMetadataDef;
import org.knime.shared.workflow.def.PortTypeDef;
import org.knime.shared.workflow.def.StyleRangeDef;
import org.knime.shared.workflow.def.TemplateLinkDef;
import org.knime.shared.workflow.def.TemplateMetadataDef;
import org.knime.shared.workflow.def.VendorDef;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.shared.workflow.def.impl.AuthorInformationDefBuilder;
import org.knime.shared.workflow.def.impl.BoundsDefBuilder;
import org.knime.shared.workflow.def.impl.ComponentMetadataDefBuilder;
import org.knime.shared.workflow.def.impl.ComponentNodeDefBuilder;
import org.knime.shared.workflow.def.impl.ConnectionDefBuilder;
import org.knime.shared.workflow.def.impl.ConnectionUISettingsDefBuilder;
import org.knime.shared.workflow.def.impl.CoordinateDefBuilder;
import org.knime.shared.workflow.def.impl.JobManagerDefBuilder;
import org.knime.shared.workflow.def.impl.MetaNodeDefBuilder;
import org.knime.shared.workflow.def.impl.NativeNodeDefBuilder;
import org.knime.shared.workflow.def.impl.NodeAnnotationDefBuilder;
import org.knime.shared.workflow.def.impl.NodeLocksDefBuilder;
import org.knime.shared.workflow.def.impl.NodeUIInfoDefBuilder;
import org.knime.shared.workflow.def.impl.PortDefBuilder;
import org.knime.shared.workflow.def.impl.PortMetadataDefBuilder;
import org.knime.shared.workflow.def.impl.PortTypeDefBuilder;
import org.knime.shared.workflow.def.impl.StyleRangeDefBuilder;
import org.knime.shared.workflow.def.impl.TemplateLinkDefBuilder;
import org.knime.shared.workflow.def.impl.TemplateMetadataDefBuilder;
import org.knime.shared.workflow.def.impl.VendorDefBuilder;
import org.knime.shared.workflow.def.impl.WorkflowDefBuilder;
import org.knime.shared.workflow.storage.multidir.util.LoaderUtils;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Dionysios Stolis, KNIME AG, Zurich, Switzerland
 * @author Martin Horn, KNIME AG, Zurich, Switzerland
 */
public class CoreToDefUtil {

    /**
     *  Converts the ConnectionContainer to def.
     *
     * @param connection a {@link ConnectionContainer}.
     * @return a {@link ConnectionDef}.L
     */
    public static ConnectionDef connectionContainerToConnectionDef(final ConnectionContainer connection) {
        final var uiInfo = Optional.ofNullable(connection.getUIInfo())//
            .map(CoreToDefUtil::toConnectionUISettingsDef)//
            .orElse(null);

        int sourceID = connection.getSource().getIndex();
        int destID = connection.getDest().getIndex();
        switch (connection.getType()) {
            case WFMIN:
                sourceID = -1;
                break;
            case WFMOUT:
                destID = -1;
                break;
            case WFMTHROUGH:
                sourceID = -1;
                destID = -1;
                break;
            default:
                // all handled above
        }

        return new ConnectionDefBuilder()//
            .setSourcePort(connection.getSourcePort())//
            .setSourceID(sourceID)//
            .setDestPort(connection.getDestPort())//
            .setDestID(destID)//
            .setUiSettings(uiInfo)//
            .setDeletable(connection.isDeletable())//
            .build();
    }

    /**
     *
     * @param cct
     * @return
     */
    public static ConnectionDef toConnectionDef(final ConnectionContainerTemplate cct) {

        return new ConnectionDefBuilder()//
                .setDeletable(cct.isDeletable())//
                .setDestID(cct.getDestSuffix())//
                .setDestPort(cct.getDestPort())//
                .setSourceID(cct.getSourceSuffix())//
                .setSourcePort(cct.getSourcePort())//
                .setUiSettings(toConnectionUISettingsDef(cct.getUiInfo()))//
                .build();
    }

    /**
     *
     * @param connectionUIInformation
     * @return
     */
    public static ConnectionUISettingsDef
        toConnectionUISettingsDef(final ConnectionUIInformation connectionUIInformation) {
        if (connectionUIInformation == null) {
            return new ConnectionUISettingsDefBuilder().build();
        }
        List<CoordinateDef> bendPoints = Arrays.stream(connectionUIInformation.getAllBendpoints())//
            .map(p -> createCoordinate(p[0], p[1]))//
            .collect(Collectors.toList());
        return new ConnectionUISettingsDefBuilder()//
            .setBendPoints(bendPoints).build();
    }

    public static NodeUIInfoDef toNodeUIInfoDef(final NodeUIInformation uiInfoDef) {

        if (uiInfoDef == null) {
            return null;
        }

        int[] bounds = uiInfoDef.getBounds();
        BoundsDef boundsDef = new BoundsDefBuilder()//
            .setLocation(createCoordinate(bounds[0], bounds[1]))//
            .setWidth(bounds[2])//
            .setHeight(bounds[3])//
            .build();

        return new NodeUIInfoDefBuilder()//
            .setBounds(boundsDef)//
//            .setHasAbsoluteCoordinates(uiInfoDef.hasAbsoluteCoordinates())//
//            .setSymbolRelative(uiInfoDef.isSymbolRelative())//
            .build();
    }

    public static NodeLocksDef toNodeLocksDef(final NodeLocks def) {
        return new NodeLocksDefBuilder()//
            .setHasConfigureLock(def.hasConfigureLock())//
            .setHasDeleteLock(def.hasDeleteLock())//
            .setHasResetLock(def.hasResetLock())//
            .build();
    }

    public static AnnotationDataDef toAnnotationDataDef(final Annotation na) {
        List<StyleRangeDef> styles = Arrays.stream(na.getStyleRanges())
            .map(s -> new StyleRangeDefBuilder().setColor(s.getFgColor()).setFontName(s.getFontName())
                .setFontSize(s.getFontSize()).setFontStyle(s.getFontStyle()).setLength(s.getLength())
                .setStart(s.getStart()).build())
            .collect(Collectors.toList());

        return new AnnotationDataDefBuilder()//
            .setText(na.getText())//
            .setTextAlignment(na.getAlignment().toString())//
            .setBgcolor(na.getBgColor())//
            .setBorderColor(na.getBorderColor())//
            .setBorderSize(na.getBorderSize())//
            .setDefaultFontSize(na.getDefaultFontSize())//
            .setHeight(na.getHeight())//
            .setWidth(na.getWidth())//
            .setLocation(createCoordinate(na.getX(), na.getY()))//
            .setStyles(styles)//
            .build();
    }

    public static NodeAnnotationDef toNodeAnnotationDef(final NodeAnnotation na) {
        return new NodeAnnotationDefBuilder()//
            .setAnnotationDefault(na.getData().isDefault())//
            .setData(toAnnotationDataDef(na))//
            .build();
    }

    public static CoordinateDef createCoordinate(final int x, final int y) {
        return new CoordinateDefBuilder().setX(x).setY(y).build();

    }

    /**
     * @param info contains either template metadata (role = template and uri == null) or a template link (uri != null)
     * @return non-empty if the given object describes a template
     */
    public static Optional<TemplateMetadataDef> toTemplateMetadataDef(final MetaNodeTemplateInformation info) {
        if (info.getSourceURI() != null) {
            return Optional.empty();
        }
        return Optional.of(new TemplateMetadataDefBuilder()//
            .setVersion(info.getTimestamp())//
            .build());
    }

    /**
     * @param info contains either template metadata (role = template and uri == null) or a template link (uri != null)
     * @return non-empty if the given object describes a link to a template
     */
    public static Optional<TemplateLinkDef> toTemplateLinkDef(final MetaNodeTemplateInformation info) {
        if (info.getSourceURI() == null) {
            return Optional.empty();
        }
        return Optional.of(new TemplateLinkDefBuilder() //
            .setVersion(info.getTimestamp()) //
            .setUri(info.getSourceURI().toString()) //
            .build());
    }

    /**
     * @param p
     * @return
     */
    public static PortDef toPortDef(final NodePort p) {
        PortTypeDef portType = new PortTypeDefBuilder()//
                .setColor(p.getPortType().getColor())//
                .setHidden(p.getPortType().isHidden())//
                .setName(p.getPortType().getName())//
                .setOptional(p.getPortType().isOptional())//
                .setPortObjectClass(p.getPortType().getPortObjectClass().getCanonicalName())//
                .setPortObjectSpecClass(p.getPortType().getPortObjectSpecClass().getCanonicalName())//
                .build();

        return new PortDefBuilder()//
            .setIndex(p.getPortIndex())//
            .setName(p.getPortName())//
            .setPortType(portType)//
            .build();
    }

    public static VendorDef toBundleVendorDef(final NodeAndBundleInformationPersistor p) {
        return new VendorDefBuilder()//
            .setName(p.getBundleName().orElse(null))//
            .setSymbolicName(p.getBundleSymbolicName().orElse(null))//
            .setVendor(p.getBundleVendor().orElse(null))//
            .setVersion(p.getBundleVersion().map(Version::toString).orElse(null))//
            .build();

    }
    public static VendorDef toFeatureVendorDef(final NodeAndBundleInformationPersistor p) {
        return new VendorDefBuilder()//
            .setName(p.getFeatureName().orElse(null))//
            .setSymbolicName(p.getFeatureSymbolicName().orElse(null))//
            .setVendor(p.getFeatureVendor().orElse(null))//
            .setVersion(p.getFeatureVersion().map(Version::toString).orElse(null))//
            .build();
    }

    public static ComponentMetadataDef toComponentMetadataDef(final ComponentMetadata m) {
        var builder = new ComponentMetadataDefBuilder();
        m.getNodeType().map(CoreToDefUtil::toComponentNodeType).ifPresent(builder::setComponentType);
        return builder//
            .setDescription(m.getDescription().orElse(null))//
            .setIcon(m.getIcon().orElse(null))//
            .setInPortMetadata(toPortMetadata(m.getInPortNames().orElse(new String[0]),
                m.getInPortDescriptions().orElse(new String[0])))//
            .setOutPortMetadata(toPortMetadata(m.getOutPortNames().orElse(new String[0]),
                m.getOutPortDescriptions().orElse(new String[0])))//
            .build();
    }

    /**
     * Combines names and descriptions.
     *
     * @param names non-null but possibly empty
     * @param descriptions non-null but possibly empty
     * @return null if both arrays are empty
     */
    private static List<PortMetadataDef> toPortMetadata(final String[] names, final String[] descriptions) {
        List<PortMetadataDef> result = new ArrayList<>();
        for (var i = 0; i < Math.max(names.length, descriptions.length); i++) {
            var name = i < names.length ? names[i] : null;
            var description = i < descriptions.length ? descriptions[i] : null;
            result.add(new PortMetadataDefBuilder().setName(name).setDescription(description).build());
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * @param coreType not null
     * @return equivalent enum value
     */
    static ComponentMetadataDef.ComponentTypeEnum toComponentNodeType(final ComponentNodeType coreType) {
        return ComponentMetadataDef.ComponentTypeEnum.valueOf(coreType.toString());
    }

    /**
     * @param jobManager
     * @param passwordHandler
     * @return
     */
    public static JobManagerDef toJobManager(final NodeExecutionJobManager jobManager,
        final PasswordRedactor passwordHandler) {
        if (jobManager == null) {
            return null;
        }

        final NodeSettings ns = new NodeSettings("jobmanager");
        jobManager.save(ns);

        try {
            return new JobManagerDefBuilder()//
                .setFactory(jobManager.getID())//
                .setSettings(LoaderUtils.toConfigMapDef(ns, passwordHandler))//
                .build();
        } catch (InvalidSettingsException ex) {
            // TODO proper exception handling
            throw new RuntimeException(ex);
        }
    }

//    /**
//     * @param s
//     * @return
//     */
//    public static FlowObjectDef toFlowVariableDef(final FlowVariable variable) {
//        NodeSettings sub = new NodeSettings("FlowVariable");
//        variable.save(sub);
//        try {
//            return new FlowVariableDefBuilder()//
//                .setValue(toConfigMapDef(sub))//
//                .build();
//        } catch (InvalidSettingsException ex) {
//            throw new IllegalArgumentException(
//                "Can not convert flow variable " + variable + " to plain java description.");
//        }
//    }

    /**
     * @param credentials
     * @return
     */
    public static CredentialPlaceholderDef toWorkflowCredentialsDef(final List<Credentials> credentials) {
        // TODO
        throw new NotImplementedException("Credentials storage not implemented yet");
//        return DefaultWorkflowCredentialsDef.builder().build();
    }

    /**
     * @param authorInformation
     * @return
     */
    public static AuthorInformationDef toAuthorInformationDef(final AuthorInformation authorInformation) {
        var builder = new AuthorInformationDefBuilder()//
            .setAuthoredBy(authorInformation.getAuthor())//
            .setAuthoredWhen(authorInformation.getAuthoredDate());//
        authorInformation.getLastEditor().ifPresent(builder::setLastEditedBy);
        authorInformation.getLastEditDate().ifPresent(builder::setLastEditedWhen);

        return builder.build();
    }

    /**
     * @param wfm the workflow manager that provides the content to be copied
     * @param content specifies the nodes and annotations to copy and additional settings
     * @param passwordRedactor handler to remove or alter passwords
     * @return a description of the selected content in intermediate workflow format (def)
     */
    public static WorkflowDef copyToDef(final WorkflowManager wfm, final WorkflowCopyContent content,
        final PasswordRedactor passwordRedactor) {

        HashSet<NodeID> nodeIDs = new HashSet<>(Arrays.asList(content.getNodeIDs()));
        HashSet<NodeID> virtualNodeIDs = new HashSet<>();
        CheckUtils.checkArgumentNotNull(passwordRedactor,
            "No password redactor provided. If passwords should remain unchanged, please specify explicitly by providing an according password redactor.");
        CheckUtils.checkArgument(nodeIDs.size() == content.getNodeIDs().length, "Copy spec contains duplicate nodes.");

        // copy contents (nodes, connections, annotations) are stored in a workflow def
        final var workflowBuilder = new WorkflowDefBuilder();

        // 1. Nodes ------------------------

        // for each node: create a def, possibly updating the id and location in the workflow (move a little bit before paste)
        for (NodeID nodeID : nodeIDs) {
            var nc = wfm.getNodeContainer(nodeID);

            // if a suggested node ID is present, use it. Otherwise use the node container's id
            int defNodeId = Optional.ofNullable(content.getSuggestedNodIDSuffix(nodeID)).orElse(nc.getID().getIndex());

            // if suggested ui information is present, apply the offset from the copy content
            // this is only present when copying a Component or Metanode, which will call
            // setNodeID(NodeID, int suggestedNodeIDSuffix, NodeUIInformation) on WorkflowCopyContent
            var suggestedUiInfo = Optional.ofNullable(content.getOverwrittenUIInfo(nodeID));
            var defUiInfo = content.getPositionOffset()
                .flatMap(offset -> suggestedUiInfo.map(si -> NodeUIInformation.builder(si).translate(offset).build()))//
                .map(CoreToDefUtil::toNodeUIInfoDef);
            var defaultUiInfo = CoreToDefUtil.toNodeUIInfoDef(nc.getUIInformation());
            Optional<BaseNodeDef> node = Optional.empty();
            // Virtual in/out nodes are excluded from the copy result if they are selected directly, otherwise
            // pasting would allow users to create virtual nodes. They are included when copied as part of an entire
            // component node (via SubnodeContainerToDefAdapter -> WorkflowManagerToDefAdapter#getNodes).
            if (nc instanceof NativeNodeContainer) {
                if (NativeNodeContainer.IS_VIRTUAL_IN_OUT_NODE.test(nc)) {
                    virtualNodeIDs.add(nodeID);
                } else {
                    var originalNativeNodeDef =
                        new NativeNodeContainerToDefAdapter((NativeNodeContainer)nc, passwordRedactor);
                    node = Optional.ofNullable(new NativeNodeDefBuilder(originalNativeNodeDef)//
                        .setId(defNodeId).setUiInfo(defUiInfo.orElse(defaultUiInfo)).build());
                }
            } else if (nc instanceof SubNodeContainer) {
                var originalComponentDef = new SubnodeContainerToDefAdapter((SubNodeContainer)nc, passwordRedactor);
                node = Optional.ofNullable(new ComponentNodeDefBuilder(originalComponentDef)//
                    .setId(defNodeId).setUiInfo(defUiInfo.orElse(defaultUiInfo)).build());
            } else if (nc instanceof WorkflowManager) {
                var originalMetanodeDef = new MetanodeToDefAdapter((WorkflowManager)nc, passwordRedactor);
                node = Optional.ofNullable(new MetaNodeDefBuilder(originalMetanodeDef)//
                    .setId(defNodeId).setUiInfo(defUiInfo.orElse(defaultUiInfo)).build());
            }

            node.ifPresent(n -> workflowBuilder.putToNodes(String.valueOf(defNodeId), n));
        }

        // 2. Connections ------------------------

        // connections between selected nodes (and optionally also between included and non-included nodes)
        var inducedConnections = wfm.getWorkflow().getInducedConnections(nodeIDs, content.isIncludeInOutConnections());

        inducedConnections.stream()//
            // filter out the connection from/to virtual nodes.
            .filter(cc -> !virtualNodeIDs.contains(cc.getDest())).filter(cc -> !virtualNodeIDs.contains(cc.getSource()))
            // apply copy content translation offset to connections, convert to def, and add to workflow
            .map(cc -> {
                var t = new ConnectionContainerTemplate(cc, false);
                t.fixPostionOffsetIfPresent(content.getPositionOffset());
                return t;
            })//
            .map(CoreToDefUtil::toConnectionDef)//
            .forEach(workflowBuilder::addToConnections);
        // 3. Annotations ------------------------
        Arrays.stream(wfm.getWorkflowAnnotations(content.getAnnotationIDs()))//
            .forEach(anno -> workflowBuilder.putToAnnotations(//
                String.valueOf(anno.getID().getIndex()), // key
                CoreToDefUtil.toAnnotationDataDef(anno))); // value
        return workflowBuilder.build();
    }

}
