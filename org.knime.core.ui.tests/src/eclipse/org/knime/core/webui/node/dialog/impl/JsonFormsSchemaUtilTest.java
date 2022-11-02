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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.PortObjectSpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */

@SuppressWarnings("unused")
class JsonFormsSchemaUtilTest {

    private static final ObjectMapper MAPPER = JsonFormsDataUtil.getMapper();

    private static class PropertyNameOverrideTestSetting {
        private static String SNAPSHOT = "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0}}";

        int m_test;
    }

    @Test
    void testPropertyNameOverride() throws JsonProcessingException {
        testSettings(PropertyNameOverrideTestSetting.class);
    }

    private static class TitleTestSetting {
        private static String SNAPSHOT =
            "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"title\":\"some title\",\"default\":0}}";

        @Schema(title = "some title")
        int test;
    }

    @Test
    void testTitle() throws JsonProcessingException {
        testSettings(TitleTestSetting.class);
    }

    private static class DescriptionSetting {
        private static String SNAPSHOT =
            "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"description\":\"some description\"}}";

        @Schema(description = "some description")
        int test;
    }

    @Test
    void testDescription() throws JsonProcessingException {
        testSettings(DescriptionSetting.class);
    }

    private static class EnumTestSetting {
        private static String SNAPSHOT = "{\"testEnum\":{\"oneOf\":["//
            + "{\"const\":\"SOME_CHOICE\",\"title\":\"Some choice\"},"//
            + "{\"const\":\"SOME_OTHER_CHOICE\",\"title\":\"second choice\"}"//
            + "]}}";

        enum TestEnum {
                SOME_CHOICE, //
                @Schema(title = "second choice")
                SOME_OTHER_CHOICE
        }

        TestEnum testEnum;
    }

    @Test
    void testEnum() throws JsonProcessingException {
        testSettings(EnumTestSetting.class);
    }

    private static class TestChoices implements ChoicesProvider {
        @Override
        public String[] choices(final PortObjectSpec[] specs) {
            return new String[]{((DataTableSpec)specs[0]).getColumnSpec(0).getName()};
        }
    }

    private static class TestChoicesSetting {
        private static String SNAPSHOT = "{\"test\":{\"oneOf\":[" + //
            "{\"const\":\"some choice\",\"title\":\"some choice\"}" + //
            "]}}";

        @Schema(choices = TestChoices.class)
        String test;
    }

    @Test
    void testChoices() throws JsonProcessingException {
        final var spec = new DataTableSpec(new DataColumnSpecCreator("some choice", StringCell.TYPE).createSpec());
        testSettings(TestChoicesSetting.class, spec);
    }

    private static class TestMultipleChoicesSetting {
        private static String SNAPSHOT = "{\"test\":{\"anyOf\":[" + //
            "{\"const\":\"some choice\",\"title\":\"some choice\"}" + //
            "]}}";

        @Schema(choices = TestChoices.class, multiple = true)
        String[] test;
    }

    @Test
    void testMultipleChoices() throws JsonProcessingException {
        final var spec = new DataTableSpec(new DataColumnSpecCreator("some choice", StringCell.TYPE).createSpec());
        testSettings(TestMultipleChoicesSetting.class, spec);
    }

    private static class MinMaxSetting {
        private static String SNAPSHOT = "{"//
            + "\"testMin\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"minimum\":0},"//
            + "\"testMax\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"maximum\":100.0},"//
            + "\"testBoth\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"minimum\":0,\"maximum\":1000.0},"//
            + "\"testDouble\":{\"type\":\"number\",\"format\":\"double\",\"default\":0.0,\"minimum\":-0.5,\"maximum\":1.5}"//
            + "}";

        @Schema(min = 0)
        int testMin;

        @Schema(max = 100)
        int testMax;

        @Schema(min = 0, max = 1000)
        int testBoth;

        @Schema(min = -0.5, max = 1.5)
        double testDouble;
    }

    @Test
    void testMinMax() throws JsonProcessingException {
        testSettings(MinMaxSetting.class);
    }

    private static class ContainerSetting {
        private static String SNAPSHOT = "{\"testIntArray\":{"//
            + "\"type\":\"array\","//
            + "\"title\":\"foo\","//
            + "\"items\":{\"type\":\"integer\",\"format\":\"int32\"}"//
            + "}}";

        @Schema(title = "foo")
        int[] testIntArray;
    }

    @Test
    void testNoAnnotationsInContainerItems() throws JsonProcessingException {
        testSettings(ContainerSetting.class);
    }

    private static class DefaultSetting {
        private static String SNAPSHOT = "{"//
            + "\"testDouble\":{\"type\":\"number\",\"format\":\"double\",\"default\":0.0},"//
            + "\"testFloat\":{\"type\":\"number\",\"format\":\"float\",\"default\":0.0},"//
            + "\"testInt\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0},"//
            + "\"testLong\":{\"type\":\"integer\",\"format\":\"int64\",\"default\":0},"//
            + "\"testBoolean\":{\"type\":\"boolean\",\"default\":false},"//
            + "\"testNoDefault\":{\"type\":\"string\"},"//
            + "\"testString\":{\"type\":\"string\",\"default\":\"foo\"},"//
            + "\"testArray\":{\"type\":\"array\",\"default\":[{\"testInt\":0}],"//
            + "\"items\":{\"type\":\"object\",\"properties\":"
            + "{\"testInt\":{\"default\":0,\"type\":\"integer\",\"format\":\"int32\"}}}}"//
            + "}";

        double testDouble;

        float testFloat;

        int testInt;

        long testLong;

        boolean testBoolean;

        String testNoDefault;

        String testString = "foo";

        IntWithDefault[] testArray = {new IntWithDefault()};
    }

    private static class IntWithDefault {
        public int testInt;
    }

    @Test
    void testDefault() throws JsonProcessingException {
        testSettings(DefaultSetting.class);
    }

    private static class IgnoreSetting {
        private static String SNAPSHOT = "{\"testInt\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0}}";

        int testInt;

        Boolean testBoxedBoolean;

        Integer testBoxedInteger;

        Long testBoxedlong;

        short testShort;

        Short testBoxedShort;

        Double testBoxedDouble;

        Float testBoxedFloat;
    }

    @Test
    void testIgnore() throws JsonProcessingException {
        testSettings(IgnoreSetting.class);
    }

    private static void testSettings(final Class<?> settingsClass, final PortObjectSpec... specs)
        throws JsonMappingException, JsonProcessingException {
        try {
            assertThatJson(MAPPER.readTree(getProperties(settingsClass, specs).toString()))
                .isEqualTo(MAPPER.readTree((String)settingsClass.getDeclaredField("SNAPSHOT").get(null)));
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            Assertions.fail("Problem accessing the SNAPSHOT of settings class " + settingsClass.getSimpleName()
                + " (most likely a problem of the test implementation itself)");
        }
    }

    private static JsonNode getProperties(final Class<?> clazz, final PortObjectSpec... specs) {
        return JsonFormsSchemaUtil.buildSchema(clazz, specs).get("properties");
    }

}
