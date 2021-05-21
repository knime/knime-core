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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.workflow.ComponentMetadata;
import org.knime.core.node.workflow.ComponentMetadata.ComponentNodeType;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.util.Version;
import org.knime.core.workflow.def.ComponentMetadataDef;
import org.knime.core.workflow.def.ConfigDef;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.NodeAndBundleInfoDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeLocksDef;
import org.knime.core.workflow.def.NodeMessageDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.StyleDef;
import org.knime.core.workflow.def.TemplateInfoDef;
import org.knime.core.workflow.def.impl.DefaultAnnotationDataDef;
import org.knime.core.workflow.def.impl.DefaultBoundsDef;
import org.knime.core.workflow.def.impl.DefaultComponentMetadataDef;
import org.knime.core.workflow.def.impl.DefaultConfigMapDef;
import org.knime.core.workflow.def.impl.DefaultConfigValueDef;
import org.knime.core.workflow.def.impl.DefaultNodeAndBundleInfoDef;
import org.knime.core.workflow.def.impl.DefaultNodeAnnotationDef;
import org.knime.core.workflow.def.impl.DefaultNodeLocksDef;
import org.knime.core.workflow.def.impl.DefaultNodeMessageDef;
import org.knime.core.workflow.def.impl.DefaultNodeUIInfoDef;
import org.knime.core.workflow.def.impl.DefaultPortDef;
import org.knime.core.workflow.def.impl.DefaultStyleDef;
import org.knime.core.workflow.def.impl.DefaultTemplateInfoDef;

/**
 *
 * @author hornm
 */
public class CoreToDefUtil {

    public static NativeNodeDef toNativeNodeDef(final Node node) {
        //TODO
        return null;
    }

    public static ConfigMapDef toConfigMapDef(final NodeSettings settings) throws InvalidSettingsException {
        ConfigDef configDef = toConfigDef(settings);
        if (configDef instanceof ConfigMapDef) {
            return (ConfigMapDef)configDef;
        } else {
            throw new IllegalArgumentException("TODO");
        }
    }

    /**
     * @param settings settings to persist
     * @return the node settings in a representation that can be converted to various formats
     * @throws InvalidSettingsException
     */
    public static ConfigDef toConfigDef(final NodeSettings settings) throws InvalidSettingsException {
        return toConfigDef(settings, settings.getKey());
    }

    /**
     * Recursive function to create a node settings tree (comprising {@link AbstractConfigEntry}s) from a
     * {@link ConfigDef} tree.
     *
     * @param settings an entity containing the recursive node settings
     * @param key the name of this subtree
     * @throws InvalidSettingsException
     */
    private static ConfigDef toConfigDef(final NodeSettings settings, final String key)
        throws InvalidSettingsException {

        // create ConfigDef from AbstractConfigurationEntry
        final Function<AbstractConfigEntry, ConfigDef> aceToDef = e -> DefaultConfigValueDef.builder()
            .setValue(e.toStringValue()).setValueType(e.getType().name()).build();

        // recursion anchor
        if (settings.isLeaf()) {
            return aceToDef.apply(settings);
        } else {
            // recurse
            final Map<String, ConfigDef> children = new LinkedHashMap<>();
            for (String childKey: settings.keySet()) {
                final AbstractConfigEntry child = settings.getEntry(childKey);
                if (settings.getEntry(childKey).isLeaf()) {
                    children.put(childKey, aceToDef.apply(child));
                } else {
                    children.put(childKey, toConfigDef((NodeSettings)settings.getConfig(childKey)));
                }
            }
            return DefaultConfigMapDef.builder().setKey(key).setChildren(children).build();
        }
    }

    public static NativeNodeDef toNativeNodeDef(final NodeAndBundleInformationPersistor def) {
        //TODO
        return null;
    }

    public static NodeMessageDef toNodeMessageDef(final NodeMessage nm) {
        return DefaultNodeMessageDef.builder().setMessage(nm.getMessage()).setType(nm.getMessageType().toString())
            .build();
    }

