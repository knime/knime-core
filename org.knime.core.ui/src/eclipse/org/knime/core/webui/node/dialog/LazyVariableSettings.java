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
 */
package org.knime.core.webui.node.dialog;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;

/**
 * An implementation of {@link VariableSettingsWO} that only adds the settings for the variables to the node settings if
 * necessary.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class LazyVariableSettings implements VariableSettingsWO {

    // See org.knime.core.node.config.ConfigEditTreeModel#CURRENT_VERSION
    private static final String CURRENT_VERSION = "V_2019_09_13";

    private static final String VERSION_CFG_KEY = "version";

    private static final String EXPOSED_VARIABLE_CFG_KEY = "exposed_variable";

    private static final String USED_VARIABLE_CFG_KEY = "used_variable";

    private final NodeSettings m_nodeSettings;

    private final SettingsType m_type;

    private VariableSettingsTree m_settingsTree;

    private VariableSettingsTree getVariablesTree() throws InvalidSettingsException {
        // Create the variable settings on demand
        if (m_settingsTree == null) {

            // The settings
            var settings = m_nodeSettings.getNodeSettings(m_type.getConfigKey());

            // The variable settings
            var variableSettings = getOrCreateSubSettings(m_nodeSettings, m_type.getVariablesConfigKey());
            addStringIfNotPresent(variableSettings, VERSION_CFG_KEY, CURRENT_VERSION);
            var variablesRoot = getOrCreateSubSettings(variableSettings, "tree");

            m_settingsTree = new VariableSettingsTree(variablesRoot, settings);
        }
        return m_settingsTree;
    }

    LazyVariableSettings(final NodeSettings nodeSettings, final SettingsType type) {
        m_nodeSettings = nodeSettings;
        m_type = type;
    }

    @Override
    public VariableSettingsWO getChild(final String key) throws InvalidSettingsException {
        return getVariablesTree().getChild(key);
    }

    @Override
    public void addUsedVariable(final String settingsKey, final String usedVariable) throws InvalidSettingsException {
        getVariablesTree().addUsedVariable(settingsKey, usedVariable);
    }

    @Override
    public void addExposedVariable(final String settingsKey, final String exposedVariable)
        throws InvalidSettingsException {
        getVariablesTree().addExposedVariable(settingsKey, exposedVariable);
    }

    private static NodeSettings getOrCreateSubSettings(final NodeSettings settings, final String key)
        throws InvalidSettingsException {
        NodeSettings subSettings;
        if (settings.containsKey(key)) {
            subSettings = settings.getNodeSettings(key);
        } else {
            subSettings = new NodeSettings(key);
            settings.addNodeSettings(subSettings);
        }
        return subSettings;
    }

    private static void addStringIfNotPresent(final NodeSettings settings, final String key, final String value) {
        if (!settings.containsKey(key)) {
            settings.addString(key, value);
        }
    }

    static final class VariableSettingsTree implements VariableSettingsWO {

        private final NodeSettings m_variablesRoot;

        private final NodeSettings m_settings;

        public VariableSettingsTree(final NodeSettings variablesRoot, final NodeSettings settings) {
            m_variablesRoot = variablesRoot;
            m_settings = settings;
        }

        @Override
        public VariableSettingsWO getChild(final String key) throws InvalidSettingsException {
            // NB: m_settings.getNodeSettings throws an exception if the object does not exist - we want this
            return new VariableSettingsTree(getOrCreateSubSettings(m_variablesRoot, key),
                m_settings.getNodeSettings(key));
        }

        @Override
        public void addUsedVariable(final String settingsKey, final String usedVariable)
            throws InvalidSettingsException {
            if (!m_settings.containsKey(settingsKey)) {
                throw new InvalidSettingsException("Cannot overwrite the setting '" + settingsKey
                    + "' with the flow variable '" + usedVariable + "' because the setting does not exist.");
            }

            final var s = getOrCreateSubSettings(m_variablesRoot, settingsKey);
            s.addString(USED_VARIABLE_CFG_KEY, usedVariable);
            addStringIfNotPresent(s, EXPOSED_VARIABLE_CFG_KEY, null);
        }

        @Override
        public void addExposedVariable(final String settingsKey, final String exposedVariable)
            throws InvalidSettingsException {
            if (!m_settings.containsKey(settingsKey)) {
                throw new InvalidSettingsException("Cannot expose the setting '" + settingsKey
                    + "' with the flow variable '" + exposedVariable + "' because the setting does not exist.");
            }

            final var s = getOrCreateSubSettings(m_variablesRoot, settingsKey);
            s.addString(EXPOSED_VARIABLE_CFG_KEY, exposedVariable);
            addStringIfNotPresent(s, USED_VARIABLE_CFG_KEY, null);
        }
    }
}
