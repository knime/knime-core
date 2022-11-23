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
 *   19 Oct 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.impl;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings.SettingsCreationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;

/**
 * Utility class for creating schema content from a settings POJO class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class JsonFormsSchemaUtil {

    private static final Set<Class<?>> PROHIBITED_TYPES =
        Stream.of(Boolean.class, Integer.class, Long.class, short.class, Short.class, Double.class, Float.class)
            .collect(Collectors.toCollection(HashSet::new));

    static final SchemaVersion VERSION = SchemaVersion.DRAFT_2019_09;

    static final String TAG_TYPE = SchemaKeyword.TAG_TYPE.forVersion(VERSION);

    static final String TAG_FORMAT = SchemaKeyword.TAG_FORMAT.forVersion(VERSION);

    static final String TAG_TYPE_NULL = SchemaKeyword.TAG_TYPE_NULL.forVersion(VERSION);

    static final String TAG_TYPE_ARRAY = SchemaKeyword.TAG_TYPE_ARRAY.forVersion(VERSION);

    static final String TAG_TYPE_OBJECT = SchemaKeyword.TAG_TYPE_OBJECT.forVersion(VERSION);

    static final String TAG_TYPE_BOOLEAN = SchemaKeyword.TAG_TYPE_BOOLEAN.forVersion(VERSION);

    static final String TAG_TYPE_STRING = SchemaKeyword.TAG_TYPE_STRING.forVersion(VERSION);

    static final String TAG_TYPE_INTEGER = SchemaKeyword.TAG_TYPE_INTEGER.forVersion(VERSION);

    static final String TAG_TYPE_NUMBER = SchemaKeyword.TAG_TYPE_NUMBER.forVersion(VERSION);

    static final String TAG_FORMAT_INT = "int32";

    static final String TAG_FORMAT_LONG = "int64";

    static final String TAG_FORMAT_FLOAT = "float";

    static final String TAG_FORMAT_DOUBLE = "double";

    static final String TAG_PROPERTIES = SchemaKeyword.TAG_PROPERTIES.forVersion(VERSION);

    static final String TAG_ITEMS = SchemaKeyword.TAG_ITEMS.forVersion(VERSION);

    static final String TAG_ALLOF = SchemaKeyword.TAG_ALLOF.forVersion(VERSION);

    static final String TAG_ANYOF = SchemaKeyword.TAG_ANYOF.forVersion(VERSION);

    static final String TAG_ONEOF = SchemaKeyword.TAG_ONEOF.forVersion(VERSION);

    static final String TAG_TITLE = SchemaKeyword.TAG_TITLE.forVersion(VERSION);

    static final String TAG_CONST = SchemaKeyword.TAG_CONST.forVersion(VERSION);

    private JsonFormsSchemaUtil() {
        // utility class
    }

    static JsonNode buildCombinedSchema(final Map<String, Class<? extends DefaultNodeSettings>> settingsClasses,
        final Map<String, DefaultNodeSettings> settings, final SettingsCreationContext context) {
        final var root = JsonFormsDataUtil.getMapper().createObjectNode();
        root.put(TAG_TYPE, TAG_TYPE_OBJECT);
        final var properties = root.putObject(TAG_PROPERTIES);
        settingsClasses.entrySet().stream()
            .forEach(e -> properties.set(e.getKey(), buildSchema(e.getValue(), settings.get(e.getKey()), context)));
        return root;
    }

    /**
     * Build an incomplete schema from a provided POJO class. The settings are incomplete, since they might be missing
     * some default values and oneOf / anyOf choices, which can only be derived from port object specs.
     *
     * @param settingsClass a POJO class for which to build the schema
     * @return an incomplete json schema
     */
    static synchronized ObjectNode buildSchema(final Class<?> settingsClass) {
        return buildSchema(settingsClass, null, null);
    }

    static synchronized ObjectNode buildSchema(final Class<?> settingsClass, final SettingsCreationContext context) {
        return buildSchema(settingsClass, null, context);
    }

    static synchronized ObjectNode buildSchema(final Class<?> settingsClass, final DefaultNodeSettings settings,
        final SettingsCreationContext context) {
        final var builder = new SchemaGeneratorConfigBuilder(JsonFormsDataUtil.getMapper(), VERSION, new OptionPreset(//
            Option.FLATTENED_ENUMS, //
            Option.EXTRA_OPEN_API_FORMAT_VALUES, //
            Option.PUBLIC_NONSTATIC_FIELDS, //
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS, //
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS, //
            Option.INLINE_ALL_SCHEMAS, //
            Option.ALLOF_CLEANUP_AT_THE_END));

        builder.forFields().withIgnoreCheck(f -> PROHIBITED_TYPES.contains(f.getType().getErasedType()));

        builder.forFields().withCustomDefinitionProvider(new ChoicesAndEnumDefinitionProvider(context, settings));

        builder.forFields().withDefaultResolver(new DefaultResolver(context));

        builder.forFields()
            .withTitleResolver(field -> Optional.ofNullable(field.getAnnotationConsideringFieldAndGetter(Schema.class))
                .map(Schema::title).filter(l -> !field.isFakeContainerItemScope() && !l.isEmpty()).orElse(null));

        builder.forFields()
            .withDescriptionResolver(field -> Optional
                .ofNullable(field.getAnnotationConsideringFieldAndGetter(Schema.class)).map(Schema::description)
                .filter(d -> !field.isFakeContainerItemScope() && !d.isEmpty()).orElse(null));

        builder.forFields().withNumberInclusiveMinimumResolver(
            field -> Optional.ofNullable(field.getAnnotationConsideringFieldAndGetter(Schema.class)).map(Schema::min)
                .filter(min -> !field.isFakeContainerItemScope() && !Double.isNaN(min)).map(BigDecimal::valueOf)
                .orElse(null));

        builder.forFields().withNumberInclusiveMaximumResolver(
            field -> Optional.ofNullable(field.getAnnotationConsideringFieldAndGetter(Schema.class)).map(Schema::max)
                .filter(max -> !field.isFakeContainerItemScope() && !Double.isNaN(max)).map(BigDecimal::valueOf)
                .orElse(null));

        builder.forFields().withPropertyNameOverrideResolver(
            field -> field.getName().startsWith("m_") ? field.getName().substring(2) : field.getName());

        return new SchemaGenerator(builder.build()).generateSchema(settingsClass);
    }

}
