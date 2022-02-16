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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.FlowContextDef;
import org.knime.core.workflow.def.FlowContextDef.ContextTypeEnum;
import org.knime.core.workflow.def.FlowObjectDef;
import org.knime.core.workflow.def.FlowVariableDef;
import org.knime.core.workflow.def.SingleNodeDef;
import org.knime.core.workflow.def.impl.FlowContextDefBuilder;
import org.knime.core.workflow.def.impl.FlowObjectDefBuilder;
import org.knime.core.workflow.def.impl.FlowVariableDefBuilder;
import org.knime.core.workflow.def.impl.SingleNodeDefBuilder;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public abstract class SingleNodeLoader extends NodeLoader {

    /**
     *
     * Can be native, component builder
     */
    SingleNodeLoader(final SingleNodeDefBuilder singleNodeDefBuilder) {
        super(singleNodeDefBuilder);
    }

    @Override
    SingleNodeLoader load(final ConfigBaseRO workflowConfig, final File nodeDirectory, final LoadVersion loadVersion)
        throws InvalidSettingsException, IOException {
        super.load(workflowConfig, nodeDirectory, loadVersion);

        //The load methods should throw specific error messages
        getNodeBuilder()//
            .setFlowStack(loadFlowStackObjects(m_nodeConfig, loadVersion)) //
            .setInternalNodeSubSettings(loadInternalNodeSubSettings(m_nodeConfig)) //
            .setModelSettings(loadModelSettings(m_nodeConfig)) //
            .setVariableSettings(loadVariableSettings(m_nodeConfig));

        return this;
    }

    @Override
    protected ConfigBaseRO loadNodeConfig(final ConfigBaseRO workflowConfig, final File nodeDirectory) {

        // TODO const string
        File configFile = new File(nodeDirectory, "settings.xml");
        // TODO decipher
//        InputStream in = new FileInputStream(configFile);
//        try {
//            in = getParentPersistor().decipherInput(in);
//            return NodeSettings.loadFromXML(new BufferedInputStream(in));
//        } finally {
//            try {
//                in.close();
//            } catch (IOException e) {
//                getLogger().error("Failed to close input stream on \""
//                        + configFile.getAbsolutePath() + "\"", e);
//            }
//        }
        try {
            return SimpleConfig.parseConfig(configFile.getAbsolutePath(), configFile);
        } catch (IOException ex) {
            // TODO error handling
            // var error = "Unable to load settings for node with ID suffix " + nodeIDSuffix;
            //            getLogger().debug(error, e);
            //            loadResult.setDirtyAfterLoad();
            //            loadResult.addError(error);
            throw new IllegalArgumentException("Cannot load settings.xml ", ex);
        }
    }

    private static ConfigMapDef loadInternalNodeSubSettings(final ConfigBaseRO settings)
        throws InvalidSettingsException {
        return CoreToDefUtil.toConfigMapDef(settings.getConfigBase("internal_node_subsettings"));
    }

    private static ConfigMapDef loadVariableSettings(final ConfigBaseRO settings) throws InvalidSettingsException {
        // TODO Need to clarify what exactly is this
        if(!settings.containsKey("variables")) {
            return null;
        }
        return CoreToDefUtil.toConfigMapDef(settings.getConfigBase("variables"));
    }

    private static ConfigMapDef loadModelSettings(final ConfigBaseRO settings) {
        // settings not present if node never had settings (different since 2.8 -- before the node always had settings,
        // defined by NodeModel#saveSettings -- these settings were never confirmed through validate/load though)
        try {
            if (settings.containsKey("model")) {
                return CoreToDefUtil.toConfigMapDef(settings.getConfigBase("model"));
            }
        } catch (InvalidSettingsException e) {
            // throw specific exception message form this lever of fucntionality
            // (String.format("Unable to load the model settings: %s", e.getMessage()));
            //TODO Logs and setDirty
            //TODO May use the CheckedFunction somehow
        }
        return null;
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
        var stackSet = loadVersion.isOlderThan(LoadVersion.V220) ? settings.getConfigBase("scope_stack")
            : settings.getConfigBase("flow_stack");

        return stackSet.keySet().stream() //
            .map(key -> {
                try {
                    if (stackSet.containsKey(key)) {
                        return stackSet.getConfigBase(key);
                    } else {
                        return null;
                    }

                } catch (InvalidSettingsException e) {
                    throw new RuntimeException(e);
                }
            }) //
            .map(SingleNodeLoader::loadFlowObjectDef) //
            .collect(Collectors.toList());
    }

    private static FlowObjectDef loadFlowObjectDef(final ConfigBaseRO sub) {
        try {
            var type = sub.getString("type");
            if ("variable".equals(type)) {
                return loadFlowVariableDef(sub);
            } else {
                // TODO If LoadVersion older than 4.6.0 then read the active from the settings.xml
                //TODO Throw invalid exception if the type does't exist
                return loadFlowContextDef(type);
            }
        } catch (InvalidSettingsException e) {
            //TODO Proper error handling
            return new FlowObjectDefBuilder().build();
        }

    }

    private static FlowContextDef loadFlowContextDef(final String type) throws InvalidSettingsException {
        ContextTypeEnum contextType = type.startsWith("LOOP") ? ContextTypeEnum.LOOP : null;
        contextType = type.startsWith("FLOW") ? ContextTypeEnum.FLOWCAPTURE : null;
        contextType = type.startsWith("SCOPE") ? ContextTypeEnum.SCOPE : null;
        boolean isActive = !type.endsWith("_INACTIVE");

        CheckUtils.checkNotNull(contextType, "Unknown flow object type: " + contextType);

        // TODO directly load the isActive from the settings and set it, no addtitional conversions for the rest string valiues
        // TODO probably the following will be the implemention for the loaders with version lower than 4.6.0
        return new FlowContextDefBuilder() //
            .setActive(isActive) //
            .setContextType(contextType) //
            .build();
    }

    private static FlowVariableDef loadFlowVariableDef(final ConfigBaseRO sub) throws InvalidSettingsException {
        return new FlowVariableDefBuilder() //
            .setValue(CoreToDefUtil.toConfigMapDef(sub)) //
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    SingleNodeDefBuilder getNodeBuilder() {
        return (SingleNodeDefBuilder)super.getNodeBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    SingleNodeDef getNodeDef() {
        return getNodeBuilder().build();
    }
}
