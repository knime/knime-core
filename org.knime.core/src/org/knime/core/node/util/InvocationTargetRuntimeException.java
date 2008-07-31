/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   07.01.2008 (ohl): created
 */
package org.knime.core.node.util;

/**
 * Thrown by the methods in the {@link ViewUtils} class that execute code in the
 * Event Dispatch Thread, if the executed code throws an exception. The original
 * exception of the executed code can be retrieved through the
 * {@link #getCause()} method.
 * 
 * @author ohl, University of Konstanz
 */
public class InvocationTargetRuntimeException extends RuntimeException {

    /**
     * Creates a new exception, wrapping the cause in a runtime exception.
     * 
     * @param msg the message for the wrapper
     * @param cause the original exception, must not be null
     * @throws NullPointerException if the specified cause is null
     */
    InvocationTargetRuntimeException(final String msg, final Throwable cause) {
        super(msg, cause);
        if (cause == null) {
            throw new NullPointerException("Cause can't be null");
        }
    }
}
