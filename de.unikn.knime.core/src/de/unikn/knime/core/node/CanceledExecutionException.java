/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
package de.unikn.knime.core.node;

/**
 * This exception is use in the <code>ExecutionMonitor</code> when a node's
 * execution has been canceled. If the <code>NodeModel</code> ask the
 * <code>NodeProgressMonitor</code> if a cancel is requested, this method will
 * throw this exception which then leads the process to terminate.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public class CanceledExecutionException extends Exception {

    /**
     * Constructs an <code>CancelExecutionException</code> with the specified
     * detail message.
     * 
     * Use a helpful message here as it will be displayed to the user, and it is
     * the only hint ones gets to correct the problem.
     * 
     * @param s A detail message about the cancelation.
     */
    public CanceledExecutionException(final String s) {
        super(s);
    }

}
