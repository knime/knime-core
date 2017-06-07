/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
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
 *   Created on 29.04.2015 by thor
 */
package org.knime.core.jaxrs.providers.exception;

import java.util.NoSuchElementException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Maps {@link NoSuchElementException}s to a <tt>404 Not Found</tt> status code.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class NoSuchElementExceptionMapper implements ExceptionMapper<NoSuchElementException> {
    /**
     * {@inheritDoc}
     */
    @Override
    public Response toResponse(final NoSuchElementException exception) {
        return Response.status(Status.NOT_FOUND).entity(exception.getMessage() + "\n").type("text/plain")
            .header("Knime-Gateway-Exception", exception.getClass().getName()).build();
    }
}
