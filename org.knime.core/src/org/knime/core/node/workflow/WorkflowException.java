/*
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.core.node.workflow;


/**
 * This is a exception for exception that can occur while processing a workflow.
 * Like with {@link java.sql.SQLException} it is possible to contruct a chain of
 * exceptions that are all combined in this exception.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class WorkflowException extends Exception {
    private WorkflowException m_next;
    
    /**
     * Creates a new workflow exception.
     * 
     * @param message a message for the exception
     * @param predecessor the predecessor exception, must not be
     * <code>null</code>
     * @param cause the real cause of the exception
     * @throws NullPointerException if predecessor is <code>null</code>
     */
    public WorkflowException(final String message,
            final WorkflowException predecessor, final Throwable cause) {
        super(message, cause);
        predecessor.m_next = this;
    }

    /**
     * Creates a new workflow exception. This exception is the first of a chain
     * of workflow exception.
     * 
     * @param message a message for the exception
     */
    public WorkflowException(final String message) {
        super(message);
    }

    
    /**
     * Returns the next exception in the chain or <code>null</code> if there are
     * no more exceptions.
     * 
     * @return the next exception
     */
    public WorkflowException getNextException() {
        return m_next;
    }
}
