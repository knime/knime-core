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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.node.workflow;

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
