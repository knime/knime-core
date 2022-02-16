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
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.MetaNodeDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.impl.BoundsDefBuilder;
import org.knime.core.workflow.def.impl.CoordinateDefBuilder;
import org.knime.core.workflow.def.impl.DefaultBoundsDef;
import org.knime.core.workflow.def.impl.MetaNodeDefBuilder;
import org.knime.core.workflow.def.impl.NodeUIInfoDefBuilder;
import org.knime.core.workflow.def.impl.PortDefBuilder;
import org.knime.core.workflow.def.impl.PortTypeDefBuilder;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class MetaNodeLoader extends NodeLoader {

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

    private LoadVersion m_loadVersion;

    public MetaNodeLoader() {
        super(new MetaNodeDefBuilder());
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    protected ConfigBaseRO loadNodeConfig(final ConfigBaseRO workflowConfig, final File nodeDirectory)
        throws IOException {
        File settingsFile = new File(nodeDirectory, Const.SETTINGS_FILE_NAME.get());
        return SimpleConfig.parseConfig(settingsFile.getAbsolutePath(), settingsFile);
    }

    private List<PortDef> loadPorts(final ConfigBaseRO workflowSett, final Const whichPorts) {
        ConfigBaseRO portsEnum = null;
        try {
            var ports = loadPortsSetting(workflowSett, whichPorts);
            if (ports != null) {
                portsEnum = loadPortsSettingsEnum(ports);
            }
        } catch (InvalidSettingsException e) {
            // TODO
            //            var error = "Can't load workflow ports, config not found: " + e.getMessage();
            //                  getLogger().debug(error, e);
            //                  loadResult.setDirtyAfterLoad();
            //                    loadResult.addError(error);
            if (whichPorts == Const.IN_PORTS) {
                // TODO
                //                  loadResult.addError(error);
                //                  loadResult.setResetRequiredAfterLoad();
            }
        }
        Set<String> keySet = portsEnum == null ? Set.of() : portsEnum.keySet();
        var portCount = portsEnum == null ? 0 : keySet.size();
        var portDefs = new PortDef[portCount];
        for (String key : keySet) {
            PortDef p;
            try {
                p = loadPort(portsEnum.getConfigBase(key));
            } catch (InvalidSettingsException e) {
                // TODO
                //                var error = "Can't load workflow inport (internal ID \"" + key + "\", skipping it: " + e.getMessage();
                //                getLogger().debug(error, e);
                //                loadResult.setDirtyAfterLoad();
                //                loadResult.addError(error);
                //                loadResult.setResetRequiredAfterLoad();
                continue;
            }
            var index = p.getIndex();
            if (index < 0 || index >= portCount) {
                //                TODO
                //                loadResult.setDirtyAfterLoad();
                //                loadResult.addError("Invalid inport index " + index);
                //                loadResult.setResetRequiredAfterLoad();
                continue;
            }
            if (portDefs[index] != null) {
                // TODO
                //                loadResult.setDirtyAfterLoad();
                //                loadResult.addError("Duplicate inport definition for index: " + index);
            }
            portDefs[index] = p;
        }
        for (var i = 0; i < portDefs.length; i++) {
            if (portDefs[i] == null) {
                // TODO
                //                loadResult.setDirtyAfterLoad();
                //                loadResult.addError("Assigning fallback port type for " + "missing input port " + i);
                //                portDefs[i] = new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }

        return List.of(portDefs);
    }

    private PortDef loadPort(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (m_loadVersion.isOlderThan(LoadVersion.V200)) {
            throw new InvalidSettingsException("No ports for metanodes in version 1.x.x");
        }
        var index = settings.getInt("index");
        var name = settings.getString("name");
        var portTypeDef = new PortTypeDefBuilder()//
            .setPortObjectClass(settings.getConfigBase("type").getString("object_class"))//
            .build();
        return new PortDefBuilder()//
            .setIndex(index)//
            .setName(name)//
            .setPortType(portTypeDef)// TODO port type def contains many more fields but I think none of them are necessary (except for spec class maybe?) since all the additional info is pulled from the port type registry using the class name
            .build();
    }

    private NodeUIInfoDef loadNodeUIInformation(final ConfigBaseRO portSettings) throws InvalidSettingsException {
        if (portSettings == null) {
            return null;
        }

        // in previous releases, the settings were directly written to the
        // top-most node settings object; since 2.0 they are put into a
        // separate sub-settings object
        var subSettings =
            m_loadVersion.isOlderThan(LoadVersion.V200) ? portSettings : portSettings.getConfigBase("ui_settings");
        final var loadOrdinal = m_loadVersion.ordinal();
        var bounds = subSettings.getIntArray("extrainfo.node.bounds");
        var symbolRelative = loadOrdinal >= LoadVersion.V230.ordinal();
        DefaultBoundsDef boundsDef =
            new BoundsDefBuilder().setLocation(new CoordinateDefBuilder().setX(bounds[0]).setY(bounds[1]).build())//
                .setWidth(bounds[2]).setHeight(bounds[3]).build();
        return new NodeUIInfoDefBuilder()//
            .setBounds(boundsDef)//
            .setSymbolRelative(symbolRelative)//
            //.setHasAbsoluteCoordinates(null) // TODO
            .build();
    }

    /**
     * @param key whether to load input port or output port settings
     * @return nullable
     * @throws InvalidSettingsException
     */
    private ConfigBaseRO loadPortsSetting(final ConfigBaseRO settings, final Const key)
        throws InvalidSettingsException {
        if (m_loadVersion.isOlderThan(LoadVersion.V200)) {
            return null;
        }
        if (settings.containsKey(key.get())) {
            return settings.getConfigBase(key.get());
        }
        return null;
    }

    /**
     * @return nullable
     * @throws InvalidSettingsException
     */
    private ConfigBaseRO loadPortsSettingsEnum(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (m_loadVersion.isOlderThan(LoadVersion.V200)) {
            return null;
        }
        return settings.getConfigBase("port_enum");
    }

    /**
     * @param key whether to load input port or output port bar information
     * @return nullable
     */
    private NodeUIInfoDef loadPortsBarUIInfo(final ConfigBaseRO settings, final Const key) {
        if (m_loadVersion.isOlderThan(LoadVersion.V200)) {
            return null;
        }
        try {
            return loadNodeUIInformation(loadPortsSetting(settings, key));
        } catch (InvalidSettingsException ex) {
            // TODO Auto-generated catch block
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    MetaNodeDefBuilder getNodeBuilder() {
        return (MetaNodeDefBuilder)super.getNodeBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    MetaNodeDef getNodeDef() {
        return getNodeBuilder().build();
    }

    @Override
    MetaNodeLoader load(final ConfigBaseRO workflowConfig, final File nodeDirectory,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException, IOException {
        super.load(workflowConfig, nodeDirectory, workflowFormatVersion);

        m_loadVersion = workflowFormatVersion;

        // TODO move to standalone metanode
        //        setTemplateInformation(tryLoadDebug("template information", MetaNodeTemplateInformation.NONE, () -> {
        //            if (m_templateInformation != null) {
        //                // template information was set after construction (this node is a link created from a template)
        //                assert m_templateInformation.getRole() == Role.Link;
        //            } else {
        //                var templateInformation = MetaNodeTemplateInformation.load(m_workflowSett, loadVersion);
        //                CheckUtils.checkSettingNotNull(m_templateInformation, "No template information");
        //                return templateInformation;
        //            }
        //            return m_templateInformation; // don't change (set again with identical value)
        //        }, loadResult));

        // TODO

        getNodeBuilder()//
            .setWorkflow(WorkflowLoader.load(nodeDirectory, workflowFormatVersion))//
            .setInPorts(loadPorts(m_nodeConfig, Const.IN_PORTS))//
            .setOutPorts(loadPorts(m_nodeConfig, Const.OUT_PORTS))//
            .setInPortsBarUIInfo(loadPortsBarUIInfo(m_nodeConfig, Const.IN_PORTS))//
            .setOutPortsBarUIInfo(loadPortsBarUIInfo(m_nodeConfig, Const.OUT_PORTS))//
            .setLink(null); // TODO
        return this;
    }

}
