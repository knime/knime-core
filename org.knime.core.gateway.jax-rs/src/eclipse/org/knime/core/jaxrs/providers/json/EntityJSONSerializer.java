package org.knime.core.jaxrs.providers.json;
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.knime.core.gateway.v0.workflow.entity.GatewayEntity;
import org.knime.core.jaxrs.IOClasses;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Mainly serializes entities ({@link GatewayEntity}) to JSON. Jackson annotations of the objects/values to be serialized are
 * expected! If not, an {@link IOClasses}-annotation allows one to specify a derived class that contains the jackson
 * annotations, see {@link #writeTo(Object, Class, Type, Annotation[], MediaType, MultivaluedMap, OutputStream)}.
 *
 * Note: copied from 'com.knime.enterprise.server.rest.providers.json' and modified.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @author Martin Horn, University of Konstanz
 */
@Produces({MediaType.APPLICATION_JSON})
@Provider
public class EntityJSONSerializer implements MessageBodyWriter<Object> {
    @Context
    private HttpHeaders m_requestHeaders;

    private final ObjectMapper m_jsonMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize(final Object value, final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        return -1; // no idea how many bytes we will writ
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        return GatewayEntity.class.isAssignableFrom(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(final Object value, final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
        throws IOException, WebApplicationException {

        Class<?> typeInterface = (Class<?>)genericType;
        //either use type or the type given in the provided annotation implementation (if a @IOClasses annotation is present for the parameter)
        Object wrappedValue = Arrays.stream(annotations).filter(
            a -> IOClasses.class.isAssignableFrom(a.getClass()) && typeInterface.isAssignableFrom(((IOClasses)a).out()))
            .findFirst().map(a -> {
                try {
                    return ((IOClasses)a).out().getDeclaredMethod("wrap", typeInterface).invoke(null, value);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }).orElse(value);
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, mediaType + ";charset=UTF-8");

        m_jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(m_jsonMapper.writeValueAsString(wrappedValue));

        ObjectWriter writer = m_jsonMapper.writerWithView(Views.Default.class);
        writer.writeValue(new CloseShieldOutputStream(entityStream), wrappedValue);
        entityStream.write('\n');
    }
}
