/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   16.02.2015 (thor): created
 */
package org.knime.core.jaxrs.providers.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * TODO doesn't work for maps, yet
 *
 * Note: copied from 'com.knime.enterprise.server.rest.providers.json' and modified.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("rawtypes")
@Consumes(MediaType.APPLICATION_JSON)
@Provider
public class MapJSONDeserializer implements MessageBodyReader<Map> {
    // note that we use the non-generic "Map" type because otherwise CXF is not able to rank this deserializer correctly
    private final ObjectMapper m_jsonMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        if (!Map.class.isAssignableFrom(type)) {
            return false;
        }
        // only accept parameterized lists otherwise the ObjectMapper doesn't know which classes to create
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }
        Type[] typeParameters = ((ParameterizedType)genericType).getActualTypeArguments();
        return (typeParameters.length == 2) && (typeParameters[0] instanceof Class)
            && (typeParameters[1] instanceof Class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<?, ?> readFrom(final Class<Map> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
        throws IOException, WebApplicationException {
        Type[] typeParameters = ((ParameterizedType)genericType).getActualTypeArguments();

        TypeFactory typeFactory = TypeFactory.defaultInstance();
        JavaType keyType = typeFactory.constructType(typeParameters[0]);
        JavaType valueType = typeFactory.constructType(typeParameters[1]);
        if ((valueType instanceof MapType) && (((MapType)valueType).getRawClass() == JsonValue.class)) {
            valueType = typeFactory.constructSimpleType(JsonValue.class, new JavaType[0]);
        }

        ObjectReader reader =
            m_jsonMapper.readerFor(TypeFactory.defaultInstance().constructMapType(HashMap.class, keyType, valueType));
        JsonParser jp = reader.getFactory().createParser(entityStream);
        jp.disable(Feature.AUTO_CLOSE_SOURCE);
        if (jp.nextToken() == null) {
            return Collections.EMPTY_MAP;
        } else {
            return reader.readValue(jp);
        }
    }
}
