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
 *   Mar 23, 2022 (hornm): created
 */
package org.knime.gateway.api.entity;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;

/**
 * Represents a node dialog's flow variable settings. I.e. which settings are overwritten by flow variables or exposed
 * as flow variables.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class FlowVariableSettingsEnt {

    private static final String CFG_VIEW_VARIABLES = "view_variables";

    private static final String CFG_MODEL_VARIABLES = "variables";

    private final Map<String, Object> m_modelVariables;

    private final Map<String, Object> m_viewVariables;

    FlowVariableSettingsEnt(final NativeNodeContainer nnc) {
        var nodeSettings = nnc.getNodeSettings();
        m_modelVariables = createSettingsTree(CFG_MODEL_VARIABLES, nodeSettings);
        m_viewVariables = createSettingsTree(CFG_VIEW_VARIABLES, nodeSettings);
    }

    private static Map<String, Object> createSettingsTree(final String settingsKey, final NodeSettings nodeSettings) {
        try {
            var ns = nodeSettings.getNodeSettings(settingsKey).getNodeSettings("tree");
            return createSettingsTree(ns, new HashMap<>());
        } catch (InvalidSettingsException ex) { // NOSONAR
            return null; // NOSONAR
        }
    }

    private static Map<String, Object> createSettingsTree(final NodeSettings nodeSettings,
        final Map<String, Object> settingsTreeNodeEnts) {
        for (var key : nodeSettings) {
            try {
                var subSettings = nodeSettings.getNodeSettings(key);
                if (hasOnlyLeafChildren(subSettings)) {
                    settingsTreeNodeEnts.put(key, new SettingsTreeLeafEnt(subSettings.getString("used_variable", null),
                        subSettings.getString("exposed_variable", null)));
                } else {
                    settingsTreeNodeEnts.put(key, createSettingsTree(subSettings, new HashMap<>()));
                }
            } catch (InvalidSettingsException ex) { // NOSONAR
                //
            }
        }
        return settingsTreeNodeEnts;
    }

    private static boolean hasOnlyLeafChildren(final NodeSettings ns) {
        for (var key : ns) {
            try {
                if (!ns.getNodeSettings(key).isLeaf()) {
                    return false;
                }
            } catch (InvalidSettingsException ex) { // NOSONAR
                //
            }
        }
        return true;
    }

    /**
     * @return the model flow variable settings tree
     */
    public Map<String, Object> getModelVariables() {
        return m_modelVariables;
    }

    /**
     * @return the view flow variable settings tree
     */
    public Map<String, Object> getViewVariables() {
        return m_viewVariables;
    }

    private static class SettingsTreeLeafEnt {

        private final String m_controllingFlowVariableName;

        private final String m_exposedFlowVariableName;

        SettingsTreeLeafEnt(final String controllingFlowVariableName, final String exposedFlowVariableName) {
            m_controllingFlowVariableName = controllingFlowVariableName;
            m_exposedFlowVariableName = exposedFlowVariableName;
        }

        @SuppressWarnings("unused")
        public boolean isLeaf() {
            return true;
        }

        @SuppressWarnings("unused")
        public String getControllingFlowVariableName() {
            return m_controllingFlowVariableName;
        }

        @SuppressWarnings("unused")
        public String getExposedFlowVariableName() {
            return m_exposedFlowVariableName;
        }

    }

}
