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
package org.knime.core.node.workflow.def;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.node.workflow.ComponentMetadata;
import org.knime.core.node.workflow.ComponentMetadata.ComponentMetadataBuilder;
import org.knime.core.node.workflow.FileNativeNodeContainerPersistor;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.TemplateType;
import org.knime.core.node.workflow.NodeAnnotationData;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.workflow.def.BoundsDef;
import org.knime.core.workflow.def.ComponentMetadataDef;
import org.knime.core.workflow.def.ConfigDef;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.ConfigValueDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeLocksDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.TemplateInfoDef;

/**
 *
 * @author hornm
 */
public class DefToCoreUtil {

    public static void toNodeAnnotationData(final NodeAnnotationData annoData, final NodeAnnotationDef def) {
        annoData.setAlignment(TextAlignment.valueOf(def.getAlignment()));
        annoData.setBgColor(def.getBgcolor());
        annoData.setBorderColor(def.getBorderColor());
        annoData.setBorderSize(def.getBorderSize());
        annoData.setDefaultFontSize(def.getDefFontSize());
        annoData.setX(def.getXCoordinate());
        annoData.setY(def.getYCoordinate());
        annoData.setWidth(def.getWidth());
        annoData.setHeight(def.getHeight());
    }

    public static Node toNode(final NativeNodeDef def) {
        NodeSettingsRO additionalFactorySettings = toNodeSettings(def.getFactorySettings());
        // TODO catch exception on factory settings load
        NodeFactory<NodeModel> nodeFactory;
        try {
            nodeFactory = FileNativeNodeContainerPersistor.loadNodeFactory(def.getFactory());
        } catch (Exception e) {
            // setDirtyAfterLoad(); // don't set dirty, missing node placeholder will be used instead
            throw new RuntimeException(e);
            // TODO
            // throw new NodeFactoryUnknownException(toNodeAndBundleInformationPersistor(def), additionalFactorySettings,
            //     e);
        }
        try {
            nodeFactory.loadAdditionalFactorySettings(additionalFactorySettings);
        } catch (Exception e) {
            // String error = "Unable to load additional factory settings into node factory (node \"" + nodeInfo + "\")";
            // getLogger().error(error);
            // setDirtyAfterLoad(); // don't set dirty, missing node placeholder
            throw new RuntimeException(e);
            // TODO
            // throw new NodeFactoryUnknownException(error, nodeInfo, additionalFactorySettings, e);
        }

        NodeSettingsRO nodeCreationSettings = toNodeSettings(def.getNodeCreationConfig());
        // TODO creation config??
        try {
            return new Node(nodeFactory,
                FileNativeNodeContainerPersistor.loadCreationConfig(nodeCreationSettings, nodeFactory).orElse(null));
        } catch (InvalidSettingsException ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }

    public static PortType toPortType(final String objectClassString) {
        if (objectClassString == null) {
            // TODO
            throw new RuntimeException(
                "No port object class found to create PortType object");
        }
        Class<? extends PortObject> obClass = PortTypeRegistry.getInstance().getObjectClass(objectClassString)
            .orElseThrow(() -> new RuntimeException(
                "Unable to restore port type, " + "can't load class \"" + objectClassString + "\""));
        return PortTypeRegistry.getInstance().getPortType(obClass);

    }

    public static ComponentMetadata toComponentMetadata(final ComponentMetadataDef def) {
        ComponentMetadataBuilder builder = ComponentMetadata.builder()//
            .description(def.getDescription())//
            .icon(def.getIcon());
        for (int i = 0; i < def.getInPortNames().size(); i++) {
            builder.addInPortNameAndDescription(def.getInPortNames().get(i), def.getInPortDescriptions().get(i));
        }
        for (int i = 0; i < def.getOutPortNames().size(); i++) {
            builder.addOutPortNameAndDescription(def.getOutPortNames().get(i), def.getOutPortDescriptions().get(i));
        }
        return builder.build();
    }

    public static MetaNodeTemplateInformation toTemplateInfo(final TemplateInfoDef def) {
        try {
            return new MetaNodeTemplateInformation(Role.valueOf(def.getRole()), TemplateType.valueOf(def.getType()),
                new URI(def.getUri()), java.util.Date.from(def.getTimestamp().toInstant()), null, null);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a node settings tree (comprising {@link AbstractConfigEntry}s) from a
     * {@link ConfigDef} tree.
     *
     * @param def an entity containing the recursive node settings
     */
    public static NodeSettingsRO toNodeSettings(final ConfigMapDef def) {
        return toNodeSettings(def, def.getKey());
    }

    /**
     * Recursive function to create a node settings tree (comprising {@link AbstractConfigEntry}s) from a
     * {@link ConfigDef} tree.
     *
     * @param def an entity containing the recursive node settings
     * @param key the name of this subtree
     */
    private static NodeSettings toNodeSettings(final ConfigMapDef def, final String key) {
        final NodeSettings settings = new NodeSettings(key);
        // recursion anchor
        if (def instanceof ConfigValueDef) {
            addLeafToSettings(settings, key, (ConfigValueDef)def);
        } else {
            // recurse
            for (Map.Entry<String, ConfigDef> childEntry : def.getChildren().entrySet()) {
                final ConfigDef child = childEntry.getValue();
                if (child instanceof ConfigMapDef) {
                    settings.addNodeSettings(toNodeSettings((ConfigMapDef)child, childEntry.getKey()));
                } else {
                    addLeafToSettings(settings, childEntry.getKey(), (ConfigValueDef)childEntry);
                }
            }
        }
        return settings;
    }

    /**
     * @param settings settings to modify
     * @param key name of the key of the mapping
     * @param configuration a string representation of the value with type annotation (saying, e.g., "xdouble"), see
     *            {@link ConfigEntries}
     */
    private static void addLeafToSettings(final NodeSettings settings, final String key, final ConfigValueDef leafDef) {
        AbstractConfigEntry leaf = ConfigEntries.valueOf(leafDef.getValueType()).createEntry(key, leafDef.getValue());
        settings.addEntry(leaf);
    }

    public static NodeAndBundleInformationPersistor toNodeAndBundleInformationPersistor(final NativeNodeDef def) {
        return NodeAndBundleInformationPersistor.load(def.getNodeAndBundleInfo(), def.getFactory());
    }

    public static NodeUIInformation toNodeUIInformation(final NodeUIInfoDef uiInfoDef) {
        BoundsDef boundsDef = uiInfoDef.getBounds();
        return NodeUIInformation.builder()//
            .setHasAbsoluteCoordinates(uiInfoDef.hasAbsoluteCoordinates())//
            .setIsDropLocation(uiInfoDef.isDropLocation())//
            .setIsSymbolRelative(uiInfoDef.isSymbolRelative())//
            .setNodeLocation(boundsDef.getX(), boundsDef.getY(), boundsDef.getWidth(), boundsDef.getHeight())//
            .setSnapToGrid(uiInfoDef.isSnapToGrid()).build();
    }

    public static NodeLocks toNodeLocks(final NodeLocksDef def) {
        return new NodeLocks(def.hasDeleteLock(), def.hasResetLock(), def.hasConfigureLock());
    }

}
