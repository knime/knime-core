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
import java.util.Date;
import java.util.List;
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
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.node.workflow.ComponentMetadata;
import org.knime.core.node.workflow.ComponentMetadata.ComponentMetadataBuilder;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.core.node.workflow.FileNativeNodeContainerPersistor;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.TemplateType;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.BoundsDef;
import org.knime.core.workflow.def.ComponentMetadataDef;
import org.knime.core.workflow.def.ConfigDef;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.ConfigValueArrayDef;
import org.knime.core.workflow.def.ConfigValueBooleanArrayDef;
import org.knime.core.workflow.def.ConfigValueBooleanDef;
import org.knime.core.workflow.def.ConfigValueByteArrayDef;
import org.knime.core.workflow.def.ConfigValueByteDef;
import org.knime.core.workflow.def.ConfigValueCharArrayDef;
import org.knime.core.workflow.def.ConfigValueCharDef;
import org.knime.core.workflow.def.ConfigValueDef;
import org.knime.core.workflow.def.ConfigValueDoubleArrayDef;
import org.knime.core.workflow.def.ConfigValueDoubleDef;
import org.knime.core.workflow.def.ConfigValueFloatArrayDef;
import org.knime.core.workflow.def.ConfigValueFloatDef;
import org.knime.core.workflow.def.ConfigValueIntArrayDef;
import org.knime.core.workflow.def.ConfigValueIntDef;
import org.knime.core.workflow.def.ConfigValueLongArrayDef;
import org.knime.core.workflow.def.ConfigValueLongDef;
import org.knime.core.workflow.def.ConfigValueShortArrayDef;
import org.knime.core.workflow.def.ConfigValueShortDef;
import org.knime.core.workflow.def.ConfigValueStringArrayDef;
import org.knime.core.workflow.def.ConfigValueStringDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.NodeLocksDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.TemplateInfoDef;
import org.knime.core.workflow.def.WorkflowUISettingsDef;

/**
 *
 * @author hornm
 */
public class DefToCoreUtil {

    public static AnnotationData toAnnotationData(final AnnotationData annoData, final AnnotationDataDef def) {
        annoData.setAlignment(TextAlignment.valueOf(def.getAlignment()));
        annoData.setBgColor(def.getBgcolor());
        annoData.setBorderColor(def.getBorderColor());
        annoData.setBorderSize(def.getBorderSize());
        annoData.setDefaultFontSize(def.getDefFontSize());
        annoData.setX(def.getLocation().getX());
        annoData.setY(def.getLocation().getY());
        annoData.setWidth(def.getWidth());
        annoData.setHeight(def.getHeight());
        return annoData;
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
            // TODO throw new NodeFactoryUnknownException(toNodeAndBundleInformationPersistor(def), additionalFactorySettings, e);
        }
        try {
            nodeFactory.loadAdditionalFactorySettings(additionalFactorySettings);
        } catch (Exception e) {
            // String error = "Unable to load additional factory settings into node factory (node \"" + nodeInfo + "\")";
            // getLogger().error(error);
            // setDirtyAfterLoad(); // don't set dirty, missing node placeholder
            throw new RuntimeException(e);
            // TODO throw new NodeFactoryUnknownException(error, nodeInfo, additionalFactorySettings, e);
        }

