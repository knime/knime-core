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
 *   9 Feb 2022 (carlwitt): created
 */
package org.knime.core.node.workflow.loader;

import static org.knime.core.node.workflow.loader.LoaderUtils.DEFAULT_EMPTY_STRING;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.loader.LoaderUtils.Const;
import org.knime.core.node.workflow.loader.WorkflowLoader.NodeType;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.MetaNodeDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.PortTypeDef;
import org.knime.core.workflow.def.impl.FallibleMetaNodeDef;
import org.knime.core.workflow.def.impl.MetaNodeDefBuilder;
import org.knime.core.workflow.def.impl.NodeUIInfoDefBuilder;
import org.knime.core.workflow.def.impl.PortDefBuilder;
import org.knime.core.workflow.def.impl.PortTypeDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;
import org.knime.core.workflow.loader.FallibleSupplier;

/**
 * Loads the description of a MetaNode into {@link MetaNodeDef}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
public final class MetaNodeLoader {

    private MetaNodeLoader() {
    }

    private static final NodeUIInfoDef DEFAULT_NODE_UI = new NodeUIInfoDefBuilder().build();

    private static final PortDef DEFAULT_PORT_DEF = new PortDefBuilder().build();

    /**
     * Loads the properties of a MetaNode into {@link FallibleMetaNodeDef}, stores the loading exceptions using the
     * {@link FallibleSupplier}
     *
     * @param workflowConfig a read only representation of the workflow.knime.
     * @param nodeDirectory a {@link File} of the node folder.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return a {@link FallibleMetaNodeDef}
     * @throws IOException whether the settings.xml can't be found.
     */
    static FallibleMetaNodeDef load(final ConfigBaseRO workflowConfig, final File nodeDirectory,
        final LoadVersion workflowFormatVersion) throws IOException {
        var metaNodeConfig = LoaderUtils.readWorkflowConfigFromFile(nodeDirectory);
        // if the template.knime doesn't exist the template information lives in the MetaNode's workflow.knime.
        var templateConfig = LoaderUtils.readTemplateConfigFromFile(nodeDirectory).orElseGet(() -> metaNodeConfig);

        var builder = new MetaNodeDefBuilder()//
            .setNodeType(NodeType.METANODE.toString()) // FIXME
            .setWorkflow(() -> WorkflowLoader.load(nodeDirectory, workflowFormatVersion),
                new WorkflowDefBuilder().build())//
            //            .setInPorts(() -> loadInPorts(metaNodeConfig, workflowFormatVersion), List.of())//
            //            .setOutPorts(() -> loadOutPorts(metaNodeConfig, workflowFormatVersion), List.of())//
            .setInPortsBarUIInfo(
                () -> loadPortsBarUIInfo(metaNodeConfig, Const.META_IN_PORTS_KEY.get(), workflowFormatVersion),
                DEFAULT_NODE_UI)//
            .setOutPortsBarUIInfo(
                () -> loadPortsBarUIInfo(metaNodeConfig, Const.META_OUT_PORTS_KEY.get(), workflowFormatVersion),
                DEFAULT_NODE_UI)//
            .setLink(() -> LoaderUtils.loadTemplateLink(templateConfig), LoaderUtils.DEFAULT_TEMPLATE_LINK) //
            .setBaseNode(NodeLoader.load(workflowConfig, metaNodeConfig, workflowFormatVersion));
        setInPorts(builder, metaNodeConfig, workflowFormatVersion);
        setOutPorts(builder, metaNodeConfig, workflowFormatVersion);

        return builder.build();
    }

    /**
     * Loads the MetaNode's input ports from the {@code settings}.
     *
     * @param settings a read only representation of the node's settings.xml.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return a list of {@link PortDef}.
     * @throws InvalidSettingsException
     */
    //    private static List<PortDef> loadInPorts(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion)
    //        throws InvalidSettingsException {
    //        var inPortsSettings = loadSetting(settings, Const.META_IN_PORTS_KEY.get(), workflowFormatVersion);
    //        if (inPortsSettings == null || inPortsSettings.keySet().isEmpty()) {
    //            return List.of();
    //        }
    //        var inPortsEnum = loadPortsSettingsEnum(inPortsSettings);
    //        return loadPorts(inPortsEnum);
    //    }

    /**
     * Loads the MetaNode's output ports from the {@code settings}.
     *
     * @param settings a read only representation of the node's settings.xml.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return a list of {@link PortDef}.
     * @throws InvalidSettingsException
     */
    //    private static List<PortDef> loadOutPorts(final ConfigBaseRO settings, final LoadVersion loadVersion)
    //        throws InvalidSettingsException {
    //        var inPortsSettings = loadSetting(settings, Const.META_OUT_PORTS_KEY.get(), loadVersion);
    //        if (inPortsSettings == null || inPortsSettings.keySet().isEmpty()) {
    //            return List.of();
    //        }
    //
    //        var outPortsEnum = loadPortsSettingsEnum(inPortsSettings);
    //        return loadPorts(outPortsEnum);
    //    }

    //    private static ConfigBaseRO loadPortsSettingsEnum(final ConfigBaseRO settings, final Const key,
    //        final LoadVersion loadVersion) throws InvalidSettingsException {
    //        try {
    //            var inPortsSettings = loadSetting(settings, key.get(), loadVersion);
    //            if (inPortsSettings == null || inPortsSettings.keySet().isEmpty() || !settings.containsKey(key.get())) {
    //                return null;
    //            }
    //
    //            return settings.getConfigBase(Const.PORT_ENUM_KEY.get());
    //        } catch (InvalidSettingsException e) {
    //            var errorMessage = "Can't load ports enum: " + e.getMessage();
    //            throw new InvalidSettingsException(errorMessage, e);
    //        }
    //    }

    /**
     * Load the input/output ports bar ui info from the {@code settings}.
     *
     * @param settings
     * @param key whether to load input port or output port bar information.
     * @return a {@link NodeUIInfoDef}
     * @throws InvalidSettingsException
     */
    private static NodeUIInfoDef loadPortsBarUIInfo(final ConfigBaseRO settings, final String key,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            return DEFAULT_NODE_UI;
        }
        return loadNodeUIInformation(loadPortsSetting(settings, key, loadVersion), loadVersion);
    }

    private static ConfigBaseRO loadSetting(final ConfigBaseRO sub, final String key, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        try {
            if (loadVersion.isOlderThan(LoadVersion.V200) || !sub.containsKey(key)) {
                return null;
            }
            return sub.getConfigBase(key);
        } catch (InvalidSettingsException e) {
            var errorMessage = String.format("Can't load workflow ports %s, config not found: %s", key, e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static ConfigBaseRO loadPortsSettingsEnum(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (settings == null || !settings.containsKey(Const.PORT_ENUM_KEY.get())) {
            return null;
        } else {
            return settings.getConfigBase(Const.PORT_ENUM_KEY.get());
        }
    }

    private static void setInPorts(final MetaNodeDefBuilder builder, final ConfigBaseRO settings,
        final LoadVersion loadVersion) {
        try {
            var inPortsSettings = loadSetting(settings, Const.META_IN_PORTS_KEY.get(), loadVersion);
            if (inPortsSettings == null) {
                builder.setInPorts(List.of());
            }
            var inPortsEnum = loadPortsSettingsEnum(inPortsSettings);
            if (inPortsEnum != null) {
                inPortsEnum.keySet().forEach(
                    key -> builder.addToInPorts(() -> loadPort(inPortsEnum.getConfigBase(key)), DEFAULT_PORT_DEF));
            }
        } catch (InvalidSettingsException ex) {
            builder.setInPorts(() -> {
                throw ex;
            }, List.of());
        }
    }

    private static void setOutPorts(final MetaNodeDefBuilder builder, final ConfigBaseRO settings,
        final LoadVersion loadVersion) {
        try {
            var inPortsSettings = loadSetting(settings, Const.META_OUT_PORTS_KEY.get(), loadVersion);
            if (inPortsSettings == null) {
                builder.setOutPorts(List.of());
            }
            var inPortsEnum = loadPortsSettingsEnum(inPortsSettings);
            if (inPortsEnum != null) {
                inPortsEnum.keySet().forEach(
                    key -> builder.addToOutPorts(() -> loadPort(inPortsEnum.getConfigBase(key)), DEFAULT_PORT_DEF));
            }
        } catch (InvalidSettingsException ex) {
            builder.setOutPorts(() -> {
                throw ex;
            }, List.of());
        }
    }

    //    private static List<PortDef> loadPorts(final ConfigBaseRO portsEnum) {
    //        Set<String> keySet = portsEnum == null ? Set.of() : portsEnum.keySet();
    //        var portCount = portsEnum == null ? 0 : keySet.size();
    //        var portDefs = new PortDef[portCount];
    //        for (String key : keySet) {
    //            if (portsEnum != null) {
    //                ConfigBase portConfig;
    //                try {
    //                    portConfig = portsEnum.getConfigBase(key);
    //                    PortDef port = loadPort(portConfig);
    //                    if (port.getIndex() >= 0 && port.getIndex() < portCount) {
    //                        portDefs[port.getIndex()] = port;
    //                    }
    //                } catch (InvalidSettingsException ex) {
    //                    //TODO Shall i follow the m_metaNodeBuilder.addToInPorts(portDef) ?
    //                    //TODO How can i put to the load problems the exceptions from the higher layer?
    //                }
    //
    //            }
    //        }
    //
    //        return List.of(portDefs);
    //    }

    private static PortDef loadPort(final ConfigBaseRO settings) {
        if (settings == null) {
            return DEFAULT_PORT_DEF;
        }
        return new PortDefBuilder()//
            .setIndex(() -> settings.getInt(Const.PORT_INDEX_KEY.get()), -1) // Negative default index
            .setName(() -> settings.getString(Const.PORT_NAME_KEY.get()), DEFAULT_EMPTY_STRING)//
            .setPortType(() -> loadPortTypeDef(settings), new PortTypeDefBuilder().build()) //
            .build();
    }

    private static PortTypeDef loadPortTypeDef(final ConfigBaseRO settings) throws InvalidSettingsException {
        return new PortTypeDefBuilder()//
            .setPortObjectClass(settings.getConfigBase(Const.PORT_TYPE_KEY.get()) //
                .getString(Const.PORT_OBJECT_CLASS_KEY.get()))//
            .build();
    }

    private static NodeUIInfoDef loadNodeUIInformation(final ConfigBaseRO portSettings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        if (portSettings == null) {
            return DEFAULT_NODE_UI;
        }

        // in previous releases, the settings were directly written to the
        // top-most node settings object; since 2.0 they are put into a
        // separate sub-settings object
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            return NodeLoader.loadUIInfo(portSettings, loadVersion);
        } else {
            if (!portSettings.containsKey(Const.UI_SETTINGS_KEY.get())) {
                return DEFAULT_NODE_UI;
            } else {
                return NodeLoader.loadUIInfo(portSettings.getConfigBase(Const.UI_SETTINGS_KEY.get()), loadVersion);
            }
        }
    }

    /**
     * @param key whether to load input port or output port settings
     * @return nullable
     * @throws InvalidSettingsException
     */
    private static ConfigBaseRO loadPortsSetting(final ConfigBaseRO settings, final String key,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V200) || !settings.containsKey(key)) {
            return null;
        }
        return settings.getConfigBase(key);
    }
}
