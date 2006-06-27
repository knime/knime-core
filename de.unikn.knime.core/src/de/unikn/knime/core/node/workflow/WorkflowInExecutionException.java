/* Created on Jun 26, 2006 4:10:35 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.core.node.workflow;

/**
 * This exception is thrown if the workflow is currently being executed and
 * a forbidden operation was called.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class WorkflowInExecutionException extends Exception {
    /**
     * Creates a new exception.
     * 
     * @param message a message with details
     */
    public WorkflowInExecutionException(final String message) {
        super(message);
    }
}
