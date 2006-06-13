/* Created on Jun 13, 2006 3:50:18 PM by thor
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
 * This exception is thrown if a node dialog cannot be opened for various
 * reasons, e.g. a missing predecessor node, a missing column, ...
 *  
 * @author Thorsten Meinl, University of Konstanz
 */
public class NotConfigurableException extends Exception {
    /**
     * Creates a new NotConfigurableException.
     * 
     * @param message a meaningfull message why the configuration dialog
     * cannot be opened
     */
    public NotConfigurableException(final String message) {
        super(message);
    }
}
