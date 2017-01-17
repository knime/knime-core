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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * TODO do not copy that code
 * WARNING this file has been modified
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
@Produces({MediaType.APPLICATION_JSON})
@Provider
public class GenericJSONSerializer implements MessageBodyWriter<Object> {
    @Context
    private HttpHeaders m_requestHeaders;

    private final ObjectMapper m_jsonMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize(final Object value, final Class<?> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType) {
        return -1; // no idea how many bytes we will write
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
        final MediaType mediaType) {
        return !StreamingOutput.class.isAssignableFrom(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(final Object value, final Class<?> type, final Type genericType,
        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
        final OutputStream entityStream) throws IOException, WebApplicationException {
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, mediaType + ";charset=UTF-8");

        if (value instanceof InputStream) {
            IOUtils.copyLarge((InputStream)value, entityStream);
            ((InputStream)value).close();
        } else if (value instanceof File) {
            httpHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, ((File) value).length());
            try (InputStream is = new FileInputStream((File) value)) {
                IOUtils.copyLarge(is, entityStream);
            }
        } else if (value instanceof Path) {
            httpHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, Files.size((Path) value));
            try (InputStream is = Files.newInputStream((Path) value)) {
                IOUtils.copyLarge(is, entityStream);
            }
        } else {
            httpHeaders.putSingle("KNIME-Class", value.getClass().getName());
            ObjectWriter writer = m_jsonMapper.writerWithView(Views.Default.class);
            List<String> preferHeader = m_requestHeaders.getRequestHeader("Prefer");
            if ((preferHeader == null) || !preferHeader.stream().anyMatch(h -> h.contains("representation=minimal"))) {
                writer = writer.withView(Views.Complete.class);
            }

            writer.writeValue(new CloseShieldOutputStream(entityStream), value);
            entityStream.write('\n');
        }
    }
}