    public static NodeUIInfoDef toNodeUIInfoDef(final NodeUIInformation uiInfoDef) {
        if (uiInfoDef == null) {
            return null;
        }
        int[] bounds = uiInfoDef.getBounds();
        DefaultBoundsDef boundsDef =
            DefaultBoundsDef.builder().setX(bounds[0]).setY(bounds[1]).setWidth(bounds[2]).setHeight(bounds[3]).build();
        return DefaultNodeUIInfoDef.builder().setBounds(boundsDef).setDropLocation(uiInfoDef.isDropLocation())
            .setHasAbsoluteCoordinates(uiInfoDef.hasAbsoluteCoordinates()).setSnapToGrid(uiInfoDef.getSnapToGrid())
            .setSymbolRelative(uiInfoDef.isSymbolRelative()).build();
    }

    public static NodeLocksDef toNodeLocksDef(final NodeLocks def) {
        return DefaultNodeLocksDef.builder().setHasConfigureLock(def.hasConfigureLock())
            .setHasDeleteLock(def.hasDeleteLock()).setHasResetLock(def.hasResetLock()).build();
    }

    public static NodeAnnotationDef toNodeAnnotationDef(final NodeAnnotation na) {
        List<StyleDef> styles = Arrays.stream(na.getStyleRanges())
            .map(s -> DefaultStyleDef.builder().setFgcolor(s.getFgColor()).setFontname(s.getFontName())
                .setFontsize(s.getFontSize()).setFontstyle(s.getFontStyle()).setLength(s.getLength())
                .setStart(s.getStart()).build())
            .collect(Collectors.toList());
        DefaultAnnotationDataDef annoData = DefaultAnnotationDataDef.builder()//
            .setText(na.getText())//
            .setAlignment(na.getAlignment().toString())//
            .setBgcolor(na.getBgColor())//
            .setBorderColor(na.getBorderColor())//
            .setBorderSize(na.getBorderSize())//
            .setDefFontSize(na.getDefaultFontSize())//
            .setHeight(na.getHeight())//
            .setWidth(na.getWidth())//
            .setXCoordinate(na.getX())//
            .setYCoordinate(na.getY())//
            .setStyles(styles)//
            .build();
        return DefaultNodeAnnotationDef.builder().setDefault(na.getData().isDefault()).setData(annoData).build();
    }

    public static TemplateInfoDef toTemplateInfoDef(final MetaNodeTemplateInformation i) {
        // TODO flow variables and example data info
        return DefaultTemplateInfoDef.builder().setRole(i.getRole().toString())
            .setTimestamp(i.getTimestamp() != null
                ? OffsetDateTime.ofInstant(i.getTimestamp().toInstant(), ZoneId.systemDefault()) : null)
            .setType(i.getNodeContainerTemplateType() != null ? i.getNodeContainerTemplateType().toString() : null)
            .build();
    }

    public static PortDef toPortDef(final NodePort p) {
        return DefaultPortDef.builder().setIndex(p.getPortIndex()).setName(p.getPortName())
            .setType(p.getPortType().toString()).build();
    }

    public static NodeAndBundleInfoDef toNodeAndBundleInfoDef(final NodeAndBundleInformationPersistor p) {
        return DefaultNodeAndBundleInfoDef.builder()//
            .setNodeBundleName(p.getBundleName().orElse(null))//
            .setNodeBundleSymbolicName(p.getBundleSymbolicName().orElse(null))//
            .setNodeBundleVendor(p.getBundleVendor().orElse(null))//
            .setNodeBundleVersion(p.getBundleVersion().map(Version::toString).orElse(null))//
            .setNodeFeatureName(p.getFeatureName().orElse(null))//
            .setNodeFeatureSymbolicName(p.getFeatureSymbolicName().orElse(null))//
            .setNodeFeatureVendor(p.getFeatureVendor().orElse(null))//
            .setNodeFeatureVersion(p.getFeatureVersion().map(Version::toString).orElse(null))//
            .build();
    }

    public static ComponentMetadataDef toComponentMetadataDef(final ComponentMetadata m) {
        return DefaultComponentMetadataDef.builder()//
            .setDescription(m.getDescription().orElse(null))//
            .setIcon(m.getIcon().orElse(null))//
            .setNodeType(m.getNodeType().map(ComponentNodeType::toString).orElse(null))//
            .setInPortNames(m.getInPortNames().map(Arrays::asList).orElse(null))//
            .setInPortDescriptions(m.getInPortDescriptions().map(Arrays::asList).orElse(null))//
            .setOutPortNames(m.getOutPortNames().map(Arrays::asList).orElse(null))//
            .setOutPortDescriptions(m.getOutPortDescriptions().map(Arrays::asList).orElse(null))//
            .build();
    }
}
