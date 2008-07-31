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
package org.knime.core.node;

/**
 * This exception is used in the
 * {@link org.knime.core.node.ExecutionMonitor} when a node's
 * execution has been canceled. If the
 * {@link org.knime.core.node.NodeModel} ask the
 * {@link org.knime.core.node.NodeProgressMonitor} if a cancel is
 * requested, this method will throw this exception which then leads the
 * process to terminate.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class CanceledExecutionException extends Exception {
    
    /**
     * Creates a new exception of this type with an error message.
     */
    public CanceledExecutionException() {
        super();
    }
    
    /**
     * Constructs an <code>CancelExecutionException</code> with the specified
     * detail message.
     * 
     * Use a helpful message here as it will be displayed to the user, and it is
     * the only hint ones gets to correct the problem.
     * 
     * @param s a detail message about the cancelation
     */
    public CanceledExecutionException(final String s) {
        super(s);
    }
}
