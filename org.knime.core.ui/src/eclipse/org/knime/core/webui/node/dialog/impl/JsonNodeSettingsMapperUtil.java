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
 *   10 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.impl;

import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_ANYOF;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_FORMAT;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_FORMAT_DOUBLE;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_FORMAT_FLOAT;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_FORMAT_INT;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_FORMAT_LONG;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_ITEMS;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_ONEOF;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_PROPERTIES;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE_ARRAY;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE_BOOLEAN;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE_INTEGER;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE_NULL;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE_NUMBER;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE_OBJECT;
import static org.knime.core.webui.node.dialog.impl.JsonFormsSchemaUtil.TAG_TYPE_STRING;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javax.swing.tree.TreeNode;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.ConfigBooleanEntry;
import org.knime.core.node.config.base.ConfigDoubleEntry;
import org.knime.core.node.config.base.ConfigFloatEntry;
import org.knime.core.node.config.base.ConfigIntEntry;
import org.knime.core.node.config.base.ConfigLongEntry;
import org.knime.core.node.config.base.ConfigStringEntry;
import org.knime.core.node.defaultnodesettings.SettingsModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper to translate json-objects ({@link ObjectNode}) into {@link NodeSettings} and vice-versa.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class JsonNodeSettingsMapperUtil {

    static final String FIELD_NAME_TYPE = "json-type" + SettingsModel.CFGKEY_INTERNAL;

    static final String FIELD_NAME_NULL = "null" + SettingsModel.CFGKEY_INTERNAL;

    // see org.knime.core.node.config.base.ConfigBase.CFG_ARRAY_SIZE
    static final String FIELD_NAME_ARRAY_SIZE = "array-size";

    private enum Type {
            DOUBLE {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    checkNotNull(node, schemaNode);
                    settings.addDouble(key, node.doubleValue());
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    node.put(key, settings.getDouble(key));
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    // code should be unreachable, since we only allow typed arrays and there is a DOUBLE_ARRAY
                    node.add(settings.getDouble(Integer.toString(index)));
                }
            },
            FLOAT {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    checkNotNull(node, schemaNode);
                    settings.addFloat(key, node.floatValue());
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    node.put(key, settings.getFloat(key));
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    // code should be unreachable, since we only allow typed arrays and there is a FLOAT_ARRAY
                    node.add(settings.getFloat(Integer.toString(index)));
                }
            },
            INT {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    checkNotNull(node, schemaNode);
                    settings.addInt(key, node.intValue());
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    node.put(key, settings.getInt(key));
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    // code should be unreachable, since we only allow typed arrays and there is a INT_ARRAY
                    node.add(settings.getInt(Integer.toString(index)));
                }

            },
            LONG {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    checkNotNull(node, schemaNode);
                    settings.addLong(key, node.longValue());
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    node.put(key, settings.getLong(key));
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    // code should be unreachable, since we only allow typed arrays and there is a LONG_ARRAY
                    node.add(settings.getLong(Integer.toString(index)));
                }
            },
            BOOLEAN {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    checkNotNull(node, schemaNode);
                    settings.addBoolean(key, node.booleanValue());
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    node.put(key, settings.getBoolean(key));
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    node.add(settings.getBoolean(Integer.toString(index)));
                }

            },
            NULL {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    createTypedSubSettings(key, schemaNode, settings);
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node) {
                    node.putNull(key);
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node) {
                    node.addNull();
                }
            },
            STRING {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    settings.addString(key, node == null ? NullNode.getInstance().textValue() : node.textValue());
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    node.put(key, settings.getString(key));
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    // code should be unreachable, since we only allow typed arrays and there is a STRING_ARRAY
                    node.add(settings.getString(Integer.toString(index)));
                }
            },
            OBJECT {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    /**
                     * Regarding the handling of null values: The node settings can't handle null values for objects,
                     * therefore we add an empty object here and traverse further according to the schema. This means
                     * that, e.g., if the schema suggests the object node to hold some "foo" property of type string,
                     * when converting the setting back to json, we get {"foo":null} instead of a null value. However,
                     * it also means that we associate the setting (as well as its sub-settings) with the correct flow
                     * variables.
                     */
                    addFieldsToSettings(node == null || node.isNull() ? JsonFormsDataUtil.getMapper().createObjectNode()
                        : (ObjectNode)node, schemaNode, createTypedSubSettings(key, schemaNode, settings));
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    putFieldsFromSettings(settings.getNodeSettings(key), node.putObject(key));
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    putFieldsFromSettings(settings.getNodeSettings(Integer.toString(index)), node.addObject());
                }
            },
            ARRAY {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    /**
                     * Regarding the handling of null values: The node settings can't differentiate between null values
                     * and empty arrays. Therefore, we add a custom null property to indicate null values.
                     */
                    final var subSettings = settings.addNodeSettings(key);
                    if (node == null || node.isNull()) {
                        subSettings.addBoolean(FIELD_NAME_NULL, true);
                    } else {
                        final var items = unpackSchema(schemaNode, TAG_ITEMS);
                        final var itemsType = valueOf(items);
                        for (var i = 0; i < node.size(); i++) {
                            final var childNode = node.get(i);
                            itemsType.addFieldToSettings(Integer.toString(i), childNode, items, subSettings);
                        }
                    }
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    final var subSettings = settings.getNodeSettings(key);
                    if (subSettings.containsKey(FIELD_NAME_NULL)) {
                        node.set(key, node.nullNode());
                    } else {
                        addArray(subSettings, node.putArray(key));
                    }
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    addArray(settings.getNodeSettings(Integer.toString(index)), node.addArray());
                }

                private void addArray(final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    for (var i = 0; i < settings.getChildCount(); i++) {
                        Type.valueOf(settings.getChildAt(i)).addValueFromSettings(i, settings, node);
                    }
                }
            },
            DOUBLE_ARRAY {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    if (!addNullOrEmptyArrayToSettings(key, node, schemaNode, settings)) {
                        settings.addDoubleArray(key, IntStream.range(0, node.size()).mapToObj(node::get)
                            .mapToDouble(JsonNode::doubleValue).toArray());
                    }

                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    if (!putNullArrayFromSettings(key, settings, node)) {
                        final var arrayNode = node.putArray(key);
                        Arrays.stream(settings.getDoubleArray(key)).forEach(arrayNode::add);
                    }
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    if (!addNullArrayFromSettings(index, settings, node)) {
                        final var arrayNode = node.addArray();
                        Arrays.stream(settings.getDoubleArray(Integer.toString(index))).forEach(arrayNode::add);
                    }
                }
            },
            FLOAT_ARRAY {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    if (!addNullOrEmptyArrayToSettings(key, node, schemaNode, settings)) {
                        final var size = node.size();
                        final var value = new float[size];
                        for (var i = 0; i < size; i++) {
                            value[i] = node.get(i).floatValue();
                        }
                        settings.addFloatArray(key, value);
                    }
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    if (!putNullArrayFromSettings(key, settings, node)) {
                        final var arrayNode = node.putArray(key);
                        for (var f : settings.getFloatArray(key)) {
                            arrayNode.add(f);
                        }
                    }
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    if (!addNullArrayFromSettings(index, settings, node)) {
                        final var arrayNode = node.addArray();
                        for (var f : settings.getFloatArray(Integer.toString(index))) {
                            arrayNode.add(f);
                        }
                    }
                }
            },
            INT_ARRAY {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    if (!addNullOrEmptyArrayToSettings(key, node, schemaNode, settings)) {
                        settings.addIntArray(key,
                            IntStream.range(0, node.size()).mapToObj(node::get).mapToInt(JsonNode::intValue).toArray());
                    }
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    if (!putNullArrayFromSettings(key, settings, node)) {
                        final var arrayNode = node.putArray(key);
                        Arrays.stream(settings.getIntArray(key)).forEach(arrayNode::add);
                    }
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    if (!addNullArrayFromSettings(index, settings, node)) {
                        final var arrayNode = node.addArray();
                        Arrays.stream(settings.getIntArray(Integer.toString(index))).forEach(arrayNode::add);
                    }
                }
            },
            LONG_ARRAY {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    if (!addNullOrEmptyArrayToSettings(key, node, schemaNode, settings)) {
                        settings.addLongArray(key, IntStream.range(0, node.size()).mapToObj(node::get)
                            .mapToLong(JsonNode::longValue).toArray());
                    }
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    if (!putNullArrayFromSettings(key, settings, node)) {
                        final var arrayNode = node.putArray(key);
                        Arrays.stream(settings.getLongArray(key)).forEach(arrayNode::add);
                    }
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    if (!addNullArrayFromSettings(index, settings, node)) {
                        final var arrayNode = node.addArray();
                        Arrays.stream(settings.getLongArray(Integer.toString(index))).forEach(arrayNode::add);
                    }
                }
            },
            BOOLEAN_ARRAY {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    if (!addNullOrEmptyArrayToSettings(key, node, schemaNode, settings)) {
                        final var size = node.size();
                        final var value = new boolean[size];
                        for (var i = 0; i < size; i++) {
                            value[i] = node.get(i).booleanValue();
                        }
                        settings.addBooleanArray(key, value);
                    }
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    if (!putNullArrayFromSettings(key, settings, node)) {
                        final var arrayNode = node.putArray(key);
                        for (var f : settings.getBooleanArray(key)) {
                            arrayNode.add(f);
                        }
                    }
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    if (!addNullArrayFromSettings(index, settings, node)) {
                        final var arrayNode = node.addArray();
                        for (var f : settings.getBooleanArray(Integer.toString(index))) {
                            arrayNode.add(f);
                        }
                    }
                }
            },
            STRING_ARRAY {
                @Override
                void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
                    final NodeSettingsWO settings) {
                    if (!addNullOrEmptyArrayToSettings(key, node, schemaNode, settings)) {
                        settings.addStringArray(key, IntStream.range(0, node.size()).mapToObj(node::get)
                            .map(JsonNode::textValue).toArray(String[]::new));
                    }
                }

                @Override
                void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
                    throws InvalidSettingsException {
                    if (!putNullArrayFromSettings(key, settings, node)) {
                        final var arrayNode = node.putArray(key);
                        Arrays.stream(settings.getStringArray(key)).forEach(arrayNode::add);
                    }
                }

                @Override
                void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
                    throws InvalidSettingsException {
                    if (!addNullArrayFromSettings(index, settings, node)) {
                        final var arrayNode = node.addArray();
                        Arrays.stream(settings.getStringArray(Integer.toString(index))).forEach(arrayNode::add);
                    }
                }
            };

        /**
         * Gets a value from a {@link JsonNode} and adds it to a {@link NodeSettingsWO settings} object under a given
         * key.
         */
        abstract void addFieldToSettings(final String key, final JsonNode node, final ObjectNode schemaNode,
            final NodeSettingsWO settings);

        /**
         * Gets the value from a {@link NodeSettingsRO settings} object under a given key and puts it into an
         * {@link ObjectNode} under the same key.
         */
        abstract void putFieldFromSettings(final String key, final NodeSettingsRO settings, final ObjectNode node)
            throws InvalidSettingsException;

        /**
         * Gets a value at a given index from a {@link NodeSettingsRO settings} object representing an array and adds it
         * to an {@link ArrayNode}.
         */
        abstract void addValueFromSettings(final int index, final NodeSettingsRO settings, final ArrayNode node)
            throws InvalidSettingsException;

        private static void checkNotNull(final JsonNode node, final ObjectNode schemaNode) {
            if (node == null || node.isNull()) {
                throw new IllegalArgumentException(String.format("Null node encountered where %s node was expected.",
                    schemaNode.get(TAG_TYPE).asText()));
            }
        }

        private static boolean addNullOrEmptyArrayToSettings(final String key, final JsonNode node,
            final ObjectNode schemaNode, final NodeSettingsWO settings) {
            if (node == null || node.isNull() || node.size() == 0) {
                final var subSettings = createTypedSubSettings(key, schemaNode, settings);
                // add array size of 0 (even for null nodes) such that the setting will be exposed as String array flow
                // variable via org.knime.core.node.workflow.VariableTypeRegistry::getCorrespondingVariableType
                subSettings.addInt(FIELD_NAME_ARRAY_SIZE, 0);
                // add info on whether this node is null to be able to reconstruct it correctly
                subSettings.addBoolean(FIELD_NAME_NULL, node == null || node.isNull());
                return true;
            }
            return false;
        }

        private static boolean putNullArrayFromSettings(final String key, final NodeSettingsRO settings,
            final ObjectNode node) throws InvalidSettingsException {
            final var subSettings = settings.getNodeSettings(key);
            if (subSettings.containsKey(FIELD_NAME_NULL) && subSettings.getBoolean(FIELD_NAME_NULL)) {
                node.putNull(key);
                return true;
            }
            return false;
        }

        private static boolean addNullArrayFromSettings(final int index, final NodeSettingsRO settings,
            final ArrayNode node) throws InvalidSettingsException {
            final var subSettings = settings.getNodeSettings(Integer.toString(index));
            if (subSettings.containsKey(FIELD_NAME_NULL) && subSettings.getBoolean(FIELD_NAME_NULL)) {
                node.addNull();
                return true;
            }
            return false;
        }

        /**
         * Adds all fields from an {@link ObjectNode} to a {@link NodeSettingsWO settings} object.
         */
        private static void addFieldsToSettings(final ObjectNode node, final ObjectNode schemaNode,
            final NodeSettingsWO settings) {
            if (!schemaNode.has(TAG_PROPERTIES)) {
                return;
            }
            final var properties = unpackSchema(schemaNode, TAG_PROPERTIES);
            final var it = properties.fields();
            while (it.hasNext()) {
                final var field = it.next();
                final var key = assertLegalKeyName(field.getKey());
                final var valueNode = node.get(key); // can be null
                final var valueSchemaNode = (ObjectNode)field.getValue();
                final var valueType = Type.valueOf(valueSchemaNode);
                valueType.addFieldToSettings(key, valueNode, valueSchemaNode, settings);
            }
        }

        private static String assertLegalKeyName(final String key) {
            if (isInternalSetting(key)) {
                throw new IllegalStateException("Settings must not end with '" + SettingsModel.CFGKEY_INTERNAL
                    + "'. Reserved for internal use only.");
            }
            return key;
        }

        private static boolean isInternalSetting(final String key) {
            return key.endsWith(SettingsModel.CFGKEY_INTERNAL);
        }

        /**
         * Creates sub-settings under a given key and adds the type according to a given schema node to it.
         */
        private static NodeSettingsWO createTypedSubSettings(final String key, final ObjectNode schemaNode,
            final NodeSettingsWO settings) {
            final var subSettings = settings.addNodeSettings(key);
            subSettings.addString(FIELD_NAME_TYPE, Type.valueOf(schemaNode).name());
            return subSettings;
        }

        /**
         * Gets all fields from a {@link NodeSettingsRO settings} object and puts them into an {@link ObjectNode}.
         */
        private static void putFieldsFromSettings(final NodeSettingsRO settings, final ObjectNode node)
            throws InvalidSettingsException {
            var i = 0;
            for (String key : settings) {
                if (isInternalSetting(key)) {
                    i++;
                    continue;
                }
                final var valueNode = settings.getChildAt(i);
                i++;
                final var valueType = Type.valueOf(valueNode);
                valueType.putFieldFromSettings(key, settings, node);
            }
        }

        /**
         * Unpacks an object node under a given key, throwing an {@link IllegalArgumentException} if the key is not
         * present or there is no object node under the key.
         */
        private static ObjectNode unpackSchema(final ObjectNode schemaNode, final String key) {
            if (!schemaNode.has(key)) {
                throw new IllegalArgumentException(String.format("No %s key found in schema node.", key));
            }
            final var value = schemaNode.get(key);
            if (!value.isObject()) {
                throw new IllegalArgumentException(String.format("Unexpected %s value in schema node.", key));
            }
            return (ObjectNode)value;
        }

        /**
         * Determines the type corresponding to a given json schema node.
         */
        private static Type valueOf(final ObjectNode schemaNode) { // NOSONAR
            /*
             * Here we rely on the fact that we currently only use anyOf and oneOf and all of its sub-schemas are
             * constant values (const) of type string (see {@link ChoicesAndEnumDefinitionProvider}). However, it is
             * worthy of note that there is also the allOf keyword and as per definition
             * (https://json-schema.org/understanding-json-schema/reference/combining.html), allOf, anyOf, oneOf are set
             * to an array, where each item is a json schema (i.e., an object). Such a json schema does not necessarily
             * have to hold a simple const, but can be arbitrarily complex. Also, as per definition
             * (https://json-schema.org/draft/2020-12/json-schema-validation.html#rfc.section.6.1.3), a const restricts
             * a value to a single value and this value may be of any type, including null. It is also important to keep
             * in mind that, since we might have generated the schema without specs, the array of consts might be empty
             * here. In any case, we agreed when discussing https://knime-com.atlassian.net/browse/UIEXT-164 that we
             * are currently fine assuming that oneOf always corresponds to type String and anyOf always corresponds to
             * type String[].
             */
            if (schemaNode.has(TAG_ONEOF)) {
                return Type.STRING;
            } else if (schemaNode.has(TAG_ANYOF)) {
                return Type.STRING_ARRAY;
            } else if (!schemaNode.has(TAG_TYPE)) {
                throw new IllegalArgumentException("No type key found in schema node.");
            }
            final var type = schemaNode.get(TAG_TYPE).asText();
            if (type.equals(TAG_TYPE_STRING)) {
                return Type.STRING;
            } else if (type.equals(TAG_TYPE_NUMBER)) {
                if (!schemaNode.has(TAG_FORMAT)) {
                    NodeLogger.getLogger(JsonNodeSettingsMapperUtil.class)
                        .warn("No format key found in numeric schema node");
                    return Type.DOUBLE;
                }
                final var format = schemaNode.get(TAG_FORMAT).asText();
                if (format.equals(TAG_FORMAT_DOUBLE)) {
                    return Type.DOUBLE;
                } else if (format.equals(TAG_FORMAT_FLOAT)) {
                    return Type.FLOAT;
                }
            } else if (type.equals(TAG_TYPE_INTEGER)) {
                if (!schemaNode.has(TAG_FORMAT)) {
                    NodeLogger.getLogger(JsonNodeSettingsMapperUtil.class)
                        .warn("No format key found in integer schema node");
                    return Type.INT;
                }
                final var format = schemaNode.get(TAG_FORMAT).asText();
                if (format.equals(TAG_FORMAT_INT)) {
                    return Type.INT;
                } else if (format.equals(TAG_FORMAT_LONG)) {
                    return Type.LONG;
                }
            } else if (type.equals(TAG_TYPE_OBJECT)) {
                return Type.OBJECT;
            } else if (type.equals(TAG_TYPE_ARRAY)) {
                return valueOfArray(schemaNode);
            } else if (type.equals(TAG_TYPE_BOOLEAN)) {
                return Type.BOOLEAN;
            } else if (type.equals(TAG_TYPE_NULL)) {
                return Type.NULL;
            }
            throw new IllegalArgumentException(String.format("Unexpected type value \"%s\" in schema node.", type));
        }

        /**
         * Determines the type corresponding to a given json schema array node.
         */
        private static Type valueOfArray(final ObjectNode schemaNode) { // NOSONAR
            switch (valueOf(unpackSchema(schemaNode, TAG_ITEMS))) {
                case DOUBLE:
                    return Type.DOUBLE_ARRAY;
                case FLOAT:
                    return Type.FLOAT_ARRAY;
                case INT:
                    return Type.INT_ARRAY;
                case LONG:
                    return Type.LONG_ARRAY;
                case BOOLEAN:
                    return Type.BOOLEAN_ARRAY;
                case STRING:
                    return Type.STRING_ARRAY;
                default:
                    return Type.ARRAY;
            }
        }

        /**
         * Determines the type corresponding to a given {@link TreeNode}.
         */
        private static Type valueOf(final TreeNode node) throws InvalidSettingsException { // NOSONAR
            if (node instanceof ConfigDoubleEntry) {
                return Type.DOUBLE;
            } else if (node instanceof ConfigFloatEntry) {
                return Type.FLOAT;
            } else if (node instanceof ConfigIntEntry) {
                return Type.INT;
            } else if (node instanceof ConfigLongEntry) {
                return Type.LONG;
            } else if (node instanceof ConfigBooleanEntry) {
                return Type.BOOLEAN;
            } else if (node instanceof ConfigStringEntry) {
                return Type.STRING;
            } else if (node instanceof NodeSettingsRO) {
                return valueOf((NodeSettingsRO)node);
            }
            throw new IllegalArgumentException(
                String.format("Unexpected tree node: %s.", node.getClass().getSimpleName()));
        }

        /**
         * Determines the type corresponding to a given {@link NodeSettingsRO}.
         */
        private static Type valueOf(final NodeSettingsRO settings) throws InvalidSettingsException { // NOSONAR
            if (settings.containsKey(FIELD_NAME_TYPE)) {
                return valueOf(settings.getString(FIELD_NAME_TYPE));
            } else if (isTypedArray(settings, ConfigDoubleEntry.class::isInstance)) {
                return Type.DOUBLE_ARRAY;
            } else if (isTypedArray(settings, ConfigFloatEntry.class::isInstance)) {
                return Type.FLOAT_ARRAY;
            } else if (isTypedArray(settings, ConfigIntEntry.class::isInstance)) {
                return Type.INT_ARRAY;
            } else if (isTypedArray(settings, ConfigLongEntry.class::isInstance)) {
                return Type.LONG_ARRAY;
            } else if (isTypedArray(settings, ConfigBooleanEntry.class::isInstance)) {
                return Type.BOOLEAN_ARRAY;
            } else if (isTypedArray(settings, ConfigStringEntry.class::isInstance)) {
                return Type.STRING_ARRAY;
            } else if (isCustomArray(settings)) {
                return Type.ARRAY;
            }
            throw new IllegalArgumentException(String.format("Unexpected settings node: %s.", settings.getKey()));
        }

        private static boolean isTypedArray(final NodeSettingsRO settings, // NOSONAR
            final Predicate<TreeNode> typePredicate) {
            if (settings.getChildCount() < 1) {
                return false;
            }
            final var it = settings.iterator();
            if (!it.next().equals(FIELD_NAME_ARRAY_SIZE)) {
                return false;
            }
            final var value = settings.getChildAt(0);
            if (!(value instanceof ConfigIntEntry)) {
                return false;
            }
            final var size = ((ConfigIntEntry)value).getInt();
            if (size + 1 != settings.getChildCount()) {
                return false;
            }
            // since we intend to only read settings that were previously written by this class, it should be sufficient
            // to read the first entry only
            if (size > 0) {
                if (!it.next().equals("0")) {
                    return false;
                }
                if (!(typePredicate.test(settings.getChildAt(1)))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isCustomArray(final NodeSettingsRO settings) {
            var i = 0;
            for (final var key : settings) {
                if (!(key.equals(Integer.toString(i)) || key.equals(FIELD_NAME_NULL))) {
                    return false;
                }
                i++;
            }
            return true;
        }

    }

    private JsonNodeSettingsMapperUtil() {
        //utility class
    }

    /**
     * Converts an arbitrary {@link ObjectNode Json object node} into {@link NodeSettingsRO node settings}. Note that
     * all properties (except for oneOf, anyOf, or const) in the given schema are required to have a type key.
     * Properties of types "integer" or "number" should also provide a format key holding an OpenAPI format value
     * ("int32" or "int64" for type "integer"; "float" or "double" for type "number").
     *
     * @param node the Json object node to read from
     * @param schemaNode the Json schema node to obtain types from
     * @param settings the node settings to write into
     */
    static void jsonObjectToNodeSettings(final ObjectNode node, final ObjectNode schemaNode,
        final NodeSettingsWO settings) {
        Type.addFieldsToSettings(node, schemaNode, settings);
    }

    /**
     * Parses an arbitrary JSON string and writes it into the provided {@link NodeSettingsWO} object. Note that all
     * properties (except for oneOf, anyOf, or const) in the given schema are required to have a type key. Properties of
     * types "integer" or "number" should also provide a format key holding an OpenAPI format value ("int32" or "int64"
     * for type "integer"; "float" or "double" for type "number").
     *
     * @param json to parse and write into settings
     * @param schema to obtain types from
     * @param settings to write to
     */
    public static void jsonStringToNodeSettings(final String json, final String schema, final NodeSettingsWO settings) {
        var mapper = JsonFormsDataUtil.getMapper();
        try {
            var node = mapper.readTree(json);
            var schemaNode = mapper.readTree(schema);
            jsonObjectToNodeSettings((ObjectNode)node, (ObjectNode)schemaNode, settings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse the provided json.", e);
        }
    }

    /**
     * Converts a {@link NodeSettingsRO node settings} object that has previously been created via the
     * {@link JsonNodeSettingsMapperUtil#jsonObjectToNodeSettings(ObjectNode, NodeSettingsWO) jsonObjectToNodeSettings}
     * method into a {@link ObjectNode Json object node}.
     *
     * @param settings the node settings to read from
     * @param node the Json object node to write into
     */
    static void nodeSettingsToJsonObject(final NodeSettingsRO settings, final ObjectNode node) {
        try {
            Type.putFieldsFromSettings(settings, node);
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(
                String.format("Exception when reading data from node settings: %s", e.getMessage()), e);
        }
    }

    /**
     * Converts a {@link NodeSettingsRO node settings} object that has previously been created via the
     * {@link JsonNodeSettingsMapperUtil#jsonObjectToNodeSettings(ObjectNode, NodeSettingsWO) jsonObjectToNodeSettings}
     * method into a {@link ObjectNode Json object node}.
     *
     * @param settings the node settings to read from
     * @return the Json object node containing the settings
     */
    static ObjectNode nodeSettingsToJsonObject(final NodeSettingsRO settings) {
        var node = JsonFormsDataUtil.getMapper().createObjectNode();
        nodeSettingsToJsonObject(settings, node);
        return node;
    }

    /**
     * Converts a {@link NodeSettingsRO node settings} object that has previously been created via the
     * {@link JsonNodeSettingsMapperUtil#jsonStringToNodeSettings(String, String, NodeSettingsWO)
     * jsonStringToNodeSettings} method into a JSON string.
     *
     * @param settings the node settings to read from
     * @return the JSON string containing the settings
     */
    public static String nodeSettingsToJsonString(final NodeSettingsRO settings) {
        return nodeSettingsToJsonObject(settings).toString();
    }

}
