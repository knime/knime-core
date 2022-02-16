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


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.ComponentDef;
import org.knime.core.workflow.def.ComponentMetadataDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.TemplateLinkDef;
import org.knime.core.workflow.def.impl.ComponentDefBuilder;
import org.knime.core.workflow.def.impl.ComponentMetadataDefBuilder;
import org.knime.core.workflow.def.impl.PortDefBuilder;
import org.knime.core.workflow.def.impl.PortTypeDefBuilder;
import org.knime.core.workflow.def.impl.TemplateLinkDefBuilder;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
public class SubNodeLoader extends SingleNodeLoader {

    /**
     * Constructor
     */
    SubNodeLoader() {
        super(new ComponentDefBuilder());
    }


    @Override
    SubNodeLoader load(final ConfigBaseRO parentSettings, final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        super.load(parentSettings, settings, loadVersion);

        //The load methods should throw specific error messages
        getNodeBuilder().setDialogSettings(null) //
            .setInPorts(loadInPorts(settings)) //
            .setOutPorts(loadOutPorts(settings)) //
            .setVirtualInNodeId(loadVirtualInNodeId(settings)) //
            .setVirtualOutNodeId(loadVirtualOutNodeId(settings)) //
            // TODO We should pass the proper setting for the link, currently passing the template.knime.
            .setLink(loadLink(settings)) //
            .setMetadata(null) //
            //TODO We should use the WorkflowLoader.
            .setWorkflow(null);

        return this;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    ComponentDefBuilder getNodeBuilder() {
        return (ComponentDefBuilder)super.getNodeBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ComponentDef getNodeDef() {
        return getNodeBuilder().build();
    }

    private TemplateLinkDef loadLink(final ConfigBaseRO settings) {
        // TODO This mehtod will be improved after the new error handling
        String uri = null;
        try {
            var templateSettings = settings.getConfigBase("workflow_template_information");
            uri = templateSettings.getString("sourceURI");
//            if (uri.isBlank()) {
//                throw new InvalidSettingsException("Cannot not read source URI from emtpy string");
//            }
        } catch (InvalidSettingsException e) {
            //FIXME After new error handling

        }
        return new TemplateLinkDefBuilder() //
            .setUri(uri) //
            .build();
    }

    private ComponentMetadataDef loadMetadata(final ConfigBaseRO settings) {
        try {
            var metadataSettings = settings.getConfigBase("metadata");
            var description = settings.getString("description");

            //TODO Use Collectors.groupingBy to return Map<String, List<String>
            // use the map to fill the following list fields
            return new ComponentMetadataDefBuilder() //
                .setDescription(description) //
                //TODO What is this?
                .setIcon(null) //
                .setInPortDescriptions(null) //
                .setInPortNames(null) //
                .setOutPortDescriptions(null) //
                .setOutPortNames(null) //
                .build();
        } catch (InvalidSettingsException e) {
            //FIXME After new error handling
            return new ComponentMetadataDefBuilder() //
                .build();
        }
    }

    private Integer loadVirtualInNodeId(final ConfigBaseRO settings) {
        try {
            var virtualId = settings.getInt("virtual-in-ID");
            CheckUtils.checkSetting(virtualId >= 0, "Node ID < 0: %d", virtualId);
            return virtualId;
        } catch (InvalidSettingsException e) {
            //FIXME After new error handling
            String error = "Can't load virtual input node ID: " + e.getMessage();
            return 0;
        }
    }

    private Integer loadVirtualOutNodeId(final ConfigBaseRO settings) {
        try {
            var virtualId = settings.getInt("virtual-out-ID");
            CheckUtils.checkSetting(virtualId >= 0, "Node ID < 0: %d", virtualId);
            return virtualId;
        } catch (InvalidSettingsException e) {
            //FIXME After new error handling
            String error = "Can't load virtual output node ID: " + e.getMessage();
            return 0;
        }
    }

    private List<PortDef> loadInPorts(final ConfigBaseRO settings) throws InvalidSettingsException {
        var inPortsSettings = settings.getConfigBase("inports");
        if (inPortsSettings.keySet().isEmpty()) {
            return Collections.emptyList();
        }
        return loadPorts(inPortsSettings);
    }

    private List<PortDef> loadOutPorts(final ConfigBaseRO settings) throws InvalidSettingsException {
        var outPortsSettings = settings.getConfigBase("outports");
        if (outPortsSettings.keySet().isEmpty()) {
            return Collections.emptyList();
        }
        return loadPorts(outPortsSettings);
    }

    private List<PortDef> loadPorts(final ConfigBaseRO portSettings) {
        //TODO ConfigBaseRO should support entrySet()
        return portSettings.keySet().stream() //
            .map(key -> {
                try {
                    return portSettings.getConfigBase(key);
                } catch (InvalidSettingsException e) {
                    //FIXME After new error handling implementation
                    return null;
                }
            }).map(inPortSetting -> {
                try {
                    return loadPort(inPortSetting);
                } catch (InvalidSettingsException | NullPointerException e) {
                    //FIXME After the decision of the error handling
                    //FIXME Nullpointer should be removed
                    return new PortDefBuilder().build();
                }
            }).collect(Collectors.toList());
    }

    private static PortDef loadPort(final ConfigBaseRO settings) throws InvalidSettingsException {
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
