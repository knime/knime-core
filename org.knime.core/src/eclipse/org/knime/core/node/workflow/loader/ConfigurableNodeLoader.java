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
 *   31 Jan 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.ConfigurableNodeDef;
import org.knime.core.workflow.def.FlowContextDef;
import org.knime.core.workflow.def.FlowContextDef.ContextTypeEnum;
import org.knime.core.workflow.def.FlowObjectDef;
import org.knime.core.workflow.def.FlowVariableDef;
import org.knime.core.workflow.def.impl.ConfigMapDefBuilder;
import org.knime.core.workflow.def.impl.ConfigurableNodeDefBuilder;
import org.knime.core.workflow.def.impl.FlowContextDefBuilder;
import org.knime.core.workflow.def.impl.FlowObjectDefBuilder;
import org.knime.core.workflow.def.impl.FlowVariableDefBuilder;

/**
 * Responsible for loading the KNIME properties which are used by the Nodes and Componets
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
final class ConfigurableNodeLoader {

    private ConfigurableNodeLoader() {}

    private static final String INTERNAL_NODE_SUBSETTINGS_KEY = "internal_node_subsettings";

    private static final String VARIABLES_KEY = "variables";

    private static final String MODEL_KEY = "model";

    private static final String SCOPE_STACK_KEY = "scope_stack";

    private static final String FLOW_STACK_KEY = "flow_stack";

    private static final ConfigMapDef DEFAULT_CONFIG_MAP = new ConfigMapDefBuilder().build();

    static ConfigurableNodeDef load(final ConfigBaseRO workflowConfig, final ConfigBaseRO nodeConfig, final LoadVersion loadVersion) {

        return new ConfigurableNodeDefBuilder()//
            .setFlowStack(() -> loadFlowStackObjects(nodeConfig, loadVersion), List.of()) //
            .setInternalNodeSubSettings(() -> loadInternalNodeSubSettings(nodeConfig), DEFAULT_CONFIG_MAP) //
            .setModelSettings(() -> loadModelSettings(nodeConfig), DEFAULT_CONFIG_MAP) //
            .setVariableSettings(() -> loadVariableSettings(nodeConfig), DEFAULT_CONFIG_MAP)
            .setBaseNode(NodeLoader.load(workflowConfig, nodeConfig, loadVersion)) //
            .build();
    }

    /**
     * Loads the internal node sub settings (e.g memory_policy).
     *
     * @param settings a representation of the node's settings.xml file.
     * @return a {@link ConfigMapDef}
     * @throws InvalidSettingsException
     */
    private static ConfigMapDef loadInternalNodeSubSettings(final ConfigBaseRO settings)
        throws InvalidSettingsException {
        try {
            return CoreToDefUtil.toConfigMapDef(settings.getConfigBase(INTERNAL_NODE_SUBSETTINGS_KEY));
        } catch (InvalidSettingsException e) {
            var errorMessage = "Unable to load the interal node sub settings: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * Loads the variable settings.
     * TODO We need better explanation of this property
     *
     * @param settings a representation of the node's settings.xml file.
     * @return a {@link ConfigMapDef}
     * @throws InvalidSettingsException
     */
    private static ConfigMapDef loadVariableSettings(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (!settings.containsKey(VARIABLES_KEY)) {
            return new ConfigMapDefBuilder().build();
        }
        try {
            return CoreToDefUtil.toConfigMapDef(settings.getConfigBase(VARIABLES_KEY));
        } catch (InvalidSettingsException e) {
            var errorMessage = "Could not load variable settings: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * Loads the node's model settings (e.g SettingsModelID)
     * TODO We need better explanation of the model setting.
     * TODO When it used ?
     *
     * @param settings a representation of the node's settings.xml file.
     * @return a {@link ConfigMapDef}
     * @throws InvalidSettingsException
     */
    private static ConfigMapDef loadModelSettings(final ConfigBaseRO settings) throws InvalidSettingsException {
        // settings not present if node never had settings (different since 2.8 -- before the node always had settings,
        // defined by NodeModel#saveSettings -- these settings were never confirmed through validate/load though)
        try {
            if (settings.containsKey(MODEL_KEY)) {
                return CoreToDefUtil.toConfigMapDef(settings.getConfigBase(MODEL_KEY));
            } else {
                return new ConfigMapDefBuilder().build();
            }
        } catch (InvalidSettingsException e) {
            var errorMessage = "Unable to load model settings: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * Loads the list of the flow stack object defs from the settings file.
     *
     * @param settings to load from, ignored in this implementation (flow variables added in later versions)
     * @return either a list of {@link FlowObjectDef} or an empty list.
     * @throws InvalidSettingsException if that fails for any reason.
     */
    private static List<FlowObjectDef> loadFlowStackObjects(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            return Collections.emptyList();
        }
        try {
            var stackSet = loadVersion.isOlderThan(LoadVersion.V220) ? settings.getConfigBase(SCOPE_STACK_KEY)
                : settings.getConfigBase(FLOW_STACK_KEY);

            return stackSet.keySet().stream() //
                .map(key -> {
                    try {
                        if (stackSet.containsKey(key)) {
                            return stackSet.getConfigBase(key);
                        } else {
                            return null;
                        }
                    } catch (InvalidSettingsException e) {
                        // FIXME Is error prone?
                        throw new RuntimeException(e);
                    }
                }) //
                .map(t -> {
                    try {
                        //FIXME Provide different solution on this.
                        return loadFlowObjectDef(t);
                    } catch (InvalidSettingsException ex) {
                        throw new RuntimeException(ex);
                    }
                }) //
                .collect(Collectors.toList());
        } catch (Exception e) {
            var errorMessage = "Error loading flow variables: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * Loads the flow stack objects, either flow variable or flow context.
     *
     * @param settings a representation of the node's settings.xml file.
     * @return a {@link FlowObjectDef}
     * @throws InvalidSettingsException
     */
    private static FlowObjectDef loadFlowObjectDef(final ConfigBaseRO sub) throws InvalidSettingsException {
        if (sub == null) {
            return new FlowObjectDefBuilder().build();
        }
        try {
            var type = sub.getString("type");
            return "variable".equals(type) ? loadFlowVariableDef(sub) : loadFlowContextDef(type);
        } catch (InvalidSettingsException e) {
            var errorMessage = "Unable to load the flow stack object: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * Loads the flow context.
     *
     * @param type the loaded context type
     * @return a {@link FlowContextDef}
     * @throws InvalidSettingsException
     */
    private static FlowContextDef loadFlowContextDef(final String type) {

        boolean isActive = !type.toUpperCase().endsWith("_INACTIVE");

        return new FlowContextDefBuilder() //
            .setActive(isActive) //
            //TODO Which is the default context type?
            .setContextType(() -> loadFlowContextType(type), null) //
            .build();
    }

    private static ContextTypeEnum loadFlowContextType(final String type) {
        ContextTypeEnum contextType = null;

        if (type.toUpperCase().startsWith("LOOP")) {
            contextType = ContextTypeEnum.LOOP;
        } else if (type.toUpperCase().startsWith("FLOW")) {
            contextType = ContextTypeEnum.FLOWCAPTURE;
        } else if (type.toUpperCase().startsWith("SCOPE")) {
            contextType = ContextTypeEnum.SCOPE;
        }

        CheckUtils.checkNotNull(contextType, "Unknown flow object type: " + contextType);

        return contextType;
    }

    /**
     * TODO
     *
     * @param sub
     * @return
     * @throws InvalidSettingsException
     */
    private static FlowVariableDef loadFlowVariableDef(final ConfigBaseRO sub) {
        return new FlowVariableDefBuilder() //
            .setValue(() -> CoreToDefUtil.toConfigMapDef(sub), new ConfigMapDefBuilder().build()) //
            .build();

    }

}
