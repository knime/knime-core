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
 * This exception is thrown during load and save settings within the dialog's
 * pane and model to indicate that the parameters set are not valid and can't be
 * applied. This exception can be catched in order to show the problem in the
 * settings.
 * 
 * @see org.knime.core.node.NodeDialogPane
 * @see org.knime.core.node.NodeModel
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class InvalidSettingsException extends Exception {
    /**
     * Constructs an <code>InvalidSettingsException</code> with the specified
     * detail message. Use a helpful message here as it will be displayed to the
     * user, and it is the only hint ones understands to actual the problem.
     * 
     * @param s the detail message.
     */
    public InvalidSettingsException(final String s) {
        super(s);
    }

    
    /**
     * Constructs an <code>InvalidSettingsException</code> with the specified
     * cause.
     * 
     * @param cause the original cause of the execption
     */
    public InvalidSettingsException(final Throwable cause) {
        super(cause);
    }


    /**
     * Constructs an <code>InvalidSettingsException</code> with the specified
     * detail message and a cause. Use a helpful message here as it will be
     * displayed to the user, and it is the only hint ones understands to actual
     * the problem.

     * @param msg the detail message
     * @param cause the root cause
     */
    public InvalidSettingsException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
