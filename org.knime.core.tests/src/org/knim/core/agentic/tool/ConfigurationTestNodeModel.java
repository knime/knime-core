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
 *   May 23, 2025 (hornm): created
 */
package org.knim.core.agentic.tool;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodePanel;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.dialog.SubNodeDescriptionProvider;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.util.JsonUtil;

import jakarta.json.JsonException;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class ConfigurationTestNodeModel extends TestNodeModel implements DialogNode {

    static JsonValue jsonValue;

    ConfigurationTestNodeModel() {
        super(null, FlowVariablePortObject.TYPE);
    }

    @Override
    public void saveCurrentValue(final NodeSettingsWO content) {
        //
    }

    @Override
    public DialogNodeRepresentation getDialogRepresentation() {
        return new TestDialogNodeRepresentation();
    }

    class TestDialogNodeRepresentation implements DialogNodeRepresentation, SubNodeDescriptionProvider {

        @Override
        public DialogNodePanel createDialogPanel() {
            return null;
        }

        @Override
        public String getLabel() {
            return "config label";
        }

        @Override
        public String getDescription() {
            return "config decription";
        }

    }

    @Override
    public DialogNodeValue createEmptyDialogValue() {
        return new DialogNodeValueAdapter();
    }

    @Override
    public void setDialogValue(final DialogNodeValue value) {
        jsonValue = value.toJson();
    }

    @Override
    public DialogNodeValue getDefaultValue() {
        return new DialogNodeValueAdapter() {

            @Override
            public JsonValue toJson() {
                final JsonObjectBuilder builder = JsonUtil.getProvider().createObjectBuilder();
                builder.add("type", "string");
                builder.add("default", "default config value");
                return builder.build();
            }

        };
    }

    @Override
    public DialogNodeValue getDialogValue() {
        return null;
    }

    @Override
    public void validateDialogValue(final DialogNodeValue value) throws InvalidSettingsException {
        //
    }

    @Override
    public String getParameterName() {
        return "configuration-parameter-name";
    }

    @Override
    public boolean isHideInDialog() {
        return false;
    }

    @Override
    public void setHideInDialog(final boolean hide) {
        //
    }

    class DialogNodeValueAdapter implements DialogNodeValue {

        private JsonValue m_json;

        @Override
        public JsonValue toJson() {
            return m_json;
        }

        @Override
        public void saveToNodeSettings(final NodeSettingsWO settings) {
            //
        }

        @Override
        public void loadFromString(final String fromCmdLine) throws UnsupportedOperationException {
            //
        }

        @Override
        public void loadFromNodeSettingsInDialog(final NodeSettingsRO settings) {
            //
        }

        @Override
        public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            //
        }

        @Override
        public void loadFromJson(final JsonValue json) throws JsonException {
            m_json = json;
        }
    }

}
