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

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Marker interface for implementations that define a {@link DefaultNodeDialog}. The implementations allow one to
 * declare the dialog's settings and widgets in a compact manner.
 *
 * The implementations must follow the following conventions:
 * <ol>
 * <li>It must provide an empty constructor and optionally a constructor that receives an array of {@link PortObjectSpec
 * PortObjectSpecs}. NOTE: array of specs can contain {@code null} values, e.g., if input port is not connected!
 * <li>Fields must be of any of the following supported types:
 * <ul>
 * <li>boolean, int, long, double, float, String, Character, char, CharSequence, Byte, or byte
 * <li>POJOs, arrays or Collections holding other supported types
 * </ul>
 * </ol>
 *
 * All fields with visibility of at least 'package scope' are represented as dialog widgets; they can optionally be
 * annotated with {@link Schema} to supply additional information (e.g. description, domain info, ...).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public interface DefaultNodeSettings {

    /**
     * Verifies a given node settings implementation, making sure that it follows the contract of
     * {@link DefaultNodeSettings}, as defined in its documentation.
     *
     * @param settingsClass the settings class to verify
     */
    static void verifySettings(final Class<? extends DefaultNodeSettings> settingsClass) {
        try {
            settingsClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            NodeLogger.getLogger(DefaultNodeSettings.class).errorWithFormat(
                "Default node settings class %s does not provide a default constructor.",
                settingsClass.getSimpleName());
        } catch (SecurityException e) {
            NodeLogger.getLogger(DefaultNodeSettings.class)
                .error(String.format(
                    "Exception when attempting to access default constructor of default node settings class %s.",
                    settingsClass.getSimpleName()), e);
        }
    }

    /**
     * Helper to serialize a {@link DefaultNodeSettings} of specified class from a {@link NodeSettingsRO}-object.
     *
     * @param <S>
     * @param settings the settings-object to create the instance from
     * @param clazz default node settings class
     * @return a new {@link DefaultNodeSettings}-instance
     */
    static <S extends DefaultNodeSettings> S loadSettings(final NodeSettingsRO settings, final Class<S> clazz) {
        final var node = JsonFormsDataUtil.getMapper().createObjectNode();
        JsonNodeSettingsMapperUtil.nodeSettingsToJsonObject(settings, node);
        return JsonFormsDataUtil.toDefaultNodeSettings(node, clazz);
    }

    /**
     * Helper to create a new {@link DefaultNodeSettings} of the specified type.
     *
     * @param <S>
     * @param clazz default node settings class
     * @param specs the specs with which to create the settings. NOTE: can contain {@code null} values, e.g., if input
     *            port is not connected
     * @return a new {@link DefaultNodeSettings}-instance
     */
    static <S extends DefaultNodeSettings> S createSettings(final Class<S> clazz, final PortObjectSpec[] specs) {
        return JsonFormsDataUtil.createDefaultNodeSettings(clazz, specs);
    }

    /**
     * Helper to serialize a {@link DefaultNodeSettings}-instance into a {@link NodeSettingsWO}-object.
     *
     * @param settingsClass the setting object's class
     * @param settingsObject the default node settings object to serialize
     * @param specs the specs with which to create the schema. NOTE: can contain {@code null}-values, e.g., if input
     *            port is not connected
     * @param settings the settings to write to
     */
    static void saveSettings(final Class<? extends DefaultNodeSettings> settingsClass,
        final DefaultNodeSettings settingsObject, final PortObjectSpec[] specs, final NodeSettingsWO settings) {
        var objectNode = (ObjectNode)JsonFormsDataUtil.toJsonData(settingsObject);
        var schemaNode = JsonFormsSchemaUtil.buildSchema(settingsClass, specs);
        JsonNodeSettingsMapperUtil.jsonObjectToNodeSettings(objectNode, schemaNode, settings);
    }

}
