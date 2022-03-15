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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.MetaNodeDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.impl.MetaNodeDefBuilder;
import org.knime.core.workflow.def.impl.NodeUIInfoDefBuilder;
import org.knime.core.workflow.def.impl.PortDefBuilder;
import org.knime.core.workflow.def.impl.PortTypeDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
public class MetaNodeLoader {

    /**
     * The identifiers for configuration elements, e.g., in template.knime {@code <config key="meta_in_ports">
    <entry key="ui_classname" type="xstring" ... }
     */
    private enum Const {
            /** @see MetaNodeDef#getInPorts() */
            IN_PORTS("meta_in_ports"),
            /** @see MetaNodeDef#getInPorts() */
            OUT_PORTS("meta_out_ports"),
            /** The name of the file that contains the configuration of this Metandode */
            SETTINGS_FILE_NAME("workflow.knime");

        private final String m_key;

        Const(final String settingsKey) {
            m_key = settingsKey;
        }

        String get() {
            return m_key;
        }
    }

    static MetaNodeDef load(final ConfigBaseRO workflowConfig, final File nodeDirectory,
        final LoadVersion workflowFormatVersion) throws IOException {
        var metaNodeConfig = LoaderUtils.readWorkflowConfigFromFile(nodeDirectory);

        return new MetaNodeDefBuilder()//
            .setWorkflow(() -> WorkflowLoader.load(nodeDirectory, workflowFormatVersion),
                new WorkflowDefBuilder().build())//
            .setInPorts(() -> loadInPorts(metaNodeConfig, workflowFormatVersion), List.of())//
            .setOutPorts(() -> loadOutPorts(metaNodeConfig, workflowFormatVersion), List.of())//
            .setInPortsBarUIInfo(() -> loadPortsBarUIInfo(metaNodeConfig, Const.IN_PORTS, workflowFormatVersion),
                new NodeUIInfoDefBuilder().build())//
            .setOutPortsBarUIInfo(() -> loadPortsBarUIInfo(metaNodeConfig, Const.OUT_PORTS, workflowFormatVersion),
                new NodeUIInfoDefBuilder().build())//
            .setLink(null) // TODO
            .setBaseNode(NodeLoader.load(workflowConfig, metaNodeConfig, workflowFormatVersion)) //
            .build();
    }

