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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Utility class mainly for creating json-forms data content from a {@link DefaultNodeSettings} POJO and vice-versa.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class JsonFormsDataUtil {

    private static ObjectMapper MAPPER; // NOSONAR

    private JsonFormsDataUtil() {
        //utility class
    }

    private static ObjectMapper createMapper() {
        final var mapper = new ObjectMapper();

        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(createDialogModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NON_PRIVATE);
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public String nameForField(final MapperConfig<?> config, final AnnotatedField field,
                final String defaultName) {
                return defaultName.startsWith("m_") ? defaultName.substring(2) : defaultName;
            }
        });

        return mapper;
    }

    private static SimpleModule createDialogModule() {
        final var module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        return module;
    }

    static ObjectMapper getMapper() {
        if (MAPPER == null) {
            MAPPER = createMapper();
        }
        return MAPPER;
    }

    static JsonNode toJsonData(final DefaultNodeSettings settings) {
        return getMapper().valueToTree(settings);
    }

    static JsonNode toCombinedJsonData(final Map<String, DefaultNodeSettings> settings) {
        final var root = getMapper().createObjectNode();
        settings.entrySet().stream().forEach(e -> root.set(e.getKey(), toJsonData(e.getValue())));
        return root;
    }

    static <T extends DefaultNodeSettings> T toDefaultNodeSettings(final JsonNode jsonFormsData, final Class<T> clazz) {
        try {
            return getMapper().treeToValue(jsonFormsData, clazz);
        } catch (JsonProcessingException e) {
            NodeLogger.getLogger(JsonFormsDataUtil.class)
                .error(String.format("Error when creating class %s from settings. Error message is: %s.",
                    clazz.getName(), e.getMessage()), e);
            return null;
        }
    }

    static <T extends DefaultNodeSettings> T createDefaultNodeSettings(final Class<T> clazz,
        final PortObjectSpec[] specs) {
        @SuppressWarnings("unchecked")
        final var settings = (T)createInstanceWithSpecs(clazz, specs);
        return settings;
    }

    static Object createInstanceWithSpecs(final Class<?> clazz, final PortObjectSpec[] specs) {
        try {
            return createInstance(clazz.getDeclaredConstructor(PortObjectSpec[].class), (Object)specs);
        } catch (NoSuchMethodException ex) { // NOSONAR
        }
        return createInstance(clazz);
    }

    static <T> T createInstance(final Class<T> clazz) {
        try {
            return createInstance(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            NodeLogger.getLogger(JsonFormsDataUtil.class)
                .error(String.format("No default constructor found for class %s.", clazz.getName()), e);
            return null;
        }
    }

    private static <T> T createInstance(final Constructor<T> constructor, final Object... initArgs) {
        constructor.setAccessible(true); // NOSONAR
        try {
            return constructor.newInstance(initArgs);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            NodeLogger.getLogger(JsonFormsDataUtil.class)
                .error(String.format("Failed to instantiate class %s.", constructor.getDeclaringClass().getName()), e);
            return null;
        }
    }

    private static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final BigDecimal value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
            gen.writeNumber(value.toPlainString());
        }

    }

}
