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
 *   19 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests JsonNodeSettingsMapperUtil.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class JsonNodeSettingsMapperUtilTest {

    private static final Consumer<ObjectNode> NOOP_NODE_CONS = node -> {
    };

    private static final BiConsumer<String, ObjectNode> NOOP_NODE_BICONS = (name, node) -> {
    };

    private static void writeRead(final BiConsumer<String, ObjectNode> nodeCons, final String... type)
        throws InvalidSettingsException {
        writeRead(nodeCons, NOOP_NODE_CONS, null, type);
    }

    private static void writeReadArray(final Consumer<ArrayNode> nodeCons, final String... elementType)
        throws InvalidSettingsException {
        writeRead((name, node) -> nodeCons.accept(node.putArray(name)), node -> {
            var items = node.putObject(TAG_ITEMS).put(TAG_TYPE, elementType[0]);
            if (elementType.length > 1) {
                items.put(TAG_FORMAT, elementType[1]);
            }
        }, null, TAG_TYPE_ARRAY);
    }

    private static void writeReadArrayOfArrays(final Consumer<ArrayNode> nodeCons, final String... elementType)
        throws InvalidSettingsException {
        writeRead((name, node) -> nodeCons.accept(node.putArray(name).addArray()), node -> {
            var items = node.putObject(TAG_ITEMS).put(TAG_TYPE, TAG_TYPE_ARRAY).putObject(TAG_ITEMS).put(TAG_TYPE,
                elementType[0]);
            if (elementType.length > 1) {
                items.put(TAG_FORMAT, elementType[1]);
            }
        }, null, TAG_TYPE_ARRAY);
    }

    private static void writeRead(final BiConsumer<String, ObjectNode> nodeCons,
        final Consumer<ObjectNode> schemaNodeCons, final BiConsumer<String, ObjectNode> expectedNodeCons,
        final String... type) throws InvalidSettingsException {
        writeRead(node -> nodeCons.accept("foo", node), schemaNode -> {
            var property = schemaNode.putObject("foo").put(TAG_TYPE, type[0]);
            if (type.length > 1) {
                property.put(TAG_FORMAT, type[1]);
            }
            schemaNodeCons.accept(property);
        }, expectedNodeCons == null ? null : node -> expectedNodeCons.accept("foo", node));
    }

    private static void writeRead(final Consumer<ObjectNode> nodeCons, final Consumer<ObjectNode> schemaNodeCons)
        throws InvalidSettingsException {
        writeRead(nodeCons, schemaNodeCons, null);
    }

    private static void writeRead(final Consumer<ObjectNode> nodeCons, final Consumer<ObjectNode> schemaNodeCons,
        final Consumer<ObjectNode> expectedNodeCons) throws InvalidSettingsException {

        var mapper = ObjectMapperUtil.getInstance().getObjectMapper();

        var node = mapper.createObjectNode();
        nodeCons.accept(node);

        var schemaNode = mapper.createObjectNode().put(TAG_TYPE, TAG_TYPE_OBJECT);
        schemaNodeCons.accept(schemaNode.putObject(TAG_PROPERTIES));

        var settings = new NodeSettings("node_settings");
        JsonNodeSettingsMapperUtil.jsonStringToNodeSettings(node.toString(), schemaNode.toString(), settings);

        var reconstructedNode = JsonNodeSettingsMapperUtil.nodeSettingsToJsonObject(settings);

        if (expectedNodeCons == null) {
            assertThat(reconstructedNode).isEqualTo(node);
        } else {
            var expectedNode = mapper.createObjectNode();
            expectedNodeCons.accept(expectedNode);
            assertThat(reconstructedNode).isEqualTo(expectedNode);
        }

    }

    @Test
    void testWriteReadEmpty() throws InvalidSettingsException {
        writeRead(NOOP_NODE_CONS, NOOP_NODE_CONS);
    }

    @Test
    void testWriteReadDouble() throws InvalidSettingsException {
        writeRead((name, node) -> node.put(name, 42d), TAG_TYPE_NUMBER, TAG_FORMAT_DOUBLE);
    }

    @Test
    void testWriteReadDoubleNull() throws InvalidSettingsException {
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.put(name, 0d);
        assertThrows(IllegalArgumentException.class,
            () -> writeRead(NOOP_NODE_BICONS, NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_NUMBER, TAG_FORMAT_DOUBLE));
        assertThrows(IllegalArgumentException.class, () -> writeRead((name, node) -> node.set(name, node.nullNode()),
            NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_NUMBER, TAG_FORMAT_DOUBLE));
    }

    @Test
    void testWriteReadFloat() throws InvalidSettingsException {
        writeRead((name, node) -> node.put(name, 42f), TAG_TYPE_NUMBER, TAG_FORMAT_FLOAT);
    }

    @Test
    void testWriteReadFloatNull() throws InvalidSettingsException {
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.put(name, 0f);
        assertThrows(IllegalArgumentException.class,
            () -> writeRead(NOOP_NODE_BICONS, NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_NUMBER, TAG_FORMAT_FLOAT));
        assertThrows(IllegalArgumentException.class, () -> writeRead((name, node) -> node.set(name, node.nullNode()),
            NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_NUMBER, TAG_FORMAT_FLOAT));
    }

    @Test
    void testWriteReadInt() throws InvalidSettingsException {
        writeRead((name, node) -> node.put(name, 42), TAG_TYPE_INTEGER, TAG_FORMAT_INT);
    }

    @Test
    void testWriteReadIntNull() throws InvalidSettingsException {
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.put(name, 0);
        assertThrows(IllegalArgumentException.class,
            () -> writeRead(NOOP_NODE_BICONS, NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_INTEGER, TAG_FORMAT_INT));
        assertThrows(IllegalArgumentException.class, () -> writeRead((name, node) -> node.set(name, node.nullNode()),
            NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_INTEGER, TAG_FORMAT_INT));
    }

    @Test
    void testWriteReadLong() throws InvalidSettingsException {
        writeRead((name, node) -> node.put(name, 42L), TAG_TYPE_INTEGER, TAG_FORMAT_LONG);
    }

    @Test
    void testWriteReadLongNull() throws InvalidSettingsException {
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.put(name, 0L);
        assertThrows(IllegalArgumentException.class,
            () -> writeRead(NOOP_NODE_BICONS, NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_INTEGER, TAG_FORMAT_LONG));
        assertThrows(IllegalArgumentException.class, () -> writeRead((name, node) -> node.set(name, node.nullNode()),
            NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_INTEGER, TAG_FORMAT_LONG));
    }

    @Test
    void testWriteReadBoolean() throws InvalidSettingsException {
        writeRead((name, node) -> node.put(name, true), TAG_TYPE_BOOLEAN);
    }

    @Test
    void testWriteReadBooleanNull() throws InvalidSettingsException {
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.put(name, false);
        assertThrows(IllegalArgumentException.class,
            () -> writeRead(NOOP_NODE_BICONS, NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_BOOLEAN));
        assertThrows(IllegalArgumentException.class, () -> writeRead((name, node) -> node.set(name, node.nullNode()),
            NOOP_NODE_CONS, expectedNodeCons, TAG_TYPE_BOOLEAN));
    }

    @Test
    void testWriteReadArrayOfBooleans() throws InvalidSettingsException {
        writeReadArray(node -> node.add(true).add(false), TAG_TYPE_BOOLEAN);
    }

    @Test
    void testWriteReadNull() throws InvalidSettingsException {
        writeRead((name, node) -> node.set(name, node.nullNode()), TAG_TYPE_NULL);
    }

    @Test
    void testWriteReadArrayOfNulls() throws InvalidSettingsException {
        writeReadArray(node -> node.add(node.nullNode()), TAG_TYPE_NULL);
    }

    @Test
    void testWriteReadString() throws InvalidSettingsException {
        writeRead((name, node) -> node.put(name, "bar"), TAG_TYPE_STRING);
    }

    @Test
    void testWriteReadStringOneOf() throws InvalidSettingsException {
        writeRead(node -> node.put("foo", "bar"), schemaNode -> {
            final var oneOf = schemaNode.putObject("foo").putArray(TAG_ONEOF);
            oneOf.addObject().put("const", "bar").put("title", "bar");
            oneOf.addObject().put("const", "baz").put("title", "baz");
        });
    }

    @Test
    void testWriteReadStringNull() throws InvalidSettingsException {
        writeRead(NOOP_NODE_BICONS, NOOP_NODE_CONS, (name, node) -> node.set(name, node.nullNode()), TAG_TYPE_STRING);
        writeRead((name, node) -> node.set(name, node.nullNode()), TAG_TYPE_STRING);
    }

    @Test
    void testWriteReadObject() throws InvalidSettingsException {
        writeRead((name, node) -> node.putObject(name), TAG_TYPE_OBJECT);
    }

    @Test
    void testWriteReadObjectNull() throws InvalidSettingsException {
        writeRead(NOOP_NODE_BICONS, NOOP_NODE_CONS, (name, node) -> node.putObject(name), TAG_TYPE_OBJECT);
        writeRead((name, node) -> node.set(name, node.nullNode()), node -> {
            node.putObject(TAG_PROPERTIES).putObject("bar").put(TAG_TYPE, TAG_TYPE_OBJECT);
        }, (name, node) -> node.putObject(name).putObject("bar"), TAG_TYPE_OBJECT);
    }

    @Test
    void testWriteReadArrayOfObjects() throws InvalidSettingsException {
        writeReadArray(node -> node.addObject(), TAG_TYPE_OBJECT);
    }

    @Test
    void testWriteReadArrayOfObjectsNull() throws InvalidSettingsException {
        Consumer<ObjectNode> schemaNodeCons = node -> node.putObject(TAG_ITEMS).put(TAG_TYPE, TAG_TYPE_OBJECT);
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.set(name, node.nullNode());
        writeRead(NOOP_NODE_BICONS, schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
        writeRead((name, node) -> node.set(name, node.nullNode()), schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
    }

    @Test
    void testWriteReadArrayOfArraysOfObjects() throws InvalidSettingsException {
        writeReadArrayOfArrays(node -> node.addObject(), TAG_TYPE_OBJECT);
    }

    @Test
    void testWriteReadDoubleArray() throws InvalidSettingsException {
        writeReadArray(node -> node.add(42d), TAG_TYPE_NUMBER, TAG_FORMAT_DOUBLE);
    }

    @Test
    void testWriteReadDoubleArrayNull() throws InvalidSettingsException {
        Consumer<ObjectNode> schemaNodeCons = schemaNode -> schemaNode.putObject(TAG_ITEMS)
            .put(TAG_TYPE, TAG_TYPE_NUMBER).put(TAG_FORMAT, TAG_FORMAT_DOUBLE);
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.set(name, node.nullNode());
        writeRead(NOOP_NODE_BICONS, schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
        writeRead((name, node) -> node.set(name, node.nullNode()), schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
    }

    @Test
    void testWriteReadArrayOfDoubleArrays() throws InvalidSettingsException {
        writeReadArrayOfArrays(node -> node.add(42d), TAG_TYPE_NUMBER, TAG_FORMAT_DOUBLE);
    }

    @Test
    void testWriteReadFloatArray() throws InvalidSettingsException {
        writeReadArray(node -> node.add(42f), TAG_TYPE_NUMBER, TAG_FORMAT_FLOAT);
    }

    @Test
    void testWriteReadFloatArrayNull() throws InvalidSettingsException {
        Consumer<ObjectNode> schemaNodeCons = schemaNode -> schemaNode.putObject(TAG_ITEMS)
            .put(TAG_TYPE, TAG_TYPE_NUMBER).put(TAG_FORMAT, TAG_FORMAT_FLOAT);
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.set(name, node.nullNode());
        writeRead(NOOP_NODE_BICONS, schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
        writeRead((name, node) -> node.set(name, node.nullNode()), schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
    }

    @Test
    void testWriteReadArrayOfFloatArrays() throws InvalidSettingsException {
        writeReadArrayOfArrays(node -> node.add(42f), TAG_TYPE_NUMBER, TAG_FORMAT_FLOAT);
    }

    @Test
    void testWriteReadIntArray() throws InvalidSettingsException {
        writeReadArray(node -> node.add(42), TAG_TYPE_INTEGER, TAG_FORMAT_INT);
    }

    @Test
    void testWriteReadIntArrayNull() throws InvalidSettingsException {
        Consumer<ObjectNode> schemaNodeCons = schemaNode -> schemaNode.putObject(TAG_ITEMS)
            .put(TAG_TYPE, TAG_TYPE_INTEGER).put(TAG_FORMAT, TAG_FORMAT_INT);
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.set(name, node.nullNode());
        writeRead(NOOP_NODE_BICONS, schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
        writeRead((name, node) -> node.set(name, node.nullNode()), schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
    }

    @Test
    void testWriteReadArrayOfIntArrays() throws InvalidSettingsException {
        writeReadArrayOfArrays(node -> node.add(42), TAG_TYPE_INTEGER, TAG_FORMAT_INT);
    }

    @Test
    void testWriteReadLongArray() throws InvalidSettingsException {
        writeReadArray(node -> node.add(42L), TAG_TYPE_INTEGER, TAG_FORMAT_LONG);
    }

    @Test
    void testWriteReadLongArrayNull() throws InvalidSettingsException {
        Consumer<ObjectNode> schemaNodeCons = schemaNode -> schemaNode.putObject(TAG_ITEMS)
            .put(TAG_TYPE, TAG_TYPE_INTEGER).put(TAG_FORMAT, TAG_FORMAT_LONG);
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.set(name, node.nullNode());
        writeRead(NOOP_NODE_BICONS, schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
        writeRead((name, node) -> node.set(name, node.nullNode()), schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
    }

    @Test
    void testWriteReadArrayOfLongArrays() throws InvalidSettingsException {
        writeReadArrayOfArrays(node -> node.add(42L), TAG_TYPE_INTEGER, TAG_FORMAT_LONG);
    }

    @Test
    void testWriteReadBooleanArray() throws InvalidSettingsException {
        writeReadArray(node -> node.add(true), TAG_TYPE_BOOLEAN);
    }

    @Test
    void testWriteReadBooleanArrayNull() throws InvalidSettingsException {
        Consumer<ObjectNode> schemaNodeCons =
            schemaNode -> schemaNode.putObject(TAG_ITEMS).put(TAG_TYPE, TAG_TYPE_BOOLEAN);
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.set(name, node.nullNode());
        writeRead(NOOP_NODE_BICONS, schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
        writeRead((name, node) -> node.set(name, node.nullNode()), schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
    }

    @Test
    void testWriteReadArrayOfBooleanArrays() throws InvalidSettingsException {
        writeReadArrayOfArrays(node -> node.add(true), TAG_TYPE_BOOLEAN);
    }

    @Test
    void testWriteReadStringArray() throws InvalidSettingsException {
        writeReadArray(node -> node.add("bar"), TAG_TYPE_STRING);
    }

    @Test
    void testWriteReadStringArrayAnyOf() throws InvalidSettingsException {
        writeRead(node -> node.putArray("foo").add("bar"), schemaNode -> {
            final var anyOf = schemaNode.putObject("foo").putArray(TAG_ANYOF);
            anyOf.addObject().put("const", "bar").put("title", "bar");
            anyOf.addObject().put("const", "baz").put("title", "baz");
        });
    }

    @Test
    void testWriteReadStringArrayNull() throws InvalidSettingsException {
        Consumer<ObjectNode> schemaNodeCons =
            schemaNode -> schemaNode.putObject(TAG_ITEMS).put(TAG_TYPE, TAG_TYPE_STRING);
        BiConsumer<String, ObjectNode> expectedNodeCons = (name, node) -> node.set(name, node.nullNode());
        writeRead(NOOP_NODE_BICONS, schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
        writeRead((name, node) -> node.set(name, node.nullNode()), schemaNodeCons, expectedNodeCons, TAG_TYPE_ARRAY);
    }

    @Test
    void testWriteReadArrayOfStringArrays() throws InvalidSettingsException {
        writeReadArrayOfArrays(node -> node.add("bar"), TAG_TYPE_STRING);
    }

    @Test
    void testWriteReadObjectWithReservedFieldNames() throws InvalidSettingsException {
        writeRead(node -> {
            node.put(JsonNodeSettingsMapperUtil.FIELD_NAME_ARRAY_SIZE, true);
            node.put(TAG_TYPE, true);
            node.put(TAG_ONEOF, true);
            node.put(TAG_PROPERTIES, true);
            node.put(TAG_ITEMS, true);
            node.put("0", true);
        }, schemaNode -> {
            schemaNode.putObject(JsonNodeSettingsMapperUtil.FIELD_NAME_ARRAY_SIZE).put(TAG_TYPE, TAG_TYPE_BOOLEAN);
            schemaNode.putObject(TAG_TYPE).put(TAG_TYPE, TAG_TYPE_BOOLEAN);
            schemaNode.putObject(TAG_ONEOF).put(TAG_TYPE, TAG_TYPE_BOOLEAN);
            schemaNode.putObject(TAG_PROPERTIES).put(TAG_TYPE, TAG_TYPE_BOOLEAN);
            schemaNode.putObject(TAG_ITEMS).put(TAG_TYPE, TAG_TYPE_BOOLEAN);
            schemaNode.putObject("0").put(TAG_TYPE, TAG_TYPE_BOOLEAN);
        });
    }

    @Test
    void testWriteObjectWithIllegalFieldNames() {
        var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
        var node = mapper.createObjectNode().put(JsonNodeSettingsMapperUtil.FIELD_NAME_TYPE, true).toString();
        var schemaNode = mapper.createObjectNode().put(TAG_TYPE, TAG_TYPE_OBJECT);
        schemaNode.putObject(TAG_PROPERTIES).putObject(JsonNodeSettingsMapperUtil.FIELD_NAME_TYPE).put(TAG_TYPE,
            TAG_TYPE_BOOLEAN);
        var schemaNodeString = schemaNode.toString();

        var ex = assertThrows(IllegalStateException.class,
            () -> JsonNodeSettingsMapperUtil.jsonStringToNodeSettings(node, schemaNodeString, null));
        assertThat(ex).hasMessageContaining("Reserved for internal use only");
    }

    @Test
    void testIllegalArgumentExceptionOnMissingItems() throws InvalidSettingsException {
        assertThrows(IllegalArgumentException.class, () -> writeRead(node -> node.putArray("foo"),
            schemaNode -> schemaNode.putObject("foo").put(TAG_TYPE, TAG_TYPE_ARRAY)));
    }

    @Test
    void testIllegalArgumentExceptionOnWrongItemsType() throws InvalidSettingsException {
        assertThrows(IllegalArgumentException.class, () -> writeRead(node -> node.put("foo", true),
            schemaNode -> schemaNode.putObject("foo").put(TAG_TYPE, TAG_TYPE_ARRAY).put(TAG_ITEMS, "bar")));
    }

    @Test
    void testIllegalArgumentExceptionOnMissingType() throws InvalidSettingsException {
        assertThrows(IllegalArgumentException.class,
            () -> writeRead(node -> node.put("foo", true), schemaNode -> schemaNode.putObject("foo")));
    }

    @Test
    void testIllegalArgumentExceptionOnUnknownType() throws InvalidSettingsException {
        assertThrows(IllegalArgumentException.class, () -> writeRead(node -> node.put("foo", true),
            schemaNode -> schemaNode.putObject("foo").put(TAG_TYPE, "")));
    }

    @Test
    void testWriteNestedObject() throws InvalidSettingsException {
        var node = "" //
            + "{\n" //
            + "    \"inner\": {\n" //
            + "        \"prop\": \"value\"\n" //
            + "    }\n" //
            + "}";
        var schema = "" //
            + "{\n" //
            + "    \"type\": \"object\",\n" //
            + "    \"properties\": {\n" //
            + "        \"inner\": {\n" //
            + "            \"type\": \"object\",\n" //
            + "            \"properties\": {\n" //
            + "                \"prop\":  {\n" //
            + "                    \"type\": \"string\"\n" //
            + "                }\n" //
            + "            }\n" //
            + "        }\n" //
            + "    }\n" //
            + "}";
        var nodeSettings = new NodeSettings("nodeSettings_to_test");
        JsonNodeSettingsMapperUtil.jsonStringToNodeSettings(node, schema, nodeSettings);
        assertThat(nodeSettings.getNodeSettings("inner").getString("prop")).isEqualTo("value");
    }

    @Test
    void testWriteNullObjectAndArray() throws InvalidSettingsException {
        var node = "{}";
        var schema = "" //
            + "{\n" //
            + "    \"type\": \"object\",\n" //
            + "    \"properties\": {\n" //
            + "        \"object\": {\n" //
            + "            \"type\": \"object\",\n" //
            + "            \"properties\": {\n" //
            + "                \"prop\":  {\n" //
            + "                    \"type\": \"string\"\n" //
            + "                }\n" //
            + "            }\n" //
            + "        },\n" //
            + "        \"array\": {\n" //
            + "            \"type\": \"array\",\n" //
            + "            \"items\": {\n" //
            + "                \"type\": \"string\"" //
            + "            }\n" //
            + "        },\n" //
            + "        \"string\": {\n" //
            + "            \"type\": \"string\"\n" //
            + "        }\n" //
            + "    }\n" //
            + "}";
        var nodeSettings = new NodeSettings("nodeSettings_to_test");
        JsonNodeSettingsMapperUtil.jsonStringToNodeSettings(node, schema, nodeSettings);

        var objectNS = nodeSettings.getNodeSettings("object");
        assertThat(objectNS.containsKey(JsonNodeSettingsMapperUtil.FIELD_NAME_NULL)).isFalse();
        assertThat(objectNS.getString(JsonNodeSettingsMapperUtil.FIELD_NAME_TYPE)).isEqualTo("OBJECT");
        assertThat(objectNS.containsKey("prop")).isTrue();
        assertThat(objectNS.getString("prop")).isNull();

        var arrayNS = nodeSettings.getNodeSettings("array");
        assertThat(arrayNS.getBoolean(JsonNodeSettingsMapperUtil.FIELD_NAME_NULL)).isTrue();
        assertThat(arrayNS.getString(JsonNodeSettingsMapperUtil.FIELD_NAME_TYPE)).isEqualTo("STRING_ARRAY");

        assertThat(nodeSettings.containsKey("string")).isTrue();
        assertThat(nodeSettings.getString("string")).isNull();

        // just for completeness also test the 'reconstruction' of the json object from the node settings
        // (it is expected that it differs from the original empty json object)
        var reconstructedNode = JsonNodeSettingsMapperUtil.nodeSettingsToJsonObject(nodeSettings);
        assertThat(reconstructedNode).hasToString("{\"object\":{\"prop\":null},\"array\":null,\"string\":null}");
    }

}
