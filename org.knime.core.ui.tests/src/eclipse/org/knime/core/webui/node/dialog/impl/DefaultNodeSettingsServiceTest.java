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
 *   9 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.impl;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.knime.core.webui.node.dialog.impl.DefaultNodeSettingsService.FIELD_NAME_DATA;
import static org.knime.core.webui.node.dialog.impl.DefaultNodeSettingsService.FIELD_NAME_SCHEMA;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests {@link DefaultNodeSettingsService}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class DefaultNodeSettingsServiceTest {

    private static final ObjectMapper MAPPER = JsonFormsDataUtil.getMapper();

    @SuppressWarnings("unused")
    private static class TestSettings implements DefaultNodeSettings {
        String m_value;

        TestSettings() {
        }

        TestSettings(final String value) {
            m_value = value;
        }

        TestSettings(final SettingsCreationContext context) {
            m_value = (context.getDataTableSpecs()[0]).getColumnSpec(0).getName();
        }
    }

    @Test
    void testGetInitialDataFromEmptySettings() throws JsonProcessingException {
        final var nodeSettings = new NodeSettings("node_settings");

        // try to obtain initial data using empty node settings
        Assertions
            .assertThatThrownBy(
                () -> obtainAndCheckInitialData(null, nodeSettings, new PortObjectSpec[]{new DataTableSpec()}))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetInitialData() throws JsonProcessingException {
        final var viewData = (ObjectNode)JsonFormsDataUtil.toJsonData(new TestSettings("foo"));
        final var specs = JsonFormsDataUtilTest.createSpecs("bar");
        final var viewDataSchema = JsonFormsSchemaUtil.buildSchema(TestSettings.class,
            DefaultNodeSettings.createSettingsCreationContext(specs));
        final var nodeSettings = new NodeSettings("node_settings");
        JsonNodeSettingsMapperUtil.jsonObjectToNodeSettings(viewData, viewDataSchema, nodeSettings);

        // obtain initial data using "foo" node settings and "bar" specs and compare against "foo" view data
        obtainAndCheckInitialData(viewData, nodeSettings, specs);
    }

    private static void obtainAndCheckInitialData(final JsonNode viewData, final NodeSettings nodeSettings,
        final PortObjectSpec[] specs) throws JsonProcessingException {

        // create settings service and obtain initial data using node settings and specs
        final var settingsService = new DefaultNodeSettingsService(Map.of(SettingsType.VIEW, TestSettings.class));
        final var initialData =
            MAPPER.readTree(settingsService.fromNodeSettings(Map.of(SettingsType.VIEW, nodeSettings), specs));

        // assert that returned data is equal to wrapped "foo" view data created via JsonFormsDataUtil
        final var wrappedViewData = MAPPER.createObjectNode().set(SettingsType.VIEW.getConfigKey(), viewData);
        assertThatJson(initialData.get(FIELD_NAME_DATA)).isEqualTo(wrappedViewData);

        // assert that returned schema is equal to wrapped schema created via JsonFormsSchemaUtil
        final var schema = JsonFormsSchemaUtil.buildSchema(TestSettings.class,
            DefaultNodeSettings.createSettingsCreationContext(specs));
        final var wrappedSchema = MAPPER.createObjectNode();
        wrappedSchema.put("type", "object").putObject("properties").set(SettingsType.VIEW.getConfigKey(), schema);
        assertThatJson(initialData.get(FIELD_NAME_SCHEMA)).isEqualTo(wrappedSchema);
    }

    @Test
    void testApplyData() throws InvalidSettingsException, JsonProcessingException {
        // create "foo" view data and empty node settings
        final var viewData = JsonFormsDataUtil.toJsonData(new TestSettings("foo"));
        final var nodeSettings = new NodeSettings("node_settings");

        // create settings service and apply wrapped "foo" view data into node settings
        final var settingsService = new DefaultNodeSettingsService(Map.of(SettingsType.VIEW, TestSettings.class));
        final var wrappedViewData = MAPPER.createObjectNode().set(SettingsType.VIEW.getConfigKey(), viewData);
        settingsService.toNodeSettings(wrappedViewData.toString(), Map.of(SettingsType.VIEW, nodeSettings));

        // assert that node settings are no longer empty but equal to the "foo" view data
        final var node = JsonFormsDataUtil.getMapper().createObjectNode();
        JsonNodeSettingsMapperUtil.nodeSettingsToJsonObject(nodeSettings, node);
        assertThatJson(node).isEqualTo(viewData);
    }

}
