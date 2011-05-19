/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: May 12, 2011
 * Author: ohl
 */
package org.knime.core.util;

/**
 * Used to indicate that the attempt to acquire a lock on a file or directory
 * failed.
 *
 * @author ohl, University of Konstanz
 */
public class LockFailedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 12052011L;

    /**
     *
     */
    public LockFailedException() {
        super();
    }

    /**
     * @param message the message
     * @param cause the underlying cause
     */
    public LockFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message for the user
     */
    public LockFailedException(final String message) {
        super(message);
    }

    /**
     * @param cause the underlying exception
     */
    public LockFailedException(final Throwable cause) {
        super(cause);
    }

}