    private static ConfigBaseRO loadSetting(final ConfigBaseRO sub, final Const key, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        try {
            if (loadVersion.isOlderThan(LoadVersion.V200) || !sub.containsKey(key.get())) {
                return null;
            }
            return sub.getConfigBase(key.get());
        } catch (InvalidSettingsException e) {
            //   String error = "Can't load workflow ports, config not found";
            var errorMessage = String.format("Unable to load the %s: %s", key.get(), e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static List<PortDef> loadInPorts(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        var inPortsSettings = loadSetting(settings, Const.IN_PORTS, loadVersion);
        if (inPortsSettings == null || inPortsSettings.keySet().isEmpty()) {
            return List.of();
        }
        var inPortsEnum = loadPortsSettingsEnum(inPortsSettings);
        return loadPorts(inPortsEnum);
    }

    private static List<PortDef> loadOutPorts(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        try {
            var inPortsSettings = loadSetting(settings, Const.OUT_PORTS, loadVersion);
            if (inPortsSettings == null || inPortsSettings.keySet().isEmpty()) {
                return List.of();
            }

            var outPortsEnum = loadPortsSettingsEnum(inPortsSettings);
            return loadPorts(outPortsEnum);
        } catch (InvalidSettingsException e) {
            var errorMessage = "Can't load meta output ports: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * @return nullable
     * @throws InvalidSettingsException
     */
    private static ConfigBaseRO loadPortsSettingsEnum(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            return settings.getConfigBase("port_enum");
        } catch (InvalidSettingsException e) {
            var errorMessage = "Can't load port enum: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static List<PortDef> loadPorts(final ConfigBaseRO portsEnum) {
        Set<String> keySet = portsEnum == null ? Set.of() : portsEnum.keySet();
        var portCount = portsEnum == null ? 0 : keySet.size();
        var portDefs = new PortDef[portCount];
        for (String key : keySet) {
            if (portsEnum != null) {
                ConfigBase portConfig;
                try {
                    portConfig = portsEnum.getConfigBase(key);
                    PortDef port = loadPort(portConfig);
                    if (port.getIndex() >= 0 && port.getIndex() < portCount) {
                        portDefs[port.getIndex()] = port;
                    }
                } catch (InvalidSettingsException ex) {
                    //TODO Shall i follow the m_metaNodeBuilder.addToInPorts(portDef) ?
                    //TODO How can i put to the load problems the exceptions from the higher layer?
                }

            }
        }
        for (var i = 0; i < portDefs.length; i++) {
            if (portDefs[i] == null) {
                //FIXME
                // new funcitonality
                portDefs[i] = new PortDefBuilder().build();
                // Old funcitonality
                //                loadResult.setDirtyAfterLoad();
                //                loadResult.addError("Assigning fallback port type for " + "missing input port " + i);
                //                portDefs[i] = new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }

        return List.of(portDefs);
    }

    private static PortDef loadPort(final ConfigBaseRO settings) {
        var portTypeDef = new PortTypeDefBuilder()//
            //TODO What do we do for the properties which are not have default values?
            .setPortObjectClass(() -> settings.getConfigBase("type").getString("object_class"), "")//
            .build();
        return new PortDefBuilder()//
            .setIndex(() -> settings.getInt("index"), -1)//
            .setName(() -> settings.getString("name"), "")//
            .setPortType(portTypeDef)// TODO port type def contains many more fields but I think none of them are necessary (except for spec class maybe?) since all the additional info is pulled from the port type registry using the class name
            .build();
    }

    private static NodeUIInfoDef loadNodeUIInformation(final ConfigBaseRO portSettings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        if (portSettings == null) {
            return new NodeUIInfoDefBuilder().build();
        }

        // in previous releases, the settings were directly written to the
        // top-most node settings object; since 2.0 they are put into a
        // separate sub-settings object
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            return NodeLoader.loadUIInfo(portSettings, loadVersion);
        } else {
            if (!portSettings.containsKey("ui_settings")) {
                return new NodeUIInfoDefBuilder().build();
            } else {
                return NodeLoader.loadUIInfo(portSettings.getConfigBase("ui_settings"), loadVersion);
            }
        }
    }

    /**
     * @param key whether to load input port or output port settings
     * @return nullable
     * @throws InvalidSettingsException
     */
    private static ConfigBaseRO loadPortsSetting(final ConfigBaseRO settings, final Const key,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            return null;
        }
        if (settings.containsKey(key.get())) {
            return settings.getConfigBase(key.get());
        }
        return null;
    }

    /**
     * @param key whether to load input port or output port bar information
     * @return nullable
     * @throws InvalidSettingsException
     */
    private static NodeUIInfoDef loadPortsBarUIInfo(final ConfigBaseRO settings, final Const key,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            return new NodeUIInfoDefBuilder().build();
        }
        try {
            return loadNodeUIInformation(loadPortsSetting(settings, key, loadVersion), loadVersion);
        } catch (InvalidSettingsException e) {
            var errorMessage = String.format("Unable to load the %s UI Ports bar info: %s", key, e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    //TODO The second implementation of the loadNodeConfig (NodeLoader abstract method)
    // This method reads the workflow.knime and used only for the metanode
    // Has been moved to the LoaderUtils.
    //    @Override
    //    protected ConfigBaseRO loadNodeConfig(final ConfigBaseRO workflowConfig, final File nodeDirectory)
    //        throws IOException {
    //        var settingsFile = new File(nodeDirectory, Const.SETTINGS_FILE_NAME.get());
    //        return SimpleConfig.parseConfig(settingsFile.getAbsolutePath(), settingsFile);
    //    }

    //    private void setPorts(MetaNodeDefBuilder builder, ConfigBaseRO nodeConfig) {
    //        builder.addToInPorts(() -> , null)
    //    }

}