        NodeSettingsRO nodeCreationSettings = toNodeSettings(def.getNodeCreationConfig());
        // TODO creation config??
        try {
            return new Node(nodeFactory,
                FileNativeNodeContainerPersistor.loadCreationConfig(nodeCreationSettings, nodeFactory).orElse(null));
        } catch (InvalidSettingsException ex) {
            // TODO throw new RuntimeException(ex);
            throw new RuntimeException(ex);
        }
    }

    public static PortType toPortType(final String objectClassString) {
        if (objectClassString == null) {
            // TODO throw new RuntimeException("No port object class found to create PortType object");
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
            if (def.getRole().equals("None")) {
                return new MetaNodeTemplateInformation(Role.None, null, null, null, null, null);
            } else {
                return new MetaNodeTemplateInformation(Role.valueOf(def.getRole()), TemplateType.valueOf(def.getType()),
                    new URI(def.getUri()), java.util.Date.from(def.getTimestamp().toInstant()), null, null);
            }
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static EditorUIInformation toEditorUIInformation(final WorkflowUISettingsDef def) {
        return EditorUIInformation.builder()//
            .setGridX(def.getGridX())//
            .setGridY(def.getGridY())//
            .setHasCurvedConnections(def.isCurvedConnections())//
            .setShowGrid(def.isShowGrid())//
            .setSnapToGrid(def.isSnapToGrid())//
            .setZoomLevel(def.getZoomLevel().doubleValue())//
            .build();
    }

    /**
     * TODO entirely replace it
     */
    public static AuthorInformation toAuthorInformation(final AuthorInformationDef def) {
        return new AuthorInformation(def.getAuthoredBy(), Date.from(def.getAuthoredWhen().toInstant()),
            def.getLastEditedBy(), Date.from(def.getLastEditedWhen().toInstant()));
    }

    /**
     * Create a node settings tree (comprising {@link AbstractConfigEntry}s) from a
     * {@link ConfigDef} tree.
     * @param def an entity containing the recursive node settings
     */
    public static NodeSettings toNodeSettings(final ConfigMapDef def) {
        // TODO should def be allowed to be null here?
        return def == null ? null : toNodeSettings(def, def.getKey());
    }

    /**
     * Recursive function to create a node settings tree (comprising {@link AbstractConfigEntry}s) from a
     * {@link ConfigDef} tree.
     *
     * @param def an entity containing the recursive node settings
     * @param key the name of this subtree
     */
    private static NodeSettings toNodeSettings(final ConfigDef def, final String key) {
        // recursion anchor
        if (def instanceof ConfigValueArrayDef) {
            // this is an array that needs to be coded in the legacy hacky format (NodeSettings with N+1 children,
            // 1 for array size, N for items
            return toNodeSettingsArray((ConfigValueArrayDef)def, key);
        } else if (def instanceof ConfigMapDef){  // this is a subtree, because it has a children map
            final NodeSettings settings = new NodeSettings(key);

            ConfigMapDef subtree = (ConfigMapDef) def;
            for (Map.Entry<String, ConfigDef> childEntry : subtree.getChildren().entrySet()) {
                final ConfigDef child = childEntry.getValue();
                // This recursion lookahead is useful because we want to do parentSettings.addBoolean(val) instead of
                // creating and returning a ConfigBooleanEntry. This way, we don't have to pass the parent down.
                if(child instanceof ConfigValueDef) {
                    addLeafToSettings(settings, childEntry.getKey(), child);
                } else {
                    // recurse
                    NodeSettings subTree = toNodeSettings(child, childEntry.getKey());
                    settings.addNodeSettings(subTree);
                }
            }
            return settings;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @param def
     * @return
     */
    private static NodeSettings toNodeSettingsArray(final ConfigValueArrayDef def, final String arrayKey) {
        NodeSettings temp = new NodeSettings("");
        if (def instanceof ConfigValueBooleanArrayDef) {
            List<Boolean> values = ((ConfigValueBooleanArrayDef)def).getArray();
            boolean[] array = new boolean[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            temp.addBooleanArray(arrayKey, array);
        } else if (def instanceof ConfigValueByteArrayDef) {
            temp.addByteArray(arrayKey, ((ConfigValueByteArrayDef)def).getValue());
        } else if (def instanceof ConfigValueCharArrayDef) {
            List<Integer> values = ((ConfigValueCharArrayDef)def).getArray();
            char[] array = new char[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = (char)values.get(i).intValue();
            }
            temp.addCharArray(arrayKey, array);
        } else if (def instanceof ConfigValueDoubleArrayDef) {
            List<Double> values = ((ConfigValueDoubleArrayDef)def).getArray();
            double[] array = new double[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            temp.addDoubleArray(arrayKey, array);
        } else if (def instanceof ConfigValueFloatArrayDef) {
            List<Float> values = ((ConfigValueFloatArrayDef)def).getArray();
            float[] array = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            temp.addFloatArray(arrayKey, array);
        } else if (def instanceof ConfigValueIntArrayDef) {
            List<Integer> values = ((ConfigValueIntArrayDef)def).getArray();
            int[] array = new int[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            temp.addIntArray(arrayKey, array);
        } else if (def instanceof ConfigValueLongArrayDef) {
            List<Long> values = ((ConfigValueLongArrayDef)def).getArray();
            long[] array = new long[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            temp.addLongArray(arrayKey, array);
        } else if (def instanceof ConfigValueShortArrayDef) {
            List<Integer> values = ((ConfigValueShortArrayDef)def).getArray();
            short[] array = new short[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = (short)values.get(i).intValue();
            }
            temp.addShortArray(arrayKey, array);
        } else if (def instanceof ConfigValueStringArrayDef) {
            List<String> values = ((ConfigValueStringArrayDef)def).getArray();
            String[] array = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            temp.addStringArray(arrayKey, array);
        }

        if (temp.containsKey(arrayKey)) {
            return (NodeSettings)temp.getEntry(arrayKey);
        }
        return null;
    }

    /**
     * Convert a {@link ConfigValueDef} to {@link NodeSettings} and add it to the given node settings.
     *
     * @param settings parent node settings to add child to
     * @param key name of the child
     * @param configuration a string representation of the value with type annotation (saying, e.g., "xdouble"), see
     *            {@link ConfigEntries}
     */
    private static void addLeafToSettings(final NodeSettings settings, final String key, final ConfigDef leafDef) {
        if(leafDef instanceof ConfigValueBooleanDef) {
            boolean value = ((ConfigValueBooleanDef) leafDef).isValue();
            settings.addBoolean(key, value);
        }
        if(leafDef instanceof ConfigValueCharDef) {
            char value = (char) ((ConfigValueCharDef) leafDef).getValue().intValue();
            settings.addChar(key, value);
        }
        if(leafDef instanceof ConfigValueDoubleDef) {
            double value = ((ConfigValueDoubleDef) leafDef).getValue();
            settings.addDouble(key, value);
        }

        if(leafDef instanceof ConfigValueFloatDef) {
            float value = ((ConfigValueFloatDef) leafDef).getValue();
            settings.addFloat(key, value);
        }
        if(leafDef instanceof ConfigValueIntDef) {
            int value = ((ConfigValueIntDef) leafDef).getValue();
            settings.addInt(key, value);
        }
        if(leafDef instanceof ConfigValueLongDef) {
            long value = ((ConfigValueLongDef) leafDef).getValue();
            settings.addLong(key, value);
        }

        if(leafDef instanceof ConfigValueByteDef) {
            byte value = (byte) ((ConfigValueByteDef) leafDef).getValue().intValue();
            settings.addByte(key, value);
        }
        if(leafDef instanceof ConfigValueShortDef) {
            short value = (short) ((ConfigValueShortDef) leafDef).getValue().intValue();
            settings.addShort(key, value);
        }
        if(leafDef instanceof ConfigValueStringDef) {
            String value = ((ConfigValueStringDef) leafDef).getValue();
            settings.addString(key, value);
        }
        // TODO password need passphrase to decode
    }

    public static NodeAndBundleInformationPersistor toNodeAndBundleInformationPersistor(final NativeNodeDef def) {
        return NodeAndBundleInformationPersistor.load(def.getNodeAndBundleInfo(), def.getFactory());
    }

    public static NodeUIInformation toNodeUIInformation(final NodeUIInfoDef uiInfoDef) {

        // TODO currently, both components and root workflows are represented as a WorkflowDef without
        // NodeUIInfo (used to be in workflow.knime only for nodes).
        if(uiInfoDef == null) {
            return NodeUIInformation.builder().build();
        }

        BoundsDef boundsDef = uiInfoDef.getBounds();
        return NodeUIInformation.builder()//
            .setHasAbsoluteCoordinates(uiInfoDef.hasAbsoluteCoordinates())//
            .setIsDropLocation(uiInfoDef.isDropLocation())//
            .setIsSymbolRelative(uiInfoDef.isSymbolRelative())//
            .setNodeLocation(boundsDef.getLocation().getX(), boundsDef.getLocation().getY(), boundsDef.getWidth(), boundsDef.getHeight())//
            .setSnapToGrid(uiInfoDef.isSnapToGrid()).build();
    }

    public static NodeLocks toNodeLocks(final NodeLocksDef def) {
        return new NodeLocks(def.hasDeleteLock(), def.hasResetLock(), def.hasConfigureLock());
    }

}
