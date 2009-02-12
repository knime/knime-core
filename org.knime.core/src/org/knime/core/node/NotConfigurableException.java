/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * This exception is thrown if a node dialog cannot be opened for various
 * reasons, e.g. a missing predecessor node, a missing column, ...
 *  
 * @author Thorsten Meinl, University of Konstanz
 */
public class NotConfigurableException extends Exception {
    /**
     * Creates a new NotConfigurableException.
     * @param message a meaningful message why the configuration dialog
     * cannot be opened
     */
    public NotConfigurableException(final String message) {
        super(message);
    }
    
    /** Creates a new NotConfigurableException with a given cause.
     * @param message a meaningful message why the configuration 
     *      dialog cannot be opened
     * @param cause The reason why the configuration failed.
     */
    public NotConfigurableException(final String message, 
            final Throwable cause) {
        super(message, cause);
    }
}
