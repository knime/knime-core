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
 *   May 19, 2021 (carlwitt): created
 */
package org.knime.core.node.workflow.def;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.workflow.def.ConfigDef;
import org.knime.core.workflow.def.ConfigMapDef;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class DefToCoreUtilTest {

    private static final NodeSettings original;

    static {
        original = new NodeSettings("root");

        original.addStringArray("colors", "red", "green", "blue");
        original.addString("answerString", "42");

        original.addBoolean("A v ¬A", true);

        original.addInt("answerInt", Integer.MIN_VALUE);
        original.addFloat("answerFloat", Float.MIN_VALUE);
        original.addDouble("answerDouble", Double.MIN_VALUE);
        original.addShort("answerShort", Short.MIN_VALUE);
        original.addByte("answerByte", Byte.MIN_VALUE);
        original.addLong("answerLong", Long.MIN_VALUE);
        // TODO
//        original.addPassword("answerPassword", "zebra", "secret");

        original.addBooleanArray("booleans", true, false, false, false, true, false);
        original.addByteArray("bytes", "bytes".getBytes());
        original.addCharArray("chars", 'A', 'B', 'C');
        original.addDoubleArray("doubles", Double.MIN_VALUE, Double.MAX_VALUE);
        original.addFloatArray("floats", Float.MIN_VALUE, Float.MAX_VALUE);
        original.addIntArray("ints", 1,2,3,4,5,6,7,8,9);
        original.addLongArray("longs", Long.MIN_VALUE, Long.MAX_VALUE);
        original.addShortArray("shorts", Short.MIN_VALUE, Short.MAX_VALUE);
        original.addStringArray("colors", "red", "green", "blue");

        // tree structure: org.knime.core.(util|node)
        NodeSettingsWO org = original.addNodeSettings("org");
        NodeSettingsWO knime = org.addNodeSettings("knime");
        // primitive setting in a tree structure, not only top level
        knime.addLong("loc", Long.MAX_VALUE);
        NodeSettingsWO core = knime.addNodeSettings("core");
        // leafs that are not primitives (addBoolean, addString, ...)
        core.addNodeSettings("util");
        core.addNodeSettings("node");

    }

    /**
     * Convert {@link NodeSettings} to {@link ConfigDef} and the {@link ConfigDef} back to {@link NodeSettings}.
     * Compare the original {@link NodeSettings} with the restored {@link NodeSettings}.
     *
     * Note that this only tests conversion to Def entitites and back, not the correct restoration of Def entities from
     * JSON or any other serialization format.
     */
    @Test
    public void testCoreToDefAndBack() {
        try {

            // TODO doubles and floats seem to be written out in scientific notation, i.e., not lossless

            ConfigMapDef def = CoreToDefUtil.toConfigMapDef(original);
            NodeSettings restored = DefToCoreUtil.toNodeSettings(def);

            // conversion process does not change information
            assertTrue(original.hasIdenticalValue(restored));

            // some additional tests on the correctness of hasIdenticalValue
            assertTrue("Configuration not identical to itself", restored.hasIdenticalValue(restored));
            assertTrue("Configuration not identical to itself", original.hasIdenticalValue(original));
            assertTrue("Configuration comparison is not commutative", restored.hasIdenticalValue(original));
        } catch (InvalidSettingsException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test the output of the JSON serializer on a {@link NodeSettings} object represented as a {@link ConfigDef}.
     */
    @Test
    public void testPersistJson() {
        try {

            ConfigDef def = CoreToDefUtil.toConfigMapDef(original);

            ObjectMapper mapper = new ObjectMapper();
            final String expected = "{\n"
                + "  \"children\" : {\n"
                + "    \"colors\" : {\n"
                + "      \"array\" : [ \"red\", \"green\", \"blue\" ],\n"
                + "      \"configType\" : \"ConfigValueStringArray\"\n"
                + "    },\n"
                + "    \"answerString\" : {\n"
                + "      \"configType\" : \"ConfigValueString\",\n"
                + "      \"value\" : \"42\"\n"
                + "    },\n"
                + "    \"A v ¬A\" : {\n"
                + "      \"configType\" : \"ConfigValueBoolean\",\n"
                + "      \"value\" : true\n"
                + "    },\n"
                + "    \"answerInt\" : {\n"
                + "      \"configType\" : \"ConfigValueInt\",\n"
                + "      \"value\" : -2147483648\n"
                + "    },\n"
                + "    \"answerFloat\" : {\n"
                + "      \"configType\" : \"ConfigValueFloat\",\n"
                + "      \"value\" : 1.4E-45\n"
                + "    },\n"
                + "    \"answerDouble\" : {\n"
                + "      \"configType\" : \"ConfigValueDouble\",\n"
                + "      \"value\" : 4.9E-324\n"
                + "    },\n"
                + "    \"answerShort\" : {\n"
                + "      \"configType\" : \"ConfigValueShort\",\n"
                + "      \"value\" : -32768\n"
                + "    },\n"
                + "    \"answerByte\" : {\n"
                + "      \"configType\" : \"ConfigValueByte\",\n"
                + "      \"value\" : -128\n"
                + "    },\n"
                + "    \"answerLong\" : {\n"
                + "      \"configType\" : \"ConfigValueLong\",\n"
                + "      \"value\" : -9223372036854775808\n"
                + "    },\n"
                + "    \"booleans\" : {\n"
                + "      \"array\" : [ true, false, false, false, true, false ],\n"
                + "      \"configType\" : \"ConfigValueBooleanArray\"\n"
                + "    },\n"
                + "    \"bytes\" : {\n"
                + "      \"configType\" : \"ConfigValueByteArray\",\n"
                + "      \"value\" : \"Ynl0ZXM=\"\n"
                + "    },\n"
                + "    \"chars\" : {\n"
                + "      \"array\" : [ 65, 66, 67 ],\n"
                + "      \"configType\" : \"ConfigValueCharArray\"\n"
                + "    },\n"
                + "    \"doubles\" : {\n"
                + "      \"array\" : [ 4.9E-324, 1.7976931348623157E308 ],\n"
                + "      \"configType\" : \"ConfigValueDoubleArray\"\n"
                + "    },\n"
                + "    \"floats\" : {\n"
                + "      \"array\" : [ 1.4E-45, 3.4028235E38 ],\n"
                + "      \"configType\" : \"ConfigValueFloatArray\"\n"
                + "    },\n"
                + "    \"ints\" : {\n"
                + "      \"array\" : [ 1, 2, 3, 4, 5, 6, 7, 8, 9 ],\n"
                + "      \"configType\" : \"ConfigValueIntArray\"\n"
                + "    },\n"
                + "    \"longs\" : {\n"
                + "      \"array\" : [ -9223372036854775808, 9223372036854775807 ],\n"
                + "      \"configType\" : \"ConfigValueLongArray\"\n"
                + "    },\n"
                + "    \"shorts\" : {\n"
                + "      \"array\" : [ -32768, 32767 ],\n"
                + "      \"configType\" : \"ConfigValueShortArray\"\n"
                + "    },\n"
                + "    \"org\" : {\n"
                + "      \"children\" : {\n"
                + "        \"knime\" : {\n"
                + "          \"children\" : {\n"
                + "            \"loc\" : {\n"
                + "              \"configType\" : \"ConfigValueLong\",\n"
                + "              \"value\" : 9223372036854775807\n"
                + "            },\n"
                + "            \"core\" : {\n"
                + "              \"children\" : {\n"
                + "                \"util\" : {\n"
                + "                  \"children\" : { },\n"
                + "                  \"configType\" : \"ConfigMap\",\n"
                + "                  \"key\" : \"util\"\n"
                + "                },\n"
                + "                \"node\" : {\n"
                + "                  \"children\" : { },\n"
                + "                  \"configType\" : \"ConfigMap\",\n"
                + "                  \"key\" : \"node\"\n"
                + "                }\n"
                + "              },\n"
                + "              \"configType\" : \"ConfigMap\",\n"
                + "              \"key\" : \"core\"\n"
                + "            }\n"
                + "          },\n"
                + "          \"configType\" : \"ConfigMap\",\n"
                + "          \"key\" : \"knime\"\n"
                + "        }\n"
                + "      },\n"
                + "      \"configType\" : \"ConfigMap\",\n"
                + "      \"key\" : \"org\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"configType\" : \"ConfigMap\",\n"
                + "  \"key\" : \"root\"\n"
                + "}"; //
            String actual = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(def);
            assertEquals("JSON Serialization of NodeSettings (via ConfigDef) returns unexpected output.", expected,
                actual);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        } catch (InvalidSettingsException e) {
            fail(e.getMessage());
        }
    }

}
