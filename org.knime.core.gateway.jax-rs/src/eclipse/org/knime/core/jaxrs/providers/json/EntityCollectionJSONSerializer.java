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
 *   20.05.2014 (thor): created
 */
package org.knime.core.jaxrs.providers.json;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.knime.core.gateway.v0.workflow.entity.GatewayEntity;
import org.knime.core.jaxrs.IOClasses;

import com.fasterxml.jackson.databind.ObjectMapper;

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
public class EntityCollectionJSONSerializer implements MessageBodyWriter<Collection<?>> {
    private final ObjectMapper m_jsonMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize(final Collection<?> value, final Class<?> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType) {
        return -1; // no idea how many byte we will write
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        if (!Collection.class.isAssignableFrom(type)) {
            return false;
        }
        // only write parameterized lists (otherwise the de-serializer will choke because it doesn't know which
        // classes to create)
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }

        // make sure we only write lists that are parameterized with a class and nothing fancy
        Type[] typeParameters = ((ParameterizedType)genericType).getActualTypeArguments();
        return (typeParameters.length == 1) && (typeParameters[0] instanceof Class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(final Collection<?> value, final Class<?> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType,
        final MultivaluedMap<java.lang.String, java.lang.Object> httpHeaders, final OutputStream entityStream)
        throws IOException, WebApplicationException {

        Class<?> typeParameter = (Class<?>)((ParameterizedType)genericType).getActualTypeArguments()[0];

        //either use type or the type given in the provided annotation implementation (if a @IOClasses annotation is present for the parameter)
        Optional<Class<?>> wrapperClass = Arrays.stream(annotations).filter(
            a -> IOClasses.class.isAssignableFrom(a.getClass()) && typeParameter.isAssignableFrom(((IOClasses)a).out()))
            .findFirst().map(a -> {
                return ((IOClasses)a).out();
            });
        Collection<?> wrappedValues;
        if (wrapperClass.isPresent()) {
            wrappedValues = value.stream().map(o -> {
                try {
                    return wrapperClass.get().getConstructor(typeParameter).newInstance(o);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }).collect(Collectors.toList());
        } else {
            wrappedValues = value;
        }

        m_jsonMapper.writeValue(new CloseShieldOutputStream(entityStream), wrappedValues);
        entityStream.write('\n');
    }
}
