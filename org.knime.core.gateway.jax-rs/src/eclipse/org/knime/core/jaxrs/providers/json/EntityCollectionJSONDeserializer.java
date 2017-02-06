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
 *   08.02.2015 (thor): created
 */
package org.knime.core.jaxrs.providers.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.knime.core.gateway.v0.workflow.entity.GatewayEntity;
import org.knime.core.jaxrs.IOClasses;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * REST message body de-/serializer for lists (of entities, {@link GatewayEntity}) that uses Jackson's
 * {@link ObjectMapper} and consumes/produces JSON. Jacson annotations are expected, a derived class with the jackson
 * annotations can be specified by adding the {@link IOClasses} annotation to the respective objects.
 *
 * Note: copied from 'com.knime.enterprise.server.rest.providers.json' and modified.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @author Martin Horn, University of Konstanz
 */
@Produces(MediaType.APPLICATION_JSON)
@Provider
public class EntityCollectionJSONDeserializer implements MessageBodyReader<Collection<?>> {
    private final ObjectMapper m_jsonMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        if (!Collection.class.isAssignableFrom(type)) {
            return false;
        }
        // only accept parameterized lists otherwise the ObjectMapper doesn't know which classes to create
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }
        Type[] typeParameters = ((ParameterizedType)genericType).getActualTypeArguments();
        return (typeParameters.length == 1) && (typeParameters[0] instanceof Class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<?> readFrom(final Class<Collection<?>> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
        final InputStream entityStream) throws IOException, WebApplicationException {
        //        InjectableValues inject = new InjectableValues.Std().addValue(NamespaceRegistry.class, new NamespaceRegistry());

        Class<?> typeParameter = (Class<?>)((ParameterizedType)genericType).getActualTypeArguments()[0];

        //either use type or the type given in the provided annotation implementation (if a @UseClass annotation is present for the parameter)
        Class<Object> subType = Arrays.stream(annotations)
            .filter(a -> IOClasses.class.isAssignableFrom(a.getClass())
                && typeParameter.isAssignableFrom(((IOClasses)a).in()))
            .findFirst().map(a -> (Class<Object>)((IOClasses)a).in()).orElse((Class<Object>)typeParameter);

        ObjectReader reader =
            m_jsonMapper.readerFor(TypeFactory.defaultInstance().constructCollectionType(type, subType));

        JsonParser jp = reader.getFactory().createParser(entityStream);
        jp.disable(Feature.AUTO_CLOSE_SOURCE);
        if (jp.nextToken() == null) {
            return Collections.EMPTY_LIST;
        } else {
            return reader.readValue(jp);
        }
    }
}
