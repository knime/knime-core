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
 *   9 Feb 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.ComponentMetadataDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.TemplateLinkDef;
import org.knime.core.workflow.def.impl.ComponentDefBuilder;
import org.knime.core.workflow.def.impl.ComponentMetadataDefBuilder;
import org.knime.core.workflow.def.impl.FallibleComponentDef;
import org.knime.core.workflow.def.impl.PortDefBuilder;
import org.knime.core.workflow.def.impl.PortTypeDefBuilder;
import org.knime.core.workflow.def.impl.TemplateLinkDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;

/**
 * Loads the description of a Component node in a workflow. Components are internally also referred to as SubNodes.
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
public final class ComponentLoader {

    private ComponentLoader() {}

    static FallibleComponentDef load(final ConfigBaseRO workflowConfig, final File nodeDirectory,
        final LoadVersion workflowFormatVersion) throws IOException {
        var componentConfig = LoaderUtils.readNodeConfigFromFile(nodeDirectory);

        return new ComponentDefBuilder().setDialogSettings(null) //
            .setInPorts(() -> loadInPorts(componentConfig), List.of()) //
            .setOutPorts(() -> loadOutPorts(componentConfig), List.of()) //
            //TODO What can be the default virtual node id?
            .setVirtualInNodeId(() -> loadVirtualInNodeId(componentConfig), 0) //
            .setVirtualOutNodeId(() -> loadVirtualOutNodeId(componentConfig), 0) //
            // TODO We should pass the proper setting for the link, currently passing the template.knime.
            .setLink(() -> loadLink(componentConfig), new TemplateLinkDefBuilder().setUri("").build()) //
            .setMetadata(() -> loadMetadata(componentConfig), new ComponentMetadataDefBuilder() //
                .build()) //
            .setWorkflow(() -> WorkflowLoader.load(nodeDirectory, workflowFormatVersion),
                new WorkflowDefBuilder().build()) //
            .setConfigurableNode(ConfigurableNodeLoader.load(workflowConfig, componentConfig, workflowFormatVersion)) //
            .build();
    }

    /**
     * TODO
     *
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static TemplateLinkDef loadLink(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (!settings.containsKey("workflow_template_information")) {
            //FIXME Change the required field
            return new TemplateLinkDefBuilder().setUri("localhost").build();
        }

        var templateSettings = settings.getConfigBase("workflow_template_information");
        return Optional.ofNullable(templateSettings.getString("sourceURI")) //
            .map(uri -> new TemplateLinkDefBuilder().setUri(uri).build()) //
            .orElseThrow(() -> new InvalidSettingsException("Cannot not read source URI from emtpy string"));
    }

    private static ComponentMetadataDef loadMetadata(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (!settings.containsKey("metadata")) {
            return new ComponentMetadataDefBuilder().build();
        }
        try {
            var metadataSettings = settings.getConfigBase("metadata");
            return new ComponentMetadataDefBuilder() //
                .setDescription(() -> metadataSettings.getString("description"), "") //
                //TODO Do we need default component icon?
                .setIcon(() -> loadIcon(metadataSettings), null) //
                .setInPortDescriptions(() -> loadPortsProperties(metadataSettings, "inports", "description"), List.of()) //
                .setInPortNames(() -> loadPortsProperties(metadataSettings, "inports", "name"), List.of()) //
                .setOutPortDescriptions(() -> loadPortsProperties(metadataSettings, "outports", "description"),
                    List.of()) //
                .setOutPortNames(() -> loadPortsProperties(metadataSettings, "outports", "name"), List.of()) //
                .build();
        } catch (InvalidSettingsException e) {
            var errorMessage = "Unable to load component metadata: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static List<String> loadPortsProperties(final ConfigBaseRO sub, final String portsKey,
        final String propertyKey) throws InvalidSettingsException {
        try {
            var inportsSetting = loadSetting(sub, portsKey);
            if (inportsSetting == null) {
                return List.of();
            }

            String[] properties = new String[inportsSetting.keySet().size()]; //NOSONAR
            inportsSetting.keySet().stream().forEach(key -> {
                try {
                    var portSetting = inportsSetting.getConfigBase(key);
                    var index = portSetting.getInt("index", -1);
                    properties[index] = portSetting.getString(propertyKey);
                } catch (InvalidSettingsException ex) {
                    throw new RuntimeException(ex);
                }
            });
            return List.of(properties);
        } catch (Exception e) {
            var errorMessage =
                String.format("Unable to load the component's %s %s: %s", portsKey, propertyKey, e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static ConfigBaseRO loadSetting(final ConfigBaseRO sub, final String key) throws InvalidSettingsException {
        try {
            if (sub.containsKey(key)) {
                return sub.getConfigBase(key);
            } else {
                return null;
            }
        } catch (InvalidSettingsException e) {
            var errorMessage = String.format("Unable to load the %s: %s", key, e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static byte[] loadIcon(final ConfigBaseRO sub) throws InvalidSettingsException {
        try {
            if (sub.containsKey("icon")) {
                return Base64.getDecoder().decode(sub.getString("icon"));
            } else {
                return new byte[0];
            }
        } catch (InvalidSettingsException e) {
            var errorMessage = "Unable to load the component's icon: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static Integer loadVirtualInNodeId(final ConfigBaseRO settings) throws InvalidSettingsException {
        var virtualId = settings.getInt("virtual-in-ID");
        CheckUtils.checkSetting(virtualId >= 0, "Node ID < 0: %d", virtualId);
        return virtualId;
    }

    private static Integer loadVirtualOutNodeId(final ConfigBaseRO settings) throws InvalidSettingsException {
        var virtualId = settings.getInt("virtual-out-ID");
        CheckUtils.checkSetting(virtualId >= 0, "Node ID < 0: %d", virtualId);
        return virtualId;
    }

    private static List<PortDef> loadInPorts(final ConfigBaseRO settings) throws InvalidSettingsException {
        var inPortsSettings = loadSetting(settings, "inports");
        if (inPortsSettings == null || inPortsSettings.keySet().isEmpty()) {
            return List.of();
        }
        return loadPorts(inPortsSettings);
    }

    private static List<PortDef> loadOutPorts(final ConfigBaseRO settings) throws InvalidSettingsException {
        var outPortsSettings = loadSetting(settings, "outports");
        if (outPortsSettings == null || outPortsSettings.keySet().isEmpty()) {
            return List.of();
        }
        return loadPorts(outPortsSettings);
    }

    private static List<PortDef> loadPorts(final ConfigBaseRO portSettings) {
        return portSettings.keySet().stream() //
            .map(key -> {
                try {
                    return portSettings.getConfigBase(key);
                } catch (InvalidSettingsException e) { //NOSONAR
                    return null;
                }
            }).map(inPortSetting -> {
                try {
                    return loadPort(inPortSetting);
                } catch (InvalidSettingsException e) { //NOSONAR
                    return new PortDefBuilder().build();
                }
            }).collect(Collectors.toList());
    }

    private static PortDef loadPort(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (settings == null) {
            return new PortDefBuilder().build();
        }
        var index = settings.getInt("index");
        var portTypeSettings = settings.getConfigBase("type");
        var objectClassString = portTypeSettings.getString("object_class");
        if (objectClassString == null) {
            throw new InvalidSettingsException("No port object class found to create PortType object");
        }
        return new PortDefBuilder() //
            .setIndex(index) //
            .setName(settings.getKey()) //
            .setPortType(new PortTypeDefBuilder().setPortObjectClass(objectClassString).build()) //
            .build();
    }

}
