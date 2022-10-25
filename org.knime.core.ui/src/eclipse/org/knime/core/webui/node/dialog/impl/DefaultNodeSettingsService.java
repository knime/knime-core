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
 *   Jan 5, 2022 (hornm): created
 */
package org.knime.core.webui.node.dialog.impl;

import static org.knime.core.webui.node.dialog.impl.JsonNodeSettingsMapperUtil.jsonObjectToNodeSettings;

import java.util.Map;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.JsonNodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.TextNodeSettingsService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link TextNodeSettingsService} that translates {@link DefaultNodeSettings}-implementations into
 * {@link NodeSettings}-objects (on data apply) and vice-versa (initial data).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class DefaultNodeSettingsService implements JsonNodeSettingsService<String> {

    static final String FIELD_NAME_DATA = "data";

    static final String FIELD_NAME_SCHEMA = "schema";

    static final String FIELD_NAME_UI_SCHEMA = "ui_schema";

    private final Map<SettingsType, Class<? extends DefaultNodeSettings>> m_settingsClasses;

    /**
     * @param settingsClasses map that associates a {@link DefaultNodeSettings}-class with a {@link SettingsType}
     */
    public DefaultNodeSettingsService(final Map<SettingsType, Class<? extends DefaultNodeSettings>> settingsClasses) {
        m_settingsClasses = settingsClasses;
    }

    @Override
    public void toNodeSettingsFromObject(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
        try {
            final var root = (ObjectNode)JsonFormsDataUtil.getMapper().readTree(textSettings);
            textSettingsToNodeSettings(root, SettingsType.MODEL, settings);
            textSettingsToNodeSettings(root, SettingsType.VIEW, settings);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                String.format("Exception when writing data to node settings: %s", e.getMessage()), e);
        }
    }

    private void textSettingsToNodeSettings(final ObjectNode rootNode, final SettingsType settingsType,
        final Map<SettingsType, NodeSettingsWO> settings) {
        if (settings.containsKey(settingsType)) {
            final var node = rootNode.get(settingsType.getConfigKey());
            if (node.isObject()) {
                final var schema = JsonFormsSchemaUtil.buildSchema(m_settingsClasses.get(settingsType));
                jsonObjectToNodeSettings((ObjectNode)node, schema, settings.get(settingsType));
            }
        }
    }

    @Override
    public String fromNodeSettingsToObject(final Map<SettingsType, NodeSettingsRO> settings,
        final PortObjectSpec[] specs) {
        final var root = JsonFormsDataUtil.getMapper().createObjectNode();
        final var jsonFormsSettings = new JsonFormsSettingsImpl(m_settingsClasses, settings, specs);
        root.set(FIELD_NAME_DATA, jsonFormsSettings.getData());
        root.set(FIELD_NAME_SCHEMA, jsonFormsSettings.getSchema());
        root.putRawValue(FIELD_NAME_UI_SCHEMA, jsonFormsSettings.getUiSchema());
        return root.toString();
    }

    @Override
    public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings, final PortObjectSpec[] specs) {
        saveDefaultSettings(settings, SettingsType.MODEL, specs);
        saveDefaultSettings(settings, SettingsType.VIEW, specs);
    }

    private void saveDefaultSettings(final Map<SettingsType, NodeSettingsWO> settingsMap,
        final SettingsType settingsType, final PortObjectSpec[] specs) {
        var nodeSettings = settingsMap.get(settingsType);
        var settingsClass = m_settingsClasses.get(settingsType);
        if (nodeSettings != null && settingsClass != null) {
            var settings = DefaultNodeSettings.createSettings(settingsClass, specs);
            DefaultNodeSettings.saveSettings(settingsClass, settings, specs, nodeSettings);
        }
    }

    @Override
    public String fromJson(final String json) {
        return json;
    }

    @Override
    public String toJson(final String obj) {
        return obj;
    }

}
